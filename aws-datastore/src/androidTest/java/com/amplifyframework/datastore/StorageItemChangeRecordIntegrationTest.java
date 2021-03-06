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

import com.amplifyframework.core.ResultListener;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelProvider;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.datastore.storage.GsonStorageItemChangeConverter;
import com.amplifyframework.datastore.storage.LocalStorageAdapter;
import com.amplifyframework.datastore.storage.StorageItemChange;
import com.amplifyframework.datastore.storage.sqlite.SQLiteStorageAdapter;
import com.amplifyframework.testmodels.commentsblog.BlogOwner;
import com.amplifyframework.testutils.LatchedResultListener;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.observers.TestObserver;

import static org.junit.Assert.assertEquals;

/**
 * Tests that the {@link SQLiteStorageAdapter} is able to serve as as repository
 * for our {@link StorageItemChange.Record}s.
 */
public final class StorageItemChangeRecordIntegrationTest {
    private static final String DATABASE_NAME = "AmplifyDatastore.db";

    private GsonStorageItemChangeConverter storageItemChangeConverter;
    private LocalStorageAdapter localStorageAdapter;

    /**
     * Prepare an instance of {@link LocalStorageAdapter}, and ensure that it will
     * return a ModelSchema for the {@link StorageItemChange.Record} type.
     * TODO: later, consider hiding this schema from the callback. This is sort
     *       of leaking an implementation detail to the customer of the API.
     */
    @Before
    public void obtainLocalStorageAndValidateModelSchema() {
        this.storageItemChangeConverter = new GsonStorageItemChangeConverter();
        ApplicationProvider.getApplicationContext().deleteDatabase(DATABASE_NAME);

        LatchedResultListener<List<ModelSchema>> schemaListener = LatchedResultListener.instance();
        ModelProvider modelProvider = ModelProviderFactory.createProviderOf(BlogOwner.class);
        this.localStorageAdapter = SQLiteStorageAdapter.forModels(modelProvider);
        localStorageAdapter.initialize(ApplicationProvider.getApplicationContext(), schemaListener);

        // Evaluate the returned set of ModelSchema. Make sure that there is one
        // for the StorageItemChange.Record system class, and one for
        // the PersistentModelVersion.

        final List<String> actualNames = new ArrayList<>();
        for (ModelSchema modelSchema : schemaListener.awaitResult()) {
            actualNames.add(modelSchema.getName());
        }
        Collections.sort(actualNames);
        assertEquals(
            Arrays.asList("BlogOwner", "PersistentModelVersion", "Record"),
            actualNames
        );
    }

    /**
     * Drop all tables and database, terminate and delete the database.
     * @throws DataStoreException from DataStore terminate
     */
    @After
    public void terminateLocalStorageAdapter() throws DataStoreException {
        localStorageAdapter.terminate();
        ApplicationProvider.getApplicationContext().deleteDatabase(DATABASE_NAME);
    }

    /**
     * The adapter must be able to save a StorageItemChange.Record. When we query the adapter for that
     * same StorageItemChange.Record, we will expect to find an exact replica of the one we
     * had saved.
     * @throws DataStoreException from storage item change
     */
    @Test
    public void adapterCanSaveAndQueryChangeRecords() throws DataStoreException {
        final BlogOwner tonyDaniels = BlogOwner.builder()
            .name("Tony Daniels")
            .build();

        final StorageItemChange<BlogOwner> originalSaveForTony = StorageItemChange.<BlogOwner>builder()
            .item(tonyDaniels)
            .itemClass(BlogOwner.class)
            .type(StorageItemChange.Type.SAVE)
            .initiator(StorageItemChange.Initiator.SYNC_ENGINE)
            .build();

        // Save the creation mutation for Tony, as a Record object.
        StorageItemChange.Record saveForTonyAsRecord =
            originalSaveForTony.toRecord(storageItemChangeConverter);
        save(saveForTonyAsRecord);

        // Now, lookup what records we have in the storage.
        List<StorageItemChange.Record> foundRecords = query();

        // There should be 1, the save for the insertionForTony.
        // and it should be identical to the thing we tried to save.
        assertEquals(1, foundRecords.size());
        StorageItemChange.Record firstResultRecord = foundRecords.get(0);
        assertEquals(saveForTonyAsRecord, firstResultRecord);

        // After we convert back from record, we should get back a copy of
        // what we created above
        StorageItemChange<BlogOwner> reconstructedSaveForTony =
            firstResultRecord.toStorageItemChange(storageItemChangeConverter);
        assertEquals(originalSaveForTony, reconstructedSaveForTony);
    }

