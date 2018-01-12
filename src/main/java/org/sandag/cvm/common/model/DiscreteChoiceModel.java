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

import java.util.Random;

public abstract class DiscreteChoiceModel implements DiscreteChoiceModelInterface {
    /** Picks one of the alternatives based on the logit model probabilities 
     * @throws ChoiceModelOverflowException */
    public abstract Alternative monteCarloChoice() throws NoAlternativeAvailable, ChoiceModelOverflowException ;

    /** Picks one of the alternatives based on the logit model probabilities and random number given*/
    public abstract Alternative monteCarloChoice(double r) throws NoAlternativeAvailable ;

    public Alternative monteCarloElementalChoice() throws NoAlternativeAvailable, ChoiceModelOverflowException {
        Alternative a = monteCarloChoice();
        while (a instanceof DiscreteChoiceModel) {
            a = ((DiscreteChoiceModel) a).monteCarloChoice();
        }
        return a;
    }
    /** Use this method if you want to give a random number */
    public Alternative monteCarloElementalChoice(double r) throws NoAlternativeAvailable {
        Alternative a = monteCarloChoice(r );
        Random newRandom = new Random(new Double(r*1000).longValue());
        while (a instanceof DiscreteChoiceModel) {
            a = ((DiscreteChoiceModel) a).monteCarloChoice(newRandom.nextDouble());
        }
        return a;
    }

    /** @param a the alternative to add into the choice set */
    public abstract void addAlternative(Alternative a);

     public abstract Alternative alternativeAt(int i);

    public abstract double[] getChoiceProbabilities();

    abstract public void allocateQuantity(double amount);
    

}

