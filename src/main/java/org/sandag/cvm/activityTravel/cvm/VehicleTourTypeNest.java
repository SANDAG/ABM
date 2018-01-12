package org.sandag.cvm.activityTravel.cvm;

import org.sandag.cvm.common.model.LogitModel;

public class VehicleTourTypeNest extends AlogitLogitModelNest {
    final String myCode;

    public VehicleTourTypeNest(String code) {
        myCode = code;
    }

    public String getCode() {
        return myCode;
    }

}
