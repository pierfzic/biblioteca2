package org.biblioteca.servlets;

import com.fasterxml.jackson.databind.ObjectMapper;
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

import javax.servlet.annotation.WebServlet;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

import static org.biblioteca.Main.PATH_BACKEND_SERVLET;
import static org.biblioteca.Main.PATH_WEBAPP_SERVLET;

@WebServlet(name = "BackEndServlet", urlPatterns = {PATH_BACKEND_SERVLET+"/*"}, loadOnStartup = 5)
public class BackEndServlet  extends HttpServlet {

    private Utente currentUser;
    private Connection connDb;
    private ConfigLoader properties;
    private List<Libro> listaLibri;
    private List<Utente> listaUtenti;
    private List<Prestito> listaPrestiti;
    private final CustomLogger logger = CustomLogger.getInstance();
    @Override
    public void init() throws ServletException {
        ServletContext context = getServletContext();
        this.listaUtenti= (List<Utente>) context.getAttribute("users");
        this.connDb = (Connection) context.getAttribute("connectionDb");
        this.listaLibri= (List<Libro>) context.getAttribute("books");
        this.listaPrestiti= (List<Prestito>) context.getAttribute("loans" );
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession();
        try {
            this.currentUser= (Utente) session.getAttribute("currentUser");
        } catch (NullPointerException npe) {

        }
        ServletContext context = getServletContext();
        this.listaUtenti= (List<Utente>) context.getAttribute("users");
        this.connDb = (Connection) context.getAttribute("connectionDb");
        this.listaLibri= (List<Libro>) context.getAttribute("books");
        this.listaPrestiti= (List<Prestito>) context.getAttribute("loans" );

        String contextPath = req.getContextPath();
        String requestUri = req.getRequestURI();
        resp.setContentType("text/html");

        if (requestUri.endsWith("/listalibri")) {
            List<Libro> listalibri=currentUser.listalibri();
            session.setAttribute("listalibri",listalibri);
            resp.sendRedirect(contextPath+PATH_WEBAPP_SERVLET+"/menu/listaLibri.html");
        }
        if (requestUri.endsWith("/listautenti")) {
            if (currentUser.getIsAdmin()) {
                List<Utente> listautenti = (List<Utente>) context.getAttribute("users");
                session.setAttribute("listautenti", listautenti);
                resp.sendRedirect(contextPath + PATH_WEBAPP_SERVLET + "/menu/listaUtenti.html");
            }
        }

        if (requestUri.endsWith("/cercalibro")) {
            String srcAutore=req.getParameter("autore");
            String srcTitolo=req.getParameter("titolo");
            List<Libro> searchResults = currentUser.cercaLibro(srcTitolo, srcAutore);
            session.setAttribute("searchResults", searchResults);
            resp.sendRedirect(contextPath+PATH_WEBAPP_SERVLET+"/menu/userpage.html");
        }
        if (requestUri.endsWith("/chiediprestito")) {
            Integer idLibro=Integer.parseInt(req.getParameter("libro"));
            List<Libro> libri=this.listaLibri.stream().filter(libro1 -> (libro1.getId().equals(idLibro))).collect(Collectors.toList());
            if ((libri!=null) && (libri.size()>0)) {
                Libro libroScelto=libri.get(0);
                boolean ok = currentUser.prendiInPrestito(libroScelto);
                session.setAttribute("statusMsg", ok? "Hai preso in prestito: "+libroScelto.getAutore()+" - "+libroScelto.getTitolo():"Il libro scelto non è disponibile");
            }
            resp.sendRedirect(contextPath+PATH_WEBAPP_SERVLET+"/menu/userpage.html");
        }
        if (requestUri.endsWith("/restituisciprestito")) {
            Integer idPrestito=Integer.parseInt(req.getParameter("prestito"));
            Prestito daRestituire=this.listaPrestiti.stream().filter(prestito1 -> (prestito1.getId().equals(idPrestito))).toList().get(0);
            if (daRestituire.getUser().equals(currentUser) &&
                    (daRestituire.getRestituito()==null) ) { //può essere restituito
                boolean okrestituzione=currentUser.restituisci(idPrestito);
                if (okrestituzione) {
                    session.setAttribute("statusLoanMsg", "Hai restituito il libro " + daRestituire.getLibro().getAutore() + " - " + daRestituire.getLibro().getTitolo());
                    resp.sendRedirect(contextPath+PATH_WEBAPP_SERVLET+"/menu/restituzionelibro.html");
                }
            }
        }
        if (requestUri.endsWith("/notificascadenza")) {
            Integer idPrestito=Integer.parseInt(req.getParameter("prestito"));
            Prestito daNotificare=this.listaPrestiti.stream().filter(prestito1 -> (prestito1.getId().equals(idPrestito))).toList().get(0);
            if (daNotificare.getUser().equals(currentUser) &&
                    (daNotificare.getRestituito()==null) ) { //può essere notificato
                boolean oknotifica= false;
                try {
                    oknotifica = currentUser.riceviNotificaScadenza(daNotificare);
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                if (oknotifica) {
                    session.setAttribute("statusLoanMsg", "Hai chiesto la notifica per la scadenza del prestito di " + daNotificare.getLibro().getAutore() + " - " + daNotificare.getLibro().getTitolo());
                    resp.sendRedirect(contextPath+PATH_WEBAPP_SERVLET+"/menu/restituzionelibro.html");
                }
            }

        }
        if (requestUri.endsWith("/prorogaprestito")) {
            Integer idPrestito = Integer.parseInt(req.getParameter("prestito"));
            Prestito prestito = this.listaPrestiti.stream().filter(prestito1 -> (prestito1.getId().equals(idPrestito))).toList().get(0);
            Libro libro = prestito.getLibro();
            boolean ok = currentUser.prolungaPrestito(prestito);
            session.setAttribute("statusLoanMsg", ok? "Hai prorogato: " + libro.getAutore() + " -" + libro.getTitolo() :"");
            logger.logInfo(currentUser.getUsername()+" ha prorograto il prestito del libro "+libro.getAutore()+" - "+libro.getTitolo());
            resp.sendRedirect(contextPath+PATH_WEBAPP_SERVLET+"/menu/restituzionelibro.html");
        }

        if (requestUri.endsWith("/cancellalibro")) {
            if (currentUser.getIsAdmin()) {
                Integer idLibro = Integer.parseInt(req.getParameter("libro"));
                Libro daCancellare=this.listaLibri.stream().filter(libro -> libro.getId().equals(idLibro)).toList().get(0);
                boolean ok=currentUser.cancellaLibro(daCancellare);
                session.setAttribute("statusMsg", ok? "Hai cancellato: " + daCancellare.getAutore() + " -" + daCancellare.getTitolo() :"");
                resp.sendRedirect(contextPath+PATH_WEBAPP_SERVLET+"/menu/listaLibri.html");
                logger.logInfo(this.currentUser+" ha cancellato il libro "+daCancellare.getAutore()+" - "+daCancellare.getTitolo());
            } else resp.sendRedirect(contextPath+PATH_WEBAPP_SERVLET+"/menu/userpage.html");
        }
        if (requestUri.endsWith("/eliminaUtente")) {
            if (currentUser.getIsAdmin()) {
                Integer idUtente = Integer.parseInt(req.getParameter("utente"));
                Utente daCancellare=this.listaUtenti.stream().filter(utente -> utente.getId().equals(idUtente)).toList().get(0);
                String deletedUsername=daCancellare.getUsername();
                this.listaUtenti.remove(daCancellare);
                boolean ok= false;
                try {
                    ok = daCancellare.delete(connDb);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                session.setAttribute("statusMsg", ok? "Hai cancellato l'utente : ID=" + daCancellare.getId()+ " - " + daCancellare.getUsername() :"");
                resp.sendRedirect(contextPath+PATH_WEBAPP_SERVLET+"/menu/listaUtenti.html");
                logger.logInfo(this.currentUser+" ha eliminato l'utente "+daCancellare.getUsername());
            } else resp.sendRedirect(contextPath+PATH_WEBAPP_SERVLET+"/menu/userpage.html");
        }
        if (requestUri.endsWith("/setAdmin")) {
            if (currentUser.getIsAdmin()) {
                Integer idUtente = Integer.parseInt(req.getParameter("utente"));
                Utente daModificare=this.listaUtenti.stream().filter(utente -> utente.getId().equals(idUtente)).toList().get(0);
                daModificare.setIsAdmin(true);
                try {
                    daModificare.update(connDb);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                session.setAttribute("statusMsg",  "Hai promosso " + daModificare.getUsername() + " ad amministratore");
                resp.sendRedirect(contextPath+PATH_WEBAPP_SERVLET+"/menu/listaUtenti.html");
                logger.logInfo(currentUser.getUsername()+" ha promosso "+daModificare.getUsername()+ " ad amministratore");
            } else resp.sendRedirect(contextPath+PATH_WEBAPP_SERVLET+"/menu/userpage.html");
        }
        if (requestUri.endsWith("/unsetAdmin")) {
            if (currentUser.getIsAdmin()) {
                Integer idUtente = Integer.parseInt(req.getParameter("utente"));
                Utente daModificare=this.listaUtenti.stream().filter(utente -> utente.getId().equals(idUtente)).toList().get(0);
                daModificare.setIsAdmin(false);
                try {
                    daModificare.updatewoutPwd(connDb);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                session.setAttribute("statusMsg",  "Hai tolto i diritti di amministratore a " + daModificare.getUsername());
                resp.sendRedirect(contextPath+PATH_WEBAPP_SERVLET+"/menu/listaUtenti.html");
                logger.logInfo(currentUser.getUsername()+" ha tolto i diritti di amministratore a "+daModificare.getUsername());
            } else resp.sendRedirect(contextPath+PATH_WEBAPP_SERVLET+"/menu/userpage.html");
        }

    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession();
        try {
            this.currentUser= (Utente) session.getAttribute("currentUser");
        } catch (NullPointerException npe) {

        }
        ServletContext context = getServletContext();
        this.listaUtenti= (List<Utente>) context.getAttribute("users");
        this.connDb = (Connection) context.getAttribute("connectionDb");
        this.listaLibri= (List<Libro>) context.getAttribute("books");
        this.listaPrestiti= (List<Prestito>) context.getAttribute("loans" );

        String contextPath = req.getContextPath();
        String requestUri = req.getRequestURI();
        resp.setContentType("text/html");

        if (requestUri.endsWith("/cambiaPassword")) {
            if (currentUser!=null) {
                List<Utente> listautenti = (List<Utente>) context.getAttribute("users");
                String vecchiaPassword=req.getParameter("oldpassword");
                String nuovaPassWord=req.getParameter("newpassword");
                boolean ok= false;
                try {
                    ok = currentUser.cambiaPassword(this.connDb, vecchiaPassword,nuovaPassWord);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                if (ok) {
                    logger.logInfo("utente "+this.currentUser.getUsername()+" ha cambiato password con successo");
                    resp.sendRedirect(contextPath + PATH_WEBAPP_SERVLET + "chgpwdOK.html");
                }
                else {
                    logger.logWarning("fallimento nel cambio password dell'utente "+this.currentUser.getUsername());
                    resp.sendRedirect(contextPath+PATH_WEBAPP_SERVLET+"chgpwdfailed.html");
                }
            }
        }
    }

    @Override
    public void destroy() {
        super.destroy();

    }
}
