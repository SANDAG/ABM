package org.sandag.cvm.common.discreteEvent;

import java.util.NoSuchElementException;

import org.apache.log4j.Logger;

public class EventDispatcher {
    static final Logger logger = Logger.getLogger(EventDispatcher.class);
    EventQueue myQueue = new EventQueue();

    public EventDispatcher() {
        
    }
    
    public void dispatchEvents() {
        try {
            while(true) {
                TimedEvent nextEvent = myQueue.popNextEvent();
                nextEvent.handleEvent(this);
            }
        } catch (NoSuchElementException e) {
           logger.info("Event queue is empty");
        }
    }

    public EventQueue getMyQueue() {
        return myQueue;
    }

    public void setMyQueue(EventQueue myQueue) {
        this.myQueue = myQueue;
    }
}
