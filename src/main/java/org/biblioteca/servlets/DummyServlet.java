package org.biblioteca.servlets;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import javax.servlet.annotation.WebServlet;

import java.io.IOException;

import static org.biblioteca.Main.PATH_WEBAPP_SERVLET;

@WebServlet(name = "DummyServletServlet", urlPatterns = "/*")
public class DummyServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String contextPath = req.getContextPath();
        resp.sendRedirect(contextPath+PATH_WEBAPP_SERVLET+"/login/login.html");
    }

    @Override
    public void init() throws ServletException {

    }
}
