/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service.legal;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.experimental.legal.ApiLicenseLegalHdsService;
import com.sonatype.insight.brain.api.experimental.legal.LegalComponentIdentifierUtil;
import com.sonatype.insight.brain.api.v2.dto.ApiLicenseDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiLicenseDataDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiLicenseThreatDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportRawDataDTOV2;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalApplicationDashboardDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalApplicationDashboardResultDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalApplicationReportDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalComponentDashboardDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalComponentReportDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalStageScanDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApplicationLicenseUsageTelemetry;
import com.sonatype.insight.brain.api.v2.dto.legal.LicenseLegalResultsOrder;
import com.sonatype.insight.brain.api.v2.dto.legal.LicenseLegalReviewStatus;
import com.sonatype.insight.brain.api.v2.service.ApiLicenseDataAdapter;
import com.sonatype.insight.brain.api.v2.service.ApiReportDataServiceV2;
import com.sonatype.insight.brain.dataaccess.AggregateFileDAO;
import com.sonatype.insight.brain.dataaccess.ApplicationComponentDAO;
import com.sonatype.insight.brain.dataaccess.ApplicationComponentLicenseDAO;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.component.HashComponentIdentifierDAO;
import com.sonatype.insight.brain.dataaccess.innersource.InnerSourceComponentDAO;
import com.sonatype.insight.brain.dataaccess.legal.ComponentCopyrightDAO;
import com.sonatype.insight.brain.dataaccess.legal.ComponentLegalFileDAO;
import com.sonatype.insight.brain.dataaccess.legal.ComponentObligationAttributionDAO;
import com.sonatype.insight.brain.dataaccess.legal.ComponentObligationDAO;
import com.sonatype.insight.brain.dataaccess.legal.CopyrightOverrideDAO;
import com.sonatype.insight.brain.dataaccess.legal.LegalFileOverrideDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseOverrideDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.tag.TagDAO;
import com.sonatype.insight.brain.hds.ComponentInfoService;
import com.sonatype.insight.brain.model.AggregateFile;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ApplicationComponent;
import com.sonatype.insight.brain.model.ApplicationComponentLicense;
import com.sonatype.insight.brain.model.ApplicationComponentLicensesDTO;
import com.sonatype.insight.brain.model.HashHelper;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.HashComponentIdentifier;
import com.sonatype.insight.brain.model.innersource.InnerSourceComponent;
import com.sonatype.insight.brain.model.legal.LegalFileType;
import com.sonatype.insight.brain.model.license.License;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.organization.ApplicationService;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.report.InnerSourceUtils;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.brain.security.AuthzFilter;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.license.dto.model.AnameAggregateFileGroup;
import com.sonatype.insight.license.dto.model.ComponentLegalCommentDTO;
import com.sonatype.insight.license.dto.model.ComponentLegalFileDTO;
import com.sonatype.insight.license.dto.model.LicenseMetadataDTO;
import com.sonatype.insight.license.dto.model.LicenseObligationDTO;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import org.apache.commons.collections4.map.MultiKeyMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

/**
 * Provides legal information for application components.
 *
 * @since 1.101
 */
@Named
public class ApiLicenseLegalService
{
  private static final Logger log = LoggerFactory.getLogger(ApiLicenseLegalService.class);

  private final MultiLicenseDAO multiLicenseDAO;

  private final ApiLicenseLegalHdsService apiLicenseLegalHdsService;

  private final ApplicationDAO applicationDAO;

  private final PolicyEvaluationDAO policyEvaluationDAO;

  private final ApiReportDataServiceV2 apiReportDataServiceV2;

  private final LegalReportBuilder legalReportBuilder;

  private final TelemetrySender telemetrySender;

  private final ApplicationComponentDAO applicationComponentDAO;

  private final HashComponentIdentifierDAO hashComponentIdentifierDAO;

  private final CopyrightOverrideDAO copyrightOverrideDAO;

  private final ComponentCopyrightDAO componentCopyrightDAO;

  private final ComponentLegalFileDAO componentLegalFileDAO;

  private final LegalFileOverrideDAO legalFileOverrideDAO;

  private final ComponentObligationDAO componentObligationDAO;

  private final ComponentObligationAttributionDAO componentObligationAttributionDAO;

  private final ComponentInfoService componentInfoService;

  private final ApiLicenseDataAdapter apiLicenseDataAdapter;

  private final ProductLicense productLicense;

  private final ApplicationService applicationService;

  private final TagDAO tagDAO;

  private final ApplicationComponentLicenseDAO applicationComponentLicenseDAO;

  private final LicenseOverrideDAO licenseOverrideDAO;

  private final AggregateFileDAO aggregateFileDAO;

  private final LicenseThreatGroupDAO licenseThreatGroupDAO;

  private final InnerSourceComponentDAO innerSourceComponentDAO;

  @Inject
  public ApiLicenseLegalService(
      MultiLicenseDAO multiLicenseDAO,
      ApiLicenseLegalHdsService apiLicenseLegalHdsService,
      ApplicationDAO applicationDAO,
      PolicyEvaluationDAO policyEvaluationDAO,
      ApiReportDataServiceV2 apiReportDataServiceV2,
      LegalReportBuilder legalReportBuilder,
      TelemetrySender telemetrySender,
      ApplicationComponentDAO applicationComponentDAO,
      HashComponentIdentifierDAO hashComponentIdentifierDAO,
      final ComponentCopyrightDAO componentCopyrightDAO,
      ComponentLegalFileDAO componentLegalFileDAO,
      ComponentInfoService componentInfoService,
      ApiLicenseDataAdapter apiLicenseDataAdapter,
      ProductLicense productLicense,
      ApplicationService applicationService,
      TagDAO tagDAO,
      ApplicationComponentLicenseDAO applicationComponentLicenseDAO,
      LicenseOverrideDAO licenseOverrideDAO,
      CopyrightOverrideDAO copyrightOverrideDAO,
      LegalFileOverrideDAO legalFileOverrideDAO,
      ComponentObligationDAO componentObligationDAO,
      ComponentObligationAttributionDAO componentObligationAttributionDAO,
      AggregateFileDAO aggregateFileDAO,
      LicenseThreatGroupDAO licenseThreatGroupDAO,
      InnerSourceComponentDAO innerSourceComponentDAO)
  {
    this.multiLicenseDAO = multiLicenseDAO;
    this.apiLicenseLegalHdsService = apiLicenseLegalHdsService;
    this.applicationDAO = applicationDAO;
    this.policyEvaluationDAO = policyEvaluationDAO;
    this.apiReportDataServiceV2 = apiReportDataServiceV2;
    this.legalReportBuilder = legalReportBuilder;
    this.telemetrySender = telemetrySender;
    this.applicationComponentDAO = applicationComponentDAO;
    this.hashComponentIdentifierDAO = hashComponentIdentifierDAO;
    this.componentCopyrightDAO = componentCopyrightDAO;
    this.componentLegalFileDAO = componentLegalFileDAO;
    this.componentInfoService = componentInfoService;
    this.componentInfoService.setToolName("ci");
    this.apiLicenseDataAdapter = apiLicenseDataAdapter;
    this.productLicense = productLicense;
    this.applicationService = applicationService;
    this.tagDAO = tagDAO;
    this.applicationComponentLicenseDAO = applicationComponentLicenseDAO;
    this.licenseOverrideDAO = licenseOverrideDAO;
    this.copyrightOverrideDAO = copyrightOverrideDAO;
    this.legalFileOverrideDAO = legalFileOverrideDAO;
    this.componentObligationDAO = componentObligationDAO;
    this.componentObligationAttributionDAO = componentObligationAttributionDAO;
    this.aggregateFileDAO = aggregateFileDAO;
    this.licenseThreatGroupDAO = licenseThreatGroupDAO;
    this.innerSourceComponentDAO = innerSourceComponentDAO;
  }

