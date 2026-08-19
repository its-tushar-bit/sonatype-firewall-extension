/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.ci;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.sonatype.clm.dto.model.ci.config.ApiCiConfigurationDto;
import com.sonatype.clm.dto.model.ci.config.ApiCiConfigurationResponseDto;
import com.sonatype.clm.dto.model.ci.config.DotNetAnalysisConfig;
import com.sonatype.clm.dto.model.ci.config.DownloadConfig;
import com.sonatype.clm.dto.model.ci.config.JavaAnalysisConfig;
import com.sonatype.clm.dto.model.ci.config.JavaScriptAnalysisConfig;
import com.sonatype.clm.dto.model.ci.config.ProxyConfig;
import com.sonatype.clm.dto.model.ci.config.ReachabilityConfig;
import com.sonatype.insight.brain.model.Application;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.CiIntegrationsConfigDao;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.configuration.CiIntegrationsConfig;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.nexus.git.utils.repository.RepositoryUrlFinderUtils.maskCredentialsFromUrl;

/**
 * Service for managing CI integration configurations with hierarchical inheritance.
 * Configurations can be set at the organization or application level and are merged
 * from the organization hierarchy with lower levels taking precedence.
 *
 * @since 1.201
 */
@Named
@Singleton
public class CiConfigurationService
{
  private static final Logger log = LoggerFactory.getLogger(CiConfigurationService.class);

  private final CiIntegrationsConfigDao ciConfigDao;

  private final OrganizationDAO organizationDAO;

  private final ApplicationDAO applicationDAO;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Inject
  public CiConfigurationService(
      final CiIntegrationsConfigDao ciConfigDao,
      final OrganizationDAO organizationDAO,
      final ApplicationDAO applicationDAO)
  {
    this.ciConfigDao = ciConfigDao;
    this.organizationDAO = organizationDAO;
    this.applicationDAO = applicationDAO;
  }

  /**
   * Retrieves CI configuration for the specified owner.
   *
   * @param ownerType the type of owner (APPLICATION or ORGANIZATION)
   * @param ownerId the internal ID of the owner
   * @param direct if true, returns only the direct configuration; if false, returns merged configuration from hierarchy
   * @return the configuration response with optional provenance tracking
   */
  @Authorize(permission = Permission.READ)
  public ApiCiConfigurationResponseDto getConfiguration(
      @AuthzContext(Key.TYPE) final OwnerType ownerType,
      @AuthzContext(Key.INTERNAL_ID) final String ownerId,
      final boolean direct)
  {
    log.debug("Getting CI configuration for {}/{} (direct={})", ownerType, ownerId, direct);

    if (direct) {
      return getDirectConfiguration(ownerType, ownerId);
    }
    else {
      return getMergedConfiguration(ownerType, ownerId);
    }
  }

  /**
   * Creates or updates CI configuration for the specified owner.
   *
   * @param ownerType the type of owner (APPLICATION or ORGANIZATION)
   * @param ownerId the internal ID of the owner
   * @param configuration the configuration to save
   * @return the saved configuration
   */
  @Authorize(permission = Permission.WRITE)
  public ApiCiConfigurationDto setConfiguration(
      @AuthzContext(Key.TYPE) final OwnerType ownerType,
      @AuthzContext(Key.INTERNAL_ID) final String ownerId,
      final ApiCiConfigurationDto configuration)
  {
    log.debug("Setting CI configuration for {}/{}", ownerType, ownerId);

    CiConfigurationValidator.validateConfiguration(configuration);

    final String configJson = serializeConfiguration(configuration);

    final CiIntegrationsConfig entity = new CiIntegrationsConfig(
        ownerId, ownerType.toString(), configJson);
    ciConfigDao.save(entity);

    auditConfigurationUpdate(configuration);

    log.info("CI configuration saved for {}/{}", ownerType, ownerId);
    return configuration;
  }

  /**
   * Deletes CI configuration for the specified owner.
   *
   * @param ownerType the type of owner (APPLICATION or ORGANIZATION)
   * @param ownerId the internal ID of the owner
   */
  @Authorize(permission = Permission.WRITE)
  public void deleteConfiguration(
      @AuthzContext(Key.TYPE) final OwnerType ownerType,
      @AuthzContext(Key.INTERNAL_ID) final String ownerId)
  {
    log.debug("Deleting CI configuration for {}/{}", ownerType, ownerId);

    final Optional<CiIntegrationsConfig> existing = ciConfigDao.findByOwner(
        ownerType.toString(), ownerId);

    if (existing.isEmpty()) {
      throw new NotFoundException("CI configuration not found for " + ownerType + "/" + ownerId);
    }

    ciConfigDao.delete(ownerType.toString(), ownerId);

    log.info("CI configuration deleted for {}/{}", ownerType, ownerId);
  }

