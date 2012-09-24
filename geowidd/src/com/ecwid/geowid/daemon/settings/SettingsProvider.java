package com.ecwid.geowid.daemon.settings;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;

/**
 * Провайдер настроек, хранимых в XML
 */
public class SettingsProvider {
    @SuppressWarnings("unchecked")
    public static Settings getSettings(String settingsFilePath) {
        try {
            File settingsFile = new File(settingsFilePath);
            JAXBContext context = JAXBContext.newInstance(Settings.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            return Settings.class.cast(unmarshaller.unmarshal(settingsFile));
        } catch (JAXBException e) {
            return null;
        }
    }
}
