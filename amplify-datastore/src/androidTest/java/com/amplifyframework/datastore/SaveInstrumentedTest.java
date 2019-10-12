/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.amplifyframework.datastore;

import androidx.test.core.app.ApplicationProvider;

import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.async.Listener;
import com.amplifyframework.core.async.Result;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * Instrumentation test for {@link AmplifyDataStorePlugin#save(DataStoreObjectModel, Listener)}.
 * These tests run on an Android device. The Android application context is provided by the
 * {@link ApplicationProvider}.
 */
public final class SaveInstrumentedTest {

    private static AmplifyDataStorePlugin amplifyDataStorePlugin;

    @BeforeClass
    public static void setUpBeforeClass() {
        amplifyDataStorePlugin = new AmplifyDataStorePlugin(ApplicationProvider.getApplicationContext());
        Amplify.addPlugin(amplifyDataStorePlugin);
        Amplify.configure(ApplicationProvider.getApplicationContext());
    }

    @AfterClass
    public static void tearDownAfterClass() {
        amplifyDataStorePlugin.tearDown();
    }

    @Test
    public void saveWritesDataToDisk() throws InterruptedException {
        final CountDownLatch waitUntilDiskWriteIsComplete = new CountDownLatch(1);
        Person person = new Person("Azlan", "Ziawa");

        amplifyDataStorePlugin.save(person, new Listener<Result>() {
            @Override
            public void onResult(Result result) {
                waitUntilDiskWriteIsComplete.countDown();
            }

            @Override
            public void onError(Exception error) {
                fail(error.getLocalizedMessage());
            }
        });

        assertTrue("Expecting the disk write to succeed",
                waitUntilDiskWriteIsComplete.await(100, TimeUnit.MINUTES));
    }
}
