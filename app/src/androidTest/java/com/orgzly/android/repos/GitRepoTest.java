package com.orgzly.android.repos;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.test.espresso.core.deps.guava.io.Files;

import com.orgzly.android.OrgzlyTest;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


public class GitRepoTest extends OrgzlyTest {
    private static final String TEMPBRANCH_NAME = "tempbranch";
    private Git remoteGit;
    private File remoteGitDir;
    private File targetDir;

    @Before
    public void before() throws GitAPIException {
        remoteGitDir = Files.createTempDir();
        remoteGit = Git.init()
                .setDirectory(remoteGitDir)
                .call();
        remoteGit.commit().setMessage("init").call();
        targetDir = Files.createTempDir();
        remoteGit.branchCreate().setName(TEMPBRANCH_NAME).call();
    }

    @Test
    public void thatSyncStartClones() throws GitAPIException, IOException {
        addRemoteRepoFile(null);
        String gitRepoUri = getRepoUri();
        GitRepo gitRepo = new GitRepo(context, gitRepoUri);
        List<VersionedRook> books = gitRepo.getBooks();
        assertEquals(0, books.size());

        gitRepo.syncStart();

        books = gitRepo.getBooks();
        assertEquals(1, books.size());
    }

    private File addRemoteRepoFile(String content) throws IOException, GitAPIException {
        File testFile = File.createTempFile("test", ".org", remoteGitDir);
        if (null != content) {
            Files.write(content.getBytes(), testFile);
        }
        remoteGit.add().addFilepattern(testFile.getName()).call();
        commitRemote();
        return testFile;
    }

    @Test
    public void thatAFileCanBeDeleted() throws GitAPIException, IOException {
        addRemoteRepoFile(null);
        GitRepo sut = syncRepoWithRemote();
        List<VersionedRook> books = sut.getBooks();
        assertEquals(1, books.size());

        checkoutTempBranch();

        sut.delete(books.get(0).getUri());

        books = sut.getBooks();
        assertEquals(0, books.size());
        checkoutMasterBranch();
        assertEquals(0, getRemoteFileCount(null));
    }

    private Ref checkoutMasterBranch() throws GitAPIException {
        return remoteGit.checkout().setName("master").call();
    }

    private Ref checkoutTempBranch() throws GitAPIException {
        return remoteGit.checkout().setName(TEMPBRANCH_NAME).call();
    }

    @NonNull
    private GitRepo syncRepoWithRemote() {
        String gitRepoUri = getRepoUri();
        GitRepo gitRepo = new GitRepo(context, gitRepoUri);
        gitRepo.syncStart();
        return gitRepo;
    }

    @Test
    public void thatAFileCanBeRetrieved() throws IOException, GitAPIException {
        String content1 = "Testfile1 contents";
        File remoteFile1 = addRemoteRepoFile(content1);
        String content2 = "Testfile2 contents";
        File remoteFile2 = addRemoteRepoFile(content2);

        GitRepo sut = syncRepoWithRemote();
        List<VersionedRook> books = sut.getBooks();
        assertEquals(2, books.size());

        File destDir = Files.createTempDir();

        File destFile1 = new File(destDir, "file1");
        VersionedRook rook1 = sut.retrieveBook(getRepoUriForRemoteFile(books, remoteFile1), destFile1);
        assertEquals(1, destDir.list().length);
        assertNotNull(rook1);

        File destFile2 = new File(destDir, "file2");
        VersionedRook rook2 = sut.retrieveBook(getRepoUriForRemoteFile(books, remoteFile2), destFile2);
        assertEquals(2, destDir.list().length);
        assertNotNull(rook2);

        String retrievedContent1 = Files.toString(destFile1, Charset.defaultCharset());
        assertEquals(content1, retrievedContent1);

        String retrievedContent2 = Files.toString(destFile2, Charset.defaultCharset());
        assertEquals(content2, retrievedContent2);
    }

    @Test
    public void thatANewBookCanBeStored() throws IOException, GitAPIException {
        String fileContents = "Testfile contents for new Book";
        GitRepo sut = syncRepoWithRemote();
        List<VersionedRook> books = sut.getBooks();
        assertEquals(0, books.size());

        checkoutTempBranch();

        File newFile = File.createTempFile("newbook", "org");
        Files.write(fileContents.getBytes(), newFile);
        VersionedRook aNewBook = sut.storeBook(newFile, "ANewBook");
        assertNotNull(aNewBook);

        checkoutMasterBranch();
        assertEquals(1,getRemoteFileCount(null));

    }


    @Test
    public void thatABookCanBeRenamed() throws IOException, GitAPIException {
        addRemoteRepoFile(null);
        GitRepo sut = syncRepoWithRemote();
        List<VersionedRook> books = sut.getBooks();
        assertEquals(1, books.size());

        checkoutTempBranch();

        String newName = "theNewName.org";
        sut.renameBook(books.get(0).getUri(), Files.getNameWithoutExtension(newName));

        checkoutMasterBranch();
        assertEquals(1,getRemoteFileCount(null));
        assertEquals(1,getRemoteFileCount(newName));
    }

    private Uri getRepoUriForRemoteFile(List<VersionedRook> books, File remoteFile) {
        for (VersionedRook book : books) {
            String filePath = book.getUri().getQueryParameter(GitUri.PARAMETER_FILEPATH);
            String fileName = new File(filePath).getName();
            if (remoteFile.getName().equals(fileName)) {
                return book.getUri();
            }
        }
        return null;
    }

    private int getRemoteFileCount(final String findFileName) {
        return remoteGitDir.list(new FilenameFilter() {
            @Override
            public boolean accept(File file, String s) {
                return !s.equals(".git") && (findFileName == null || s.equals(findFileName));
            }
        }).length;
    }

    private void commitRemote() throws GitAPIException {
        remoteGit.commit().setMessage("test").call();
    }

    private String getRepoUri() {
        Uri gitRepoUri = new Uri.Builder()
                .scheme(GitUri.SCHEME)
                .path(remoteGitDir.getAbsolutePath())
                .appendQueryParameter(GitUri.PARAMETER_LOCAL_DIR, targetDir.getAbsolutePath())
                .appendQueryParameter(GitUri.PARAMETER_TRANSPORT, GitUri.Transport.FILE.name())
                .build();
        return gitRepoUri.toString();
    }

}