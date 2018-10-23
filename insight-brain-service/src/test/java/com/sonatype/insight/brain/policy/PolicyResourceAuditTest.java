/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.conditions.ConditionTypes;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.Before;
import org.junit.Test;

public class PolicyResourceAuditTest
    extends AbstractAuditTest
{
  private Organization organization;

  @Before
  public void before() {
    organization = tempEntity.newOrganization();
  }

  @Test
  public void testImportPolicies() throws Exception {
    PolicyExportResult policyExportResult = new PolicyExportResult();
    policyExportResult.policies = Arrays.asList(policy(), policy());
    policyExportResult.labels = Arrays.asList(label(), label(), label());
    policyExportResult.licenseThreatGroups = Collections.singletonList(licenseThreatGroup());
    policyExportResult.tags = Arrays.asList(tag(), tag(), tag(), tag());

    restRequest(OwnerType.ORGANIZATION, organization.getId()).path("import").part("file", "file", policyExportResult)
        .post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.IMPORT, null);
    assertOrganizationData(auditDTO, organization);
    assertPolicyImportData(auditDTO, 2, 3, 1, 4);
  }

  @Test
  public void testImportPolicies_Unauthorized() throws Exception {
    PolicyExportResult policyExportResult = new PolicyExportResult();
    policyExportResult.policies = Collections.singletonList(policy());

    restRequest(OwnerType.ORGANIZATION, organization.getId())
        .auth(unauthorizedUser.getUsername(), unauthorizedUser.getPassword()).path("import")
        .part("file", "file", policyExportResult).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.IMPORT, "unauthorized");
    assertOrganizationData(auditDTO, organization);
    assertPolicyImportData(auditDTO, null, null, null, null);
  }

  private Policy policy() {
    Policy policy = new Policy();
    policy.setName(UUID.randomUUID().toString());
    Constraint constraint = new Constraint();
    constraint.setName("constraintName");
    Condition condition = new Condition(ConditionTypes.MatchStateConditionType.getId(), "is", "exact");
    constraint.setConditions(Collections.singletonList(condition));
    policy.setConstraints(Collections.singletonList(constraint));
    return policy;
  }

  private Label label() {
    Label label = new Label();
    label.setLabel(UUID.randomUUID().toString());
    return label;
  }

  private LicenseThreatGroup licenseThreatGroup() {
    LicenseThreatGroup licenseThreatGroup = new LicenseThreatGroup();
    licenseThreatGroup.setName(UUID.randomUUID().toString());
    return licenseThreatGroup;
  }

  private Tag tag() {
    Tag tag = new Tag();
    tag.setName("tagName");
    tag.setDescription("tagDescription");
    return tag;
  }

  private HttpRequest restRequest(OwnerType ownerType, String ownerId) {
    return restRequest().path(PolicyResource.RESOURCE_PATH).parameter(ownerType, ownerId);
  }

  private AuditDTO assertAuditLog(AuditEvent auditEvent, String error) {
    AuditDTO auditDTO = awaitLogEntries(auditEvent, 1).get(0);
    assertStandardData(auditDTO, auditEvent, error);
    return auditDTO;
  }

  private void assertPolicyImportData(AuditDTO auditDTO,
                                      Integer policyCount,
                                      Integer componentLabelCount,
                                      Integer licenseThreatGroupCount,
                                      Integer applicationCategoryCount)
  {
    assertCustomData(auditDTO, "policyCount", policyCount);
    assertCustomData(auditDTO, "componentLabelCount", componentLabelCount);
    assertCustomData(auditDTO, "licenseThreatGroupCount", licenseThreatGroupCount);
    assertCustomData(auditDTO, "applicationCategoryCount", applicationCategoryCount);
  }
}
