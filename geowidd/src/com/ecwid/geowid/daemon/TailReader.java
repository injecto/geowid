/*
 * Copyright (c) 2012, Creative Development LLC
 * Available under the New BSD license
 * see http://github.com/injecto/geowid for details
 */

package com.ecwid.geowid.daemon;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Реализует слежение за обновлением лога. Аналог unix-утилиты tail (с параметром -f)
 */
public class TailReader {

    /**
     * ctor
     * @param logFileCatalog путь к каталогу, в котором расположен файл лога
     * @param logFileNamePattern regexp, соответствующий имени файла лога
     * @param updatePeriod период проверки изменений в логе (миллисекунд)
     * @throws IllegalArgumentException в случае передачи некорректных параметров
     */
    public TailReader(String logFileCatalog, String logFileNamePattern, long updatePeriod)
            throws IllegalArgumentException {
        if (null == logFileCatalog
                || null == logFileNamePattern
                || logFileCatalog.isEmpty()
                || logFileNamePattern.isEmpty()
                || updatePeriod <= 0)
            throw new IllegalArgumentException("Incorrect log reader's initialization parameters");

        this.logFileCatalog = logFileCatalog;
        this.logFileNamePattern = logFileNamePattern;
        this.updatePeriod = updatePeriod;
    }

    /**
     * вернуть следующую новую запись лога
     * @return запись
     * @throws InterruptedException если в процессе ожидания новой записи поток был прерван
     */
    public String nextRecord() throws InterruptedException {
        return recordsQueue.take();
    }

    /**
     * начать чтение лога
     */
    public void start() {
        if (null == worker || !worker.isAlive()) {
            worker = new Thread(new CollectTask(), "geowidd_tail_reader");
            worker.start();
        }
    }

    /**
     * остановить чтение лога
     * Если уже остановлено, то ничего не происходит
     * @return true в случае успеха, иначе false
     */
    public boolean stop() {
        if (null == worker || !worker.isAlive())
            return true;

        worker.interrupt();
        Thread.interrupted();
        try {
            worker.join();
        } catch (InterruptedException e) {
            return false;
        }
        return true;
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

        prepare();
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
        String line;
        do {
            try {
                line = logReader.readLine();
            } catch (IOException e) {
                logger.warn("Log reading I/O error");
                break;
            }
        } while (null != line || Thread.currentThread().isInterrupted());
    }

    /**
     * принудительно открыть лог
     * @return файл лога
     * @throws FileNotFoundException если лог не удалось найти
     * @throws InterruptedException если поток исполнения был прерван
     */
    private File waitForLog() throws FileNotFoundException, InterruptedException {
        File log;
        int c = 0;
        do {
            c++;
            log = getLogFile();
            if (null == log) {
                Thread.sleep(logSearchAttemptPeriod);
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
                return pathname.canRead()
                        && pathname.isFile()
                        && pathname.getName().matches(logFileNamePattern);
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

    /**
     * задача сбора новых данных из лога
     */
    private class CollectTask implements Runnable {

        @Override
        public void run() {
            try {
                prepare();
            } catch (FileNotFoundException e) {
                logger.fatal(e.getMessage());
                return;
            } catch (InterruptedException e) {
                return;
            }

            String line;
            int slipCounter = 0;
            while (!Thread.currentThread().isInterrupted()) {
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
                        if (recordsQueue.size() >= maxQueueSize)
                            recordsQueue.poll();
                        recordsQueue.put(line);
                    }
                } catch (IOException e) {
                    logger.warn("Log reading some I/O error. Miss one line from log");
                } catch (InterruptedException e) {
                    break;
                }
            }

            try {
                logReader.close();
            } catch (IOException e1) {
                logger.warn("Can't close log stream (file {}). It will no affect, but unpleasantly",
                        currentLogFile.getAbsolutePath());
            }
        }
    }

    private static final int maxSlipNumber = 3;    // количество "промахов", после которого считается, что создан новый лог
    private static final long logSearchAttemptPeriod = 100;    // время между попытками поиска лога
    private static final int maxLogSearchAttempt = 100;    // максимальное количество попыток поиска лога
    private static final int maxQueueSize = 200;  // максимальный размер очереди новых записей лога

    private final LinkedBlockingQueue<String> recordsQueue = new LinkedBlockingQueue<String>();
    private final long updatePeriod;
    private final String logFileCatalog,
                         logFileNamePattern;
    private BufferedReader logReader;
    private File currentLogFile;
    private Thread worker = null;

    private static final Logger logger = LogManager.getLogger(TailReader.class);
}
