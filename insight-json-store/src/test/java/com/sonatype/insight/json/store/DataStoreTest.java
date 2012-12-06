/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.json.store;

import static com.sonatype.insight.json.store.DataStore.augmentTable;
import static com.sonatype.insight.json.store.DataStore.loadData;
import static com.sonatype.insight.json.store.DataStore.parseData;
import static com.sonatype.insight.json.store.DataStore.saveData;
import static com.sonatype.insight.json.store.DataStore.streamData;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.equalToIgnoringWhiteSpace;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.codehaus.plexus.util.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class DataStoreTest
{
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder( new File( "target" ) );

    private File store;

    @Before
    public void setUp()
    {
        store = FileUtils.createTempFile( "audit", "test", temporaryFolder.getRoot() );
    }

    @Test
    public void testStoreObject()
        throws IOException
    {
        final String data =
            "{ \"license\" : \"EPL\", \"coords\" : { \"groupId\" : \"some\", \"artifactId\" : \"example\", \"version\" : \"0.1\" } }";

        saveData( store, parseData( data.getBytes( "UTF-8" ) ) );
        final byte[] buf = streamData( loadData( store ) );

        assertThat( new String( buf, "UTF-8" ), equalToIgnoringWhiteSpace( data ) );
    }

    @Test
    public void testStoreArray()
        throws IOException
    {
        final String data = "[ { \"id\" : \"one\" }, { \"id\" : \"two\" }, { \"id\" : \"three\" } ]";

        saveData( store, parseData( data.getBytes( "UTF-8" ) ) );
        final byte[] buf = streamData( loadData( store ) );

        assertThat( new String( buf, "UTF-8" ), equalToIgnoringWhiteSpace( data ) );
    }

    @Test
    public void testStoreData()
        throws IOException
    {
        final int[] data = { 1, 1, 2, 3, 5, 8 };

        assertThat( parseData( Arrays.toString( data ).getBytes( "UTF-8" ), int[].class ), equalTo( data ) );

        saveData( store, parseData( Arrays.toString( data ).getBytes( "UTF-8" ) ) );

        assertThat( loadData( store, int[].class ), equalTo( data ) );
    }

    @Test
    public void testAugmentData()
        throws IOException
    {
        final String table = "[ { \"id\" : \"one\" }, { \"id\" : \"two\" }, { \"id\" : \"three\" } ]";

        final String additions =
            "[ { \"data\" : [ { \"modified\" : \"true\", \"id\" : \"three\" } ] }, { \"data\" : [ { \"id\" : \"one\", \"count\" : 42 } ] } ]";

        final String result =
            "[ { \"id\" : \"one\", \"count\" : 42 }, { \"id\" : \"two\" }, { \"id\" : \"three\", \"modified\" : \"true\" } ]";

        saveData( store, parseData( additions.getBytes( "UTF-8" ) ) );

        final byte[] buf = streamData( augmentTable( parseData( table.getBytes( "UTF-8" ) ), store ) );

        assertThat( new String( buf, "UTF-8" ), equalToIgnoringWhiteSpace( result ) );
    }

    @Test
    public void testAugmentAAData()
        throws IOException
    {
        final String table = "{ \"aaData\" : [ { \"id\" : \"one\" }, { \"id\" : \"two\" }, { \"id\" : \"three\" } ] }";

        final String additions =
            "[ { \"data\" : [ { \"modified\" : \"true\", \"id\" : \"three\" } ] }, { \"data\" : [ { \"id\" : \"one\", \"count\" : 42 } ] } ]";

        final String result =
            "{ \"aaData\" : [ { \"id\" : \"one\", \"count\" : 42 }, { \"id\" : \"two\" }, { \"id\" : \"three\", \"modified\" : \"true\" } ] }";

        saveData( store, parseData( additions.getBytes( "UTF-8" ) ) );

        final byte[] buf = streamData( augmentTable( parseData( table.getBytes( "UTF-8" ) ), store ) );

        assertThat( new String( buf, "UTF-8" ), equalToIgnoringWhiteSpace( result ) );
    }

    @Test
    public void testAugmentNestedData()
        throws IOException
    {
        final String table = "[ { \"outer\" : { \"inner\" : { \"A\" : \"1\", \"B\" : \"2\" } } } ]";

        final String additions =
            "[ { \"data\" : [ { \"outer\" : { \"inner\" : { \"B\" : \"2\", \"level\" : 3 }, \"level\" : 2 }, \"level\" : 1 } ] } ]";

        final String result =
            "[ { \"outer\" : { \"inner\" : { \"A\" : \"1\", \"B\" : \"2\", \"level\" : 3 }, \"level\" : 2 }, \"level\" : 1 } ]";

        saveData( store, parseData( additions.getBytes( "UTF-8" ) ) );

        final byte[] buf = streamData( augmentTable( parseData( table.getBytes( "UTF-8" ) ), store ) );

        assertThat( new String( buf, "UTF-8" ), equalToIgnoringWhiteSpace( result ) );
    }

    @Test
    public void testAugmentDataOnlyAppliedToFirstMatchingRow()
        throws IOException
    {
        final String table = "[ { \"A\" : \"1\", \"B\" : \"2\" }, { \"A\" : \"1\", \"B\" : \"2\" } ]";

        final String additions = "[ { \"data\" : [ { \"B\" : \"2\", \"C\" : \"3\" } ] } ]";

        final String result = "[ { \"A\" : \"1\", \"B\" : \"2\", \"C\" : \"3\" }, { \"A\" : \"1\", \"B\" : \"2\" } ]";

        saveData( store, parseData( additions.getBytes( "UTF-8" ) ) );

        final byte[] buf = streamData( augmentTable( parseData( table.getBytes( "UTF-8" ) ), store ) );

        assertThat( new String( buf, "UTF-8" ), equalToIgnoringWhiteSpace( result ) );
    }
}
