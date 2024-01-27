package org.biblioteca.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.*;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.*;
import java.nio.charset.StandardCharsets;

import static org.biblioteca.Main.*;

public class Utente {
    @JsonIgnore
    private  String sessionId;
    @JsonIgnore
    private final String URL_SERVICES="http://localhost:8080"+PATH_APP+PATH_SERVICES_SERVLET;
    @JsonIgnore
    private final String URL_BACKEND="http://localhost:8080"+PATH_APP+PATH_BACKEND_SERVLET;

    private Integer id;

    private String username;
    @JsonIgnore
    private String password;

    private boolean isAdmin=false;
    @JsonIgnore
    private List<Prestito> listaPrestitiUtente;



    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }


    public Utente() {
        listaPrestitiUtente =new ArrayList<>();
    }

    public Utente(Integer id, String username, String password) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.isAdmin=false;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public List<Prestito> getListaPrestitiUtente() throws IOException {
        return searchListaPrestiti();
    }

    public boolean getIsAdmin() {
        return isAdmin;
    }

    public void setIsAdmin(boolean admin) {
        isAdmin = admin;
    }

    public boolean prendiInPrestito(Libro libro) throws IOException {
        if (libro==null)
            return false;
        ObjectMapper mapper=new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        Integer idUtente=this.getId();
        Integer idLibro=libro.getId();
        String response= null;
        try {
            response = sendGetRequest(URL_SERVICES+"/prestito?utente="+idUtente+"&libro="+idLibro);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        //searchListaPrestiti();
        return !response.equals("");

    }
    public boolean restituisci(Integer idPrestito) throws IOException {
        ObjectMapper mapper=new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        String response= null;
        try {
            response = sendGetRequest(URL_SERVICES+"/restituzione?prestito="+idPrestito);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        Prestito p= mapper.readValue(response, Prestito.class);
        //searchListaPrestiti();
        return p != null;
    }
    public List<Libro> cercaLibro(String titolo, String autore) throws IOException {
        ObjectMapper mapper=new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        if (titolo==null)
            titolo="";
        if (autore==null)
            autore="";
        String encodedTitolo = URLEncoder.encode(titolo, StandardCharsets.UTF_8.toString());
        String encodedAutore = URLEncoder.encode(autore, StandardCharsets.UTF_8.toString());
        String response= null;
        try {
            response = sendGetRequest(URL_SERVICES+"/ricerca?autore="+encodedAutore+"&titolo="+encodedTitolo);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        List<Libro> result= Collections.emptyList();
        if (!response.equals("") && !response.equals("\r\n"))
            result = mapper.readValue(response, new TypeReference<List<Libro>>() {});
        return result;


    }

    public List<Libro> listalibri() throws IOException {
        ObjectMapper mapper=new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        String response= null;
        try {
            response = sendGetRequest(URL_SERVICES+"/listalibri");
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        List<Libro> result =  mapper.readValue(response, new TypeReference<List<Libro>>(){});

        try {
            sendGetRequest(URL_SERVICES+"/listalibri");
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return result;
    }
    public boolean riceviNotificaScadenza(Prestito prestito) throws IOException, URISyntaxException, InterruptedException {
        if (prestito==null)
            return false;
        ObjectMapper mapper=new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        String body=mapper.writeValueAsString(prestito);
        String response= sendPostRequest(URL_SERVICES+"/ricevinotifica", body);
        if (response==null)
            return false;
        boolean ok=mapper.readValue(response, Boolean.class);
        return ok;

    }

    public boolean prolungaPrestito(Prestito prestito) throws IOException {
        if (prestito==null)
            return false;
        ObjectMapper mapper=new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        Integer idUtente=prestito.getUser().getId();
        Integer idLibro=prestito.getLibro().getId();
        String response= null;
        try {
            response = sendGetRequest(URL_SERVICES+"/proroga?prestito="+prestito.getId());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
//      Prestito p = mapper.readValue(response, Prestito.class);
        return !response.equals("");
    }

    public boolean cancellaLibro(Libro daCancellare) {
        if (daCancellare == null)
            return false;
        String response;

        if (this.getIsAdmin()) {
            try {
                response = sendGetRequest(URL_SERVICES + "/cancellalibro?libro=" + daCancellare.getId());
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return !response.equals("");
        }
        else return false;

    }

    private String sendGetRequest(String urlString) throws IOException, URISyntaxException, InterruptedException {
        HttpClient client = HttpClient.newBuilder()
                .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(urlString))
                .header("Cookie", "JSESSIONID="+this.sessionId +".node0")
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        Map<String, List<String>> headers = response.headers().map();
        List<String> cookies = headers.get("set-cookie");
        return response.body();
    }

    private String sendPostRequest(String urlString, String bodyString) throws IOException, URISyntaxException, InterruptedException {
        HttpClient client = HttpClient.newBuilder()
                .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(urlString))
                .header("Content-Type", "application/json; utf-8")
                .header("Accept", "application/json")
                .header("Cookie", "JSESSIONID="+this.sessionId +".node0")
                .POST(HttpRequest.BodyPublishers.ofString(bodyString))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        Map<String, List<String>> headers = response.headers().map();
        List<String> cookies = headers.get("set-cookie");
        return response.body();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Utente utente)) return false;
        return Objects.equals(username, utente.username) && this.id.equals(((Utente) o).getId()) ;
    }

    @Override
    public int hashCode() {
        return Objects.hash(username, password);
    }

    public boolean persist(Connection conn) throws SQLException {
        PreparedStatement pStatement = conn.prepareStatement("INSERT INTO UTENTE(USERNAME, PASSWORD, IS_ADMIN) VALUES (?, ?,?)", Statement.RETURN_GENERATED_KEYS);
        pStatement.setString(1, this.getUsername());
        pStatement.setString(2, hashMD5(this.getPassword()));
        pStatement.setBoolean(3, this.getIsAdmin());
        int affectedRows = pStatement.executeUpdate();
        try (ResultSet generatedKeys = pStatement.getGeneratedKeys()) {
            if (generatedKeys.next()) {
                Integer idGenerato = generatedKeys.getInt("ID");
                this.id=idGenerato;
            }
            else {
                throw new SQLException("ERROR. No generated ID");
            }
        }
        pStatement.close();
        return affectedRows != 0;
    }

    public boolean update(Connection conn) throws SQLException {
        PreparedStatement pStatement = conn.prepareStatement("UPDATE UTENTE SET USERNAME=?, PASSWORD=?, IS_ADMIN=? WHERE ID=? ", Statement.RETURN_GENERATED_KEYS);
        pStatement.setString(1,this.getUsername());
        pStatement.setString(2,hashMD5(this.getPassword()));
        pStatement.setBoolean(3, this.getIsAdmin());
        pStatement.setInt(4, this.id);
        int affectedRows = pStatement.executeUpdate();
        pStatement.close();
        return affectedRows != 0;
    }
    public boolean updatewoutPwd(Connection conn) throws SQLException {
        PreparedStatement pStatement = conn.prepareStatement("UPDATE UTENTE SET USERNAME=?, IS_ADMIN=? WHERE ID=? ", Statement.RETURN_GENERATED_KEYS);
        pStatement.setString(1,this.getUsername());
        pStatement.setBoolean(2, this.getIsAdmin());
        pStatement.setInt(3, this.id);
        int affectedRows = pStatement.executeUpdate();
        pStatement.close();
        return affectedRows != 0;
    }

    public boolean delete(Connection conn) throws SQLException {
        PreparedStatement pStatement = conn.prepareStatement("DELETE FROM UTENTE  WHERE ID=? ", Statement.RETURN_GENERATED_KEYS);
        pStatement.setInt(1,this.getId());
        int affectedRows = pStatement.executeUpdate();
        pStatement.close();
        return affectedRows != 0;
    }

    private String hashMD5(String password) {
        if (password==null)
            return "";
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
            throw new RuntimeException("MD5 hashing algorithm not found", e);
        }
    }

    public List<Prestito> searchListaPrestiti() throws IOException {
        ObjectMapper mapper=new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        Integer idUtente=this.getId();
        String response= null;
        try {
            response = sendGetRequest(URL_SERVICES+"/listaprestitiutente?utente="+idUtente);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        List<Prestito> result = mapper.readValue(response, new TypeReference<List<Prestito>>(){});
        return result;
    }

    public boolean cambiaPassword(Connection connDb, String oldPassword, String newPassword) throws SQLException {
        String hashOldPassword=hashMD5(oldPassword);
        String hashNewPassword=hashMD5(newPassword);
        PreparedStatement pStatement = connDb.prepareStatement("SELECT PASSWORD FROM UTENTE WHERE ID=?", Statement.RETURN_GENERATED_KEYS);
        pStatement.setInt(1, this.getId());
        ResultSet rsUsers = pStatement.executeQuery();
        String hashedPasswordFromDb = null;
        boolean abilitato=false;
        if (rsUsers.next()) {
            hashedPasswordFromDb=rsUsers.getString("PASSWORD");
            abilitato = hashOldPassword.equals(hashedPasswordFromDb);
        }
        boolean okpersist=false;
        if (abilitato) {
            this.setPassword(newPassword);
            okpersist=this.update(connDb);
        }

        return okpersist;
    }


}
