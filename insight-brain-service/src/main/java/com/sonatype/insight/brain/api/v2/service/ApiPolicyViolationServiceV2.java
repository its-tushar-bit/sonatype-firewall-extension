/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.component.ComponentDisplayName;
import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.InvalidComponentIdentifierException;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.api.v2.ApiApplicationAdapter;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationViolationDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationViolationListDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentTransitivePolicyViolationsDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiEnhancedPolicyViolationDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiStagePolicyViolationComponentDTO;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.PolicyAuditDTO;
import com.sonatype.insight.brain.dataaccess.ApplicationComponentDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.component.ComponentDAO;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.landing.UserInterfaceLinksHelper;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ApplicationComponent;
import com.sonatype.insight.brain.model.HashHelper;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.policy.InvalidStageException;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.organization.ApplicationService;
import com.sonatype.insight.brain.policy.StageTypeService;
import com.sonatype.insight.brain.policy.evaluator.PolicyThreats;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationLoader;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationLoader.ApplicationStageView;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationLoader.ApplicationView;
import com.sonatype.insight.brain.policy.evaluator.ScanPolicyEvaluator;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.report.Report;
import com.sonatype.insight.brain.report.ReportEntry;
import com.sonatype.insight.brain.report.ReportService;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightConfig.Feature;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.shiro.authz.UnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.13.0
 */
@Named
public class ApiPolicyViolationServiceV2
{
  private static final Logger log = LoggerFactory.getLogger(ApiPolicyViolationServiceV2.class);

  private final PolicyViolationLoader policyViolationLoader;

  private final ApplicationService applicationService;

  private final ApiApplicationAdapter applicationAdapter;

  private final ApplicationComponentDAO applicationComponentDAO;

  private final PolicyEvaluationDAO policyEvaluationDAO;

  private final OwnerDAO ownerDAO;

  private final ReportService reportService;

  private final StageTypeService stageTypeService;

  private final InsightConfig insightConfig;

  @Inject
  public ApiPolicyViolationServiceV2(
      final PolicyViolationLoader policyViolationLoader,
      final ApplicationService applicationService,
      final ApiApplicationAdapter applicationAdapter,
      final ApplicationComponentDAO applicationComponentDAO,
      final PolicyEvaluationDAO policyEvaluationDAO,
      final OwnerDAO ownerDAO,
      final ReportService reportService,
      final StageTypeService stageTypeService,
      final InsightConfig insightConfig)
  {
    this.policyViolationLoader = policyViolationLoader;
    this.applicationService = applicationService;
    this.applicationAdapter = applicationAdapter;
    this.applicationComponentDAO = applicationComponentDAO;
    this.policyEvaluationDAO = policyEvaluationDAO;
    this.ownerDAO = ownerDAO;
    this.reportService = reportService;
    this.stageTypeService = stageTypeService;
    this.insightConfig = insightConfig;
  }

  public ApiApplicationViolationListDTOV2 getPolicyViolations(final Set<String> policyIds) {
    List<Application> applications = applicationService.getApplications();

    AuditData.get().setData("selectedPolicies", PolicyAuditDTO.transcribe(policyIds))
        .setData("inspectedApplicationCount", applications.size());

    Collection<ApplicationView> appViews = policyViolationLoader.getViolations(applications, null,
        true, violation -> policyIds.contains(violation.getPolicyId()));

    return buildApplicationDTOs(appViews);
  }

  private ApiApplicationViolationListDTOV2 buildApplicationDTOs(Collection<ApplicationView> appViews) {
    ApiApplicationViolationListDTOV2 apiViolationListDTO = new ApiApplicationViolationListDTOV2();
    for (ApplicationView appView : appViews) {
      List<ApiEnhancedPolicyViolationDTOV2> policyViolationDTOs = buildPolicyViolationDTOs(appView);
      if (!policyViolationDTOs.isEmpty()) {
        ApiApplicationViolationDTOV2 apiApplicationViolationDTO = new ApiApplicationViolationDTOV2();
        apiViolationListDTO.applicationViolations.add(apiApplicationViolationDTO);
        apiApplicationViolationDTO.application =
            applicationAdapter.convertToApplicationBaseDTO(appView.getApplication());
        apiApplicationViolationDTO.policyViolations = policyViolationDTOs;
      }
    }
    return apiViolationListDTO;
  }

  private List<ApiEnhancedPolicyViolationDTOV2> buildPolicyViolationDTOs(ApplicationView appView) {
    List<ApiEnhancedPolicyViolationDTOV2> apiPolicyViolationDTOs = new ArrayList<>();
    Application application = appView.getApplication();
    for (ApplicationStageView appStageView : appView.getStageViews()) {
      PolicyEvaluation policyEvaluation = appStageView.getLastEvaluation();
      for (PolicyViolation policyViolation : appStageView.getFilteredViolations()) {
        ApiEnhancedPolicyViolationDTOV2 apiPolicyViolationDTO = new ApiEnhancedPolicyViolationDTOV2();
        apiPolicyViolationDTOs.add(apiPolicyViolationDTO);
        apiPolicyViolationDTO.policyId = policyViolation.getPolicyId();
        apiPolicyViolationDTO.policyName = policyViolation.getPolicyName();
        apiPolicyViolationDTO.policyViolationId = policyViolation.getId();
        apiPolicyViolationDTO.threatLevel = policyViolation.getThreatLevel();
        apiPolicyViolationDTO.openTime = policyViolation.getOpenTime();
        apiPolicyViolationDTO.reportUrl = UserInterfaceLinksHelper.getReportUrl(application.getPublicId(),
            policyEvaluation.getScanId());
        apiPolicyViolationDTO.stageId = policyEvaluation.getStageTypeId();
        apiPolicyViolationDTO.reportId = policyEvaluation.getScanId();
        ApplicationComponent applicationComponent = applicationComponentDAO.getByApplicationIdAndStageTypeIdAndHash(
            application.getId(), policyEvaluation.getStageTypeId(), policyViolation.getHash());
        apiPolicyViolationDTO.component = new ApiComponentDTOV2();
        apiPolicyViolationDTO.component.hash = policyViolation.getHash();
        apiPolicyViolationDTO.component.proprietary = applicationComponent != null
            && applicationComponent.isProprietary();
        apiPolicyViolationDTO.component.componentIdentifier = ApiComponentIdentifierDTOV2
            .fromComponentIdentifier(policyViolation.getComponentIdentifier());
        apiPolicyViolationDTO.component.packageUrl =
            PackageUrlIdentifier.toPackageUrl(policyViolation.getComponentIdentifier());
        ComponentDisplayName componentDisplayName =
            ComponentDisplayNameUtil.fromIdentifier(policyViolation.getComponentIdentifier());
        apiPolicyViolationDTO.component.displayName =
            componentDisplayName != null ? componentDisplayName.toString() : null;
        apiPolicyViolationDTO.constraintViolations = PolicyViolationAdapter.convert(policyViolation);
      }
    }
    return apiPolicyViolationDTOs;
  }