    /**
     * When {@link LocalStorageAdapter#save(Model, StorageItemChange.Initiator, ResultListener)}
     * is called for a {@link StorageItemChange.Record}, we should see this event on the
     * {@link LocalStorageAdapter#observe()} method's {@link Observable}.
     * @throws DataStoreException from storage item change
     */
    @Test
    public void saveIsObservedForChangeRecord() throws DataStoreException {

        // Start watching observe() ...
        TestObserver<StorageItemChange.Record> observer = TestObserver.create();
        localStorageAdapter.observe().subscribe(observer);

        // Save something ..
        StorageItemChange.Record record = StorageItemChange.<BlogOwner>builder()
            .initiator(StorageItemChange.Initiator.SYNC_ENGINE)
            .item(BlogOwner.builder()
                .name("Juan Gonzales")
                .build())
            .itemClass(BlogOwner.class)
            .type(StorageItemChange.Type.SAVE)
            .build()
            .toRecord(new GsonStorageItemChangeConverter());

        // Wait for it to save...
        LatchedResultListener<StorageItemChange.Record> listener = LatchedResultListener.instance();
        localStorageAdapter.save(record, StorageItemChange.Initiator.SYNC_ENGINE, listener);
        listener.awaitResult();

        // Assert that our observer got the item;
        // The record we get back has the saved record inside of it, as the contained item field.
        observer.awaitCount(1);
        assertEquals(
            record,
            observer.values().get(0)
                .toStorageItemChange(new GsonStorageItemChangeConverter())
                .item()
        );
    }

    /**
     * When {@link LocalStorageAdapter#save(Model, StorageItemChange.Initiator, ResultListener)}
     * is called for a {@link StorageItemChange.Record}, we should expect to observe a
     * record /containing/ that record within it, via the {@link Observable} returned from
     * {@link LocalStorageAdapter#observe()}.
     *
     * Similarly, when we update the record that we had just saved, we should see an update
     * record on the observable. The type will be StorageItemChange.Record and inside of it
     * will be a StorageItemChange.Record which itself contains a BlogOwner.
     * @throws DataStoreException from storage item change
     */
    @Ignore("update operations are not currently implemented! TODO: validate this, once available")
    @Test
    public void updatesAreObservedForChangeRecords() throws DataStoreException {
        // Establish a subscription to listen for storage change records
        TestObserver<StorageItemChange.Record> recordObserver = TestObserver.create();
        localStorageAdapter.observe().subscribe(recordObserver);

        // Create a record for Joe, and a change to save him into storage
        BlogOwner joeLastNameMispelled = BlogOwner.builder()
            .name("Joe Sweeneyy")
            .build();
        StorageItemChange<BlogOwner> saveJoeWrongLastName = StorageItemChange.<BlogOwner>builder()
            .type(StorageItemChange.Type.SAVE)
            .item(joeLastNameMispelled)
            .itemClass(BlogOwner.class)
            .initiator(StorageItemChange.Initiator.SYNC_ENGINE)
            .build();
        StorageItemChange.Record saveJoeWrongLastNameRecord =
            saveJoeWrongLastName.toRecord(storageItemChangeConverter);

        // Save our saveJoeWrongLastName change item, as a record.
        save(saveJoeWrongLastNameRecord);

        // Now, suppose we have to update that change object. Maybe it contained a bad item payload.
        BlogOwner joeWithLastNameFix = BlogOwner.builder()
            .name("Joe Sweeney")
            .build();
        StorageItemChange<BlogOwner> saveJoeCorrectLastName = StorageItemChange.<BlogOwner>builder()
            .changeId(saveJoeWrongLastName.changeId().toString()) // Same ID
            .item(joeWithLastNameFix) // But with a patch to the item
            .itemClass(BlogOwner.class)
            .initiator(StorageItemChange.Initiator.SYNC_ENGINE)
            .type(StorageItemChange.Type.SAVE) // We're still saving Joe, we're updating this change.
            .build();
        StorageItemChange.Record saveJoeCorrectLastNameRecord =
            saveJoeCorrectLastName.toRecord(storageItemChangeConverter);

        // Save an update (same model type, same unique ID) to the thing we saved before.
        save(saveJoeCorrectLastNameRecord);

        // Our observer got the records to save Joe with wrong age, and also to save joe with right age
        recordObserver.awaitCount(2);
        recordObserver.assertNoErrors();
        recordObserver.assertValues(saveJoeWrongLastNameRecord, saveJoeCorrectLastNameRecord);
    }

