/*
 * Copyright (c) 2012, Creative Development LLC
 * Available under the New BSD license
 * see http://github.com/injecto/geowid for details
 */

package com.ecwid.geowid.daemon.settings;

import javax.xml.bind.annotation.XmlElement;

/**
 * Описание типа записи в логе
 */
public class Event {

    public Event() { }

    public Event(String type, String pattern) {
        this.type = type;
        this.pattern = pattern;
    }

    public String getType() {
        return type;
    }

    public String getPattern() {
        return pattern;
    }

    @XmlElement(name = "type")
    private String type;
    @XmlElement(name = "pattern")
    private String pattern;
}
