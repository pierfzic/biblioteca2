package org.biblioteca.servlets;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sun.mail.smtp.SMTPAddressFailedException;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.biblioteca.model.Libro;
import org.biblioteca.model.Prestito;
import org.biblioteca.model.Utente;
import org.biblioteca.utils.ConfigLoader;
import org.biblioteca.utils.CustomLogger;
import org.h2.tools.Server;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Collectors;

import static org.biblioteca.Main.PATH_SERVICES_SERVLET;
import static org.biblioteca.Main.PATH_WEBAPP_SERVLET;
@javax.servlet.annotation.WebServlet(name = "BibliotecaServlet", urlPatterns = {PATH_SERVICES_SERVLET+"/*"}, loadOnStartup = 3)
public class BibliotecaServlet extends HttpServlet {
    private Integer PORT_EMAIL ;
    private String PASSWORD_EMAIL ;
    private Integer GIORNI_PRESTITO;
    private Integer GIORNI_DA_NOTIFICA;
    private Integer PERIODO_PROROGA;
    private String HOST_MAIL;
    private String FROM_EMAIL;
    private String SUBJECT_SCADENZA;
    private List<Libro> listaLibri;
    private List<Prestito> listaPrestiti;
    private List<Prestito> listaNotifichePrestiti;
    private Connection connDb;
    private ConfigLoader properties;
    private List<Utente> listaUtenti;

    private final CustomLogger logger = CustomLogger.getInstance();

