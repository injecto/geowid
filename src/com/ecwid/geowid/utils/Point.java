package com.ecwid.geowid.utils;

import java.io.Serializable;

/**
 * Точка события лога на карте
 */
public class Point implements Serializable {

    public Point(float lat, float lng, String type) {
        this.lat = lat;
        this.lng = lng;
        this.type = type;
    }

    public float getLat() {
        return lat;
    }

    public float getLng() {
        return lng;
    }

    public String getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Point point = (Point) o;

        if (Float.compare(point.lat, lat) != 0) return false;
        if (Float.compare(point.lng, lng) != 0) return false;
        if (!type.equals(point.type)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (lat != +0.0f ? Float.floatToIntBits(lat) : 0);
        result = 31 * result + (lng != +0.0f ? Float.floatToIntBits(lng) : 0);
        result = 31 * result + type.hashCode();
        return result;
    }

    private final float lat;
    private final float lng;
    private final String type;
}
