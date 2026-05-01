/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scanhealth;

import java.io.IOException;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ScanHealthConfigDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.configuration.scanhealth.ScanHealthConfig;
import com.sonatype.insight.brain.model.configuration.scanhealth.ScanHealthConfigDTO;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.model.OwnerType.APPLICATION;
import static com.sonatype.insight.brain.model.OwnerType.ORGANIZATION;

import java.util.List;

/**
 * Service for managing Scan Health configuration with hierarchical inheritance.
 */
@Named
@Singleton
public class ScanHealthService
{
  private static final Logger log = LoggerFactory.getLogger(ScanHealthService.class);

  public static final String SCAN_FAILED_ZERO_COMPONENTS_DETECTED_MESSAGE =
      "Scan failed: zero components detected. This may indicate a scan misconfiguration.";

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final ScanHealthConfigDAO scanHealthConfigDAO;

  private final OrganizationDAO organizationDAO;

  @Inject
  public ScanHealthService(
      final ScanHealthConfigDAO scanHealthConfigDAO,
      final OrganizationDAO organizationDAO)
  {
    this.scanHealthConfigDAO = scanHealthConfigDAO;
    this.organizationDAO = organizationDAO;
  }

  private String serializeConfig(final ScanHealthConfigDTO config) {
    if (config == null) {
      return null;
    }
    try {
      return OBJECT_MAPPER.writeValueAsString(config);
    }
    catch (IOException e) {
      throw new BadRequestException("Invalid Scan Health configuration: " + e.getMessage(), e);
    }
  }

  private ScanHealthConfigDTO deserializeConfig(final String json) {
    if (json == null || json.isEmpty()) {
      return new ScanHealthConfigDTO();
    }
    try {
      return OBJECT_MAPPER.readValue(json, ScanHealthConfigDTO.class);
    }
    catch (IOException e) {
      log.warn("Failed to deserialize Scan Health configuration: {}", json, e);
      return new ScanHealthConfigDTO();
    }
  }

  /**
   * Get configuration for a specific owner (direct, no inheritance).
   */
  @Authorize(permission = Permission.READ)
  public ScanHealthConfigDTO getConfiguration(
      @AuthzContext(AuthzContext.Key.TYPE) final OwnerType ownerType,
      @AuthzContext(AuthzContext.Key.INTERNAL_ID) final String ownerId)
  {
    log.debug("Getting Scan Health configuration for {}/{}", ownerType, ownerId);

    Optional<ScanHealthConfig> config = scanHealthConfigDAO.findByOwner(ownerType.toString(), ownerId);

    return config
        .map(c -> deserializeConfig(c.getConfigurationJson()))
        .orElse(new ScanHealthConfigDTO());
  }

  /**
   * Set configuration for a specific owner.
   */
  @Authorize(permission = Permission.WRITE)
  public ScanHealthConfigDTO saveConfiguration(
      @AuthzContext(AuthzContext.Key.TYPE) final OwnerType ownerType,
      @AuthzContext(AuthzContext.Key.INTERNAL_ID) final String ownerId,
      final ScanHealthConfigDTO config)
  {
    log.debug("Setting Scan Health configuration for {}/{}", ownerType, ownerId);

    if (config == null) {
      throw new BadRequestException("Scan Health configuration body must not be null.");
    }

    ScanHealthConfig entity = new ScanHealthConfig(
        ownerId,
        ownerType.toString(),
        serializeConfig(config));

    scanHealthConfigDAO.save(entity);

    log.info("Scan Health configuration saved for {}/{}", ownerType, ownerId);

    return config;
  }

  /**
   * Delete configuration for a specific owner.
   */
  @Authorize(permission = Permission.WRITE)
  public void deleteConfiguration(
      @AuthzContext(AuthzContext.Key.TYPE) final OwnerType ownerType,
      @AuthzContext(AuthzContext.Key.INTERNAL_ID) final String ownerId)
  {
    log.debug("Deleting Scan Health configuration for {}/{}", ownerType, ownerId);

    final Optional<ScanHealthConfig> existing = scanHealthConfigDAO.findByOwner(
        ownerType.toString(), ownerId);

    if (existing.isEmpty()) {
      throw new NotFoundException("Scan Health configuration not found for " + ownerType + "/" + ownerId);
    }

    scanHealthConfigDAO.delete(ownerType.toString(), ownerId);
    log.info("Scan Health configuration deleted for {}/{}", ownerType, ownerId);
  }

