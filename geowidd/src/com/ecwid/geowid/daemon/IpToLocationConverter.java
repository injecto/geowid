/*
 * Copyright (c) 2012, Creative Development LLC
 * Available under the New BSD license
 * see http://github.com/injecto/geowid for details
 */

package com.ecwid.geowid.daemon;

import com.ecwid.geowid.daemon.resolvers.ResolveRecord;
import com.ecwid.geowid.daemon.resolvers.RuIpResolver;
import com.ecwid.geowid.utils.Point;
import com.maxmind.geoip.Location;
import com.maxmind.geoip.LookupService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

/**
 * Конвертер IP в геолокацию
 */
public class IpToLocationConverter {

    /**
     * ctor
     * @param cacheFilePath кэш-файл для сохранения уже запрошенных IP
     * @param ttl TTL записи в кэше (секунд)
     * @param maxmindDBFile путь к файлу данных движка Maxmind
     * @throws IOException если произошла ошибка при инициализации Maxmind
     * @throws IllegalArgumentException если переданы некорректные параметры
     */
    public IpToLocationConverter(String cacheFilePath, long ttl, String maxmindDBFile)
            throws IOException, IllegalArgumentException {
        if (null == maxmindDBFile)
            throw new IllegalArgumentException();

        try {
            lookupService = new LookupService(maxmindDBFile, dbMemoryUsage);
        } catch (IOException e) {
            logger.fatal("Maxmind DB I/O exception", e);
            throw e;
        }
        ruIpResolver = new RuIpResolver(cacheFilePath, ttl);
    }

    /**
     * сконвертировать объект записи в объект карты
     * @param logEvent объект записи
     * @return объект карты или null в случае невозможности разрешения геоположения
     */
    public Point convert(LogEvent logEvent) {
        if (null == logEvent)
            return null;

        Location location;
        try {
            location = lookupService.getLocation(logEvent.getIp());
        } catch (ArrayIndexOutOfBoundsException e) {
            logger.warn("{} (for {})", e.getMessage(), logEvent.toString());
            return null;
        }

        Point point = null;
        if (null != location) {
            if (location.countryCode.equals("RU") || location.countryCode.equals("UA")) {
                ResolveRecord record = ruIpResolver.resolve(logEvent.getIp());
                if (null != record)
                    point = new Point(record.getLat(), record.getLng(), logEvent.getType());
            }
            if (null == point)
                point = new Point(location.latitude, location.longitude, logEvent.getType());

            return point;
        } else
            return null;
    }

    private final RuIpResolver ruIpResolver;
    private final LookupService lookupService;
    private static final int dbMemoryUsage = LookupService.GEOIP_INDEX_CACHE;

    private static final Logger logger = LogManager.getLogger(IpToLocationConverter.class);
}
