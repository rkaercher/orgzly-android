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

    private void createDir(File dir) throws IOException {
        if (!dir.isDirectory()) {
            if (!dir.mkdirs()) {
                throw new IOException("Failed creating directory " + dir);
            }
        }
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
    public VersionedRook retrieveBook(Uri uri, File destinationFile) throws IOException {
        File sourceFile = getFileForUri(uri);

        /* "Download" the file. */
        MiscUtils.copyFile(sourceFile, destinationFile);

        String rev = String.valueOf(sourceFile.lastModified());
        long mtime = sourceFile.lastModified();

        return new VersionedRook(repoUri, uri, rev, mtime);
    }

    @Override
    public VersionedRook storeBook(File file, String fileName) throws IOException {
        if (!file.exists()) {
            throw new FileNotFoundException("File " + file + " does not exist");
        }

        File destinationFile = new File(getLocalDirectory(), fileName);

        /* Create necessary directories. */
        createDir(destinationFile.getParentFile());

        String content = MiscUtils.readStringFromFile(file);
        MiscUtils.writeStringToFile(content, destinationFile);

        String rev = String.valueOf(destinationFile.lastModified());
        long mtime = destinationFile.lastModified();

        Uri uri = repoUri.buildUpon().appendPath(fileName).build();

        return new VersionedRook(repoUri, uri, rev, mtime);
    }

    @Override
    public VersionedRook renameBook(Uri fromUri, String name) throws IOException {
        File fromFile = getFileForUri(fromUri);
        Uri newUri = UriUtils.getUriForNewName(fromUri, name);
        File toFile = new File(newUri.getPath());

        if (toFile.exists()) {
            throw new IOException("File " + toFile + " already exists");
        }

        if (!fromFile.renameTo(toFile)) {
            throw new IOException("Failed renaming " + fromFile + " to " + toFile);
        }

        String rev = String.valueOf(toFile.lastModified());
        long mtime = toFile.lastModified();

        return new VersionedRook(repoUri, newUri, rev, mtime);
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
