/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.codehaus.plexus.util.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;

public class PolicyEvaluationLogTest
{

    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder();

    @Test
    public void testLast_None()
        throws Exception
    {
        PolicyEvaluationLog log = new PolicyEvaluationLog( tmpDir.getRoot() );
        assertNull( log.lastByStage( Stage.ID_BUILD ) );
    }

    @Test
    public void testAdd()
        throws Exception
    {
        PolicyEvaluationLog log = new PolicyEvaluationLog( tmpDir.getRoot() );
        log.add( new Stage( Stage.ID_BUILD ), "scanId", "user", "ip" );

        assertNull( log.lastByStage( Stage.ID_RELEASE ) );
        assertNull( log.lastByStage( Stage.ID_STAGE_RELEASE ) );
        assertNull( log.lastByStage( Stage.ID_OPERATE ) );
        assertNull( log.lastByStage( Stage.ID_PROCURE ) );
        assertNull( log.lastByStage( Stage.ID_DEVELOP ) );

        PolicyEvaluation eval = log.lastByStage( Stage.ID_BUILD );
        assertNotNull( eval );
        assertNotNull( eval.getStage() );
        assertEquals( Stage.ID_BUILD, eval.getStage().getStageTypeId() );
        assertEquals( "scanId", eval.getScanId() );
        assertFalse( "isReevaluation", eval.isReevaluation() );
        assertEquals( "user", eval.getUser() );
        assertTrue( System.currentTimeMillis() - eval.getTime() < 60 * 1000 );
    }

    @Test
    public void testAdd_Reevaluation()
        throws Exception
    {
        PolicyEvaluationLog log = new PolicyEvaluationLog( tmpDir.getRoot() );
        log.add( new Stage( Stage.ID_BUILD ), "scanId", true /* isReevaluation */, "user", "ip" );

        assertNull( log.lastByStage( Stage.ID_RELEASE ) );
        assertNull( log.lastByStage( Stage.ID_STAGE_RELEASE ) );
        assertNull( log.lastByStage( Stage.ID_OPERATE ) );
        assertNull( log.lastByStage( Stage.ID_PROCURE ) );
        assertNull( log.lastByStage( Stage.ID_DEVELOP ) );

        PolicyEvaluation eval = log.lastByStage( Stage.ID_BUILD );
        assertNotNull( eval );
        assertNotNull( eval.getStage() );
        assertEquals( Stage.ID_BUILD, eval.getStage().getStageTypeId() );
        assertEquals( "scanId", eval.getScanId() );
        assertTrue( "isReevaluation", eval.isReevaluation() );
        assertEquals( "user", eval.getUser() );
        assertTrue( System.currentTimeMillis() - eval.getTime() < 60 * 1000 );
    }

    @Test
    public void testGet()
        throws IOException
    {
        final Stage stage = new Stage( Stage.ID_BUILD );
        final String scanId = "scanId";
        final String user = "user";
        final String ip = "ip";

        PolicyEvaluationLog log = new PolicyEvaluationLog( tmpDir.getRoot() );

        PolicyEvaluation evaluation = log.lastByScan( scanId );
        assertNull( evaluation );

        log.add( stage, scanId, user, ip );

        evaluation = log.lastByScan( scanId );
        assertNotNull( evaluation );
        assertNotNull( evaluation.getStage() );
        assertEquals( stage.getStageTypeId(), evaluation.getStage().getStageTypeId() );
        assertEquals( scanId, evaluation.getScanId() );
        assertEquals( user, evaluation.getUser() );
        assertTrue( System.currentTimeMillis() - evaluation.getTime() < 60 * 1000 );
    }

    @Test
    public void testMigrate()
        throws Exception
    {
        File legacyLog = new File( tmpDir.getRoot(), "policyevaluations.json" );
        FileUtils.copyURLToFile( getClass().getResource( "/PolicyEvaluationLogTest/policyevaluations.json" ), legacyLog );

        PolicyEvaluationLog log = new PolicyEvaluationLog( tmpDir.getRoot() );

        PolicyEvaluation eval = log.lastByStage( Stage.ID_BUILD );
        assertNotNull( eval );
        assertNotNull( eval.getStage() );
        assertEquals( Stage.ID_BUILD, eval.getStage().getStageTypeId() );
        assertEquals( "4ec4edff03b145e38b6915dda1d0b00f", eval.getScanId() );
        assertEquals( "John", eval.getUser() );

        eval = log.lastByStage( Stage.ID_RELEASE );
        assertNotNull( eval );
        assertNotNull( eval.getStage() );
        assertEquals( Stage.ID_RELEASE, eval.getStage().getStageTypeId() );
        assertEquals( "46969a0aa117487aa769b8c550095973", eval.getScanId() );
        assertEquals( "Jane", eval.getUser() );

        assertFalse( legacyLog.exists() );

        assertNull( log.lastByStage( Stage.ID_STAGE_RELEASE ) );
        assertNull( log.lastByStage( Stage.ID_OPERATE ) );
        assertNull( log.lastByStage( Stage.ID_PROCURE ) );
        assertNull( log.lastByStage( Stage.ID_DEVELOP ) );
    }

}
