/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.clm.dto.model.DerivedFromAiModel;
import com.sonatype.clm.dto.model.component.AiModelContentType;
import com.sonatype.clm.dto.model.component.ComponentDisplayName;
import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.ApiAiModelContentTypeDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiAiModelDataDTO;
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
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.dataaccess.component.ComponentLoader;
import com.sonatype.insight.brain.dataaccess.component.ComponentLoaderFactory;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.InnerSourceData;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.organization.ReportMetadataDTO;
import com.sonatype.insight.brain.policy.evaluator.PolicyThreats;
import com.sonatype.insight.brain.policy.evaluator.PolicyThreats.PolicyCondition;
import com.sonatype.insight.brain.policy.evaluator.PolicyThreats.PolicyConstraint;
import com.sonatype.insight.brain.policy.evaluator.PolicyThreats.PolicyViolation;
import com.sonatype.insight.brain.report.ApplicationReport;
import com.sonatype.insight.brain.report.ReportEntry;
import com.sonatype.insight.brain.report.ReportService;
import com.sonatype.insight.brain.sbom.utils.SbomCycloneDxUtils;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.thirdparty.ThirdPartyComponentDAO;
import com.sonatype.insight.brain.thirdparty.ThirdPartyReportComponentDTO;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ContainerNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.report.ApplicationReport.ReportFile.BOM_JSON;
import static com.sonatype.insight.brain.report.ApplicationReport.ReportFile.DATA_JSON;
import static com.sonatype.insight.brain.report.ApplicationReport.ReportFile.DEPENDENCIES_JSON;
import static com.sonatype.insight.brain.report.ApplicationReport.ReportFile.LICENSES_JSON;
import static com.sonatype.insight.brain.report.ApplicationReport.ReportFile.POLICY_THREATS;
import static com.sonatype.insight.brain.report.ApplicationReport.ReportFile.SECURITY_JSON;
import static com.sonatype.insight.brain.utils.DependencyTreeDirectFlagProcessor.populateDirectFlags;
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

  private final ApplicationDAO appDAO;

  private final PolicyViolationDAO policyViolationDAO;

  private final ReportService reportService;

  private final ApiLicenseDataAdapter licenseDataAdapter;

  private final ApiSecurityDataAdapter securityDataAdapter;

  private final ComponentLoaderFactory componentLoaderFactory;

  private final ThirdPartyComponentDAO thirdPartyComponentDAO;

  @Inject
  public ApiReportDataServiceV2(
      ApplicationDAO appDAO,
      PolicyViolationDAO policyViolationDAO,
      ReportService reportService,
      ApiLicenseDataAdapter licenseDataAdapter,
      ApiSecurityDataAdapter securityDataAdapter,
      ComponentLoaderFactory componentLoaderFactory,
      ThirdPartyComponentDAO thirdPartyComponentDAO)
  {
    this.appDAO = appDAO;
    this.policyViolationDAO = policyViolationDAO;
    this.reportService = reportService;
    this.licenseDataAdapter = licenseDataAdapter;
    this.securityDataAdapter = securityDataAdapter;
    this.componentLoaderFactory = componentLoaderFactory;
    this.thirdPartyComponentDAO = thirdPartyComponentDAO;
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
      String scanId,
      boolean includeViolationTimes) throws IOException
  {
    return getPolicyViolationsDataNoAuth(applicationPublicId, scanId, includeViolationTimes);
  }

  public ApiReportPolicyDataDTOV2 getPolicyViolationsDataNoAuth(
      String applicationPublicId,
      String scanId,
      boolean includeViolationTimes) throws IOException
  {
    Application app = appDAO.getByPublicIdNotNull(applicationPublicId);
    ApplicationReport applicationReport = reportService.getReport(app.getId(), scanId);

    Map<String, ReportEntry> entries = applicationReport.getEntries(List.of(
        BOM_JSON.getName(),
        DATA_JSON.getName(),
        POLICY_THREATS.getName()));
    ReportEntry bomEntry = entries.get(BOM_JSON.getName());
    ReportEntry countsEntry = entries.get(DATA_JSON.getName());
    ReportEntry policyThreatsEntry = entries.get(POLICY_THREATS.getName());

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

    if (includeViolationTimes) {
      includeViolationTimes(data);
    }

    return data;
  }

  @Authorize(permission = Permission.READ)
  public ApiDependencyTreeResponseDTO getDependencyTree(
      @AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) String applicationPublicId,
      final String scanId) throws IOException
  {
    return new ApiDependencyTreeResponseDTO(getDependencyTreeNoAuth(applicationPublicId, scanId));
  }

  public ApiDependencyTreeNodeDTO getDependencyTreeNoAuth(
      String applicationPublicId,
      String scanId) throws IOException
  {
    ApiDependencyTreeNodeDTO dependencyTree = new ApiDependencyTreeNodeDTO();
    Application application = appDAO.getByPublicIdNotNull(applicationPublicId);
    String appId = application.getId();

    ApplicationReport applicationReport = reportService.getReport(appId, scanId);
    ReportEntry dependenciesEntry = applicationReport.getEntry(DEPENDENCIES_JSON.getName());
    if (dependenciesEntry != null) {
      JsonNode dependenciesNode = JsonUtils.parse(dependenciesEntry.buf);
      if (dependenciesNode != null) {
        JsonNode dependencyTreeNode = dependenciesNode.get("dependencyTree");
        if (dependencyTreeNode != null && !dependencyTreeNode.isNull() && !dependencyTreeNode.isEmpty()) {
          ObjectNode dependencyTreeObject = (ObjectNode) dependencyTreeNode;
          dependencyTreeObject.remove("componentIdentifier");
          dependencyTree = JsonUtils.asPojo(dependencyTreeObject, ApiDependencyTreeNodeDTO.class);
          dependencyTree.setChildren(resolveDependencyTreePackageUrls(dependencyTree));

          // CLM-36797 - travers dependencies and set direct flag to indicate if they are a direct dependency or not
          populateDirectFlags(dependencyTree);
        }
      }
    }
    return dependencyTree;
  }

  // CLM-36995: resolves package URLs for all dependency tree nodes without filtering.
  // Previously this method filtered nodes against a BOM index, dropping unknown-matchState
  // components and their entire subtrees. Now all nodes are included to match the UI.
  private List<ApiDependencyTreeNodeDTO> resolveDependencyTreePackageUrls(ApiDependencyTreeNodeDTO root) {
    List<ApiDependencyTreeNodeDTO> children = root.getChildren();
    if (children == null || children.isEmpty()) {
      return Collections.emptyList();
    }

    List<ApiDependencyTreeNodeDTO> updatedChildren = new LinkedList<>();
    for (ApiDependencyTreeNodeDTO child : children) {
      String key = resolvePackageUrl(child);
      child.setChildren(resolveDependencyTreePackageUrls(child));
      child.setPackageUrl(key);
      updatedChildren.add(child);
    }

    return updatedChildren;
  }

  private static String resolvePackageUrl(ApiDependencyTreeNodeDTO node) {
    if (StringUtils.isNotEmpty(node.getPackageUrl())) {
      return node.getPackageUrl();
    }
    if (node.getComponentIdentifier() == null) {
      return null;
    }
    ComponentIdentifier identifier = node.getComponentIdentifier().toComponentIdentifier();
    PackageUrlIdentifier purlId = PackageUrlIdentifier.fromComponentIdentifier(identifier);
    return purlId != null ? purlId.getPackageUrl() : null;
  }

  private List<ApiReportComponentPolicyViolationsDTOV2> getComponents(
      byte[] bomData,
      PolicyThreats policyThreats) throws IOException
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
          component.displayName = JsonUtils.getTypeToString(componentJson.path(ComponentLoader.DISPLAY_NAME_FIELD),
              ComponentDisplayName.class);
          component.originalPurl = JsonUtils.getNullableString(componentJson.get("originalPurl"));
          component.violations = violationsByHash.getOrDefault(component.hash, Collections.emptyList());
          if (isDependencyDataInRestApiSupported()) {
            Boolean directDependency = getBooleanValue(componentJson, "directDependency");
            if (directDependency != null) {
              component.dependencyData = new ApiDependencyDataDTO();
              component.dependencyData.directDependency = directDependency;
              component.dependencyData.innerSource = getBooleanValue(componentJson, "innerSource");
              component.dependencyData.parentComponentPurls =
                  JsonUtils.getStringSetFromArray(componentJson.path(ComponentLoader.PARENT_COMPONENT_PURLS_FIELD));
              component.dependencyData.innerSourceData = JsonUtils
                  .getObjectSetFromArray(componentJson.path(ComponentLoader.INNER_SOURCE_DATA_FIELD),
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

  private static ApiApplicationBaseDTO getApplicationMetadata(Application application) {
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
    reportCounts.put("legacyViolationCount",
        countsData.getOrDefault("legacyViolationCount", countsData.get("grandfatheredPolicyViolationCount")));
    return reportCounts;
  }

  private List<String> getPathnames(JsonNode componentNode) {
    List<String> pathnames = new ArrayList<>();
    for (JsonNode pathname : componentNode.path("pathnames")) {
      pathnames.add(SbomCycloneDxUtils.getFilteredPathname(pathname.asText()));
    }
    return pathnames;
  }

  private Map<String, List<ApiReportPolicyViolationDTOV2>> getPolicyViolationsByHash(PolicyThreats policyThreats) {
    return policyThreats.aaData.stream()
        .collect(
            Collectors.toMap(o -> o.hash, policyThreats.version < 3 ? this::getLegacyViolations : this::getViolations));
  }

  private List<ApiReportPolicyViolationDTOV2> getViolations(PolicyThreats.Component component) {
    return component.allViolations.stream().map(this::getViolation).collect(toList());
  }

  private List<ApiReportPolicyViolationDTOV2> getLegacyViolations(PolicyThreats.Component component) {
    // CLM-15450 in policythreats.json allViolations, waived, and legacy are absent in reports generated prior to
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
    dto.legacyViolation = violation.legacyViolation || violation.grandfathered;
    dto.waived = violation.waived;
    dto.waivedWithAutoWaiver = violation.waivedWithAutoWaiver;
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

  public ApiReportRawDataDTOV2 getDataNoAuthWithDependencyData(
      final String applicationPublicId,
      final String scanId) throws IOException
  {
    return getDataNoAuth(applicationPublicId, scanId, false, true);
  }

  public ApiReportRawDataDTOV2 getDataNoAuth(
      final String applicationPublicId,
      final String scanId,
      final boolean useLicensesJsonOverriddenLicenses) throws IOException
  {
    final boolean doAddDependencyData = isDependencyDataInRestApiSupported();
    return getDataNoAuth(applicationPublicId, scanId, useLicensesJsonOverriddenLicenses, doAddDependencyData);
  }

  public ApiReportRawDataDTOV2 getDataNoAuth(
      final String applicationPublicId,
      final String scanId,
      final boolean useLicensesJsonOverriddenLicenses,
      final boolean doAddDependencyData) throws IOException
  {
    Application app = appDAO.getByPublicIdNotNull(applicationPublicId);
    ApplicationReport applicationReport = reportService.getReport(app.getId(), scanId);

    Map<String, ReportEntry> entries = applicationReport.getEntries(List.of(
        BOM_JSON.getName(),
        SECURITY_JSON.getName(),
        LICENSES_JSON.getName(),
        DATA_JSON.getName(),
        DEPENDENCIES_JSON.getName()));
    ReportEntry bomEntry = entries.get(BOM_JSON.getName());
    ReportEntry securityEntry = entries.get(SECURITY_JSON.getName());
    ReportEntry licenseEntry = entries.get(LICENSES_JSON.getName());
    ReportEntry dataEntry = entries.get(DATA_JSON.getName());
    ReportEntry dependenciesReportEntry = entries.get(DEPENDENCIES_JSON.getName());

    if (bomEntry == null || securityEntry == null || licenseEntry == null || dataEntry == null ||
        dependenciesReportEntry == null)
    {
      throw new BadRequestException("The report with ID " + scanId + " contains no component data.");
    }

    List<Component> components =
        componentLoaderFactory.createComponentLoader(app)
            .getAll(licenseEntry.buf, useLicensesJsonOverriddenLicenses,
                securityEntry.buf, bomEntry.buf, dependenciesReportEntry.buf);

    Map<String, ThirdPartyReportComponentDTO> tpComponentsByHash = thirdPartyComponentDAO.getData(applicationReport);

    ApiReportRawDataDTOV2 data = new ApiReportRawDataDTOV2();
    for (Component comp : components) {
      ApiReportComponentDTOV2 component = new ApiReportComponentDTOV2();
      component.hash = comp.getHash();
      component.sha256 = comp.getSha256();
      component.componentIdentifier = ApiComponentIdentifierDTOV2
          .fromComponentIdentifier(comp.getComponentIdentifier());

      component.packageUrl = StringUtils.isNotBlank(comp.getPackageUrl())
          ? comp.getPackageUrl()
          : PackageUrlIdentifier.toPackageUrl(comp.getComponentIdentifier());

      component.originalPurl = comp.getOriginalPurl();

      ComponentDisplayName componentDisplayName =
          ComponentDisplayNameUtil.fromIdentifier(comp.getComponentIdentifier());
      component.displayName = componentDisplayName != null ? componentDisplayName.toString() : null;

      component.matchState = comp.getMatchState().getId();
      component.proprietary = comp.isProprietary();
      setPathnames(comp, component);
      setFilenames(comp, component);
      component.displayName = comp.getDisplayName();
      component.identificationSource =
          comp.getIdentificationSource() == null ? null : comp.getIdentificationSource().getName();
      if (!MatchState.UNKNOWN.equals(comp.getMatchState())) {
        component.securityData = securityDataAdapter.convertToDTO(comp);
        component.licenseData = licenseDataAdapter.convertToDTOV2(comp);
        populateAiModelData(comp, component);
      }

      if (doAddDependencyData) {
        populateDependencyData(comp, component);
      }

      if (tpComponentsByHash.containsKey(comp.getHash())) {
        ThirdPartyReportComponentDTO tpComp = tpComponentsByHash.get(comp.getHash());
        if (tpComp.bomRow != null) {
          component.cpe = tpComp.bomRow.cpe;
          component.swid = tpComp.bomRow.swid;
        }
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

  private static void populateAiModelData(Component comp, ApiReportComponentDTOV2 component) {
    boolean hasContentTypes = !comp.getAiModelContentTypes().isEmpty();
    boolean hasDerivedFrom = comp.getDerivedFromAiModel() != null;
    if (!hasContentTypes && !hasDerivedFrom) {
      return;
    }

    component.aiModelData = new ApiAiModelDataDTO();

    for (AiModelContentType contentType : comp.getAiModelContentTypes()) {
      ApiAiModelContentTypeDTO dto = new ApiAiModelContentTypeDTO();
      dto.id = contentType.getId();
      dto.name = contentType.getName();
      component.aiModelData.contentTypes.add(dto);
    }

    if (hasDerivedFrom) {
      DerivedFromAiModel derivedFrom = comp.getDerivedFromAiModel();
      component.aiModelData.derivedFromComponentIdentifier =
          ApiComponentIdentifierDTOV2.fromComponentIdentifier(derivedFrom.getComponentIdentifier());
      component.aiModelData.derivedFromSimilarityScore = derivedFrom.getSimilarityScore();
    }
  }

  private void setPathnames(Component comp, ApiReportComponentDTOV2 component) {
    for (String pathname : comp.getPathnames()) {
      component.pathnames.add(SbomCycloneDxUtils.getFilteredPathname(pathname));
    }
  }

  private void setFilenames(Component comp, ApiReportComponentDTOV2 component) {
    for (String filename : comp.getFilenames()) {
      component.filenames.add(filename);
    }
  }

  private void includeViolationTimes(final ApiReportPolicyDataDTOV2 apiReportPolicyDataDTOV2) {
    Set<ApiReportPolicyViolationDTOV2> policyViolations = apiReportPolicyDataDTOV2.components.stream()
        .filter(Objects::nonNull)
        .flatMap(component -> component.violations.stream())
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());

    Set<String> policyViolationIds = policyViolations.stream()
        .map(violation -> violation.policyViolationId)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());

    Map<String, com.sonatype.insight.brain.model.policy.PolicyViolation> policyViolationById =
        policyViolationDAO.getByIds(policyViolationIds)
            .stream()
            .collect(
                Collectors.toMap(com.sonatype.insight.brain.model.policy.PolicyViolation::getId, Function.identity()));

    for (ApiReportPolicyViolationDTOV2 policyViolation : policyViolations) {
      com.sonatype.insight.brain.model.policy.PolicyViolation policyViolationFromDb =
          policyViolationById.get(policyViolation.policyViolationId);
      if (policyViolationFromDb != null) {
        policyViolation.openTime = policyViolationFromDb.getOpenTime();
        policyViolation.waiveTime = policyViolationFromDb.getWaiveTime();
        policyViolation.fixTime = policyViolationFromDb.getFixTime();
        policyViolation.legacyViolationTime = policyViolationFromDb.getLegacyViolationTime();
      }
    }
  }
}
