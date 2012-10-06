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

import com.glines.socketio.common.ConnectionState;
import com.glines.socketio.common.DisconnectReason;
import com.glines.socketio.common.SocketIOException;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
class DefaultSession implements SocketIOSession {

    private static final int SESSION_ID_LENGTH = 20;
    private static final Logger LOGGER = Logger.getLogger(DefaultSession.class.getName());

    private final SocketIOSessionManager socketIOSessionManager;
    private final String sessionId;
    private final AtomicLong messageId = new AtomicLong(0);
    private final Map<String, Object> attributes = new ConcurrentHashMap<String, Object>();

    private LinkedHashMap<String,String> objHandshake = new LinkedHashMap<String,String>();

    private SocketIOInbound inbound;
    private TransportHandler handler;
    private ConnectionState state = ConnectionState.CONNECTING;
    private long hbDelay;
    private SessionTask hbDelayTask;
    private long timeout;
    private SessionTask timeoutTask;
    private boolean timedout;
    private String closeId;
    
    long lStartTime = (long)0;

    DefaultSession(SocketIOSessionManager socketIOSessionManager, SocketIOInbound inbound, String sessionId, LinkedHashMap<String,String> objHandshake) {
        this.socketIOSessionManager = socketIOSessionManager;
        this.inbound = inbound;
        this.sessionId = sessionId;
        this.objHandshake = objHandshake;
        if (LOGGER.isLoggable(Level.FINE))
            LOGGER.log(Level.FINE, "DefaultSession was created.");
    }

    @Override
    public void setAttribute(String key, Object val) {
        attributes.put(key, val);
    }

    @Override
    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    @Override
    public String getSessionId() {
        return sessionId;
    }
    
    public LinkedHashMap<String,String> getHandshake() {
        return objHandshake;
    }
    @Override
    public ConnectionState getConnectionState() {
        return state;
    }

    @Override
    public SocketIOInbound getInbound() {
        return inbound;
    }

    @Override
    public TransportHandler getTransportHandler() {
        return handler;
    }

    private void onTimeout() {
        if (LOGGER.isLoggable(Level.FINE))
            LOGGER.log(Level.FINE, "Session[" + sessionId + "]: onTimeout");
        if (!timedout) {
            timedout = true;
            state = ConnectionState.CLOSED;
            onDisconnect(DisconnectReason.TIMEOUT);
            handler.abort();
        }
    }

    @Override
    public void startTimeoutTimer() {
    	if(lStartTime != 0)
    		clearTimeoutTimer();
        lStartTime = new Date().getTime(); //start time
        if (!timedout && timeout > 0) {
            timeoutTask = scheduleTask(new Runnable() {
                @Override
                public void run() {
                    DefaultSession.this.onTimeout();
                }
            }, timeout);
        }
    }

    @Override
    public void clearTimeoutTimer() {
        if (timeoutTask != null) {
            timeoutTask.cancel();
            timeoutTask = null;
        }
        if (LOGGER.isLoggable(Level.FINE))
            LOGGER.log(Level.FINE, "heartbeat timeout was resetted. ping/pong dualing time is "+
            		Long.toString((long) new Date().getTime() - lStartTime) + " milliseconds.");
        lStartTime = (long)0;
    }

    private void sendPing() {
        String data = "" + messageId.incrementAndGet();
        try {
        	handler.sendMessage(new SocketIOFrame(
        			SocketIOFrame.FrameType.HEARTBEAT, SocketIOFrame.FrameType.MESSAGE.value(), data));
        } catch (SocketIOException e) {
            if (LOGGER.isLoggable(Level.FINE))
                LOGGER.log(Level.FINE, "handler.sendMessage failed: ", e);
            handler.abort();
        }
        startTimeoutTimer();
    }

    @Override
    public void startHeartbeatTimer() {
        clearHeartbeatTimer();
        if (!timedout && hbDelay > 0) {
            hbDelayTask = scheduleTask(new Runnable() {
                @Override
                public void run() {
                    sendPing();
                }
            }, hbDelay);
        }
    }

    @Override
    public void clearHeartbeatTimer() {
        if (hbDelayTask != null) {
            hbDelayTask.cancel();
            hbDelayTask = null;
        }
    }

    @Override
    public void setHeartbeat(long delay) {
        hbDelay = delay;
    }

    @Override
    public long getHeartbeat() {
        return hbDelay;
    }

    @Override
    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    @Override
    public long getTimeout() {
        return timeout;
    }

