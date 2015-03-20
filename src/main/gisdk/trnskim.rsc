/********************************************************************************
transit skim matrix to create skim matrix files
 skim values includes fare, in vehicle time, initial wait time,
 transfer wait time, tranfer walk time, access time, egress time
 dwell time, transfer penalty, # of transfers
 *TMO, or *TMP (depends on which time period)
for premium service skim *TMO, or *TMP by mode, additional cores were created
input files: transit.dbd - transit line layer
   transitrt.rts - transit route layer
   localop.tnw - local bus off peak transit network
   localpk.tnw - local bus peak transit network
   premop.tnw - premium bus service off peak transit network
   prempk.tnw - premium bus service peak transit network
   modenu.dbf - dbf file for mode table
   fare.mtx - fare matrix for zonal fares for lightrail and coaster

output files:
   implbopx2.mtx - skim matrix file for local bus off peak service
   implbpkx2.mtx - skim matrix file for local bus peak service
   imppropx2.mtx - skim matrix file for premium bus off peak service
   impprpkx2.mtx - skim matrix file for premium bus peak service
     implr.mtx - skim ltrzone 
   impcr.mtx - skim crzone 
history      
   10/8/03    zou to change the fare system to flat fare and skim 
         rail link fields separately.
   9/3/04      retain the commuter rail from the light rail category 
   1/25/05      rewrite the script using moduels
      4/14/09         ZOU changed it to 3tod         
********************************************************************************/
Macro "Create transit network"

   shared path,inputDir, outputDir, mxtap
   ok=RunMacro("Create transit networks") 
     if !ok then goto quit
   Return(1)
   
   quit:
      Return(0) 
EndMacro

/***********************************************************************************
*******************************************************************/

Macro "Build transit skims"

   shared path,inputDir, outputDir, mxtap
  
   ok = RunMacro("Update transit time fields")  
   if !ok then goto quit  
   
   /*
   ok=RunMacro("Create rail net")  
   if !ok then goto quit
   */
   ok=RunMacro("Create transit networks") 
   if !ok then goto quit

   ok = RunMacro("Skim transit networks")  
   if !ok then goto quit
  
  /*
   ok= RunMacro("zero null ivt time")
   if !ok then goto quit
*/
   ok= RunMacro("Process transit skims") 
   if !ok then goto quit

   Return(1)
   
   quit:
      Return(0) 
EndMacro
/***********************************************************************************


Inputs:
   input\mode5tod.dbf      Mode table
   output\transit.dbd      Transit line layer
   output\xxxx_yy.tnw      Transit networks
   
   where xxxxx is transit mode (locl or prem) and yy is period (EA, AM, MD, PM, EV)
   
Outputs:
   impxxxx_yy.mtx          Transit skims
   
   where xxxxx is transit mode (locl or prem) and yy is period (EA, AM, MD, PM, EV)

***********************************************************************************/
Macro "Skim transit networks" 
  
   shared path,inputDir, outputDir, mxtap
   NumofCPU=2
  
   mode_tb = inputDir+"\\mode5tod.dbf"
   // xfer_tb = path+"\\modexfer.dbf"
   db_file = outputDir + "\\transit.dbd"
   {node_lyr, link_lyr} = RunMacro("TCB Add DB Layers", db_file)
   ok = (node_lyr <> null && link_lyr <> null)
   if !ok then goto quit

   db_node_lyr = db_file + "|" + node_lyr

   periods = {"_EA","_AM","_MD","_PM","_EV"}
   modes = {"prem","locl"}
   
   //varies by modes
   skmodes={{4,5,6,7,8,9,10},}
   skvar={{"Fare", "Initial Wait Time", "Transfer Wait Time", "Transfer Walk Time", "Access Walk Time","Egress Walk Time","Dwelling Time", "Number of Transfers"},
         {"Fare", "In-Vehicle Time", "Initial Wait Time", "Transfer Wait Time", "Transfer Walk Time","Access Walk Time","Egress Walk Time", "Dwelling Time", "Number of Transfers"}}
   
   // not sure what the PRE and LOC time are yet
   skvars={skvar[1]+{"Length","*TM"},skvar[2]} 
   
   
   for i = 1 to periods.length do
      for j = 1 to modes.length do   
         Opts = null
   //      Opts.Global.[Force Threads] = NumofCPU
         Opts.Input.Database = db_file
         Opts.Input.Network = outputDir+"\\"+modes[j]+periods[i]+".tnw" 
         Opts.Input.[Origin Set] = {db_node_lyr, node_lyr, "Selection", "Select * where id <"+i2s(mxtap)}
         Opts.Input.[Destination Set] = {db_node_lyr, node_lyr, "Selection"}
         Opts.Input.[Mode Table] = {mode_tb}
         // Opts.Input.[Mode Xfer Table] = {xfer_tb}
         Opts.Global.[Skim Var] = skvars[j]
         Opts.Global.[OD Layer Type] = 2
         if skmodes<> null then Opts.Global.[Skim Modes] = skmodes[j]
         Opts.Flag.[Do Skimming] = 1
         Opts.Flag.[Do Maximum Fare] = 1 //added for 4.8 build 401
         Opts.Output.[Skim Matrix].Label = "Skim Matrix ("+modes[j]+periods[i]+")"
         Opts.Output.[Skim Matrix].Compression = 0 //uncompressed
         Opts.Output.[Skim Matrix].[File Name] = outputDir+"\\imp"+modes[j]+periods[i]+".mtx"
       //  Opts.Output.[TPS Table] = outputDir+"\\"+modes[j]+periods[i]+".tps"
         ok = RunMacro("TCB Run Procedure", i, "Transit Skim PF", Opts)
         
         if !ok then goto quit
      end
   end                   

   ok=1
   quit:
      RunMacro("close all")
      Return(ok)
