package com.tomcat.tomcat;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

public class AppLifecycleListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        System.out.println("[AppLifecycleListener] Application started - context path: "
                + sce.getServletContext().getContextPath());
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        System.out.println("[AppLifecycleListener] Application stopped");
    }
}