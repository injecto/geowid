package com.ecwid.geowid.daemon.resolvers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.LinkedList;

/**
 * Резолвер координат для IP адресов из России и Украины
 */
public class RuIpResolver {

    /**
     * ctor
     * @param cacheFilePath путь к файлу кэша
     * @param cacheRecordTtl TTL записи в файле кэша (секунд)
     */
    public RuIpResolver(String cacheFilePath, long cacheRecordTtl) {
        cache = new Cache(cacheFilePath);

        if (cacheRecordTtl < 1 * 24 * 60 * 60) {
            logger.info("Given IP resolver cache's records TTL ({}s) too small. Use default TTL ({}s)",
                    cacheRecordTtl, ttl);
        } else
            ttl = cacheRecordTtl;
    }

    /**
     * определить координаты IP адреса
     * @param ip адрес
     * @return запись кэша для адреса или null в случае евозможности определения
     */
    public ResolveRecord resolve(String ip) {
        ResolveRecord record = cache.getRecord(ip);
        if (null != record)
            return record;
        else {
            record = serviceRequest(ip);
            if (null == record)
                return null;
            else {
                cache.addRecord(record);
                return record;
            }
        }
    }

    /**
     * запрос определения координат IP адреса с сервиса ipgeobase.ru
     * @param ip адрес
     * @return запись кэша для адреса или null в случае ошибок в процессе запроса
     */
    private ResolveRecord serviceRequest(String ip) {
        String range = null,
               lat = null,
               lng = null;

        XMLEventReader reader = null;
        try {
            String uri = serviceUrl + ip;
            URL reqUrl = new URL(uri);
            InputStream respStream = reqUrl.openStream();
            reader = XMLInputFactory.newInstance().createXMLEventReader(uri, respStream);
            XMLEvent event;

            while (reader.hasNext()) {
                event = reader.nextEvent();
                if (event.getEventType() == XMLStreamConstants.START_ELEMENT) {
                    String elementName = event.asStartElement().getName().getLocalPart();
                    if (elementName.equals("inetnum")) {
                        range = reader.peek().asCharacters().getData();
                    }
                    if (elementName.equals("lat")) {
                        lat = reader.peek().asCharacters().getData();
                    }
                    if (elementName.equals("lng")) {
                        lng = reader.peek().asCharacters().getData();
                    }
                }
            }
        } catch (MalformedURLException e) {
            return null;
        } catch (XMLStreamException e) {
            logger.warn("IP resolver service response invalid");
            return null;
        } catch (IOException e) {
            logger.warn("I/O error in IP resolver service request");
            return null;
        } finally {
            if (null != reader)
                try {
                    reader.close();
                } catch (XMLStreamException e) {
                    logger.warn("Some problem with XML stream (can't close)");
                    return null;
                }
        }

        if (null == range || null == lat || null == lng)
            return null;
        else {
            try {
                IpRange ipRange = new IpRange(range);
                float latitude = Float.parseFloat(lat);
                float longitude = Float.parseFloat(lng);
                return new ResolveRecord(ipRange, latitude, longitude, new Date(new Date().getTime() + ttl * 1000));
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    /**
     * Кэш записей диапазонов IP адресов
     */
    private class Cache {

        Cache(String cacheFilePath) {
            if ("".equals(cacheFilePath)) {
                this.cacheFilePath = new StringBuilder()
                    .append(System.getProperty("java.io.tmpdir"))
                    .append(File.separator)
                    .append("ipresolver.cache").toString();
                logger.info("IP resolver cache file not specified. Use {}", this.cacheFilePath);
            } else
                this.cacheFilePath = cacheFilePath;

            load();

            Thread flushThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        while (true) {
                            Thread.sleep(flushPeriod);
                            flush();
                        }
                    } catch (InterruptedException e) {
                        flush();
                        return;
                    }
                }
            });
            flushThread.setDaemon(true);
            flushThread.start();
        }

        /**
         * вернуть запись кэша, соответствующую заданному IP
         * @param ip адрес для поиска в кэше
         * @return запись кэша или null в случае её отсутствия
         */
        public ResolveRecord getRecord(String ip) {
            synchronized (data) {
                for (ResolveRecord record : data) {
                    if (record.getRange().inRange(ip))
                        if (record.getExpireTime().before(new Date())) {
                            data.remove(record);
                            return null;
                        } else
                            return record;
                }
                return null;
            }
        }

        /**
         * добавить запись в кэш
         * @param record запись
         */
        public void addRecord(ResolveRecord record) {
            synchronized (data) {
                data.add(record);
            }
        }

        /**
         * загрузить кэш из файла
         */
        @SuppressWarnings("unchecked")
        private void load() {
            ObjectInputStream objectInputStream;
            try {
                objectInputStream = new ObjectInputStream(new FileInputStream(cacheFilePath));
            } catch (IOException e) {
                logger.info("IP resolver cache file not found. Use clear cache");
                data = new LinkedList<ResolveRecord>();
                return;
            }

            try {
                synchronized (data) {
                    Object raw = objectInputStream.readObject();
                    if (raw instanceof LinkedList)
                        data = (LinkedList<ResolveRecord>) raw;
                }
            } catch (ClassNotFoundException e) {
                logger.warn("IP resolver cache file cannot read. Use clear cache");
            } catch (IOException e) {
                logger.warn("IP resolver cache file is corrupted. Use clear cache");
            } finally {
                try {
                    objectInputStream.close();
                } catch (IOException e) {
                    logger.warn("Some problem with IP resolver cache file stream. Can't close stream");
                }
            }
        }

        /**
         * сохранить кэш в файл
         */
        private void flush() {
            new File(cacheFilePath).delete();

            ObjectOutputStream objectOutputStream;
            try {
                objectOutputStream = new ObjectOutputStream(new FileOutputStream(cacheFilePath));
            } catch (IOException e) {
                logger.warn("Cannot write IP cache file");
                return;
            }

            try {
                synchronized (data) {
                    objectOutputStream.writeObject(data);
                }
            } catch (IOException e) {
                logger.warn("Cannot write IP cache file");
            } finally {
                try {
                    objectOutputStream.close();
                } catch (IOException e) {
                    logger.warn("Some problem with IP resolver cache file stream. Can't close stream");
                }
            }
        }

        private final String cacheFilePath;
        private long flushPeriod = 15 * 60 * 1000;
        private LinkedList<ResolveRecord> data = new LinkedList<ResolveRecord>();
    }

    private final Cache cache;
    private final String serviceUrl = "http://ipgeobase.ru:7020/geo?ip=";
    private long ttl = 7 * 24 * 60 * 60;
    private static final Logger logger = LogManager.getLogger(RuIpResolver.class);
}
