package com.sprinklr.javasip.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;

/**
 * Helper class to handle digest authentication
 */
public class DigestMD5Converter {

    private static final Logger LOGGER = LoggerFactory.getLogger(DigestMD5Converter.class);
    private static final String MD5_ALGORITHM = "MD5";
    private static MessageDigest md;

    static {
        try {
            md = MessageDigest.getInstance(MD5_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            md = null;
        }
    }

    /**
     * Private constructor since it is a utility class
     */
    private DigestMD5Converter() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Returns the MD5 hash of the strings passed (concatenated by ":")
     * @param args The strings to be concatenated with ":" and hashed
     * @return a single hashed concatenated string
     * @throws NoSuchAlgorithmException
     */
    private static String hashToMD5(String... args) throws NoSuchAlgorithmException {
        if (md == null) {
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

    /**
     * Performs digest authentication. Retrieves response from nonce.
     * Refer <a href="https://en.wikipedia.org/wiki/Digest_access_authentication">...</a>
     * @param username username of user
     * @param realm realm of user
     * @param password password of user
     * @param method the SIP method
     * @param uri the SIP uri
     * @param nonce the nonce key returned by server
     * @return the response to the nonce key
     * @throws NoSuchAlgorithmException
     */
    public static String digestResponseFromNonce(String username, String realm, String password, String method, String uri, String nonce) throws NoSuchAlgorithmException {
        String ha1 = hashToMD5(username, realm, password);
        String ha2 = hashToMD5(method, uri);
        return hashToMD5(ha1, nonce, ha2);
    }
}
