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
import java.net.SocketTimeoutException;

public class GeowidDaemon implements Daemon {

    public static void main(String[] args) {
        if (args.length != 2) {
            logger.fatal("Using: geowidd.jar settings_file maxmind_db_file");
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

        try {
            openServerSocket();
        } catch (IOException e) {
            logger.fatal("Could not listen on port {}", settings.getPort());
            return;
        }

        Socket clientSocket = null;
        ObjectOutputStream out = null;

        while (!Thread.currentThread().isInterrupted()) {
            try {
                clientSocket = serverSocket.accept();
            } catch (SocketTimeoutException e) {
                continue;
            } catch (IOException e) {
                logger.error("I/O error when waiting for client connection. Try reopen server socket...");
                try {
                    reopenServerSocket();
                } catch (IOException ex) {
                    logger.fatal("Server socket is broken. Shutting down...", ex);
                    break;
                }
                logger.info("Server socket reopened");
                continue;
            }

            converter.getPointsQueue().clear();

            try {
                out = new ObjectOutputStream(clientSocket.getOutputStream());
            } catch (IOException e) {
                logger.warn("I/O error when creating client output stream. Try again...");
                try {
                    clientSocket.close();
                } catch (IOException ex) {
                    logger.warn("Can't close client socket. Use new client socket");
                }
                continue;
            }

            try {
                while (!Thread.currentThread().isInterrupted())
                    out.writeObject(converter.getPointsQueue().take());
            } catch (InterruptedException e) {
                break;
            } catch (IOException e) {
                logger.warn("I/O error when sending data to client. Re-create client socket...");
                try {
                    out.close();
                } catch (IOException ex) {
                    logger.warn("Can't close client output stream. Use new stream");
                }

                try {
                    clientSocket.close();
                } catch (IOException ex) {
                    logger.warn("Can't close client socket. Use new client socket");
                }
                continue;
            }
        }

        if (!(converter.close() && parser.close() && tailReader.close()))
            logger.warn("Several service threads not die. Sorry");

        try {
            if (null != out)
                out.close();

            if (null != clientSocket && !clientSocket.isClosed())
                clientSocket.close();
        } catch (IOException e) {
            logger.warn("Can't close client socket. Sorry");
        }

        try {
            serverSocket.close();
        } catch (IOException e) {
            logger.error("Can't close server socket");
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
        }, "geowidd_main");
        mainThread.start();
    }

    @Override
    public void stop() throws Exception {
        logger.info("Stopping...");
        mainThread.interrupt();
        mainThread.join();
    }

    @Override
    public void destroy() {
        logger.info("Done");
    }

    private static void openServerSocket() throws IllegalArgumentException, IOException {
        if (null == settings)
            throw new IllegalArgumentException("Need app settings");

        serverSocket = new ServerSocket(settings.getPort(), 1);
        serverSocket.setSoTimeout(250);
    }

    private static void reopenServerSocket() throws IllegalArgumentException, IOException {
        try {
            serverSocket.close();
        } catch (IOException e) {
            throw new IOException("Can't close server socket", e);
        }
        openServerSocket();
    }

    private DaemonContext context;
    private static Thread mainThread;
    private static Settings settings;
    private static ServerSocket serverSocket;

    private static final Logger logger = LogManager.getLogger(GeowidDaemon.class);
}
