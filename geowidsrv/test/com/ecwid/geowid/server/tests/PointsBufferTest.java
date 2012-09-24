package com.ecwid.geowid.server.tests;

import com.ecwid.geowid.server.IPointListener;
import com.ecwid.geowid.server.PointsBuffer;
import com.ecwid.geowid.utils.Point;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;

/**
 * Тест класса PointsBuffer
 */
public class PointsBufferTest {
    @Before
    public void init() {
        try {
            serverSocket = new ServerSocket(port);

            testPoints.add(new Point(1, 1, "1"));
            testPoints.add(new Point(2, 2, "2"));
            testPoints.add(new Point(3, 3, "3"));
            testPoints.add(new Point(4, 4, "4"));
            testPoints.add(new Point(5, 5, "5"));
        } catch (IOException e) {
            fail("Can't emulate a environment");
        }
    }

    @Test
    public void testPointsBuffer() throws Exception {
        Thread serverThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Socket clientSocket = serverSocket.accept();
                    ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());

                    for (Point point : testPoints)
                        out.writeObject(point);

                    out.close();
                    clientSocket.close();
                } catch (IOException e) {
                    fail("PointsBuffer can't connect to server");
                }
            }
        });
        serverThread.start();

        PointsBuffer buffer = new PointsBuffer(testPoints.size() / 2, "127.0.0.1", port);
        final int[] sliceCounter = {0};
        buffer.addListener(new IPointListener() {
            @Override
            public void onSlice(String slice) {
                sliceCounter[0]++;
            }
        });
        buffer.fillBuffer();
        Thread.sleep(1000);
        assertEquals(testPoints.size() / 2, sliceCounter[0]);
    }

    @After
    public void fin() {
        if (null != serverSocket)
            try {
                serverSocket.close();
            } catch (IOException e) {
                System.out.println("Oops, can't close server socket");
            }
    }

    private ServerSocket serverSocket;
    private int port = 9968;

    private List<Point> testPoints = new ArrayList<Point>();
}