  public ApiLicenseLegalApplicationDashboardResultDTO getLicenseLegalApplicationsDashboard(
      Set<String> organizationIds,
      Set<String> applicationIds,
      Set<String> tagIds,
      Set<String> stageTypeIds,
      Set<String> licenseIds,
      Set<LicenseLegalReviewStatus> reviewStatus,
      LicenseLegalResultsOrder order,
      int page,
      int pageSize)
  {
    checkLicense();

    if (page <= 0 || pageSize <= 0) {
      throw new BadRequestException("Request must include page and pageSize values greater than zero.");
    }

    Map<String, Application> mapApplicationIds =
        getApplicationsByIdsAndOrganizationIdsAndTagIds(organizationIds, applicationIds, tagIds).stream()
            .collect(Collectors.toMap(Application::getId, Function.identity()));
    Set<String> applicationIdsToCheck = new HashSet<>(mapApplicationIds.keySet());

    Set<String> stageTypeIdsToCheck =
        isEmpty(stageTypeIds)
            ? StageTypes.getAll().stream().map(StageType::getId).collect(Collectors.toSet())
            : stageTypeIds;

    if (isNotEmpty(applicationIdsToCheck) && isNotEmpty(reviewStatus) && !reviewStatus
        .containsAll(Sets.newHashSet(LicenseLegalReviewStatus.OPEN, LicenseLegalReviewStatus.NOT_STARTED))) {
      List<Object[]> applicationIdsAndStageTypeIds =
          applicationComponentDAO.getApplicationIdsAndStageTypeIdsByReviewStatus(applicationIdsToCheck,
              stageTypeIdsToCheck, reviewStatus.contains(LicenseLegalReviewStatus.OPEN));

      recalculateApplicationIdsAndStateTypeIds(applicationIdsAndStageTypeIds, applicationIdsToCheck,
          stageTypeIdsToCheck);
    }

    if (isNotEmpty(applicationIdsToCheck) && isNotEmpty(licenseIds)) {
      List<Object[]> applicationIdsAndStageTypeIds = applicationComponentDAO
          .getApplicationIdsAndStageTypeIdsByLicenses(applicationIdsToCheck, stageTypeIdsToCheck, licenseIds);

      recalculateApplicationIdsAndStateTypeIds(applicationIdsAndStageTypeIds, applicationIdsToCheck,
          stageTypeIdsToCheck);
    }

    if (isEmpty(applicationIdsToCheck) || isEmpty(stageTypeIdsToCheck)) {
      return new ApiLicenseLegalApplicationDashboardResultDTO();
    }

    List<PolicyEvaluation> policyEvaluations =
        policyEvaluationDAO.getLastByApplicationIdsAndStageIds(applicationIdsToCheck, stageTypeIdsToCheck);

    int startIndex = (page - 1) * pageSize;
    if (startIndex >= policyEvaluations.size()) {
      ApiLicenseLegalApplicationDashboardResultDTO resultDto = new ApiLicenseLegalApplicationDashboardResultDTO();
      resultDto.totalResultsCount = policyEvaluations.size();
      return resultDto;
    }

    Map<String, List<String>> mapApplicationIdTagNames = getTagNamesByApplicationIds(applicationIdsToCheck);

    List<ApiLicenseLegalApplicationDashboardDTO> result = new ArrayList<>(policyEvaluations.size());
    for (PolicyEvaluation policyEvaluation : policyEvaluations) {
      Application application = mapApplicationIds.get(policyEvaluation.getApplicationId());

      ApiLicenseLegalApplicationDashboardDTO dto = new ApiLicenseLegalApplicationDashboardDTO();
      dto.applicationId = application.getId();
      dto.applicationName = application.getName();
      dto.applicationPublicId = application.getPublicId();
      dto.lastScanTime = policyEvaluation.getTime().getTime();
      dto.stageTypeId = policyEvaluation.getStageTypeId();
      dto.stageTypeName = StageTypes.getById(policyEvaluation.getStageTypeId()).getName();
      dto.applicationTagNames.addAll(mapApplicationIdTagNames.get(application.getId()));

      result.add(dto);
    }

    result.sort(newDashboardComparator(order));

    ApiLicenseLegalApplicationDashboardResultDTO resultDto = new ApiLicenseLegalApplicationDashboardResultDTO();
    resultDto.totalResultsCount = result.size();

    result = result.subList(startIndex, Math.min(page * pageSize, result.size()));
    calculateComponentsReviewed(result);

    resultDto.results = result;
    return resultDto;
  }

