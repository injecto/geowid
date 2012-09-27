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
        if (null == worker)
            return true;

        worker.interrupt();
        boolean interrupt = Thread.interrupted();
        try {
            worker.join();
        } catch (InterruptedException e) {
            return false;
        }
        return true;
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
     */
    private void collect() {
        while (!Thread.currentThread().isInterrupted()) {
            Socket socket = null;
            try {
                socket = new Socket(logHost, logPort);
            } catch (IOException e) {
                logger.warn("Can't create socket to connect to geowidd-service. Try again...");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    break;
                }
                continue;
            }

            ObjectInputStream inputStream = null;
            try {
                inputStream = new ObjectInputStream(socket.getInputStream());
            } catch (IOException e) {
                logger.warn("Can't create socket input stream. Try again...");
                try {
                    socket.close();
                } catch (IOException ex) {
                    logger.warn("Can't close socket. Use new socket");
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    break;
                }
                continue;
            }

            while (!Thread.currentThread().isInterrupted()) {
                Point point = null;
                try {
                    point = (Point) inputStream.readObject();
                } catch (IOException e) {
                    logger.warn("I/O error when read from input stream. Re-create socket...");
                    try {
                        inputStream.close();
                        socket.close();
                    } catch (IOException ex) {
                        logger.warn("Can't close socket resources");
                    }
                    break;
                } catch (ClassNotFoundException e) {
                    logger.warn("Deserialization error", e);
                    continue;
                }

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

            try {
                if (null != inputStream)
                    inputStream.close();
            } catch (IOException e) {
                logger.warn("Can't close socket input stream");
            }

            try {
                if (null != socket && !socket.isClosed())
                    socket.close();
            } catch (IOException e) {
                logger.warn("Can't close socket");
            }
        }
    }

    private final List<Point> chunk;
    private final int chunkSize;
    private final String logHost;
    private final int logPort;

    private final Gson gson = new Gson();
    private Thread worker = null;

    private final List<IPointListener> listeners = Collections.synchronizedList(new LinkedList<IPointListener>());

    private static final Logger logger = LogManager.getLogger(PointsBuffer.class);
}