  private ApiCiConfigurationResponseDto getDirectConfiguration(
      final OwnerType ownerType,
      final String internalOwnerId)
  {
    final Optional<CiIntegrationsConfig> config = ciConfigDao.findByOwner(
        ownerType.toString(), internalOwnerId);

    if (config.isEmpty()) {
      throw new NotFoundException("CI configuration not found");
    }

    final ApiCiConfigurationDto dto = deserializeConfiguration(config.get().getConfigurationJson());

    ApiCiConfigurationResponseDto response = new ApiCiConfigurationResponseDto();
    response.setData(dto);
    return response;
  }

  private ApiCiConfigurationResponseDto getMergedConfiguration(
      final OwnerType ownerType,
      final String internalOwnerId)
  {
    final List<String> hierarchyIds = getHierarchyOwnerIds(internalOwnerId, ownerType);

    final List<CiIntegrationsConfig> configs = ciConfigDao.findByOwnerList(hierarchyIds);

    if (configs.isEmpty()) {
      throw new NotFoundException("No CI configuration found in hierarchy");
    }

    return mergeConfigurations(configs, hierarchyIds, internalOwnerId, ownerType);
  }

  private List<String> getHierarchyOwnerIds(final String ownerId, final OwnerType ownerType) {
    final List<String> hierarchyIds = new ArrayList<>();

    if (ownerType == OwnerType.APPLICATION) {
      hierarchyIds.add(ownerId); // The query won't include the application itself, so add it manually
      final List<Organization> parentOrgs = organizationDAO.getAllParentOrganizations(ownerId, OwnerType.APPLICATION);
      for (final Organization org : parentOrgs) {
        hierarchyIds.add(org.getId());
      }
    }
    else if (ownerType == OwnerType.ORGANIZATION) {
      final List<Organization> allOrgs = organizationDAO.getAllParentOrganizations(ownerId, OwnerType.ORGANIZATION);
      for (final Organization org : allOrgs) {
        hierarchyIds.add(org.getId());
      }
    }
    else {
      throw new BadRequestException("Unsupported owner type: " + ownerType);
    }

    return hierarchyIds;
  }

  /**
   * Merges CI configurations from an organization hierarchy with provenance tracking.
   *
   * <p>
   * Configurations are merged from the top of the hierarchy (root organization) down to the
   * specific owner (application or organization), with lower levels taking precedence. This means
   * a configuration field set at the application level will override the same field set at any
   * parent organization level.
   *
   * @param configs the list of configuration entities found in the hierarchy
   * @param hierarchyIds the ordered list of owner IDs from closest to farthest (application first, then parent orgs)
   * @param internalOwnerId the internal ID of the requesting owner (used for public ID conversion)
   * @param type the type of the requesting owner (APPLICATION or ORGANIZATION)
   * @return response containing the merged configuration and a source map indicating provenance
   */
  private ApiCiConfigurationResponseDto mergeConfigurations(
      final List<CiIntegrationsConfig> configs,
      final List<String> hierarchyIds,
      final String internalOwnerId,
      final OwnerType type)
  {
    // Create a map of internalId -> config for a quick lookup
    final Map<String, CiIntegrationsConfig> configMap = configs.stream()
        .collect(Collectors.toMap(CiIntegrationsConfig::getOwnerId, config -> config));

    // Merged result and provenance tracking (using internal IDs)
    ApiCiConfigurationDto mergedConfig = new ApiCiConfigurationDto();
    final Map<String, String> sourceWithInternalIds = new HashMap<>();

    // Walk hierarchy from the farthest ancestor to the closest (reverse order)
    // This way, closer ancestors override farther ones
    for (int i = hierarchyIds.size() - 1; i >= 0; i--) {
      final String ownerId = hierarchyIds.get(i);
      final CiIntegrationsConfig config = configMap.get(ownerId);

      if (config != null) {
        final ApiCiConfigurationDto levelDto = deserializeConfiguration(config.getConfigurationJson());
        overrideConfiguration(mergedConfig, levelDto, ownerId, sourceWithInternalIds);
      }
    }

    // Convert application internal ID to public ID in the source map so it can differentiate from org ids
    final Map<String, String> source = convertSourceMapToPublicIds(sourceWithInternalIds, internalOwnerId, type);

    ApiCiConfigurationResponseDto response = new ApiCiConfigurationResponseDto();
    response.setData(mergedConfig);
    response.setSource(source);
    return response;
  }

