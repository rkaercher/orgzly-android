package com.orgzly.android.repos;

import android.net.Uri;
import android.support.annotation.NonNull;

import com.orgzly.android.LocalStorage;
import com.orgzly.android.util.MiscUtils;
import com.orgzly.android.util.UriUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class DirectoryRepo extends LocalDirectoryBasedRepo {
    public static final String SCHEME = "file";

    private File localDirectory;

    private final Uri repoUri;

    /**
     * @param url  repo url, in the format (file:/a/b/c)
     * @param wipe should files be deleted first from directory
     */
    DirectoryRepo(String url, boolean wipe) throws IOException {
        repoUri = Uri.parse(url);

        localDirectory = new File(repoUri.getPath());

        /* Delete entire contents of directory. */
        if (wipe) {
            LocalStorage.deleteRecursive(getLocalDirectory());
        }

        createDir(getLocalDirectory());
    }

    @Override
    public boolean requiresConnection() {
        return false;
    }

    @Override
    public Uri getUri() {
        return repoUri;
    }

    @Override
    protected Uri getRookUri(File rookFile) {
        return repoUri.buildUpon().appendPath(rookFile.getName()).build();
    }

    @Override
    @NonNull
    protected File getFileForUri(Uri uri) {
        return new File(uri.getPath());
    }

    @Override
    public void syncStart() {

    }

    public File getDirectory() {
        return getLocalDirectory();
    }

    @Override
    public String toString() {
        return repoUri.toString();
    }

    @Override
    public File getLocalDirectory() {
        return localDirectory;
    }
}
