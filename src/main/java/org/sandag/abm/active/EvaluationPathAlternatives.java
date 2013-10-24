package org.sandag.abm.active;

public class EvaluationPathAlternatives<N extends Node, E extends Edge<N>> implements PathAlternatives {
	private final PathAlternativeList<N,E> pathAlternativeList;
	
	public EvaluationPathAlternatives(PathAlternativeList<N,E> pathAlternativeList) {
		this.pathAlternativeList = pathAlternativeList;
	}

	@Override
	public double getSizeAlt(int path) {
		return pathAlternativeList.getSizeMeasures().get(path-1);
	}

	@Override
	public double getDistanceAlt(int path) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getDistanceClass1Alt(int path) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getDistanceClass2Alt(int path) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getDistanceClass3Alt(int path) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getDistanceArtNoLaneAlt(int path) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getDistanceCycleTrackAlt(int path) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getDistanceBikeBlvdAlt(int path) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getDistanceWrongWayAlt(int path) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getGainAlt(int path) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getTurnsAlt(int path) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getPathCount() {
		return pathAlternativeList.getCount();
	}

}
