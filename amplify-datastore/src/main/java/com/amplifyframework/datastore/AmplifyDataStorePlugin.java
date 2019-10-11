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

import android.content.ContentValues;
import android.content.Context;

import androidx.annotation.NonNull;

import com.amplifyframework.AmplifyRuntimeException;
import com.amplifyframework.core.async.Listener;
import com.amplifyframework.core.async.Result;
import com.amplifyframework.core.category.CategoryType;
import com.amplifyframework.core.plugin.PluginException;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class AmplifyDataStorePlugin implements DataStorePlugin {

    private final static String DATABASE_NAME = "AmplifyDataStorePlugin";
    private final static int DATABASE_VERSION = 1;

    private final Map<String, Class<?>> classNameToClassObject;
    private final DatabaseManager databaseManager;
    private final Executor diskIOExecutor;

    public AmplifyDataStorePlugin(@NonNull final Context context) {
        this.classNameToClassObject = new ConcurrentHashMap<String, Class<?>>();
        populateAllClassNames();
        this.databaseManager = new DatabaseManager(context,
                DATABASE_NAME,
                DATABASE_VERSION,
                getTableNameList(),
                getCreateTableSqlList());
        this.databaseManager.openDB();
        this.diskIOExecutor = Executors.newSingleThreadExecutor();
    }

    void tearDown() {
        this.databaseManager.closeDB();
    }

    /**
     * Gets a key which uniquely identifies the plugin instance.
     *
     * @return the identifier that identifies the plugin implementation
     */
    @Override
    public String getPluginKey() {
        return AmplifyDataStorePlugin.class.getSimpleName();
    }

    /**
     * Configure the plugin with customized configuration object.
     *
     * @param pluginConfiguration plugin-specific configuration
     * @throws PluginException when configuration for a plugin was not found
     */
    @Override
    public void configure(@NonNull Object pluginConfiguration) throws PluginException {

    }

    /**
     * Returns escape hatch for plugin to enable lower-level client use-cases.
     *
     * @return the client used by category plugin
     */
    @Override
    public Void getEscapeHatch() {
        return null;
    }

    /**
     * Gets the category type associated with the current object.
     *
     * @return The category type to which the current object is affiliated
     */
    @Override
    public CategoryType getCategoryType() {
        return null;
    }

    /**
     * @param object
     * @param listener
     */
    @Override
    public <T extends DataStoreObjectModel> void save(@NonNull final T object,
                                                      @NonNull final Listener<Result> listener) {
        // Get the properties of the class
        final Class<?> clazz = object.getClass();
        if (clazz.equals(DataStoreObjectModel.class)) {
            listener.onError(new IllegalArgumentException("DataStoreObjectModel was passed as an " +
                    "argument. Only subclasses of these can be used as arguments to methods that " +
                    "accept a DataStore Object Model class."));
            return;
        }

        final String className = clazz.getSimpleName();
        if (className == null || className.isEmpty()) {
            listener.onError(new AmplifyRuntimeException("Error in constructing the tableName."));
            return;
        }

        diskIOExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    // Lookup table name based on the class name
                    final String tableName = getTableNameForClass(className);

                    // Insert data into the table
                    databaseManager.insert(tableName, getContentValuesFromClass(object, clazz));

                    // Report success in the listener onResult
                    listener.onResult(new SaveResult());
                } catch (Exception ex) {
                    // Report any error in the listener onError
                    listener.onError(new AmplifyRuntimeException("Error in saving object. " + ex));
                }
            }
        });
    }

    private void populateAllClassNames() {
        classNameToClassObject.put(Person.class.getSimpleName(), Person.class);
    }

    private List<String> getTableNameList() {
        final List<String> tableNames = new ArrayList<String>();
        for (String className: classNameToClassObject.keySet()) {
            tableNames.add(getTableNameForClass(className));
        }
        return tableNames;
    }

    private List<String> getCreateTableSqlList() {
        final List<String> createTableSqlList = new ArrayList<String>();
        for (Map.Entry<String, Class<?>> entry : classNameToClassObject.entrySet()) {
            final String className = entry.getKey();
            final String tableName = getTableNameForClass(className);
            final Class<?> clazz = entry.getValue();
            final Set<Field> classFields = findFields(clazz, DataStoreField.class);

            String createSqlQuery;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("CREATE TABLE " + tableName + " (");
            Iterator<Field> iterator = classFields.iterator();
            while (iterator.hasNext()) {
                Field field = iterator.next();
                if (!iterator.hasNext()) {
                    stringBuilder.append(field.getName() + " " +
                            getSqlDataTypeForJavaType(field) + ")");
                } else {
                    stringBuilder.append(field.getName() + " " +
                            getSqlDataTypeForJavaType(field) + ",");
                }
            }
            createSqlQuery = stringBuilder.toString();
            createTableSqlList.add(createSqlQuery);
        }
        return createTableSqlList;
    }

    private String getSqlDataTypeForJavaType(@NonNull final Field field) {
        final Class<?> fieldDataType = field.getType();
        if (fieldDataType.equals(int.class) ||
            fieldDataType.equals(Integer.class) ||
            fieldDataType.equals(long.class) ||
            fieldDataType.equals(Long.class)) {
            return "INTEGER";
        } else if (fieldDataType.equals(String.class)) {
            return "TEXT";
        } else if (fieldDataType.equals(float.class) ||
                   fieldDataType.equals(Float.class)){
            return "REAL";
        } else {
            return "NULL";
        }
    }

    private String getTableNameForClass(final String className) {
        return "AmplifyDataStoreTable_" + className;
    }


    /**
     * @return null safe set
     */
    public static Set<Field> findFields(Class<?> clazz, Class<? extends Annotation> ann) {
        Set<Field> set = new HashSet<>();
        Class<?> c = clazz;
        while (c != null) {
            for (Field field : c.getDeclaredFields()) {
                if (field.isAnnotationPresent(ann)) {
                    set.add(field);
                }
            }
            c = c.getSuperclass();
        }
        return set;
    }

    private <T extends DataStoreObjectModel> ContentValues getContentValuesFromClass(
            T object, Class<?> clazz) throws IllegalAccessException {
        final ContentValues contentValues = new ContentValues();
        final Set<Field> classFields = findFields(clazz, DataStoreField.class);
        Iterator<Field> fields = classFields.iterator();
        while (fields.hasNext()) {
            Field field = fields.next();
            field.setAccessible(true);
            if (field.getType().equals(float.class) || field.getType().equals(Float.class)) {
                contentValues.put(field.getName(), (Float) field.get(object));
            } else if (field.getType().equals(int.class) || field.getType().equals(Integer.class)) {
                contentValues.put(field.getName(), (Integer) field.get(object));
            } else if (field.getType().equals(long.class) || field.getType().equals(Long.class)) {
                contentValues.put(field.getName(), (Long) field.get(object));
            } else if (field.getType().equals(double.class) || field.getType().equals(Double.class)) {
                contentValues.put(field.getName(), (Double) field.get(object));
            }
        }
        return contentValues;
    }
}
