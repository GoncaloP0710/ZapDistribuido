package psd.group4.utils;

import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

public class SuppressMongoLogs {
    public static void disableMongoLogs() {
        // Suppress logs for the MongoDB driver
        Logger rootLogger = (Logger) LoggerFactory.getLogger("org.mongodb.driver");
        rootLogger.setLevel(Level.OFF);

        // Suppress logs for other MongoDB related loggers
        Logger commandLogger = (Logger) LoggerFactory.getLogger("org.mongodb.driver.protocol.command");
        commandLogger.setLevel(Level.OFF);

        Logger connectionLogger = (Logger) LoggerFactory.getLogger("org.mongodb.driver.connection");
        connectionLogger.setLevel(Level.OFF);

        Logger clusterLogger = (Logger) LoggerFactory.getLogger("org.mongodb.driver.cluster");
        clusterLogger.setLevel(Level.OFF);

        Logger protocolLogger = (Logger) LoggerFactory.getLogger("org.mongodb.driver.protocol");
        protocolLogger.setLevel(Level.OFF);

        Logger operationLogger = (Logger) LoggerFactory.getLogger("org.mongodb.driver.operation");
        operationLogger.setLevel(Level.OFF);
    }
}