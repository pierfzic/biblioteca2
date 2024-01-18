package org.biblioteca.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.TimerTask;

import static org.biblioteca.Main.PATH_APP;
import static org.biblioteca.Main.PATH_SERVICES_SERVLET;

public class TaskNotificheScadenza extends TimerTask {
    private final String URL_SERVICES="http://localhost:8080"+PATH_APP+PATH_SERVICES_SERVLET;
    @Override
    public void run() {
        String url=URL_SERVICES+"/notificascadenze";
        String resp;
        try {
             resp=sendGetRequest(url);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String sendGetRequest(String uriServizio) throws IOException {
        URL url = new URL(uriServizio);
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
}
