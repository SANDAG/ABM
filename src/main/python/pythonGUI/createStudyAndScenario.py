# import tkinter
# import tkconstants
# import tkFileDialog
import os
# from tkinter import *
import tkinter
from tkinter import constants
from tkinter import filedialog
#from tkMessageBox import showerror
# from PIL import Image,ImageTk
import popupMsg
import sys

class SelectStudyYears(tkinter.Frame):
    def __init__(self, root, parent):
        tkinter.Frame.__init__(self, root, border = 5)
        self.root = root
        self.parent = parent
        body = tkinter.Frame(self)
        body.pack(fill=constants.X, expand=1)
        body.grid_columnconfigure(1, weight=2)

        current_row = 0
        tkinter.Label(body, text=u"Year Selection", font=("Helvetica", 10, 'bold')).grid(row=current_row)
        current_row += 1

        self.is_selected_year = {}
        for year in parent.studyYearList:
            self.is_selected_year[year] = tkinter.BooleanVar(self.root)
            if year in parent.selected_study_years:
                self.is_selected_year[year].set(True)
            tkinter.Checkbutton(body, text = year, variable = self.is_selected_year[year], font = ("Helvetica", 8)).grid(row=current_row, sticky = 'W')
            current_row += 1

        tkinter.Button(body, text=u"Select", font=("Helvetica", 8, 'bold'),width=10,command=lambda: self.record_selection()).grid(row=current_row)

    def record_selection(self):
        selection = []
        for year in self.is_selected_year:
            if self.is_selected_year[year].get():
                selection.append(year)
        self.parent.selected_study_years = ','.join(selection)
        self.parent.studyyears.delete(0, constants.END)
        self.parent.studyyears.insert(0, self.parent.selected_study_years)
        self.root.destroy()
        pass


