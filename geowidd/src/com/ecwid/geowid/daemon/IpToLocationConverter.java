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
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Конвертер IP в геолокацию
 */
public class IpToLocationConverter {

    /**
     * ctor
     * @param ipQueue очередь событий
     * @param cacheFilePath кэш-файл для сохранения уже запрошенных IP
     * @param ttl TTL записи в кэше (секунд)
     */
    public IpToLocationConverter(LinkedBlockingQueue<Ip> ipQueue, String cacheFilePath, long ttl,
                                 String maxmindDBFile) {
        this.ipQueue = ipQueue;
        this.cacheFilePath = cacheFilePath;
        this.ttl = ttl;
        maxmindDB = new File(maxmindDBFile);

        Thread worker = new Thread(new Runnable() {
            @Override
            public void run() {
                convert();
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    /**
     * вернуть очередь геолокаций
     * @return очередь геолокаций
     */
    public LinkedBlockingQueue<Point> getPointsQueue() {
        return pointsQueue;
    }

    private void convert() {
        LookupService lookupService = null;
        try {
            lookupService = new LookupService(maxmindDB, LookupService.GEOIP_MEMORY_CACHE);
        } catch (IOException e) {
            logger.fatal("Maxmind DB I/O exception", e);
            return;
        }

        RuIpResolver ruIpResolver = new RuIpResolver(cacheFilePath, ttl);

        while (!Thread.currentThread().isInterrupted()) {
            try {
                Ip ip = ipQueue.take();
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

                    pointsQueue.put(point);
                }
            } catch (InterruptedException e) {
                break;
            }
        }

        lookupService.close();
    }

    private final LinkedBlockingQueue<Ip> ipQueue;
    private final LinkedBlockingQueue<Point> pointsQueue = new LinkedBlockingQueue<Point>();
    private File maxmindDB;

    private final String cacheFilePath;
    private final long ttl;

    private static final Logger logger = LogManager.getLogger(IpToLocationConverter.class);
}
