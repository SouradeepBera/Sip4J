package com.sprinklr.sip4j.sip;

import com.sprinklr.sip4j.agent.AgentConfig;
import com.sprinklr.sip4j.utils.DigestMD5Converter;
import gov.nist.javax.sip.header.AllowList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.sprinklr.sip4j.sip.SipAllFactories.ADDRESS_FACTORY;
import static com.sprinklr.sip4j.sip.SipAllFactories.HEADER_FACTORY;
import static com.sprinklr.sip4j.sip.SipAllFactories.MESSAGE_FACTORY;


/**
 * Helper class which creates requests
 */
public class SipRequestCreator {

    private static final Logger LOGGER = LoggerFactory.getLogger(SipRequestCreator.class);
    private static final String AUTHENTICATION_SCHEME = "Digest";
    private static final List<String> SIP_ALLOWED_METHODS = Collections.unmodifiableList(Arrays.asList(Request.INVITE, Request.BYE, Request.CANCEL, Request.ACK));
    private static final AllowList ALLOW_LIST;
    static{
        ALLOW_LIST = new AllowList();
        try {
            ALLOW_LIST.setMethods(SIP_ALLOWED_METHODS);
        } catch (ParseException e) {
            LOGGER.error("ParseException, cannot create Allow Header. Empty header will be created. {}", e.toString());
        }
    }
    private final ListeningPoint listeningPoint;
    private final AgentConfig agentConfig;
    private final CallIdHeader callIdHeader;
    //separately keep track of cseq number since REGISTER not part of dialog
    private long cseqNmb = 1;

    public SipRequestCreator(SipProvider sipProvider, AgentConfig agentConfig) {
        this.listeningPoint = sipProvider.getListeningPoint(agentConfig.getTransportMode());
        // Create a new CallId header unique for this call
        this.callIdHeader = sipProvider.getNewCallId();
        this.agentConfig = agentConfig;
    }

    /**
     * Create a REGISTER request without authentication to be sent to the Registrar server
     * @return The REGISTER request
     * @throws ParseException
     * @throws InvalidArgumentException
     */
    public Request createRegisterRequest() throws ParseException, InvalidArgumentException {
        FromHeader fromHeader = getFromHeader();

        ToHeader toHeader = getToHeader();

        // create Register URI - this figures out where to send the request V.IMP
        // The "userinfo" and "@" components of the SIP URI MUST NOT be present, RFC 3261
        SipURI registerURI = ADDRESS_FACTORY.createSipURI(null, agentConfig.getSipRegistrarIp() + ":" + agentConfig.getSipRegistrarPort());

        // Create ViaHeaders - this figures out where the responses to this request are to be sent
        // If via headers are already existing, add your current via header on top of list.
        // Since REGISTER request is initiated by you, your via header will be the only one in the list, hence the use of Collections.singletonList
        ViaHeader viaHeader = HEADER_FACTORY.createViaHeader(listeningPoint.getIPAddress(), listeningPoint.getPort(), agentConfig.getTransportMode(), null);
        List<ViaHeader> viaHeaders = Collections.singletonList(viaHeader);

        // Create a new Cseq header
        CSeqHeader cSeqHeader = HEADER_FACTORY.createCSeqHeader(cseqNmb, Request.REGISTER);

        // Create a new MaxForwardsHeader (convention is 70, but can be anything)
        MaxForwardsHeader maxForwards = HEADER_FACTORY.createMaxForwardsHeader(70);

        // Create the request.
        Request request = MESSAGE_FACTORY.createRequest(registerURI, Request.REGISTER, callIdHeader, cSeqHeader, fromHeader, toHeader, viaHeaders, maxForwards);

        // Create Contact header after creating the contact address.
        //where to contact, differs from FROM header, refer https://stackoverflow.com/questions/31034422/what-is-the-difference-in-contact-and-from-header
        ContactHeader contactHeader = getContactHeader();
        request.addHeader(contactHeader);

        // Create Allow header
        request.addHeader(ALLOW_LIST);

        // Create an Expires header
        ExpiresHeader expiresHeader = HEADER_FACTORY.createExpiresHeader(agentConfig.getSipRegisterExpiryTimeSec());
        request.addHeader(expiresHeader);

        cseqNmb++;
        return request;
    }

    public ContactHeader getContactHeader() throws ParseException {
        SipURI contactURI = ADDRESS_FACTORY.createSipURI(agentConfig.getSipLocalUsername(), agentConfig.getSipLocalIp() + ":" + agentConfig.getSipLocalPort());
        Address contactAddress = ADDRESS_FACTORY.createAddress(agentConfig.getSipLocalDisplayName(), contactURI);
        return HEADER_FACTORY.createContactHeader(contactAddress);
    }

    public ToHeader getToHeader() throws ParseException {
        SipURI toAddress = ADDRESS_FACTORY.createSipURI(agentConfig.getSipLocalUsername(), agentConfig.getSipLocalRealm());
        Address toNameAddress = ADDRESS_FACTORY.createAddress(agentConfig.getSipLocalDisplayName(), toAddress);
        return HEADER_FACTORY.createToHeader(toNameAddress, null);
    }

    public FromHeader getFromHeader() throws ParseException {
        SipURI fromAddress = ADDRESS_FACTORY.createSipURI(agentConfig.getSipLocalUsername(), agentConfig.getSipLocalRealm());
        Address fromNameAddress = ADDRESS_FACTORY.createAddress(agentConfig.getSipLocalDisplayName(), fromAddress);
        return HEADER_FACTORY.createFromHeader(fromNameAddress, agentConfig.getSipLocalTag());
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

        //names of headers in last line of respective header file in jain-sip-ri
        WWWAuthenticateHeader wwwAuthenticateHeader = (WWWAuthenticateHeader) response.getHeader(WWWAuthenticateHeader.NAME);
        //Scheme of authorization should be Digest
        if (!AUTHENTICATION_SCHEME.equals(wwwAuthenticateHeader.getScheme())) {
            throw new SipIncorrectAuthenticationSchemeException(wwwAuthenticateHeader.getScheme() + " not valid. Expected " + AUTHENTICATION_SCHEME);
        }
        Request newRequest = createRegisterRequest();

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