  /**
   * Applies non-null fields from the override configuration to the base configuration.
   *
   * <p>
   * For each configuration field in the override object, if the field is non-null, it will:
   * <ol>
   * <li>Override the corresponding field in the base configuration</li>
   * <li>Record the owner ID as the source of that field in the provenance map</li>
   * </ol>
   *
   * <p>
   * For nested objects (proxy, download, reachability), performs deep merging at the field level,
   * allowing child configurations to override specific fields while inheriting others from parent.
   *
   * @param base the base configuration to be modified (accumulated merged result)
   * @param override the configuration to apply (from one level of the hierarchy)
   * @param internalId the owner ID to record as the source for overridden fields
   * @param source the provenance map tracking which owner contributed each field
   */
  void overrideConfiguration(
      ApiCiConfigurationDto base,
      ApiCiConfigurationDto override,
      String internalId,
      Map<String, String> source)
  {
    if (override.getParameterPriority() != null) {
      base.setParameterPriority(override.getParameterPriority());
      source.put("parameterPriority", internalId);
    }
    if (override.getScanPatterns() != null) {
      base.setScanPatterns(override.getScanPatterns());
      source.put("scanPatterns", internalId);
    }
    if (override.getModuleExcludes() != null) {
      base.setModuleExcludes(override.getModuleExcludes());
      source.put("moduleExcludes", internalId);
    }
    if (override.getEnableDebugLogging() != null) {
      base.setEnableDebugLogging(override.getEnableDebugLogging());
      source.put("enableDebugLogging", internalId);
    }
    if (override.getFailBuildOnNetworkError() != null) {
      base.setFailBuildOnNetworkError(override.getFailBuildOnNetworkError());
      source.put("failBuildOnNetworkError", internalId);
    }
    if (override.getFailBuildOnScanningErrors() != null) {
      base.setFailBuildOnScanningErrors(override.getFailBuildOnScanningErrors());
      source.put("failBuildOnScanningErrors", internalId);
    }
    if (override.getFailBuildOnReachabilityErrors() != null) {
      base.setFailBuildOnReachabilityErrors(override.getFailBuildOnReachabilityErrors());
      source.put("failBuildOnReachabilityErrors", internalId);
    }
    if (override.getFailBuildOnPolicyWarnings() != null) {
      base.setFailBuildOnPolicyWarnings(override.getFailBuildOnPolicyWarnings());
      source.put("failBuildOnPolicyWarnings", internalId);
    }
    if (override.getUnstableBuildOnPolicyWarnings() != null) {
      base.setUnstableBuildOnPolicyWarnings(override.getUnstableBuildOnPolicyWarnings());
      source.put("unstableBuildOnPolicyWarnings", internalId);
    }
    if (override.getAdvancedProperties() != null) {
      base.setAdvancedProperties(override.getAdvancedProperties());
      source.put("advancedProperties", internalId);
    }
    if (override.getResultFile() != null) {
      base.setResultFile(override.getResultFile());
      source.put("resultFile", internalId);
    }
    if (override.getSarifFile() != null) {
      base.setSarifFile(override.getSarifFile());
      source.put("sarifFile", internalId);
    }
    // Deep merge nested configurations
    mergeProxyConfig(base, override.getProxy(), internalId, source);
    mergeDownloadConfig(base, override.getDownload(), internalId, source);
    mergeReachabilityConfig(base, override.getReachability(), internalId, source);
  }

  /**
   * Deep merges proxy configuration fields.
   *
   * @param base the base configuration containing the proxy to merge into
   * @param overrideProxy the proxy configuration to merge from (it may be null)
   * @param internalId the owner ID for provenance tracking
   * @param source the provenance map
   */
  private void mergeProxyConfig(
      ApiCiConfigurationDto base,
      ProxyConfig overrideProxy,
      String internalId,
      Map<String, String> source)
  {
    if (overrideProxy == null) {
      return;
    }

    ProxyConfig baseProxy = base.getProxy();
    if (baseProxy == null) {
      baseProxy = new ProxyConfig();
      base.setProxy(baseProxy);
    }

    if (overrideProxy.getHost() != null) {
      baseProxy.setHost(overrideProxy.getHost());
      source.put("proxy.host", internalId);
    }
  }

