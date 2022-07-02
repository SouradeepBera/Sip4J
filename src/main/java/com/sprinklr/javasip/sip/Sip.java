package com.sprinklr.javasip.sip;

import com.sprinklr.javasip.agent.AgentConfig;
import com.sprinklr.javasip.agent.AgentState;
import com.sprinklr.javasip.rtp.RtpAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sdp.*;
import javax.sip.*;
import javax.sip.address.Address;
import javax.sip.address.AddressFactory;
import javax.sip.address.SipURI;
import javax.sip.header.CSeqHeader;
import javax.sip.header.ContactHeader;
import javax.sip.header.HeaderFactory;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import javax.sip.message.Response;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/*
* Sip entity which handles singalling on Agent's behalf
 */
public class Sip implements SipListener, Callable<RtpAddress> {

    private static final Logger LOGGER = LoggerFactory.getLogger(Sip.class);

    private final SdpFactory sdpFactory;
    private final AddressFactory addressFactory;
    private final MessageFactory messageFactory;
    private final HeaderFactory headerFactory;
    private final SipStack sipStack;
    private final AgentState agentState;
    private final Timer timer;
    private final TimerTask sendRegisterRequestTask;
    private final Request registerRequest;
    private final SipRequestCreator sipRequestCreator;
    private final SipProvider sipProvider;
    private final AgentConfig agentConfig;

    private ServerTransaction inviteServerTransaction; //storing for CANCEL request
    private Request inviteRequest; //storing for CANCEL request
    private RtpAddress rtpRemoteAddress;
    private volatile boolean isCallableReady = false;

    public Sip(SipAllFactories sipAllFactories, AgentState agentState, AgentConfig agentConfig) throws PeerUnavailableException, TransportNotSupportedException, InvalidArgumentException, ObjectInUseException, TooManyListenersException, ParseException {

        this.agentState = agentState;
        this.agentConfig = agentConfig;
        SipFactory sipFactory = sipAllFactories.getSipFactory();
        this.sdpFactory = sipAllFactories.getSdpFactory();
        this.messageFactory = sipAllFactories.getMessageFactory();
        this.addressFactory = sipAllFactories.getAddressFactory();
        this.headerFactory = sipAllFactories.getHeaderFactory();

        Properties properties = new Properties();
        properties.setProperty("javax.sip.STACK_NAME", agentConfig.agentName);
        sipStack = sipFactory.createSipStack(properties);

        ListeningPoint listeningPoint = sipStack.createListeningPoint(agentConfig.sipLocalIp, agentConfig.sipLocalPort, agentConfig.transportMode);
        sipProvider = sipStack.createSipProvider(listeningPoint);
        sipProvider.addSipListener(this);

        //use SipRequestCreator to create any requests to be sent from our sip entity. Currently, only REGISTER request is sent.
        sipRequestCreator = new SipRequestCreator(sipProvider, addressFactory, messageFactory, headerFactory, agentConfig);
        registerRequest = sipRequestCreator.createRegisterRequest();

        timer = new Timer();
        sendRegisterRequestTask = new SendRegisterRequestTask();
        //re-registers to prevent expiry
        timer.scheduleAtFixedRate(sendRegisterRequestTask,
                0,      // run first occurrence immediately
                TimeUnit.SECONDS.toMillis(agentConfig.sipRegisterExpiryTimeSec/2)); // run every REGISTER_EXPIRY_TIME/2 seconds
    }

    @Override
    public RtpAddress call() throws InterruptedException {
        while(!isCallableReady){
            //wait for rtpRemoteAddress to be initialised
            Thread.sleep(20); //sleeping for 20ms to save cpu cycles
        }
        return rtpRemoteAddress;
    }

    //Handle authentication if required by modifying function. Refer https://www.youtube.com/watch?v=iJeJ072UejI
    class SendRegisterRequestTask extends TimerTask{

