package org.sandag.cvm.common.emme2;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.BeforeClass;
import org.junit.Test;

import com.pb.common.matrix.Emme2311MatrixReader;
import com.pb.common.matrix.Matrix;

public class TestEmme2311MatrixReader {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @Test
    public final void testReadMatrix() {
        // TODO set up test data in the subversion project
        File f = new File("C:\\Documents and Settings\\John\\My Documents\\Consult\\Calgary Support\\PeTTs\\Skims\\TestSkims\\HOV-Time.txt");
        Emme2311MatrixReader reader = new Emme2311MatrixReader(f);
        Matrix m = reader.readMatrix();
        assertTrue("1619 to 1610 should be 4.713 but is "+m.getValueAt(1619,1610),m.getValueAt(1619, 1610)>=4.712999999 || m.getValueAt(1619,1610)<=4.7130000001);
        assertTrue("name should be mf22 but is "+m.getName(),m.getName().equals("mf22"));
    }

}
