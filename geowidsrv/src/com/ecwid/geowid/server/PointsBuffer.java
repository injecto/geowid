package com.ecwid.geowid.server;

import com.ecwid.geowid.utils.Point;
import com.google.gson.Gson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
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
     * @param logHost хост удаленного сервера - провайдера точек
     * @param logPort порт удаленного сервера - провайдера точек
     */
    public PointsBuffer(int chunkSize, String logHost, int logPort) {
        this.chunkSize = chunkSize;
        this.logHost = logHost;
        this.logPort = logPort;

        chunk = new ArrayList<Point>(chunkSize);
    }

    /**
     * начать заполнение буфера
     */
    public void fillBuffer() {
        Thread worker = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        collect();
                    } catch (IOException e) {
                        logger.warn("Remote log host connection error. Try again...", e);
                    } catch (ClassNotFoundException e) {
                        logger.warn("Remote log host response deserialization error. Try again...", e);
                    }
                }
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    /**
     * добавить слушателя события заполнения буфера
     * @param listener слушатель
     * @return true если добавлен, иначе false
     */
    public boolean addListener(IPointListener listener) {
        if (null != listener)
            return listeners.add(listener);
        else
            return false;
    }

    /**
     * удалить слушателя события заполнения буфера
     * @param listener слушатель
     * @return true если удален, иначе false
     */
    public boolean removeListener(IPointListener listener) {
        if (null != listener)
            return  listeners.remove(listener);
        else
            return false;
    }

    /**
     * собрать данные от удаленного хоста в буфер
     * @throws IOException в случае проблем с сокетом
     * @throws ClassNotFoundException в случае проблем с десериализацией
     */
    private void collect() throws IOException, ClassNotFoundException {
        Socket socket = new Socket(logHost, logPort);
        ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());

        while (!Thread.currentThread().isInterrupted()) {
            Point point = (Point) inputStream.readObject();
            if (null != point) {
                chunk.add(point);
                if (chunk.size() >= chunkSize) {
                    String slice = gson.toJson(chunk);
                    chunk.clear();

                    for (IPointListener listener : listeners)
                        listener.onSlice(slice);
                }
            }
        }

        inputStream.close();
        socket.close();
    }

    private final List<Point> chunk;
    private final int chunkSize;
    private final String logHost;
    private final int logPort;

    Gson gson = new Gson();

    private final List<IPointListener> listeners = Collections.synchronizedList(new LinkedList<IPointListener>());

    private static final Logger logger = LogManager.getLogger(PointsBuffer.class);
}
