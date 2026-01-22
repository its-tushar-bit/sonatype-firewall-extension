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
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.audit.AuditSession;
import com.sonatype.insight.brain.jira.JiraIssueCreateRequest.JiraIssueCreateResponse;
import com.sonatype.insight.brain.landing.UserInterfaceLinksHelper;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.jira.JiraConfiguration;
import com.sonatype.insight.brain.model.policy.notifications.JiraNotification;
import com.sonatype.insight.brain.model.policy.notifications.PolicyNotification;
import com.sonatype.insight.brain.organization.ApplicationContactLoader;
import com.sonatype.insight.brain.organization.ContactDTO;
import com.sonatype.insight.brain.policy.evaluator.PolicyAlertCounts;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.security.UserDirectory;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.shutdown.ShutdownPriority;
import com.sonatype.insight.brain.tenancy.TenantAwareOneTimeRunnable;
import com.sonatype.insight.brain.utils.TemplateUtils;
import com.sonatype.insight.license.model.LicensedFeature;

import freemarker.template.Configuration;
import freemarker.template.Template;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates JIRA issues for policy alert notifications.
 * This class is only for internal Jira integration. Jira Cloud uses Webhooks.
 * @since 1.21.0
 */
@Named
public class JiraPolicyAlertNotifier
{
  private static final Logger log = LoggerFactory.getLogger(JiraPolicyAlertNotifier.class);

  private final ADFBuilder adfBuilder;

  private final UserDirectory userDirectory;

  private final JiraService jiraService;

  private final Template descriptionTemplate;

  private final BaseUrl baseUrl;

  private final AuditRecorder auditRecorder;

  private final ProductLicense productLicense;

  private final ShutdownHandler shutdownHandler;

  private Boolean cloudDeployment;

