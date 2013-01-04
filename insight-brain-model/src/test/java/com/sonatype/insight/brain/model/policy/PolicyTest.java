/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy;

import org.junit.Assert;
import org.junit.Test;

import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityConditionType;

public class PolicyTest
{
    @Test
    public void testValidate_NameNull()
    {
        Policy policy = new Policy();
        Constraint constraint = new Constraint( "Constraint Id", "Constraint Name", LogicalOperator.AND );
        constraint.addCondition( new Condition( SecurityVulnerabilityConditionType.ID, "present" ) );
        policy.addConstraint( constraint );
        ValidationResult result = policy.validate();
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
        ValidationResult result = policy.validate();
        assertValidationResult( "The policy name must not be null or empty", result );
    }

    @Test
    public void testValidate_NoConstraints()
    {
        Policy policy = new Policy();
        policy.setName( "Policy Name" );
        ValidationResult result = policy.validate();
        assertValidationResult( "The 'Policy Name' policy does not have any constraints", result );
    }

    @Test
    public void testValidate_ConstraintNameNull()
    {
        Policy policy = new Policy( "PolicyId", "Policy Name" );
        Constraint constraint = new Constraint( "Constraint Id", null /* name */, LogicalOperator.AND );
        constraint.addCondition( new Condition( SecurityVulnerabilityConditionType.ID, "present" ) );
        policy.addConstraint( constraint );
        ValidationResult result = policy.validate();
        assertValidationResult( "The constraint name must not be null or empty", result );
    }

    @Test
    public void testValidate_ConstraintNameEmpty()
    {
        Policy policy = new Policy( "PolicyId", "Policy Name" );
        Constraint constraint = new Constraint( "Constraint Id", " " /* name */, LogicalOperator.AND );
        constraint.addCondition( new Condition( SecurityVulnerabilityConditionType.ID, "present" ) );
        policy.addConstraint( constraint );
        ValidationResult result = policy.validate();
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
        ValidationResult result = policy.validate();
        assertValidationResult( "Duplicate constraint name 'Constraint Name'", result );
    }

    @Test
    public void testValidate_ConstraintNoConditions()
    {
        Policy policy = new Policy( "PolicyId", "Policy Name" );
        Constraint constraint = new Constraint( "Constraint Id", "Constraint Name", LogicalOperator.AND );
        policy.addConstraint( constraint );
        ValidationResult result = policy.validate();
        assertValidationResult( "The 'Constraint Name' constraint does not have any conditions", result );
    }

    @Test
    public void testValidate_ConditionTypeIdNull()
    {
        Policy policy = new Policy( "PolicyId", "Policy Name" );
        Constraint constraint = new Constraint( "Constraint Id", "Constraint Name", LogicalOperator.AND );
        constraint.addCondition( new Condition( null /* conditionTypeId */, "present" ) );
        policy.addConstraint( constraint );
        ValidationResult result = policy.validate();
        assertValidationResult( "Invalid condition type id: 'null'", result );
    }

    @Test
    public void testValidate_ConditionTypeIdEmpty()
    {
        Policy policy = new Policy( "PolicyId", "Policy Name" );
        Constraint constraint = new Constraint( "Constraint Id", "Constraint Name", LogicalOperator.AND );
        constraint.addCondition( new Condition( " " /* conditionTypeId */, "present" ) );
        policy.addConstraint( constraint );
        ValidationResult result = policy.validate();
        assertValidationResult( "Invalid condition type id: ' '", result );
    }

    @Test
    public void testValidate_ConditionTypeIdInvalid()
    {
        Policy policy = new Policy( "PolicyId", "Policy Name" );
        Constraint constraint = new Constraint( "Constraint Id", "Constraint Name", LogicalOperator.AND );
        constraint.addCondition( new Condition( "abc" /* conditionTypeId */, "present" ) );
        policy.addConstraint( constraint );
        ValidationResult result = policy.validate();
        assertValidationResult( "Invalid condition type id: 'abc'", result );
    }

    @Test
    public void testValidate_ConditionNoOperator()
    {
        Policy policy = new Policy( "PolicyId", "Policy Name" );
        Constraint constraint = new Constraint( "Constraint Id", "Constraint Name", LogicalOperator.AND );
        constraint.addCondition( new Condition( SecurityVulnerabilityConditionType.ID, null /* operator */) );
        policy.addConstraint( constraint );
        ValidationResult result = policy.validate();
        assertValidationResult( "Invalid condition: Condition [conditionTypeId=SecurityVulnerability, operator=null, value=null]: Operator is null",
                                result );
    }

    private void assertValidationResult( String error, ValidationResult result )
    {
        Assert.assertNotNull( result );
        Assert.assertFalse( result.isValid() );
        Assert.assertEquals( 1, result.getErrors().size() );
        Assert.assertEquals( error, result.getErrors().get( 0 ) );
    }
}
