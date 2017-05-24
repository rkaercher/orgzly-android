package com.orgzly.android.util;


import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.KeyPair;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class SshUtil {

    public static class SshKeys {
        private String publicKey;
        private String privateKey;

        public SshKeys(String publicKey, String privateKey) {
            this.publicKey = publicKey;
            this.privateKey = privateKey;
        }

        public String getPublicKey() {
            return publicKey;
        }

        public String getPrivateKey() {
            return privateKey;
        }
    }

    public static SshKeys generateKeys() {
        JSch jsch = new JSch();

        KeyPair kpair;
        ByteArrayOutputStream os = null;
        String publicKey = null, privateKey = null;

        try {
            kpair = KeyPair.genKeyPair(jsch, KeyPair.DSA);

            os = new ByteArrayOutputStream();
            kpair.writePrivateKey(os);
            privateKey = new String(os.toByteArray());
            os.close();
            os = new ByteArrayOutputStream();
            kpair.writePublicKey(os, "Orgzly generated key");
            publicKey = new String(os.toByteArray());

        } catch (JSchException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return new SshKeys(publicKey, privateKey);
    }
}
