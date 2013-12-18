package org.sandag.abm.ctramp;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.sandag.abm.modechoice.MgraDataManager;

/**
 * The {@code BikeLogsum} class holds bike logsums for use in the SANDAG model. This class is intended to be used as a singleton, and so the only way to
 * access it is via the {@code getBikeLogsum} static method. It is constructed on demand and safe for concurrent access. 
 * <p>
 * Internally, the logsums are held in a mapping using node-pairs as keys. The taz and mgra pairs are held in the same mapping, with the tazs multiplied
 * by -1 to avoid conflicts. To ensure good performance when building the object, a good guess as to the number of node pairs (maz pairs plus taz pairs)
 * can be provided (a default value of 26 million will be used otherwise) via the {@code BIKE_LOGSUM_NODE_PAIR_COUNT_PROPERTY} property.
 */
public class BikeLogsum implements SegmentedSparseMatrix<BikeLogsumSegment> {
    private Logger logger = Logger.getLogger(BikeLogsum.class);
	
	public static final String BIKE_LOGSUM_OUTPUT_PROPERTY = "active.output.bike";
	public static final String BIKE_LOGSUM_MGRA_FILE_PROPERTY = "active.logsum.matrix.file.bike.mgra";
	public static final String BIKE_LOGSUM_TAZ_FILE_PROPERTY = "active.logsum.matrix.file.bike.taz";
	public static final String BIKE_LOGSUM_NODE_PAIR_COUNT_PROPERTY = "active.logsum.matrix.node.pair.count";
	/**
	 * The default logsum node pair count.
	 */
	public static final int DEFAULT_BIKE_LOGSUM_NODE_PAIR_COUNT = 26_000_000; //testing found 18_880_631, so this should be good enough to start
																  
	
	private final Map<MatrixLookup,double[]> logsum;
	private final int[] mgraIndex;
	
	private static volatile BikeLogsum instance = null;
	
	/**
	 * Get the {@code BikeLogsum} instance.
	 * 
	 * @param rbMap
	 *        The model property mapping.
	 *        
	 * @return the {@code BikeLogsum} instance.
	 */
	public static BikeLogsum getBikeLogsum(Map<String,String> rbMap) {
		if (instance == null) {
			synchronized (BikeLogsum.class) {
				if (instance == null) { //check again to see if we waited for another thread to do the initialization already
					int nodePairCount = rbMap.containsKey(BIKE_LOGSUM_NODE_PAIR_COUNT_PROPERTY) ? 
							Integer.parseInt(rbMap.get(BIKE_LOGSUM_NODE_PAIR_COUNT_PROPERTY)) : DEFAULT_BIKE_LOGSUM_NODE_PAIR_COUNT;
				    String mgraFile = Paths.get(rbMap.get(CtrampApplication.PROPERTIES_PROJECT_DIRECTORY),rbMap.get(MgraDataManager.PROPERTIES_MGRA_DATA_FILE)).toString();
				    String tazLogsumFile = Paths.get(rbMap.get(BIKE_LOGSUM_OUTPUT_PROPERTY),rbMap.get(BIKE_LOGSUM_TAZ_FILE_PROPERTY)).toString();
				    String mgraLogsumFile = Paths.get(rbMap.get(BIKE_LOGSUM_OUTPUT_PROPERTY),rbMap.get(BIKE_LOGSUM_MGRA_FILE_PROPERTY)).toString();
					instance = new BikeLogsum(tazLogsumFile,mgraLogsumFile,nodePairCount,mgraFile);
				}
			}
		}
		return instance;
	}
	
	private BikeLogsum(String tazLogsumFile, String mgraLogsumFile, int nodePairCount, String mgraFile) {
		logsum = new HashMap<BikeLogsum.MatrixLookup, double[]>(nodePairCount,1.01f); //capacity of nodepairs, plus a little buffer just in case
		Map<Integer,Set<Integer>> tazMgraMapping = loadTazMgraMapping(mgraFile);
		mgraIndex = buildMgraIndex(tazMgraMapping);
		loadLogsum(tazLogsumFile,true);
		loadLogsum(mgraLogsumFile,false);
	}
	
