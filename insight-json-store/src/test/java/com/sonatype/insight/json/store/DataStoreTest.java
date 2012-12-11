/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.json.store;

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

    private File file;

    private JsonStore store;

    @Before
    public void setUp()
    {
        file = FileUtils.createTempFile( "audit", "test", temporaryFolder.getRoot() );
        store = new JsonFileStore( file.getParentFile() );
    }

    @Test
    public void testStoreObject()
        throws IOException
    {
        final String data =
            "{ \"license\" : \"EPL\", \"coords\" : { \"groupId\" : \"some\", \"artifactId\" : \"example\", \"version\" : \"0.1\" } }";

        JsonUtils.write( file, JsonUtils.parse( data.getBytes( "UTF-8" ) ) );
        final byte[] buf = JsonUtils.generate( JsonUtils.read( file ) );

        assertThat( new String( buf, "UTF-8" ), equalToIgnoringWhiteSpace( data ) );
    }

    @Test
    public void testStoreArray()
        throws IOException
    {
        final String data = "[ { \"id\" : \"one\" }, { \"id\" : \"two\" }, { \"id\" : \"three\" } ]";

        JsonUtils.write( file, JsonUtils.parse( data.getBytes( "UTF-8" ) ) );
        final byte[] buf = JsonUtils.generate( JsonUtils.read( file ) );

        assertThat( new String( buf, "UTF-8" ), equalToIgnoringWhiteSpace( data ) );
    }

    @Test
    public void testStoreData()
        throws IOException
    {
        final int[] data = { 1, 1, 2, 3, 5, 8 };

        assertThat( JsonUtils.parse( Arrays.toString( data ).getBytes( "UTF-8" ), int[].class ), equalTo( data ) );

        JsonUtils.write( file, JsonUtils.parse( Arrays.toString( data ).getBytes( "UTF-8" ) ) );

        assertThat( JsonUtils.read( file, int[].class ), equalTo( data ) );
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

        JsonUtils.write( file, JsonUtils.parse( additions.getBytes( "UTF-8" ) ) );

        final byte[] buf =
            JsonUtils.generate( store.augment( JsonUtils.parse( table.getBytes( "UTF-8" ) ), file.getName() ) );

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

        JsonUtils.write( file, JsonUtils.parse( additions.getBytes( "UTF-8" ) ) );

        final byte[] buf =
            JsonUtils.generate( store.augment( JsonUtils.parse( table.getBytes( "UTF-8" ) ), file.getName() ) );

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

        JsonUtils.write( file, JsonUtils.parse( additions.getBytes( "UTF-8" ) ) );

        final byte[] buf =
            JsonUtils.generate( store.augment( JsonUtils.parse( table.getBytes( "UTF-8" ) ), file.getName() ) );

        assertThat( new String( buf, "UTF-8" ), equalToIgnoringWhiteSpace( result ) );
    }

    @Test
    public void testAugmentDataOnlyAppliedToFirstMatchingRow()
        throws IOException
    {
        final String table = "[ { \"A\" : \"1\", \"B\" : \"2\" }, { \"A\" : \"1\", \"B\" : \"2\" } ]";

        final String additions = "[ { \"data\" : [ { \"B\" : \"2\", \"C\" : \"3\" } ] } ]";

        final String result = "[ { \"A\" : \"1\", \"B\" : \"2\", \"C\" : \"3\" }, { \"A\" : \"1\", \"B\" : \"2\" } ]";

        JsonUtils.write( file, JsonUtils.parse( additions.getBytes( "UTF-8" ) ) );

        final byte[] buf =
            JsonUtils.generate( store.augment( JsonUtils.parse( table.getBytes( "UTF-8" ) ), file.getName() ) );

        assertThat( new String( buf, "UTF-8" ), equalToIgnoringWhiteSpace( result ) );
    }
}
