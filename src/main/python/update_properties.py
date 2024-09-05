import sys

path = sys.argv[1]
year = sys.argv[2]
suffix = sys.argv[3]

with open(path, 'r') as file:
    properties = file.read()
properties = properties.replace(r"${year}",year)
properties = properties.replace(r"${suffix}",suffix)
with open(path, 'w') as file:
    file.writelines(properties)