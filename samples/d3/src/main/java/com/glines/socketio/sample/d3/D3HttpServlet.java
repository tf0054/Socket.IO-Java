package com.glines.socketio.sample.d3;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class D3HttpServlet extends HttpServlet {

	static String username = "";
	static String apikey = "";
	static String streamid = "";

	private static int intCount = 1000;

    public void init() {
        //this.userDAO = Config.getInstance(getServletContext()).getDAOFactory().getUserDAO();
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
//        username = request.getParameter("username");
//        apikey = request.getParameter("apikey");
//        streamid = request.getParameter("streamid");
    	setIntCount(Integer.parseInt(request.getParameter("count")));
//        User user = userDAO.find(username, password);
        response.sendRedirect("/?random="+Math.random()*1000);
//        request.setAttribute("error", "Unknown user, please try again");
//        request.getRequestDispatcher("/login.jsp").forward(request, response);
    }

	public static int getIntCount() {
		return intCount;
	}

	public static void setIntCount(int intCount) {
		D3HttpServlet.intCount = intCount;
	}

}