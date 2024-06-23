package models;

public class PeerInfo {
    private String peerName;
    public String address;


    public PeerInfo(String peerName, String address){
        setPeerName(peerName);
        setAddress(address);
    }


    // getters & getter

    public String getPeerName() {
        return this.peerName;
    }

    private PeerInfo setPeerName(String peerName) {
        this.peerName = peerName;
        return this;
    }

    public String getAddress() {
        return address;
    }

    private void setAddress(String address) {
        this.address = address;
    }
<<<<<<< HEAD
}
=======
}
>>>>>>> c5dc479 (added processCommand)
