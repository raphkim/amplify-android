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
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatabaseManager {

    // This is the activity context.
    private Context ctx = null;

    // Database helper class to manipulate sqlite database.
    private SQLiteDatabaseHelper dbHelper = null;

    // Represent connection with sqlite database.
    private SQLiteDatabase database = null;

    // Database name.
    private String dbName = "";

    // Database version.
    private int dbVersion = 0;

    // All tables that this database contains.
    private List<String> tableNameList = null;

    // All table create sql that this db need.
    private List<String> createTableSqlList = null;

    // Create database manager instance.
    public DatabaseManager(Context ctx,
                           String dbName,
                           int dbVersion,
                           List<String> tableNameList,
                           List<String> createTableSqlList) {
        this.ctx = ctx;
        this.dbName = dbName;
        this.dbVersion = dbVersion;
        this.tableNameList = tableNameList;
        this.createTableSqlList = createTableSqlList;
    }

    // Open database connection.
    public DatabaseManager openDB() {
        // Create database helper instance.
        dbHelper = new SQLiteDatabaseHelper(ctx,
                this.dbName,
                null,
                this.dbVersion,
                this.tableNameList,
                this.createTableSqlList);

        // Get sqlite database connection. If not exist then
        // create one else return a database object.
        this.database = dbHelper.getWritableDatabase();
        return this;
    }

    // close database connection.
    public void closeDB() {
        this.database.close();
        this.dbHelper.close();
        this.database = null;
        this.dbHelper = null;
    }

    /* Insert data into table.
     *  tableName : The table name.
     *  columnList : A list of table column name value pair.
     * */
    public void insert(String tableName, List<TableColumn> columnList) {
        if (!TextUtils.isEmpty(tableName) && columnList != null) {
            int size = columnList.size();

            if (size > 0) {
                // Create a content values object.
                ContentValues contentValues = new ContentValues();

                // Loop in the table column list
                for (int i = 0; i < size; i++) {
                    TableColumn tableColumn = columnList.get(i);

                    // Put column name and value in content values.
                    if (!TextUtils.isEmpty(tableColumn.getColumnName())) {
                        contentValues.put(tableColumn.getColumnName(), tableColumn.getColumnValue());
                    }
                }

                // Insert data.
                this.database.insert(tableName, null, contentValues);
            }
        }
    }

    /* Insert data into table.
     *  tableName : The table name.
     *  columnList : A list of table column name value pair.
     * */
    public void insert(String tableName, ContentValues contentValues) {
        if (contentValues != null && contentValues.size() > 0) {
            this.database.insert(tableName, null, contentValues);
        }
    }

    /*
     *  Update data in table.
     *  tableName : The table name.
     *  columnList : Need update column name and value.
     *  whereClause : The update rows meet condition.
     * */
    public int update(String tableName, List<TableColumn> columnList, String whereClause) {
        int ret = 0;
        if (!TextUtils.isEmpty(tableName) && columnList != null) {
            int size = columnList.size();

            if (size > 0) {
                // Create a content values object.
                ContentValues contentValues = new ContentValues();

                // Loop in the table column list
                for (int i = 0; i < size; i++) {
                    TableColumn tableColumn = columnList.get(i);

                    // Put column name and value in content values.
                    if (!TextUtils.isEmpty(tableColumn.getColumnName())) {
                        contentValues.put(tableColumn.getColumnName(), tableColumn.getColumnValue());
                    }
                }

                // Update data.Return update row count.
                ret = this.database.update(tableName, contentValues, whereClause, null);
            }
        }
        return ret;
    }

    /* Delete rows in table.
     *  tableName : Delete data table name.
     *  whereClause : Deleted rows meet condition. */
    public void delete(String tableName, String whereClause) {
        if (!TextUtils.isEmpty(tableName)) {
            this.database.delete(tableName, whereClause, null);
        }
    }

    /*
     *  Query all rows in sqlite database table.
     *  tableName : The table name.
     *  Return a list a Map object. Each map object represent a row of data in table.
     * */
    public List<Map<String, String>> queryAllReturnListMap(String tableName)
    {
        List<Map<String, String>> ret = new ArrayList<Map<String, String>>();

        // Query all rows in table.
        Cursor cursor = this.database.query(tableName,
                null,
                null,
                null,
                null,
                null,
                null);
        if (cursor != null)
        {
            // Get all column name in an array.
            String[] columnNamesArr = cursor.getColumnNames();

            // Move to first cursor.
            cursor.moveToFirst();
            do {
                // Create a map object represent a table row data.
                Map<String, String> rowMap = new HashMap<String, String>();

                // Get column count.
                int columnCount = columnNamesArr.length;
                for (int i = 0; i < columnCount; i++) {
                    // Get each column name.
                    String columnName = columnNamesArr[i];

                    // This is the column value.
                    String columnValue = "";

                    // Get column index value.
                    int columnIndex = cursor.getColumnIndex(columnName);

                    // Get current column data type.
                    int columnType = cursor.getType(columnIndex);

                    if (Cursor.FIELD_TYPE_STRING == columnType) {
                        columnValue = cursor.getString(columnIndex);
                    } else if(Cursor.FIELD_TYPE_INTEGER == columnType) {
                        columnValue = String.valueOf(cursor.getInt(columnIndex));
                    } else if(Cursor.FIELD_TYPE_FLOAT == columnType) {
                        columnValue = String.valueOf(cursor.getFloat(columnIndex));
                    } else if(Cursor.FIELD_TYPE_BLOB == columnType) {
                        columnValue = String.valueOf(cursor.getBlob(columnIndex));
                    } else if(Cursor.FIELD_TYPE_NULL == columnType) {
                        columnValue = "null";
                    }

                    rowMap.put(columnName, columnValue);

                    ret.add(rowMap);
                }
            } while(cursor.moveToNext());

            cursor.close();
        }
        return ret;
    }

    /*
     *  Query all rows in sqlite database table.
     *  tableName : The table name.
     *  Return a Cursor object.
     * */
    public Cursor queryAllReturnCursor(String tableName) {
        // Query all rows in table.
        Cursor cursor = this.database.query(tableName,
                null,
                null,
                null,
                null,
                null,
                null);
        if (cursor != null) {
            // Move to first cursor.
            cursor.moveToFirst();
        }
        return cursor;
    }

    public SQLiteDatabaseHelper getDbHelper() {
        return dbHelper;
    }

    public SQLiteDatabase getDatabase() {
        return database;
    }
}