  public List<ApiLicenseLegalComponentDashboardDTO> getLicenseLegalComponentsDashboard(
      Set<String> organizationIds,
      Set<String> applicationIds,
      Set<String> tagIds,
      Set<String> stageTypeIds,
      Set<String> licenseIds)
  {
    checkLicense();

    List<Application> applications =
        getApplicationsByIdsAndOrganizationIdsAndTagIds(organizationIds, applicationIds, tagIds);
    Set<String> stageTypeIdsToCheck = isEmpty(stageTypeIds)
        ? StageTypes.getAll().stream().map(StageType::getId).collect(Collectors.toSet())
        : stageTypeIds;

    Map<String, ApiLicenseLegalComponentDashboardDTO> mapHashComponent = new HashMap<>();
    Map<String, Set<String>> mapHashApplicationOccurrences = new HashMap<>();
    Map<String, Set<String>> mapHashLicenseIds = new HashMap<>();

    try (TransactionContext tx = applicationComponentDAO.createTransactionContext()) {
      for (Application application : applications) {
        for (String stageTypeId : stageTypeIdsToCheck) {
          List<ApplicationComponent> applicationComponents =
              applicationComponentDAO.getByApplicationIdAndStageTypeId(tx, application.getId(), stageTypeId);

          for (ApplicationComponent applicationComponent : applicationComponents) {
            if (applicationComponent.getComponentIdentifier() == null) {
              continue;
            }

            ApiLicenseLegalComponentDashboardDTO dto = mapHashComponent.getOrDefault(applicationComponent.getHash(),
                new ApiLicenseLegalComponentDashboardDTO(applicationComponent));

            Set<String> licensesAlreadyFound =
                mapHashLicenseIds.getOrDefault(applicationComponent.getHash(), new HashSet<>());

            LicenseOverride licenseOverride = licenseOverrideDAO.getAppliedByOwnerIdAndComponentIdentifierWithHierarchy(
                tx, application, applicationComponent.getComponentIdentifier());

            if (licenseOverride != null && isNotEmpty(licenseOverride.getLicenseIds())) {
              licensesAlreadyFound.addAll(licenseOverride.getLicenseIds());
            }
            else {
              List<ApplicationComponentLicense> applicationComponentLicenses =
                  applicationComponentLicenseDAO.getByApplicationComponentId(tx, applicationComponent.getId());

              licensesAlreadyFound.addAll(applicationComponentLicenses.stream()
                  .map(ApplicationComponentLicense::getEffectiveLicenseId)
                  .collect(Collectors.toSet()));
            }

            if (isNotEmpty(licensesAlreadyFound)) {
              Set<String> applicationOccurrences =
                  mapHashApplicationOccurrences.getOrDefault(applicationComponent.getHash(), new HashSet<>());
              applicationOccurrences.add(application.getId());
              dto.applicationOccurrences = applicationOccurrences.size();

              mapHashApplicationOccurrences.put(applicationComponent.getHash(), applicationOccurrences);
              mapHashLicenseIds.put(applicationComponent.getHash(), licensesAlreadyFound);
              mapHashComponent.put(applicationComponent.getHash(), dto);
            }
          }
        }
      }
    }

    return mapHashComponent.values().stream()
        .filter(dto -> isEmpty(licenseIds) || !Collections.disjoint(mapHashLicenseIds.get(dto.hash), licenseIds))
        .map(dto -> {
          dto.licenseNames.addAll(mapHashLicenseIds.get(dto.hash).stream()
              .map(licenseId -> multiLicenseDAO.getById(licenseId).getShortDisplayName())
              .collect(Collectors.toSet()));
          return dto;
        }).collect(Collectors.toList());
  }

  @AuthzFilter(permission = Permission.LEGAL_REVIEWER, context = AuthzFilter.Context.APPLICATION)
  protected List<Application> getApplicationsByIdsAndOrganizationIdsAndTagIds(
      Set<String> organizationIds,
      Set<String> applicationIds,
      Set<String> tagIds)
  {
    return applicationService
        .getApplicationsByIdsAndOrganizationIdsAndTagIdsNoAuthz(organizationIds, applicationIds, tagIds);
  }

  @Authorize(permission = Permission.LEGAL_REVIEWER)
  public ApiLicenseLegalApplicationReportDTO getLicenseLegalApplicationReport(
      @AuthzContext(Key.OWNER) Owner application, String stageId)
  {
    checkLicense();
    log.info("Processing license metadata request for {}", application.getId());
    ApiReportRawDataDTOV2 latestRawReport = getLastRawApplicationReportByStageId(application.getPublicId(), stageId)
        .orElseThrow(() -> new NotFoundException(
            "Report for application " + application.getId() + " at stage " + stageId + " not found."));
    return getApplicationReportFromReportRawData(application, latestRawReport);
  }

  @Authorize(permission = Permission.LEGAL_REVIEWER)
  public ApiLicenseLegalApplicationReportDTO getLicenseLegalApplicationReport(
      @AuthzContext(Key.OWNER) Owner application)
  {
    checkLicense();

    log.info("Processing license metadata request for {}", application.getId());
    ApiReportRawDataDTOV2 latestRawReport = getLastRawApplicationReport(application.getPublicId())
        .orElseThrow(() -> new NotFoundException("Report for application " + application.getId() + " not found."));
    return getApplicationReportFromReportRawData(application, latestRawReport);
  }

  private ApiLicenseLegalApplicationReportDTO getApplicationReportFromReportRawData(
      final Owner application, final ApiReportRawDataDTOV2 latestRawReport)
  {
    filterInnerSourceComponents(latestRawReport);

    Map<ComponentIdentifier, Set<ApiLicenseDTO>> componentIdentifierToMultiLicenses =
        getReportMultiLicenses(latestRawReport);

    Set<ApiLicenseDTO> allMultiLicenses = componentIdentifierToMultiLicenses.entrySet().stream()
        .flatMap(e -> e.getValue().stream())
        .collect(Collectors.toCollection(LinkedHashSet::new));

    Map<ApiLicenseDTO, Set<License>> multiLicenseToSingleLicense =
        buildMultiLicenseToSingleLicenseMap(allMultiLicenses);

    Set<License> allSingleLicenses = multiLicenseToSingleLicense.values().stream()
        .flatMap(Collection::stream)
        .collect(Collectors.toSet());

    sendApplicationTelemetryData(application.getPublicId(), latestRawReport, allMultiLicenses);

    Map<String, LicenseMetadataDTO> licenseMetadataById = allMultiLicenses.isEmpty() ? Collections.emptyMap() :
        apiLicenseLegalHdsService.getLicenseMetadata(
            allSingleLicenses.stream()
                .map(License::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new)))
            .stream()
            .collect(toMap(LicenseMetadataDTO::getLicenseId, Function.identity()));

    final Set<ApiReportComponentDTOV2> apiReportComponentDTOV2s = new HashSet<>(latestRawReport.components);

    Map<ApiReportComponentDTOV2, ComponentIdentifierLegalData> componentIdentifierToLegalData =
        fetchApiReportComponentDTOV2ToLegalData(
            application,
            apiReportComponentDTOV2s
        );

    Map<ComponentIdentifier, Set<ComponentLegalCommentDTO>> componentLegalCommentsByComponentIdentifier =
        getComponentLegalCommentsByComponentIdentifier(Collections.singleton(latestRawReport));

