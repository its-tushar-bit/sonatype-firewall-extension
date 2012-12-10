/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.drools.builder.KnowledgeBuilder;
import org.drools.builder.KnowledgeBuilderFactory;
import org.drools.builder.ResourceType;
import org.drools.io.ResourceFactory;
import org.junit.Assert;
import org.junit.Test;

import com.sonatype.insight.brain.model.policy.Action;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LicenseCategoryConditionType;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.MarkAsFailedActionType;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.SecurityVulnerabilityConditionType;
import com.sonatype.insight.brain.model.policy.SimpleCondition;

public class DroolsGeneratorTest
{
    @Test
    public void testGenerate()
    {
        final List<Constraint> constraints = new ArrayList<Constraint>();
        final Constraint constraint1 = new Constraint();
        constraint1.setId( "ConstraintId1" );
        constraint1.setName( "Constraint Name 1" );
        constraint1.setOperator( LogicalOperator.AND );
        SimpleCondition condition1 = new SimpleCondition();
        condition1.setConditionTypeId( SecurityVulnerabilityConditionType.ID );
        condition1.setOperator( "present" );
        constraint1.addCondition( condition1 );
        SimpleCondition condition2 = new SimpleCondition();
        condition2.setConditionTypeId( LicenseCategoryConditionType.ID );
        condition2.setOperator( "is" );
        condition2.setValue( "Copyleft" );
        constraint1.addCondition( condition2 );
        constraints.add( constraint1 );
        final Constraint constraint2 = new Constraint();
        constraint2.setId( "ConstraintId2" );
        constraint2.setName( "Constraint Name 2" );
        constraint2.setOperator( LogicalOperator.OR );
        condition1 = new SimpleCondition();
        condition1.setConditionTypeId( SecurityVulnerabilityConditionType.ID );
        condition1.setOperator( "absent" );
        constraint2.addCondition( condition1 );
        condition2 = new SimpleCondition();
        condition2.setConditionTypeId( LicenseCategoryConditionType.ID );
        condition2.setOperator( "is not" );
        condition2.setValue( "Weak Copyleft" );
        constraint2.addCondition( condition2 );
        constraints.add( constraint2 );

        final Policy policy = new Policy();
        policy.setId( "PolicyId1" );
        policy.setName( "Policy Name 1" );
        policy.setConstraints( constraints );
        Action action = new Action();
        action.setActionTypeId( MarkAsFailedActionType.ID );
        policy.addAction( action );

        final DroolsGenerator generator = new DroolsGenerator();
        final String droolsCode = generator.generate( Arrays.asList( policy ) );
        System.out.println( droolsCode );
        // TODO Add asserts - for now it's good if we get no exceptions :)

        final KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();
        // this will parse and compile in one step
        kbuilder.add( ResourceFactory.newReaderResource( new StringReader( droolsCode ) ), ResourceType.DRL );
        Assert.assertFalse( kbuilder.getErrors().toString(), kbuilder.hasErrors() );
    }

    // @Test
    // public void test()
    // {
    // final KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();
    // // this will parse and compile in one step
    // kbuilder.add( ResourceFactory.newFileResource( "c:/temp/test.drl" ), ResourceType.DRL );
    // Assert.assertFalse( kbuilder.getErrors().toString(), kbuilder.hasErrors() );
    // }
}
