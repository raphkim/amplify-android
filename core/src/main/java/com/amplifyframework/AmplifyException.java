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

package com.amplifyframework;

import androidx.annotation.NonNull;

/**
 * Top-level exception in the Amplify framework. All other Amplify exceptions should extend this.
 */
public class AmplifyException extends Exception {
    /**
     * All Amplify Exceptions should have a recovery suggestion. This string can be used as a filler until one is
     * defined but should ultimately be replaced as all good todos.
     */
    public static final String TODO_RECOVERY_SUGGESTION = "Sorry, we don't have a suggested fix for this error yet.";
    private static final long serialVersionUID = 1L;

    private final String recoverySuggestion;

    /**
     * Creates a new exception with a message, root cause, and recovery suggestion.
     * @param message An error message describing why this exception was thrown
     * @param throwable The underlying cause of this exception
     * @param recoverySuggestion Text suggesting a way to recover from the error being described
     */
    public AmplifyException(
            @NonNull final String message,
            final Throwable throwable,
            @NonNull final String recoverySuggestion
    ) {
        super(message, throwable);
        this.recoverySuggestion = recoverySuggestion;
    }

    /**
     * Constructs a new exception using a provided message and an associated error.
     * @param message Explains the reason for the exception
     * @param recoverySuggestion Text suggesting a way to recover from the error being described
     */
    public AmplifyException(final String message, final String recoverySuggestion) {
        this(message, null, recoverySuggestion);
    }

    /**
     * Gets the recovery suggestion message.
     * @return customized recovery suggestion message
     */
    public final String getRecoverySuggestion() {
        return recoverySuggestion;
    }
}
