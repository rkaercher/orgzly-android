package com.orgzly.android.ui.fragments.repo;

import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.TextInputLayout;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.orgzly.BuildConfig;
import com.orgzly.R;
import com.orgzly.android.provider.clients.ReposClient;
import com.orgzly.android.repos.GitUri;
import com.orgzly.android.repos.Repo;
import com.orgzly.android.repos.RepoFactory;
import com.orgzly.android.sync.GitSync;
import com.orgzly.android.ui.CommonActivity;
import com.orgzly.android.ui.util.ActivityUtils;
import com.orgzly.android.util.AppPermissions;
import com.orgzly.android.util.LogUtils;
import com.orgzly.android.util.SshUtil;


public class GitRepoFragment extends RepoFragment implements RepoFragmentWithFileUri, AdapterView.OnItemSelectedListener {
    private static final String TAG = GitRepoFragment.class.getName();

    private static final String ARG_REPO_ID = "repo_id";

    /**
     * Name used for {@link android.app.FragmentManager}.
     */
    public static final String FRAGMENT_TAG = GitRepoFragment.class.getName();
    private static final int GIT_AUTH_TYPE_SSH_KEYS = 0;
    private static final int GIT_AUTH_TYPE_USER_PASSWORD = 1;
    private String mLocalPath;
    private GitUri mRemoteUri;

    private RepoFragmentListener mListener;

    private TextInputLayout directoryInputLayout;
    private TextView localDirectoryText;
    private EditText remoteUriText;

    private Button copyToClipboardButton;
    private Spinner authMethodSpinner;
    private View publicKeyControls;
    private View passwordControls;
    private View testLoginButton;
    private View testLoginIcon;

    public static GitRepoFragment getInstance() {
        return new GitRepoFragment();
    }

    public static GitRepoFragment getInstance(long repoId) {
        GitRepoFragment fragment = new GitRepoFragment();
        Bundle args = new Bundle();

        args.putLong(ARG_REPO_ID, repoId);

        fragment.setArguments(args);

        return fragment;
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public GitRepoFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* Would like to add items to the Options Menu.
         * Required (for fragments only) to receive onCreateOptionsMenu() call.
         */
        setHasOptionsMenu(true);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_repo_git, container, false);

        // Not working when done in XML
//        localDirectoryText.setHorizontallyScrolling(false);
//        localDirectoryText.setMaxLines(3);
//
//        localDirectoryText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
//            @Override
//            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
//                save();
//                return true;
//            }
//        });
        mRemoteUri = new GitUri();
        remoteUriText = (EditText) view.findViewById(R.id.fragment_repo_git_remote_uri);
        remoteUriText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                mRemoteUri.setGitRemoteUri(editable.toString());
                setUIStates();
            }
        });

        setFromArgument(); // TODO find out when this should not be called