EndMacro
/***********************************************************************************


Inputs:
   input\mode5tod.dbf
   output\transit.dbd
   
***********************************************************************************/
Macro "Special transit skims" (arr)
  
   shared path,inputDir, outputDir, mxtap
  
   skimvar=arr[1]

   mode_tb = inputDir+"\\mode5tod.dbf"
   // xfer_tb = path+"\\modexfer.dbf"
   db_file = outputDir + "\\transit.dbd"
   {node_lyr, link_lyr} = RunMacro("TCB Add DB Layers", db_file)
   ok = (node_lyr <> null && link_lyr <> null)
   if !ok then goto quit

   db_node_lyr = db_file + "|" + node_lyr

   fpr=openfile(path+"\\hwycad.log","a")
   mytime=GetDateAndTime() 
   writeline(fpr,mytime+", skim trnets")
  
   if skimvar="rail" then do //skim lrtzone and crzone to get the trolley and coaster fares   
      skvars={{"lrtzone"},{"Crzone"}}
      trskimmtxs={"implr.mtx","impcr.mtx"}
      trnets={"lr.tnw","cr.tnw"}
      mtxdes={"Skim Matrix (light Rail)","Skim Matrix (coaster)"}
   end 
   else if skimvar="fwylen" then do //skim length and fwylen
      skvars={{"length","fwylen"},{"length","fwylen"}}
      trskimmtxs={"imppropdst2.mtx","impprpkdst2.mtx"}
      trnets={"prem_MD.tnw","prem_AM.tnw"}
      mtxdes={"Length (Shortest OP Premium Path)","Length (Shortest PK Premium Path)"}
   end

   for i = 1 to trnets.length do        
     Opts = null
     Opts.Global.[Force Threads] = NumofCPU
     Opts.Input.Database = db_file
     Opts.Input.Network = path+"\\"+trnets[i]
     Opts.Input.[Origin Set] = {db_node_lyr, node_lyr, "Selection", "Select * where id <"+i2s(mxtap)}
     Opts.Input.[Destination Set] = {db_node_lyr, node_lyr, "Selection"}
     Opts.Input.[Mode Table] = {mode_tb}
    // Opts.Input.[Mode Xfer Table] = {xfer_tb}
     Opts.Global.[Skim Var] = skvars[i]
     Opts.Global.[OD Layer Type] = 2
     if skmodes<> null then Opts.Global.[Skim Modes] = skmodes[i]
     Opts.Flag.[Do Skimming] = 1
     Opts.Flag.[Do Maximum Fare] = 1 //added for 4.8 build 401
     Opts.Output.[Skim Matrix].Label = mtxdes[i]
     Opts.Output.[Skim Matrix].Compression = 0 //uncompressed
     Opts.Output.[Skim Matrix].[File Name] = path+"\\"+trskimmtxs[i]
     ok = RunMacro("TCB Run Procedure", i, "Transit Skim PF", Opts)
    // ok = RunMacro("TCB Run Procedure", i, "Transit Skim Max Fare", Opts)//using maximized fare

     if !ok then goto quit
   end                   

   ok=1
   quit:
      RunMacro("close all")
      Return(ok)
EndMacro

/***************************************************************************************************************************
This macro puts zeros in the null cells for unprocessed transit skims: premium and local modes
3/19/2015 Wu modified to zero out all ivts
****************************************************************************************************************************/
Macro "zero null ivt time"
  
     shared path, outputDir   
   periods = {"_EA","_AM","_MD","_PM","_EV"}

      for i=1 to periods.length do
                        
         //open matrix
         fileNameSkimP = outputDir + "\\impprem"+periods[i]+".mtx"
         mp = OpenMatrix(fileNameSkimP,)
         currsp= CreateMatrixCurrencies(mp, , , )
	 currsp.("*TM (Local)"):=Nz(currsp.("*TM (Local)"))
	 currsp.("*TM (Commuter Rail)"):=Nz(currsp.("*TM (Commuter Rail)"))
	 currsp.("*TM (Light Rail)"):=Nz(currsp.("*TM (Light Rail)"))
	 currsp.("*TM (Regional BRT (Yello)"):=Nz(currsp.("*TM (Regional BRT (Yello)"))
	 currsp.("*TM (Regional BRT (Red))"):=Nz(currsp.("*TM (Regional BRT (Red))"))
	 currsp.("*TM (Limited Express)"):=Nz(currsp.("*TM (Limited Express)"))
	 currsp.("*TM (Express)"):=Nz(currsp.("*TM (Express)"))

         fileNameSkimL = outputDir + "\\implocl"+periods[i]+".mtx"
         ml = OpenMatrix(fileNameSkimL,)
         currsl= CreateMatrixCurrencies(ml, , , )
	 currsl.("In-Vehicle Time"):=Nz(currsl.("In-Vehicle Time"))
      end
   quit:
      Return(1)
EndMacro

