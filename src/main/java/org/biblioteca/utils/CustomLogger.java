package org.biblioteca.utils;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.*;
import java.io.IOException;

public class CustomLogger {
    private static CustomLogger instance;
    private Logger logger;
    public static final Level DEBUG = new Level("DEBUG", Level.INFO.intValue() + 1) {};

    private CustomLogger() {

        logger = Logger.getLogger("bibliotecaLog");
        logger.setUseParentHandlers(false);
        try {
            FileHandler fileHandler = new FileHandler("biblioteca.log", true);
            //fileHandler.setFormatter(new SimpleFormatter());
            fileHandler.setFormatter(new MyCustomFormatter());
            logger.addHandler(fileHandler);

            ConsoleHandler consoleHandler = new ConsoleHandler();
            consoleHandler.setFormatter(new MyCustomFormatter());
            logger.addHandler(consoleHandler);

        } catch (IOException e) {
            logger.severe("Errore nella configurazione del FileHandler: " + e.getMessage());
        }
    }

    public static CustomLogger getInstance() {
        if (instance == null) {
            instance = new CustomLogger();
        }
        return instance;
    }

    public void logInfo(String message) {
        logger.info(message);
    }

    public void logWarning(String message) {
        logger.warning(message);
    }

    public void logSevere(String message) {
        logger.severe(message);
    }

    public void logDebug(String message) {
        logger.log(DEBUG, message);
    }

    // Classe Formatter personalizzata
    private class MyCustomFormatter extends Formatter {
        @Override
        public String format(LogRecord record) {
            ZonedDateTime dateTime=record.getInstant().atZone(ZoneId.systemDefault());
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
            String formattedDateTime = dateTime.format(formatter);
            return dateTime.toString()+" "+record.getLevel() + " : " +
                    record.getSourceClassName()+" : "+ record.getMessage() + System.lineSeparator();
        }
    }


}