        @Override
        public void run() {
            try{
                //create client transaction
                ClientTransaction registerTransaction = sipProvider.getNewClientTransaction(registerRequest);
                //send the request
                registerTransaction.sendRequest();
                LOGGER.info("{} sent REGISTER request", agentConfig.agentName);
            } catch (Exception ex){
                agentState.setSipState(SipState.REGISTRATION_FAILED);
                LOGGER.error("Error while sending REGISTER request in {}: {}", agentConfig.agentName, ex.toString());
            }
        }
    }

    /*
    Process the requests sent by Ozontel's User Agent Client to Sprinklr's SipEntity (UAS)
     */
    @Override
    public void processRequest(RequestEvent requestEvent) {
        Request request = requestEvent.getRequest();
        ServerTransaction serverTransaction = requestEvent.getServerTransaction();

        LOGGER.info("Request {} received at {} with serverTransaction:{}",
                request.getMethod(), sipStack.getStackName(), serverTransaction);

        switch(request.getMethod()){
            case Request.INVITE:
                processInviteRequest(requestEvent, serverTransaction);
                break;
            case Request.ACK:
                processAckRequest(serverTransaction);
                break;
            case Request.BYE:
                processByeRequest(requestEvent, serverTransaction);
                break;
            case Request.CANCEL: //CANCEL the pending INVITE request, not used for other requests as of now
                processCancelRequest(requestEvent, serverTransaction);
                break;
            default:
                LOGGER.warn("Request method not supported, not processing in {}", agentConfig.agentName);
        }
    }

    /*
    Process the responses sent by Ozontel's User Agent Server to Sprinklr's SipEntity (UAC)
     */
    @Override
    public void processResponse(ResponseEvent responseEvent) {
        Response response = responseEvent.getResponse();
        CSeqHeader cseq = (CSeqHeader) response.getHeader(CSeqHeader.NAME);

        LOGGER.info("{} received a response: Status Code = {} {}", agentConfig.agentName, response.getStatusCode(), cseq);
        if(cseq == null){
            LOGGER.warn("Empty cseq header, response not processed in {}", agentConfig.agentName);
            return;
        }

        if (!Request.REGISTER.equals(cseq.getMethod())){
            LOGGER.error("Not a response for REGISTER request, not processing response in {}", agentConfig.agentName);
            return;
        }

        processRegisterResponse(response);
    }

    /*
    Process the response to our REGISTER request, acting as UAC
     */
    public void processRegisterResponse(Response response){
        if (response.getStatusCode() == Response.OK) {
            agentState.setSipState(SipState.REGISTERED);
        } else if (response.getStatusCode() == Response.UNAUTHORIZED){
            LOGGER.info("Received {} for REGISTER request, resending from {}", Response.UNAUTHORIZED, agentConfig.agentName);
            try {
                Request newRegisterRequest = sipRequestCreator.createRegisterRequestWithCredentials(response);
                ClientTransaction registerTransaction = sipProvider.getNewClientTransaction(newRegisterRequest); //resending REGISTER request with credentials
                registerTransaction.sendRequest();

            } catch (ParseException | InvalidArgumentException | NoSuchAlgorithmException | SipException e) {
                LOGGER.error("Exception while authenticating REGISTER request in {}: {}", agentConfig.agentName, e.toString());
                agentState.setSipState(SipState.REGISTRATION_FAILED);
            }

        } else {
            LOGGER.error("No 200 or 401 received for REGISTER in {}, some error has occurred", agentConfig.agentName);
            agentState.setSipState(SipState.REGISTRATION_FAILED);
        }
    }

    /*
     Process the ACK request, acting as UAS
     */
    public void processAckRequest(ServerTransaction serverTransaction) {
        LOGGER.info("{} (UAS): got an ACK! ", agentConfig.agentName);
        if(serverTransaction.getDialog() == null) {
            LOGGER.info("Dialog for {} is null", agentConfig.agentName);
        } else {
            LOGGER.info("Dialog State in {} = {}", agentConfig.agentName, serverTransaction.getDialog().getState());
        }
    }

