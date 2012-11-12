/*
 * Copyright (c) 2012, Creative Development LLC
 * Available under the New BSD license
 * see http://github.com/injecto/geowid for details
 */

package com.ecwid.geowid.daemon;

/**
 * Слушатель обновления точек на карте
 */
public interface IPointListener {
    /**
     * при возникновении новой порции данных-точек
     * @param slice массив точек в формате JSON
     */
    void onSlice(String slice);
}
