package com.ecwid.geowid.daemon;

/**
 * Запись события лога в виде IP-адреса и типа события
 */
public class Ip {

    public Ip(String ip, String type) {
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
