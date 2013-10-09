package org.sandag.abm.active;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;


public class PathAlternativeListWriter <N extends Node, E extends Edge<N>>
{
    private FileWriter pathWriter;
    private FileWriter linkWriter;

    public PathAlternativeListWriter(String pathFileName, String linkFileName) throws IOException
    {
        pathWriter = new FileWriter(new File(pathFileName));
        linkWriter = new FileWriter(new File(linkFileName));
    }
    
    public void writeHeaders() throws IOException
    {
        pathWriter.write("origNode,destNode,index,length,size\n");
        linkWriter.write("origNode,destNode,index,link,fromNode,toNode\n");
    }
    
    public void write(PathAlternativeList<N,E> alternativeList) throws IOException
    {
        Path<N> path;
        int index = 0;
        for (int i=0; i<alternativeList.getCount(); i++) {
            path = alternativeList.get(i);
            pathWriter.write(path.getNode(0).getId() + "," + path.getNode(path.getLength()-1).getId() + "," + index + "," + path.getLength() + "," + alternativeList.getSizeMeasures().get(i) + "\n");
            N previous = null;
            int j=0;
            for ( N node : path ) {
                if ( previous != null ) {
                    linkWriter.write(path.getNode(0).getId() + "," + path.getNode(path.getLength()-1).getId() + "," + index + "," + j + "," + previous.getId() + "," + node.getId() + "\n");
                }
                previous = node;
                j++;
            }
            index ++;
        }
    }
    
    public void close() throws IOException
    {
        pathWriter.flush();
        pathWriter.close();
        linkWriter.flush();
        linkWriter.close();
    }
}
