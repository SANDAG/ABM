__author__ = 'wsu'
#Wu.Sun@sandag.org 7-1-2016
import Tkinter
import Tkconstants
import tkFileDialog
import os
from tkinter import *
from PIL import Image,ImageTk

class CreateScenarioGUI(Tkinter.Frame):
        def __init__(self, root):
            Tkinter.Frame.__init__(self, root, border=5)
            self.status = Tkinter.Label(self, text=u"Create a SANDAG ABM Scenario")
            self.status.pack(fill=Tkconstants.X, expand=1)
            body = Tkinter.Frame(self)
            body.pack(fill=Tkconstants.X, expand=1)
            sticky = Tkconstants.E + Tkconstants.W
            body.grid_columnconfigure(1, weight=2)

            Tkinter.Label(body, text=u"Year").grid(row=0)
            var = StringVar(root)
            self.year="2012"
            optionList1=["2012", "2015", "2020", "2050"]
            option1=Tkinter.OptionMenu(body,var,*optionList1,command=self.setyear)
            option1.config(width=35)
            option1.grid(row=0, column=1)

            Tkinter.Label(body, text=u"Cluster").grid(row=1)
            var = StringVar(root)
            self.cluster="local"
            optionList2=["local", "aztec", "gaucho", "wildcat"]
            option2=Tkinter.OptionMenu(body,var,*optionList2, command=self.setcluster)
            option2.config(width=35)
            option2.grid(row=1, column=1)

            Tkinter.Label(body, text=u"Scenario Folder").grid(row=2)
            self.scenariopath = Tkinter.Entry(body, width=30)
            self.scenariopath.grid(row=2, column=1, sticky=sticky)
            button = Tkinter.Button(body, text=u"...",width=4,command=self.get_scenariopath)
            button.grid(row=2, column=2)

            Tkinter.Label(body, text=u"Network Folder").grid(row=3)
            self.networkpath = Tkinter.Entry(body, width=30)
            self.networkpath.grid(row=3, column=1, sticky=sticky)
            button = Tkinter.Button(body, text=u"...",width=4,command=self.get_networkpath)
            button.grid(row=3, column=2)

            """
            Wu's Note: inputs, including default network files, always copied from standard release; disallow input folder browsing for now.
            Tkinter.Label(body, text=u"Input Folder").grid(row=4)
            self.inputpath = Tkinter.Entry(body, width=30)
            self.inputpath.grid(row=4, column=1, sticky=sticky)
            button = Tkinter.Button(body, text=u"...",width=4,command=self.get_inputpath)
            button.grid(row=4, column=2)
            """

            """
            Wu's Note: image seems like depend on Python version; hard to implement when users' Python versions are different
            image = Image.open("SANDAG_logo.jpg")
            photo = ImageTk.PhotoImage(image)
            label = Label(image=photo)
            label.image = photo # keep a reference!
            label.pack()
            label.grid(row=5,column=1)
            """

            buttons = Tkinter.Frame(self)
            buttons.pack()
            botton = Tkinter.Button(buttons, text=u"Create", width=10, command=self.checkPath)
            botton.pack(side=Tkconstants.LEFT)
            Tkinter.Frame(buttons, width=10).pack(side=Tkconstants.LEFT)
            button = Tkinter.Button(buttons, text=u"Quit", width=10, command=self.quit)
            button.pack(side=Tkconstants.RIGHT)

        #set default input and network paths based on selected year
        def setyear(self,value):
            self.defaultpath='T:\\ABM\\release\\version13\\input\\'+value
            self.scenariopath.delete(0, Tkconstants.END)
            self.scenariopath.insert(0, "T:\\projects\\sr13")
            #self.inputpath.delete(0, Tkconstants.END)
            #self.inputpath.insert(0, self.defaultpath)
            self.networkpath.delete(0, Tkconstants.END)
            self.networkpath.insert(0, self.defaultpath)
            self.year=value
            return

        #set cluster
        def setcluster(self,value):
            self.cluster=value
            return

        #set scenario path
        def get_scenariopath(self):
            # defining options for opening a directory
            self.dir_opt = options = {}
            options['initialdir'] = "T:\\projects\\sr13"
            options['mustexist'] = False
            options['parent'] = root
            options['title'] = 'This is a title'
            scenariopath = tkFileDialog.askdirectory(**self.dir_opt)
            if scenariopath:
                scenariopath = os.path.normpath(scenariopath)
                self.scenariopath.delete(0, Tkconstants.END)
                self.scenariopath.insert(0, scenariopath)
            return

        #set network path
        def get_networkpath(self):
            self.dir_opt = options = {}
            options['initialdir'] = "T:\\projects\\sr13"
            options['mustexist'] = False
            options['parent'] = root
            options['title'] = 'This is a title'
            networkpath = tkFileDialog.askdirectory(**self.dir_opt)
            if networkpath:
                networkpath = os.path.normpath(networkpath)
                self.networkpath.delete(0, Tkconstants.END)
                self.networkpath.insert(0, networkpath)
            return

        #check scenario path validity
        def checkPath(self):
            if os.path.exists(self.scenariopath.get()):
                self.popup=Tkinter.Tk()
                self.popupmsg("Selected scenario folder already exists! Proceeding will overwrite existing files!",2)
            elif not self.scenariopath.get():
                self.popup=Tkinter.Tk()
                self.popupmsg("Scenario is empty!",1)
            else:
                self.executeBath()
            return

        #close popup window and run batch
        def letsgo(self):
            self.popup.destroy()
            self.executeBath()
            return

        #run batch
        def executeBath(self):
            commandstr=u"create_scenario.cmd "+self.scenariopath.get()+" "+self.year+" "+self.cluster+" "+self.networkpath.get()
            #commandstr=u"create_scenario.cmd t:\\abm\\test3 2012 aztec"
            print commandstr
            # TODO: Finalize later
            os.chdir('T:/ABM/release/ABM/13.3.0-SNAPSHOT_3')
            os.system(commandstr)
            return

        #popup window for path validity checking
        def popupmsg(self,msg,numButtons):
            self.popup.wm_title("!!!WARNING!!!")
            label = Tkinter.Label(self.popup, text=msg)
            label.pack(side="top", fill="x", pady=10)
            popbuttons = Tkinter.Frame(self.popup)
            popbuttons.pack()
            B1 = Tkinter.Button(popbuttons, text="Proceed", command =self.letsgo)
            B2 = Tkinter.Button(popbuttons, text="Abandon", command = self.popup.destroy)
            if numButtons>1:
                B1.pack(side=Tkconstants.LEFT)
            B2.pack(side=Tkconstants.RIGHT)
            Tkinter.Frame(popbuttons, width=10).pack(side=Tkconstants.LEFT)

        #set input path; network path is deafult to the same location as input path
        """
        Wu's Note: inputs, including default network files, always copied from standard release; disallow input folder browsing for now.
        def get_inputpath(self):
            self.dir_opt = options = {}
            options['initialdir'] = self.defaultpath
            options['mustexist'] = False
            options['parent'] = root
            options['title'] = 'This is a title'
            inputpath = tkFileDialog.askdirectory(**self.dir_opt)
            if inputpath:
                inputpath = os.path.normpath(inputpath)
                self.inputpath.delete(0, Tkconstants.END)
                self.inputpath.insert(0, inputpath)
                self.networkpath.delete(0, Tkconstants.END)
                self.networkpath.insert(0, inputpath)
            return
        """

root = Tkinter.Tk()
root.resizable(True, False)
root.minsize(370, 0)
CreateScenarioGUI(root).pack(fill=Tkconstants.X, expand=1)
root.mainloop()


