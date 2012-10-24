/*
 * Copyright (c) 2012, Creative Development LLC
 * Available under the New BSD license
 * see http://github.com/injecto/geowid for details
 */

package com.ecwid.geowid.server;

import com.ecwid.geowid.utils.Point;
import com.ecwid.geowid.utils.SignedMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.security.*;

/**
 * Провайдер точек карты
 */
public class PointsProvider {
    /**
     * ctor
     * @param listenPort порт для прослушивания входящих соединений
     * @param keyFilePath путь к файлу открытого RSA-ключа, используемого для установления надежности источника
     * @throws IOException в случае ошибок открытия слушающего сокета или чтения файла ключа
     */
    public PointsProvider(int listenPort, String keyFilePath) throws IOException {
        ObjectInputStream pubKeyStream = null;
        try {
            pubKeyStream = new ObjectInputStream(getClass().getClassLoader().getResourceAsStream(keyFilePath));
            publicKey = (PublicKey) pubKeyStream.readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException("Invalid key file format", e);
        } catch (NullPointerException e) {
            throw new IOException("Can't find key file", e);
        } finally {
            if (null != pubKeyStream)
                pubKeyStream.close();
        }

        serverSocket = new ServerSocket(listenPort);
    }

    /**
     * блокировать выполнение до тех пор, пока не поступит аутентифицированное входящее соединение
     * @return true в случае успешного соединения, иначе false
     * @throws InterruptedException если в процессе ожидания соединения поток будет прерван
     */
    public boolean waitForConnection() throws InterruptedException {
        boolean result = false;
        while (!result) {
            try {
                clientSocket = getClientSocket(serverSocket);
                clientInStream = new ObjectInputStream(clientSocket.getInputStream());

                result = isAuthClient(clientInStream);
            } catch (IOException e) {
                // continue
            } catch (ClassNotFoundException e) {
                // continue
            } catch (NoSuchAlgorithmException e) {
                logger.error(e.getMessage());
                return false;
            } catch (InvalidKeyException e) {
                logger.error(e.getMessage());
                return false;
            } catch (SignatureException e) {
                // continue
            }
        }

        return true;
    }

    /**
     * вернуть следующую точку карты из потока провайдера
     * @return точка или null в случае, если поток пуст
     * @throws IllegalStateException если метод вызван перед инициализацией объекта класса провайдера
     * @throws InterruptedException если поток (thread) был прерван
     */
    public Point next() throws IllegalStateException, InterruptedException {
        if (null == clientInStream || null == clientSocket)
            throw new IllegalStateException("Points provider not initialized");

        while (!Thread.currentThread().isInterrupted()) {
            try {
                return (Point) clientInStream.readObject();
            } catch (ClassNotFoundException e) {
                return null;
            } catch (IOException e) {
                try {
                    clientInStream.close();
                    clientSocket.close();
                } catch (IOException ex) {
                    // ok
                }
                while (!waitForConnection()) { }
            }
        }
        return null;
    }

    /**
     * разорвать соединение.
     * После вызова объект становится невалидным и его дальнейшее использование невозможно
     */
    public void breakConnection() {
        try {
            if (null != clientInStream)
                clientInStream.close();

            if (null != clientSocket)
                clientSocket.close();

            serverSocket.close();
        } catch (IOException e) {
            logger.error("Incorrect break of connection");
        }
    }

    /**
     * ожидать подключения и вернуть соответствующий сокет
     * @param serverSocket серверный сокет, на котором ожидается соединение
     * @return сокет соединения
     * @throws InterruptedException если поток был прерван в процессе ожидания подключения
     * @throws IOException если произошла I/O ошибка в серверном сокете
     */
    private Socket getClientSocket(ServerSocket serverSocket) throws InterruptedException, IOException {
        final int infiniteTimeOut = 0;

        serverSocket.setSoTimeout(socketAcceptTimeOut);
        Socket clientSocket = null;
        while (null == clientSocket) {
            try {
                if (Thread.currentThread().isInterrupted())
                    throw new InterruptedException();

                clientSocket = serverSocket.accept();
            } catch (SocketTimeoutException e) {
                // continue
            }
        }

        serverSocket.setSoTimeout(infiniteTimeOut);
        clientSocket.shutdownOutput();
        return clientSocket;
    }

    /**
     * проверить, является ли подключенный клиент доверенным источником данных
     * @param clientInStream входной поток от клиента
     * @return true в случае подтверждения достоверности, иначе false
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     * @throws ClassNotFoundException
     * @throws IOException
     * @throws SignatureException
     */
    private boolean isAuthClient(ObjectInputStream clientInStream)
            throws NoSuchAlgorithmException, InvalidKeyException,
                   ClassNotFoundException, IOException, SignatureException {
        Signature verifySign = Signature.getInstance("SHA256withRSA");
        verifySign.initVerify(publicKey);

        SignedMessage signedMessage = (SignedMessage) clientInStream.readObject();

        verifySign.update(signedMessage.getMessage());
        return verifySign.verify(signedMessage.getSignature());
    }

    private ServerSocket serverSocket;
    private PublicKey publicKey;

    private Socket clientSocket = null;
    private ObjectInputStream clientInStream = null;

    private static final int socketAcceptTimeOut = 100;    // тайм-аут ожидания подключения

    private static final Logger logger = LogManager.getLogger(PointsProvider.class);
}
