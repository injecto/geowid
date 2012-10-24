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
import java.io.IOException;
import java.net.ConnectException;

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
                        logger.fatal("Init failed. Check settings");
                        System.exit(ErrorCodes.INIT_ERROR.getCode());
                    } catch (JAXBException e) {
                        logger.fatal("Init failed. Incorrect settings file format");
                        System.exit(ErrorCodes.INIT_ERROR.getCode());
                    } catch (IOException e) {
                        logger.fatal("Init failed. Some problem with Maxmind DB file");
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

        Settings settings = SettingsProvider.getSettings(args[0]);
        TailReader reader = new TailReader(settings.getLogFileCatalog(),
                    settings.getLogFilePattern(), settings.getUpdatePeriod());
        RecordParser parser = new RecordParser(settings.getEvents(), true);
        IpToLocationConverter converter = new IpToLocationConverter(settings.getCacheFilePath(),
                    settings.getCacheRecordTtl(), settings.getResolverDbFilePath());
        Connection connection = new Connection(settings.getServerHost(), settings.getServerPort(),
                    settings.getPrivKeyFilePath());

        try {
            connection.connect();
            reader.start();

            while (!Thread.currentThread().isInterrupted()) {
                connection.send(converter.convert(parser.parse(reader.nextRecord())));
            }
        } catch (ConnectException e) {
            // ok
        } finally {
            reader.stop();
            try {
                connection.close();
            } catch (IOException e) {
                // ok
            }
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

    private static final Logger logger = LogManager.getLogger(GeowidDaemon.class);
}