    @Override
    public void init() {
        try {
            properties = new ConfigLoader("config.properties");
            GIORNI_PRESTITO = Integer.parseInt(properties.getProperty("periodo.prestito"));
            PERIODO_PROROGA = Integer.parseInt(properties.getProperty("periodo.proroga"));
            GIORNI_DA_NOTIFICA = Integer.parseInt(properties.getProperty("giorni.danotifica"));
            HOST_MAIL = properties.getProperty("host.email");
            PORT_EMAIL=Integer.parseInt(properties.getProperty("port.host.email"));
            FROM_EMAIL = properties.getProperty("from.email");
            PASSWORD_EMAIL=properties.getProperty("password.email");
            SUBJECT_SCADENZA = properties.getProperty("subject.email");

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        startH2Server();
        try {
            inizializzaLista();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        ServletContext context = getServletContext();
        context.setAttribute("connectionDb", this.connDb);
        context.setAttribute("properties", this.properties);
        context.setAttribute("users", this.listaUtenti);
        context.setAttribute("books", this.listaLibri);
        context.setAttribute("loans", this.listaPrestiti);

    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        String contextPath = req.getContextPath();
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        if (false) {
            //check autenticazione

            String username = null;
            if (session != null && session.getAttribute("username") != null) {
                // L'utente è loggato
                username = (String) session.getAttribute("username");
                // Gestisci la richiesta dell'utente autenticato
            } else {
                // L'utente non è loggato, reindirizza al login
                resp.sendRedirect(contextPath+PATH_WEBAPP_SERVLET+"/login.html");
            }
        }


        String requestUri = req.getRequestURI();
        resp.setContentType("application/json");
        PrintWriter out = resp.getWriter();
        if (requestUri.endsWith("/dummy")) {
            out.println("");
        }

        if (requestUri.endsWith("/listalibri")) {
            String jsonString = mapper.writeValueAsString(this.listaLibri);
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().println(jsonString);
        }
        if (requestUri.endsWith("/listautenti")) {
            String jsonString = mapper.writeValueAsString(this.listaUtenti);
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().println(jsonString);
        }
        if (requestUri.endsWith("/ricerca")) {
            String srcAutore = req.getParameter("autore");
            String srcTitolo = req.getParameter("titolo");
            List<Libro> libriTrovati = ricercaLibro(srcAutore, srcTitolo);
            if ((libriTrovati != null) && (libriTrovati.size() > 0)) {
                ServletContext context = getServletContext();
                context.setAttribute("searchResults", libriTrovati);
                String jsonString = mapper.writeValueAsString(libriTrovati);
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().println(jsonString);
            } else {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.getWriter().println("");
            }
        }
        if (requestUri.endsWith("/prestito")) {
            Integer idUtente = Integer.parseInt(req.getParameter("utente"));
            Integer idLibro = Integer.parseInt(req.getParameter("libro"));
            Utente utente = this.listaUtenti.stream().filter(utente1 -> (utente1.getId().equals(idUtente))).toList().get(0);
            Libro libro = this.listaLibri.stream().filter(libro1 -> (libro1.getId().equals(idLibro))).toList().get(0);
            Prestito prestito = prestito(utente, libro);
            if (prestito != null) {
                String jsonString = null;
                try {
                    jsonString = mapper.writeValueAsString(prestito.getId());
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }

                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().println(jsonString);
            } else {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.getWriter().println("");
            }
        }
        if (requestUri.endsWith("/libroInPrestito")) {
            Integer idLibro = Integer.parseInt(req.getParameter("libro"));
            Libro libro = this.listaLibri.stream().filter(libro1 -> (libro1.getId().equals(idLibro))).toList().get(0);
            List<Prestito> prestati=this.listaPrestiti.stream().filter(prestito -> (prestito.getLibro().getId().equals(idLibro) && (prestito.getRestituito()==null) ) ).toList();
            String jsonString = null;
            if ((prestati==null) || (prestati.size()==0))
                jsonString="false";
            else jsonString="true";
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().println(jsonString);

        }

        if (requestUri.endsWith("/cancellalibro")) {
            Integer idLibro = Integer.parseInt(req.getParameter("libro"));
            Libro libro = this.listaLibri.stream().filter(libro1 -> (libro1.getId().equals(idLibro))).toList().get(0);
            boolean ok=this.listaLibri.remove(libro);
            try {
                libro.delete(connDb);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            if (ok)
                resp.getWriter().println("");
            else resp.getWriter().println("false");
        }

        if (requestUri.endsWith("/eliminaUtente")) {
            Integer idUtente = Integer.parseInt(req.getParameter("utente"));
            Utente daEliminare=this.listaUtenti.stream().filter(utente -> utente.getId().equals(idUtente)).toList().get(0);
            boolean ok=this.listaUtenti.remove(daEliminare);
            try {
                daEliminare.delete(connDb);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            if (ok)
                resp.getWriter().println("");
            else resp.getWriter().println("false");
        }
        if (requestUri.endsWith("/restituzione")) {
            Integer idPrestito = Integer.parseInt(req.getParameter("prestito"));
            Prestito prestito = restituisci(idPrestito);
            if (prestito != null) {
                String jsonString = mapper.writeValueAsString(prestito);
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().println(jsonString);
            } else {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.getWriter().println("");
            }
        }
        if (requestUri.endsWith("/proroga")) {
            Integer idPrestito = Integer.parseInt(req.getParameter("prestito"));
            Prestito prestito = prorogaPrestito(idPrestito);
            if (prestito != null) {
                String jsonString = mapper.writeValueAsString(prestito);
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().println(jsonString);
            } else {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.getWriter().println("");
            }
        }
        if (requestUri.endsWith("/listanotifiche")) {
            Integer idUtente = Integer.parseInt(req.getParameter("utente"));
            Utente utente = this.listaUtenti.stream().filter(utente1 -> (utente1.getId().equals(idUtente))).toList().get(0);
            List<Prestito> inScadenza = listaNotifichePerUtente(idUtente);
            if (inScadenza != null) {
                String jsonString = mapper.writeValueAsString(inScadenza);
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().println(jsonString);
            } else {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.getWriter().println("");
            }
        }

        if (requestUri.endsWith("/listaprestitiutente")) {
            Integer idUtente = Integer.parseInt(req.getParameter("utente"));
            Utente utente = this.listaUtenti.stream().filter(utente1 -> (utente1.getId().equals(idUtente))).toList().get(0);
            List<Prestito> prestitiUtente = getListaPrestitiUtente(idUtente);
            if (prestitiUtente != null) {
                String jsonString = mapper.writeValueAsString(prestitiUtente);
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().println(jsonString);
            } else {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.getWriter().println("");
            }

        }
        if (requestUri.endsWith("/notificascadenze")) {
            notificaUtenti();
        }

    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String contextPath = req.getContextPath();
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        String requestUri = req.getRequestURI();
        resp.setContentType("application/json");
        PrintWriter out = resp.getWriter();
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = req.getReader();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        String requestBody = sb.toString();

        if (requestUri.endsWith("/ricevinotifica")) {
            Prestito prestito = mapper.readValue(requestBody, Prestito.class);
            boolean ok;
            try {
                ok = richiediNotifica(prestito);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            if (ok) {
                String jsonString = mapper.writeValueAsString(ok);
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().println(jsonString);
            } else {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.getWriter().println("");
            }
        }


    }

    private void inizializzaLista() throws SQLException {
        //deletePrestiti(); //TODO: da cancellare finiti i test
        Statement stmt = connDb.createStatement();
        logger.logInfo("Inizializzo la lista di utenti");
        this.listaUtenti = new ArrayList<Utente>();
        ResultSet rsUsers = stmt.executeQuery("SELECT * FROM UTENTE");
        while (rsUsers.next()) {
            Utente user = new Utente(rsUsers.getInt("ID"), rsUsers.getString("USERNAME"), rsUsers.getString("PASSWORD"));
            user.setIsAdmin(rsUsers.getBoolean("IS_ADMIN"));
            this.listaUtenti.add(user);

        }
        logger.logInfo("Inizializzo la lista di libri");
        this.listaLibri = new ArrayList<Libro>();
        ResultSet rs = stmt.executeQuery("SELECT * FROM LIBRO");
        while (rs.next()) {
            Libro libro = new Libro(rs.getInt("ID"), rs.getString("AUTORE"), rs.getString("TITOLO"),
                    rs.getString("EDITORE"), rs.getInt("ANNO"));
            libro.setDisponibilita(rs.getInt("DISPONIBILITA"));
            this.listaLibri.add(libro);


        }
        logger.logInfo("Inizializzo la lista di prestiti");
        this.listaPrestiti = new ArrayList<Prestito>();
        ResultSet rsLoans = stmt.executeQuery("SELECT * FROM PRESTITO");
        while (rsLoans.next()) {
            Integer idUtente = rsLoans.getInt("ID_USER");
            Integer idLibro = rsLoans.getInt("ID_LIBRO");
            Utente utente = this.listaUtenti.stream().filter(utente1 -> (Objects.equals(utente1.getId(), idUtente))).toList().get(0);
            Libro libro = this.listaLibri.stream().filter(libro1 -> (Objects.equals(libro1.getId(), idLibro))).toList().get(0);
            Prestito prestito = new Prestito(rsLoans.getInt("ID"), utente, libro, rsLoans.getDate("SCADENZA").toLocalDate());
            prestito.setNotificato(rsLoans.getBoolean("NOTIFICATO"));
            prestito.setNotificare(rsLoans.getBoolean("NOTIFICARE"));
            this.listaPrestiti.add(prestito);
        }

        logger.logInfo("SERVLET AVVIATA!");
    }


    public List<Libro> ricercaLibro(String autore, String titolo) {
        if ((autore != null) && (titolo == null)) {
            List<Libro> libroTrovato = this.listaLibri.stream()
                    .filter(libro -> (libro.getAutore().contains(autore)))
                    .collect(Collectors.toList());
            if (libroTrovato != null) {
                return libroTrovato;
            }
        }
        if ((autore == null) && (titolo != null)) {
            List<Libro> libroTrovato = this.listaLibri.stream()
                    .filter(libro -> (libro.getTitolo().contains(titolo)))
                    .collect(Collectors.toList());
            if (libroTrovato != null) {
                return libroTrovato;
            }
        }
        List<Libro> libroTrovato = this.listaLibri.stream()
                .filter(libro -> (libro.getAutore().contains(autore) && libro.getTitolo().contains(titolo)))
                .collect(Collectors.toList());
        return libroTrovato;
    }

    public Prestito prestito(Utente user, Libro librodaPrestare) {
        Libro libro = this.listaLibri.stream().filter(libro1 -> libro1.getId().equals(librodaPrestare.getId())).toList().get(0);
        logger.logInfo("Utente " + user.getUsername() + " prende in prestito il libro " + libro.getTitolo());
        if (libro.getDisponibilita() < 1) {
            logger.logWarning("Libro " + libro.toString() + " non disponibile ");
            return null;
        }
        Prestito p = new Prestito(0, user, libro, LocalDate.now().plusDays(GIORNI_PRESTITO));
        p.setRestituito(null);
        Integer disponibile = libro.getDisponibilita();
        libro.setDisponibilita(--disponibile);
        try {
            libro.update(connDb);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        try {
            p.persist(connDb);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        this.listaPrestiti.add(p);
        return p;
    }

    public Prestito restituisci(Integer idPrestito) {
        Prestito p = this.listaPrestiti.stream().filter(prestito -> (prestito.getId().equals(idPrestito))).toList().get(0);
        if (p==null)
            return  null;
        Libro libro = p.getLibro();
        Utente user = p.getUser();
        logger.logInfo("Utente " + user.getUsername() + " restituisce il libro " + libro.getTitolo());

        if ((p != null) && (p.getRestituito()==null)) {
            p.setRestituito(LocalDate.now());
            Integer disponibile = libro.getDisponibilita();
            libro.setDisponibilita(disponibile + 1);
            try {
                libro.update(connDb);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            try {
                p.update(this.connDb);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            logger.logInfo("Restituzione del libro " + libro.getTitolo() + " riuscita!");
            return p;
        }
        return p;


    }

    public Prestito prorogaPrestito(Integer idPrestito) {
        Prestito p = this.listaPrestiti.stream().filter(prestito1 -> (prestito1.getId().equals(idPrestito))).toList().get(0);
        if (p==null)
            return  null;
        Libro libro = p.getLibro();
        Utente user = p.getUser();
        if ((p != null) && (p.getRestituito()==null)) {
            logger.logInfo("Utente " + user.getUsername() + " proroga  prestito del libro " + libro.getTitolo());
            p.setScadenza(p.getScadenza().plusDays(PERIODO_PROROGA));
            try {
                p.update(connDb);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            return p;
        } else {
            logger.logInfo("Utente " + user.getUsername() + " non può prorogare il prestito del libro " + libro.getTitolo());
            return null;
        }
    }

    public List<Prestito> listaNotifichePerUtente(Integer userId) { //TODO: DA OTTIMIZZARE
        List<Prestito> scadenze=new ArrayList<>();
        for( Prestito prestito: this.listaPrestiti) {
            //           List<Prestito> scadenze = this.listaPrestiti.stream()
            //                   .filter(prestito -> //
            if                   (((prestito.getScadenza().minusDays(GIORNI_DA_NOTIFICA).isEqual(LocalDate.now())) || (prestito.getScadenza().minusDays(GIORNI_DA_NOTIFICA).isBefore(LocalDate.now())))
                    &&  (prestito.getUser().getId().equals(userId))
                    && (prestito.isNotificare())
                    && (!prestito.isNotificato())
                    && (prestito.getRestituito() == null))
                scadenze.add(prestito);
            //                   .collect(Collectors.toList());
        }
        return scadenze;
//        for(Prestito p: scadenze) {
//            Utente user=p.getUser();
//            Libro libro=p.getLibro();
//            LocalDate scadenza=p.getScadenza();
//            user.addNotifica(libro,scadenza);
//            this.listaNotifichePrestiti.remove(p);
//        }
    }

    private boolean richiediNotifica(Prestito prestito) throws SQLException {
        if (prestito == null)
            return false;
        Prestito prestitoDaNotificare=this.listaPrestiti.stream().filter(prestito1 -> (prestito1.getId().equals(prestito.getId()))).toList().get(0);
        prestitoDaNotificare.setNotificare(true);
        prestitoDaNotificare.setNotificato(false);
        prestitoDaNotificare.update(connDb);
        logger.logInfo("Richiesta notifica per prestito id="+prestito.getId()+" da utente "+prestito.getUser().getUsername());
        return true;
    }

    private boolean eliminaUtente(Integer idUtente) {
        Utente daEliminare=this.listaUtenti.stream().filter(utente -> utente.getId().equals(idUtente)).toList().get(0);
        boolean ok=this.listaUtenti.remove(daEliminare);
        try {
            return daEliminare.delete(connDb);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Prestito> getPrestitiUtente(Utente user) {
        List<Prestito> prestitiUtente = this.listaPrestiti.stream()
                .filter(prestito -> (prestito.getUser().equals(user)))
                .collect(Collectors.toList());
        return prestitiUtente;

    }

    private void deletePrestiti() throws SQLException {
        Statement stmt = connDb.createStatement();

        stmt.executeUpdate("DELETE FROM PRESTITO");
        stmt.executeUpdate("UPDATE LIBRO SET DISPONIBILITA=1 WHERE ID=2");
        stmt.close();

    }

    private void startH2Server() {

        String dbUrl = properties.getProperty("database.url");
        String dbUser = properties.getProperty("database.user");
        String dbPwd = properties.getProperty("database.password");
        String dbDriver = properties.getProperty("database.driver");
        String dbPort = properties.getProperty("database.port");

        try {
            Class.forName(dbDriver);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        try {
            Server server = Server.createTcpServer("-tcpPort", dbPort, "-tcpAllowOthers").start();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        logger.logInfo("Server H2 avviato e in ascolto sulla porta " + dbPort);

        try {
            this.connDb = DriverManager.getConnection(dbUrl, dbUser, dbPwd);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void notificaUtenti() {
        for(Utente utente: this.listaUtenti) { //TODO:OTTIMIZZARE


            List<Prestito> notifiche = listaNotifichePerUtente(utente.getId());
            for (Prestito p : notifiche) {
                StringBuilder testoMail = new StringBuilder();
                testoMail.append("La scadenza del prestito del libro ")
                        .append(p.getLibro().getTitolo())
                        .append(" si avvicina.")
                        .append("Il prestito scade il giorno " + p.getScadenza().toString());
                sendEmail(utente.getUsername(), FROM_EMAIL, HOST_MAIL, SUBJECT_SCADENZA, testoMail.toString());
                p.setNotificato(true);
                try {
                    p.update(connDb);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        logger.logInfo("Notifiche eseguite!");
    }

    private void sendEmail(String to, String from, String host, String subject, String text) {
        logger.logInfo("Ho inviato mail di notifica scadenza a " + to);
        // Imposta le proprietà per la connessione SMTP
        Properties properties = System.getProperties();
        properties.setProperty("mail.smtp.host", host);
        properties.setProperty("mail.smtp.port", String.valueOf(PORT_EMAIL)); // Modifica con la porta SMTP del tuo server
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        // Crea una sessione
        Session mailSession = Session.getInstance(properties, new javax.mail.Authenticator() { protected PasswordAuthentication getPasswordAuthentication() { return new PasswordAuthentication(FROM_EMAIL, PASSWORD_EMAIL); }
        });

        try {
            // Crea un oggetto MimeMessage predefinito
            MimeMessage message = new MimeMessage(mailSession);

            // Imposta i parametri dell'e-mail
            message.setFrom(new InternetAddress(from));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
            message.setSubject(subject);
            message.setText(text);

            // Invia l'e-mail
            try {
                Transport.send(message);
            } catch (SMTPAddressFailedException smtpe) {
                System.err.println(smtpe.getAddress().getAddress()+" : "+smtpe.getMessage());
                logger.logSevere("Errore nell'invio della mail di notifica "+smtpe.getMessage());
                return;
            } catch (SendFailedException sfe) {
                System.err.println("Errore nell'invio della mail : "+sfe.getMessage());
                logger.logSevere("Errore nell'invio della mail di notifica "+sfe.getMessage());
                return;
            }
        } catch (MessagingException mex) {
            mex.printStackTrace();
            logger.logSevere("Errore nell'invio della mail di notifica "+mex.getMessage());
        }
    }

    public List<Prestito> getListaPrestitiUtente(Integer idUtente) {
        return this.listaPrestiti.stream().filter(prestito -> (prestito.getUser().getId().equals(idUtente))).toList();
    }

}