    /*
     * Process the INVITE request, acting as UAS
     */
    public void processInviteRequest(RequestEvent requestEvent, ServerTransaction serverTransaction) {

        Request request = requestEvent.getRequest();

        if(!agentState.getSipState().equals(SipState.REGISTERED))
            return;

        try {
            LOGGER.info("{} (UAS) sending TRYING", agentConfig.agentName);
            Response tryingResponse = messageFactory.createResponse(Response.RINGING, request);

            if (serverTransaction == null) {
                LOGGER.info("Found null serverTransaction while processing INVITE, creating from Sip Provider in {}", agentConfig.agentName);
                serverTransaction = sipProvider.getNewServerTransaction(request);
            }

            serverTransaction.sendResponse(tryingResponse);

            agentState.setSipState(SipState.CONNECTING);

            Response okResponse = messageFactory.createResponse(Response.OK, request);
            //Contact Header is mandatory for the OK to the INVITE
            SipURI contactURI = addressFactory.createSipURI(agentConfig.sipLocalUsername, agentConfig.sipLocalIp + ":" + agentConfig.sipLocalPort);
            Address contactAddress = addressFactory.createAddress(agentConfig.sipLocalDisplayName, contactURI);
            ContactHeader contactHeader = headerFactory.createContactHeader(contactAddress);
            okResponse.addHeader(contactHeader);
            this.inviteServerTransaction = serverTransaction;
            LOGGER.info("Invite transaction id: {}", this.inviteServerTransaction);

            this.inviteRequest = request;

            if (inviteServerTransaction.getState() != TransactionState.COMPLETED) {
                LOGGER.info("Dialog state in {} before 200: {}", agentConfig.agentName, inviteServerTransaction.getDialog().getState());
                inviteServerTransaction.sendResponse(okResponse);

                LOGGER.info("Dialog state in {} after 200: {}", agentConfig.agentName, inviteServerTransaction.getDialog().getState());

                SessionDescription remoteSdp = extractSDP(requestEvent);
                Media media = extractMedia(remoteSdp);
                Connection connection = extractConnection(remoteSdp);
                rtpRemoteAddress = new RtpAddress(media.getMediaPort(), connection.getAddress(), connection.getAddressType(), connection.getNetworkType());
                isCallableReady = true;
                LOGGER.info("{} set remote rtp address for Agent", agentConfig.agentName);

                agentState.setSipState(SipState.CONNECTED);
            }

        } catch (Exception ex) {
            agentState.setSipState(SipState.DISCONNECTED);
            LOGGER.error("Error while processing INVITE request in {}: {}", agentConfig.agentName, ex.toString());
        }
    }

    /*
     * Process the BYE request, acting as UAS
     */
    public void processByeRequest(RequestEvent requestEvent, ServerTransaction serverTransaction) {

        Request request = requestEvent.getRequest();
        LOGGER.info("{} Local party = {}", agentConfig.agentName, serverTransaction.getDialog().getLocalParty());
        try {
            LOGGER.info("{} (UAS):  got a BYE sending OK.", agentConfig.agentName);
            Response response = messageFactory.createResponse(200, request);
            serverTransaction.sendResponse(response);

            agentState.setSipState(SipState.DISCONNECTED);

            LOGGER.info("Dialog State in {} is {}", agentConfig.agentName, serverTransaction.getDialog().getState());
            shutDown();
        } catch (Exception ex) {
            agentState.setSipState(SipState.DISCONNECTED);
            LOGGER.error("Error while processing BYE request in {}: {}", agentConfig.agentName, ex.toString());
        }
    }

    /*
     * Process the CANCEL request, acting as UAS. A CANCEL request SHOULD NOT be sent to cancel a request other than
     * INVITE. Refer RFC 3261.
     */
    public void processCancelRequest(RequestEvent requestEvent, ServerTransaction serverTransaction) {

        Request request = requestEvent.getRequest();
        try {
            LOGGER.info("{} (UAS) got a CANCEL", agentConfig.agentName);
            if (serverTransaction == null) {
                LOGGER.warn("Received null serverTransaction in {}, treating as stray response", agentConfig.agentName);
                return;
            }
            Response response = messageFactory.createResponse(Response.OK, request);
            //send 200 response for CANCEL request
            serverTransaction.sendResponse(response);
            if (serverTransaction.getDialog().getState() != DialogState.CONFIRMED) {
                //send 487 response for the corresponding invite request, client then sends an ACK ending the transaction
                response = messageFactory.createResponse(Response.REQUEST_TERMINATED, inviteRequest);
                inviteServerTransaction.sendResponse(response);
                agentState.setSipState(SipState.DISCONNECTED);
                shutDown();
            }
        } catch (Exception ex) {
            agentState.setSipState(SipState.DISCONNECTED);
            LOGGER.error("Error while processing CANCEL request in {} : {}", agentConfig.agentName, ex.toString());
        }
    }

