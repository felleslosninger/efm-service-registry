package no.difi.meldingsutveksling.serviceregistry.logging;

import net.logstash.logback.marker.LogstashMarker;
import net.logstash.logback.marker.Markers;

public class SRMarkerFactory {

    private SRMarkerFactory() {}

    private static final String REMOTE_IP = "remote_ip";
    private static final String CLIENT_ID = "client_id";
    private static final String AUTH_TOKEN = "auth_token";

    private static LogstashMarker remoteIpMarker(String remoteIp) {
        return Markers.append(REMOTE_IP, remoteIp);
    }

    private static LogstashMarker clientIdMarker(String clientId) {
        return Markers.append(CLIENT_ID, clientId);
    }

    private static LogstashMarker authTokenMarker(String authToken) {
        return Markers.append(AUTH_TOKEN, authToken);
    }

    public static LogstashMarker markerFrom(String remoteIp, String clientId, String authToken) {
        return remoteIpMarker(remoteIp).and(clientIdMarker(clientId)).and(authTokenMarker(authToken));
    }

    public static LogstashMarker markerFrom(String remoteIp) {
        return remoteIpMarker(remoteIp);
    }
}
