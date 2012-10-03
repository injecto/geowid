package com.ecwid.geowid.daemon;

import com.ecwid.geowid.daemon.settings.Event;
import com.ecwid.geowid.daemon.utils.SearchBotAgent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Парсер записей лога
 */
public class RecordParser {

    /**
     * ctor
     * @param events необходимые события
     * @param filterSearchBots true - фильтровать записи от запросов поисковых роботов
     */
    public RecordParser(List<Event> events, boolean filterSearchBots) {
        for (Event e : events) {
            patternMap.put(e.getType(), Pattern.compile(e.getPattern()));
        }
        kSet = patternMap.keySet();

        this.filterSearchBots = filterSearchBots;
    }

    /**
     * распарсить запись
     * @param record запись
     * @return объект, соответствующуй записи, или null в случае отсутствия описания типа записи
     */
    public Ip parse(String record) {
        for (String e : kSet) {
            Matcher matcher = patternMap.get(e).matcher(record);
            if (matcher.matches() && (filterSearchBots ? !agent.isSearchBot(record) : true)) {
                return new Ip(matcher.group(1), e);
            }
        }
        return null;
    }

    private Map<String, Pattern> patternMap = new HashMap<String, Pattern>();
    private Set<String> kSet;

    private boolean filterSearchBots = false;
    private SearchBotAgent agent = new SearchBotAgent()
            .addAgentRegExp(SearchBotAgent.Google)
            .addAgentRegExp(SearchBotAgent.Baidu)
            .addAgentRegExp(SearchBotAgent.Bing)
            .addAgentRegExp(SearchBotAgent.Yahoo)
            .addAgentRegExp(SearchBotAgent.Yandex);
}
