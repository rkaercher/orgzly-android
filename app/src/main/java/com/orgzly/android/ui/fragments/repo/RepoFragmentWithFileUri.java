package com.orgzly.android.ui.fragments.repo;

import android.net.Uri;

/**
 * Interface for Fragments which have one or more file uris which can be set.
 */
public interface RepoFragmentWithFileUri {
    void updateFileUri(Uri uri, String TAG);
}
