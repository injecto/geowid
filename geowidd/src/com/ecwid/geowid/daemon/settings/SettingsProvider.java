/*
 * Copyright (c) 2012, Creative Development LLC
 * Available under the New BSD license
 * see http://github.com/injecto/geowid for details
 */

package com.ecwid.geowid.daemon.settings;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;

/**
 * Провайдер настроек, хранимых в XML
 */
public class SettingsProvider {
    /**
     * загрузить настройки из XML и вернуть объект настроек
     * @param settingsFilePath путь к файлу настроек
     * @return настройки
     * @throws JAXBException в случае ошибок парсинга файла
     */
    @SuppressWarnings("unchecked")
    public static Settings getSettings(String settingsFilePath) throws JAXBException {
        File settingsFile = new File(settingsFilePath);
        JAXBContext context = JAXBContext.newInstance(Settings.class);
        Unmarshaller unmarshaller = context.createUnmarshaller();
        return Settings.class.cast(unmarshaller.unmarshal(settingsFile));
    }
}
