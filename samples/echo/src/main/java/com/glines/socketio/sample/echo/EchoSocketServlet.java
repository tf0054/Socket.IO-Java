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
package com.glines.socketio.sample.echo;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import com.glines.socketio.common.DisconnectReason;
import com.glines.socketio.server.SocketIOInbound;
import com.glines.socketio.server.SocketIOOutbound;
import com.glines.socketio.server.SocketIOServlet;
import com.glines.socketio.server.transport.jetty.JettyWebSocketTransportHandler;

public class EchoSocketServlet extends SocketIOServlet {
	private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(JettyWebSocketTransportHandler.class.getName());

	private class EchoConnectionImpl implements SocketIOInbound {
		private volatile SocketIOOutbound outbound = null;
		private Intercepter objIntercepter = null;
		
		EchoConnectionImpl(Intercepter a){
			this.objIntercepter = a;
		}

		@Override
		public void onConnect(SocketIOOutbound outbound) {
			this.outbound = outbound;
		}

		@Override
		public void onDisconnect(DisconnectReason reason, String errorMessage) {
				this.outbound = null;
			}

		@Override
		public void onMessage(String strKey, String message) {
            try {
            	outbound.sendMessage(message);
            } catch (IOException e) {
                outbound.disconnect();
            }
        }

        @Override
        public String[] setEventnames() {
        	return new String[]{"message"};
        }

        public void setNamespace(String a) {
        		objIntercepter.setNamespace(a);
        }
	}
	
	private class EchoConnectionImplWithAsterisk implements SocketIOInbound {
		private volatile SocketIOOutbound outbound = null;
		private Intercepter objIntercepter = null;
		
		EchoConnectionImplWithAsterisk(Intercepter a){
			this.objIntercepter = a;
		}
		
		@Override
		public void onConnect(SocketIOOutbound outbound) {
			this.outbound = outbound;
		}

		@Override
		public void onDisconnect(DisconnectReason reason, String errorMessage) {
				this.outbound = null;
			}

		@Override
		public void onMessage(String strKey, String message) {
            try {
            	outbound.sendMessage("*** "+message);
            } catch (IOException e) {
                outbound.disconnect();
            }
        }

        @Override
        public String[] setEventnames() {
        	return new String[]{"message"};
        }
        
        public void setNamespace(String a) {
        		objIntercepter.setNamespace(a);
        }
	}
	
    public class Intercepter implements InvocationHandler{
    	String strNamespace = "";
    	HashMap<String, Object> targets = new HashMap<String, Object>();
    	
    	public void setIntercepter(HashMap<String, Object> objects){
    		this.targets = objects;
    	}
    	
    	public void setNamespace(String a){
    		strNamespace = a;
    	}
    	
    	public Object invoke(Object arg0, Method method, Object[] arg2) throws Throwable {
    		Object ret = null;
    		if(method.getName().equals("onConnect")){
    			//we have to hand the outbound over to each classes. with this both can handle the next message.
        		for (Object target :targets.values()) {
        			method.invoke(target, arg2);
        		}
    		}else{
    			if(strNamespace.length() != 0){
    				ret = method.invoke(targets.get(strNamespace), arg2);
    			} else {
    				// This is for calling setNamespace
    				ret = method.invoke(targets.get("/"), arg2);
    			}
    		}
    		return ret;
    	}
    };

	@Override
	protected SocketIOInbound doSocketIOConnect(HttpServletRequest request) {
		Intercepter objIntercepter = new Intercepter();
		
		// make target pairs (url, object for that)
		HashMap<String, Object> targets = new HashMap<String, Object>();
		targets.put("/", new EchoConnectionImpl(objIntercepter));
		targets.put("/chat", new EchoConnectionImplWithAsterisk(objIntercepter));
		// set target pairs to intercepter
		objIntercepter.setIntercepter(targets);
		
		SocketIOInbound proxyObj = (SocketIOInbound) Proxy.newProxyInstance(
			SocketIOInbound.class.getClassLoader(),
	    	new Class<?>[] { SocketIOInbound.class },
	    	objIntercepter);
		
		return (SocketIOInbound) proxyObj;
	}

}
