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
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;

//import com.glines.socketio.sample.pointers.PointersShareServlet.PointersConnectionImpl;
import com.glines.socketio.server.SocketIOInbound;
import com.glines.socketio.server.SocketIOOutbound;
import com.glines.socketio.server.transport.jetty.JettyWebSocketTransportHandler;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Intercepter implements InvocationHandler{
	private String strNamespace = "";
	private HashMap<String, Object> targets = new HashMap<String, Object>();
	
	//private HashMap<String, Queue> connections = new HashMap<String, Queue>();
	public static Queue<SocketIOOutbound> clients = new ConcurrentLinkedQueue<SocketIOOutbound>();

	private static final Logger LOGGER = Logger.getLogger(JettyWebSocketTransportHandler.class.getName());
    private AtomicInteger ids = new AtomicInteger(1);

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
          if (LOGGER.isLoggable(Level.FINE))
          LOGGER.log(Level.FINE, method.getName()+": strNamespace = "+strNamespace+" - "+ ids.getAndIncrement());
		} else if(method.getName().equals("disConnect")){
			if(strNamespace.length() != 0){
				ret = method.invoke(targets.get(strNamespace), arg2);
			} else {
				// This is for calling setNamespace
				ret = method.invoke(targets.get("/"), arg2);
			}
          if (LOGGER.isLoggable(Level.FINE))
              LOGGER.log(Level.FINE, method.getName()+": strNamespace = "+strNamespace+" - "+ ids.getAndIncrement());
//    		if(connections.get(strNamespace).size() > 1){
//    			connections.get(strNamespace).remove((SocketIOInbound)targets.get(strNamespace));
//    		}else{
//	    		connections.remove(strNamespace);
//    		}
		} else {
	          if (LOGGER.isLoggable(Level.FINE))
	              LOGGER.log(Level.FINE, method.getName()+": strNamespace = "+strNamespace+" - "+ ids.getAndIncrement());
			if(strNamespace.length() != 0){
				ret = method.invoke(targets.get(strNamespace), arg2);
//	    		if(connections.get(strNamespace) != null){
//	    			connections.get(strNamespace).offer((SocketIOInbound)targets.get(strNamespace));
//	    		}else{
//		    		clients.offer((SocketIOInbound)targets.get(strNamespace));
//		    		connections.put(strNamespace, clients);
//	    		}
			} else {
				// This is for calling setNamespace
				ret = method.invoke(targets.get("/"), arg2);
			}
		}
		return ret;
	}
	
    public void emit(SocketIOOutbound outbound, String strKey, String message) {
        try {
        	outbound.emitMessage(strKey, message);
        } catch (Exception e) {
            outbound.disconnect();
        	e.printStackTrace();
        }
    }
    
    public void broadcast(SocketIOOutbound outbound, String strKey, String message) {
//        if (LOGGER.isLoggable(Level.FINE))
//            LOGGER.log(Level.FINE, this + " broadcasted to "+connections.get(strNamespace).size()+" clients");
//
//        if(strNamespace.length() == 0)
//        	strNamespace = "/";
//        Queue<SocketIOInbound> clients = connections.get(strNamespace);
        for (SocketIOOutbound objTmp : clients) {
            if (objTmp != outbound) {
                try {
                	objTmp.emitMessage(strKey, message);
                } catch (IOException e) {
                	objTmp.disconnect();
                }
            }
        }
    }

	public SocketIOInbound getProxyObj(){
		SocketIOInbound proxyObj = (SocketIOInbound) Proxy.newProxyInstance(
				SocketIOInbound.class.getClassLoader(),
		    	new Class<?>[] { SocketIOInbound.class },
		    	this);
		return proxyObj;
	}
};