//        if (savedInstanceState == null && TextUtils.isEmpty(localDirectoryText.getText()) && mLocalPath == null) {
//            setFromArgument();
//        }

        //   directoryInputLayout = (TextInputLayout) view.findViewById(R.id.fragment_repo_git_local_directory_input_layout);

        localDirectoryText = (TextView) view.findViewById(R.id.fragment_repo_git_local_directory);
        //       MiscUtils.clearErrorOnTextChange(localDirectoryText, directoryInputLayout);
        authMethodSpinner = (Spinner) view.findViewById(R.id.git_auth_method_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getContext(), R.array.git_repo_auth_types, R.layout.support_simple_spinner_dropdown_item);
        authMethodSpinner.setAdapter(adapter);
        authMethodSpinner.setOnItemSelectedListener(this);

        publicKeyControls = view.findViewById(R.id.git_auth_public_key_controls);
        passwordControls = view.findViewById(R.id.fragment_repo_git_password_controls);

        testLoginIcon = view.findViewById(R.id.fragment_repo_git_test_icon);

        setupDirectoryBrowser(view);

        setupGenerateKeysButton(view);
        setupCopyClipboardButton(view);
        setupGTestLoginButton(view);

        setUIStates();

        return view;
    }

    private void setFromArgument() {
        if (getArguments() != null && getArguments().containsKey(ARG_REPO_ID)) {
            long repoId = getArguments().getLong(ARG_REPO_ID);


            Uri parsed = Uri.parse(ReposClient.getUrl(getActivity(), repoId));
            mRemoteUri = new GitUri(parsed);
            mLocalPath = parsed.getQueryParameter(GitUri.PARAMETER_LOCAL_DIR);

        }
    }

    /**
     * Delay opening the browser.
     * Buttons would briefly appear in the middle of the screen
     * because of the opened keyboard.
     */
    private void startBrowserDelayed() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                startBrowser();
            }
        }, 100);
    }

    private void startBrowser() {
        String uri = null;

        if (!TextUtils.isEmpty(localDirectoryText.getText())) {
            uri = localDirectoryText.getText().toString();
        }

        if (uri != null) {
            mListener.onBrowseDirectories(Uri.parse(uri).getPath(), "PATH");
        } else {
            mListener.onBrowseDirectories(null, "PATH");
        }
    }

    @Override
    public void onResume() {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG);
        super.onResume();

        /* Set directory view's value. */
        if (mLocalPath != null) {
            localDirectoryText.setText(mLocalPath);
            mLocalPath = null;
        }

        /* Check for permissions. */
        AppPermissions.isGrantedOrRequest((CommonActivity) getActivity(), AppPermissions.FOR_LOCAL_REPO);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        /* This makes sure that the container activity has implemented
         * the callback interface. If not, it throws an exception
         */
        try {
            mListener = (RepoFragmentListener) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString() + " must implement " + RepoFragmentListener.class);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();

        mListener = null;
    }

    /**
     * Callback for options menu.
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, menu, inflater);

        inflater.inflate(R.menu.done_or_close, menu);

        /* Remove search item. */
        // menu.removeItem(R.id.options_menu_item_search);
    }

    /**
     * Callback for options menu.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.close:
                if (mListener != null) {
                    mListener.onRepoCancelRequest();
                }
                return true;

            case R.id.done:
                save();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void save() {
        /* Check for storage permission. */
        if (!AppPermissions.isGrantedOrRequest((CommonActivity) getActivity(), AppPermissions.FOR_LOCAL_REPO)) {
            return;
        }

        String uriString = remoteUriText.getText().toString().trim();
        String localDirString = localDirectoryText.getText().toString().trim();

        if (TextUtils.isEmpty(uriString)) {
            directoryInputLayout.setError(getString(R.string.can_not_be_empty));
            return;
        } else {
            directoryInputLayout.setError(null);
        }

        Uri uri = mRemoteUri.getUri();

        //   GitSync gitSync = new GitSync(Uri.parse("ssh://git@192.168.1.56:223/syncorg/essential-org"));

        Repo repo = RepoFactory.getFromUri(getActivity(), uri);

        if (repo == null) {
            directoryInputLayout.setError(getString(R.string.invalid_repo_url, uri));
            return;
        }

        if (getArguments() != null && getArguments().containsKey(ARG_REPO_ID)) { // Existing repo
            long repoId = getArguments().getLong(ARG_REPO_ID);

            if (mListener != null) {
                mListener.onRepoUpdateRequest(repoId, repo);
            }

        } else {
            if (mListener != null) {
                mListener.onRepoCreateRequest(repo);
            }
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        getActivity().getMenuInflater().inflate(R.menu.repos_context, menu);
    }

    @Override
    public void updateFileUri(Uri uri, String TAG) {
        this.localDirectoryText.setText(uri.getPath());
        this.mLocalPath = uri.getPath();
        this.mRemoteUri.setLocalRepoDir(uri.getPath());

    }

    private void setupGenerateKeysButton(View view) {
        view.findViewById(R.id.fragment_repo_git_generate_keys).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                AsyncTask<String, String, SshUtil.SshKeys> asyncTask = new AsyncTask<String, String, SshUtil.SshKeys>() {
                    @Override
                    protected SshUtil.SshKeys doInBackground(String... params) {
                        return SshUtil.generateKeys();
                    }

                    @Override
                    protected void onPostExecute(SshUtil.SshKeys sshKeys) {
                        mRemoteUri.setPrivateKey(sshKeys.getPrivateKey());
                        mRemoteUri.setPublicKey(sshKeys.getPublicKey());
                        copyPubkeyToClipboardWithNotify();
                        setUIStates();
                    }
                };
                asyncTask.execute();
            }
        });
    }

    private void copyPubkeyToClipboardWithNotify() {
        ClipboardManager cm = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clipData = ClipData.newPlainText("Orgzly Pubkey", mRemoteUri.getPublicKey());
        cm.setPrimaryClip(clipData);
        Toast.makeText(getContext(), "Public key copied to clipboard", Toast.LENGTH_SHORT).show();
    }

    private void setVisibleIfAuthmethodMatches(View view, int authType) {
        view.setVisibility(authMethodSpinner.getSelectedItemPosition() == authType ? View.VISIBLE : View.GONE);
    }

    private void setUIStates() {
        copyToClipboardButton.setEnabled(mRemoteUri.hasKeys());

        setVisibleIfAuthmethodMatches(publicKeyControls, GIT_AUTH_TYPE_SSH_KEYS);
        setVisibleIfAuthmethodMatches(passwordControls, GIT_AUTH_TYPE_USER_PASSWORD);

        testLoginButton.setEnabled(areLoginCredentialsComplete());
        testLoginIcon.setEnabled(areLoginCredentialsComplete());
    }

    private boolean areLoginCredentialsComplete() {
        if (!mRemoteUri.hasRemoteUri()) {
            return false;
        }
        switch (authMethodSpinner.getSelectedItemPosition()) {
            case GIT_AUTH_TYPE_SSH_KEYS:
                return mRemoteUri.hasKeys();
            case GIT_AUTH_TYPE_USER_PASSWORD:
                return mRemoteUri.hasPassword();
        }
        return false;
    }

    private void setupGTestLoginButton(View view) {
        testLoginButton = view.findViewById(R.id.fragment_repo_git_test_login);
        testLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                AsyncTask<String, String, GitSync.GitResult> asyncTask = new AsyncTask<String, String, GitSync.GitResult>() {
                    private ProgressDialog dlg;

                    @Override
                    protected void onPreExecute() {
                        dlg = new ProgressDialog(getContext());
                        dlg.setTitle("Checking Repo");
                        dlg.show();
                    }

                    @Override
                    protected GitSync.GitResult doInBackground(String... params) {
                        GitSync sync = new GitSync(mRemoteUri);
                        return sync.isRepoReadable();
                    }

                    @Override
                    protected void onPostExecute(GitSync.GitResult repoReadable) {
                        if (dlg.isShowing()) {
                            dlg.hide();
                        }
                        if (repoReadable.isSuccess()) {
                            Toast.makeText(getContext(), "Git repository valid!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getContext(), "Repository and/or credentials are not valid:\n" + repoReadable.getErrorMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                };
                asyncTask.execute();
            }
        });
    }

    private void setupCopyClipboardButton(View view) {
        copyToClipboardButton = (Button) view.findViewById(R.id.fragment_repo_git_copy_pubkey);
        copyToClipboardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                copyPubkeyToClipboardWithNotify();
            }
        });
    }

    private void setupDirectoryBrowser(View view) {
        view.findViewById(R.id.fragment_repo_git_directory_browse_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /* Close the keyboard before opening the browser. */
                if (getActivity() != null) {
                    ActivityUtils.closeSoftKeyboard(getActivity());
                }

                /* Do not open the browser unless we have the storage permission. */
                if (AppPermissions.isGrantedOrRequest((CommonActivity) getActivity(), AppPermissions.FOR_LOCAL_REPO)) {
                    startBrowserDelayed();
                }
            }
        });
    }

    // for auth type spinner
    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        setUIStates();
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {
    }
}