/***********************************************************************************************************************************
 Extract Main Mode from Transit Skims

 This macro: 
------------
 1- extracts the main mode from transit skims
 2- writes main mode as a core to output transit matrix

 Inputs: 
----------

  output\impxxxx_yy.mtx

  where:
     xxxx is mode (locl or prem)
     yy is time period (EA, AM, MD, PM, NT)


 Outputs:


  output\impxxxx_yyo.mtx

  where:
     xxxx is mode (locl or prem)
     yy is time period (EA, AM, MD, PM, NT)


 Input transit modes: 
----------------------
 CR, LR, BRT Yellow, BRT Red, Limited EXP, EXP, LB

 Output transit modes:
-----------------------
 1-CR                        Mode choice code 4
 2-LR                        Mode choice code 5
 3-BRT (BRT Yellow+BRT Red)  Mode choice code 6
 4-EXP (EXP+Limited EXP)     Mode choice code 7
 5-LB                        Mode choice code 8

 Input-output Matrix cores correspondence table:
-------------------------------------------------
 1) Premium input-output
 Input                                                              Output
 Fare(1)                                                            Fare(1)
 Initial Wait Time(2)                                               Initial Wait Time(2)
 Transfer Wait Time(3)                                              Transfer Wait Time(3)
 Transfer Walk Time(4)+Access Walk Time(5)+Egress Walk Time(6)      Walk Time(4)
 Dwelling time(7)                                                   ------
 Number of Transfers(8)                                             Number of Transfers(5)
 Length:CR(9)                                                       Length:CR(6)
 Length:LR(10)                                                      Length:LR(7)
 Length:BRT Yellow(11)+BRT Red(12)                                  Length:BRT(8)
 Length:Limited EXP(13)+EXP(14)                                     Length:EXP(9)
 Length:LB(15)                                                      Length:LB(10)
 IVT:CR(16)                                                         IVT:CR(11)
 IVT:LR(17)                                                         IVT:LR(12)
 IVT:BRT Yellow(18)+BRT Red(19)                                     IVT:BRT(13)
 IVT:Limited EXP(20)+EXP(21)                                        IVT:EXP(14)
 IVT:LB(22)                                                         IVT:LB(15)                        
 ---                                                                IVT:Sum(16)                     
 ---                                                                IVT:Main Mode(17)

 2) Local input-output
 Input                                                              Output
 Fare(1)                                                            Fare(1)
 In-Vehicle Time(2)+Dwelling Time(8)                                Total IV Time(2)
 Initial Wait Time(3)                                               Initial Wait Time(3)
 Transfer Wait Time(4)                                              Transfer Wait Time(4)
 Transfer Walk Time(5)+Access Walk Time(6)+Egress Walk Time(7)      Walk Time(5)
 Number of Transfers(9)                                             Number of Transfers(6)                        

 Author: Wu Sun
 wsu@sandag.org, SANDAG
 05/25/09 ver2,
 modified 4/16/2012 jef - for AB model
 

***********************************************************************************************************************************/
Macro "Process transit skims"
   shared path,inputDir, outputDir

   periods = {"_EA","_AM","_MD","_PM","_EV"}
   modes = {"prem","locl"}

   //output core names, by mode
   outMatrixCores={{"Fare","Initial Wait Time","Transfer Wait Time","Walk Time", "Number of Transfers", "Length:CR",
                    "Length:LR","Length:BRT","Length:EXP","Length:LB","IVT:CR","IVT:LR","IVT:BRT","IVT:EXP","IVT:LB","IVT:Sum","Main Mode"},
                    {"Fare","Total IV Time","Initial Wait Time","Transfer Wait Time","Walk Time", "Number of Transfers"}}

   //input output matrix core lookup table, by mode
   //Note:  if index is set to -1, it represents an aggregation
   inOutCoreLookUp={{1,2,3,-1,8,9,10,-1,-1,15,16,17,-1,-1,22},
                    {1,-1,3,4,-1,9}}

   //skim aggretation lookup table, by mode
   //Note:  items match up with those in inOutCoreLookUp array where index is set to -1
   aggLookUp={ {{4,5,6},{11,12},{13,14},{18,19},{20,21}},
               {{2,8},{5,6,7}}}

   //dwelling time core index, by mode
   // Note:  set to -1 if no dwelling time allocation
   dwlTimeIndex={7, -1}

   //dwelling time allocation line-haul modes (line-haul modes that ivt times need to be adjusted using dwelling time), by mode
   // Note 1: rail modes are not included because they already include station dwell time.  
   // Note 2: Set to -1 if no dwelling time allocation
   dwlAlloModes={{13,14,15}, -1}

   //ivt core start index, by mode
   ivtStartIndex={11,2}

   //number of line-haul modes in output matrices, by mode
   numOutModes={5,1}

   //calculate indices, including ivt sum index, main mode index, and ivt end idnex
   dim ivtSumIndex[modes.length]
   dim ivtEndIndex[modes.length]
   for i=1 to modes.length do
      ivtSumIndex[i]=outMatrixCores[i].length-1
      ivtEndIndex[i]=ivtStartIndex[i]+numOutModes[i]-1 
   end

   //evaluation expression for coding main mode, by mode
   //Note: if no main mode coding is necessary, leave null
   expr={{"if(([IVT:LB]/ [IVT:Sum]) >0.5) then 8 else if [IVT:Sum]=null then 0 else if ([IVT:EXP]> [IVT:CR] & [IVT:EXP]> [IVT:LR] & [IVT:EXP]> [IVT:BRT] & [IVT:EXP]> [IVT:LB]) then 7 else if ([IVT:BRT]> [IVT:CR] & [IVT:BRT]> [IVT:LR] ) then 6",
          "if ([Main Mode]=null & [IVT:LR]> [IVT:CR]) then 5 else if([Main Mode]=null) then 4 else [Main Mode]"},}


   //--------------------------------------------------
   //This section aggregates matrices, allocates dwell time, and extracts main mode
   //--------------------------------------------------

   for i=1 to modes.length do
      for j=1 to periods.length do
                        
         //set input matrix currencies
         inputFile = "imp"+modes[i]+periods[j]+".mtx"
         inMatrixCurrency=RunMacro("set input matrix currencies",outputDir,inputFile)
               
         //set up output matrix currencies, empty at this point
         outputFile = "imp"+modes[i]+periods[j]+"o.mtx" 
         matrixLabel = modes[i]+" "+periods[j]
         outMatrixCurrency=RunMacro("set output matrix currencies",outputDir,inMatrixCurrency[1],outputFile,outMatrixCores[i],matrixLabel)

         //populate output matrix currencies except 'ivt sum' and 'main mode' cores
         outMatrixCurrency=RunMacro("transit aggregate skims",inMatrixCurrency,outMatrixCurrency,inOutCoreLookUp[i],aggLookUp[i])

         //populate 'main mode' core in output matrix
         if expr[i]<>null then do
            outMatrixCurrency=RunMacro("set main mode",inMatrixCurrency,outMatrixCurrency,dwlTimeIndex[i],ivtStartIndex[i],ivtEndIndex[i],ivtSumIndex[i],dwlAlloModes[i],expr[i])
         end
      
      end
   end
        
   Return(1)  
EndMacro

/********************************************************************************************************************************
set input matrix currencies

Create and return an array of matrix currencies for the specified file in  the specified directory

*********************************************************************************************************************************/
Macro "set input matrix currencies" (dir, trnInSkim)
    //inputs, keyed to scenarioDirectory
    inskim=dir+"\\"+trnInSkim

    //open input transit matrices
    inMatrix = OpenMatrix(inskim, "True")
    inMatrixCores = GetMatrixCoreNames(inMatrix)
    numCoresIn=inMatrixCores.length 
    matrixInfo=GetMatrixInfo(inMatrix)

    //create inMatrix currencies
    dim inMatrixCurrency[numCoresIn]
    for i = 1 to numCoresIn do
       inMatrixCurrency[i] = CreateMatrixCurrency(inMatrix, inMatrixCores[i], null, null, )
    end

    Return(inMatrixCurrency)
EndMacro
                                                                                                                                  
/******************************************************************************************************************************** 
set output matrix currencies                                                                                                       
                                                                                                                                  
Create a matrix file and return an array of matrix currencies for the file                                
                                                                                                                                  
*********************************************************************************************************************************/

Macro "set output matrix currencies" (dir, inMatrixCurrency, trnOutSkim, outMatrixCores, label)
    //outputs, keyed to scenarioDirectory
    outskim=dir+"\\"+trnOutSkim 

    //outMatrix core length 
    numCoresOut=outMatrixCores.length

    //outMatirx structure
    dim outMatrixStructure[numCoresOut]
    for i=1 to numCoresOut do
   outMatrixStructure[i]=inMatrixCurrency
   end

    //Create the output transit matrix (with main mode core)
    Opts = null
    Opts.[Compression] = 1
    Opts.[Tables] = outMatrixCores
    Opts.[Type] = "Float"
    Opts.[File Name] =outskim
    Opts.[Label] = label
    outMatrix = CopyMatrixStructure(outMatrixStructure,Opts)
 
    //create outMatrix currencies
    dim outMatrixCurrency[numCoresOut]
    for i=1 to numCoresOut do
   outMatrixCurrency[i] = CreateMatrixCurrency(outMatrix, outMatrixCores[i], null, null, )
   outMatrixCurrency[i]:=0.0
      end

    //return outMatrixCurrency
    return(outMatrixCurrency)
