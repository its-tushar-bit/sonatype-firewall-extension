/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.jira;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.audit.AuditSession;
import com.sonatype.insight.brain.jira.JiraIssueCreateRequest.JiraIssueCreateResponse;
import com.sonatype.insight.brain.landing.UserInterfaceLinksResource;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.notifications.JiraNotification;
import com.sonatype.insight.brain.model.policy.notifications.PolicyNotification;
import com.sonatype.insight.brain.organization.ApplicationAdapter;
import com.sonatype.insight.brain.policy.evaluator.PolicyAlertCounts;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.security.UserDirectory;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.utils.TemplateUtils;
import com.sonatype.insight.license.model.LicensedFeature;

import freemarker.template.Configuration;
import freemarker.template.Template;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates JIRA issues for policy alert notifications.
 *
 * @since 1.21.0
 */
@Named
public class JiraPolicyAlertNotifier
{
  private static final Logger log = LoggerFactory.getLogger(JiraPolicyAlertNotifier.class);

  private final InsightConfig insightConfig;

  private final UserDirectory userDirectory;

  private final JiraService jiraService;

  private final Template descriptionTemplate;

  private final BaseUrl baseUrl;

  private final AuditRecorder auditRecorder;

  private final ProductLicense productLicense;

  @Inject
  public JiraPolicyAlertNotifier(
      final InsightConfig insightConfig,
      final UserDirectory userDirectory,
      final JiraService jiraService,
      final BaseUrl baseUrl,
      final AuditRecorder auditRecorder,
      final ProductLicense productLicense)
  {
    this.insightConfig = insightConfig;
    this.userDirectory = userDirectory;
    this.jiraService = jiraService;
    this.baseUrl = baseUrl;
    this.auditRecorder = auditRecorder;
    this.productLicense = productLicense;

    // resolve template used to render issue description
    try {
      Configuration config = TemplateUtils.createFreemarkerConfig();
      config.setClassForTemplateLoading(getClass(), "/" + getClass().getPackage().getName().replace('.', '/'));
      this.descriptionTemplate = config.getTemplate("description.ftl");
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public void sendNotifications(final Application app,
                                final String scanId,
                                final Stage stage,
                                final List<PolicyNotification> policyNotifications)
  {
    if (!productLicense.hasFeature(LicensedFeature.NOTIFICATIONS)) {
      log.debug("Not sending JIRA notifications for application {} and scan {} in stage {}" +
          ", license does not support notifications", app.getPublicId(), scanId, stage.getStageTypeId());
      return;
    }
    if (!jiraService.isEnabled()) {
      log.debug("JIRA integration is not enabled; skipping issue creation");
      return;
    }

    log.debug("Sending JIRA notifications for application: {}, scan: {}, stage: {}", app.getId(), scanId, stage);

    new Thread("PolicyAlertJIRANotifierForScan-" + scanId)
    {
      @Override
      public void run() {
        JiraConfig jiraConfig = insightConfig.getJiraConfig();
        Map<String, Object> customFields = jiraConfig.getCustomFields();

        Map<JiraNotification, List<PolicyFact>> policyFactsByJiraNotifications = getPolicyFactsByJiraNotifications(
            policyNotifications);

        if (policyFactsByJiraNotifications.isEmpty()) {
          log.debug("Not sending JIRA notifications for application {} and scan {} in stage {}"
              + ", no JIRA projects configured for any violated policy", app.getPublicId(), scanId, stage);
          return;
        }
        for (final Entry<JiraNotification, List<PolicyFact>> policyFactsByJiraNotification :
            policyFactsByJiraNotifications.entrySet()) {
          try (AuditSession session = auditRecorder.recordSystemEvent(AuditEvent.CREATE_JIRA_ISSUE)) {
            JiraNotification jiraNotification = policyFactsByJiraNotification.getKey();
            List<PolicyFact> policyFacts = policyFactsByJiraNotification.getValue();

            try {
              AuditData.get().setApplication(app).setScanId(scanId).setStageId(stage.getStageTypeId());
              JiraIssueCreateRequest request = new JiraIssueCreateRequest();

              // include optional fields; before we add more specific details
              if (customFields != null) {
                request.getFields().putAll(customFields);
              }

              request.project(jiraNotification.getProjectKey());
              request.issueType(jiraNotification.getIssueTypeId());

              final PolicyAlertCounts counts = new PolicyAlertCounts(policyFacts);

              AuditData.get().setData("jiraProjectKey", jiraNotification.getProjectKey())
                  .setData("jiraIssueTypeId", jiraNotification.getIssueTypeId())
                  .setData("totalPolicyViolationCount", counts.getTotal());

              request.summary(String
                  .format("Nexus IQ: Application %s; %s stage; %d Policy alerts", app.getName(), stage.getStageName(),
                      counts.getTotal()));

              // render description from template; prepare template parameters with appropriate details
              Map<String, Object> params = createPolicyMailModel(app, scanId, stage, counts, policyFacts);
              request.description(TemplateUtils.render(descriptionTemplate, params));

              log.debug("Creating JIRA issue: {}", request);
              JiraClient client = jiraService.client();
              JiraIssueCreateResponse response = client.createIssue(request);
              log.info("Created JIRA issue: {}", response.getKey());
            }
            catch (Exception e) {
              AuditData.get().setException(e);
              log.error(
                  "Failed to create JIRA notification for JIRA project key " + jiraNotification.getProjectKey() +
                    " and JIRA issue type id " + jiraNotification.getIssueTypeId() + ". Failed for application " +
                    app.getPublicId() + " and scan " + scanId + " in stage " + stage.getStageTypeId(), e);
            }
          }
        }
      }
    }.start();
  }

  // Visible for tests
  Map<String, Object> createPolicyMailModel(Application app,
                                                    String scanId,
                                                    Stage stage,
                                                    PolicyAlertCounts counts,
                                                    List<PolicyFact> policyFacts)
  {
    String stringBaseUrl = baseUrl.getConfigured();
    Map<String, Object> model = new HashMap<>();
    model.put("baseUrl", stringBaseUrl);
    model.put("app", app);
    model.put("scanId", scanId);
    model.put("stage", stage.getStageName());
    model.put("policyAlertSections", new PolicyAlertSections(policyFacts));
    model.put("policyAlertCounts", counts);
    model.put("contact", ApplicationAdapter.getInstance(userDirectory).getContact(app.getContactInternalName()));
    model.put("detailedReportUrl", stringBaseUrl + UserInterfaceLinksResource.getReportUrl(app.getPublicId(), scanId));

    return model;
  }

  private Map<JiraNotification, List<PolicyFact>> getPolicyFactsByJiraNotifications(
      List<PolicyNotification> policyNotifications)
  {
    final Map<JiraNotification, List<PolicyFact>> policyFactsByJiraNotifications = new HashMap<>();
    for (PolicyNotification policyNotification : policyNotifications) {
      PolicyFact policyFact = policyNotification.getPolicyFact();
      List<JiraNotification> jiraNotifications = policyNotification.getNotifications().getJiraNotifications();
      for (JiraNotification jiraNotification : jiraNotifications) {
        List<PolicyFact> policyFacts = policyFactsByJiraNotifications.get(jiraNotification);
        if (policyFacts == null) {
          policyFactsByJiraNotifications.put(jiraNotification, policyFacts = new ArrayList<>());
        }
        if (!policyFacts.contains(policyFact)) {
          policyFacts.add(policyFact);
        }
      }
    }
    return policyFactsByJiraNotifications;
  }

  /**
   * Representation of policy alert sections.
   *
   * Each section is a rollup of the alerts for a given policy.
   *
   * Must be public with getters for ftl access.
   */
  public static class PolicyAlertSections
  {
    public class Section
    {
      private final int threatLevel;

      private final String policyName;

      private final List<PolicyFact> facts = new ArrayList<>();

      public Section(final int threatLevel, final String policyName) {
        this.threatLevel = threatLevel;
        this.policyName = policyName;
      }

      public int getThreatLevel() {
        return threatLevel;
      }

      public String getPolicyName() {
        return policyName;
      }

      public List<PolicyFact> getFacts() {
        return facts;
      }

      public void add(final PolicyFact fact) {
        facts.add(fact);
      }
    }

    /**
     * Policy-id -> section map.
     *
     * For now relies on the input policies to be sorted, and will create sections sorted as well.
     */
    private final Map<String, Section> sections = new LinkedHashMap<>();

    public PolicyAlertSections(final List<PolicyFact> policyFacts) {
      for (PolicyFact policyFact : policyFacts) {
        Section section = sections.get(policyFact.getPolicyId());
        if (section == null) {
          section = new Section(policyFact.getThreatLevel(), policyFact.getPolicyName());
          sections.put(policyFact.getPolicyId(), section);
        }
        section.add(policyFact);
      }
    }

    public Collection<Section> getSections() {
      return sections.values();
    }
  }
}
