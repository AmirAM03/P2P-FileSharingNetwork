package models;

public class PeerRequestLog {
    private PeerInfo applicantPeerInfo;
    private String requestData;
    private boolean isSuccess;


    public PeerRequestLog(PeerInfo applicantPeerInfo, String requestData, boolean isSuccess) {
        this.applicantPeerInfo = applicantPeerInfo;
        this.requestData = requestData;
        this.isSuccess = isSuccess;
    }

    @Override
    public String toString() {
        return "applicantPeerInfo:"+applicantPeerInfo+", requestData:"+requestData+", isSuccess:"+isSuccess;
    }


    // getters & setters

    public PeerInfo getApplicantPeerInfo() {
        return applicantPeerInfo;
    }

    public void setApplicantPeerInfo(PeerInfo applicantPeerInfo) {
        this.applicantPeerInfo = applicantPeerInfo;
    }


    public String getRequestData() {
        return requestData;
    }

    public void setRequestData(String requestData) {
        this.requestData = requestData;
    }


    public boolean isSuccess() {
        return isSuccess;
    }

    public void setSuccess(boolean success) {
        isSuccess = success;
    }
}