##//////////////////////////////////////////////////////////////////////////////
#////                                                                        ///
#//// Copyright INRO, 2019.                                                  ///
#//// Rights to use and modify are granted to the                            ///
#//// San Diego Association of Governments and partner agencies.             ///
#//// This copyright notice must be preserved.                               ///
#////                                                                        ///
#////  utilities/omxwrapper.py                                               ///
#////                                                                        ///
#////                                                                        ///
#////                                                                        ///
#////                                                                        ///
#///////////////////////////////////////////////////////////////////////////////
import inro.modeller as _m
import tables


try:
    import openmatrix as _omx


    def open_file(file_path, mode):
        return OmxMatrix(_omx.open_file(file_path, mode))
    
except Exception as e:
    import omx as _omx
    

    def open_file(file_path, mode):
        return OmxMatrix(_omx.openFile(file_path, mode))

class OmxMatrix(object):

    def __init__(self, matrix):
        self.matrix = matrix

    def mapping(self, name):
        return self.matrix.mapping(name)

    def list_mappings(self):
        return self.matrix.listMappings()

    def __getitem__(self, key):
        return self.matrix[key]

    def __setitem__(self, key, value):
        self.matrix[key] = value

    def create_mapping(self, name, ids):
        exception_raised = False
        try:
            self.matrix.create_mapping(name, ids) # Emme 44 and above
        except Exception as e:
            exception_raised = True
        
        if exception_raised:
            self.matrix.createMapping(name, ids) # Emme 437 
                

    def create_matrix(self, key, obj, chunkshape, attrs):
        exception_raised = False
        try: # Emme 44 and above
            self.matrix.create_matrix(
                key,
                obj=obj,
                chunkshape=chunkshape,
                attrs=attrs
            )
        except Exception as e:
            exception_raised = True
            
        if exception_raised: # Emme 437
            self.matrix.createMatrix(
                key,
                obj=obj,
                chunkshape=chunkshape,
                attrs=attrs
            )

    def close(self):
        self.matrix.close()



class OmxWrapper(_m.Tool()):
    def page(self):
        pb = _m.ToolPageBuilder(
            self,
            runnable=False,
            title="OMX wrapper",
            description="OMX utility for handling of OMX related libraries"
        )
        return pb.render()