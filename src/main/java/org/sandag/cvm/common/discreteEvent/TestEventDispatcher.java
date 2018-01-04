package org.sandag.cvm.common.discreteEvent;

import org.junit.BeforeClass;
import org.junit.Test;

public class TestEventDispatcher {

    class RequeueEvent extends TimedEvent {

        public RequeueEvent(double myTime) {
            super(myTime);
        }

        @Override
        public void handleEvent(EventDispatcher dispatch) {
            numOfEvents++;
            if (numOfEvents + averageQueueSize < numberOfEventsToSimulate) {
                double selector = (int) (Math.random()*3);
                // queue up 0, 1 or 2 new events.
                for (int i=0;i<selector;i++) { 
                    RequeueEvent yetAnother = new RequeueEvent(myTime
                        + Math.random());
                    queue.enqueue(yetAnother);
                }
            }
            if (numOfEvents % logFrequency == 0) {
                // System.out.print(" "+numOfEvents);
                logger.info("Did " + numOfEvents + " events, queue size is "
                        + queue.size() + ", time is " + myTime);

            }
        }

    }

    static final int averageQueueSize = 1000000;

    static final int logFrequency = 20000;

    static final int numberOfEventsToSimulate = 8000000;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    private EventDispatcher dispatch;

    int numOfEvents = 0;

    private EventQueue queue;

    @Test
    public final void testDispatchEvents() {
        dispatch = new EventDispatcher();
        queue = dispatch.myQueue;
        // add 1000 events to dispatcher;
        for (int i = 0; i < averageQueueSize; i++) {
            RequeueEvent anEvent = new RequeueEvent(Math.random());
            queue.enqueue(anEvent);
        }
        dispatch.dispatchEvents();

    }

}
