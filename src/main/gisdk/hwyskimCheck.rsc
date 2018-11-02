/***********************************************
Hwy skim Check -- 5/29/2018, RCU & YMA

This rsc file includes 2 macros:

Macro 'DatskimCreate' is to create 'Dat' skim for highway network error checking purpose,
Macro 'DatskimCheck' is to check if 'Null' cell exist in 'Dat' skim.
They are called right after highway network building.

If 'Null' cell exist, message '"Suspicious Skim - Check for Nulls!" will be shown, 
and two csv files will be found in output folder for further checking:
    check_impdat_AM_count_by_origin.csv
    check_impdat_AM_count_by_destination.csv    
***********************************************/

Macro "DatskimCreate" 

   shared path, inputDir, outputDir, mxzone

   ok=RunMacro("hwy skim",{"dat"}) 
   if !ok then goto quit

   return(1)
   
   quit:
      return(0)
  
EndMacro


Macro "DatskimCheck"

    shared path, inputDir, outputDir, mxzone
    
    mat = OpenMatrix(outputDir + "\\impdat_AM.mtx", )
    stat_array = MatrixStatistics(mat, )
    
		
    if stat_array.[Length (Skim)].Count  = (mxzone*mxzone) then do
        RunMacro("HwycadLog",{"sandag_abm_master.rsc:","have checked testing skim of impdat_AM.mtx,and NO Null found!"})
        
        return(1)
   
    end

    else do

        strMxzone = I2S(mxzone)
		mc = CreateMatrixCurrency(mat, "Length (Skim)", , , )
        
		rows = {"1",strMxzone}
        ExportMatrix(mc, rows, "Rows", "CSV", outputDir + "\\check_impdat_AM_count_by_origin.csv", {{"Marginal", "Count"}})

		columns = {"1",strMxzone}
        ExportMatrix(mc, columns, "Columns", "CSV", outputDir + "\\check_impdat_AM_count_by_destination.csv", {{"Marginal", "Count"}})
        
        ShowMessage("Suspicious Skim - Check for Nulls!")
        return(0)
        quit: return(0)
    
    end
EndMacro




