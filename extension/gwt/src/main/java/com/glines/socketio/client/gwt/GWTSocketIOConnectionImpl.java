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
package com.glines.socketio.client.gwt;

import com.glines.socketio.client.common.SocketIOConnection;
import com.glines.socketio.client.common.SocketIOConnectionListener;
import com.glines.socketio.common.ConnectionState;
import com.glines.socketio.common.DisconnectReason;
import com.glines.socketio.common.SocketIOException;
import com.google.gwt.core.client.JavaScriptObject;

public class GWTSocketIOConnectionImpl implements SocketIOConnection {
	private static final class SocketIOImpl extends JavaScriptObject {
		public static native SocketIOImpl create(GWTSocketIOConnectionImpl impl, String host, String port) /*-{
			//var socket = new $wnd.io.Socket(host != null ? {host: host} : {host: "localhost"}, port != null ? {port: port} : {port: "8081"});
			//var socket = new $wnd.io.Socket({host: "localhost", port: "8080", rememberTransport: false, transports:["websocket"] });
			var socket = $wnd.io.connect('http://localhost:8080',{rememberTransport: false, transports:["websocket"]});
			socket.on('connect', $entry(function() {
      			impl.@com.glines.socketio.client.gwt.GWTSocketIOConnectionImpl::onConnect()();
    		}));
			socket.on('message', $entry(function(msg) {
				if(msg.welcome){
      				impl.@com.glines.socketio.client.gwt.GWTSocketIOConnectionImpl::onMessage(Ljava/lang/String;)("{\"welcome\": \""+msg.welcome+"\"}");
				}else if(msg.announcement){
      				impl.@com.glines.socketio.client.gwt.GWTSocketIOConnectionImpl::onMessage(Ljava/lang/String;)("{\"announcement\": \""+msg.announcement+"\"}");
				}else if(msg.message){
					if(isArray(msg.message)){
      					impl.@com.glines.socketio.client.gwt.GWTSocketIOConnectionImpl::onMessage(Ljava/lang/String;)("{\"message\": [\""+msg.message[0]+"\",\""+msg.message[1]+"\"]}");
      				}else{
      					impl.@com.glines.socketio.client.gwt.GWTSocketIOConnectionImpl::onMessage(Ljava/lang/String;)("{\"message\": \""+msg.message+"\"}");
      				}
				}else{
					$wnd.alert("cannot convert to string");
				}
    		}));
			socket.on('disconnect', $entry(function(dr, message) {
      			impl.@com.glines.socketio.client.gwt.GWTSocketIOConnectionImpl::onDisconnect(ILjava/lang/String;)(dr, message);
    		}));
			function isArray(a)
			{
			    return Object.prototype.toString.apply(a) === '[object Array]';
			}
			return socket;
		}-*/;

		protected SocketIOImpl() {
	    }

	    public native int getSocketState() /*-{ ;
	    	if(this.socket.connecting){
	    		return 0;
	    	}else if(this.socket.connected){
	    		return 1;
	    	}else{
	    		return 3;
	    	}
	    }-*/;

	    public native void connect() /*-{this.socket.connect();}-*/;

	    public native void close() /*-{this.close();}-*/;

	    public native void disconnect() /*-{this.disconnect();}-*/;

	    public native void send(int messageType, String data) /*-{
			this.socket.transport.send("3:1::"+data);
	    }-*/;
	}
	
	private final SocketIOConnectionListener listener;
	private final String host;
	private final String port;
	private SocketIOImpl socket = null;

	GWTSocketIOConnectionImpl(SocketIOConnectionListener listener,
			String host, short port) {
		this.listener = listener;
		this.host = host;
		if (port > 0) {
			this.port = "" + port;
		} else {
			this.port = null;
		}
	}

	@Override
	public void connect() {
		if (socket == null) {
			socket = SocketIOImpl.create(this, host, port);
		}

		if (ConnectionState.CLOSED != getConnectionState()) {
			throw new IllegalStateException("Connection isn't closed!");
		}
		socket.connect();
	}

	@Override
	public void close() {
		if (ConnectionState.CONNECTED == getConnectionState()) {
			socket.close();
		}
	}
	
	@Override
	public void disconnect() {
		if (ConnectionState.CLOSED != getConnectionState()) {
			socket.disconnect();
		}
	}

	@Override
	public ConnectionState getConnectionState() {
		ConnectionState state;
		if (socket != null) {
			state = ConnectionState.fromInt(socket.getSocketState());
		} else {
			state = ConnectionState.CLOSED;
		}
		return state;
	}

	@Override
	public void sendMessage(String message) throws SocketIOException {
		sendMessage(0, message);
	}

	@Override
	public void sendMessage(int messageType, String message)
			throws SocketIOException {
		if (ConnectionState.CONNECTED != getConnectionState()) {
			throw new IllegalStateException("Not connected");
		}
		socket.send(messageType, message);
	}

	@SuppressWarnings("unused")
	private void onConnect() {
		listener.onConnect();
	}

	@SuppressWarnings("unused")
	private void onDisconnect(int dr, String errorMessage) {
		DisconnectReason reason = DisconnectReason.fromInt(dr);
		listener.onDisconnect(reason, errorMessage);
	}

	@SuppressWarnings("unused")
	private void onMessage(int messageType, String message) {
		listener.onMessage(messageType, message);
	}
	
	@SuppressWarnings("unused")
	private void onMessage(String msg) {
		listener.onMessage(msg);
	}	
}
