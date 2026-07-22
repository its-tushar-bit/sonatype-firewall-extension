/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.clm.dto.model.component.ComponentDisplayName;
import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.InvalidComponentIdentifierException;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.api.v2.ApiApplicationAdapter;
import com.sonatype.insight.brain.api.v2.FirewallPermissionGate;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationViolationDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationViolationListDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentTransitivePolicyViolationsDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiEnhancedPolicyViolationDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiPageResult;
import com.sonatype.insight.brain.api.v2.dto.ApiStagePolicyViolationComponentDTO;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.PolicyAuditDTO;
import com.sonatype.insight.brain.dataaccess.ApplicationComponentDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.dataaccess.component.ComponentLoaderFactory;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO.ContainerImageInQuarantineData;
import com.sonatype.insight.brain.landing.UserInterfaceLinksHelper;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ApplicationComponent;
import com.sonatype.insight.brain.model.HashHelper;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.InvalidStageException;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.organization.ApplicationService;
import com.sonatype.insight.brain.policy.StageTypeService;
import com.sonatype.insight.brain.policy.evaluator.PolicyThreats;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationComparator;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.report.LifecycleReport;
import com.sonatype.insight.brain.report.ReportEntry;
import com.sonatype.insight.brain.report.ReportService;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.shiro.authz.UnauthorizedException;
import org.joda.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.report.LifecycleReport.ReportFile.BOM_JSON;
import static com.sonatype.insight.brain.report.LifecycleReport.ReportFile.POLICY_THREATS;
import static java.util.stream.Collectors.toSet;

/**
 * @since 1.13.0
 */
@Named
public class ApiPolicyViolationServiceV2
{
  private static final Logger log = LoggerFactory.getLogger(ApiPolicyViolationServiceV2.class);

  private final ApplicationService applicationService;

  private final ApplicationComponentDAO applicationComponentDAO;

  private final PolicyEvaluationDAO policyEvaluationDAO;

  private final PolicyViolationDAO policyViolationDAO;

  private final OwnerDAO ownerDAO;

  private final PolicyDAO policyDAO;

  private final ReportService reportService;

  private final StageTypeService stageTypeService;

  private final ComponentLoaderFactory componentLoaderFactory;

  private final IdUtils idUtils;

  private final FirewallPermissionGate firewallPermissionGate;

  @Inject
  public ApiPolicyViolationServiceV2(
      final ApplicationService applicationService,
      final ApplicationComponentDAO applicationComponentDAO,
      final PolicyEvaluationDAO policyEvaluationDAO,
      final PolicyViolationDAO policyViolationDAO,
      final OwnerDAO ownerDAO,
      final PolicyDAO policyDAO,
      final ReportService reportService,
      final StageTypeService stageTypeService,
      final ComponentLoaderFactory componentLoaderFactory,
      final IdUtils idUtils,
      final FirewallPermissionGate firewallPermissionGate)
  {
    this.applicationService = applicationService;
    this.applicationComponentDAO = applicationComponentDAO;
    this.policyEvaluationDAO = policyEvaluationDAO;
    this.policyViolationDAO = policyViolationDAO;
    this.ownerDAO = ownerDAO;
    this.policyDAO = policyDAO;
    this.reportService = reportService;
    this.stageTypeService = stageTypeService;
    this.componentLoaderFactory = componentLoaderFactory;
    this.idUtils = idUtils;
    this.firewallPermissionGate = firewallPermissionGate;
  }

