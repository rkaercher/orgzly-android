package com.orgzly.android.repos;

import android.net.Uri;

import com.orgzly.android.OrgzlyTest;

import org.junit.Test;

import static org.junit.Assert.*;


public class GitUriTest extends OrgzlyTest {

    public static final String GIT_REMOTE_URI = "git@host.com:123/a/path";
    public static final String PRIVATE_KEY = "private_key";
    public static final String PUBLIC_KEY = "public_key";
    public static final String LOCAL_REPO_DIR = "/tmp";
    public static final String PASSWORD = "aPassword";

    @Test
    public void thatUriCanBeCreatedAndReparsed() {
        GitUri sut = new GitUri();
        sut.setGitRemoteUri(GIT_REMOTE_URI);
        sut.setPrivateKey(PRIVATE_KEY);
        sut.setPublicKey(PUBLIC_KEY);
        sut.setTransport(GitUri.Transport.SSH_KEYPAIR);
        sut.setLocalRepoDir(LOCAL_REPO_DIR);

        Uri orgzlyUri = sut.getOrgzlyUri();

        sut = new GitUri(orgzlyUri);

        assertEquals(GIT_REMOTE_URI, sut.getGitRemoteUri());
        assertEquals(PRIVATE_KEY, sut.getPrivateKey());
        assertEquals(PUBLIC_KEY, sut.getPublicKey());
        assertEquals(LOCAL_REPO_DIR, sut.getLocalRepoDir());

        sut.setTransport(GitUri.Transport.SSH_PASSWORD);
        sut.setPassword(PASSWORD);

        orgzlyUri = sut.getOrgzlyUri();

        sut = new GitUri(orgzlyUri);

        assertEquals(GIT_REMOTE_URI, sut.getGitRemoteUri());
        assertEquals(LOCAL_REPO_DIR, sut.getLocalRepoDir());
        assertEquals(PASSWORD,sut.getPassword());

    }
}