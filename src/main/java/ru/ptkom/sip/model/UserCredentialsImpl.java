package ru.ptkom.sip.model;

import gov.nist.javax.sip.clientauthutils.UserCredentials;
import org.apache.log4j.Logger;

public class UserCredentialsImpl implements UserCredentials {

    private static final Logger log = Logger.getLogger(UserCredentialsImpl.class);

    private String userName;
    private String sipDomain;
    private String password;

    public UserCredentialsImpl(String userName, String sipDomain, String password) {
        this.userName = userName;
        this.sipDomain = sipDomain;
        this.password = password;
        log.info(String.format("\nAuthentication name: %s\nPassword: %s\nDomain: %s", this.userName, this.password, this.sipDomain));
    }

    public String getPassword() {
        return password;
    }

    public String getSipDomain() {
        return sipDomain;
    }

    public String getUserName() {
        return userName;
    }
}
