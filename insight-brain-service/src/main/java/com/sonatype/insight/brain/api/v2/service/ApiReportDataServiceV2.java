/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationBaseDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportComponentPolicyViolationsDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportConstraintConditionDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportConstraintViolationDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportPolicyDataDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportPolicyViolationDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportRawDataDTOV2;
import com.sonatype.insight.brain.component.ComponentResolver;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.organization.ReportMetadataDTO;
import com.sonatype.insight.brain.policy.evaluator.PolicyThreats;
import com.sonatype.insight.brain.policy.evaluator.PolicyThreats.PolicyCondition;
import com.sonatype.insight.brain.policy.evaluator.PolicyThreats.PolicyConstraint;
import com.sonatype.insight.brain.policy.evaluator.PolicyThreats.PolicyViolation;
import com.sonatype.insight.brain.policy.evaluator.ScanPolicyEvaluator;
import com.sonatype.insight.brain.report.Report;
import com.sonatype.insight.brain.report.ReportEntry;
import com.sonatype.insight.brain.report.ReportService;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ContainerNode;

import static java.util.stream.Collectors.toList;

/**
 * Provides data from an application's composition report in a format suitable for consumption by 3rd-party clients.
 * 
 * @since 1.13.0
 */
@Named
public class ApiReportDataServiceV2
{
  private final InsightWork work;

  private final ApplicationDAO appDAO;

  private final ReportService reportService;

  private final ApiLicenseDataAdapter licenseDataAdapter;

  private final ApiSecurityDataAdapter securityDataAdapter;

  private final ComponentResolver componentResolver;

  @Inject
  public ApiReportDataServiceV2(
      InsightWork work,
      ApplicationDAO appDAO,
      ReportService reportService,
      ApiLicenseDataAdapter licenseDataAdapter,
      ApiSecurityDataAdapter securityDataAdapter,
      ComponentResolver componentResolver)
  {
    this.work = work;
    this.appDAO = appDAO;
    this.reportService = reportService;
    this.licenseDataAdapter = licenseDataAdapter;
    this.securityDataAdapter = securityDataAdapter;
    this.componentResolver = componentResolver;
  }

