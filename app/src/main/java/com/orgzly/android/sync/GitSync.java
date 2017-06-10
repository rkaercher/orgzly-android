package com.orgzly.android.sync;

import android.support.annotation.NonNull;
import android.util.Log;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.orgzly.android.repos.GitUri;

import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LsRemoteCommand;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.TransportConfigCallback;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.errors.UnsupportedCredentialItem;
import org.eclipse.jgit.lib.TextProgressMonitor;
import org.eclipse.jgit.transport.CredentialItem;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.transport.Transport;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.util.FS;

import java.io.File;
import java.io.StringWriter;

public class GitSync {
    private final GitUri uri;

    private String TAG = GitSync.class.getName();

    public class GitResult {
        private final boolean success;
        private final String errorMessage;

        public GitResult(boolean success, String errorMessage) {
            this.success = success;
            this.errorMessage = errorMessage;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    public GitSync(GitUri uriRepresentation) {
        this.uri = uriRepresentation;
    }


    public GitResult isRepoReadable() {
        GitResult result = null;
        final LsRemoteCommand lsCmd = new LsRemoteCommand(null);
        lsCmd.setRemote(uri.getGitRemoteUriWithScheme());
        lsCmd.setCredentialsProvider(getCredentialsProvider());
        if (isSshTransport()) {
            lsCmd.setTransportConfigCallback(getTransportConfigCallback());
        }
        try {
            String msg = lsCmd.call().toString();
            Log.d("LSGIT", msg);
            result = new GitResult(true, null);
        } catch ( Exception e) {
            result = new GitResult(false, e.getMessage());
        }
        return result;
    }

    public boolean pull() {
        CredentialsProvider allowHosts = getCredentialsProvider();
        File localRepoDir = new File(uri.getLocalRepoDir());
        cloneIfEmpty(allowHosts, localRepoDir);

        try {
            Log.d(TAG, "Pulling repo");
            PullCommand pullCommand = Git.open(localRepoDir).pull();
            pullCommand.setCredentialsProvider(allowHosts);
            if (isSshTransport()) {
                pullCommand.setTransportConfigCallback(getTransportConfigCallback());
            }
            PullResult pullResult = pullCommand.call();
            if (pullResult.isSuccessful()) {
                Log.d(TAG, "Pull successful.");
                // TODO: 07.04.17 push
            } else {
                Log.e(TAG, "Gitrepo pull was not successful.");
                // // TODO: 07.04.17 handle problems
            }
        } catch (Exception e) {
            Log.e(TAG, "Problem during git pull.", e);
        }

        return true;
    }

    public boolean push() {
        CredentialsProvider allowHosts = getCredentialsProvider();

        try {
            Log.d(TAG, "Pushing repo");
            Git git = Git.open(new File(uri.getLocalRepoDir()));
            git.add().addFilepattern(".").call();

            git.commit().setAll(true).setMessage("Changes from Orgzly").call();

            PushCommand pushCommand = git.push().setPushAll().setRemote("origin");

            pushCommand.setCredentialsProvider(allowHosts);
            if (isSshTransport()) {
                pushCommand.setTransportConfigCallback(getTransportConfigCallback());
            }
            Iterable<PushResult> pushResult = pushCommand.call();
            for (PushResult result : pushResult) {
                for (RemoteRefUpdate update : result.getRemoteUpdates()) {
                    Log.d(TAG, update.toString());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return true;
    }

    private boolean isSshTransport() {
        switch (uri.getTransport()) {
            case SSH_KEYPAIR:
            case SSH_PASSWORD:
                return true;

        }
        return false;
    }


    private void cloneIfEmpty(CredentialsProvider allowHosts, File localPath) {
        if (localPath.listFiles().length == 0) {
            Log.d(TAG, "Local repo dir is empty, cloning repo");
            cloneRepo(allowHosts, localPath);
        }
    }

    private void cloneRepo(CredentialsProvider allowHosts, File localPath) {
        System.out.println("Cloning from " + uri.getGitRemoteUriWithScheme() + " to " + localPath);
        Git result = null;
        try {
            StringWriter stringWriter = new StringWriter();
            CloneCommand cmd = Git.cloneRepository()
                    .setURI(uri.getGitRemoteUriWithScheme())
                    .setDirectory(localPath)
                    .setCredentialsProvider(allowHosts);
            if (isSshTransport()) {
                cmd.setTransportConfigCallback(getTransportConfigCallback());
            }
            cmd.setProgressMonitor(new TextProgressMonitor(stringWriter)); // TODO: 12.04.17
            result = cmd.call();

//            String branchName = "master";
//            Ref ref = result.checkout().
//                    setCreateBranch(true).
//                    setName("branchName").
//                    setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK).
//                    setStartPoint("origin/" + branchName).
//                    call();

            // Note: the call() returns an opened repository already which needs to be closed to avoid file handle leaks!
            System.out.println("Having repository: " + result.getRepository().getDirectory());
        } catch (InvalidRemoteException e) {
            e.printStackTrace();
        } catch (TransportException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (result != null)
                result.close();
        }
    }

    @NonNull
    private TransportConfigCallback getTransportConfigCallback() {
        final SshSessionFactory sshSessionFactory = createJshSessionFactory();

        return new TransportConfigCallback() {
            @Override
            public void configure(Transport transport) {
                SshTransport sshTransport = (SshTransport) transport;
                sshTransport.setSshSessionFactory(sshSessionFactory);

            }

        };
    }

    @NonNull
    private CredentialsProvider getCredentialsProvider() {
        return new CredentialsProvider() {

            @Override
            public boolean supports(CredentialItem... items) {
                for (CredentialItem item : items) {
                    if ((item instanceof CredentialItem.YesNoType)) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public boolean get(URIish uri, CredentialItem... items) throws UnsupportedCredentialItem {
                for (CredentialItem item : items) {
                    if (item instanceof CredentialItem.YesNoType) {
                        ((CredentialItem.YesNoType) item).setValue(true);
                        return true;
                    }
                }
                return false;
            }

            @Override
            public boolean isInteractive() {
                return false;
            }
        };
    }

    @NonNull
    private JschConfigSessionFactory createJshSessionFactory() {
        return new JschConfigSessionFactory() {
            @Override
            protected void configure(OpenSshConfig.Host host, Session session) {
                // do nothing
            }

            @Override
            protected JSch createDefaultJSch(FS fs) throws JSchException {
                JSch defaultJSch = super.createDefaultJSch(fs);
                String pubKey = "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAAAgQC3j9w/Bprf9TYwMFdgoQonYCFl5BFB64OIXyGaoXH+z3aZI6E7gSBYdAXRGZZMX0MGP5iCiTkWSxI5oQBMsDzXNo3ryORXEm9W7yBqknc2DDz9CnJPQ4Vns/BDal2BCZbiFI4Vv9vn2Zk4NqwbCmP2mQSXbxomaad8ykV4rRgjDw==";
                String privKey = "-----BEGIN RSA PRIVATE KEY-----\n" +
                        "MIICXQIBAAKBgQC3j9w/Bprf9TYwMFdgoQonYCFl5BFB64OIXyGaoXH+z3aZI6E7\n" +
                        "gSBYdAXRGZZMX0MGP5iCiTkWSxI5oQBMsDzXNo3ryORXEm9W7yBqknc2DDz9CnJP\n" +
                        "Q4Vns/BDal2BCZbiFI4Vv9vn2Zk4NqwbCmP2mQSXbxomaad8ykV4rRgjDwIDAQAB\n" +
                        "AoGABf8oJdL0ak6bd5WMtTZtm32zeZ2qxqmIAYOCGIgfrkBe0n/se4IraVhY/EzG\n" +
                        "sTYHfYPEWrda6xSGwuU0mkzDoAIY0djeM6XhxFkAeItMOZfZ2utTw6iPIw8KZm81\n" +
                        "xOVmYXoqbJNTarGxK3I0bDyPxMHgoAn1i+RnA3+i2h1l6VkCQQDcbtoWjU27t1O1\n" +
                        "/SXvIuysqUS3NJJFf7qPhiEEx+0V43owAe6e+jurVJuqU2JVcJOPOXejy14GPjJZ\n" +
                        "UhITUMgtAkEA1S4FqEpM4s45p8MRfVVgM8K9Fg8biKDsnOL2j6YQ4fim10l2rC6N\n" +
                        "y/9dIZYCH/csy7DUJ+ecgxvSMgPPyutBqwJBAKt4f6WzXZh8T13uAig3nqvhMFbj\n" +
                        "SjVN2q3yxJSXgNHaFh5qIlkAhpIMStr/6ipUXDS5m1uKwyVQFJJuySjFWjECQBBy\n" +
                        "PH1/Pe9BebE3m2HP9FwJ6gyJndYslBSGbf8nEKZeSIDTahRegxH54XV13TQaHZqZ\n" +
                        "ScTKWYjD3LN/F8jP3YECQQDRhQ326ZKtMfYRLxYtV7n+euR7Je+PiYmbAD6/h7g2\n" +
                        "/S1WfYS12kPmxqpECxwNVY8ehpsZL2HZOdZVR364fnEG\n" +
                        "-----END RSA PRIVATE KEY-----\n";

                defaultJSch.addIdentity("me", privKey.getBytes(), pubKey.getBytes(), null);
                return defaultJSch;
            }
        };
    }


}
