__author__ = 'wsu'
import os

def check(fname, txt):
    with open(fname) as dataf:
        return any(txt in line for line in dataf)

def replace_temp(old_fname, new_fname, replaceDic):
    f1 = open(old_fname, 'r')
    f2 = open(new_fname, 'w')
    for line in f1:
        for pair in replaceDic:
            line=line.replace(pair[0], pair[1])
        f2.write(line)
    f1.close()
    f2.close()
    return

def replace(old_fname, replaceDic):
    replace_temp(old_fname,'T:/ABM/temp.txt',replaceDic)
    os.remove(old_fname)
    os.rename('T:/ABM/temp.txt',old_fname)
    return

def find(fname, txt):
    if check(fname, txt):
        return True
    else:
        return False

def findPropertyValue(fname, p):
    with open(fname) as dataf:
        for line in dataf:
            s=line.rsplit("=")[0].strip()
            if p.lower()==s.lower():
                return line.rsplit("=")[1].strip()

def findPropertySetting(fname, p):
    with open(fname) as dataf:
        for line in dataf:
            if line.find(p)>=0:
                return line