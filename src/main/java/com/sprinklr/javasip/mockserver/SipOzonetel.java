package com.sprinklr.javasip.mockserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sip.*;
import javax.sip.address.Address;
import javax.sip.address.AddressFactory;
import javax.sip.address.SipURI;
import javax.sip.header.*;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import javax.sip.message.Response;
import java.util.ArrayList;
import java.util.Properties;

/*
 * Mocks the sip entity on Ozonetel's end which sends INVITE requests and acts as UAC.
 * Also, mocks registrar server where local (Sprinklr's) UA is to be registered.
 */
public class SipOzonetel implements SipListener, Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(SipOzonetel.class);

    private SipProvider sipProvider;

    private AddressFactory addressFactory;

    private MessageFactory messageFactory;

    private HeaderFactory headerFactory;

    private SipStack sipStack;

    private ContactHeader contactHeader;

    private ListeningPoint udpListeningPoint;

    private ClientTransaction inviteTid;

    private Dialog dialog;

    boolean isShutdown = false;

    @Override
    public void run() {
        init();
    }

    public static void main(String[] args) {
        new SipOzonetel().init();
    }

    public void sendBye() {
        try {
            if (dialog == null) {
                LOGGER.warn("Null dialog for bye");
                return;
            }
            Request byeRequest = dialog.createRequest(Request.BYE);
            ClientTransaction ct = sipProvider.getNewClientTransaction(byeRequest);
            dialog.sendRequest(ct);
            LOGGER.info("Ozonetel says BYE");
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(0);
        }
    }

    public void processRequest(RequestEvent requestReceivedEvent) {
        Request request = requestReceivedEvent.getRequest();
        ServerTransaction serverTransactionId = requestReceivedEvent
                .getServerTransaction();

        LOGGER.info("Request {} received at {} with serverTransaction:{}",
                request.getMethod(), sipStack.getStackName(), serverTransactionId);

        // We are the UAC, the only request we get is the REGISTER (mimicking the registrar server)
        if (request.getMethod().equals(Request.REGISTER))
            processRegister(request, serverTransactionId);
        else {
            LOGGER.info("No implementation on client side found for request method {}", request.getMethod());
        }

    }

    public void processRegister(Request request,
                                ServerTransaction serverTransactionId) {
        try {
            LOGGER.info("client got a register .");
            if (serverTransactionId == null) {
                LOGGER.info("client :  null serverTransactionId.");
                serverTransactionId = sipProvider.getNewServerTransaction(request);
            }
            Dialog currDialog = serverTransactionId.getDialog();
            LOGGER.info("Dialog = {}", currDialog);

            if (request.getHeader("Authorization") == null) {
                LOGGER.info("Auth required, sneding 401");
                Response response = messageFactory.createResponse(Response.UNAUTHORIZED, request);
                /*
                Create www-authenticate header depending on use case. Currently values like nonce are hardcoded, just to mimic Ozonetel's sip UA.
                 */
                WWWAuthenticateHeader wwwAuthenticateHeader = headerFactory.createWWWAuthenticateHeader("Digest realm=\"ozonetel.com\", domain=\"sip:sprinklr.com\", nonce=\"f84f1cec41e6cbe5aea9c8e88d359\", algorithm=MD5");
                response.addHeader(wwwAuthenticateHeader);
                serverTransactionId.sendResponse(response);
            } else {
                /*
                Authorization header present in REGISTER request from Sprinklr's UA. This party will perform verification based on nonce and response keys.
                For simplicity, assuming the response key is correct, no check has been performed here.
                 */
                Response response = messageFactory.createResponse(Response.OK, request);
                serverTransactionId.sendResponse(response);
                LOGGER.info("client:  Sending OK.");
            }

            LOGGER.info("Dialog = {}", currDialog);

        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(0);
        }
    }

    // Save the created ACK request, to respond to retransmitted 2xx
    private Request ackRequest;

    public void processResponse(ResponseEvent responseReceivedEvent) {
        LOGGER.info("Client got a response");
        Response response = responseReceivedEvent.getResponse();
        ClientTransaction tid = responseReceivedEvent.getClientTransaction();
        CSeqHeader cseq = (CSeqHeader) response.getHeader(CSeqHeader.NAME);

        LOGGER.info("Response received : Status Code = {} {}", response.getStatusCode(), cseq);

        if (tid == null) {

            // RFC3261: MUST respond to every 2xx
            if (ackRequest != null && dialog != null) {
                LOGGER.info("re-sending ACK");
                try {
                    dialog.sendAck(ackRequest);
                } catch (SipException se) {
                    se.printStackTrace();
                }
                LOGGER.info("re-sending BYE");
                sendBye();
            }
            return;
        }

        try {
            if (response.getStatusCode() == Response.OK) {
                if (cseq.getMethod().equals(Request.INVITE)) {
                    LOGGER.info("Dialog after 200 OK  {}", dialog);
                    LOGGER.info("Dialog State after 200 OK  {}", dialog.getState());
                    ackRequest = dialog.createAck(((CSeqHeader) response.getHeader(CSeqHeader.NAME)).getSeqNumber());
                    LOGGER.info("Sending ACK");
                    dialog.sendAck(ackRequest);

                } else if (cseq.getMethod().equals(Request.CANCEL) && dialog.getState() == DialogState.CONFIRMED) {
                    // oops cancel went in too late. Need to hang up the dialog
                    LOGGER.info("Sending BYE -- cancel went in too late !!");
                    sendBye();
                    shutDown();
                }
            }
        } catch (InvalidArgumentException | SipException e) {
            LOGGER.error("Error while processing response in sip client {}", e.toString());
            System.exit(0);
        }
    }

    public void processTimeout(TimeoutEvent timeoutEvent) {

        LOGGER.info("Transaction Time out");
    }

    public void init() {

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                sendBye();
            }
        });

        SipFactory sipFactory;
        sipStack = null;
        sipFactory = SipFactory.getInstance();
        sipFactory.setPathName("gov.nist");
        Properties properties = new Properties();

        String transport = "udp";
        String peerHostPort = "127.0.0.1:5070";
        properties.setProperty("javax.sip.OUTBOUND_PROXY", peerHostPort + "/"
                + transport);

        properties.setProperty("javax.sip.STACK_NAME", "Ozonetel");

        // Drop the client connection after we are done with the transaction.
        properties.setProperty("gov.nist.javax.sip.CACHE_CLIENT_CONNECTIONS",
                "false");

        try {
            // Create SipStack object
            sipStack = sipFactory.createSipStack(properties);
            LOGGER.info("createSipStack {}", sipStack);
        } catch (PeerUnavailableException e) {
            // could not find
            // gov.nist.jain.protocol.ip.sip.SipStackImpl
            // in the classpath
            LOGGER.error("PeerUnavailableException in SipOzonetel , {}", e.toString());
            System.exit(0);
        }

        try {
            headerFactory = sipFactory.createHeaderFactory();
            addressFactory = sipFactory.createAddressFactory();
            messageFactory = sipFactory.createMessageFactory();
            //Hardcoded host and port since it is just to mimic Ozonetel or the remote sip UA (remote w.r.t Sprinklr SIP UA which is local)
            udpListeningPoint = sipStack.createListeningPoint("127.0.0.1", 5060, transport);
            LOGGER.info("listeningPoint = {}", udpListeningPoint);
            sipProvider = sipStack.createSipProvider(udpListeningPoint);
            LOGGER.info("SipProvider = {}", sipProvider);
            sipProvider.addSipListener(this);

            String fromName = "Ozonetel";
            String fromSipAddress = "ozontel.com";
            String fromDisplayName = "Ozonetel Telecom";

            String toSipAddress = "sprinklr.com";
            String toUser = "Souradeep";
            String toDisplayName = "Code it";

            // create >From Header
            SipURI fromAddress = addressFactory.createSipURI(fromName,
                    fromSipAddress);

            Address fromNameAddress = addressFactory.createAddress(fromAddress);
            fromNameAddress.setDisplayName(fromDisplayName);
            FromHeader fromHeader = headerFactory.createFromHeader(fromNameAddress,
                    "12345"); //12345 is the tag, it could be any randomly generated integer

            //To understand tags, refer https://andrewjprokop.wordpress.com/2013/09/23/lets-play-sip-tag/

            // create To Header
            SipURI toAddress = addressFactory
                    .createSipURI(toUser, toSipAddress);
            Address toNameAddress = addressFactory.createAddress(toAddress);
            toNameAddress.setDisplayName(toDisplayName);
            ToHeader toHeader = headerFactory.createToHeader(toNameAddress,
                    null); //the to-tag is put by the remote party

            // create Request URI
            SipURI requestURI = addressFactory.createSipURI(toUser, peerHostPort);

            // Create ViaHeaders

            ArrayList<ViaHeader> viaHeaders = new ArrayList<>(); //empty via headers list since this is starting the request
            String ipAddress = udpListeningPoint.getIPAddress();
            ViaHeader viaHeader = headerFactory.createViaHeader(ipAddress,
                    sipProvider.getListeningPoint(transport).getPort(),
                    transport, "z9hG4bK" + "83783"); //z9hG4bK83783 is the branch, refer https://andrewjprokop.wordpress.com/2014/03/06/understanding-the-sip-via-header/

            // add via headers
            viaHeaders.add(viaHeader);

            // Create ContentTypeHeader
            ContentTypeHeader contentTypeHeader = headerFactory
                    .createContentTypeHeader("application", "sdp");

            // Create a new CallId header
            CallIdHeader callIdHeader = sipProvider.getNewCallId();

            // Create a new Cseq header
            CSeqHeader cSeqHeader = headerFactory.createCSeqHeader(1L,
                    Request.INVITE);

            // Create a new MaxForwardsHeader
            MaxForwardsHeader maxForwards = headerFactory
                    .createMaxForwardsHeader(70);

            // Create the request.
            Request request = messageFactory.createRequest(requestURI,
                    Request.INVITE, callIdHeader, cSeqHeader, fromHeader,
                    toHeader, viaHeaders, maxForwards);
            // Create contact headers
            String host = "127.0.0.1"; //hardcoded since this is not handled by us. Mimics a sip client

            // Create the contact name address.
            SipURI contactURI = addressFactory.createSipURI(fromName, host);
            contactURI.setPort(sipProvider.getListeningPoint(transport)
                    .getPort());

            Address contactAddress = addressFactory.createAddress(contactURI);

            // Add the contact address.
            contactAddress.setDisplayName(fromName);

            contactHeader = headerFactory.createContactHeader(contactAddress);
            request.addHeader(contactHeader);

            // You can add extension headers of your own making
            // to the outgoing SIP request.
            // Add the extension header.
            Header extensionHeader = headerFactory.createHeader("My-Header",
                    "my header value");
            request.addHeader(extensionHeader);

            /*
            Just a sample SDP message, to try and extract rtp port and host. Rtp Port is RTP_REMOTE_PORT=6024 and host is 192.168.1.8 as seen in the message
             */
            String sdpData = "v=0\r\n"
                    + "o=4855 13760799956958020 13760799956958020"
                    + " IN IP4 192.168.1.8\r\n" + "s=mysession session\r\n"
                    + "p=+46 8 52018010\r\n" + "c=IN IP4 192.168.1.8\r\n"
                    + "t=0 0\r\n" + "m=audio 6024 RTP/AVP 0 4 18\r\n"
                    + "a=rtpmap:0 PCMU/8000\r\n" + "a=rtpmap:4 G723/8000\r\n"
                    + "a=rtpmap:18 G729A/8000\r\n" + "a=ptime:20\r\n";
            byte[] contents = sdpData.getBytes();

            request.setContent(contents, contentTypeHeader);
            // You can add as many extension headers as you
            // want.

            extensionHeader = headerFactory.createHeader("My-Other-Header",
                    "my new header value ");
            request.addHeader(extensionHeader);

            Header callInfoHeader = headerFactory.createHeader("Call-Info",
                    "<http://www.antd.nist.gov>");
            request.addHeader(callInfoHeader);

            // Create the client transaction.
            inviteTid = sipProvider.getNewClientTransaction(request);

            LOGGER.info("inviteTid = {}", inviteTid);

            // send the request out.

            inviteTid.sendRequest();

            dialog = inviteTid.getDialog();

        } catch (Exception ex) {
            LOGGER.info(ex.getMessage());
            ex.printStackTrace();
        }
    }

    public void processIOException(IOExceptionEvent exceptionEvent) {
        LOGGER.error("IOException event received for host:{} and port:{}", exceptionEvent.getHost(), exceptionEvent.getPort());
    }

    public void processTransactionTerminated(TransactionTerminatedEvent transactionTerminatedEvent) {
        LOGGER.info("Transaction terminated event received");
    }

    public void processDialogTerminated(DialogTerminatedEvent dialogTerminatedEvent) {
        LOGGER.info("dialogTerminatedEvent");
        if (!isShutdown)
            shutDown();
    }

    public void shutDown() {
        try {

            LOGGER.info("nulling client references");
            sipStack.deleteListeningPoint(udpListeningPoint);
            // This will close down the stack and exit all threads
            sipProvider.removeSipListener(this);

            sipStack.deleteSipProvider(sipProvider);

            sipStack.stop();
            sipProvider = null;
            this.inviteTid = null;
            this.contactHeader = null;
            addressFactory = null;
            headerFactory = null;
            messageFactory = null;
            this.udpListeningPoint = null;
            LOGGER.info("Client shutdown");
            isShutdown = true;

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}