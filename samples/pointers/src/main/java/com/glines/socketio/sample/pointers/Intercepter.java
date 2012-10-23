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
	private String strNamespace = "/";
	
	// SocketIOInbounds / targets (coded classed assigned for providing responses)
	private HashMap<String, Object> targets = new HashMap<String, Object>();
	
	// SocketIOOutbounds / unique on this class != unique on this vm (because this isn't static class)
	private static HashMap<String, Queue<SocketIOOutbound>> connections = new HashMap<String, Queue<SocketIOOutbound>>();
	
	private static final Logger LOGGER = Logger.getLogger(JettyWebSocketTransportHandler.class.getName());
    private AtomicInteger ids = new AtomicInteger(1);
    
    // unique on this instance 
    private boolean setClient = false;
    private SocketIOOutbound objOutboundTmp = null;

	public void setIntercepter(HashMap<String, Object> objects){
		this.targets = objects;
	}
	
	public Object invoke(Object arg0, Method method, Object[] arg2) throws Throwable {
		Object ret = null;
		if(method.getName().equals("onConnect")){
			//we have to hand the outbound over to all classes. with this both can handle the next message.
    		if (LOGGER.isLoggable(Level.FINE))
    			LOGGER.log(Level.FINE, method.getName()+": namespace = "+strNamespace+" - "+arg2[0]+" - "+ ids.getAndIncrement());
    		for (Object target :targets.values()) {
    			method.invoke(target, arg2);
    		}
    		objOutboundTmp = (SocketIOOutbound) arg2[0];
    		
		} else if(method.getName().equals("onDisconnect")){
			ret = method.invoke(targets.get(strNamespace), arg2);
			if(setClient){
				if (LOGGER.isLoggable(Level.FINE))
					LOGGER.log(Level.FINE, method.getName()+": connction was removed: "+objOutboundTmp.toString());
				connections.get(strNamespace).remove(objOutboundTmp);
				setClient = false;
			}
			
		} else if(method.getName().equals("setNamespace")){
			// useful if this session use namespace.
			strNamespace = (String) arg2[0];
			if(!setClient){
	          if (LOGGER.isLoggable(Level.FINE))
	        	  LOGGER.log(Level.FINE, method.getName()+": setting connctions can be done: "+strNamespace+" = "+targets.get(strNamespace));
	          if(!connections.containsKey(strNamespace)){
	        	  connections.put(strNamespace, new ConcurrentLinkedQueue<SocketIOOutbound>());
	          }
	          connections.get(strNamespace).offer(objOutboundTmp);  
	          setClient = true;
			}
			
		} else if(method.getName().equals("setEventnames")){
			// useful only if this session does'nt use namespace.
			ret = method.invoke(targets.get(strNamespace), arg2);			
			if(!setClient){
	          if (LOGGER.isLoggable(Level.FINE))
	        	  LOGGER.log(Level.FINE, method.getName()+": setting connctions can be done: "+strNamespace+" = "+targets.get(strNamespace));
	          if(!connections.containsKey(strNamespace)){
	        	  connections.put(strNamespace, new ConcurrentLinkedQueue<SocketIOOutbound>());
	          }
	          connections.get(strNamespace).offer(objOutboundTmp);  
	          setClient = true;
			}
			
		} else {
	        if (LOGGER.isLoggable(Level.FINE))
	            LOGGER.log(Level.FINE, method.getName()+": namespace = "+strNamespace+" - "+ ids.getAndIncrement());
			ret = method.invoke(targets.get(strNamespace), arg2);
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
    	// own clients can be used here.
    	Queue<SocketIOOutbound> clients = connections.get(strNamespace);
    	if (LOGGER.isLoggable(Level.FINE))
            LOGGER.log(Level.FINE, "broadcasted to "+clients.size()+" clients");
        for (SocketIOOutbound objTmp : clients) {
            if (objTmp != outbound) {
                try {
                	objTmp.emitMessage(strKey, message);
                } catch (Exception e) {
                	objTmp.disconnect();
                	e.printStackTrace();
                }
            }
        }
    }

    public void dumpClients(){
    	StringBuilder sb = new StringBuilder();
    	for(String a: connections.keySet()){
    		Object[] b = connections.get(a).toArray();
    		sb.append("the num of total clients for "+a+": "+b.length+"\n");
        	for (int i = 0; i < b.length; i++){
        		sb.append("client("+Integer.toString(i)+"): "+b[i].toString()+"\n");
        	}
    	}
    	LOGGER.log(Level.INFO, sb.toString()); 
    }

    public int getClientsSize(){
    	return connections.get(strNamespace).size();
    }
    
	public SocketIOInbound getProxyObj(){
		SocketIOInbound proxyObj = (SocketIOInbound) Proxy.newProxyInstance(
				SocketIOInbound.class.getClassLoader(),
		    	new Class<?>[] { SocketIOInbound.class },
		    	this);
		return proxyObj;
	}
};