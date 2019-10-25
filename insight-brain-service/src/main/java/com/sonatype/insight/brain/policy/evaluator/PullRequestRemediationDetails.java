/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.landing.UserInterfaceLinksResource;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.notifications.PolicyNotification;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.utils.TemplateUtils;

import com.google.common.collect.ImmutableMap;
import freemarker.template.Template;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.clm.dto.model.component.ComponentIdentifier.MAVEN_ARTIFACT_ID;
import static com.sonatype.clm.dto.model.component.ComponentIdentifier.MAVEN_GROUP_ID;
import static com.sonatype.clm.dto.model.component.ComponentIdentifier.VERSION;

/**
 * Constructs the remediation details which will be presented to the user in a Pull Request
 */
public class PullRequestRemediationDetails
{
  private static final Logger log = LoggerFactory.getLogger(PullRequestRemediationDetails.class);

  private static final Pattern CVE_REGEX_PATTERN = Pattern.compile("((CVE|SONATYPE|sonatype)-\\d+-\\d+)");

  private static final OrganizationDAO organizationDAO = new OrganizationDAO();

  private static final String DELIMITER = "<br>";

  private static final String PREFIX = "<p>";

  private static final String SUFFIX = "</p>";

  private static Template policyThreatsTemplate;

  private final Application app;

  private final String pullRequestBranchName;

  private String title;

  private String contents;
  
  private ComponentIdentifier toBeRemediated;
  
  private String remediatedVersion;

  static {
    String templateName = "pullrequest-threats.ftl";
    try {
      policyThreatsTemplate = TemplateUtils.createFreemarkerConfig().getTemplate(templateName);
    }
    catch (IOException e) {
      log.error("Error loading {}: {}", templateName, e.getMessage(), e);
    }
  }

  public PullRequestRemediationDetails(final ComponentIdentifier toBeRemediated,
                                       final String remediatedVersion,
                                       final String pullRequestBranchName,
                                       final List<PolicyNotification> notifications,
                                       final Application app,
                                       final String scanId,
                                       final Stage stage,
                                       final BaseUrl baseUrl) throws IOException
  {
    if (policyThreatsTemplate == null) {
      throw new IOException("Unable to construct PullRequestRemediationDetails: no template available");
    }

    this.toBeRemediated = toBeRemediated;
    this.remediatedVersion = remediatedVersion;
    this.pullRequestBranchName = pullRequestBranchName;
    this.title = constructTitle();
    this.app = app;
    this.contents = constructContents(toBeRemediated, notifications, app, scanId, stage, baseUrl);
  }

  public String getTitle() {
    return title;
  }

  /**
   * The Markdown-formatted contents of the Pull Request
   */
  public String getContents() {
    return contents;
  }

  public ComponentIdentifier getToBeRemediated() {
    return toBeRemediated;
  }

  public String getRemediatedVersion() {
    return remediatedVersion;
  }
  
  public String getPullRequestBranchName() {
    return pullRequestBranchName;
  }

  public Application getApp() {
    return app;
  }

  private String constructTitle() {
    String identifierString = constructIdentifierString(toBeRemediated);

    return MessageFormat.format("Bump {0} to {1}", identifierString, remediatedVersion);
  }

  private String constructIdentifierString(final ComponentIdentifier componentIdentifier) {
    // fromIdentifier has embedded spaces, so strip out all of them before returning
    return ComponentDisplayNameUtil.fromIdentifier(componentIdentifier).toString().replaceAll("\\s+", "");
  }