class CreateScenarioGUI(tkinter.Frame):
        def __init__(self, root, emme_version = "4.3.7", year = "2022", geo = "1", lu = "S0", year_suffix = ""):
            tkinter.Frame.__init__(self, root, border=5)
            body = tkinter.Frame(self)
            body.pack(fill=constants.X, expand=1)
            sticky = constants.E + constants.W
            body.grid_columnconfigure(1, weight=2)

            #Define land use options
            self.lu_options = {
                "S0": {
                    "name": "Baseline",
                    "location": r"T:\socioec\Current_Projects\SR15\S0\version19",
                    "years": ["2022", "2035", "2050"]
                },
                "S1": {
                    "name": "Baseline + RHNA Adjustment",
                    "location": r"T:\socioec\Current_Projects\SR15\S1\version20",
                    "years": ["2026", "2029", "2035"]
                },
                "S2": {
                    "name": "Sustainable Community Strategy",
                    "location": r"T:\socioec\Current_Projects\SR15\S2\version17",
                    "years": ["2026", "2029", "2035"]
                },
                "S3": {
                    "name": "SCS with Higher Density",
                    "location": r"T:\socioec\Current_Projects\SR15\S3\version18",
                    "years": ["2035"]
                }
            }

            self.root = root
            self.emme_version = emme_version
            self.year = year
            self.geo = geo            
            self.lu = lu
            self.year_suffix = year_suffix
 
            # if self.year not in self.lu_options[self.lu]["years"]:
            #     if self.year in self.lu_options["DS41"]["years"]:
            #         self.lu = "DS41"
            #     else:
            #         self.lu = "DS42"

            yearOptionList = []
            for lu in self.lu_options:
                yearOptionList += self.lu_options[lu]["years"]
            yearOptionList = list(set(yearOptionList)) #Remove duplicates
            yearOptionList.sort()

            self.yearOptionList = yearOptionList
            self.studyYearList = ["2016", "2020", "2023", "2025_Vision", "2025nb", "2026_Vision", "2029_Vision",
                                  "2030_Vision", "2030nb", "2032_Vision", "2035_Vision", "2035nb", "2040_Vision",
                                  "2040nb", "2050_Vision", "2050nb"]

            #divider line
            divider=u"_"*200
            if getattr(sys, 'frozen', False):
                self.releaseDir=os.path.abspath(os.path.join(os.path.dirname(sys.executable), '..', '..'))
            else:
                self.releaseDir=os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..', '..', '..'))
            self.defaultScenarioDir="T:\\projects\\sr15\\MyRun"
            self.defaultNetworkDir="T:\\RTP\\2021RP\\2021rp_final\\network_build"

            current_row = 0
            n_columns = 3

            self.buttonVar= tkinter.IntVar(root)
            self.yButton=tkinter.Radiobutton(body, text="Yes", variable=self.buttonVar, value=1, command=self.initStudy)
            self.nButton=tkinter.Radiobutton(body, text="No", variable=self.buttonVar, value=0,command=self.initStudy)
            tkinter.Label(body, text=u"Release Version 15.0.0\n"+divider, font=("Helvetica", 11, 'bold'), width=50, fg='royal blue').grid(row=current_row,columnspan=5)
            current_row += 1
            tkinter.Label(body, text=u"Create an ABM Work Space", font=("Helvetica", 10, 'bold')).grid(row=current_row,columnspan=n_columns)
            current_row += 1
            self.yButton.grid(row=current_row,column=0, columnspan=n_columns-1)
            self.nButton.grid(row=current_row,column=1, columnspan=n_columns-1)
            current_row += 1

            tkinter.Label(body, text=u"Study Folder", font=("Helvetica", 8, 'bold')).grid(row=current_row)
            self.studypath = tkinter.Entry(body, width=40)
            self.studypath.grid(row=current_row, column=1, sticky=sticky)
            self.studypath.delete(0, constants.END)
            self.studypath.insert(0, self.defaultScenarioDir)
            self.studybutton = tkinter.Button(body, text=u"...",width=4,command=lambda:self.get_path("study"))
            self.studybutton.grid(row=current_row, column=n_columns-1)
            current_row += 1

            tkinter.Label(body, text=u"Network Folder",font=("Helvetica", 8, 'bold')).grid(row=current_row)
            self.studynetworkpath = tkinter.Entry(body, width=40)
            self.studynetworkpath.grid(row=current_row, column=1, sticky=sticky)
            self.studynetworkpath.delete(0, constants.END)
            self.studynetworkpath.insert(0, self.defaultNetworkDir)
            self.studynetworkbutton = tkinter.Button(body, text=u"...",width=4,command=lambda: self.get_path("studynetwork"))
            self.studynetworkbutton.grid(row=current_row, column=n_columns-1)
            self.selected_study_years = ''
            current_row += 1

            tkinter.Label(body, text=u"Year Selection",font=("Helvetica", 8, 'bold')).grid(row=current_row)
            self.studyyears = tkinter.Entry(body, width=40)
            self.studyyears.grid(row=current_row, column=1, sticky=sticky)
            self.studyyears.delete(0, constants.END)
            self.studyyearsbutton = tkinter.Button(body, text=u"...",width=4,command=lambda: self.select_study_years())
            self.studyyearsbutton.grid(row=current_row, column=n_columns-1)
            current_row += 1
            
            self.copyButton = tkinter.Button(body, text=u"Create", font=("Helvetica", 8, 'bold'),width=10, command=lambda: self.checkPath("study"))
            self.copyButton.grid(row=current_row,column=0,columnspan=n_columns+1)
            current_row += 1

            tkinter.Label(body, text=divider, font=("Helvetica", 11, 'bold'), width=50, fg='royal blue').grid(row=current_row,columnspan=n_columns+2)
            current_row += 1
            tkinter.Label(body, text=u"Create an ABM scenario", font=("Helvetica", 10, 'bold')).grid(row=current_row,columnspan=n_columns)
            current_row += 1

            #tkinter.Label(body, text=u"Version", font=("Helvetica", 8, 'bold')).grid(row=8)
            #var = StringVar(root)
            if getattr(sys, 'frozen', False):
                self.version=os.path.basename(os.path.abspath(os.path.join(os.path.dirname(sys.executable), '..')))
            else:
                self.version=os.path.basename(os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..', '..')))
            #optionList=["version_14_2_2"]
            #option=tkinter.OptionMenu(body,var,*optionList,command=self.setversion)
            #option.config(width=50)
            #option.grid(row=8, column=1)

            tkinter.Label(body, text=u"Emme Version", font=("Helvetica", 8, 'bold')).grid(row=current_row)
            var = tkinter.StringVar(root)
            #self.emme_version = "4.4.4.1"
            optionList = ["4.3.7"]
            var.set(self.emme_version)
            option = tkinter.OptionMenu(body, var, *optionList, command=self.setEmmeVersion)
            option.config(width=50)
            option.grid(row=current_row, column=1)
            current_row += 1

            tkinter.Label(body, text=u"Year", font=("Helvetica", 8, 'bold')).grid(row=current_row)
            var = tkinter.StringVar(root)
            #self.year="2016"
            #yearOptionList = ["2016", "2020", "2023", "2025", "2025nb", "2026", "2029", "2030", "2030nb", "2032", "2035", "2035nb", "2040", "2040nb", "2050","2050nb"]
            #if self.select_lu:
            var.set(self.year)
            #else:
            #    var.set("Select Year")
            option=tkinter.OptionMenu(body,var,*yearOptionList,command=self.setyear)
            option.config(text = self.year)
            option.config(width=50)
            option.grid(row=current_row, column=1)
            current_row += 1
            #option.pack(expand = True)

            tkinter.Label(body, text=u"Build Status", font=("Helvetica", 8, 'bold')).grid(row=current_row)
            var = tkinter.StringVar(root)
            buildOptionList = ["Build", "No Build"]
            if self.year == "2022":
                self.year_suffix = ""
                buildOptionList = ["Base"]
                var.set("Base")
            elif self.year_suffix == "nb":
                var.set("No Build")
            else:
                var.set("Build")
            option=tkinter.OptionMenu(body,var,*buildOptionList,command=self.setSuffix)
            option.config(width=50)
            option.grid(row=current_row, column=1)
            current_row += 1
			
            tkinter.Label(body, text=u"Land Use", font=("Helvetica", 8, 'bold')).grid(row=current_row)
            #self.lu="DS41"
            #if self.select_lu:
            var = tkinter.StringVar(root)
            var.set(self.lu + '-' + self.lu_options[self.lu]["name"])
            #if self.year in self.invalid_combos["DS42"]:
            #    luOptionList = ["DS41-Baseline"]
            #elif self.year in self.invalid_combos["DS41"]:
            #    luOptionList = ["DS42-Sustainable Community Strategy"]
            #else:
            #    luOptionList = ["DS41-Baseline", "DS42"]
            luOptionList = []
            for lu in self.lu_options:
                if self.year in self.lu_options[lu]["years"]:
                    luOptionList.append(lu + '-' + self.lu_options[lu]["name"])
            option=tkinter.OptionMenu(body,var,*luOptionList,command=self.setLU)

            option.config(width=50)
            option.grid(row=current_row, column=1)
            current_row += 1

            tkinter.Label(body, text=u"Geography ID", font=("Helvetica", 8, 'bold')).grid(row=current_row)
            #self.geo="1"
            self.geo = tkinter.Entry(body, width=40)
            self.geo.grid(row=current_row, column=1, sticky=sticky)
            self.geo.delete(0, constants.END)
            self.geo.insert(0, 1)
            current_row += 1
            #option.pack(expand = True)
			
            tkinter.Label(body, text=u"Scenario Folder", font=("Helvetica", 8, 'bold')).grid(row=current_row)
            self.scenariopath = tkinter.Entry(body, width=40)
            self.scenariopath.grid(row=current_row, column=1, sticky=sticky)
            button = tkinter.Button(body, text=u"...",width=4,command=lambda: self.get_path("scenario"))
            button.grid(row=current_row, column=2)
            current_row += 1

            tkinter.Label(body, text=u"Network Folder",font=("Helvetica", 8, 'bold')).grid(row=current_row)
            self.networkpath = tkinter.Entry(body, width=40)
            self.networkpath.grid(row=current_row, column=1, sticky=sticky)
            button = tkinter.Button(body, text=u"...",width=4,command=lambda: self.get_path("network"))
            button.grid(row=current_row, column=2)
            current_row += 1

            buttons = tkinter.Frame(self)
            buttons.pack()
            botton = tkinter.Button(buttons, text=u"Create", font=("Helvetica", 8, 'bold'),width=10, command=lambda: self.checkPath("scenario"))
            botton.pack(side=constants.LEFT)
            #botton.grid(row=13, column = 0)
            tkinter.Frame(buttons, width=10).pack(side=constants.LEFT)
            button = tkinter.Button(buttons, text=u"Quit", font=("Helvetica", 8, 'bold'), width=10, command=self.quit)
            button.pack(side=constants.RIGHT)
            #button.grid(row = 13, columns = 2)

            self.defaultpath=self.releaseDir+"\\"+self.version+'\\input\\'+self.year+self.year_suffix
            self.scenariopath.delete(0, constants.END)
            self.scenariopath.insert(0, self.defaultScenarioDir)
            self.networkpath.delete(0, constants.END)
            self.networkpath.insert(0, self.defaultpath)

            self.initStudy()

        def initStudy(self):
            #disable study setting buttons
            if self.buttonVar.get()==1:
                self.studypath.config(state=tkinter.NORMAL)
                self.studybutton.config(state=tkinter.NORMAL)
                self.studynetworkpath.config(state=tkinter.NORMAL)
                self.studynetworkbutton.config(state=tkinter.NORMAL)
                self.studyyears.config(state=tkinter.NORMAL)
                self.studyyearsbutton.config(state=tkinter.NORMAL)
                self.copyButton.configure(state=tkinter.NORMAL)
            #enable study setting buttons
            elif self.buttonVar.get()==0:
                self.studypath.config(state=tkinter.DISABLED)
                self.studybutton.config(state=tkinter.DISABLED)
                self.studynetworkpath.config(state=tkinter.DISABLED)
                self.studynetworkbutton.config(state=tkinter.DISABLED)
                self.studyyears.config(state=tkinter.DISABLED)
                self.studyyearsbutton.config(state=tkinter.DISABLED)
                self.copyButton.configure(state=tkinter.DISABLED)

        #set default input and network paths based on selected year
        def setversion(self,value):
            self.version=value
            return

        # set Emme version
        def setEmmeVersion(self, value):
            self.emme_version = value
            return

        #set default input and network paths based on selected year
        def setyear(self,value):
            self.year=value
            #Refresh the GUI with inputs already entered
            self.destroy()
            CreateScenarioGUI(self.root, self.emme_version, self.year, self.geo, self.lu, self.year_suffix).pack(fill=constants.X, expand=1)
            return

        # set Geography Set ID
        def setgeo(self, value):
            self.geo = value
            return

        # set land use
        def setLU(self,value):
            self.lu = value.split("-")[0]
            return
        
        # set build/no-build
        def setSuffix(self, value):
            if value == "No Build":
                self.year_suffix = "nb"
            else:
                self.year_suffix = ""
            #Refresh the GUI with inputs already entered
            self.destroy()
            CreateScenarioGUI(self.root, self.emme_version, self.year, self.geo, self.lu, self.year_suffix).pack(fill=constants.X, expand=1)
            return

        #set cluster
        def setcluster(self,value):
            self.cluster=value
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
            path = filedialog.askdirectory(**self.dir_opt)
            if type=="scenario":
                if path:
                    spath = os.path.normpath(path)
                    self.scenariopath.delete(0, constants.END)
                    self.scenariopath.insert(0, spath)
            elif type=="network":
                if path:
                    npath = os.path.normpath(path)
                    self.networkpath.delete(0, constants.END)
                    self.networkpath.insert(0, npath)
            elif type=="study":
                if path:
                    studypath = os.path.normpath(path)
                    self.studypath.delete(0, constants.END)
                    self.studypath.insert(0, studypath)
            elif type=="studynetwork":
                if path:
                    studynetworkpath = os.path.normpath(path)
                    self.studynetworkpath.delete(0, constants.END)
                    self.studynetworkpath.insert(0, studynetworkpath)
            return

        #check if a path already exisits or is empty
        def checkPath(self,type):
            self.popup=tkinter.Tk()
            if type=="scenario":
                #Check if invalid year/land use combo and don't create scenario if that is the case
                #if self.year not in self.lu_options[self.lu]["years"]: 
                #    showerror("Error", "Invalid year/land use combination")
                #    return
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
            return

        def select_study_years(self):
            selection_root = tkinter.Tk()
            SelectStudyYears(selection_root, self).pack(fill=constants.X, expand=1)

        #Update properties file
        def update_property(self, old, new):
            property_file = os.path.join(self.scenariopath.get(), 'conf', 'sandag_abm.properties')
            property_file = property_file.replace('\\\\', '/')
            with open(property_file, 'r') as file :
                filedata = file.read()
            filedata = filedata.replace(old, new)
            with open(property_file, 'w') as file:
                file.write(filedata)

        #execute DOS commands
        def executeBatch(self, type):
            self.popup.destroy()
            if type=="scenario":
                if self.lu == "S0" and self.year in ["2035", "2050"]:
                    lu_input_path = os.path.join(self.lu_options[self.lu]["location"], "abm_csv", "processed", self.year + "nb")
                else:
                    lu_input_path = os.path.join(self.lu_options[self.lu]["location"], "abm_csv", "processed", self.year)
                commandstr = u"create_scenario.cmd %s %s %s %s %s %s" % (
                    self.scenariopath.get(),
                    self.year,
                    self.networkpath.get(),
                    self.emme_version,
                    lu_input_path,
                    '"' + self.year_suffix + '"'
                )
                os.chdir(self.releaseDir+"\\"+self.version+'\\')
                os.system(commandstr)
                self.update_property("landuse.version = S0", "landuse.version = " + self.lu)
                self.update_property("geographyID = 1", "geographyID = " + self.geo.get())
                self.update_property("network = NETWORK", "network = " + self.networkpath.get())
            elif type=="study":
                studyyears = self.studyyears.get().split(',')
                exclude_file = self.studynetworkpath.get() + '\\exclude.txt'
                exclude = ["exclude.txt", "\\2050_vision_nopurple\\"]
                for year in self.studyYearList:
                    if year not in studyyears:
                        exclude.append("\\" + year + "\\")
                f = open(exclude_file, 'w')
                f.write('\n'.join(exclude))
                f.close()
                commandstr=u"copy_networkfiles_to_study.cmd "+self.studypath.get()+" "+self.studynetworkpath.get()
                print(commandstr)
                os.chdir(self.releaseDir+"\\"+self.version+'\\')
                os.system(commandstr)    
                os.remove(exclude_file)
            self.popup=tkinter.Tk()
            msg="You have successfully created the "+ type+"!"
            popupMsg.popupmsg(self,msg,1,type)
            return

root = tkinter.Tk()
root.resizable(True, False)
root.minsize(370, 0)
logo = tkinter.PhotoImage(file=r"T:\ABM\release\ABM\SANDAG_logo.gif")
w=tkinter.Label(root, image=logo, width=200)
w.pack(side='top', fill='both', expand='yes')
CreateScenarioGUI(root).pack(fill=constants.X, expand=1)
root.mainloop()