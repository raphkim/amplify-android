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

package com.amplifyframework.storage;

import com.amplifyframework.core.category.CategoryConfiguration;
import com.amplifyframework.core.category.CategoryType;

/**
 * Strongly-typed configuration for Storage category that also
 * contains configuration for individual plugins.
 */
public final class StorageCategoryConfiguration extends CategoryConfiguration {
    // Any category level properties would be defined here and populateFromJson would be overridden
    // below to fill in these values from the JSON data.

    /**
     * Constructs a new StorageCategoryConfiguration.
     */
    public StorageCategoryConfiguration() {
        super();
    }

    /**
     * Gets the category type associated with the current object.
     *
     * @return The category type to which the current object is affiliated
     */
    @Override
    public CategoryType getCategoryType() {
        return CategoryType.STORAGE;
    }
}
