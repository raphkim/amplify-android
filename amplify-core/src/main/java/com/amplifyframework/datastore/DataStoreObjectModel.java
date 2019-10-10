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

/**
 * Interface for marking classes as objects that {@link DataStoreCategoryBehavior} can
 * operate on.
 *
 * <p>
 * Any model that is expected to be supported with {@link DataStoreCategoryBehavior} should
 * implement this interface.
 * <p>
 * <pre>
 * {@code
 *   public final class Weather implements DataStoreObjectModel {
 *       private final double temperatureInCelsius;
 *
 *       public Fruit(double temperatureInCelsius) {
 *           this.temperatureInCelsius = temperatureInCelsius;
 *       }
 *   }
 *
 *   Weather weather = new Weather("36.9");
 *   // Now, the object weather and Weather.class can be used in the
 *   // {@link DataStoreCategoryBehavior } operations.
 * }
 * </pre>
 * <p>
 */
public interface DataStoreObjectModel {

}