  private String constructContents(
      final ComponentIdentifier componentIdentifier,
      final List<PolicyNotification> notifications,
      final Application app,
      final String scanId,
      final Stage stage, 
      final BaseUrl baseUrl) throws IOException
  {
    List<Map<String, Object>> threatList = notifications.stream()
        .map(policyNotification -> {
          return ImmutableMap.<String, Object>builder()
              .put("policy", policyNotification.getPolicyFact().getPolicyName())
              .put("threat", policyNotification.getPolicyFact().getThreatLevel())
              .put("constraint", getConstraintName(policyNotification))
              .put("conditions", getConditionText(policyNotification, baseUrl))
              .build();
        })
        .collect(Collectors.toList());

    Map<String, Object> model = ImmutableMap.<String, Object>builder()
        .put("initialSearchUrl", constructMavenSearchUrl(componentIdentifier.getCoordinates()))
        .put("initialCoordinates", constructIdentifierString(componentIdentifier))
        .put("targetVersion", remediatedVersion)
        .put("targetSearchUrl",
            constructMavenSearchUrl(componentIdentifier.createAlternativeVersion(remediatedVersion).getCoordinates()))
        .put("applicationName", app.getName())
        .put("organizationName", getOrganizationName(app))
        .put("threatList", threatList)
        .put("scanId", scanId)
        .put("stage", stage.getStageTypeId())
        .put("detailedReportUrl", baseUrl.getConfigured() +
            UserInterfaceLinksResource.getReportUrl(app.getPublicId(), scanId))
        .put("baseIqUrl", baseUrl.getConfigured())
        .build();

    return TemplateUtils.render(policyThreatsTemplate, model);
  }

  private Object getOrganizationName(final Application app) {
    Organization organization = organizationDAO.getById(app.getOrganizationId());
    return organization.getName();
  }

  private String getConditionText(final PolicyNotification policyNotification, BaseUrl baseUrl) {
    PolicyFact policyFact = policyNotification.getPolicyFact();
    if (!hasComponentFacts(policyFact)) {
      return "";
    }
    return policyFact.getComponentFacts().stream()
        .filter(fact -> fact.getComponentIdentifier().equals(toBeRemediated))
        .map(ComponentFact::getConstraintFacts)
        .filter(list -> list != null && !list.isEmpty())
        .flatMap(list -> list.stream())
        .map(ConstraintFact::getConditionFacts)
        .filter(facts -> facts != null && !facts.isEmpty())
        .flatMap(facts -> facts.stream())
        .map(ConditionFact::getReason)
        .distinct()
        .map(reason -> addCveLinks(reason, baseUrl))
        .collect(Collectors.joining(DELIMITER, PREFIX, SUFFIX));
  }

  private String getConstraintName(final PolicyNotification policyNotification) {
    PolicyFact policyFact = policyNotification.getPolicyFact();
    if (!hasComponentFacts(policyFact)) {
      return "";
    }
    return policyFact.getComponentFacts().stream()
        .filter(fact -> fact.getComponentIdentifier().equals(toBeRemediated))
        .map(ComponentFact::getConstraintFacts)
        .filter(list -> list != null && !list.isEmpty())
        .flatMap(list -> list.stream())
        .map(ConstraintFact::getConstraintName)
        .distinct()
        .collect(Collectors.joining(DELIMITER, PREFIX, SUFFIX));
  }

  private boolean hasComponentFacts(final PolicyFact policyFact) {
    return !(policyFact == null || policyFact.getComponentFacts() == null || policyFact.getComponentFacts().isEmpty());
  }

  private String addCveLinks(String text, BaseUrl baseUrl) {
    Matcher matcher = CVE_REGEX_PATTERN.matcher(text);
    StringBuffer stringBuffer = new StringBuffer();
    while (matcher.find()) {
      String cveCode = matcher.group(1);
      matcher.appendReplacement(stringBuffer,
          MessageFormat.format("[{0}]({1}ui/links/vln/{0})", cveCode, baseUrl.getConfigured()));
    }
    matcher.appendTail(stringBuffer);
    return stringBuffer.toString();
  }

  private String constructMavenSearchUrl(Map<String, String> coordinates) {
    return MessageFormat.format("https://search.maven.org/artifact/{0}/{1}/{2}/jar",
        coordinates.get(MAVEN_GROUP_ID), coordinates.get(MAVEN_ARTIFACT_ID), coordinates.get(VERSION));
  }
}
