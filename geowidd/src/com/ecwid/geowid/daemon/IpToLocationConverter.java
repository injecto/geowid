package com.ecwid.geowid.daemon;

import com.ecwid.geowid.daemon.resolvers.ResolveRecord;
import com.ecwid.geowid.daemon.resolvers.RuIpResolver;
import com.ecwid.geowid.utils.Point;
import com.maxmind.geoip.Location;
import com.maxmind.geoip.LookupService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
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
     */
    public IpToLocationConverter(String cacheFilePath, long ttl, String maxmindDBFile) throws IOException {
        this.cacheFilePath = cacheFilePath;
        this.ttl = ttl;
        maxmindDB = new File(maxmindDBFile);

        try {
            lookupService = new LookupService(maxmindDB, LookupService.GEOIP_MEMORY_CACHE);
        } catch (IOException e) {
            logger.fatal("Maxmind DB I/O exception", e);
            throw e;
        }
        ruIpResolver = new RuIpResolver(cacheFilePath, ttl);
    }

    /**
     * сконвертировать объект записи в объект карты
     * @param ip объект записи
     * @return объект карты или null в слушае невозможности резолвинга
     */
    public Point convert(Ip ip) {
        Location location = lookupService.getLocation(ip.getIp());

        Point point = null;
        if (null != location) {
            if (location.countryCode.equals("RU") || location.countryCode.equals("UA")) {
                ResolveRecord record = ruIpResolver.resolve(ip.getIp());
                if (null != record)
                    point = new Point(record.getLat(), record.getLng(), ip.getType());
            }
            if (null == point)
                point = new Point(location.latitude, location.longitude, ip.getType());

            return point;
        } else
            return null;
    }

    /**
     * закрыть сервис разрешения IP-адресов
     */
    public void closeService() {
        if (!ruIpResolver.stopService())
            logger.warn("Can't correct stop IP resolver cache service");

        lookupService.close();
    }

    private File maxmindDB;
    private final RuIpResolver ruIpResolver;
    private LookupService lookupService;

    private final String cacheFilePath;
    private final long ttl;

    private static final Logger logger = LogManager.getLogger(IpToLocationConverter.class);
}
