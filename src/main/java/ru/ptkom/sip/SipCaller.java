package ru.ptkom.sip;

import gov.nist.javax.sdp.MediaDescriptionImpl;
import gov.nist.javax.sip.SipStackExt;
import gov.nist.javax.sip.clientauthutils.AuthenticationHelper;
import org.apache.log4j.Logger;
import ru.ptkom.rtp.RTPTransmitter;
import ru.ptkom.sdp.SessionDescriptionFabric;
import ru.ptkom.sip.enumeration.CallPhase;
import ru.ptkom.sip.model.AccountManagerImpl;
import ru.ptkom.sip.model.SipAccount;

import javax.sdp.*;
import javax.sip.*;
import javax.sip.address.Address;
import javax.sip.address.AddressFactory;
import javax.sip.address.SipURI;
import javax.sip.header.*;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import javax.sip.message.Response;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Thread.sleep;

public class SipCaller implements SipListener {

    private static final Logger log = Logger.getLogger(SipCaller.class);

    private static final String SIP = "sip:";
    private static final Character URI_ADDRESS_SPLITTER = '@';
    private static final Character ADDRESS_PORT_SPLITTER = ':';
    private static final Integer TIMEOUT_BETWEEN_CALLS = 5000;

    private String username;
    private String displayName;
    private String authenticationName;
    private String password;
    private String sipDomain;
    private String traceLevel = "off";
    private String codecNameRTP;
    private String codecNameSDP;
    private String codecCode;
    private String serverAddress;
    private int serverPort;

    private static SipStack sipStack;
    private static SipFactory sipFactory;
    private static AddressFactory addressFactory;
    private static HeaderFactory headerFactory;
    private static MessageFactory messageFactory;
    private static SipProvider sipProvider;

    private final static Integer MIN_MEDIA_PORT = 24000;
    private final static Integer MAX_MEDIA_PORT = 32000;

    private Integer ssrc = 10;
    private Integer sessionId = 10;
    private Integer mediaPort = MIN_MEDIA_PORT;

    private CallPhase status;
    private Long speakingPhaseStartTime;
    private Long speakingPhaseEndTime;

    private SessionDescription sessionDescription;
    private Dialog dialog;
    private ClientTransaction clientTransaction;
    private Request ackRequest;

    private RTPTransmitter rtpTransmitter;

    public SipCaller(SipAccount sipAccount, String clientAddress, int port, String transport, String codecNameRTP, String codecNameSDP, String codecCode, String traceLevel) throws PeerUnavailableException, TransportNotSupportedException, InvalidArgumentException, ObjectInUseException, TooManyListenersException {
        this.traceLevel = traceLevel;
        this.codecNameRTP = codecNameRTP;
        this.codecNameSDP = codecNameSDP;
        this.codecCode = codecCode;
        this.serverAddress = sipAccount.getServerAddress();
        this.serverPort = sipAccount.getServerPort();
        setUsername(sipAccount.getNumber());
        setDisplayName(sipAccount.getDisplayName());
        setAuthenticationName(sipAccount.getAuthenticationName());
        setPassword(sipAccount.getPassword());
        setSipDomain(sipAccount.getSipDomain());
        //ssrc = globalSSRC.getAndIncrement();
        //sessionId = globalSessionId.getAndIncrement();
        //mediaPort = globalMediaPort.getAndAdd(2);
        Properties properties = initializeJavaxSipProperties();
        initializeJavaxSipSettings(properties, clientAddress, port, transport);
        sessionDescription = SessionDescriptionFabric.createSDP(getHost(), mediaPort, this.codecCode, this.codecNameSDP, this.ssrc.toString(), this.sessionId);
    }

    private Properties initializeJavaxSipProperties() {
        Properties properties = new Properties();
        properties.setProperty("javax.sip.STACK_NAME", "JavaSIP");
        properties.setProperty("gov.nist.javax.sip.TRACE_LEVEL", traceLevel);
        properties.setProperty("gov.nist.javax.sip.SERVER_LOG", "java-sip.txt");
        properties.setProperty("javax.sip.AUTOMATIC_DIALOG_SUPPORT", "on");
        //properties.setProperty("gov.nist.javax.sip.DEBUG_LOG", "java-sip-debug.log");
        return properties;
    }

