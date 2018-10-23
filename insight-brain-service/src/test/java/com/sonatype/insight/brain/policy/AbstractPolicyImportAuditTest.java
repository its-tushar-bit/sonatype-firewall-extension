/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import java.util.Collections;
import java.util.UUID;

import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.conditions.ConditionTypes;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.service.AbstractAuditTest;

public abstract class AbstractPolicyImportAuditTest
    extends AbstractAuditTest
{
  protected Policy policy() {
    Policy policy = new Policy();
    policy.setName(UUID.randomUUID().toString());
    Constraint constraint = new Constraint();
    constraint.setName("constraintName");
    Condition condition = new Condition(ConditionTypes.MatchStateConditionType.getId(), "is", "exact");
    constraint.setConditions(Collections.singletonList(condition));
    policy.setConstraints(Collections.singletonList(constraint));
    return policy;
  }

  protected Label label() {
    Label label = new Label();
    label.setLabel(UUID.randomUUID().toString());
    return label;
  }

  protected LicenseThreatGroup licenseThreatGroup() {
    LicenseThreatGroup licenseThreatGroup = new LicenseThreatGroup();
    licenseThreatGroup.setName(UUID.randomUUID().toString());
    return licenseThreatGroup;
  }

  protected Tag tag() {
    Tag tag = new Tag();
    tag.setName("tagName");
    tag.setDescription("tagDescription");
    return tag;
  }

  protected void assertPolicyImportData(AuditDTO auditDTO,
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
