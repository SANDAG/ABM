package org.sandag.abm.ctramp;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.sandag.abm.modechoice.MgraDataManager;

public class BikeLogsum implements SegmentedSparseMatrix<BikeLogsumSegment> {
	public static final String BIKE_LOGSUM_OUTPUT_PROPERTY = "active.output.bike";
	public static final String BIKE_LOGSUM_MGRA_FILE_PROPERTY = "active.logsum.matrix.file.bike.mgra";
	public static final String BIKE_LOGSUM_TAZ_FILE_PROPERTY = "active.logsum.matrix.file.bike.taz";
	public static final String BIKE_LOGSUM_NODE_PAIR_COUNT_PROPERTY = "active.logsum.matrix.node.pair.count";
	public static final int DEFAULT_BIKE_LOGSUM_NODE_PAIR_COUNT = 20_000_000;
	
	private final Map<MatrixLookup,double[]> logsum;
	
	private static volatile BikeLogsum instance = null;
	
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
		logsum = new HashMap<BikeLogsum.MatrixLookup, double[]>(nodePairCount,1.1f); //capacity of nodepairs, plus a little buffer just in case
		Map<Integer,Set<Integer>> tazMgraMapping = loadTazMgraMapping(mgraFile);
		//load tazs first, then mgras, so if mgra (effectively) exists in both, the latter will overwrite with more detail
		loadLogsum(tazLogsumFile,tazMgraMapping);
		loadLogsum(mgraLogsumFile);
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
	
	private void loadLogsum(String logsumFile) {
		loadLogsum(logsumFile,null);
	}
	
	private void loadLogsum(String logsumFile, Map<Integer,Set<Integer>> tazMgraMapping) {
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
				double[] logsumData = new double[segmentWidth];
				for (int i = 0; i < logsumData.length; i++)
					logsumData[i] = Double.parseDouble(lineData[segmentIndex[i]]);
				if (tazMgraMapping != null) {
					Set<Integer> fromMgras = tazMgraMapping.get(Integer.parseInt(lineData[0]));
					Set<Integer> toMgras = tazMgraMapping.get(Integer.parseInt(lineData[1]));
					for (int fromMgra : fromMgras) {
						for (int toMgra : toMgras) {
							MatrixLookup ml = new MatrixLookup(fromMgra,toMgra);
							logsum.put(ml,logsumData);							
						}
					}
				} else {
					MatrixLookup ml = new MatrixLookup(Integer.parseInt(lineData[0]),Integer.parseInt(lineData[1]));
					logsum.put(ml,logsumData);
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public double getValue(BikeLogsumSegment segment, int rowId, int columnId) {
		double[] logsums = logsum.get(new MatrixLookup(rowId,columnId));
		return logsums == null ? -999 : logsums[segment.getSegmentId()];
	}
	
	public double getMultiSegmentLogsum(int rowId, int columnId, BikeLogsumSegment ... segments) {
		double logsum = 0.0;
		for (BikeLogsumSegment segment : segments)
			logsum += getValue(segment,rowId,columnId);
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
	
	public static void main(String ... args) {
		Map<String,String> testRb = new HashMap<>();
		
		testRb.put(BIKE_LOGSUM_OUTPUT_PROPERTY,"D:/projects/sandag/output_test");
		testRb.put(BIKE_LOGSUM_MGRA_FILE_PROPERTY,"bikeMgraLogsum.csv");
		testRb.put(BIKE_LOGSUM_TAZ_FILE_PROPERTY,"bikeTazLogsum.csv");
		testRb.put(BIKE_LOGSUM_NODE_PAIR_COUNT_PROPERTY,"55000");
		testRb.put(CtrampApplication.PROPERTIES_PROJECT_DIRECTORY,"D:/projects/sandag/abm_reporting/abm_shell");
		testRb.put(MgraDataManager.PROPERTIES_MGRA_DATA_FILE,"input/mgra12_based_input08_rev.csv");
		BikeLogsum logsum = BikeLogsum.getBikeLogsum(testRb);
		
		System.out.println(logsum.logsum.size());

		
		int origin = 3668;
		int destination = 9707;
		boolean[] bs = {true,false};
		for (boolean mandatory : bs) {
			for (boolean female : bs) {
				for (boolean inbound : bs) {
					BikeLogsumSegment bls = new BikeLogsumSegment(female,mandatory,inbound);
					System.out.println("origin " + origin + ", destination " + destination + ", " + bls + ": " + logsum.getValue(bls,origin,destination));
				}
			}
		}
	}

}
