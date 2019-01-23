/*
 * Copyright (C) 2019 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.launcher3.security.hidelock;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.launcher3.LauncherAppState;
import com.android.launcher3.R;
import com.android.launcher3.Utilities;
import com.android.launcher3.security.hidelock.db.HideLockComponent;
import com.android.launcher3.security.hidelock.db.HideLockDatabaseHelper;

import java.util.List;

import static com.android.launcher3.security.hidelock.db.HideLockComponent.Kind.HIDDEN;
import static com.android.launcher3.security.hidelock.db.HideLockComponent.Kind.PROTECTED;

public class HideLockAppsActivity extends Activity implements
        HideLockAppsAdapter.Listener,
        LoadHideLockComponentsTask.Callback,
        UpdateItemTask.UpdateCallback {

    private static final int REQUEST_AUTH_CODE = 92;
    private static final String KEY_HIDELOCK_ONBOARDING = "pref_hidelock_onboarding";

    private RecyclerView mRecyclerView;
    private LinearLayout mLoadingView;
    private ProgressBar mProgressBar;

    private HideLockDatabaseHelper mDbHelper;
    private HideLockAppsAdapter mAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstance) {
        super.onCreate(savedInstance);

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        setContentView(R.layout.activity_hidden_apps);
        mRecyclerView = findViewById(R.id.hidden_apps_list);
        mLoadingView = findViewById(R.id.hidden_apps_loading);
        mProgressBar = findViewById(R.id.hidden_apps_progress_bar);

        mAdapter = new HideLockAppsAdapter(this);
        mDbHelper = HideLockDatabaseHelper.getInstance(this);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setAdapter(mAdapter);

        authenticate();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_AUTH_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                showUi();
            } else {
                finish();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_hidelock_apps, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        } else if (id == R.id.menu_hidelock_help) {
            showOnBoarding(true);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onHiddenItemChanged(@NonNull HideLockComponent component) {
        new UpdateItemTask(mDbHelper, this, HIDDEN).execute(component);
    }

    @Override
    public void onProtectedItemChanged(@NonNull HideLockComponent component) {
        new UpdateItemTask(mDbHelper, this, PROTECTED).execute(component);
    }

    @Override
    public void onUpdated(boolean result) {
        LauncherAppState state = LauncherAppState.getInstanceNoCreate();
        if (state != null) {
            state.getModel().forceReload();
        }
    }

    @Override
    public void onLoadListProgress(int progress) {
        mProgressBar.setProgress(progress);
    }

    @Override
    public void onLoadCompleted(List<HideLockComponent> result) {
        mLoadingView.setVisibility(View.GONE);
        mRecyclerView.setVisibility(View.VISIBLE);
        mAdapter.update(result);
    }

    private void authenticate() {
        KeyguardManager manager = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ?
                getSystemService(KeyguardManager.class) :
                (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        if (manager == null) {
            throw new NullPointerException("No KeyguardManager found!");
        }

        String title = getString(R.string.hidelock_apps_manager_name);
        String message = getString(R.string.hidelock_apps_auth_manager);
        Intent intent = manager.createConfirmDeviceCredentialIntent(title, message);

        if (intent != null) {
            startActivityForResult(intent, REQUEST_AUTH_CODE);
            return;
        }

        Toast.makeText(this, R.string.hidelock_apps_no_lock_error,
                Toast.LENGTH_LONG).show();
        finish();
    }

    private void showUi() {
        mLoadingView.setVisibility(View.VISIBLE);

        showOnBoarding(false);

        new LoadHideLockComponentsTask(mDbHelper, getPackageManager(), this).execute();
    }

    private void showOnBoarding(boolean forceShow) {
        SharedPreferences preferenceManager = Utilities.getPrefs(this);
        if (!forceShow && preferenceManager.getBoolean(KEY_HIDELOCK_ONBOARDING, false)) {
            return;
        }

        preferenceManager.edit()
                .putBoolean(KEY_HIDELOCK_ONBOARDING, true)
                .apply();

        new AlertDialog.Builder(this)
                .setView(R.layout.dialog_hidelock_welcome)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }
}
