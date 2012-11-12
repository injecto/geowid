/*
 * Copyright (c) 2012, Creative Development LLC
 * Available under the New BSD license
 * see http://github.com/injecto/geowid for details
 */

package com.ecwid.geowid.daemon.tests;

import com.ecwid.geowid.daemon.resolvers.Cache;
import com.ecwid.geowid.daemon.resolvers.IpRange;
import com.ecwid.geowid.daemon.resolvers.ResolveRecord;
import com.ecwid.geowid.daemon.resolvers.RuIpResolver;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.internal.collections.Pair;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Тест классов из пакета com.ecwid.geowid.daemon.resolvers
 */
public class ResolverTest {

    @DataProvider(name = "ipRanges")
    public Object[][] getDataForIpRangeTest() {
        return new Object[][] {
                { "0.0.0.0-0.0.0.255", "0.0.0.0", Boolean.TRUE },
                { "192.168.0.10-192.168.0.10", "192.168.0.10", Boolean.TRUE },
                { "192.168.0.10-192.168.0.10", "192.168.0.11", Boolean.FALSE },
                { "192.168.0.0-255.255.255.255", "192.168.0.10", Boolean.TRUE },
                { "192.168.0.0-255.255.255.255", "192.167.255.255", Boolean.FALSE }
        };
    }

    @DataProvider(name = "illegalIpRanges")
    public Object[][] getDataForIllegalIpRangeTest() {
        return new Object[][] {
                { "0.0.0.00.0.0.255", "0.0.0.0", Boolean.TRUE },
                { "192.168.0.10-192.168.0.10", "192168.0.10", Boolean.TRUE },
                { "192.168.0.-192.168.0.10", "192.168.0.11", Boolean.FALSE },
                { "192.168.0.0-255.255.255.255", "192.168.0.", Boolean.TRUE },
                { "255.255.255.255-192.168.0.0", "192.167.255.255", Boolean.FALSE }
        };
    }

    @Test(dataProvider = "ipRanges")
    public void testIpRange(String range, String ip, boolean isInRange) throws Exception {
        Assert.assertEquals(new IpRange(range).inRange(ip), isInRange);
    }

    @Test(dataProvider = "illegalIpRanges", expectedExceptions = IllegalArgumentException.class)
    public void testIllegalIpRange(String range, String ip, boolean isInRange) throws Exception {
        Assert.assertEquals(new IpRange(range).inRange(ip), isInRange);
    }

    @Test
    public void testCache() throws Exception {
        Cache cache = new Cache("cache");

        Assert.assertTrue(cache.getRecord("10.0.0.1") == null);

        ResolveRecord record = new ResolveRecord(new IpRange("10.0.0.0-10.0.0.15"), 15.0f, 12.0f,
                new Date(new Date().getTime() + 500));
        cache.addRecord(record);
        Assert.assertTrue(cache.getRecord("10.0.0.0").equals(record));
        Assert.assertTrue(cache.getRecord("10.0.0.16") == null);
        Thread.sleep(1000);
        Assert.assertTrue(cache.getRecord("10.0.0.0") == null);
    }

    @Test
    public void testRuIpResolver() throws Exception {
        RuIpResolver resolver = new RuIpResolver("cache", 0);

        Map<String, Pair<Float, Float>> ipCoords = new HashMap<String, Pair<Float, Float>>();
        ipCoords.put("144.206.192.6", new Pair<Float, Float>(55.755787f, 37.617634f));
        ipCoords.put("2.56.176.1", new Pair<Float, Float>(50.450001f, 30.523333f));

        for (String ip : ipCoords.keySet()) {
            ResolveRecord record = resolver.resolve(ip);
            Assert.assertTrue(ipCoords.get(ip).first().compareTo(record.getLat()) == 0
                                && ipCoords.get(ip).second().compareTo(record.getLng()) == 0);
        }

        Assert.assertTrue(resolver.resolve("92.2.3.4") == null);
    }
}
