package no.difi.meldingsutveksling.serviceregistry.logging;

import net.logstash.logback.marker.LogstashMarker;
import net.logstash.logback.marker.Markers;

public class SRMarkerFactory {

    private static final String REMOTE_IP = "remote_ip";
    private static final String REMOTE_HOST = "remote_host";
    private static final String CLIENT_ID = "client_id";

    private SRMarkerFactory() {}

    private static LogstashMarker remoteIpMarker(String remoteIp) {
        return Markers.append(REMOTE_IP, remoteIp);
    }

    private static LogstashMarker remoteHostMarker(String remoteHost) {
        return Markers.append(REMOTE_HOST, remoteHost);
    }

    private static LogstashMarker clientIdMarker(String clientId) {
        return Markers.append(CLIENT_ID, clientId);
    }

    public static LogstashMarker markerFrom(String remoteIp, String remoteHost, String clientId) {
        return remoteIpMarker(remoteIp).and(remoteHostMarker(remoteHost)).and(clientIdMarker(clientId));
    }

    public static LogstashMarker markerFrom(String remoteIp) {
        return remoteIpMarker(remoteIp);
    }
}
