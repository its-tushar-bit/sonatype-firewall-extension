/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.sonatype.insight.brain.model.rule.LogicalOperator;
import com.sonatype.insight.brain.model.rule.Rule;
import com.sonatype.insight.brain.model.rule.SimpleCondition;

public class RuleDAOTest
{
    @org.junit.Rule
    public TemporaryFolder tempDir = new TemporaryFolder();

    @Test
    public void testCRUD()
        throws Exception
    {
        File dataStoreDir = tempDir.newFolder( "RuleDAOTest" );
        RuleDAO ruleDAO = new RuleDAO( dataStoreDir );
        String applicationId = "RuleDAOTest_AppId";

        // Add a rule
        Rule rule1 = new Rule();
        rule1.setName( "RuleDAOTest new rule 1" );
        rule1.setOperator( LogicalOperator.OR );
        List<SimpleCondition> conditions = new ArrayList<SimpleCondition>();
        SimpleCondition condition = new SimpleCondition();
        // TODO condition.setConditionType( conditionType )
        condition.setOperator( "<" );
        condition.setValue( "5" );
        conditions.add( condition );
        rule1.setConditions( conditions );
        ruleDAO.insert( applicationId, rule1 );

        List<Rule> rules = ruleDAO.getByApplicationId( applicationId );
        Assert.assertNotNull( rules );
        Assert.assertEquals( 1, rules.size() );
        assertRule( rule1, rules.get( 0 ) );

        // Add another rule
        Rule rule2 = new Rule();
        rule2.setName( "RuleDAOTest new rule 2" );
        rule2.setOperator( LogicalOperator.OR );
        conditions = new ArrayList<SimpleCondition>();
        condition = new SimpleCondition();
        // TODO condition.setConditionType( conditionType )
        condition.setOperator( ">" );
        condition.setValue( "7" );
        conditions.add( condition );
        rule2.setConditions( conditions );
        ruleDAO.insert( applicationId, rule2 );

        rules = ruleDAO.getByApplicationId( applicationId );
        Assert.assertNotNull( rules );
        Assert.assertEquals( 2, rules.size() );
        assertRule( rule1, rules.get( 0 ) );
        assertRule( rule2, rules.get( 1 ) );

        // Update a rule
        rule1.setName( "RuleDAOTest updated rule 1" );
        ruleDAO.update( applicationId, rule1 );

        rules = ruleDAO.getByApplicationId( applicationId );
        Assert.assertNotNull( rules );
        Assert.assertEquals( 2, rules.size() );
        assertRule( rule1, rules.get( 0 ) );
        assertRule( rule2, rules.get( 1 ) );

        // Update another rule
        rule2.setName( "RuleDAOTest updated rule 2" );
        ruleDAO.update( applicationId, rule2 );

        rules = ruleDAO.getByApplicationId( applicationId );
        Assert.assertNotNull( rules );
        Assert.assertEquals( 2, rules.size() );
        assertRule( rule1, rules.get( 0 ) );
        assertRule( rule2, rules.get( 1 ) );

        // Delete a rule
        ruleDAO.delete( applicationId, rule1 );

        rules = ruleDAO.getByApplicationId( applicationId );
        Assert.assertNotNull( rules );
        Assert.assertEquals( 1, rules.size() );
        assertRule( rule2, rules.get( 0 ) );

        // Delete another rule
        ruleDAO.delete( applicationId, rule2 );

        rules = ruleDAO.getByApplicationId( applicationId );
        Assert.assertNotNull( rules );
        Assert.assertEquals( 0, rules.size() );
    }

    private void assertRule( Rule expected, Rule actual )
    {
        Assert.assertEquals( expected.getId(), actual.getId() );
        Assert.assertEquals( expected.getOperator(), actual.getOperator() );
        // TODO assert all fields
    }
}
