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
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.ApiLicenseDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiLicenseDataDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportRawDataDTOV2;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalApplicationReportDTO;
import com.sonatype.insight.brain.api.v2.service.ApiReportDataServiceV2;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.model.license.License;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.license.dto.model.ComponentLegalCommentDTO;
import com.sonatype.insight.license.dto.model.ComponentLegalFileDTO;
import com.sonatype.insight.license.dto.model.LicenseMetadataDTO;

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

  @Inject
  public ApiLicenseLegalService(
      MultiLicenseDAO multiLicenseDAO,
      ApiLicenseLegalHdsService apiLicenseLegalHdsService,
      ApplicationDAO applicationDAO,
      PolicyEvaluationDAO policyEvaluationDAO,
      ApiReportDataServiceV2 apiReportDataServiceV2,
      LegalReportBuilder legalReportBuilder)
  {
    this.multiLicenseDAO = multiLicenseDAO;
    this.apiLicenseLegalHdsService = apiLicenseLegalHdsService;
    this.applicationDAO = applicationDAO;
    this.policyEvaluationDAO = policyEvaluationDAO;
    this.apiReportDataServiceV2 = apiReportDataServiceV2;
    this.legalReportBuilder = legalReportBuilder;
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
        .collect(Collectors.toSet());
    Map<String, LicenseMetadataDTO> licenseMetadataById = multiLicenses.isEmpty() ? Collections.emptyMap() :
        apiLicenseLegalHdsService.getLicenseMetadata(
            licenses.stream()
                .map(License::getId)
                .collect(Collectors.toSet()))
            .stream()
            .collect(Collectors.toMap(LicenseMetadataDTO::getLicenseId, Function.identity()));
    Map<ComponentIdentifier, Set<ComponentLegalCommentDTO>> componentLegalCommentsByComponentIdentifier =
        getComponentLegalCommentsByComponentIdentifier(Collections.singleton(latestRawReport));
    Map<ComponentIdentifier, Set<ComponentLegalFileDTO>> componentLegalFilesByComponentIdentifier =
        getComponentLegalFilesByComponentIdentifier(Collections.singleton(latestRawReport));
    log.info("Building license metadata report.");
    return legalReportBuilder
        .getLicenseLegalApplicationReport(latestRawReport, componentLegalCommentsByComponentIdentifier,
            componentLegalFilesByComponentIdentifier, licenses, licenseMetadataById);
  }

  private Set<ComponentIdentifier> getComponentIdentifiers(Collection<ApiReportRawDataDTOV2> rawReports) {
    return rawReports.stream()
        .flatMap(rawReport -> getComponentIdentifiers(rawReport).stream())
        .collect(Collectors.toSet());
  }

  private Set<ComponentIdentifier> getComponentIdentifiers(ApiReportRawDataDTOV2 rawReport) {
    return rawReport.components.stream()
        .map(component -> component.componentIdentifier)
        .filter(Objects::nonNull)
        .map(apiComponentIdentifierDTOV2 -> apiComponentIdentifierDTOV2.toComponentIdentifier())
        .collect(Collectors.toSet());
  }

  private Set<ApiLicenseDTO> getReportMultiLicenses(ApiReportRawDataDTOV2 rawReport) {
    return rawReport.components.stream()
        .filter(component -> component.licenseData != null)
        .flatMap(component -> getAllLicenses(component.licenseData).stream())
        .collect(Collectors.toSet());
  }

  private Set<ApiLicenseDTO> getAllLicenses(ApiLicenseDataDTOV2 licenses) {
    return Stream.concat(Stream.concat(licenses.declaredLicenses.stream(), licenses.observedLicenses.stream()),
        licenses.effectiveLicenses.stream()).collect(Collectors.toSet());
  }

  private Map<ComponentIdentifier, Set<ComponentLegalCommentDTO>> getComponentLegalCommentsByComponentIdentifier(
      Collection<ApiReportRawDataDTOV2> rawReports)
  {
    return apiLicenseLegalHdsService.getComponentLegalComments(
        getComponentIdentifiers(rawReports)).stream()
        .collect(Collectors.groupingBy(
            c -> LegalReportBuilder.removeClassifierAndExtension(c.getComponentIdentifier()), Collectors.toSet()));
  }

  private Map<ComponentIdentifier, Set<ComponentLegalFileDTO>> getComponentLegalFilesByComponentIdentifier(
      Collection<ApiReportRawDataDTOV2> rawReports)
  {
    return apiLicenseLegalHdsService.getComponentLegalFiles(
        getComponentIdentifiers(rawReports)).stream()
        .collect(Collectors.groupingBy(
            c -> LegalReportBuilder.removeClassifierAndExtension(c.getComponentIdentifier()), Collectors.toSet()));
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
}
