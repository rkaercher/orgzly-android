package com.orgzly.android.ui.fragments.repo.git;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import com.orgzly.android.repos.GitUri;
import com.orgzly.android.sync.GitSync;



public class TestGitRepoConnectivityTask extends AsyncTask<String, String, GitSync.GitResult> {
    private ProgressDialog progressDialog;
    private Context context;
    private GitUri gitUri;

    public TestGitRepoConnectivityTask(Context context, GitUri gitUri) {
        this.context = context;
        this.gitUri = gitUri;
    }

    @Override
    protected void onPreExecute() {
        progressDialog = new ProgressDialog(this.context);
        progressDialog.setTitle("Checking Repo Connection");
        progressDialog.show();
    }

    @Override
    protected GitSync.GitResult doInBackground(String... params) {
        GitSync sync = new GitSync(gitUri);
        return sync.isRepoReadable();
    }

    @Override
    protected void onPostExecute(GitSync.GitResult repoReadable) {
        if (progressDialog.isShowing()) {
            progressDialog.hide();
        }
        if (repoReadable.isSuccess()) {
            Toast.makeText(this.context, "Git repository is valid!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this.context, "Problem connecting to the repository:\n" + repoReadable.getErrorMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
