package com.ecwid.geowid.server; /**
* Package: com.ecwid
* User:    vio
* Date:    20.08.12 16:36
*/

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
