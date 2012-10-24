/*
 * Copyright (c) 2012, Creative Development LLC
 * Available under the New BSD license
 * see http://github.com/injecto/geowid for details
 */

package com.ecwid.geowid.server;

import com.ecwid.geowid.utils.Point;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Буфер точек для отображения на карте
 */
public class PointsBuffer {
    /**
     * ctor
     * @param chunkSize размер порции точек для единовременной отправки на фронтэнд
     * @param provider провайдер точек
     */
    public PointsBuffer(int chunkSize, PointsProvider provider) {
        if (chunkSize <= 0) {
            logger.info("Size of chunk defined incorrect ({}). Use default value ({})", chunkSize, defaultChunkSize );
            this.chunkSize = defaultChunkSize;
        } else
            this.chunkSize = chunkSize;

        this.provider = provider;

        chunk = new ArrayList<Point>(chunkSize);
    }

    /**
     * начать заполнение буфера
     */
    public void fillBuffer() {
        worker = new Thread(new Runnable() {
            @Override
            public void run() {
                collect();
            }
        }, "geowidsrv_points_buffer");
        worker.start();
    }

    /**
     * закрыть буфер
     * @return true в случае успеха
     */
    public boolean close() {
        if (null == worker || !worker.isAlive())
            return true;

        worker.interrupt();
        Thread.interrupted();
        try {
            worker.join();
        } catch (InterruptedException e) {
            return false;
        }

        provider.breakConnection();
        return true;
    }

    /**
     * добавить слушателя события заполнения буфера
     * @param listener слушатель
     * @return true если добавлен, иначе false
     */
    public boolean addListener(IPointListener listener) {
        return null != listener && listeners.add(listener);
    }

    /**
     * удалить слушателя события заполнения буфера
     * @param listener слушатель
     * @return true если удален, иначе false
     */
    public boolean removeListener(IPointListener listener) {
        return null != listener && listeners.remove(listener);
    }

    /**
     * собрать данные в буфер
     */
    private void collect() {
        try {
            while (!provider.waitForConnection()) { }
        } catch (InterruptedException e) {
            return;
        }

        while (!Thread.currentThread().isInterrupted()) {
            try {
                Point point = provider.next();
                if (null == point)
                    continue;

                chunk.add(point);
                if (chunk.size() >= chunkSize) {
                    String slice = gson.toJson(chunk, new TypeToken<List<Point>>(){}.getType());
                    chunk.clear();

                    for (IPointListener listener : listeners)
                        listener.onSlice(slice);
                }
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    private final List<Point> chunk;
    private final int chunkSize;
    private final PointsProvider provider;

    private final Gson gson = new Gson();
    private static final int defaultChunkSize = 16;
    private Thread worker = null;

    private final List<IPointListener> listeners = Collections.synchronizedList(new LinkedList<IPointListener>());

    private static final Logger logger = LogManager.getLogger(PointsBuffer.class);
}