  public ApiApplicationViolationListDTOV2 getPolicyViolations(
      final Set<String> policyIds,
      String openTimeAfter,
      String openTimeBefore,
      Set<PolicyViolationType> violationTypes)
  {
    Date openTimeAfterDate = null;
    Date openTimeBeforeDate = null;

    if (openTimeAfter != null) {
      try {
        openTimeAfterDate = Instant.parse(openTimeAfter).toDate();
      }
      catch (IllegalArgumentException e) {
        throw new BadRequestException("Provided value for openTimeAfter is not a valid date.");
      }
    }

    if (openTimeBefore != null) {
      try {
        openTimeBeforeDate = Instant.parse(openTimeBefore).toDate();
      }
      catch (IllegalArgumentException e) {
        throw new BadRequestException("Provided value for openTimeBefore is not a valid date.");
      }
    }

    if (policyIds == null || policyIds.isEmpty()) {
      return new ApiApplicationViolationListDTOV2();
    }

    long start = System.currentTimeMillis();

    // Returns all apps the user has READ permission for.
    List<Application> applications = applicationService.getApplications();

    AuditData.get()
        .setData("selectedPolicies", transcribeToPolicyAuditDTO(policyIds))
        .setData("inspectedApplicationCount", applications.size());

    Map<String, Application> applicationsById =
        applications.stream().collect(Collectors.toMap(Application::getId, Function.identity()));
    Set<String> applicationIds = applicationsById.keySet();

    Collection<PolicyEvaluation> policyEvaluations = loadPolicyEvaluations(applicationIds);
    // Filter app ids to those that have policy evaluations
    applicationIds = policyEvaluations.stream().map(PolicyEvaluation::getApplicationId).collect(toSet());

    Collection<PolicyViolation> policyViolations =
        loadPolicyViolations(applicationIds, policyIds, openTimeAfterDate, openTimeBeforeDate, violationTypes);

    Map<String, List<PolicyViolation>> policyViolationsByAppId =
        policyViolations.stream().collect(Collectors.groupingBy(PolicyViolation::getApplicationId));

    // Sort violations using the standard violation comparator in order to get consistent results.
    sortPolicyViolations(policyViolationsByAppId);

    Map<String, PolicyEvaluation> policyEvaluationsByAppIdAndStageId = policyEvaluations.stream()
        .collect(Collectors.toMap(
            policyEvaluation -> policyEvaluation.getApplicationId() + policyEvaluation.getStageTypeId(),
            Function.identity()));

    Map<ApplicationComponentDAO.ApplicationComponentKey, ApplicationComponent> componentsByAppIdStageIdHash =
        batchLoadApplicationComponents(policyViolationsByAppId);

    ApiApplicationViolationListDTOV2 apiApplicationViolationListDTOV2 = new ApiApplicationViolationListDTOV2();
    for (Entry<String, List<PolicyViolation>> entry : policyViolationsByAppId.entrySet()) {
      String appId = entry.getKey();
      List<PolicyViolation> policyViolationsForApp = entry.getValue();

      ApiApplicationViolationDTOV2 apiApplicationViolationDTOV2 = new ApiApplicationViolationDTOV2();
      Application application = applicationsById.get(appId);
      apiApplicationViolationDTOV2.application = ApiApplicationAdapter.convertToApplicationBaseDTO(application);
      apiApplicationViolationDTOV2.policyViolations = new ArrayList<>();
      for (PolicyViolation policyViolation : policyViolationsForApp) {
        PolicyEvaluation policyEvaluation = policyEvaluationsByAppIdAndStageId
            .get(policyViolation.getApplicationId() + policyViolation.getStageTypeId());
        ApiEnhancedPolicyViolationDTOV2 apiEnhancedPolicyViolationDTOV2 =
            toApiEnhancedPolicyViolationDTOV2(application, policyEvaluation, policyViolation,
                componentsByAppIdStageIdHash);
        apiApplicationViolationDTOV2.policyViolations.add(apiEnhancedPolicyViolationDTOV2);
      }

      apiApplicationViolationListDTOV2.applicationViolations.add(apiApplicationViolationDTOV2);
    }

    log.debug("Retrieved {} policy violations for {} applications and {} policies in {} ms", policyViolations.size(),
        applicationIds.size(), policyIds.size(), System.currentTimeMillis() - start);

    return apiApplicationViolationListDTOV2;
  }

  private List<PolicyAuditDTO> transcribeToPolicyAuditDTO(final Set<String> policyIds) {
    List<PolicyAuditDTO> policyAuditDTOs = new ArrayList<>();
    for (String policyId : policyIds) {
      policyAuditDTOs.add(new PolicyAuditDTO(policyId, policyDAO.getById(policyId)));
    }
    return policyAuditDTOs;
  }

