/**********************************************************************************************************
Reduce matrix precision
About: 
    Script to reduce matrix precision.  Precision defined in property file. 
    Author: Wu Sun, SANDAG
    Developed: May 2015
    
Note: 
Aggregate models such as truck, commercial vehicle, and EI models tend to have fake precisions.
The fake precisions make these matrices very large.
When matrixes are loaded into ABM database, it takes a large amount of DB space.
This scripts is to reduce fake precisions to make space managable.
**********************************************************************************************************/
Macro "reduce matrix precision"(dir, mat, precision)

    //get matrix info
    m = OpenMatrix(dir+"//"+mat, )
    coreNames = GetMatrixCoreNames(m)
    numCores=coreNames.length 

    //initialize arrays
    dim sum[numCores]
    dim rsum[numCores]

    //get matrix currency
    currency=RunMacro("set input matrix currencies",dir, mat)

    for i = 1 to numCores do
        //zero out null cells
	currency[i]:=Nz(currency[i])

        //sums of matrix by core
	marginal_sums = GetMatrixMarginals(currency[i], "Sum", "row" )
        sum[i]=Sum(marginal_sums)
	RunMacro("HwycadLog",{"matrixPrecisionReduction.rsc:","sum of "+coreNames[i]+" before reduction:"+r2s(sum[i])})

	//reduce matrix precision using matrix expression.
    	expr="if(["+coreNames[i]+"]<"+precision+") then 0.0 else ["+coreNames[i]+"]"
    	EvaluateMatrixExpression(currency[i], expr,,, )
	rmarginal_sums = GetMatrixMarginals(currency[i], "Sum", "row" )
        rsum[i]=Sum(rmarginal_sums)
	RunMacro("HwycadLog",{"matrixPrecisionReduction.rsc:","sum of "+coreNames[i]+" after reduction:"+r2s(rsum[i])})
    end

    //scale up reduced matrix to orirginal sum
    for i = 1 to numCores do
       currency[i]:=currency[i]*sum[i]/rsum[i]
    end

EndMacro

