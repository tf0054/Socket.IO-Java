/**
 * The MIT License
 * Copyright (c) 2010 Tad Glines
 *
 * Contributors: Ovea.com, Mycila.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.glines.socketio.server.transport;

import com.glines.socketio.server.*;
import com.glines.socketio.util.Web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class AbstractHttpTransport extends AbstractTransport {

    private static final Logger LOGGER = Logger.getLogger(AbstractHttpTransport.class.getName());
    public static final String SESSION_KEY = AbstractHttpTransport.class.getName() + ".Session";

    @Override
    public final void handle(HttpServletRequest request,
                             HttpServletResponse response,
                             Transport.InboundFactory inboundFactory,
                             SessionManager sessionFactory) throws IOException {
        if (LOGGER.isLoggable(Level.FINE))
            LOGGER.fine("Handling request " + request.getRequestURI() + " by " + getClass().getName());

        SocketIOSession session = null;
        String sessionId = Web.extractSessionId(request);
        if (sessionId != null && sessionId.length() > 0) {
            session = sessionFactory.getSession(sessionId);
        }

        if (session != null) {
            TransportHandler handler = session.getTransportHandler();
            if (handler != null) {
                handler.handle(request, response, session);
            } else {
                session.onShutdown();
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        } else {
            if ("GET".equals(request.getMethod())) {
                session = connect(request, response, inboundFactory,
                                  sessionFactory, sessionId);
                if (session == null) {
                    response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                }
            } else {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            }
        }
    }

    private SocketIOSession connect(HttpServletRequest request,
                                    HttpServletResponse response,
                                    InboundFactory inboundFactory,
                                    SessionManager sessionFactory,
                                    String sessionId) throws IOException {
        SocketIOInbound inbound = inboundFactory.getInbound(request);
        if (inbound != null) {
            if (sessionId == null)
                sessionId = request.getSession().getId().toString();
			Enumeration<String> headerNames = request.getHeaderNames();
			LinkedHashMap<String,String> objHandshake = new LinkedHashMap<String,String>();
			while (headerNames.hasMoreElements()) {
				String headerName = (String) headerNames.nextElement();
				objHandshake.put(headerName, request.getHeader(headerName));
			}
            SocketIOSession session = sessionFactory.createSession(inbound, sessionId,objHandshake);
            // get and init data handler
            DataHandler dataHandler = newDataHandler(session);
            dataHandler.init(getConfig());
            // get and init transport handler
            TransportHandler transportHandler = newHandler(ConnectableTransportHandler.class, session);
            ConnectableTransportHandler connectableTransportHandler = ConnectableTransportHandler.class.cast(transportHandler);
            connectableTransportHandler.setDataHandler(dataHandler);
            transportHandler.init(getConfig());
            // connect transport to session
            connectableTransportHandler.connect(request, response);
            return session;
        }
        return null;
    }

    protected abstract DataHandler newDataHandler(SocketIOSession session);
}
