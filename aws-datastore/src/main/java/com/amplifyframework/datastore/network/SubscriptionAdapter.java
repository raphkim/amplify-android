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

package com.amplifyframework.datastore.network;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.ApiCategoryBehavior;
import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.core.StreamListener;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.datastore.DataStoreException;

/**
 * Listens to subscription data from an {@link ApiCategoryBehavior}, with type String.class.
 * Is an {@link StreamListener} for that interface.
 * Converts responses from the {@link ApiCategoryBehavior} into the StreamListener type
 * needed for the {@link AppSyncEndpoint} contract.
 * Adapts between the two types of {@link StreamListener} by means of using a
 * {@link ResponseDeserializer}.
 * @param <T> Type of object being de-serialized from API subscription data
 */
final class SubscriptionAdapter<T extends Model> implements StreamListener<GraphQLResponse<String>> {
    private final StreamListener<GraphQLResponse<ModelWithMetadata<T>>> listener;
    private final Class<T> modelClass;
    private final ResponseDeserializer responseDeserializer;

    SubscriptionAdapter(
            StreamListener<GraphQLResponse<ModelWithMetadata<T>>> listener,
            Class<T> modelClass,
            ResponseDeserializer responseDeserializer) {
        this.listener = listener;
        this.modelClass = modelClass;
        this.responseDeserializer = responseDeserializer;
    }

    @Override
    public void onNext(GraphQLResponse<String> item) {
        if (item.hasErrors()) {
            listener.onError(new DataStoreException(
                "Bad subscription data for " + modelClass.getSimpleName() + ": " + item.getErrors(),
                AmplifyException.TODO_RECOVERY_SUGGESTION
            ));
            return;
        }
        listener.onNext(responseDeserializer.deserialize(item.getData(), modelClass));
    }

    @Override
    public void onComplete() {
        listener.onComplete();
    }

    @Override
    public void onError(Throwable error) {
        listener.onError(error);
    }
}
