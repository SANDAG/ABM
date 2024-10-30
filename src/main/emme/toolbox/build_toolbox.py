#//////////////////////////////////////////////////////////////////////////////
#////                                                                       ///
#//// Copyright INRO, 2016-2017.                                            ///
#//// Rights to use and modify are granted to the                           ///
#//// San Diego Association of Governments and partner agencies.            ///
#//// This copyright notice must be preserved.                              ///
#////                                                                       ///
#//// build_toolbox.py                                                      ///
#////                                                                       ///
#////     Generates an mtbx (Emme Modeller Toolbox), based on the structure ///  
#////     of the Python source tree.                                        ///
#////                                                                       ///
#////     Usage: build_toolbox.py [-s source_folder] [-p toolbox_path]      ///
#////                                                                       ///
#////         [-p toolbox_path]: Specifies the name of the MTBX file.       ///
#////              If omitted,defaults to "sandag_toolbox.mtbx"             ///
#////         [-s source_folder]: The location of the source code folder.   ///
#////             If omitted, defaults to the working directory.            ///
#////         [-l] [--link] Build the toolbox with references to the files  ///
#////             Use with developing or debugging scripts, changes to the  ///
#////             scripts can be used with a "Refresh" of the toolbox       ///
#////         [-c] [--consolidate] Build the toolbox with copies of the     ///
#////             scripts included inside the toolbox.                      ///
#////             Use to have a "frozen" version of the scripts with node   ///
#////             changes available.                                        ///
#////                                                                       ///
#////                                                                       ///
#////                                                                       ///
#//////////////////////////////////////////////////////////////////////////////
#
# Example:
#  python "T:\projects\sr13\develop\emme_conversion\git\sandag_abm\ABM_EMME\src\main\emme\toolbox\build_toolbox.py" --link 
#    -p "T:\projects\sr14\abm2_test\abm_runs\14_2_0\2035D_Hyperloop\emme_project\Scripts\sandag_toolbox.mtbx" 
#    -s T:\projects\sr13\develop\emme_conversion\git\sandag_abm\ABM_EMME\src\main\emme\toolbox


import os
import re
from datetime import datetime
import subprocess
import sqlite3.dbapi2 as sqllib
import base64
import pickle


def check_namespace(ns):
    if not re.match("^[a-zA-Z][a-zA-Z0-9_]*$", ns) and ns != '__pycache__':
        raise Exception("Namespace '%s' is invalid" % ns)


