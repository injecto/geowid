/*
 * Copyright (c) 2012, Creative Development LLC
 * Available under the New BSD license
 * see http://github.com/injecto/geowid for details
 */

package com.ecwid.geowid.daemon;

import com.ecwid.geowid.utils.Point;
import com.ecwid.geowid.utils.SignedMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Signature;

/**
 * соединение с сервером
 */
public class Connection {

    /**
     * ctor
     * @param host адрес сервера
     * @param port порт подключения
     * @param keyFilePath путь к файлу, содержащему закрытый RSA-ключ
     * @throws IllegalArgumentException если переданы некорректные параметры
     */
    public Connection(String host, int port, String keyFilePath) throws IllegalArgumentException {
        if (null == host
                || host.isEmpty()
                || port < 1024
                || null == keyFilePath
                || keyFilePath.isEmpty())
            throw new IllegalArgumentException();

        address = new InetSocketAddress(host, port);
        if (address.isUnresolved())
            throw new IllegalArgumentException();

        ObjectInputStream privKeyStream = null;
        try {
            privKeyStream = new ObjectInputStream(new FileInputStream(keyFilePath));
            privKey = (PrivateKey) privKeyStream.readObject();
        } catch (Exception e) {
            throw new IllegalArgumentException();
        } finally {
            if (null != privKeyStream)
                try {
                    privKeyStream.close();
                } catch (IOException e) {
                    // ok
                }
        }
    }

    /**
     * подключиться
     * Блокирует поток до тех пор, пока не произойдет успешное подключение
     * @throws InterruptedException если в процессе подключения поток был прерван
     * @throws ConnectException если подключение инициализировано некорректно
     */
    public void connect() throws InterruptedException, ConnectException {
        waitForConnection();
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privKey);
            byte[] message = new byte[20];
            new SecureRandom().nextBytes(message);
            signature.update(message);
            outputStream.writeObject(new SignedMessage(message, signature.sign()));
        } catch (Exception e) {
            ConnectException ex = new ConnectException();
            ex.initCause(e);
            throw ex;
        }
    }

    /**
     * закрыть соединение
     * Если оно уже закрыто, то ничего не происходит
     * @throws IOException в случае I/O-ошибок в процессе закрытия
     */
    public void close() throws IOException {
        if (socket.isConnected()) {
            socket.close();
            socket = new Socket();
        }
    }

    /**
     * отправить точку карты серверу
     * Может блокировать выполнение потока до тех пор, пока данные не будут отправлены
     * @param point точка
     * @throws ConnectException если подключение инициализировано некорректно
     * @throws InterruptedException если в процессе подключения поток был прерван
     */
    public void send(Point point) throws ConnectException, InterruptedException {
        if (null == point)
            return;

        while (!Thread.currentThread().isInterrupted()) {
            try {
                outputStream.writeObject(point);
                if (++sendCounter >= resetPeriod) {
                    outputStream.reset();
                    sendCounter = 0;
                }
                return;
            } catch (IOException e) {
                connect();
            }
        }
        throw new InterruptedException();
    }

    /**
     * (пере)подключиться к серверу
     * Блокирует поток до тех пор, пока соединение не будет установлено
     * @throws InterruptedException если поток был прерван в процессе подключения
     */
    private void waitForConnection() throws InterruptedException {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                socket.close();
                socket = new Socket();

                socket.connect(address, timeOut);
                logger.info("Connect with {} [{}:{}]", socket.getInetAddress().getCanonicalHostName(),
                        socket.getInetAddress().getHostAddress(), socket.getPort());
                socket.shutdownInput();
                outputStream = new ObjectOutputStream(socket.getOutputStream());
                return;
            } catch (IOException e) {
                // continue
            }
        }
        throw new InterruptedException();
    }

    private Socket socket = new Socket();
    private ObjectOutputStream outputStream;
    private int sendCounter = 0;
    private final InetSocketAddress address;
    private final PrivateKey privKey;
    private static final int timeOut = 2000;    // тайм-аут между попытками подключения
    private static final int resetPeriod = 1000; // период сброса выходного потока (для предотвращения memory leaks)

    private static final Logger logger = LogManager.getLogger(Connection.class);
}