    Map<ComponentIdentifier, Set<ComponentLegalFileDTO>> componentLegalFilesByComponentIdentifier =
        getComponentLegalFilesByComponentIdentifier(Collections.singleton(latestRawReport));

    log.info("Building license metadata report.");
    return legalReportBuilder
        .getLicenseLegalApplicationReport(
            latestRawReport,
            componentIdentifierToLegalData,
            componentLegalCommentsByComponentIdentifier,
            componentLegalFilesByComponentIdentifier,
            multiLicenseToSingleLicense,
            licenseMetadataById);
  }

  private void filterInnerSourceComponents(final ApiReportRawDataDTOV2 latestRawReport) {
    Set<PackageUrlIdentifier> componentPurls = latestRawReport.components.stream()
        .filter(c -> Objects.nonNull(c.componentIdentifier))
        .map(c -> InnerSourceUtils.getVersionlessPackageUrl(c.componentIdentifier.toComponentIdentifier()))
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());

    Set<String> innerSourcePackageUrls = innerSourceComponentDAO.getByPackageUrls(componentPurls).stream()
        .map(InnerSourceComponent::getPackageUrl)
        .collect(Collectors.toSet());

    latestRawReport.components.removeIf(c ->
        Objects.nonNull(c.componentIdentifier) &&
            LegalComponentIdentifierUtil
                .isComponentAKnownInnerSource(innerSourcePackageUrls, c.componentIdentifier.toComponentIdentifier()));
  }

  /**
   * <p>Given an {@link OwnerType} and ownerId specifying an {@link Owner}, with either a {@link ComponentIdentifier},
   * package url, or component hash, this generates a {@link ApiLicenseLegalComponentReportDTO} containing the following
   * component information:</p>
   * <ul>
   *   <li>Licenses</li>
   *   <li>License obligations</li>
   *   <li>Obligation status</li>
   *   <li>Obligation attributions</li>
   *   <li>Copyright statements</li>
   *   <li>Notice texts</li>
   *   <li>License texts</li>
   * </ul>
   * <p>The {@link Owner} and its ancestors as well as the {@link ComponentIdentifier} determine</p>
   * <ul>
   *   <li>Overrides for licenses (which determine license obligations), copyrights, notice texts, and license
   *   texts.</li>
   *   <li>License obligation data (i.e. statuses, comments, and attributions).</li>
   * </ul>
   * <p>preference is given to overrides and data at lower scopes (starting at the given scope).</p>
   * Note: specifying more than one of component identifier, package url, or hash, or not specifying any will cause a
   * {@link BadRequestException} to be thrown.
   *
   * @param ownerType            the {@link OwnerType} of the {@link Owner}, required.
   * @param ownerId              the id of the {@link Owner}, required.
   * @param componentIdentifier  a {@link ComponentIdentifier}, optional.
   * @param packageUrl           a package url string, optional.
   * @param hash                 a component hash, optional.
   * @param identificationSource the component identification source, optional.
   * @param scanId               the scan id for the report where the component was identified, only used with a third
   *                             party identification source, optional.
   * @return an {@link ApiLicenseLegalComponentReportDTO} for the given component.
   * @throws IOException if we have issues communicating with HDS.
   */
  @Authorize(permission = Permission.LEGAL_REVIEWER)
  public ApiLicenseLegalComponentReportDTO getLicenseLegalComponentReport(
      @AuthzContext(AuthzContext.Key.TYPE) OwnerType ownerType,
      @AuthzContext(AuthzContext.Key.ID) String ownerId,
      ComponentIdentifier componentIdentifier,
      String packageUrl,
      String hash,
      HttpServletRequest httpRequest,
      String identificationSource,
      String scanId) throws IOException
  {
    checkLicense();

    Owner owner = IdUtils.getOwnerNotNull(ownerType, ownerId);
    ComponentIdentifier compIdentifier = getComponentIdentifier(componentIdentifier, packageUrl, hash);
    // We get the component by coordinates from HDS
    // A passed in aggregate component synthetic hash (i.e. hash of its coordinates)
    // will be different to the HDS component hash i.e. hash may not equal component.getHash()
    Component component = componentInfoService.augmentComponentDetails(owner, componentInfoService
        .getUnaugmentedComponentDetails(owner, compIdentifier, httpRequest, identificationSource, scanId));
    if (component.getHash() == null && hash != null) {
      component.setHash(hash);
    }
    ApiLicenseDataDTOV2 licenseData = apiLicenseDataAdapter.convertToDTOV2(component);

    Set<ApiLicenseDTO> allMultiLicenses = getAllLicenses(licenseData);

    Map<ApiLicenseDTO, Set<License>> multiLicenseToSingleLicense =
        buildMultiLicenseToSingleLicenseMap(allMultiLicenses);

    Set<License> allSingleLicenses = multiLicenseToSingleLicense.values().stream()
        .flatMap(Collection::stream)
        .collect(Collectors.toSet());

    Map<String, LicenseMetadataDTO> licenseMetadataById =
        allMultiLicenses.isEmpty() ? Collections.emptyMap() :
            apiLicenseLegalHdsService.getLicenseMetadata(
                allSingleLicenses.stream()
                    .map(License::getId)
                    .collect(Collectors.toCollection(LinkedHashSet::new)))
                .stream()
                .collect(toMap(LicenseMetadataDTO::getLicenseId, Function.identity()));

    Set<ComponentLegalCommentDTO> componentLegalComments =
        getComponentLegalComments(compIdentifier, component.getHash());
    Set<ComponentLegalFileDTO> componentLegalFiles =
        apiLicenseLegalHdsService.getComponentLegalFiles(Collections.singleton(compIdentifier));

    ApiReportComponentDTOV2 apiReportComponentDTOV2 = LegalComponentIdentifierUtil.toApiReportComponentDTOV2(component,
        licenseData);

    ComponentIdentifierLegalData componentIdentifierLegalData =
        fetchApiReportComponentDTOV2ToLegalData(
            owner,
            Collections.singleton(apiReportComponentDTOV2)
        ).entrySet().iterator().next().getValue();

    // We prefer hash over component.getHash() to get the stage scans since
    // hash should always equal ApplicationComponent.getHash()
    // component.getHash() may not equal ApplicationComponent.getHash()
    componentIdentifierLegalData.setStageScans(getStageScans(owner, hash, compIdentifier));

    return legalReportBuilder.getLicenseLegalComponentReport(
        apiReportComponentDTOV2,
        componentIdentifierLegalData,
        componentLegalComments,
        componentLegalFiles,
        multiLicenseToSingleLicense,
        licenseMetadataById
    );
  }

  private Map<ApiLicenseDTO, Set<License>> buildMultiLicenseToSingleLicenseMap(
      final Set<ApiLicenseDTO> allMultiLicenses)
  {
    return allMultiLicenses.stream()
        .collect(Collectors.toMap(
            Function.identity(),
            m -> multiLicenseDAO.getLicensesByMultiLicenseIdNotNull(m.licenseId),
            (prev, next) -> next,
            HashMap::new
        ));
  }

  private ApiLicenseThreatDTOV2 getHighestLicenseThreatGroupWithHierarchy(
      TransactionContext tx, String ownerId, Set<String> licenseIds)
  {
    LicenseThreatGroup result =
        licenseThreatGroupDAO.getHighestLicenseThreatGroupWithHierarchy(tx, ownerId, licenseIds);
    return result == null ? null : new ApiLicenseDataAdapter().convert(result);
  }

  /**
   * Tries to find the scan report information for each applicable stage for the given owner and application component
   * hash or component identifier.
   * <p>
   * If the passed owner is not an application, then null is returned because under the context of an organization, a
   * component may belong to multiple different application stage scans.
   * <p>
   * If the passed owner is an application, then its application components are requested either by application
   * component hash (may be passed) or by component identifier (always passed).
   * <p>
   * If an application component exists for a given stage, then the last policy evaluation is found for the application
   * and stage and its details are returned.
   *
   * @param owner                    the {@link Owner} to get the stage scans for.
   * @param applicationComponentHash the application component hash to get the stage scans for, may be null, if it is
   *                                 null the componentIdentifier will be used instead.
   * @param componentIdentifier      the {@link ComponentIdentifier} to get the stage scans for, ignored if the
   *                                 applicationComponentHash is passed.
   * @return null if the owner is not an application, else a list of {@link ApiLicenseLegalStageScanDTO} for each
   * applicable stage.
   */
  private List<ApiLicenseLegalStageScanDTO> getStageScans(
      Owner owner,
      String applicationComponentHash,
      ComponentIdentifier componentIdentifier)
  {
    if (owner.getType() != OwnerType.APPLICATION) {
      return null;
    }

    List<ApiLicenseLegalStageScanDTO> results = new ArrayList<>();
    try (TransactionContext tx = applicationComponentDAO.createTransactionContext()) {
      List<ApplicationComponent> applicationComponents;
      if (applicationComponentHash != null) {
        applicationComponents =
            applicationComponentDAO.getByApplicationIdAndHash(tx, owner.getId(), applicationComponentHash);
      }
      else {
        applicationComponents =
            applicationComponentDAO.getByApplicationIdAndComponentIdentifier(tx, owner.getId(), componentIdentifier);
      }
      for (StageType stageType : StageTypes.getAll()) {
        if (StageTypes.isIgnoredForDashboard(stageType.getId())) {
          continue;
        }
        ApiLicenseLegalStageScanDTO apiLicenseLegalStageScanDTO = new ApiLicenseLegalStageScanDTO();
        apiLicenseLegalStageScanDTO.setStageName(stageType.getName());
        ApplicationComponent applicationComponentForStage = applicationComponents.stream()
            .filter(applicationComponent -> stageType.getId().equals(applicationComponent.getStageTypeId())).findFirst()
            .orElse(null);
        if (applicationComponentForStage != null) {
          PolicyEvaluation policyEvaluation =
              policyEvaluationDAO.getLastByApplicationIdAndStageId(tx, owner.getId(), stageType.getId());
          if (policyEvaluation != null) {
            apiLicenseLegalStageScanDTO.setScanId(policyEvaluation.getScanId());
            apiLicenseLegalStageScanDTO.setScanDate(policyEvaluation.getTime());
          }
        }
        results.add(apiLicenseLegalStageScanDTO);
      }
    }
    return results;
  }

  @VisibleForTesting
  Set<ComponentLegalCommentDTO> getComponentLegalComments(
      final ComponentIdentifier componentIdentifier,
      final String componentHash)
  {
    if (componentIdentifier.isAname()) {
      final List<String> componentAggregateHashes = getAggregateHashes(componentIdentifier);
      if (componentAggregateHashes.isEmpty()) {
        return Collections.emptySet();
      }
      final AnameAggregateFileGroup anameAggregateFileGroup =
          new AnameAggregateFileGroup(componentIdentifier, componentAggregateHashes);
      return apiLicenseLegalHdsService.getAnameComponentLegalComments(
          Collections.singleton(anameAggregateFileGroup),
          ImmutableMap.of(componentIdentifier, componentHash));
    }
    else {
      return apiLicenseLegalHdsService.getComponentLegalComments(Collections.singleton(componentIdentifier));
    }
  }

  private List<String> getAggregateHashes(final ComponentIdentifier componentIdentifier) {
    if (componentIdentifier == null) {
      return Collections.emptyList();
    }
    final ApplicationComponent lastByComponentIdentifier =
        applicationComponentDAO.getLastByComponentIdentifier(componentIdentifier);
    if (lastByComponentIdentifier == null) {
      return Collections.emptyList();
    }
    return aggregateFileDAO.getByApplicationComponentId(lastByComponentIdentifier.getId()).stream()
        .map(AggregateFile::getHash)
        .collect(Collectors.toList());
  }

  private ComponentIdentifier getComponentIdentifier(
      ComponentIdentifier componentIdentifier,
      String packageUrl,
      String hash)
  {
    if (!isOnlyOneTrue(componentIdentifier != null, packageUrl != null, hash != null)) {
      throw new BadRequestException("Only one of componentIdentifier, packageUrl, or hash must be specified.");
    }
    if (componentIdentifier != null) {
      componentIdentifier.ensureComplete();
      return componentIdentifier;
    }
    if (packageUrl != null) {
      return new PackageUrlIdentifier(packageUrl).ensureCompleteIdentifier();
    }
    String truncatedHash = HashHelper.truncateHash(hash);
    HashComponentIdentifier hashComponentIdentifier = hashComponentIdentifierDAO.getByHash(truncatedHash);
    if (hashComponentIdentifier != null) {
      return hashComponentIdentifier.getComponentIdentifier();
    }
    ApplicationComponent applicationComponent = applicationComponentDAO.getLastByHash(truncatedHash);
    if (applicationComponent != null) {
      ComponentIdentifier applicationComponentIdentifier = applicationComponent.getComponentIdentifier();
      if (applicationComponentIdentifier != null) {
        return applicationComponentIdentifier;
      }
    }
    throw new BadRequestException("Unable to determine componentIdentifier.");
  }

  private boolean isOnlyOneTrue(boolean... booleans) {
    return IntStream.range(0, booleans.length).mapToObj(idx -> booleans[idx]).filter(bool -> bool).count() == 1;
  }

  private Set<ComponentIdentifier> getComponentIdentifiers(Collection<ApiReportRawDataDTOV2> rawReports) {
    return rawReports.stream()
        .flatMap(rawReport -> getComponentIdentifiers(rawReport).stream())
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  private Set<Pair<ComponentIdentifier, String>> getComponentIdentifierHashes(
      Collection<ApiReportRawDataDTOV2> rawReports)
  {
    return rawReports.stream()
        .flatMap(rawReport -> getComponentIdentifierHashes(rawReport).stream())
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  private Set<ComponentIdentifier> getComponentIdentifiers(ApiReportRawDataDTOV2 rawReport) {
    return rawReport.components.stream()
        .map(component -> component.componentIdentifier)
        .filter(Objects::nonNull)
        .map(apiComponentIdentifierDTOV2 -> apiComponentIdentifierDTOV2.toComponentIdentifier())
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  private Set<Pair<ComponentIdentifier, String>> getComponentIdentifierHashes(ApiReportRawDataDTOV2 rawReport) {
    return rawReport.components.stream()
        .map(component -> Pair.of(component.componentIdentifier, component.hash))
        .filter(pair -> pair.getLeft() != null)
        .map(pair -> Pair.of(pair.getLeft().toComponentIdentifier(), pair.getRight()))
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  private Map<ComponentIdentifier, Set<ApiLicenseDTO>> getReportMultiLicenses(ApiReportRawDataDTOV2 rawReport) {
    Map<ComponentIdentifier, Set<ApiLicenseDTO>> componentToLicenses = new HashMap<>();
    for (final ApiReportComponentDTOV2 apiReportComponentDTOV2 : rawReport.components) {
      if (apiReportComponentDTOV2.componentIdentifier == null) {
        continue;
      }
      if (apiReportComponentDTOV2.licenseData == null) {
        componentToLicenses.put(apiReportComponentDTOV2.componentIdentifier.toComponentIdentifier(), new HashSet<>());
      }
      Set<ApiLicenseDTO> allLicenses = getAllLicenses(apiReportComponentDTOV2.licenseData);
      componentToLicenses.put(apiReportComponentDTOV2.componentIdentifier.toComponentIdentifier(), allLicenses);
    }

    return componentToLicenses;
  }

  private Set<ApiLicenseDTO> getAllLicenses(ApiLicenseDataDTOV2 licenses) {
    return Stream.concat(Stream.concat(licenses.declaredLicenses.stream(), licenses.observedLicenses.stream()),
        licenses.effectiveLicenses.stream()).collect(Collectors.toCollection(LinkedHashSet::new));
  }

  private Map<ComponentIdentifier, Set<ComponentLegalCommentDTO>> getComponentLegalCommentsByComponentIdentifier(
      Collection<ApiReportRawDataDTOV2> rawReports)
  {
    final Map<ComponentIdentifier, String> componentIdentifiers = getComponentIdentifierHashes(rawReports)
        .stream()
        .collect(Collectors.toMap(
            Pair::getKey,
            Pair::getValue,
            (h1, h2) -> h1));

    // get all non-a-name comments
    final Map<ComponentIdentifier, Set<ComponentLegalCommentDTO>>
        nonAname = getNonAnameComponentLegalComments(componentIdentifiers);

    // get all a-name comments
    final Map<ComponentIdentifier, Set<ComponentLegalCommentDTO>>
        aname = getAnameComponentLegalComments(componentIdentifiers);

    // combine it all together. components in aname and nonAname do not intersect, so we can just pu them together
    // in the same map
    final Map<ComponentIdentifier, Set<ComponentLegalCommentDTO>> result = new HashMap<>();
    result.putAll(nonAname);
    result.putAll(aname);

    return result;
  }

  /**
   * Filters out A-Name components and performs a request to HDS to retrieve Comments for the rest of components
   *
   * @param componentIdentifiers Pairs of ComponentIdentifier - Component Hash
   * @return Map from component identifier to a set of comments
   */
  @VisibleForTesting
  Map<ComponentIdentifier, Set<ComponentLegalCommentDTO>> getAnameComponentLegalComments(
      final Map<ComponentIdentifier, String> componentIdentifiers)
  {
    final Set<AnameAggregateFileGroup> aNameComponents = componentIdentifiers.keySet().stream()
        .filter(ComponentIdentifier::isAname)
        .map(s -> new AnameAggregateFileGroup(s,
            getAggregateHashes(s)))
        .collect(Collectors.toCollection(LinkedHashSet::new));

    return apiLicenseLegalHdsService.getAnameComponentLegalComments(
        aNameComponents,
        componentIdentifiers).stream()
        // reconstruct component comments by adding component hashes to each element of returned set
        .collect(
            groupingBy(c -> LegalComponentIdentifierUtil.removeClassifierAndExtension(c.getComponentIdentifier()),
                Collectors.toCollection(LinkedHashSet::new)));
  }

  /**
   * Filters out non-A-Name components and performs a request to HDS to retrieve Comments for the A-Name components
   *
   * @param componentIdentifiers Pairs of ComponentIdentifier - Component Hash
   * @return Map from component identifier to a set of comments
   */
  private Map<ComponentIdentifier, Set<ComponentLegalCommentDTO>> getNonAnameComponentLegalComments(
      final Map<ComponentIdentifier, String> componentIdentifiers)
  {
    final Set<ComponentIdentifier> nonAnameComponents = componentIdentifiers.keySet().stream()
        .filter(s -> !s.isAname())
        .collect(Collectors.toCollection(LinkedHashSet::new));

    return apiLicenseLegalHdsService.getComponentLegalComments(nonAnameComponents).stream()
        .collect(
            groupingBy(c -> LegalComponentIdentifierUtil.removeClassifierAndExtension(c.getComponentIdentifier()),
                Collectors.toCollection(LinkedHashSet::new)));
  }

  private Map<ComponentIdentifier, Set<ComponentLegalFileDTO>> getComponentLegalFilesByComponentIdentifier(
      Collection<ApiReportRawDataDTOV2> rawReports)
  {
    return apiLicenseLegalHdsService.getComponentLegalFiles(
        getComponentIdentifiers(rawReports)).stream()
        .collect(Collectors
            .groupingBy(c -> LegalComponentIdentifierUtil.removeClassifierAndExtension(c.getComponentIdentifier()),
                Collectors.toCollection(LinkedHashSet::new)));
  }

  @VisibleForTesting
  Optional<ApiReportRawDataDTOV2> getLastRawApplicationReport(String applicationPublicId) {
    return Optional.ofNullable(applicationDAO.getByPublicId(applicationPublicId)).flatMap(
        application -> policyEvaluationDAO
            .getLastByApplicationIds(Collections.singleton(application.getId()))
            .stream()
            .max(Comparator.comparing(PolicyEvaluation::getTime))
            .map(policyEvaluation -> getLastRawApplicationReport(application.getPublicId(), policyEvaluation)));
  }

  @VisibleForTesting
  Optional<ApiReportRawDataDTOV2> getLastRawApplicationReportByStageId(String applicationPublicId, String stageId) {
    return Optional.ofNullable(applicationDAO.getByPublicId(applicationPublicId)).flatMap(
        application -> policyEvaluationDAO
            .getLastByApplicationIdsAndStageIds(Collections.singleton(application.getId()),
                Collections.singleton(stageId))
            .stream()
            .max(Comparator.comparing(PolicyEvaluation::getTime))
            .map(policyEvaluation -> getLastRawApplicationReport(application.getPublicId(), policyEvaluation)));
  }

  private ApiReportRawDataDTOV2 getLastRawApplicationReport(
      String applicationPublicId,
      PolicyEvaluation lastPolicyEvaluation)
  {
    try {
      return apiReportDataServiceV2.getDataNoAuth(applicationPublicId, lastPolicyEvaluation.getScanId());
    }
    catch (IOException e) {
      throw new UncheckedIOException(e.getMessage(), e);
    }
  }

  private void sendApplicationTelemetryData(
      String applicationPublicId,
      ApiReportRawDataDTOV2 latestRawReport,
      Set<ApiLicenseDTO> multiLicenses)
  {
    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.APPLICATION_LICENSE_USAGE);
    telemetryData.put(ApplicationLicenseUsageTelemetry.ATTRIBUTE_NAME,
        new ApplicationLicenseUsageTelemetry(
            applicationPublicId,
            latestRawReport.components.stream()
                .map(component -> component.hash)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new)),
            multiLicenses.stream()
                .map(license -> license.licenseId)
                .collect(Collectors.toSet())));

    telemetrySender.send(telemetryData);
  }

  private void checkLicense() {
    if (!productLicense.hasFeature(LicensedFeature.ADVANCED_LEGAL_PACK)) {
      log.debug("License does not support Advanced Legal Pack features");
      throw new InvalidLicenseException();
    }
  }

  private Map<String, List<String>> getTagNamesByApplicationIds(Set<String> applicationIds) {
    Map<String, List<String>> mapApplicationIdTagNames;
    try (TransactionContext tx = tagDAO.createTransactionContext()) {
      mapApplicationIdTagNames = applicationIds.stream()
          .collect(Collectors.toMap(
              applicationId -> applicationId,
              applicationId -> tagDAO.getByApplicationId(tx, applicationId).stream()
                  .map(Tag::getName)
                  .sorted()
                  .collect(Collectors.toList())));
    }
    return mapApplicationIdTagNames;
  }

  private Comparator<ApiLicenseLegalApplicationDashboardDTO> newDashboardComparator(LicenseLegalResultsOrder order) {
    Comparator<ApiLicenseLegalApplicationDashboardDTO> comparator;
    switch (order != null ? order : LicenseLegalResultsOrder.APPLICATION_NAME_ASC) {
      case APPLICATION_NAME_ASC:
        comparator = Comparator.comparing(dto -> dto.applicationName, String.CASE_INSENSITIVE_ORDER);
        break;
      case APPLICATION_NAME_DESC:
        comparator = Comparator.comparing(dto -> dto.applicationName, String.CASE_INSENSITIVE_ORDER);
        comparator = comparator.reversed();
        break;
      case LAST_SCAN_TIME_ASC:
        comparator = Comparator.comparing(dto -> dto.lastScanTime);
        break;
      case LAST_SCAN_TIME_DESC:
        comparator = Comparator.comparing(dto -> dto.lastScanTime);
        comparator = comparator.reversed();
        break;
      case TAG_NAMES_ASC:
        comparator =
            Comparator.comparing(dto -> StringUtils.join(dto.applicationTagNames, ','), String.CASE_INSENSITIVE_ORDER);
        break;
      case TAG_NAMES_DESC:
        comparator =
            Comparator.comparing(dto -> StringUtils.join(dto.applicationTagNames, ','), String.CASE_INSENSITIVE_ORDER);
        comparator = comparator.reversed();
        break;
      default:
        throw new IllegalArgumentException("Unknown ordering: " + order);
    }
    return comparator.thenComparing(dto -> dto.stageTypeName);
  }

  private void recalculateApplicationIdsAndStateTypeIds(
      List<Object[]> applicationIdsAndStageTypeIds,
      Set<String> applicationIdsToCheck,
      Set<String> stageTypeIdsToCheck)
  {
    applicationIdsToCheck.clear();
    stageTypeIdsToCheck.clear();

    for (Object[] array : applicationIdsAndStageTypeIds) {
      applicationIdsToCheck.add(array[0].toString());
      stageTypeIdsToCheck.add(array[1].toString());
    }
  }

  private void calculateComponentsReviewed(List<ApiLicenseLegalApplicationDashboardDTO> result) {
    MultiKeyMap<String, List<ApplicationComponentLicensesDTO>> applicationIdStageTypeIdComponentLicensesMap =
        new MultiKeyMap<>();
    Set<String> licenseIdsFound = new HashSet<>();
    Map<String, Map<ComponentIdentifier, Set<String>>> applicationIdAddressedObligationsMap = new HashMap<>();

    for (ApiLicenseLegalApplicationDashboardDTO dto : result) {

      List<ApplicationComponentLicensesDTO> applicationComponentLicensesDTOS = applicationComponentLicenseDAO
          .getApplicationComponentEffectiveLicenses(dto.applicationId, Sets.newHashSet(dto.stageTypeId));

      filterInnerSourceComponents(applicationComponentLicensesDTOS);

      applicationIdStageTypeIdComponentLicensesMap
          .put(dto.applicationId, dto.stageTypeId, applicationComponentLicensesDTOS);
      // Collect all licenses to make a single HDS call instead of one per component
      licenseIdsFound.addAll(applicationComponentLicensesDTOS.stream()
          .flatMap(component -> component.getLicenses().stream())
          .collect(Collectors.toSet()));

      applicationIdAddressedObligationsMap.computeIfAbsent(dto.applicationId,
          componentObligationDAO::getAddressedObligationsByOwnerIdWithHierarchy);
    }

    Map<String, Set<String>> licenseIdObligationNamesMap = licenseIdsFound.isEmpty() ? new HashMap<>(0) :
        apiLicenseLegalHdsService.getLicenseMetadata(licenseIdsFound).parallelStream()
            .collect(Collectors.toMap(
                LicenseMetadataDTO::getLicenseId,
                licenseMetadata -> licenseMetadata.getLicenseObligations().stream()
                    .map(LicenseObligationDTO::getName)
                    .collect(Collectors.toSet())));

    for (ApiLicenseLegalApplicationDashboardDTO dto : result) {
      List<ApplicationComponentLicensesDTO> componentLicenses =
          applicationIdStageTypeIdComponentLicensesMap.get(dto.applicationId, dto.stageTypeId);
      dto.componentsTotalCount = componentLicenses.size();
      dto.componentsReviewedCount = getComponentsReviewedCount(componentLicenses, licenseIdObligationNamesMap,
          applicationIdAddressedObligationsMap.get(dto.applicationId));
    }
  }

  private void filterInnerSourceComponents(
      final List<ApplicationComponentLicensesDTO> applicationComponentLicensesDTOS)
  {
    Set<PackageUrlIdentifier> componentPurls = applicationComponentLicensesDTOS.stream()
        .map(ac -> InnerSourceUtils.getVersionlessPackageUrl(ac.getComponentIdentifier()))
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());

    Set<String> innerSourcePackageUrls = innerSourceComponentDAO.getByPackageUrls(componentPurls).stream()
        .map(InnerSourceComponent::getPackageUrl)
        .collect(Collectors.toSet());

    applicationComponentLicensesDTOS.removeIf(c -> LegalComponentIdentifierUtil
        .isComponentAKnownInnerSource(innerSourcePackageUrls, c.getComponentIdentifier()));
  }

  private int getComponentsReviewedCount(
      List<ApplicationComponentLicensesDTO> componentLicenses,
      Map<String, Set<String>> licenseIdObligationNamesMap,
      Map<ComponentIdentifier, Set<String>> componentObligationsAddressed)
  {
    int componentsFullyReviewed = 0;

    for (ApplicationComponentLicensesDTO componentLicensesDTO : componentLicenses) {
      Set<String> allObligationNames = componentLicensesDTO.getLicenses().stream()
          .filter(licenseIdObligationNamesMap::containsKey)
          .flatMap(licenseId -> licenseIdObligationNamesMap.get(licenseId).stream())
          .collect(Collectors.toSet());

      long addressedObligationCount = componentObligationsAddressed
          .getOrDefault(componentLicensesDTO.getComponentIdentifier(), Collections.emptySet()).stream()
          .filter(allObligationNames::contains)
          .count();

      if (addressedObligationCount >= allObligationNames.size()) {
        componentsFullyReviewed++;
      }
    }

    return componentsFullyReviewed;
  }

  private Map<ApiReportComponentDTOV2, ComponentIdentifierLegalData> fetchApiReportComponentDTOV2ToLegalData(
      Owner owner,
      Collection<ApiReportComponentDTOV2> apiReportComponentDTOV2s)
  {

    String ownerId = owner.getId();

    Map<ApiReportComponentDTOV2, ComponentIdentifierLegalData> componentIdentifierLegalDataMap =
        new HashMap<>(apiReportComponentDTOV2s.size());

    try (final TransactionContext tx = componentCopyrightDAO.createTransactionContext()) {
      for (ApiReportComponentDTOV2 apiReportComponentDTOV2 : apiReportComponentDTOV2s) {
        if (apiReportComponentDTOV2.componentIdentifier == null) {
          continue;
        }
        ComponentIdentifier componentIdentifier = apiReportComponentDTOV2.componentIdentifier.toComponentIdentifier();
        ComponentIdentifierLegalData componentIdentifierLegalData =
            new ComponentIdentifierLegalData(
                LegalComponentIdentifierUtil.removeClassifierAndExtension(componentIdentifier));

        componentIdentifierLegalData.getCopyrightOverrides().addAll(
            copyrightOverrideDAO.getByOwnerIdAndComponentIdentifierWithHierarchy(tx, ownerId, componentIdentifier));

        componentIdentifierLegalData.setComponentCopyrights(
            componentCopyrightDAO.getByOwnerIdAndComponentIdentifier(tx, ownerId, componentIdentifier));

        componentIdentifierLegalData.setLicenseOverrides(legalFileOverrideDAO
            .getByOwnerIdAndComponentIdentifierAndTypeWithHierarchy(tx, ownerId, componentIdentifier,
                LegalFileType.LICENSE));

        componentIdentifierLegalData.setComponentLicense(
            componentIdentifierLegalData.getLicenseOverrides().isEmpty() ? null :
                componentLegalFileDAO
                    .getById(tx, componentIdentifierLegalData.getLicenseOverrides().get(0).getComponentLegalFileId()));

        componentIdentifierLegalData.setNoticeOverrides(legalFileOverrideDAO
            .getByOwnerIdAndComponentIdentifierAndTypeWithHierarchy(tx, ownerId, componentIdentifier,
                LegalFileType.NOTICE));

        componentIdentifierLegalData.setComponentNotice(
            componentIdentifierLegalData.getNoticeOverrides().isEmpty() ? null :
                componentLegalFileDAO
                    .getById(tx, componentIdentifierLegalData.getNoticeOverrides().get(0).getComponentLegalFileId()));

        componentIdentifierLegalData.setObligations(componentObligationDAO
            .getByOwnerIdAndComponentIdentifierWithHierarchy(tx, ownerId, componentIdentifier));

        componentIdentifierLegalData.setAttributions(componentObligationAttributionDAO
            .getByOwnerIdAndComponentIdentifierWithHierarchy(tx, ownerId, componentIdentifier));

        componentIdentifierLegalDataMap
            .put(apiReportComponentDTOV2, componentIdentifierLegalData);

        componentIdentifierLegalData.setHighestEffectiveLicenseThreatGroup(
            getHighestLicenseThreatGroupWithHierarchy(tx, owner.getId(),
                apiReportComponentDTOV2.licenseData.effectiveLicenses
                    .stream()
                    .map(effectiveLicense -> effectiveLicense.licenseId)
                    .collect(Collectors.toCollection(LinkedHashSet::new)))
        );
      }
    }
    return componentIdentifierLegalDataMap;
  }
}
