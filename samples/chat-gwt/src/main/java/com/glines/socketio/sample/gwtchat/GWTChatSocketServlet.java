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
package com.glines.socketio.sample.gwtchat;

import com.glines.socketio.common.DisconnectReason;
import com.glines.socketio.server.SocketIOInbound;
import com.glines.socketio.server.SocketIOOutbound;
import com.glines.socketio.server.SocketIOServlet;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GWTChatSocketServlet extends SocketIOServlet {
    private static final long serialVersionUID = 1L;
    private AtomicInteger ids = new AtomicInteger(1);
    private Queue<GWTChatConnection> connections = new ConcurrentLinkedQueue<GWTChatConnection>();

	private Logger LOGGER = Logger.getLogger(this.getClass().getName());

    private class GWTChatConnection implements SocketIOInbound {
        private volatile SocketIOOutbound outbound = null;
        private Integer sessionId = ids.getAndIncrement();

        @Override
        public void onConnect(SocketIOOutbound outbound) {
            this.outbound = outbound;
            connections.offer(this);
			emit("welcome", "Welcome to GWT Chat!");
            broadcast("announcement", sessionId + " connected");
            
            // getting handshake information example
			String strKey = "";
			LinkedHashMap<String, String> objHandshake = outbound.getHandshake();
			Set<String> set = objHandshake.keySet();
			for (Iterator<String> iter = set.iterator(); iter.hasNext();) {
				strKey = (String) iter.next();
				//if(strKey.startsWith("Cookie") || strKey.startsWith("address"))
				LOGGER.info("Server.handshake: '" + strKey + "','"
						+ (String) objHandshake.get(strKey) + "'");
			}
        }

        @Override
        public void onDisconnect(DisconnectReason reason, String errorMessage) {
        	
            this.outbound = null;
            connections.remove(this);
            broadcast("announcement", sessionId + " disconnected ("+reason.toString()+","+errorMessage+")");
        }

        // v0.7- style - always used.
        public void onMessage(String strKey, String message) {
        	if(strKey.equals("message")){
        		LOGGER.info("Server.onMessage: '"+strKey+"','"+message+"'");
	        	
	            if (message.equals("/rclose")) {
	                outbound.close();
	            } else if (message.equals("/rdisconnect")) {
	                outbound.disconnect();
	            } else if (message.startsWith("/sleep")) {
	                int sleepTime = 1;
	                String parts[] = message.split("\\s+");
	                if (parts.length == 2) {
	                    sleepTime = Integer.parseInt(parts[1]);
		                try {
		                    Thread.sleep(sleepTime * 1000);
		                } catch (InterruptedException e) {
		                    // Ignore
		                }
	                	emit("message", "Slept for " + sleepTime + " seconds.");
	                }else{
	                	emit("message", "please input like '/sleep 5'.");
	                }
	            } else {
	            	/*
	            	JSONObject objOldMsg = JSONParser.parseLenient(message).isObject();
	            	JSONObject objNewMsg = JSONParser.parseLenient("{\"message\":[\""+sessionId.toString()+"\",\""+objOldMsg.get("message")+"\"]}").isObject();
	            	broadcast(messageType, objNewMsg.toString());
	            	*/
	            	
	            	// add sessionId (client id) on message.
	            	// TODO: change this code to general one..
	            	message = "[\""+sessionId.toString()+"\",\""+message+"\"]";
	            	broadcast("message", message);
	            }
        	}
        }

        // TODO this function should be treated similer with emitMessage.
        private void broadcast(String strKey, String message) {
            for (GWTChatConnection c : connections) {
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
        	return new String[]{"message"};
        }

		@Override
		public void setNamespace(String a) {
            if (LOGGER.isLoggable(Level.FINE))
                LOGGER.fine("No namespace with this servlet.");
		}

    }
    
    @Override
    protected SocketIOInbound doSocketIOConnect(HttpServletRequest request) {
        return new GWTChatConnection();
    }

}
