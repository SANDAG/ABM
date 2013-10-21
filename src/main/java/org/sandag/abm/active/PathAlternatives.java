package org.sandag.abm.active;

public interface PathAlternatives {
	double getSizeAlt(int path);
    double getDistanceAlt(int path);
    double getDistanceClass1Alt(int path);
    double getDistanceClass2Alt(int path);
    double getDistanceClass3Alt(int path);
    double getDistanceArtNoLaneAlt(int path);
    double getDistanceCycleTrackAlt(int path);
    double getDistanceBikeBlvdAlt(int path);
    double getDistanceWrongWayAlt(int path);
    double getGainAlt(int path);
    double getTurnsAlt(int path);
    int getPathCount();
}
