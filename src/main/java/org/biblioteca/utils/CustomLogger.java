package org.biblioteca.utils;

import java.time.ZoneId;
import java.util.logging.*;
import java.io.IOException;

public class CustomLogger {
    private static CustomLogger instance;
    private Logger logger;
    public static final Level DEBUG = new Level("DEBUG", Level.INFO.intValue() + 1) {};

    private CustomLogger() {
        logger = Logger.getLogger("bibliotecaLog");
        try {
            FileHandler fileHandler = new FileHandler("biblioteca.log", true);
            //fileHandler.setFormatter(new SimpleFormatter());
            fileHandler.setFormatter(new MyCustomFormatter());
            logger.addHandler(fileHandler);
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
            return record.getInstant().atZone(ZoneId.systemDefault()).toLocalDateTime().toString()+" "+record.getLevel() + ": " +
                    record.getSourceClassName()+": "+ record.getMessage() ;// + System.lineSeparator();
        }
    }


}
