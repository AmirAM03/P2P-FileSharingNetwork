package models;

import java.net.URI;
import java.net.URISyntaxException;

public class PeerInfo {
    private String peerName;
    private URI uri;


    public PeerInfo(String peerName, String address) throws URISyntaxException {
        setPeerName(peerName);
        setUri(address);
    }


    // getters & getter

    public String getPeerName() {
        return this.peerName;
    }

    public PeerInfo setPeerName(String peerName) {
        this.peerName = peerName;
        return this;
    }


    public URI getUri() {
        return this.uri;
    }

    public PeerInfo setUri(String address) throws URISyntaxException {
        this.uri = new URI("http://" + address + "/");
        return this;
    }
}
