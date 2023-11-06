package ru.ptkom.sdp;

import javax.sdp.SdpException;
import javax.sdp.SdpParseException;
import javax.sdp.Version;

public class VersionImpl implements Version {

    private int version;

    public VersionImpl(int version){
        this.version = version;
    }
    public VersionImpl(Integer version){
        this.version = version;
    }
    public VersionImpl(){}

    @Override
    public int getVersion() throws SdpParseException {
        return version;
    }

    @Override
    public void setVersion(int version) throws SdpException {
        this.version = version;
    }

    @Override
    public char getTypeChar() {
        return 0;
    }

    @Override
    public VersionImpl clone() {
        try {
            return (VersionImpl) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
