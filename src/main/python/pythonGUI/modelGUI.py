__author__ = 'wsu'
#Wu.Sun@sandag.org 7-12-2016
import Tkinter
import Tkconstants
import os

class ModelGUI(Tkinter.Frame):
        def __init__(self, root):
            Tkinter.Frame.__init__(self, root, border=5)
            self.status = Tkinter.Label(self, text=u"SANDAG ABM", font=("Helvetica", 12, 'bold'))
            self.status.pack(fill=Tkconstants.X, expand=1)
            body = Tkinter.Frame(self)
            body.pack(fill=Tkconstants.X, expand=1)
            sticky = Tkconstants.E + Tkconstants.W
            body.grid_columnconfigure(1, weight=2)

            button1 = Tkinter.Button(body, text=u"Create a Scenario", font=("Helvetica", 10, 'bold'),width=30, command=lambda: os.system('T:/ABM/release/ABM/dist/createScenario.exe'))
            button1.pack(side=Tkconstants.LEFT)
            button1.grid(row=0)

            button2 = Tkinter.Button(body, text=u"Edit Model Parameters", font=("Helvetica", 10, 'bold'),width=30, command=lambda: os.system('T:/ABM/release/ABM/dist/parameterEditor.exe'))
            button2.pack(side=Tkconstants.LEFT)
            button2.grid(row=1)

            button3 = Tkinter.Button(body, text=u"Quit", font=("Helvetica", 10, 'bold'), width=30, command=self.quit)
            button3.pack(side=Tkconstants.LEFT)
            button3.grid(row=2)

root = Tkinter.Tk()
root.resizable(True, False)
root.minsize(200, 0)
ModelGUI(root).pack(fill=Tkconstants.X, expand=1)
root.mainloop()
