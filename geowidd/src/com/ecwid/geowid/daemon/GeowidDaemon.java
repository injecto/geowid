/*
 * Copyright (c) 2012, Creative Development LLC
 * Available under the New BSD license
 * see http://github.com/injecto/geowid for details
 */

package com.ecwid.geowid.daemon;

import com.ecwid.geowid.daemon.settings.Settings;
import com.ecwid.geowid.daemon.settings.SettingsProvider;
import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
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
public class GeowidDaemon implements Daemon {

    @Override
    public void init(DaemonContext daemonContext) throws Exception {
        logger.info("Initialization...");
        daemonArgs = daemonContext.getArguments();
    }

    @Override
    public void start() throws Exception {
        logger.info("Starting...");
        daemonThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        process(daemonArgs);
                    } catch (IllegalArgumentException e) {
                        logger.fatal(e.getMessage());
                        System.exit(ErrorCodes.INIT_ERROR.getCode());
                    } catch (JAXBException e) {
                        logger.fatal(e.getMessage());
                        System.exit(ErrorCodes.INIT_ERROR.getCode());
                    } catch (IOException e) {
                        logger.fatal(e.getMessage());
                        System.exit(ErrorCodes.INIT_ERROR.getCode());
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }
        }, "geowidd_main");

        daemonThread.start();
    }

    @Override
    public void stop() throws Exception {
        logger.info("Stopping...");
        daemonThread.interrupt();
        Thread.interrupted();
        daemonThread.join(joinTime);
    }

    @Override
    public void destroy() {
        logger.info("Done");
    }

    /**
     * запустить обработку лога
     * @param args аргументы запуска
     * @throws IllegalArgumentException если настройки демона некорректны
     * @throws JAXBException если файл настроек имеет неверный формат
     * @throws IOException в случае проблем с MaxmindDB
     * @throws InterruptedException если процесс был прерван
     */
    private void process(String[] args)
            throws IllegalArgumentException, JAXBException, IOException, InterruptedException {

        if (args.length != 1) {
            logger.fatal("Using: java -jar geowidd.jar <settings_file>");
            throw new IllegalArgumentException("Incorrect args");
        }

        final Settings settings = SettingsProvider.getSettings(args[0]);
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

    /**
     * коды ошибок при некорректном завершении работы
     */
    private static enum ErrorCodes {
        OK(0),
        INIT_ERROR(1);

        ErrorCodes(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }

        private final int code;
    }

    private String[] daemonArgs;
    private Thread daemonThread;
    private static final long joinTime = 3000; // время ожидания корректного завершения потока демона
    private static final int connectionTimeOut = 1000;

    private static final Logger logger = LogManager.getLogger(GeowidDaemon.class);
}
