package com.orgzly.android.repos;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.orgzly.android.sync.GitSync;

import java.io.File;
import java.io.IOException;
import java.util.List;


public class GitRepo extends LocalDirectoryBasedRepo {
    private static final String TAG = GitRepo.class.getName();

    public static final String SCHEME = "git";
    public static final String PARAMETER_LOCAL_DIR = "PARAMETER_LOCAL_DIR";
    public static final String PARAMETER_PRIVATE_KEY = "PARAMETER_PRIVATE_KEY";
    public static final String PARAMETER_PUBLIC_KEY = "PARAMETER_PUBLIC_KEY";
    public static final String PARAMETER_TRANSPORT = "PARAMETER_TRANSPORT";
    public static final String PARAMETER_FILEPATH = "PARAMETER_FILEPATH";


    public enum Transport {
        SSH,
        HTTPS,
        FILE
    }

    private final File localDir;
    private final String gitUri;
    private final String privateKey;
    private final String publicKey;
    private final Uri uriRepresentation;

    private final GitSync syncer;

    public GitRepo(Context context, String uriString) {
        uriRepresentation = Uri.parse(uriString);
        localDir = new File(uriRepresentation.getQueryParameter(PARAMETER_LOCAL_DIR));
        gitUri = uriRepresentation.buildUpon().clearQuery().scheme("ssh").build().toString();
        privateKey = uriRepresentation.getQueryParameter(PARAMETER_PRIVATE_KEY);
        publicKey = uriRepresentation.getQueryParameter(PARAMETER_PUBLIC_KEY);
        syncer = new GitSync(uriRepresentation);
//        GitSync gitSync = new GitSync(Uri.parse("ssh://git@192.168.1.56:223/syncorg/essential-org"));

    }

    @Override
    public boolean requiresConnection() {
        return true;
    }

    @Override
    public Uri getUri() {
        return uriRepresentation;
    }

    @Override
    public VersionedRook retrieveBook(Uri uri, File destination) throws IOException {
        return null;
    }

    @Override
    public VersionedRook storeBook(File file, String fileName) throws IOException {
        return null;
    }

    @Override
    public VersionedRook renameBook(Uri from, String name) throws IOException {
        return null;
    }


    @Override
    public void syncStart() {
        syncer.pull();
    }

    @Override
    public void delete(Uri uri) throws IOException {
        super.delete(uri);
        syncer.push();
    }

    @Override
    protected Uri getRookUri(File rookFile) {
       return getUri().buildUpon().appendQueryParameter(PARAMETER_FILEPATH, rookFile.getAbsolutePath()).build();
    }

    @NonNull
    @Override
    protected File getFileForUri(Uri uri) {
        return new File(uri.getQueryParameter(PARAMETER_FILEPATH));
    }

    @Override
    public File getLocalDirectory() {
        return localDir;
    }
}
