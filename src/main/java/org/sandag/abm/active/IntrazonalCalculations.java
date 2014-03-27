package org.sandag.abm.active;

import java.util.Map;

import org.apache.log4j.Logger;

/**
 * The {@code IntrazonalCalculations} class provides convenient default implementations of the {IntrazonalCalculation} interface.
 * 
 */
public class IntrazonalCalculations {
	
	private static final Logger logger = Logger.getLogger(IntrazonalCalculations.class);

	//this class is more of a static factory provider, so constructor is hidden
	private IntrazonalCalculations() {}
	
	/**
	 * The {@code Factorizer} interface provides a framework for transforming an input value. It is essentially a function of one
	 * variable.
	 */
	public static interface Factorizer {
		/**
		 * Factor, or transform, an input value.
		 * 
		 * @param inputValue
		 *        The input value.
		 *        
		 * @return the transformation of {@code inputValue}.
		 */
		double factor(double inputValue);
	}
	
	/**
	 * Get a simple {@code Factorizer} implementation which applies a linear scale and offset. That is, for an input <code>x</code>,
	 * the function will return the following value:
	 * <p>
	 * <code>factor*x + offset</code>
	 * 
	 * @param factor
	 *        The multiplicative factor.
	 *        
	 * @param offset
	 *        The addititive offset.
	 *        
	 * @return the factorizer which will linearly scale and offset an input.
	 */
	public static Factorizer simpleFactorizer(final double factor, final double offset) {
		return new Factorizer() {
			@Override
			public double factor(double inputValue) {
				return inputValue*factor + offset;
			}
		};
	}
	
	/**
	 * Get a {@code Factorizer} which applies a linear transformation, with different scale and offset for positive and negative input
	 * values. That is, for an input <code>x</code>, the factorizer function will return the following value
	 * <p>
	 * <code>factor*x + offset</code>
	 * <p>
	 * where <code>factor</code> and <code>offset</code> may differ according to whether <code>x</code> is positive or negative (if <code>x</code>
	 * is 0, it is considered positive).
	 * 
	 * @param negativeFactor
	 *        The multiplicative factor for negative input values.
	 *        
	 * @param negativeOffset
	 *        The additivie offset for negative input values.
	 *        
	 * @param positiveFactor
	 *        The multiplicative factor for positive input values.
	 *        
	 * @param positiveOffset
	 *        The additivie offset for positive input values.
	 *        
	 * @return the factorizer which will linearly scale and offset an input, using different transoformations based on the input's sign.
	 */
	public static Factorizer positiveNegativeFactorizer(final double negativeFactor, final double negativeOffset,
														final double positiveFactor, final double positiveOffset) {
		return new Factorizer() {
			@Override
			public double factor(double inputValue) {
				if (inputValue < 0)
					return negativeFactor*inputValue + negativeOffset;
				return positiveFactor*inputValue + positiveOffset;
			}
		};
	}
	
