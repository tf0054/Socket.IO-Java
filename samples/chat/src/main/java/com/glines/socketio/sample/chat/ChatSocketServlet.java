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
package com.glines.socketio.sample.chat;

import com.glines.socketio.common.DisconnectReason;
import com.glines.socketio.common.SocketIOException;
import com.glines.socketio.server.SocketIOFrame;
import com.glines.socketio.server.SocketIOInbound;
import com.glines.socketio.server.SocketIOOutbound;
import com.glines.socketio.server.SocketIOServlet;
import com.glines.socketio.util.JdkOverLog4j;
import com.google.gson.Gson;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Collections;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class ChatSocketServlet extends SocketIOServlet {
    private static final long serialVersionUID = 1L;
    private AtomicInteger ids = new AtomicInteger(1);
    private Queue<ChatConnection> connections = new ConcurrentLinkedQueue<ChatConnection>();

    @Override
    public void init(ServletConfig config) throws ServletException {
        JdkOverLog4j.install();
        super.init(config);
    }

    private class ChatConnection implements SocketIOInbound {
        private volatile SocketIOOutbound outbound = null;
        private Integer sessionId = ids.getAndIncrement();

        @Override
        public void onConnect(SocketIOOutbound outbound) {
            this.outbound = outbound;
            connections.offer(this);
            emit("welcome", "Welcome to Socket.IO Chat!");
            broadcast("announcement", sessionId + " connected");
        }

        @Override
        public void onDisconnect(DisconnectReason reason, String errorMessage) {
            this.outbound = null;
            connections.remove(this);
            broadcast("announcement", sessionId + " disconnected");
        }

        @Override
		public void onMessage(String strKey, String message) {
        	if(strKey.equals("message")){
	            if (message.equals("/rclose")) {
	                outbound.close();
	            } else if (message.equals("/rdisconnect")) {
	                outbound.disconnect();
	            } else if (message.startsWith("/sleep")) {
	                int sleepTime = 1;
	                String parts[] = message.split("\\s+");
	                if (parts.length == 2) {
	                    sleepTime = Integer.parseInt(parts[1]);
	                }
	                try {
	                    Thread.sleep(sleepTime * 1000);
	                } catch (InterruptedException e) {
	                    // Ignore
	                }
                    emit("message", "Slept for " + sleepTime + " seconds.");
	            } else if (message.startsWith("/burst")) {
	            	// send back burst message to the user.
	                int burstNum = 10;
	                String parts[] = message.split("\\s+");
	                if (parts.length == 2) {
	                    burstNum = Integer.parseInt(parts[1]);
	                }
                    for (int i = 0; i < burstNum; i++) {
                        emit("message", "Hi " + i + " " +
                                        "0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF" +
                                        "0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF" +
                                        "0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF" +
                                        "0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF" +
                                        "0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF" +
                                        "0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF" +
                                        "0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF" +
                                        "0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF"
                            );
                        try {
                            Thread.sleep(250);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    broadcast(strKey, "[\""+sessionId+"\",\"(bursting with server)\"");
	            } else {
	                broadcast(strKey, "[\""+sessionId+"\",\""+message+"\"]");
	            }
        	}
        }

        private void broadcast(String strKey, String message) {
            for (ChatConnection c : connections) {
                if (c != this) {
                    try {
                    	c.outbound.emitMessage(strKey, message);
                    } catch (IOException e) {
                        c.outbound.disconnect();
                    }
                }
            }
        }
        
        private void emit(String strKey, String message) {
            try {
            	outbound.emitMessage(strKey, message);
            } catch (IOException e) {
                outbound.disconnect();
            }
        }
        
		@Override
		public String[] setEventnames() {
        	return new String[]{"message","announcement","welcome"};
		}
    }

    @Override
    protected SocketIOInbound doSocketIOConnect(HttpServletRequest request) {
        return new ChatConnection();
    }

}
