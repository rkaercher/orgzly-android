package com.orgzly.android.repos;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;

import com.orgzly.android.BookName;
import com.orgzly.android.util.MiscUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Base class that contains common file operations for repos which have a local representation
 * on the device filesystem.
 */

abstract class LocalDirectoryBasedRepo implements Repo {
    private static final String TAG = LocalDirectoryBasedRepo.class.getName();


    void createDir(File dir) throws IOException {
        if (!dir.isDirectory()) {
            if (!dir.mkdirs()) {
                throw new IOException("Failed creating directory " + dir);
            }
        }
    }

    protected abstract Uri getRookUri(File rookFile);

    @Override
    public List<VersionedRook> getBooks() throws IOException {
        List<VersionedRook> result = new ArrayList<>();

        File[] files = getLocalDirectory().listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return BookName.isSupportedFormatFileName(filename);
            }
        });

        if (files != null) {
            Arrays.sort(files);

            for (File file : files) {
                Uri uri = getRookUri(file);

                result.add(new VersionedRook(
                        getUri(),
                        uri,
                        String.valueOf(file.lastModified()),
                        file.lastModified()
                ));
            }

        } else {
            Log.e(TAG, "Listing files in " + getLocalDirectory() + " returned null. No storage permission?");
        }

        return result;
    }

    @Override
    public void delete(Uri uri) throws IOException {
        File file = getFileForUri(uri);

        if (file.exists()) {
            if (!file.delete()) {
                throw new IOException("Failed deleting file " + uri.getPath());
            }
        }
    }

    @Override
    public VersionedRook retrieveBook(Uri uri, File destinationFile) throws IOException {
        File sourceFile = getFileForUri(uri);

        /* "Download" the file. */
        MiscUtils.copyFile(sourceFile, destinationFile);

        long mtime = sourceFile.lastModified();
        String rev = String.valueOf(mtime);

        return new VersionedRook(getUri(), uri, rev, mtime);
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

        long mtime = destinationFile.lastModified();
        String rev = String.valueOf(mtime);

        Uri uri =  getRookUri(destinationFile);

        return new VersionedRook(getUri(), uri, rev, mtime);
    }

    @Override
    public VersionedRook renameBook(Uri fromUri, String name) throws IOException {
        File fromFile = getFileForUri(fromUri);
        File toFile = new File(fromFile.getParentFile(), name + ".org");
        Uri newUri = getRookUri(toFile);

        if (toFile.exists()) {
            throw new IOException("File " + toFile + " already exists");
        }

        if (!fromFile.renameTo(toFile)) {
            throw new IOException("Failed renaming " + fromFile + " to " + toFile);
        }

        long mtime = toFile.lastModified();
        String rev = String.valueOf(mtime);

        return new VersionedRook(getUri(), newUri, rev, mtime);
    }

    @NonNull
    protected abstract File getFileForUri(Uri uri);

    public abstract File getLocalDirectory();
}