    @Override
    public void startClose() {
        state = ConnectionState.CLOSING;
        closeId = "server";
        try {
            handler.sendMessage(new SocketIOFrame(SocketIOFrame.FrameType.CLOSE, 0, closeId));
        } catch (SocketIOException e) {
            if (LOGGER.isLoggable(Level.FINE))
                LOGGER.log(Level.FINE, "handler.sendMessage failed: ", e);
            handler.abort();
        }
    }

    @Override
    public void onMessage(SocketIOFrame message) {
        //LOGGER.log(Level.INFO, "Session[" + sessionId + "] with state = "+state+" on "+ this.toString());
        switch (message.getFrameType()) {
            case CONNECT:
                onPing(message.getData());
            case HEARTBEAT:
                // Ignore this message type as they are only intended to be from server to client.
                if (LOGGER.isLoggable(Level.FINE))
                    LOGGER.log(Level.FINE, "Session[" + sessionId + "]: HEARTBEAT: " + message.getData());
                onPong("");
                startHeartbeatTimer();
                break;
            case CLOSE:
                if (LOGGER.isLoggable(Level.FINE))
                    LOGGER.log(Level.FINE, "Session[" + sessionId + "]: CLOSE: " + message.getData());
                onClose(message.getData());
                break;
            case MESSAGE:
                if (LOGGER.isLoggable(Level.FINE))
                    LOGGER.log(Level.FINE, "Session[" + sessionId + "]: MESSAGE: " + message.getData());
                onMessage(message.getFrameType().value(), message.getData());
                break;
            case JSON_MESSAGE:
                if (LOGGER.isLoggable(Level.FINE))
                    LOGGER.log(Level.FINE, "Session[" + sessionId + "]: JSON: " + message.getData());
                onMessage(message.getFrameType().value(), message.getData());
                break;
            case EVENT:
                if (LOGGER.isLoggable(Level.FINE))
                    LOGGER.log(Level.FINE, "Session[" + sessionId + "]: EVENT: " + message.getData());
                onMessage(message.getFrameType().value(), message.getData());
                break;
            default:
                // Ignore unknown message types
                break;
        }
    }

    //@Override
    public void onConnect(String data) {
        try {
            handler.sendMessage(new SocketIOFrame(SocketIOFrame.FrameType.CONNECT, 0, data));
        } catch (SocketIOException e) {
            if (LOGGER.isLoggable(Level.FINE))
                LOGGER.log(Level.FINE, "handler.sendMessage failed: ", e);
            handler.abort();
        }
    }
    @Override
    public void onPing(String data) {
        try {
        	handler.sendMessage(new SocketIOFrame(
        			SocketIOFrame.FrameType.HEARTBEAT, SocketIOFrame.FrameType.MESSAGE.value(), data));
        } catch (SocketIOException e) {
            if (LOGGER.isLoggable(Level.FINE))
                LOGGER.log(Level.FINE, "handler.sendMessage failed: ", e);
            handler.abort();
        }
    }

    @Override
    public void onPong(String data) {
        clearTimeoutTimer();
    }

    @Override
    public void onClose(String data) {
        if (state == ConnectionState.CLOSING) {
            if (closeId != null && closeId.equals(data)) {
                state = ConnectionState.CLOSED;
                onDisconnect(DisconnectReason.CLOSED);
                handler.abort();
            } else {
                try {
                    handler.sendMessage(new SocketIOFrame(SocketIOFrame.FrameType.CLOSE, 0, data));
                } catch (SocketIOException e) {
                    if (LOGGER.isLoggable(Level.FINE))
                        LOGGER.log(Level.FINE, "handler.sendMessage failed: ", e);
                    handler.abort();
                }
            }
        } else {
            state = ConnectionState.CLOSING;
            try {
                handler.sendMessage(new SocketIOFrame(SocketIOFrame.FrameType.CLOSE, 0, data));
                handler.disconnectWhenEmpty();
                if ("client".equals(data))
                    onDisconnect(DisconnectReason.CLOSED_REMOTELY);
            } catch (SocketIOException e) {
                if (LOGGER.isLoggable(Level.FINE))
                    LOGGER.log(Level.FINE, "handler.sendMessage failed: ", e);
                handler.abort();
            }
        }
    }

    @Override
    public SessionTask scheduleTask(Runnable task, long delay) {
        final Future<?> future = socketIOSessionManager.executor.schedule(task, delay, TimeUnit.MILLISECONDS);
        return new SessionTask() {
            @Override
            public boolean cancel() {
                return future.cancel(false);
            }
        };
    }

