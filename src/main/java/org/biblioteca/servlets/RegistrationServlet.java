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
import org.biblioteca.model.Utente;
import org.biblioteca.utils.ConfigLoader;
import org.h2.jdbc.JdbcSQLIntegrityConstraintViolationException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

import java.sql.*;
import java.util.List;
import java.util.stream.Collectors;

import static org.biblioteca.Main.PATH_REGISTER_SERVLET;
import static org.biblioteca.Main.PATH_WEBAPP_SERVLET;

@WebServlet(name = "RegistrationServlet", urlPatterns = {PATH_REGISTER_SERVLET+"/*"}, loadOnStartup = 4)
public class RegistrationServlet extends HttpServlet {
    private Connection connDb;
    private ConfigLoader properties;
    private List<Utente> listaUtenti;

    @Override
    public void init()  {
        ServletContext context = getServletContext();
        this.connDb = (Connection) context.getAttribute("connectionDb");
        this.properties= (ConfigLoader) context.getAttribute("properties");
        this.listaUtenti= (List<Utente>) context.getAttribute("users");
    }


    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        inserisciNuovoUtente(request, response);

    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        inserisciNuovoUtente(req, resp);
    }

    private void inserisciNuovoUtente(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String contextPath = request.getContextPath();
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        //check autenticazione
        HttpSession session = request.getSession();
        String username = null;

        Integer idUser;
        if (session!=null)
            idUser=(Integer) session.getAttribute("iduser");
        else {
            idUser = 0;
        }
        Utente currUser=this.listaUtenti.stream().filter(utente -> (utente.getId().equals(idUser))).collect(Collectors.toList()).get(0);

        if (session != null && currUser.getIsAdmin()) {
            // L'utente è loggato ed è admin
            username = (String) session.getAttribute("username");
            // Gestisci la richiesta dell'utente autenticato
        } else {
            // L'utente non è loggato o non è admin, reindirizza al login
            response.sendRedirect(contextPath+PATH_WEBAPP_SERVLET+"/login/login.html");
        }

        String nuovousername= request.getParameter("username");
        String nuovapassword= request.getParameter("password");
        String stringIsAdmin=request.getParameter("isAdmin");
        boolean isAdmin = (stringIsAdmin != null && stringIsAdmin.equals("on"));

        Utente nuovoUtente=new Utente();
        nuovoUtente.setUsername(nuovousername);
        nuovoUtente.setPassword(nuovapassword);
        nuovoUtente.setIsAdmin(isAdmin);
        boolean ok;
        try {
            ok=nuovoUtente.persist(this.connDb);
        } catch (JdbcSQLIntegrityConstraintViolationException  e) {
            ok=false;
            response.sendRedirect(contextPath+PATH_WEBAPP_SERVLET+"/registration/usernameexists.html");
        } catch (SQLException e) {
            ok=false;
        }
        if (ok) {
            this.listaUtenti.add(nuovoUtente);
            response.sendRedirect(contextPath+PATH_WEBAPP_SERVLET+"/registration/registrationsuccess.html");
        }
        else response.sendRedirect(contextPath+PATH_WEBAPP_SERVLET+"/registration/registrationfailed.html");
    }

}
