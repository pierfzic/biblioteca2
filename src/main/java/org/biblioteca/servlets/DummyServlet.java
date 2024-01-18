package org.biblioteca.servlets;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.biblioteca.model.Utente;

import javax.servlet.annotation.WebServlet;

import java.io.IOException;
import java.util.stream.Collectors;

import static org.biblioteca.Main.PATH_WEBAPP_SERVLET;

@WebServlet(name = "DummyServletServlet", urlPatterns = "/*", loadOnStartup = 7)
public class DummyServlet extends HttpServlet {

    @Override
    public void init() throws ServletException {

    }
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String contextPath = request.getContextPath();
        HttpSession session = request.getSession();
        Utente  currentUser;
        if (session != null)
          currentUser = (Utente) session.getAttribute("currentUser");
        else {
            currentUser = null;
        }
        if (currentUser!=null) {
            response.sendRedirect(contextPath+PATH_WEBAPP_SERVLET+"/menu/userpage.html");
        } else {
            response.sendRedirect(contextPath+PATH_WEBAPP_SERVLET+"/login/login.html");
        }

    }


}
