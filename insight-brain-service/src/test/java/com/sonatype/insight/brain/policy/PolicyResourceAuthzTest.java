/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import java.util.Collections;

import com.sonatype.insight.brain.AuthedRestAccess;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityConditionType;
import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.test.RestAccess;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;
import com.ning.http.multipart.ByteArrayPartSource;
import com.ning.http.multipart.FilePart;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class PolicyResourceAuthzTest
    extends AbstractResourceAuthzTest
{
  private Policy newPolicy() {
    Policy policy = new Policy(null, "Policy " + tempEntity.uuid());
    Constraint constraint = new Constraint(null, "Test Constraint", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilityConditionType.ID, "present"));
    policy.addConstraint(constraint);
    return policy;
  }

  @Test
  public void testGetPolicies() throws Exception {
    grantReadPermission(app.getId());

    String url = getRestUrl(PolicyResource.SERVICE_PATH, IdUtils.TYPE_APPLICATION, app.getPublicId());
    testAuthzGet(url);

    grantReadPermission(org.getId());

    url = getRestUrl(PolicyResource.SERVICE_PATH, IdUtils.TYPE_ORGANIZATION, org.getId());
    testAuthzGet(url);
  }

  @Test
  public void testGetApplicablePolicies() throws Exception {
    grantReadPermission(app.getId());

    String url = getRestUrl(PolicyResource.SERVICE_PATH + "/applicable", IdUtils.TYPE_APPLICATION, app.getPublicId());
    testAuthzGet(url);

    grantReadPermission(org.getId());

    url = getRestUrl(PolicyResource.SERVICE_PATH + "/applicable", IdUtils.TYPE_ORGANIZATION, org.getId());
    testAuthzGet(url);
  }

  @Test
  public void testAddPolicy() throws Exception {
    grantWritePermission(app.getId());

    Policy policy = newPolicy();
    String url = getRestUrl(PolicyResource.SERVICE_PATH, IdUtils.TYPE_APPLICATION, app.getPublicId());
    testAuthzPost(url, toJson(policy));

    grantWritePermission(org.getId());

    policy = newPolicy();
    url = getRestUrl(PolicyResource.SERVICE_PATH, IdUtils.TYPE_ORGANIZATION, org.getId());
    testAuthzPost(url, toJson(policy));
  }

  @Test
  public void testUpdatePolicy() throws Exception {
    grantWritePermission(app.getId());

    Policy policy = tempEntity.newPolicy(app.getId(), "testUpdatePolicy app");
    String url = getRestUrl(PolicyResource.SERVICE_PATH, IdUtils.TYPE_APPLICATION, app.getPublicId());
    testAuthzPut(url, toJson(policy));

    grantWritePermission(org.getId());

    policy = tempEntity.newPolicy(org.getId(), "testUpdatePolicy org");
    url = getRestUrl(PolicyResource.SERVICE_PATH, IdUtils.TYPE_ORGANIZATION, org.getId());
    testAuthzPut(url, toJson(policy));
  }

  @Test
  public void testDeletePolicy() throws Exception {
    grantWritePermission(app.getId());

    Policy policy = tempEntity.newPolicy(app.getId(), "testDeletePolicy");
    String url = getRestUrl(PolicyResource.SERVICE_PATH + "/{policyId}", IdUtils.TYPE_APPLICATION, app.getPublicId(),
        policy.getId());
    testAuthzDelete(url);

    grantWritePermission(org.getId());

    policy = tempEntity.newPolicy(org.getId(), "testDeletePolicy");
    url = getRestUrl(PolicyResource.SERVICE_PATH + "/{policyId}", IdUtils.TYPE_ORGANIZATION, org.getId(),
        policy.getId());
    testAuthzDelete(url);
  }

  @Test
  public void testExportPolicies() throws Exception {
    grantReadPermission(app.getId());

    String url = getRestUrl(PolicyResource.SERVICE_PATH + "/export", IdUtils.TYPE_APPLICATION, app.getPublicId());
    testAuthzGet(url);
  }

  @Test
  public void testImportPolicies_ToExistingApp() throws Exception {
    grantWritePermission(app.getId());

    PolicyExportResult export = new PolicyExportResult();
    export.labels = Collections.emptyList();
    export.licenseThreatGroups = Collections.emptyList();
    export.licenseThreatGroupLicenses = Collections.emptyList();
    export.policies = Collections.emptyList();

    String url = getRestUrl(PolicyResource.SERVICE_PATH + "/import", IdUtils.TYPE_APPLICATION, app.getPublicId());
    testAuthzPut(url, toJson(export));
  }

  @Test
  public void testImportPolicies_ToExistingOrg() throws Exception {
    grantWritePermission(org.getId());

    PolicyExportResult export = new PolicyExportResult();
    export.labels = Collections.emptyList();
    export.licenseThreatGroups = Collections.emptyList();
    export.licenseThreatGroupLicenses = Collections.emptyList();
    export.policies = Collections.emptyList();
    new ApplicationDAO().delete(app);

    String url = getRestUrl(PolicyResource.SERVICE_PATH + "/import", IdUtils.TYPE_ORGANIZATION, org.getId());
    testAuthzPut(url, toJson(export));
  }

  @Test
  public void testImportPolicies_IE9() throws Exception {
    grantWritePermission(org.getId());

    PolicyExportResult export = new PolicyExportResult();
    export.labels = Collections.emptyList();
    export.licenseThreatGroups = Collections.emptyList();
    export.licenseThreatGroupLicenses = Collections.emptyList();
    export.policies = Collections.emptyList();
    new ApplicationDAO().delete(app);

    String url = getRestUrl(PolicyResource.SERVICE_PATH + "/import/ie", IdUtils.TYPE_ORGANIZATION, org.getId());

    byte[] policyArray = toJson(export).getBytes();
    AsyncHttpClient.BoundRequestBuilder builder = AuthedRestAccess.getClient().preparePost(url);
    builder.addBodyPart(new FilePart("file", new ByteArrayPartSource("file", policyArray)));
    RestAccess.addAuthorization(builder, unauthorized.getUsername(), unauthorized.getPassword());
    Response response = builder.execute().get();
    assertResponseStatus(200, response);
    assertThat(response.getResponseBody(), is("Insufficient permissions"));

    builder = AuthedRestAccess.getClient().preparePost(url);
    builder.addBodyPart(new FilePart("file", new ByteArrayPartSource("file", policyArray)));
    RestAccess.addAuthorization(builder, authorized.getUsername(), authorized.getPassword());
    response = builder.execute().get();
    assertResponseStatus(200, response);
    assertThat(response.getResponseBody(), is(""));
  }
}
