/*
 * Copyright (c) 2012, Creative Development LLC
 * Available under the New BSD license
 * see http://github.com/injecto/geowid for details
 */

package com.ecwid.geowid.daemon.resolvers;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Диапазон IP адресов
 */
public class IpRange implements Externalizable {

    public IpRange() { }

    /**
     * ctor
     * @param range диапазон адресов в формате "x.x.x.x - x.x.x.x"
     * @throws IllegalArgumentException если переданная строка диапазона некорректна
     */
    public IpRange(String range) throws IllegalArgumentException {
        String[] split = range.split("-");
        if (split.length != 2)
            throw new IllegalArgumentException("IP range string needed (x.x.x.x - x.x.x.x)");

        try {
            rawStartIp = ipToLong(InetAddress.getByName(split[0].trim()));
            rawEndIp = ipToLong(InetAddress.getByName(split[1].trim()));
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("IP range string needed (x.x.x.x - x.x.x.x)", e);
        }

        if (rawStartIp > rawEndIp)
            throw new IllegalArgumentException("IP range incorrect");
    }

    /**
     * проверить заданный IP на нахождение в диапазоне
     * @param ip IP адрес
     * @return true если находится в диапазоне, иначе false
     * @throws IllegalArgumentException если передан некорректный IP
     */
    public boolean inRange(String ip) throws IllegalArgumentException {
        try {
            InetAddress testIp = InetAddress.getByName(ip);
            return inRange(testIp);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Incorrect IP", e);
        }
    }

    /**
     * проверить заданный адрес на нахождение в диапазоне
     * @param ip адрес
     * @return true если находится в диапазоне, иначе false
     */
    public boolean inRange(InetAddress ip) {
        long testIp = ipToLong(ip);
        return rawStartIp <= testIp && rawEndIp >= testIp;
    }

    /**
     * конвертировать адрес в низкоуровневый целочисленный формат
     * @param ip адрес
     * @return адрес в целочисленном формате
     */
    private long ipToLong(InetAddress ip) {
        byte[] octets = ip.getAddress();
        long result = 0;
        for (byte octet : octets) {
            result <<= 8;
            result |= octet & 0xff;
        }
        return result;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeLong(rawStartIp);
        out.writeLong(rawEndIp);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        rawStartIp = in.readLong();
        rawEndIp = in.readLong();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IpRange ipRange = (IpRange) o;

        return rawEndIp == ipRange.rawEndIp
                && rawStartIp == ipRange.rawStartIp;
    }

    @Override
    public int hashCode() {
        int result = (int) (rawStartIp ^ (rawStartIp >>> 32));
        result = 31 * result + (int) (rawEndIp ^ (rawEndIp >>> 32));
        return result;
    }

    private long rawStartIp;
    private long rawEndIp;
}
