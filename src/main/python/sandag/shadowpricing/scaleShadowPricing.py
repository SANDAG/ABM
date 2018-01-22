# Wu.Sun@sandag.org 12/5/2017
import pandas as pd
def update_SP(path,iterations,type,sample):
    """
    Scale shadow pricing files

    :type path:str
    :param path: shadow price file path

    :type iterations:int
    :param iterations:number of iterations run to create shadow price files

    :type type:str
    :param type:shadow price file type: 'work' or 'school'

    :type sample:str
    :param sample:sample size used to create shadow price files
    """
    spFile=path+'/ShadowPricingOutput_'+type+'_'+str(iterations-1)+'.csv'
    df1=pd.read_csv(spFile)
    for c in df1.columns.values.tolist():
        """might be ok to scale other columns as well"""
        if c.find('_sizeScaled')>0 or c.find('_modeledDests')>0 or c.find('_sizeFinal')>0:
            df1[c]=df1[c]/sample
    df1.to_csv(spFile,index=False)

update_SP('T:/ABM/temp',10,'work',0.1)