  @Inject
  public JiraPolicyAlertNotifier(
      final UserDirectory userDirectory,
      final JiraService jiraService,
      final BaseUrl baseUrl,
      final AuditRecorder auditRecorder,
      final ProductLicense productLicense,
      final ShutdownHandler shutdownHandler)
  {
    this.userDirectory = userDirectory;
    this.jiraService = jiraService;
    this.baseUrl = baseUrl;
    this.auditRecorder = auditRecorder;
    this.productLicense = productLicense;
    this.shutdownHandler = shutdownHandler;

    // resolve template used to render issue description
    try {
      Configuration config = TemplateUtils.createFreemarkerConfig();
      config.setClassForTemplateLoading(getClass(), "/" + getClass().getPackage().getName().replace('.', '/'));
      this.descriptionTemplate = config.getTemplate("description.ftl");
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    adfBuilder = new ADFBuilder(baseUrl);
  }

  public void sendNotifications(
      final Application app,
      final String scanId,
      final Stage stage,
      final List<PolicyNotification> policyNotifications)
  {
    String appId = app.getPublicId();

    if (!productLicense.hasFeature(LicensedFeature.NOTIFICATIONS)) {
      log.debug("Not sending internal JIRA notifications for application {} and scan {} in stage {}" +
          ", license does not support notifications.", appId, scanId, stage.getStageTypeId());
      return;
    }
    JiraConfiguration jiraConfiguration = jiraService.getConfiguration();
    if (jiraConfiguration == null) {
      log.debug("Internal JIRA integration is not enabled; skipping issue creation.");
      return;
    }

    log.debug("Sending Internal JIRA notifications for application: {}, scan: {}, stage: {}.", appId, scanId, stage);

    Thread jiraNotificationThread = new Thread(new TenantAwareOneTimeRunnable(() -> {
      Map<String, Object> customFields = jiraConfiguration.getCustomFields();

      Map<JiraNotification, List<PolicyFact>> policyFactsByJiraNotifications = getPolicyFactsByJiraNotifications(
          policyNotifications);

      if (policyFactsByJiraNotifications.isEmpty()) {
        log.debug("Not sending Internal JIRA notifications for application {} and scan {} in stage {}"
            + ", no JIRA projects configured for any violated policy.", appId, scanId, stage);
        return;
      }

      ContactDTO appContact =
          ApplicationContactLoader.getInstance(userDirectory).getContact(app.getContactInternalName());
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
            request.description(
                createDescription(app, appContact, scanId, stage, counts, policyFacts,
                    isCloudDeployment(jiraConfiguration)));

            log.debug("Creating Internal JIRA issue: {}.", request);
            JiraClient client = jiraService.client(jiraConfiguration);
            JiraIssueCreateResponse response = client.createIssue(request, isCloudDeployment(jiraConfiguration));
            log.info("Created Internal JIRA issue: {}.", response.getKey());
          }
          catch (Exception e) {
            AuditData.get().setException(e);
            log.error(
                "Failed to create notification for Internal JIRA project key " + jiraNotification.getProjectKey() +
                    " and issue type id " + jiraNotification.getIssueTypeId() + ". Failed for application " +
                        appId + " and scan " + scanId + " in stage " + stage.getStageTypeId() + ".", e);
          }
        }
      }
    }), "PolicyAlertJIRANotifierForScan-" + scanId);
    shutdownHandler.add(jiraNotificationThread, ShutdownPriority.NOTIFICATIONS);
    jiraNotificationThread.start();
  }

  private Object createDescription(
      final Application app,
      final ContactDTO appContact,
      final String scanId,
      final Stage stage,
      final PolicyAlertCounts counts,
      final List<PolicyFact> policyFacts,
      final boolean cloudDeployment) throws IOException
  {
    if (cloudDeployment) {
      return adfBuilder.createDescription(app, appContact, scanId, stage, counts, policyFacts);
    }
    // render description from template; prepare template parameters with appropriate details
    Map<String, Object> params = createPolicyMailModel(app, appContact, scanId, stage, counts, policyFacts);
    return TemplateUtils.render(descriptionTemplate, params);
  }

  // Visible for tests
  Map<String, Object> createPolicyMailModel(
      Application app,
      ContactDTO appContact,
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
    model.put("contact", appContact);
    model.put("detailedReportUrl", stringBaseUrl + UserInterfaceLinksHelper.getReportUrl(app.getPublicId(), scanId));

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

  private boolean isCloudDeployment(JiraConfiguration jiraConfiguration) throws IOException {
    if (cloudDeployment == null) {
      JiraClient client = jiraService.client(jiraConfiguration);
      cloudDeployment = client.isCloudDeployment();
    }
    return cloudDeployment;
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
    public static class Section
    {
      private final int threatLevel;

      private final String policyName;

      private final Map<String, Integer> componentViolationCountMap = new LinkedHashMap<>();

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

      public Map<String, Integer> getComponentViolationCountMap() {
        return componentViolationCountMap;
      }

      public void add(final PolicyFact fact) {
        for (ComponentFact componentFact : fact.getComponentFacts()) {
          String text = componentFact.getDisplayName() == null ?
              "Hash: " + componentFact.getHash() : componentFact.getDisplayName().toString();
          componentViolationCountMap.compute(text, (k, v) -> (v == null) ? 1 : v + 1);
        }
      }
    }

    /**
     * Policy-id -> section map.
     *
     * For now relies on the input policies to be sorted, and will create sections sorted as well.
     */
    private final Map<String, Section> sections = new LinkedHashMap<>();

    public PolicyAlertSections(final List<PolicyFact> policyFacts) {
      if (policyFacts != null) {
        for (PolicyFact policyFact : policyFacts) {
          Section section = sections.get(policyFact.getPolicyId());
          if (section == null) {
            section = new Section(policyFact.getThreatLevel(), policyFact.getPolicyName());
            sections.put(policyFact.getPolicyId(), section);
          }
          section.add(policyFact);
        }
      }
    }

    public Collection<Section> getSections() {
      return sections.values();
    }
  }
}
