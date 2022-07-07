package com.sprinklr.javasip.sip;

import com.sprinklr.javasip.agent.AgentConfig;
import com.sprinklr.javasip.agent.AgentState;
import com.sprinklr.javasip.rtp.RtpAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sdp.Connection;
import javax.sdp.Media;
import javax.sdp.MediaDescription;
import javax.sdp.SdpException;
import javax.sdp.SdpParseException;
import javax.sdp.SessionDescription;

import javax.sip.ClientTransaction;
import javax.sip.Dialog;
import javax.sip.DialogState;
import javax.sip.DialogTerminatedEvent;
import javax.sip.IOExceptionEvent;
import javax.sip.InvalidArgumentException;
import javax.sip.ListeningPoint;
import javax.sip.ObjectInUseException;
import javax.sip.PeerUnavailableException;
import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.ServerTransaction;
import javax.sip.SipException;
import javax.sip.SipListener;
import javax.sip.SipProvider;
import javax.sip.SipStack;
import javax.sip.TimeoutEvent;
import javax.sip.Transaction;
import javax.sip.TransactionState;
import javax.sip.TransactionTerminatedEvent;
import javax.sip.TransportNotSupportedException;
import javax.sip.address.Address;
import javax.sip.address.SipURI;
import javax.sip.header.CSeqHeader;
import javax.sip.header.ContactHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;

import java.security.NoSuchAlgorithmException;
import java.text.ParseException;

import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TooManyListenersException;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static com.sprinklr.javasip.sip.SipAllFactories.ADDRESS_FACTORY;
import static com.sprinklr.javasip.sip.SipAllFactories.HEADER_FACTORY;
import static com.sprinklr.javasip.sip.SipAllFactories.MESSAGE_FACTORY;
import static com.sprinklr.javasip.sip.SipAllFactories.SDP_FACTORY;
import static com.sprinklr.javasip.sip.SipAllFactories.SIP_FACTORY;
import static com.sprinklr.javasip.utils.Constants.SLEEP_CPU_TIME_MS;

/**
 * Sip entity which handles signalling on Agent's behalf.
 * It implements SipListener which defines the methods required by an application to receive and process Events that are emitted by an object implementing the SipProvider interface.
 */