  private Map<ApplicationComponentDAO.ApplicationComponentKey, ApplicationComponent> batchLoadApplicationComponents(
      Map<String, List<PolicyViolation>> policyViolationsByAppId)
  {
    Set<String> applicationIds = new HashSet<>();
    Set<String> stageTypeIds = new HashSet<>();
    Set<String> hashes = new HashSet<>();

    for (List<PolicyViolation> violations : policyViolationsByAppId.values()) {
      for (PolicyViolation pv : violations) {
        applicationIds.add(pv.getApplicationId());
        stageTypeIds.add(pv.getStageTypeId());
        hashes.add(pv.getHash());
      }
    }

    return applicationComponentDAO.getMapByApplicationIdsAndStageTypeIdsAndHashes(
        applicationIds, stageTypeIds, hashes);
  }

  private void sortPolicyViolations(Map<String, List<PolicyViolation>> policyViolationsByAppId) {
    for (List<PolicyViolation> policyViolations : policyViolationsByAppId.values()) {
      policyViolations.sort(PolicyViolationComparator.COMPARATOR);
    }
  }

  private Collection<PolicyViolation> loadPolicyViolations(
      Set<String> applicationIds,
      Set<String> policyIds,
      Date openTimeAfter,
      Date openTimeBefore,
      Set<PolicyViolationType> violationTypes)
  {
    long start = System.currentTimeMillis();

    Collection<PolicyViolation> policyViolations = policyViolationDAO.getByApplicationIdsAndPolicyIdsAndTypes(
        applicationIds,
        policyIds,
        openTimeAfter,
        openTimeBefore,
        violationTypes.contains(PolicyViolationType.ACTIVE),
        violationTypes.contains(PolicyViolationType.WAIVED),
        violationTypes.contains(PolicyViolationType.LEGACY));

    policyViolationDAO.loadConstraintFacts(policyViolations);
    log.debug("Loaded {} policy violations for {} applications and {} policies in {} ms", policyViolations.size(),
        applicationIds.size(), policyIds.size(), System.currentTimeMillis() - start);

    return policyViolations;
  }

  private Collection<PolicyEvaluation> loadPolicyEvaluations(Set<String> applicationIds) {
    long start = System.currentTimeMillis();

    Collection<PolicyEvaluation> policyEvaluations = policyEvaluationDAO.getLastByApplicationIds(applicationIds);
    log.debug("Loaded {} policy evaluations for {} applications across all stages in {} ms", policyEvaluations.size(),
        applicationIds.size(), System.currentTimeMillis() - start);

    return policyEvaluations;
  }

  private ApiEnhancedPolicyViolationDTOV2 toApiEnhancedPolicyViolationDTOV2(
      Application application,
      PolicyEvaluation policyEvaluation,
      PolicyViolation policyViolation,
      Map<ApplicationComponentDAO.ApplicationComponentKey, ApplicationComponent> componentsByAppIdStageIdHash)
  {
    ApiEnhancedPolicyViolationDTOV2 apiPolicyViolationDTO = new ApiEnhancedPolicyViolationDTOV2();
    apiPolicyViolationDTO.policyId = policyViolation.getPolicyId();
    apiPolicyViolationDTO.policyName = policyViolation.getPolicyName();
    apiPolicyViolationDTO.policyViolationId = policyViolation.getId();
    apiPolicyViolationDTO.threatLevel = policyViolation.getThreatLevel();
    apiPolicyViolationDTO.openTime = policyViolation.getOpenTime();
    apiPolicyViolationDTO.reportUrl =
        UserInterfaceLinksHelper.getReportUrl(application.getPublicId(), policyEvaluation.getScanId());
    apiPolicyViolationDTO.stageId = policyViolation.getStageTypeId();
    apiPolicyViolationDTO.reportId = policyEvaluation.getScanId();
    ApplicationComponentDAO.ApplicationComponentKey lookupKey = new ApplicationComponentDAO.ApplicationComponentKey(
        application.getId(), policyViolation.getStageTypeId(), policyViolation.getHash());
    ApplicationComponent applicationComponent = componentsByAppIdStageIdHash.get(lookupKey);
    apiPolicyViolationDTO.component = new ApiComponentDTOV2();
    apiPolicyViolationDTO.component.hash = policyViolation.getHash();
    apiPolicyViolationDTO.component.proprietary = applicationComponent != null && applicationComponent.isProprietary();
    apiPolicyViolationDTO.component.componentIdentifier =
        ApiComponentIdentifierDTOV2.fromComponentIdentifier(policyViolation.getComponentIdentifier());
    apiPolicyViolationDTO.component.packageUrl =
        PackageUrlIdentifier.toPackageUrl(policyViolation.getComponentIdentifier());
    ComponentDisplayName componentDisplayName =
        ComponentDisplayNameUtil.fromIdentifier(policyViolation.getComponentIdentifier());
    apiPolicyViolationDTO.component.displayName =
        componentDisplayName != null ? componentDisplayName.toString() : policyViolation.getFilename();
    apiPolicyViolationDTO.constraintViolations = PolicyViolationAdapter.convert(policyViolation);

    apiPolicyViolationDTO.isWaived = policyViolation.isWaived();
    apiPolicyViolationDTO.isLegacy = policyViolation.isLegacyViolation();

    return apiPolicyViolationDTO;
  }

