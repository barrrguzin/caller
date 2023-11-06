package ru.ptkom;

import ru.ptkom.sip.SipCaller;
import ru.ptkom.sip.model.SipAccount;
import ru.ptkom.sip.model.TargetSubscriber;
import ru.ptkom.tts.TextToSpeech;
import javax.media.format.AudioFormat;
import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import java.util.ArrayList;
import java.util.TooManyListenersException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SipApplication {

    private static final String myAddress = "";
    private static final int myPort = 5061;

    private static final String transport = "udp";

    private static final String numberA = "";
    private static final String displayName = "";
    private static final String authenticationName = "";
    private static final String password = "";
    private static final String sipDomain = "";

    private static final String numberB1 = "";
    private static final String numberB2 = "";
    private static final String serverAddress = "";
    private static final int serverPort = 5060;

    private static final String codecNameRTP = AudioFormat.ULAW_RTP;
    private static final String codecNameSDP = "PCMU/8000/1";
    private static final String codecCode = "0";

    private static String pathToFile1 = "";
    private static String pathToFile2 = "";

    private static final String SIP = "sip:";
    private static final Character URI_ADDRESS_SPLITTER = '@';
    private static final Character ADDRESS_PORT_SPLITTER = ':';

    public static void main(String[] args) {

        var x = new TextToSpeech();
        x.setVoice();

        try {
            SipAccount sipAccount = new SipAccount(numberA, displayName, authenticationName, password, sipDomain, serverAddress, serverPort);
            SipCaller sipCaller = new SipCaller(sipAccount, myAddress, myPort, transport, codecNameRTP, codecNameSDP, codecCode, "0");

            var targetNumbers = Stream.of(numberB1, numberB2, numberB1, numberB2, numberB1).collect(Collectors.toList());
            var results = new ArrayList<TargetSubscriber>();



            targetNumbers.forEach(number -> {
                var subscriber = new TargetSubscriber(number, 10l, (byte) 10, sipCaller, pathToFile1);
                results.add(subscriber);
                subscriber.callToPlayAnnouncementAndGetCallDuration();
            });

            results.forEach(result -> {
                System.err.println(result.getCallDuration());
            });



        } catch (InvalidArgumentException | TooManyListenersException | SipException e) {
            throw new RuntimeException(e);
        }
    }
}
