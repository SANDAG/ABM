package org.sandag.cvm.common.discreteEvent;

import org.apache.log4j.Logger;

public abstract class TimedEvent implements Comparable {
    
    final public float myTime;
    private double myRandomNumber = 0;
    static final Logger logger = Logger.getLogger(TimedEvent.class);

    public TimedEvent(float myTime) {
        this.myTime = myTime;
    }
    
    public TimedEvent(double myTime) {
        this.myTime = (float) myTime;
    }
    
    boolean processEvenIfAfterSimulationTime=false;
    
    
    public abstract void handleEvent(EventDispatcher dispatch);

    public int compareTo(Object arg0) {
        if (myTime > ((TimedEvent)arg0).myTime) {
            return 2;
        }
        if (myTime < ((TimedEvent)arg0).myTime) {
            return -2;
        }
        if (arg0==this) return 0;
        // ok, our times are identical!
        if (myRandomNumber ==0) myRandomNumber = (Math.random()+.001);
        TimedEvent other = (TimedEvent) arg0;
        if (other.myRandomNumber == 0) other.myRandomNumber = (Math.random()+.001);
        if (myRandomNumber > other.myRandomNumber) return 1;
        if (myRandomNumber < other.myRandomNumber) return -1;
        //TODO maybe do something smarter?
        logger.error("randomly generated event sortings are the same");
        return 0;
    }

    @Override
    public String toString() {
        return "Event at "+myTime;
    }

    public boolean isProcessEvenIfAfterSimulationTime() {
        return processEvenIfAfterSimulationTime;
    }

    public void setProcessEvenIfAfterSimulationTime(
            boolean processEvenIfAfterSimulationTime) {
        this.processEvenIfAfterSimulationTime = processEvenIfAfterSimulationTime;
    }

}