    private void initializeJavaxSipSettings(Properties properties, String address, int port, String transport) {
        try {
            sipFactory = SipFactory.getInstance();
            sipFactory.setPathName("gov.nist");
            sipStack = sipFactory.createSipStack(properties);
            headerFactory = sipFactory.createHeaderFactory();
            addressFactory = sipFactory.createAddressFactory();
            messageFactory = sipFactory.createMessageFactory();
            ListeningPoint listeningPoint = sipStack.createListeningPoint(address, port, transport);
            sipProvider = sipStack.createSipProvider(listeningPoint);
            sipProvider.addSipListener(this);
        } catch (TransportNotSupportedException | TooManyListenersException | InvalidArgumentException |
                 PeerUnavailableException | ObjectInUseException e) {
            throw new RuntimeException(e);
        }
    }

    public CompletableFuture<Long> dial(String destinationNumber, RTPTransmitter rtpTransmitter) {
        clearCallLengthTimer();
        setRtpTransmitter(rtpTransmitter);
        return CompletableFuture.supplyAsync(() -> {
            String to = SIP + destinationNumber + URI_ADDRESS_SPLITTER + serverAddress + ADDRESS_PORT_SPLITTER + serverPort;
            Request request = makeInviteRequest(to, getTransport());
            status = CallPhase.STARTED;
            sendRequest(request);
            waiteForCallEnd();
            incrementMediaPort();
            return stopRtpAndGetCallResult();
        });
    }

    public Long getCallLength() {
        return (speakingPhaseEndTime - speakingPhaseStartTime)/1000;
    }

    private void sendRequest(Request request) {
        try {
            clientTransaction = sipProvider.getNewClientTransaction(request);
            clientTransaction.sendRequest();
            dialog = clientTransaction.getDialog();
        } catch (SipException e) {
            throw new RuntimeException(e);
        }
    }

    private Request makeInviteRequest(String to, String transport) {
        String username = to.substring(to.indexOf(":") + 1, to.indexOf("@"));
        String address = to.substring(to.indexOf("@") + 1);
        try {
            //Request-Line
            SipURI requestURI = makeRequestURI(username, address, transport);
            String method = Request.INVITE;

            //MessageHeader
            ViaHeader viaHeader = headerFactory.createViaHeader(getHost(), getPort(), transport, null);
            FromHeader fromHeader = makeFromHeader();
            ToHeader toHeader = makeToHeader(username, address);
            CallIdHeader callIdHeader = sipProvider.getNewCallId();
            CSeqHeader cSeqHeader = headerFactory.createCSeqHeader(1, Request.INVITE);
            ContactHeader contactHeader = makeContactHeader();
            ContentTypeHeader contentTypeHeader = headerFactory.createContentTypeHeader("application", "sdp");
            AllowHeader allowHeader = headerFactory.createAllowHeader("INVITE, ACK, PARK");
            //AllowHeader allowHeader = headerFactory.createAllowHeader("INVITE, ACK, CANCEL, MESSAGE, NOTIFY, OPTIONS, REFER, UPDATE, PARK");
            MaxForwardsHeader maxForwardsHeader = headerFactory.createMaxForwardsHeader(70);
            SupportedHeader supportedHeader = headerFactory.createSupportedHeader("replaces, from-change, 100rel");
            UserAgentHeader userAgentHeader = headerFactory.createUserAgentHeader(List.of("JavaSIP"));
            ArrayList<ViaHeader> viaHeaders = new ArrayList<ViaHeader>();
            viaHeader.setRPort();
            viaHeaders.add(viaHeader);

            Request request =  messageFactory.createRequest(
                    requestURI,
                    method,
                    callIdHeader,
                    cSeqHeader,
                    fromHeader,
                    toHeader,
                    viaHeaders,
                    maxForwardsHeader);
            request.addHeader(contactHeader);
            request.addHeader(allowHeader);
            request.addHeader(supportedHeader);
            request.addHeader(userAgentHeader);
            request.setContent(sessionDescription, contentTypeHeader);
            return request;
        } catch (InvalidArgumentException | ParseException e) {
            throw new RuntimeException(e);
        }
    }

