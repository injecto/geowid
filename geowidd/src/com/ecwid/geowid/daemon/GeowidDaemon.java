package com.ecwid.geowid.daemon;

import com.ecwid.geowid.daemon.settings.Settings;
import com.ecwid.geowid.daemon.settings.SettingsProvider;
import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
import org.apache.commons.daemon.DaemonInitException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class GeowidDaemon implements Daemon {

    public static void main(String[] args) {
        if (args.length != 2) {
            logger.fatal("Using: java -jar geowidd.jar settings_file maxmind_db_file");
            return;
        }

        settings = SettingsProvider.getSettings(args[0]);
        if (null == settings) {
            logger.fatal("Some error in settings file");
            return;
        }

        TailReader tailReader = new TailReader(settings.getLogFileCatalog(), settings.getLogFilePattern(),
                settings.getUpdatePeriod());
        RecordParser parser = new RecordParser(tailReader.getRecordsQueue(), settings.getEvents(), true);
        IpToLocationConverter converter = new IpToLocationConverter(parser.getIpQueue(), settings.getCacheFilePath(),
                settings.getCacheRecordTtl(), args[1]);

        ServerSocket serverSocket;
        try {
            serverSocket = new ServerSocket(settings.getPort());
        } catch (IOException e) {
            logger.fatal("Could not listen on port {}", settings.getPort());
            return;
        }

        Socket clientSocket;
        ObjectOutputStream out;
        while (!Thread.currentThread().isInterrupted()) {
            try {
                clientSocket = serverSocket.accept();
                converter.getPointsQueue().clear();
                out = new ObjectOutputStream(clientSocket.getOutputStream());

                while (!Thread.currentThread().isInterrupted())
                    out.writeObject(converter.getPointsQueue().take());

                out.close();
                clientSocket.close();
            } catch (IOException e) {
                logger.warn("Socket I/O error", e);
                continue;
            } catch (InterruptedException e) {
                break;
            }
        }

        try {
            serverSocket.close();
        } catch (IOException e) {
            logger.warn("Cannot close listen socket");
        }
    }

    @Override
    public void init(DaemonContext daemonContext) throws DaemonInitException, Exception {
        logger.info("Initialization...");
        context = daemonContext;
    }

    @Override
    public void start() throws Exception {
        logger.info("Starting...");
        mainThread = new Thread(new Runnable() {
            @Override
            public void run() {
                main(context.getArguments());
            }
        });
        mainThread.start();
    }

    @Override
    public void stop() throws Exception {
        logger.info("Stopping...");
        mainThread.interrupt();
    }

    @Override
    public void destroy() {
        logger.info("Done");
    }

    private DaemonContext context;

    private static Thread mainThread;

    private static Settings settings;
    private static final Logger logger = LogManager.getLogger(GeowidDaemon.class);
}
