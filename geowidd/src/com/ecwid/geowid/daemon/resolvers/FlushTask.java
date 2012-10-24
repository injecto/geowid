/*
 * Copyright (c) 2012, Creative Development LLC
 * Available under the New BSD license
 * see http://github.com/injecto/geowid for details
 */

package com.ecwid.geowid.daemon.resolvers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.TimerTask;

/**
 * Задача сброса данных кэша в файл
 */
public class FlushTask extends TimerTask {

    /**
     * ctor
     * @param cacheData данные для сохранения
     * @param file файл для сохранения
     */
    public FlushTask(List<ResolveRecord> cacheData, String file) {
        this.cacheData = cacheData;
        this.file = file;
    }

    @Override
    public void run() {
        new File(file).delete();

        ObjectOutputStream objectOutputStream;
        try {
            objectOutputStream = new ObjectOutputStream(new FileOutputStream(file));
        } catch (IOException e) {
            logger.warn("Cannot write IP cache file");
            return;
        }

        try {
            synchronized (cacheData) {
                objectOutputStream.writeInt(cacheData.size());
                for (ResolveRecord record : cacheData)
                    objectOutputStream.writeObject(record);
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

    private final List<ResolveRecord> cacheData;
    private final String file;
    private static final Logger logger = LogManager.getLogger(FlushTask.class);
}