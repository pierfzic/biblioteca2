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

import javax.servlet.annotation.WebServlet;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.util.List;
import java.util.stream.Collectors;

import static org.biblioteca.Main.PATH_BACKEND_SERVLET;
import static org.biblioteca.Main.PATH_WEBAPP_SERVLET;

@WebServlet(name = "BackEndServlet", urlPatterns = {PATH_BACKEND_SERVLET+"/*"}, loadOnStartup = 5)
public class BackEndServlet  extends HttpServlet {

    Utente currentUser;
    Connection connDb;
    ConfigLoader properties;
    List<Libro> listaLibri;
    List<Utente> listaUtenti;
    List<Prestito> listaPrestiti;
    @Override
    public void init() throws ServletException {
//        Utente utente=new Utente();
//        utente.setUsername("admin");
//        utente.setPassword("admin");
//        utente.setId(1);

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
        //session.setAttribute("currentUser", this.currentUser);
        //this.currentUser=listaUtenti.get(0);
        String requestUri = req.getRequestURI();
        resp.setContentType("text/html");
        PrintWriter out = resp.getWriter();

        if (requestUri.endsWith("/listalibri")) {
            List<Libro> listalibri=currentUser.listalibri();
            session.setAttribute("listalibri",listalibri);
            resp.sendRedirect(contextPath+PATH_WEBAPP_SERVLET+"/userpage.html");
        }
        if (requestUri.endsWith("/cercalibro")) {
            String srcAutore=req.getParameter("autore");
            String srcTitolo=req.getParameter("titolo");
            List<Libro> searchResults = currentUser.cercaLibro(srcTitolo, srcAutore);
            session.setAttribute("searchResults", searchResults);
            //context.setAttribute("searchResults", searchResults);
            resp.sendRedirect(contextPath+PATH_WEBAPP_SERVLET+"/userpage.html");
        }
        if (requestUri.endsWith("/chiediprestito")) {
            Integer idLibro=Integer.parseInt(req.getParameter("libro"));
            List<Libro> libri=this.listaLibri.stream().filter(libro1 -> (libro1.getId().equals(idLibro))).collect(Collectors.toList());
            if ((libri!=null) && (libri.size()>0)) {
                Libro libroScelto=libri.get(0);
                boolean ok = currentUser.prendiInPrestito(libroScelto);
                if (ok) {
                    session.setAttribute("statusMsg", "Hai preso in prestito: "+libroScelto.getAutore()+" - "+libroScelto.getTitolo());
                }
                else
                    session.setAttribute("statusMsg", "Il libro scelto non è disponibile");
            }
            resp.sendRedirect(contextPath+PATH_WEBAPP_SERVLET+"/userpage.html");
        }
        if (requestUri.endsWith("/restituisciprestito")) {
            Integer idPrestito=Integer.parseInt(req.getParameter("prestito"));
            Prestito daRestituire=this.listaPrestiti.stream().filter(prestito1 -> (prestito1.getId().equals(idPrestito))).toList().get(0);
            if (daRestituire.getUser().equals(currentUser) &&
                    (daRestituire.getRestituito()==null) ) { //può essere restituito
                boolean okrestituzione=currentUser.restituisci(idPrestito);
                if (okrestituzione) {
                    session.setAttribute("statusLoanMsg", "Hai restituito il libro " + daRestituire.getLibro().getAutore() + " - " + daRestituire.getLibro().getTitolo());
                    resp.sendRedirect(contextPath+PATH_WEBAPP_SERVLET+"/restituzionelibro.html");
                }
            }

        }
        if (requestUri.endsWith("/notificascadenza")) {
            Integer idPrestito=Integer.parseInt(req.getParameter("prestito"));
            Prestito daNotificare=this.listaPrestiti.stream().filter(prestito1 -> (prestito1.getId().equals(idPrestito))).toList().get(0);
            if (daNotificare.getUser().equals(currentUser) &&
                    (daNotificare.getRestituito()==null) ) { //può essere notificato
                boolean oknotifica=currentUser.riceviNotificaScadenza(daNotificare);
                if (oknotifica) {
                    session.setAttribute("statusLoanMsg", "Hai chiesto la notifica per la scadenza del prestito di " + daNotificare.getLibro().getAutore() + " - " + daNotificare.getLibro().getTitolo());
                    resp.sendRedirect(contextPath+PATH_WEBAPP_SERVLET+"/restituzionelibro.html");
                }
            }

        }


        if (requestUri.endsWith("/prorogaprestito")) {
            Integer idPrestito = Integer.parseInt(req.getParameter("prestito"));
            Prestito prestito = this.listaPrestiti.stream().filter(prestito1 -> (prestito1.getId().equals(idPrestito))).toList().get(0);
            Libro libro = prestito.getLibro();
            boolean ok = currentUser.prolungaPrestito(prestito);
            if (ok) {
               session.setAttribute("statusLoanMsg", "Hai prorogato: " + libro.getAutore() + " -" + libro.getTitolo());
            } else {
                session.setAttribute("statusLoanMsg", "");
            }
            resp.sendRedirect(contextPath+PATH_WEBAPP_SERVLET+"/restituzionelibro.html");

        }



    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doPost(req, resp);
        String contextPath = req.getContextPath();
        String requestUri = req.getRequestURI();
        ObjectMapper mapper = new ObjectMapper();
        resp.setContentType("application/json");
        PrintWriter out = resp.getWriter();
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = req.getReader();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        String requestBody = sb.toString();
    }

    @Override
    public void destroy() {
        super.destroy();

    }
}
