package ru.ptkom.sip.model;

public class SipAccount {

    private String number;
    private String displayName;
    private String authenticationName;
    private String password;
    private String sipDomain;
    private String serverAddress;
    private Integer serverPort;

    public SipAccount() {}

    public SipAccount(String number, String displayName, String authenticationName, String password, String sipDomain, String serverAddress, Integer serverPort) {
        setNumber(number);
        setDisplayName(displayName);
        setAuthenticationName(authenticationName);
        setPassword(password);
        setSipDomain(sipDomain);
        setServerAddress(serverAddress);
        setServerPort(serverPort);
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getAuthenticationName() {
        return authenticationName;
    }

    public void setAuthenticationName(String authenticationName) {
        this.authenticationName = authenticationName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getSipDomain() {
        return sipDomain;
    }

    public void setSipDomain(String sipDomain) {
        this.sipDomain = sipDomain;
    }

    public String getServerAddress() {
        return serverAddress;
    }

    public void setServerAddress(String serverAddress) {
        this.serverAddress = serverAddress;
    }

    public Integer getServerPort() {
        return serverPort;
    }

    public void setServerPort(Integer serverPort) {
        this.serverPort = serverPort;
    }
}
