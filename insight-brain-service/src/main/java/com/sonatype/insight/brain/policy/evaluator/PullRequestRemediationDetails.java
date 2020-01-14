/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.io.IOException;
import java.text.MessageFormat;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.landing.UserInterfaceLinksResource;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityStatusConditionType;
import com.sonatype.insight.brain.model.policy.notifications.PolicyNotification;
import com.sonatype.insight.brain.utils.TemplateUtils;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import freemarker.template.Template;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.clm.dto.model.component.ComponentIdentifier.MAVEN_ARTIFACT_ID;
import static com.sonatype.clm.dto.model.component.ComponentIdentifier.MAVEN_GROUP_ID;
import static com.sonatype.clm.dto.model.component.ComponentIdentifier.VERSION;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.partitioningBy;
import static java.util.stream.Collectors.toList;

/**
 * Constructs the remediation details which will be presented to the user in a Pull Request
 */
public class PullRequestRemediationDetails
{
  @VisibleForTesting
  static Clock clock = Clock.systemDefaultZone();
  
  private static final Logger log = LoggerFactory.getLogger(PullRequestRemediationDetails.class);

  private static final Pattern CVE_REGEX_PATTERN = Pattern.compile("((CVE|SONATYPE|sonatype)-\\d+-\\d+)");

  private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter
      .ofPattern("yyyy-MM-dd HH:mm:ss O").withLocale(Locale.ENGLISH);

  private static final List<String> SECURITY_CONDITIONS = ImmutableList
      .of(SecurityVulnerabilitySeverityConditionType.ID, SecurityVulnerabilityStatusConditionType.ID);

  private static final OrganizationDAO organizationDAO = new OrganizationDAO();

  private static Template policyThreatsTemplate;

  private final Application app;

  private final String pullRequestBranchName;

  private final String title;

  private final String contents;
  
  private final ComponentIdentifier toBeRemediated;
  
  private final String remediatedVersion;

  private final String scanId;

  private final String stage;

  private final String baseUrl;

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
                                       final String stage,
                                       final String baseUrl) throws IOException
  {
    if (policyThreatsTemplate == null) {
      throw new IOException("Unable to construct PullRequestRemediationDetails: no template available");
    }

    this.toBeRemediated = toBeRemediated;
    this.remediatedVersion = remediatedVersion;
    this.pullRequestBranchName = pullRequestBranchName;
    this.title = constructTitle();
    this.app = app;
    this.scanId = scanId;
    this.stage = stage;
    this.baseUrl = baseUrl;
    this.contents = constructContents(notifications);
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

  public String getBaseUrl() {
    return baseUrl;
  }

  private String constructTitle() {
    return MessageFormat.format("Bump {0} to {1}", getShortComponentName(toBeRemediated), remediatedVersion);
  }

  private String constructContents(
      final List<PolicyNotification> notifications
  ) throws IOException
  {
    List<Map<String, Object>> threatList = notifications.stream()
        .map(policyNotification -> ImmutableMap.<String, Object>builder()
            .put("policy", policyNotification.getPolicyFact().getPolicyName())
            .put("threat", policyNotification.getPolicyFact().getThreatLevel())
            .put("details", getViolationDetails(policyNotification))
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
        .put("detailedReportUrl", baseUrl + UserInterfaceLinksResource.getReportUrl(app.getPublicId(), scanId))
        .put("baseIqUrl", baseUrl)
        .build();

    return TemplateUtils.render(policyThreatsTemplate, model);
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
      default:
        return sanitizeDisplayName(componentIdentifier);
    }
  }

  private Object getOrganizationName(final Application app) {
    Organization organization = organizationDAO.getById(app.getOrganizationId());
    return organization.getName();
  }

  private String getViolationDetails(final PolicyNotification policyNotification) {
    PolicyFact policyFact = policyNotification.getPolicyFact();
    if (!hasComponentFacts(policyFact)) {
      return "";
    }

    // Within the 'Violation details' column we obey the following rules per table row (i.e. per policy violation):
    //  - One line per constraint
    //  - The constraint name is bold
    //  - Followed by a comma separated list of reasons
    //  - For 'Security Severity' types, we apply custom formatting
    return policyFact.getComponentFacts().stream()
        .filter(fact -> fact.getComponentIdentifier().equals(toBeRemediated))
        .map(ComponentFact::getConstraintFacts)
        .filter(list -> list != null && !list.isEmpty())
        .flatMap(Collection::stream)
        .collect(groupingBy(ConstraintFact::getConstraintName, LinkedHashMap::new, toList()))
        .entrySet().stream()
        .map(this::printConstraint)
        .collect(joining());
  }

  private String printConstraint(final Entry<String, List<ConstraintFact>> constraint) {
    String constraintName = constraint.getKey();
    List<ConstraintFact> constraintFacts = constraint.getValue();
    List<ConditionFact> conditionFacts = constraintFacts.stream()
        .map(ConstraintFact::getConditionFacts)
        .flatMap(Collection::stream)
        .collect(toList());

    // Separate security conditions out from the rest so we can sort them first and do the custom formatting
    Map<Boolean, List<ConditionFact>> conditionsByType = conditionFacts.stream().collect(partitioningBy(cf ->
        SECURITY_CONDITIONS.contains(cf.getConditionTypeId())));

    List<String> securityConditionFacts = conditionsByType.get(Boolean.TRUE)
        .stream()
        .map(this::fetchCVE)
        .distinct()
        .collect(toList());
    List<String> nonSecurityConditionFacts = conditionsByType.get(Boolean.FALSE)
        .stream()
        .map(ConditionFact::getReason)
        .distinct()
        .collect(toList());

    return String.format("<b>%s:</b><ul>%s%s</ul>",
        constraintName,
        printSecurityConditions(securityConditionFacts),
        printNonSecurityConditions(nonSecurityConditionFacts));
  }

  private String printSecurityConditions(final List<String> securityConditionFacts) {
    return securityConditionFacts.isEmpty() ? "" : printListEntry(
        getSecurityPrefix(securityConditionFacts) + String.join(", ", securityConditionFacts));
  }

  private String printNonSecurityConditions(final List<String> nonSecurityConditionFacts) {
    return nonSecurityConditionFacts.stream().map(this::printListEntry).collect(joining());
  }

  private String printListEntry(final String item) {
    return String.format("<li>%s</li>", item);
  }

  private String getSecurityPrefix(final List<String> securityConditionFacts) {
    return "Found security " + (securityConditionFacts.size() == 1 ? "vulnerability: " : "vulnerabilities: ");
  }

  private boolean hasComponentFacts(final PolicyFact policyFact) {
    return !(policyFact == null || policyFact.getComponentFacts() == null || policyFact.getComponentFacts().isEmpty());
  }

  private String fetchCVE(final ConditionFact conditionFact) {
    Matcher matcher = CVE_REGEX_PATTERN.matcher(conditionFact.getReference().getValue());
    StringBuffer stringBuffer = new StringBuffer();
    while (matcher.find()) {
      String cveCode = matcher.group(1);
      matcher.appendReplacement(stringBuffer,
          MessageFormat.format("[{0}]({1}ui/links/vln/{0})", cveCode, baseUrl));
    }
    matcher.appendTail(stringBuffer);
    return stringBuffer.toString();
  }

  private String constructMavenSearchUrl(Map<String, String> coordinates) {
    return MessageFormat.format("https://search.maven.org/artifact/{0}/{1}/{2}/jar",
        coordinates.get(MAVEN_GROUP_ID), coordinates.get(MAVEN_ARTIFACT_ID), coordinates.get(VERSION));
  }
}
