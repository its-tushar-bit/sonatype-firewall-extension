/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.policy.actions.ActionTypes;
import com.sonatype.insight.brain.model.policy.notifications.PolicyNotification;
import com.sonatype.insight.brain.service.InsightMail;
import com.sonatype.insight.brain.utils.TemplateUtils;

import freemarker.template.Template;

/**
 * @since 1.21
 */
public abstract class AbstractPolicyAlertEmailer
{
  private static final String LIFECYCLE_POLICY_ALERT_TEMPLATE_NAME = "policythreats.ftl";

  private static final String SBOM_MANAGER_POLICY_ALERT_TEMPLATE_NAME = "sbom-manager-policythreats.ftl";

  private final InsightMail mail;

  private final PolicyAlertEmailResolver policyAlertEmailResolver;

  private static Template policyThreatsTemplate;

  public AbstractPolicyAlertEmailer(final InsightMail mail, final PolicyAlertEmailResolver policyAlertEmailResolver) {
    this.mail = mail;
    this.policyAlertEmailResolver = policyAlertEmailResolver;
  }

  protected Map<String, List<PolicyFact>> getPolicyFactsByEmailAddress(
      Owner owner,
      final List<PolicyNotification> policyNotifications)
  {
    return policyAlertEmailResolver.getPolicyFactsByEmailAddress(owner, policyNotifications);
  }

  protected String createPolicyMailSubject(PolicyAlertCounts counts, String ownerName, StageType stageType) {
    StringBuilder buffer = new StringBuilder(128);
    buffer.append("Policy Alert for ").append(ownerName);
    if (stageType != null) {
      buffer.append(" at stage ").append(stageType.getName());
    }
    buffer.append(": ");
    int highest = 0;
    if (counts.getRed() > 0) {
      buffer.append(highest = counts.getRed()).append(" critical");
    }
    else if (counts.getOrange() > 0) {
      buffer.append(highest = counts.getOrange()).append(" severe");
    }
    else if (counts.getYellow() > 0) {
      buffer.append(highest = counts.getYellow()).append(" moderate");
    }
    else if (counts.getBlue() > 0 || counts.getDarkBlue() > 0) {
      buffer.append(highest = counts.getBlue() + counts.getDarkBlue()).append(" neutral");
    }
    buffer.append(" violation").append(highest != 1 ? "s" : "");
    buffer.append(" out of ").append(counts.getTotal());
    return buffer.toString();
  }

  protected InsightMail getMail() {
    return mail;
  }

  protected String createPolicyMailBody(Map<String, Object> model) throws IOException {
    return TemplateUtils.render(getPolicyThreatsTemplate(LIFECYCLE_POLICY_ALERT_TEMPLATE_NAME), model);
  }

  protected String createPolicyMailBodyForSbomManager(Map<String, Object> model) throws IOException {
    return TemplateUtils.render(getPolicyThreatsTemplate(SBOM_MANAGER_POLICY_ALERT_TEMPLATE_NAME), model);
  }

  private static synchronized Template getPolicyThreatsTemplate(final String templateName) throws IOException {
    if (policyThreatsTemplate == null || !policyThreatsTemplate.getName().equals(templateName)) {
      policyThreatsTemplate = TemplateUtils.createFreemarkerConfig().getTemplate(templateName);
    }
    return policyThreatsTemplate;
  }

  protected Map<String, Object> createPolicyMailModel(
      final String cdnUrl,
      final Owner owner,
      final StageType stageType,
      final List<PolicyFact> policyFacts)
  {
    PolicyAlertCounts counts = new PolicyAlertCounts(policyFacts);

    final Map<String, Object> model = new HashMap<>();

    model.put("cdnUrl", cdnUrl);
    model.put("policyFacts", policyFacts);
    model.put("policyThreatStage", stageType.getName());
    model.put("policyThreatApp", owner.getPublicId());
    model.put("policyThreatTime", new SimpleDateFormat("MMMM dd, yyyy", Locale.ENGLISH).format(new Date()));
    model.put("policyThreatRedCount", counts.getRed());
    model.put("policyThreatOrangeCount", counts.getOrange());
    model.put("policyThreatYellowCount", counts.getYellow());
    model.put("policyThreatDarkBlueCount", counts.getDarkBlue());
    model.put("policyThreatBlueCount", counts.getBlue());
    model.put("actionTypes", ActionTypes.getAll());

    return model;
  }
}