  public void ensureInnerSourceTransitiveWaiverEnabled() {
    if (!insightConfig.isExperimentalFeatureEnabled(Feature.INNER_SOURCE_TRANSITIVE_WAIVER)) {
      throw new UnauthorizedException(Feature.INNER_SOURCE_TRANSITIVE_WAIVER.getFlag() + " feature is disabled.");
    }
  }

  @Authorize(permission = Permission.READ)
  public ApiComponentTransitivePolicyViolationsDTO getTransitivePolicyViolationsByAppScanComponent(
      @AuthzContext(AuthzContext.Key.TYPE) final OwnerType ownerType,
      @AuthzContext(AuthzContext.Key.ID) final String ownerId,
      final String scanId,
      final ComponentIdentifier componentIdentifier,
      final String packageUrl,
      final String hash)
  {
    if (!OwnerType.APPLICATION.equals(ownerType)) {
      throw new BadRequestException("scanId can only be specified for an application.");
    }
    Owner owner = IdUtils.getOwnerNotNull(ownerType, ownerId);
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
      @AuthzContext(AuthzContext.Key.TYPE) final OwnerType ownerType,
      @AuthzContext(AuthzContext.Key.ID) final String ownerId,
      final String stageId,
      final ComponentIdentifier componentIdentifier,
      final String packageUrl,
      final String hash)
  {
    Owner owner = IdUtils.getOwnerNotNull(ownerType, ownerId);
    String stageIdLowercase = stageId.toLowerCase(Locale.ROOT);
    if (!Stage.isValidStageTypeId(stageIdLowercase)) {
      throw new InvalidStageException(stageId);
    }
    List<PolicyEvaluation> policyEvaluations = policyEvaluationDAO
        .getLastByApplicationIdsAndStageIds(ownerDAO.getDescendantOrSelfApplicationIds(owner),
            Collections.singleton(stageIdLowercase));
    return getTransitivePolicyViolations(stageIdLowercase, componentIdentifier, packageUrl, hash, policyEvaluations);
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

    AuditData.get().setStageId(stageId).setComponentIdentifier(foundComponent.getComponentIdentifier())
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
        .comparing((ApiStagePolicyViolationComponentDTO policyViolation) -> policyViolation.threatLevel).reversed()
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
      File reportFile = reportService.getReport(applicationId, scanId);
      ReportEntry reportEntry = Report.getEntry(reportFile, Report.BOM_JSON_FILENAME);
      if (reportEntry != null) {
        return new ComponentDAO(IdUtils.getOwnerNotNull(OwnerType.APPLICATION, applicationId))
            .getAll(null, null, reportEntry.buf, null);
      }
      log.debug("{} not found for application id {} and scan id {}.", Report.BOM_JSON_FILENAME, applicationId,
          scanId);
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

  private List<Component> getComponentsByParentComponentIdentifier(
      List<Component> components,
      ComponentIdentifier parentComponentIdentifier)
  {
    if (parentComponentIdentifier == null) {
      return Collections.emptyList();
    }
    return components.stream()
        .filter(component -> component.getParentComponentPurls() != null &&
            component.getParentComponentPurls().stream()
                .map(ComponentIdentifierAdapter::toComponentIdentifier)
                .map(this::getComplete).collect(Collectors.toSet())
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
    List<Component> childComponents = getComponentsByParentComponentIdentifier(components, dependency);
    transitiveComponents.addAll(childComponents);
    childComponents.forEach(childComponent ->
        addTransitiveComponents(transitiveComponents, childComponent.getComponentIdentifier(), components));
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
      File reportFile = reportService.getReport(applicationId, scanId);
      ReportEntry reportEntry = Report.getEntry(reportFile, ScanPolicyEvaluator.POLICY_THREATS_FILENAME);
      if (reportEntry != null) {
        return JsonUtils.parse(reportEntry.buf, PolicyThreats.class).aaData.stream()
            .flatMap(component -> toPolicyViolations(applicationId, stageTypeId, component).stream())
            .collect(Collectors.toList());
      }
      log.debug("{} not found for application id {} and scan id {}.", ScanPolicyEvaluator.POLICY_THREATS_FILENAME,
          applicationId, scanId);
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
      result.setConstraintFactsJson(policyViolation.constraintFactsJson);
      result.setComponentIdentifier(component.componentIdentifier);
      result.setHash(component.hash);
      results.add(result);
    }
    return results;
  }
}
