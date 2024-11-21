__author__ = 'wsu'
#Wu.Sun@sandag.org 7-20-2016

import Tkinter
import Tkconstants
import tkFileDialog
import os
from Tkinter import *
from PIL import Image,ImageTk
import stringFinder
import popupMsg


class ParametersGUI(Tkinter.Frame):
        def __init__(self, root):
            Tkinter.Frame.__init__(self, root, border=5)
            self.status = Tkinter.Label(self, text=u"ABM Parameter Editor", font=("Helvetica", 12, 'bold'))
            self.status.pack(fill=Tkconstants.X, expand=1)
            body = Tkinter.Frame(self)
            body.pack(fill=Tkconstants.X, expand=1)
            sticky = Tkconstants.E + Tkconstants.W
            body.grid_columnconfigure(1, weight=2)

            #section labels
            sectionLabels=(u"Model Initial Start Options",u"Network Building Options",u"Final Assignment Options:",u"Data Loading Options:")
            #radio button lables
            rbLabels=(u"Copy warm start trip tables:",u"Copy bike AT access files:",u"Create bike AT access files:",u"Build highway network:",u"Build transit network:",u"Run highway assignment:",
                      u"Run highway skimming:",u"Run transit assignment:",u"Run transit skimming:",u"Export results to CSVs:",u"Load results to database:")
            #properties
            self.properties=("RunModel.skipCopyWarmupTripTables","RunModel.skipBikeLogsums","RunModel.skipBuildHwyNetwork","RunModel.skipBuildTransitNetwork","RunModel.skipFinalHighwayAssignment",
                          "RunModel.skipFinalHighwaySkimming","RunModel.skipFinalTransitAssignment","RunModel.skipFinalTransitSkimming",
                          "RunModel.skipDataExport","RunModel.skipDataLoadRequest")

            #divider line
            divider=u"_"*120

            #number of properties in GUI
            self.pNum=self.properties.__len__()

            #initialize yes and no buttons
            self.yButton = [0 for x in range(self.pNum)]
            self.nButton = [0 for x in range(self.pNum)]
            self.buttonVar= [0 for x in range(self.pNum)]
            for i in range(self.pNum):
                self.buttonVar[i] = IntVar(root)
                self.yButton[i]=Radiobutton(body, text="Yes", variable=self.buttonVar[i], value=1)
                self.nButton[i]=Radiobutton(body, text="No", variable=self.buttonVar[i], value=0)

            #set standard property values
            self.setDefaultProperties()

            #set AT states-activate and deactivate by selections
            #self.setATButtons()

            #scenario folder browser
            Tkinter.Label(body, text=u"Scenario Folder", font=("Helvetica", 8, 'bold'),width=15).grid(row=0)
            self.scenariopath = Tkinter.Entry(body, width=25)
            self.scenariopath.grid(row=0, column=1, sticky=sticky, columnspan=3)
            self.scenariopath.insert(0,sys.argv[1])
            button = Tkinter.Button(body, text=u"...",width=4,command=self.get_scenariopath)
            button.grid(row=0, column=4)

            #initial start section
            for i in range(1,25):
               if i==1: #intial start section header
                  Tkinter.Label(body, text=divider, font=("Helvetica", 10, 'bold'), width=40, fg='royal blue').grid(row=i,columnspan=5)
                  Tkinter.Label(body, text=sectionLabels[0], font=("Helvetica", 10, 'bold'), width=30, fg='royal blue').grid(row=i+1,columnspan=5)
               elif i>2 and i<6:
                   Tkinter.Label(body, text=rbLabels[i-3], font=("Helvetica", 8, 'bold')).grid(row=i)
                   self.yButton[i-3].grid(row=i,column=1)
                   self.nButton[i-3].grid(row=i,column=3)
               elif i==6: #network building section header
                  Tkinter.Label(body, text=divider, font=("Helvetica", 10, 'bold'), width=40, fg='royal blue').grid(row=i,columnspan=5)
                  Tkinter.Label(body, text=sectionLabels[1], font=("Helvetica", 10, 'bold'), width=30, fg='royal blue').grid(row=i+1,columnspan=5)
               elif i>7 and i<10:
                   Tkinter.Label(body, text=rbLabels[i-5], font=("Helvetica", 8, 'bold')).grid(row=i)
                   self.yButton[i-5].grid(row=i,column=1)
                   self.nButton[i-5].grid(row=i,column=3)
               elif i==10: #final assignment section header
                  Tkinter.Label(body, text=divider, font=("Helvetica", 10, 'bold'), width=40, fg='royal blue').grid(row=i,columnspan=5)
                  Tkinter.Label(body, text=sectionLabels[2], font=("Helvetica", 10, 'bold'), width=30, fg='royal blue').grid(row=i+1,columnspan=5)
               elif i>11 and i<16:
                   Tkinter.Label(body, text=rbLabels[i-7], font=("Helvetica", 8, 'bold')).grid(row=i)
                   self.yButton[i-7].grid(row=i,column=1)
                   self.nButton[i-7].grid(row=i,column=3)
               elif i==16: #data load section header
                  Tkinter.Label(body, text=divider, font=("Helvetica", 10, 'bold'), width=40, fg='royal blue').grid(row=i,columnspan=5)
                  Tkinter.Label(body, text=sectionLabels[3], font=("Helvetica", 10, 'bold'), width=30, fg='royal blue').grid(row=i+1,columnspan=5)
               elif i>17 and i<20:
                   Tkinter.Label(body, text=rbLabels[i-9], font=("Helvetica", 8, 'bold')).grid(row=i)
                   self.yButton[i-9].grid(row=i,column=1)
                   self.nButton[i-9].grid(row=i,column=3)
               elif i==20: #iteration section
                    Tkinter.Label(body, text=divider, font=("Helvetica", 10, 'bold'), width=40, fg='royal blue').grid(row=i,columnspan=5)
                    Tkinter.Label(body, text=u"Iteration Options", font=("Helvetica", 10, 'bold'), width=30, fg='royal blue').grid(row=i+1,columnspan=5)
                    Tkinter.Label(body, text=u"Start from iteration:", font=("Helvetica", 8, 'bold')).grid(row=i+2)
                    self.var = IntVar(root)
                    self.button1=Radiobutton(body, text="1", variable=self.var, value=1)
                    self.button1.grid(row=i+2,column=1)
                    self.button1.select()
                    self.button2=Radiobutton(body, text="2", variable=self.var, value=2).grid(row=i+2,column=2)
                    self.button3=Radiobutton(body, text="3", variable=self.var, value=3).grid(row=i+2,column=3)
                    self.button4=Radiobutton(body, text="Skip", variable=self.var, value=4).grid(row=i+2,column=4)
               elif i==23:
                    Tkinter.Label(body, text=u"Sample rates:", font=("Helvetica", 8, 'bold')).grid(row=i)
                    sv = StringVar(root)
                    sv.set("0.2,0.5,1.0")
                    self.samplerates="0.2,0.5,1.0"
                    sv.trace("w", lambda name, index, mode, sv=sv: self.setsamplerates(sv))
                    e = Entry(body, textvariable=sv)
                    e.config(width=15)
                    e.grid(row=i,column=1,columnspan=3)
               elif i==24:#action buttons
                    Tkinter.Label(body, text=u"", width=30).grid(row=i,columnspan=2)
                    buttons = Tkinter.Frame(self)
                    buttons.pack()
                    botton = Tkinter.Button(buttons, text=u"Update", font=("Helvetica", 9, 'bold'),width=10, command=self.update_parameters)
                    botton.pack(side=Tkconstants.LEFT)
                    Tkinter.Frame(buttons, width=10).pack(side=Tkconstants.LEFT)
                    button = Tkinter.Button(buttons, text=u"Quit", font=("Helvetica", 9, 'bold'), width=10, command=self.quit)
                    button.pack(side=Tkconstants.RIGHT)

        def setsamplerates(self,value):
            self.samplerates=value.get()

        def setDefaultProperties(self):
            self.runtimeFile=sys.argv[1]+"\\conf\\sandag_abm.properties"
            self.standardFile=sys.argv[1]+"\\conf\\sandag_abm_standard.properties"
            self.populateProperties()
            """
            for i in range(self.pNum):
                if i<3 or i>6:
                    self.yButton[i].select()
                    self.nButton[i].deselect()
                else:
                    self.yButton[i].deselect()
                    self.nButton[i].select()
            """

        def setATButtons(self):
            #disable create bike and walk logsums if 'copy' is chosen
            self.yButton[1].config(command=lambda: self.yButton[3].config(state=DISABLED))
            #self.yButton[2].config(command=lambda: self.yButton[4].config(state=DISABLED))
            #disable copy bike and walk logsums if 'create' is chosen
            self.yButton[3].config(command=lambda: self.yButton[1].config(state=DISABLED))
            #self.yButton[4].config(command=lambda: self.yButton[2].config(state=DISABLED))
            #enable create bike and walk logsums if NOT 'copy' is chosen
            self.nButton[1].config(command=lambda: self.yButton[3].config(state=ACTIVE))
            #self.nButton[2].config(command=lambda: self.yButton[4].config(state=ACTIVE))
            #enable copy bike and walk logsums if NOT 'create' is chosen
            self.nButton[3].config(command=lambda: self.yButton[1].config(state=ACTIVE))
            #self.nButton[4].config(command=lambda: self.yButton[2].config(state=ACTIVE))

        #set scenario path
        def get_scenariopath(self):
            # defining options for opening a directory; initialize default path from command line
            self.dir_opt = options = {}
            options['initialdir'] = sys.argv[1]
            options['mustexist'] = False
            options['parent'] = root
            options['title'] = 'This is a title'
            scenariopath = tkFileDialog.askdirectory(**self.dir_opt)
            if scenariopath:
                scenariopath = os.path.normpath(scenariopath)
                self.scenariopath.delete(0, Tkconstants.END)
                self.scenariopath.insert(0, scenariopath)
            else:
                self.scenariopath.delete(0, Tkconstants.END)
                self.scenariopath.insert(0, sys.argv[1])

            #property file settings
            self.runtimeFile=self.scenariopath.get()+"\\conf\\sandag_abm.properties"
            self.standardFile=self.scenariopath.get()+"\\conf\\sandag_abm_standard.properties"

            #populate properties
            self.populateProperties()

            return

        #populate properties with exisiting settings in scenario folder
        def populateProperties(self):
            if self.checkFile():
                for i in range(self.pNum):
                    if stringFinder.find(self.runtimeFile, self.properties[i]+" = true"):
                        self.yButton[i].deselect()
                        self.nButton[i].select()
                    elif stringFinder.find(self.runtimeFile, self.properties[i]+" = false"):
                        self.yButton[i].select()
                        self.nButton[i].deselect()
                    else:
                        print "Invalid property "+self.properties[i]+" value!, Property either has to be set to true or false."
            return

        # update parameters with user inputs
        def update_parameters(self):
            #property file settings
            self.runtimeFile=self.scenariopath.get()+"\\conf\\sandag_abm.properties"
            self.standardFile=self.scenariopath.get()+"\\conf\\sandag_abm_standard.properties"

            self.deleteProperty()
            self.old_text = [0 for x in range(self.pNum)]
            self.new_text = [0 for x in range(self.pNum)]
            for i in range(self.pNum):
                if self.buttonVar[i].get()==1:
                    self.old_text[i]=self.properties[i]+" = true"
                    self.new_text[i]=self.properties[i]+" = false"
                elif self.buttonVar[i].get()==0:
                    self.old_text[i]=self.properties[i]+" = false"
                    self.new_text[i]=self.properties[i]+" = true"

            #create a property update dictionary
            dic=[]
            for i in range(self.pNum):
                pair=(self.old_text[i],self.new_text[i])
                dic.append(pair)
                print dic[i][0],dic[i][1]
            #add iteration update to dictionary
            dic.append(("RunModel.startFromIteration = 1","RunModel.startFromIteration = "+str(self.var.get())))
            #add sample rates update to dictionary
            dic.append(("sample_rates=0.2,0.5,1.0","sample_rates="+self.samplerates))
            stringFinder.replace(self.standardFile,self.runtimeFile,dic)
            self.quit()

        #check if property file exists
        def checkFile(self):
            result=True
            if not os.path.exists(self.runtimeFile):
                self.popup=Tkinter.Tk()
                popupMsg.popupmsg(self,self.runtimeFile+" doesn't exist!",1)
                result=False
            return result

        #close popup window and run batch
        def letsgo(self):
            self.popup.destroy()
            return

        #run batch
        def deleteProperty(self):
            commandstr=u"del "+self.scenariopath.get()+"\\conf\\sandag_abm.properties"
            print commandstr
            os.system(commandstr)
            return

root = Tkinter.Tk()
root.resizable(True, False)
root.minsize(370, 0)
logo = Tkinter.PhotoImage(file=r"T:\ABM\release\ABM\SANDAG_logo.gif")
w=Label(root, image=logo, width=200)
w.pack(side='top', fill='both', expand='yes')
ParametersGUI(root).pack(fill=Tkconstants.X, expand=1, anchor=W)

root.mainloop()
