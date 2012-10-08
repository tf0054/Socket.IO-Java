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
package com.glines.socketio.server;

import java.util.LinkedHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class SocketIOSessionManager implements SessionManager {

    private final ConcurrentMap<String, SocketIOSession> socketIOSessions = new ConcurrentHashMap<String, SocketIOSession>();
    final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

    private static final Logger LOGGER = Logger.getLogger(SocketIOSessionManager.class.getName());

    @Override
    public SocketIOSession createSession(SocketIOInbound inbound, String sessionId, LinkedHashMap<String,String> objHandshake) {
        DefaultSession impl = new DefaultSession(this, inbound, sessionId, objHandshake);
        socketIOSessions.put(impl.getSessionId(), impl);
        return impl;
    }

    @Override
    public SocketIOSession getSession(String sessionId) {
        return socketIOSessions.get(sessionId);
    }

	@Override
	public void removeSession(String sessionId) {
		socketIOSessions.remove(sessionId);
        if (LOGGER.isLoggable(Level.WARNING))
            LOGGER.log(Level.WARNING, "Session[" + sessionId + "]: session("+sessionId+") was removed. "+socketIOSessions.size());
	}
}
