package org.biblioteca.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.sql.*;
import java.time.LocalDate;

public class Prestito {

    private Integer id;
    private Utente user;
    private Libro libro;
    private LocalDate scadenza;
    private LocalDate restituito;
    private boolean notificare=false;
    private boolean notificato=false;



    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public LocalDate getRestituito() {
        return restituito;
    }

    public void setRestituito(LocalDate restituito) {
        this.restituito = restituito;
    }

    public Prestito() {
    }

    public Prestito(Integer id, Utente user, Libro libro, LocalDate scadenza) {
        this.id=id;
        this.user = user;
        this.libro=libro;
        this.scadenza = scadenza;
        this.notificare=false;
        this.notificato=false;
    }

    public Utente getUser() {
        return user;
    }

    public void setUser(Utente user) {
        this.user = user;
    }

    public LocalDate getScadenza() {
        return scadenza;
    }

    public void setScadenza(LocalDate scadenza) {
        this.scadenza = scadenza;
    }

    public Libro getLibro() {
        return libro;
    }

    public void setLibro(Libro libro) {
        this.libro = libro;
    }

    public boolean isNotificato() {
        return notificato;
    }

    public void setNotificato(boolean notificato) {
        this.notificato = notificato;
    }

    public boolean isNotificare() {
        return notificare;
    }

    public void setNotificare(boolean notificare) {
        this.notificare = notificare;
    }

    public boolean persist(Connection conn) throws SQLException {
        PreparedStatement pStatement = conn.prepareStatement("INSERT INTO PRESTITO(ID_USER, ID_LIBRO, SCADENZA, NOTIFICARE, NOTIFICATO) VALUES (?, ?,?, FALSE,FALSE)", Statement.RETURN_GENERATED_KEYS);
        pStatement.setInt(1, this.user.getId());
        pStatement.setInt(2, this.libro.getId());
        pStatement.setDate(3, java.sql.Date.valueOf(this.scadenza));
        int affectedRows = pStatement.executeUpdate();
        try (ResultSet generatedKeys = pStatement.getGeneratedKeys()) {
            if (generatedKeys.next()) {
                Integer idGenerato = generatedKeys.getInt("ID");
                this.id=idGenerato;
            }
            else {
                throw new SQLException("Error. No ID available.");
            }
        }
        pStatement.close();
        return affectedRows != 0;
    }

    public boolean update(Connection conn) throws SQLException {
        PreparedStatement pStatement = conn.prepareStatement("UPDATE PRESTITO SET SCADENZA=?, RESTITUITO=?, NOTIFICATO=?, NOTIFICARE=? WHERE ID=? ", Statement.RETURN_GENERATED_KEYS);
        pStatement.setDate(1, java.sql.Date.valueOf(this.scadenza));
        if (this.restituito!=null)
            pStatement.setDate(2, java.sql.Date.valueOf(this.restituito));
        else
            pStatement.setNull(2,Types.DATE);
        pStatement.setBoolean(3, this.notificato);
        pStatement.setBoolean(4,this.notificare);
        pStatement.setInt(5, this.id);
        int affectedRows = pStatement.executeUpdate();
        pStatement.close();
        return affectedRows != 0;
    }
}
