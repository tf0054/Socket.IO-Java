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
package com.glines.socketio.sample.gwtchat.client;

import com.glines.socketio.client.common.SocketIOConnection;
import com.glines.socketio.client.common.SocketIOConnectionListener;
import com.glines.socketio.client.gwt.GWTSocketIOConnectionFactory;
import com.glines.socketio.common.DisconnectReason;
import com.glines.socketio.common.SocketIOException;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.json.client.JSONValue;

public class GWTChatClient implements EntryPoint, SocketIOConnectionListener {

	SocketIOConnection socket;
	HTML htmlPanel;
	FlowPanel submitPanel;
	TextBox textBox;
	
	public void onModuleLoad() {
		RootPanel rootPanel = RootPanel.get();
		rootPanel.setSize("800", "300");
		
		htmlPanel = new HTML("Connecting...");
		htmlPanel.setStyleName("chat");
		rootPanel.add(htmlPanel);
		
		submitPanel = new FlowPanel();
		submitPanel.setStyleName("submitPanel");
		rootPanel.add(submitPanel);
		submitPanel.setWidth("800");
		submitPanel.setVisible(false);
		
		textBox = new TextBox();
		textBox.setVisibleLength(109);
		textBox.addKeyPressHandler(new KeyPressHandler() {
			public void onKeyPress(KeyPressEvent event) {
				if (event.getCharCode() == KeyCodes.KEY_ENTER) {
					onSubmit();
				}
			}
		});
		textBox.setSize("", "30");
		submitPanel.add(textBox);
		
		Button btnSubmit = new Button("Submit");
		btnSubmit.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				onSubmit();
			} 
		});
		btnSubmit.setHeight("30");
		submitPanel.add(btnSubmit);
		
		socket = GWTSocketIOConnectionFactory.INSTANCE.create(this, "", (short)0, 
				new String[] {"message", "welcome", "announcement"});
		socket.connect();
	}
	
	private void addLine(String line) {
		Element p = DOM.createElement("p");
		p.setInnerHTML(line);
		htmlPanel.getElement().appendChild(p);
		htmlPanel.getElement().setScrollTop(1000000);
	}
	
	private void onSubmit() {
		String text = textBox.getText();
		if(text.length() == 0)
			return;
		if (text.equals("/lclose")) {
			addLine("<em>closing...</em>");
			socket.close();
		} else if (text.equals("/ldisconnect")) {
			addLine("<em>disconnecting...</em>");
			socket.disconnect();
		} else {
			addLine("<b>you:</b> " + text);
			textBox.setText("");
			try {
				socket.emitMessage("message", text);
			} catch (SocketIOException e) {
				// Ignore. This wwon't happen in the GWT version.
			}
		}
	}

	@Override
	public void onConnect() {
		htmlPanel.setHTML("");
		addLine("<b>Connected</b>");
		submitPanel.setVisible(true);
	}

	@Override
	public void onDisconnect(DisconnectReason reason, String errorMessage) {
		submitPanel.setVisible(false);
		if (errorMessage != null) {
			addLine("<b>Disconnected["+reason+"]:</b> " + errorMessage);
		} else {
			addLine("<b>Disconnected["+reason+"]</b>");
		}
	}
	
	public void onMessage(String strKey, String message) {
		JSONValue obj = null;

		if(message.startsWith("{") || message.startsWith("[")){
			obj = JSONParser.parseStrict(message);
		}else{
			message = message.replaceAll("^\"", "").replaceAll("\"$", "");
		}
		
		if (strKey.equals("welcome")) {
				addLine("<em><b>" + message + "</b></em>");
		} else if (strKey.equals("announcement")) {
				addLine("<em>" + message + "</em>");
		} else if (strKey.equals("message")) {
			if(obj == null){
				addLine("<b>Server:</b> " + message);
			}else{
				JSONArray arr = obj.isArray();
				if (arr != null){
					if(arr.size() >= 2) {
						JSONString id = arr.get(0).isString();
						JSONString msg = arr.get(1).isString(); 
						if (id != null && msg != null) {
							addLine("<b>" + id.stringValue() + ":</b> " + msg.stringValue());
						}
					}
				} else {
					Window.alert("unsupported json: "+obj.toString());
				}
			}
		} else {
			Window.alert("unrecognized message: "+obj.toString());
		}
	}

	@Override
	public void onMessage(int messageType, String message) {
		if (messageType == 1) { 
				onMessage("message",message);
		} else {
			Window.alert("messageType = "+messageType+", message = "+message);
		}
	}
}
