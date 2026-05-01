/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;

import static com.sonatype.insight.brain.model.license.License.UNSPECIFIED_ID;

import com.sonatype.clm.dto.model.License;
import com.sonatype.clm.dto.model.SecurityVulnerability;
import com.sonatype.clm.dto.model.component.ComponentCategory;
import com.sonatype.clm.dto.model.component.ComponentDetails;
import com.sonatype.clm.dto.model.component.ComponentDetailsList;
import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.NamedComponentDetails;
import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.api.v2.dto.remediation.ApiComponentRemediationValueDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionDTO;
import com.sonatype.insight.brain.api.v2.service.ApiComponentDetailsServiceV2;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.ComponentCategoryDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.dataaccess.license.LicenseDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlEventDAO;
import com.sonatype.insight.brain.git.ManualPullRequestImpossibilityReason;
import com.sonatype.insight.brain.git.ManualPullRequestService;
import com.sonatype.insight.brain.git.utils.PullRequestBranchNameGenerator;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.HashHelper;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.DependencyType;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.license.MultiLicense;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.DevelopStageType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.policy.evaluator.ComponentPolicyEvaluator;
import com.sonatype.insight.brain.report.ReportDataReader;
import com.sonatype.insight.brain.repository.RepositoryAllVersionsResponse;
import com.sonatype.insight.brain.repository.RepositoryClient;
import com.sonatype.insight.brain.repository.RepositoryComponentResult;
import com.sonatype.insight.brain.repository.RepositoryQueryService;
import com.sonatype.insight.brain.repository.RepositorySourceResponseDTO;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyScanDAO;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyScan;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.telemetry.NonBreakingRecommendationTelemetryStats.SourceEndpoint;
import com.sonatype.insight.brain.thirdparty.ThirdPartyComponentDAO;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.lqa.LqaFormat;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.scan.util.HashUtils;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.apache.shiro.authz.UnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.IdentificationSource.isThirdPartyIdentificationSource;
import static com.sonatype.insight.brain.telemetry.NonBreakingRecommendationTelemetryStats.SourceEndpoint.COMPONENT_INFO;

@Named
public class ComponentInfoService
{
  private static final Logger log = LoggerFactory.getLogger(ComponentInfoService.class);

  private static final String COMPONENT_DETAILS_PURPOSE = "integration";

  private static final String VERSIONS_BY_COMPONENT_ENDPOINT_URL = "rest/component/versions/list";

  private License unspecifiedLicense;

  private List<ComponentCategory> otherComponentCategories;

  private final ApplicationDAO applicationDAO;

  private final LicenseDAO licenseDAO;

  private final ComponentCategoryDAO componentCategoryDAO;

  private final LicenseThreatGroupDAO licenseThreatGroupDAO;

  private final OwnerDAO ownerDAO;

  private final PolicyDAO policyDAO;

  private final SourceControlEventDAO sourceControlEventDAO;

  private final HdsClient hdsClient;

  private final ComponentPolicyEvaluator componentPolicyEvaluator;

  private final ComponentDetailsLoaderFactory componentDetailsLoaderFactory;

  private final ComponentRemediationService componentRemediationService;

  private final ThirdPartyComponentDAO thirdPartyComponentDAO;

  private final RepositoryQueryService repositoryQueryService;

  private final ApiComponentDetailsServiceV2 apiComponentDetailsServiceV2;

  private final ManualPullRequestService manualPullRequestService;

  private final MultiLicenseDAO multiLicenseDAO;

  private final IdUtils idUtils;

  private final PullRequestBranchNameGenerator pullRequestBranchNameGenerator;

  private final ReportDataReader reportDataReader;

  private final ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO;

  private final ThirdPartyScanDAO thirdPartyScanDAO;

  private static final String OTHER_CATEGORY_ID = "113";

  private String toolName;

  @Inject
  public ComponentInfoService(
      HdsClient hdsClient,
      ComponentPolicyEvaluator componentPolicyEvaluator,
      ComponentDetailsLoaderFactory componentDetailsLoaderFactory,
      ComponentRemediationService componentRemediationService,
      ThirdPartyComponentDAO thirdPartyComponentDAO,
      RepositoryQueryService repositoryQueryService,
      ApiComponentDetailsServiceV2 apiComponentDetailsServiceV2,
      MultiLicenseDAO multiLicenseDAO,
      ApplicationDAO applicationDAO,
      LicenseDAO licenseDAO,
      ComponentCategoryDAO componentCategoryDAO,
      LicenseThreatGroupDAO licenseThreatGroupDAO,
      final OwnerDAO ownerDAO,
      final PolicyDAO policyDAO,
      final SourceControlEventDAO sourceControlEventDAO,
      final IdUtils idUtils,
      ManualPullRequestService manualPullRequestService,
      PullRequestBranchNameGenerator pullRequestBranchNameGenerator,
      ReportDataReader reportDataReader,
      ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO,
      ThirdPartyScanDAO thirdPartyScanDAO)
  {
    this.hdsClient = hdsClient;
    this.componentPolicyEvaluator = componentPolicyEvaluator;
    this.componentDetailsLoaderFactory = componentDetailsLoaderFactory;
    this.componentRemediationService = componentRemediationService;
    this.thirdPartyComponentDAO = thirdPartyComponentDAO;
    this.repositoryQueryService = repositoryQueryService;
    this.apiComponentDetailsServiceV2 = apiComponentDetailsServiceV2;
    this.multiLicenseDAO = multiLicenseDAO;
    this.applicationDAO = applicationDAO;
    this.licenseDAO = licenseDAO;
    this.componentCategoryDAO = componentCategoryDAO;
    this.licenseThreatGroupDAO = licenseThreatGroupDAO;
    this.ownerDAO = ownerDAO;
    this.policyDAO = policyDAO;
    this.sourceControlEventDAO = sourceControlEventDAO;
    this.idUtils = idUtils;
    this.manualPullRequestService = manualPullRequestService;
    this.pullRequestBranchNameGenerator = pullRequestBranchNameGenerator;
    this.reportDataReader = reportDataReader;
    this.thirdPartySbomMetadataDAO = thirdPartySbomMetadataDAO;
    this.thirdPartyScanDAO = thirdPartyScanDAO;
    initUnspecifiedLicense();
    initOtherCategory();
  }

