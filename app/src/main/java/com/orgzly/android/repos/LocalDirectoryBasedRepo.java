package com.orgzly.android.repos;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;

import com.orgzly.android.BookName;

import java.io.File;
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

    @NonNull
    protected abstract File getFileForUri(Uri uri);

    public abstract File getLocalDirectory();
}
