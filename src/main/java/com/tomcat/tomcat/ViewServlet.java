package com.tomcat.tomcat;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public class ViewServlet extends HttpServlet {

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setAttribute("message", "Hello from Servlet!");
        request.setAttribute("items", new String[]{"Apple", "Banana", "Strawberry"});

        request.getRequestDispatcher("/WEB-INF/view.jsp").forward(request, response);
    }
}