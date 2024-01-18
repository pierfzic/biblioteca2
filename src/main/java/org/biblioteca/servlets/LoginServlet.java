package org.biblioteca.servlets;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.biblioteca.model.Utente;
import org.biblioteca.utils.CustomLogger;
import org.h2.tools.Server;
import org.biblioteca.utils.ConfigLoader;
import java.io.*;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.List;
import java.util.Map;

import static org.biblioteca.Main.*;

@WebServlet(name = "LoginServlet", urlPatterns = {PATH_LOGIN_SERVLET+"/*"}, loadOnStartup = 1)
public class LoginServlet extends HttpServlet {
    private Connection connDb;
    private List<Utente> listaUtenti;
    private String currentSessionid;
    private ConfigLoader properties;
    private final CustomLogger logger = CustomLogger.getInstance();


    @Override
    public void init() throws ServletException {
        ServletContext context = getServletContext();

        this.connDb = (Connection) context.getAttribute("connectionDb");
        try {
            this.properties = new ConfigLoader("config.properties");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.listaUtenti= (List<Utente>) context.getAttribute("users");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        login(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        login(req, resp);
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

    private String sendGetRequest(String urlString) throws IOException, URISyntaxException, InterruptedException {
        HttpClient client = HttpClient.newBuilder()
                .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(urlString))
                //.header("Cookie", "JSESSIONID="+this.currentSessionid +".node0")
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        Map<String, List<String>> headers = response.headers().map();
        List<String> cookies = headers.get("set-cookie");
        return response.body();
    }

    private String sendPostRequest(String urlString, String body) throws IOException, URISyntaxException, InterruptedException {
        HttpClient client = HttpClient.newBuilder()
                .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(urlString))
                //.header("Cookie", "JSESSIONID="+this.currentSessionid +".node0")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        Map<String, List<String>> headers = response.headers().map();
        List<String> cookies = headers.get("set-cookie");
        return response.body();
    }

    private void login(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession();
        String contextPath = request.getContextPath();
        String requestUri = request.getRequestURI();
        StringBuffer sb=new StringBuffer();
        Utente currentUser = null;
        PrintWriter out = response.getWriter();
        String path = requestUri.substring(contextPath.length());
        String pathCartella=path.substring(request.getServletPath().length());
        String pagina = path.substring(path.lastIndexOf('/') + 1);
        String estensione = pagina.substring(pagina.lastIndexOf('.') + 1);

        if (requestUri.endsWith("/entrance/logout")) {
            closeSession(request,response);
            currentUser.setSessionId("");
            currentUser=null;
            this.currentSessionid="";
            return;
        }

        if (request.getParameter("username")==null) {
            response.setContentType("text/"+estensione+";charset=UTF-8");
            session.setAttribute("firstLogin", Boolean.TRUE);
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
            currentUser=this.listaUtenti.stream().filter(utente -> (utente.getId().equals(idUser))).toList().get(0);
            // Crea una nuova sessione
            session = request.getSession();
            this.currentSessionid=session.getId();
            currentUser.setSessionId(session.getId());

            try {
                sendGetRequest("http://localhost:8080"+contextPath+PATH_SERVICES_SERVLET+"/dummy");
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            session.setAttribute("username", username);
            session.setAttribute("iduser", idUser);
            session.setAttribute("currentUser", currentUser);
            Utente utentecorrente= (Utente) session.getAttribute("currentUser");
            //  timeout
            session.setMaxInactiveInterval(30 * 60);
            // user page
            response.sendRedirect(contextPath+PATH_WEBAPP_SERVLET+"/menu/userpage.html");
        } else {
            // login failed
            response.sendRedirect(contextPath+PATH_WEBAPP_SERVLET+"/login/loginerror.html");
        }
    }


}
