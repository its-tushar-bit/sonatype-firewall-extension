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
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionDTO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.landing.UserInterfaceLinksResource;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
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

  private static final Pattern CVE_REGEX_PATTERN = Pattern.compile("(CVE-\\d+-\\d+)");

  private String title;

  private String contents;

  private OrganizationDAO organizationDAO;

  private static Template policyThreatsTemplate;

  static {
    String templateName = "pullrequest-threats.ftl";
    try {
      policyThreatsTemplate = TemplateUtils.createFreemarkerConfig().getTemplate(templateName);
    }
    catch (IOException e) {
      log.error("Error loading {}: {}", templateName, e.getMessage(), e);
    }
  }

  public PullRequestRemediationDetails(final ComponentIdentifier componentIdentifier,
                                       final List<PolicyNotification> notifications,
                                       final ApiVersionChangeOptionDTO versionChangeOptionDTO,
                                       final Application app,
                                       final PolicyEvaluation policyEvaluation,
                                       final BaseUrl baseUrl) throws IOException
  {
    if (policyThreatsTemplate == null) {
      throw new IOException("Unable to construct PullRequestRemediationDetails: no template available");
    }

    this.organizationDAO = new OrganizationDAO();

    this.title = constructTitle(componentIdentifier, versionChangeOptionDTO);

    this.contents =
        constructContents(componentIdentifier, versionChangeOptionDTO, notifications, app, policyEvaluation, baseUrl);
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

  private String constructTitle(ComponentIdentifier componentIdentifier,
                                ApiVersionChangeOptionDTO versionChangeOptionDTO)
  {
    String identifierString = constructIdentifierString(componentIdentifier);

    String targetVersion = getTargetVersion(versionChangeOptionDTO);

    return MessageFormat.format("Bump {0} to {1}", identifierString, targetVersion);
  }

  private String getTargetVersion(final ApiVersionChangeOptionDTO versionChangeOptionDTO) {
    return versionChangeOptionDTO.getData().getComponent().componentIdentifier.getCoordinates().get(VERSION);
  }

  private String constructIdentifierString(final ComponentIdentifier componentIdentifier) {
    // fromIdentifier has embedded spaces, so strip out all of them before returning
    return ComponentDisplayNameUtil.fromIdentifier(componentIdentifier).toString().replaceAll("\\s+", "");
  }

  private String constructContents(ComponentIdentifier componentIdentifier,
                                   ApiVersionChangeOptionDTO versionChangeOptionDTO,
                                   List<PolicyNotification> notifications,
                                   Application app,
                                   PolicyEvaluation policyEvaluation,
                                   BaseUrl baseUrl) throws IOException
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
        .put("targetVersion", getTargetVersion(versionChangeOptionDTO))
        .put("targetSearchUrl", constructMavenSearchUrl(
            versionChangeOptionDTO.getData().getComponent().componentIdentifier.getCoordinates()))
        .put("applicationName", app.getName())
        .put("organizationName", getOrganizationName(app))
        .put("threatList", threatList)
        .put("scanId", policyEvaluation.getScanId())
        .put("stage", policyEvaluation.getStageTypeId())
        .put("detailedReportUrl", baseUrl.getConfigured() +
            UserInterfaceLinksResource.getReportUrl(app.getPublicId(), policyEvaluation.getScanId()))
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
        .map(ComponentFact::getConstraintFacts)
        .filter(list -> list != null && !list.isEmpty())
        .flatMap(list -> list.stream())
        .map(ConstraintFact::getConditionFacts)
        .filter(facts -> facts != null && !facts.isEmpty())
        .flatMap(facts -> facts.stream())
        .map(ConditionFact::getReason)
        .map(reason -> addCveLinks(reason, baseUrl))
        .collect(Collectors.joining(", "));
  }

  private String getConstraintName(final PolicyNotification policyNotification) {
    PolicyFact policyFact = policyNotification.getPolicyFact();
    if (!hasComponentFacts(policyFact)) {
      return "";
    }
    return policyFact.getComponentFacts().stream()
      .map(ComponentFact::getConstraintFacts)
      .filter(list -> list != null && !list.isEmpty())
      .flatMap(list -> list.stream())
      .map(ConstraintFact::getConstraintName)
      .collect(Collectors.joining(", "));
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
