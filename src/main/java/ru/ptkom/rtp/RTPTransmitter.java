package ru.ptkom.rtp;


import org.apache.log4j.Logger;
import org.jitsi.impl.neomedia.RTPConnectorUDPImpl;
import org.jitsi.impl.neomedia.format.MediaFormatFactoryImpl;
import org.jitsi.service.neomedia.DefaultStreamConnector;
import org.jitsi.service.neomedia.format.MediaFormat;

import javax.media.*;
import javax.media.control.TrackControl;
import javax.media.format.AudioFormat;
import javax.media.format.UnsupportedFormatException;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.DataSource;
import javax.media.rtp.*;
import javax.media.rtp.event.*;

import java.io.File;

import java.io.IOException;
import java.net.*;
import java.util.Arrays;


import static java.lang.Thread.sleep;

public class RTPTransmitter implements ReceiveStreamListener {

    private final static Logger log = Logger.getLogger(RTPTransmitter.class);

    private String pathToFile;
    private SendStream stream;
    private RTPManager rtpManager;
    private Processor processor;
    private Player player;
    private DataSource rtpDataSource;

    private String codecName;
    private int codecCode;
    private AudioFormat audioFormat;
    private MediaFormat rtpEventFormat;

    private String remoteAddressString;
    private int remotePort;
    private String sourceAddressString;
    private int sourcePort;
    private Boolean voiceExchangeStarted = false;

    private final static int DEFAULT_TTL_VALUE = 64;
    private final static Format RTP_EVENT_FORMAT = new AudioFormat("telephone-event", 8000, AudioFormat.NOT_SPECIFIED, AudioFormat.NOT_SPECIFIED);
    private final static int RTP_EVENT_PAYLOAD_TYPE = 101;



    public RTPTransmitter(String pathToFile) {
        MediaFormatFactoryImpl mediaFormatFactory = new MediaFormatFactoryImpl();
        long start = System.currentTimeMillis();
        this.pathToFile = pathToFile;
        initializeProcessor();
        log.info("Create RTPTransmitter: " + (System.currentTimeMillis() - start));
    }


    public void startTransmission() {
        processor.start();
        voiceExchangeStarted = true;
    }

