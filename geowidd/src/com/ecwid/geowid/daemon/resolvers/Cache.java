/*
 * Copyright (c) 2012, Creative Development LLC
 * Available under the New BSD license
 * see http://github.com/injecto/geowid for details
 */

package com.ecwid.geowid.daemon.resolvers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Date;
import java.util.LinkedList;
import java.util.Timer;

/**
 * Кэш записей диапазонов IP адресов
 */
public class Cache {

    /**
     * ctor
     * @param cacheFilePath путь к файлу кэша. Если передан null или пустая строка -- использует кэш-файл по умолчанию
     */
    public Cache(String cacheFilePath) {
        if (null == cacheFilePath || cacheFilePath.isEmpty()) {
            this.cacheFilePath = defaultCacheFilePath;
            logger.info("IP resolver cache file not specified. Use {}", this.cacheFilePath);
        } else
            this.cacheFilePath = cacheFilePath;

        load();

        flushTimer.schedule(new FlushTask(data, this.cacheFilePath), flushPeriod, flushPeriod);
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
            return;
        }

        try {
            for (int i = 0; i < objectInputStream.readInt(); i++) {
                data.add((ResolveRecord) objectInputStream.readObject());
            }
        } catch (ClassNotFoundException e) {
            logger.warn("IP resolver cache file cannot read. Use clear cache");
        } catch (IOException e) {
            logger.warn("IP resolver cache file is corrupted. Some data has been lost");
        } finally {
            try {
                objectInputStream.close();
            } catch (IOException e) {
                logger.warn("Some problem with IP resolver cache file stream. Can't close stream");
            }
        }
    }

    private static final String defaultCacheFilePath = "ipresolver.cache";
    private static final long flushPeriod = 5 * 60 * 1000;

    private final LinkedList<ResolveRecord> data = new LinkedList<ResolveRecord>();
    private final String cacheFilePath;
    private final Timer flushTimer = new Timer("cache_flush_thread", true);

    private static final Logger logger = LogManager.getLogger(Cache.class);
}
