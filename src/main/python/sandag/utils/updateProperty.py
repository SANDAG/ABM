# Wu.Sun@sandag.org 12/19/2017
from sandag.utils import stringFinder as sf
from optparse import OptionParser

def update_property(path,fName,pName,pNewValue):
    """
    Update properties

    :type path:str
    :param path: property file path

    :type fName:str
    :param fName: property file name

    :type pName:str
    :param pName:property name

    :type pNewValue:str
    :param pNewValue:new property value
    """
    logFile = open(path + "/logFiles/updateProperties.log", "w")
    if sf.find(path+'/'+fName,pName)==False:
        logFile.write(pName+ " doesn't exist in "+path+'\\'+fName+"!\n")
        return -1
    else:
        dic=[]
        oldValue = sf.findPropertySetting(path+"/"+fName, pName)
        pair = (oldValue, pName + "=" + pNewValue+"\n")
        dic.append(pair)
        sf.replace(path+'/'+fName,dic)
        return 0

 # Set Parser Options
parser = OptionParser()
parser.add_option("--path",action="store", dest="path",help="project scenario path")
parser.add_option("--pfile", action="store", dest="pfile",help="property file")
parser.add_option("--pname",action="store", dest="pname",help="property name")
parser.add_option("--newpvalue",action="store", dest="newpvalue",help="new property value")
(options, args) = parser.parse_args()

#properties can be updated while file is open
update_property(options.path,options.pfile,options.pname,options.newpvalue)