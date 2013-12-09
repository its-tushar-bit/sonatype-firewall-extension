/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityConditionType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;
import com.sonatype.insight.brain.utils.IdUtils;

import com.ning.http.client.Response;
import org.junit.Test;

public class PolicyWaiverResourceAuthzTest
    extends AbstractResourceAuthzTest
{
  @Test
  public void testAddPolicyWaiver() throws Exception {
    grantWritePermission(app.getId());

    PolicyWaiver waiver = new PolicyWaiver("hash", "policyId", null, "comment");

    String url = getRestUrl(PolicyWaiverResource.SERVICE_PATH, IdUtils.TYPE_APPLICATION, app.getPublicId());
    Response response = testAuthzPost(url, toJson(waiver));
    waiver = fromJson(response, PolicyWaiver.class);
    new PolicyWaiverDAO().delete(waiver);

    grantWritePermission(org.getId());
    waiver = new PolicyWaiver("hash", "policyId", null, "comment");

    url = getRestUrl(PolicyWaiverResource.SERVICE_PATH, IdUtils.TYPE_ORGANIZATION, org.getId());
    response = testAuthzPost(url, toJson(waiver));
    waiver = fromJson(response, PolicyWaiver.class);
    new PolicyWaiverDAO().delete(waiver);
  }

  @Test
  public void testDeletePolicyWaiver() throws Exception {
    grantWritePermission(app.getId());

    PolicyWaiver waiver = tempEntity.newWaiver("hash", "policyId", app.getId());

    String url = getRestUrl(PolicyWaiverResource.SERVICE_PATH + "/{waiverId}", IdUtils.TYPE_APPLICATION,
        app.getPublicId(), waiver.getId());
    testAuthzDelete(url);

    grantWritePermission(org.getId());
    waiver = tempEntity.newWaiver("hash", "policyId", org.getId());

    url = getRestUrl(PolicyWaiverResource.SERVICE_PATH + "/{waiverId}", IdUtils.TYPE_ORGANIZATION, org.getId(),
        waiver.getId());
    testAuthzDelete(url);
  }

  @Test
  public void testGetPolicyWaiversByHash() throws Exception {
    Role role = tempEntity.newRole(false, Permission.READ);
    tempEntity.newMembershipMapping(app.getId(), role.getId(), authorized.getUsername());

    String url = getRestUrl(PolicyWaiverResource.SERVICE_PATH + "/component/{hash}", IdUtils.TYPE_APPLICATION,
        app.getPublicId(), "hash");
    testAuthzGet(url);

    tempEntity.newMembershipMapping(org.getId(), role.getId(), authorized.getUsername());

    url = getRestUrl(PolicyWaiverResource.SERVICE_PATH + "/component/{hash}", IdUtils.TYPE_ORGANIZATION, org.getId(),
        "hash");
    testAuthzGet(url);
  }

  @Test
  public void testGetApplicableContexts() throws Exception {
    Role role = tempEntity.newRole(false, Permission.READ);
    tempEntity.newMembershipMapping(app.getId(), role.getId(), authorized.getUsername());

    Policy policy = new Policy(null, "Test Policy");
    Constraint constraint = new Constraint(null, "Test Constraint", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilityConditionType.ID, "present"));
    policy.addConstraint(constraint);
    PolicyDAO policyDAO = new PolicyDAO(brain.getWorkDir());
    policyDAO.insert(org.getId(), policy);
    try {
      String url = getRestUrl(PolicyWaiverResource.SERVICE_PATH + "/applicable/context/{policyId}",
          IdUtils.TYPE_APPLICATION, app.getPublicId(), policy.getId());
      testAuthzGet(url);
    }
    finally {
      policyDAO.delete(org.getId(), policy.getId());
    }
  }
}
