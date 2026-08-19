/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.audit.ApplicationCategoryAuditDTO;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.service.AuditTestSupport;
import com.sonatype.insight.brain.tag.PolicyTagResource;
import com.sonatype.insight.test.LogOutput;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static java.util.Arrays.asList;

@IqH2Test
class IqH2PolicyTagResourceAuditTest
    implements AuditTestSupport
{
  private IqTestContext ctx;

  private Organization org;

  private Policy policy;

  private User unauthorizedUser;

  private final TestLogOutput logOutput = new TestLogOutput(AuditRecorder.BASE_LOGGER_NAME);

  @BeforeEach
  void setUp() {
    logOutput.before();
    logOutput.clear();
    unauthorizedUser = ctx.tempEntity().newUser();
    org = ctx.tempEntity().newOrganization();
    policy = ctx.tempEntity().newPolicy(org.getId(), "policy");
  }

  @AfterEach
  void after() {
    logOutput.tearDown();
  }

  @Override
  public LogOutput getLogOutput() {
    return logOutput;
  }

  @Override
  public String getUnauthorizedUsername() {
    return unauthorizedUser.getUsername();
  }

  @Override
  public PolicyDAO getPolicyDAO() {
    return ctx.lookup(PolicyDAO.class);
  }

  private Consumer<HttpRequest> unauthorizedUser() {
    return httpRequest -> httpRequest.auth(unauthorizedUser);
  }

  @Test
  void testUpdatePolicyTags_Organization_InheritMatchingCategory() throws Exception {
    Tag exitingPolicyTag = ctx.tempEntity().newTag(org.getId(), "existingTag");
    ctx.tempEntity().newPolicyTag(policy.getId(), exitingPolicyTag.getId());

    List<Tag> tags = asList(exitingPolicyTag, ctx.tempEntity().newTag(org.getId(), "newTag"));
    updatePolicyTags(null, policy, org, tags);

    AuditDTO auditDTO = assertAuditLog(null);
    assertOrganizationData(auditDTO, org);
    assertPolicyTagUpdateAuditData(auditDTO, "matching-application-category");
    assertCustomObject(auditDTO, "applicationCategories", ApplicationCategoryAuditDTO.transcribe(tags));
  }

  @Test
  void testUpdatePolicyTags_Organization_InheritAll() throws Exception {
    updatePolicyTags(null, policy, org, new ArrayList<>());

    AuditDTO auditDTO = assertAuditLog(null);
    assertOrganizationData(auditDTO, org);
    assertPolicyTagUpdateAuditData(auditDTO, "all-children");
  }

  @Test
  void testUpdatePolicyTags_Organization_Unauthorized() throws Exception {
    updatePolicyTags(unauthorizedUser(), policy, org, new ArrayList<>());

    AuditDTO auditDTO = assertAuditLog("unauthorized");
    assertOrganizationData(auditDTO, org);
  }

  private void assertPolicyTagUpdateAuditData(final AuditDTO auditDTO, final String inheritanceScope) {
    assertCustomData(auditDTO, "policyId", policy.getId());
    assertCustomData(auditDTO, "policyName", policy.getName());
    assertCustomData(auditDTO, "inheritanceScope", inheritanceScope);
  }

  private AuditDTO assertAuditLog(final String error) {
    AuditDTO auditDTO = awaitLogEntries(AuditEvent.CONFIGURE_POLICY_INHERITANCE, 1).get(0);
    assertStandardData(auditDTO, AuditEvent.CONFIGURE_POLICY_INHERITANCE, error);
    return auditDTO;
  }

  private void updatePolicyTags(
      Consumer<HttpRequest> user,
      Policy policy,
      Owner owner,
      List<Tag> newTags) throws Exception
  {
    ctx.restRequest()
        .with(user)
        .path(PolicyTagResource.RESOURCE_PATH)
        .parameter(policy.getId(), owner.getType(), owner.getPublicId())
        .body(newTags)
        .put();
  }

  private static final class TestLogOutput
      extends LogOutput
  {
    TestLogOutput(String... loggerNames) {
      super(loggerNames);
    }

    void tearDown() {
      after();
    }
  }
}