  public void ensureInnerSourceTransitiveWaiverEnabled() {
    if (!SystemConfigurationPropertyFeature.INNER_SOURCE_TRANSITIVE_WAIVER.isEnabled()) {
      throw new UnauthorizedException(
          SystemConfigurationPropertyFeature.INNER_SOURCE_TRANSITIVE_WAIVER.getId() + " feature is disabled.");
    }
  }

  @Authorize(permission = Permission.READ)
  public ApiComponentTransitivePolicyViolationsDTO getTransitivePolicyViolationsByAppScanComponent(
      @AuthzContext(Key.TYPE) final OwnerType ownerType,
      @AuthzContext(Key.ID) final String ownerId,
      final String scanId,
      final ComponentIdentifier componentIdentifier,
      final String packageUrl,
      final String hash)
  {
    if (!OwnerType.APPLICATION.equals(ownerType)) {
      throw new BadRequestException("scanId can only be specified for an application.");
    }
    Owner owner = idUtils.getOwnerNotNull(ownerType, ownerId);
    PolicyEvaluation policyEvaluation = policyEvaluationDAO.getLastByApplicationIdAndScanId(owner.getId(), scanId);
    if (policyEvaluation == null) {
      throw new NotFoundException("scanId " + scanId + " not found for application " + owner.getPublicId() + ".");
    }
    AuditData.get().setScanId(scanId);
    return getTransitivePolicyViolations(policyEvaluation.getStageTypeId(), componentIdentifier, packageUrl, hash,
        Collections.singletonList(policyEvaluation));
  }

  @Authorize(permission = Permission.READ)
  public ApiComponentTransitivePolicyViolationsDTO getTransitivePolicyViolationsByOwnerStageComponent(
      @AuthzContext(Key.TYPE) final OwnerType ownerType,
      @AuthzContext(Key.ID) final String ownerId,
      final String stageId,
      final ComponentIdentifier componentIdentifier,
      final String packageUrl,
      final String hash)
  {
    Owner owner = idUtils.getOwnerNotNull(ownerType, ownerId);
    String stageIdLowercase = stageId.toLowerCase(Locale.ROOT);
    if (!Stage.isValidStageTypeId(stageIdLowercase)) {
      throw new InvalidStageException(stageId);
    }
    List<PolicyEvaluation> policyEvaluations = policyEvaluationDAO
        .getLastByApplicationIdsAndStageIds(ownerDAO.getDescendantOrSelfApplicationIds(owner),
            Collections.singleton(stageIdLowercase));
    return getTransitivePolicyViolations(stageIdLowercase, componentIdentifier, packageUrl, hash, policyEvaluations);
  }

  public ApiPageResult<ContainerImageInQuarantineData> getContainerImagesInQuarantine(
      final int page,
      final int pageSize)
  {
    Set<String> permittedRepositoryIds = firewallPermissionGate.resolvePermittedRepositoryIds();
    if (permittedRepositoryIds != null) {
      long total = policyViolationDAO.getContainerImagesQuarantinedCountByRepositoryIds(permittedRepositoryIds);
      List<ContainerImageInQuarantineData> pageRows =
          policyViolationDAO.getContainerImagesInQuarantineByRepositoryIds(permittedRepositoryIds, page, pageSize);
      return new ApiPageResult<>(total, page, pageSize, pageRows);
    }
    long total = policyViolationDAO.getContainerImagesQuarantinedCount();
    List<ContainerImageInQuarantineData> containerImagesInQuarantine =
        policyViolationDAO.getContainerImagesInQuarantine(page, pageSize);
    return new ApiPageResult<>(total, page, pageSize, containerImagesInQuarantine);
  }

