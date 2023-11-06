package ru.ptkom.sip.model;

import gov.nist.javax.sip.clientauthutils.AccountManager;
import gov.nist.javax.sip.clientauthutils.UserCredentials;

import javax.sip.ClientTransaction;

public class AccountManagerImpl implements AccountManager {

    private UserCredentials userCredentials;

    public AccountManagerImpl(String authorizationName, String password, String sipDomain) {
        this.userCredentials = new UserCredentialsImpl(authorizationName, sipDomain, password);
    }

    public UserCredentials getCredentials(ClientTransaction challengedTransaction, String realm) {
        return userCredentials;
    }

}
