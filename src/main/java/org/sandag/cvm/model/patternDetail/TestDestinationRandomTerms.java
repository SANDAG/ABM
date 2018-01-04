/*
 * Created on Oct 23, 2007
 *
 * Copyright  2007 HBA Specto Incorporated
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.sandag.cvm.model.patternDetail;

import static org.junit.Assert.*;

import org.junit.Test;


/**
 * @author John Abraham
 *
 */
public class TestDestinationRandomTerms {


	
	/**
     * Test method for {@link org.sandag.cvm.model.patternDetail.DestinationRandomTerms#getExtremeNormal(int, int, int, int, double)}.
     */
    @Test
    public final void testGetExtremeNormal() {
        
        DestinationRandomTerms sampler = new DestinationRandomTerms();
        
        // check consistency.
        for (int i=0;i<50;i++) {
            int fakeZone = (int) (Math.random()*5000);
            int offSet = (int) (Math.random()*10000);
            int n = (int) (Math.random()*1000);
            int skip = (int) (Math.random()*10);
            double sample1 = sampler.getExtremeNormal(fakeZone, offSet, skip, n, 1);
            double sample2 = sampler.getExtremeNormal(fakeZone, offSet, skip, n, 1);
            assertTrue("Asking for the same extreme normal twice should give same value",sample1==sample2);
            double sample3 = sampler.getExtremeNormal(fakeZone, offSet, skip, n, 2);
            assertTrue ("Asking for twice the std dev should give twice as much", sample3/sample2 < 2.0001 && sample3/sample2 > 1.9999);
        }
        // let's check for n=20, see if we get expected within a certain tolerance
        // these are the expected histograms from another experiment, with 4777 maximum random draws
        // they are 0.1 apart and the first upper bound is 0.2
        int[] expectedHistograms = new int[]{
                0,
                0,
                1,
                0,
                5,
                13,
                26,
                38,
                74,
                91,
                155,
                217,
                254,
                327,
                355,
                350,
                370,
                394,
                330,
                330,
                305,
                229,
                205,
                163,
                116,
                114,
                97,
                61,
                47,
                31,
                17,
                22,
                15,
                3,
                10,
                2,
                3,
                4,
                1,
                0,
                0,
                0,
                0,
                0,
                2,
                0,
                0,
                0,
                0};
        int offSet = (int) (Math.random()*10000);
        int skip = (int) (Math.random()*10);
        for (int i=0;i<4777;i++) {
            int fakeZone = (int) (Math.random()*15000);
            double sample1 = sampler.getExtremeNormal(fakeZone, offSet, skip, 20, 1);
            int bin = (int) ((sample1-0.1)/0.1);
            if (bin <0) bin =0;
            if (bin >= expectedHistograms.length) bin = expectedHistograms.length-1;
            expectedHistograms[bin]--;
        }
        // now check to see if maximum diff < 70;
        int maximumDiff = 0;
        for (int i=0;i<expectedHistograms.length;i++) {
            maximumDiff = Math.max(maximumDiff,Math.abs(expectedHistograms[i]));
        }
        assertTrue("Each bin in ExtremeNormal(20) should be within tolerance",maximumDiff<120);

        // let's check for n=2000, see if we get expected within a certain tolerance
        // these are the expected histograms from another experiment, with 4777 maximum random draws
        // they are 0.1 apart and the first upper bound is 1.2
        expectedHistograms = new int[]{
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                5,
                32,
                82,
                191,
                371,
                553,
                594,
                632,
                575,
                461,
                372,
                272,
                224,
                147,
                86,
                62,
                42,
                25,
                14,
                17,
                8,
                4,
                5,
                1,
                1,
                1,
                0,
                0,
                0,
                0,
                0,
                0,
                };
        offSet = (int) (Math.random()*10000);
        skip = (int) (Math.random()*10);
        for (int i=0;i<4777;i++) {
            int fakeZone = (int) (Math.random()*15000);
            double sample1 = sampler.getExtremeNormal(fakeZone, offSet, skip, 2000, 1);
            int bin = (int) ((sample1-1.1)/0.1);
            if (bin <0) bin =0;
            if (bin >= expectedHistograms.length) bin = expectedHistograms.length-1;
            expectedHistograms[bin]--;
        }
        // now check to see if maximum diff < 100;
        maximumDiff = 0;
        for (int i=0;i<expectedHistograms.length;i++) {
            maximumDiff = Math.max(maximumDiff,Math.abs(expectedHistograms[i]));
        }
        assertTrue("Each bin in ExtremeNormal(2000) should be within tolerance",maximumDiff<150);


        // let's check for n=2000, see if we get expected within a certain tolerance
        // these are the expected histograms from the Hall distribution.
        // they are 0.1 apart and the first upper bound is 1.2
        expectedHistograms = new int[]{
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                2,
                17,
                72,
                188,
                343,
                484,
                565,
                576,
                532,
                458,
                374,
                295,
                226,
                170,
                127,
                93,
                68,
                49,
                36,
                26,
                18,
                13,
                9,
                7,
                5,
                3,
                2,
                1,
                1,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0
        };
        offSet = (int) (Math.random()*10000);
        skip = (int) (Math.random()*10);
        for (int i=0;i<4777;i++) {
            int fakeZone = (int) (Math.random()*15000);
            double sample1 = sampler.getExtremeNormal(fakeZone, offSet, skip, 2000, 1);
            int bin = (int) ((sample1-1.1)/0.1);
            if (bin <0) bin =0;
            if (bin >= expectedHistograms.length) bin = expectedHistograms.length-1;
            expectedHistograms[bin]--;
        }
        // now check to see if maximum diff < 5;
        maximumDiff = 0;
        for (int i=0;i<expectedHistograms.length;i++) {
            maximumDiff = Math.max(maximumDiff,Math.abs(expectedHistograms[i]));
        }

        assertTrue("Each bin in ExtremeNormal(2000) should be within tolerance",maximumDiff<100);
    }
}
