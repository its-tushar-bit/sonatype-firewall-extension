/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.hds.HdsClientAnalytics;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import static java.util.stream.Collectors.toList;

@Named
@Singleton
public class RepositoryConfigurationCollector
    implements TelemetryCollector
{
  public static final String REPOSITORY_TELEMETRY = "repository_telemetry";

  public static final String IS_QUARANTINE_ENABLED = "is_quarantine_enabled";

  private final ProductLicense productLicense;

  private final RepositoryDAO repositoryDAO;

  @Inject
  public RepositoryConfigurationCollector(
      final ProductLicense productLicense,
      final RepositoryDAO repositoryDAO)
  {
    this.productLicense = productLicense;
    this.repositoryDAO = repositoryDAO;
  }

  @Override
  public TelemetryData collectData() {
    TelemetryData telemetryData = null;

    // Only collect telemetry if a Firewall license is present
    if (productLicense.hasFeature(LicensedFeature.FIREWALL) ||
        productLicense.hasFeature(LicensedFeature.FIREWALL_FOR_ARTIFACTORY)) {
      telemetryData = new TelemetryData(TelemetryPurpose.REPOSITORY_CONFIGURATION);

      List<RepositoryTelemetry> repositoryTelemetries =
          repositoryDAO.getAll().stream().map(this::collectData).collect(toList());

      boolean isQuarantineEnabled = repositoryTelemetries.stream().anyMatch(RepositoryTelemetry::isQuarantineEnabled);

      telemetryData.put(REPOSITORY_TELEMETRY, repositoryTelemetries);
      telemetryData.put(IS_QUARANTINE_ENABLED, isQuarantineEnabled);
    }

    return telemetryData;
  }

  private RepositoryTelemetry collectData(final Repository repository) {
    return new RepositoryTelemetry(repository.getRepositoryManagerId(), repository.getId(), repository.getFormat(),
        repository.isEnabled(), repository.isQuarantineEnabled());
  }

  static class RepositoryTelemetry
  {
    private final String repositoryManagerId;

    private final String repositoryId;

    private final String repositoryFormat;

    private final boolean enabled;

    private final boolean quarantineEnabled;

    public RepositoryTelemetry(
        final String repositoryManagerId,
        final String repositoryId,
        final String repositoryFormat,
        final boolean enabled,
        final boolean quarantineEnabled)
    {
      this.repositoryManagerId = HdsClientAnalytics.obfuscate(repositoryManagerId);
      this.repositoryId = HdsClientAnalytics.obfuscate(repositoryId);
      this.repositoryFormat = repositoryFormat;
      this.enabled = enabled;
      this.quarantineEnabled = quarantineEnabled;
    }

    public String getRepositoryManagerId() {
      return repositoryManagerId;
    }

    public String getRepositoryId() {
      return repositoryId;
    }

    public String getRepositoryFormat() {
      return repositoryFormat;
    }

    public boolean isEnabled() {
      return enabled;
    }

    public boolean isQuarantineEnabled() {
      return quarantineEnabled;
    }
  }
}
