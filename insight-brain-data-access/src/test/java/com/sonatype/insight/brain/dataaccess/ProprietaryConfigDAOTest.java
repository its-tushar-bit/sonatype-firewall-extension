/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.sonatype.clm.dto.model.ProprietaryConfig;

public class ProprietaryConfigDAOTest
{

    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder();

    private ProprietaryConfigDAO dao;

    @Before
    public void init()
        throws Exception
    {
        dao = new ProprietaryConfigDAO( tmpDir.newFolder() );
    }

    @Test
    public void testGet_NoConfigPersisted()
    {
        ProprietaryConfig config = dao.get();
        assertNotNull( config );
        assertEquals( 0, config.getPackages().size() );
    }

    @Test
    public void testUpdate()
    {
        List<String> packages = Arrays.asList( "org.sonatype", "com.sonatype" );
        ProprietaryConfig config = new ProprietaryConfig();
        config.setPackages( packages );
        dao.update( config );
        config = dao.get();
        assertEquals( packages, config.getPackages() );
    }

}
