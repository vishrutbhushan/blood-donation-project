package com.G9.hemoconnect.util;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class PincodeCoordinates {
    private static final Map<String, double[]> MAP = Map.of(
        "111111", new double[]{12.9716, 77.5946},
        "111112", new double[]{12.9800, 77.6000}
        // Add all pincodes used in Task 1 dummy data here
        // Coordinate with Task 1 teammate for the full list
    );

    public double[] getCoords(String pincode) {
        return MAP.getOrDefault(pincode, new double[]{12.9716, 77.5946});
    }
}
