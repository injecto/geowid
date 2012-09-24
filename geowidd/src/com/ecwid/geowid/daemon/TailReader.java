package com.ecwid.geowid.daemon;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Реализует слежение за обновлением лога
 */
public class TailReader {

    /**
     * ctor
     * @param logFileCatalog путь к каталогу, в котором расположен файл лога
     * @param logFileNamePattern regexp, соответствующий имени файла лога
     * @param updatePeriod период проверки изменений в логе (миллисекунд)
     */
    public TailReader(String logFileCatalog, String logFileNamePattern, long updatePeriod) {
        this.logFileCatalog = logFileCatalog;
        this.logFileNamePattern = logFileNamePattern;
        this.updatePeriod = updatePeriod;

        Thread worker = new Thread(new Runnable() {
            @Override
            public void run() {
                collect();
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    /**
     * вернуть очередь новых записей лога
     * @return очередь новых записей лога
     */
    public LinkedBlockingQueue<String> getRecordsQueue() {
        return recordsQueue;
    }

    /**
     * начать сбор новых данных лога
     */
    private void collect() {
        try {
            prepare();
        } catch (FileNotFoundException e) {
            logger.fatal(e.getMessage());
            return;
        } catch (InterruptedException e) {
            return;
        }

        String line = null;
        int slipCounter = 0;
        while (true) {
            try {
                line = logReader.readLine();
                if (null == line) {
                    if (slipCounter >= maxSlipNumber) {
                        reopen();
                        slipCounter = 0;
                        continue;
                    }
                    slipCounter++;
                    Thread.sleep(updatePeriod);
                } else {
                    slipCounter = 0;
                    recordsQueue.put(line);
                }
            } catch (IOException e) {
                logger.warn("Log reading some I/O error. Miss one line from log");
                continue;
            } catch (InterruptedException e) {
                try {
                    logReader.close();
                } catch (IOException e1) {
                    logger.warn("Can't close log stream (file {}). It will no affect, but unpleasantly",
                            currentLogFile.getAbsolutePath());
                }
                break;
            }
        }
    }

    /**
     * переоткрыть лог
     * @throws FileNotFoundException если лог не удалось найти
     * @throws InterruptedException если поток исполнения был прерван
     */
    private void reopen() throws InterruptedException, FileNotFoundException {
        try {
            logReader.close();
        } catch (IOException e) {
            logger.warn("Can't close log stream (file {}). It will no affect, but unpleasantly",
                    currentLogFile.getAbsolutePath());
        }

        currentLogFile = waitForLog();
        logger.info("New log opened {}", currentLogFile.getAbsolutePath());
        logReader = new BufferedReader(new FileReader(currentLogFile));
    }

    /**
     * подготовиться к работе
     * @throws FileNotFoundException если лог не удалось найти
     * @throws InterruptedException если поток исполнения был прерван
     */
    private void prepare() throws FileNotFoundException, InterruptedException {
        currentLogFile = waitForLog();
        logger.info("Open a log {}", currentLogFile.getAbsolutePath());
        logReader = new BufferedReader(new FileReader(currentLogFile));
        String line = null;
        do {
            try {
                line = logReader.readLine();
            } catch (IOException e) {
                logger.warn("Log reading I/O error");
                break;
            }
        } while (null != line);
    }

    /**
     * принудительно открыть лог
     * @return файл лога
     * @throws FileNotFoundException если лог не удалось найти
     * @throws InterruptedException если поток исполнения был прерван
     */
    private File waitForLog() throws FileNotFoundException, InterruptedException {
        File log = null;
        int c = 0;
        do {
            c++;
            log = getLogFile();
            if (null == log) {
                Thread.sleep(logSearchAttemptPeriod);
                continue;
            } else {
                return log;
            }
        } while (c < maxLogSearchAttempt);
        throw new FileNotFoundException("Log file not found [number of attempts: " + c + "]");
    }

    /**
     * вернуть используемый в данный момент лог-файл
     * @return лог-файл или null в случае отсутствия
     */
    private File getLogFile() {
        File logCatalog = new File(logFileCatalog);
        File[] files = logCatalog.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                if (pathname.canRead()
                        && pathname.isFile()
                        && pathname.getName().matches(logFileNamePattern))
                    return true;
                return false;
            }
        });

        if (0 == files.length)
            return null;

        if (files.length > 1)
            Arrays.sort(files, new Comparator<File>() {
                @Override
                public int compare(File o1, File o2) {
                    return (int) (o1.lastModified() - o2.lastModified());
                }
            });

        return files[files.length - 1];
    }

    private final String logFileCatalog,
                         logFileNamePattern;

    private final long updatePeriod;

    private BufferedReader logReader;
    private File currentLogFile;

    private final int maxSlipNumber = 3;    // количество "промахов", после которого считается, что создан новый лог
    private final long logSearchAttemptPeriod = 100;    // время между попытками поиска лога
    private final int maxLogSearchAttempt = 100;    // максимальное количество попыток поиска лога

    private final LinkedBlockingQueue<String> recordsQueue = new LinkedBlockingQueue<String>();

    private static final Logger logger = LogManager.getLogger(TailReader.class);
}
