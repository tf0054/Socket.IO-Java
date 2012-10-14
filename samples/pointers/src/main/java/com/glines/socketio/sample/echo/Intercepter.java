package com.glines.socketio.sample.echo;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;

import com.glines.socketio.server.SocketIOInbound;

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
	
	public SocketIOInbound getProxyObj(){
		SocketIOInbound proxyObj = (SocketIOInbound) Proxy.newProxyInstance(
				SocketIOInbound.class.getClassLoader(),
		    	new Class<?>[] { SocketIOInbound.class },
		    	this);
		return proxyObj;
	}
};