EndMacro
                                                        
/******************************************************************************************************************************** 
transit aggregate skims

Aggregate matrix currrencies in the input file and store the results in the output file
                                                                                                                                  
*********************************************************************************************************************************/

Macro "transit aggregate skims"(inMatrixCurrency,outMatrixCurrency,inOutCoreLookUp,aggLookUp)
    //populate all outMatrix cores except the last 2, using input-output core lookup table and aggregation lookup table
    aggCounter=0
    for i = 1 to inOutCoreLookUp.length do
      outMatrixCurrency[i]:=0.0
      lookupIndex=inOutCoreLookUp[i]
      if lookupIndex=-1 then do
         aggCounter=aggCounter+1
         aggIndices=aggLookUp[aggCounter]
         for j=1 to aggIndices.length do
            //name = inMatrixCurrency[aggIndices[j]].Core  
            //expr = "if ["+ name + "] = null then 0.0 else [" + name +"]"
            //EvaluateMatrixExpression(inMatrixCurrency[aggIndices[j]], expr, , , )
            outMatrixCurrency[i]:=outMatrixCurrency[i]+Nz(inMatrixCurrency[aggIndices[j]])
            end
         end
      else do
         outMatrixCurrency[i]:=Nz(inMatrixCurrency[lookupIndex])      
         end
     end       

    return(outMatrixCurrency)
EndMacro
                                                        
/******************************************************************************************************************************** 
set main mode                                                                                                       
                                                                                                                                  
Set the main mode in the file based upon the expression
                                                                                                                                  
*********************************************************************************************************************************/

Macro "set main mode"(inMatrixCurrency,outMatrixCurrency,dwlTimeIndex,ivtStartIndex,ivtEndIndex,ivtSumIndex,dam,expr)

    //initialize outMatrixCurrency[ivtSumIndex], use as a temporary currency for storing sum values
    outMatrixCurrency[ivtSumIndex]:=0.0

    //sum ivt of dwelling allocation modes
    for i=1 to dam.length do
   outMatrixCurrency[ivtSumIndex]:=outMatrixCurrency[ivtSumIndex]+outMatrixCurrency[dam[i]]
   end

    //allocate dwelling time
    for i=1 to dam.length do
   outMatrixCurrency[dam[i]]:=outMatrixCurrency[dam[i]]*(1.0+inMatrixCurrency[dwlTimeIndex]/outMatrixCurrency[ivtSumIndex])
   end

    //zero out outMatrixCurrency[ivtSumIndex]
    outMatrixCurrency[ivtSumIndex]:=0.0

    //set total ivt time to outMatrix
    for i=ivtStartIndex to ivtEndIndex do
   //outMatrixCurrency[i]:=Nz(outMatrixCurrency[i])
   outMatrixCurrency[ivtSumIndex]:=outMatrixCurrency[ivtSumIndex]+outMatrixCurrency[i]
   end

    //set main mode to outMatrix using an expression
    for i=1 to expr.length do
       EvaluateMatrixExpression(outMatrixCurrency[outMatrixCurrency.length], expr[i],,, )
       end

    return(outMatrixCurrency)
EndMacro
/*********************************************************************************

Update transit time fields

This macro updates transit time fields with MSA times from flow tables

The following fields are updated:
   
xxField_yy where 

Field is TM

xx is AB or BA
yy is period 
   EA: Early AM
   AM: AM peak
   MD: Midday
   PM: PM peak
   EV: Evening
   
   TODO:  Update times for transit based on postload code pasted below

*********************************************************************************/
Macro "Update transit time fields"
  
   shared path, inputDir, outputDir

   periods = {"_EA","_AM","_MD","_PM","_EV"}
   
   // input files
   db_file = outputDir + "\\transit.dbd"
   
   {node_lyr, link_lyr} = RunMacro("TCB Add DB Layers", db_file)
   SetLayer(link_lyr)
   vw = GetView()
                                                                        
   /*
         if(shoulder) then !adjust times for freeway shoulder operation                  
            xxspd=xlen*60.0/xtime                                                          
            if(xxspd.lt.35.0) xtime=xlen*(60.0/35.0)                                       
         endif                                                                           
         tranlt(ipk,idir)=xtime                                                          
         brtlt(ipk,idir)=xtime                                                           
         
         adjust time for priority treatment
         if(aatfc(1).gt.1.and.aatfc(1).lt.8.and.artxlkid(aatid).and.                     
       *  myear.eq.2030) then !adjust times for priority treatment                       
          xtime=xtime*0.90             
          
         if(aatcnt(idir,1).eq.5) then
        hovlt(ipk,idir)=hovlt(ipk,idir)*hovfac(ipk) !hovfac = 0.33 for peak, 1 for off-peak
        tranlt(ipk,idir)=tranlt(ipk,idir)*hovfac(ipk)
        brtlt(ipk,idir)=brtlt(ipk,idir)*hovfac(ipk)
                                                  
   */
                                                                           
   //Recompute generalized cost using MSA cost in flow table, for links with MSA cost (so that transit only links aren't overwritten with null)
   for i = 1 to periods.length do
      flowTable = outputDir+"\\hwyload"+periods[i]+".bin"     
        
      // The Dataview Set is a joined view of the link layer and the flow table, based on link ID
      Opts.Input.[Dataview Set] = {{db_file+"|"+link_lyr, flowTable, {"ID"}, {"ID1"}},"AB time"+periods[i] }   
      Opts.Global.Fields = {"ABTM"+periods[i]}                                    // the field to fill
      Opts.Global.Method = "Formula"                                                                  // the fill method
      Opts.Global.Parameter = {"if AB_MSA_Cost<>null then AB_MSA_Cost else ABTM"+periods[i] }   
      ok = RunMacro("TCB Run Operation", "Fill Dataview", Opts, &Ret)
      if !ok then goto quit

     // The Dataview Set is a joined view of the link layer and the flow table, based on link ID
      Opts.Input.[Dataview Set] = {{db_file+"|"+link_lyr, flowTable, {"ID"}, {"ID1"}},"BA time"+periods[i]}   
      Opts.Global.Fields = {"BATM"+periods[i]}                                    // the field to fill
      Opts.Global.Method = "Formula"                                              // the fill method
      Opts.Global.Parameter = {"if BA_MSA_Cost<>null then BA_MSA_Cost else BATM"+periods[i] }   
      ok = RunMacro("TCB Run Operation", "Fill Dataview", Opts, &Ret)
      if !ok then goto quit

   end

   ok=1
   quit:    
      RunMacro("close all")
      return(ok)

