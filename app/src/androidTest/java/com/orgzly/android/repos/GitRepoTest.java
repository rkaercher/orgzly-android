package com.orgzly.android.repos;

import android.net.Uri;
import android.support.test.espresso.core.deps.guava.io.Files;
import android.support.test.espresso.core.deps.guava.io.PatternFilenameFilter;

import com.orgzly.android.OrgzlyTest;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;


public class GitRepoTest extends OrgzlyTest {
    public static final String TEMPBRANCH_NAME = "tempbranch";
    private Git remoteGit;
    private File remoteGitDir;
    private File targetDir;

    @Before
    public void before() throws GitAPIException {
        remoteGitDir = Files.createTempDir();
        remoteGit = Git.init()
                .setDirectory(remoteGitDir)
                .call();
        targetDir = Files.createTempDir();
    }

    @Test
    public void thatSyncStartClones() throws GitAPIException, IOException {
        File testFile = File.createTempFile("test1", ".org", remoteGitDir);
        remoteGit.add().addFilepattern(testFile.getName()).call();
        commitRemote();
        String gitRepoUri = getRepoUri();
        GitRepo gitRepo = new GitRepo(context, gitRepoUri);
        List<VersionedRook> books = gitRepo.getBooks();
        assertEquals(0, books.size());

        gitRepo.syncStart();

        books = gitRepo.getBooks();
        assertEquals(1, books.size());
    }

    @Test
    public void thatAFileCanBeDeleted() throws GitAPIException, IOException {
        File testFile = File.createTempFile("test1", ".org", remoteGitDir);
        remoteGit.add().addFilepattern(testFile.getName()).call();
        commitRemote();
        String gitRepoUri = getRepoUri();
        GitRepo gitRepo = new GitRepo(context, gitRepoUri);
        gitRepo.syncStart();
        List<VersionedRook> books = gitRepo.getBooks();
        assertEquals(1, books.size());

        remoteGit.branchCreate().setName(TEMPBRANCH_NAME).call();
        remoteGit.checkout().setName(TEMPBRANCH_NAME).call();

        gitRepo.delete(books.get(0).getUri());

        books = gitRepo.getBooks();
        assertEquals(0, books.size());
        remoteGit.checkout().setName("master").call();
        assertEquals(0, getRemoteFileCount());
    }

    private int getRemoteFileCount() {
        return remoteGitDir.list(new FilenameFilter() {
            @Override
            public boolean accept(File file, String s) {
                return !s.equals(".git");
            }
        }).length;
    }

    private void commitRemote() throws GitAPIException {
        remoteGit.commit().setMessage("test").call();
    }

    private String getRepoUri() {
        Uri gitrepoUri = new Uri.Builder()
                .scheme(GitRepo.SCHEME)
                .path(remoteGitDir.getAbsolutePath())
                .appendQueryParameter(GitRepo.PARAMETER_LOCAL_DIR, targetDir.getAbsolutePath())
                .appendQueryParameter(GitRepo.PARAMETER_TRANSPORT, GitRepo.Transport.FILE.name())
                .build();
        return gitrepoUri.toString();
    }

}