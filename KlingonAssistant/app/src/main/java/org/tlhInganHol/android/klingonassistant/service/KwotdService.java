/*
 * Copyright (C) 2017 De'vID jonpIn (David Yonge-Mallo)
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

package org.tlhInganHol.android.klingonassistant.service;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.util.Log;

public class KwotdService extends JobService {
    private static final String TAG = "KwotdService";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "KwotdService created");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "KwotdService destroyed");
    }

    @Override
    public boolean onStartJob(final JobParameters params) {
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // TODO: Do stuff.

                // Release the wake lock.
                jobFinished(params, false);
            }
        }, 1000);
        Log.i(TAG, "on start job: " + params.getJobId());

        // Return true to hold the wake lock.
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        Log.i(TAG, "on stop job: " + params.getJobId());

        // Return false to drop the job.
        return false;
    }
}
