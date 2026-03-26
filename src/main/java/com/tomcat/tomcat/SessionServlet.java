package com.tomcat.tomcat;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;

public class SessionServlet extends HttpServlet {

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/html; charset=UTF-8");

        String action = request.getParameter("action");
        if ("invalidate".equals(action)) {
            HttpSession session = request.getSession(false);
            if (session != null) session.invalidate();
            response.sendRedirect("session");
            return;
        }

        HttpSession session = request.getSession();
        Integer count = (Integer) session.getAttribute("count");
        if (count == null) count = 0;
        session.setAttribute("count", ++count);

        PrintWriter out = response.getWriter();
        out.println("<html><body>");
        out.println("<h2>Session ID: " + session.getId() + "</h2>");
        out.println("<p>방문 횟수: " + count + "</p>");
        out.println("<p>세션 생성 시각: " + new java.util.Date(session.getCreationTime()) + "</p>");
        out.println("<a href='session?action=invalidate'>세션 무효화</a>");
        out.println("</body></html>");
    }
}