/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.ApiLegacyViolationChangeResponseDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiLegacyViolationStatusDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyViolationDTOV2;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.policy.LegacyViolationService;
import com.sonatype.insight.brain.policy.LegacyViolationService.LegacyViolationStatusDTO;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.license.model.LicensedFeature;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

@Named
@Singleton
public class ApiLegacyViolationService
{
  private static final Logger log = LoggerFactory.getLogger(ApiLegacyViolationService.class);

  private static final Comparator<PolicyViolation> LEGACY_VIOLATION_ORDER = Comparator
      .comparing(PolicyViolation::getLegacyViolationTime, Comparator.reverseOrder())
      .thenComparing(PolicyViolation::getId);

  private final LegacyViolationService legacyViolationService;

  private final ApplicationDAO applicationDAO;

  private final PolicyViolationDAO policyViolationDAO;

  private final ProductLicense productLicense;

  @Inject
  public ApiLegacyViolationService(
      final LegacyViolationService legacyViolationService,
      final ApplicationDAO applicationDAO,
      final PolicyViolationDAO policyViolationDAO,
      final ProductLicense productLicense)
  {
    this.legacyViolationService = checkNotNull(legacyViolationService);
    this.applicationDAO = checkNotNull(applicationDAO);
    this.policyViolationDAO = checkNotNull(policyViolationDAO);
    this.productLicense = checkNotNull(productLicense);
  }

  public void validateLicense() {
    if (!productLicense.hasFeature(LicensedFeature.POLICY_GRANDFATHERING)) {
      log.debug("Legacy violations are not supported by the current license.");
      throw new InvalidLicenseException();
    }
  }

  @Authorize(permission = Permission.READ)
  public List<ApiPolicyViolationDTOV2> listLegacyViolations(
      @AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) final String applicationPublicId,
      final String policyIdFilter,
      final ComponentIdentifier componentIdentifierFilter)
  {
    Objects.requireNonNull(applicationPublicId, "applicationPublicId must not be null");
    validateLicense();

    Application app = applicationDAO.getByPublicIdNotNull(applicationPublicId);
    List<PolicyViolation> legacyViolations =
        policyViolationDAO.getUnfixedLegacyViolationByApplicationId(app.getId());
    policyViolationDAO.loadConstraintFacts(legacyViolations);

    return legacyViolations.stream()
        .filter(matchesFilters(policyIdFilter, componentIdentifierFilter))
        .sorted(LEGACY_VIOLATION_ORDER)
        .map(ApiLegacyViolationAdapter::convert)
        .toList();
  }

  public ApiLegacyViolationChangeResponseDTO revoke(final String applicationPublicId) {
    Objects.requireNonNull(applicationPublicId, "applicationPublicId must not be null");
    validateLicense();
    int changed = legacyViolationService.revokeLegacyViolationStatus(applicationPublicId);
    return new ApiLegacyViolationChangeResponseDTO(changed);
  }

  public ApiLegacyViolationChangeResponseDTO grant(final String applicationPublicId) {
    Objects.requireNonNull(applicationPublicId, "applicationPublicId must not be null");
    validateLicense();
    int changed = legacyViolationService.grantLegacyViolationStatus(applicationPublicId);
    return new ApiLegacyViolationChangeResponseDTO(changed);
  }

  public ApiLegacyViolationStatusDTO getConfig(final OwnerType ownerType, final String ownerId) {
    Objects.requireNonNull(ownerType, "ownerType must not be null");
    Objects.requireNonNull(ownerId, "ownerId must not be null");
    validateLicense();
    LegacyViolationStatusDTO internal = legacyViolationService.getLegacyViolationsStatus(ownerType, ownerId);
    return toApiDto(internal);
  }

  public ApiLegacyViolationStatusDTO setConfig(
      final OwnerType ownerType,
      final String ownerId,
      final ApiLegacyViolationStatusDTO request)
  {
    Objects.requireNonNull(ownerType, "ownerType must not be null");
    Objects.requireNonNull(ownerId, "ownerId must not be null");
    Objects.requireNonNull(request, "request must not be null");
    validateLicense();
    LegacyViolationStatusDTO updated =
        legacyViolationService.setLegacyViolationStatus(ownerType, ownerId, toInternalDto(request));
    return toApiDto(updated);
  }

  private static Predicate<PolicyViolation> matchesFilters(
      final String policyIdFilter,
      final ComponentIdentifier componentIdentifierFilter)
  {
    return pv -> (policyIdFilter == null || policyIdFilter.equals(pv.getPolicyId()))
        && (componentIdentifierFilter == null
            || componentIdentifierFilter.equals(pv.getComponentIdentifier()));
  }

  private ApiLegacyViolationStatusDTO toApiDto(LegacyViolationStatusDTO internal) {
    if (internal == null) {
      return null;
    }
    ApiLegacyViolationStatusDTO dto = new ApiLegacyViolationStatusDTO();
    dto.enabled = internal.enabled;
    dto.allowOverride = internal.allowOverride;
    dto.allowChange = internal.allowChange;
    dto.enabledInParent = internal.enabledInParent;
    dto.inheritedFromOrganizationName = internal.inheritedFromOrganizationName;
    return dto;
  }

  private LegacyViolationStatusDTO toInternalDto(ApiLegacyViolationStatusDTO dto) {
    LegacyViolationStatusDTO internal = new LegacyViolationStatusDTO();
    internal.enabled = dto.enabled;
    internal.allowOverride = dto.allowOverride;
    internal.allowChange = dto.allowChange;
    internal.enabledInParent = dto.enabledInParent;
    internal.inheritedFromOrganizationName = dto.inheritedFromOrganizationName;
    return internal;
  }
}
