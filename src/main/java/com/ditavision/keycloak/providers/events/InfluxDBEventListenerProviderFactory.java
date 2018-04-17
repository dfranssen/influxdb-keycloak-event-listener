package com.ditavision.keycloak.providers.events;

import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.OperationType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

import java.util.HashSet;
import java.util.Set;

public class InfluxDBEventListenerProviderFactory implements EventListenerProviderFactory {

    private Set<EventType> excludedEvents;
    private Set<OperationType> excludedAdminOperations;

    private final String influxdbHost = getEnvOrDefault("INFLUXDB_HOST", "influxDB");
    private final String influxdbPort = getEnvOrDefault("INFLUXDB_PORT", "8086");
    private final String influxdbUser = getEnvOrDefault("INFLUXDB_USER", "root");
    private final String influxdbPassword = getEnvOrDefault("INFLUXDB_PWD", "root");
    private final String influxdbDBName = getEnvOrDefault("INFLUXDB_DB", "keycloak");
    private final String influxdbRetentionPolicy = getEnvOrDefault("INFLUXDB_DB_RETENTION_POLICY", "14d");
    private final String influxUrl = String.format("http://%s:%s", influxdbHost, influxdbPort);

    private InfluxDB influxDB;

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        return new InfluxDBEventListenerProvider(excludedEvents, excludedAdminOperations, influxDB, influxdbDBName, influxdbRetentionPolicy);
    }

    @Override
    public void init(Config.Scope config) {
        String[] excludes = config.getArray("exclude-events");
        if (excludes != null) {
            excludedEvents = new HashSet<>();
            for (String e : excludes) {
                excludedEvents.add(EventType.valueOf(e));
            }
        }
        
        String[] excludesOperations = config.getArray("excludesOperations");
        if (excludesOperations != null) {
            excludedAdminOperations = new HashSet<>();
            for (String e : excludesOperations) {
                excludedAdminOperations.add(OperationType.valueOf(e));
            }
        }

        try {
            influxDB = InfluxDBFactory.connect(influxUrl, influxdbUser, influxdbPassword);
            influxDB.createRetentionPolicy(influxdbRetentionPolicy, influxdbDBName, influxdbRetentionPolicy, null, 1);
        } catch (Exception ex) {
            System.err.println("Ignoring InfluxDB setup, exception occured: " + ex.getMessage());
        }
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }
    @Override
    public void close() {
        if (influxDB != null) {
            influxDB.close();
        }
    }

    @Override
    public String getId() {
        return "influxDB";
    }

    private String getEnvOrDefault(String name, String defaultValue) {
        String result = System.getenv(name);
        return result == null ? defaultValue : result;
    }

}
