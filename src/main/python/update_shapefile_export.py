import os
from os.path import join
import re

file_match = 'hwy_load_\d+.shp'
regex = re.compile(file_match)

for root, dirs, files in os.walk('T:/projects/sr13'):
    file_name = filter(regex.match, files)
    if not dirs:
        print root
    if file_name:
        for name in file_name:
            print '>> %s' % join(root, name)