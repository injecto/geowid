package com.ecwid.geowid.daemon.utils;

import java.util.regex.Pattern;

/**
 * Матчер на соответствие строки лога User-Agent'у какой-либо поисковой машины
 */
public class SearchBotAgent {

    /**
     * добавить regexp, определяющий соответствие записи User-Agent'у поискового движка
     * @param regExp regexp
     * @return ссылка на текущий объект
     */
    public SearchBotAgent addAgentRegExp(String regExp) {
        if (generalRegExp.toString().isEmpty())
            generalRegExp.append(regExp);
        else
            generalRegExp.append("|")
                         .append(regExp);

        pattern = Pattern.compile(generalRegExp.toString());
        return this;
    }

    /**
     * очистить шаблон соответствия
     */
    public void clearPattern() {
        generalRegExp.delete(0, generalRegExp.length()-1);
    }

    /**
     * проверить на соответствие поисковому боту
     * @param logRecord запись лога
     * @return true если соответствует, иначе false
     */
    public boolean isSearchBot(String logRecord) {
        if (generalRegExp.toString().isEmpty())
            return false;

        return pattern.matcher(logRecord).matches();
    }

    private StringBuilder generalRegExp = new StringBuilder();
    private Pattern pattern;

    //
    //  RegExp'ы для наиболее популярных движков
    //
    public static final String Google = ".+\\bGooglebot/\\d+\\.?\\d*.+";
    public static final String Yahoo = ".+\\bMozilla/5\\.0 \\(compatible; Yahoo! Slurp.+\\).+";
    public static final String Baidu = ".+\\bBaiduspider\\+.+";
    public static final String Bing = ".+\\bMozilla/5\\.0 \\(compatible; bingbot/\\d+\\.?\\d*;.+\\).+";
    public static final String Yandex =".+\\bMozilla/5\\.0 \\(compatible; Yandex.+\\).+";
}
