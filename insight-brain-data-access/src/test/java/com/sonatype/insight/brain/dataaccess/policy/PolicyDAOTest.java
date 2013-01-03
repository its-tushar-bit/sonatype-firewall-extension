/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import java.io.File;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.InvalidPolicyException;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityConditionType;

public class PolicyDAOTest
{
    @org.junit.Rule
    public TemporaryFolder tempDir = new TemporaryFolder();

    @Test
    public void testCRUD()
        throws Exception
    {
        final File dataStoreDir = tempDir.newFolder( "PolicyDAOTest" );
        final PolicyDAO policyDAO = new PolicyDAO( dataStoreDir );
        final String applicationId = "PolicyDAOTest_AppId";

        // Add a policy
        final Policy policy1 = new Policy();
        policy1.setName( "PolicyDAOTest new policy 1" );
        final Constraint constraint1 = new Constraint( null, "PolicyDAOTest new constraint 1", LogicalOperator.AND );
        constraint1.addCondition( new Condition( SecurityVulnerabilityConditionType.ID, "present" ) );
        policy1.addConstraint( constraint1 );
        policyDAO.insert( applicationId, policy1 );

        List<Policy> policies = policyDAO.getByApplicationId( applicationId );
        Assert.assertNotNull( policies );
        Assert.assertEquals( 1, policies.size() );
        assertPolicy( policy1, policies.get( 0 ) );

        // Add another policy
        final Policy policy2 = new Policy();
        policy2.setName( "PolicyDAOTest new policy 2" );
        final Constraint constraint2 = new Constraint( null, "PolicyDAOTest new constraint 2", LogicalOperator.AND );
        constraint2.addCondition( new Condition( SecurityVulnerabilityConditionType.ID, "present" ) );
        policy2.addConstraint( constraint2 );
        policyDAO.insert( applicationId, policy2 );

        policies = policyDAO.getByApplicationId( applicationId );
        Assert.assertNotNull( policies );
        Assert.assertEquals( 2, policies.size() );
        assertPolicy( policy1, policies.get( 0 ) );
        assertPolicy( policy2, policies.get( 1 ) );

        // Update a policy
        policy1.setName( "PolicyDAOTest updated policy 1" );
        policyDAO.update( applicationId, policy1 );

        policies = policyDAO.getByApplicationId( applicationId );
        Assert.assertNotNull( policies );
        Assert.assertEquals( 2, policies.size() );
        assertPolicy( policy1, policies.get( 0 ) );
        assertPolicy( policy2, policies.get( 1 ) );

        // Update another policy
        policy2.setName( "PolicyDAOTest updated policy 2" );
        policyDAO.update( applicationId, policy2 );

        policies = policyDAO.getByApplicationId( applicationId );
        Assert.assertNotNull( policies );
        Assert.assertEquals( 2, policies.size() );
        assertPolicy( policy1, policies.get( 0 ) );
        assertPolicy( policy2, policies.get( 1 ) );

        // Delete a policy
        policyDAO.delete( applicationId, policy1.getId() );

        policies = policyDAO.getByApplicationId( applicationId );
        Assert.assertNotNull( policies );
        Assert.assertEquals( 1, policies.size() );
        assertPolicy( policy2, policies.get( 0 ) );

        // Delete another policy
        policyDAO.delete( applicationId, policy2.getId() );

        policies = policyDAO.getByApplicationId( applicationId );
        Assert.assertNotNull( policies );
        Assert.assertEquals( 0, policies.size() );
    }

    @Test
    public void testValidateOnInsert()
        throws Exception
    {
        File dataStoreDir = tempDir.newFolder( "PolicyDAOTest" );
        PolicyDAO policyDAO = new PolicyDAO( dataStoreDir );
        String applicationId = "PolicyDAOTest_AppId";

        // Policy without name
        Policy policy = new Policy();
        Constraint constraint1 = new Constraint( "Constraint Id", "Constraint Name", LogicalOperator.AND );
        constraint1.addCondition( new Condition( SecurityVulnerabilityConditionType.ID, "present" ) );
        policy.addConstraint( constraint1 );
        try
        {
            policyDAO.insert( applicationId, policy );
            Assert.fail( "Expected InvalidPolicyException" );
        }
        catch ( InvalidPolicyException expected )
        {
        }
    }

    @Test
    public void testValidateOnUpdate()
        throws Exception
    {
        File dataStoreDir = tempDir.newFolder( "PolicyDAOTest" );
        PolicyDAO policyDAO = new PolicyDAO( dataStoreDir );
        String applicationId = "PolicyDAOTest_AppId";

        // Add a policy
        Policy policy = new Policy();
        policy.setName( "PolicyDAOTest Policy Name" );
        Constraint constraint1 = new Constraint( "Constraint Id", "Constraint Name", LogicalOperator.AND );
        constraint1.addCondition( new Condition( SecurityVulnerabilityConditionType.ID, "present" ) );
        policy.addConstraint( constraint1 );
        policyDAO.insert( applicationId, policy );

        // Update the policy
        policy.setName( null );
        try
        {
            policyDAO.update( applicationId, policy );
            Assert.fail( "Expected InvalidPolicyException" );
        }
        catch ( InvalidPolicyException expected )
        {
        }
    }

    private static void assertPolicy( final Policy expected, final Policy actual )
    {
        Assert.assertEquals( expected.getId(), actual.getId() );
        Assert.assertEquals( expected.getConstraints().get( 0 ).getOperator(),
                             actual.getConstraints().get( 0 ).getOperator() );
        // TODO assert all fields
    }
}
