/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service.legal;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import static com.sonatype.insight.brain.api.v2.service.legal.LicenseLegalComparators.LEGAL_SOURCE_LINK_COMPARATOR;
import static com.sonatype.insight.brain.api.v2.service.legal.LicenseLegalComparators.newApplicationDashboardComparator;
import static com.sonatype.insight.brain.api.v2.service.legal.LicenseLegalComparators.newComponentDashboardComparator;
import static com.sonatype.insight.brain.model.license.License.NOT_DECLARED_ID;
import static com.sonatype.insight.brain.model.license.License.NOT_SUPPORTED_ID;
import static com.sonatype.insight.brain.model.license.License.NO_SOURCES_ID;
import static com.sonatype.insight.brain.model.license.License.NO_SOURCE_LICENSE_ID;
import static com.sonatype.insight.brain.model.license.License.UNKNOWN_ID;
import static com.sonatype.insight.brain.model.license.License.UNSPECIFIED_ID;
import static java.util.stream.Collectors.groupingBy;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.experimental.legal.ApiLicenseLegalHdsService;
import com.sonatype.insight.brain.api.experimental.legal.ComponentLegalService;
import com.sonatype.insight.brain.api.experimental.legal.LegalComponentIdentifierUtil;
import com.sonatype.insight.brain.api.v2.dto.ApiLicenseDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiLicenseDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiLicenseDataDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiLicenseThreatDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportRawDataDTOV2;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalApplicationDashboardDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalApplicationDashboardResultDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalApplicationReportDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalComponentDashboardDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalComponentDashboardResultDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalComponentReportDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalStageScanDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApplicationLicenseUsageTelemetry;
import com.sonatype.insight.brain.api.v2.dto.legal.LegalSourceLinkDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.LicenseLegalFilterDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.LicenseLegalResultsOrder;
import com.sonatype.insight.brain.api.v2.dto.legal.LicenseLegalReviewStatus;
import com.sonatype.insight.brain.api.v2.dto.legal.LicenseObligationReviewStatus;
import com.sonatype.insight.brain.api.v2.service.ApiLicenseDataAdapter;
import com.sonatype.insight.brain.api.v2.service.ApiReportDataServiceV2;
import com.sonatype.insight.brain.dataaccess.AggregateFileDAO;
import com.sonatype.insight.brain.dataaccess.ApplicationComponentDAO;
import com.sonatype.insight.brain.dataaccess.ApplicationComponentLicenseDAO;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.component.HashComponentIdentifierDAO;
import com.sonatype.insight.brain.dataaccess.innersource.InnerSourceApplicationDAO;
import com.sonatype.insight.brain.dataaccess.legal.ComponentCopyrightDAO;
import com.sonatype.insight.brain.dataaccess.legal.ComponentObligationAttributionDAO;
import com.sonatype.insight.brain.dataaccess.legal.ComponentObligationDAO;
import com.sonatype.insight.brain.dataaccess.legal.CopyrightOverrideDAO;

import com.sonatype.insight.brain.dataaccess.legal.LegalFileOverrideDAO;
import com.sonatype.insight.brain.dataaccess.legal.SourceLinkOverrideDAO;
import com.sonatype.insight.brain.model.legal.SourceLinkOverride;
import com.sonatype.insight.brain.model.legal.ComponentCopyright;
import com.sonatype.insight.brain.model.legal.ComponentObligationAttribution;
import com.sonatype.insight.brain.model.legal.CopyrightOverride;
import com.sonatype.insight.brain.model.legal.LegalFileType;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.tag.TagDAO;
import com.sonatype.insight.brain.hds.ComponentInfoService;
import com.sonatype.insight.brain.model.AggregateFile;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ApplicationComponent;
import com.sonatype.insight.brain.model.ApplicationComponentLicensesDTO;
import com.sonatype.insight.brain.model.HashHelper;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.HashComponentIdentifier;
import com.sonatype.insight.brain.model.innersource.InnerSourceApplication;
import com.sonatype.insight.brain.model.legal.ComponentLegalPartStatus;
import com.sonatype.insight.brain.model.legal.ComponentObligation;
import com.sonatype.insight.brain.model.license.License;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.license.MultiLicense;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.organization.ApplicationService;
import com.sonatype.insight.brain.policy.StageTypeService;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.report.InnerSourceUtils;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.brain.security.AuthzFilter;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.telemetry.TelemetryUtils;
import com.sonatype.insight.brain.tenancy.TenantAwareSupplier;
import com.sonatype.insight.brain.utils.ExecutorThreadPools;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.license.dto.model.AnameAggregateFileGroup;
import com.sonatype.insight.license.dto.model.ComponentLegalCommentDTO;
import com.sonatype.insight.license.dto.model.ComponentLegalFileDTO;
import com.sonatype.insight.license.dto.model.LicenseMetadataDTO;
import com.sonatype.insight.license.dto.model.LicenseObligationDTO;
import com.sonatype.insight.license.dto.model.LicenseThreatGroupDTO;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.map.MultiKeyMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sonatype.insight.brain.lifecycle.Managed;

/**
 * Provides legal information for application components.
 *
 * @since 1.101
 */
