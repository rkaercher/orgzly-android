package com.orgzly.android.repos;

import android.net.Uri;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Remote source of books (such as Dropbox directory, SSH directory, etc.)
 */
public interface Repo {
    boolean requiresConnection();

    /**
     * Unique URL.
     */
    Uri getUri();

    /**
     * Retrieve the list of all available books.
     *
     * @return array of all available books
     * @throws IOException
     */
    List<VersionedRook> getBooks() throws IOException;

    /**
     * Download the latest available revision of the book and store its content to {@code File}.
     */
    VersionedRook retrieveBook(Uri uri, File destination) throws IOException;

    /**
     * Uploads book storing it under given filename under repo's url.
     */
    VersionedRook storeBook(File file, String fileName) throws IOException;

    VersionedRook renameBook(Uri from, String name) throws IOException;

    // VersionedRook moveBook(Uri from, Uri uri) throws IOException;

    void delete(Uri uri) throws IOException;

    /**
     * Gets called once before syncing any books to allow the repository to update a cached
     * representation of the remote content.
     */
    void syncStart();

    String toString();
}
