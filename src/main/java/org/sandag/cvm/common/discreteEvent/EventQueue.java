package org.sandag.cvm.common.discreteEvent;

import java.util.NoSuchElementException;
import java.util.TreeSet;

public class EventQueue {
    
    //ENHANCEMENT look at using other storage for events.
    // For instance we could have  3600*24 buckets, one for 
    // each second in the day, and then optionally sort the events
    // within the buckets.
    // Then we could use an array or linked list for each bucket?
   TreeSet<TimedEvent> futureEvents;

    public EventQueue() {
        futureEvents = new TreeSet<TimedEvent> ();
    }
    
    public void enqueue(TimedEvent e) {
        futureEvents.add(e);
    }
    
    public TimedEvent popNextEvent() throws NoSuchElementException {
        TimedEvent nextEvent = futureEvents.first();
        futureEvents.remove(nextEvent);
        return nextEvent;
        
    }

    public int size() {
        return futureEvents.size();
    }

}
