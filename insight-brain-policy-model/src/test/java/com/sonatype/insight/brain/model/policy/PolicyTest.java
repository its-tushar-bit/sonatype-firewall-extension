/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy;

import org.junit.Assert;
import org.junit.Test;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.actions.NotifyActionType;
import com.sonatype.insight.brain.model.policy.actions.WarnActionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityConditionType;

public class PolicyTest
{
    private String applicationId = "PolicyTest_AppId";

    @Test
    public void testValidate_NameNull()
    {
        Policy policy = new Policy();
        Constraint constraint = new Constraint( "Constraint Id", "Constraint Name", LogicalOperator.AND );
        constraint.addCondition( new Condition( SecurityVulnerabilityConditionType.ID, "present" ) );
        policy.addConstraint( constraint );
        ValidationResult result = policy.validate( applicationId );
        assertValidationResult( "The policy name must not be null or empty", result );
    }

    @Test
    public void testValidate_NameEmpty()
    {
        Policy policy = new Policy();
        Constraint constraint = new Constraint( "Constraint Id", "Constraint Name", LogicalOperator.AND );
        constraint.addCondition( new Condition( SecurityVulnerabilityConditionType.ID, "present" ) );
        policy.addConstraint( constraint );
        policy.setName( " " );
        ValidationResult result = policy.validate( applicationId );
        assertValidationResult( "The policy name must not be null or empty", result );
    }

    @Test
    public void testValidate_NoConstraints()
    {
        Policy policy = new Policy();
        policy.setName( "Policy Name" );
        ValidationResult result = policy.validate( applicationId );
        assertValidationResult( "Policy 'Policy Name' has no constraints", result );
    }

    @Test
    public void testValidate_ConstraintNameNull()
    {
        Policy policy = new Policy( "PolicyId", "Policy Name" );
        Constraint constraint = new Constraint( "Constraint Id", null /* name */, LogicalOperator.AND );
        constraint.addCondition( new Condition( SecurityVulnerabilityConditionType.ID, "present" ) );
        policy.addConstraint( constraint );
        ValidationResult result = policy.validate( applicationId );
        assertValidationResult( "The constraint name must not be null or empty", result );
    }

    @Test
    public void testValidate_ConstraintNameEmpty()
    {
        Policy policy = new Policy( "PolicyId", "Policy Name" );
        Constraint constraint = new Constraint( "Constraint Id", " " /* name */, LogicalOperator.AND );
        constraint.addCondition( new Condition( SecurityVulnerabilityConditionType.ID, "present" ) );
        policy.addConstraint( constraint );
        ValidationResult result = policy.validate( applicationId );
        assertValidationResult( "The constraint name must not be null or empty", result );
    }

    @Test
    public void testValidate_ConstraintNameDuplicate()
    {
        Policy policy = new Policy( "PolicyId", "Policy Name" );
        Constraint constraint1 = new Constraint( "Constraint Id 1", "Constraint Name", LogicalOperator.AND );
        constraint1.addCondition( new Condition( SecurityVulnerabilityConditionType.ID, "present" ) );
        policy.addConstraint( constraint1 );
        Constraint constraint2 = new Constraint( "Constraint Id 2", "Constraint Name", LogicalOperator.AND );
        constraint2.addCondition( new Condition( SecurityVulnerabilityConditionType.ID, "present" ) );
        policy.addConstraint( constraint2 );
        ValidationResult result = policy.validate( applicationId );
        assertValidationResult( "Duplicate constraint name 'Constraint Name'", result );
    }

    @Test
    public void testValidate_ConstraintNoConditions()
    {
        Policy policy = new Policy( "PolicyId", "Policy Name" );
        Constraint constraint = new Constraint( "Constraint Id", "Constraint Name", LogicalOperator.AND );
        policy.addConstraint( constraint );
        ValidationResult result = policy.validate( applicationId );
        assertValidationResult( "Constraint 'Constraint Name' has no conditions", result );
    }

    @Test
    public void testValidate_ConditionTypeIdNull()
    {
        Policy policy = new Policy( "PolicyId", "Policy Name" );
        Constraint constraint = new Constraint( "Constraint Id", "Constraint Name", LogicalOperator.AND );
        constraint.addCondition( new Condition( null /* conditionTypeId */, "present" ) );
        policy.addConstraint( constraint );
        ValidationResult result = policy.validate( applicationId );
        assertConditionValidationResult( "Invalid condition type id: 'null'", result );
    }

