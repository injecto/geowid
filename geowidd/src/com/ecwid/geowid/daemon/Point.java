/*
 * Copyright (c) 2012, Creative Development LLC
 * Available under the New BSD license
 * see http://github.com/injecto/geowid for details
 */

package com.ecwid.geowid.daemon;

/**
 * Точка события лога на карте
 */
class Point {
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
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Point");
        sb.append("{lat=").append(lat);
        sb.append(", lng=").append(lng);
        sb.append(", type='").append(type).append('\'');
        sb.append('}');
        return sb.toString();
    }

    private float lat;
    private float lng;
    private String type;
}
