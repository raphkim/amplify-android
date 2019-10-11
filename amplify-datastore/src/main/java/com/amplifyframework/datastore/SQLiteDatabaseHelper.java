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

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;
import android.util.Log;

import java.util.List;

public class SQLiteDatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = SQLiteDatabaseHelper.class.getSimpleName();

    // Contains all table name string list.
    private final List<String> tableNameList;

    // Contains all create table sql command string list.
    private final List<String> createTableSqlList;

    // This is the android activity context.
    private Context ctx = null;

    /* Constructor with all input parameter*/
    public SQLiteDatabaseHelper(Context context,
                                String name,
                                SQLiteDatabase.CursorFactory factory,
                                int version,
                                List<String> tableNameList,
                                List<String> createTableSqlList) {
        super(context, name, factory, version);
        ctx = context;
        this.tableNameList = tableNameList;
        this.createTableSqlList = createTableSqlList;
    }

    /* Generally run all create table sql in this method. */
    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        int size = createTableSqlList.size();
        for (int i = 0; i < size; i++) {
            // Loop all the create table sql command string in the list.
            // each sql will create a table in sqlite database.
            String createTableSql = createTableSqlList.get(i);
            sqLiteDatabase.execSQL(createTableSql);

            Log.d(TAG, "Run sql successfully, " + createTableSql);
        }
    }

    /* When the new db version is bigger than current exist db version, this method will be invoked.
     * It always drop all tables and then call onCreate() method to create all table again.*/
    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        // Loop and drop all exist sqlite table.
        int size = tableNameList.size();
        for (int i = 0; i < size; i++) {
            String tableName = tableNameList.get(i);
            if(!TextUtils.isEmpty(tableName)) {
                sqLiteDatabase.execSQL("drop table if exists " + tableName);
            }
        }

        // After drop all exist tables, create all tables again.
        onCreate(sqLiteDatabase);
    }
}