EndMacro  

/***************************************************************************************
Create rail net

This script creates two rail networks; one for commuter rail and one for light-rail.  The
networks are used to create light rail zonal fare matrix by skimming light rail stops for LR network.
 
to create lrzone skim matrix replaced the value of lrzone by light rail fares

Inputs:
   input\modenu061.dbf        Mode table
   output\transit.dbd         Transit line layer
   output\transitrt.rts       Transit route system             

Outputs:
   output\cr.tnw              Commuter rail transit network
   output\lr.tnw              Light rail transit network
   
3/16/05 zou  c
***************************************************************************************/
Macro "Create rail net"   

   shared path,mxtap, inputDir, outputDir   

   db_file=outputDir+"\\transit.dbd"
   rte_file=outputDir+"\\transitrt.rts"

   {node_lyr, link_lyr} = RunMacro("TCB Add DB Layers", db_file,,)  
   ok = (node_lyr <> null && link_lyr <> null)
   if !ok then goto quit
  
   db_link_lyr=db_file+"|"+link_lyr
   db_node_lyr=db_file+"|"+node_lyr
   rte_lyr = RunMacro("TCB Add RS Layers", rte_file, , )           
   
   stp_lyr = GetStopsLayerFromRS(rte_lyr)
   db_rte_lyr = rte_file + "|" + rte_lyr
   db_stp_lyr = rte_file + "|" + stp_lyr   

   //route selection set names
   sets = {"crpk","lrpk"}
  
   //selection query strings
   //rcu - changed query to drop routes 399 and 599 12/27/06
   // todo: fix this
   query_strs={"select * where am_headway >0 and route_name <> '399103' and route_name <> '399203' and route_name <> '599101' and route_name <> '599201'",
               "select * where am_headway >0 and route_name <> '399103' and route_name <> '399203' and route_name <> '599101' and route_name <> '599201'"}
   trnets = {"cr.tnw","lr.tnw"}

   // Build 4(2?) Transit Networks
   for i = 1 to trnets.length do
      Opts = null
      Opts.Input.[Transit RS] = rte_file
      Opts.Input.[RS Set] = {db_rte_lyr, rte_lyr,sets[i],query_strs[i]}
      Opts.Input.[Walk Link Set] = {db_link_lyr, link_lyr, "walklink", "Select * where MINMODE<4"}
      Opts.Input.[Stop Set] = {db_stp_lyr, stp_lyr}
      Opts.Global.[Network Options].[Route Attributes].mode =   {rte_lyr+".mode"}    
      Opts.Global.[Network Options].[Route Attributes].AM_HEADWAY = {rte_lyr+".AM_HEADWAY"}
      Opts.Global.[Network Options].[Route Attributes].OP_HEADWAY = {rte_lyr+".OP_HEADWAY"} 
      Opts.Global.[Network Options].[Route Attributes].FARE = {rte_lyr+".FARE"}  
      Opts.Global.[Network Options].[Street Attributes].Length = {link_lyr+".Length",link_lyr+".Length"}
      Opts.Global.[Network Options].[Street Attributes].[*TM_MD] = {link_lyr+".ABTM_MD", link_lyr+".BATM_MD"}
      Opts.Global.[Network Options].[Street Attributes].[*TM_AM] = {link_lyr+".ABTM_AM", link_lyr+".BATM_AM"}
      Opts.Global.[Network Options].[Street Attributes].LRTZONE = {link_lyr+".LRTZONE", link_lyr+".LRTZONE"}
      Opts.Global.[Network Options].[Street Attributes].CRZONE = {link_lyr+".CRZONE", link_lyr+".CRZONE"}
      Opts.Global.[Network Options].Walk = "Yes"
      Opts.Global.[Network Options].Overide = {stp_lyr+".ID", stp_lyr+".nearnode"}  
      Opts.Global.[Network Options].[Link Attributes] = 
         {{"Length", {link_lyr+".Length",link_lyr+".Length"}, "SUMFRAC"}, 
         {"*TM_MD",   {link_lyr+".ABTM_MD", link_lyr+".BATM_MD"}, "SUMFRAC"}, 
         {"*TM_AM",   {link_lyr+".ABTM_AM", link_lyr+".BATM_AM"}, "SUMFRAC"},
         {"lrtzone",{link_lyr+".lrtzone", link_lyr+".lrtzone"}, "SUMFRAC"},
         {"crzone", {link_lyr+".crzone", link_lyr+".crzone"}, "SUMFRAC"}}
      Opts.Global.[Network Options].[Mode Field] = rte_lyr+".Mode"
      Opts.Global.[Network Options].[Walk Mode] = link_lyr+".minmode"
      Opts.Output.[Network File] = outputDir+"\\"+trnets[i]
      RunMacro("HwycadLog",{"createtrnnet.rsc: Create rail net","Build Transit Network: "+trnets[i]})
      ok = RunMacro("TCB Run Operation", i, "Build Transit Network", Opts)

     if !ok then goto quit
   end 

   mode_tb = inputDir+"\\modenu061.dbf"
   mode_vw = RunMacro("TCB OpenTable",,, {mode_tb})

   // xfer_tb = path+"\\xferfares.dbf" 
   // xferf_vw = RunMacro("TCB OpenTable",,, {xfer_tb})

  //transit network settings
   impds = {"*tma", "*tma"}
   impwts = {mode_vw+".wt_ivtpk", mode_vw+".wt_ivtpk"}    
   iwtwts = {mode_vw+".wt_fwtpk", mode_vw+".wt_fwtpk"}
   xwaitwts = {mode_vw+".wt_xwtpk", mode_vw+".wt_xwtpk"}
   modeused={mode_vw+".crmode",mode_vw+".lrmode"}

  for i = 1 to trnets.length do
     Opts = null
     Opts.Input.[Transit RS] = rte_file
     Opts.Input.[Transit Network] = outputDir+"\\"+trnets[i]
     Opts.Input.[Mode Table] = {mode_tb}
      //  Opts.Input.[Mode Cost Table] = {xfer_tb}
     //Opts.Input.[Fare Currency] = {inputDir+"\\fare.mtx", "coaster fare", , }    
     Opts.Input.[Centroid Set] = {db_node_lyr,node_lyr, "Selection", "Select * where ID<"+i2s(mxtap)}
     Opts.Field.[Link Impedance] = "*TM"
     Opts.Field.[Route Headway] = headways[i]
     Opts.Field.[Route Fare] = "Fare"
     Opts.Field.[Stop Zone ID] = "farezone"
     Opts.Field.[Mode Fare Type] = mode_vw+".faretype"
     Opts.Field.[Mode Fare Core] = mode_vw+".farefield"
     Opts.Field.[Mode Fare Weight] = farewts[i]
     Opts.Field.[Mode Xfer Time] = mode_vw+".xferpentm"
     Opts.Field.[Mode Xfer Weight] = mode_vw+".wtxfertm"
     Opts.Field.[Mode Impedance] = trntime[i]   //impedance by transit mode
     Opts.Field.[Mode Imp Weight] = impwts[i]
     Opts.Field.[Mode IWait Weight] = iwtwts[i]
     Opts.Field.[Mode XWait Weight] = xwaitwts[i]
     Opts.Field.[Mode Dwell Weight] = impwts[i]       
     Opts.Field.[Mode Dwell On Time] = mode_vw+".dwelltime"      
     Opts.Field.[Mode Used] = modeused[j]
     Opts.Field.[Mode Access] = mode_vw+".mode_acces"
     Opts.Field.[Mode Egress] = mode_vw+".mode_egres"
     Opts.Field.[Inter-Mode Xfer From] =xferf_vw+".from"
     Opts.Field.[Inter-Mode Xfer To] = xferf_vw+".to"
     Opts.Field.[Inter-Mode Xfer Stop] = xferf_vw+".stop"
     Opts.Field.[Inter-Mode Xfer Proh] = xferf_vw+".prohibitio"
     Opts.Field.[Inter-Mode Xfer Time] =  xferf_vw+".xfer_penal"
     Opts.Field.[Inter-Mode Xfer Fare] =  xferf_vw+".fare"
     Opts.Field.[Inter-Mode Xfer Wait] =  xferf_vw+".wait_time"   
     Opts.Global.[Class Names] = {"Class 1"}
     Opts.Global.[Class Description] = {"Class 1"}
     Opts.Global.[current class] = "Class 1"
     Opts.Global.[Global Fare Type] = "Flat"
     Opts.Global.[Global Fare Value] = 2.25
     Opts.Global.[Global Xfer Fare] = 0
     Opts.Global.[Global Fare Core] = "coaster fare"
     Opts.Global.[Global Fare Weight] = 1
     Opts.Global.[Global Imp Weight] = 1
     Opts.Global.[Global Init Weight] = 1
     Opts.Global.[Global Xfer Weight] = 1
     Opts.Global.[Global IWait Weight] = 2
     Opts.Global.[Global XWait Weight] = 2
     Opts.Global.[Global Dwell Weight] = 1
     Opts.Global.[Global Dwell On Time] = 0
     Opts.Global.[Global Dwell Off Time] = 0
     Opts.Global.[Global Headway] = 30
     Opts.Global.[Global Init Time] = 0
     Opts.Global.[Global Xfer Time] = 10
     Opts.Global.[Global Max IWait] = 60
     Opts.Global.[Global Min IWait] = 2
     Opts.Global.[Global Max XWait] = 60
     Opts.Global.[Global Min XWait] = 2
     Opts.Global.[Global Layover Time] = 5
     Opts.Global.[Global Max WACC Path] = 20
     Opts.Global.[Global Max Access] = 30
     Opts.Global.[Global Max Egress] = 30
     Opts.Global.[Global Max Transfer] = 20
     Opts.Global.[Global Max Imp] = 180
     Opts.Global.[Value of Time] = vot[i]
     Opts.Global.[Max Xfer Number] = 3
     Opts.Global.[Max Trip Time] = 999
     Opts.Global.[Walk Weight] = 1.8
     Opts.Global.[Zonal Fare Method] = 1
     Opts.Global.[Interarrival Para] = 0.5
     Opts.Global.[Path Threshold] = 0
     Opts.Flag.[Use All Walk Path] = "No"
     Opts.Flag.[Use Mode] = "Yes"
     Opts.Flag.[Use Mode Cost] = "Yes"
     Opts.Flag.[Combine By Mode] = "Yes"
     Opts.Flag.[Fare By Mode] = "No"
     Opts.Flag.[M2M Fare Method] = 2
     Opts.Flag.[Fare System] = 3
     Opts.Flag.[Use Park and Ride] = "No"
     Opts.Flag.[Use Egress Park and Ride] = "No"
     Opts.Flag.[Use P&R Walk Access] = "No"
     Opts.Flag.[Use P&R Walk Egress] = "No"
     Opts.Flag.[Use Parking Capacity] = "No"
     RunMacro("HwycadLog",{"createtrnnet.rsc: create rail net","Transit Network Setting PF: "+trnets[i]})    
     ok = RunMacro("TCB Run Operation", i, "Transit Network Setting PF", Opts)
     if !ok then goto quit
  end
  
  ok=1
  quit: 
