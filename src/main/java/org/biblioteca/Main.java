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
        context.setContextPath("/biblioteca");
        context.setBaseResource(Resource.newClassPathResource("/WEB-INF"));
        server.setHandler(context);

        //avvio le servlet
        context.addServlet(new ServletHolder( new BibliotecaServlet()), PATH_SERVICES_SERVLET+"/*"); //1
        context.addServlet(new ServletHolder( new WebAppServlet()), PATH_WEBAPP_SERVLET+"/*"); //2
        context.addServlet(new ServletHolder( new LoginServlet()), PATH_LOGIN_SERVLET+"/*"); //3
        context.addServlet(new ServletHolder( new RegistrationServlet()), PATH_REGISTER_SERVLET+"/*"); //4
        context.addServlet(new ServletHolder( new BackEndServlet()), PATH_BACKEND_SERVLET+"/*"); //5
        context.addServlet(new ServletHolder( new InsertBookServlet()), PATH_INSERTBOOK_SERVLET+"/*"); //6
        context.addServlet(new ServletHolder( new DummyServlet()), "/*"); //6

        server.start();

        //avvio il task che ogni giorno notifica le scadenze
        Timer timer = new Timer();
        TaskNotificheScadenza task = new TaskNotificheScadenza();

        // Esegue il task ogni 24 ore
        Integer ogniGiorno=86400000;//24*60*60*1000;
        timer.schedule(task, 100000, ogniGiorno);

        //test
        if (false) {
            Utente utente1=new Utente();
            utente1.setUsername("admin");
            utente1.setPassword("admin");
            utente1.setId(1);
            List<Libro> libroCercato=utente1.cercaLibro(null, "Eco");
            Libro libroPreso=libroCercato.get(0);
            System.out.println("Disponibilità libro: "+libroPreso.getDisponibilita());
            List<Libro> libri=utente1.listalibri();
            Utente utente2=new Utente();
            utente2.setUsername("piero779@gmail.com");
            utente2.setPassword("pippero");
            utente2.setId(2);
            boolean ok1=utente1.prendiInPrestito(libroPreso);
            if (ok1)
                System.out.println("utente admin ha preso in prestito libro Eco");


            List<Prestito> lista1=utente1.getListaPrestitiUtente();
            System.out.println("***lista prestiti utente admin");
            lista1.forEach(prestito -> {
                System.out.println("utente :"+prestito.getUser().getId()+" PrestitoId :"+prestito.getId()+" LibroId: "+prestito.getLibro().getId()+ " - "+prestito.getLibro().getTitolo());
            });
            Prestito prestitoPerNotifica=lista1.get(0);
            boolean oknotifica=utente1.riceviNotificaScadenza(prestitoPerNotifica);
            if (oknotifica)
                System.out.println("utente admin ha richiesto notifica scadenza");
//            System.out.println("Adesso mi addormento per 11 secondi");
//            Thread.sleep(11000);
            Prestito prestito1 = null;
            if (lista1.size()>0)
                prestito1=lista1.get(0);
//            boolean ok1prolunga=utente1.prolungaPrestito(prestito1);
//            if (ok1prolunga)
//                System.out.println("admin ha prolungato il prestito");
            System.out.println("utente piero cerca di prendere il libro");
            boolean ok2=utente2.prendiInPrestito(libroPreso);
            if (ok2)
                System.out.println("WARNING utente piero è riuscito a prendere il libro");
            else System.out.println("ERRORE non è possibile prendere il libro non è disponibile");
            System.out.println("*** lista prestiti utente piero");
            List<Prestito> lista2=utente2.getListaPrestitiUtente();
            lista2.forEach(prestito -> {
                System.out.println("utente :"+prestito.getUser().getId()+" PrestitoId :"+prestito.getId()+" LibroId: "+prestito.getLibro().getId()+ " - "+prestito.getLibro().getTitolo());
            });
            Prestito prestito2 = null;
//            if (lista2.size()>0)
//                 prestito2=utente2.getListaPrestitiUtente().get(0);
//            System.out.println("Adesso utente piero cerca di restituire un prestito non suo");
//
//            boolean ok2ter=utente1.restituisci(prestito1.getId());
//            if (ok2ter)
//                System.out.println("WARNING utente  piero è riuscito a restituire un libro non suo");
//            else System.out.println("ok la restituzione non è stata possibile");
            boolean ok1_ter=utente1.restituisci(prestito1.getId());
            if (ok1_ter)
                System.out.println("utente admin ha restituito il libro");
            else System.out.println("WARNING utente admin NON è riuscito a restituire il libro");
            boolean ok2bis=utente2.prendiInPrestito(libroPreso);
            lista2=utente2.getListaPrestitiUtente();
            System.out.println("*** lista prestiti utente piero");
            List<Prestito> lista3=utente2.getListaPrestitiUtente();
            lista3.forEach(prestito -> {
                System.out.println("utente :"+prestito.getUser().getId()+" PrestitoId :"+prestito.getId()+" LibroId: "+prestito.getLibro().getId()+ " - "+prestito.getLibro().getTitolo());
            });
            Prestito prestito2bis=null;
            if (lista2.size()>0)
                prestito2bis=lista2.get(0);
            if (ok2bis)
                System.out.println("utente piero è riuscito a prendere il libro");
            else System.out.println("WARNING utente piero non è riuscito a prendere il libro");
            Prestito prestitoPerNotifica2=lista3.get(0);
            boolean oknotifica2=utente2.riceviNotificaScadenza(prestitoPerNotifica2);
            System.out.println("Adesso mi addormento per 20 secondi");
            Thread.sleep(20000);
            if (prestito2bis!=null) {
                utente2.restituisci(prestito2bis.getId());
                System.out.println("utente piero ha restituito il libro");

            }
            System.out.println("FINE");
        }
//        List<Libro> libriCercati=utente.cercaLibro("", "Asimov");
//        System.out.println("Libri trovati ");
//
//        List<Libro> libriCercati2=utente.cercaLibro(null, "Asimov");
//        System.out.println("Libri trovati ");
//        for(Libro l: libriCercati2) {
//            System.out.println(l.toString());
//        }
//        Libro libroCercato=libriCercati2.get(0);
//        boolean prendi=utente.prendiInPrestito(libroCercato);
//        System.out.println("Preso in prestito: "+prendi);
//        boolean prolunga=utente.prolungaPrestito(libroCercato);
//        System.out.println("Prolunga prestito: "+prolunga);
//        boolean restituisci=utente.restituisci(libroCercato);
//        System.out.println("restituzione: "+restituisci);
//        List<Prestito> not = utente.fetchListaNotifiche();
//        System.out.println("***notifiche");
//        for(Prestito n: not) {
//            System.out.println(n.getLibro().getTitolo()+ " - scadenza : "+n.getScadenza().toString());
//        }
//        utente.printListaNotifiche();
   }
    private static void serverH2() {
        try {
            // Avvia il server TCP di H2 sulla porta 9093
            Server server = Server.createTcpServer("-tcpPort", "9093", "-tcpAllowOthers").start();

            // Avvia la console web di H2 sulla porta 8082
            Server webServer = Server.createWebServer("-webPort", "8082", "-webAllowOthers").start();

            System.out.println("Server H2 is started on port 9093. Press [Enter] to stop.");
            System.in.read();

            server.stop();
            webServer.stop();
        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }
    }




    }
