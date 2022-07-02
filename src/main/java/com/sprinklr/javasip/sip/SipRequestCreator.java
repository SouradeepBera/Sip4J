package com.sprinklr.javasip.sip;

import com.sprinklr.javasip.agent.AgentConfig;
import com.sprinklr.javasip.utils.DigestMD5Converter;
import gov.nist.javax.sip.header.AllowList;

import javax.sip.InvalidArgumentException;
import javax.sip.ListeningPoint;
import javax.sip.SipProvider;
import javax.sip.address.Address;
import javax.sip.address.AddressFactory;
import javax.sip.address.SipURI;
import javax.sip.header.*;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import javax.sip.message.Response;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.ArrayList;

/*
 * Helper class which creates requests
 */
public class SipRequestCreator {
    private final SipProvider sipProvider;
    private final AddressFactory addressFactory;
    private final MessageFactory messageFactory;
    private final HeaderFactory headerFactory;
    private final ListeningPoint listeningPoint;
    private final AgentConfig agentConfig;

    public SipRequestCreator(SipProvider sipProvider, AddressFactory addressFactory, MessageFactory messageFactory, HeaderFactory headerFactory, AgentConfig agentConfig) {
        this.sipProvider = sipProvider;
        this.addressFactory = addressFactory;
        this.messageFactory = messageFactory;
        this.headerFactory = headerFactory;
        this.listeningPoint = sipProvider.getListeningPoint(agentConfig.transportMode);
        this.agentConfig = agentConfig;
    }

    public Request createRegisterRequest() throws ParseException, InvalidArgumentException {
        //create from header
        SipURI fromAddress = addressFactory.createSipURI(agentConfig.sipLocalUsername, agentConfig.sipLocalRealm);
        Address fromNameAddress = addressFactory.createAddress(agentConfig.sipLocalDisplayName, fromAddress);
        FromHeader fromHeader = headerFactory.createFromHeader(fromNameAddress, agentConfig.sipLocalTag);

        // create to Header
        SipURI toAddress = addressFactory.createSipURI(agentConfig.sipLocalUsername, agentConfig.sipLocalRealm);
        Address toNameAddress = addressFactory.createAddress(agentConfig.sipLocalDisplayName, toAddress);
        ToHeader toHeader = headerFactory.createToHeader(toNameAddress, null);

        // create Register URI
        SipURI registerURI = addressFactory.createSipURI(null, agentConfig.sipRegistrarIp + ":" + agentConfig.sipRegistrarPort); //The "userinfo" and "@" components of the SIP URI MUST NOT be present, RFC 3261

        // Create ViaHeaders
        ArrayList<ViaHeader> viaHeaders = new ArrayList<>();
        ViaHeader viaHeader = headerFactory.createViaHeader(listeningPoint.getIPAddress(), listeningPoint.getPort(), agentConfig.transportMode, null);
        // add via headers
        viaHeaders.add(viaHeader);

        // Create a new CallId header
        CallIdHeader callIdHeader = sipProvider.getNewCallId();

        // Create a new Cseq header
        CSeqHeader cSeqHeader = headerFactory.createCSeqHeader(1L, Request.REGISTER);

        // Create a new MaxForwardsHeader
        MaxForwardsHeader maxForwards = headerFactory.createMaxForwardsHeader(70);

        // Create the request.
        Request request = messageFactory.createRequest(registerURI,
                Request.REGISTER, callIdHeader, cSeqHeader, fromHeader,
                toHeader, viaHeaders, maxForwards);

        // Create Contact header after creating the contact address.
        //where to contact, differs from FROM header, refer https://stackoverflow.com/questions/31034422/what-is-the-difference-in-contact-and-from-header
        SipURI contactURI = addressFactory.createSipURI(agentConfig.sipLocalUsername, agentConfig.sipLocalIp + ":" + agentConfig.sipLocalPort);
        Address contactAddress = addressFactory.createAddress(agentConfig.sipLocalDisplayName, contactURI);
        ContactHeader contactHeader = headerFactory.createContactHeader(contactAddress);
        request.addHeader(contactHeader);

        // Create Allow header
        AllowList allowList = new AllowList();
        allowList.setMethods(agentConfig.sipAllowedMethods);
        request.addHeader(allowList);

        // Create an Expires header
        ExpiresHeader expiresHeader = headerFactory.createExpiresHeader(agentConfig.sipRegisterExpiryTimeSec);
        request.addHeader(expiresHeader);

        return request;
    }

    public Request createRegisterRequestWithCredentials(Response response) throws ParseException, InvalidArgumentException, NoSuchAlgorithmException {

        WWWAuthenticateHeader wwwAuthenticateHeader = (WWWAuthenticateHeader) response.getHeader("WWW-Authenticate"); //names of headers in last line of respective header file in jain-sip-ri
        if (!"Digest".equals(wwwAuthenticateHeader.getScheme())) { //Scheme of authorization should be Digest
            throw new NoSuchAlgorithmException();
        }
        Request newRequest = createRegisterRequest();
        CallIdHeader oldCallIdHeader = (CallIdHeader) response.getHeader("Call-ID");
        newRequest.setHeader(oldCallIdHeader); //All registrations from a UAC SHOULD use the same Call-ID header field value for registrations sent to a particular registrar
        String userName = agentConfig.sipLocalUsername;
        String realm = wwwAuthenticateHeader.getRealm();
        String password = agentConfig.password;
        String method = Request.REGISTER;
        String uri = newRequest.getRequestURI().toString();
        String nonce = wwwAuthenticateHeader.getNonce();
        String ans = DigestMD5Converter.digestResponseFromNonce(userName, realm, password, method, uri, nonce);

        AuthorizationHeader authorizationHeader = headerFactory.createAuthorizationHeader("");
        authorizationHeader.setAlgorithm(wwwAuthenticateHeader.getAlgorithm());
        authorizationHeader.setNonce(nonce);
        authorizationHeader.setRealm(realm);
        authorizationHeader.setResponse(ans);
        authorizationHeader.setUsername(userName);
        authorizationHeader.setURI(newRequest.getRequestURI());
        authorizationHeader.setScheme(wwwAuthenticateHeader.getScheme());

        newRequest.addHeader(authorizationHeader);
        return newRequest;

    }

}