    @Override
    public void onConnect(TransportHandler handler) {
        //LOGGER.log(Level.INFO, "Session[" + sessionId + "] with state = "+state+" on "+ this.toString());
        if (handler == null) {
            state = ConnectionState.CLOSED;
            inbound = null;
            socketIOSessionManager.socketIOSessions.remove(sessionId);
        } else if (this.handler == null) {
            this.handler = handler;
            if (inbound == null) {
            	LOGGER.log(Level.INFO,"hander was aborted.");
                state = ConnectionState.CLOSED;
                handler.abort();
            } else {
                try {
                    state = ConnectionState.CONNECTED;
                    inbound.onConnect(handler);
                } catch (Throwable e) {
                    if (LOGGER.isLoggable(Level.WARNING))
                        LOGGER.log(Level.WARNING, "Session[" + sessionId + "]: Exception thrown by SocketIOInbound.onConnect()", e);
                    state = ConnectionState.CLOSED;
                    handler.abort();
                }
            }
        } else {
            handler.abort();
        }
    }

    @Override
	public void onMessage(int frameType, String message) {
		if (inbound != null) {
			switch (frameType) {
			case 3: // SocketIOFrame.FrameType.MESSAGE.value():
			case 4: // SocketIOFrame.FrameType.JSON_MESSAGE.value():
				if (LOGGER.isLoggable(Level.FINE))
					LOGGER.log(Level.FINE, "Session[" + sessionId
							+ "]: message header is \"3:\" (" + message + ")");
				// i don't know how to send messages with "4:" header 
				inbound.onMessage("message", message);
				break;
			case 5: // SocketIOFrame.FrameType.EVENT.value()
				if (LOGGER.isLoggable(Level.FINE))
					LOGGER.log(Level.FINE, "Session[" + sessionId
							+ "]: message header is \"5:\" (" + message + ")");
				try {
					// try calling new-style method (all call will be treated as
					// json).
					// TODO get key like "message" with parsing message as json.
					// TODO is it great if we have no onMessage declarations on
					// interface?
					Pattern KEY_PATTERN = Pattern
							.compile("\\{\"name\":\"([^\"]+)\",\"args\":\\[(.*)\\]\\}");
					String strKey = "message";
					try {
						Matcher a = KEY_PATTERN.matcher(message);
						if (a.find()) {
							strKey = a.group(1);
							message = a.group(2);
							// for premitive value.
							if (message.startsWith("\"")) {
								message = message.substring(1,
										message.length() - 1);
								// checking authed event-name
								for (String b : inbound.setEventnames()) {
									if (strKey.equals(b))
										inbound.onMessage(strKey, message);
								}
							}
							if (LOGGER.isLoggable(Level.FINE))
								LOGGER.log(Level.FINE, "Session[" + sessionId
										+ "]: matcher got (" + strKey + ","
										+ message + ")");
						} else {
							if (LOGGER.isLoggable(Level.FINE))
								LOGGER.log(Level.FINE, "Session[" + sessionId
										+ "]: matcher cannot be matched ("
										+ message + ")");
						}
					} catch (Exception e) {
						if (LOGGER.isLoggable(Level.FINE))
							LOGGER.log(Level.FINE, "Session[" + sessionId
									+ "]: matcher got an exception (" + strKey
									+ "," + message + ")");
					}
				} catch (Throwable e) {
					if (LOGGER.isLoggable(Level.WARNING))
						LOGGER.log(
								Level.WARNING,
								"Session["
										+ sessionId
										+ "]: Exception thrown by SocketIOInbound.onMessage()",
								e);
				}
			}
		}
	}

    @Override
    public void onDisconnect(DisconnectReason reason) {
        if (LOGGER.isLoggable(Level.FINE))
            LOGGER.log(Level.FINE, "Session[" + sessionId + "]: onDisconnect: " + reason);
        clearTimeoutTimer();
        clearHeartbeatTimer();
        if (inbound != null) {
            state = ConnectionState.CLOSED;
            try {
                inbound.onDisconnect(reason, null);
            } catch (Throwable e) {
                if (LOGGER.isLoggable(Level.WARNING))
                    LOGGER.log(Level.WARNING, "Session[" + sessionId + "]: Exception thrown by SocketIOInbound.onDisconnect()", e);
            }
            inbound = null;
        }
    }

    @Override
    public void onShutdown() {
        if (LOGGER.isLoggable(Level.FINE))
            LOGGER.log(Level.FINE, "Session[" + sessionId + "]: onShutdown");
        if (inbound != null) {
            if (state == ConnectionState.CLOSING) {
                if (closeId != null) {
                    onDisconnect(DisconnectReason.CLOSE_FAILED);
                } else {
                    onDisconnect(DisconnectReason.CLOSED_REMOTELY);
                }
            } else {
                onDisconnect(DisconnectReason.ERROR);
            }
        }
        socketIOSessionManager.socketIOSessions.remove(sessionId);
    }
}
