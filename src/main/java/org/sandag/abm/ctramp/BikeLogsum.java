package org.sandag.abm.ctramp;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.sandag.abm.modechoice.MgraDataManager;

import com.pb.common.util.ResourceUtil;

/**
 * The {@code BikeLogsum} class holds bike logsums for use in the SANDAG model. This class is intended to be used as a singleton, and so the only way to
 * access it is via the {@code getBikeLogsum} static method. It is constructed on demand and safe for concurrent access. 
 * <p>
 * Internally, the logsums are held in a mapping using node-pairs as keys. The taz and mgra pairs are held in the same mapping, with the tazs multiplied
 * by -1 to avoid conflicts. To ensure good performance when building the object, a good guess as to the number of node pairs (maz pairs plus taz pairs)
 * can be provided (a default value of 26 million will be used otherwise) via the {@code BIKE_LOGSUM_NODE_PAIR_COUNT_PROPERTY} property.
 */
public class BikeLogsum implements SegmentedSparseMatrix<BikeLogsumSegment>,Serializable {
	private static final long serialVersionUID = 660793106399818667L;
	private static Logger logger = Logger.getLogger(BikeLogsum.class);
	
	public static final String BIKE_LOGSUM_OUTPUT_PROPERTY = "active.output.bike";
	public static final String BIKE_LOGSUM_MGRA_FILE_PROPERTY = "active.logsum.matrix.file.bike.mgra";
	public static final String BIKE_LOGSUM_TAZ_FILE_PROPERTY = "active.logsum.matrix.file.bike.taz";
	public static final String BIKE_LOGSUM_NODE_PAIR_COUNT_PROPERTY = "active.logsum.matrix.node.pair.count";
	/**
	 * The default logsum node pair count.
	 */
	public static final int DEFAULT_BIKE_LOGSUM_NODE_PAIR_COUNT = 26_000_000; //testing found 18_880_631, so this should be good enough to start
																  
	
	private Map<MatrixLookup,double[]> logsum;
	private int[] mgraIndex;
	
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
			int logsumIndex = -1;
			int timeIndex = -1;
			boolean first = true;

			String line;
			while ((line = reader.readLine()) != null) {
				String[] lineData = line.trim().split(",");
				for (int i = 0; i < lineData.length; i++)
					lineData[i] = lineData[i].trim();
				if (first) {
					for (int i = 2; i < lineData.length; i++) { //first two are for row and column
						String columnName = lineData[i].toLowerCase();
						if (columnName.contains("logsum"))
							logsumIndex = i;
						if (columnName.contains("time"))
							timeIndex = i;
					}
					first = false;
					continue;
				}
				if (++counter % 100_000 == 0)
					logger.debug("Finished processing " + counter + " node pairs (logsum lookup size: " + logsum.size() + ")");
				//if we ever bring back segmented logsums, then this will be a bit more complicated
				// the basic idea is all logsums first, then times (in same order) so lookups are straightforward
				// without having to replicate the hashmap, which is a big data structure
				double[] data = new double[] {Double.parseDouble(lineData[logsumIndex]),Double.parseDouble(lineData[timeIndex])};

				int fromZone = Integer.parseInt(lineData[0]);
				int toZone = Integer.parseInt(lineData[1]);
				int indexFactor = taz ? -1 : 1;
				MatrixLookup ml = new MatrixLookup(indexFactor*fromZone,indexFactor*toZone);
			    logsum.put(ml,data);
			    	
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
	
	public double getLogsum(BikeLogsumSegment segment, int rowId, int columnId) {
		return getValue(segment,rowId,columnId);
	}
	
	public double getTime(BikeLogsumSegment segment, int rowId, int columnId) {
		double[] logsums = getLogsums(rowId,columnId);
		return logsums == null ? Double.POSITIVE_INFINITY : logsums[segment.getSegmentId()+BikeLogsumSegment.segmentWidth()];
	}
	
	private static class MatrixLookup implements Serializable {
		private static final long serialVersionUID = -5048040835197200584L;
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
	
	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
		out.writeObject(logsum);
		out.writeObject(mgraIndex);
	}
	
	@SuppressWarnings("unchecked")
	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		logsum = (Map<MatrixLookup,double[]>) in.readObject();
		mgraIndex = (int[]) in.readObject();
		synchronized (BikeLogsum.class) {
			//ensures singleton - readResolve will ensure all get this single value
			//we need to allow the above reading of fields, though, so that deserialization is aligned correctly
			if (instance == null) 
				instance = this;
		}
	}
	
	private Object readResolve() throws ObjectStreamException {
		return instance; //ensures singelton
	}
	
	public static void main(String ... args) {
		org.apache.log4j.BasicConfigurator.configure();
		LogManager.getRootLogger().setLevel(Level.INFO);
		logger.info("usage: org.sandag.abm.ctramp.BikeLogsum properties origin_mgra dest_mgra");
		Map<String,String> properties = ResourceUtil.getResourceBundleAsHashMap(args[0]);
		int originMgra = Integer.parseInt(args[1]);
		int destMgra = Integer.parseInt(args[2]);
		BikeLogsum bls = BikeLogsum.getBikeLogsum(properties);

	    BikeLogsumSegment defaultSegment = new BikeLogsumSegment(true,true,true);
		double logsum = bls.getLogsum(new BikeLogsumSegment(true,true,true),originMgra,destMgra);
		double time = bls.getTime(new BikeLogsumSegment(true,true,true),originMgra,destMgra);
		logger.info(String.format("omgra: %s, dmgra: %s, logsum: %s, time: %s",originMgra,destMgra,logsum,time));
	}

}
