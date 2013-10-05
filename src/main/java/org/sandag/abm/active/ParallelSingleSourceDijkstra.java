package org.sandag.abm.active;

import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;

import com.pb.sawdust.util.concurrent.DnCRecursiveTask;

public class ParallelSingleSourceDijkstra<N extends Node> implements ShortestPathStrategy<N> {
	private final ShortestPathStrategy<N> sp;
	private final ParallelMethod method;
	
	public ParallelSingleSourceDijkstra(ShortestPathStrategy<N> sp, ParallelMethod method) {
		this.sp = sp;
		this.method = method;
	}
	
	public static enum ParallelMethod {
		FORK_JOIN,
		QUEUE
	}

	@Override
	public ShortestPathResultSet<N> getShortestPaths(Set<N> originNodes, Set<N> destinationNodes, double maxCost) {
		switch (method) {
			case FORK_JOIN : {
				ShortestPathRecursiveTask task = new ShortestPathRecursiveTask(sp,originNodes,destinationNodes,maxCost);
				new ForkJoinPool().execute(task);
				ModifiableShortestPathResultSet<N> sprc = task.getResult();
				return sprc;
			}
			case QUEUE : {
				int threadCount = Runtime.getRuntime().availableProcessors();
				ExecutorService executor = Executors.newFixedThreadPool(threadCount);
				final Queue<ModifiableShortestPathResultSet<N>> sprcQueue = new ConcurrentLinkedQueue<>();
				final Queue<N> originNodeQueue = new ConcurrentLinkedQueue<>(originNodes);
				ThreadLocal<ModifiableShortestPathResultSet<N>> sprcThreadLocal = new ThreadLocal<ModifiableShortestPathResultSet<N>>() {
					@Override
					public ModifiableShortestPathResultSet<N> initialValue() {
						ModifiableShortestPathResultSet<N> sprc = new BasicShortestPathResultSet<>();
						sprcQueue.add(sprc);
						return sprc;
					}
				};
				final CountDownLatch latch = new CountDownLatch(threadCount);
				final AtomicInteger counter = new AtomicInteger();
				for (int i = 0; i < threadCount; i++)
					executor.execute(new QueueMethodTask(sp,originNodeQueue,destinationNodes,maxCost,counter,sprcThreadLocal,latch));
				try {
					latch.await();
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
				executor.shutdown();
				
				ModifiableShortestPathResultSet<N> finalContainer = null;
				for (ModifiableShortestPathResultSet<N> sprc : sprcQueue)
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
	public ShortestPathResultSet<N> getShortestPaths(Set<N> originNodes, Set<N> destinationNodes) {
		return getShortestPaths(originNodes,destinationNodes,Double.POSITIVE_INFINITY);
	}
	
	private class QueueMethodTask implements Runnable {
		private final ShortestPathStrategy<N> sp;
		private final Queue<N> originNodes;
		private final Set<N> destinationNodes;
		private final double maxCost;
		private final AtomicInteger counter;
		private final ThreadLocal<ModifiableShortestPathResultSet<N>> spr;
		private final CountDownLatch latch;
		
		private QueueMethodTask(ShortestPathStrategy<N> sp, Queue<N> originNodes, Set<N> destinationNodes, double maxCost, AtomicInteger counter, ThreadLocal<ModifiableShortestPathResultSet<N>> spr, CountDownLatch latch) {
			this.sp = sp;
			this.destinationNodes = destinationNodes;
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
				ShortestPathResultSet<N> result = sp.getShortestPaths(origins,destinationNodes,maxCost);
				ModifiableShortestPathResultSet<N> sprc = spr.get();
				for (ShortestPathResult<N> spResult : result.getResults()) 
					sprc.addResult(spResult);
				int c = counter.addAndGet(origins.size()); 
				if (c % segmentSize < origins.size())
					System.out.println("   done with " + ((c / segmentSize)*segmentSize) + " origins");
				origins.clear();
			}
			latch.countDown();
		}
	}
	
	
	private class ShortestPathRecursiveTask extends DnCRecursiveTask<ModifiableShortestPathResultSet<N>> {
		AtomicInteger counter;
		private final ShortestPathStrategy<N> sp;
		private final Set<N> destinations;
		private final Node[] origins;
		private final double maxCost;

		protected ShortestPathRecursiveTask(ShortestPathStrategy<N> sp, Set<N> origins, Set<N> destinations, double maxCost) {
			super(0,origins.size());
			this.sp = sp;
			this.origins = origins.toArray(new Node[origins.size()]);
			this.destinations = destinations;
			this.maxCost = maxCost;
			counter = new AtomicInteger(0);
		}

		protected ShortestPathRecursiveTask(long start, long length, DnCRecursiveTask<ModifiableShortestPathResultSet<N>> next,
				                            ShortestPathStrategy<N> sp, Node[] origins, Set<N> destinations, double maxCost, AtomicInteger counter) {
			super(start,length,next);
			this.sp = sp;
			this.origins = origins;
			this.destinations = destinations;
			this.maxCost = maxCost;
			this.counter = counter;
		}

		@Override
		@SuppressWarnings("unchecked") //origins only hold N, we just can't declare as such because of generics
		protected ModifiableShortestPathResultSet<N> computeTask(long start, long length) {
			Set<N> originNodes = new HashSet<>();
			int end = (int) (start + length);
			for (int n = (int) start; n < end; n++) 
				originNodes.add((N) origins[n]);
			ShortestPathResultSet<N> result = sp.getShortestPaths(originNodes,destinations);
			ModifiableShortestPathResultSet<N> spr = new BasicShortestPathResultSet<>();
			for (ShortestPathResult<N> spResult : result.getResults()) 
				spr.addResult(spResult);
			
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
		protected DnCRecursiveTask<ModifiableShortestPathResultSet<N>> getNextTask(long start, long length, DnCRecursiveTask<ModifiableShortestPathResultSet<N>> next) {
			return new ShortestPathRecursiveTask(start,length,next,sp,origins,destinations,maxCost,counter);
		}

		@Override
		protected ModifiableShortestPathResultSet<N> joinResults(ModifiableShortestPathResultSet<N> spr1, ModifiableShortestPathResultSet<N> spr2) {
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
