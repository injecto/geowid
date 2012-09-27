package com.ecwid.geowid.daemon.tests;

import com.ecwid.geowid.daemon.RecordParser;
import com.ecwid.geowid.daemon.TailReader;
import com.ecwid.geowid.daemon.settings.Event;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.util.LinkedList;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

/**
 * Тест конвейера-обработчика записей лога:
 * классов TailReader и RecordParser
 */
public class ConveyorTest {
    @Test
    public void testCollect() throws Exception {
        FileWriter writer = new FileWriter("test.log");
        writer.write("0\n");
        writer.flush();

        TailReader tailReader = new TailReader(".", "test.log", 1000);
        Thread.sleep(1000);
        writer.write("1\n");
        writer.write("2\n");
        writer.flush();
        Thread.sleep(1000);
        assertEquals("1", tailReader.getRecordsQueue().take());
        assertEquals("2", tailReader.getRecordsQueue().take());

        Event event = new Event("ip", "\\b((?:\\d{1,3}\\.){3}\\d{1,3})\\b");
        LinkedList<Event> events = new LinkedList<Event>();
        events.add(event);

        RecordParser parser = new RecordParser(tailReader.getRecordsQueue(), events, false);
        writer.write("some string\n");
        writer.flush();
        Thread.sleep(1000);
        assertEquals(0, parser.getIpQueue().size());
        writer.write("255.255.255.0\n");
        writer.flush();
        Thread.sleep(1000);
        assertTrue("255.255.255.0".equals(parser.getIpQueue().take().getIp()));

        parser.close();
        tailReader.close();

        writer.close();
        new File("test.log").delete();
    }
}
