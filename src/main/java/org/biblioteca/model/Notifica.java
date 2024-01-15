package org.biblioteca.model;

import org.biblioteca.model.Libro;

import java.time.LocalDate;

public class Notifica {
    private Libro libro;
    private LocalDate scadenza;

    public Libro getLibro() {
        return libro;
    }

    public void setLibro(Libro libro) {
        this.libro = libro;
    }

    public LocalDate getScadenza() {
        return scadenza;
    }

    public void setScadenza(LocalDate scadenza) {
        this.scadenza = scadenza;
    }
}