	/**
	 * Get an {@code IntrazonalCalculation} which will apply a function to the sum of the largest origin-based logsum values. That is,
	 * an intrazonal value is calculated by a function (defined by a {@code Factorizer}) which acts on the sum of the largest <code>maxCount</code>
	 * logsum values whose origin is the intrazonal's zone, where <code>maxCount</code> is set by the call to this function.
	 * 
	 * @param <N>
	 *        The type of the zone nodes.
	 * 
	 * @param factorizer
	 *        The factorizer used to calculate the intrazonal value.
	 *        
	 * @param maxCount
	 *        The number of logsum values to be used in the intrazonal calculation.
	 *        
	 * @return an intrazonal calculation which will apply {@code factorizer} to the sum of the largest {@code maxCount} logsum values.
	 */
	public static <N extends Node> IntrazonalCalculation<N> maxFactorIntrazonalCalculation(final Factorizer factorizer, final int maxCount) {
		return new IntrazonalCalculation<N>() {
			
			@Override
			public double getIntrazonalValue(N originNode, Map<N,double[]> logsums, int logsumIndex) {
				MinHeap maxValues = new MinHeap(maxCount);
				int initialCount = maxCount;
				double minValue = 0; //will be filled in when needed
				for (N node : logsums.keySet()) {
					if (!node.equals(originNode)) {
						double value = logsums.get(node)[logsumIndex];
						if (initialCount > 0) {
							maxValues.insert(value);
							if (--initialCount == 0)
								minValue = maxValues.getMin();
						} else if (value > minValue) {
							maxValues.removeMin();
							maxValues.insert(value);
							minValue = maxValues.getMin();
						}
					}
				}
				return factorizer.factor(maxValues.getSum());
			}
		};
	}

	
	/**
	 * Get an {@code IntrazonalCalculation} which will apply a function to the sum of the smallest origin-based logsum values. That is,
	 * an intrazonal value is calculated by a function (defined by a {@code Factorizer}) which acts on the sum of the smallest <code>minCount</code>
	 * logsum values whose origin is the intrazonal's zone, where <code>minCount</code> is set by the call to this function.
	 * 
	 * @param <N>
	 *        The type of the zone nodes.
	 *        
	 * @param factorizer
	 *        The factorizer used to calculate the intrazonal value.
	 *        
	 * @param minCount
	 *        The number of logsum values to be used in the intrazonal calculation.
	 *        
	 * @return an intrazonal calculation which will apply {@code factorizer} to the sum of the smallest {@code minCount} logsum values.
	 */
	public static <N extends Node> IntrazonalCalculation<N> minFactorIntrazonalCalculation(final Factorizer factorizer, final int minCount) {
		return new IntrazonalCalculation<N>() {
			
			@Override
			public double getIntrazonalValue(N originNode, Map<N,double[]> logsums, int logsumIndex) {
				MaxHeap minValues = new MaxHeap(minCount);
				int initialCount = minCount;
				double maxValue = 0; //will be filled in when needed
				for (N node : logsums.keySet()) {
					if (!node.equals(originNode)) {
						double value = logsums.get(node)[logsumIndex];
						if (initialCount > 0) {
							minValues.insert(value);
							if (--initialCount == 0)
								maxValue = minValues.getMax();
						} else if (value < maxValue) {
							minValues.removeMax();
							minValues.insert(value);
							maxValue = minValues.getMax();
						}
					}
				}
				return factorizer.factor(minValues.getSum());
			}
		};
	}
	
	
	private static class Heap {
		protected final double[] heap;
		protected int end;
		
		private Heap(int size) {
			heap = new double[size];
			end = 0;
		}
		
		public double getSum() {
			double sum = 0;
			for (int i = 0; i < end; i++)
				sum += heap[i];
			return sum;
		}
	}
	
	private static class MaxHeap extends Heap {
		
		private MaxHeap(int size) {
			super(size);
		}
		
		public void insert(double value) {
			int point = end++;
			if (point == 0) {
				heap[0] = value;
				return;
			}
			while (point > 0) {
				int newPoint = (point-1)/2;
				if (heap[newPoint] < value) {
					heap[point] = heap[newPoint];
					point = newPoint;
				} else {
					heap[point] = value;
					break;
				}
				if (point == 0)
					heap[0] = value;
			}
		}
		
		public double getMax() {
			return heap[0];
		}
		
		public double removeMax() {
			double max = heap[0];
			heap[0] = heap[--end];
			double value = heap[0];
			int point = 0;
			while (true) {
				int left = 2*point+1;
				int right = left+1;
				int largest = point;
				if ((left < end) && (heap[left] > heap[largest]))
					largest = left;
				if ((right < end) && (heap[right] > heap[largest]))
					largest = right;
				if (largest != point) { 
					heap[point] = heap[largest];
					point = largest;
				} else {
					heap[point] = value;
					break;
				}
			}
			return max;
		}
	}
	
	private static class MinHeap extends Heap {
		
		private MinHeap(int size) {
			super(size);
		}
		
		public void insert(double value) {
			int point = end++;
			if (point == 0) {
				heap[0] = value;
				return;
			}
			while (point > 0) {
				int newPoint = (point-1)/2;
				if (heap[newPoint] > value) {
					heap[point] = heap[newPoint];
					point = newPoint;
				} else {
					heap[point] = value;
					break;
				}
				if (point == 0)
					heap[0] = value;
			}
		}
		
		public double getMin() {
			return heap[0];
		}
		
		public double removeMin() {
			double min = heap[0];
			heap[0] = heap[--end];
			double value = heap[0];
			int point = 0;
			while (true) {
				int left = 2*point+1;
				int right = left+1;
				int largest = point;
				if ((left < end) && (heap[left] < heap[largest]))
					largest = left;
				if ((right < end) && (heap[right] < heap[largest]))
					largest = right;
				if (largest != point) { 
					heap[point] = heap[largest];
					point = largest;
				} else {
					heap[point] = value;
					break;
				}
			}
			return min;
		}
	}
}
