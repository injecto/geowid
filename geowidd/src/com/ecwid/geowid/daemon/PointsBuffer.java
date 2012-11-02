/*
 * Copyright (c) 2012, Creative Development LLC
 * Available under the New BSD license
 * see http://github.com/injecto/geowid for details
 */

package com.ecwid.geowid.daemon;

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
     */
    public PointsBuffer(int chunkSize) {
        if (chunkSize <= 0) {
            logger.info("Size of chunk defined incorrect ({}). Use default value ({})", chunkSize, defaultChunkSize );
            this.chunkSize = defaultChunkSize;
        } else
            this.chunkSize = chunkSize;

        chunk = new ArrayList<Point>(chunkSize);
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
     * добавить точку в буфер
     * @param point точка
     */
    public void addPoint(Point point) {
        if (null == point)
            return;

        chunk.add(point);
        if (chunk.size() >= chunkSize) {
            String slice = gson.toJson(chunk, new TypeToken<List<Point>>(){}.getType());
            chunk.clear();

            for (IPointListener listener : listeners)
                listener.onSlice(slice);
        }
    }

    private final List<Point> chunk;
    private final int chunkSize;

    private final Gson gson = new Gson();
    private static final int defaultChunkSize = 16;

    private final List<IPointListener> listeners = Collections.synchronizedList(new LinkedList<IPointListener>());

    private static final Logger logger = LogManager.getLogger(PointsBuffer.class);
}
