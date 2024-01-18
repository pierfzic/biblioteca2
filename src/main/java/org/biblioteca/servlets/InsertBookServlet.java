package org.biblioteca.servlets;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.biblioteca.model.Libro;
import org.biblioteca.model.Utente;
import org.biblioteca.utils.ConfigLoader;


import java.io.IOException;
import java.sql.*;
import java.util.List;
import java.util.stream.Collectors;

import static org.biblioteca.Main.PATH_INSERTBOOK_SERVLET;
import static org.biblioteca.Main.PATH_WEBAPP_SERVLET;

@WebServlet(name = "InsertBookServlet", urlPatterns = {PATH_INSERTBOOK_SERVLET+"/*"}, loadOnStartup = 6)
public class InsertBookServlet extends HttpServlet {
    private ConfigLoader properties;
    private List<Utente> listaUtenti;
    private List<Libro> listaLibri;
    private Connection connDb;


    @Override
    public void init() throws ServletException {
        super.init();
        ServletContext context = getServletContext();
        this.connDb = (Connection) context.getAttribute("connectionDb");
        this.properties = (ConfigLoader) context.getAttribute("properties");
        this.listaUtenti = (List<Utente>) context.getAttribute("users");
        this.listaLibri = (List<Libro>) context.getAttribute("books");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
       inserisciNuovoLibro(request, response);
    }
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws  IOException {
       inserisciNuovoLibro(request, response);
    }


    private void inserisciNuovoLibro(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String contextPath = request.getContextPath();
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        //check autenticazione
        HttpSession session = request.getSession();
        String username = null;

        Integer idUser;
        if (session != null)
            idUser = (Integer) session.getAttribute("iduser");
        else {
            idUser = 0;
        }
        Utente currUser = this.listaUtenti.stream().filter(utente -> (utente.getId().equals(idUser))).collect(Collectors.toList()).get(0);

        if (session != null && currUser.getIsAdmin()) {
            // L'utente è loggato ed è admin
            username = (String) session.getAttribute("username");
            // Gestisci la richiesta dell'utente autenticato
        } else {
            // L'utente non è loggato o non è admin, reindirizza al login
            response.sendRedirect(contextPath+PATH_WEBAPP_SERVLET+"/login/login.html");
        }

        String autore = request.getParameter("autore");
        String titolo = request.getParameter("titolo");
        String editore = request.getParameter("editore");
        Integer anno = Integer.parseInt(request.getParameter("anno"));
        Integer disponibilita = Integer.parseInt(request.getParameter("disponibilita"));
        Libro nuovoLibro = new Libro(0, autore, titolo, editore, anno);
        nuovoLibro.setDisponibilita(disponibilita);
        boolean ok;
        try {
            ok=nuovoLibro.persist(connDb);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        if (ok) {
            this.listaLibri.add(nuovoLibro);
            response.sendRedirect(contextPath+PATH_WEBAPP_SERVLET+ PATH_INSERTBOOK_SERVLET+"/insertsuccess.html");
        } else response.sendRedirect(contextPath+PATH_WEBAPP_SERVLET+PATH_INSERTBOOK_SERVLET+"/insertfailed.html");

    }
    @Override
    public void destroy() {
        super.destroy();
    }

}
