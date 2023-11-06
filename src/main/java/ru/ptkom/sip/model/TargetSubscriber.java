package ru.ptkom.sip.model;

import org.apache.log4j.Logger;
import ru.ptkom.rtp.RTPTransmitter;
import ru.ptkom.sip.SipCaller;

import java.util.concurrent.ExecutionException;

public class TargetSubscriber {

    private static final Logger log = Logger.getLogger(TargetSubscriber.class);

    private String number;
    private Long balance;
    private Byte dayToBan;
    private Long callDuration;
    private Boolean finished;

    private String messageAudio;

    private SipCaller caller;
    private RTPTransmitter transmitter;


    public TargetSubscriber(String number, String balance, String dayToBan, SipCaller caller) {
        this.number = number;
        this.balance = Long.parseLong(balance);
        this.dayToBan = Byte.parseByte(dayToBan);
        this.caller = caller;
        finished = false;
    }

    public TargetSubscriber(String number, Long balance, Byte dayToBan, SipCaller caller) {
        this.number = number;
        this.balance = balance;
        this.dayToBan = dayToBan;
        this.caller = caller;
        finished = false;
    }

    public TargetSubscriber(String number, Long balance, Byte dayToBan, SipCaller caller, String pathToAudioMessage) {
        this.number = number;
        this.balance = balance;
        this.dayToBan = dayToBan;
        this.caller = caller;
        this.messageAudio = pathToAudioMessage;
        finished = false;
    }


    public Long callToPlayAnnouncementAndGetCallDuration() {
        createTransmitter();
        try {
            callDuration = caller.dial(number, transmitter).get();
            finished = true;
            return callDuration;
        } catch (ExecutionException | InterruptedException e) {
            log.error(String.format("Call to %s ended with error: %s", number, e.getMessage()));
            callDuration = 0l;
            finished = true;
            return callDuration;
        } finally {
            clearTransmitter();
        }
    }

    private void createTransmitter() {
        var messageAudio = getMessageAudio();
        transmitter = new RTPTransmitter(messageAudio);
    }

    private void clearTransmitter() {
        if (transmitter != null) {
            transmitter.closeTransmitter();
            transmitter = null;
        }
    }

    private String getMessageAudio() {
        return messageAudio;
    }


    public String getNumber() {
        return number;
    }

    public Long getBalance() {
        return balance;
    }

    public Byte getDayToBan() {
        return dayToBan;
    }

    public Long getCallDuration() {
        return callDuration;
    }

    public Boolean getFinished() {
        return finished;
    }

    public SipCaller getCaller() {
        return caller;
    }

    public void setCaller(SipCaller sipCaller) {
        this.caller = sipCaller;
    }
}
