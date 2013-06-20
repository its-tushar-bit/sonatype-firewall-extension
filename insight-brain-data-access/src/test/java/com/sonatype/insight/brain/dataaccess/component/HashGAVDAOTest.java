/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.component;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.component.HashGAV;
import com.sonatype.insight.error.exception.BadRequestException;

public class HashGAVDAOTest
    extends AbstractDbDAOTest
{
    @Test
    public void testCRUD()
        throws Exception
    {
        HashGAVDAO dao = new HashGAVDAO();

        String hash = "123456789012345678901";
        assertTrue( hash.length() > 20 );
        String truncatedHash = hash.substring( 0, 20 );
        String groupId = "HashGAVDAOTest_G";
        String artifactId = "HashGAVDAOTest_A";
        String version = "HashGAVDAOTest_V";
        String extension = "HashGAVDAOTest_E";
        String classifier = "HashGAVDAOTest_C";

        // Create
        HashGAV hashGAV = new HashGAV( hash, groupId, artifactId, version, extension, classifier );
        assertNull( hashGAV.getId() );
        dao.insert( hashGAV );
        assertNotNull( hashGAV.getId() );

        // Read
        hashGAV = dao.getById( hashGAV.getId() );
        assertNotNull( hashGAV );
        assertHashGAV( truncatedHash, groupId, artifactId, version, extension, classifier, hashGAV );

        // Update is not allowed
        try
        {
            dao.update( hashGAV );
            fail( "Expected UnsupportedOperationException, updates to HashGAV are not allowed" );
        }
        catch ( UnsupportedOperationException expected )
        {
        }

        // Delete
        dao.delete( hashGAV );

        hashGAV = dao.getById( hashGAV.getId() );
        assertNull( hashGAV );
    }

    @Test
    public void testExtensionNotRequired_Null()
        throws Exception
    {
        HashGAVDAO dao = new HashGAVDAO();

        String hash = "ab1234ab1234ab";
        String groupId = "HashGAVDAOTest_G";
        String artifactId = "HashGAVDAOTest_A";
        String version = "HashGAVDAOTest_V";
        String extension = null;
        String classifier = "HashGAVDAOTest_C";

        HashGAV hashGAV = new HashGAV( hash, groupId, artifactId, version, extension, classifier );
        dao.insert( hashGAV );
        assertNotNull( hashGAV.getId() );
        hashGAV = dao.getById( hashGAV.getId() );
        assertNotNull( hashGAV );
        assertHashGAV( hash, groupId, artifactId, version, extension, classifier, hashGAV );

        dao.delete( hashGAV );
    }

    @Test
    public void testExtensionNotRequired_Empty()
        throws Exception
    {
        HashGAVDAO dao = new HashGAVDAO();

        String hash = "ab1234ab1234ab";
        String groupId = "HashGAVDAOTest_G";
        String artifactId = "HashGAVDAOTest_A";
        String version = "HashGAVDAOTest_V";
        String extension = " ";
        String classifier = "HashGAVDAOTest_C";

        HashGAV hashGAV = new HashGAV( hash, groupId, artifactId, version, extension, classifier );
        dao.insert( hashGAV );
        assertNotNull( hashGAV.getId() );
        hashGAV = dao.getById( hashGAV.getId() );
        assertNotNull( hashGAV );
        assertHashGAV( hash, groupId, artifactId, version, null /* extension */, classifier, hashGAV );

        dao.delete( hashGAV );
    }

    @Test
    public void testClassifierNotRequired_Null()
        throws Exception
    {
        HashGAVDAO dao = new HashGAVDAO();

        String hash = "ab1234ab1234ab";
        String groupId = "HashGAVDAOTest_G";
        String artifactId = "HashGAVDAOTest_A";
        String version = "HashGAVDAOTest_V";
        String extension = "HashGAVDAOTest_E";
        String classifier = null;

        HashGAV hashGAV = new HashGAV( hash, groupId, artifactId, version, extension, classifier );
        dao.insert( hashGAV );
        assertNotNull( hashGAV.getId() );
        hashGAV = dao.getById( hashGAV.getId() );
        assertNotNull( hashGAV );
        assertHashGAV( hash, groupId, artifactId, version, extension, classifier, hashGAV );

        dao.delete( hashGAV );
    }

    @Test
    public void testClassifierNotRequired_Empty()
        throws Exception
    {
        HashGAVDAO dao = new HashGAVDAO();

        String hash = "ab1234ab1234ab";
        String groupId = "HashGAVDAOTest_G";
        String artifactId = "HashGAVDAOTest_A";
        String version = "HashGAVDAOTest_V";
        String extension = "HashGAVDAOTest_E";
        String classifier = " ";

        HashGAV hashGAV = new HashGAV( hash, groupId, artifactId, version, extension, classifier );
        dao.insert( hashGAV );
        assertNotNull( hashGAV.getId() );
        hashGAV = dao.getById( hashGAV.getId() );
        assertNotNull( hashGAV );
        assertHashGAV( hash, groupId, artifactId, version, extension, null /* classifier */, hashGAV );

        dao.delete( hashGAV );
    }

    private void assertHashGAV( String hash, String groupId, String artifactId, String version, String extension,
                                String classifier, HashGAV hashGAV )
    {
        assertEquals( hash, hashGAV.getHash() );
        assertEquals( groupId, hashGAV.getGroupId() );
        assertEquals( artifactId, hashGAV.getArtifactId() );
        assertEquals( version, hashGAV.getVersion() );
        assertEquals( extension, hashGAV.getExtension() );
        assertEquals( classifier, hashGAV.getClassifier() );
    }

    @Test
    public void testAddDuplicateByHash()
        throws Exception
    {
        HashGAVDAO dao = new HashGAVDAO();

        String hash = "ab1234ab1234ab";
        String groupId = "HashGAVDAOTest_G";
        String artifactId = "HashGAVDAOTest_A";
        String version = "HashGAVDAOTest_V";
        String extension = "HashGAVDAOTest_E";
        String classifier = "HashGAVDAOTest_C";

        HashGAV hashGAV1 = new HashGAV( hash, groupId, artifactId, version, extension, classifier );
        dao.insert( hashGAV1 );

        HashGAV hashGAV2 = new HashGAV( hash, groupId + "New", artifactId, version, extension, classifier );
        try
        {
            dao.insert( hashGAV2 );
            fail( "Expected BadRequestException" );
        }
        catch ( BadRequestException expected )
        {
            assertEquals( "This component is already mapped to 'HashGAVDAOTest_G:HashGAVDAOTest_A:HashGAVDAOTest_V:HashGAVDAOTest_E:HashGAVDAOTest_C'",
                          expected.getMessage() );
        }

        dao.delete( hashGAV1 );
    }

    @Test
    public void testAddDuplicateByGAVEC()
        throws Exception
    {
        HashGAVDAO dao = new HashGAVDAO();

        String hash = "ab1234ab1234ab";
        String groupId = "HashGAVDAOTest_G";
        String artifactId = "HashGAVDAOTest_A";
        String version = "HashGAVDAOTest_V";
        String extension = "HashGAVDAOTest_E";
        String classifier = "HashGAVDAOTest_C";

        HashGAV hashGAV1 = new HashGAV( hash, groupId, artifactId, version, extension, classifier );
        dao.insert( hashGAV1 );

        HashGAV hashGAV2 = new HashGAV( hash + "1", groupId, artifactId, version, extension, classifier );
        try
        {
            dao.insert( hashGAV2 );
            fail( "Expected BadRequestException" );
        }
        catch ( BadRequestException expected )
        {
            assertEquals( "Another component is already mapped to 'HashGAVDAOTest_G:HashGAVDAOTest_A:HashGAVDAOTest_V:HashGAVDAOTest_E:HashGAVDAOTest_C'",
                          expected.getMessage() );
        }

        dao.delete( hashGAV1 );
    }
}
