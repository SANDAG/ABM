__author__ = 'wsu'
import Tkinter
import Tkconstants
#popup window for path validity checking
def popupmsg(self,msg,numButtons,type):
    self.popup.wm_title("!!!WARNING!!!")
    label = Tkinter.Label(self.popup, text=msg)
    label.pack(side="top", fill="x", pady=10)
    popbuttons = Tkinter.Frame(self.popup)
    popbuttons.pack()
    #can't pass arguments to a callback, otherwise callback is called before widget is constructed; use lambda function instead
    B1 = Tkinter.Button(popbuttons, text="Proceed", command =lambda: self.executeBatch(type))
    B2 = Tkinter.Button(popbuttons, text="Quit", command = self.popup.destroy)
    if numButtons>1:
        B1.pack(side=Tkconstants.LEFT)
    B2.pack(side=Tkconstants.RIGHT)
    Tkinter.Frame(popbuttons, width=10).pack(side=Tkconstants.LEFT)
