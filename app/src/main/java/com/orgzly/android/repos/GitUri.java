package com.orgzly.android.repos;


import android.net.Uri;

import java.io.File;
import java.io.Serializable;

public class GitUri implements Serializable {


    public GitUri() {

    }

    public enum Transport {
        SSH,
        HTTPS,
        FILE
    }

    public static final String SCHEME = "git";
    public static final String PARAMETER_LOCAL_DIR = "PARAMETER_LOCAL_DIR";
    public static final String PARAMETER_PRIVATE_KEY = "PARAMETER_PRIVATE_KEY";
    public static final String PARAMETER_PUBLIC_KEY = "PARAMETER_PUBLIC_KEY";
    public static final String PARAMETER_TRANSPORT = "PARAMETER_TRANSPORT";
    public static final String PARAMETER_FILEPATH = "PARAMETER_FILEPATH";


    private  File localRepoDir;
    private  String privateKey;
    private  String publicKey;
    private  Transport transport;
    private  Uri uri;


    public GitUri(String uri) {
        this(Uri.parse(uri));
    }

    public GitUri(Uri uri) {
        this.uri = uri;
        this.localRepoDir = new File(uri.getQueryParameter(PARAMETER_LOCAL_DIR));
        this.publicKey = uri.getQueryParameter(PARAMETER_PUBLIC_KEY);
        this.privateKey = uri.getQueryParameter(PARAMETER_PRIVATE_KEY);
        this.transport = GitUri.Transport.valueOf(uri.getQueryParameter(PARAMETER_TRANSPORT));
    }

    public String getGitRemoteUri() {
        switch (this.transport) {
            case FILE:
                return this.uri.buildUpon().clearQuery().scheme("file").build().toString();
            default:
                return this.uri.buildUpon().clearQuery().scheme("ssh").build().toString();
        }
    }

    public File getLocalRepoDir() {
        return localRepoDir;
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

    public Uri getUri() {
        return uri;
    }
}
