package org.biblioteca.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.sql.*;
import java.util.Objects;

public class Libro {
    private Integer id;
    private String autore;
    private String titolo;
    private String editore;
    private Integer anno;
    private Integer disponibilita;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getAutore() {
        return autore;
    }

    public void setAutore(String autore) {
        this.autore = autore;
    }

    public Integer getDisponibilita() {
        return disponibilita;
    }

    public void setDisponibilita(Integer disponibilita) {
        this.disponibilita = disponibilita;
    }

    public Libro() {
    }

    public Libro(Integer id, String autore, String titolo, String editore, Integer anno) {
        this.id=id;
        this.autore = autore;
        this.titolo = titolo;
        this.editore = editore;
        this.anno = anno;
        this.disponibilita=0;
    }



    public String getTitolo() {
        return titolo;
    }

    public void setTitolo(String titolo) {
        this.titolo = titolo;
    }

    public String getEditore() {
        return editore;
    }

    public void setEditore(String editore) {
        this.editore = editore;
    }

    public Integer getAnno() {
        return anno;
    }

    public void setAnno(Integer anno) {
        this.anno = anno;
    }

    @Override
    public String toString() {
        return this.id+" - "+this.getAutore()+", "+this.getTitolo()+", "+this.getEditore()+" - "+this.getAnno();
    }

    public boolean persist(Connection conn) throws SQLException {
        PreparedStatement pStatement = conn.prepareStatement("INSERT INTO LIBRO(AUTORE, TITOLO, EDITORE, ANNO, DISPONIBILITA) VALUES (?, ?,?,?,?)", Statement.RETURN_GENERATED_KEYS);
        pStatement.setString(1, this.getAutore());
        pStatement.setString(2, this.getTitolo());
        pStatement.setString(3, this.getEditore());
        pStatement.setInt(4, this.getAnno());
        pStatement.setInt(5,this.disponibilita);
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
        PreparedStatement pStatement = conn.prepareStatement("UPDATE LIBRO SET  DISPONIBILITA=? WHERE ID=? ", Statement.RETURN_GENERATED_KEYS);
        pStatement.setInt(1,this.getDisponibilita());
        pStatement.setInt(2, this.id);
        int affectedRows = pStatement.executeUpdate();
        pStatement.close();
        return affectedRows != 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Libro libro)) return false;
        return Objects.equals(id, libro.id) && Objects.equals(autore, libro.autore) && Objects.equals(titolo, libro.titolo) && Objects.equals(editore, libro.editore) && Objects.equals(anno, libro.anno);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, autore, titolo, editore, anno);
    }

    public boolean delete(Connection conn) throws SQLException {
        PreparedStatement pStatement = conn.prepareStatement("DELETE FROM LIBRO  WHERE ID=? ", Statement.RETURN_GENERATED_KEYS);
        pStatement.setInt(1, this.id);
        int affectedRows = pStatement.executeUpdate();
        pStatement.close();
        return affectedRows != 0;
    }
}
