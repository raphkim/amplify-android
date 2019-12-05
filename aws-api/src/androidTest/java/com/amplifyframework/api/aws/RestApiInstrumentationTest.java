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

package com.amplifyframework.api.aws;

import android.util.Log;

import com.amplifyframework.api.rest.RestOptions;
import com.amplifyframework.api.rest.RestResponse;
import com.amplifyframework.core.Amplify;

import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.Callback;
import com.amazonaws.mobile.client.UserStateDetails;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Validates the functionality of the {@link AWSApiPlugin} for REST operations.
 */
public final class RestApiInstrumentationTest {

    /**
     * Configure the Amplify framework, if that hasn't already happened in this process instance.
     *
     * @throws Exception Exception is thrown if configuration fails.
     */
    @BeforeClass
    public static void onceBeforeTests() throws Exception {
        AmplifyTestConfigurator.configureIfNotConfigured();

        final CountDownLatch latch = new CountDownLatch(1);
        AWSMobileClient.getInstance().initialize(getApplicationContext(), new Callback<UserStateDetails>() {
            @Override
            public void onResult(UserStateDetails userStateDetails) {
                Log.i("INIT", "onResult: " + userStateDetails.getUserState());
                latch.countDown();
            }

            @Override
            @SuppressWarnings("ParameterName")
            public void onError(Exception e) {
                Log.e("INIT", "Initialization error.", e);
                latch.countDown();
            }
        });
        latch.await();
    }

    /**
     * Test whether we can make api Rest call in none auth.
     *
     * @throws JSONException Exception is thrown if JSON parsing fails.
     */
    @Test
    public void getRequestWithNoAuth() throws JSONException {
        final RestOptions options = new RestOptions("simplesuccess");
        LatchedRestResponseListener responseListener = new LatchedRestResponseListener();
        Amplify.API.get("nonAuthApi", options, responseListener);
        RestResponse response = responseListener.awaitTerminalEvent().awaitSuccessResponse();
        assertTrue("Should return a non null data", response.getData() != null);

        final JSONObject resultJSON = response.getData().asJSONObject();
        final JSONObject contextJSON = resultJSON.getJSONObject("context");
        assertNotNull("Should contain an object called context", contextJSON);
        assertEquals(
                "Should return the right value",
                "GET",
                contextJSON.getString("http-method"));
        assertEquals(
                "Should return the right value",
                "/simplesuccess",
                contextJSON.getString("resource-path"));
    }

    /**
     * Test whether we can make POST api Rest call in none auth.
     *
     * @throws JSONException Exception is thrown if JSON parsing fails.
     */
    @Test
    public void postRequestWithNoAuth() throws JSONException {
        final RestOptions options = new RestOptions("simplesuccess", "sample body".getBytes());
        LatchedRestResponseListener responseListener = new LatchedRestResponseListener();
        Amplify.API.post("nonAuthApi", options, responseListener);
        RestResponse response = responseListener.awaitTerminalEvent().awaitSuccessResponse();
        assertTrue("Should return a non null data", response.getData() != null);

        final JSONObject resultJSON = response.getData().asJSONObject();
        final JSONObject contextJSON = resultJSON.getJSONObject("context");
        assertNotNull("Should contain an object called context", contextJSON);
        assertEquals(
                "Should return the right value",
                "POST",
                contextJSON.getString("http-method"));
        assertEquals(
                "Should return the right value",
                "/simplesuccess",
                contextJSON.getString("resource-path"));
    }

    /**
     * Test whether we can make api Rest call in api key as auth type.
     *
     * @throws JSONException Exception is thrown if JSON parsing fails.
     */
    @Test
    public void getRequestWithApiKey() throws JSONException {
        final RestOptions options = new RestOptions("simplesuccessapikey");
        LatchedRestResponseListener responseListener = new LatchedRestResponseListener();
        Amplify.API.get("apiKeyApi", options, responseListener);
        RestResponse response = responseListener.awaitTerminalEvent().awaitSuccessResponse();
        assertTrue("Should return a non null data", response.getData() != null);

        final JSONObject resultJSON = response.getData().asJSONObject();
        final JSONObject contextJSON = resultJSON.getJSONObject("context");
        assertNotNull("Should contain an object called context", contextJSON);
        assertEquals(
                "Should return the right value",
                "GET",
                contextJSON.getString("http-method"));
        assertEquals(
                "Should return the right value",
                "/simplesuccessapikey",
                contextJSON.getString("resource-path"));
    }

    /**
     * Test whether we can make api Rest call in IAM as auth type.
     */
    @Test
    public void getRequestWithIAM() {
        final RestOptions options = RestOptions.builder().addPath("items").build();
        LatchedRestResponseListener responseListener = new LatchedRestResponseListener();
        Amplify.API.get("iamAPI", options, responseListener);
        RestResponse response = responseListener.awaitTerminalEvent().awaitSuccessResponse();
        assertTrue("Should return a non null data", response.getData() != null);
    }

    /**
     * Test whether we can get failed response for access denied.
     */
    @Test
    public void getRequestWithIAMFailedAccess() {
        final RestOptions options = RestOptions.builder().addPath("invalidPath").build();
        LatchedRestResponseListener responseListener = new LatchedRestResponseListener();
        Amplify.API.get("iamAPI", options, responseListener);
        RestResponse response = responseListener.awaitTerminalEvent().awaitErrors();
        assertTrue("Should return a non null data", response.getData() != null);
        assertFalse("Response should be unsuccessful", response.getCode().isSucessful());
    }
}
