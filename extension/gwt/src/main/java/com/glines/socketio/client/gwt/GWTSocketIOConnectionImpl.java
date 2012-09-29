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
import com.glines.socketio.server.SocketIOFrame;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;

public class GWTSocketIOConnectionImpl implements SocketIOConnection {
	private static final class SocketIOImpl extends JavaScriptObject {
		public static native SocketIOImpl create(GWTSocketIOConnectionImpl impl, String host, String port, JsArrayString aryKeys) /*-{
			var socket = $wnd.io.connect('http://'+host+':'+port,{rememberTransport: false, transports:["websocket"]});
			socket.on('connect', $entry(function() {
      			impl.@com.glines.socketio.client.gwt.GWTSocketIOConnectionImpl::onConnect()();
    		}));
			aryKeys.forEach(
				function addSocketON(strKey) {
					socket.on(strKey, $entry(function(msg) {
						//impl.@com.glines.socketio.client.gwt.GWTSocketIOConnectionImpl::onMessage(ILjava/lang/String;)(1, $wnd.io.JSON.stringify(msg));
						impl.@com.glines.socketio.client.gwt.GWTSocketIOConnectionImpl::onMessage(Ljava/lang/String;Ljava/lang/String;)(strKey, $wnd.io.JSON.stringify(msg));
		    		}));
		    	}
			);
			socket.on('disconnect', $entry(function(dr, message) {
      			impl.@com.glines.socketio.client.gwt.GWTSocketIOConnectionImpl::onDisconnect(ILjava/lang/String;)(dr, message);
    		}));
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

	    public native void disconnect() /*-{this.socket.disconnect();}-*/;

	    public native void send(int messageType, String data) /*-{
	    	// the messageType is 0 or 1 and isn't same as the FrameType (3=Text,4=Json,etc)
	    	if(messageType == 0){
	    		// we can send a raw string with using transport.send().
				//this.socket.transport.send("3:1::"+data);
				this.send(data);
			}else{
				// java->javascript converting is done via string. so we need eval that.
				this.socket.transport.send("4:1::"+data);
				//this.json.send(eval("("+data+")"));
			}
	    }-*/;
	}
	
	private final SocketIOConnectionListener listener;
	private final String host;
	private final String port;
	private final String[] aryKeys;
	private SocketIOImpl socket = null;

	GWTSocketIOConnectionImpl(SocketIOConnectionListener listener,
			String host, short port, String[] aryKeys) {
		this.listener = listener;
		if (host.length() > 0) {
			this.host = host;
		}else{
			this.host = "localhost";
		}
		if (port > 0) {
			this.port = "" + port;
		} else {
			this.port = "8080";
		}
		if (aryKeys != null) {
			this.aryKeys = aryKeys;
		} else {
			this.aryKeys = new String[]{"message"};
		}
		
	}

	@Override
	public void connect() {
		if (socket == null) {
			JsArrayString jsStrings = (JsArrayString)JsArrayString.createArray();
			for (String s : aryKeys) {
				  jsStrings.push(s);
			}
			socket = SocketIOImpl.create(this, host, port, jsStrings);
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
		//sendMessage(SocketIOFrame.TEXT_MESSAGE_TYPE, message);
		emitMessage("message", message);
	}

	@Override
	public void emitMessage(String strKey, String message) throws SocketIOException {
		// This is for emitting message.
		// TODO maybe this message should be changed to JSONObject or something.
		if(message.startsWith("\\{") || message.startsWith("\\[")){
			sendMessage(SocketIOFrame.JSON_MESSAGE_TYPE, "{\""+strKey+"\":"+message+"}");
		}else{
			sendMessage(SocketIOFrame.JSON_MESSAGE_TYPE, "{\""+strKey+"\":\""+message+"\"}");
		}
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
	private void onMessage(String strKey, String message) {
		// i don't know why this
		//message = message.replaceAll("^\\{\""+strKey+"\":", "");
		//message = message.replaceAll("\\}$", "");
		listener.onMessage(strKey, message);
	}

	@SuppressWarnings("unused")
	private void onMessage(int messageType, String message) {
		listener.onMessage(messageType, message);
	}
}
