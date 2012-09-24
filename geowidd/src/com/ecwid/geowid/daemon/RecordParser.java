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
     */
    public RecordParser(LinkedBlockingQueue<String> recordsQueue, List<Event> events, boolean filterSearchBots) {
        this.recordsQueue = recordsQueue;
        this.events = events;
        this.filterSearchBots = filterSearchBots;

        Thread worker = new Thread(new Runnable() {
            @Override
            public void run() {
                parse();
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    /**
     * вернуть очередь событий лога
     * @return блокирующая очередь событий
     */
    public LinkedBlockingQueue<Ip> getIpQueue() {
        return ipQueue;
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

    private boolean filterSearchBots = false;
    private SearchBotAgent agent = new SearchBotAgent()
            .addAgentRegExp(SearchBotAgent.Google)
            .addAgentRegExp(SearchBotAgent.Baidu)
            .addAgentRegExp(SearchBotAgent.Bing)
            .addAgentRegExp(SearchBotAgent.Yahoo)
            .addAgentRegExp(SearchBotAgent.Yandex);
}
