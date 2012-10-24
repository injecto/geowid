/*
 * Copyright (c) 2012, Creative Development LLC
 * Available under the New BSD license
 * see http://github.com/injecto/geowid for details
 */

package com.ecwid.geowid.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.continuation.Continuation;
import org.eclipse.jetty.continuation.ContinuationSupport;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Сервлет, реализующий асинхронную обработку запросов новых точек карты
 */
public class GeowidServlet extends HttpServlet {

    @Override
    public void init() throws ServletException {
        String keyFilePath = getInitParameter("pub-key");
        if (null == keyFilePath) {
            logger.fatal("Key file path not defined in web.xml");
            throw new ServletException("Incorrect settings");
        }

        int port, chunkSize;
        try {
            timeOut = Long.parseLong(getInitParameter("time-out"));
            port = Integer.parseInt(getInitParameter("listen-port"));
            chunkSize = Integer.parseInt(getInitParameter("chunk-size"));
            buffer = new PointsBuffer(chunkSize, new PointsProvider(port, keyFilePath));
        } catch (NumberFormatException e) {
            logger.fatal("Some of parameter in web.xml incorrect specified", e);
            throw new ServletException("Incorrect settings");
        } catch (IOException e) {
            logger.fatal(e.getMessage(), e);
            throw new ServletException("Initialization error");
        }

        buffer.addListener(new IPointListener() {
            @Override
            public void onSlice(String slice) {
                synchronized (continuations) {
                    for (Continuation continuation : continuations.values()) {
                        continuation.setAttribute(resultAttribute, slice);
                        try {
                            continuation.resume();
                        } catch (IllegalStateException e) {
                            // ok
                        }
                    }
                    continuations.clear();
                }
            }
        });

        buffer.fillBuffer();
        logger.info("Servlet initialized");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String reqId = req.getParameter(idParameterName);

        if (null == reqId) {
            resp.sendError(400, "Request ID needed");
            logger.info("Request without ID rejected [{}]", req.getRequestURI());
            return;
        }

        Object result = req.getAttribute(resultAttribute);

        if (null == result) {
            Continuation continuation = ContinuationSupport.getContinuation(req);
            synchronized (continuations) {
                if (!continuations.containsKey(reqId)) {
                    continuation.setTimeout(timeOut);
                    try {
                        continuation.suspend();
                        continuations.put(reqId, continuation);
                    } catch (IllegalStateException e) {
                        logger.warn("Continuation with reqID={} can't be suspended", reqId);
                        resp.sendError(500);
                    }
                } else
                if (continuation.isExpired()) {
                    synchronized (continuations) {
                        continuations.remove(reqId);
                    }
                    resp.setContentType(contentType);
                    resp.getWriter().println(emptyResult);
                } else {
                    resp.sendError(400, "Request ID conflict");
                }
            }
        } else {
            resp.setContentType(contentType);
            resp.getWriter().println((String) result);
        }
    }

    @Override
    public void destroy() {
        if (!buffer.close())
            logger.warn("Buffer's thread not die. It's sorrowfully");

        super.destroy();

        logger.info("Servlet destroyed");
    }

    private long timeOut;

    private static final String resultAttribute = "result";
    private static final String idParameterName = "id";
    private static final String contentType = "application/json;charset=utf-8";
    private static final String emptyResult = "[]";

    private final Map<String, Continuation> continuations = new HashMap<String, Continuation>();
    private transient PointsBuffer buffer;

    private static final Logger logger = LogManager.getLogger(GeowidServlet.class);
}
