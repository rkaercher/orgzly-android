package com.orgzly.android.repos;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.orgzly.android.sync.GitSync;

import java.io.File;
import java.io.IOException;


public class GitRepo extends LocalDirectoryBasedRepo {
    private static final String TAG = GitRepo.class.getName();

    private final GitUri uriRepresentation;

    private final GitSync syncer;

    GitRepo(Context context, String uriString) {
        uriRepresentation = new GitUri(uriString);
        syncer = new GitSync(uriRepresentation);
    }


    @Override
    public boolean requiresConnection() {
        return true;
    }

    @Override
    public Uri getUri() {
        return uriRepresentation.getOrgzlyUri();
    }

    @Override
    public VersionedRook storeBook(File file, String fileName) throws IOException {
        VersionedRook result = super.storeBook(file, fileName);
        syncer.push();
        return result;
    }

    @Override
    public VersionedRook renameBook(Uri from, String name) throws IOException {
        VersionedRook result = super.renameBook(from, name);
        syncer.push();
        return result;
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
        return getUri().buildUpon().path(rookFile.getAbsolutePath()).build();
    }

    @NonNull
    @Override
    protected File getFileForUri(Uri uri) {
        return new File(uri.getPath());
    }

    @Override
    public File getLocalDirectory() {
        return new File(uriRepresentation.getLocalRepoDir());
    }

    @Override
    public String toString() {
        return "GIT - " + uriRepresentation.getGitRemoteUri();
    }
}
