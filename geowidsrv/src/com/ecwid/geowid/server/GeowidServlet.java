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
        timeOut = Long.parseLong(getServletConfig().getInitParameter("time-out"));

        buffer = new PointsBuffer(Integer.parseInt(getServletConfig().getInitParameter("chunk-size")),
                getServletConfig().getInitParameter("log-stream-host"),
                Integer.parseInt(getServletConfig().getInitParameter("log-stream-port")));

        buffer.addListener(new IPointListener() {
            @Override
            public void onSlice(String slice) {
                synchronized (continuations) {
                    for (Map.Entry<String, Continuation> entry : continuations.entrySet()) {
                        entry.getValue().setAttribute(resultAttribute, slice);
                        try {
                            entry.getValue().resume();
                        } catch (IllegalStateException e) {
                            logger.warn("Continuation with reqID={} not suspended", entry.getKey());
                        }
                    }
                    continuations.clear();
                }
            }
        });

        buffer.fillBuffer();
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
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
                    } catch (IllegalStateException e) {
                        logger.warn("Continuation with reqID={} can't be suspended", reqId);
                        resp.sendError(500);
                        return;
                    }
                    continuations.put(reqId, continuation);
                } else
                    if (continuation.isExpired()) {
                        synchronized (continuations) {
                            continuations.remove(reqId);
                        }
                        resp.setContentType(contentType);
                        resp.getWriter().println(emptyResult);
                        return;
                    } else {
                        resp.sendError(400, "Request ID conflict");
                        return;
                    }
            }
        } else {
            resp.setContentType(contentType);
            resp.getWriter().println((String) result);
        }
    }

    private long timeOut;

    private final String resultAttribute = "result";
    private final String idParameterName = "id";
    private final String contentType = "application/json;charset=utf-8";
    private final String emptyResult = "[]";

    private final Map<String, Continuation> continuations = new HashMap<String, Continuation>();
    private PointsBuffer buffer = null;

    private static final Logger logger = LogManager.getLogger(GeowidServlet.class);
}
