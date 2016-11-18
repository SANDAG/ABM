__author__ = 'wsu'
#Wu.Sun@sandag.org 11-18-2016
import Tkinter
import Tkconstants
import tkFileDialog
import os
from Tkinter import *
from PIL import Image,ImageTk
import popupMsg

class CreateScenarioGUI(Tkinter.Frame):
        def __init__(self, root):
            Tkinter.Frame.__init__(self, root, border=5)
            body = Tkinter.Frame(self)
            body.pack(fill=Tkconstants.X, expand=1)
            sticky = Tkconstants.E + Tkconstants.W
            body.grid_columnconfigure(1, weight=2)

            #divider line
            divider=u"_"*120
            self.releaseDir='T:\\ABM\\release\\ABM'
            self.defaultScenarioDir="T:\\projects\\sr13"
            self.defaultNetworkDir="T:\\projects\\sr13\\version13_3_0\\network_build"

            #study space section
            self.buttonVar= IntVar(root)
            self.yButton=Radiobutton(body, text="Yes", variable=self.buttonVar, value=1, command=self.initStudy)
            self.nButton=Radiobutton(body, text="No", variable=self.buttonVar, value=0,command=self.initStudy)
            Tkinter.Label(body, text=divider, font=("Helvetica", 11, 'bold'), width=50, fg='royal blue').grid(row=0,columnspan=5)
            Tkinter.Label(body, text=u"Create an ABM Space", font=("Helvetica", 10, 'bold')).grid(row=1,columnspan=3)
            self.yButton.grid(row=2,column=0, columnspan=2)
            self.nButton.grid(row=2,column=1, columnspan=2)

            Tkinter.Label(body, text=u"Study Folder", font=("Helvetica", 8, 'bold')).grid(row=3)
            self.studypath = Tkinter.Entry(body, width=40)
            self.studypath.grid(row=3, column=1, sticky=sticky)
            self.studypath.delete(0, Tkconstants.END)
            self.studypath.insert(0, self.defaultScenarioDir)
            self.studybutton = Tkinter.Button(body, text=u"...",width=4,command=lambda:self.get_path("study"))
            self.studybutton.grid(row=3, column=2)

            Tkinter.Label(body, text=u"Network Folder",font=("Helvetica", 8, 'bold')).grid(row=4)
            self.studynetworkpath = Tkinter.Entry(body, width=40)
            self.studynetworkpath.grid(row=4, column=1, sticky=sticky)
            self.studynetworkpath.delete(0, Tkconstants.END)
            self.studynetworkpath.insert(0, self.defaultNetworkDir)
            self.studynetworkbutton = Tkinter.Button(body, text=u"...",width=4,command=lambda:self.get_path("studynetwork"))
            self.studynetworkbutton.grid(row=4, column=2)

            self.copyButton = Tkinter.Button(body, text=u"Create", font=("Helvetica", 8, 'bold'),width=10, command=lambda:self.checkPath("study"))
            self.copyButton.grid(row=5,column=0,columnspan=4)

            #scenario section
            Tkinter.Label(body, text=divider, font=("Helvetica", 11, 'bold'), width=50, fg='royal blue').grid(row=6,columnspan=5)
            Tkinter.Label(body, text=u"Create an ABM scenario", font=("Helvetica", 10, 'bold')).grid(row=7,columnspan=3)

            Tkinter.Label(body, text=u"Version", font=("Helvetica", 8, 'bold')).grid(row=8)
            var = StringVar(root)
            self.version="version_13_3_1_SNAPSHOT_test"
            optionList=["version_13_3_0", "version_13_3_1_SNAPSHOT"]
            option=Tkinter.OptionMenu(body,var,*optionList,command=self.setversion)
            option.config(width=50)
            option.grid(row=8, column=1)

            Tkinter.Label(body, text=u"Year", font=("Helvetica", 8, 'bold')).grid(row=9)
            var = StringVar(root)
            self.year="2012"
            optionList=["2012", "2014", "2016", "2017", "2020", "2025", "2030", "2035", "2040", "2045", "2050"]
            option=Tkinter.OptionMenu(body,var,*optionList,command=self.setyear)
            option.config(width=50)
            option.grid(row=9, column=1)

            Tkinter.Label(body, text=u"Scenario Folder", font=("Helvetica", 8, 'bold')).grid(row=10)
            self.scenariopath = Tkinter.Entry(body, width=40)
            self.scenariopath.grid(row=10, column=1, sticky=sticky)
            button = Tkinter.Button(body, text=u"...",width=4,command=lambda:self.get_path("scenario"))
            button.grid(row=10, column=2)

            Tkinter.Label(body, text=u"Network Folder",font=("Helvetica", 8, 'bold')).grid(row=11)
            self.networkpath = Tkinter.Entry(body, width=40)
            self.networkpath.grid(row=11, column=1, sticky=sticky)
            button = Tkinter.Button(body, text=u"...",width=4,command=lambda:self.get_path("network"))
            button.grid(row=11, column=2)

            self.scenarioButton = Tkinter.Button(body, text=u"Create", font=("Helvetica", 8, 'bold'),width=10, command=lambda:self.checkPath("scenario"))
            self.scenarioButton.grid(row=12,column=0,columnspan=4)

            #validation section
            Tkinter.Label(body, text=divider, font=("Helvetica", 11, 'bold'), width=50, fg='royal blue').grid(row=13,columnspan=5)
            Tkinter.Label(body, text=u"Validate a Scenario", font=("Helvetica", 10, 'bold')).grid(row=14,columnspan=3)
            self.vbuttonVar= IntVar(root)
            self.vyButton=Radiobutton(body, text="Yes", variable=self.vbuttonVar, value=1, command=self.initValidation)
            self.vnButton=Radiobutton(body, text="No", variable=self.vbuttonVar, value=0,command=self.initValidation)
            self.vyButton.grid(row=15,column=0, columnspan=2)
            self.vnButton.grid(row=15,column=1, columnspan=2)

            Tkinter.Label(body, text=u"Scenario Number", font=("Helvetica", 8, 'bold')).grid(row=16)
            vv = StringVar(root)
            vv.set("540")
            self.validationScenario="540"
            vv.trace("w", lambda name, index, mode, vv=vv: self.setValidationScenario(vv))
            self.ve = Entry(body, textvariable=vv)
            self.ve.config(width=15)
            self.ve.grid(row=16,column=1,sticky=sticky)

            Tkinter.Label(body, text=u"Output Folder", font=("Helvetica", 8, 'bold')).grid(row=17)
            self.validationpath = Tkinter.Entry(body, width=40)
            self.validationpath.grid(row=17, column=1, sticky=sticky)
            self.voutbutton = Tkinter.Button(body, text=u"...",width=4,command=lambda:self.get_path("validate"))
            self.voutbutton.grid(row=17, column=2)

            self.validateButton = Tkinter.Button(body, text=u"Validate", font=("Helvetica", 8, 'bold'),width=10, command=lambda:self.checkPath("validate"))
            self.validateButton.grid(row=18,column=0,columnspan=4)

            self.initStudy()
            self.initValidation()

        def initStudy(self):
            #disable study setting buttons
            if self.buttonVar.get()==1:
                self.studypath.config(state=NORMAL)
                self.studybutton.config(state=NORMAL)
                self.studynetworkpath.config(state=NORMAL)
                self.studynetworkbutton.config(state=NORMAL)
                self.copyButton.configure(state=NORMAL)
            #enable study setting buttons
            elif self.buttonVar.get()==0:
                self.studypath.config(state=DISABLED)
                self.studybutton.config(state=DISABLED)
                self.studynetworkpath.config(state=DISABLED)
                self.studynetworkbutton.config(state=DISABLED)
                self.copyButton.configure(state=DISABLED)

        def initValidation(self):
            #disable study setting buttons
            if self.vbuttonVar.get()==1:
                self.validationpath.config(state=NORMAL)
                self.validateButton.configure(state=NORMAL)
                self.voutbutton.configure(state=NORMAL)
                self.ve.configure(state=NORMAL)
            #enable study setting buttons
            elif self.vbuttonVar.get()==0:
                self.validationpath.config(state=DISABLED)
                self.validateButton.configure(state=DISABLED)
                self.voutbutton.configure(state=DISABLED)
                self.ve.configure(state=DISABLED)

        #set default input and network paths based on selected year
        def setversion(self,value):
            self.version=value
            return

        #set default input and network paths based on selected year
        def setyear(self,value):
            self.defaultpath=self.releaseDir+"\\"+self.version+'\\input\\'+value
            self.scenariopath.delete(0, Tkconstants.END)
            self.scenariopath.insert(0, self.defaultScenarioDir)
            self.networkpath.delete(0, Tkconstants.END)
            self.networkpath.insert(0, self.defaultpath)
            self.year=value
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
            path = tkFileDialog.askdirectory(**self.dir_opt)
            if type=="scenario":
                if path:
                    spath = os.path.normpath(path)
                    self.scenariopath.delete(0, Tkconstants.END)
                    self.scenariopath.insert(0, spath)
            elif type=="validate":
                if path:
                    vpath = os.path.normpath(path)
                    self.validationpath.delete(0, Tkconstants.END)
                    self.validationpath.insert(0, vpath)
            elif type=="network":
                if path:
                    npath = os.path.normpath(path)
                    self.networkpath.delete(0, Tkconstants.END)
                    self.networkpath.insert(0, npath)
            elif type=="study":
                if path:
                    studypath = os.path.normpath(path)
                    self.studypath.delete(0, Tkconstants.END)
                    self.studypath.insert(0, studypath)
            elif type=="studynetwork":
                if path:
                    studynetworkpath = os.path.normpath(path)
                    self.studynetworkpath.delete(0, Tkconstants.END)
                    self.studynetworkpath.insert(0, studynetworkpath)
            return

        #check if a path already exisits or is empty
        def checkPath(self,type):
            self.popup=Tkinter.Tk()
            if type=="scenario":
                if os.path.exists(self.scenariopath.get()):
                    if not self.networkpath.get():
                        popupMsg.popupmsg(self,"Network folder is empty!",1,type)
                    else:
                        popupMsg.popupmsg(self,"Selected scenario folder already exists! Proceeding will overwrite existing files!",2,type)
                else:
                    if not self.scenariopath.get():
                        popupMsg.popupmsg(self,"Scenario folder is empty!",1,type)
                    elif not self.networkpath.get():
                        popupMsg.popupmsg(self,"Network folder is empty!",1,type)
                    else:
                        self.executeBatch(type)
            elif type=="study":
                if os.path.exists(self.studypath.get()):
                    if not self.studynetworkpath.get():
                        popupMsg.popupmsg(self,"Network folder is empty!",1,type)
                    else:
                        popupMsg.popupmsg(self,"Selected study folder already exists! Proceeding will overwrite existing files!",2,type)
                else:
                    if not self.studypath.get():
                        popupMsg.popupmsg(self,"Study folder is empty!",1,type)
                    elif not self.studynetworkpath.get():
                        popupMsg.popupmsg(self,"Network folder is empty!",1,type)
                    else:
                        self.executeBatch(type)
            elif type=="validate":
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
            dir=self.releaseDir+"\\"+self.version
            if type=="scenario":
                commandstr=u"create_scenario.cmd "+self.scenariopath.get()+" "+self.year+" "+self.networkpath.get()
            elif type=="study":
                commandstr=u"copy_networkfiles_to_study.cmd "+self.studypath.get()+" "+self.studynetworkpath.get()
            elif type=="validate":
                commandstr=u"validate.cmd "+self.validationScenario+" "+self.validationpath.get()+"\\"
                msg="Validation resulta are in "+self.validationpath.get()
                dir=self.releaseDir+"\\"+self.version+'\\validation\\'
            print commandstr
            os.chdir(dir)
            os.system(commandstr)
            self.popup=Tkinter.Tk()
            popupMsg.popupmsg(self,msg,1,type)
            return

        def setValidationScenario(self,value):
            self.validationScenario=value.get()

root = Tkinter.Tk()
root.resizable(True, False)
root.minsize(370, 0)
logo = Tkinter.PhotoImage(file=r"T:\ABM\release\ABM\SANDAG_logo.gif")
w=Label(root, image=logo, width=200)
w.pack(side='top', fill='both', expand='yes')
CreateScenarioGUI(root).pack(fill=Tkconstants.X, expand=1)
root.mainloop()