@Named
public class ApiLicenseLegalService
    implements Managed
{
  private static final Logger log = LoggerFactory.getLogger(ApiLicenseLegalService.class);

  private static final String PROCESSING_LICENSE_METADATA_REQUEST = "Processing license metadata request for {}";

  private final MultiLicenseDAO multiLicenseDAO;

  private final ApiLicenseLegalHdsService apiLicenseLegalHdsService;

  private final ApplicationDAO applicationDAO;

  private final PolicyEvaluationDAO policyEvaluationDAO;

  private final ApiReportDataServiceV2 apiReportDataServiceV2;

  private final LegalReportBuilder legalReportBuilder;

  private final TelemetrySender telemetrySender;

  private final ApplicationComponentDAO applicationComponentDAO;

  private final HashComponentIdentifierDAO hashComponentIdentifierDAO;

  private final ComponentObligationDAO componentObligationDAO;

  private final ComponentInfoService componentInfoService;

  private final ApiLicenseDataAdapter apiLicenseDataAdapter;

  private final ProductLicense productLicense;

  private final ApplicationService applicationService;

  private final TagDAO tagDAO;

  private final ApplicationComponentLicenseDAO applicationComponentLicenseDAO;

  private final AggregateFileDAO aggregateFileDAO;

  private final LicenseThreatGroupDAO licenseThreatGroupDAO;

  private final InnerSourceApplicationDAO innerSourceApplicationDAO;

  private final LegalDashboardsService legalDashboardService;

  private final ComponentLegalService componentLegalService;

  private final CopyrightOverrideDAO copyrightOverrideDAO;

  private final ComponentCopyrightDAO componentCopyrightDAO;

  private final LegalFileOverrideDAO legalFileOverrideDAO;

  private final SourceLinkOverrideDAO sourceLinkOverrideDAO;

  private final ComponentObligationAttributionDAO componentObligationAttributionDAO;

  private final ForkJoinPool attributionReportForkJoinPool;

  private final IdUtils idUtils;

  private final TelemetryUtils telemetryUtils;

  private final StageTypeService stageTypeService;

  private static final Set<String> SONATYPE_SPECIAL_LICENSES = new HashSet<>(Arrays.asList(
      UNSPECIFIED_ID,
      UNKNOWN_ID,
      NOT_DECLARED_ID,
      NO_SOURCES_ID,
      NO_SOURCE_LICENSE_ID,
      NOT_SUPPORTED_ID,
      "COMMERCIAL",
      "Generic-Copyleft-Clause",
      "Generic-Liberal-Clause",
      "Generic-Open-Source-Clause",
      "Generic-Weak-Copyleft-Clause",
      "See-License-Clause"));

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
      ComponentInfoService componentInfoService,
      ApiLicenseDataAdapter apiLicenseDataAdapter,
      ProductLicense productLicense,
      ApplicationService applicationService,
      TagDAO tagDAO,
      ApplicationComponentLicenseDAO applicationComponentLicenseDAO,
      ComponentObligationDAO componentObligationDAO,
      AggregateFileDAO aggregateFileDAO,
      LicenseThreatGroupDAO licenseThreatGroupDAO,
      InnerSourceApplicationDAO innerSourceApplicationDAO,
      LegalDashboardsService legalDashboardService,
      ComponentLegalService componentLegalService,
      CopyrightOverrideDAO copyrightOverrideDAO,
      ComponentCopyrightDAO componentCopyrightDAO,
      LegalFileOverrideDAO legalFileOverrideDAO,
      SourceLinkOverrideDAO sourceLinkOverrideDAO,
      ComponentObligationAttributionDAO componentObligationAttributionDAO,
      IdUtils idUtils,
      TelemetryUtils telemetryUtils,
      StageTypeService stageTypeService,
      ExecutorThreadPools executorThreadPools)
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
    this.componentInfoService = componentInfoService;
    this.idUtils = idUtils;
    this.telemetryUtils = telemetryUtils;
    this.componentInfoService.setToolName("ci");
    this.apiLicenseDataAdapter = apiLicenseDataAdapter;
    this.productLicense = productLicense;
    this.applicationService = applicationService;
    this.tagDAO = tagDAO;
    this.applicationComponentLicenseDAO = applicationComponentLicenseDAO;
    this.componentObligationDAO = componentObligationDAO;
    this.aggregateFileDAO = aggregateFileDAO;
    this.licenseThreatGroupDAO = licenseThreatGroupDAO;
    this.innerSourceApplicationDAO = innerSourceApplicationDAO;
    this.legalDashboardService = legalDashboardService;
    this.componentLegalService = componentLegalService;
    this.copyrightOverrideDAO = copyrightOverrideDAO;
    this.componentCopyrightDAO = componentCopyrightDAO;
    this.legalFileOverrideDAO = legalFileOverrideDAO;
    this.sourceLinkOverrideDAO = sourceLinkOverrideDAO;
    this.componentObligationAttributionDAO = componentObligationAttributionDAO;
    this.stageTypeService = stageTypeService;

    attributionReportForkJoinPool =
        executorThreadPools.createThreadPool(1, 5, 5, "insight.threads.attribution.report");
  }

  @Override
  public void stop() throws Exception {
    attributionReportForkJoinPool.shutdown();
  }

  public ApiLicenseLegalApplicationDashboardResultDTO getLicenseLegalApplicationsDashboard(
      Set<String> organizationIds,
      Set<String> applicationIds,
      Set<String> tagIds,
      Set<String> stageTypeIds,
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
        getApplicationsByIdsAndOrganizationIdsAndTagIds(organizationIds, applicationIds, tagIds)
            .stream()
            .collect(Collectors.toMap(Application::getId, Function.identity()));
    Set<String> applicationIdsToCheck = new HashSet<>(mapApplicationIds.keySet());

    Set<String> stageTypeIdsToCheck =
        isEmpty(stageTypeIds)
            ? stageTypeService.getLicensedStageTypes(StageTypeService.LIFECYCLE_CONTEXT)
                .stream()
                .map(StageType::getId)
                .collect(Collectors.toSet())
            : stageTypeIds;

    if (isNotEmpty(applicationIdsToCheck) && isNotEmpty(reviewStatus) && !reviewStatus
        .containsAll(Sets.newHashSet(LicenseLegalReviewStatus.OPEN, LicenseLegalReviewStatus.NOT_STARTED)))
    {
      List<Object[]> applicationIdsAndStageTypeIds =
          applicationComponentDAO.getApplicationIdsAndStageTypeIdsByReviewStatus(applicationIdsToCheck,
              stageTypeIdsToCheck, reviewStatus.contains(LicenseLegalReviewStatus.OPEN));

      recalculateApplicationIdsAndStateTypeIds(applicationIdsAndStageTypeIds, applicationIdsToCheck,
          stageTypeIdsToCheck);
    }

    if (isEmpty(applicationIdsToCheck) || isEmpty(stageTypeIdsToCheck)) {
      return new ApiLicenseLegalApplicationDashboardResultDTO();
    }

    List<PolicyEvaluation> policyEvaluations =
        policyEvaluationDAO.getLastByApplicationIdsAndStageIds(applicationIdsToCheck, stageTypeIdsToCheck);

    ApiLicenseLegalApplicationDashboardResultDTO resultDto = new ApiLicenseLegalApplicationDashboardResultDTO();
    resultDto.totalResultsCount = policyEvaluations.size();

    int startIndex = (page - 1) * pageSize;
    if (startIndex >= policyEvaluations.size()) {
      return resultDto;
    }

    List<ApiLicenseLegalApplicationDashboardDTO> results = new ArrayList<>(policyEvaluations.size());
    for (PolicyEvaluation policyEvaluation : policyEvaluations) {
      Application application = mapApplicationIds.get(policyEvaluation.getApplicationId());

      ApiLicenseLegalApplicationDashboardDTO dto = new ApiLicenseLegalApplicationDashboardDTO();
      dto.applicationId = application.getId();
      dto.applicationName = application.getName();
      dto.applicationPublicId = application.getPublicId();
      dto.lastScanTime = policyEvaluation.getTime().getTime();
      dto.stageTypeId = policyEvaluation.getStageTypeId();
      dto.stageTypeName = StageTypes.getById(policyEvaluation.getStageTypeId()).getName();

      results.add(dto);
    }

    results = orderAndPage(results, order, page, pageSize, applicationIdsToCheck);

    calculateComponentsReviewed(results);

    resultDto.results = results;

    return resultDto;
  }

  private List<ApiLicenseLegalApplicationDashboardDTO> orderAndPage(
      final List<ApiLicenseLegalApplicationDashboardDTO> results,
      final LicenseLegalResultsOrder order,
      final int page,
      final int pageSize,
      final Set<String> allApplicationIdsToCheck)
  {
    int startIndex = (page - 1) * pageSize;

    if (LicenseLegalResultsOrder.TAG_NAMES_ASC == order || LicenseLegalResultsOrder.TAG_NAMES_DESC == order) {
      // if the order is by tag name, all applications must be considered
      // if there are many applications, retrieving the tag names takes a long time.
      fillTagNames(results, allApplicationIdsToCheck);

      results.sort(newApplicationDashboardComparator(order));
      return results.subList(startIndex, Math.min(page * pageSize, results.size()));
    }

    // if the order is not by tag name, the sorting and pagination is applied first,
    // then the tag names are retrieved using a small number of applications.
    results.sort(newApplicationDashboardComparator(order));

    List<ApiLicenseLegalApplicationDashboardDTO> paginatedResults =
        results.subList(startIndex, Math.min(page * pageSize, results.size()));

    Set<String> smallApplicationIdsToCheck = paginatedResults.stream()
        .map(r -> r.applicationId)
        .collect(Collectors.toSet());

    fillTagNames(paginatedResults, smallApplicationIdsToCheck);

    return paginatedResults;
  }

  private void fillTagNames(
      final List<ApiLicenseLegalApplicationDashboardDTO> results,
      final Set<String> applicationIdsToCheck)
  {
    Map<String, List<String>> mapApplicationIdTagNames = getTagNamesByApplicationIds(applicationIdsToCheck);
    for (ApiLicenseLegalApplicationDashboardDTO dto : results) {
      dto.applicationTagNames.addAll(mapApplicationIdTagNames.get(dto.applicationId));
    }
  }

  public ApiLicenseLegalComponentDashboardResultDTO getLicenseLegalComponentsDashboard(LicenseLegalFilterDTO filter) {
    checkLicense();

    if (filter.page <= 0 || filter.pageSize <= 0) {
      throw new BadRequestException("Request must include page and pageSize values greater than zero.");
    }

    Map<String, Application> mapApplicationIds =
        getApplicationsByIdsAndOrganizationIdsAndTagIds(filter.organizationIds, filter.applicationIds,
            filter.tagIds)
                .stream()
                .collect(Collectors.toMap(Application::getId, Function.identity()));
    Set<String> applicationIdsToCheck = new HashSet<>(mapApplicationIds.keySet());

    Set<String> stageTypeIdsToCheck = isEmpty(filter.stageTypeIds)
        ? stageTypeService.getLicensedStageTypes(StageTypeService.LIFECYCLE_CONTEXT)
            .stream()
            .map(StageType::getId)
            .collect(Collectors.toSet())
        : filter.stageTypeIds;

    if (isEmpty(applicationIdsToCheck) || isEmpty(stageTypeIdsToCheck)) {
      return new ApiLicenseLegalComponentDashboardResultDTO();
    }

    List<ApplicationComponentLicensesDTO> applicationComponentLicenses =
        applicationComponentLicenseDAO.getApplicationComponentEffectiveLicensesWithOverridesAtRootOrganization(
            applicationIdsToCheck, stageTypeIdsToCheck);

    Map<String, ApiLicenseLegalComponentDashboardDTO> componentDtoByHash = new HashMap<>();
    Map<String, Set<String>> multiLicenseIdsByHash = new HashMap<>();
    Map<String, Set<String>> singleLicenseIdsByHash = new HashMap<>();

    Set<String> multiLicenseIdsFound = legalDashboardService.getLicenseIds(applicationComponentLicenses);
    Map<String, String> multiLicenseNamesById = getLicenseNames(multiLicenseIdsFound);
    Map<String, Set<String>> multiLicenseIdToSingleLicenseIds = getMultiLicensesFromLicensesSet(multiLicenseIdsFound);
    Set<String> singleLicenseIdsFound = multiLicenseIdToSingleLicenseIds.values()
        .stream()
        .flatMap(Collection::stream)
        .collect(Collectors.toSet());
    Map<String, Set<String>> obligationNamesByLicenseId =
        legalDashboardService.getLicenseObligationsFromHds(singleLicenseIdsFound);

    for (ApplicationComponentLicensesDTO applicationComponent : applicationComponentLicenses) {
      if (applicationComponent.getComponentIdentifier() == null || isEmpty(applicationComponent.getLicenses())) {
        continue;
      }

      ApiLicenseLegalComponentDashboardDTO dto = componentDtoByHash.getOrDefault(applicationComponent.getHash(),
          new ApiLicenseLegalComponentDashboardDTO(applicationComponent));

      if (StringUtils.isNotBlank(filter.componentName) && !dto.displayName.contains(filter.componentName)) {
        continue;
      }

      Set<String> multiLicenseIds = multiLicenseIdsByHash.getOrDefault(applicationComponent.getHash(), new HashSet<>());
      multiLicenseIds.addAll(applicationComponent.getLicenses());

      Set<String> singleLicenseIds =
          singleLicenseIdsByHash.getOrDefault(applicationComponent.getHash(), new HashSet<>());
      singleLicenseIds.addAll(multiLicenseIds.stream()
          .map(multiLicenseIdToSingleLicenseIds::get)
          .flatMap(Set::stream)
          .collect(Collectors.toSet()));

      dto.applicationOccurrences += 1;
      componentDtoByHash.put(applicationComponent.getHash(), dto);
      multiLicenseIdsByHash.put(applicationComponent.getHash(), multiLicenseIds);
      singleLicenseIdsByHash.put(applicationComponent.getHash(), singleLicenseIds);
    }

    ApiLicenseLegalComponentDashboardResultDTO resultDto = new ApiLicenseLegalComponentDashboardResultDTO();
    boolean needsReviewStatusFilter = CollectionUtils.size(filter.reviewStatus) == 1;

    try (TransactionContext tx = componentObligationDAO.createTransactionContext()) {
      List<ApiLicenseLegalComponentDashboardDTO> components = componentDtoByHash.values()
          .stream()
          .map(dto -> {
            if (needsReviewStatusFilter) {
              fillReviewProgress(tx, dto, singleLicenseIdsByHash, obligationNamesByLicenseId);
            }
            return dto;
          })
          .filter(dto -> !needsReviewStatusFilter || filterByReviewStatus(dto, filter))
          .map(dto -> fillLicenses(dto, multiLicenseIdsByHash.get(dto.hash),
              multiLicenseNamesById))
          .sorted(newComponentDashboardComparator(filter.order))
          .collect(Collectors.toList());

      resultDto.totalResultsCount = components.size();

      int startIndex = (filter.page - 1) * filter.pageSize;
      if (startIndex >= resultDto.totalResultsCount) {
        return resultDto;
      }

      resultDto.results = components.subList(startIndex, Math.min(filter.page * filter.pageSize, components.size()));
    }

    try (TransactionContext tx = licenseThreatGroupDAO.createTransactionContext()) {
      resultDto.results.forEach(dto -> {
        if (!needsReviewStatusFilter) {
          fillReviewProgress(tx, dto, singleLicenseIdsByHash, obligationNamesByLicenseId);
        }

        fillLicenseThreatGroups(tx, dto, multiLicenseIdsFound, multiLicenseIdToSingleLicenseIds);
      });
    }

    return resultDto;
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
      @AuthzContext(Key.OWNER) Owner application,
      String stageId,
      boolean includeInnerSource,
      boolean includeSonatypeSpecialLicenses)
  {
    checkLicense();
    log.info(PROCESSING_LICENSE_METADATA_REQUEST, application.getId());
    ApiReportRawDataDTOV2 latestRawReport = getLastRawApplicationReportByStageId(application.getPublicId(), stageId)
        .orElseThrow(() -> new NotFoundException(
            "Report for application " + application.getId() + " at stage " + stageId + " not found."));
    return getApplicationReportFromReportRawData(application, latestRawReport, includeInnerSource,
        includeSonatypeSpecialLicenses);
  }

  @Authorize(permission = Permission.LEGAL_REVIEWER)
  public ApiReportRawDataDTOV2 getApiReportRawDataForMultiApplicationReport(
      @AuthzContext(Key.OWNER) Owner application,
      String stageId)
  {
    log.info(PROCESSING_LICENSE_METADATA_REQUEST, application.getId());
    return getLastRawApplicationReportByStageId(application.getPublicId(), stageId)
        .orElseThrow(() -> new NotFoundException(
            "Report for application " + application.getId() + " at stage " + stageId + " not found."));
  }

  public Set<Optional<ApiLicenseLegalApplicationReportDTO>> getLicenseLegalMultiApplicationReport(
      List<Owner> applications,
      List<String> stageIds,
      boolean includeInnerSource,
      boolean includeSonatypeSpecialLicenses)
  {
    checkLicense();
    if (applications.size() != stageIds.size()) {
      throw new BadRequestException("Applications and stages sizes do not match");
    }

    List<ApiReportRawDataDTOV2> latestRawReports = new ArrayList<>();
    List<Owner> matchApplication = new ArrayList<>();
    ApiReportRawDataDTOV2 currentReport;
    for (int i = 0; i < applications.size(); ++i) {
      try {
        currentReport =
            getApiReportRawDataForMultiApplicationReport(applications.get(i), stageIds.get(i));
      }
      catch (NotFoundException e) {
        continue;
      }
      latestRawReports.add(currentReport);
      matchApplication.add(applications.get(i));
    }
    return applications.isEmpty()
        ? Collections.singleton(Optional.empty())
        : getMultiApplicationReportFromReportRawData(matchApplication, latestRawReports, includeInnerSource,
            includeSonatypeSpecialLicenses);
  }

  @Authorize(permission = Permission.LEGAL_REVIEWER)
  public ApiLicenseLegalApplicationReportDTO getLicenseLegalApplicationReport(
      @AuthzContext(Key.OWNER) Owner application)
  {
    checkLicense();

    log.info(PROCESSING_LICENSE_METADATA_REQUEST, application.getId());
    ApiReportRawDataDTOV2 latestRawReport = getLastRawApplicationReport(application.getPublicId())
        .orElseThrow(() -> new NotFoundException("Report for application " + application.getId() + " not found."));

    return getApplicationReportFromReportRawData(application, latestRawReport, false,
        false);
  }

  private Set<Optional<ApiLicenseLegalApplicationReportDTO>> getMultiApplicationReportFromReportRawData(
      final List<Owner> applications,
      final List<ApiReportRawDataDTOV2> latestRawReports,
      boolean includeInnerSource,
      boolean includeSonatypeSpecialLicenses)
  {
    if (applications.size() != latestRawReports.size()) {
      throw new BadRequestException("Size of parameters are not the same to generate multi application report");
    }

    Map<String, List<ApiReportRawDataDTOV2>> reportsByApplicationId = new HashMap<>();
    for (int i = 0; i < applications.size(); i++) {
      String applicationId = applications.get(i).getId();
      List<ApiReportRawDataDTOV2> reports = Lists.newArrayList(latestRawReports.get(i));

      reportsByApplicationId.merge(applicationId, reports, (existing, newValue) -> {
        existing.addAll(newValue);
        return existing;
      });
    }

    if (!includeSonatypeSpecialLicenses) {
      for (ApiReportRawDataDTOV2 apiReportRawDataDTOV2 : latestRawReports) {
        filterSonatypeSpecialLicensesComponents(apiReportRawDataDTOV2);
      }
    }

    if (!includeInnerSource) {
      for (ApiReportRawDataDTOV2 apiReportRawDataDTOV2 : latestRawReports) {
        filterInnerSourceComponents(apiReportRawDataDTOV2);
      }
    }

    Set<ApiLicenseDTO> allMultiLicenses = latestRawReports.stream()
        .map(this::getReportMultiLicenses)
        .flatMap(m -> m.entrySet().stream())
        .flatMap(e -> e.getValue().stream())
        .collect(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(dto -> dto.licenseId))));

    Map<ApiLicenseDTO, Set<License>> multiLicenseToSingleLicense =
        buildMultiLicenseToSingleLicenseMap(allMultiLicenses);

    for (int i = 0; i < applications.size(); ++i) {
      Owner application = applications.get(i);
      sendApplicationTelemetryData(application.getId(), latestRawReports.get(i), allMultiLicenses);
    }

    CompletableFuture<Map<String, LicenseMetadataDTO>> licenseMetadataById =
        CompletableFuture.supplyAsync(new TenantAwareSupplier<>(() -> {
          if (multiLicenseToSingleLicense.isEmpty()) {
            return Collections.emptyMap();
          }

          List<LicenseMetadataDTO> licenseMetadataDTOS =
              apiLicenseLegalHdsService.getLicenseMetadata(multiLicenseToSingleLicense.values()
                  .stream()
                  .flatMap(Collection::stream)
                  .map(License::getId)
                  .collect(Collectors.toCollection(LinkedHashSet::new)));

          Map<String, LicenseMetadataDTO> metadataByLicenseId = licenseMetadataDTOS.isEmpty()
              ? Collections.emptyMap()
              : applications.stream()
                  .map(app -> getLicenseMetadata(licenseMetadataDTOS, app.getId()))
                  .flatMap(m -> m.entrySet().stream())
                  .collect(Collectors.toMap(Entry::getKey, Entry::getValue, (prev, next) -> next));
          return metadataByLicenseId;
        }), attributionReportForkJoinPool);

    CompletableFuture<Map<ApiReportComponentDTOV2, ComponentIdentifierLegalData>> componentIdentifierToLegalData =
        CompletableFuture.supplyAsync(new TenantAwareSupplier<>(
            (Supplier<Map<ApiReportComponentDTOV2, ComponentIdentifierLegalData>>) () -> reportsByApplicationId.keySet()
                .stream()
                .map(appId -> {
                  Collection<ApiReportComponentDTOV2> components = reportsByApplicationId.get(appId)
                      .stream()
                      .flatMap(rawReport -> rawReport.components.stream())
                      .collect(Collectors.toSet());
                  return fetchApiReportComponentDTOV2ToLegalData(appId, components, multiLicenseToSingleLicense);
                })
                .flatMap(m -> m.entrySet().stream())
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue, (prev, next) -> next))),
            attributionReportForkJoinPool);

    CompletableFuture<Map<ComponentIdentifier, Set<ComponentLegalCommentDTO>>> componentLegalCommentsByComponentIdentifier =
        CompletableFuture.supplyAsync(new TenantAwareSupplier<>(
            () -> getComponentLegalCommentsByComponentIdentifier(latestRawReports)), attributionReportForkJoinPool);

    CompletableFuture<Map<ComponentIdentifier, Set<ComponentLegalFileDTO>>> componentLegalFilesByComponentIdentifier =
        CompletableFuture.supplyAsync(new TenantAwareSupplier<>(
            () -> getComponentLegalFilesByComponentIdentifier(latestRawReports)), attributionReportForkJoinPool);

    CompletableFuture<Map<ComponentIdentifier, Set<LegalSourceLinkDTO>>> sourceLinksByComponentIdentifier =
        CompletableFuture.supplyAsync(new TenantAwareSupplier<>(
            (Supplier<Map<ComponentIdentifier, Set<LegalSourceLinkDTO>>>) () -> {

              Set<ComponentIdentifier> componentIdentifiersFromRawReports = getComponentIdentifiers(latestRawReports);
              Map<ComponentIdentifier, Set<LegalSourceLinkDTO>> sourceLinksByComponent = apiLicenseLegalHdsService
                  .getSourceLinksFromComponentIdentifierSet(componentIdentifiersFromRawReports);

              return reportsByApplicationId.keySet().stream().map(appId -> {
                List<ApiReportRawDataDTOV2> reportsForApp = reportsByApplicationId.get(appId);
                return getSourceLinksByComponentIdentifier(appId, getComponentIdentifiers(reportsForApp),
                    sourceLinksByComponent);
              })
                  .flatMap(m -> m.entrySet().stream())
                  .collect(Collectors.toMap(Entry::getKey, Entry::getValue,
                      (prev, next) -> Stream.concat(prev.stream(), next.stream()).collect(Collectors.toSet())));
            }),
            attributionReportForkJoinPool);

    CompletableFuture.allOf(componentLegalCommentsByComponentIdentifier, componentLegalFilesByComponentIdentifier,
        sourceLinksByComponentIdentifier, licenseMetadataById, componentIdentifierToLegalData).join();

    Set<Optional<ApiLicenseLegalApplicationReportDTO>> result = new HashSet<>();
    for (int i = 0; i < applications.size(); ++i) {
      log.info("Building license metadata report for {}.", applications.get(i).getName());
      result.add(Optional.of(legalReportBuilder.getLicenseLegalApplicationReport(
          latestRawReports.get(i),
          componentIdentifierToLegalData.join(),
          componentLegalCommentsByComponentIdentifier.join(),
          componentLegalFilesByComponentIdentifier.join(),
          multiLicenseToSingleLicense,
          licenseMetadataById.join(),
          sourceLinksByComponentIdentifier.join())));
    }
    return result;
  }

  private ApiLicenseLegalApplicationReportDTO getApplicationReportFromReportRawData(
      final Owner application,
      final ApiReportRawDataDTOV2 latestRawReport,
      boolean includeInnerSource,
      boolean includeSonatypeSpecialLicenses)
  {
    NotFoundException ex = new NotFoundException("Report for application " + application.getId() + " not found.");
    Set<Optional<ApiLicenseLegalApplicationReportDTO>> result = getMultiApplicationReportFromReportRawData(
        Collections.singletonList(application), Collections.singletonList(latestRawReport), includeInnerSource,
        includeSonatypeSpecialLicenses);
    return result.stream().findAny().orElseThrow(() -> ex).orElseThrow(() -> ex);
  }

  private void filterInnerSourceComponents(final ApiReportRawDataDTOV2 latestRawReport) {
    Set<PackageUrlIdentifier> componentPurls = latestRawReport.components.stream()
        .filter(c -> Objects.nonNull(c.componentIdentifier))
        .map(c -> InnerSourceUtils.getVersionlessPackageUrl(c.componentIdentifier.toComponentIdentifier()))
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());

    Set<String> innerSourcePackageUrls = innerSourceApplicationDAO.getByPackageUrls(componentPurls)
        .stream()
        .map(InnerSourceApplication::getPackageUrl)
        .collect(Collectors.toSet());

    latestRawReport.components.removeIf(c -> Objects.nonNull(c.componentIdentifier) &&
        LegalComponentIdentifierUtil
            .isComponentAKnownInnerSource(innerSourcePackageUrls, c.componentIdentifier.toComponentIdentifier()));
  }

  private void filterSonatypeSpecialLicensesComponents(final ApiReportRawDataDTOV2 latestRawReport) {
    Iterator<ApiReportComponentDTOV2> componentIterator = latestRawReport.components.iterator();
    while (componentIterator.hasNext()) {
      ApiReportComponentDTOV2 component = componentIterator.next();
      if (Objects.nonNull(component.componentIdentifier) &&
          Objects.nonNull(component.licenseData) &&
          Objects.nonNull(component.licenseData.effectiveLicenses))
      {
        component.licenseData.effectiveLicenses.removeIf(apiLicenseDTO -> SONATYPE_SPECIAL_LICENSES
            .contains(apiLicenseDTO.licenseId));
      }
    }
  }

  /**
   * <p>
   * Given an {@link OwnerType} and ownerId specifying an {@link Owner}, with either a {@link ComponentIdentifier},
   * package url, or component hash, this generates a {@link ApiLicenseLegalComponentReportDTO} containing the following
   * component information:
   * </p>
   * <ul>
   * <li>Licenses</li>
   * <li>License obligations</li>
   * <li>Obligation status</li>
   * <li>Obligation attributions</li>
   * <li>Copyright statements</li>
   * <li>Notice texts</li>
   * <li>License texts</li>
   * </ul>
   * <p>
   * The {@link Owner} and its ancestors as well as the {@link ComponentIdentifier} determine
   * </p>
   * <ul>
   * <li>Overrides for licenses (which determine license obligations), copyrights, notice texts, and license
   * texts.</li>
   * <li>License obligation data (i.e. statuses, comments, and attributions).</li>
   * </ul>
   * <p>
   * preference is given to overrides and data at lower scopes (starting at the given scope).
   * </p>
   * Note: specifying more than one of component identifier, package url, or hash, or not specifying any will cause a
   * {@link BadRequestException} to be thrown.
   *
   * @param ownerType the {@link OwnerType} of the {@link Owner}, required.
   * @param ownerId the id of the {@link Owner}, required.
   * @param componentIdentifier a {@link ComponentIdentifier}, optional.
   * @param packageUrl a package url string, optional.
   * @param hash a component hash, optional.
   * @param identificationSource the component identification source, optional.
   * @param scanId the scan id for the report where the component was identified, only used with a third
   *          party identification source, optional.
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
      String identificationSource,
      String scanId) throws IOException
  {
    checkLicense();

    log.debug(
        "Getting LicenseLegalComponentReport for owner type={} with ID={} for"
            + " componentIdentifier={}, packageUrl={}, hash={}.",
        ownerType, ownerId, componentIdentifier, packageUrl, hash);

    Owner owner = idUtils.getOwnerNotNull(ownerType, ownerId);
    ComponentIdentifier compIdentifier = getComponentIdentifier(componentIdentifier, packageUrl, hash);
    // We get the component by coordinates from HDS
    // A passed in aggregate component synthetic hash (i.e. hash of its coordinates)
    // will be different to the HDS component hash i.e. hash may not equal component.getHash()
    Component component = componentInfoService.augmentComponentDetails(owner, componentInfoService
        .getUnaugmentedComponentDetails(owner, compIdentifier, null, identificationSource, scanId));
    if (component.getHash() == null && hash != null) {
      component.setHash(hash);
    }
    ApiLicenseDataDTOV2 licenseData = apiLicenseDataAdapter.convertToDTOV2(component);

    Set<ApiLicenseDTO> allMultiLicenses = getAllLicenses(licenseData);

    Map<ApiLicenseDTO, Set<License>> multiLicenseToSingleLicense =
        buildMultiLicenseToSingleLicenseMap(allMultiLicenses);

    Set<License> allSingleLicenses = multiLicenseToSingleLicense.values()
        .stream()
        .flatMap(Collection::stream)
        .collect(Collectors.toSet());

    Map<String, LicenseMetadataDTO> licenseMetadataById =
        allMultiLicenses.isEmpty() ? Collections.emptyMap() : getLicenseMetadata(allSingleLicenses, owner.getId());

    Set<ComponentLegalCommentDTO> componentLegalComments =
        getComponentLegalComments(compIdentifier, component.getHash());
    Set<ComponentLegalFileDTO> componentLegalFiles =
        apiLicenseLegalHdsService.getComponentLegalFiles(Collections.singleton(compIdentifier));

    ApiReportComponentDTOV2 apiReportComponentDTOV2 = LegalComponentIdentifierUtil.toApiReportComponentDTOV2(component,
        licenseData);

    ComponentIdentifierLegalData componentIdentifierLegalData =
        fetchApiReportComponentDTOV2ToLegalData(
            owner.getId(),
            Collections.singleton(apiReportComponentDTOV2),
            multiLicenseToSingleLicense).entrySet().iterator().next().getValue();

    // We prefer hash over component.getHash() to get the stage scans since
    // hash should always equal ApplicationComponent.getHash()
    // component.getHash() may not equal ApplicationComponent.getHash()
    componentIdentifierLegalData.setStageScans(getStageScans(owner, hash, compIdentifier));

    // Get sourceLinks
    Set<LegalSourceLinkDTO> sourceLinks = mergeLegalSourceLinkAndSourceLinkOverride(compIdentifier, owner);

    return legalReportBuilder.getLicenseLegalComponentReport(
        apiReportComponentDTOV2,
        componentIdentifierLegalData,
        componentLegalComments,
        componentLegalFiles,
        multiLicenseToSingleLicense,
        licenseMetadataById,
        sourceLinks);
  }

  private Set<LegalSourceLinkDTO> mergeLegalSourceLinkAndSourceLinkOverride(
      ComponentIdentifier compIdentifier,
      Owner owner)
  {
    return mergeLegalSourceLinkAndSourceLinkOverride(compIdentifier, owner.getId(), null);
  }

  private Set<LegalSourceLinkDTO> mergeLegalSourceLinkAndSourceLinkOverride(
      ComponentIdentifier compIdentifier,
      String ownerId,
      Map<ComponentIdentifier, Set<LegalSourceLinkDTO>> sourceLinksByComponent)
  {
    Set<LegalSourceLinkDTO> sourceLinks;
    if (sourceLinksByComponent == null) {
      sourceLinks = apiLicenseLegalHdsService.getSourceLinksFromComponentIdentifier(compIdentifier);
    }
    else {
      sourceLinks = sourceLinksByComponent.getOrDefault(compIdentifier, Collections.emptySet());
    }
    Set<LegalSourceLinkDTO> sourceLinkOverrides =
        componentLegalService.getSourceLinksOverridesFromComponentIdentifier(ownerId, compIdentifier);
    sourceLinks = sourceLinks.stream()
        .filter(sourceLinkHDS -> sourceLinkOverrides.stream()
            .noneMatch(customSourceLink -> customSourceLink.originalContent.equals(sourceLinkHDS.originalContent)))
        .sorted(LEGAL_SOURCE_LINK_COMPARATOR)
        .collect(Collectors.toCollection(LinkedHashSet::new));
    sourceLinks.addAll(sourceLinkOverrides);
    return sourceLinks;
  }

  /**
   * Given a set of {@link License}s and the ownerId, return map of LicenseId to LicenseMetadataDTO.
   *
   * @param singleLicenses set of {@link License}
   * @param ownerId ownerId
   * @return map of license id to licenseMetadataDto
   */
  private Map<String, LicenseMetadataDTO> getLicenseMetadata(final Set<License> singleLicenses, String ownerId) {
    List<LicenseMetadataDTO> licenseMetadataDTOS = apiLicenseLegalHdsService.getLicenseMetadata(
        singleLicenses.stream()
            .map(License::getId)
            .collect(Collectors.toCollection(LinkedHashSet::new)));

    return getLicenseMetadata(licenseMetadataDTOS, ownerId);
  }

  /**
   * Given a set of {@link LicenseMetadataDTO}s and the ownerId, return map of LicenseId to LicenseMetadataDTO.
   *
   * @param licenseMetadataDTOS set of {@link LicenseMetadataDTO}
   * @param ownerId ownerId
   * @return map of license id to licenseMetadataDto
   */
  private Map<String, LicenseMetadataDTO> getLicenseMetadata(
      List<LicenseMetadataDTO> licenseMetadataDTOS,
      String ownerId)
  {
    Map<String, LicenseMetadataDTO> licenseMetadataMap = new HashMap<>();

    try (TransactionContext tx = applicationComponentDAO.createTransactionContext()) {
      // Need to check if any LicenseThreatGroup overrides have been performed
      for (LicenseMetadataDTO licenseMetadataDTO : licenseMetadataDTOS) {
        ApiLicenseThreatDTOV2 licenseThreatGroup = getHighestLicenseThreatGroupWithHierarchy(tx, ownerId,
            Collections.singleton(licenseMetadataDTO.getLicenseId()));

        if (licenseThreatGroup != null) {
          licenseMetadataDTO.setLicenseThreatGroup(
              new LicenseThreatGroupDTO(licenseThreatGroup.licenseThreatGroupName,
                  licenseThreatGroup.licenseThreatGroupLevel));
        }

        licenseMetadataMap.put(licenseMetadataDTO.getLicenseId(), licenseMetadataDTO);
      }
    }

    return licenseMetadataMap;
  }

  private Map<ApiLicenseDTO, Set<License>> buildMultiLicenseToSingleLicenseMap(
      final Collection<ApiLicenseDTO> allMultiLicenses)
  {
    return allMultiLicenses.stream()
        .collect(Collectors.toMap(
            Function.identity(),
            m -> multiLicenseDAO.getLicensesByMultiLicenseIdNotNull(m.licenseId),
            (prev, next) -> next,
            () -> new TreeMap<>(Comparator.comparing(dto -> dto.licenseId))));
  }

  /**
   * This method takes a set of licenses ids and checks for multi licenses,
   * it returns a Map where the key is the multi license id and value is a set
   * with licenses that belong to the multi license, if license is not a multi license,
   * returns the license as key and a set with the same key as value.
   *
   * This method uses {@link #buildMultiLicenseToSingleLicenseMap(Collection)}
   *
   * @param licenseIds a set with licensesIds
   * @return map with multi license as key and set of licenses as value
   */
  private Map<String, Set<String>> getMultiLicensesFromLicensesSet(final Set<String> licenseIds) {
    return buildMultiLicenseToSingleLicenseMap(
        getLicenseNames(licenseIds).entrySet()
            .stream()
            .map(e -> new ApiLicenseDTO(e.getKey(), e.getValue()))
            .collect(Collectors.toList()))
                .entrySet()
                .stream()
                .collect(
                    Collectors.toMap(
                        entry -> entry.getKey().licenseId,
                        entry -> entry.getValue()
                            .stream()
                            .map(License::getId)
                            .collect(Collectors.toSet()),
                        (prev, next) -> next));
  }

  private ApiLicenseThreatDTOV2 getHighestLicenseThreatGroupWithHierarchy(
      TransactionContext tx,
      String ownerId,
      Set<String> licenseIds)
  {
    LicenseThreatGroup result =
        tx != null
            ? licenseThreatGroupDAO.getHighestLicenseThreatGroupWithHierarchy(tx, ownerId, licenseIds)
            : licenseThreatGroupDAO.getHighestLicenseThreatGroupWithHierarchy(ownerId, licenseIds);
    return result == null ? null : new ApiLicenseDataAdapter(multiLicenseDAO).convert(result);
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
   * @param owner the {@link Owner} to get the stage scans for.
   * @param applicationComponentHash the application component hash to get the stage scans for, may be null, if it is
   *          null the componentIdentifier will be used instead.
   * @param componentIdentifier the {@link ComponentIdentifier} to get the stage scans for, ignored if the
   *          applicationComponentHash is passed.
   * @return null if the owner is not an application, else a list of {@link ApiLicenseLegalStageScanDTO} for each
   *         applicable stage.
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
      // Route through the licensed-stages enumeration so this respects every stage filter
      // documented on StageTypeService (DASHBOARD_CONTEXT excludes DEVELOP/PROXY/COMPLIANCE
      // and CLM-39870 HOSTED). Previously this iterated StageTypes.getAll() and used only
      // isIgnoredForDashboard, which is documented as a low-level predicate that does NOT
      // encode the HOSTED suppression — so HOSTED was leaking into the legal stage-scan
      // response for every component.
      for (StageType stageType : stageTypeService.getLicensedStageTypes(StageTypeService.DASHBOARD_CONTEXT)) {
        ApiLicenseLegalStageScanDTO apiLicenseLegalStageScanDTO = new ApiLicenseLegalStageScanDTO();
        apiLicenseLegalStageScanDTO.setStageName(stageType.getName());
        ApplicationComponent applicationComponentForStage = applicationComponents.stream()
            .filter(applicationComponent -> stageType.getId().equals(applicationComponent.getStageTypeId()))
            .findFirst()
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
    return aggregateFileDAO.getByApplicationComponentId(lastByComponentIdentifier.getId())
        .stream()
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

  public ComponentIdentifier getComponentIdentifier(ComponentIdentifier componentIdentifier, String packageUrl) {
    if (packageUrl == null && componentIdentifier == null) {
      throw new BadRequestException("componentIdentifier or packageUrl must be specified.");
    }
    return packageUrl != null ? new PackageUrlIdentifier(packageUrl).ensureCompleteIdentifier() : componentIdentifier;
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
        apiReportComponentDTOV2.licenseData = new ApiLicenseDataDTOV2();
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
    final Map<ComponentIdentifier, Set<ComponentLegalCommentDTO>> nonAname =
        getNonAnameComponentLegalComments(componentIdentifiers);

    // get all a-name comments
    final Map<ComponentIdentifier, Set<ComponentLegalCommentDTO>> aname =
        getAnameComponentLegalComments(componentIdentifiers);

    // combine it all together. components in aname and nonAname do not intersect, so we can just put them together
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
    final Set<AnameAggregateFileGroup> aNameComponents = componentIdentifiers.keySet()
        .stream()
        .filter(ComponentIdentifier::isAname)
        .map(s -> new AnameAggregateFileGroup(s,
            getAggregateHashes(s)))
        .collect(Collectors.toCollection(LinkedHashSet::new));

    return apiLicenseLegalHdsService.getAnameComponentLegalComments(
        aNameComponents,
        componentIdentifiers)
        .stream()
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
    final Set<ComponentIdentifier> nonAnameComponents = componentIdentifiers.keySet()
        .stream()
        .filter(s -> !s.isAname())
        .collect(Collectors.toCollection(LinkedHashSet::new));

    return apiLicenseLegalHdsService.getComponentLegalComments(nonAnameComponents)
        .stream()
        .collect(
            groupingBy(c -> LegalComponentIdentifierUtil.removeClassifierAndExtension(c.getComponentIdentifier()),
                Collectors.toCollection(LinkedHashSet::new)));
  }

  private Map<ComponentIdentifier, Set<ComponentLegalFileDTO>> getComponentLegalFilesByComponentIdentifier(
      Collection<ApiReportRawDataDTOV2> rawReports)
  {
    return apiLicenseLegalHdsService.getComponentLegalFiles(
        getComponentIdentifiers(rawReports))
        .stream()
        .collect(Collectors
            .groupingBy(c -> LegalComponentIdentifierUtil.removeClassifierAndExtension(c.getComponentIdentifier()),
                Collectors.toCollection(LinkedHashSet::new)));
  }

  /**
   * Merges HDS-provided source links with hierarchy-resolved overrides from the database.
   * Overrides replace any HDS link with the same {@code originalContent}, then only ENABLED links are kept.
   * Results are keyed by simplified component identifier (classifier/extension removed).
   */
  private Map<ComponentIdentifier, Set<LegalSourceLinkDTO>> getSourceLinksByComponentIdentifier(
      final String ownerId,
      final Set<ComponentIdentifier> componentIdentifiersFromRawReports,
      final Map<ComponentIdentifier, Set<LegalSourceLinkDTO>> hdsSourceLinksByComponent)
  {
    Map<ComponentIdentifier, List<SourceLinkOverride>> overridesByComponent =
        sourceLinkOverrideDAO.batchGetWithHierarchy(ownerId, componentIdentifiersFromRawReports);

    SetMultimap<ComponentIdentifier, LegalSourceLinkDTO> multimap =
        componentIdentifiersFromRawReports.stream()
            .collect(Multimaps.flatteningToMultimap(
                LegalComponentIdentifierUtil::removeClassifierAndExtension,
                ci -> mergeSourceLinks(
                    hdsSourceLinksByComponent.getOrDefault(ci, Collections.emptySet()),
                    overridesByComponent.getOrDefault(ci, Collections.emptyList())),
                () -> MultimapBuilder.hashKeys().treeSetValues(LEGAL_SOURCE_LINK_COMPARATOR).build()));

    return Multimaps.asMap(multimap);
  }

  /**
   * Applies user overrides on top of auto-discovered (HDS) source links for a single component.
   * An override matches an HDS link by {@code originalContent} (the originally-discovered URL) and can
   * edit its URL, disable it (hide from reports), or add an entirely new link. Only ENABLED links
   * (not hidden by the user) are included in the result.
   */
  private Stream<LegalSourceLinkDTO> mergeSourceLinks(
      final Set<LegalSourceLinkDTO> hdsLinks,
      final List<SourceLinkOverride> overrides)
  {
    List<LegalSourceLinkDTO> overrideDtos = overrides.stream().map(LegalSourceLinkDTO::new).toList();
    Set<String> overriddenContents = overrideDtos.stream()
        .map(o -> o.originalContent)
        .collect(Collectors.toSet());

    return Stream.concat(
        hdsLinks.stream().filter(hds -> !overriddenContents.contains(hds.originalContent)),
        overrideDtos.stream())
        .filter(link -> link.status == ComponentLegalPartStatus.ENABLED);
  }

  @VisibleForTesting
  Optional<ApiReportRawDataDTOV2> getLastRawApplicationReport(String applicationPublicId) {
    return Optional.ofNullable(applicationDAO.getByPublicId(applicationPublicId))
        .flatMap(
            application -> policyEvaluationDAO
                .getLastByApplicationIds(Collections.singleton(application.getId()))
                .stream()
                .max(Comparator.comparing(PolicyEvaluation::getTime))
                .map(policyEvaluation -> getLastRawApplicationReport(application.getPublicId(), policyEvaluation)));
  }

  @VisibleForTesting
  Optional<ApiReportRawDataDTOV2> getLastRawApplicationReportByStageId(String applicationPublicId, String stageId) {
    return Optional.ofNullable(applicationDAO.getByPublicId(applicationPublicId))
        .flatMap(
            application -> policyEvaluationDAO.getLastByApplicationIdsAndStageIds(
                Collections.singleton(application.getId()),
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
      final String applicationId,
      ApiReportRawDataDTOV2 latestRawReport,
      Set<ApiLicenseDTO> multiLicenses)
  {
    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.APPLICATION_LICENSE_USAGE);

    final ApplicationLicenseUsageTelemetry applicationLicenseUsageTelemetry = new ApplicationLicenseUsageTelemetry(
        applicationId,
        latestRawReport.components.stream()
            .map(component -> component.hash)
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.toCollection(LinkedHashSet::new)),
        multiLicenses.stream()
            .map(license -> license.licenseId)
            .collect(Collectors.toSet()));
    applicationLicenseUsageTelemetry.setRealApplicationId(
        telemetryUtils.obfuscateIfAdvancedReportingDisabled(applicationId));

    telemetryData.put(ApplicationLicenseUsageTelemetry.ATTRIBUTE_NAME, applicationLicenseUsageTelemetry);

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
              applicationId -> tagDAO.getByApplicationId(tx, applicationId)
                  .stream()
                  .map(Tag::getName)
                  .sorted()
                  .collect(Collectors.toList())));
    }
    return mapApplicationIdTagNames;
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

    Map<String, Set<String>> licenseIdObligationNamesMap = licenseIdsFound.isEmpty()
        ? new HashMap<>(0)
        : apiLicenseLegalHdsService.getLicenseMetadata(licenseIdsFound)
            .parallelStream()
            .collect(Collectors.toMap(
                LicenseMetadataDTO::getLicenseId,
                licenseMetadata -> licenseMetadata.getLicenseObligations()
                    .stream()
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

    Set<String> innerSourcePackageUrls = innerSourceApplicationDAO.getByPackageUrls(componentPurls)
        .stream()
        .map(InnerSourceApplication::getPackageUrl)
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
      Set<String> allObligationNames = componentLicensesDTO.getLicenses()
          .stream()
          .filter(licenseIdObligationNamesMap::containsKey)
          .flatMap(licenseId -> licenseIdObligationNamesMap.get(licenseId).stream())
          .collect(Collectors.toSet());

      // If the component has no matching obligations in the licenseIdObligationNamesMap, we skip it from the count
      if (allObligationNames.isEmpty()) {
        continue;
      }

      long addressedObligationCount = componentObligationsAddressed
          .getOrDefault(componentLicensesDTO.getComponentIdentifier(), Collections.emptySet())
          .stream()
          .filter(allObligationNames::contains)
          .count();

      if (addressedObligationCount >= allObligationNames.size()) {
        componentsFullyReviewed++;
      }
    }

    return componentsFullyReviewed;
  }

  private Map<ApiReportComponentDTOV2, ComponentIdentifierLegalData> fetchApiReportComponentDTOV2ToLegalData(
      String ownerId,
      Collection<ApiReportComponentDTOV2> components,
      Map<ApiLicenseDTO, Set<License>> multiLicenseToSingleLicense)
  {
    // Filter to components with a valid identifier (used for both batch queries and result assembly)
    List<ApiReportComponentDTOV2> validComponents = components.stream()
        .filter(dto -> dto.componentIdentifier != null)
        .toList();

    Set<ComponentIdentifier> allComponentIds = validComponents.stream()
        .map(dto -> dto.componentIdentifier.toComponentIdentifier())
        .collect(Collectors.toSet());

    // Batch fetch all legal data via individual per-entity DAOs (concurrently, since each is a network call)
    var copyrightOverridesFuture = CompletableFuture.supplyAsync(
        new TenantAwareSupplier<>(() -> copyrightOverrideDAO.batchGetWithHierarchy(ownerId, allComponentIds)),
        attributionReportForkJoinPool);
    var nonHierarchyCopyrightFuture = CompletableFuture.supplyAsync(
        new TenantAwareSupplier<>(
            () -> componentCopyrightDAO.batchGetByOwnerIdAndComponentIdentifiers(ownerId, allComponentIds)),
        attributionReportForkJoinPool);
    var legalFileOverridesFuture = CompletableFuture.supplyAsync(
        new TenantAwareSupplier<>(
            () -> legalFileOverrideDAO.batchGetWithHierarchyAllTypes(ownerId, allComponentIds)),
        attributionReportForkJoinPool);
    var obligationsFuture = CompletableFuture.supplyAsync(
        new TenantAwareSupplier<>(() -> componentObligationDAO.batchGetWithHierarchy(ownerId, allComponentIds)),
        attributionReportForkJoinPool);
    var attributionsFuture = CompletableFuture.supplyAsync(
        new TenantAwareSupplier<>(
            () -> componentObligationAttributionDAO.batchGetWithHierarchy(ownerId, allComponentIds)),
        attributionReportForkJoinPool);

    CompletableFuture.allOf(copyrightOverridesFuture, nonHierarchyCopyrightFuture,
        legalFileOverridesFuture, obligationsFuture, attributionsFuture).join();

    Map<ComponentIdentifier, List<CopyrightOverride>> copyrightOverridesMap = copyrightOverridesFuture.join();
    Map<ComponentIdentifier, ComponentCopyright> nonHierarchyCopyrightMap = nonHierarchyCopyrightFuture.join();
    var legalFileOverrides = legalFileOverridesFuture.join();
    Map<ComponentIdentifier, LegalFileOverrideDAO.BatchResult> licenseResultsMap =
        legalFileOverrides.row(LegalFileType.LICENSE);
    Map<ComponentIdentifier, LegalFileOverrideDAO.BatchResult> noticeResultsMap =
        legalFileOverrides.row(LegalFileType.NOTICE);
    Map<ComponentIdentifier, List<ComponentObligation>> obligationsMap = obligationsFuture.join();
    Map<ComponentIdentifier, List<ComponentObligationAttribution>> attributionsMap = attributionsFuture.join();

    // Collect all single license IDs per component for threat group lookup
    Set<String> allSingleLicenseIds = new HashSet<>();
    Map<ApiReportComponentDTOV2, Set<String>> componentToSingleLicenseIds = new HashMap<>();
    for (ApiReportComponentDTOV2 dto : validComponents) {
      Set<String> componentMultiLicenses = dto.licenseData.effectiveLicenses.stream()
          .map(l -> l.licenseId)
          .collect(Collectors.toSet());

      Set<String> componentSingleLicense = multiLicenseToSingleLicense.entrySet()
          .stream()
          .filter(e -> componentMultiLicenses.contains(e.getKey().licenseId))
          .flatMap(e -> e.getValue().stream())
          .map(License::getId)
          .collect(Collectors.toSet());

      componentToSingleLicenseIds.put(dto, componentSingleLicense);
      allSingleLicenseIds.addAll(componentSingleLicense);
    }

    // Single batch call for all license threat groups across all components (hierarchy resolved in query)
    Map<String, List<LicenseThreatGroup>> threatGroupsByLicenseId =
        licenseThreatGroupDAO.getLicenseIdThreatGroupsByLicenseIdsWithHierarchy(ownerId, allSingleLicenseIds);

    ApiLicenseDataAdapter licenseDataAdapter = new ApiLicenseDataAdapter(multiLicenseDAO);

    // Build result map sequentially using batch data
    Map<ApiReportComponentDTOV2, ComponentIdentifierLegalData> componentIdentifierLegalDataMap =
        new HashMap<>(validComponents.size());

    for (ApiReportComponentDTOV2 dto : validComponents) {
      ComponentIdentifier componentIdentifier = dto.componentIdentifier.toComponentIdentifier();
      ComponentIdentifierLegalData componentIdentifierLegalData = new ComponentIdentifierLegalData(
          LegalComponentIdentifierUtil.removeClassifierAndExtension(componentIdentifier));

      componentIdentifierLegalData.setCopyrightOverrides(
          copyrightOverridesMap.getOrDefault(componentIdentifier, Collections.emptyList()));
      componentIdentifierLegalData.setComponentCopyrights(nonHierarchyCopyrightMap.get(componentIdentifier));

      LegalFileOverrideDAO.BatchResult licenseResult = licenseResultsMap.get(componentIdentifier);
      if (licenseResult != null) {
        componentIdentifierLegalData.setLicenseOverrides(licenseResult.overrides());
        componentIdentifierLegalData.setComponentLicense(licenseResult.componentLegalFile());
      }

      LegalFileOverrideDAO.BatchResult noticeResult = noticeResultsMap.get(componentIdentifier);
      if (noticeResult != null) {
        componentIdentifierLegalData.setNoticeOverrides(noticeResult.overrides());
        componentIdentifierLegalData.setComponentNotice(noticeResult.componentLegalFile());
      }

      componentIdentifierLegalData.setObligations(
          obligationsMap.getOrDefault(componentIdentifier, Collections.emptyList()));
      componentIdentifierLegalData.setAttributions(
          attributionsMap.getOrDefault(componentIdentifier, Collections.emptyList()));

      // Resolve highest license threat group from batch results
      Set<String> componentSingleLicenseIds = componentToSingleLicenseIds.get(dto);
      if (componentSingleLicenseIds != null && !componentSingleLicenseIds.isEmpty()) {
        LicenseThreatGroup highest = componentSingleLicenseIds.stream()
            .flatMap(licenseId -> threatGroupsByLicenseId
                .getOrDefault(licenseId, Collections.emptyList())
                .stream())
            .max(Comparator.comparingInt(LicenseThreatGroup::getThreatLevel)
                .thenComparing(LicenseThreatGroup::getNameLowercaseNoWhitespace))
            .orElse(null);
        componentIdentifierLegalData.setHighestEffectiveLicenseThreatGroup(
            highest == null ? null : licenseDataAdapter.convert(highest));
      }

      componentIdentifierLegalDataMap.put(dto, componentIdentifierLegalData);
    }

    return componentIdentifierLegalDataMap;
  }

  private Map<String, String> getLicenseNames(Set<String> licenseIds) {
    try (TransactionContext tx = multiLicenseDAO.createTransactionContext()) {
      return licenseIds.stream().collect(Collectors.toMap(Function.identity(), licenseId -> {
        MultiLicense license = multiLicenseDAO.getById(tx, licenseId);
        return license != null ? license.getShortDisplayName() : licenseId;
      }));
    }
  }

  private void fillReviewProgress(
      TransactionContext tx,
      ApiLicenseLegalComponentDashboardDTO dto,
      Map<String, Set<String>> singleLicenseIdsByHash,
      Map<String, Set<String>> obligationNamesByLicenseId)
  {
    Set<String> singleLicenseIds = singleLicenseIdsByHash.get(dto.hash);

    Set<String> allObligationsNames = singleLicenseIds.stream()
        .filter(obligationNamesByLicenseId::containsKey)
        .flatMap(licenseId -> obligationNamesByLicenseId.get(licenseId).stream())
        .collect(Collectors.toSet());

    List<ComponentObligation> componentObligationsNames =
        componentObligationDAO.getByOwnerIdsAndComponentIdentifierAndObligationNames(tx,
            List.of(Organization.ROOT_ORGANIZATION_ID), dto.componentIdentifier, allObligationsNames);

    Map<String, Integer> countMap =
        legalDashboardService.countObligations(componentObligationsNames, allObligationsNames);
    dto.reviewCompletedCount = countMap.getOrDefault(LegalDashboardsService.ADDRESSEDCOUNT, 0);
    dto.reviewTotalCount = allObligationsNames.size();
    dto.reviewStatus = legalDashboardService.getReviewStatus(
        countMap.get(LegalDashboardsService.FLAGGEDCOUNT), countMap.get(LegalDashboardsService.OPENCOUNT),
        countMap.get(LegalDashboardsService.ADDRESSEDCOUNT), allObligationsNames, singleLicenseIds);
  }

  private boolean filterByReviewStatus(ApiLicenseLegalComponentDashboardDTO dto, LicenseLegalFilterDTO filter) {
    LicenseLegalReviewStatus filterStatus = filter.reviewStatus.iterator().next();
    boolean status = true;
    if (filterStatus == LicenseLegalReviewStatus.NOT_STARTED
        && dto.reviewStatus != LicenseObligationReviewStatus.UNREVIEWED)
    {
      return false;
    }
    if (filterStatus == LicenseLegalReviewStatus.OPEN && dto.reviewStatus == LicenseObligationReviewStatus.UNREVIEWED) {
      return false;
    }
    return status;
  }

  private ApiLicenseLegalComponentDashboardDTO fillLicenses(
      ApiLicenseLegalComponentDashboardDTO dto,
      Set<String> multiLicensesInComponent,
      Map<String, String> multiLicenseNamesById)
  {
    Set<ApiLicenseDTOV2> licenses =
        new TreeSet<>(Comparator.comparing(licenseDto -> licenseDto.licenseName, String.CASE_INSENSITIVE_ORDER));
    for (String multiLicenseId : multiLicensesInComponent) {
      licenses
          .add(new ApiLicenseDTOV2(multiLicenseId, multiLicenseNamesById.get(multiLicenseId), Collections.emptyList()));
    }
    dto.licenses = licenses;
    return dto;
  }

  private void fillLicenseThreatGroups(
      TransactionContext tx,
      ApiLicenseLegalComponentDashboardDTO dto,
      Set<String> multiLicensesInComponent,
      Map<String, Set<String>> multiLicenseIdToSingleLicenseIds)
  {
    Set<String> singleLicensesInComponent = multiLicensesInComponent.stream()
        .filter(multiLicenseIdToSingleLicenseIds::containsKey)
        .map(multiLicenseIdToSingleLicenseIds::get)
        .flatMap(Set::stream)
        .collect(Collectors.toSet());

    Map<String, List<LicenseThreatGroup>> threatGroupsByLicenseId =
        licenseThreatGroupDAO.getLicenseIdThreatGroupsByOwnerIdsAndLicenseIds(tx,
            List.of(Organization.ROOT_ORGANIZATION_ID), singleLicensesInComponent);

    ApiLicenseDataAdapter licenseDataAdapter = new ApiLicenseDataAdapter(multiLicenseDAO);

    dto.licenses.forEach(licenseDto -> {
      Set<String> singleLicenseIds = multiLicenseIdToSingleLicenseIds.get(licenseDto.licenseId);

      licenseDto.licenseThreatGroups = singleLicenseIds.stream()
          .flatMap(singleLicenseId -> threatGroupsByLicenseId
              .getOrDefault(singleLicenseId, Collections.emptyList())
              .stream())
          .map(licenseDataAdapter::convert)
          .collect(Collectors.toList());
    });
  }
}