    @Test
    public void testValidate_ConditionTypeIdEmpty()
    {
        Policy policy = new Policy( "PolicyId", "Policy Name" );
        Constraint constraint = new Constraint( "Constraint Id", "Constraint Name", LogicalOperator.AND );
        constraint.addCondition( new Condition( " " /* conditionTypeId */, "present" ) );
        policy.addConstraint( constraint );
        ValidationResult result = policy.validate( applicationId );
        assertConditionValidationResult( "Invalid condition type id: ' '", result );
    }

    @Test
    public void testValidate_ConditionTypeIdInvalid()
    {
        Policy policy = new Policy( "PolicyId", "Policy Name" );
        Constraint constraint = new Constraint( "Constraint Id", "Constraint Name", LogicalOperator.AND );
        constraint.addCondition( new Condition( "abc" /* conditionTypeId */, "present" ) );
        policy.addConstraint( constraint );
        ValidationResult result = policy.validate( applicationId );
        assertConditionValidationResult( "Invalid condition type id: 'abc'", result );
    }

    @Test
    public void testValidate_ConditionNoOperator()
    {
        Policy policy = new Policy( "PolicyId", "Policy Name" );
        Constraint constraint = new Constraint( "Constraint Id", "Constraint Name", LogicalOperator.AND );
        constraint.addCondition( new Condition( SecurityVulnerabilityConditionType.ID, null /* operator */) );
        policy.addConstraint( constraint );
        ValidationResult result = policy.validate( applicationId );
        assertConditionValidationResult( "Invalid condition 'SecurityVulnerability null null', Operator is null", result );
    }

    @Test
    public void testValidate_NotifyActionType()
    {
        Policy policy = new Policy( "PolicyId", "Policy Name" );
        Constraint constraint = new Constraint( "Constraint Id", "Constraint Name", LogicalOperator.AND );
        constraint.addCondition( new Condition( SecurityVulnerabilityConditionType.ID, "present" ) );
        policy.addConstraint( constraint );
        Action action = new Action( NotifyActionType.ID );
        policy.addAction( FailActionType.ID, action );
        ValidationResult result = policy.validate( applicationId );
        assertValidationResult( "Invalid action 'Notify': A target is required", result );

        // Fix the action and validate again
        action.setTarget( "tester@sonatype.com" );
        result = policy.validate( applicationId );
        Assert.assertTrue( result.isValid() );
    }

    @Test
    public void testValidate_FailActionType()
    {
        Policy policy = new Policy( "PolicyId", "Policy Name" );
        Constraint constraint = new Constraint( "Constraint Id", "Constraint Name", LogicalOperator.AND );
        constraint.addCondition( new Condition( SecurityVulnerabilityConditionType.ID, "present" ) );
        policy.addConstraint( constraint );
        Action action = new Action( FailActionType.ID );
        action.setTarget( "abc" );
        policy.addAction( FailActionType.ID, action );
        ValidationResult result = policy.validate( applicationId );
        assertValidationResult( "Invalid action 'Fail': This action does not support targets", result );

        // Fix the action and validate again
        action.setTarget( null );
        result = policy.validate( applicationId );
        Assert.assertTrue( result.isValid() );
    }

    @Test
    public void testValidate_WarnActionType()
    {
        Policy policy = new Policy( "PolicyId", "Policy Name" );
        Constraint constraint = new Constraint( "Constraint Id", "Constraint Name", LogicalOperator.AND );
        constraint.addCondition( new Condition( SecurityVulnerabilityConditionType.ID, "present" ) );
        policy.addConstraint( constraint );
        Action action = new Action( WarnActionType.ID );
        action.setTarget( "abc" );
        policy.addAction( FailActionType.ID, action );
        ValidationResult result = policy.validate( applicationId );
        assertValidationResult( "Invalid action 'Warn': This action does not support targets", result );

        // Fix the action and validate again
        action.setTarget( null );
        result = policy.validate( applicationId );
        Assert.assertTrue( result.isValid() );
    }

    private void assertValidationResult( String error, ValidationResult result )
    {
        Assert.assertNotNull( result );
        Assert.assertFalse( result.isValid() );
        Assert.assertEquals( result.toMessageString(), 1, result.getErrors().size() );
        Assert.assertEquals( error, result.getErrors().get( 0 ) );
    }

    private void assertConditionValidationResult( String error, ValidationResult result )
    {
        Assert.assertNotNull( result );
        Assert.assertFalse( result.isValid() );
        Assert.assertEquals( 2, result.getErrors().size() );
        Assert.assertEquals( "Constraint 'Constraint Name' has invalid conditions:", result.getErrors().get( 0 ) );
        Assert.assertEquals( error, result.getErrors().get( 1 ) );
    }
}
