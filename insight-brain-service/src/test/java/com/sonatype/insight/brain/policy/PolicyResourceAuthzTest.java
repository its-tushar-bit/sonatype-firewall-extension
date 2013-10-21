/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import java.util.Collections;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityConditionType;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.test.RestAccess;

import com.ning.http.client.Response;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class PolicyResourceAuthzTest
    extends AbstractResourceAuthzTest
{
  private PolicyDAO policyDAO;

  private Policy newPolicy() {
    Policy policy = new Policy(null, "Policy " + tempEntity.uuid());
    Constraint constraint = new Constraint(null, "Test Constraint", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilityConditionType.ID, "present"));
    policy.addConstraint(constraint);
    return policy;
  }

  @Before
  public void init() {
    policyDAO = new PolicyDAO(brain.getWorkDir());
  }

  @After
  public void exit() {
    policyDAO.deleteByOwnerId(app.getId());
    policyDAO.deleteByOwnerId(org.getId());
  }

  @Test
  public void testGetPolicies() throws Exception {
    Role role = tempEntity.newRole(false, Permission.READ);
    tempEntity.newMembershipMapping(app.getId(), role.getId(), authorized.getUsername());

    String url = getRestUrl(PolicyResource.SERVICE_PATH, IdUtils.TYPE_APPLICATION, app.getPublicId());
    Response response = RestAccess.get(url, unauthorized.getUsername(), unauthorized.getPassword());
    assertResponseStatus(403, response);

    response = RestAccess.get(url, authorized.getUsername(), authorized.getPassword());
    assertResponseStatus(200, response);

    tempEntity.newMembershipMapping(org.getId(), role.getId(), authorized.getUsername());

    url = getRestUrl(PolicyResource.SERVICE_PATH, IdUtils.TYPE_ORGANIZATION, org.getId());
    response = RestAccess.get(url, unauthorized.getUsername(), unauthorized.getPassword());
    assertResponseStatus(403, response);

    response = RestAccess.get(url, authorized.getUsername(), authorized.getPassword());
    assertResponseStatus(200, response);
  }

  @Test
  public void testGetApplicablePolicies() throws Exception {
    Role role = tempEntity.newRole(false, Permission.READ);
    tempEntity.newMembershipMapping(app.getId(), role.getId(), authorized.getUsername());

    String url = getRestUrl(PolicyResource.SERVICE_PATH + "/applicable", IdUtils.TYPE_APPLICATION, app.getPublicId());
    Response response = RestAccess.get(url, unauthorized.getUsername(), unauthorized.getPassword());
    assertResponseStatus(403, response);

    response = RestAccess.get(url, authorized.getUsername(), authorized.getPassword());
    assertResponseStatus(200, response);

    tempEntity.newMembershipMapping(org.getId(), role.getId(), authorized.getUsername());

    url = getRestUrl(PolicyResource.SERVICE_PATH + "/applicable", IdUtils.TYPE_ORGANIZATION, org.getId());
    response = RestAccess.get(url, unauthorized.getUsername(), unauthorized.getPassword());
    assertResponseStatus(403, response);

    response = RestAccess.get(url, authorized.getUsername(), authorized.getPassword());
    assertResponseStatus(200, response);
  }

  @Test
  public void testAddPolicy() throws Exception {
    Role role = tempEntity.newRole(false, Permission.WRITE);
    tempEntity.newMembershipMapping(app.getId(), role.getId(), authorized.getUsername());

    Policy policy = newPolicy();
    String url = getRestUrl(PolicyResource.SERVICE_PATH, IdUtils.TYPE_APPLICATION, app.getPublicId());
    Response response = RestAccess.post(url, unauthorized.getUsername(), unauthorized.getPassword(), toJson(policy));
    assertResponseStatus(403, response);

    response = RestAccess.post(url, authorized.getUsername(), authorized.getPassword(), toJson(policy));
    assertResponseStatus(200, response);

    tempEntity.newMembershipMapping(org.getId(), role.getId(), authorized.getUsername());

    policy = newPolicy();
    url = getRestUrl(PolicyResource.SERVICE_PATH, IdUtils.TYPE_ORGANIZATION, org.getId());
    response = RestAccess.post(url, unauthorized.getUsername(), unauthorized.getPassword(), toJson(policy));
    assertResponseStatus(403, response);

    response = RestAccess.post(url, authorized.getUsername(), authorized.getPassword(), toJson(policy));
    assertResponseStatus(200, response);
  }

  @Test
  public void testUpdatePolicy() throws Exception {
    Role role = tempEntity.newRole(false, Permission.WRITE);
    tempEntity.newMembershipMapping(app.getId(), role.getId(), authorized.getUsername());

    Policy policy = newPolicy();
    policyDAO.insert(app.getId(), policy);
    String url = getRestUrl(PolicyResource.SERVICE_PATH, IdUtils.TYPE_APPLICATION, app.getPublicId());
    Response response = RestAccess.put(url, unauthorized.getUsername(), unauthorized.getPassword(), toJson(policy));
    assertResponseStatus(403, response);

    response = RestAccess.put(url, authorized.getUsername(), authorized.getPassword(), toJson(policy));
    assertResponseStatus(200, response);

    tempEntity.newMembershipMapping(org.getId(), role.getId(), authorized.getUsername());

    policy = newPolicy();
    policyDAO.insert(org.getId(), policy);
    url = getRestUrl(PolicyResource.SERVICE_PATH, IdUtils.TYPE_ORGANIZATION, org.getId());
    response = RestAccess.put(url, unauthorized.getUsername(), unauthorized.getPassword(), toJson(policy));
    assertResponseStatus(403, response);

    response = RestAccess.put(url, authorized.getUsername(), authorized.getPassword(), toJson(policy));
    assertResponseStatus(200, response);
  }

  @Test
  public void testDeletePolicy() throws Exception {
    Role role = tempEntity.newRole(false, Permission.WRITE);
    tempEntity.newMembershipMapping(app.getId(), role.getId(), authorized.getUsername());

    Policy policy = newPolicy();
    policyDAO.insert(app.getId(), policy);
    String url = getRestUrl(PolicyResource.SERVICE_PATH + "/{policyId}", IdUtils.TYPE_APPLICATION, app.getPublicId(),
        policy.getId());
    Response response = RestAccess.delete(url, unauthorized.getUsername(), unauthorized.getPassword());
    assertResponseStatus(403, response);

    response = RestAccess.delete(url, authorized.getUsername(), authorized.getPassword());
    assertResponseStatus(204, response);

    tempEntity.newMembershipMapping(org.getId(), role.getId(), authorized.getUsername());

    policy = newPolicy();
    policyDAO.insert(org.getId(), policy);
    url = getRestUrl(PolicyResource.SERVICE_PATH + "/{policyId}", IdUtils.TYPE_ORGANIZATION, org.getId(),
        policy.getId());
    response = RestAccess.delete(url, unauthorized.getUsername(), unauthorized.getPassword());
    assertResponseStatus(403, response);

    response = RestAccess.delete(url, authorized.getUsername(), authorized.getPassword());
    assertResponseStatus(204, response);
  }

  @Test
  public void testExportPolicies() throws Exception {
    Role role = tempEntity.newRole(false, Permission.READ);
    tempEntity.newMembershipMapping(app.getId(), role.getId(), authorized.getUsername());

    String url = getRestUrl(PolicyResource.SERVICE_PATH + "/export", IdUtils.TYPE_APPLICATION, app.getPublicId());
    Response response = RestAccess.get(url, unauthorized.getUsername(), unauthorized.getPassword());
    assertResponseStatus(403, response);

    response = RestAccess.get(url, authorized.getUsername(), authorized.getPassword());
    assertResponseStatus(200, response);
  }

  @Test
  public void testImportPolicies_ToExistingApp() throws Exception {
    Role role = tempEntity.newRole(false, Permission.WRITE);
    tempEntity.newMembershipMapping(app.getId(), role.getId(), authorized.getUsername());

    PolicyExportResult export = new PolicyExportResult();
    export.labels = Collections.emptyList();
    export.licenseThreatGroups = Collections.emptyList();
    export.licenseThreatGroupLicenses = Collections.emptyList();
    export.policies = Collections.emptyList();
    String json = toJson(export);

    String url = getRestUrl(PolicyResource.SERVICE_PATH + "/import", IdUtils.TYPE_APPLICATION, app.getPublicId());
    Response response = RestAccess.put(url, unauthorized.getUsername(), unauthorized.getPassword(), json);
    assertResponseStatus(403, response);

    response = RestAccess.put(url, authorized.getUsername(), authorized.getPassword(), json);
    assertResponseStatus(200, response);
  }

  @Test
  public void testImportPolicies_ToNewApp() throws Exception {
    Role role = tempEntity.newRole(false, Permission.WRITE);
    tempEntity.newMembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID, role.getId(), authorized.getUsername());

    PolicyExportResult export = new PolicyExportResult();
    export.labels = Collections.emptyList();
    export.licenseThreatGroups = Collections.emptyList();
    export.licenseThreatGroupLicenses = Collections.emptyList();
    export.policies = Collections.emptyList();
    String json = toJson(export);
    String publicId = tempEntity.uuid();

    String url = getRestUrl(PolicyResource.SERVICE_PATH + "/import", IdUtils.TYPE_APPLICATION, publicId);
    Response response = RestAccess.put(url, unauthorized.getUsername(), unauthorized.getPassword(), json);
    assertResponseStatus(403, response);

    response = RestAccess.put(url, authorized.getUsername(), authorized.getPassword(), json);
    assertResponseStatus(200, response);
    ApplicationDAO appDAO = new ApplicationDAO();
    appDAO.delete(appDAO.getByPublicIdNotNull(publicId));
  }

  @Test
  public void testImportPolicies_ToExistingOrg() throws Exception {
    Role role = tempEntity.newRole(false, Permission.WRITE);
    tempEntity.newMembershipMapping(org.getId(), role.getId(), authorized.getUsername());

    PolicyExportResult export = new PolicyExportResult();
    export.labels = Collections.emptyList();
    export.licenseThreatGroups = Collections.emptyList();
    export.licenseThreatGroupLicenses = Collections.emptyList();
    export.policies = Collections.emptyList();
    String json = toJson(export);
    new ApplicationDAO().delete(app);

    String url = getRestUrl(PolicyResource.SERVICE_PATH + "/import", IdUtils.TYPE_ORGANIZATION, org.getId());
    Response response = RestAccess.put(url, unauthorized.getUsername(), unauthorized.getPassword(), json);
    assertResponseStatus(403, response);

    response = RestAccess.put(url, authorized.getUsername(), authorized.getPassword(), json);
    assertResponseStatus(200, response);
  }
}