  public Pair<Component, List<Pair<PolicyViolation, Component>>> getTransitivePolicyViolationsForLastEvaluation(
      String applicationId,
      String scanId,
      ComponentIdentifier componentIdentifier,
      String packageUrl,
      String hash)
  {
    PolicyEvaluation policyEvaluation = policyEvaluationDAO.getLastByApplicationIdAndScanId(applicationId, scanId);
    if (policyEvaluation == null) {
      throw new NotFoundException("Evaluation not found with application " + applicationId + " and scan " + scanId);
    }
    return getTransitivePolicyViolationsByComponent(policyEvaluation.getStageTypeId(), componentIdentifier, packageUrl,
        hash, Collections.singletonList(policyEvaluation));
  }

  // Visible for testing
  ApiComponentTransitivePolicyViolationsDTO getTransitivePolicyViolations(
      String stageId,
      ComponentIdentifier componentIdentifier,
      String packageUrl,
      String hash,
      List<PolicyEvaluation> policyEvaluations)
  {
    Pair<Component, List<Pair<PolicyViolation, Component>>> pair =
        getTransitivePolicyViolationsByComponent(stageId, componentIdentifier, packageUrl, hash, policyEvaluations);
    Component foundComponent = pair.getLeft();
    List<Pair<PolicyViolation, Component>> allTransitivePolicyViolations = pair.getRight();

    AuditData.get()
        .setStageId(stageId)
        .setComponentIdentifier(foundComponent.getComponentIdentifier())
        .setComponentHash(foundComponent.getHash());
    ApiComponentTransitivePolicyViolationsDTO result =
        new ApiComponentTransitivePolicyViolationsDTO(foundComponent, allTransitivePolicyViolations);
    sort(result.transitivePolicyViolations);
    return result;
  }

  Pair<Component, List<Pair<PolicyViolation, Component>>> getTransitivePolicyViolationsByComponent(
      String stageId,
      ComponentIdentifier componentIdentifier,
      String packageUrl,
      String hash,
      List<PolicyEvaluation> policyEvaluations)
  {
    if (!stageTypeService.getLicensedStageTypes().contains(StageTypes.getById(stageId))) {
      throw new InvalidLicenseException("Stage '" + stageId + "' is not supported by your license.");
    }
    if (componentIdentifier == null && packageUrl == null && hash == null) {
      throw new BadRequestException("componentIdentifier or packageUrl or hash must be specified.");
    }
    ComponentIdentifier compIdentifier = getComponentIdentifier(componentIdentifier, packageUrl);
    String truncatedHash = HashHelper.truncateHash(hash);
    List<Pair<PolicyViolation, Component>> allTransitivePolicyViolations = new ArrayList<>();
    Component foundComponent = null;
    for (PolicyEvaluation policyEvaluation : policyEvaluations) {
      List<Component> components = getComponents(policyEvaluation.getApplicationId(), policyEvaluation.getScanId());
      if (components == null || components.isEmpty()) {
        continue;
      }
      Component component = getComponentByComponentIdentifierOrHash(components, compIdentifier, truncatedHash);
      if (component == null) {
        continue;
      }
      foundComponent = component;
      if (component.getComponentIdentifier() == null) {
        continue;
      }
      List<Component> transitiveComponents = getTransitiveComponents(component.getComponentIdentifier(), components);
      List<PolicyViolation> activePolicyViolations =
          getPolicyViolations(policyEvaluation.getApplicationId(), stageId, policyEvaluation.getScanId());
      List<Pair<PolicyViolation, Component>> transitivePolicyViolations = activePolicyViolations.stream()
          .map(policyViolation -> Pair.of(policyViolation, getComponentByComponentIdentifierOrHash(
              transitiveComponents, policyViolation.getComponentIdentifier(), policyViolation.getHash())))
          .filter(policyViolationAndComponent -> policyViolationAndComponent.getRight() != null)
          .collect(Collectors.toList());
      allTransitivePolicyViolations.addAll(transitivePolicyViolations);
    }
    if (foundComponent == null) {
      throw new NotFoundException("Component not found.");
    }

    return Pair.of(foundComponent, allTransitivePolicyViolations);
  }