//  if fpr<> null then  closefile(fpr)
  Return(ok)
EndMacro


/*******************************************************************************
Create transit networks

create 6 transit network from the route system: 
local bus op, local bus pk, premium op, premium pk

Input files: 
      output\transit.dbd
      output\transitrt.rts
      input\timexfer.dbf
      input\mode3tod.dbf - dbf file for mode table
      input\modexfer.dbf
      output\fare.mtx 
      
              
output files:
     localop.tnw - off peak local bus transit network
     localpk.tnw - peak local bus transit network
     prepop.tnw - off peak premium service transit network
     preppk.tnw - peak premium service transit network

*****************************************************************************/
Macro "Create transit networks"
   shared path, inputDir, outputDir, mxtap
   
   db_file=outputDir+"\\transit.dbd"
   rte_file=outputDir+"\\transitrt.rts"
   timexfer_tb = inputDir+"\\timexfer.bin"
   mode_tb = inputDir+"\\mode5tod.dbf"
   modexfer_tb = inputDir+"\\modexfer.dbf" 

   periods = {"_EA","_AM","_MD","_PM","_EV"}
   modes = {"prem","locl"}
   
  // timexfer_per = {"NO", "YES", "NO", "YES", "NO"}
   timexfer_per = {"NO", "YES", "NO", "NO", "NO"}
   timexfer_mod = {"YES", "NO"}
   
   
   
   ftype = RunMacro("G30 table type", timexfer_tb)
   view = OpenTable("test", ftype, {timexfer_tb})
   if view = null then do
      RunMacro("TCB Error", "Can't open table " + timexfer_tb)
      Return(0)
   end

   {node_lyr, link_lyr} = RunMacro("TCB Add DB Layers", db_file,,)  
   ok = (node_lyr <> null && link_lyr <> null)
   if !ok then goto quit
  
   db_link_lyr=db_file+"|"+link_lyr
   db_node_lyr=db_file+"|"+node_lyr
   rte_lyr = RunMacro("TCB Add RS Layers", rte_file, , )           
   stp_lyr = GetStopsLayerFromRS(rte_lyr)
   db_rte_lyr = rte_file + "|" + rte_lyr
   db_stp_lyr = rte_file + "|" + stp_lyr     

   /*
   fpr=OpenFile(path+"\\hwycad.log","a")
   mytime=GetDateAndTime() 
   writeline(fpr,mytime+", create trnets")
   */
    
   //selection query strings: vary by period
   query_strs={"select * where op_headway >0",
               "select * where am_headway >0",
               "select * where op_headway >0",
               "select * where pm_headway >0",
               "select * where op_headway >0"}
   
   // Build Transit Networks
   for i = 1 to periods.length do
      for j = 1 to modes.length do   
         Opts = null
         Opts.Input.[Transit RS] = rte_file
         Opts.Input.[RS Set] = {db_rte_lyr, rte_lyr,modes[j]+periods[i],query_strs[i]}
         Opts.Input.[Walk Link Set] = {db_link_lyr, link_lyr, "walklink", "Select * where MINMODE<4"}
         Opts.Input.[Stop Set] = {db_stp_lyr, stp_lyr}
         Opts.Global.[Network Options].[Route Attributes].mode =   {rte_lyr+".mode"}        
         Opts.Global.[Network Options].[Route Attributes].OP_HEADWAY = {rte_lyr+".OP_HEADWAY"} 
         Opts.Global.[Network Options].[Route Attributes].AM_HEADWAY = {rte_lyr+".AM_HEADWAY"}
         Opts.Global.[Network Options].[Route Attributes].PM_HEADWAY = {rte_lyr+".PM_HEADWAY"}
         Opts.Global.[Network Options].[Route Attributes].FARE = {rte_lyr+".FARE"}  
         Opts.Global.[Network Options].[Stop Attributes].farezone = {stp_lyr+".farezone"}   
         Opts.Global.[Network Options].[Street Attributes].Length = {link_lyr+".Length",link_lyr+".Length"}
         Opts.Global.[Network Options].[Street Attributes].FWYLEN = {link_lyr+".FWYLEN", link_lyr+".FWYLEN"}
         Opts.Global.[Network Options].[Street Attributes].[*TM] = {link_lyr+".ABTM"+periods[i], link_lyr+".BATM"+periods[i]}
     //    Opts.Global.[Network Options].[Street Attributes].[*PRETIME] = {link_lyr+".ABPRETIME"+periods[i], link_lyr+".BAPRETIME"+periods[i]}
     //    Opts.Global.[Network Options].[Street Attributes].[*LOCTIME] = {link_lyr+".ABLOCTIME"+periods[i], link_lyr+".BALOCTIME"+periods[i]}
         Opts.Global.[Network Options].[Street Attributes].LRTZONE = {link_lyr+".LRTZONE", link_lyr+".LRTZONE"}
         Opts.Global.[Network Options].[Street Attributes].CRZONE = {link_lyr+".CRZONE", link_lyr+".CRZONE"}
         Opts.Global.[Network Options].Walk = "Yes"
         Opts.Global.[Network Options].Overide = {stp_lyr+".ID", stp_lyr+".nearnode"}  
         Opts.Global.[Network Options].[Link Attributes] = 
          {{"Length", {link_lyr+".Length",link_lyr+".Length"}, "SUMFRAC"},
           {"fwylen",{link_lyr+".fwylen", link_lyr+".fwylen"}, "SUMFRAC"}, 
           {"*TM",   {link_lyr+".ABTM"+periods[i], link_lyr+".BATM"+periods[i]}, "SUMFRAC"}, 
      //     {"*PRETIME",   {link_lyr+".ABPRETIME"+periods[i], link_lyr+".BAPRETIME"+periods[i]}, "SUMFRAC"}, 
      //     {"*LOCTIME",   {link_lyr+".ABLOCTIME"+periods[i], link_lyr+".BALOCTIME"+periods[i]}, "SUMFRAC"}, 
           {"lrtzone",{link_lyr+".lrtzone", link_lyr+".lrtzone"}, "SUMFRAC"},
           {"crzone", {link_lyr+".crzone", link_lyr+".crzone"}, "SUMFRAC"}}
         Opts.Global.[Network Options].[Mode Field] = rte_lyr+".Mode"
         Opts.Global.[Network Options].[Walk Mode] = link_lyr+".minmode"
         Opts.Output.[Network File] = outputDir+"\\"+modes[j]+periods[i]+".tnw"

         ok = RunMacro("TCB Run Operation", i, "Build Transit Network", Opts)
         if !ok then goto quit
      end
   end 

   dif2=GetDirectoryInfo(timexfer_tb,"file")
   if dif2.length>0 then blnxfer=1 else blnxfer=0

   mode_vw = RunMacro("TCB OpenTable",,, {mode_tb})
   xferf_vw = RunMacro("TCB OpenTable",,, {modexfer_tb})

   // following vary by period
   headways = {"op_headway", "am_headway", "op_headway", "pm_headway", "op_headway"}
   trntime=   {mode_vw+".TRNTIME_EA",mode_vw+".TRNTIME_AM",mode_vw+".TRNTIME_MD",mode_vw+".TRNTIME_PM",mode_vw+".TRNTIME_EV"}//transit travel time by mode
   impwts =   {mode_vw+".wt_ivtop", mode_vw+".wt_ivtpk", mode_vw+".wt_ivtop", mode_vw+".wt_ivtpk", mode_vw+".wt_ivtop"}    
   iwtwts =   {mode_vw+".wt_fwtop", mode_vw+".wt_fwtpk", mode_vw+".wt_fwtop", mode_vw+".wt_fwtpk", mode_vw+".wt_fwtop"}
   xwaitwts = {mode_vw+".wt_xwtop", mode_vw+".wt_xwtpk", mode_vw+".wt_xwtop", mode_vw+".wt_xwtpk", mode_vw+".wt_xwtop"}
   farewts=   {mode_vw+".wt_fareop", mode_vw+".wt_farepk", mode_vw+".wt_fareop", mode_vw+".wt_farepk", mode_vw+".wt_fareop"}  
   vot =      {0.05, 0.1, 0.05, 0.1, 0.05} //PB recommended 0.1
   wt_walk =  { 1.6, 1.8,  1.6, 1.8,  1.6}
   
   // following varies by mode
   modeused={mode_vw+".premode",mode_vw+".locmode"}
   faresys={3, 1} //3, mixed fare, 1, flat fare
   
   
   //transit network settings in TransCAD 6.0 R2
   for i = 1 to periods.length do
      for j = 1 to modes.length do   
            Opts = null
            Opts.Input.[Transit RS] = rte_file
            Opts.Input.[Transit Network] = outputDir+"\\"+modes[j]+periods[i]+".tnw"
            Opts.Input.[Mode Table] = {mode_tb}
            Opts.Input.[Mode Cost Table] = {modexfer_tb}
            Opts.Input.[Fare Currency] = {inputDir+"\\fare.mtx", "coaster fare", , }    
            // add timed transfers to premium peak networks (?)
            if blnxfer=1 and timexfer_per[i] ="YES" and timexfer_mod[j]="YES" then Opts.Input.[Xfer Wait Table] = {timexfer_tb}
            Opts.Input.[Centroid Set] = {db_node_lyr,node_lyr, "Selection", "Select * where ID<"+i2s(mxtap)}
            Opts.Field.[Link Impedance] = "*TM"
	    Opts.Field.[Route Headway] = headways[i]
	    Opts.Field.[Route Fare] = "Fare"
	    Opts.Field.[Stop Zone ID] = "farezone"
	    Opts.Field.[Mode Fare Type] = mode_vw+".faretype"
	    Opts.Field.[Mode Fare Core] = mode_vw+".farefield"
	    Opts.Field.[Mode Fare Weight] = farewts[i]
	    Opts.Field.[Mode Xfer Time] = mode_vw+".xferpentm"
	    Opts.Field.[Mode Xfer Weight] = mode_vw+".wtxfertm"
	    Opts.Field.[Mode Impedance] = trntime[i]   //impedance by transit mode
	    Opts.Field.[Mode Imp Weight] = impwts[i]
	    Opts.Field.[Mode IWait Weight] = iwtwts[i]
	    Opts.Field.[Mode XWait Weight] = xwaitwts[i]
	    Opts.Field.[Mode Dwell Weight] = impwts[i]       
	    Opts.Field.[Mode Dwell On Time] = mode_vw+".dwelltime"      
	    Opts.Field.[Mode Used] = modeused[j]
	    Opts.Field.[Mode Access] = mode_vw+".mode_acces"
	    Opts.Field.[Mode Egress] = mode_vw+".mode_egres"
	    Opts.Field.[Inter-Mode Xfer From] =xferf_vw+".from"
	    Opts.Field.[Inter-Mode Xfer To] = xferf_vw+".to"
	    Opts.Field.[Inter-Mode Xfer Stop] = xferf_vw+".stop"
	    Opts.Field.[Inter-Mode Xfer Proh] = xferf_vw+".prohibitio"
	    Opts.Field.[Inter-Mode Xfer Time] =  xferf_vw+".xfer_penal"
	    Opts.Field.[Inter-Mode Xfer Fare] =  xferf_vw+".fare"
	    Opts.Field.[Inter-Mode Xfer Wait] =  xferf_vw+".wait_time"   
	    Opts.Global.[Class Names] = {"Class 1"}
	    Opts.Global.[Class Description] = {"Class 1"}
	    Opts.Global.[current class] = "Class 1"
	    Opts.Global.[Global Fare Type] = "Flat"
	    Opts.Global.[Global Fare Value] = 2.25
	    Opts.Global.[Global Xfer Fare] = 0
	    Opts.Global.[Global Fare Core] = "coaster fare"
	    Opts.Global.[Global Fare Weight] = 1
	    Opts.Global.[Global Imp Weight] = 1
	    Opts.Global.[Global Init Weight] = 1
	    Opts.Global.[Global Xfer Weight] = 1
	    Opts.Global.[Global IWait Weight] = 2
	    Opts.Global.[Global XWait Weight] = 2
	    Opts.Global.[Global Dwell Weight] = 1
	    Opts.Global.[Global Dwell On Time] = 0
	    Opts.Global.[Global Dwell Off Time] = 0
	    Opts.Global.[Global Headway] = 30
	    Opts.Global.[Global Init Time] = 0
	    Opts.Global.[Global Xfer Time] = 10
	    Opts.Global.[Global Max IWait] = 60
	    Opts.Global.[Global Min IWait] = 2
	    Opts.Global.[Global Max XWait] = 60
	    Opts.Global.[Global Min XWait] = 2
	    Opts.Global.[Global Layover Time] = 5
	    Opts.Global.[Global Max WACC Path] = 20
	    Opts.Global.[Global Max Access] = 30
	    Opts.Global.[Global Max Egress] = 30
	    Opts.Global.[Global Max Transfer] = 20
	    Opts.Global.[Global Max Imp] = 180
	    Opts.Global.[Value of Time] = vot[i]
	    Opts.Global.[Max Xfer Number] = 3
	    Opts.Global.[Max Trip Time] = 999
	    Opts.Global.[Walk Weight] = 1.8
	    Opts.Global.[Zonal Fare Method] = 1
	    Opts.Global.[Interarrival Para] = 0.5
	    Opts.Global.[Path Threshold] = 0
	    Opts.Flag.[Use All Walk Path] = "No"
	    Opts.Flag.[Use Mode] = "Yes"
	    Opts.Flag.[Use Mode Cost] = "Yes"
	    Opts.Flag.[Combine By Mode] = "Yes"
	    Opts.Flag.[Fare By Mode] = "No"
	    Opts.Flag.[M2M Fare Method] = 2
	    Opts.Flag.[Fare System] = 3
	    Opts.Flag.[Use Park and Ride] = "No"
	    Opts.Flag.[Use Egress Park and Ride] = "No"
	    Opts.Flag.[Use P&R Walk Access] = "No"
	    Opts.Flag.[Use P&R Walk Egress] = "No"
	    Opts.Flag.[Use Parking Capacity] = "No" 
         ok = RunMacro("TCB Run Operation", i, "Transit Network Setting PF", Opts)
         if !ok then goto quit
      end
   end
  
   ok=1
   quit:    
      if fpr <> null then closefile(fpr)
      RunMacro("close all")
      Return(ok)
EndMacro
