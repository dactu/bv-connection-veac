package com.vht.connection.Process;

public class CalculatePosition {
    private static CalculatePosition instance;

    public static CalculatePosition getInstance() {
        synchronized (CalculatePosition.class) {
            if (instance == null) {
                instance = new CalculatePosition();
            }
        }
        return instance;
    }

    public double calculateDistance(double   lat1, double long1, double lat2, double long2) {
        int R = 6371000;
        double deltaPhi = lat2 - lat1;
        double deltaGam = long2 - long1;
        double a = Math.sin(deltaPhi/2)*Math.sin(deltaPhi/2) + Math.cos(lat1)*Math.cos(lat2)*Math.sin(deltaGam/2)*Math.sin(deltaGam/2);
        double c = 2*Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return R*c;
    }

    public double calculateBearing(double lat1, double long1, double lat2, double long2) {
        double deltaGam = long2 - long1;
        return Math.atan2(Math.sin(deltaGam)*Math.cos(lat2), Math.cos(lat1)*Math.sin(lat2)-Math.sin(lat1)*Math.cos(lat2)*Math.cos(deltaGam));
    }
}
