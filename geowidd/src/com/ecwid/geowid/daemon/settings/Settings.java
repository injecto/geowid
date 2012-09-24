package com.ecwid.geowid.daemon.settings;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * Настройки приложения
 */
@XmlRootElement(name = "settings")
public class Settings {

    public String getLogFileCatalog() {
        return logFileCatalog;
    }

    public String getLogFilePattern() {
        return logFilePattern;
    }

    public long getUpdatePeriod() {
        return updatePeriod;
    }

    public String getCacheFilePath() {
        return cacheFilePath;
    }

    public long getCacheRecordTtl() {
        return cacheRecordTtl;
    }

    public int getPort() {
        return port;
    }

    public List<Event> getEvents() {
        return events;
    }

    @XmlElement(name = "log-file-catalog")
    private String logFileCatalog;
    @XmlElement(name = "log-file-pattern")
    private String logFilePattern;
    @XmlElement(name = "update-period")
    private long updatePeriod;
    @XmlElement(name = "ip-resolver-cache-file")
    private String cacheFilePath;
    @XmlElement(name = "ip-resolver-rec-ttl")
    private long cacheRecordTtl;
    @XmlElement(name = "connection-port")
    private int port;
    @XmlElementWrapper(name = "events")
    @XmlElement(name = "event")
    private List<Event> events;
}
