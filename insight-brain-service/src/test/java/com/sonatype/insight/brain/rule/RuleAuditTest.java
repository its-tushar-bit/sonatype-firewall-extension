/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.rule;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sonatype.insight.brain.data.JsonUtils;
import com.sonatype.insight.brain.model.rule.Action;
import com.sonatype.insight.brain.model.rule.LicenseCategoryConditionType;
import com.sonatype.insight.brain.model.rule.LogicalOperator;
import com.sonatype.insight.brain.model.rule.Rule;
import com.sonatype.insight.brain.model.rule.SecurityVulnerabilityPresentConditionType;
import com.sonatype.insight.brain.model.rule.SimpleCondition;

public class RuleAuditTest
{
    @org.junit.Rule
    public TemporaryFolder tempDir = new TemporaryFolder();

    @Test
    public void testRuleAudit_Add()
        throws Exception
    {
        final File auditDir = tempDir.newFolder();
        final String user = "James Blond";
        final String ip = "1.2.3.4";
        final String where = "not here";

        final Rule rule = new Rule();
        rule.setId( "An id" );
        rule.setName( "A rule" );
        final List<SimpleCondition> conditions = new ArrayList<SimpleCondition>();
        SimpleCondition condition = new SimpleCondition();
        condition.setConditionTypeId( SecurityVulnerabilityPresentConditionType.ID );
        condition.setOperator( "present" );
        conditions.add( condition );
        condition = new SimpleCondition();
        condition.setConditionTypeId( LicenseCategoryConditionType.ID );
        condition.setOperator( "is" );
        condition.setValue( "Copyleft" );
        conditions.add( condition );
        rule.setConditions( conditions );
        rule.setOperator( LogicalOperator.AND );
        final Action action = new Action();
        action.setValue( "be happy" );
        final List<Action> actions = new ArrayList<Action>();
        actions.add( action );
        rule.setActions( actions );

        RuleAudit.saveChange( auditDir, rule, user, ip, where );

        final ArrayNode auditData = JsonUtils.read( new File( auditDir, RuleAudit.RULE_AUDIT_FILENAME ) );
        Assert.assertNotNull( auditData );
        Assert.assertEquals( auditData.toString(), 1, auditData.size() );
        assertRuleAuditData( user, ip, where, (ObjectNode) auditData.get( 0 ) );
    }

    private static void assertRuleAuditData( final String user, final String ip, final String where,
                                             final ObjectNode actual )
    {
        Assert.assertEquals( user, actual.get( "user" ).asText() );
        Assert.assertEquals( ip, actual.get( "ip" ).asText() );
        Assert.assertEquals( where, actual.get( "where" ).asText() );
    }
}
