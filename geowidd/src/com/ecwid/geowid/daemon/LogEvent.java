/*
 * Copyright (c) 2012, Creative Development LLC
 * Available under the New BSD license
 * see http://github.com/injecto/geowid for details
 */

package com.ecwid.geowid.daemon;

/**
 * Запись события лога в виде IP-адреса и типа события
 */
public class LogEvent {

    public LogEvent(String ip, String type) {
        this.ip = ip;
        this.type = type;
    }

    public String getIp() {
        return ip;
    }

    public String getType() {
        return type;
    }

    private final String ip;
    private final String type;
}
