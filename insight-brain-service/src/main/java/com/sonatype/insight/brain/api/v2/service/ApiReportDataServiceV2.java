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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.component.ComponentDisplayName;
import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.ApiConfigFeaturesService.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationBaseDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiDependencyDataDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiDependencyTreeNodeDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiDependencyTreeResponseDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiReportComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportComponentPolicyViolationsDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportConstraintConditionDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportConstraintViolationDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportPolicyDataDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportPolicyViolationDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportRawDataDTOV2;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.component.ComponentDAO;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.InnerSourceData;
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
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.scan.application.BillOfMaterialsRowDTO;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ContainerNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.stream.Collectors.toList;

/**
 * Provides data from an application's composition report in a format suitable for consumption by 3rd-party clients.
 *
 * @since 1.13.0
 */
@Named
public class ApiReportDataServiceV2
{
  private static final Logger log = LoggerFactory.getLogger(ApiReportDataServiceV2.class);

  private static final String DEPENDENCY_PREFIX = "dependency:";

  private final ApplicationDAO appDAO;

  private final ReportService reportService;

  private final ApiLicenseDataAdapter licenseDataAdapter;

  private final ApiSecurityDataAdapter securityDataAdapter;

  @Inject
  public ApiReportDataServiceV2(
      ApplicationDAO appDAO,
      ReportService reportService,
      ApiLicenseDataAdapter licenseDataAdapter,
      ApiSecurityDataAdapter securityDataAdapter)
  {
    this.appDAO = appDAO;
    this.reportService = reportService;
    this.licenseDataAdapter = licenseDataAdapter;
    this.securityDataAdapter = securityDataAdapter;
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
    return getPolicyViolationsDataNoAuth(applicationPublicId, scanId);
  }

  public ApiReportPolicyDataDTOV2 getPolicyViolationsDataNoAuth(String applicationPublicId, String scanId)
      throws IOException
  {
    Application app = appDAO.getByPublicIdNotNull(applicationPublicId);
    File reportFile = reportService.getReport(app.getId(), scanId);

    ReportEntry bomEntry = Report.getEntry(reportFile, Report.BOM_JSON_FILENAME);
    ReportEntry countsEntry = Report.getEntry(reportFile, Report.DATA_JSON_FILENAME);
    ReportEntry policyThreatsEntry = Report.getEntry(reportFile, ScanPolicyEvaluator.POLICY_THREATS_FILENAME);

    if (bomEntry == null || policyThreatsEntry == null || countsEntry == null) {
      throw new BadRequestException(
          "The report with ID " + scanId + " contains no component, policy threats or counts data.");
    }

    PolicyThreats policyThreats = JsonUtils.parse(policyThreatsEntry.buf, PolicyThreats.class);
    if (policyThreats.version < 4) {
      log.warn("Policy violation data is incomplete for application id {} and report id {}.", app.getId(), scanId);
    }

    ApiReportPolicyDataDTOV2 data = new ApiReportPolicyDataDTOV2();

    ReportMetadataDTO metadata = reportService.getReportMetadataNoAuth(applicationPublicId, scanId);
    data.reportTime = metadata.getReportTime();
    data.reportTitle = metadata.getReportTitle();
    data.commitHash = metadata.getCommitHash();
    data.application = getApplicationMetadata(metadata.getApplication());
    data.initiator = metadata.getInitiator();
    data.counts = getReportCounts(countsEntry.buf);
    data.components = getComponents(bomEntry.buf, policyThreats);

    return data;
  }

