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
package com.glines.socketio.sample.d3;

import java.util.HashMap;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import com.glines.socketio.common.DisconnectReason;
import com.glines.socketio.server.SocketIOInbound;
import com.glines.socketio.server.SocketIOOutbound;
import com.glines.socketio.server.SocketIOServlet;
import com.glines.socketio.util.Intercepter;
import com.glines.socketio.util.InterceptedSocketIOInbound;

public class D3SocketServlet extends SocketIOServlet {
	private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(D3SocketServlet.class.getName());

	//private class EchoConnectionImpl implements SocketIOInbound {
    private class EchoConnectionImpl extends InterceptedSocketIOInbound {
		private volatile SocketIOOutbound outbound = null;
		private Intercepter objIntercepter = null;
		
		EchoConnectionImpl(Intercepter a){
			this.objIntercepter = a;
		}

		@Override
		public void onConnect(SocketIOOutbound outbound) {
			// this method is called when tcp connection is established.
			// this method must be returned immediately (without emit or some other methods must not be used here.)
			this.outbound = outbound;
		}

		@Override
		public void onDisconnect(DisconnectReason reason, String errorMessage) {
			this.outbound = null;
		}

		@Override
		public void onMessage(String strKey, String message) {
			int sleepTime = 1;
			int intLoops = 1000;
			int intInitial = D3HttpServlet.getIntCount();
			
			if(strKey.equals("stop")){
				D3HttpServlet.setIntCount(0);
			}
			
			while(intLoops-- > 0){
	            try {
	                Thread.sleep((long) (sleepTime * (Math.random()*1000+10)));
	            } catch (InterruptedException e) {
	                // Ignore
	            }
	            // the number of loops was changed.
	            if(intInitial != D3HttpServlet.getIntCount()){
	            	LOGGER.info("loop was changed: " + intInitial + " -> " + D3HttpServlet.getIntCount());
	            	intLoops = D3HttpServlet.getIntCount();
	            	intInitial = D3HttpServlet.getIntCount();
	            	objIntercepter.emit(this.outbound, "reset", Integer.toString(intLoops));
	            }else{
	            	objIntercepter.emit(this.outbound, "data", Integer.toString(intLoops));
	            }
			}
        }

        @Override
        public String[] setEventnames() {
        	return new String[]{"start", "stop"};
        }
	}
	
	@Override
	protected SocketIOInbound doSocketIOConnect(HttpServletRequest request) {
		Intercepter objIntercepter = new Intercepter();
		
		// make target pairs (url, object for that)
		HashMap<String, Object> targets = new HashMap<String, Object>();
		targets.put("/", new EchoConnectionImpl(objIntercepter));
		
		// set target pairs to intercepter
		objIntercepter.setIntercepter(targets);

		return objIntercepter.getProxyObj();
	}
}