    public CallPhase getCallState() {
        return status;
    }

    @Override
    public void processRequest(RequestEvent requestEvent) {
        log.info("Process request: " + requestEvent.getRequest().getMethod());

        if (requestEvent.getRequest().getMethod().equals(Request.BYE)) {
            speakingPhaseEndTime = System.currentTimeMillis();
            if (status == CallPhase.SPEAKING) {
                status = CallPhase.SUCCESS;
            } else {
                status = CallPhase.FAILED;
            }
            clearRtpTransmitter();
            Request request = requestEvent.getRequest();
            ServerTransaction serverTransactionId = requestEvent.getServerTransaction();
            processBye(request, serverTransactionId);
        }
    }

    @Override
    public void processResponse(ResponseEvent responseReceivedEvent) {
        var statusCode = responseReceivedEvent.getResponse().getStatusCode();
        log.info("Process response: " + statusCode);
        clientTransaction = responseReceivedEvent.getClientTransaction();
        dialog = responseReceivedEvent.getDialog();
        if (responseReceivedEvent.getClientTransaction() == null) {
            // RFC3261: MUST respond to every 2xx
            Dialog dialog = responseReceivedEvent.getDialog();
            if (ackRequest!=null && dialog!=null) {
                log.info("Re-sending ACK");
                try {
                    dialog.sendAck(ackRequest);
                } catch (SipException e) {
                    throw new RuntimeException(e);
                }
            }
            return;
        }

        if (responseReceivedEvent.getResponse().getStatusCode() == Response.OK) {
            log.info("Status is OK, time to ACK");
            sendAnswerAck(responseReceivedEvent);
            if (responseReceivedEvent.getClientTransaction().getRequest().getMethod() == Request.INVITE) {
                log.info("Status of INVITE request is OK");
                startRtpStream();
                startRtpTransmission();
                speakingPhaseStartTime = System.currentTimeMillis();
                status = CallPhase.SPEAKING;
            }

        } else if (responseReceivedEvent.getResponse().getStatusCode() == Response.UNAUTHORIZED
                || responseReceivedEvent.getResponse().getStatusCode() == Response.PROXY_AUTHENTICATION_REQUIRED) {
            sendAnswerWithCredentials(responseReceivedEvent);

        } else if (responseReceivedEvent.getResponse().getStatusCode() == Response.RINGING
                || responseReceivedEvent.getResponse().getStatusCode() == Response.SESSION_PROGRESS
                || responseReceivedEvent.getResponse().getStatusCode() == Response.CALL_IS_BEING_FORWARDED) {
            if (responseReceivedEvent.getResponse().getHeader(ContentTypeHeader.NAME).toString().contains("application/sdp")) {
                initializeRtpSession(responseReceivedEvent);
                System.err.println("RTP DONE");
            }
            log.info("Status is Ringing, time to PrACK");
            sendAnswerPrAck(responseReceivedEvent);

        } else if (responseReceivedEvent.getResponse().getStatusCode() == Response.TRYING) {
            status = CallPhase.RINGING;
            log.info("Trying caught");

        } else if (responseReceivedEvent.getResponse().getStatusCode() == Response.TEMPORARILY_UNAVAILABLE
                || responseReceivedEvent.getResponse().getStatusCode() == Response.BUSY_HERE
                || responseReceivedEvent.getResponse().getStatusCode() == Response.NOT_FOUND
                || responseReceivedEvent.getResponse().getStatusCode() == Response.REQUEST_TIMEOUT
                || responseReceivedEvent.getResponse().getStatusCode() == Response.SERVER_INTERNAL_ERROR) {
            log.info("Unavailable, stop RTP session and send ACK");
            status = CallPhase.FAILED;
            stopRtpTransmission();
            clearRtpTransmitter();
            sendAnswerAck(responseReceivedEvent);

        } else if (responseReceivedEvent.getResponse().getStatusCode() == Response.FORBIDDEN) {
            log.info("Forbidden, send ACK");
            status = CallPhase.FAILED;
            clearRtpTransmitter();
            sendAnswerAck(responseReceivedEvent);
        }
    }

