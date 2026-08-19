/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Date;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.ComponentChangeDetectionConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.ComponentChangeDetectionEventDAO;
import com.sonatype.insight.brain.model.ComponentChangeDetectionConfiguration;
import com.sonatype.insight.brain.model.ComponentChangeDetectionEvent;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.tenancy.TenantUtil;
import com.sonatype.insight.license.model.LicensedFeature;

import static com.google.common.base.Preconditions.checkNotNull;

@Named
@Singleton
public class ApiComponentChangeDetectionService
{
  private final ComponentChangeDetectionConfigurationDAO componentChangeDetectionConfigurationDAO;

  private final ComponentChangeDetectionEventDAO componentChangeDetectionEventDAO;

  private final Configuration configuration;

  private final ProductLicense productLicense;

  private static final TenantUtil tenantUtil = new TenantUtil();

  @Inject
  public ApiComponentChangeDetectionService(
      final ComponentChangeDetectionConfigurationDAO componentChangeDetectionConfigurationDAO,
      final ComponentChangeDetectionEventDAO componentChangeDetectionEventDAO,
      final Configuration configuration,
      final ProductLicense productLicense)
  {
    this.componentChangeDetectionConfigurationDAO = checkNotNull(componentChangeDetectionConfigurationDAO);
    this.componentChangeDetectionEventDAO = checkNotNull(componentChangeDetectionEventDAO);
    this.configuration = checkNotNull(configuration);
    this.productLicense = checkNotNull(productLicense);
  }

  public List<ComponentChangeDetectionConfiguration> getConfiguration(int page, int pageSize) {
    validateLicense();
    return componentChangeDetectionConfigurationDAO.getComponents(page, pageSize);
  }

  /// Adds items to the configuration Returns a list of components that have been removed from the configuration if the
  /// bucket size is exceeded
  public List<ComponentChangeDetectionConfiguration> addItemsToConfiguration(
      List<ComponentChangeDetectionConfiguration> components)
  {
    validateLicense();
    return componentChangeDetectionConfigurationDAO.addComponents(
        configuration.getComponentChangeDetectionMaxComponents(), components);
  }

  public void updateHashForComponent(String purl, String hash) {
    validateLicense();
    componentChangeDetectionConfigurationDAO.updateComparisonHashOfPurl(purl, hash);
  }

  public void addEvent(ComponentChangeDetectionEvent event) {
    validateLicense();
    componentChangeDetectionEventDAO.insert(event);
  }

  public void acknowledgeEventsOlderThan(Date time) {
    validateLicense();
    componentChangeDetectionEventDAO.deleteEntriesOlderThan(time);
  }

  public void removeExcessEvents() {
    validateLicense();
    componentChangeDetectionEventDAO.removeExcessEvents(configuration.getComponentChangeDetectionMaxEvents());
  }

  public void updateHashAndVersionForComponent(final String purl, final String hash, final String version) {
    validateLicense();
    componentChangeDetectionConfigurationDAO.updateComparisonHashAndVersionOfPurl(purl, hash, version);
  }

  public boolean isMultiTenant() {
    return tenantUtil.isMultiTenant();
  }

  public boolean isFeatureFlagOrLicenseDisabled() {
    return !SystemConfigurationPropertyFeature.COMPONENT_CHANGE_DETECTION_API.isEnabled() ||
        !productLicense.hasFeature(LicensedFeature.FIREWALL);
  }

  private boolean isLicenseDisabled() {
    return !productLicense.hasFeature(LicensedFeature.FIREWALL);
  }

  public void validateLicense() {
    if (isLicenseDisabled()) {
      throw new InvalidLicenseException();
    }
  }
}
