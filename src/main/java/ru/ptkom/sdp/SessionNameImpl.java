package ru.ptkom.sdp;

import javax.sdp.SdpException;
import javax.sdp.SdpParseException;
import javax.sdp.SessionName;

public class SessionNameImpl implements SessionName {

    private String sessionName;

    public SessionNameImpl(String sessionName) {
        try {
            setValue(sessionName);
        } catch (SdpException e) {
            throw new RuntimeException(e);
        }
    }
    public SessionNameImpl() {}

    @Override
    public String getValue() throws SdpParseException {
        return sessionName;
    }

    @Override
    public void setValue(String sessionName) throws SdpException {
        this.sessionName = sessionName;
    }

    @Override
    public char getTypeChar() {
        return 0;
    }

    @Override
    public SessionNameImpl clone() {
        try {
            return (SessionNameImpl) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