  private ComponentIdentifier getComponentIdentifier(ComponentIdentifier componentIdentifier, String packageUrl) {
    if (componentIdentifier != null) {
      try {
        getComplete(componentIdentifier);
      }
      catch (InvalidComponentIdentifierException e) {
        throw new BadRequestException(e.getMessage(), e);
      }
      return componentIdentifier;
    }
    if (packageUrl != null) {
      PackageUrlIdentifier packageUrlIdentifier = new PackageUrlIdentifier(packageUrl);
      packageUrlIdentifier.ensureCompleteIdentifier();
      return packageUrlIdentifier.toComponentIdentifier();
    }
    return null;
  }

  // Visible for testing
  void sort(List<ApiStagePolicyViolationComponentDTO> policyViolations) {
    policyViolations.sort(Comparator
        .comparing((ApiStagePolicyViolationComponentDTO policyViolation) -> policyViolation.threatLevel)
        .reversed()
        .thenComparing(
            policyViolation -> ApiComponentIdentifierDTOV2.toComponentIdentifier(policyViolation.componentIdentifier),
            Comparator.nullsLast(Comparator.naturalOrder())));
  }

  List<Component> getTransitiveComponentsByAppScanComponent(
      String applicationId,
      String scanId,
      ComponentIdentifier componentIdentifier,
      String packageUrl,
      String hash)
  {
    List<Component> components = getComponents(applicationId, scanId);
    if (components == null) {
      components = Collections.emptyList();
    }
    ComponentIdentifier compIdentifier = getComponentIdentifier(componentIdentifier, packageUrl);
    String truncatedHash = HashHelper.truncateHash(hash);
    Component component = getComponentByComponentIdentifierOrHash(components, compIdentifier, truncatedHash);
    if (component == null) {
      throw new NotFoundException("Component not found.");
    }
    return getTransitiveComponents(component.getComponentIdentifier(), components);
  }

  private List<Component> getComponents(String applicationId, String scanId) {
    try {
      LifecycleReport applicationReport = reportService.getReport(applicationId, scanId);
      ReportEntry reportEntry = applicationReport.getEntry(BOM_JSON.getName());
      if (reportEntry != null) {
        return componentLoaderFactory.createComponentLoader(
            idUtils.getOwnerNotNull(OwnerType.APPLICATION, applicationId))
            .getAll(null, null, reportEntry.buf, null);
      }
      log.debug("{} not found for application id {} and scan id {}.", BOM_JSON.getName(), applicationId, scanId);
    }
    catch (IOException | NotFoundException e) {
      log.debug(e.getMessage(), e);
    }
    return null;
  }

  private Component getComponentByComponentIdentifierOrHash(
      List<Component> components,
      ComponentIdentifier componentIdentifier,
      String hash)
  {
    if (componentIdentifier != null) {
      ComponentIdentifier completeComponentIdentifier = getComplete(componentIdentifier);
      return components.stream()
          .filter(c -> completeComponentIdentifier.equals(getComplete(c.getComponentIdentifier())))
          .findFirst()
          .orElse(null);
    }
    if (hash != null) {
      return components.stream()
          .filter(component -> hash.equals(component.getHash()))
          .findFirst()
          .orElse(null);
    }
    return null;
  }

  List<Component> getInnerComponentsByParentComponentIdentifier(
      List<Component> components,
      ComponentIdentifier parentComponentIdentifier)
  {
    if (parentComponentIdentifier == null) {
      return Collections.emptyList();
    }
    return components.stream()
        .filter(component -> component.getInnerComponentPurls()
            .stream()
            .map(ComponentIdentifierAdapter::toComponentIdentifier)
            .map(this::getComplete)
            .collect(toSet())
            .contains(parentComponentIdentifier))
        .toList();
  }

  private List<Component> getComponentsByParentComponentIdentifier(
      List<Component> components,
      ComponentIdentifier parentComponentIdentifier)
  {
    if (parentComponentIdentifier == null) {
      return Collections.emptyList();
    }
    return components.stream()
        .filter(component -> component.getParentComponentPurls() != null &&
            component.getParentComponentPurls()
                .stream()
                .map(ComponentIdentifierAdapter::toComponentIdentifier)
                .map(this::getComplete)
                .collect(toSet())
                .contains(parentComponentIdentifier))
        .collect(Collectors.toList());
  }

