/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tag;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.audit.ApplicationCategoryAuditDTO;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.Before;
import org.junit.Test;

import static java.util.Arrays.asList;

public class PolicyTagResourceAuditTest
    extends AbstractAuditTest
{
  private Organization org;

  private Policy policy;

  @Before
  public void before() {
    org = tempEntity.newOrganization();
    policy = tempEntity.newPolicy(org.getId(), "policy");
  }

  @Test
  public void testUpdatePolicyTags_Organization_InheritMatchingCategory() throws Exception {
    Tag exitingPolicyTag = tempEntity.newTag(org.getId(), "existingTag");
    tempEntity.newPolicyTag(policy.getId(), exitingPolicyTag.getId());

    List<Tag> tags = asList(exitingPolicyTag, tempEntity.newTag(org.getId(), "newTag"));
    updatePolicyTags(null, policy, org, tags);

    AuditDTO auditDTO = assertAuditLog(null);
    assertOrganizationData(auditDTO, org);
    assertPolicyTagUpdateAuditData(auditDTO, "matching-application-category");
    assertCustomObject(auditDTO, "applicationCategories", ApplicationCategoryAuditDTO.transcribe(tags));
  }

  @Test
  public void testUpdatePolicyTags_Organization_InheritAll() throws Exception {
    updatePolicyTags(null, policy, org, new ArrayList<>());

    AuditDTO auditDTO = assertAuditLog(null);
    assertOrganizationData(auditDTO, org);
    assertPolicyTagUpdateAuditData(auditDTO, "all-children");
  }

  @Test
  public void testUpdatePolicyTags_Organization_Unauthorized() throws Exception {
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
    restRequest().with(user)
        .path(PolicyTagResource.RESOURCE_PATH)
        .parameter(policy.getId(), owner.getType(), owner.getPublicId())
        .body(newTags)
        .put();
  }
}
