/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.sonatype.insight.brain.model.rule.Action;
import com.sonatype.insight.brain.model.rule.LicenseCategoryConditionType;
import com.sonatype.insight.brain.model.rule.LogicalOperator;
import com.sonatype.insight.brain.model.rule.MarkAsFailedActionType;
import com.sonatype.insight.brain.model.rule.Rule;
import com.sonatype.insight.brain.model.rule.SecurityVulnerabilityPresentType;
import com.sonatype.insight.brain.model.rule.SimpleCondition;

public class DroolsGeneratorTest
{
    @Test
    public void testGenerate()
    {
        List<Rule> rules = new ArrayList<Rule>();
        Rule rule1 = new Rule();
        rule1.setId( "RuleId1" );
        rule1.setName( "Rule Name 1" );
        rule1.setOperator( LogicalOperator.AND );
        SimpleCondition condition1 = new SimpleCondition();
        condition1.setConditionTypeId( SecurityVulnerabilityPresentType.ID );
        condition1.setOperator( "present" );
        rule1.addCondition( condition1 );
        SimpleCondition condition2 = new SimpleCondition();
        condition2.setConditionTypeId( LicenseCategoryConditionType.ID );
        condition2.setOperator( "is" );
        condition2.setValue( "Copyleft" );
        rule1.addCondition( condition2 );
        Action action = new Action();
        action.setActionTypeId( MarkAsFailedActionType.ID );
        rule1.addAction( action );
        rules.add( rule1 );
        Rule rule2 = new Rule();
        rule2.setId( "RuleId2" );
        rule2.setName( "Rule Name 2" );
        rule2.setOperator( LogicalOperator.OR );
        condition1 = new SimpleCondition();
        condition1.setConditionTypeId( SecurityVulnerabilityPresentType.ID );
        condition1.setOperator( "present" );
        rule2.addCondition( condition1 );
        condition2 = new SimpleCondition();
        condition2.setConditionTypeId( LicenseCategoryConditionType.ID );
        condition2.setOperator( "is" );
        condition2.setValue( "Copyleft" );
        rule2.addCondition( condition2 );
        action = new Action();
        action.setActionTypeId( MarkAsFailedActionType.ID );
        rule2.addAction( action );
        rules.add( rule2 );

        DroolsGenerator generator = new DroolsGenerator();
        String droolsCode = generator.generate( rules );
        System.out.println( droolsCode );
        // TODO Add asserts - for now it's good if we get no exceptions :)
    }
}
