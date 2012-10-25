/*
 * Copyright (c) 2012, Creative Development LLC
 * Available under the New BSD license
 * see http://github.com/injecto/geowid for details
 */

package com.ecwid.geowid.utils;

import java.io.*;

/**
 * Точка события лога на карте
 */
public final class Point implements Externalizable {

    public Point() {
        this(0.0f, 0.0f, null);
    }

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
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeFloat(lat);
        out.writeFloat(lng);
        byte[] rawStr = type.getBytes();
        out.writeInt(rawStr.length);
        out.write(rawStr);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        lat = in.readFloat();
        lng = in.readFloat();
        byte[] rawStr = new byte[in.readInt()];
        if (in.read(rawStr) != rawStr.length)
            throw new IOException();

        type = new String(rawStr);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if ((o == null) || (this.getClass() != o.getClass())) return false;

        Point point = (Point) o;

        return Float.compare(point.lat, lat) == 0
                && Float.compare(point.lng, lng) == 0
                && type.equals(point.type);
    }

    @Override
    public int hashCode() {
        int result = (lat != +0.0f ? Float.floatToIntBits(lat) : 0);
        result = 31 * result + (lng != +0.0f ? Float.floatToIntBits(lng) : 0);
        result = 31 * result + type.hashCode();
        return result;
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

    private static final long serialVersionUID = -8202145592911306167L;
}