  /**
   * Get the effective configuration considering inheritance. Priority: Application -> Organization hierarchy
   * (bottom-up) -> Default (disabled)
   *
   * <p>
   * <strong>Internal use only.</strong> This method bypasses authorization checks and is intended solely
   * for use by the scan evaluation flow where authorization is handled at the evaluation layer.
   * Do not expose this method through REST endpoints or other external interfaces without adding
   * appropriate authorization.
   *
   * @param applicationId the application ID (required)
   * @return the effective configuration
   */
  public ScanHealthConfigDTO getEffectiveConfig(final String applicationId) {
    // 1. Check application-level config
    if (applicationId != null) {
      Optional<ScanHealthConfig> appConfig = scanHealthConfigDAO.findByOwner(APPLICATION.toString(), applicationId);
      if (appConfig.isPresent()) {
        ScanHealthConfigDTO dto = deserializeConfig(appConfig.get().getConfigurationJson());
        if (dto.failOnZeroComponents() != null) {
          log.debug("Using application-level Scan Health config for app {}", applicationId);
          return dto;
        }
      }
    }

    // 2. Walk up the organization hierarchy (from closest parent to root)
    if (applicationId != null) {
      final List<Organization> parentOrgs = organizationDAO.getAllParentOrganizations(applicationId, APPLICATION);
      for (final Organization org : parentOrgs) {
        Optional<ScanHealthConfig> orgConfig = scanHealthConfigDAO.findByOwner(ORGANIZATION.toString(), org.getId());
        if (orgConfig.isPresent()) {
          ScanHealthConfigDTO dto = deserializeConfig(orgConfig.get().getConfigurationJson());
          if (dto.failOnZeroComponents() != null) {
            log.debug("Inheriting Scan Health config from org {} in hierarchy", org.getId());
            return dto;
          }
        }
      }
    }

    // 3. Default: disabled
    log.debug("Using default Scan Health config (disabled)");
    return new ScanHealthConfigDTO();
  }

  /**
   * Fail a scan if configured to do so when zero components are detected.
   *
   * <p>
   * Uses BadRequestException (HTTP 400) to match the pattern used by other scan validation failures
   * in ScanPolicyEvaluator (e.g., REEVALUATE_NOT_ALLOWED_FOR_OUT_OF_DATE_SCAN_MESSAGE). While HTTP 422
   * (Unprocessable Entity) might be semantically more precise, HTTP 400 is the established convention
   * in this codebase for post-input-validation failures in the scan evaluation flow.
   *
   * @param application the application being scanned
   * @throws BadRequestException if the feature is enabled and zero components were detected
   */
  public void failOnEvaluateResultContainingZeroComponentsIfConfigured(final Application application) {
    if (shouldFailOnZeroComponents(application)) {
      throw new BadRequestException(SCAN_FAILED_ZERO_COMPONENTS_DETECTED_MESSAGE);
    }
  }

  public boolean shouldFailOnZeroComponents(final Application application) {
    return shouldFailOnZeroComponents(application.getId());
  }

  /**
   * Check if a scan should fail due to zero components.
   *
   * @param applicationId the internal ID of the application
   * @return true if the scan should fail (feature enabled AND zero components)
   */
  public boolean shouldFailOnZeroComponents(final String applicationId) {
    final ScanHealthConfigDTO config = getEffectiveConfig(applicationId);
    final boolean shouldFail = Boolean.TRUE.equals(config.failOnZeroComponents());

    if (shouldFail) {
      log.info("Scan failing due to zero components for application {}", applicationId);
    }

    return shouldFail;
  }

}
