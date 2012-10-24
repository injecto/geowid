/*
 * Copyright (c) 2012, Creative Development LLC
 * Available under the New BSD license
 * see http://github.com/injecto/geowid for details
 */

package com.ecwid.geowid.daemon.resolvers;

import java.io.*;
import java.util.Date;

/**
 * Запись в кэше адресов
 */
public class ResolveRecord implements Externalizable {

    public ResolveRecord() { }

    /**
     * ctor
     * @param range диапазон адресов, соответствующих записи
     * @param lat географическая широта
     * @param lng географическая долгота
     * @param expireTime абсолютное время, когда запись будет считаться устаревшей
     */
    public ResolveRecord(IpRange range, float lat, float lng, Date expireTime) {
        this.range = range;
        this.lat = lat;
        this.lng = lng;
        this.expireTime = (Date) expireTime.clone();
    }

    public IpRange getRange() {
        return range;
    }

    public float getLat() {
        return lat;
    }

    public float getLng() {
        return lng;
    }

    public Date getExpireTime() {
        return (Date) expireTime.clone();
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(range);
        out.writeFloat(lat);
        out.writeFloat(lng);
        out.writeLong(expireTime.getTime());
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        range = (IpRange) in.readObject();
        lat = in.readFloat();
        lng = in.readFloat();
        expireTime = new Date(in.readLong());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResolveRecord record = (ResolveRecord) o;

        if (Float.compare(record.lat, lat) != 0) return false;
        if (Float.compare(record.lng, lng) != 0) return false;
        if (expireTime != null ? !expireTime.equals(record.expireTime) : record.expireTime != null) return false;
        return !(range != null ? !range.equals(record.range) : record.range != null);

    }

    @Override
    public int hashCode() {
        int result = range != null ? range.hashCode() : 0;
        result = 31 * result + (lat != +0.0f ? Float.floatToIntBits(lat) : 0);
        result = 31 * result + (lng != +0.0f ? Float.floatToIntBits(lng) : 0);
        result = 31 * result + (expireTime != null ? expireTime.hashCode() : 0);
        return result;
    }

    private IpRange range;
    private float lat;
    private float lng;
    private Date expireTime;
}
