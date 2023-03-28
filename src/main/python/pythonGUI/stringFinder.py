__author__ = 'wsu'
def check(fname, txt):
    with open(fname) as dataf:
        return any(txt in line for line in dataf)

def replace(old_fname, new_fname, replaceDic):
    f1 = open(old_fname, 'r')
    f2 = open(new_fname, 'w', newline='')
    for line in f1:
        for pair in replaceDic:
            line=line.replace(pair[0], pair[1])
        f2.write(line)
    f1.close()
    f2.close()
    return

def find(fname, txt):
    if check(fname, txt):
        return True
    else:
        return False