package ru.ptkom.sdp;

import gov.nist.core.NameValue;
import gov.nist.javax.sdp.MediaDescriptionImpl;
import gov.nist.javax.sdp.fields.*;

import javax.sdp.Media;
import javax.sdp.SdpException;
import javax.sdp.SdpFactory;
import javax.sdp.SessionDescription;
import java.util.Vector;

public class SessionDescriptionFabric {

    private static final String NETWORK_TYPE = "IN";
    private static final String ADDRESS_TYPE = "IP4";
    private static final Integer SESSION_VERSION = 1;
    private static final String USERNAME = "JavaSIP";
    private static final String SESSION_NAME = "Session-by-JavaSIP";
    private static final Integer PROTO_VERSION = 0;
    private static final String MEDIA_TYPE = "audio";
    private static final String PROTOCOL = "RTP/AVP";
    private static final String RTP_MAP = "rtpmap";
    private static final String SSRC = "ssrc";
    private static final String EVENT_TYPE = "sendrecv";
    private static final String T_ATTRIBUTE_NAME = "t";
    private static final String T_ATTRIBUTE_VALUE = "0 0";


    private SessionDescriptionFabric() {}


    public static SessionDescription createSDP(String address, int mediaPort, String codecCode, String codecName, String ssrc, int sessionId) {
        return makeMessageSDP(address, mediaPort, codecCode, codecName, ssrc, sessionId);
    }


    private static SessionDescription makeMessageSDP(String address, Integer mediaPort, String codecCode, String codecName, String ssrc, Integer sessionId) {
        try {
            ConnectionField connectionField = new ConnectionField();
            connectionField.setNetworkType(NETWORK_TYPE);
            connectionField.setAddressType(ADDRESS_TYPE);
            connectionField.setAddress(address);

            OriginField originField = new OriginField();
            originField.setAddress(address);
            originField.setAddressType(ADDRESS_TYPE);
            originField.setNetworkType(NETWORK_TYPE);
            originField.setSessionId(sessionId);
            originField.setSessionVersion(SESSION_VERSION);
            originField.setUsername(USERNAME);

            SessionNameField sessionNameField = new SessionNameField();
            sessionNameField.setSessionName(SESSION_NAME);

            ProtoVersionField versionField = new ProtoVersionField();
            versionField.setProtoVersion(PROTO_VERSION);

            SdpFactory sdpFactory = SdpFactory.getInstance();
            javax.sdp.SessionDescription sessionDescription = sdpFactory.createSessionDescription("");

            Media media = new MediaField();
            media.setMediaPort(mediaPort);
            media.setMediaType(MEDIA_TYPE);
            media.setProtocol(PROTOCOL);
            var formats = new Vector<>();
            formats.add(codecCode);
            //formats.add("101");
            media.setMediaFormats(formats);

            MediaDescriptionImpl mediaDescription = new MediaDescriptionImpl();

            var attributesVector = new Vector<>();
            attributesVector.add(makeAttributeField(RTP_MAP, codecCode + " " + codecName));
            mediaDescription.setAttributes(attributesVector);
            attributesVector.add(makeAttributeField(SSRC, ssrc));
            attributesVector.add(makeAttributeField(EVENT_TYPE, null));

            mediaDescription.setMedia(media);
            var mediaVector = new Vector<>();
            mediaVector.add(mediaDescription);
            sessionDescription.setMediaDescriptions(mediaVector);

            sessionDescription.setOrigin(originField);
            sessionDescription.setSessionName(sessionNameField);
            sessionDescription.setVersion(versionField);
            sessionDescription.setConnection(connectionField);

            sessionDescription.setAttribute(T_ATTRIBUTE_NAME, T_ATTRIBUTE_VALUE);

            return sessionDescription;
        } catch (SdpException e) {
            throw new RuntimeException(e);
        }
    }

    private static AttributeField makeAttributeField(String key, String value) {
        AttributeField attributeField = new AttributeField();
        NameValue nameValue = new NameValue(key, value);
        attributeField.setAttribute(nameValue);
        return attributeField;
    }
}
