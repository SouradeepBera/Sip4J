package com.sprinklr.javasip.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;

public class DigestMD5Converter {

    private static MessageDigest md;
    private static final Logger LOGGER = LoggerFactory.getLogger(DigestMD5Converter.class);

    static {
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            md = null;
        }
    }

    private DigestMD5Converter(){}

    private static String hashToMD5(String... args) throws NoSuchAlgorithmException {
        if(md==null){
            LOGGER.error("MessageDigest null, algorithm exception occurred");
            throw new NoSuchAlgorithmException();
        }

        String inputString = String.join(":", args);
        byte[] inputBytes = md.digest(inputString.getBytes());
        BigInteger no = new BigInteger(1, inputBytes);
        String hashText = no.toString(16);
        int padLen = 32 - hashText.length();
        String padding = String.join("", Collections.nCopies(padLen, "0"));
        hashText = padding + hashText;
        return hashText;
    }

    /*
    Refer https://en.wikipedia.org/wiki/Digest_access_authentication
     */
    public static String digestResponseFromNonce(String username, String realm, String password, String method, String uri, String nonce) throws NoSuchAlgorithmException {
        String ha1 = hashToMD5(username, realm, password);
        String ha2 = hashToMD5(method, uri);
        return hashToMD5(ha1, nonce, ha2);
    }
}
