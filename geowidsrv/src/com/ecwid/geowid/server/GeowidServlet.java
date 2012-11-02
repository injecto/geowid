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
        try {
            timeOut = Long.parseLong(getInitParameter("time-out"));
        } catch (NumberFormatException e) {
            logger.fatal("Some of parameter in web.xml incorrect specified", e);
            throw new ServletException("Incorrect settings");
        }

        logger.info("Servlet initialized");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        synchronized (continuations) {
            for (Continuation continuation : continuations.values()) {
                continuation.setAttribute(resultAttribute, req.getParameter(requestKey));
                try {
                    continuation.resume();
                } catch (IllegalStateException e) {
                    // ok
                }
            }
            continuations.clear();
            resp.setStatus(HttpServletResponse.SC_OK);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String reqId = req.getParameter(idParameterName);

        if (null == reqId) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Request ID needed");
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
                        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    }
                } else
                if (continuation.isExpired()) {
                    synchronized (continuations) {
                        continuations.remove(reqId);
                    }
                    resp.setContentType(contentType);
                    resp.getWriter().println(emptyResult);
                } else {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Request ID conflict");
                }
            }
        } else {
            resp.setContentType(contentType);
            resp.getWriter().println((String) result);
        }
    }

    @Override
    public void destroy() {
        logger.info("Servlet destroyed");
    }

    private long timeOut;

    private static final String resultAttribute = "result";
    private static final String idParameterName = "id";
    private static final String contentType = "application/json;charset=utf-8";
    private static final String emptyResult = "[]";
    private static final String requestKey = "points";

    private final Map<String, Continuation> continuations = new HashMap<String, Continuation>();

    private static final Logger logger = LogManager.getLogger(GeowidServlet.class);
}
