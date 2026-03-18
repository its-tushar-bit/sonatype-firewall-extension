/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.license;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.InvalidComponentIdentifierException;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseOverrideDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateLicenseDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.brain.model.thirdpartyscans.ResolvedLicenseDTO;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateLicense;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.product.license.CLMLicenseManager;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.purl.InvalidPackageURLException;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import org.apache.commons.lang3.StringUtils;

@Named
@Singleton
public class ThirdPartyComponentLicenseResolutionService
{
  private final LicenseOverrideDAO licenseOverrideDAO;

  private final ThirdPartyCoordinateLicenseDAO thirdPartyCoordinateLicenseDAO;

  private final ProductLicense productLicense;

  private final ApplicationDAO applicationDAO;

  private static final Set<LicenseOverrideStatus> LICENSE_OVERRIDE_STATUSES =
      Set.of(LicenseOverrideStatus.SELECTED, LicenseOverrideStatus.OVERRIDDEN);

  @Inject
  public ThirdPartyComponentLicenseResolutionService(
      final LicenseOverrideDAO licenseOverrideDAO,
      final ThirdPartyCoordinateLicenseDAO thirdPartyCoordinateLicenseDAO,
      final ProductLicense productLicense,
      final ApplicationDAO applicationDAO)
  {
    this.licenseOverrideDAO = licenseOverrideDAO;
    this.thirdPartyCoordinateLicenseDAO = thirdPartyCoordinateLicenseDAO;
    this.productLicense = productLicense;
    this.applicationDAO = applicationDAO;
  }

  /**
   * Retrieves license overrides for a specific component within an application.
   * <p>
   * This method checks if there are any license overrides applied to the given package URL within the context of the
   * specified application. It considers the application hierarchy when looking for overrides.
   *
   * @param appInternalId the internal ID of the application
   * @param packageUrl the package URL of the component to retrieve license overrides for
   * @return a set of {@link ResolvedLicenseDTO} objects representing the overridden licenses, or an empty set if no
   *         overrides exist or the component identifier is invalid
   */
  public Set<ResolvedLicenseDTO> getLicenseOverrides(
      String appInternalId,
      String packageUrl)
  {
    ComponentIdentifier componentIdentifier = getCompleteIdentifier(packageUrl);
    if (componentIdentifier == null) {
      return Collections.emptySet();
    }

    LicenseOverride licenseOverride = licenseOverrideDAO.getAppliedByOwnerIdAndComponentIdentifierWithHierarchy(
        applicationDAO.getByIdNotNull(appInternalId), componentIdentifier);
    if (licenseOverride != null && LICENSE_OVERRIDE_STATUSES.contains(licenseOverride.getStatus())) {
      return licenseOverride.getLicenseIds()
          .stream()
          .map(licenseId -> new ResolvedLicenseDTO(licenseId, null, null, null, licenseOverride.getStatus()))
          .collect(Collectors.toSet());
    }
    return Collections.emptySet();
  }

  /**
   * Determines if license overrides should be considered based on the product license.
   * <p>
   * This method checks if the current product license is either an advanced legal pack product or a lifecycle product.
   *
   * @return true if license overrides should be considered, false otherwise
   */
  public boolean shouldConsiderLicenseOverrides() {
    return CLMLicenseManager.hasAdvancedLegalPackProduct(productLicense) ||
        CLMLicenseManager.hasLifecycleProduct(productLicense);
  }

  /**
   * Resolves licenses for a component by checking for overrides first, then falling back to third-party licenses.
   * <p>
   * This method performs a hierarchical resolution process:
   * <ol>
   * <li>First attempts to find license overrides for the component if the identifier is valid and
   * the product license supports overrides</li>
   * <li>If no overrides are found, searches for third-party licenses associated with the component</li>
   * <li>Returns an empty set if neither overrides nor third-party licenses are found</li>
   * </ol>
   *
   * @param appInternalId the internal ID of the application context
   * @param thirdPartyFileCoordinate the component coordinate to resolve licenses for
   * @return a set of {@link ResolvedLicenseDTO} objects representing the resolved licenses, which may come from
   *         overrides or third-party licenses, or an empty set if none found
   */
  public Set<ResolvedLicenseDTO> resolveLicenseOverridesOrThirdPartyLicenses(
      String appInternalId,
      ThirdPartyFileCoordinate thirdPartyFileCoordinate)
  {
    return resolveLicenseOverridesOrThirdPartyLicenses(applicationDAO.getByIdNotNull(appInternalId),
        thirdPartyFileCoordinate);
  }

  public Set<ResolvedLicenseDTO> resolveLicenseOverridesOrThirdPartyLicenses(
      Application app,
      ThirdPartyFileCoordinate thirdPartyFileCoordinate)
  {
    return resolveLicenses(app, thirdPartyFileCoordinate);
  }

  private Set<ResolvedLicenseDTO> resolveLicenses(
      Application application,
      ThirdPartyFileCoordinate thirdPartyFileCoordinate)
  {
    ComponentIdentifier componentId = getCompleteIdentifier(thirdPartyFileCoordinate.getPackageUrl());

    // Try to find license overrides if component identifier is valid
    // and the product belongs to lifecycle products or products with ALP add-on
    if (componentId != null && shouldConsiderLicenseOverrides()) {
      LicenseOverride licenseOverride =
          licenseOverrideDAO.getAppliedByOwnerIdAndComponentIdentifierWithHierarchy(application, componentId);

      if (licenseOverride != null && LICENSE_OVERRIDE_STATUSES.contains(licenseOverride.getStatus())) {
        return licenseOverride.getLicenseIds()
            .stream()
            .map(licenseId -> new ResolvedLicenseDTO(licenseId, null, null, null, licenseOverride.getStatus()))
            .collect(Collectors.toSet());
      }
    }

    // If no overrides found, look for third party licenses
    Set<ThirdPartyCoordinateLicense> thirdPartyLicenses = new HashSet<>(
        thirdPartyCoordinateLicenseDAO.getByFileCoordinateIds(Collections.singleton(thirdPartyFileCoordinate.getId())));

    if (!thirdPartyLicenses.isEmpty()) {
      return thirdPartyLicenses.stream()
          .map(license -> new ResolvedLicenseDTO(
              license.getLicenseId(),
              license.getName(),
              license.getUrl(),
              license.getIdentificationSources(),
              null))
          .collect(Collectors.toSet());
    }

    // No results found
    return Collections.emptySet();
  }

  private ComponentIdentifier getCompleteIdentifier(final String packageUrl) {
    ComponentIdentifier id = null;
    try {
      if (StringUtils.isNotEmpty(packageUrl)) {
        id = new PackageUrlIdentifier(packageUrl).toComponentIdentifier();
        id.ensureComplete();
      }
    }
    catch (InvalidComponentIdentifierException | InvalidPackageURLException e) {
      // no-op
    }
    return id;
  }
}