  private List<Component> getTransitiveComponents(
      ComponentIdentifier dependency,
      List<Component> components)
  {
    List<Component> transitiveComponents = new ArrayList<>();
    addTransitiveComponents(transitiveComponents, dependency, components);
    return transitiveComponents;
  }

  private void addTransitiveComponents(
      List<Component> transitiveComponents,
      ComponentIdentifier dependency,
      List<Component> components)
  {
    Set<ComponentIdentifier> processedComponentIdentifiers = new LinkedHashSet<>();
    Queue<ComponentIdentifier> queue = new LinkedList<>();
    processedComponentIdentifiers.add(dependency);
    queue.add(dependency);

    while (!queue.isEmpty()) {
      ComponentIdentifier currentDependency = queue.remove();
      List<Component> childComponents = getComponentsByParentComponentIdentifier(components, currentDependency);
      transitiveComponents.addAll(childComponents);
      List<Component> innerChildComponents =
          getInnerComponentsByParentComponentIdentifier(components, currentDependency);
      transitiveComponents.addAll(innerChildComponents);

      List<ComponentIdentifier> childIdentifierList = childComponents.stream()
          .map(Component::getComponentIdentifier)
          .filter(ci -> !processedComponentIdentifiers.contains(ci))
          .collect(Collectors.toList());
      processedComponentIdentifiers.addAll(childIdentifierList);
      queue.addAll(childIdentifierList);
    }
  }

  private ComponentIdentifier getComplete(ComponentIdentifier componentIdentifier) {
    if (componentIdentifier == null) {
      return null;
    }
    ComponentIdentifier completeComponentIdentifier =
        new ComponentIdentifier(componentIdentifier.getFormat(), componentIdentifier.getCoordinates());
    completeComponentIdentifier.ensureComplete();
    return completeComponentIdentifier;
  }

  private List<PolicyViolation> getPolicyViolations(String applicationId, String stageTypeId, String scanId) {
    try {
      LifecycleReport applicationReport = reportService.getReport(applicationId, scanId);
      ReportEntry reportEntry = applicationReport.getEntry(POLICY_THREATS.getName());
      if (reportEntry != null) {
        return JsonUtils.parse(reportEntry.buf, PolicyThreats.class).aaData.stream()
            .flatMap(component -> toPolicyViolations(applicationId, stageTypeId, component).stream())
            .collect(Collectors.toList());
      }
      log.debug("{} not found for application id {} and scan id {}.", POLICY_THREATS.getName(), applicationId, scanId);
    }
    catch (IOException | NotFoundException e) {
      log.debug(e.getMessage(), e);
    }
    return Collections.emptyList();
  }

  private List<PolicyViolation> toPolicyViolations(
      String applicationId,
      String stageTypeId,
      PolicyThreats.Component component)
  {
    List<PolicyViolation> results = new ArrayList<>();
    for (PolicyThreats.PolicyViolation policyViolation : component.activeViolations) {
      PolicyViolation result = new PolicyViolation();
      result.setApplicationId(applicationId);
      result.setStageTypeId(stageTypeId);
      result.setPolicyId(policyViolation.policyId);
      result.setPolicyName(policyViolation.policyName);
      result.setThreatLevel(policyViolation.policyThreatLevel);
      result.setThreatCategory(
          PolicyThreatCategory.getByName(policyViolation.policyThreatCategory.toLowerCase(Locale.ROOT)));
      result.setId(policyViolation.policyViolationId);
      result.setActionTypeId(policyViolation.actions.isEmpty() ? null : policyViolation.actions.get(0).actionType);
      try {
        result.setConstraintFacts(
            Arrays.asList(JsonUtils.parse(policyViolation.constraintFactsJson, ConstraintFact[].class)));
      }
      catch (IOException e) {
        throw new UncheckedIOException(e);
      }
      result.setComponentIdentifier(component.componentIdentifier);
      result.setHash(component.hash);
      results.add(result);
    }
    return results;
  }

  @Authorize(permission = Permission.READ)
  void checkReadPermission(@SuppressWarnings("unused") @AuthzContext(AuthzContext.Key.OWNER) Owner owner) {
  }
}
