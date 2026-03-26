package com.tomcat.tomcat;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;

public class LoggingFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) {
        System.out.println("[LoggingFilter] init");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        System.out.println("[LoggingFilter] BEFORE - " + httpRequest.getMethod() + " " + httpRequest.getRequestURI());

        chain.doFilter(request, response);

        System.out.println("[LoggingFilter] AFTER - " + httpRequest.getRequestURI());
    }

    @Override
    public void destroy() {
        System.out.println("[LoggingFilter] destroy");
    }
}