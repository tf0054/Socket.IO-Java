/**
 * The MIT License
 * Copyright (c) 2012 Takeshi NAKANO
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
package com.glines.socketio.sample.pointers;

import java.io.IOException;
import java.util.HashMap;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import com.glines.socketio.common.DisconnectReason;
import com.glines.socketio.server.SocketIOInbound;
import com.glines.socketio.server.SocketIOOutbound;
import com.glines.socketio.server.SocketIOServlet;
import com.glines.socketio.server.transport.jetty.JettyWebSocketTransportHandler;

import com.google.gson.Gson;

public class PointersShareServlet extends SocketIOServlet {
	private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(JettyWebSocketTransportHandler.class.getName());

    //private Queue<PointersConnectionImpl> connections = new ConcurrentLinkedQueue<PointersConnectionImpl>();

	private class PointersConnectionImpl implements SocketIOInbound {
		private volatile SocketIOOutbound outbound = null;
		private Intercepter objIntercepter = null;
     	private Gson gson = new Gson();
     	private HashMap<String, int[]> hashCache = new HashMap<String, int[]>(); 

		PointersConnectionImpl(Intercepter a){
			this.objIntercepter = a;
		}

		@Override
		public void onConnect(SocketIOOutbound outbound) {
			this.outbound = outbound;
			objIntercepter.clients.offer(outbound);
			for (String a : hashCache.keySet()) {
				Pojo b = new Pojo(a, hashCache.get(a)[0], hashCache.get(a)[1]);
				String strJson = gson.toJson(b);
				objIntercepter.emit(this.outbound, "updatePointer", strJson);
			}
		}
	
		public void onMessage(String strKey, String message) {
        	if(strKey.equals("updatePointer")){
        	    Pojo after = gson.fromJson(message, Pojo.class);
        	    int[] intTmp = {after.x, after.y};
        	    hashCache.put(after.clientId, intTmp);
        	    objIntercepter.broadcast(this.outbound, "updatePointer", message);
        	} else if(strKey.equals("clearPointer")){
        	    Pojo after = gson.fromJson(message, Pojo.class);
        	    hashCache.remove(after.clientId);
        	    objIntercepter.broadcast(this.outbound, "clearPointer", message);
        	} else{
                if (LOGGER.isLoggable(Level.FINE))
                    LOGGER.log(Level.FINE, this + "cannot parse with gson: "+message);
        	}
		}
		
		@Override
		public void onDisconnect(DisconnectReason reason, String errorMessage) {
			objIntercepter.clients.remove(outbound);
			this.outbound = null;
		}
                
        @Override
        public String[] setEventnames() {
        	return new String[]{"updatePointer", "clearPointer"};
        }

        public void setNamespace(String a) {
        	objIntercepter.setNamespace(a);
        }
	}

	@Override
	protected SocketIOInbound doSocketIOConnect(HttpServletRequest request) {
		Intercepter objIntercepter = new Intercepter();
		
		// make target pairs (url, object for that)
		HashMap<String, Object> targets = new HashMap<String, Object>();
		targets.put("/", new PointersConnectionImpl(objIntercepter));
		
		// set target pairs to intercepter
		objIntercepter.setIntercepter(targets);

		return objIntercepter.getProxyObj();
	}

}