	private int[] buildMgraIndex(Map<Integer,Set<Integer>> tazMgraMapping) {
		int maxMgra = 0;
		for (Set<Integer> mgras : tazMgraMapping.values())
			for (int mgra : mgras)
				if (maxMgra < mgra)
					maxMgra = mgra;
		
		int[] mgraIndex = new int[maxMgra+1];
		for (int taz : tazMgraMapping.keySet())
			for (int mgra : tazMgraMapping.get(taz))
				mgraIndex[mgra] = -1*taz;
		
		return mgraIndex;
	}
	
	private Map<Integer,Set<Integer>> loadTazMgraMapping(String mgraFile) {
		Map<Integer,Set<Integer>> tazMgraMapping = new HashMap<>();
		boolean first = true;
		String mgraColumnName = MgraDataManager.MGRA_FIELD_NAME.toLowerCase();
		String tazColumnName = MgraDataManager.MGRA_TAZ_FIELD_NAME.toLowerCase();
		int mgraColumn = -1;
		int tazColumn = -1;
		try (BufferedReader reader = new BufferedReader(new FileReader(mgraFile))) {
			String line;
			while ((line = reader.readLine()) != null) {
				String[] lineData = line.trim().split(",");
				if (first) {
					for (int i = 0; i < lineData.length; i++) {
						String column = lineData[i].toLowerCase();
						if (column.equals(mgraColumnName))
							mgraColumn = i;
						if (column.equals(tazColumnName))
							tazColumn = i;
					}
					first = false;
					continue;
				}
				if (lineData.length < 2)
					continue;
				int mgra = Integer.parseInt(lineData[mgraColumn]);
				int taz = Integer.parseInt(lineData[tazColumn]);
				if (!tazMgraMapping.containsKey(taz))
					tazMgraMapping.put(taz,new HashSet<Integer>());
				tazMgraMapping.get(taz).add(mgra);
			}
			
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return tazMgraMapping;
	}
	
	private void loadLogsum(String logsumFile, boolean taz) {
		logger.info("Processing bike logsum from " + logsumFile);
		int counter = 0;
		long startTime = System.currentTimeMillis();
		
		int segmentWidth = BikeLogsumSegment.segmentWidth();
		try (BufferedReader reader = new BufferedReader(new FileReader(logsumFile))) {
			int[] segmentIndex = new int[segmentWidth];
			boolean first = true;

			String line;
			while ((line = reader.readLine()) != null) {
				String[] lineData = line.trim().split(",");
				for (int i = 0; i < lineData.length; i++)
					lineData[i] = lineData[i].trim();
				if (first) {
					for (int i = 2; i < (2 + segmentWidth); i++) { //first two are for row and column
						String columnName = lineData[i].toLowerCase();
						boolean isFemale = columnName.contains("female");
						boolean mandatory = columnName.contains("mandatory");
						boolean inbound = columnName.contains("inbound");
						segmentIndex[new BikeLogsumSegment(isFemale,mandatory,inbound).getSegmentId()] = i;
					}
					first = false;
					continue;
				}
				if (++counter % 100_000 == 0)
					logger.debug("Finished processing " + counter + " node pairs (logsum lookup size: " + logsum.size() + ")");
				double[] logsumData = new double[segmentWidth];
				for (int i = 0; i < logsumData.length; i++)
					logsumData[i] = Double.parseDouble(lineData[segmentIndex[i]]);

				int fromZone = Integer.parseInt(lineData[0]);
				int toZone = Integer.parseInt(lineData[1]);
				int indexFactor = taz ? -1 : 1;
				MatrixLookup ml = new MatrixLookup(indexFactor*Integer.parseInt(lineData[0]),indexFactor*Integer.parseInt(lineData[1]));
			    logsum.put(ml,logsumData);
			    	
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		logger.info("Finished processing " + counter + " node pairs (logsum lookup size: " + logsum.size() + ") in " + ((System.currentTimeMillis() - startTime) / 60000.0) + " minutes");
	}
	
	private double[] getLogsums(int rowId, int columnId) {
		double[] logsums = logsum.get(new MatrixLookup(rowId,columnId));
		if (logsums == null)
			logsums = logsum.get(new MatrixLookup(mgraIndex[rowId],mgraIndex[columnId]));
		return logsums;
	}

	@Override
	public double getValue(BikeLogsumSegment segment, int rowId, int columnId) {
		double[] logsums = getLogsums(rowId,columnId);
		return logsums == null ? -999 : logsums[segment.getSegmentId()];
	}
	
	/**
	 * Get the logsum value when full segmentation is not possible. This method collects all of the logsums corresponding to the provided segments, 
	 * and then averages them.
	 * 
	 * @param rowId
	 *        The row id.
	 *        
	 * @param columnId
	 *        The column id.
	 *        
	 * @param segments
	 *        The segments to get the logsum for. 
	 * 
	 * @return the matrix value at <code>(rowId,columnId)</code>, averaged across all of the segments specified by {@code segments}.
	 */
	public double getMultiSegmentLogsum(int rowId, int columnId, BikeLogsumSegment ... segments) {
		double[] logsums = getLogsums(rowId,columnId);
		double logsum = 0.0;
		for (BikeLogsumSegment segment : segments)
			logsum += logsums[segment.getSegmentId()];
		return logsum / segments.length;
	}
	
	private class MatrixLookup {
		private final int row;
		private final int column;
		
		private MatrixLookup(int row, int column) {
			this.row = row;
			this.column = column;
		}
		
		public boolean equals(Object o) {
			if ((o == null) || (!(o instanceof MatrixLookup)))
				return false;
			MatrixLookup ml = (MatrixLookup) o;
			return (row == ml.row) && (column == ml.column); 
		}
		
		public int hashCode() {
			return row + 37*column;
		}
	}
	
//	public static void main(String ... args) {
//		Map<String,String> testRb = new HashMap<>();
//		
//		long time = System.currentTimeMillis();
//		testRb.put(BIKE_LOGSUM_OUTPUT_PROPERTY,"D:/projects/sandag/output_test");
//		testRb.put(BIKE_LOGSUM_MGRA_FILE_PROPERTY,"bikeMgraLogsum.csv");
//		testRb.put(BIKE_LOGSUM_TAZ_FILE_PROPERTY,"bikeTazLogsum.csv");
//		//testRb.put(BIKE_LOGSUM_NODE_PAIR_COUNT_PROPERTY,"100000000");
//		testRb.put(CtrampApplication.PROPERTIES_PROJECT_DIRECTORY,"D:/projects/sandag/abm_reporting/abm_shell");
//		testRb.put(MgraDataManager.PROPERTIES_MGRA_DATA_FILE,"input/mgra12_based_input08_rev.csv");
//		BikeLogsum logsum = BikeLogsum.getBikeLogsum(testRb);
//		System.out.println("total minutes to load: " + ((System.currentTimeMillis() - time) / 60000.0));
//		
//		int origin = 3668;
//		int destination = 9707;
//		boolean[] bs = {true,false};
//		for (boolean mandatory : bs) {
//			for (boolean female : bs) {
//				for (boolean inbound : bs) {
//					BikeLogsumSegment bls = new BikeLogsumSegment(female,mandatory,inbound);
//					System.out.println("origin " + origin + ", destination " + destination + ", " + bls + ": " + logsum.getValue(bls,origin,destination));
//				}
//			}
//		}
//		origin = 10649;
//		destination = 10291;
//		for (boolean mandatory : bs) {
//			for (boolean female : bs) {
//				for (boolean inbound : bs) {
//					BikeLogsumSegment bls = new BikeLogsumSegment(female,mandatory,inbound);
//					System.out.println("origin " + origin + ", destination " + destination + ", " + bls + ": " + logsum.getValue(bls,origin,destination));
//				}
//			}
//		}
//		origin = 1;
//		destination = 9707;
//		for (boolean mandatory : bs) {
//			for (boolean female : bs) {
//				for (boolean inbound : bs) {
//					BikeLogsumSegment bls = new BikeLogsumSegment(female,mandatory,inbound);
//					System.out.println("origin " + origin + ", destination " + destination + ", " + bls + ": " + logsum.getValue(bls,origin,destination));
//				}
//			}
//		}
//	}

}
