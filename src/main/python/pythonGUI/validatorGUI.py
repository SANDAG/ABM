__author__ = 'wsu'
#Wu.Sun@sandag.org 2-10-2017
#wsu updated 5/2/2017 for release 13.3.2
import tkinter
import tkinter.constants
import tkinter.filedialog
import os
from tkinter import *
from PIL import Image,ImageTk
import popupMsg
import stringFinder

class CreateScenarioGUI(tkinter.Frame):
        def __init__(self, root):
            tkinter.Frame.__init__(self, root, border=5)
            body = tkinter.Frame(self)
            body.pack(fill=tkinter.constants.X, expand=1)
            sticky = tkinter.constants.E + tkinter.constants.W
            body.grid_columnconfigure(1, weight=2)

            self.version="version_13_3_2"

            #divider line
            divider="_"*120
            self.releaseDir='T:\\ABM\\release\\ABM'
            self.defaultScenarioDir="T:\\projects\\sr13"

            #validation section
            tkinter.Label(body, text=divider, font=("Helvetica", 11, 'bold'), width=50, fg='royal blue').grid(row=13,columnspan=5)
            tkinter.Label(body, text="Validate a Base Year Scenario", font=("Helvetica", 10, 'bold')).grid(row=14,columnspan=3)
            tkinter.Label(body, text="Base Year", font=("Helvetica", 8, 'bold')).grid(row=15)
            var = StringVar(root)
            self.year="2012"
            optionList=["2012", "2014"]
            option=tkinter.OptionMenu(body,var,*optionList,command=self.setyear)
            option.config(width=50)
            option.grid(row=15, column=1)

            tkinter.Label(body, text="Scenario Number", font=("Helvetica", 8, 'bold')).grid(row=16)
            vv = StringVar(root)
            vv.set("540")
            self.validationScenario="540"
            vv.trace("w", lambda name, index, mode, vv=vv: self.setValidationScenario(vv))
            self.ve = Entry(body, textvariable=vv)
            self.ve.config(width=15)
            self.ve.grid(row=16,column=1,sticky=sticky)

            tkinter.Label(body, text="Output Folder", font=("Helvetica", 8, 'bold')).grid(row=17)
            self.validationpath = tkinter.Entry(body, width=40)
            self.validationpath.grid(row=17, column=1, sticky=sticky)
            self.voutbutton = tkinter.Button(body, text="...",width=4,command=lambda:self.get_path("validate"))
            self.voutbutton.grid(row=17, column=2)

            self.validateButton = tkinter.Button(body, text="Validate", font=("Helvetica", 8, 'bold'),width=10, command=lambda:self.checkPath("validate"))
            self.validateButton.grid(row=18,column=0,columnspan=4)

        #set default input and network paths based on selected year
        def setyear(self,value):
            dic=[]
            pair=("xxxx",value)
            dic.append(pair)
            dir=self.releaseDir+"\\"+self.version+'\\validation\\'
            stringFinder.replace(dir+"sandag_validate_generic.properties",dir+"sandag_validate.properties",dic)
            return

        #set default options for folded browsers
        def setPathOptions(self):
            self.dir_opt = options = {}
            options['initialdir'] = self.defaultScenarioDir
            options['mustexist'] = False
            options['parent'] = root
            options['title'] = 'This is a title'

        #get a path after the browse button is clicked on
        def get_path(self,type):
            self.setPathOptions()
            path = tkinter.filedialog.askdirectory(**self.dir_opt)
            if path:
                vpath = os.path.normpath(path)
                self.validationpath.delete(0, tkinter.constants.END)
                self.validationpath.insert(0, vpath)
            return

        #check if a path already exisits or is empty
        def checkPath(self,type):
            self.popup=tkinter.Tk()
            if not self.validationScenario:
                popupMsg.popupmsg(self,"No validation scenario was selected!",1,type)
            else:
                if not self.validationpath.get():
                    popupMsg.popupmsg(self,"Validation output directory is empty!",1,type)
                else:
                    self.executeBatch(type)
            return

        #execute DOS commands
        def executeBatch(self, type):
            self.popup.destroy()
            msg="You have successfully created the "+ type+"!"
            commandstr="validate.cmd "+self.validationScenario+" "+self.validationpath.get()+"\\"
            msg="Validation results are in "+self.validationpath.get()
            dir=self.releaseDir+"\\"+self.version+'\\validation\\'
            print(commandstr)
            os.chdir(dir)
            os.system(commandstr)
            self.popup=tkinter.Tk()
            popupMsg.popupmsg(self,msg,1,type)
            return

        def setValidationScenario(self,value):
            self.validationScenario=value.get()

root = tkinter.Tk()
root.resizable(True, False)
root.minsize(370, 0)
logo = tkinter.PhotoImage(file=r"T:\ABM\release\ABM\SANDAG_logo.gif")
w=Label(root, image=logo, width=200)
w.pack(side='top', fill='both', expand='yes')
CreateScenarioGUI(root).pack(fill=tkinter.constants.X, expand=1)
root.mainloop()


