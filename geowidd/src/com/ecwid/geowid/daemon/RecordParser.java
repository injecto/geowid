package com.ecwid.geowid.daemon;

import com.ecwid.geowid.daemon.settings.Event;
import com.ecwid.geowid.daemon.utils.SearchBotAgent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Парсер записей лога
 */
public class RecordParser {

    /**
     * ctor
     * @param recordsQueue очередь записей
     * @param events необходимые события
     * @param filterSearchBots true - фильтровать записи от запросов поисковых роботов
     */
    public RecordParser(LinkedBlockingQueue<String> recordsQueue, List<Event> events, boolean filterSearchBots) {
        this.recordsQueue = recordsQueue;
        this.events = events;
        this.filterSearchBots = filterSearchBots;

        worker = new Thread(new Runnable() {
            @Override
            public void run() {
                parse();
            }
        }, "geowidd_parser");
        worker.start();
    }

    /**
     * вернуть очередь событий лога
     * @return блокирующая очередь событий
     */
    public LinkedBlockingQueue<Ip> getIpQueue() {
        return ipQueue;
    }

    /**
     * остановить парсинг
     * @return true в случае успеха, иначе false
     */
    public boolean close() {
        worker.interrupt();
        boolean interrupt = Thread.interrupted();
        try {
            worker.join();
        } catch (InterruptedException e) {
            return false;
        }
        return true;
    }

    private void parse() {
        HashMap<Pattern, String> patternMap = new HashMap<Pattern, String>();
        for (Event e : events) {
            patternMap.put(Pattern.compile(e.getPattern()), e.getType());
        }
        while (!Thread.currentThread().isInterrupted()) {
            try {
                String record = recordsQueue.take();
                for (Map.Entry<Pattern, String> e : patternMap.entrySet()) {
                    Matcher matcher = e.getKey().matcher(record);
                    if (matcher.matches() && (filterSearchBots ? !agent.isSearchBot(record) : true)) {
                        ipQueue.put(new Ip(matcher.group(1), e.getValue()));
                    }
                }
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    private final LinkedBlockingQueue<Ip> ipQueue = new LinkedBlockingQueue<Ip>();
    private final LinkedBlockingQueue<String> recordsQueue;
    private final List<Event> events;

    private final Thread worker;

    private boolean filterSearchBots = false;
    private SearchBotAgent agent = new SearchBotAgent()
            .addAgentRegExp(SearchBotAgent.Google)
            .addAgentRegExp(SearchBotAgent.Baidu)
            .addAgentRegExp(SearchBotAgent.Bing)
            .addAgentRegExp(SearchBotAgent.Yahoo)
            .addAgentRegExp(SearchBotAgent.Yandex);
}