public class SipExtension implements SipListener, Callable<RtpAddress> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SipExtension.class);
    private final SipStack sipStack;
    /**
     * Maintains states (SIP state and Websocket state) of the agent
     */
    private final AgentState agentState;
    /**
     * Used to schedule the sendRegisterRequestTask for future execution in a background thread
     */
    private final Timer timer;
    /**
     * The task representing the REGISTER request which is to be sent in regular intervals
     */
    private final TimerTask sendRegisterRequestTask;
    /**
     * The REGISTER request (without authorization) which is to be sent in regular intervals to maintain registration of the Agent in the Registrar server
     */
    private final Request registerRequest;
    /**
     * Helper object to create requests
     */
    private final SipRequestCreator sipRequestCreator;
    /**
     * This interface represents the messaging entity of a SIP stack and as such is the interface that defines the messaging and transactional component view of the SIP stack.
     */
    private final SipProvider sipProvider;
    /**
     * Represents configuration of an Agent, as read from the config file
     */
    private final AgentConfig agentConfig;

    /**
     * The invite server transaction, which might be used in a CANCEL request
     */
    private ServerTransaction inviteServerTransaction;
    /**
     * The INVITE request sent to SipExtension, which might be used in a CANCEL request
     */
    private Request inviteRequest; //storing for CANCEL request
    /**
     * The remote RTP address where data packets are to be sent. It is set upon parsing the INVITE SDP and returned to the Agent
     */
    private RtpAddress rtpRemoteAddress;
    /**
     * Flag variable specifying whether or not the remote RTP address has been set
     */
    private volatile boolean isCallableReady = false;

    /**
     * Initialises a SipExtension for an Agent. Assigns factories, registers it to the registrar server and schedules its future registrations
     * @param agentState Maintains states (SIP state and Websocket state) of the agent
     * @param agentConfig Represents configuration of an Agent, as read from the config file
     * @throws ParseException
     * @throws TooManyListenersException
     * @throws ObjectInUseException
     * @throws PeerUnavailableException
     * @throws TransportNotSupportedException
     * @throws InvalidArgumentException
     */
    public SipExtension(AgentState agentState, AgentConfig agentConfig) throws ParseException, TooManyListenersException, ObjectInUseException, PeerUnavailableException, TransportNotSupportedException, InvalidArgumentException {

        this.agentState = agentState;
        this.agentConfig = agentConfig;

        Properties properties = new Properties();
        properties.setProperty("javax.sip.STACK_NAME", agentConfig.getAgentName());
        sipStack = SIP_FACTORY.createSipStack(properties);

        /*
        This interface represents a unique IP network listening point, which consists of port transport and IP.
        A ListeningPoint is a Java representation of the socket that a SipProvider messaging entity uses to send and receive messages.
         */
        ListeningPoint listeningPoint = sipStack.createListeningPoint(agentConfig.getSipLocalIp(), agentConfig.getSipLocalPort(), agentConfig.getTransportMode());
        sipProvider = sipStack.createSipProvider(listeningPoint);
        sipProvider.addSipListener(this);

        //use SipRequestCreator to create any requests to be sent from our sip entity. Currently, only REGISTER request is sent.
        sipRequestCreator = new SipRequestCreator(sipProvider, agentConfig);
        registerRequest = sipRequestCreator.createRegisterRequest();

        timer = new Timer();
        sendRegisterRequestTask = new SendRegisterRequestTask();
        //re-registers to prevent expiry
        timer.scheduleAtFixedRate(sendRegisterRequestTask,
                0,      // run first occurrence immediately
                TimeUnit.SECONDS.toMillis(agentConfig.getSipRegisterExpiryTimeSec() / 2)); // run every REGISTER_EXPIRY_TIME/2 seconds
    }

    /**
     * Overridden method of Callable.
     * @return The remote RTP address after it has been set
     * @throws InterruptedException
     */
    @Override
    public RtpAddress call() throws InterruptedException {
        while (!isCallableReady) {
            //wait for rtpRemoteAddress to be initialised
            Thread.sleep(SLEEP_CPU_TIME_MS); //sleeping for specified time to save cpu cycles
        }
        return rtpRemoteAddress;
    }

    //Handle authentication if required by modifying function. Refer https://www.youtube.com/watch?v=iJeJ072UejI
    class SendRegisterRequestTask extends TimerTask {

        @Override
        public void run() {
            try {
                //create client transaction
                ClientTransaction registerTransaction = sipProvider.getNewClientTransaction(registerRequest);
                //send the request
                registerTransaction.sendRequest();
                LOGGER.info("{} sent REGISTER request", agentConfig.getAgentName());
            } catch (Exception ex) {
                agentState.setSipState(SipState.REGISTRATION_FAILED);
                LOGGER.error("Error while sending REGISTER request in {}: {}", agentConfig.getAgentName(), ex.toString());
            }
        }
    }

    /**
     * Process the requests sent by Ozontel's User Agent Client to Sprinklr's SipEntity (UAS)
     * @param requestEvent Request events represent request messages that are received
     */
    @Override
    public void processRequest(RequestEvent requestEvent) {
        Request request = requestEvent.getRequest();
        ServerTransaction serverTransaction = requestEvent.getServerTransaction();

        LOGGER.info("Request {} received at {} with serverTransaction:{}",
                request.getMethod(), sipStack.getStackName(), serverTransaction);

        switch (request.getMethod()) {
            case Request.INVITE:
                processInviteRequest(requestEvent, serverTransaction);
                break;
            case Request.ACK:
                processAckRequest(serverTransaction);
                break;
            case Request.BYE:
                processByeRequest(requestEvent, serverTransaction);
                break;
            case Request.CANCEL: //CANCEL the pending INVITE request, not used for other requests
                processCancelRequest(requestEvent, serverTransaction);
                break;
            default:
                LOGGER.warn("Request method not supported, not processing in {}", agentConfig.getAgentName());
        }
    }

    /**
     * Process the responses sent by Ozontel's User Agent Server to Sprinklr's SipEntity (UAC)
     * @param responseEvent Response messages emitted as events by the SipProvider.
     */
    @Override
    public void processResponse(ResponseEvent responseEvent) {
        Response response = responseEvent.getResponse();
        CSeqHeader cseq = (CSeqHeader) response.getHeader(CSeqHeader.NAME);

        LOGGER.info("{} received a response: Status Code = {} {}", agentConfig.getAgentName(), response.getStatusCode(), cseq);
        if (cseq == null) {
            LOGGER.warn("Empty cseq header, response not processed in {}", agentConfig.getAgentName());
            return;
        }

        if (!Request.REGISTER.equals(cseq.getMethod())) {
            LOGGER.error("Not a response for REGISTER request, not processing response in {}", agentConfig.getAgentName());
            return;
        }

        processRegisterResponse(response);
    }

    /**
     * Process the response to our REGISTER request, acting as UAC
     * @param response The response obtained from the registrar server to our REGISTER request
     */
    public void processRegisterResponse(Response response) {
        if (response.getStatusCode() == Response.OK) {
            agentState.setSipState(SipState.REGISTERED);
        } else if (response.getStatusCode() == Response.UNAUTHORIZED) {
            LOGGER.info("Received {} for REGISTER request, resending from {}", Response.UNAUTHORIZED, agentConfig.getAgentName());
            try {
                Request newRegisterRequest = sipRequestCreator.createRegisterRequestWithCredentials(response);
                ClientTransaction registerTransaction = sipProvider.getNewClientTransaction(newRegisterRequest); //resending REGISTER request with credentials
                registerTransaction.sendRequest();

            } catch (ParseException | InvalidArgumentException | NoSuchAlgorithmException | SipException e) {
                LOGGER.error("Exception while authenticating REGISTER request in {}: {}", agentConfig.getAgentName(), e.toString());
                agentState.setSipState(SipState.REGISTRATION_FAILED);
            }

        } else {
            LOGGER.error("No 200 or 401 received for REGISTER in {}, some error has occurred", agentConfig.getAgentName());
            agentState.setSipState(SipState.REGISTRATION_FAILED);
        }
    }

    /**
     * Process the ACK request, acting as UAS
     * @param serverTransaction Transaction from server's side
     */
    public void processAckRequest(ServerTransaction serverTransaction) {
        LOGGER.info("{} (UAS): got an ACK! ", agentConfig.getAgentName());
        if (serverTransaction.getDialog() == null) {
            LOGGER.info("Dialog for {} is null", agentConfig.getAgentName());
        } else {
            LOGGER.info("Dialog State in {} = {}", agentConfig.getAgentName(), serverTransaction.getDialog().getState());
        }
    }

    /**
     * Process the INVITE request, acting as UAS. Transitions Agent's SipState from REGISTERED->CONNECTING->CONNECTED
     * @param requestEvent The Request event representing the INVITE request messages that is received
     * @param serverTransaction Transaction from server's side
     */
    public void processInviteRequest(RequestEvent requestEvent, ServerTransaction serverTransaction) {

        Request request = requestEvent.getRequest();

        if (!agentState.getSipState().equals(SipState.REGISTERED))
            return;

        try {
            LOGGER.info("{} (UAS) sending TRYING", agentConfig.getAgentName());
            Response tryingResponse = MESSAGE_FACTORY.createResponse(Response.RINGING, request);

            if (serverTransaction == null) {
                LOGGER.info("Found null serverTransaction while processing INVITE, creating from Sip Provider in {}", agentConfig.getAgentName());
                serverTransaction = sipProvider.getNewServerTransaction(request);
            }

            serverTransaction.sendResponse(tryingResponse);

            agentState.setSipState(SipState.CONNECTING);

            Response okResponse = MESSAGE_FACTORY.createResponse(Response.OK, request);
            //Contact Header is mandatory for the OK to the INVITE
            SipURI contactURI = ADDRESS_FACTORY.createSipURI(agentConfig.getSipLocalUsername(), agentConfig.getSipLocalIp() + ":" + agentConfig.getSipLocalPort());
            Address contactAddress = ADDRESS_FACTORY.createAddress(agentConfig.getSipLocalDisplayName(), contactURI);
            ContactHeader contactHeader = HEADER_FACTORY.createContactHeader(contactAddress);
            okResponse.addHeader(contactHeader);
            //set other data to be conveyed to Ozonetel using okResponse.setContent(). Also add ContentTypeHeader and other headers as required
            this.inviteServerTransaction = serverTransaction;
            LOGGER.info("Invite transaction id: {}", this.inviteServerTransaction);

            this.inviteRequest = request;

            if (inviteServerTransaction.getState() != TransactionState.COMPLETED) {
                LOGGER.info("Dialog state in {} before 200: {}", agentConfig.getAgentName(), inviteServerTransaction.getDialog().getState());
                inviteServerTransaction.sendResponse(okResponse);

                LOGGER.info("Dialog state in {} after 200: {}", agentConfig.getAgentName(), inviteServerTransaction.getDialog().getState());

                SessionDescription remoteSdp = extractSDP(requestEvent);
                Media media = extractMedia(remoteSdp);
                Connection connection = extractConnection(remoteSdp);
                rtpRemoteAddress = new RtpAddress(media.getMediaPort(), connection.getAddress(), connection.getAddressType(), connection.getNetworkType());
                isCallableReady = true;
                LOGGER.info("{} set remote rtp address for Agent", agentConfig.getAgentName());

                agentState.setSipState(SipState.CONNECTED);
            }

        } catch (Exception ex) {
            agentState.setSipState(SipState.DISCONNECTED);
            LOGGER.error("Error while processing INVITE request in {}: {}", agentConfig.getAgentName(), ex.toString());
        }
    }

    /**
     * Process the BYE request, acting as UAS. Transitions Agent's SipState to DISCONNECTED.
     * @param requestEvent The Request event representing the BYE request messages that is received
     * @param serverTransaction Transaction from server's side
     */
    public void processByeRequest(RequestEvent requestEvent, ServerTransaction serverTransaction) {

        Request request = requestEvent.getRequest();
        LOGGER.info("{} Local party = {}", agentConfig.getAgentName(), serverTransaction.getDialog().getLocalParty());
        try {
            LOGGER.info("{} (UAS):  got a BYE sending OK.", agentConfig.getAgentName());
            Response response = MESSAGE_FACTORY.createResponse(200, request);
            serverTransaction.sendResponse(response);

            agentState.setSipState(SipState.DISCONNECTED);

            LOGGER.info("Dialog State in {} is {}", agentConfig.getAgentName(), serverTransaction.getDialog().getState());
            shutDown();
        } catch (Exception ex) {
            agentState.setSipState(SipState.DISCONNECTED);
            LOGGER.error("Error while processing BYE request in {}: {}", agentConfig.getAgentName(), ex.toString());
        }
    }

    /**
     * Process the CANCEL request, acting as UAS. A CANCEL request SHOULD NOT be sent to cancel a request other than INVITE. Refer RFC 3261. Transitions Agent's SipState to DISCONNECTED.
     *
     * @param requestEvent The Request event representing the CANCEL request messages that is received
     * @param serverTransaction Transaction from server's side
     */
    public void processCancelRequest(RequestEvent requestEvent, ServerTransaction serverTransaction) {

        Request request = requestEvent.getRequest();
        try {
            LOGGER.info("{} (UAS) got a CANCEL", agentConfig.getAgentName());
            if (serverTransaction == null) {
                LOGGER.warn("Received null serverTransaction in {}, treating as stray response", agentConfig.getAgentName());
                return;
            }
            Response response = MESSAGE_FACTORY.createResponse(Response.OK, request);
            //send 200 response for CANCEL request
            serverTransaction.sendResponse(response);
            if (serverTransaction.getDialog().getState() != DialogState.CONFIRMED) {
                //send 487 response for the corresponding invite request, client then sends an ACK ending the transaction
                response = MESSAGE_FACTORY.createResponse(Response.REQUEST_TERMINATED, inviteRequest);
                inviteServerTransaction.sendResponse(response);
                agentState.setSipState(SipState.DISCONNECTED);
                shutDown();
            }
        } catch (Exception ex) {
            agentState.setSipState(SipState.DISCONNECTED);
            LOGGER.error("Error while processing CANCEL request in {} : {}", agentConfig.getAgentName(), ex.toString());
        }
    }

    public void processTimeout(TimeoutEvent timeoutEvent) {
        Transaction transaction;
        if (timeoutEvent.isServerTransaction()) {
            transaction = timeoutEvent.getServerTransaction();
        } else {
            transaction = timeoutEvent.getClientTransaction();
        }
        LOGGER.warn("Transaction Timeout event received for {}", agentConfig.getAgentName());
        LOGGER.info("{} state = {}", agentConfig.getAgentName(), transaction.getState());
        LOGGER.info("{} dialog = {}", agentConfig.getAgentName(), transaction.getDialog());
        if (transaction.getDialog() != null) {
            LOGGER.info("{} dialogState = {}", agentConfig.getAgentName(), transaction.getDialog().getState());
        }
    }

    public void processIOException(IOExceptionEvent exceptionEvent) {
        LOGGER.error("IOException event received for {}, host:{} and port:{}", agentConfig.getAgentName(), exceptionEvent.getHost(), exceptionEvent.getPort());
    }

    public void processTransactionTerminated(TransactionTerminatedEvent transactionTerminatedEvent) {
        if (transactionTerminatedEvent.isServerTransaction())
            LOGGER.info("Transaction(as server) terminated event received for {}: {}", agentConfig.getAgentName(), transactionTerminatedEvent.getServerTransaction());
        else
            LOGGER.info("Transaction(as client) terminated event received for {}: {}", agentConfig.getAgentName(), transactionTerminatedEvent.getClientTransaction());
    }

    public void processDialogTerminated(DialogTerminatedEvent dialogTerminatedEvent) {
        LOGGER.info("{} received dialog terminated event", agentConfig.getAgentName());
        Dialog d = dialogTerminatedEvent.getDialog();
        LOGGER.info("Local Party = {}", d.getLocalParty());
    }

    /**
     * Shuts down SipExtension of the Agent. Triggered when a BYE request is received.
     */
    private void shutDown() {
        LOGGER.info("nulling server references for {}", agentConfig.getAgentName());
        sipStack.stop();
        inviteServerTransaction = null;
        inviteRequest = null;
        //cancel registration task running at regular intervals
        LOGGER.info("Cancelling registration task for {}: {}", agentConfig.getAgentName(), sendRegisterRequestTask.cancel());
        timer.cancel();
        LOGGER.info("Cancelling timer for {}", agentConfig.getAgentName());
        LOGGER.info("Server shutdown in {}", agentConfig.getAgentName());
    }

    /**
     * Helper function to extract the SDP content from a request
     * @param requestEvent he Request event representing the request messages that is received
     * @return SessionDescription object representing the SDP content
     * @throws SdpParseException
     */
    public SessionDescription extractSDP(RequestEvent requestEvent) throws SdpParseException {
        Request request = requestEvent.getRequest();
        byte[] sdpContent = (byte[]) request.getContent();
        return SDP_FACTORY.createSessionDescription(new String(sdpContent));
    }

    /**
     * Extracts the connection information from a SDP
     * @param sdp The SessionDescription representing the SDP content
     * @return Connection object representing the connection information
     */
    public Connection extractConnection(SessionDescription sdp) {
        return sdp.getConnection();
    }

    /**
     * Extracts the connection information from a SDP
     * @param sdp The SessionDescription representing the SDP content
     * @return Media object representing the connection information
     * @throws SdpException
     */
    public Media extractMedia(SessionDescription sdp) throws SdpException {
        Vector<MediaDescription> mediaDescriptions = sdp.getMediaDescriptions(false);
        MediaDescription mediaDescription = mediaDescriptions.get(0);
        return mediaDescription.getMedia();
    }
}