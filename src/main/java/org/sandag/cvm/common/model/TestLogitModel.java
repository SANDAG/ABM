/*
 Travel Model Microsimulation library
 Copyright (C) 2005 PbConsult, JE Abraham and others


  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.

*/




package org.sandag.cvm.common.model;

public class TestLogitModel {
    public static void main(String[] args) throws ChoiceModelOverflowException {
      try {
        LogitModel lm = new LogitModel();
        Alternative a = new FixedUtilityAlternative(1.0);
        Alternative b = new FixedUtilityAlternative(2.0);
        lm.addAlternative(a);
        lm.addAlternative(b);
        for (double dp=.001;dp<100000 ;dp*=2 )
        {
            lm.setDispersionParameter(dp);
             int acount=0;
             int bcount=0;
             for (int i=0;i<1000 ;i++ )
             {
                     if (lm.monteCarloChoice()==a) acount++; else bcount++;
             }
             System.out.println("DispersionParameter="+dp+" acount="+acount+" bcount"+bcount);
        }
           lm=new LogitModel();
           lm.addAlternative(new FixedUtilityAlternative(Math.exp(50000)));
           System.out.println("Composite utility of one infinite utility alternative is "+lm.getUtility());
           lm.monteCarloChoice();
           lm=new LogitModel();
           lm.addAlternative(new FixedUtilityAlternative(Math.log(0)));
           lm.addAlternative(new FixedUtilityAlternative(5.0));

           System.out.println("Composite utility of one negative infinite utility alternative and a '5' alt is "+lm.getUtility());
           lm.monteCarloChoice();

           lm.addAlternative(new FixedUtilityAlternative(Math.log(0)));
           System.out.println("Composite utility of one negative infinite utility alternative and a '5' alt and an alternative with zero size is "+lm.getUtility());
           lm.monteCarloChoice();
        } catch (NoAlternativeAvailable e) {
          System.out.println("No alternative available somewhere here...");
        }

    }
}
