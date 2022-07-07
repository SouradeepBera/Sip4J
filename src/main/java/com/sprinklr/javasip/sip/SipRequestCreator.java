package com.sprinklr.javasip.sip;

import com.sprinklr.javasip.agent.AgentConfig;
import com.sprinklr.javasip.utils.DigestMD5Converter;
import gov.nist.javax.sip.header.AllowList;

import javax.sip.InvalidArgumentException;
import javax.sip.ListeningPoint;
import javax.sip.SipProvider;
import javax.sip.address.Address;
import javax.sip.address.SipURI;
import javax.sip.header.AuthorizationHeader;
import javax.sip.header.CSeqHeader;
import javax.sip.header.CallIdHeader;
import javax.sip.header.ContactHeader;
import javax.sip.header.ExpiresHeader;
import javax.sip.header.FromHeader;
import javax.sip.header.MaxForwardsHeader;
import javax.sip.header.ToHeader;
import javax.sip.header.ViaHeader;
import javax.sip.header.WWWAuthenticateHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.sprinklr.javasip.sip.SipAllFactories.ADDRESS_FACTORY;
import static com.sprinklr.javasip.sip.SipAllFactories.HEADER_FACTORY;
import static com.sprinklr.javasip.sip.SipAllFactories.MESSAGE_FACTORY;


/**
 * Helper class which creates requests
 */
public class SipRequestCreator {

    private static final String AUTHENTICATION_SCHEME = "Digest";
    private static final List<String> SIP_ALLOWED_METHODS = Collections.unmodifiableList(Arrays.asList(Request.INVITE, Request.BYE, Request.CANCEL, Request.ACK));
    private final SipProvider sipProvider;
    private final ListeningPoint listeningPoint;
    private final AgentConfig agentConfig;
    private long cseqNmb = 1; //separately keep track of cseq number since REGISTER not part of dialog

    public SipRequestCreator(SipProvider sipProvider, AgentConfig agentConfig) {
        this.sipProvider = sipProvider;
        this.listeningPoint = sipProvider.getListeningPoint(agentConfig.getTransportMode());
        this.agentConfig = agentConfig;
    }

    /**
     * Create a REGISTER request without authentication to be sent to the Registrar server
     * @return The REGISTER request
     * @throws ParseException
     * @throws InvalidArgumentException
     */
    public Request createRegisterRequest() throws ParseException, InvalidArgumentException {
        //create from header
        SipURI fromAddress = ADDRESS_FACTORY.createSipURI(agentConfig.getSipLocalUsername(), agentConfig.getSipLocalRealm());
        Address fromNameAddress = ADDRESS_FACTORY.createAddress(agentConfig.getSipLocalDisplayName(), fromAddress);
        FromHeader fromHeader = HEADER_FACTORY.createFromHeader(fromNameAddress, agentConfig.getSipLocalTag());

        // create to Header
        SipURI toAddress = ADDRESS_FACTORY.createSipURI(agentConfig.getSipLocalUsername(), agentConfig.getSipLocalRealm());
        Address toNameAddress = ADDRESS_FACTORY.createAddress(agentConfig.getSipLocalDisplayName(), toAddress);
        ToHeader toHeader = HEADER_FACTORY.createToHeader(toNameAddress, null);

        // create Register URI
        SipURI registerURI = ADDRESS_FACTORY.createSipURI(null, agentConfig.getSipRegistrarIp() + ":" + agentConfig.getSipRegistrarPort()); //The "userinfo" and "@" components of the SIP URI MUST NOT be present, RFC 3261

        // Create ViaHeaders
        ArrayList<ViaHeader> viaHeaders = new ArrayList<>();
        ViaHeader viaHeader = HEADER_FACTORY.createViaHeader(listeningPoint.getIPAddress(), listeningPoint.getPort(), agentConfig.getTransportMode(), null);
        // add via headers
        viaHeaders.add(viaHeader);

        // Create a new CallId header
        CallIdHeader callIdHeader = sipProvider.getNewCallId();

        // Create a new Cseq header
        CSeqHeader cSeqHeader = HEADER_FACTORY.createCSeqHeader(cseqNmb, Request.REGISTER);

        // Create a new MaxForwardsHeader
        MaxForwardsHeader maxForwards = HEADER_FACTORY.createMaxForwardsHeader(70);

        // Create the request.
        Request request = MESSAGE_FACTORY.createRequest(registerURI,
                Request.REGISTER, callIdHeader, cSeqHeader, fromHeader,
                toHeader, viaHeaders, maxForwards);

        // Create Contact header after creating the contact address.
        //where to contact, differs from FROM header, refer https://stackoverflow.com/questions/31034422/what-is-the-difference-in-contact-and-from-header
        SipURI contactURI = ADDRESS_FACTORY.createSipURI(agentConfig.getSipLocalUsername(), agentConfig.getSipLocalIp() + ":" + agentConfig.getSipLocalPort());
        Address contactAddress = ADDRESS_FACTORY.createAddress(agentConfig.getSipLocalDisplayName(), contactURI);
        ContactHeader contactHeader = HEADER_FACTORY.createContactHeader(contactAddress);
        request.addHeader(contactHeader);

        // Create Allow header
        AllowList allowList = new AllowList();
        allowList.setMethods(SIP_ALLOWED_METHODS);
        request.addHeader(allowList);

        // Create an Expires header
        ExpiresHeader expiresHeader = HEADER_FACTORY.createExpiresHeader(agentConfig.getSipRegisterExpiryTimeSec());
        request.addHeader(expiresHeader);

        cseqNmb++;
        return request;
    }

    /**
     * Create a REGISTER request with authentication to be sent to the Registrar server
     * @param response The 401 initial response which contains the nonce key
     * @return The REGISTER request with the Authorization header
     * @throws ParseException
     * @throws InvalidArgumentException
     * @throws NoSuchAlgorithmException
     */
    public Request createRegisterRequestWithCredentials(Response response) throws ParseException, InvalidArgumentException, NoSuchAlgorithmException {

        WWWAuthenticateHeader wwwAuthenticateHeader = (WWWAuthenticateHeader) response.getHeader(WWWAuthenticateHeader.NAME); //names of headers in last line of respective header file in jain-sip-ri
        if (!AUTHENTICATION_SCHEME.equals(wwwAuthenticateHeader.getScheme())) { //Scheme of authorization should be Digest
            throw new NoSuchAlgorithmException();
        }
        Request newRequest = createRegisterRequest();
        CallIdHeader oldCallIdHeader = (CallIdHeader) response.getHeader(CallIdHeader.NAME);
        newRequest.setHeader(oldCallIdHeader); //All registrations from a UAC SHOULD use the same Call-ID header field value for registrations sent to a particular registrar

        String userName = agentConfig.getSipLocalUsername();
        String realm = wwwAuthenticateHeader.getRealm();
        String password = agentConfig.getPassword();
        String method = Request.REGISTER;
        String uri = newRequest.getRequestURI().toString();
        String nonce = wwwAuthenticateHeader.getNonce();
        String ans = DigestMD5Converter.digestResponseFromNonce(userName, realm, password, method, uri, nonce);

        AuthorizationHeader authorizationHeader = HEADER_FACTORY.createAuthorizationHeader("");
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
