package com.ecwid.geowid.daemon.resolvers;

import java.io.Serializable;
import java.util.Date;

/**
 * Запись в кэше адресов
 */
public class ResolveRecord implements Serializable {
    /**
     * ctor
     * @param range диапазон адресов, соответствующих записи
     * @param lat географическая широта
     * @param lng географическая долгота
     * @param expireTime абсолютное время, когда запись будет считаться устаревшей
     */
    ResolveRecord(IpRange range, float lat, float lng, Date expireTime) {
        this.range = range;
        this.lat = lat;
        this.lng = lng;
        this.expireTime = expireTime;
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
        return expireTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResolveRecord record = (ResolveRecord) o;

        if (Float.compare(record.lat, lat) != 0) return false;
        if (Float.compare(record.lng, lng) != 0) return false;
        if (expireTime != null ? !expireTime.equals(record.expireTime) : record.expireTime != null) return false;
        if (range != null ? !range.equals(record.range) : record.range != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = range != null ? range.hashCode() : 0;
        result = 31 * result + (lat != +0.0f ? Float.floatToIntBits(lat) : 0);
        result = 31 * result + (lng != +0.0f ? Float.floatToIntBits(lng) : 0);
        result = 31 * result + (expireTime != null ? expireTime.hashCode() : 0);
        return result;
    }

    private final IpRange range;
    private final float lat;
    private final float lng;
    private final Date expireTime;
}
