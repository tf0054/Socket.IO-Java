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

import javax.servlet.http.HttpServletRequest;

import com.glines.socketio.common.DisconnectReason;
import com.glines.socketio.server.SocketIOInbound;
import com.glines.socketio.server.SocketIOOutbound;
import com.glines.socketio.server.SocketIOServlet;

public class EchoSocketServlet extends SocketIOServlet {
	private static final long serialVersionUID = 1L;

	private class EchoConnectionImpl implements SocketIOInbound {
		private volatile SocketIOOutbound outbound = null;

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
        	if(a.equals("/chat"))
        		objIntercepter.setFlag(1);
        }
	}
	
	private class EchoConnectionWithAsterisk implements SocketIOInbound {
		private volatile SocketIOOutbound outbound = null;

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
        	if(a.equals("/chat"))
        		objIntercepter.setFlag(1);
        }

	}
	
    public class Intercepter implements InvocationHandler{
    	Object target1, target2;
    	int intFlag = 0;
    	public Intercepter(Object object1, Object object2){
    		 this.target1 = object1;
    		 this.target2 = object2;
    	}
    	
    	public void setFlag(int a){
    		intFlag = a;
    	}
    	
    	public Object invoke(Object arg0, Method arg1, Object[] arg2) throws Throwable {
    		Object ret = null;
    		if(arg1.getName().equals("onConnect")){
    			// ugly: we have to hand the outbound over to each classes. with this both can handle the next message.
    			ret = arg1.invoke(target1, arg2);
    			ret = arg1.invoke(target2, arg2);
    		}
    		if(intFlag == 0){
    			 ret = arg1.invoke(target1, arg2);
    		}else{
    			 ret = arg1.invoke(target2, arg2);
    		}
    		return ret;
    	}
    };

    Intercepter objIntercepter = null;
    
	@Override
	protected SocketIOInbound doSocketIOConnect(HttpServletRequest request) {
		String strUrl = request.getRequestURL().toString();
		strUrl = strUrl.substring(strUrl.lastIndexOf("/"));
		//
		objIntercepter = new Intercepter(
			new EchoConnectionImpl(),
			new EchoConnectionWithAsterisk());
		
		SocketIOInbound dataTmp = (SocketIOInbound) Proxy.newProxyInstance(
			SocketIOInbound.class.getClassLoader(),
	    	new Class<?>[] { SocketIOInbound.class },
	    	objIntercepter);
		
		return (SocketIOInbound) dataTmp;
	}

}
