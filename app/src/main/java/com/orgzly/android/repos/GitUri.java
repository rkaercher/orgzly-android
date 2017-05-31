package com.orgzly.android.repos;


import android.net.Uri;

import com.orgzly.org.OrgStringUtils;

import java.io.File;
import java.io.Serializable;

public class GitUri implements Serializable {

    // used for individual files
    public static final String PARAMETER_FILEPATH = "PARAMETER_FILEPATH";

    public GitUri() {
        transport = Transport.SSH;
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
    public static final String PARAMETER_PASSWORD = "PARAMETER_PASSWORD";


    private String gitRemoteUri;
    private File localRepoDir;
    private String privateKey;
    private String publicKey;
    private Transport transport;
    private String password;


    public GitUri(String uri) {
        this(Uri.parse(uri));
    }

    public GitUri(Uri uri) {
        this.gitRemoteUri = makeGitRemoteUri(uri);
        this.localRepoDir = new File(uri.getQueryParameter(PARAMETER_LOCAL_DIR));
        this.publicKey = uri.getQueryParameter(PARAMETER_PUBLIC_KEY);
        this.privateKey = uri.getQueryParameter(PARAMETER_PRIVATE_KEY);
        this.transport = GitUri.Transport.valueOf(uri.getQueryParameter(PARAMETER_TRANSPORT));
        this.password = uri.getQueryParameter(PARAMETER_PASSWORD);
    }

    private String makeGitRemoteUri(Uri uri) {
        switch (this.transport) {
            case FILE:
                return uri.buildUpon().clearQuery().scheme("file").build().toString();
            default:
                return uri.buildUpon().clearQuery().scheme("ssh").build().toString();
        }
    }

    public File getLocalRepoDir() {
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

    public Uri getUri() {
        Uri uri = Uri.parse(gitRemoteUri);

        Uri.Builder builder = uri.buildUpon();
        builder.
                scheme(SCHEME).
                appendQueryParameter(PARAMETER_LOCAL_DIR, localRepoDir.getAbsolutePath()).
                appendQueryParameter(PARAMETER_TRANSPORT, transport.toString());

        //TODO make conditional
        builder.appendQueryParameter(PARAMETER_PUBLIC_KEY, publicKey);
        builder.appendQueryParameter(PARAMETER_PRIVATE_KEY, privateKey);

        return uri;
    }

    public boolean hasKeys() {
        return !OrgStringUtils.isEmpty(privateKey) && !OrgStringUtils.isEmpty(publicKey);
    }

    public boolean hasPassword() {
        return !OrgStringUtils.isEmpty(password);
    }

    public boolean hasRemoteUri() {
        return !OrgStringUtils.isEmpty(gitRemoteUri);
    }

    public void setLocalRepoDir(String localRepoDir) {
        this.localRepoDir = new File(localRepoDir);
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

}
