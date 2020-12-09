/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental.legal;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
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
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalApplicationReportDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalComponentDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalComponentReportDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalDataDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApplicationLicenseUsageTelemetry;
import com.sonatype.insight.brain.api.v2.service.ApiLicenseDataAdapter;
import com.sonatype.insight.brain.api.v2.service.ApiReportDataServiceV2;
import com.sonatype.insight.brain.dataaccess.ApplicationComponentDAO;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.component.HashComponentIdentifierDAO;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.hds.ComponentInfoService;
import com.sonatype.insight.brain.model.ApplicationComponent;
import com.sonatype.insight.brain.model.HashHelper;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.HashComponentIdentifier;
import com.sonatype.insight.brain.model.license.License;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.license.dto.model.ComponentLegalCommentDTO;
import com.sonatype.insight.license.dto.model.ComponentLegalFileDTO;
import com.sonatype.insight.license.dto.model.LicenseMetadataDTO;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

  private final ComponentInfoService componentInfoService;

  private final ApiLicenseDataAdapter apiLicenseDataAdapter;

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
      ApiLicenseDataAdapter apiLicenseDataAdapter)
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
  }

  @Authorize(permission = Permission.READ)
  public ApiLicenseLegalApplicationReportDTO getLicenseLegalApplicationReport(
      @AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) String applicationPublicId)
  {
    log.info("Processing license metadata request for {}", applicationPublicId);
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
            .collect(Collectors.toMap(LicenseMetadataDTO::getLicenseId, Function.identity()));
    Map<ComponentIdentifier, Set<ComponentLegalCommentDTO>> componentLegalCommentsByComponentIdentifier =
        getComponentLegalCommentsByComponentIdentifier(Collections.singleton(latestRawReport));
    Map<ComponentIdentifier, Set<ComponentLegalFileDTO>> componentLegalFilesByComponentIdentifier =
        getComponentLegalFilesByComponentIdentifier(Collections.singleton(latestRawReport));
    log.info("Building license metadata report.");
    return legalReportBuilder
        .getLicenseLegalApplicationReport(latestRawReport, componentLegalCommentsByComponentIdentifier,
            componentLegalFilesByComponentIdentifier, multiLicenses, licenses, licenseMetadataById);
  }

  /**
   * <p>Given an {@link OwnerType} and ownerId specifying an {@link Owner} which determines license overrides
   * with either a {@link ComponentIdentifier}, package url, or component hash, this generates a {@link
   * ApiLicenseLegalComponentReportDTO} containing the following component information:</p>
   * <ul>
   *     <li>License obligations</li>
   *     <li>Licenses</li>
   *     <li>Obligation status</li>
   *     <li>Copyright statements</li>
   *     <li>Notice texts</li>
   *     <li>License texts</li>
   * </ul>
   * <p>
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
  @Authorize(permission = Permission.READ)
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
                .collect(Collectors.toMap(LicenseMetadataDTO::getLicenseId, Function.identity()));
    Set<ComponentLegalCommentDTO> componentLegalComments =
        apiLicenseLegalHdsService.getComponentLegalComments(Collections.singleton(compIdentifier));
    Set<ComponentLegalFileDTO> componentLegalFiles =
        apiLicenseLegalHdsService.getComponentLegalFiles(Collections.singleton(compIdentifier));
    ApiLicenseLegalDataDTO licenseLegalData =
        legalReportBuilder.getLicenseLegalData(licenseData, componentLegalComments, componentLegalFiles);
    ApiLicenseLegalComponentDTO componentDTO =
        new ApiLicenseLegalComponentDTO(toComponentDTO(component), licenseLegalData);
    return new ApiLicenseLegalComponentReportDTO(componentDTO,
        legalReportBuilder.getLicenseLegalMetadata(licenseData.effectiveLicenses, licenses, licenseMetadataById));
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

  private Map<ComponentIdentifier, Set<ComponentLegalFileDTO>> getComponentLegalFilesByComponentIdentifier(
      Collection<ApiReportRawDataDTOV2> rawReports)
  {
    return apiLicenseLegalHdsService.getComponentLegalFiles(
        getComponentIdentifiers(rawReports)).stream()
        .collect(Collectors.groupingBy(c -> LegalReportBuilder.removeClassifierAndExtension(c.getComponentIdentifier()),
            Collectors.toCollection(LinkedHashSet::new)));
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
}
