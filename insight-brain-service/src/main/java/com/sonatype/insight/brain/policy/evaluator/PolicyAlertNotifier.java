/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.landing.UserInterfaceLinksResource;
import com.sonatype.insight.brain.model.policy.actions.ActionTypes;
import com.sonatype.insight.brain.model.policy.actions.NotifyActionType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.service.InsightMail;
import com.sonatype.insight.brain.utils.TemplateUtils;

import org.sonatype.micromailer.Address;

import freemarker.template.Template;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class used to send (email) notifications for policy alerts.
 * 
 * @since 1.8
 */
@Named
public class PolicyAlertNotifier
{
  private static final Logger log = LoggerFactory.getLogger(PolicyAlertNotifier.class);

  private static Template policyThreatsTemplate;

  private final InsightMail mail;

  private final BaseUrl baseUrl;

  @Inject
  public PolicyAlertNotifier(InsightMail mail, BaseUrl baseUrl) {
    this.mail = mail;
    this.baseUrl = baseUrl;
  }

  /**
   * Sends notifications in case of a difference between the current and previous policy alerts for a given application
   * and stage.
   */
  public void sendNotifications(final String applicationPublicId, String appId, final String scanId, final Stage stage,
      final List<PolicyAlert> currentAlerts, final List<PolicyAlert> previousAlerts)
  {
    @SuppressWarnings("unchecked")
    List<PolicyAlert>[] digest = new List[] { currentAlerts, Collections.emptyList() };
    if (!previousAlerts.isEmpty()) {
      digest = PolicyAlertDigester.digestPolicyAlerts(currentAlerts, previousAlerts);
    }

    if (digest != null) {
      sendNotifications(applicationPublicId, appId, scanId, stage, digest[0]);
    }
  }

  private void sendNotifications(final String applicationPublicId, String appId, final String scanId, final Stage stage,
      final List<PolicyAlert> policyAlerts)
  {
    for (final Entry<String, List<PolicyAlert>> details : byRecipients(policyAlerts).entrySet()) {
      try {
        final String mailId = "SONATYPE-CLM-" + applicationPublicId + '-' + scanId;
        final List<Address> addresses = Arrays.asList(new Address(details.getKey()));
        final String subject = createPolicyMailSubject(new MailPolicyAlertCounts(details.getValue()));
        final String body = summarizeThreats(applicationPublicId, appId, scanId, stage, details.getValue());
        mail.sendHtml(mailId, addresses, subject, body);
      }
      catch (final Exception e) {
        log.error("Unable to send notification to: {}", details.getKey(), e);
      }
    }

    // TODO: notify about cleared policy alerts...
  }

  private static Map<String, List<PolicyAlert>> byRecipients(final List<PolicyAlert> alerts) {
    final Map<String, List<PolicyAlert>> byRecipients = new HashMap<String, List<PolicyAlert>>();
    for (final PolicyAlert alert : alerts) {
      for (final Action action : alert.getActions()) {
        if (NotifyActionType.ID.equals(action.getActionTypeId())) {
          final String address = action.getTarget();
          List<PolicyAlert> personalAlerts = byRecipients.get(address);
          if (personalAlerts == null) {
            byRecipients.put(address, personalAlerts = new ArrayList<PolicyAlert>());
          }
          if (!personalAlerts.contains(alert)) {
            personalAlerts.add(alert);
          }
        }
      }
    }
    return byRecipients;
  }

  static String createPolicyMailSubject(MailPolicyAlertCounts counts) {
    StringBuilder buffer = new StringBuilder(128);
    buffer.append("Policy Alert: ");
    int total = counts.red + counts.orange + counts.yellow + counts.darkBlue + counts.blue;
    int highest = 0;
    if (counts.red > 0) {
      buffer.append(highest = counts.red).append(" critical");
    }
    else if (counts.orange > 0) {
      buffer.append(highest = counts.orange).append(" severe");
    }
    else if (counts.yellow > 0) {
      buffer.append(highest = counts.yellow).append(" moderate");
    }
    else if (counts.blue > 0 || counts.darkBlue > 0) {
      buffer.append(highest = counts.blue + counts.darkBlue).append(" neutral");
    }
    buffer.append(" violation").append(highest != 1 ? "s" : "");
    buffer.append(" out of ").append(total);
    return buffer.toString();
  }

  private String summarizeThreats(final String applicationPublicId, final String appId, final String scanId,
      final Stage stage, final List<PolicyAlert> policyAlerts) throws IOException
  {
    final Map<String, Object> model = createPolicyMailModel(baseUrl.get(), mail.getCdnUrl(), applicationPublicId,
        scanId, stage, policyAlerts);
    return TemplateUtils.render(getPolicyThreatsTemplate(), model);
  }

  private synchronized static Template getPolicyThreatsTemplate() throws IOException {
    if (policyThreatsTemplate == null) {
      policyThreatsTemplate = TemplateUtils.createFreemarkerConfig().getTemplate("policythreats.ftl");
    }
    return policyThreatsTemplate;
  }

  static Map<String, Object> createPolicyMailModel(final String serverUrl, final String cdnUrl,
      final String applicationPublicId, final String scanId, final Stage stage, final List<PolicyAlert> policyAlerts)
  {
    MailPolicyAlertCounts counts = new MailPolicyAlertCounts(policyAlerts);

    Collections.sort(policyAlerts, new Comparator<PolicyAlert>()
    {
      @Override
      public int compare(PolicyAlert o1, PolicyAlert o2) {
        int t1 = o1.getTrigger().getThreatLevel();
        int t2 = o2.getTrigger().getThreatLevel();
        int r = t2 - t1;
        if (r == 0) {
          r = String.CASE_INSENSITIVE_ORDER.compare(o1.getTrigger().getPolicyName(), o2.getTrigger().getPolicyName());
        }
        return r;
      }
    });

    final Map<String, Object> model = new HashMap<String, Object>();

    model.put("cdnUrl", cdnUrl);
    model.put("detailedReportUrl", serverUrl + UserInterfaceLinksResource.getReportUrl(applicationPublicId, scanId));
    model.put("policyAlerts", policyAlerts);
    model.put("policyThreatStage", StageTypes.getById(stage.getStageTypeId()).getName());
    model.put("policyThreatApp", applicationPublicId);
    model.put("policyThreatTime", new SimpleDateFormat("MMMM dd, yyyy", Locale.ENGLISH).format(new Date()));
    model.put("policyThreatRedCount", counts.red);
    model.put("policyThreatOrangeCount", counts.orange);
    model.put("policyThreatYellowCount", counts.yellow);
    model.put("policyThreatDarkBlueCount", counts.darkBlue);
    model.put("policyThreatBlueCount", counts.blue);
    model.put("actionTypes", ActionTypes.getAll());

    return model;
  }

  static class MailPolicyAlertCounts
  {
    public int red, orange, yellow, darkBlue, blue;

    public MailPolicyAlertCounts(final int red, final int orange, final int yellow, final int darkBlue, final int blue)
    {
      this.red = red;
      this.orange = orange;
      this.yellow = yellow;
      this.darkBlue = darkBlue;
      this.blue = blue;
    }

    public MailPolicyAlertCounts(final List<PolicyAlert> alerts) {
      for (PolicyAlert alert : alerts) {
        int level = alert.getTrigger().getThreatLevel();
        int components = alert.getTrigger().getComponentFacts().size();

        if (level > 7) {
          red += components;
        }
        else if (level > 3) {
          orange += components;
        }
        else if (level > 1) {
          yellow += components;
        }
        else if (level == 1) {
          darkBlue += components;
        }
        else {
          blue += components;
        }
      }
    }
  }
}
