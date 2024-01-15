package org.biblioteca.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static org.biblioteca.Main.PATH_BACKEND_SERVLET;
import static org.biblioteca.Main.PATH_SERVICES_SERVLET;

public class Utente {
    @JsonIgnore
    private final String URL_SERVICES="http://localhost:8080/biblioteca"+PATH_SERVICES_SERVLET;
    @JsonIgnore
    private final String URL_BACKEND="http://localhost:8080/biblioteca"+PATH_BACKEND_SERVLET;

    private Integer id;

    private String username;
    @JsonIgnore
    private String password;

    private boolean isAdmin=false;
    @JsonIgnore
    private List<Prestito> listaPrestitiUtente;

    public void setListaNotifiche(List<Notifica> listaNotifiche) {
        this.listaNotifiche = listaNotifiche;
    }

    private List<Notifica> listaNotifiche;

    public List<Notifica> getListaNotifiche() {
        return listaNotifiche;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }


    public Utente() {
        listaPrestitiUtente =new ArrayList<>();
        listaNotifiche=new ArrayList<>();
    }

    public Utente(Integer id, String username, String password) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.isAdmin=false;
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
        //searchListaPrestiti();
        return searchListaPrestiti();
    }

    public boolean getIsAdmin() {
        return isAdmin;
    }

    public void setIsAdmin(boolean admin) {
        isAdmin = admin;
    }

    public void setListaPrestitiUtente(List<Prestito> listaPrestitiUtente) {
        this.listaPrestitiUtente = listaPrestitiUtente;
    }

    public boolean prendiInPrestito(Libro libro) throws IOException {
        if (libro==null)
            return false;
        ObjectMapper mapper=new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        Integer idUtente=this.getId();
        Integer idLibro=libro.getId();
        String response= sendGetRequest(URL_SERVICES+"/prestito?utente="+idUtente+"&libro="+idLibro);
        //searchListaPrestiti();
        return !response.equals("");

    }
    public boolean restituisci(Integer idPrestito) throws IOException {
        ObjectMapper mapper=new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        String response= sendGetRequest(URL_SERVICES+"/restituzione?prestito="+idPrestito);
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
        String response= sendGetRequest(URL_SERVICES+"/ricerca?autore="+encodedAutore+"&titolo="+encodedTitolo);
        List<Libro> result= Collections.emptyList();
        if (!response.equals(""))
            result = mapper.readValue(response, new TypeReference<List<Libro>>() {});
        return result;


    }

    public List<Prestito> fetchListaNotifiche() throws IOException {
        ObjectMapper mapper=new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        Integer userId=this.getId();
        String response= sendGetRequest(URL_SERVICES+"/listanotifiche?utente="+userId);
        List<Prestito> result = mapper.readValue(response, new TypeReference<List<Prestito>>(){});
        return result;

    }
    public List<Libro> listalibri() throws IOException {
        ObjectMapper mapper=new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        String response= sendGetRequest(URL_SERVICES+"/listalibri");
        List<Libro> result =  mapper.readValue(response, new TypeReference<List<Libro>>(){});

        sendGetRequest(URL_SERVICES+"/listalibri");
        return result;
    }
    public boolean riceviNotificaScadenza(Prestito prestito) throws IOException {
        if (prestito==null)
            return false;
        ObjectMapper mapper=new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        String body=mapper.writeValueAsString(prestito);
        String response= sendPostRequest2(URL_SERVICES+"/ricevinotifica", body);
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
        String response= sendGetRequest(URL_SERVICES+"/proroga?prestito="+prestito.getId());
//      Prestito p = mapper.readValue(response, Prestito.class);
        return !response.equals("");
    }

    private String sendGetRequest(String uriServizio) throws IOException {
        String urlString = uriServizio;

        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        String inputLine;
        StringBuilder response = new StringBuilder();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        } catch (java.io.FileNotFoundException e) {
           response.append("");
           return response.toString();
        }
        while ((inputLine = reader.readLine()) != null) {
            response.append(inputLine);
        }
        reader.close();
        connection.disconnect();
        return response.toString();
    }


    public String sendPostRequest2(String requestUrl, String jsonInputString) {
        String resp="";
        HttpURLConnection connection = null;

        try {
            URL url = new URL(requestUrl);
            connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json; utf-8");
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoOutput(true);

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();

            // Gestione delle risposte
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                resp= response.toString();
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return resp;
    }

    public static void sendPostRequest3(String requestUrl, String jsonInputString) {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(requestUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonInputString))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    private String sendPutRequest(String requestUrl, String jsonInputString) throws IOException {
        String resp="";
        HttpURLConnection connection = null;

        try {
            URL url = new URL(requestUrl);
            connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("PUT");
            connection.setRequestProperty("Content-Type", "application/json; utf-8");
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoOutput(true);

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();

            // Gestione delle risposte
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                resp= response.toString();
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return resp;
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
            PreparedStatement pStatement = conn.prepareStatement("UPDATE UTENTE SET USERNAME=?, PASSWORD=? WHERE ID=? ", Statement.RETURN_GENERATED_KEYS);
        pStatement.setString(1,this.getUsername());
        pStatement.setString(2,hashMD5(this.getPassword()));
        pStatement.setInt(3, this.id);
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
        String response= sendGetRequest(URL_SERVICES+"/listaprestitiutente?utente="+idUtente);
        List<Prestito> result = mapper.readValue(response, new TypeReference<List<Prestito>>(){});
        return result;
    }
}