    public void startStream () {
        try {
            stream.start();
            processor.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void stopTransmission() {
        try {
            log.info("Prepare to stop");
            stream.stop();
            stream.close();
            processor.stop();
            processor.close();
            voiceExchangeStarted = false;
            log.info("Transmission stopped");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void initializeRtpSession(String remoteAddressString, int remotePort, String sourceAddressString, int sourcePort, String codecName, String codecCode) {
        this.codecName = codecName;
        this.codecCode = Integer.parseInt(codecCode);
        this.audioFormat = new AudioFormat(codecName);
        this.remoteAddressString = remoteAddressString;
        this.remotePort = remotePort;
        this.sourceAddressString = sourceAddressString;
        this.sourcePort = sourcePort;
        try {
            rtpManager = createRTPManager();
            stream = createDataStream(rtpManager);
        } catch (InvalidSessionAddressException | UnsupportedFormatException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void initializeRtpSession(String remoteAddressString, int remotePort, String sourceAddressString, int sourcePort, String codecName, int codecCode) {
        this.codecName = codecName;
        this.codecCode = codecCode;
        this.audioFormat = new AudioFormat(codecName);
        this.remoteAddressString = remoteAddressString;
        this.remotePort = remotePort;
        this.sourceAddressString = sourceAddressString;
        this.sourcePort = sourcePort;
        try {
            rtpManager = createRTPManager();
            stream = createDataStream(rtpManager);
        } catch (InvalidSessionAddressException | UnsupportedFormatException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void initializeProcessor() {
        try {
            processor = createProcessor();
            processor.configure();
            while (processor.getState() != Processor.Configured) {
                sleep(1);
            }
            trackControlSettings();
            processor.setContentDescriptor(new ContentDescriptor(ContentDescriptor.RAW_RTP));
            processor.realize();
            while (processor.getState() != Processor.Realized) {
                sleep(1);
            }

        } catch (NoProcessorException | IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private Processor createProcessor() throws IOException, NoProcessorException {
        File mediaFile = new File(pathToFile);
        URL mediaURL = mediaFile.toURI().toURL();
        MediaLocator mediaLocator = new MediaLocator(mediaURL);
        return Manager.createProcessor(mediaLocator);
    }

    private RTPManager createRTPManager() throws IOException, InvalidSessionAddressException {
        //This solution for fixing 4 second time to creation rtpManager.addTarget(remoteAddress);
        InetAddress temporaryRemoteAddress = InetAddress.getByName(remoteAddressString);
        byte[] bytesOfRemoteAddress = temporaryRemoteAddress.getAddress();
        InetAddress remoteIpAddress = InetAddress.getByAddress(InetAddress.getLocalHost().getHostName(), bytesOfRemoteAddress);
        SessionAddress remoteAddress = new SessionAddress(remoteIpAddress, remotePort, DEFAULT_TTL_VALUE);
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        InetAddress sourceIpAddress = InetAddress.getByName(sourceAddressString);
        SessionAddress sourceAddress = new SessionAddress(sourceIpAddress, sourcePort, DEFAULT_TTL_VALUE);
        RTPManager rtpManager = RTPManager.newInstance();
        rtpManager.addFormat(audioFormat, codecCode);
        /////////////////////////////////
        rtpManager.addFormat(RTP_EVENT_FORMAT, RTP_EVENT_PAYLOAD_TYPE);
        rtpManager.addReceiveStreamListener(this);
        //rtpManager.addRemoteListener(this);
        ////////////////////////////////
        rtpManager.initialize(sourceAddress);
        long start = System.currentTimeMillis();
        rtpManager.addTarget(remoteAddress);
        log.info("Creating RTPManager: " + (System.currentTimeMillis() - start));
        return rtpManager;
    }

    private SendStream createDataStream(RTPManager rtpManager) throws UnsupportedFormatException, IOException {
        long start = System.currentTimeMillis();
        DataSource output = processor.getDataOutput();
        SendStream sendStream = rtpManager.createSendStream(output, 0);
        log.info("Creating SendStream: " + (System.currentTimeMillis() - start));
        return sendStream;
    }

    private void trackControlSettings() {
        TrackControl[] tracks = processor.getTrackControls();

        if (tracks == null || tracks.length < 1) {
            log.info("Couldn't find tracks in processor");
            System.exit(1);
        }

        for (TrackControl track : tracks) {
            log.info("Format: " + track.getFormat());
        }

        Format supported[];
        Format chosen;
        boolean atLeastOneTrack = false;

        // Program the tracks.
        for (int i = 0; i < tracks.length; i++) {
            Format format = tracks[i].getFormat();

            log.info("Trenutni format je " +format.getEncoding());

            if (tracks[i].isEnabled()) {
                supported = tracks[i].getSupportedFormats();
                for (int n = 0; n < supported.length; n++)
                    log.info("Supported format: " + supported[n]);

                if (supported.length > 0) {
                    chosen = supported[0]; // this is where I tried changing formats
                    tracks[i].setFormat(chosen);
                    log.info("Track " + i + " is set to transmit as: " + chosen);
                    atLeastOneTrack = true;
                } else
                    tracks[i].setEnabled(false);
            } else
                tracks[i].setEnabled(false);
        }
    }

    @Override
    public synchronized void update(ReceiveStreamEvent receiveStreamEvent) {
        RTPControl[] controls = (RTPControl[]) receiveStreamEvent.getReceiveStream().getDataSource().getControls();
        Arrays.stream(controls).forEach(control -> log.info(control.getFormat()));
        if (receiveStreamEvent instanceof NewReceiveStreamEvent) {
            log.debug("This is a NewReceiveStreamEvent: " + receiveStreamEvent.getClass());
            playReceivedRtpStream((NewReceiveStreamEvent) receiveStreamEvent);
        } else if (receiveStreamEvent instanceof StreamMappedEvent){
            log.debug("This is a StreamMappedEvent: " + receiveStreamEvent.getClass());
        } else if (receiveStreamEvent instanceof RemotePayloadChangeEvent) {
            log.debug("This is a RemotePayloadChangeEvent(" + ((RemotePayloadChangeEvent) receiveStreamEvent).getNewPayload() + "): " + receiveStreamEvent.getClass());
            changePayload((RemotePayloadChangeEvent) receiveStreamEvent);
        }
    }

    private void playReceivedRtpStream(NewReceiveStreamEvent event) {
        try {
            var receivedStream = event.getReceiveStream();
            rtpDataSource = receivedStream.getDataSource();
            ((RTPControl) rtpDataSource.getControls()[0]).addFormat(RTP_EVENT_FORMAT, RTP_EVENT_PAYLOAD_TYPE);
            player = Manager.createRealizedPlayer(rtpDataSource);
            player.start();
        } catch (IOException | NoPlayerException | CannotRealizeException e) {
            throw new RuntimeException(e);
        }
    }



    private void changePayload(RemotePayloadChangeEvent event) {
        try {
            player.stop();
            while (player.getState() == Player.Started) {
                sleep(1);
            }
            //player.removeControllerListener(rtpEventListener);
            player.close();

            while (player.getState() == Controller.Started) {
                sleep(1);
            }

            rtpDataSource.connect();
            Player newPlayer = Manager.createPlayer(rtpDataSource);
            //newPlayer.addControllerListener(rtpEventListener);
            newPlayer.realize();
            player = newPlayer;
        } catch (IOException | NoPlayerException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void closeTransmitter() {
        closeAndClearRtpManager();
        closeAndClearStream();
        closeAndClearProcessor();
        closeAndClearPlayer();
        closeAndClearDataSource();
    }

    private void closeAndClearRtpManager() {
        if (rtpManager != null) {
            rtpManager.dispose();
            rtpManager = null;
        }
    }

    private void closeAndClearStream() {
        if (stream != null) {
            try {
                stream.stop();
            } catch (IOException e) {
                log.error(String.format("Unable to stop SendStream: %s", e.getMessage()));
            } finally {
                stream.close();
                stream = null;
            }

        }
    }

    private void closeAndClearProcessor() {
        if (processor != null) {
            processor.stop();
            processor.close();
            processor = null;
        }
    }

    private void closeAndClearPlayer() {
        if (player != null) {
            player.stop();
            player.close();
            player = null;
        }
    }

    private void closeAndClearDataSource() {
        if (rtpDataSource != null) {
            rtpDataSource.disconnect();
            try {
                rtpDataSource.stop();
            } catch (IOException e) {
                log.error(String.format("Unable to stop DataSource: %s", e.getMessage()));
            } finally {
                rtpDataSource = null;
            }
        }
    }
}
