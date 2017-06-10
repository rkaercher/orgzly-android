package com.orgzly.android.repos;


import android.net.Uri;

import com.orgzly.org.OrgStringUtils;

import java.io.File;
import java.io.Serializable;

public class GitUri implements Serializable {

    // used for individual files
    public static final String PARAMETER_FILEPATH = "PARAMETER_FILEPATH";

    public GitUri() {
        transport = Transport.SSH_KEYPAIR;
    }

    public enum Transport {
        SSH_KEYPAIR,
        SSH_PASSWORD,
        HTTPS_PASSWORD,
        FILE
    }

    public static final String SCHEME = "git";
    public static final String PARAMETER_REMOTE_URI = "PARAMETER_REMOTE_URI";
    public static final String PARAMETER_LOCAL_DIR = "PARAMETER_LOCAL_DIR";
    public static final String PARAMETER_PRIVATE_KEY = "PARAMETER_PRIVATE_KEY";
    public static final String PARAMETER_PUBLIC_KEY = "PARAMETER_PUBLIC_KEY";
    public static final String PARAMETER_TRANSPORT = "PARAMETER_TRANSPORT";
    public static final String PARAMETER_PASSWORD = "PARAMETER_PASSWORD";


    private String gitRemoteUri;
    private String localRepoDir;
    private String privateKey;
    private String publicKey;
    private Transport transport;
    private String password;


    public GitUri(String uri) {
        this(Uri.parse(uri));
    }

    public GitUri(Uri uri) {
        this.localRepoDir = uri.getPath();
        this.publicKey = uri.getQueryParameter(PARAMETER_PUBLIC_KEY);
        this.privateKey = uri.getQueryParameter(PARAMETER_PRIVATE_KEY);
        this.transport = GitUri.Transport.valueOf(uri.getQueryParameter(PARAMETER_TRANSPORT));
        this.password = uri.getQueryParameter(PARAMETER_PASSWORD);
        this.gitRemoteUri = uri.getQueryParameter(PARAMETER_REMOTE_URI);
    }

    private String getScheme(Transport transport) {
        switch (transport) {
            case FILE:
                return "file";
            default:
                return "ssh";
        }
    }

    public String getLocalRepoDir() {
        return localRepoDir;
    }


    public String getPassword() {
        return password;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public Transport getTransport() {
        return transport;
    }

    public String getGitRemoteUri() {
        return gitRemoteUri;
    }

    public String getGitRemoteUriWithScheme() {
        return getScheme(this.transport) + "://" + gitRemoteUri;
    }

    public Uri getOrgzlyUri() {
        Uri.Builder builder = new Uri.Builder();
        builder.scheme(SCHEME);
        builder.path(localRepoDir);
        builder.appendQueryParameter(PARAMETER_LOCAL_DIR, localRepoDir);
        builder.appendQueryParameter(PARAMETER_TRANSPORT, transport.toString());
        builder.appendQueryParameter(PARAMETER_REMOTE_URI, gitRemoteUri);

        switch (transport) {
            case SSH_KEYPAIR:
                builder.appendQueryParameter(PARAMETER_PUBLIC_KEY, publicKey);
                builder.appendQueryParameter(PARAMETER_PRIVATE_KEY, privateKey);
                break;
            case SSH_PASSWORD:
                builder.appendQueryParameter(PARAMETER_PASSWORD, password);
                break;
        }

        return builder.build();
    }

    public boolean hasKeys() {
        return !OrgStringUtils.isEmpty(privateKey) && !OrgStringUtils.isEmpty(publicKey);
    }

    public boolean areLoginCredentialsComplete() {
        if (!hasRemoteUri()) {
            return false;
        }
        switch (transport) {
            case SSH_KEYPAIR:
                return hasKeys();
            case SSH_PASSWORD:
                return hasPassword();
        }
        return false;
    }

    public boolean hasPassword() {
        return !OrgStringUtils.isEmpty(password);
    }

    public boolean hasRemoteUri() {
        return !OrgStringUtils.isEmpty(gitRemoteUri);
    }

    public void setLocalRepoDir(String localRepoDir) {
        this.localRepoDir = localRepoDir;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setGitRemoteUri(String uriString) {
        gitRemoteUri = uriString;
    }

    public void setTransport(Transport transport) {
        this.transport = transport;
    }
}
