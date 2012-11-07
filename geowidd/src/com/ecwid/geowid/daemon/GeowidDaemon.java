/*
 * Copyright (c) 2012, Creative Development LLC
 * Available under the New BSD license
 * see http://github.com/injecto/geowid for details
 */

package com.ecwid.geowid.daemon;

import com.ecwid.geowid.daemon.settings.Settings;
import com.ecwid.geowid.daemon.settings.SettingsProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.xml.bind.JAXBException;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;

/**
 * демон, осуществляющий работу с логом
 */
public class GeowidDaemon {

    public static void main(String[] args) {
        logger.info("Geowid daemon start...");

        if (args.length != 1) {
            logger.fatal("Using: java -jar geowidd.jar <settings_file_path>");
            return;
        }

        Thread hookThread = new Thread(new Runnable() {
            @Override
            public void run() {
                shutdownHook();
            }
        });
        Runtime.getRuntime().addShutdownHook(hookThread);

        final String settingsFilePath = args[0];
        daemonThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    process(settingsFilePath);
                } catch (JAXBException e) {
                    logger.fatal(e.getMessage());
                } catch (IOException e) {
                    logger.fatal(e.getMessage());
                } catch (InterruptedException e) {
                    // ok
                }
            }
        });
        daemonThread.start();
    }

    private static void shutdownHook() {
        logger.info("Shutdown...");
        if (null != daemonThread && daemonThread.isAlive()) {
            daemonThread.interrupt();
            try {
                daemonThread.join(joinTime);
            } catch (InterruptedException e) {
                logger.error("Shutdown hook's thread is interrupted. Daemon thread can be incorrect complete");
            }
        }
        logger.info("done.");
    }

    /**
     * запустить обработку лога
     * @param settingsFilePath путь к файлу настроек
     * @throws IllegalArgumentException если настройки демона некорректны
     * @throws JAXBException если файл настроек имеет неверный формат
     * @throws IOException в случае проблем с MaxmindDB
     * @throws InterruptedException если процесс был прерван
     */
    private static void process(String settingsFilePath)
            throws IllegalArgumentException, JAXBException, IOException, InterruptedException {

        final Settings settings = SettingsProvider.getSettings(settingsFilePath);
        final TailReader reader = new TailReader(settings.getLogFileCatalog(),
                    settings.getLogFilePattern(), settings.getUpdatePeriod());
        final RecordParser parser = new RecordParser(settings.getEvents(), true);
        final IpToLocationConverter converter = new IpToLocationConverter(settings.getCacheFilePath(),
                    settings.getCacheRecordTtl(), settings.getResolverDB());
        final PointsBuffer buffer = new PointsBuffer(settings.getChunkSize());

        final URL serverUrl = new URL(settings.getServerUrl());

        buffer.addListener(new IPointListener() {
            @Override
            public void onSlice(String slice) {
                HttpURLConnection connection = null;
                DataOutputStream outputStream = null;
                try {
                    connection = (HttpURLConnection) serverUrl.openConnection();
                    String urlParams = "points=" + URLEncoder.encode(slice, "utf-8");
                    connection.setDoOutput(true);
                    connection.setConnectTimeout(connectionTimeOut);
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    connection.setRequestProperty("charset", "utf-8");
                    connection.setRequestProperty("Content-Length", Integer.toString(urlParams.getBytes().length));

                    outputStream = new DataOutputStream(connection.getOutputStream());
                    outputStream.writeBytes(urlParams);
                    outputStream.flush();

                    if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
                        logger.warn("Server not receive data");
                } catch (SocketTimeoutException e) {
                    // ok
                } catch (IOException e) {
                    // ok
                } finally {
                    if (null != outputStream)
                        try {
                            outputStream.close();
                        } catch (IOException e) {
                            logger.warn("Can't close HTTP output stream");
                        }
                    if (null != connection)
                        connection.disconnect();
                }
            }
        });

        reader.start();

        try {
            while (!Thread.currentThread().isInterrupted()) {
                buffer.addPoint(converter.convert(parser.parse(reader.nextRecord())));
            }
        } finally {
            reader.stop();
        }
    }

    private static Thread daemonThread = null;
    private static final long joinTime = 3000; // время ожидания корректного завершения потока демона
    private static final int connectionTimeOut = 1000;

    private static final Logger logger = LogManager.getLogger(GeowidDaemon.class);
}
