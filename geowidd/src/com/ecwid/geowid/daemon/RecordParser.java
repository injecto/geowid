/*
 * Copyright (c) 2012, Creative Development LLC
 * Available under the New BSD license
 * see http://github.com/injecto/geowid for details
 */

package com.ecwid.geowid.daemon;

import com.ecwid.geowid.daemon.settings.Event;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Парсер записей лога
 */
public class RecordParser {

    /**
     * ctor
     * @param events интересующие записи лога (события)
     * @param filterSearchBots true - фильтровать записи от запросов поисковых роботов
     * @throws IllegalArgumentException если события лога имеют некорректное описание
     */
    public RecordParser(List<Event> events, boolean filterSearchBots) throws IllegalArgumentException {
        for (Event e : events) {
            if (null == e.getPattern()
                    || e.getPattern().isEmpty()
                    || null == e.getType()
                    || e.getType().isEmpty())
                throw new IllegalArgumentException();

            try {
                if (!filterSearchBots)
                    matchersMap.put(e.getType(), Pattern.compile(e.getPattern()).matcher(""));
                else
                    matchersMap.put(e.getType(), Pattern.compile(e.getPattern() + "(?!.+(?:"
                            + UserAgentRegexp.any() + "))").matcher(""));
            } catch (PatternSyntaxException ex) {
                throw new IllegalArgumentException(ex.getMessage(), ex);
            }
        }
        typesSet = matchersMap.keySet();
    }

    /**
     * распарсить запись
     * @param record запись
     * @return объект, соответствующуй записи, или null в случае отсутствия соответствия
     */
    public LogEvent parse(String record) {
        for (String e : typesSet) {
            Matcher matcher = matchersMap.get(e).reset(record);
            if (matcher.find()) {
                return new LogEvent(matcher.group(1), e);
            }
        }
        return null;
    }

    private Map<String, Matcher> matchersMap = new HashMap<String, Matcher>();
    private Set<String> typesSet;

    /**
     * Регулярное выражение, соответствующее строке User Agent поисковых роботов
     */
    private static class UserAgentRegexp {

        /**
         * вернуть regexp, соответствующий User Agent'у любого наиболее популярного робота
         * @return regexp
         */
        public static String any() {
            if (null != anyStr)
                return anyStr;

            StringBuilder stringBuilder = new StringBuilder();
            Collection<String> expressions = exps.values();
            Iterator<String> iterator = expressions.iterator();
            while (iterator.hasNext()) {
                stringBuilder.append(iterator.next());
                if (iterator.hasNext())
                    stringBuilder.append("|");
            }
            anyStr = stringBuilder.toString();
            return anyStr;
        }

        private static Map<String, String> exps = new HashMap<String, String>();
        private static String anyStr = null;

        static {
            exps.put("Google", "Googlebot");
            exps.put("Yahoo", "Yahoo! Slurp");
            exps.put("Baidu", "Baiduspider");
            exps.put("Bing", "bingbot");
            exps.put("Yandex", "Yandex");
        }
    }
}
