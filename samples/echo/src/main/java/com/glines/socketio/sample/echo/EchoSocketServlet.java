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

import javax.servlet.http.HttpServletRequest;

import com.glines.socketio.common.DisconnectReason;
import com.glines.socketio.server.SocketIOInbound;
import com.glines.socketio.server.SocketIOOutbound;
import com.glines.socketio.server.SocketIOServlet;

public class EchoSocketServlet extends SocketIOServlet {
	private static final long serialVersionUID = 1L;

	private class EchoConnection implements SocketIOInbound {
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
	}

	@Override
	protected SocketIOInbound doSocketIOConnect(HttpServletRequest request) {
		return new EchoConnection();
	}

}
