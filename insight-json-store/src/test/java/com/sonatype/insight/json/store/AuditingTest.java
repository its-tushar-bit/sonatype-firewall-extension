/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.json.store;

import static com.sonatype.insight.json.store.Auditing.applyAugmentedData;
import static com.sonatype.insight.json.store.Auditing.filterAuditLog;
import static com.sonatype.insight.json.store.Auditing.getModificationCount;
import static com.sonatype.insight.json.store.Auditing.saveAugmentedData;
import static com.sonatype.insight.json.store.DataStore.parseData;
import static com.sonatype.insight.json.store.DataStore.streamData;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.equalToIgnoringWhiteSpace;
import static org.junit.Assert.assertNull;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

import org.codehaus.plexus.util.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

@SuppressWarnings( "boxing" )
public class AuditingTest
{
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder( new File( "target" ) );

    private File auditDir;

    @Before
    public void setUp()
    {
        auditDir = FileUtils.createTempFile( "audit", "test", temporaryFolder.getRoot() );
    }

    @Test
    public void testNoAugmentedData()
        throws IOException
    {
        final String table = "{ \"aaData\" : [ { \"id\" : \"A\" }, { \"id\" : \"B\" }, { \"id\" : \"C\" } ] }";

        final byte[] buf =
            streamData( applyAugmentedData( parseData( table.getBytes( "UTF-8" ) ), auditDir, "sample.json" ) );

        assertThat( new String( buf, "UTF-8" ), equalToIgnoringWhiteSpace( table ) );

        assertThat( getModificationCount( auditDir ), equalTo( 0 ) );
    }

    @Test
    public void testSingleAugmentedData()
        throws IOException
    {
        assertThat( getModificationCount( auditDir ), equalTo( 0 ) );

        final String table = "{ \"aaData\" : [ { \"id\" : \"A\" }, { \"id\" : \"B\" }, { \"id\" : \"C\" } ] }";

        final String addition = "[ { \"id\" : \"B\", \"override\" : \"EPL\", \"comment\" : \"Testing...\" } ]";

        final String result =
            "{ \"aaData\" : [ { \"id\" : \"A\" }, { \"id\" : \"B\", \"override\" : \"EPL\", \"comment\" : \"Testing...\" }, { \"id\" : \"C\" } ] }";

        saveAugmentedData( auditDir, "sample.json", new ByteArrayInputStream( addition.getBytes( "UTF-8" ) ), "anon",
                           "127.0.0.1", "office" );

        final byte[] buf =
            streamData( applyAugmentedData( parseData( table.getBytes( "UTF-8" ) ), auditDir, "sample.json" ) );

        assertThat( new String( buf, "UTF-8" ), equalToIgnoringWhiteSpace( result ) );

        assertThat( getModificationCount( auditDir ), equalTo( 1 ) );
    }

    @Test
    public void testMultipleAugmentedData()
        throws IOException
    {
        assertThat( getModificationCount( auditDir ), equalTo( 0 ) );

        final String table = "{ \"aaData\" : [ { \"id\" : \"A\" }, { \"id\" : \"B\" }, { \"id\" : \"C\" } ] }";

        final String addition1 =
            "[ { \"id\" : \"A\", \"override\" : \"EPL\" }, { \"id\" : \"B\", \"override\" : \"APL\" } ]";

        final String addition2 = "[ { \"id\" : \"B\", \"override\" : \"ASL\", \"comment\" : \"Fix typo\" } ]";

        final String result =
            "{ \"aaData\" : [ { \"id\" : \"A\", \"override\" : \"EPL\" }, { \"id\" : \"B\", \"override\" : \"ASL\", \"comment\" : \"Fix typo\" }, { \"id\" : \"C\" } ] }";

        saveAugmentedData( auditDir, "sample.json", new ByteArrayInputStream( addition1.getBytes( "UTF-8" ) ), "anon",
                           "127.0.0.1", "office" );

        assertThat( getModificationCount( auditDir ), equalTo( 1 ) );

        saveAugmentedData( auditDir, "sample.json", new ByteArrayInputStream( addition2.getBytes( "UTF-8" ) ), "anon",
                           "127.0.0.1", "office" );

        assertThat( getModificationCount( auditDir ), equalTo( 2 ) );

        final byte[] buf =
            streamData( applyAugmentedData( parseData( table.getBytes( "UTF-8" ) ), auditDir, "sample.json" ) );

        assertThat( new String( buf, "UTF-8" ), equalToIgnoringWhiteSpace( result ) );
    }

    @Test
    public void testFilteredNamedAuditFeed()
        throws IOException
    {
        final String addition1 =
            "[ { \"id\" : \"A\", \"override\" : \"EPL\" }, { \"id\" : \"B\", \"override\" : \"APL\" } ]";

        final String addition2 = "[ { \"id\" : \"B\", \"override\" : \"ASL\", \"comment\" : \"Fix typo\" } ]";

        final String result =
            "{ \"aaData\" : [ { \"id\" : \"B\", \"override\" : \"ASL\", \"comment\" : \"Fix typo\", \"time\" : 0, \"user\" : \"test\", \"ip\" : \"192.168.1.8\", \"where\" : \"home\", \"filename\" : \"sample.json\" }, "
                + "{ \"id\" : \"B\", \"override\" : \"APL\", \"time\" : 0, \"user\" : \"anon\", \"ip\" : \"127.0.0.1\", \"where\" : \"office\", \"filename\" : \"sample.json\" } ] }";

        saveAugmentedData( auditDir, "sample.json", new ByteArrayInputStream( addition1.getBytes( "UTF-8" ) ), "anon",
                           "127.0.0.1", "office" );

        saveAugmentedData( auditDir, "sample.json", new ByteArrayInputStream( addition2.getBytes( "UTF-8" ) ), "test",
                           "192.168.1.8", "home" );

        final byte[] buf = filterAuditLog( auditDir, "{\"id\":\"B\"}".getBytes( "UTF-8" ), "sample.json" );

        assertThat( new String( buf, "UTF-8" ).replaceAll( "\"time\" : [0-9]+", "\"time\" : 0" ),
                    equalToIgnoringWhiteSpace( result ) );
    }

