package ru.ptkom.sdp;

import javax.sdp.Origin;

public class OriginImpl implements Origin {

    private String username;
    private long sessionId;
    private long sessionVersion;
    private  String address;
    private String networkType;
    private String addressType;

    public OriginImpl(String username, long sessionId, long sessionVersion, String address, String networkType, String addressType) {
        setUsername(username);
        setSessionId(sessionId);
        setSessionVersion(sessionVersion);
        setAddress(address);
        setNetworkType(networkType);
        setAddressType(addressType);
    }

    public OriginImpl(String username, Long sessionId, Long sessionVersion, String address, String networkType, String addressType) {
        setUsername(username);
        setSessionId(sessionId);
        setSessionVersion(sessionVersion);
        setAddress(address);
        setNetworkType(networkType);
        setAddressType(addressType);
    }

    public OriginImpl() {}

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public long getSessionId() {
        return sessionId;
    }

    @Override
    public void setSessionId(long sessionId) {
        this.sessionId = sessionId;
    }

    @Override
    public long getSessionVersion() {
        return sessionVersion;
    }

    @Override
    public void setSessionVersion(long sessionVersion) {
        this.sessionVersion = sessionVersion;
    }

    @Override
    public String getAddress() {
        return address;
    }

    @Override
    public void setAddress(String address) {
        this.address = address;
    }

    @Override
    public String getNetworkType() {
        return networkType;
    }

    @Override
    public void setNetworkType(String networkType) {
        this.networkType = networkType;
    }

    @Override
    public String getAddressType() {
        return addressType;
    }

    @Override
    public void setAddressType(String addressType) {
        this.addressType = addressType;
    }

    @Override
    public char getTypeChar() {
        return 0;
    }

    @Override
    public OriginImpl clone() {
        try {
            return (OriginImpl) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
