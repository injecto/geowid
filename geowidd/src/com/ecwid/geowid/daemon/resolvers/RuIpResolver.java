/*
 * Copyright (c) 2012, Creative Development LLC
 * Available under the New BSD license
 * see http://github.com/injecto/geowid for details
 */

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

        if (cacheRecordTtl < minTtl) {
            logger.info("Given IP resolver cache's records TTL ({}s) too small. Use default TTL ({}s)",
                    cacheRecordTtl, ttl);
        } else
            ttl = cacheRecordTtl;
    }

    /**
     * определить координаты IP адреса
     * @param ip адрес
     * @return запись кэша для адреса или null в случае невозможности определения
     */
    public ResolveRecord resolve(String ip) {
        ResolveRecord record = cache.getRecord(ip);
        if (null != record)
            return record;
        else {
            record = serviceRequest(ip);
            if (null != record)
                cache.addRecord(record);

            return record;
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

    private final Cache cache;
    private final String serviceUrl = "http://ipgeobase.ru:7020/geo?ip=";
    private long ttl = 7 * 24 * 60 * 60;
    private static long minTtl = 1 * 24 * 60 * 60;

    private static final Logger logger = LogManager.getLogger(RuIpResolver.class);
}
