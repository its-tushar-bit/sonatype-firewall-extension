/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.telemetry.SonatypeUserAgentUtil;
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

  private final RepositoryManagerDAO repositoryManagerDAO;

  @Inject
  public RepositoryConfigurationCollector(
      final ProductLicense productLicense,
      final RepositoryDAO repositoryDAO,
      final RepositoryManagerDAO repositoryManagerDAO)
  {
    this.productLicense = productLicense;
    this.repositoryDAO = repositoryDAO;
    this.repositoryManagerDAO = repositoryManagerDAO;
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

  @Override
  public boolean isClusterTelemetry() {
    return true;
  }

  private RepositoryTelemetry collectData(final Repository repository) {
    SonatypeUserAgentUtil.UserAgent userAgent = getUserAgent(repository);

    return new RepositoryTelemetry(
        repository.getRepositoryManagerId(),
        repository.getId(),
        repository.getFormat(),
        repository.isAuditEnabled(),
        repository.isQuarantineEnabled(),
        userAgent != null ? userAgent.hostProductName : null,
        userAgent != null ? userAgent.productEdition : null,
        userAgent != null ? userAgent.hostProductVersion : null,
        userAgent != null ? userAgent.environment : null,
        userAgent != null ? userAgent.environmentVersion : null,
        userAgent != null ? userAgent.os : null,
        userAgent != null ? userAgent.osVersion : null,
        userAgent != null ? userAgent.product : null,
        userAgent != null ? userAgent.version : null
    );
  }

  private SonatypeUserAgentUtil.UserAgent getUserAgent(final Repository repository) {
    RepositoryManager repositoryManager = repositoryManagerDAO.getById(repository.getRepositoryManagerId());
    return SonatypeUserAgentUtil.parse(repositoryManager.getUserAgent());
  }

  static class RepositoryTelemetry
  {
    private final String repositoryManagerId;

    private final String repositoryId;

    private final String repositoryFormat;

    private final boolean enabled;

    private final boolean quarantineEnabled;

    private final String repositoryManagerName;

    private final String repositoryManagerEdition;

    private final String repositoryManagerVersion;

    private final String environment;

    private final String environmentVersion;

    private final String os;

    private final String osVersion;

    private final String pluginName;

    private final String pluginVersion;

    public RepositoryTelemetry(
        final String repositoryManagerId,
        final String repositoryId,
        final String repositoryFormat,
        final boolean enabled,
        final boolean quarantineEnabled,
        final String repositoryManagerName,
        final String repositoryManagerEdition,
        final String repositoryManagerVersion,
        final String environment,
        final String environmentVersion,
        final String os,
        final String osVersion,
        final String pluginName,
        final String pluginVersion
    )
    {
      this.repositoryManagerId = repositoryManagerId;
      this.repositoryId = repositoryId;
      this.repositoryFormat = repositoryFormat;
      this.enabled = enabled;
      this.quarantineEnabled = quarantineEnabled;
      this.repositoryManagerName = repositoryManagerName;
      this.repositoryManagerEdition = repositoryManagerEdition;
      this.repositoryManagerVersion = repositoryManagerVersion;
      this.environment = environment;
      this.environmentVersion = environmentVersion;
      this.os = os;
      this.osVersion = osVersion;
      this.pluginName = pluginName;
      this.pluginVersion = pluginVersion;
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

    public String getRepositoryManagerName() {
      return repositoryManagerName;
    }

    public String getRepositoryManagerEdition() {
      return repositoryManagerEdition;
    }

    public String getRepositoryManagerVersion() {
      return repositoryManagerVersion;
    }

    public String getEnvironment() {
      return environment;
    }

    public String getEnvironmentVersion() {
      return environmentVersion;
    }

    public String getOs() {
      return os;
    }

    public String getOsVersion() {
      return osVersion;
    }

    public String getPluginName() {
      return pluginName;
    }

    public String getPluginVersion() {
      return pluginVersion;
    }
  }
}
