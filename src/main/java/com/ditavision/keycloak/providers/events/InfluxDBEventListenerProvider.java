package com.ditavision.keycloak.providers.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.influxdb.InfluxDB;
import org.influxdb.dto.Point;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.OperationType;

import java.util.Set;
import java.util.concurrent.TimeUnit;

public class InfluxDBEventListenerProvider implements EventListenerProvider {

    private final Set<EventType> excludedEvents;
    private final Set<OperationType> excludedAdminOperations;
    private final InfluxDB influxDB;
    private final String influxDBName;
    private final String influxDBRetention;


    public InfluxDBEventListenerProvider(Set<EventType> excludedEvents, Set<OperationType> excludedAdminOpearations, InfluxDB influxDB, String influxDBName, String influxDBRetention) {
        this.excludedEvents = excludedEvents;
        this.excludedAdminOperations = excludedAdminOpearations;
        this.influxDB = influxDB;
        this.influxDBName = influxDBName;
        this.influxDBRetention = influxDBRetention;
    }

    @Override
    public void onEvent(Event event) {
        // Ignore excluded events
        if (excludedEvents != null && excludedEvents.contains(event.getType())) {
            return;
        } else {
            toInfluxDB(event);
        }
    }

    @Override
    public void onEvent(AdminEvent event, boolean includeRepresentation) {
        // Ignore excluded operations
        if (excludedAdminOperations != null && excludedAdminOperations.contains(event.getOperationType())) {
            return;
        } else {
            toInfluxDB(event);
        }
    }

    @Override
    public void close() {
    }

    private void toInfluxDB(Event event) {
        Point.Builder pb = Point.measurement("event").
                tag("type", event.getType().toString()).
                tag("realmId", event.getRealmId()).
                tag("clientId", event.getClientId() != null ? event.getClientId() : "unknown").
                addField("userId", event.getUserId() != null ? event.getUserId() : "unknown").
                addField("ipAddress", event.getIpAddress() != null ? event.getIpAddress() : "unknown").
                time(event.getTime(), TimeUnit.MILLISECONDS);
        if (event.getError() != null) {
            pb.addField("error", event.getError());
        }

        if (event.getDetails() != null) {
            String username = event.getDetails().get("username");
            if( username != null) {
                pb.addField("username", username);
            }

            //store details as json as probably not needed as separate fields
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                String json = objectMapper.writeValueAsString(event.getDetails());
                if (json != null && !"{}".equalsIgnoreCase(json)) {
                    pb.addField("details", json);
                }
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }

        influxDB.write(influxDBName, influxDBRetention, pb.build());
    }

    private void toInfluxDB(AdminEvent event) {
        Point.Builder pb = Point.measurement("adminEvent").
                tag("operationType", event.getOperationType().toString()).
                tag("resourceType", event.getResourceType() != null ? event.getResourceType().toString() : "unknown").
                tag("realmId", event.getRealmId()).
                tag("clientId", event.getAuthDetails().getClientId() != null ? event.getAuthDetails().getClientId() : "unknown").
                addField("userId", event.getAuthDetails().getUserId() != null ? event.getAuthDetails().getUserId() : "unknown").
                addField("ipAddress", event.getAuthDetails().getIpAddress() != null ? event.getAuthDetails().getIpAddress() : "unknown").
                addField("resourcePath", event.getResourcePath() != null ? event.getResourcePath() : "unknown").
                addField("representation", event.getRepresentation() != null ? event.getRepresentation() : "unknown").
                time(event.getTime(), TimeUnit.MILLISECONDS);
        if (event.getError() != null) {
            pb.addField("error", event.getError());
        }

        influxDB.write(influxDBName, influxDBRetention, pb.build());
    }

}
