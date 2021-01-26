/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental.legal;

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

import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiLicenseDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiLicenseDataDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportRawDataDTOV2;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalApplicationDashboardDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalApplicationReportDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalComponentDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalComponentDashboardDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalComponentReportDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalDataDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalMetadataDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalObligationDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApplicationLicenseUsageTelemetry;
import com.sonatype.insight.brain.api.v2.service.ApiLicenseDataAdapter;
import com.sonatype.insight.brain.api.v2.service.ApiReportDataServiceV2;
import com.sonatype.insight.brain.dataaccess.ApplicationComponentDAO;
import com.sonatype.insight.brain.dataaccess.ApplicationComponentLicenseDAO;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.component.HashComponentIdentifierDAO;
import com.sonatype.insight.brain.dataaccess.legal.ComponentObligationAttributionDAO;
import com.sonatype.insight.brain.dataaccess.legal.ComponentObligationDAO;
import com.sonatype.insight.brain.dataaccess.legal.CopyrightOverrideDAO;
import com.sonatype.insight.brain.dataaccess.legal.LegalFileOverrideDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseOverrideDAO;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.tag.TagDAO;
import com.sonatype.insight.brain.hds.ComponentInfoService;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ApplicationComponent;
import com.sonatype.insight.brain.model.ApplicationComponentLicense;
import com.sonatype.insight.brain.model.HashHelper;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.HashComponentIdentifier;
import com.sonatype.insight.brain.model.legal.ComponentObligation;
import com.sonatype.insight.brain.model.legal.ComponentObligationAttribution;
import com.sonatype.insight.brain.model.legal.CopyrightOverride;
import com.sonatype.insight.brain.model.legal.LegalFileOverride;
import com.sonatype.insight.brain.model.legal.LegalFileType;
import com.sonatype.insight.brain.model.legal.ObligationStatus;
import com.sonatype.insight.brain.model.license.License;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.organization.ApplicationService;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzFilter;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.license.dto.model.ComponentLegalCommentDTO;
import com.sonatype.insight.license.dto.model.ComponentLegalFileDTO;
import com.sonatype.insight.license.dto.model.LicenseMetadataDTO;
import com.sonatype.insight.license.dto.model.LicenseObligationDTO;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import org.apache.commons.lang3.StringUtils;
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
      LicenseOverrideDAO licenseOverrideDAO,
      CopyrightOverrideDAO copyrightOverrideDAO,
      LegalFileOverrideDAO legalFileOverrideDAO,
      ComponentObligationDAO componentObligationDAO,
      ComponentObligationAttributionDAO componentObligationAttributionDAO)
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
  }

  public List<ApiLicenseLegalApplicationDashboardDTO> getLicenseLegalApplicationsDashboard(
      Set<String> organizationIds,
      Set<String> applicationIds,
      Set<String> tagIds,
      Set<String> stageTypeIds,
      Set<String> licenseIds)
  {
    checkLicense();

    List<Application> applications =
        getApplicationsByIdsAndOrganizationIdsAndTagIds(organizationIds, applicationIds, tagIds);
    Set<String> stageTypeIdsToCheck =
        isEmpty(stageTypeIds)
            ? StageTypes.getAll().stream().map(StageType::getId).collect(Collectors.toSet())
            : stageTypeIds;
    Map<String, List<ApiLicenseLegalApplicationDashboardDTO>> mapApplicationIdResults = new HashMap<>();
    Map<String, Set<String>> mapApplicationIdLicenseIds = new HashMap<>();

    try (TransactionContext tx = policyEvaluationDAO.createTransactionContext()) {
      for (Application application : applications) {
        Set<String> licensesAlreadyFound =
            mapApplicationIdLicenseIds.computeIfAbsent(application.getId(), key -> new HashSet<>());
        for (String stageTypeId : stageTypeIdsToCheck) {
          PolicyEvaluation policyEvaluation =
              policyEvaluationDAO.getLastByApplicationIdAndStageId(tx, application.getId(), stageTypeId);

          if (policyEvaluation == null) {
            continue;
          }

          ApiLicenseLegalApplicationDashboardDTO dto = new ApiLicenseLegalApplicationDashboardDTO();
          dto.applicationId = application.getId();
          dto.applicationName = application.getName();
          dto.applicationPublicId = application.getPublicId();
          dto.lastScanTime = policyEvaluation.getTime().getTime();
          dto.stageTypeId = stageTypeId;
          dto.stageTypeName = StageTypes.getById(stageTypeId).getName();

          List<Tag> tags = tagDAO.getByApplicationId(tx, application.getId());
          for (Tag tag : tags) {
            dto.applicationTagNames.add(tag.getName());
          }

          mapApplicationIdResults.computeIfAbsent(application.getId(), key -> new ArrayList<>()).add(dto);

          if (isEmpty(licenseIds)) {
            continue;
          }

          licensesAlreadyFound.addAll(getApplicationComponentEffectiveLicenses(tx, application, stageTypeId));
        }
      }
    }

    return mapApplicationIdResults.entrySet().stream().filter(entry -> isEmpty(licenseIds) ||
        !Collections.disjoint(mapApplicationIdLicenseIds.get(entry.getKey()), licenseIds))
        .flatMap(entry -> entry.getValue().stream()).collect(Collectors.toList());
  }

  private Set<String> getApplicationComponentEffectiveLicenses(
      TransactionContext tx,
      Application application,
      String stageTypeId)
  {
    Set<String> effectiveLicenseIds = new HashSet<>();
    List<ApplicationComponent> applicationComponents =
        applicationComponentDAO.getByApplicationIdAndStageTypeId(tx, application.getId(), stageTypeId);
    for (ApplicationComponent applicationComponent : applicationComponents) {
      if (applicationComponent.getComponentIdentifier() == null) {
        continue;
      }

      LicenseOverride licenseOverride = licenseOverrideDAO.getAppliedByOwnerIdAndComponentIdentifierWithHierarchy(tx,
          application, applicationComponent.getComponentIdentifier());

      if (licenseOverride != null && isNotEmpty(licenseOverride.getLicenseIds())) {
        effectiveLicenseIds.addAll(licenseOverride.getLicenseIds());
      }
      else {
        List<ApplicationComponentLicense> applicationComponentLicenses =
            applicationComponentLicenseDAO.getByApplicationComponentId(tx, applicationComponent.getId());

        effectiveLicenseIds.addAll(applicationComponentLicenses.stream()
            .map(ApplicationComponentLicense::getEffectiveLicenseId)
            .collect(Collectors.toSet()));
      }
    }
    return effectiveLicenseIds;
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
      @AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) String applicationPublicId)
  {
    checkLicense();

    log.info("Processing license metadata request for {}", applicationPublicId);
    Application application = applicationDAO.getByPublicIdNotNull(applicationPublicId);
    ApiReportRawDataDTOV2 latestRawReport = getLastRawApplicationReport(applicationPublicId)
        .orElseThrow(() -> new NotFoundException("Report for application " + applicationPublicId + " not found."));
    Set<ApiLicenseDTO> multiLicenses = getReportMultiLicenses(latestRawReport);
    Set<License> licenses = multiLicenses.stream()
        .map(multiLicense -> multiLicense.licenseId)
        .flatMap(multiLicenseId -> multiLicenseDAO.getLicensesByMultiLicenseIdNotNull(multiLicenseId).stream())
        .collect(Collectors.toCollection(LinkedHashSet::new));

    sendApplicationTelemetryData(applicationPublicId, latestRawReport, multiLicenses);

    Map<String, LicenseMetadataDTO> licenseMetadataById = multiLicenses.isEmpty() ? Collections.emptyMap() :
        apiLicenseLegalHdsService.getLicenseMetadata(
            licenses.stream()
                .map(License::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new)))
            .stream()
            .collect(toMap(LicenseMetadataDTO::getLicenseId, Function.identity()));
    Map<ComponentIdentifier, Set<ComponentLegalCommentDTO>> componentLegalCommentsByComponentIdentifier =
        getComponentLegalCommentsByComponentIdentifier(Collections.singleton(latestRawReport));
    Map<ComponentIdentifier, List<CopyrightOverride>> copyrightOverridesByComponentIdentifier =
        getCopyrightOverridesByComponentIdentifier(application.getId(), getComponentIdentifiers(latestRawReport));
    Map<ComponentIdentifier, Set<ComponentLegalFileDTO>> componentLegalFilesByComponentIdentifier =
        getComponentLegalFilesByComponentIdentifier(Collections.singleton(latestRawReport));
    Map<ComponentIdentifier, List<LegalFileOverride>> licenseOverridesByComponentIdentifier =
        getLegalFileOverridesByComponentIdentifier(application.getId(), getComponentIdentifiers(latestRawReport),
            LegalFileType.LICENSE);
    Map<ComponentIdentifier, List<LegalFileOverride>> noticeOverridesByComponentIdentifier =
        getLegalFileOverridesByComponentIdentifier(application.getId(), getComponentIdentifiers(latestRawReport),
            LegalFileType.NOTICE);
    log.info("Building license metadata report.");
    return legalReportBuilder
        .getLicenseLegalApplicationReport(latestRawReport, componentLegalCommentsByComponentIdentifier,
            copyrightOverridesByComponentIdentifier, componentLegalFilesByComponentIdentifier,
            licenseOverridesByComponentIdentifier, noticeOverridesByComponentIdentifier, multiLicenses, licenses,
            licenseMetadataById);
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
    Component component = componentInfoService.augmentComponentDetails(owner, componentInfoService
        .getUnaugmentedComponentDetails(owner, compIdentifier, httpRequest, identificationSource, scanId));
    if (component.getHash() == null && hash != null) {
      component.setHash(hash);
    }
    ApiLicenseDataDTOV2 licenseData = apiLicenseDataAdapter.convertToDTOV2(component);
    Set<License> licenses = licenseData.effectiveLicenses.stream()
        .map(multiLicense -> multiLicense.licenseId)
        .flatMap(multiLicenseId -> multiLicenseDAO.getLicensesByMultiLicenseIdNotNull(multiLicenseId).stream())
        .collect(Collectors.toCollection(LinkedHashSet::new));
    Map<String, LicenseMetadataDTO> licenseMetadataById =
        licenseData.effectiveLicenses.isEmpty() ? Collections.emptyMap() :
            apiLicenseLegalHdsService.getLicenseMetadata(
                licenses.stream()
                    .map(License::getId)
                    .collect(Collectors.toCollection(LinkedHashSet::new)))
                .stream()
                .collect(toMap(LicenseMetadataDTO::getLicenseId, Function.identity()));
    Set<ComponentLegalCommentDTO> componentLegalComments =
        apiLicenseLegalHdsService.getComponentLegalComments(Collections.singleton(compIdentifier));
    Set<ComponentLegalFileDTO> componentLegalFiles =
        apiLicenseLegalHdsService.getComponentLegalFiles(Collections.singleton(compIdentifier));
    List<CopyrightOverride> copyrightOverrides =
        copyrightOverrideDAO.getByOwnerIdAndComponentIdentifierWithHierarchy(ownerId, compIdentifier);
    List<LegalFileOverride> licenseOverrides =
        legalFileOverrideDAO
            .getByOwnerIdAndComponentIdentifierAndTypeWithHierarchy(ownerId, compIdentifier, LegalFileType.LICENSE);
    List<LegalFileOverride> noticeOverrides =
        legalFileOverrideDAO
            .getByOwnerIdAndComponentIdentifierAndTypeWithHierarchy(ownerId, compIdentifier, LegalFileType.NOTICE);
    ApiLicenseLegalDataDTO licenseLegalData =
        legalReportBuilder.getLicenseLegalData(licenseData, componentLegalComments, copyrightOverrides,
            componentLegalFiles, licenseOverrides, noticeOverrides);
    ApiLicenseLegalComponentDTO componentDTO =
        new ApiLicenseLegalComponentDTO(toComponentDTO(component), licenseLegalData);
    Set<ApiLicenseLegalMetadataDTO> licenseLegalMetadata = legalReportBuilder.getLicenseLegalMetadata(
        licenseData.effectiveLicenses, licenses, licenseMetadataById);
    Set<ApiLicenseLegalObligationDTO> obligations =
        createApiLicenseLegalObligationDTOs(ownerId, compIdentifier, getObligationNames(licenseLegalMetadata));
    return new ApiLicenseLegalComponentReportDTO(componentDTO, licenseLegalMetadata, obligations);
  }

  private Set<String> getObligationNames(Set<ApiLicenseLegalMetadataDTO> apiLicenseLegalMetadataDTOs) {
    return apiLicenseLegalMetadataDTOs.stream()
        .filter(dto -> dto.obligations != null)
        .flatMap(dto -> dto.obligations.stream())
        .map(LicenseObligationDTO::getName)
        .collect(Collectors.toSet());
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

  private ApiComponentDTOV2 toComponentDTO(Component component) {
    ApiComponentDTOV2 componentDTO = new ApiComponentDTOV2();
    String hash = component.getHash();
    ComponentIdentifier componentIdentifier = component.getComponentIdentifier();
    componentDTO.componentIdentifier =
        ApiComponentIdentifierDTOV2.fromComponentIdentifier(componentIdentifier);
    componentDTO.packageUrl = PackageUrlIdentifier.toPackageUrl(componentIdentifier);
    componentDTO.hash = hash;
    componentDTO.displayName = ComponentDisplayNameUtil.fromIdentifier(componentIdentifier).toString();
    componentDTO.proprietary = component.isProprietary();
    componentDTO.thirdParty =
        IdentificationSource.isThirdPartyIdentificationSource(component.getIdentificationSource().getId());
    return componentDTO;
  }

  private Set<ApiLicenseLegalObligationDTO> createApiLicenseLegalObligationDTOs(
      String ownerId,
      ComponentIdentifier componentIdentifier,
      Set<String> obligationNames)
  {
    Set<ApiLicenseLegalObligationDTO> apiLicenseLegalObligationDTOs = new LinkedHashSet<>();
    List<ComponentObligation> componentObligations = componentObligationDAO
        .getByOwnerIdAndComponentIdentifierAndObligationNamesWithHierarchy(ownerId, componentIdentifier,
            obligationNames);
    Map<String, ComponentObligation> componentObligationByName = componentObligations.stream()
        .collect(toMap(ComponentObligation::getObligationName, Function.identity()));
    List<ComponentObligationAttribution> componentObligationAttributions = componentObligationAttributionDAO
        .getByOwnerIdAndComponentIdentifierAndObligationNamesWithHierarchy(ownerId, componentIdentifier,
            obligationNames);
    Map<String, List<ComponentObligationAttribution>> componentObligationAttributionsByName =
        componentObligationAttributions.stream().collect(groupingBy(ComponentObligationAttribution::getObligationName));
    for (String obligationName : obligationNames) {
      ComponentObligation componentObligation = componentObligationByName.get(obligationName);
      ApiLicenseLegalObligationDTO apiLicenseLegalObligationDTO = new ApiLicenseLegalObligationDTO();
      apiLicenseLegalObligationDTO.name = obligationName;
      apiLicenseLegalObligationDTO.status =
          componentObligation == null ? ObligationStatus.OPEN : componentObligation.getStatus();
      apiLicenseLegalObligationDTO.comment = componentObligation == null ? null : componentObligation.getComment();
      apiLicenseLegalObligationDTO.attributions = componentObligationAttributionsByName
          .getOrDefault(obligationName, Collections.emptyList()).stream()
          .map(ComponentObligationAttribution::getContent)
          .collect(Collectors.toList());
      apiLicenseLegalObligationDTOs.add(apiLicenseLegalObligationDTO);
    }
    return apiLicenseLegalObligationDTOs;
  }

  private Set<ComponentIdentifier> getComponentIdentifiers(Collection<ApiReportRawDataDTOV2> rawReports) {
    return rawReports.stream()
        .flatMap(rawReport -> getComponentIdentifiers(rawReport).stream())
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  private Set<ComponentIdentifier> getComponentIdentifiers(ApiReportRawDataDTOV2 rawReport) {
    return rawReport.components.stream()
        .map(component -> component.componentIdentifier)
        .filter(Objects::nonNull)
        .map(apiComponentIdentifierDTOV2 -> apiComponentIdentifierDTOV2.toComponentIdentifier())
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  private Set<ApiLicenseDTO> getReportMultiLicenses(ApiReportRawDataDTOV2 rawReport) {
    return rawReport.components.stream()
        .filter(component -> component.licenseData != null)
        .flatMap(component -> getAllLicenses(component.licenseData).stream())
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  private Set<ApiLicenseDTO> getAllLicenses(ApiLicenseDataDTOV2 licenses) {
    return Stream.concat(Stream.concat(licenses.declaredLicenses.stream(), licenses.observedLicenses.stream()),
        licenses.effectiveLicenses.stream()).collect(Collectors.toCollection(LinkedHashSet::new));
  }

  private Map<ComponentIdentifier, Set<ComponentLegalCommentDTO>> getComponentLegalCommentsByComponentIdentifier(
      Collection<ApiReportRawDataDTOV2> rawReports)
  {
    return apiLicenseLegalHdsService.getComponentLegalComments(
        getComponentIdentifiers(rawReports)).stream()
        .collect(Collectors.groupingBy(c -> LegalReportBuilder.removeClassifierAndExtension(c.getComponentIdentifier()),
            Collectors.toCollection(LinkedHashSet::new)));
  }

  private Map<ComponentIdentifier, List<CopyrightOverride>> getCopyrightOverridesByComponentIdentifier(
      String ownerId,
      Collection<ComponentIdentifier> componentIdentifiers)
  {
    Map<ComponentIdentifier, List<CopyrightOverride>> copyrightOverridesByComponentIdentifier = new HashMap<>();
    try (TransactionContext tx = copyrightOverrideDAO.createTransactionContext()) {
      tx.begin();
      for (ComponentIdentifier componentIdentifier : componentIdentifiers) {
        copyrightOverridesByComponentIdentifier.put(componentIdentifier,
            copyrightOverrideDAO.getByOwnerIdAndComponentIdentifierWithHierarchy(tx, ownerId, componentIdentifier));
      }
      tx.commit();
    }
    return copyrightOverridesByComponentIdentifier;
  }

  private Map<ComponentIdentifier, Set<ComponentLegalFileDTO>> getComponentLegalFilesByComponentIdentifier(
      Collection<ApiReportRawDataDTOV2> rawReports)
  {
    return apiLicenseLegalHdsService.getComponentLegalFiles(
        getComponentIdentifiers(rawReports)).stream()
        .collect(Collectors.groupingBy(c -> LegalReportBuilder.removeClassifierAndExtension(c.getComponentIdentifier()),
            Collectors.toCollection(LinkedHashSet::new)));
  }

  private Map<ComponentIdentifier, List<LegalFileOverride>> getLegalFileOverridesByComponentIdentifier(
      String ownerId,
      Collection<ComponentIdentifier> componentIdentifiers,
      LegalFileType legalFileType)
  {
    Map<ComponentIdentifier, List<LegalFileOverride>> legalFileOverridesByComponentIdentifier = new HashMap<>();
    try (TransactionContext tx = copyrightOverrideDAO.createTransactionContext()) {
      tx.begin();
      for (ComponentIdentifier componentIdentifier : componentIdentifiers) {
        legalFileOverridesByComponentIdentifier.put(componentIdentifier, legalFileOverrideDAO
            .getByOwnerIdAndComponentIdentifierAndTypeWithHierarchy(tx, ownerId, componentIdentifier, legalFileType));
      }
      tx.commit();
    }
    return legalFileOverridesByComponentIdentifier;
  }

  // Visible for testing
  Optional<ApiReportRawDataDTOV2> getLastRawApplicationReport(String applicationPublicId) {
    return Optional.ofNullable(applicationDAO.getByPublicId(applicationPublicId)).flatMap(
        application -> policyEvaluationDAO
            .getLastByApplicationIds(Collections.singleton(application.getId()))
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
                .collect(Collectors.toCollection(LinkedHashSet::new))));

    telemetrySender.send(telemetryData);
  }

  private void checkLicense() {
    if (!productLicense.hasFeature(LicensedFeature.ADVANCED_LEGAL_PACK)) {
      log.debug("License does not support Advanced Legal Pack features");
      throw new InvalidLicenseException();
    }
  }
}
