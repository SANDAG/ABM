__author__ = 'wsu'
import tkinter
import tkinter.constants
#popup window for path validity checking
def popupmsg(self,msg,numButtons,type):
    self.popup.wm_title("!!!WARNING!!!")
    label = tkinter.Label(self.popup, text=msg)
    label.pack(side="top", fill="x", pady=10)
    popbuttons = tkinter.Frame(self.popup)
    popbuttons.pack()
    #can't pass arguments to a callback, otherwise callback is called before widget is constructed; use lambda function instead
    B1 = tkinter.Button(popbuttons, text="Proceed", command =lambda: self.executeBatch(type))
    B2 = tkinter.Button(popbuttons, text="Quit", command = self.popup.destroy)
    if numButtons>1:
        B1.pack(side=tkinter.constants.LEFT)
    B2.pack(side=tkinter.constants.RIGHT)
    tkinter.Frame(popbuttons, width=10).pack(side=tkinter.constants.LEFT)
