/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.data;

import static com.sonatype.insight.brain.data.DataStore.augmentTable;
import static com.sonatype.insight.brain.data.DataStore.loadData;
import static com.sonatype.insight.brain.data.DataStore.parseData;
import static com.sonatype.insight.brain.data.DataStore.saveData;
import static com.sonatype.insight.brain.data.DataStore.streamData;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.equalToIgnoringWhiteSpace;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.junit.Test;

public class DataStoreTest
{
    @Test
    public void testStoreObject()
        throws IOException
    {
        final File store = File.createTempFile( "datastore", "test", new File( "target" ) );
        try
        {
            final String data =
                "{ \"license\" : \"EPL\", \"coords\" : { \"groupId\" : \"some\", \"artifactId\" : \"example\", \"version\" : \"0.1\" } }";

            saveData( store, parseData( data.getBytes( "UTF-8" ) ) );
            final byte[] buf = streamData( loadData( store ) );

            assertThat( new String( buf, "UTF-8" ), equalToIgnoringWhiteSpace( data ) );
        }
        finally
        {
            store.delete();
        }
    }

    @Test
    public void testStoreArray()
        throws IOException
    {
        final File store = File.createTempFile( "datastore", "test", new File( "target" ) );
        try
        {
            final String data = "[ { \"id\" : \"one\" }, { \"id\" : \"two\" }, { \"id\" : \"three\" } ]";

            saveData( store, parseData( data.getBytes( "UTF-8" ) ) );
            final byte[] buf = streamData( loadData( store ) );

            assertThat( new String( buf, "UTF-8" ), equalToIgnoringWhiteSpace( data ) );
        }
        finally
        {
            store.delete();
        }
    }

    @Test
    public void testStoreData()
        throws IOException
    {
        final File store = File.createTempFile( "datastore", "test", new File( "target" ) );
        try
        {
            final int[] data = { 1, 1, 2, 3, 5, 8 };

            saveData( store, parseData( Arrays.toString( data ).getBytes( "UTF-8" ) ) );

            assertThat( data, equalTo( loadData( store, int[].class ) ) );
        }
        finally
        {
            store.delete();
        }
    }

    @Test
    public void testAugmentData()
        throws IOException
    {
        final File store = File.createTempFile( "datastore", "test", new File( "target" ) );
        try
        {
            final String table =
                "{ \"aaData\" : [ { \"id\" : \"one\" }, { \"id\" : \"two\" }, { \"id\" : \"three\" } ] }";

            final String additions =
                "[ { \"data\" : [ { \"modified\" : \"true\", \"id\" : \"three\" } ] }, { \"data\" : [ { \"id\" : \"one\", \"count\" : \"42\" } ] } ]";

            final String result =
                "{ \"aaData\" : [ { \"id\" : \"one\", \"count\" : \"42\" }, { \"id\" : \"two\" }, { \"id\" : \"three\", \"modified\" : \"true\" } ] }";

            saveData( store, parseData( additions.getBytes( "UTF-8" ) ) );

            final byte[] buf = streamData( augmentTable( parseData( table.getBytes( "UTF-8" ) ), store ) );

            assertThat( new String( buf, "UTF-8" ), equalToIgnoringWhiteSpace( result ) );
        }
        finally
        {
            store.delete();
        }
    }
}
