package org.biblioteca.servlets;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.biblioteca.model.Utente;
import org.eclipse.jetty.server.Authentication;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.List;

import static org.biblioteca.Main.PATH_LOGIN_SERVLET;
import static org.biblioteca.Main.PATH_WEBAPP_SERVLET;

@WebServlet(name = "LoginServlet", urlPatterns = {PATH_LOGIN_SERVLET+"/*"}, loadOnStartup = 3)
public class LoginServlet extends HttpServlet {
    private Connection connDb;
    private List<Utente> listaUtenti;

    @Override
    public void init() throws ServletException {
        ServletContext context = getServletContext();
        this.listaUtenti= (List<Utente>) context.getAttribute("users");
        this.connDb = (Connection) context.getAttribute("connectionDb");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String contextPath = request.getContextPath();
        ServletContext context = getServletContext();
        String requestUri = request.getRequestURI();
        String uri = request.getRequestURI();
        StringBuffer sb=new StringBuffer();

        if (requestUri.endsWith("/entrance/logout")) {
            closeSession(request,response);
            return;
        }


            // Ottiene il servletPath
            String servletPath = request.getServletPath();
            String path = uri.substring(contextPath.length());
            String pathCartella=path.substring(request.getServletPath().length());
            String pagina = path.substring(path.lastIndexOf('/') + 1);
            String estensione = pagina.substring(pagina.lastIndexOf('.') + 1);
            // Ottieni il writer della risposta

            PrintWriter out = response.getWriter();
        if (request.getParameter("username")==null) {
            response.setContentType("text/"+estensione+";charset=UTF-8");
            try (BufferedReader reader = new BufferedReader(new FileReader("."+path))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            } catch (FileNotFoundException e) {
                out.println("<html><body><h2>File non trovato</h2></body></html>");
            } catch (IOException e) {
                out.println("<html><body><h2>Errore durante la lettura del file</h2></body></html>");
            }

            out.println(sb.toString());
            return;
        }


        String username = request.getParameter("username");
        String password = request.getParameter("password");
        String hashedPassword = hashMD5(password);
        // Verifica le credenziali dell'utente
        Integer idUser;
        try {
            idUser = authenticate(username, hashedPassword);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        if (idUser>0) {
            Utente currentUser=this.listaUtenti.stream().filter(utente -> (utente.getId().equals(idUser))).toList().get(0);
            // Crea una nuova sessione
            HttpSession session = request.getSession();
            session.setAttribute("username", username);
            session.setAttribute("iduser", idUser);
            session.setAttribute("currentUser", currentUser);
            Utente utentecorrente= (Utente) session.getAttribute("currentUser");
            //  timeout
            session.setMaxInactiveInterval(30 * 60);
            // user page
            response.sendRedirect(contextPath+PATH_WEBAPP_SERVLET+"/userpage.html");
        } else {
            // login failed
            response.sendRedirect(contextPath+PATH_WEBAPP_SERVLET+"/login/loginerror.html");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String contextPath = req.getContextPath();
        ObjectMapper mapper = new ObjectMapper();
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
        String paramUsername=req.getParameter("username");
        String paramPassword=req.getParameter("password");
            //Utente user = mapper.readValue(requestBody, Utente.class);
            Utente user=new Utente(0,paramUsername,paramPassword);
            boolean ok;
            String hashedPassword = hashMD5(user.getPassword());
            Integer idUser;
            try {
                idUser = authenticate(user.getUsername(), hashedPassword);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        if (idUser>0) {
            Utente currentUser=this.listaUtenti.stream().filter(utente -> (utente.getId().equals(idUser))).toList().get(0);
            // Crea una nuova sessione
            HttpSession session = req.getSession();
            session.setAttribute("username", currentUser.getUsername());
            session.setAttribute("iduser", idUser);
            session.setAttribute("currentUser", currentUser);

            Utente utentecorrente= (Utente) session.getAttribute("currentUser");
                session.setMaxInactiveInterval(30 * 60);
                resp.sendRedirect(contextPath+PATH_WEBAPP_SERVLET+"/userpage.html");
            } else {
                // login failed
                resp.sendRedirect(contextPath+PATH_WEBAPP_SERVLET+"/login/loginerror.html");
            }

    }

    private Integer authenticate(String username, String hpassword) throws SQLException {
        PreparedStatement pStatement = connDb.prepareStatement("SELECT ID, PASSWORD FROM UTENTE WHERE USERNAME=?", Statement.RETURN_GENERATED_KEYS);
        pStatement.setString(1, username);
        ResultSet rsUsers = pStatement.executeQuery();
        Integer idUtente=-1;
        String hashedPasswordFromDb = null;
        boolean ok=false;
        if (rsUsers.next()) {
            idUtente = rsUsers.getInt("ID");
            hashedPasswordFromDb=rsUsers.getString("PASSWORD");
            ok = hpassword.equals(hashedPasswordFromDb);
        }
        if (ok)
            return idUtente;
        else return -1;
    }

    private void closeSession(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String contextPath = request.getContextPath();
        HttpSession session = request.getSession();
        if (session != null) {
            session.invalidate();
        }
        response.sendRedirect(contextPath+PATH_WEBAPP_SERVLET+"/login/login.html");
    }

    private String hashMD5(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(password.getBytes());
            byte[] hashedPasswordBytes = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : hashedPasswordBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("ERROR founding MD5 hashing algorithm", e);
        }
    }
}