    public void processTimeout(TimeoutEvent timeoutEvent) {
        Transaction transaction;
        if (timeoutEvent.isServerTransaction()) {
            transaction = timeoutEvent.getServerTransaction();
        } else {
            transaction = timeoutEvent.getClientTransaction();
        }
        LOGGER.warn("Transaction Timeout event received for {}", agentConfig.agentName);
        LOGGER.info("{} state = {}", agentConfig.agentName, transaction.getState());
        LOGGER.info("{} dialog = {}", agentConfig.agentName, transaction.getDialog());
        if(transaction.getDialog() != null) {
            LOGGER.info("{} dialogState = {}", agentConfig.agentName, transaction.getDialog().getState());
        }
    }

    public void processIOException(IOExceptionEvent exceptionEvent) {
        LOGGER.error("IOException event received for {}, host:{} and port:{}", agentConfig.agentName, exceptionEvent.getHost(), exceptionEvent.getPort());
    }

    public void processTransactionTerminated(TransactionTerminatedEvent transactionTerminatedEvent) {
        if (transactionTerminatedEvent.isServerTransaction())
            LOGGER.info("Transaction(as server) terminated event received for {}: {}", agentConfig.agentName, transactionTerminatedEvent.getServerTransaction());
        else
            LOGGER.info("Transaction(as client) terminated event received for {}: {}", agentConfig.agentName, transactionTerminatedEvent.getClientTransaction());
    }

    public void processDialogTerminated(DialogTerminatedEvent dialogTerminatedEvent) {
        LOGGER.info("{} received dialog terminated event", agentConfig.agentName);
        Dialog d = dialogTerminatedEvent.getDialog();
        LOGGER.info("Local Party = {}", d.getLocalParty());
    }

    private void shutDown() {
        try {
            LOGGER.info("nulling server references for {}", agentConfig.agentName);
            sipStack.stop();
            inviteServerTransaction = null;
            inviteRequest = null;
            //cancel registration task running at regular intervals
            LOGGER.info("Cancelling registration task for {}: {}", agentConfig.agentName, sendRegisterRequestTask.cancel());
            timer.cancel();
            LOGGER.info("Cancelling timer for {}", agentConfig.agentName);

            LOGGER.info("Server shutdown in {}", agentConfig.agentName);

        } catch (Exception ex) {
            LOGGER.error("Error in shutting down server for {}: {}", agentConfig.agentName, ex.toString());
        }
    }

    public SessionDescription extractSDP(RequestEvent requestEvent){
        Request request = requestEvent.getRequest();
        byte[] sdpContent = (byte[]) request.getContent();
        SessionDescription sessionDescription = null;
        try {
            sessionDescription = sdpFactory.createSessionDescription(new String(sdpContent));
        } catch (SdpParseException e) {
            LOGGER.error("Error while extracting SDP in {}", agentConfig.agentName);
        }
        return sessionDescription;
    }

    public Connection extractConnection(SessionDescription sdp){
        return sdp.getConnection();
    }

    public Media extractMedia(SessionDescription sdp){
        Media media = null;
        try {
            Vector<MediaDescription> mediaDescriptions= sdp.getMediaDescriptions(false);
            MediaDescription mediaDescription = mediaDescriptions.get(0);
            media = mediaDescription.getMedia();
        } catch (SdpException e) {
            LOGGER.error("Error while extracting media from sdp content in {}", agentConfig.agentName);
        }
        return media;
    }
}