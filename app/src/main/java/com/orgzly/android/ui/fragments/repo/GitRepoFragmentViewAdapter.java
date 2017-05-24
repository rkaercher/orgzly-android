package com.orgzly.android.ui.fragments.repo;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.orgzly.R;


class GitRepoFragmentViewAdapter extends PagerAdapter {
    public static final int MAIN_VIEW = 0;
    public static final int KEYS_VIEW = 1;

    private Context context;
    private InstantiationListener instantiationListener;

    public GitRepoFragmentViewAdapter(Context context, InstantiationListener instantiationListener) {
        this.context = context;
        this.instantiationListener = instantiationListener;
    }

    @Override
    public int getCount() {
        return 2;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        LayoutInflater inflater = LayoutInflater.from(context);
        int resource = R.layout.fragment_repo_git_main;
        switch (position) {
            case MAIN_VIEW:
                resource = R.layout.fragment_repo_git_main;
                break;
            case KEYS_VIEW:
                resource = R.layout.fragment_repo_git_keys;
        }
        View view = inflater.inflate(resource, null);
        container.addView(view);
        instantiationListener.onItemInstantiation(position, view);

        return view;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
            case MAIN_VIEW:
                return "Repository Data";
            case KEYS_VIEW:
                return "Authentication";
        }
        return null;
    }

    public interface InstantiationListener {
        void onItemInstantiation(int viewId, View view);
    }
}
