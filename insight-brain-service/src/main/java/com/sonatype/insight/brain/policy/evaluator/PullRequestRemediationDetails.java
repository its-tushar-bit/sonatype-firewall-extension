/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.io.IOException;
import java.text.MessageFormat;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.insight.brain.landing.UserInterfaceLinksHelper;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.notifications.PolicyNotification;
import com.sonatype.insight.brain.utils.TemplateUtils;
import com.sonatype.nexus.scm.SourceControlProvider;

import com.google.common.collect.ImmutableMap;
import freemarker.template.Template;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.clm.dto.model.component.ComponentIdentifier.MAVEN_ARTIFACT_ID;
import static com.sonatype.clm.dto.model.component.ComponentIdentifier.MAVEN_GROUP_ID;
import static com.sonatype.clm.dto.model.component.ComponentIdentifier.VERSION;
import static java.util.stream.Collectors.toList;

/**
 * Constructs the remediation details which will be presented to the user in a Pull Request
 */
public class PullRequestRemediationDetails
    extends PullRequestDetailsBase
{
  private static final Logger log = LoggerFactory.getLogger(PullRequestRemediationDetails.class);

  private static Template policyThreatsMDEmbeddedHtmlTemplate;

  private static Template policyThreatsMDMinimalTemplate;

  private final Application app;

  private final String pullRequestBranchName;

  private final String title;

  private String contents;

  private final ComponentIdentifier toBeRemediated;

  private final String remediatedVersion;

  private final String scanId;

  private final String stage;

  static {
    try {
      policyThreatsMDEmbeddedHtmlTemplate =
          TemplateUtils.createFreemarkerConfig().getTemplate("pullrequest-threats.ftl");
      policyThreatsMDMinimalTemplate =
          TemplateUtils.createFreemarkerConfig().getTemplate("pullrequest-minimal-markdown-threats.ftl");
    }
    catch (IOException e) {
      log.error("Error loading threats template: {}", e.getMessage(), e);
    }
  }

  private PullRequestRemediationDetails(
      final ComponentIdentifier toBeRemediated,
      final String remediatedVersion,
      final String pullRequestBranchName,
      final Application app,
      final String scanId,
      final String stage) throws IOException
  {
    if (policyThreatsMDEmbeddedHtmlTemplate == null) {
      throw new IOException(
          "Unable to construct PullRequestRemediationDetails: rich Markdown template is unavailable");
    }
    if (policyThreatsMDMinimalTemplate == null) {
      throw new IOException(
          "Unable to construct PullRequestRemediationDetails: minimal Markdown template is unavailable");
    }

    this.toBeRemediated = toBeRemediated;
    this.remediatedVersion = remediatedVersion;
    this.pullRequestBranchName = pullRequestBranchName;
    this.title = constructTitle();
    this.app = app;
    this.scanId = scanId;
    this.stage = stage;
  }

  public PullRequestRemediationDetails(
      final ComponentIdentifier toBeRemediated,
      final String remediatedVersion,
      final String pullRequestBranchName,
      final List<PolicyNotification> notifications,
      final Application app,
      final String scanId,
      final String stage,
      final String baseUrl,
      final SourceControlProvider provider) throws IOException
  {
    this(toBeRemediated, remediatedVersion, pullRequestBranchName, app, scanId, stage);
    this.contents = constructContents(notifications, baseUrl, provider);
  }

  public PullRequestRemediationDetails(
      final ComponentIdentifier toBeRemediated,
      final String remediatedVersion,
      final String pullRequestBranchName,
      final Application app,
      final String scanId,
      final String stage,
      final String contents) throws IOException
  {
    this(toBeRemediated, remediatedVersion, pullRequestBranchName, app, scanId, stage);
    this.contents = contents;
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

  public String getScanId() {
    return scanId;
  }

  public String getStage() {
    return stage;
  }

  private String constructTitle() {
    return MessageFormat.format("Bump {0} to {1}", getShortComponentName(toBeRemediated), remediatedVersion);
  }

  private String constructContents(
      final List<PolicyNotification> notifications,
      final String baseUrl,
      final SourceControlProvider provider) throws IOException
  {
    List<Map<String, Object>> threatList = notifications.stream()
        .map(policyNotification -> ImmutableMap.<String, Object>builder()
            .put("policy", policyNotification.getPolicyFact().getPolicyName())
            .put("threat", policyNotification.getPolicyFact().getThreatLevel())
            .put("constraints", getViolationDetails(policyNotification, baseUrl))
            .build())
        .collect(toList());

    ComponentIdentifier remediatedComponent = toBeRemediated.createAlternativeVersion(remediatedVersion);
    Map<String, Object> model = ImmutableMap.<String, Object>builder()
        .put("componentName", getComponentName(getToBeRemediated()))
        .put("initialVersionDisplay", constructVersionDisplay(toBeRemediated))
        .put("targetVersionDisplay", constructVersionDisplay(remediatedComponent))
        .put("applicationName", app.getName())
        .put("organizationName", getOrganizationName(app))
        .put("threatList", threatList)
        .put("date", DATE_TIME_FORMATTER.format(ZonedDateTime.now(clock)))
        .put("stage", stage)
        .put("detailedReportUrl",
            baseUrl + UserInterfaceLinksHelper.getReportUrl(app.getPublicId(), scanId) + "?source=auto-pr")
        .put("baseIqUrl", baseUrl)
        .put("provider", provider)
        .build();

    return TemplateUtils.render(getPolicyTemplate(provider), model);
  }

  /**
   * Strip out whitespace from the display name.
   */
  private String sanitizeDisplayName(final ComponentIdentifier componentIdentifier) {
    return ComponentDisplayNameUtil.fromIdentifier(componentIdentifier).toString().replaceAll("\\s+", "");
  }

  private String constructVersionDisplay(final ComponentIdentifier componentIdentifier) {
    switch (componentIdentifier.getFormat()) {
      // maven includes a markdown formatted link
      case ComponentIdentifier.FORMAT_MAVEN:
        return "[" + componentIdentifier.get(VERSION) + "]" + "(" +
            constructMavenSearchUrl(componentIdentifier.getCoordinates()) + ")";
      default:
        return componentIdentifier.get(VERSION);
    }
  }

  /**
   * Use a short component name for the PR title. For most formats this is just the name (e.g. npm) but for formats
   * with longer names like Maven, we shorten it (e.g. 'jackson-databind' instead of
   * 'com.fasterxml.jackson.core:jackson-databind')
   */
  private String getShortComponentName(final ComponentIdentifier componentIdentifier) {
    switch (componentIdentifier.getFormat()) {
      case ComponentIdentifier.FORMAT_MAVEN:
        return componentIdentifier.get(MAVEN_ARTIFACT_ID);
      case ComponentIdentifier.FORMAT_NPM:
        return componentIdentifier.get(ComponentIdentifier.NPM_PACKAGE_ID);
      case ComponentIdentifier.FORMAT_GOLANG:
        return componentIdentifier.get(ComponentIdentifier.GOLANG_NAME);
      default:
        return sanitizeDisplayName(componentIdentifier);
    }
  }

  private String getComponentName(final ComponentIdentifier componentIdentifier) {
    switch (componentIdentifier.getFormat()) {
      case ComponentIdentifier.FORMAT_MAVEN:
        return String.join(" : ", componentIdentifier.get(MAVEN_GROUP_ID), componentIdentifier.get(MAVEN_ARTIFACT_ID));
      case ComponentIdentifier.FORMAT_NPM:
        return componentIdentifier.get(ComponentIdentifier.NPM_PACKAGE_ID);
      case ComponentIdentifier.FORMAT_GOLANG:
        return componentIdentifier.get(ComponentIdentifier.GOLANG_NAME);
      default:
        return sanitizeDisplayName(componentIdentifier);
    }
  }

  private List<Map<String, Object>> getViolationDetails(
      final PolicyNotification policyNotification,
      final String baseUrl)
  {
    final PolicyFact policyFact = policyNotification.getPolicyFact();
    if (!hasComponentFacts(policyFact)) {
      return Collections.emptyList();
    }

    return getConstraintDetailsForConstraints(policyFact.getComponentFacts()
        .stream()
        .filter(fact -> fact.getComponentIdentifier().equals(toBeRemediated))
        .map(ComponentFact::getConstraintFacts)
        .filter(list -> list != null && !list.isEmpty())
        .flatMap(Collection::stream)
        .collect(Collectors.toList()), baseUrl, true);
  }

  private boolean hasComponentFacts(final PolicyFact policyFact) {
    return !(policyFact == null || policyFact.getComponentFacts() == null || policyFact.getComponentFacts().isEmpty());
  }

  private String constructMavenSearchUrl(Map<String, String> coordinates) {
    return MessageFormat.format("https://search.maven.org/artifact/{0}/{1}/{2}/jar",
        coordinates.get(MAVEN_GROUP_ID), coordinates.get(MAVEN_ARTIFACT_ID), coordinates.get(VERSION));
  }

  private Template getPolicyTemplate(SourceControlProvider provider) {
    if (provider.supportsEmbeddedHtmlInMarkdown()) {
      return policyThreatsMDEmbeddedHtmlTemplate;
    }
    return policyThreatsMDMinimalTemplate;
  }
}
