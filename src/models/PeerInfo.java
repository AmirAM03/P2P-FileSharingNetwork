package models;

import java.net.URI;
import java.net.URISyntaxException;

public class PeerInfo {
    private String peerName;
    private String address;


    public PeerInfo(String peerName, String address){
        setPeerName(peerName);
        this.address = address;
    }


    // getters & getter

    public String getPeerName() {
        return this.peerName;
    }

    public PeerInfo setPeerName(String peerName) {
        this.peerName = peerName;
        return this;
    }
}