    /**
     * When an {@link StorageItemChange.Record} is deleted from the DataStore, the
     * {@link Observable} returned by {@link LocalStorageAdapter#observe()} shall
     * emit that same change record.
     * @throws DataStoreException from storage item change
     */
    @Ignore("delete() is not currently implemented! Validate this test when it is.")
    @Test
    public void deletionIsObservedForChangeRecord() throws DataStoreException {
        // What we are really observing are items of type StorageItemChange.Record that contain
        // StorageItemChange.Record of BlogOwner
        TestObserver<StorageItemChange.Record> saveObserver = TestObserver.create();
        localStorageAdapter.observe().subscribe(saveObserver);

        BlogOwner beatrice = BlogOwner.builder()
            .name("Beatrice Stone")
            .build();
        StorageItemChange<BlogOwner> saveBeatrice = StorageItemChange.<BlogOwner>builder()
            .item(beatrice)
            .itemClass(BlogOwner.class)
            .type(StorageItemChange.Type.SAVE)
            .initiator(StorageItemChange.Initiator.SYNC_ENGINE)
            .build();
        StorageItemChange.Record saveBeatriceRecord =
            saveBeatrice.toRecord(storageItemChangeConverter);

        save(saveBeatriceRecord);

        // Assert that we do observe the record being saved ...
        saveObserver.awaitCount(1);
        saveObserver.assertNoErrors();
        assertEquals(
            saveBeatriceRecord,
            saveObserver.values().get(0)
                .toStorageItemChange(storageItemChangeConverter)
                .item()
        );
        saveObserver.dispose();

        TestObserver<StorageItemChange.Record> deletionObserver = TestObserver.create();
        localStorageAdapter.observe().subscribe(deletionObserver);

        // The mutation record doesn't change, but we want to delete it, itself.
        delete(saveBeatriceRecord);

        deletionObserver.awaitCount(1);
        deletionObserver.assertNoErrors();
        deletionObserver.assertValue(saveBeatriceRecord);
        deletionObserver.dispose();
    }

    private void save(StorageItemChange.Record storageItemChangeRecord) throws DataStoreException {
        // The thing we are saving is a StorageItemChange.Record.
        // The fact that it is getting saved means it gets wrapped into another
        // StorageItemChange.Record, which itself contains the original StorageItemChange.Record.
        LatchedResultListener<StorageItemChange.Record> saveResultListener = LatchedResultListener.instance();

        localStorageAdapter.save(storageItemChangeRecord,
            StorageItemChange.Initiator.SYNC_ENGINE, saveResultListener);

        final StorageItemChange<?> convertedResult =
            saveResultListener.awaitResult().toStorageItemChange(storageItemChangeConverter);

        // Peel out the item from the save result - the item inside is the thing we tried to save,
        // e.g., the mutation to create BlogOwner
        // It should be identical to the thing that we tried to save.
        assertEquals(storageItemChangeRecord, convertedResult.item());
    }

    private List<StorageItemChange.Record> query() {
        // Okay, now we're going to do a query, then await & stash the query results.
        LatchedResultListener<Iterator<StorageItemChange.Record>> queryResultsListener =
            LatchedResultListener.instance();

        // TODO: if/when there is a form of query() which shall accept QueryPredicate, use that instead.
        localStorageAdapter.query(StorageItemChange.Record.class, queryResultsListener);

        final Iterator<StorageItemChange.Record> queryResultsIterator = queryResultsListener.awaitResult();
        final List<StorageItemChange.Record> storageItemChangeRecords = new ArrayList<>();
        while (queryResultsIterator.hasNext()) {
            storageItemChangeRecords.add(queryResultsIterator.next());
        }
        return storageItemChangeRecords;
    }

    private void delete(StorageItemChange.Record record) throws DataStoreException {
        // The thing we are deleting is a StorageItemChange.Record, which is wrapping
        // a StorageItemChange.Record, which is wrapping an item.
        LatchedResultListener<StorageItemChange.Record> recordDeletionListener =
            LatchedResultListener.instance();

        localStorageAdapter.delete(record, StorageItemChange.Initiator.SYNC_ENGINE, recordDeletionListener);

        // Peel out the inner record out from the save result -
        // the record inside is the thing we tried to save,
        // that is, the record to change an item
        // That interior record should be identical to the thing that we tried to save.
        StorageItemChange<StorageItemChange.Record> recordOfDeletion =
            recordDeletionListener.awaitResult().toStorageItemChange(storageItemChangeConverter);
        assertEquals(record, recordOfDeletion.item());

        // The record of the record itself has type DELETE, corresponding to our call to delete().
        assertEquals(StorageItemChange.Type.DELETE, recordOfDeletion.type());
    }
}