  @Authorize(permission = Permission.READ)
  public ApiDependencyTreeResponseDTO getDependencyTree(
      @AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) String applicationPublicId,
      final String scanId) throws IOException
  {
    return new ApiDependencyTreeResponseDTO(getDependencyTreeNoAuth(applicationPublicId, scanId));
  }

  public ApiDependencyTreeNodeDTO getDependencyTreeNoAuth(String applicationPublicId, String scanId)
      throws IOException
  {
    ApiDependencyTreeNodeDTO dependencyTree = new ApiDependencyTreeNodeDTO();
    Application application = appDAO.getByPublicIdNotNull(applicationPublicId);
    String appId = application.getId();

    final File reportFile = reportService.getReport(appId, scanId);
    ReportEntry dependenciesEntry = Report.getEntry(reportFile, Report.toEntryName(Report.DEPENDENCIES_JSON_FILENAME));
    ReportEntry bomEntry = Report.getEntry(reportFile, Report.toEntryName(Report.BOM_JSON_FILENAME));
    if (dependenciesEntry != null && bomEntry != null) {
      JsonNode dependenciesNode = JsonUtils.parse(dependenciesEntry.buf);
      JsonNode bomNode = JsonUtils.parse(bomEntry.buf);
      if (dependenciesNode != null && bomNode != null) {
        JsonNode dependencyTreeNode = dependenciesNode.get("dependencyTree");
        JsonNode aaDataNode = bomNode.get("aaData");
        if (dependencyTreeNode != null && !dependencyTreeNode.isNull() && !dependencyTreeNode.isEmpty()) {
          ObjectNode  dependencyTreeObject = (ObjectNode) dependencyTreeNode;
          dependencyTreeObject.remove("componentIdentifier");
          Map<String,BillOfMaterialsRowDTO> componentsIndex = indexBom(aaDataNode);
          dependencyTree = JsonUtils.asPojo(dependencyTreeObject, ApiDependencyTreeNodeDTO.class);
          dependencyTree.setChildren(correlateDependencyTreeWithComponentIndex(dependencyTree, componentsIndex));
        }
      }
    }
    return dependencyTree;
  }

  private List<ApiDependencyTreeNodeDTO> correlateDependencyTreeWithComponentIndex(
      ApiDependencyTreeNodeDTO root, 
      Map<String,BillOfMaterialsRowDTO> componentsIndex
  )
  {
    List<ApiDependencyTreeNodeDTO> children = root.getChildren();
    if (children == null || children.isEmpty()) {
      return Collections.emptyList();
    }

    List<ApiDependencyTreeNodeDTO> updatedChildren = new LinkedList<>();
    for (ApiDependencyTreeNodeDTO child : children) {
      String key;
      if (child.getPackageUrl() != null && !child.getPackageUrl().isEmpty()) {
        key = child.getPackageUrl();
      }
      else {
        ComponentIdentifier identifier = child.getComponentIdentifier().toComponentIdentifier();
        key = PackageUrlIdentifier.fromComponentIdentifier(identifier).getPackageUrl();
      }
      if (componentsIndex.containsKey(key)) {
        child.setChildren(correlateDependencyTreeWithComponentIndex(child,componentsIndex));
        child.setPackageUrl(key);
        updatedChildren.add(child);
      }
    }

    return updatedChildren;
  }

  private Map<String,BillOfMaterialsRowDTO> indexBom(JsonNode aaDataNode) throws IOException {
    Map<String,BillOfMaterialsRowDTO> components = new HashMap<>();
    if (aaDataNode == null || aaDataNode.isNull() || aaDataNode.isEmpty()) {
      return components;
    }
    
    final ArrayNode bomJsonArray = (ArrayNode) aaDataNode;
    for (JsonNode componentJson : bomJsonArray) {
      BillOfMaterialsRowDTO component = JsonUtils.asPojo(componentJson, BillOfMaterialsRowDTO.class);
      if (!component.matchState.equals("unknown")) {
        components.put(component.packageUrl, component);
      }
    }
    return components;
  }

  private List<ApiReportComponentPolicyViolationsDTOV2> getComponents(byte[] bomData, PolicyThreats policyThreats)
      throws IOException
  {
    List<ApiReportComponentPolicyViolationsDTOV2> components = new ArrayList<>();

    // violations per component
    Map<String, List<ApiReportPolicyViolationDTOV2>> violationsByHash = getPolicyViolationsByHash(policyThreats);

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
          ComponentDisplayName componentDisplayName =
              ComponentDisplayNameUtil.fromIdentifier(componentIdentifier);
          component.displayName = componentDisplayName != null ? componentDisplayName.toString() : null;
          component.proprietary = componentJson.get("proprietary").booleanValue();
          component.pathnames = getPathnames(componentJson);
          component.displayName = JsonUtils.getTypeToString(componentJson.path(ComponentDAO.DISPLAY_NAME_FIELD),
              ComponentDisplayName.class);
          component.violations = violationsByHash.getOrDefault(component.hash, Collections.emptyList());
          if (isDependencyDataInRestApiSupported()) {
            Boolean directDependency = getBooleanValue(componentJson, "directDependency");
            if (directDependency != null) {
              component.dependencyData = new ApiDependencyDataDTO();
              component.dependencyData.directDependency = directDependency;
              component.dependencyData.innerSource = getBooleanValue(componentJson, "innerSource");
              component.dependencyData.parentComponentPurls =
                  JsonUtils.getStringSetFromArray(componentJson.path(ComponentDAO.PARENT_COMPONENT_PURLS_FIELD));
              component.dependencyData.innerSourceData = JsonUtils
                  .getObjectSetFromArray(componentJson.path(ComponentDAO.INNER_SOURCE_DATA_FIELD),
                      InnerSourceData.class);
            }
          }
          components.add(component);
        }
      }
    }
    return components;
  }

  private Boolean getBooleanValue(JsonNode node, String fieldName) {
    return node.get(fieldName) == null ? null : node.get(fieldName).booleanValue();
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
    for (JsonNode pathname : componentNode.path("pathnames")) {
      pathnames.add(getPathname(pathname.asText()));
    }
    return pathnames;
  }

  private Map<String, List<ApiReportPolicyViolationDTOV2>> getPolicyViolationsByHash(PolicyThreats policyThreats) {
    return policyThreats.aaData.stream().collect(
        Collectors.toMap(o -> o.hash, policyThreats.version < 3 ? this::getLegacyViolations : this::getViolations));
  }

  private List<ApiReportPolicyViolationDTOV2> getViolations(PolicyThreats.Component component) {
    return component.allViolations.stream().map(this::getViolation).collect(toList());
  }

  private List<ApiReportPolicyViolationDTOV2> getLegacyViolations(PolicyThreats.Component component) {
    // CLM-15450 in policythreats.json allViolations, waived, and grandfathered are absent in reports generated prior to
    // 1.50.0-01 and policyViolationId is absent in reports generated prior to 1.70.0-04
    component.waivedViolations.forEach(violation -> violation.waived = true);
    component.allViolations.addAll(component.activeViolations);
    component.allViolations.addAll(component.waivedViolations);
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
    return getDataNoAuth(applicationPublicId, scanId, false);
  }

  public ApiReportRawDataDTOV2 getDataNoAuth(
      String applicationPublicId,
      String scanId,
      boolean useLicensesJsonOverriddenLicenses) throws IOException
  {
    Application app = appDAO.getByPublicIdNotNull(applicationPublicId);
    File reportFile = reportService.getReport(app.getId(), scanId);

    ReportEntry bomEntry = Report.getEntry(reportFile, Report.BOM_JSON_FILENAME);
    ReportEntry securityEntry = Report.getEntry(reportFile, Report.SECURITY_JSON_FILENAME);
    ReportEntry licenseEntry = Report.getEntry(reportFile, Report.LICENSES_JSON_FILENAME);
    ReportEntry dataEntry = Report.getEntry(reportFile, Report.DATA_JSON_FILENAME);
    ReportEntry dependenciesReportEntry = Report.getEntry(reportFile, Report.DEPENDENCIES_JSON_FILENAME);

    if (bomEntry == null || securityEntry == null || licenseEntry == null || dataEntry == null ||
        dependenciesReportEntry == null) {
      throw new BadRequestException("The report with ID " + scanId + " contains no component data.");
    }

    List<Component> components = new ComponentDAO(app).getAll(licenseEntry.buf, useLicensesJsonOverriddenLicenses,
        securityEntry.buf, bomEntry.buf, dependenciesReportEntry.buf);

    ApiReportRawDataDTOV2 data = new ApiReportRawDataDTOV2();
    for (Component comp : components) {
      ApiReportComponentDTOV2 component = new ApiReportComponentDTOV2();
      component.hash = comp.getHash();
      component.sha256 = comp.getSha256();
      component.componentIdentifier = ApiComponentIdentifierDTOV2
          .fromComponentIdentifier(comp.getComponentIdentifier());
      component.packageUrl = PackageUrlIdentifier.toPackageUrl(comp.getComponentIdentifier());
      ComponentDisplayName componentDisplayName =
          ComponentDisplayNameUtil.fromIdentifier(comp.getComponentIdentifier());
      component.displayName = componentDisplayName != null ? componentDisplayName.toString() : null;

      component.matchState = comp.getMatchState().getId();
      component.proprietary = comp.isProprietary();
      setPathnames(comp, component);
      component.displayName = comp.getDisplayName();
      component.identificationSource =
          comp.getIdentificationSource() == null ? null : comp.getIdentificationSource().getName();
      if (!MatchState.UNKNOWN.equals(comp.getMatchState())) {
        component.securityData = securityDataAdapter.convertToDTO(comp);
        component.licenseData = licenseDataAdapter.convertToDTOV2(comp);
      }

      if (isDependencyDataInRestApiSupported()) {
        populateDependencyData(comp, component);
      }
      data.components.add(component);
    }

    ContainerNode<?> dataJson = JsonUtils.parse(dataEntry.buf);
    data.matchSummary.knownComponentCount = dataJson.get("knownArtifactCount").intValue();
    data.matchSummary.totalComponentCount = dataJson.get("totalArtifactCount").intValue();
    addGlobalInformation(dataJson, data);
    return data;
  }

  private void addGlobalInformation(ContainerNode<?> dataJson, ApiReportRawDataDTOV2 data) throws IOException {
    JsonNode globalsNode = dataJson.get("globals");

    Map<String, JsonNode> map = JsonUtils.asPojo(globalsNode, Map.class);

    if (map != null && map.containsKey("dataVersionDate")) {
      data.globalInformation.dataVersionDate = String.valueOf(map.get("dataVersionDate"));
    }
  }

  private boolean isDependencyDataInRestApiSupported() {
    return SystemConfigurationPropertyFeature.DEPENDENCY_DATA_IN_API.isEnabled();
  }

  private void populateDependencyData(final Component comp, final ApiReportComponentDTOV2 component) {
    if (comp.getDirectDependency() != null) {
      component.dependencyData = new ApiDependencyDataDTO();
      component.dependencyData.directDependency = comp.getDirectDependency();
      if (comp.getParentComponentPurls() != null) {
        component.dependencyData.parentComponentPurls = comp.getParentComponentPurls();
      }
      if (comp.getInnerSource() != null) {
        component.dependencyData.innerSource = comp.getInnerSource();
      }
      if (comp.getInnerSourceData() != null) {
        component.dependencyData.innerSourceData = comp.getInnerSourceData();
      }
    }
  }

  private void setPathnames(Component comp, ApiReportComponentDTOV2 component) {
    for (String pathname : comp.getPathnames()) {
      component.pathnames.add(getPathname(pathname));
    }
  }

  private String getPathname(String pathname) {
    if (!pathname.startsWith(DEPENDENCY_PREFIX)) {
      return pathname;
    }
    else {
      return StringUtils.removeStart(pathname, DEPENDENCY_PREFIX + "/");
    }
  }
}