  @Authorize(permission = Permission.EVALUATE_COMPONENT)
  public NamedComponentDetails getComponentDetails_EvaluateComponentPermission(
      @AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) String applicationPublicId,
      ComponentIdentifier identifier,
      String matchState,
      String hash,
      boolean proprietary,
      HttpServletRequest httpRequest) throws IOException
  {
    auditComponentAccess(identifier, hash);
    Application app = applicationDAO.getByPublicIdNotNull(applicationPublicId);
    NamedComponentDetails details = getComponentDetails(app, identifier, matchState, hash, proprietary, httpRequest);

    return details;
  }

  @Authorize(permission = Permission.READ)
  public NamedComponentDetails getComponentDetails_ReadPermission(
      @AuthzContext(AuthzContext.Key.TYPE) final OwnerType ownerType,
      @AuthzContext(AuthzContext.Key.ID) final String ownerId,
      ComponentIdentifier componentIdentifier,
      String matchState,
      String hash,
      boolean proprietary,
      HttpServletRequest httpRequest,
      String identificationSource,
      String scanId,
      DependencyType dependencyType) throws IOException
  {
    auditComponentAccess(componentIdentifier, hash);
    final Owner owner = idUtils.getOwnerNotNull(ownerType, ownerId);
    return getComponentDetails(owner, componentIdentifier, matchState, hash, proprietary, httpRequest,
        identificationSource, scanId, dependencyType);
  }

  public NamedComponentDetails getComponentDetails(
      Owner owner,
      final ComponentIdentifier identifier,
      String matchState,
      String hash,
      boolean proprietary,
      HttpServletRequest httpRequest) throws IOException
  {
    return getComponentDetails(owner, identifier, matchState, hash, proprietary, httpRequest, null, null, null);
  }

  NamedComponentDetails getComponentDetails(
      Owner owner,
      final ComponentIdentifier identifier,
      String matchState,
      String hash,
      boolean proprietary,
      HttpServletRequest httpRequest,
      String identificationSource,
      String scanId,
      DependencyType dependencyType) throws IOException
  {
    long start = System.currentTimeMillis();

    // clients like Nexus provide full SHA1 values
    hash = HashHelper.truncateHash(hash);

    NamedComponentDetails componentDetails;

    if (identifier == null) {
      // See CLM-4195
      componentDetails = createEmptyComponentDetails(hash, identifier);
    }
    else {
      componentDetails = getComponentDetailsBasedOnSource(identifier, owner, scanId, matchState, hash, httpRequest,
          identificationSource);
      augmentEmptyLicensesAsUnspecified(componentDetails);
      augmentEmptyCategoriesAsOther(componentDetails);
    }

    Component component = componentDetailsLoaderFactory.newInstance(owner)
        .augmentComponentDetails(componentDetails, dependencyType);
    component.setProprietary(proprietary);

    // Evaluate the policies
    List<PolicyAlert> policyAlerts = componentPolicyEvaluator.evaluate(owner.getId(), new Stage(DevelopStageType.ID),
        Collections.singletonList(component));
    componentDetails.setPolicyAlerts(policyAlerts);

    Map<PolicyThreatCategory, Integer> policyMaxThreatLevelsByCategory;
    Map<String, Policy> policiesById = getPoliciesById(owner);
    policyMaxThreatLevelsByCategory = getMaxPolicyThreatLevelsByCategory(policyAlerts, policiesById);
    componentDetails.setPolicyMaxThreatLevelsByCategory(maxPolicyThreatLevelToString(policyMaxThreatLevelsByCategory));

    if (identifier != null) {
      try {
        String packageUrl = PackageUrlIdentifier.toPackageUrl(identifier);
        componentDetails.setPackageUrl(packageUrl);
      }
      catch (RuntimeException e) {
        // Some component identifiers may have invalid names for Package URL format
        // Log and continue without setting packageUrl
        log.debug("Could not generate packageUrl for identifier {}: {}", identifier, e.getMessage());
      }
    }

    log.debug("Loaded component details for {}, hash {}, in {} ms.", identifier, hash, System.currentTimeMillis()
        - start);

    return componentDetails;
  }

  private NamedComponentDetails getComponentDetailsBasedOnSource(
      ComponentIdentifier identifier,
      Owner owner,
      String scanId,
      String matchState,
      String hash,
      HttpServletRequest httpRequest,
      String identificationSource) throws IOException
  {
    // Case 1: SBOM identification source
    if (IdentificationSource.SBOM.getId().equals(identificationSource)) {
      if (ComponentIdentifier.isFormatValidForCpeMatching(identifier.getFormat())) {
        // CPE/generic formats: use report data directly (not HDS-supported)
        return reportDataReader.getComponentDetailsByIdentifier(identifier, owner.getId(), scanId);
      }
      // HDS-supported formats: try HDS for known components, fall back to report data for unknown
      NamedComponentDetails hdsDetails =
          getComponentDetailsFromHDS(matchState, hash, identifier, httpRequest, identificationSource);
      if (hdsDetails != null && !hasOnlyUnspecifiedLicenses(hdsDetails)) {
        return hdsDetails;
      }
      String resolvedScanId = resolveSbomScanId(scanId, owner);
      if (resolvedScanId != null) {
        NamedComponentDetails reportDetails =
            reportDataReader.getComponentDetailsByIdentifier(identifier, owner.getId(), resolvedScanId);
        if (reportDetails != null) {
          return reportDetails;
        }
      }
      return hdsDetails;
    }

    boolean isFromFirewallForContainers =
        ComponentIdentifier.FORMAT_CONTAINER.equals(identifier.getFormat());

    // Case 2: Vulnerability from Firewall for Containers, use report data
    if (IdentificationSource.SONATYPE_CONTAINER.getId().equals(identificationSource) && isFromFirewallForContainers) {
      return getComponentDetailsFromFirewallForContainers(identifier, owner, scanId);
    }

    // Case 3: Third-party identification source
    if (isThirdPartyIdentificationSource(identificationSource)) {
      return thirdPartyComponentDAO.getComponentDetailsByIdentifier(identifier, owner.getId(), scanId);
    }

    // Default case: Get details from Hosted Data Services
    return getComponentDetailsFromHDS(matchState, hash, identifier, httpRequest, identificationSource);
  }

  private String resolveSbomScanId(String scanId, Owner owner) {
    if (owner == null) {
      return scanId;
    }
    if (scanId == null) {
      ThirdPartySbomMetadata latest = thirdPartySbomMetadataDAO.getLatestActiveByApplicationId(owner.getId());
      if (latest != null) {
        ThirdPartyScan scan = thirdPartyScanDAO.getByThirdPartyFileId(latest.getThirdPartyFileId());
        if (scan != null) {
          return scan.getScanId();
        }
      }
      return null;
    }
    if (thirdPartySbomMetadataDAO.getByScanId(scanId) != null) {
      return scanId;
    }
    ThirdPartySbomMetadata metadata =
        thirdPartySbomMetadataDAO.getByApplicationIdAndSbomVersion(owner.getId(), scanId);
    if (metadata != null) {
      ThirdPartyScan scan = thirdPartyScanDAO.getByThirdPartyFileId(metadata.getThirdPartyFileId());
      if (scan != null) {
        return scan.getScanId();
      }
    }
    return scanId;
  }

  private static boolean hasOnlyUnspecifiedLicenses(NamedComponentDetails details) {
    Set<License> declared = details.getDeclaredLicenses();
    if (declared == null || declared.isEmpty()) {
      return true;
    }
    return declared.size() == 1 &&
        UNSPECIFIED_ID.equals(declared.iterator().next().getLicenseId());
  }

  private NamedComponentDetails getComponentDetailsFromFirewallForContainers(
      ComponentIdentifier identifier,
      Owner owner,
      String scanId)
  {
    NamedComponentDetails namedComponentDetails =
        reportDataReader.getComponentDetailsByIdentifier(identifier, owner.getId(), scanId);

    if (namedComponentDetails != null && namedComponentDetails.getSecurityVulnerabilities() != null) {
      namedComponentDetails.getSecurityVulnerabilities()
          .stream()
          .forEach(
              vulnerability -> vulnerability.setIdentificationSource(IdentificationSource.SONATYPE_CONTAINER.getId()));
    }

    return namedComponentDetails;
  }

  // Visible for testing
  public NamedComponentDetails getComponentDetailsFromHDS(
      String matchState,
      final String hash,
      final ComponentIdentifier identifier,
      final HttpServletRequest httpRequest,
      final String identificationSource) throws IOException
  {
    return ComponentDetailsLoader.getComponentDetails(identifier, hash, matchState,
        new ComponentDetailsLoader.HostedDataServicesSource()
        {
          @Override
          public NamedComponentDetails getDetails() throws IOException {
            NamedComponentDetails componentDetails;

            Map<String, String> queryParams = new HashMap<>();
            queryParams.put("componentIdentifier", ComponentIdentifierAdapter.toJson(identifier));
            if (hash != null) {
              queryParams.put("hash", hash);
            }

            try {
              componentDetails = hdsClient.relay(httpRequest, NamedComponentDetails.class, "rest/" + toolName
                  + "/componentDetails", queryParams).content;
              if (componentDetails == null) {
                // HDS returned 204 No content
                return createEmptyComponentDetails(hash, identifier);
              }
              componentDetails.setMatchState(MatchState.EXACT.getId());
            }
            catch (NotFoundException e) {
              // Identifier is unknown to HDS, still want to provide minimal data for details view
              if (isPackageManifestOrExternalRepoIdentificationSource(identificationSource)) {
                componentDetails =
                    createComponentDetails(hash, identifier, IdentificationSource.getById(identificationSource));
              }
              else {
                componentDetails = createEmptyComponentDetails(hash, identifier);
              }
            }
            return componentDetails;
          }
        });
  }

  // Intended for unknown cases
  private NamedComponentDetails createEmptyComponentDetails(String hash, ComponentIdentifier identifier) {
    NamedComponentDetails details = new NamedComponentDetails();
    details.setComponentIdentifier(identifier);
    details.setHash(hash);
    details.setMatchState(MatchState.UNKNOWN.getId());
    return details;
  }

  private NamedComponentDetails createComponentDetails(
      String hash,
      ComponentIdentifier identifier,
      IdentificationSource identificationSource)
  {
    NamedComponentDetails details = new NamedComponentDetails();
    details.setComponentIdentifier(identifier);
    details.setHash(hash);
    details.setMatchState(MatchState.EXACT.getId());
    details.setIdentificationSource(identificationSource.getId());
    return details;
  }

  private void initUnspecifiedLicense() {
    final com.sonatype.insight.brain.model.license.License licenseNotProvided =
        licenseDAO.getByIdNotNull(com.sonatype.insight.brain.model.license.License.UNSPECIFIED_ID);
    unspecifiedLicense = new License(licenseNotProvided.getId(), licenseNotProvided.getShortDisplayName());
  }

  private void initOtherCategory() {
    com.sonatype.insight.brain.model.component.ComponentCategory otherComponentCategory =
        componentCategoryDAO.getById(OTHER_CATEGORY_ID);
    otherComponentCategories = Collections.singletonList(
        new ComponentCategory(Integer.parseInt(otherComponentCategory.getId()), otherComponentCategory.getPath()));
  }

  private void augmentEmptyLicensesAsUnspecified(ComponentDetails componentDetails) {
    if (componentDetails != null) {
      if (componentDetails.getDeclaredLicenses().isEmpty()) {
        componentDetails.getDeclaredLicenses().add(unspecifiedLicense);
      }
      if (componentDetails.getObservedLicenses().isEmpty()) {
        componentDetails.getObservedLicenses().add(unspecifiedLicense);
      }
    }
  }

  private void augmentEmptyCategoriesAsOther(ComponentDetails componentDetails) {
    if (componentDetails != null && CollectionUtils.isEmpty(componentDetails.getComponentCategories())) {
      componentDetails.setComponentCategories(otherComponentCategories);
    }
  }

  private String generateFakeHash(ComponentIdentifier componentIdentifier) {
    StringBuilder plainText = new StringBuilder(componentIdentifier.getFormat());
    for (String coordinate : componentIdentifier.getCoordinates().values()) {
      plainText.append(":").append(coordinate);
    }
    String hash = HashUtils.hash(plainText.toString(), HashUtils.SHA1);
    return HashHelper.truncateHash(hash);
  }

  /**
   * Returns a list of component details for the given application and component identifier. It does not evaluate
   * policies and it does not return policy violations.
   *
   * @deprecated since 1.48. Not used by Insight or plugins, but left here as our customers use these APIs.
   */
  @Deprecated
  @Authorize(permission = Permission.EVALUATE_COMPONENT)
  public ComponentDetailsList getComponentDetailsList_EvaluateComponentPermission(
      @AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) String applicationPublicId,
      ComponentIdentifier identifier,
      String matchState)
  {
    auditComponentAccess(identifier, null);
    Application app = applicationDAO.getByPublicIdNotNull(applicationPublicId);
    ComponentDetailsList componentDetailsList = getComponentDetailsList(identifier, null, null, null, null,
        false).getLeft();
    componentDetailsLoaderFactory.newInstance(app)
        .augmentComponentDetails(componentDetailsList.getList(), matchState,
            null);
    return componentDetailsList;
  }

  /**
   * Returns a list of component details for the given application and component identifier.
   * It also evaluates policies and returns max threat levels per category, as well as count of violated policies.
   *
   * This method is called by the IDE and RM plugins, so it needs to check the EVALUATE_COMPONENT permission.
   */
  @Authorize(permission = Permission.EVALUATE_COMPONENT)
  public ComponentVersionInfoDTO getComponentVersionInfo_EvaluateComponentPermission(
      @AuthzContext(Key.APPLICATION_PUBLIC_ID) String applicationPublicId,
      ComponentIdentifier componentIdentifier,
      SourceEndpoint sourceEndpoint)
  {
    auditComponentAccess(componentIdentifier, null);
    return getComponentVersionInfoNoAuth(OwnerType.APPLICATION, applicationPublicId, componentIdentifier, null, null,
        null, null, sourceEndpoint, false);
  }

  /**
   * Returns a list of component details for the given application and component identifier. It does not evaluate
   * policies and it does not return policy violations.
   *
   * @deprecated since 1.48. Not used by Insight or plugins, but left here as our customers use these APIs.
   */
  @Deprecated
  @Authorize(permission = Permission.READ)
  public ComponentDetailsList getComponentDetailsList_ReadPermission(
      @AuthzContext(AuthzContext.Key.TYPE) final OwnerType ownerType,
      @AuthzContext(AuthzContext.Key.ID) final String ownerId,
      ComponentIdentifier componentIdentifier,
      String matchState)
  {
    auditComponentAccess(componentIdentifier, null);
    final Owner owner = idUtils.getOwnerNotNull(ownerType, ownerId);
    ComponentDetailsList componentDetailsList =
        getComponentDetailsList(componentIdentifier, owner, null, null, null,
            false).getLeft();
    componentDetailsLoaderFactory.newInstance(owner)
        .augmentComponentDetails(componentDetailsList.getList(), matchState,
            null);
    return componentDetailsList;
  }

  @Authorize(permission = Permission.READ)
  void checkReadPermission(
      @SuppressWarnings("unused") @AuthzContext(Key.TYPE) OwnerType ownerType,
      @SuppressWarnings("unused") @AuthzContext(Key.ID) String ownerId)
  {
  }

  @Authorize(permission = Permission.EVALUATE_COMPONENT)
  void checkEvaluateComponentPermission(
      @SuppressWarnings("unused") @AuthzContext(Key.TYPE) OwnerType ownerType,
      @SuppressWarnings("unused") @AuthzContext(Key.ID) String ownerId)
  {
  }

  /**
   * Returns a list of component details for the given owner and component identifier.
   * It also evaluates policies and returns max threat levels per category, as well as count of violated policies.
   *
   * Requires READ or EVALUATE_COMPONENT permissions.
   */
  ComponentVersionInfoDTO getComponentVersionInfo(
      OwnerType ownerType,
      String ownerId,
      ComponentIdentifier componentIdentifier,
      String stageId,
      String identificationSource,
      String scanId,
      DependencyType dependencyType)
  {
    auditComponentAccess(componentIdentifier, null);

    try {
      checkReadPermission(ownerType, ownerId);
    }
    catch (UnauthorizedException e) {
      checkEvaluateComponentPermission(ownerType, ownerId);
    }

    return getComponentVersionInfoNoAuth(ownerType, ownerId, componentIdentifier, stageId, identificationSource, scanId,
        dependencyType, COMPONENT_INFO, true);
  }

  public ComponentVersionInfoDTO getComponentVersionInfoNoAuth(
      OwnerType ownerType,
      String ownerId,
      ComponentIdentifier componentIdentifier,
      String stageId,
      String identificationSource,
      String scanId,
      DependencyType dependencyType,
      SourceEndpoint sourceEndpoint,
      boolean stableVersionsOnly)
  {
    Owner owner = idUtils.getOwnerNotNull(ownerType, ownerId);
    // For performance, it's very important to use only one instance of ComponentDetailsLoader.
    // See https://sonatype.atlassian.net/browse/CLM-28129
    ComponentDetailsLoader componentDetailsLoader = componentDetailsLoaderFactory.newInstance(owner);
    Pair<List<ComponentDetailsDTO>, RepositorySourceResponseDTO> result =
        getComponentDetailsForAllVersionsNoAuth(owner, componentIdentifier, stageId, identificationSource, scanId,
            dependencyType, componentDetailsLoader, stableVersionsOnly);
    List<ComponentDetailsDTO> componentDetailsDTOs = result.getLeft();

    ApiComponentRemediationValueDTO remediationDto;
    if (IdentificationSource.isThirdPartyIdentificationSource(identificationSource)) {
      remediationDto = thirdPartyComponentDAO.getSuggestedRemmediation(owner.getId(), componentIdentifier, scanId);
    }
    else {
      remediationDto = componentRemediationService.getSuggestedRemediation(componentIdentifier, componentDetailsDTOs,
          owner, stageId, componentDetailsLoader, sourceEndpoint);
    }

    AutomatedRemediationStatusDTO remediationStatusDTO =
        getAutomatedRemediationStatusDTO(componentIdentifier, stageId, dependencyType, owner,
            remediationDto);

    return new ComponentVersionInfoDTO(
        componentDetailsDTOs,
        remediationDto,
        result.getRight(),
        remediationStatusDTO);
  }

  private AutomatedRemediationStatusDTO getAutomatedRemediationStatusDTO(
      ComponentIdentifier componentIdentifier,
      String stageId,
      DependencyType dependencyType,
      Owner owner,
      ApiComponentRemediationValueDTO remediationDto)
  {
    Optional<ApiVersionChangeOptionDTO> suggestedVersion = Optional.ofNullable(remediationDto)
        .flatMap((remediation) -> componentRemediationService.getApplicableVersionChangeFromAllType(
            remediationDto.suggestedVersionChange,
            remediationDto.versionChanges));

    if (suggestedVersion.isPresent() && OwnerType.APPLICATION == owner.getType()) {
      Optional<AutomatedRemediationStatusDTO> prStatus =
          getPullRequestStatus(componentIdentifier, (Application) owner, suggestedVersion.get());
      if (prStatus.isPresent() && DependencyType.DIRECT.equals(dependencyType)) {
        return prStatus.get();
      }
    }

    Optional<ManualPullRequestImpossibilityReason> manualPRDisabledReason =
        manualPullRequestService.isManualPullRequestPossible(componentIdentifier, stageId, dependencyType,
            owner,
            remediationDto);
    if (manualPRDisabledReason.isEmpty()) {
      return new AutomatedRemediationStatusDTO.ManualPullRequestPossibleDTO();
    }
    return new AutomatedRemediationStatusDTO.ManualPullRequestNotPossibleDTO(manualPRDisabledReason.get());
  }

  private Optional<AutomatedRemediationStatusDTO> getPullRequestStatus(
      ComponentIdentifier componentIdentifier,
      Application application,
      ApiVersionChangeOptionDTO suggestedVersion)
  {
    String branchName =
        pullRequestBranchNameGenerator.getBranchName(componentIdentifier, application, suggestedVersion);

    List<SourceControlEvent> sourceControlEvents =
        sourceControlEventDAO.getRemediationEventsForBranch(application.getId(), branchName);
    Optional<SourceControlEvent> sourceControlEvent = getHighestPriorityPullRequestEvent(sourceControlEvents);

    if (sourceControlEvent.isPresent()) {
      try {
        return Optional.of(AutomatedRemediationStatusDTO.fromSourceControlEvent(sourceControlEvent.get()));
      }
      catch (IllegalStateException e) {
        String errorMessage = String.format(
            "Pull request for branch %s and application %s has reached an invalid status.",
            branchName, application.getPublicId());
        log.error(errorMessage + " {}", e.getMessage());
        return Optional.of(new AutomatedRemediationStatusDTO.PullRequestCreationFailedDTO(errorMessage));
      }
    }
    return Optional.empty();
  }

  private Optional<SourceControlEvent> getHighestPriorityPullRequestEvent(
      List<SourceControlEvent> sourceControlEvents)
  {
    return sourceControlEvents.stream()
        .min(Comparator.comparingInt(e -> switch (e.getEventStatus())
        {
          case SourceControlEvent.EVENT_STATUS_COMPLETE -> 0;
          case SourceControlEvent.EVENT_STATUS_IN_PROGRESS -> 1;
          case SourceControlEvent.EVENT_STATUS_NEW -> 2;
          case SourceControlEvent.EVENT_STATUS_ERROR -> 3;
          default -> 4;
        }));
  }

  /**
   * Build a map of versionless component identifiers (packages) to a list of version details for
   * the package, where the versions are greater than or equal to the provided componentIdentifier
   * versions.
   */
  public Map<ComponentIdentifier, List<ComponentDetailsDTO>> getComponentDetailsForAllVersionsNoAuthBulk(
      Owner owner,
      List<ComponentIdentifier> componentIdentifiers,
      String stageId,
      String scanId,
      ComponentDetailsLoader componentDetailsLoader,
      boolean stableVersionsOnly)
  {
    Map<ComponentIdentifier, List<ComponentDetails>> componentDetailsByComponentIdentifier =
        getComponentDetailsListBulk(componentIdentifiers, owner, scanId, stableVersionsOnly);

    List<ComponentDetails> allComponentDetails = componentDetailsByComponentIdentifier.entrySet()
        .stream()
        .flatMap(componentIdentifierListEntry -> componentIdentifierListEntry.getValue().stream())
        .collect(Collectors.toList());
    List<Component> allComponents = componentDetailsLoader.augmentComponentDetails(
        allComponentDetails,
        MatchState.EXACT.getId(),
        null);

    List<ComponentDetailsDTO> allComponentDetailsDTOs =
        evaluatePoliciesAndGetComponentDetails(owner, stageId, allComponents, allComponentDetails);

    Map<ComponentIdentifier, List<ComponentDetailsDTO>> results = new LinkedHashMap<>();
    for (ComponentDetailsDTO dto : allComponentDetailsDTOs) {
      ComponentIdentifier pkgIdentifier = dto.componentIdentifier.createAlternativeVersion(null);
      if (!results.containsKey(pkgIdentifier)) {
        results.put(pkgIdentifier, new ArrayList<>());
      }
      results.get(pkgIdentifier).add(dto);
    }
    return results;
  }

  Map<ComponentIdentifier, List<ComponentDetails>> getComponentDetailsListBulk(
      List<ComponentIdentifier> componentIdentifiers,
      Owner owner,
      String scanId,
      boolean stableVersionsOnly)
  {
    long start = System.currentTimeMillis();

    if (CollectionUtils.isEmpty(componentIdentifiers)) {
      log.warn("There are no componentIdentifiers to expand");
      return Collections.emptyMap();
    }

    List<ComponentIdentifier> componentsWithKnownFormats = new ArrayList<>();
    List<ComponentIdentifier> componentsWithUnknownFormats = new ArrayList<>();

    for (ComponentIdentifier componentIdentifier : componentIdentifiers) {
      if (isKnownSupportedFormat(componentIdentifier)) {
        componentsWithKnownFormats.add(componentIdentifier);
      }
      else {
        componentsWithUnknownFormats.add(componentIdentifier);
      }
    }

    // Terraform components
    Map<ComponentIdentifier, List<ComponentDetails>> terraformDetails = componentsWithKnownFormats
        .stream()
        .filter(ComponentIdentifier::isTerraform)
        .map(componentIdentifier -> {
          // Terraform information is not stored in HDS
          ComponentDetailsList allVersions =
              thirdPartyComponentDAO.getAllVersions(owner.getId(), componentIdentifier, scanId);
          return Maps.immutableEntry(componentIdentifier, allVersions.getList());
        })
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    // Non-terraform components
    List<ComponentIdentifier> nonTerraformComponents = componentsWithKnownFormats.stream()
        .filter(componentIdentifier -> !componentIdentifier.isTerraform())
        .collect(Collectors.toList());

    Map<ComponentIdentifier, List<ComponentDetails>> nonTerraformDetails =
        getInformationVersionsHdsBulk(nonTerraformComponents, stableVersionsOnly);

    // Unknown formats - Generic components
    Map<ComponentIdentifier, List<ComponentDetails>> unknownComponentDetails =
        createComponentDetailsForUnknownFormats(owner.getId(), scanId, componentsWithUnknownFormats);

    Map<ComponentIdentifier, List<ComponentDetails>> allComponentDetails =
        Stream.of(terraformDetails.entrySet(), nonTerraformDetails.entrySet(), unknownComponentDetails.entrySet())
            .flatMap(Set::stream)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

    if (MapUtils.isNotEmpty(allComponentDetails)) {
      log.debug("Loaded component details list(s) for {} component identifiers in {} ms.",
          componentIdentifiers.size(), System.currentTimeMillis() - start);
    }
    else {
      log.debug("No component details loaded for {} component identifiers in {} ms.",
          componentIdentifiers.size(), System.currentTimeMillis() - start);
    }

    return allComponentDetails;
  }

  private Map<ComponentIdentifier, List<ComponentDetails>> createComponentDetailsForUnknownFormats(
      String appId,
      String scanId,
      List<ComponentIdentifier> componentsWithUnknownFormats)
  {
    return componentsWithUnknownFormats
        .stream()
        .map(componentIdentifier -> {
          ComponentDetailsList componentDetailsListForGenericIdentifier = null;
          final ComponentDetails componentDetails =
              thirdPartyComponentDAO.resolveComponentDetails(appId, componentIdentifier, scanId);

          if (Objects.nonNull(componentDetails) &&
              isThirdPartyIdentificationSource(componentDetails.getIdentificationSource()))
        {
            componentDetailsListForGenericIdentifier =
                thirdPartyComponentDAO.getAllVersions(appId, componentIdentifier, scanId);
          }
          else if (componentIdentifier.isGeneric()) {
            // allow generic component identifier which are components that do not
            // currently have broad support in the lifecycle ecosystem
            componentDetailsListForGenericIdentifier =
                createComponentDetailsListForGenericIdentifier(componentIdentifier);
          }
          else {
            log.warn("Could not create ComponentDetailsList for component {}. Invalid format {}",
                componentIdentifier,
                componentIdentifier.getFormat());
          }

          final Map.Entry<ComponentIdentifier, List<ComponentDetails>> maps;
          if (componentDetailsListForGenericIdentifier == null) {
            maps = Maps.immutableEntry(componentIdentifier, Collections.emptyList());
          }
          else {
            maps = Maps.immutableEntry(componentIdentifier, componentDetailsListForGenericIdentifier.getList());
          }

          return maps;
        })
        .filter(componentIdentifierListEntry -> CollectionUtils.isNotEmpty(componentIdentifierListEntry.getValue()))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  public Pair<List<ComponentDetailsDTO>, RepositorySourceResponseDTO> getComponentDetailsForAllVersionsNoAuth(
      Owner owner,
      ComponentIdentifier componentIdentifier,
      String stageId,
      String identificationSource,
      String scanId,
      DependencyType dependencyType,
      ComponentDetailsLoader componentDetailsLoader,
      boolean stableVersionsOnly)
  {
    Pair<ComponentDetailsList, RepositorySourceResponseDTO> componentDetailsListAndSource =
        getComponentDetailsList(componentIdentifier, owner, identificationSource, scanId, dependencyType,
            stableVersionsOnly);
    List<ComponentDetails> componentDetailsList = componentDetailsListAndSource.getLeft().getList();

    // Fix match state to exact as there's no point propagating it to other versions.
    List<Component> components =
        componentDetailsLoader.augmentComponentDetails(componentDetailsList, MatchState.EXACT.getId(), dependencyType);

    // Evaluate the policies and get the PolicyAlerts
    List<ComponentDetailsDTO> componentDetailsDTOs =
        evaluatePoliciesAndGetComponentDetails(owner, stageId, components, componentDetailsList);

    return Pair.of(componentDetailsDTOs, componentDetailsListAndSource.getRight());
  }

  private List<ComponentDetailsDTO> evaluatePoliciesAndGetComponentDetails(
      Owner owner,
      String stageId,
      List<Component> components,
      List<ComponentDetails> componentDetailsList)
  {
    List<PolicyAlert> allPolicyAlerts = componentPolicyEvaluator
        .evaluate(owner.getId(), new Stage(stageId != null ? stageId : BuildStageType.ID), components);

    Map<ComponentIdentifier, List<PolicyAlert>> policyAlertsByComponent = new HashMap<>();
    for (PolicyAlert policyAlert : allPolicyAlerts) {
      for (ComponentFact componentFact : policyAlert.getTrigger().getComponentFacts()) {
        policyAlertsByComponent.computeIfAbsent(componentFact.getComponentIdentifier(), key -> new ArrayList<>())
            .add(policyAlert);
      }
    }

    List<ComponentDetailsDTO> componentDetailsDTOs = new ArrayList<>(componentDetailsList.size());
    Map<String, Policy> policiesById = getPoliciesById(owner);
    for (ComponentDetails componentDetails : componentDetailsList) {
      ComponentDetailsDTO dto = new ComponentDetailsDTO();
      dto.matchState = componentDetails.getMatchState();
      dto.declaredLicenses = componentDetails.getDeclaredLicenses();
      dto.observedLicenses = componentDetails.getObservedLicenses();
      dto.overriddenLicenses = componentDetails.getOverriddenLicenses();
      dto.effectiveLicenses = componentDetails.getEffectiveLicenses();
      dto.effectiveLicenseStatus = componentDetails.getEffectiveLicenseStatus();
      dto.catalogDate = componentDetails.getCatalogDate();
      dto.relativePopularity = componentDetails.getRelativePopularity();
      dto.website = componentDetails.getWebsite();
      dto.majorRevisionStep = componentDetails.isMajorRevisionStep();
      dto.identificationSource = componentDetails.getIdentificationSource();
      dto.identificationSourceComment = componentDetails.getIdentificationSourceComment();

      List<PolicyAlert> policyAlerts = policyAlertsByComponent
          .getOrDefault(componentDetails.getComponentIdentifier(), Collections.emptyList());

      dto.policyAlerts = policyAlerts;
      dto.policyMaxThreatLevelsByCategory = getMaxPolicyThreatLevelsByCategory(policyAlerts, policiesById);

      dto.violatedPolicyCount = policyAlerts.stream()
          .map(PolicyAlert::getTrigger)
          .map(PolicyFact::getPolicyId)
          .collect(Collectors.toSet())
          .size();

      OptionalDouble highestSecurityVulnerabilitySeverity = componentDetails.getSecurityVulnerabilities()
          .stream()
          .mapToDouble(SecurityVulnerability::getSeverity)
          .max();

      dto.securityVulnerabilityCount = componentDetails.getSecurityVulnerabilities().size();
      dto.highestSecurityVulnerabilitySeverity = (float) highestSecurityVulnerabilitySeverity.orElse(0);

      dto.displayName = ComponentDisplayNameUtil.fromIdentifier(componentDetails.getComponentIdentifier());
      dto.componentIdentifier = componentDetails.getComponentIdentifier();
      dto.breakingChangesCount = componentDetails.getBreakingChangesCount();
      dto.securityVulnerabilities = componentDetails.getSecurityVulnerabilities();

      componentDetailsDTOs.add(dto);
    }
    return componentDetailsDTOs;
  }

  protected Map<String, Policy> getPoliciesById(Owner owner) {
    return policyDAO.getApplicableByOwnerIdWithHierarchy(owner.getId())
        .stream()
        .collect(Collectors.toMap(Policy::getId, Function.identity()));
  }

  private Map<PolicyThreatCategory, Integer> getMaxPolicyThreatLevelsByCategory(
      List<PolicyAlert> policyAlerts,
      Map<String, Policy> policiesById)
  {
    HashMap<PolicyThreatCategory, Integer> policyMaxThreatLevelsByCategory;
    policyMaxThreatLevelsByCategory = new HashMap<>();
    for (PolicyAlert policyAlert : policyAlerts) {
      PolicyFact policyFact = policyAlert.getTrigger();
      PolicyThreatCategory threatCategory = policiesById.get(policyFact.getPolicyId()).getThreatCategory();
      policyMaxThreatLevelsByCategory.merge(threatCategory, policyFact.getThreatLevel(), Math::max);
    }

    return policyMaxThreatLevelsByCategory;
  }

  private Map<String, Integer> maxPolicyThreatLevelToString(Map<PolicyThreatCategory, Integer> maxPolicyThreat) {
    Map<String, Integer> result = new HashMap<>();
    maxPolicyThreat.forEach((k, v) -> {
      result.put(k.getName(), v);
    });
    return result;
  }

  Pair<ComponentDetailsList, RepositorySourceResponseDTO> getComponentDetailsList(
      ComponentIdentifier identifier,
      Owner owner,
      String identificationSource,
      String scanId,
      DependencyType dependencyType,
      boolean stableVersionsOnly)
  {
    long start = System.currentTimeMillis();

    if (identifier == null) {
      throw new BadRequestException("componentIdentifier is required");
    }

    ComponentDetailsList componentDetailsList = null;
    RepositorySourceResponseDTO sourceResponseDTO = null;

    boolean isFromSbomWithCpeMatching = IdentificationSource.SBOM.getId().equals(identificationSource)
        && ComponentIdentifier.isFormatValidForCpeMatching(identifier.getFormat());

    boolean isFromFirewallForContainers = IdentificationSource.SONATYPE_CONTAINER.getId().equals(identificationSource);

    // Case 1: Terraform components (which are also 'knownFormat')
    if (isKnownFormat(identifier) && identifier.isTerraform()) {
      componentDetailsList = thirdPartyComponentDAO.getAllVersions(owner.getId(), identifier, scanId);
    }
    // Case 2: SBOM identification source with formats not supported by HDS (CPE matches) or Firewall for Containers
    else if (isFromSbomWithCpeMatching || isFromFirewallForContainers) {
      NamedComponentDetails details =
          reportDataReader.getComponentDetailsByIdentifier(identifier, owner.getId(), scanId);
      if (details != null) {
        componentDetailsList = new ComponentDetailsList();
        componentDetailsList.setList(Collections.singletonList(details));
      }
    }
    // Case 3: Other 'knownFormat' components (i.e., not Terraform and not the specific SBOM case above)
    // This handles HDS and potential repository lookups.
    else if (isKnownFormat(identifier)) {
      Pair<ComponentDetailsList, RepositorySourceResponseDTO> result =
          getFromHdsOrRepository(identifier, identificationSource, owner, scanId, dependencyType, stableVersionsOnly);
      componentDetailsList = result.getLeft();
      sourceResponseDTO = result.getRight();
    }
    // Case 4: Third-party identification source
    else if (isThirdPartyIdentificationSource(identificationSource)) {
      componentDetailsList = thirdPartyComponentDAO.getAllVersions(owner.getId(), identifier, scanId);
    }
    // Case 5: Generic components (if not a known format and not a third-party source)
    else if (identifier.isGeneric()) {
      // allow generic component identifier which are components that do not
      // currently have broad support in the lifecycle ecosystem
      componentDetailsList = createComponentDetailsListForGenericIdentifier(identifier);
    }
    // Case 6: Invalid format if none of the above conditions were met
    else {
      throw new BadRequestException("Invalid format: " + identifier.getFormat());
    }

    if (componentDetailsList != null && CollectionUtils.isNotEmpty(componentDetailsList.getList())) {
      // Set packageUrl for each component in the list
      for (ComponentDetails componentDetails : componentDetailsList.getList()) {
        ComponentIdentifier componentIdentifier = componentDetails.getComponentIdentifier();
        if (componentIdentifier != null) {
          try {
            String packageUrl = PackageUrlIdentifier.toPackageUrl(componentIdentifier);
            componentDetails.setPackageUrl(packageUrl);
          }
          catch (RuntimeException e) {
            // Some component identifiers may have invalid names for Package URL format
            // Log and continue without setting packageUrl
            log.debug("Could not generate packageUrl for identifier {}: {}", componentIdentifier, e.getMessage());
          }
        }
      }

      log.debug("Loaded component details list for {} versions of component identifier {} in {} ms.",
          componentDetailsList.getList().size(), identifier, System.currentTimeMillis() - start);
    }

    return Pair.of(componentDetailsList, sourceResponseDTO);
  }

  private Pair<ComponentDetailsList, RepositorySourceResponseDTO> getFromHdsOrRepository(
      ComponentIdentifier identifier,
      String identificationSource,
      Owner owner,
      String scanId,
      DependencyType dependencyType,
      boolean stableVersionsOnly)
  {
    ComponentDetailsList detailsList =
        getInformationVersionsHds(identifier, identificationSource, owner, scanId, stableVersionsOnly);
    RepositorySourceResponseDTO sourceResponseDTO = null;

    if (shouldGetFromRepositoryData(detailsList, identifier, dependencyType, identificationSource)) {
      Pair<RepositoryAllVersionsResponse, RepositorySourceResponseDTO> repoResult =
          repositoryQueryService.getAllVersions(identifier, owner);
      sourceResponseDTO = repoResult.getRight();
      if (CollectionUtils.isNotEmpty(repoResult.getLeft().getComponents())) {
        detailsList = transformToComponentDetailsList(repoResult.getLeft(), identifier);
      }
      else if (sourceResponseDTO != null) {
        sourceResponseDTO.source = null;
      }
    }
    return Pair.of(detailsList, sourceResponseDTO);
  }

  private boolean shouldGetFromRepositoryData(
      final ComponentDetailsList componentDetailsList,
      final ComponentIdentifier identifier,
      final DependencyType dependencyType,
      final String identificationSource)
  {
    if (componentDetailsList != null && CollectionUtils.isNotEmpty(componentDetailsList.getList()) &&
        componentDetailsList.getList().size() > 1)
    {
      return false;
    }

    if (DependencyType.INNER_SOURCE.equals(dependencyType) &&
        RepositoryClient.REPOSITORY_SUPPORTED_FORMATS.contains(identifier.getFormat()) &&
        SystemConfigurationPropertyFeature.INNER_SOURCE_REPOSITORY_INTEGRATION.isEnabled())
    {

      if (CollectionUtils.isNotEmpty(componentDetailsList.getList()) && componentDetailsList.getList().size() == 1) {
        return isThirdPartyIdentificationSource(identificationSource) ||
            isPackageManifestOrExternalRepoIdentificationSource(identificationSource);
      }
    }
    return false;
  }

  private ComponentDetailsList createComponentDetailsListForGenericIdentifier(ComponentIdentifier identifier) {
    ComponentDetailsList componentDetailsList = new ComponentDetailsList();
    componentDetailsList.setList(Collections.singletonList(
        createComponentDetails(identifier, generateFakeHash(identifier), IdentificationSource.PACKAGE_MANIFEST)));
    return componentDetailsList;
  }

  private ComponentDetailsList transformToComponentDetailsList(
      RepositoryAllVersionsResponse results,
      ComponentIdentifier identifier)
  {
    ComponentDetailsList componentDetailsList = new ComponentDetailsList();
    List<ComponentDetails> detailsList = new ArrayList<>();
    boolean requestVersionAdded = false;

    for (RepositoryComponentResult result : results.getComponents()) {
      if (!requestVersionAdded) {
        int comparison = compareVersions(identifier, result);
        if (comparison <= 0) {
          String hash = comparison == 0 ? getHash(identifier, result) : generateFakeHash(identifier);
          detailsList.add(createComponentDetails(identifier, hash, IdentificationSource.PACKAGE_MANIFEST));
          requestVersionAdded = true;
          if (comparison == 0) {
            continue;
          }
        }
      }

      String hash = getHash(identifier, result);
      detailsList.add(createComponentDetails(result.getIdentifier(), hash, IdentificationSource.PACKAGE_MANIFEST));
    }

    if (!requestVersionAdded) {
      detailsList.add(
          createComponentDetails(identifier, generateFakeHash(identifier), IdentificationSource.PACKAGE_MANIFEST));
    }

    componentDetailsList.setList(detailsList);
    return componentDetailsList;
  }

  private String getHash(final ComponentIdentifier identifier, final RepositoryComponentResult result) {
    return result.getSha1() != null ? HashHelper.truncateHash(result.getSha1()) : generateFakeHash(identifier);
  }

  private int compareVersions(
      ComponentIdentifier identifier,
      RepositoryComponentResult result)
  {
    ComparableVersion requestVersion = new ComparableVersion(identifier.get(ComponentIdentifier.VERSION));
    ComparableVersion resultVersion = new ComparableVersion(result.getIdentifier().get(ComponentIdentifier.VERSION));
    return requestVersion.compareTo(resultVersion);
  }

  private void updateThirdPartyInformation(
      final ComponentIdentifier identifier,
      final String identificationSource,
      final ComponentDetailsList componentDetailsList,
      final Owner owner,
      final String scanId)
  {
    if (isThirdPartyIdentificationSource(identificationSource)) {
      if (CollectionUtils.isEmpty(componentDetailsList.getList())) {
        componentDetailsList.getList()
            .add(thirdPartyComponentDAO.resolveComponentDetails(owner.getId(), identifier, scanId));
      }
      else {
        for (int i = 0; i < componentDetailsList.getList().size(); i++) {
          ComponentDetails componentDetails = componentDetailsList.getList().get(i);

          if (Objects.equals(identifier, componentDetails.getComponentIdentifier())) {
            componentDetailsList.getList()
                .set(i, thirdPartyComponentDAO.resolveComponentDetails(owner.getId(), identifier, scanId));
            break;
          }
        }
      }
    }
  }

  private ComponentDetailsList getInformationVersionsHds(
      final ComponentIdentifier identifier,
      final String identificationSource,
      final Owner owner,
      final String scanId,
      boolean stableVersionsOnly)
  {
    String url = "rest/" + toolName + "/componentDetails/list";
    ComponentDetailsList componentDetailsList = new ComponentDetailsList();
    try {
      final Map<String, String> queryParams = Map.of("componentIdentifier",
          ComponentIdentifierAdapter.toJson(identifier), "stableVersionsOnly", String.valueOf(stableVersionsOnly));
      componentDetailsList = hdsClient.get(ComponentDetailsList.class, url, queryParams);
    }
    catch (BadRequestException e) {
      // try using third party data
      if (isThirdPartyIdentificationSource(identificationSource)) {
        log.debug("Failed to get component details from HDS, trying with third party data for component {}: ",
            identifier, e);
        return thirdPartyComponentDAO.getAllVersions(owner.getId(), identifier, scanId);
      }
      throw e;
    }
    if (CollectionUtils.isEmpty(componentDetailsList.getList()) &&
        isPackageManifestOrExternalRepoIdentificationSource(identificationSource))
    {
      componentDetailsList = new ComponentDetailsList();
      componentDetailsList.setList(Collections
          .singletonList(createComponentDetails(identifier, generateFakeHash(identifier),
              IdentificationSource.getById(identificationSource))));
    }
    // In case it's a third-party component, the data must be replaced with the local information
    updateThirdPartyInformation(identifier, identificationSource, componentDetailsList, owner, scanId);
    return componentDetailsList;
  }

  private ComponentDetails createComponentDetails(
      ComponentIdentifier identifier,
      String hash,
      IdentificationSource identificationSource)
  {
    NamedComponentDetails details = createComponentDetails(hash, identifier, identificationSource);
    augmentEmptyLicensesAsUnspecified(details);
    return details;
  }

  private boolean isKnownFormat(ComponentIdentifier identifier) {
    return ComponentIdentifier.getFormatsSupportedByHds().contains(identifier.getFormat())
        ||
        LqaFormat.isLqaFormat(identifier.getFormat());
  }

  private boolean isKnownSupportedFormat(ComponentIdentifier identifier) {
    // SDEV-1097 Bulk request would not include deprecated format "deb"
    return ComponentIdentifier.getFormatsSupportedByHds().contains(identifier.getFormat());
  }

  private Map<ComponentIdentifier, List<ComponentDetails>> getInformationVersionsHdsBulk(
      List<ComponentIdentifier> componentIdentifiers,
      boolean stableVersionsOnly)
  {
    if (CollectionUtils.isEmpty(componentIdentifiers)) {
      log.warn("No component identifiers provided, unable to fetch versions from HDS");
      return Collections.emptyMap();
    }

    final Map<String, String> queryParams = Map.of("stableVersionsOnly", String.valueOf(stableVersionsOnly));
    Map<String, List<String>> versionsByComponent =
        hdsClient.post(Map.class, VERSIONS_BY_COMPONENT_ENDPOINT_URL, componentIdentifiers, queryParams);

    log.debug("Fetched versions for {} components from HDS.", versionsByComponent.size());

    List<ComponentIdentifier> expandedComponentIdentifiers =
        expandVersionsByComponent(componentIdentifiers, versionsByComponent);

    if (CollectionUtils.isEmpty(expandedComponentIdentifiers)) {
      log.warn("There are no expanded componentIdentifiers to get details from in HDS");
      return Collections.emptyMap();
    }

    List<ComponentEvaluationDataList.ComponentEvaluationData> componentEvaluationData =
        apiComponentDetailsServiceV2.getComponentDetailsListFromHds(
            expandedComponentIdentifiers,
            COMPONENT_DETAILS_PURPOSE);

    Map<ComponentIdentifier, List<ComponentEvaluationDataList.ComponentEvaluationData>> groupedComponentIdentifiers =
        groupComponentIdentifiers(componentEvaluationData);

    Map<ComponentIdentifier, List<ComponentDetails>> componentDetailsMap =
        mapComponentEvaluationDataToComponentDetails(groupedComponentIdentifiers);

    log.debug("{} componentDetails mapped", componentDetailsMap.size());

    return componentDetailsMap;
  }

  public Map<ComponentIdentifier, List<ComponentEvaluationDataList.ComponentEvaluationData>> groupComponentIdentifiers(
      List<ComponentEvaluationDataList.ComponentEvaluationData> componentEvaluationData)
  {
    Map<ComponentIdentifier, List<ComponentEvaluationDataList.ComponentEvaluationData>> groupedComponentIdentifiers =
        new LinkedHashMap<>();

    componentEvaluationData.forEach(currentComponentEvaluationData -> {
      ComponentIdentifier componentIdentifier = currentComponentEvaluationData.componentIdentifier;
      List<ComponentEvaluationDataList.ComponentEvaluationData> currentComponentEvaluationList =
          groupedComponentIdentifiers.get(componentIdentifier);
      if (currentComponentEvaluationList != null) {
        currentComponentEvaluationList.add(currentComponentEvaluationData);
      }
      else {
        List<ComponentEvaluationDataList.ComponentEvaluationData> componentEvaluationList = new ArrayList<>();
        componentEvaluationList.add(currentComponentEvaluationData);
        groupedComponentIdentifiers.put(componentIdentifier, componentEvaluationList);
      }
    });

    return groupedComponentIdentifiers;
  }

  public Map<ComponentIdentifier, List<ComponentDetails>> mapComponentEvaluationDataToComponentDetails(
      Map<ComponentIdentifier, List<ComponentEvaluationDataList.ComponentEvaluationData>> groupedComponentIdentifiers)
  {
    return groupedComponentIdentifiers
        .entrySet()
        .stream()
        .map((Map.Entry<ComponentIdentifier, List<ComponentEvaluationDataList.ComponentEvaluationData>> entry) -> {
          ComponentIdentifier key = entry.getKey();
          List<ComponentDetails> mappedComponentDetailsList = entry.getValue()
              .stream()
              .map(componentEvaluationData -> {
                ComponentDetails componentDetails = new ComponentDetails();
                componentDetails.setHash(componentEvaluationData.hash);
                componentDetails.setComponentIdentifier(componentEvaluationData.componentIdentifier);
                componentDetails.setMatchState(componentEvaluationData.matchState);
                componentDetails.setDeclaredLicenses(componentEvaluationData.declaredLicenses);
                componentDetails.setObservedLicenses(componentEvaluationData.observedLicenses);
                componentDetails.setCatalogDate(componentEvaluationData.catalogDate);
                componentDetails.setRelativePopularity(componentEvaluationData.relativePopularity);
                componentDetails.setSecurityVulnerabilities(componentEvaluationData.securityVulnerabilities);
                componentDetails.setAnalyzerFeatures(componentEvaluationData.analyzerFeatures);
                componentDetails.setComponentCategories(componentEvaluationData.componentCategories);
                componentDetails.setHygieneRating(componentEvaluationData.hygieneRating);
                componentDetails.setIntegrityRating(componentEvaluationData.integrityRating);
                return componentDetails;
              })
              .collect(Collectors.toList());
          return Maps.immutableEntry(key, mappedComponentDetailsList);
        })
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
  }

  /**
   * Given a list of component identifiers, and a list of all versions for each component, generate a new list of
   * component identifiers representing all versions of the components with versions greater or equal to that
   * of the original component.
   */
  public List<ComponentIdentifier> expandVersionsByComponent(
      final List<ComponentIdentifier> componentIdentifiers,
      Map<String, List<String>> versionsByComponent)
  {
    List<ComponentIdentifier> results = new ArrayList<>();

    componentIdentifiers.forEach(cid -> {
      PackageUrlIdentifier pid = PackageUrlIdentifier.fromComponentIdentifier(cid);
      // Get the PURL without a version number
      PackageUrlIdentifier packageOnly = PackageUrlIdentifier
          .fromComponentIdentifier(cid)
          .createAlternativeVersion(null);
      String purl = packageOnly.getPackageUrl();
      List<String> availableVersions = versionsByComponent.get(purl);
      if (availableVersions != null) {
        List<ComponentIdentifier> result = availableVersions.stream()
            .filter(availableCid -> {
              ComparableVersion availableVersion = new ComparableVersion(availableCid);
              ComparableVersion minVersion = new ComparableVersion(pid.getVersion());
              return availableVersion.compareTo(minVersion) >= 0;
            })
            .map(version -> cid.createAlternativeVersion(version))
            .collect(Collectors.toList());

        results.addAll(result);
      }
    });

    return results;
  }

  /**
   *
   * @since 1.76
   */
  @Authorize(permission = Permission.READ)
  public ComponentLicenses getLicenses(
      @AuthzContext(AuthzContext.Key.TYPE) final OwnerType ownerType,
      @AuthzContext(AuthzContext.Key.ID) final String ownerId,
      ComponentIdentifier componentIdentifier,
      HttpServletRequest httpRequest,
      String identificationSource,
      String scanId) throws IOException
  {
    auditComponentAccess(componentIdentifier, null);
    if (componentIdentifier == null) {
      throw new BadRequestException("componentIdentifier is required");
    }

    final Owner owner = idUtils.getOwnerNotNull(ownerType, ownerId);

    ComponentLicenses result = new ComponentLicenses();

    ComponentDetails componentDetails =
        getUnaugmentedComponentDetails(owner, componentIdentifier, httpRequest, identificationSource, scanId);
    augmentComponentDetails(owner, componentDetails);
    result.declaredlicenses = getLicensesWithThreatLevels(owner, componentDetails.getDeclaredLicenses());
    result.observedlicenses = getLicensesWithThreatLevels(owner, componentDetails.getObservedLicenses());
    result.effectiveLicenses = getLicensesWithThreatLevels(owner, componentDetails.getEffectiveLicenses());
    result.selectableLicenses = new ArrayList<>(getSelectableLicenses(componentDetails.getDeclaredLicenses(),
        componentDetails.getObservedLicenses()));
    return result;
  }

  /**
   *
   * @since 1.134
   */
  @Authorize(permission = Permission.READ)
  public ComponentMultiLicenses getMultiLicensesForRead(
      @AuthzContext(AuthzContext.Key.TYPE) OwnerType ownerType,
      @AuthzContext(AuthzContext.Key.ID) String ownerId,
      ComponentIdentifier componentIdentifier,
      HttpServletRequest httpRequest,
      String identificationSource,
      String scanId) throws IOException
  {
    return getMultiLicensesNoAuth(ownerType, ownerId, componentIdentifier, httpRequest, identificationSource, scanId);
  }

  /**
   *
   * @since 1.167
   */
  @Authorize(permission = Permission.LEGAL_REVIEWER)
  public ComponentMultiLicenses getMultiLicensesForLegalReviewer(
      @AuthzContext(AuthzContext.Key.TYPE) OwnerType ownerType,
      @AuthzContext(AuthzContext.Key.ID) String ownerId,
      ComponentIdentifier componentIdentifier,
      HttpServletRequest httpRequest,
      String identificationSource,
      String scanId) throws IOException
  {
    return getMultiLicensesNoAuth(ownerType, ownerId, componentIdentifier, httpRequest, identificationSource, scanId);
  }

  @VisibleForTesting
  ComponentMultiLicenses getMultiLicensesNoAuth(
      OwnerType ownerType,
      String ownerId,
      ComponentIdentifier componentIdentifier,
      HttpServletRequest httpRequest,
      String identificationSource,
      String scanId) throws IOException
  {
    auditComponentAccess(componentIdentifier, null);
    if (componentIdentifier == null) {
      throw new BadRequestException("componentIdentifier is required");
    }

    Owner owner = idUtils.getOwnerNotNull(ownerType, ownerId);

    ComponentMultiLicenses result = new ComponentMultiLicenses();

    ComponentDetails componentDetails =
        getUnaugmentedComponentDetails(owner, componentIdentifier, httpRequest, identificationSource, scanId);

    Component component = augmentComponentDetails(owner, componentDetails);

    result.declaredLicenses = getMultiLicenseWithThreatLevels(owner, componentDetails.getDeclaredLicenses());
    result.observedLicenses = getMultiLicenseWithThreatLevels(owner, componentDetails.getObservedLicenses());
    result.effectiveLicenses = getMultiLicenseWithThreatLevels(owner, componentDetails.getEffectiveLicenses());
    result.selectableLicenses = new ArrayList<>(
        getSelectableLicenses(componentDetails.getDeclaredLicenses(), componentDetails.getObservedLicenses()));
    result.hiddenObservedLicenses = component.isHiddenObservedLicenses();
    result.supportAlpObservedLicenses = com.sonatype.insight.brain.model.license.License
        .isAlpObservedLicenseFormatHidden(componentIdentifier.getFormat());
    return result;
  }

  public ComponentDetails getUnaugmentedComponentDetails(
      Owner owner,
      ComponentIdentifier componentIdentifier,
      HttpServletRequest httpRequest,
      String identificationSource,
      String scanId) throws IOException
  {
    return getComponentDetailsBasedOnSource(componentIdentifier, owner, scanId, null, null, httpRequest,
        identificationSource);
  }

  public Component augmentComponentDetails(Owner owner, ComponentDetails componentDetails) {
    augmentEmptyLicensesAsUnspecified(componentDetails);
    return componentDetailsLoaderFactory.newInstance(owner).augmentComponentDetails(componentDetails);
  }

  /**
   * @since 1.18.0
   */
  @Authorize(permission = Permission.READ)
  public ComponentSecurityVulnerabilities getSecurityVulnerabilities(
      @AuthzContext(AuthzContext.Key.TYPE) final OwnerType ownerType,
      @AuthzContext(AuthzContext.Key.ID) final String ownerId,
      final String hash,
      final ComponentIdentifier componentIdentifier,
      final HttpServletRequest httpRequest,
      final String identificationSource,
      final String scanId) throws IOException
  {
    auditComponentAccess(componentIdentifier, hash);
    if (componentIdentifier == null) {
      throw new BadRequestException("componentIdentifier is required");
    }
    String internalId = idUtils.getInternalOwnerId(ownerType, ownerId);
    Owner owner = ownerDAO.getById(internalId);

    ComponentDetails componentDetails =
        getComponentDetailsBasedOnSource(componentIdentifier, owner, scanId, null, hash, httpRequest,
            identificationSource);

    componentDetailsLoaderFactory.newInstance(owner).augmentComponentDetails(componentDetails);
    return new ComponentSecurityVulnerabilities(componentDetails.getSecurityVulnerabilities());
  }

  private void auditComponentAccess(final ComponentIdentifier identifier, final String hash) {
    AuditData.get().setComponentIdentifier(identifier).setComponentHash(hash);
  }

  private Set<License> getSelectableLicenses(Collection<License> declared, Collection<License> observed) {
    Set<License> result = new LinkedHashSet<>();
    Set<License> licenses = new LinkedHashSet<>();
    licenses.addAll(declared);
    licenses.addAll(observed);
    for (final License license : licenses) {
      if (com.sonatype.insight.brain.model.license.License.isEffectivelyUnspecified(license.getLicenseId())) {
        continue;
      }

      MultiLicense multiLicense = multiLicenseDAO.getById(license.getLicenseId());
      Set<com.sonatype.insight.brain.model.license.License> dbLicenses = multiLicenseDAO
          .getLicensesByMultiLicenseIdNotNull(multiLicense.getId());
      for (com.sonatype.insight.brain.model.license.License dbLicense : dbLicenses) {
        if (dbLicense.getId().endsWith("-UNSPECIFIED")) {
          String licenseIdPrefix = dbLicense.getId().substring(0, dbLicense.getId().length() - "UNSPECIFIED".length());
          for (com.sonatype.insight.brain.model.license.License otherLicense : licenseDAO.getAll()) {
            if (otherLicense.getId().startsWith(licenseIdPrefix) && !dbLicense.getId().equals(otherLicense.getId())) {
              result.add(new License(otherLicense.getId(), otherLicense.getShortDisplayName()));
            }
          }
        }
        result.add(new License(dbLicense.getId(), dbLicense.getShortDisplayName()));
      }
    }
    return result;
  }

  /**
   * @since 1.6
   */
  private List<LicenseWithThreatLevel> getLicensesWithThreatLevels(Owner owner, Set<License> multiLicenses) {
    List<LicenseWithThreatLevel> result = new ArrayList<>();

    if (multiLicenses != null) {
      Set<String> alreadyProcessedLicenseIds = new HashSet<>();
      for (License multiLicense : multiLicenses) {
        Set<com.sonatype.insight.brain.model.license.License> licenses = multiLicenseDAO
            .getLicensesByMultiLicenseIdNotNull(multiLicense.getLicenseId());
        for (com.sonatype.insight.brain.model.license.License license : licenses) {
          if (!alreadyProcessedLicenseIds.add(license.getId())) {
            continue;
          }
          LicenseWithThreatLevel licenseWithThreatLevel = getLicenseWithThreatLevel(owner, license);
          result.add(licenseWithThreatLevel);
        }
      }
    }

    return result;
  }

  private LicenseWithThreatLevel getLicenseWithThreatLevel(
      Owner owner,
      com.sonatype.insight.brain.model.license.License license)
  {
    LicenseWithThreatLevel licenseWithThreatLevel = new LicenseWithThreatLevel();
    licenseWithThreatLevel.license = new License(license.getId(), license.getShortDisplayName());
    licenseWithThreatLevel.threatLevel =
        licenseThreatGroupDAO.getLicenseThreatLevelByOwnerAndLicenseIdWithHierarchy(owner, license.getId());
    return licenseWithThreatLevel;
  }

  private List<MultiLicenseWithThreatLevel> getMultiLicenseWithThreatLevels(Owner owner, Set<License> multiLicenses) {
    List<MultiLicenseWithThreatLevel> result = new ArrayList<>();

    if (multiLicenses != null) {
      for (License multiLicense : multiLicenses) {
        MultiLicenseWithThreatLevel multiLicenseWithThreatLevel = new MultiLicenseWithThreatLevel();
        multiLicenseWithThreatLevel.licenseId = multiLicense.getLicenseId();
        multiLicenseWithThreatLevel.licenseName = multiLicense.getLicenseName();

        Set<com.sonatype.insight.brain.model.license.License> licenses =
            multiLicenseDAO.getLicensesByMultiLicenseIdNotNull(multiLicense.getLicenseId());

        for (com.sonatype.insight.brain.model.license.License license : licenses) {
          LicenseWithThreatLevel licenseWithThreatLevel = new LicenseWithThreatLevel();
          licenseWithThreatLevel.license = new License(license.getId(), license.getShortDisplayName());
          licenseWithThreatLevel.threatLevel =
              licenseThreatGroupDAO.getLicenseThreatLevelByOwnerAndLicenseIdWithHierarchy(owner, license.getId());
          multiLicenseWithThreatLevel.licenses.add(licenseWithThreatLevel);
        }

        Collections.sort(multiLicenseWithThreatLevel.licenses,
            Comparator.comparing(licenseWithThreatLevel -> licenseWithThreatLevel.license.getLicenseName()));
        result.add(multiLicenseWithThreatLevel);
      }
    }

    Collections.sort(result, Comparator.comparing(
        multiLicenseWithThreatLevel -> multiLicenseWithThreatLevel.licenses.get(0).license.getLicenseName()));
    return result;
  }

  private static boolean isPackageManifestOrExternalRepoIdentificationSource(String identificationSource) {
    return IdentificationSource.PACKAGE_MANIFEST.getId().equals(identificationSource) ||
        IdentificationSource.EXTERNAL_REPO.getId().equals(identificationSource);
  }

  /**
   * @since 1.18.0
   */
  public static class ComponentSecurityVulnerabilities
  {
    public List<SecurityVulnerability> securityVulnerabilities;

    public ComponentSecurityVulnerabilities() {
    }

    public ComponentSecurityVulnerabilities(final List<SecurityVulnerability> securityVulnerabilities) {
      this.securityVulnerabilities = securityVulnerabilities;
    }
  }

  /**
   * @since 1.6
   */
  public static class ComponentLicenses
  {
    public List<LicenseWithThreatLevel> declaredlicenses;

    public List<LicenseWithThreatLevel> observedlicenses;

    /**
     * @since 1.12
     */
    public List<LicenseWithThreatLevel> effectiveLicenses;

    /**
     * @since 1.13
     */
    public List<License> selectableLicenses;
  }

  /**
   * @since 1.6
   */
  public static class LicenseWithThreatLevel
  {
    public License license;

    public Integer threatLevel;
  }

  /**
   * @since 1.134
   */
  public static class ComponentMultiLicenses
  {
    public List<MultiLicenseWithThreatLevel> declaredLicenses;

    public List<MultiLicenseWithThreatLevel> observedLicenses;

    public List<MultiLicenseWithThreatLevel> effectiveLicenses;

    public List<License> selectableLicenses;

    public boolean hiddenObservedLicenses;

    public boolean supportAlpObservedLicenses;
  }

  /**
   * @since 1.134
   */
  public static class MultiLicenseWithThreatLevel
  {
    public String licenseId;

    public String licenseName;

    public List<LicenseWithThreatLevel> licenses = new ArrayList<>();
  }

  public void setToolName(String toolName) {
    this.toolName = toolName;
  }
}