    private void initializeRtpSession(ResponseEvent responseReceivedEvent) {
        var content = responseReceivedEvent.getResponse().getRawContent();
        try {
            SdpFactory sdpFactory = SdpFactory.getInstance();
            var sdp = sdpFactory.createSessionDescription(new String(content, StandardCharsets.US_ASCII));
            var mediaVector = sdp.getMediaDescriptions(true);
            int port = ((MediaDescriptionImpl) mediaVector.get(0)).getMedia().getMediaPort();
            var connection = ((MediaDescriptionImpl) mediaVector.get(0)).getConnection();
            rtpTransmitter.initializeRtpSession(connection.getAddress(), port, getHost(), mediaPort, codecNameRTP, codecCode);

        } catch (SdpException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendAnswerPrAck(ResponseEvent responseReceivedEvent) {
        try {
            Response response = responseReceivedEvent.getResponse();
            ClientTransaction transaction = responseReceivedEvent.getClientTransaction();
            SipProvider provider = (SipProvider) responseReceivedEvent.getSource();
            RequireHeader requireHeader = (RequireHeader) response.getHeader(RequireHeader.NAME);
            if (requireHeader.getOptionTag().equalsIgnoreCase("100rel")) {
                Dialog dialog = transaction.getDialog();
                Request prackRequest = dialog.createPrack(response);
                ClientTransaction ct = provider.getNewClientTransaction(prackRequest);
                dialog.sendRequest(ct);
            }
        } catch (SipException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendAnswerWithCredentials(ResponseEvent responseReceivedEvent) {
        try {
            Response response = (Response) responseReceivedEvent.getResponse();
            ClientTransaction transaction = responseReceivedEvent.getClientTransaction();
            SipProvider provider = (SipProvider) responseReceivedEvent.getSource();
            AuthenticationHelper authenticationHelper =
                    ((SipStackExt) sipStack).getAuthenticationHelper(new AccountManagerImpl(authenticationName, password, sipDomain), headerFactory);
            clientTransaction = authenticationHelper.handleChallenge(response, transaction, provider, 5);
            clientTransaction.sendRequest();
            dialog = clientTransaction.getDialog();
        } catch (SipException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendAnswerAck(ResponseEvent responseReceivedEvent) {
        try {
            Response response = responseReceivedEvent.getResponse();
            CSeqHeader cseq = (CSeqHeader) response.getHeader(CSeqHeader.NAME);
            Dialog dialog = responseReceivedEvent.getDialog();
            ackRequest = dialog.createAck(cseq.getSeqNumber());
            dialog.sendAck(ackRequest);
        } catch (InvalidArgumentException | SipException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void processTimeout(TimeoutEvent timeoutEvent) {
        log.info("Process timeout: " + timeoutEvent.getTimeout());
    }


    @Override
    public void processIOException(IOExceptionEvent ioExceptionEvent) {
        log.warn("Previous message not sent: I/O Exception");
    }

    @Override
    public void processTransactionTerminated(TransactionTerminatedEvent transactionTerminatedEvent) {
        log.info("Process transaction terminated: " + transactionTerminatedEvent.getClientTransaction().getState());
    }

    @Override
    public void processDialogTerminated(DialogTerminatedEvent dialogTerminatedEvent) {
        log.info("Process dialog terminated: " + dialogTerminatedEvent.getDialog().getState());
    }

    private FromHeader makeFromHeader() {
        try {
            SipURI from = addressFactory.createSipURI(getUsername(), getHost() + ":" + getPort());
            Address fromNameAddress = addressFactory.createAddress(from);
            fromNameAddress.setDisplayName(getDisplayName());
            return headerFactory.createFromHeader(fromNameAddress, ssrc.toString());
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    private ToHeader makeToHeader(String username, String address) {
        try {
            SipURI toAddress = addressFactory.createSipURI(username, address);
            Address toNameAddress = addressFactory.createAddress(toAddress);
            toNameAddress.setDisplayName(username);
            return headerFactory.createToHeader(toNameAddress, null);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    private SipURI makeRequestURI(String username, String address, String transport) {
        try {
            SipURI requestURI = addressFactory.createSipURI(username, address);
            requestURI.setTransportParam(transport);
            return requestURI;
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    private ContactHeader makeContactHeader() {
        try {
            SipURI contactURI = addressFactory.createSipURI(getUsername(), getHost());
            contactURI.setPort(getPort());
            Address contactAddress = addressFactory.createAddress(contactURI);
            contactAddress.setDisplayName(getDisplayName());
            return headerFactory.createContactHeader(contactAddress);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    public void processBye(Request request,
                           ServerTransaction serverTransactionId) {
        try {
            log.info("Got a bye request");
            stopRtpTransmission();
            if (serverTransactionId == null) {
                log.info("ServerTransactionId in null, cant response OK");
                return;
            }
            Dialog dialog = serverTransactionId.getDialog();
            Response response = messageFactory.createResponse(200, request);
            serverTransactionId.sendResponse(response);
            log.info("Sending OK");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        if (displayName == null || displayName.equals("")) {
            this.displayName = getUsername();
        } else if (username != null && !username.equals("")){
            this.displayName = displayName;
        } else {
            throw new RuntimeException("Initialize username first");
        }
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

    public String getHost() {
        return getFirstListeningPoint().getIPAddress();
    }

    public int getPort() {
        return getFirstListeningPoint().getPort();
    }

    public String getTransport() {
        return getFirstListeningPoint().getTransport();
    }

    private ListeningPoint getFirstListeningPoint() {
        Iterator listeningPoints = sipStack.getListeningPoints();
        if (listeningPoints.hasNext()) {
            return (ListeningPoint) listeningPoints.next();
        } else {
            throw new RuntimeException("No one listening point");
        }
    }


    private void setRtpTransmitter(RTPTransmitter rtpTransmitter) {
        this.rtpTransmitter = rtpTransmitter;
    }

    private void clearRtpTransmitter() {
        if (rtpTransmitter != null) {
            this.rtpTransmitter.closeTransmitter();
            this.rtpTransmitter = null;
        }
    }

    private void stopRtpTransmission() {
        if (rtpTransmitter != null) {
            rtpTransmitter.stopTransmission();
        }
    }

    private void startRtpTransmission() {
        if (rtpTransmitter != null) {
            rtpTransmitter.startTransmission();
        }
    }

    private void startRtpStream() {
        if (rtpTransmitter != null) {
            rtpTransmitter.startStream();
        }
    }

    private void incrementMediaPort() {
        if (mediaPort < MAX_MEDIA_PORT) {
            mediaPort = mediaPort + 2;
        } else {
            mediaPort = MIN_MEDIA_PORT;
        }
    }

    private void waiteForCallEnd() {
        while (getCallState() != CallPhase.SUCCESS && getCallState() != CallPhase.FAILED) {
            try {
                sleep(10);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private Long stopRtpAndGetCallResult() {
        if (speakingPhaseEndTime == null) {
            clearRtpTransmitter();
            try {
                sleep(TIMEOUT_BETWEEN_CALLS);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return 0l;
        } else {
            try {
                sleep(TIMEOUT_BETWEEN_CALLS);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return getCallLength();
        }
    }

    private void clearCallLengthTimer() {
        speakingPhaseEndTime = 0l;
        speakingPhaseStartTime = 0l;
    }
}
