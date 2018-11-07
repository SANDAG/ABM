# This script checks the structure of the select link query file by attempting to open the file.
# The contents of the file are not checked
# Rick Curry, 5/16/18, initialed
# Yun Ma, 7/12/2018, modified

import os
import csv
import string
import sys

# check if property file exists
if not os.path.isfile('..\\input\\selectlink_query.qry'):
    print "selectlink_query.qry file not found"

else:
    import xml.etree.ElementTree as et
    try:
        tree = et.parse('..\\input\\selectlink_query.qry')
    except et.ParseError:
        print "ERROR: Something went wrong. Please check the select link query file."
        raise sys.exit(1)

