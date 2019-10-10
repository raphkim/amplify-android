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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amplifyframework.core.async.Listener;
import com.amplifyframework.core.async.Result;
import com.amplifyframework.core.category.Category;
import com.amplifyframework.core.category.CategoryType;

public final class DataStoreCategory extends Category<DataStorePlugin> implements DataStoreCategoryBehavior {
    @Override
    public CategoryType getCategoryType() {
        return CategoryType.DATA;
    }

    /**
     * @param object
     * @param callback
     */
    @Override
    public <T> void save(@NonNull T object, @Nullable Listener<Result> callback) {
        getSelectedPlugin().save(object, callback);
    }
}