  @Authorize(permission = Permission.READ)
  public ApiReportRawDataDTOV2 getRawData(
      @AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) String applicationPublicId,
      String scanId) throws IOException
  {
    return getDataNoAuth(applicationPublicId, scanId);
  }

  /**
   * @since 1.64
   */
  @Authorize(permission = Permission.READ)
  public ApiReportPolicyDataDTOV2 getPolicyViolationsData(
      @AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) String applicationPublicId,
      String scanId) throws IOException
  {
    Application app = appDAO.getByPublicIdNotNull(applicationPublicId);
    File reportFile = reportService.getReport(work, app.getId(), scanId);

    ReportEntry bomEntry = Report.getEntry(reportFile, "bom.json");
    ReportEntry countsEntry = Report.getEntry(reportFile, Report.DATA_JSON_FILENAME);
    ReportEntry policyThreatsEntry = Report.getEntry(reportFile, ScanPolicyEvaluator.POLICY_THREATS_FILENAME);

    if (bomEntry == null || policyThreatsEntry == null || countsEntry == null) {
      throw new BadRequestException(
          "The report with ID " + scanId + " contains no component, policy threats or counts data.");
    }

    ApiReportPolicyDataDTOV2 data = new ApiReportPolicyDataDTOV2();

    ReportMetadataDTO metadata = reportService.getReportMetadata(applicationPublicId, scanId);
    data.reportTime = metadata.getReportTime();
    data.reportTitle = metadata.getReportTitle();
    data.application = getApplicationMetadata(metadata.getApplication());
    data.counts = getReportCounts(countsEntry.buf);
    data.components = getComponents(bomEntry.buf, policyThreatsEntry.buf);

    return data;
  }

  private List<ApiReportComponentPolicyViolationsDTOV2> getComponents(byte[] bomData, byte[] policyThreatsData)
      throws IOException
  {
    List<ApiReportComponentPolicyViolationsDTOV2> components = new ArrayList<>();

    // violations per component
    Map<String, List<ApiReportPolicyViolationDTOV2>> violationsByHash = getPolicyViolationsByHash(policyThreatsData);

    JsonNode bomJson = JsonUtils.parse(bomData);
    if (bomJson != null) {
      bomJson = bomJson.get("aaData");
      if (bomJson != null) {
        ArrayNode bomJsonArray = (ArrayNode) bomJson;
        for (int i = 0; i < bomJsonArray.size(); i++) {
          JsonNode componentJson = bomJsonArray.get(i);
          ApiReportComponentPolicyViolationsDTOV2 component = new ApiReportComponentPolicyViolationsDTOV2();
          component.hash = JsonUtils.getNullableString(componentJson.get("hash"));
          component.matchState = JsonUtils.getNullableString(componentJson.get("matchState"));
          ComponentIdentifier componentIdentifier = ComponentIdentifierAdapter.getComponentIdentifier(componentJson);
          component.componentIdentifier = ApiComponentIdentifierDTOV2
              .fromComponentIdentifier(componentIdentifier);
          component.packageUrl = PackageUrlIdentifier.toPackageUrl(componentIdentifier);
          component.proprietary = componentJson.get("proprietary").booleanValue();
          component.pathnames = getPathnames(componentJson);
          component.violations = violationsByHash.getOrDefault(component.hash, Collections.emptyList());
          components.add(component);
        }
      }
    }
    return components;
  }

  private ApiApplicationBaseDTO getApplicationMetadata(Application application) {
    ApiApplicationBaseDTO applicationDto = new ApiApplicationBaseDTO();
    applicationDto.id = application.getId();
    applicationDto.publicId = application.getPublicId();
    applicationDto.name = application.getName();
    applicationDto.organizationId = application.getOrganizationId();
    applicationDto.contactUserName = application.getContactInternalName();
    return applicationDto;
  }

  private Map<String, Integer> getReportCounts(byte[] countsJsonData) throws IOException {
    Map<String, Integer> reportCounts = new HashMap<>();
    Map<String, Integer> countsData = JsonUtils.parse(countsJsonData, reportCounts.getClass());
    reportCounts.put("partiallyMatchedComponentCount", countsData.get("partiallyMatchedComponentCount"));
    reportCounts.put("exactlyMatchedComponentCount", countsData.get("exactlyMatchedComponentCount"));
    reportCounts.put("totalComponentCount", countsData.get("totalArtifactCount"));
    reportCounts.put("grandfatheredPolicyViolationCount", countsData.get("grandfatheredPolicyViolationCount"));
    return reportCounts;
  }

  private List<String> getPathnames(JsonNode componentNode) {
    List<String> pathnames = new ArrayList<>();
    ArrayNode pathnamesArray = (ArrayNode) componentNode.get("pathnames");
    for (int i = 0; i < pathnamesArray.size(); i++) {
      String pathname = pathnamesArray.get(i).asText();
      if (!pathname.startsWith("dependency:")) {
        pathnames.add(pathname);
      }
    }
    return pathnames;
  }

  private Map<String, List<ApiReportPolicyViolationDTOV2>> getPolicyViolationsByHash(byte[] policyThreatsData)
      throws IOException
  {
    PolicyThreats policyThreats = JsonUtils.parse(policyThreatsData, PolicyThreats.class);
    return policyThreats.aaData.stream().collect(Collectors.toMap(o -> o.hash,  this::getViolations));
  }

  private List<ApiReportPolicyViolationDTOV2> getViolations(PolicyThreats.Component component) {
    return component.allViolations.stream().map(this::getViolation).collect(toList());
  }

  private ApiReportPolicyViolationDTOV2 getViolation(PolicyViolation violation) {
    ApiReportPolicyViolationDTOV2 dto = new ApiReportPolicyViolationDTOV2();
    dto.policyId = violation.policyId;
    dto.policyName = violation.policyName;
    dto.policyThreatCategory = violation.policyThreatCategory;
    dto.policyThreatLevel = violation.policyThreatLevel;
    dto.policyViolationId = violation.policyViolationId;
    dto.grandfathered = violation.grandfathered;
    dto.waived = violation.waived;
    dto.constraints = getConstraints(violation.constraints);
    return dto;
  }

  private List<ApiReportConstraintViolationDTOV2> getConstraints(List<PolicyConstraint> policyConstraints) {
    return policyConstraints.stream().map(this::getConstraint).collect(toList());
  }

  private ApiReportConstraintViolationDTOV2 getConstraint(PolicyConstraint policyConstraint) {
    ApiReportConstraintViolationDTOV2 dto = new ApiReportConstraintViolationDTOV2();
    dto.constraintId = policyConstraint.constraintId;
    dto.constraintName = policyConstraint.constraintName;
    dto.conditions = getConstraintConditions(policyConstraint.conditions);
    return dto;
  }

  private List<ApiReportConstraintConditionDTOV2> getConstraintConditions(List<PolicyCondition> policyConditions) {
    return policyConditions.stream().map(this::getConstraintConditionDTO).collect(toList());
  }

  private ApiReportConstraintConditionDTOV2 getConstraintConditionDTO(PolicyCondition policyCondition) {
    ApiReportConstraintConditionDTOV2 dto = new ApiReportConstraintConditionDTOV2();
    dto.conditionSummary = policyCondition.conditionSummary;
    dto.conditionReason = policyCondition.conditionReason;
    return dto;
  }

  public ApiReportRawDataDTOV2 getDataNoAuth(String applicationPublicId, String scanId) throws IOException {
    Application app = appDAO.getByPublicIdNotNull(applicationPublicId);
    File reportFile = reportService.getReport(work, app.getId(), scanId);

    ReportEntry bomEntry = Report.getEntry(reportFile, "bom.json");
    ReportEntry securityEntry = Report.getEntry(reportFile, "security.json");
    ReportEntry licenseEntry = Report.getEntry(reportFile, "licenses.json");
    ReportEntry dataEntry = Report.getEntry(reportFile, Report.DATA_JSON_FILENAME);

    if (bomEntry == null || securityEntry == null || licenseEntry == null || dataEntry == null) {
      throw new BadRequestException("The report with ID " + scanId + " contains no component data.");
    }

    List<Component> components =
        componentResolver.getComponents(app, licenseEntry.buf, securityEntry.buf, bomEntry.buf, scanId);

    ApiReportRawDataDTOV2 data = new ApiReportRawDataDTOV2();
    for (Component comp : components) {
      ApiReportComponentDTOV2 component = new ApiReportComponentDTOV2();
      component.hash = comp.getHash();
      component.componentIdentifier = ApiComponentIdentifierDTOV2
          .fromComponentIdentifier(comp.getComponentIdentifier());
      component.packageUrl = PackageUrlIdentifier.toPackageUrl(comp.getComponentIdentifier());

      component.matchState = comp.getMatchState().getId();
      component.proprietary = comp.isProprietary();
      for (String pathname : comp.getPathnames()) {
        if (!pathname.startsWith("dependency:")) {
          component.pathnames.add(pathname);
        }
      }
      if (!MatchState.UNKNOWN.equals(comp.getMatchState())) {
        component.securityData = securityDataAdapter.convertToDTO(comp);
        component.licenseData = licenseDataAdapter.convertToDTOV2(comp);
      }

      data.components.add(component);
    }

    ContainerNode<?> dataJson = JsonUtils.parse(dataEntry.buf);
    data.matchSummary.knownComponentCount = dataJson.get("knownArtifactCount").intValue();
    data.matchSummary.totalComponentCount = dataJson.get("totalArtifactCount").intValue();

    return data;
  }
}
