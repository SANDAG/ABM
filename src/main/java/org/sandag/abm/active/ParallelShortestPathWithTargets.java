package org.sandag.abm.active;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import com.pb.sawdust.util.concurrent.DnCRecursiveTask;

public class ParallelShortestPathWithTargets<N extends Node> implements ShortestPathWithTargets<N> {
	private final ShortestPath<N> sp;
	private final ParallelMethod method;
	
	public ParallelShortestPathWithTargets(ShortestPath<N> sp, ParallelMethod method) {
		this.sp = sp;
		this.method = method;
	}
	
	public static enum ParallelMethod {
		FORK_JOIN,
		QUEUE
	}

	@Override
	public ShortestPathResults<N> getShortestPaths(Map<N,HashSet<N>> originDestinationMap, double maxCost) {
		switch (method) {
			case FORK_JOIN : {
				ShortestPathRecursiveTask task = new ShortestPathRecursiveTask(sp,originDestinationMap,maxCost);
				new ForkJoinPool().execute(task);
				ShortestPathResultsContainer<N> sprc = task.getResult();
				return sprc;
			}
			case QUEUE : {
				int threadCount = Runtime.getRuntime().availableProcessors();
				ExecutorService executor = Executors.newFixedThreadPool(threadCount);
				Set<N> originNodes = originDestinationMap.keySet();
				final Queue<ShortestPathResultsContainer<N>> sprcQueue = new ConcurrentLinkedQueue<>();
				final Queue<N> originNodeQueue = new ConcurrentLinkedQueue<>(originNodes);
				ThreadLocal<ShortestPathResultsContainer<N>> sprcThreadLocal = new ThreadLocal<ShortestPathResultsContainer<N>>() {
					@Override
					public ShortestPathResultsContainer<N> initialValue() {
						ShortestPathResultsContainer<N> sprc = new BasicShortestPathResults<>();
						sprcQueue.add(sprc);
						return sprc;
					}
				};
				final CountDownLatch latch = new CountDownLatch(threadCount);
				final AtomicInteger counter = new AtomicInteger();
				for (int i = 0; i < threadCount; i++)
					executor.execute(new QueueMethodTask(sp,originNodeQueue,originDestinationMap,maxCost,counter,sprcThreadLocal,latch));
				try {
					latch.await();
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
				executor.shutdown();
				
				ShortestPathResultsContainer<N> finalContainer = null;
				for (ShortestPathResultsContainer<N> sprc : sprcQueue)
					if (finalContainer == null)
						finalContainer = sprc;
					else
						finalContainer.addAll(sprc);
				
				return finalContainer;
			}
			default : throw new IllegalStateException("Should not be here.");
		}
	}

	@Override
	public ShortestPathResults<N> getShortestPaths(Map<N,HashSet<N>> originDestinationMap) {
		return getShortestPaths(originDestinationMap,Double.POSITIVE_INFINITY);
	}
	
	private class QueueMethodTask implements Runnable {
		private final ShortestPath<N> sp;
		private final Queue<N> originNodes;
		private final Map<N, HashSet<N>> originDestinationMap;
		private final double maxCost;
		private final AtomicInteger counter;
		private final ThreadLocal<ShortestPathResultsContainer<N>> spr;
		private final CountDownLatch latch;
		
		private QueueMethodTask(ShortestPath<N> sp, Queue<N> originNodes, Map<N, HashSet<N>> originDestinationMap, double maxCost, AtomicInteger counter, ThreadLocal<ShortestPathResultsContainer<N>> spr, CountDownLatch latch) {
			this.sp = sp;
			this.originDestinationMap = originDestinationMap;
			this.originNodes = originNodes;
			this.maxCost = maxCost;
			this.counter = counter;
			this.spr = spr;
			this.latch = latch;
		}

		@Override
		public void run() {
			int segmentSize = 5;
			final Set<N> origins = new HashSet<>();
			while (originNodes.size() > 0) {
				while ((originNodes.size() > 0) && (origins.size() < segmentSize)) {
					N origin = originNodes.poll();
					if (origin != null)
						origins.add(origin);
				}
				if (origins.size() == 0)
					break;
				ShortestPathResultsContainer<N> sprc = spr.get();
				for (N currentOriginNode : origins) {
				    Set<N> singleOriginSet = new HashSet<N>();
				    singleOriginSet.add(currentOriginNode);
				    ShortestPathResults<N> result = sp.getShortestPaths(origins,originDestinationMap.get(currentOriginNode),maxCost);
				    for (ShortestPathResult<N> spResult : result.getResults()) 
				        sprc.addResult(spResult);
				}
				
				int c = counter.addAndGet(origins.size()); 
				if (c % segmentSize < origins.size())
					System.out.println("   done with " + ((c / segmentSize)*segmentSize) + " origins");
				origins.clear();
			}
			latch.countDown();
		}
	}
	
	
	private class ShortestPathRecursiveTask extends DnCRecursiveTask<ShortestPathResultsContainer<N>> {
		AtomicInteger counter;
		private final ShortestPath<N> sp;
		private final Map<N,HashSet<N>> originDestinationMap;
		private final Node[] origins;
		private final double maxCost;

		protected ShortestPathRecursiveTask(ShortestPath<N> sp, Map<N,HashSet<N>> originDestinationMap, double maxCost) {
		    super(0,originDestinationMap.keySet().size());			
		    this.sp = sp;
			this.origins = originDestinationMap.keySet().toArray(new Node[originDestinationMap.keySet().size()]);
			this.originDestinationMap = originDestinationMap;
			this.maxCost = maxCost;
			counter = new AtomicInteger(0);
		}

		protected ShortestPathRecursiveTask(long start, long length, DnCRecursiveTask<ShortestPathResultsContainer<N>> next,
				                            ShortestPath<N> sp, Node[] origins, Map<N,HashSet<N>> originDestinationMap, double maxCost, AtomicInteger counter) {
			super(start,length,next);
			this.sp = sp;
			this.origins = origins;
			this.originDestinationMap = originDestinationMap;
			this.maxCost = maxCost;
			this.counter = counter;
		}

		@Override
		@SuppressWarnings("unchecked") //origins only hold N, we just can't declare as such because of generics
		protected ShortestPathResultsContainer<N> computeTask(long start, long length) {
			Set<N> originSubset = new HashSet<>();
			Set<N> destinationSubset = new HashSet<>();
			Map<N, HashSet<N>> subsetMap = new HashMap<N,HashSet<N>>();
			ShortestPathResultsContainer<N> spr = new BasicShortestPathResults<>();
			int end = (int) (start + length);
			for (int n = (int) start; n < end; n++) {
				originSubset.clear();
				originSubset.add((N) origins[n]);
				destinationSubset.clear();
				subsetMap.put((N) origins[n], originDestinationMap.get(origins[n]));
			
				ShortestPathResults<N> result = sp.getShortestPaths(originSubset,destinationSubset);
			
				for (ShortestPathResult<N> spResult : result.getResults()) 
				    spr.addResult(spResult);
		    }
			
			int c = counter.addAndGet((int) length); 
			if (c % 10 < length)
				System.out.println("   done with " + ((c / 10)*10) + " origins");
			return spr;
		}

		@Override
		protected boolean continueDividing(long newLength) {
			return (newLength > 5) && (getSurplusQueuedTaskCount() < 3);
		}

		@Override
		protected DnCRecursiveTask<ShortestPathResultsContainer<N>> getNextTask(long start, long length, DnCRecursiveTask<ShortestPathResultsContainer<N>> next) {
			return new ShortestPathRecursiveTask(start,length,next,sp,origins,originDestinationMap,maxCost,counter);
		}

		@Override
		protected ShortestPathResultsContainer<N> joinResults(ShortestPathResultsContainer<N> spr1, ShortestPathResultsContainer<N> spr2) {
			if (spr1.size() > spr2.size()) {
				spr1.addAll(spr2);
				return spr1;
			} else {
				spr2.addAll(spr1);
				return spr2;
			}
		}
	}
	
	

}