    @Test
    public void testFilteredAuditFeed()
        throws IOException
    {
        final String addition1 =
            "[ { \"id\" : \"A\", \"override\" : \"EPL\" }, { \"id\" : \"B\", \"override\" : \"APL\" } ]";

        final String addition2 = "[ { \"id\" : \"B\", \"override\" : \"ASL\", \"comment\" : \"Fix typo\" } ]";

        final String addition3 = "[ { \"id\" : \"B\", \"confirmed\" : true, \"comment\" : \"Must fix\" } ]";

        final String result =
            "{ \"aaData\" : [ "
                + "{ \"id\" : \"B\", \"confirmed\" : true, \"comment\" : \"Must fix\", \"time\" : 0, \"user\" : \"test\", \"ip\" : \"127.0.0.1\", \"where\" : \"cafe\", \"filename\" : \"another.json\" }, "
                + "{ \"id\" : \"B\", \"override\" : \"ASL\", \"comment\" : \"Fix typo\", \"time\" : 0, \"user\" : \"test\", \"ip\" : \"192.168.1.8\", \"where\" : \"home\", \"filename\" : \"sample.json\" }, "
                + "{ \"id\" : \"B\", \"override\" : \"APL\", \"time\" : 0, \"user\" : \"anon\", \"ip\" : \"127.0.0.1\", \"where\" : \"office\", \"filename\" : \"sample.json\" }"
                + " ] }";

        saveAugmentedData( auditDir, "sample.json", new ByteArrayInputStream( addition1.getBytes( "UTF-8" ) ), "anon",
                           "127.0.0.1", "office" );

        saveAugmentedData( auditDir, "sample.json", new ByteArrayInputStream( addition2.getBytes( "UTF-8" ) ), "test",
                           "192.168.1.8", "home" );

        saveAugmentedData( auditDir, "another.json", new ByteArrayInputStream( addition3.getBytes( "UTF-8" ) ), "test",
                           "127.0.0.1", "cafe" );

        final byte[] buf = filterAuditLog( auditDir, "{\"id\":\"B\"}".getBytes( "UTF-8" ) );

        assertThat( new String( buf, "UTF-8" ).replaceAll( "\"time\" : [0-9]+", "\"time\" : 0" ),
                    equalToIgnoringWhiteSpace( result ) );
    }

    @Test
    public void testAuditFeed()
        throws IOException
    {
        final String addition1 =
            "[ { \"id\" : \"A\", \"override\" : \"EPL\" }, { \"id\" : \"B\", \"override\" : \"APL\" } ]";

        final String addition2 = "[ { \"id\" : \"B\", \"override\" : \"ASL\", \"comment\" : \"Fix typo\" } ]";

        final String addition3 = "[ { \"id\" : \"B\", \"confirmed\" : true, \"comment\" : \"Must fix\" } ]";

        final String result =
            "{ \"aaData\" : [ "
                + "{ \"id\" : \"B\", \"confirmed\" : true, \"comment\" : \"Must fix\", \"time\" : 0, \"user\" : \"test\", \"ip\" : \"127.0.0.1\", \"where\" : \"cafe\", \"filename\" : \"another.json\" }, "
                + "{ \"id\" : \"B\", \"override\" : \"ASL\", \"comment\" : \"Fix typo\", \"time\" : 0, \"user\" : \"test\", \"ip\" : \"192.168.1.8\", \"where\" : \"home\", \"filename\" : \"sample.json\" }, "
                + "{ \"id\" : \"A\", \"override\" : \"EPL\", \"time\" : 0, \"user\" : \"anon\", \"ip\" : \"127.0.0.1\", \"where\" : \"office\", \"filename\" : \"sample.json\" }, "
                + "{ \"id\" : \"B\", \"override\" : \"APL\", \"time\" : 0, \"user\" : \"anon\", \"ip\" : \"127.0.0.1\", \"where\" : \"office\", \"filename\" : \"sample.json\" }"
                + " ] }";

        saveAugmentedData( auditDir, "sample.json", new ByteArrayInputStream( addition1.getBytes( "UTF-8" ) ), "anon",
                           "127.0.0.1", "office" );

        saveAugmentedData( auditDir, "sample.json", new ByteArrayInputStream( addition2.getBytes( "UTF-8" ) ), "test",
                           "192.168.1.8", "home" );

        saveAugmentedData( auditDir, "another.json", new ByteArrayInputStream( addition3.getBytes( "UTF-8" ) ), "test",
                           "127.0.0.1", "cafe" );

        final byte[] buf = filterAuditLog( auditDir, null );

        assertThat( new String( buf, "UTF-8" ).replaceAll( "\"time\" : [0-9]+", "\"time\" : 0" ),
                    equalToIgnoringWhiteSpace( result ) );
    }

    @Test
    public void testEmptyAuditFeed()
        throws IOException
    {
        assertNull( filterAuditLog( auditDir, null, "" ) );
    }
}