  /**
   * Deep merges download configuration fields.
   *
   * @param base the base configuration containing the download config to merge into
   * @param overrideDownload the download configuration to merge from (it may be null)
   * @param internalId the owner ID for provenance tracking
   * @param source the provenance map
   */
  private void mergeDownloadConfig(
      ApiCiConfigurationDto base,
      DownloadConfig overrideDownload,
      String internalId,
      Map<String, String> source)
  {
    if (overrideDownload == null) {
      return;
    }

    DownloadConfig baseDownload = base.getDownload();
    if (baseDownload == null) {
      baseDownload = new DownloadConfig();
      base.setDownload(baseDownload);
    }

    if (overrideDownload.getIqCliUrl() != null) {
      baseDownload.setIqCliUrl(overrideDownload.getIqCliUrl());
      source.put("download.iqCliUrl", internalId);
    }
    if (overrideDownload.getIqCliVersion() != null) {
      baseDownload.setIqCliVersion(overrideDownload.getIqCliVersion());
      source.put("download.iqCliVersion", internalId);
    }
  }

  /**
   * Deep merges reachability configuration fields, including 2nd level nested configs.
   *
   * @param base the base configuration containing the reachability config to merge into
   * @param overrideReachability the reachability configuration to merge from (it may be null)
   * @param internalId the owner ID for provenance tracking
   * @param source the provenance map
   */
  private void mergeReachabilityConfig(
      ApiCiConfigurationDto base,
      ReachabilityConfig overrideReachability,
      String internalId,
      Map<String, String> source)
  {
    if (overrideReachability == null) {
      return;
    }

    ReachabilityConfig baseReachability = base.getReachability();
    if (baseReachability == null) {
      baseReachability = new ReachabilityConfig();
      base.setReachability(baseReachability);
    }

    if (overrideReachability.getFailOnError() != null) {
      baseReachability.setFailOnError(overrideReachability.getFailOnError());
      source.put("reachability.failOnError", internalId);
    }

    // Deep merge JavaAnalysisConfig (2nd level nesting)
    mergeJavaAnalysisConfig(baseReachability, overrideReachability.getJavaAnalysis(), internalId, source);

    // Deep merge JavaScriptAnalysisConfig (2nd level nesting)
    mergeJavaScriptAnalysisConfig(baseReachability, overrideReachability.getJavaScriptAnalysis(), internalId, source);

    // Deep merge DotNetAnalysisConfig (2nd level nesting)
    mergeDotNetAnalysisConfig(baseReachability, overrideReachability.getDotNetAnalysis(), internalId, source);
  }

  /**
   * Deep merges Java analysis configuration fields.
   *
   * @param baseReachability the base reachability config containing the Java analysis to merge into
   * @param overrideJava the Java analysis configuration to merge from (it may be null)
   * @param internalId the owner ID for provenance tracking
   * @param source the provenance map
   */
  private void mergeJavaAnalysisConfig(
      ReachabilityConfig baseReachability,
      JavaAnalysisConfig overrideJava,
      String internalId,
      Map<String, String> source)
  {
    if (overrideJava == null) {
      return;
    }

    JavaAnalysisConfig baseJava = baseReachability.getJavaAnalysis();
    if (baseJava == null) {
      baseJava = new JavaAnalysisConfig();
      baseReachability.setJavaAnalysis(baseJava);
    }

    if (overrideJava.getEnabled() != null) {
      baseJava.setEnabled(overrideJava.getEnabled());
      source.put("reachability.javaAnalysis.enabled", internalId);
    }
    if (overrideJava.getEntrypointStrategy() != null) {
      baseJava.setEntrypointStrategy(overrideJava.getEntrypointStrategy());
      source.put("reachability.javaAnalysis.entrypointStrategy", internalId);
    }
    if (overrideJava.getNamespaces() != null) {
      baseJava.setNamespaces(overrideJava.getNamespaces());
      source.put("reachability.javaAnalysis.namespaces", internalId);
    }
  }

  /**
   * Deep merges JavaScript analysis configuration fields.
   *
   * @param baseReachability the base reachability config containing the JS analysis to merge into
   * @param overrideJs the JavaScript analysis configuration to merge from (it may be null)
   * @param internalId the owner ID for provenance tracking
   * @param source the provenance map
   */
  private void mergeJavaScriptAnalysisConfig(
      ReachabilityConfig baseReachability,
      JavaScriptAnalysisConfig overrideJs,
      String internalId,
      Map<String, String> source)
  {
    if (overrideJs == null) {
      return;
    }

    JavaScriptAnalysisConfig baseJs = baseReachability.getJavaScriptAnalysis();
    if (baseJs == null) {
      baseJs = new JavaScriptAnalysisConfig();
      baseReachability.setJavaScriptAnalysis(baseJs);
    }

    if (overrideJs.getEnabled() != null) {
      baseJs.setEnabled(overrideJs.getEnabled());
      source.put("reachability.javaScriptAnalysis.enabled", internalId);
    }
    if (overrideJs.getProjectRoot() != null) {
      baseJs.setProjectRoot(overrideJs.getProjectRoot());
      source.put("reachability.javaScriptAnalysis.projectRoot", internalId);
    }
    if (overrideJs.getNodeJsExecutable() != null) {
      baseJs.setNodeJsExecutable(overrideJs.getNodeJsExecutable());
      source.put("reachability.javaScriptAnalysis.nodeJsExecutable", internalId);
    }
    if (overrideJs.getJsSources() != null) {
      baseJs.setJsSources(overrideJs.getJsSources());
      source.put("reachability.javaScriptAnalysis.jsSources", internalId);
    }
    if (overrideJs.getJsExcludes() != null) {
      baseJs.setJsExcludes(overrideJs.getJsExcludes());
      source.put("reachability.javaScriptAnalysis.jsExcludes", internalId);
    }
  }

