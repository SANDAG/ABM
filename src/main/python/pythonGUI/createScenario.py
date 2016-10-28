__author__ = 'wsu'
#Wu.Sun@sandag.org 7-22-2016
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
            self.status = Tkinter.Label(self, text=u"Create an ABM Scenario", font=("Helvetica", 12, 'bold'))
            self.status.pack(fill=Tkconstants.X, expand=1)
            body = Tkinter.Frame(self)
            body.pack(fill=Tkconstants.X, expand=1)
            sticky = Tkconstants.E + Tkconstants.W
            body.grid_columnconfigure(1, weight=2)

            Tkinter.Label(body, text=u"Version", font=("Helvetica", 8, 'bold')).grid(row=0)
            var = StringVar(root)
            self.version="version_13_3_0"
            optionList=["version_13_3_0", "version_13_3_1_SNAPSHOT"]
            option=Tkinter.OptionMenu(body,var,*optionList,command=self.setversion)
            option.config(width=50)
            option.grid(row=0, column=1)

            Tkinter.Label(body, text=u"Year", font=("Helvetica", 8, 'bold')).grid(row=1)
            var = StringVar(root)
            self.year="2012"
            optionList=["2012", "2014", "2016", "2017", "2020", "2025", "2030", "2035", "2040", "2045", "2050"]
            option=Tkinter.OptionMenu(body,var,*optionList,command=self.setyear)
            option.config(width=50)
            option.grid(row=1, column=1)

            """
            Tkinter.Label(body, text=u"Cluster", font=("Helvetica", 8, 'bold')).grid(row=2)
            var = StringVar(root)
            self.cluster="local"
            optionList=["local", "aztec", "gaucho", "wildcat"]
            option=Tkinter.OptionMenu(body,var,*optionList, command=self.setcluster)
            option.config(width=50)
            option.grid(row=2, column=1)
            """

            Tkinter.Label(body, text=u"Scenario Folder", font=("Helvetica", 8, 'bold')).grid(row=3)
            self.scenariopath = Tkinter.Entry(body, width=40)
            self.scenariopath.grid(row=3, column=1, sticky=sticky)
            button = Tkinter.Button(body, text=u"...",width=4,command=self.get_scenariopath)
            button.grid(row=3, column=2)

            Tkinter.Label(body, text=u"Network Folder",font=("Helvetica", 8, 'bold')).grid(row=4)
            self.networkpath = Tkinter.Entry(body, width=40)
            self.networkpath.grid(row=4, column=1, sticky=sticky)
            button = Tkinter.Button(body, text=u"...",width=4,command=self.get_networkpath)
            button.grid(row=4, column=2)

            buttons = Tkinter.Frame(self)
            buttons.pack()
            botton = Tkinter.Button(buttons, text=u"Create", font=("Helvetica", 8, 'bold'),width=10, command=self.checkPath)
            botton.pack(side=Tkconstants.LEFT)
            Tkinter.Frame(buttons, width=10).pack(side=Tkconstants.LEFT)
            button = Tkinter.Button(buttons, text=u"Quit", font=("Helvetica", 8, 'bold'), width=10, command=self.quit)
            button.pack(side=Tkconstants.RIGHT)

        #set default input and network paths based on selected year
        def setversion(self,value):
            self.version=value
            return

        #set default input and network paths based on selected year
        def setyear(self,value):
            self.defaultpath='T:\\ABM\\release\\ABM\\'+self.version+'\\input\\'+value
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
            self.popup=Tkinter.Tk()
            if os.path.exists(self.scenariopath.get()):
                popupMsg.popupmsg(self,"Selected scenario folder already exists! Proceeding will overwrite existing files!",2,"scenario")
            elif not self.scenariopath.get():
                popupMsg.popupmsg(self,"Scenario is empty!",1)
            else:
                self.executeBatch()
            return

        #run batch
        def executeBatch(self):
            self.popup.destroy()
            commandstr=u"create_scenario.cmd "+self.scenariopath.get()+" "+self.year+" "+self.networkpath.get()
            print commandstr
            os.chdir('T:/ABM/release/ABM/'+self.version+'/')
            os.system(commandstr)
            self.popup=Tkinter.Tk()
            popupMsg.popupmsg(self,"You have successfully created the scenario!",1)
            return

root = Tkinter.Tk()
root.resizable(True, False)
root.minsize(370, 0)
logo = Tkinter.PhotoImage(file=r"T:\ABM\release\ABM\SANDAG_logo.gif")
w=Label(root, image=logo, width=200)
w.pack(side='top', fill='both', expand='yes')
CreateScenarioGUI(root).pack(fill=Tkconstants.X, expand=1)
root.mainloop()