def get_emme_version():
    emme_process = subprocess.Popen(['Emme', '-V'], stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    output = emme_process.communicate()[0]
    return output.decode().split(' ')[2]


def usc_transform(value):
    try:
        return str(value)
    except Exception:
        return str(str(value), encoding="raw-unicode-escape")


class BaseNode(object):
    def __init__(self, namespace, title):
        check_namespace(namespace)
        self.namespace = namespace 
        self.title = title
        self.element_id = None
        self.parent = None
        self.root = None
        self.children = []

    def add_folder(self, namespace):
        node = FolderNode(namespace, parent=self)
        self.children.append(node)
        return node
    
    def add_tool(self, script_path, namespace):
        try:
            node = ToolNode(namespace, script_path, parent=self)
            self.children.append(node)
            with open(script_path, 'r') as f:
                for line in f:
                    if line.startswith("TOOLBOX_ORDER"):
                        node.order = int(line.split("=")[1])
                    if line.startswith("TOOLBOX_TITLE"):
                        title = line.split("=")[1].strip()
                        node.title = title[1:-1]  # exclude first and last quotes
        except Exception(e):
            print (script_path, namespace)
            print (type(e), str(e))
            return None
        return node

    def consolidate(self):
        for child in self.children:
            child.consolidate()

    def set_toolbox_order(self):
        self.element_id = self.root.next_id()
        self.children.sort(key=lambda x: x.order or 0)
        for child in self.children:
            child.set_toolbox_order()


class ElementTree(BaseNode):
    
    def __init__(self, namespace, title):
        super(ElementTree, self).__init__(namespace, title)
        self.next_element_id = 0
        self.begin = str(datetime.now())
        self.version = "Emme %s" % get_emme_version()
        self.root = self
    
    def next_id(self):
        self.next_element_id += 1
        return self.next_element_id


class FolderNode(BaseNode):
    
    def __init__(self, namespace, parent):
        title = namespace.replace("_", " ").capitalize()
        super(FolderNode, self).__init__(namespace, title)
        self.parent = parent
        self.root = parent.root
        self.element_id = None

    @property
    def order(self):
        child_order = [child.order for child in self.children if child.order is not None]
        if child_order:
            return min(child_order)
        return None


class ToolNode():
    
    def __init__(self, namespace, script_path, parent):
        check_namespace(namespace)
        self.namespace = namespace
        self.title = namespace.replace("_", " ").capitalize()

        self.root = parent.root
        self.parent = parent
        self.element_id = None
        self.order = None
        
        self.script = script_path
        self.extension = '.py'
        self.code = ''
            
    def consolidate(self):
        with open(self.script, 'r') as f:
            code = f.read()
        self.code = base64.b64encode(pickle.dumps(code))
        self.script = ''

    def set_toolbox_order(self):
        self.element_id = self.root.next_id()

class MTBXDatabase():    
    FORMAT_MAGIC_NUMBER = 'B8C224F6_7C94_4E6F_8C2C_5CC06F145271'
    TOOLBOX_MAGIC_NUMBER = 'TOOLBOX_C6809332_CD61_45B3_9060_411D825669F8'
    CATEGORY_MAGIC_NUMBER = 'CATEGORY_984876A0_3350_4374_B47C_6D9C5A47BBC8'
    TOOL_MAGIC_NUMBER = 'TOOL_1AC06B56_6A54_431A_9515_0BF77013646F'
    
    def __init__(self, filepath, title):
        if os.path.exists(filepath): 
            os.remove(filepath)
        
        self.db = sqllib.connect(filepath)
        
        self._create_attribute_table()
        self._create_element_table()
        self._create_document_table()
        self._create_triggers()
        
        self._initialize_documents_table(title)

    def _create_attribute_table(self):
        sql = """CREATE TABLE attributes(
            element_id INTEGER REFERENCES elements(element_id),
            name VARCHAR,
            value VARCHAR,
            PRIMARY KEY(element_id, name));"""
        
        self.db.execute(sql)
    
    def _create_element_table(self):
        sql = """CREATE TABLE elements(
            element_id INTEGER PRIMARY KEY AUTOINCREMENT,
            parent_id INTEGER REFERENCES elements(element_id),
            document_id INTEGER REFERENCES documents(document_id),
            tag VARCHAR,
            text VARCHAR,
            tail VARCHAR);"""
        
        self.db.execute(sql)
    
    def _create_document_table(self):
        sql = """CREATE TABLE documents(
            document_id INTEGER PRIMARY KEY AUTOINCREMENT,
            title VARCHAR);"""
        
        self.db.execute(sql)
    
    def _create_triggers(self):
        sql = """CREATE TRIGGER documents_delete
            BEFORE DELETE on documents
            FOR EACH ROW BEGIN
                DELETE FROM elements WHERE document_id = OLD.document_id;
            END"""
            
        self.db.execute(sql)
        
        sql = """CREATE TRIGGER elements_delete
            BEFORE DELETE on elements
            FOR EACH ROW BEGIN
                DELETE FROM attributes WHERE element_id = OLD.element_id;
            END"""
        
        self.db.execute(sql)
    
    def _initialize_documents_table(self, title):
        sql = """INSERT INTO documents (document_id, title)
                VALUES (1, '%s');""" % title
        
        self.db.execute(sql)
        self.db.commit()

    def populate_tables_from_tree(self, tree):        
        
        #Insert into the elements table
        column_string = "element_id, document_id, tag, text, tail"
        value_string = "{id}, 1, '{title}', '', ''".format(
            id=tree.element_id, title=tree.title)
        sql = """INSERT INTO elements (%s)
                VALUES (%s);""" % (column_string, value_string)
        self.db.execute(sql)
        
        #Insert into the attributes table
        column_string = "element_id, name, value"
        atts = {'major': '',
                'format': MTBXDatabase.FORMAT_MAGIC_NUMBER,
                'begin': tree.begin,
                'version': tree.version,
                'maintenance': '',
                'minor': '',
                'name': tree.title,
                'description': '',
                'namespace': tree.namespace,
                MTBXDatabase.TOOLBOX_MAGIC_NUMBER: 'True'}
        for key, val in atts.items():
            value_string = "{id}, '{name}', '{value}'".format(
                id=tree.element_id, name=key, value=val)
            sql = """INSERT INTO attributes (%s)
                    VALUES (%s);""" % (column_string, value_string)
            self.db.execute(sql)
        
        self.db.commit()
        
        #Handle children nodes
        for child in tree.children:
            if isinstance(child, ToolNode):
                self._insert_tool(child)
            else:
                self._insert_folder(child)
    
    def _insert_folder(self, node):
        #Insert into the elements table
        column_string = "element_id, parent_id, document_id, tag, text, tail"
        value_string = "{id}, {parent}, 1, '{title}', '', ''".format(
            id=node.element_id, parent=node.parent.element_id, title=node.title)
        sql = """INSERT INTO elements (%s)
                VALUES (%s);""" % (column_string, value_string)
        self.db.execute(sql)
        
        #Insert into the attributes table
        column_string = "element_id, name, value"
        atts = {'namespace': node.namespace,
                'description': '',
                'name': node.title,
                'children': [c.element_id for c in node.children],
                MTBXDatabase.CATEGORY_MAGIC_NUMBER: 'True'}
        for key, val in atts.items():
            value_string = "{id}, '{name}', '{value}'".format(
                id=node.element_id, name=key, value=val)
            sql = """INSERT INTO attributes (%s)
                    VALUES (%s);""" % (column_string, value_string)
            self.db.execute(sql)
            
        self.db.commit()
        
        #Handle children nodes
        for child in node.children:
            if isinstance(child, ToolNode):
                self._insert_tool(child)
            else:
                self._insert_folder(child)
    
    def _insert_tool(self, node):
        #Insert into the elements table
        column_string = "element_id, parent_id, document_id, tag, text, tail"
        value_string = "{id}, {parent}, 1, '{title}', '', ''".format(
            id=node.element_id, parent=node.parent.element_id, title=node.title)
        
        sql = """INSERT INTO elements (%s)
                VALUES (%s);""" % (column_string, value_string)
        self.db.execute(sql)
        
        #Insert into the attributes table
        column_string = "element_id, name, value"
        atts = {'code': node.code,
                'description': '',
                'script': node.script,
                'namespace': node.namespace,
                'python_suffix': node.extension,
                'name': node.title,
                MTBXDatabase.TOOL_MAGIC_NUMBER: 'True'}
        for key, val in atts.items():
            value_string = "{id}, '{name}', '{value!s}'".format(
                id=node.element_id, name=key, value=val)
            sql = """INSERT INTO attributes (%s)
                    VALUES (?, ?, ?);""" % column_string
            self.db.execute(sql, (node.element_id, key, val))
        
        self.db.commit()


def build_toolbox(toolbox_file, source_folder, title, namespace, consolidate):
    print ("------------------------")
    print (" Build Toolbox Utility")
    print ("------------------------")
    print ("")
    print ("toolbox: %s" % toolbox_file)
    print ("source folder: %s" % source_folder)
    print ("title: %s" % title)
    print ("namespace: %s" % namespace)
    print ("")
    
    print ("Loading toolbox structure")
    tree = ElementTree(namespace, title)
    explore_source_folder(source_folder, tree)
    tree.set_toolbox_order()
    print ("Done. Found %s elements." % (tree.next_element_id))
    if consolidate:
        print ("Consolidating code...")
        tree.consolidate()
        print ("Consolidate done")
    
    print ("")
    print ("Building MTBX file...")
    mtbx = MTBXDatabase(toolbox_file, title)
    mtbx.populate_tables_from_tree(tree)
    print ("Build MTBX file done.")


def explore_source_folder(root_folder_path, parent_node):
    folders = []
    files = []
    for item in os.listdir(root_folder_path):
        itempath = os.path.join(root_folder_path, item)
        if os.path.isfile(itempath):
            name, extension = os.path.splitext(item)
            if extension != '.py': 
                continue # skip non-Python files
            if os.path.normpath(itempath) == os.path.normpath(os.path.abspath(__file__)):
                continue # skip this file
            files.append((name, extension))
        else:
            folders.append(item)
    
    for foldername in folders:
        folderpath = os.path.join(root_folder_path, foldername)
        folder_node = parent_node.add_folder(namespace=foldername)
        explore_source_folder(folderpath, folder_node)
    
    for filename, ext in files:
        script_path = os.path.join(root_folder_path, filename + ext)
        parent_node.add_tool(script_path, namespace=filename)    


if __name__ == "__main__":
    '''
    Usage: build_toolbox.py [-p toolbox_path] [-s source_folder] [-l] [-c]
    '''
    
    import argparse
    parser = argparse.ArgumentParser()
    parser.add_argument('-s', '--src', help= "Path to the source code folder. Default is the working folder.")
    parser.add_argument('-p', '--path', help= "Output file path. Default is 'sandag_toolbox.mtbx' in the source code folder.")
    parser.add_argument('-l', '--link', help= "Link the python source files from their current location (instead of consolidate (compile) the toolbox).", action= 'store_true')
    parser.add_argument('-c', '--consolidate', help= "Consolidate (compile) the toolbox (default option).", action= 'store_true')
    
    args = parser.parse_args()

    source_folder = args.src or os.path.dirname(os.path.abspath(__file__))
    folder_name = os.path.split(source_folder)[1]
    toolbox_file = args.path or "sandag_toolbox.mtbx"
    title = "SANDAG toolbox"
    namespace = "sandag"
    consolidate = args.consolidate
    link = args.link
    if consolidate and link:
        raise Exception("-l and -c (--link and --consolidate) are mutually exclusive options")
    if not consolidate and not link:
        consolidate = True  # default if neither is specified
    
    build_toolbox(toolbox_file, source_folder, title, namespace, consolidate)
