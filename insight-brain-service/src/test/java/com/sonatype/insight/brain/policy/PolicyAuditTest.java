/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sonatype.insight.brain.model.policy.Action;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.actions.WarnActionType;
import com.sonatype.insight.brain.model.policy.conditions.LicenseCategoryConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityConditionType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.json.store.JsonUtils;

public class PolicyAuditTest
{
    @org.junit.Rule
    public TemporaryFolder tempDir = new TemporaryFolder();

    @Test
    public void testPolicyAudit_Add()
        throws Exception
    {
        final File auditDir = tempDir.newFolder();
        final String user = "James Blond";
        final String ip = "1.2.3.4";
        final String where = "not here";

        final Policy policy = new Policy();
        policy.setId( "An id" );
        policy.setName( "A policy" );
        final Constraint constraint = new Constraint();
        constraint.setId( "Another id" );
        constraint.setName( "A constraint" );
        final List<Condition> conditions = new ArrayList<Condition>();
        Condition condition = new Condition();
        condition.setConditionTypeId( SecurityVulnerabilityConditionType.ID );
        condition.setOperator( "present" );
        conditions.add( condition );
        condition = new Condition();
        condition.setConditionTypeId( LicenseCategoryConditionType.ID );
        condition.setOperator( "is" );
        condition.setValue( "Copyleft" );
        conditions.add( condition );
        constraint.setConditions( conditions );
        constraint.setOperator( LogicalOperator.AND );
        policy.addConstraint( constraint );
        final Action action = new Action();
        action.setActionTypeId( WarnActionType.ID );
        final List<Action> actions = new ArrayList<Action>();
        actions.add( action );
        policy.setActions( BuildStageType.ID, actions );

        PolicyAudit.saveChange( auditDir, policy, user, ip, where );

        final ArrayNode auditData = JsonUtils.read( new File( auditDir, PolicyAudit.POLICY_AUDIT_FILENAME ) );
        Assert.assertNotNull( auditData );
        Assert.assertEquals( auditData.toString(), 1, auditData.size() );
        assertPolicyAuditData( user, ip, where, (ObjectNode) auditData.get( 0 ) );
    }

    private static void assertPolicyAuditData( final String user, final String ip, final String where,
                                               final ObjectNode actual )
    {
        Assert.assertEquals( user, actual.get( "user" ).asText() );
        Assert.assertEquals( ip, actual.get( "ip" ).asText() );
        Assert.assertEquals( where, actual.get( "where" ).asText() );
    }
}
