package com.ecwid.geowid.daemon.resolvers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.Date;
import java.util.LinkedList;

/**
 * Кэш записей диапазонов IP адресов
 */
class Cache {
    Cache(String cacheFilePath) {
        if ("".equals(cacheFilePath)) {
            logger.info("IP resolver cache file not specified. Use {}", this.cacheFilePath);
        } else
            this.cacheFilePath = cacheFilePath;

        load();

        flushThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (!Thread.currentThread().isInterrupted()) {
                        Thread.sleep(flushPeriod);
                        flush();
                    }
                } catch (InterruptedException e) {
                    flush();
                    return;
                }
            }
        }, "geowidd_cache_flush");
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
     * отменить задачу периодического сброса кэша на диск
     * @return true в случае успеха, иначе false
     */
    public boolean close() {
        flushThread.interrupt();
        boolean interrupt = Thread.interrupted();
        try {
            flushThread.join();
        } catch (InterruptedException e) {
            return false;
        }
        return true;
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
            Object raw = objectInputStream.readObject();
            if (raw instanceof LinkedList)
                data = (LinkedList<ResolveRecord>) raw;
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

    private String cacheFilePath = "ipresolver.cache";
    private long flushPeriod = 15 * 60 * 1000;
    private LinkedList<ResolveRecord> data = new LinkedList<ResolveRecord>();

    private final Thread flushThread;

    private static final Logger logger = LogManager.getLogger(Cache.class);
}
