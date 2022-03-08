package com.aliyuncs.auth.signers;

import com.aliyuncs.auth.AlibabaCloudCredentials;
import com.aliyuncs.auth.Signer;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public class HmacSM3Signer extends Signer {
    public static final String ENCODING = "UTF-8";
    private static final String ALGORITHM_NAME = "HMAC-SM3";
    private static String HASH_SM3 = "SM3";

    @Override
    public String signString(String stringToSign, String accessKeySecret) {
        throw new IllegalArgumentException();
    }

    @Override
    public String signString(String stringToSign, AlibabaCloudCredentials credentials) {
        return signString(stringToSign, credentials.getAccessKeySecret());
    }

    @Override
    public String getSignerName() {
        return ALGORITHM_NAME;
    }

    @Override
    public String getSignerVersion() {
        return "3.0";
    }

    @Override
    public String getSignerType() {
        return null;
    }

    @Override
    public byte[] hash(byte[] raw) throws NoSuchAlgorithmException {
        if (null == raw) {
            return null;
        }
        MessageDigest digest = MessageDigest.getInstance(HASH_SM3);
        return digest.digest(raw);
    }

    @Override
    public String getContent() {
        return "x-acs-content-sm3";
    }
}
