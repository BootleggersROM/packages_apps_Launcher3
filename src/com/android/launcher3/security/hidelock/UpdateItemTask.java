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

import android.os.AsyncTask;
import androidx.annotation.NonNull;

import com.android.launcher3.security.hidelock.db.HideLockComponent;
import com.android.launcher3.security.hidelock.db.HideLockDatabaseHelper;

public class UpdateItemTask extends AsyncTask<HideLockComponent, Void, Boolean> {
    @NonNull
    private HideLockDatabaseHelper mDbHelper;
    @NonNull
    private UpdateCallback mCallback;
    @NonNull
    private HideLockComponent.Kind mKind;

    UpdateItemTask(@NonNull HideLockDatabaseHelper dbHelper,
                   @NonNull UpdateCallback callback,
                   @NonNull HideLockComponent.Kind kind) {
        mDbHelper = dbHelper;
        mCallback = callback;
        mKind = kind;
    }

    @Override
    protected Boolean doInBackground(HideLockComponent... hlComponents) {
        if (hlComponents.length < 1) {
            return false;
        }

        HideLockComponent component = hlComponents[0];
        String pkgName = component.getPackageName();

        switch (mKind) {
            case HIDDEN:
                if (component.isHidden()) {
                    mDbHelper.addHiddenApp(pkgName);
                } else {
                    mDbHelper.removeHiddenApp(pkgName);
                }
                break;
            case PROTECTED:
                if (component.isProtected()) {
                    mDbHelper.addProtectedApp(pkgName);
                } else {
                    mDbHelper.removeProtectedApp(pkgName);
                }
                break;
        }
        return true;
    }

    @Override
    protected void onPostExecute(Boolean result) {
        mCallback.onUpdated(result);
    }

    interface UpdateCallback {
        void onUpdated(boolean result);
    }
}
