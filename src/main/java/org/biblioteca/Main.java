package org.biblioteca;
import org.biblioteca.model.Libro;
import org.biblioteca.model.Prestito;
import org.biblioteca.model.Utente;
import org.biblioteca.servlets.*;
import org.biblioteca.utils.ConfigLoader;
import org.biblioteca.utils.CustomLogger;
import org.biblioteca.utils.TaskNotificheScadenza;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;
import org.h2.tools.Server;

import java.io.IOException;
import java.sql.*;
import java.util.List;
import java.util.Timer;

public class Main {
    public static final String PATH_APP="/biblioteca";
    public static final String PATH_WEBAPP_SERVLET="/WEB-INF";
    public static final String PATH_SERVICES_SERVLET="/services";
    public static final String PATH_BACKEND_SERVLET="/backend";
    public static final String PATH_REGISTER_SERVLET="/register";
    public static final String PATH_INSERTBOOK_SERVLET="/insertbook";
    public static final String PATH_LOGIN_SERVLET=PATH_WEBAPP_SERVLET+"/login";

    public static void main(String[] args) throws Exception {
        final CustomLogger LOGGER = CustomLogger.getInstance();
        ConfigLoader properties = new ConfigLoader("config.properties");
        int servertPort=Integer.parseInt(properties.getProperty("server.port"));
        System.out.println("Avvio dell'applicazione");
        //Avvio server Jetty
        org.eclipse.jetty.server.Server server = new org.eclipse.jetty.server.Server(servertPort);
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath(PATH_APP);
        context.setBaseResource(Resource.newClassPathResource(PATH_WEBAPP_SERVLET));
        server.setHandler(context);

        //carico le servlet
        context.addServlet(new ServletHolder( new LoginServlet()), PATH_LOGIN_SERVLET+"/*");
        context.addServlet(new ServletHolder( new BibliotecaServlet()), PATH_SERVICES_SERVLET+"/*");
        context.addServlet(new ServletHolder( new WebAppServlet()), PATH_WEBAPP_SERVLET+"/*");
        context.addServlet(new ServletHolder( new RegistrationServlet()), PATH_REGISTER_SERVLET+"/*");
        context.addServlet(new ServletHolder( new BackEndServlet()), PATH_BACKEND_SERVLET+"/*");
        context.addServlet(new ServletHolder( new InsertBookServlet()), PATH_INSERTBOOK_SERVLET+"/*");
        context.addServlet(new ServletHolder( new DummyServlet()), "/*");

        server.start();

        //avvio il task che ogni giorno notifica le scadenze
        Timer timer = new Timer();
        TaskNotificheScadenza task = new TaskNotificheScadenza();

        // Esegue il task ogni 24 ore
        Integer ogniGiorno=86400000;//24*60*60*1000;
        timer.schedule(task, 200000, ogniGiorno);


   }

    }
