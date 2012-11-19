/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.rule;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.junit.Assert;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.sonatype.insight.brain.data.DataStore;
import com.sonatype.insight.brain.model.rule.Action;
import com.sonatype.insight.brain.model.rule.LicenseInListConditionType;
import com.sonatype.insight.brain.model.rule.LogicalOperator;
import com.sonatype.insight.brain.model.rule.Rule;
import com.sonatype.insight.brain.model.rule.SecurityVulnerabilityCountConditionType;
import com.sonatype.insight.brain.model.rule.SimpleCondition;

public class RuleAuditTest
{
    @org.junit.Rule
    public TemporaryFolder tempDir = new TemporaryFolder();

    @Test
    public void testRuleAudit_Add()
        throws Exception
    {
        File auditDir = tempDir.newFolder();
        String user = "James Blond";
        String ip = "1.2.3.4";
        String where = "not here";

        Rule rule = new Rule();
        rule.setId( "An id" );
        rule.setName( "A rule" );
        List<SimpleCondition> conditions = new ArrayList<SimpleCondition>();
        SimpleCondition condition = new SimpleCondition();
        condition.setConditionTypeId( SecurityVulnerabilityCountConditionType.ID );
        condition.setOperator( "<" );
        condition.setValue( "2" );
        conditions.add( condition );
        condition = new SimpleCondition();
        condition.setConditionTypeId( LicenseInListConditionType.ID );
        condition.setOperator( "in" );
        condition.setValue( "Apache-2.0" );
        conditions.add( condition );
        rule.setConditions( conditions );
        rule.setOperator( LogicalOperator.AND );
        Action action = new Action();
        action.setValue( "be happy" );
        List<Action> actions = new ArrayList<Action>();
        actions.add( action );
        rule.setActions( actions );

        RuleAudit.saveChange( auditDir, rule, user, ip, where );

        ArrayNode auditData = DataStore.loadData( new File( auditDir, RuleAudit.RULE_AUDIT_FILENAME ) );
        Assert.assertNotNull( auditData );
        Assert.assertEquals( auditData.toString(), 1, auditData.size() );
        assertRuleAuditData( user, ip, where, (ObjectNode) auditData.get( 0 ) );
    }

    private void assertRuleAuditData( String user, String ip, String where, ObjectNode actual )
    {
        Assert.assertEquals( user, actual.get( "user" ).asText() );
        Assert.assertEquals( ip, actual.get( "ip" ).asText() );
        Assert.assertEquals( where, actual.get( "where" ).asText() );
    }
}