  /**
   * Deep merges .NET analysis configuration fields.
   *
   * @param baseReachability the base reachability config containing the .NET analysis to merge into
   * @param overrideDotNet the .NET analysis configuration to merge from (it may be null)
   * @param internalId the owner ID for provenance tracking
   * @param source the provenance map
   */
  private void mergeDotNetAnalysisConfig(
      ReachabilityConfig baseReachability,
      DotNetAnalysisConfig overrideDotNet,
      String internalId,
      Map<String, String> source)
  {
    if (overrideDotNet == null) {
      return;
    }

    DotNetAnalysisConfig baseDotNet = baseReachability.getDotNetAnalysis();
    if (baseDotNet == null) {
      baseDotNet = new DotNetAnalysisConfig();
      baseReachability.setDotNetAnalysis(baseDotNet);
    }

    if (overrideDotNet.getEnabled() != null) {
      baseDotNet.setEnabled(overrideDotNet.getEnabled());
      source.put("reachability.dotNetAnalysis.enabled", internalId);
    }
    if (overrideDotNet.getNamespaces() != null) {
      baseDotNet.setNamespaces(overrideDotNet.getNamespaces());
      source.put("reachability.dotNetAnalysis.namespaces", internalId);
    }
    if (overrideDotNet.getEntrypointStrategy() != null) {
      baseDotNet.setEntrypointStrategy(overrideDotNet.getEntrypointStrategy());
      source.put("reachability.dotNetAnalysis.entrypointStrategy", internalId);
    }
    if (overrideDotNet.getDotnetPath() != null) {
      baseDotNet.setDotnetPath(overrideDotNet.getDotnetPath());
      source.put("reachability.dotNetAnalysis.dotnetPath", internalId);
    }
  }

  /**
   * Converts the source map values from internal IDs to public IDs.
   * For organizations, internal ID == public ID, so no conversion needed.
   * For applications, we need to convert internal ID to public ID.
   */
  private Map<String, String> convertSourceMapToPublicIds(
      final Map<String, String> sourceWithInternalIds,
      final String internalOwnerId,
      final OwnerType type)
  {
    if (type != OwnerType.APPLICATION) {
      return sourceWithInternalIds;
    }

    final Application app = applicationDAO.getById(internalOwnerId);
    if (app == null) {
      return sourceWithInternalIds;
    }

    // Convert any source value that matches the application's internal ID to its public ID
    final Map<String, String> result = new HashMap<>();
    for (Map.Entry<String, String> entry : sourceWithInternalIds.entrySet()) {
      String value = entry.getValue();
      if (value.equals(internalOwnerId)) {
        value = app.getPublicId();
      }
      result.put(entry.getKey(), value);
    }
    return result;
  }

  private String serializeConfiguration(final ApiCiConfigurationDto config) {
    try {
      return objectMapper.writeValueAsString(config);
    }
    catch (JsonProcessingException e) {
      throw new BadRequestException("Invalid configuration format: " + e.getMessage(), e);
    }
  }

  private ApiCiConfigurationDto deserializeConfiguration(final String json) {
    try {
      return objectMapper.readValue(json, ApiCiConfigurationDto.class);
    }
    catch (JsonProcessingException e) {
      throw new BadRequestException("Invalid configuration JSON: " + e.getMessage(), e);
    }
  }

  private void auditConfigurationUpdate(final ApiCiConfigurationDto config) {
    if (config.getDownload() != null && !StringUtils.isBlank(config.getDownload().getIqCliUrl())) {
      config.getDownload().setIqCliUrl(maskCredentialsFromUrl(config.getDownload().getIqCliUrl()));
    }
    AuditData.get().setData("ciConfiguration", config);
  }
}
