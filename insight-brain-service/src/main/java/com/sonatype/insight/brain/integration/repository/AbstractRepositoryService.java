/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration.repository;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList.ComponentEvaluationData;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.ProprietaryComponentNames;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataList;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList.RepositoryComponentEvaluationDataRequest;
import com.sonatype.clm.dto.model.component.UnquarantinedComponentList;
import com.sonatype.clm.dto.model.policy.RepositoryPolicyEvaluationSummary;
import com.sonatype.clm.dto.model.repository.ConfigureRepositoriesRequest;
import com.sonatype.clm.dto.model.repository.QuarantinedComponentReport;
import com.sonatype.clm.dto.model.repository.RepositoryDTO;
import com.sonatype.clm.dto.model.repository.RepositoryType;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryDTO;
import com.sonatype.insight.brain.api.v2.service.ApiRepositoryAdapter;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditSession;
import com.sonatype.insight.brain.container.images.ContainerImageReportService;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.hds.FirewallQuarantineHdsClient;
import com.sonatype.insight.brain.landing.UserInterfaceLinksHelper;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.HashHelper;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.PolicyViolationSummary;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.repository.ProprietaryComponentNamePattern;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.organization.ApplicationService;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLogEvent;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLoggerFactory;
import com.sonatype.insight.brain.policy.violation.RepositoryPolicyViolationLogger;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.repository.ContainerImageSummaryDTO;
import com.sonatype.insight.brain.repository.ProprietaryComponentNameDetector;
import com.sonatype.insight.brain.repository.RepositoryPolicyEvaluator;
import com.sonatype.insight.brain.repository.RepositoryService;
import com.sonatype.insight.brain.repository.RequestSafeComponentsMetricEventService;
import com.sonatype.insight.brain.repository.component.DbQuarantinedComponentAccessManager;
import com.sonatype.insight.brain.repository.component.QuarantinedComponentAccessManager;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.brain.telemetry.RepositoryComponentTelemetry.RepositoryComponentTelemetryEventType;
import com.sonatype.insight.brain.telemetry.RepositoryComponentTelemetryCreator;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.lqa.LqaComponentIdentifier;
import com.sonatype.insight.telemetry.SonatypeUserAgentUtil;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.stream.Collectors.toMap;

public abstract class AbstractRepositoryService
{
  private static final Logger log = LoggerFactory.getLogger(AbstractRepositoryService.class);

  static final String HDS_COMPONENT_METADATA_PATH = "rest/component/details/firewall/allVersions";

  private final RepositoryManagerDAO repositoryManagerDAO;

  protected final RepositoryDAO repositoryDAO;

  protected final RepositoryComponentDAO repositoryComponentDAO;

  private final RepositoryPolicyViolationDAO repositoryPolicyViolationDAO;

  protected final ProductLicense productLicense;

  private final RepositoryPolicyEvaluator repositoryPolicyEvaluator;

  private final ProprietaryComponentNameDetector proprietaryComponentNameDetector;

  private final PolicyViolationLoggerFactory policyViolationLoggerFactory;

  private final RepositoryComponentTelemetryCreator repositoryComponentTelemetryCreator;

  private final QuarantinedComponentAccessManager quarantinedComponentAccessManager;

  private final FirewallQuarantineHdsClient quarantineHdsClient;

  private final TelemetrySender telemetrySender;

  private final FirewallIgnorePatternService firewallIgnorePatternService;

  private final ApplicationDAO applicationDAO;

  private final ApplicationService applicationService;

  private final RepositoryService repositoryService;

  private final ContainerImageReportService containerImageReportService;

  static final String REPOSITORY_COMPONENT_REQUESTED_VERSION_COUNT = "repository_component_requested_version_count";

  static final String REPOSITORY_COMPONENT_POLICY_COMPLIANT_VERSION_COUNT =
      "repository_component_policy_compliant_version_count";

  static final String REPOSITORY_COMPONENT_METADATA_EVALUATION_TIME = "repository_component_metadata_evaluation_time";

  // Visible for tests
  final LicensedFeature requiredFeature;

  private final RequestSafeComponentsMetricEventService requestSafeComponentsMetricEventService;

  @Inject
  public AbstractRepositoryService(
      RepositoryPolicyEvaluator repositoryPolicyEvaluator,
      ProprietaryComponentNameDetector proprietaryComponentNameDetector,
      ProductLicense productLicense,
      PolicyViolationLoggerFactory policyViolationLoggerFactory,
      LicensedFeature requiredFeature,
      RepositoryComponentTelemetryCreator repositoryComponentTelemetryCreator,
      DbQuarantinedComponentAccessManager quarantinedComponentAccessManager,
      FirewallQuarantineHdsClient quarantineHdsClient,
      ApplicationDAO applicationDAO,
      ApplicationService applicationService,
      TelemetrySender telemetrySender,
      RepositoryManagerDAO repositoryManagerDAO,
      RepositoryDAO repositoryDAO,
      RepositoryComponentDAO repositoryComponentDAO,
      RepositoryPolicyViolationDAO repositoryPolicyViolationDAO,
      FirewallIgnorePatternService firewallIgnorePatternService,
      RequestSafeComponentsMetricEventService requestSafeComponentsMetricEventService,
      RepositoryService repositoryService,
      ContainerImageReportService containerImageReportService)
  {
    this.repositoryPolicyEvaluator = repositoryPolicyEvaluator;
    this.proprietaryComponentNameDetector = proprietaryComponentNameDetector;
    this.productLicense = productLicense;
    this.policyViolationLoggerFactory = policyViolationLoggerFactory;
    this.requiredFeature = requiredFeature;
    this.repositoryComponentTelemetryCreator = repositoryComponentTelemetryCreator;
    this.quarantinedComponentAccessManager = quarantinedComponentAccessManager;
    this.quarantineHdsClient = quarantineHdsClient;
    this.telemetrySender = telemetrySender;
    this.applicationDAO = applicationDAO;
    this.applicationService = applicationService;
    this.repositoryManagerDAO = repositoryManagerDAO;
    this.repositoryDAO = repositoryDAO;
    this.repositoryComponentDAO = repositoryComponentDAO;
    this.repositoryPolicyViolationDAO = repositoryPolicyViolationDAO;
    this.firewallIgnorePatternService = firewallIgnorePatternService;
    this.requestSafeComponentsMetricEventService = requestSafeComponentsMetricEventService;
    this.repositoryService = repositoryService;
    this.containerImageReportService = containerImageReportService;
  }

  protected RepositoryManagerDAO getRepositoryManagerDAO() {
    return repositoryManagerDAO;
  }

  protected void checkLicenseFeature() {
    productLicense.validateFeature(requiredFeature);
  }

  private void auditComponentPath(final String pathname) {
    AuditData.get().setData("componentPathname", pathname);
  }

  RepositoryPolicyEvaluationSummary getPolicyEvaluationSummary(
      final String repositoryManagerInstanceId,
      final String repositoryPublicId,
      final String clientUserAgent)
  {
    checkLicenseFeature();
    final Repository repository = getRepository(repositoryManagerInstanceId, repositoryPublicId, clientUserAgent);

    return getPolicyEvaluationSummary(repository);
  }

  String getRepositoryResultsUrl(
      final String repositoryManagerInstanceId,
      final String repositoryPublicId,
      final String clientUserAgent)
  {
    checkLicenseFeature();
    final Repository repository =
        getRepository(repositoryManagerInstanceId, repositoryPublicId, clientUserAgent);

    return getRepositoryResultsUrl(repository);
  }

  private Repository getRepository(
      final String repositoryManagerInstanceId,
      final String repositoryPublicId,
      final String clientUserAgent)
  {
    Repository repository = repositoryDAO.getByRepositoryManagerInstanceIdAndPublicIdNotNull(
        repositoryManagerInstanceId, repositoryPublicId);
    validateIsProxyRepository(repository);

    if (!repository.isAuditEnabled()) {
      throw new BadRequestException("Repository " + repositoryPublicId + " is disabled.");
    }

    updateUserAgent(clientUserAgent, repository);
    return repository;
  }

  @Authorize(permission = Permission.EVALUATE_COMPONENT)
  RepositoryPolicyEvaluationSummary getPolicyEvaluationSummary(
      @AuthzContext(Key.REPOSITORY) final Repository repository)
  {
    return getPolicyEvaluationSummaryInternal(repository);
  }

  @Authorize(permission = Permission.EVALUATE_COMPONENT)
  String getRepositoryResultsUrl(@AuthzContext(Key.REPOSITORY) final Repository repository) {
    return UserInterfaceLinksHelper.getRepositoryReportUrl(repository.getId());
  }

  ApiRepositoryDTO setAuditEnabled(
      String repositoryManagerInstanceId,
      String repositoryPublicId,
      boolean auditEnabled,
      final String clientUserAgent)
  {
    AuditData.get()
        .setRepositoryManagerInstanceId(repositoryManagerInstanceId)
        .setRepositoryPublicId(repositoryPublicId);
    checkLicenseFeature();

    log.debug("{} audit for repository {}:{}", auditEnabled ? "Enabling" : "Disabling", repositoryManagerInstanceId,
        repositoryPublicId);

    Repository repository = repositoryDAO.getByRepositoryManagerInstanceIdAndPublicId(repositoryManagerInstanceId,
        repositoryPublicId);
    if (repository == null) {
      String repositoryManagerId = null;
      RepositoryManager repositoryManager = repositoryManagerDAO.getByInstanceId(repositoryManagerInstanceId);
      if (repositoryManager != null) {
        repositoryManagerId = repositoryManager.getId();
      }
      repository = new Repository(repositoryManagerId, repositoryPublicId);
    }
    else {
      validateIsProxyRepository(repository);
    }
    setAuditEnabled(repositoryManagerInstanceId, repository, auditEnabled);
    if (!auditEnabled) {
      policyViolationLoggerFactory.newLogger(new Date(), repository).logClearEvent();
    }

    updateUserAgent(clientUserAgent, repository);

    log.info("{} audit for repository {}:{} ({})", auditEnabled ? "Enabled" : "Disabled", repositoryManagerInstanceId,
        repositoryPublicId, repository.getId());

    return ApiRepositoryAdapter.convert(repository);
  }

  @Authorize(permission = Permission.EVALUATE_COMPONENT)
  void setAuditEnabled(
      String repositoryManagerInstanceId,
      @AuthzContext(Key.REPOSITORY) Repository repository,
      boolean enable)
  {
    repository.setAuditEnabled(enable);
    if (repository.getId() == null) {
      RepositoryManager repositoryManager = getOrCreateRepositoryManager(repositoryManagerInstanceId);
      repository.setRepositoryManagerId(repositoryManager.getId());
      repositoryDAO.insert(repository);
    }
    else {
      repositoryDAO.update(repository);
    }
    AuditData.get().setRepository(repository);
  }

  // synchronized to avoid race between enable requests from different repos of the same instance
  protected synchronized RepositoryManager getOrCreateRepositoryManager(String repositoryManagerInstanceId) {
    RepositoryManager repositoryManager = repositoryManagerDAO.getByInstanceId(repositoryManagerInstanceId);
    if (repositoryManager == null) {
      repositoryManager = new RepositoryManager(repositoryManagerInstanceId);
      repositoryManagerDAO.insert(repositoryManager);
    }
    return repositoryManager;
  }

  void setQuarantine(
      final String repositoryManagerInstanceId,
      final String repositoryPublicId,
      final boolean enabled,
      final String clientUserAgent)
  {
    AuditData.get().setData("quarantine", enabled ? "enabled" : "disabled");
    checkLicenseFeature();

    log.debug("{} quarantine for repository {}:{}", enabled ? "Enabling" : "Disabling", repositoryManagerInstanceId,
        repositoryPublicId);

    Repository repository = repositoryDAO.getByRepositoryManagerInstanceIdAndPublicIdNotNull(
        repositoryManagerInstanceId, repositoryPublicId);
    validateIsProxyRepository(repository);

    if (enabled && !repository.isAuditEnabled()) {
      throw new BadRequestException("Cannot enable quarantine when repository " + repositoryPublicId + " is disabled.");
    }

    setQuarantine(repository, enabled);

    updateUserAgent(clientUserAgent, repository);

    log.info("{} quarantine for repository {}:{} ({})", enabled ? "Enabled" : "Disabled", repositoryManagerInstanceId,
        repositoryPublicId, repository.getId());
  }

  @Authorize(permission = Permission.EVALUATE_COMPONENT)
  void setQuarantine(@AuthzContext(Key.REPOSITORY) final Repository repository, final boolean enabled) {
    repository.setQuarantineEnabled(enabled);
    repositoryDAO.update(repository);
  }

  RepositoryComponentEvaluationDataList evaluateComponents(
      String repositoryManagerInstanceId,
      String repositoryPublicId,
      RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList,
      boolean withQuarantine,
      final String clientUserAgent)
  {
    auditRepoComponentEvalList(componentEvaluationDataRequestList);
    checkLicenseFeature();

    Repository repository = repositoryDAO.getByRepositoryManagerInstanceIdAndPublicIdNotNull(
        repositoryManagerInstanceId, repositoryPublicId);
    validateIsProxyRepository(repository);

    log.debug("Evaluating components for repository {}:{} ({}) with quarantine {}", repositoryManagerInstanceId,
        repositoryPublicId, repository.getId(), withQuarantine);

    return evaluateComponents(repository, repositoryManagerInstanceId, componentEvaluationDataRequestList,
        withQuarantine, true, clientUserAgent);
  }

  /**
   * Evaluates policies on variants of the same component.
   * The specified componentEvaluationDataRequestList must contain only variants of the same component
   * Only the npm and pypi formats are supported.
   *
   * It is very important for performance to minimize the number of round trips between:
   * - IQ and HDS
   * - IQ and the IQ ODS db
   * - HDS and HDS dm db
   *
   * How it works:
   * - NXRM sends a list of hash+pathname pairs (all for the same component name) to IQ for policy evaluation.
   * - IQ picks up one hash+pathname pair and sends it to HDS.
   * - HDS finds the component identifier and name for the hash+pathname pair,
   * retrieves all variants for the component name and all the data associated with the variants (licenses, SVs, etc).
   * - IQ matches the data from HDS to the data from NXRM by hash+filename, runs policy evaluation for all variants,
   * determines which components would be quarantined and returns the results to NXRM.
   *
   * @since 1.133
   */
  RepositoryComponentEvaluationDataList evaluateComponentMetadata(
      String repositoryManagerInstanceId,
      String repositoryPublicId,
      RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList,
      String clientUserAgent)
  {
    checkLicenseFeature();

    Repository repository = repositoryDAO
        .getByRepositoryManagerInstanceIdAndPublicIdNotNull(repositoryManagerInstanceId, repositoryPublicId);
    validateIsProxyRepository(repository);

    log.debug("Evaluating component metadata for repository {}:{} ({})", repositoryManagerInstanceId,
        repositoryPublicId, repository.getId());

    return evaluateComponentMetadata(repository, componentEvaluationDataRequestList, clientUserAgent);
  }

  /**
   * Evaluates policies on variants of the same component.
   * The specified componentEvaluationDataRequestList must contain only variants of the same component
   * Only the npm and pypi formats are supported.
   *
   * It is very important for performance to minimize the number of round trips between:
   * - IQ and HDS
   * - IQ and the IQ ODS db
   * - HDS and HDS dm db
   *
   * How it works:
   * - NXRM sends a list of hash+pathname pairs (all for the same component name) to IQ for policy evaluation.
   * - IQ picks up one hash+pathname pair and sends it to HDS.
   * - HDS finds the component identifier and name for the hash+pathname pair,
   * retrieves all variants for the component name and all the data associated with the variants (licenses, SVs, etc).
   * - IQ matches the data from HDS to the data from NXRM by hash+filename, runs policy evaluation for all variants,
   * determines which components would be quarantined and returns the results to NXRM.
   *
   * @since 1.133
   */
  @Authorize(permission = Permission.EVALUATE_COMPONENT)
  RepositoryComponentEvaluationDataList evaluateComponentMetadata(
      @AuthzContext(Key.REPOSITORY) Repository repository,
      RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList,
      String clientUserAgent)
  {
    long start = System.currentTimeMillis();

    if (!repository.isAuditEnabled() || !repository.isQuarantineEnabled()) {
      throw new BadRequestException("The repository must be enabled in quarantine mode.");
    }

    if (componentEvaluationDataRequestList == null || componentEvaluationDataRequestList.isEmpty()) {
      return new RepositoryComponentEvaluationDataList();
    }
    validateComponentMetadataEvaluateRequest(componentEvaluationDataRequestList);

    String format = componentEvaluationDataRequestList.components.get(0).format;
    normalizeComponents(componentEvaluationDataRequestList);
    if (!ComponentIdentifier.FORMAT_NPM.equals(format) && !ComponentIdentifier.FORMAT_PYPI.equals(format)) {
      throw new BadRequestException("The repository format must be " + ComponentIdentifier.FORMAT_NPM + " or "
          + ComponentIdentifier.FORMAT_PYPI + ".");
    }

    if (StringUtils.isBlank(repository.getFormat())) {
      repository.setFormat(componentEvaluationDataRequestList.components.get(0).format);
      repositoryDAO.update(repository);
    }

    // HDS will return data for all versions/variants for the pathname,
    // so it doesn't matter which pathname we send to HDS.
    String pathname = componentEvaluationDataRequestList.components.get(0).pathname;
    // The hash is missing (aka null) for PyPI
    String hash = componentEvaluationDataRequestList.components.get(0).hash;
    ComponentEvaluationDataList componentDetailsFromHds =
        getComponentMetadataFromHds(repository.getFormat(), pathname, hash, clientUserAgent);
    // for (ComponentEvaluationData c : componentDetailsFromHds.components) {
    // System.err.println("From HDS: " + c.componentIdentifier + ", " + c.hash + ", " + c.filename);
    // }
    componentDetailsFromHds = matchHdsComponentDetailsToRequestListByHashAndFilename(componentDetailsFromHds,
        componentEvaluationDataRequestList);
    RepositoryComponentEvaluationDataList result =
        repositoryPolicyEvaluator.evaluate(repository, componentEvaluationDataRequestList, componentDetailsFromHds,
            true /* withQuarantine */, false /* persistEvaluationResults */, false /* forMonitoring */);

    int policyCompliantVersionCount = 0;

    for (int i = 0; i < componentEvaluationDataRequestList.components.size(); i++) {
      log.trace("Path {}: {}", componentEvaluationDataRequestList.components.get(i).pathname,
          result.componentEvalResults.get(i).quarantine);

      if (!result.componentEvalResults.get(i).quarantine) {
        policyCompliantVersionCount++;
      }
    }
    updateUserAgent(clientUserAgent, repository);

    log.debug("Evaluated component metadata for repository {}:{} ({}) for {} components in {} ms.",
        repository.getRepositoryManagerId(), repository.getPublicId(), repository.getId(),
        result.componentEvalResults.size(), System.currentTimeMillis() - start);

    sendTelemetry(componentEvaluationDataRequestList.components.size(), policyCompliantVersionCount,
        System.currentTimeMillis() - start);
    requestSafeComponentsMetricEventService.postRequestSafeComponentsMetricEvent();

    return result;
  }

  private ComponentEvaluationDataList matchHdsComponentDetailsToRequestListByHashAndFilename(
      ComponentEvaluationDataList componentDetailsFromHdsList,
      RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList)
  {
    ComponentEvaluationDataList result = new ComponentEvaluationDataList();

    // See https://sonatype.atlassian.net/browse/CLM-27246
    // At least for PyPI, there are binaries with the same hash and filename published under different coordinates.
    // The function below selects the component with the smallest component identifier in case of duplicates.
    // The choice for "smallest component identifier" is just a way to ensure reproducible/consistent results (it is not
    // a business rule or requirement).
    BinaryOperator<ComponentEvaluationData> mapMergeDeduplicator =
        (componentEvaluationData1, componentEvaluationData2) -> {
          return componentEvaluationData1.componentIdentifier
              .compareTo(componentEvaluationData2.componentIdentifier) <= 0
                  ? componentEvaluationData1
                  : componentEvaluationData2;
        };
    Map<String, ComponentEvaluationData> componentDetailsFromHdsByHashAndFilename =
        componentDetailsFromHdsList.components.stream()
            .collect(toMap(componentEvaluationData -> toHashFilenameKey(componentEvaluationData), Function.identity(),
                mapMergeDeduplicator));

    for (int requestIndex = 0; requestIndex < componentEvaluationDataRequestList.components.size(); requestIndex++) {
      RepositoryComponentEvaluationDataRequest componentEvaluationDataRequest =
          componentEvaluationDataRequestList.components.get(requestIndex);
      String filename = Paths.get(componentEvaluationDataRequest.pathname).getFileName().toString();
      ComponentEvaluationData componentDetailsFromHds =
          componentDetailsFromHdsByHashAndFilename.get(componentEvaluationDataRequest.hash + "|" + filename);
      if (componentDetailsFromHds == null) {
        // There are no HDS details for this pathname.
        // Add an entry for the unknown component, so it will be included in the policy evaluation.
        componentDetailsFromHds = new ComponentEvaluationData();
        componentDetailsFromHds.declaredLicenses = Collections.emptySet();
        componentDetailsFromHds.observedLicenses = Collections.emptySet();
        componentDetailsFromHds.securityVulnerabilities = Collections.emptyList();
        componentDetailsFromHds.matchState = MatchState.UNKNOWN.toString();
        componentDetailsFromHdsList.components.add(componentDetailsFromHds);
      }
      componentDetailsFromHds.hash = componentEvaluationDataRequest.hash;
      componentDetailsFromHds.requestIndex = requestIndex;

      result.components.add(componentDetailsFromHds);
    }

    return result;
  }

  private static String toHashFilenameKey(ComponentEvaluationData componentEvaluationData) {
    String hash = ComponentIdentifier.FORMAT_PYPI.equals(componentEvaluationData.componentIdentifier.getFormat())
        ? null
        : componentEvaluationData.hash;
    return hash + "|" + componentEvaluationData.filename;
  }

  private ComponentEvaluationDataList getComponentMetadataFromHds(
      String format,
      String pathname,
      String hash,
      String clientUserAgent)
  {
    long start = System.currentTimeMillis();

    Map<String, String> queryParams = new HashMap<>();
    queryParams.put("format", format);
    queryParams.put("pathname", pathname);
    queryParams.put("hash", hash);
    ComponentEvaluationDataList result = quarantineHdsClient.get(ComponentEvaluationDataList.class,
        HDS_COMPONENT_METADATA_PATH, clientUserAgent, queryParams);

    log.debug("Got component details (all versions) from HDS for {} components in {} ms.", result.components.size(),
        System.currentTimeMillis() - start);

    return result;
  }

  protected void auditRepoComponentEvalList(RepositoryComponentEvaluationDataRequestList repoComponentEvalList) {
    if (repoComponentEvalList != null) {
      AuditData.get()
          .setData("componentCount",
              repoComponentEvalList.components == null ? 0 : repoComponentEvalList.components.size());
      if (repoComponentEvalList.cause != null) {
        AuditData.get().setData("evaluationCause", repoComponentEvalList.cause.replace('_', '-'));
      }
    }
  }

  private void normalizeComponents(RepositoryComponentEvaluationDataRequestList componentEvalRequestList) {
    for (RepositoryComponentEvaluationDataRequest componentEvalRequest : componentEvalRequestList.components) {
      truncateHash(componentEvalRequest);
      componentEvalRequest.pathname = normalizePathname(componentEvalRequest.pathname);
    }
  }

  private void truncateHash(final RepositoryComponentEvaluationDataRequest componentEvaluationDataRequest) {
    componentEvaluationDataRequest.hash = HashHelper.truncateHash(componentEvaluationDataRequest.hash);
  }

  private void validateComponentEvaluateRequest(RepositoryComponentEvaluationDataRequestList componentEvalRequestList) {
    List<RepositoryComponentEvaluationDataRequest> validComponents = new ArrayList<>();

    for (RepositoryComponentEvaluationDataRequest componentEvalRequest : componentEvalRequestList.components) {
      validateFormatAndHash(componentEvalRequest, false /* allowNullHash */);

      if (hasValidPathname(componentEvalRequest)) {
        validComponents.add(componentEvalRequest);
      }
      else {
        logInvalidPathname(componentEvalRequest);
      }
    }

    // Replace the list reference instead of modifying in place to avoid UnsupportedOperationException
    componentEvalRequestList.components = validComponents;
  }

  private void validateComponentMetadataEvaluateRequest(
      RepositoryComponentEvaluationDataRequestList componentEvalRequestList)
  {
    if (componentEvalRequestList.components.isEmpty()) {
      return;
    }

    // Hashes are null for PyPI
    boolean allowNullHash = ComponentIdentifier.FORMAT_PYPI.equals(componentEvalRequestList.components.get(0).format);

    for (RepositoryComponentEvaluationDataRequest componentEvalRequest : componentEvalRequestList.components) {
      validateEvaluateRequest(componentEvalRequest, allowNullHash);
    }
  }

  private void validateEvaluateRequest(
      final RepositoryComponentEvaluationDataRequest componentEvaluationDataRequest,
      boolean allowNullHash)
  {
    if (componentEvaluationDataRequest == null) {
      throw new BadRequestException("The componentEvaluationDataRequest cannot be null.");
    }
    if (StringUtils.isBlank(componentEvaluationDataRequest.pathname)) {
      throw new BadRequestException("The pathname cannot be null or empty.");
    }
    if (StringUtils.isBlank(componentEvaluationDataRequest.format)) {
      throw new BadRequestException("The format cannot be null or empty.");
    }
    if (!allowNullHash
        && StringUtils.isBlank(componentEvaluationDataRequest.hash))
    {
      throw new BadRequestException("The hash cannot be null or empty.");
    }
  }

  private void validateFormatAndHash(
      final RepositoryComponentEvaluationDataRequest componentEvaluationDataRequest,
      boolean allowNullHash)
  {
    if (componentEvaluationDataRequest == null) {
      throw new BadRequestException("The componentEvaluationDataRequest cannot be null.");
    }
    if (StringUtils.isBlank(componentEvaluationDataRequest.format)) {
      throw new BadRequestException("The format cannot be null or empty.");
    }
    if (!allowNullHash && StringUtils.isBlank(componentEvaluationDataRequest.hash)) {
      throw new BadRequestException("The hash cannot be null or empty.");
    }
  }

  private boolean hasValidPathname(final RepositoryComponentEvaluationDataRequest componentEvaluationDataRequest) {
    return componentEvaluationDataRequest != null
        && !StringUtils.isBlank(componentEvaluationDataRequest.pathname);
  }

  private void logInvalidPathname(final RepositoryComponentEvaluationDataRequest componentEvaluationDataRequest) {
    log.info("Skipping component evaluation due to validation failure. " +
        "Hash: {}, Format: {}, Pathname: {}, Reason: pathname is null or empty",
        componentEvaluationDataRequest != null ? componentEvaluationDataRequest.hash : "null",
        componentEvaluationDataRequest != null ? componentEvaluationDataRequest.format : "null",
        componentEvaluationDataRequest != null ? componentEvaluationDataRequest.pathname : "null");
  }

  @Authorize(permission = Permission.EVALUATE_COMPONENT)
  public RepositoryComponentEvaluationDataList evaluateComponents(
      @AuthzContext(Key.REPOSITORY) Repository repository,
      String repositoryManagerInstanceId,
      RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList,
      final boolean withQuarantine,
      final boolean persistEvaluationResults,
      final String clientUserAgent)
  {
    long start = System.currentTimeMillis();

    validateRepositoryMatchesRepositoryManager(repository, repositoryManagerInstanceId);

    String format = null;
    if (componentEvaluationDataRequestList != null && componentEvaluationDataRequestList.components != null
        && !componentEvaluationDataRequestList.components.isEmpty())
    {
      format = componentEvaluationDataRequestList.components.get(0).format;
    }

    configureAuditAndQuarantineInRepository(null, repositoryManagerInstanceId, repository, format, withQuarantine,
        persistEvaluationResults);

    updateUserAgent(clientUserAgent, repository);

    if (componentEvaluationDataRequestList == null || componentEvaluationDataRequestList.isEmpty()) {
      return new RepositoryComponentEvaluationDataList();
    }
    validateComponentEvaluateRequest(componentEvaluationDataRequestList);

    normalizeComponents(componentEvaluationDataRequestList);

    RepositoryComponentEvaluationDataList result = repositoryPolicyEvaluator.evaluate(repository,
        componentEvaluationDataRequestList, withQuarantine, persistEvaluationResults, clientUserAgent,
        false /* forMonitoring */);

    log.debug("Evaluated {} components with quarantine {} for repository {}:{} ({}) because of {} in {} ms.",
        componentEvaluationDataRequestList.components.size(), withQuarantine, repository.getRepositoryManagerId(),
        repository.getPublicId(), repository.getId(), componentEvaluationDataRequestList.cause,
        System.currentTimeMillis() - start);

    return result;
  }

  public void configureAuditAndQuarantineInRepository(
      TransactionContext tx,
      String repositoryManagerInstanceId,
      Repository repository,
      String format,
      boolean withQuarantine,
      boolean persistEvaluationResults)
  {
    boolean needToUpdateRepository = false;

    if (!repository.isAuditEnabled() || (withQuarantine && !repository.isQuarantineEnabled())
        || repository.getFormat() == null)
    {
      if (!repository.isAuditEnabled() && persistEvaluationResults) {
        log.info("Enabled audit for repository {}:{} ({})", repository.getRepositoryManagerId(),
            repository.getPublicId(), repository.getId());
        repository.setAuditEnabled(true);
        needToUpdateRepository = true;
        try (AuditSession auditSession = AuditData.get().recordSubEvent(AuditEvent.CONNECT_REPOSITORY, false)) {
          AuditData.get().setRepository(repository).setData("repositoryManagerInstanceId", repositoryManagerInstanceId);
        }
      }
      if (withQuarantine && persistEvaluationResults) {
        if (!repository.isQuarantineEnabled()) {
          log.info("Enabled quarantine for repository {}:{} ({})", repository.getRepositoryManagerId(),
              repository.getPublicId(), repository.getId());
          repository.setQuarantineEnabled(true);
          needToUpdateRepository = true;
          try (AuditSession auditSession = AuditData.get().recordSubEvent(AuditEvent.CONFIGURE_QUARANTINE, false)) {
            AuditData.get().setRepository(repository).setData("quarantine", "enabled");
          }
        }
      }
      if (format != null && !format.equals(repository.getFormat())) {
        repository.setFormat(format);
        needToUpdateRepository = true;
      }

      if (needToUpdateRepository) {
        if (tx != null) {
          repositoryDAO.update(tx, repository);
        }
        else {
          repositoryDAO.update(repository);
        }
        AuditData.get().commitSubEvents();
      }
    }
  }

  private void validateRepositoryMatchesRepositoryManager(
      final Repository repository,
      final String repositoryManagerInstanceId)
  {
    RepositoryManager repositoryManager = repositoryManagerDAO.getByIdNotNull(repository.getRepositoryManagerId());
    if (!repositoryManagerInstanceId.equals(repositoryManager.getInstanceId())) {
      throw new BadRequestException(
          String.format("Repository '%s' and Repository Manager '%s' not found", repository.getId(),
              repositoryManager.getId()));
    }
  }

  private RepositoryPolicyEvaluationSummary getPolicyEvaluationSummaryInternal(final Repository repository) {
    RepositoryPolicyEvaluationSummary policyEvaluationSummary = new RepositoryPolicyEvaluationSummary();

    if ("docker".equals(repository.getFormat()) && RepositoryType.proxy == repository.getRepositoryType()) {
      ContainerImageSummaryDTO containerImageSummaryDTO =
          containerImageReportService.getContainerImagesSummaryNoAuthz(repository.getId());

      policyEvaluationSummary.setCriticalComponentCount((int) containerImageSummaryDTO.criticalViolationCount);
      policyEvaluationSummary.setSevereComponentCount((int) containerImageSummaryDTO.severeViolationCount);
      policyEvaluationSummary.setModerateComponentCount((int) containerImageSummaryDTO.moderateViolationCount);
      policyEvaluationSummary.setAffectedComponentCount((int) containerImageSummaryDTO.affectedContainerImageCount);
      policyEvaluationSummary
          .setQuarantinedComponentCount((int) containerImageSummaryDTO.quarantinedContainerImageCount);
    }
    else {
      PolicyViolationSummary summary = repositoryPolicyViolationDAO.getPolicyViolationSummary(repository.getId());

      policyEvaluationSummary.setCriticalComponentCount(summary.getCriticalCount());
      policyEvaluationSummary.setSevereComponentCount(summary.getSevereCount());
      policyEvaluationSummary.setModerateComponentCount(summary.getModerateCount());
      policyEvaluationSummary.setAffectedComponentCount(summary.getAffectedComponentCount());
      policyEvaluationSummary.setQuarantinedComponentCount(
          repositoryComponentDAO.getQuarantinedComponentCountByRepositoryId(repository.getId()));
    }

    policyEvaluationSummary.setReportUrl(UserInterfaceLinksHelper.getRepositoryReportUrl(repository.getId()));
    return policyEvaluationSummary;
  }

  void removeComponent(
      String repositoryManagerInstanceId,
      String repositoryPublicId,
      String pathname,
      final String clientUserAgent)
  {
    checkLicenseFeature();

    Repository repository = repositoryDAO.getByRepositoryManagerInstanceIdAndPublicIdNotNull(
        repositoryManagerInstanceId, repositoryPublicId);
    validateIsProxyRepository(repository);

    updateUserAgent(clientUserAgent, repository);

    removeComponent(repository, normalizePathname(pathname));
  }

  private void updateUserAgent(String clientUserAgent, Repository repository) {
    if (StringUtils.isNotBlank(clientUserAgent) && SonatypeUserAgentUtil.parse(clientUserAgent) != null) {
      RepositoryManager repositoryManager = repositoryManagerDAO.getById(repository.getRepositoryManagerId());
      repositoryManager.setUserAgent(clientUserAgent);
      repositoryManagerDAO.update(repositoryManager);
    }
  }

  private void updateUserAgent(String clientUserAgent, RepositoryManager repositoryManager) {
    if (StringUtils.isNotBlank(clientUserAgent) && SonatypeUserAgentUtil.parse(clientUserAgent) != null) {
      repositoryManager.setUserAgent(clientUserAgent);
      repositoryManagerDAO.update(repositoryManager);
    }
  }

  @Authorize(permission = Permission.EVALUATE_COMPONENT)
  void removeComponent(@AuthzContext(Key.REPOSITORY) Repository repository, String pathname) {
    if (!repository.isAuditEnabled()) {
      repository.setAuditEnabled(true);
      repositoryDAO.update(repository);
    }

    if (repository.getFormat() != null && repository.getFormat().equals("docker") &&
        repository.getRepositoryType().equals(RepositoryType.proxy))
    {
      Application application = applicationDAO.getByPublicId(pathname);
      if (application == null) {
        // do nothing, the container image was already deleted
        return;
      }
      String applicationOrganizationId = application.getOrganizationId();
      // do nothing if the relationship between the organization and the repository is not found
      if (!repository.getRelatedOrganizationId().equals(applicationOrganizationId)) {
        return;
      }
      try {
        applicationService.deleteApplicationByPublicId(pathname);
        return;
      }
      catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }

    RepositoryComponent repositoryComponent = repositoryComponentDAO.getByRepositoryIdAndPathname(repository.getId(),
        pathname);
    if (repositoryComponent != null) {
      RepositoryPolicyViolationLogger repositoryPolicyViolationLogger = policyViolationLoggerFactory
          .newLogger(new Date(), repository);
      List<RepositoryPolicyViolation> repositoryPolicyViolations;
      try (TransactionContext tx = repositoryComponentDAO.createTransactionContext()) {
        tx.begin();
        repositoryPolicyViolations = repositoryPolicyViolationDAO
            .getActiveByRepositoryIdAndPathname(tx, repositoryComponent.getRepositoryId(),
                repositoryComponent.getPathname());
        repositoryPolicyViolationDAO.loadConstraintFacts(repositoryPolicyViolations);
        for (RepositoryPolicyViolation policyViolation : repositoryPolicyViolations) {
          repositoryPolicyViolationDAO.delete(tx, policyViolation);
          repositoryPolicyViolationLogger.add(PolicyViolationLogEvent.FIX, policyViolation);
        }
        repositoryComponentDAO.delete(tx, repositoryComponent);
        tx.commit();
      }
      repositoryPolicyViolationLogger.log();
      if (repositoryComponent.isQuarantined()) {
        try (AuditSession auditSession = AuditData.get().recordSubEvent(AuditEvent.RESET_QUARANTINE, true)) {
          AuditData.get().setRepository(repository).setComponentHash(repositoryComponent.getHash());
          auditComponentPath(repositoryComponent.getPathname());
        }
      }

      if (!repositoryPolicyViolations.isEmpty()) {
        repositoryComponentTelemetryCreator
            .sendRepositoryComponentTelemetry(repositoryComponent, repositoryPolicyViolations,
                repository.getRepositoryManagerId(), RepositoryComponentTelemetryEventType.DELETE);
      }
    }
  }

  public static String normalizePathname(String pathname) {
    if (pathname != null && pathname.startsWith("/")) {
      return pathname.substring(1);
    }

    return pathname;
  }

  UnquarantinedComponentList getUnquarantinedComponents(
      String repositoryManagerInstanceId,
      String repositoryPublicId,
      long sinceUtcTimestamp,
      final String clientUserAgent)
  {
    checkLicenseFeature();

    Repository repository = repositoryDAO.getByRepositoryManagerInstanceIdAndPublicIdNotNull(
        repositoryManagerInstanceId, repositoryPublicId);
    validateIsProxyRepository(repository);

    updateUserAgent(clientUserAgent, repository);

    log.debug("Getting unquarantined components for repository {}:{} ({}), since {}.", repositoryManagerInstanceId,
        repositoryPublicId, repository.getId(), sinceUtcTimestamp);

    return getUnquarantinedComponents(repository, sinceUtcTimestamp);
  }

  @Authorize(permission = Permission.EVALUATE_COMPONENT)
  UnquarantinedComponentList getUnquarantinedComponents(
      @AuthzContext(Key.REPOSITORY) Repository repository,
      long sinceUtcTimestamp)
  {
    long start = System.currentTimeMillis();

    UnquarantinedComponentList result = new UnquarantinedComponentList();
    List<RepositoryComponent> unquarantinedComponents = repositoryComponentDAO.getUnquarantinedByRepositoryId(
        repository.getId(), new Date(sinceUtcTimestamp));

    for (RepositoryComponent unquarantinedComponent : unquarantinedComponents) {
      result.pathnames.add(unquarantinedComponent.getPathname());
    }

    log.debug("Retrieved {} unquarantined components for repository ID {} in {} ms.", result.pathnames.size(),
        repository.getId(), System.currentTimeMillis() - start);

    return result;
  }

  public void addProprietaryNamespaceNames(
      final String repositoryManagerInstanceId,
      final String repositoryPublicId,
      final String format,
      final List<String> namespaces)
  {
    if (namespaces == null || namespaces.isEmpty()) {
      throw new BadRequestException("namespaces must be provided.");
    }
    else if (format == null) {
      throw new BadRequestException("format must be provided.");
    }
    else if (!validateFormat(format)) {
      throw new BadRequestException(String.format("'%s' format is not supported.", format));
    }

    ProprietaryComponentNames proprietaryComponentNames = new ProprietaryComponentNames(format);
    proprietaryComponentNames.addNamespaces(namespaces);

    AuditData.get().setData("repositoryPublicId", repositoryPublicId);
    addProprietaryComponentNames(repositoryManagerInstanceId, repositoryPublicId, proprietaryComponentNames);
  }

  private boolean validateFormat(final String format) {
    return firewallIgnorePatternService.getIgnorePatterns().regexpsByRepositoryFormat.containsKey(format);
  }

  public void removeProprietaryNamespaceNames(
      final String repositoryManagerInstanceId,
      final String repositoryPublicId)
  {
    checkLicenseFeature();

    AuditData.get().setData("repositoryPublicId", repositoryPublicId);
    removeProprietaryComponentNames(repositoryManagerInstanceId, repositoryPublicId);
  }

  public void addProprietaryComponentNames(
      String repositoryManagerInstanceId,
      String repositoryPublicId,
      ProprietaryComponentNames proprietaryComponentNames)
  {
    AuditData.get().setRepositoryManagerInstanceId(repositoryManagerInstanceId);
    checkLicenseFeature();

    if (proprietaryComponentNames == null) {
      throw new BadRequestException("No component name patterns specified");
    }
    if (StringUtils.isBlank(proprietaryComponentNames.format)) {
      throw new BadRequestException("No component format specified");
    }

    Repository repository =
        repositoryDAO.getByRepositoryManagerInstanceIdAndPublicId(repositoryManagerInstanceId, repositoryPublicId);
    if (repository == null) {
      String repositoryManagerId = null;
      RepositoryManager repositoryManager = repositoryManagerDAO.getByInstanceId(repositoryManagerInstanceId);
      if (repositoryManager != null) {
        repositoryManagerId = repositoryManager.getId();
      }
      repository = new Repository(repositoryManagerId, repositoryPublicId);
      repository.setAuditEnabled(false);
      repository.setRepositoryType(RepositoryType.hosted);
      repository.setFormat(proprietaryComponentNames.format);
      repository.setNamespaceConfusionProtectionEnabled(true);
    }
    else {
      validateIsHostedRepository(repository);
      if (!repository.getFormat().equals(proprietaryComponentNames.format)) {
        throw new BadRequestException("Format '" + proprietaryComponentNames.format
            + "' does not match the repository format '" + repository.getFormat() + "'.");
      }
    }

    addProprietaryComponentNames(repositoryManagerInstanceId, repository, proprietaryComponentNames);
  }

  @Authorize(permission = Permission.EVALUATE_COMPONENT)
  void addProprietaryComponentNames(
      String repositoryManagerInstanceId,
      @AuthzContext(Key.REPOSITORY) Repository repository,
      ProprietaryComponentNames proprietaryComponentNames)
  {
    if (proprietaryComponentNames == null || (CollectionUtils.isEmpty(proprietaryComponentNames.namespaces) &&
        CollectionUtils.isEmpty(proprietaryComponentNames.names)))
    {
      throw new BadRequestException("No component name patterns specified");
    }

    validatePatterns(proprietaryComponentNames.namespaces, "namespace");
    validatePatterns(proprietaryComponentNames.names, "name");

    if (repository.getId() == null) {
      RepositoryManager repositoryManager = getOrCreateRepositoryManager(repositoryManagerInstanceId);
      repository.setRepositoryManagerId(repositoryManager.getId());
      repositoryDAO.insert(repository);
    }

    String format = translateRepositoryFormat(proprietaryComponentNames.format);
    List<ProprietaryComponentNamePattern> patterns = addPatterns(repository, format, proprietaryComponentNames);

    int added = proprietaryComponentNameDetector.addPatterns(format, patterns);
    AuditData.get().setData("addedPatternCount", added);
  }

  private void validatePatterns(final Set<String> components, final String type) {
    if (components != null) {
      for (String component : components) {
        validatePattern(type, component);
      }
    }
  }

  private List<ProprietaryComponentNamePattern> addPatterns(
      Repository repository,
      String format,
      ProprietaryComponentNames proprietaryComponentNames)
  {
    List<ProprietaryComponentNamePattern> patterns = new ArrayList<>();
    if (proprietaryComponentNames.namespaces != null) {
      for (String namespace : proprietaryComponentNames.namespaces) {
        patterns.add(new ProprietaryComponentNamePattern(repository.getId(), format).withNamespacePattern(namespace));
      }
    }
    if (proprietaryComponentNames.names != null) {
      for (String name : proprietaryComponentNames.names) {
        patterns.add(new ProprietaryComponentNamePattern(repository.getId(), format).withNamePattern(name));
      }
    }
    return patterns;
  }

  private void validatePattern(String type, String pattern) {
    if (StringUtils.isBlank(pattern)) {
      throw new BadRequestException("Empty component " + type + " pattern");
    }
    if (pattern.length() > 300) {
      throw new BadRequestException(
          "Component " + pattern + " is too long. Maximum length is 300 characters.");
    }
    int first = pattern.indexOf('*');
    int next = pattern.indexOf('*', first + 1);
    if (first >= 0 && (next >= 0 || (first > 0 && first < pattern.length() - 1) || pattern.length() == 1)) {
      throw new BadRequestException("Invalid component " + type + " pattern: " + pattern);
    }
  }

  public static String translateRepositoryFormat(String format) {
    switch (format) {
      case "apk":
        return LqaComponentIdentifier.FORMAT_ALPINE;
      case "apt":
        return LqaComponentIdentifier.FORMAT_DEBIAN;
      case "go":
        return ComponentIdentifier.FORMAT_GOLANG;
      case "maven2":
        return ComponentIdentifier.FORMAT_MAVEN;
      case "r":
        return ComponentIdentifier.FORMAT_CRAN;
      case "rubygems":
        return ComponentIdentifier.FORMAT_RUBYGEMS;
      case "yum":
        return ComponentIdentifier.FORMAT_RPM;
      default:
        return format;
    }
  }

  public void removeProprietaryComponentNames(String repositoryManagerInstanceId, String repositoryPublicId) {
    AuditData.get().setRepositoryManagerInstanceId(repositoryManagerInstanceId);

    Repository repository =
        repositoryDAO.getByRepositoryManagerInstanceIdAndPublicId(repositoryManagerInstanceId, repositoryPublicId);
    if (repository != null) {
      validateIsHostedRepository(repository);
      checkEvaluateComponentPermission(repository);
      proprietaryComponentNameDetector.removePatterns(repository.getId());
    }
  }

  public QuarantinedComponentReport getQuarantinedComponentReportUrl(
      final String repositoryManagerInstanceId,
      final String repositoryPublicId,
      final String pathname,
      final String clientUserAgent)
  {
    Repository repository = repositoryDAO.getByRepositoryManagerInstanceIdAndPublicIdNotNull(
        repositoryManagerInstanceId, repositoryPublicId);

    if (repository == null) {
      throw new BadRequestException(String
          .format("Cannot find repository for repository manager %s and public id %s", repositoryManagerInstanceId,
              repositoryPublicId));
    }
    validateIsProxyRepository(repository);

    updateUserAgent(clientUserAgent, repository);

    return getQuarantinedComponentReportUrl(repository, pathname);
  }

  @Authorize(permission = Permission.EVALUATE_COMPONENT)
  public QuarantinedComponentReport getQuarantinedComponentReportUrl(
      @AuthzContext(Key.REPOSITORY) final Repository repository,
      final String pathname)
  {
    if (repository == null) {
      throw new BadRequestException("Specified repository cannot be null");
    }

    RepositoryComponent repositoryComponent =
        repositoryComponentDAO.getByRepositoryIdAndPathname(repository.getId(), normalizePathname(pathname));

    if (repositoryComponent == null) {
      throw new NotFoundException(String
          .format("Repository component for repository %s and pathname %s does not exist", repository.getPublicId(),
              pathname));
    }

    final String token = quarantinedComponentAccessManager.createToken(repositoryComponent);
    final QuarantinedComponentReport quarantinedComponentReport = new QuarantinedComponentReport();
    quarantinedComponentReport.setReportUrl(UserInterfaceLinksHelper.getQuarantinedComponentReportPath(token));
    return quarantinedComponentReport;
  }

  private void sendTelemetry(
      final int requestedVersionCount,
      final int policyCompliantVersionCount,
      final long evaluationTime)
  {
    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.REPOSITORY_COMPONENT_METADATA_EVALUATION);

    Map<String, Object> attributes = telemetryData.getAttributes();
    attributes.put(REPOSITORY_COMPONENT_REQUESTED_VERSION_COUNT, requestedVersionCount);
    attributes.put(REPOSITORY_COMPONENT_POLICY_COMPLIANT_VERSION_COUNT, policyCompliantVersionCount);
    attributes.put(REPOSITORY_COMPONENT_METADATA_EVALUATION_TIME, evaluationTime);

    telemetrySender.send(telemetryData);
  }

  private void auditConfigureRepository(Repository repository, String errorMessage) {
    try (AuditSession auditSession = AuditData.get().recordSubEvent(AuditEvent.CONFIGURE_REPOSITORY, false)) {
      AuditData.get().setRepository(repository);
      if (errorMessage != null) {
        AuditData.get().setError(errorMessage);
      }
    }
  }

  void configureRepositories(
      String repositoryManagerInstanceId,
      ConfigureRepositoriesRequest configureRepositoriesRequest,
      String clientUserAgent)
  {
    AuditData.get().setRepositoryManagerInstanceId(repositoryManagerInstanceId);

    checkLicenseFeature();

    RepositoryManager repositoryManager = repositoryManagerDAO.getByInstanceId(repositoryManagerInstanceId);
    if (repositoryManager == null) {
      checkEvaluateComponentPermission(RepositoryContainer.SINGLETON);

      repositoryManager = new RepositoryManager(repositoryManagerInstanceId);
      repositoryManagerDAO.insert(repositoryManager);
    }
    else {
      checkEvaluateComponentPermission(repositoryManager);
    }

    validateConfigureRepositoriesRequest(configureRepositoriesRequest);

    boolean repositoryManagerUpdated = false;

    if (!configureRepositoriesRequest.repositoryManagerProductName.equals(repositoryManager.getProductName())
        || !configureRepositoriesRequest.repositoryManagerProductVersion
            .equals(repositoryManager.getProductVersion()))
    {
      repositoryManager.setProductName(configureRepositoriesRequest.repositoryManagerProductName);
      repositoryManager.setProductVersion(configureRepositoriesRequest.repositoryManagerProductVersion);
      repositoryManagerUpdated = true;
    }

    if (!Objects.equals(repositoryManager.getBaseUrl(), configureRepositoriesRequest.baseUrl)) {
      repositoryManager.setBaseUrl(configureRepositoriesRequest.baseUrl);
      repositoryManagerUpdated = true;
    }

    if (!repositoryManager.isConfigured()) {
      repositoryManager.setConfigured(true);
      repositoryManagerUpdated = true;
    }

    if (repositoryManagerUpdated) {
      repositoryManagerDAO.update(repositoryManager);
    }

    List<RepositoryDTO> repositoryDTOs = configureRepositoriesRequest.repositories;
    if (repositoryDTOs == null) {
      repositoryDTOs = Collections.emptyList();
    }

    log.debug("Configuring {} repositories for repository manager instance ID:{} ({})", repositoryDTOs.size(),
        repositoryManagerInstanceId, repositoryManager.getId());

    updateUserAgent(clientUserAgent, repositoryManager);

    configureRepositoriesNoAuthz(repositoryManager, repositoryDTOs, false);
  }

  public void configureRepositoriesNoAuthz(
      RepositoryManager repositoryManager,
      List<RepositoryDTO> repositoryDTOs,
      boolean updateLastManualConfigureTime)
  {
    try {
      for (RepositoryDTO repositoryDTO : repositoryDTOs) {
        Repository repository =
            repositoryDAO.getByRepositoryManagerInstanceIdAndPublicId(repositoryManager.getInstanceId(),
                repositoryDTO.name);

        if (repository == null) {
          // This is a new repository
          repository = new Repository(repositoryManager.getId(), repositoryDTO.name);
          repository.setFormat(repositoryDTO.format);
          repository.setRepositoryType(repositoryDTO.type);
          repository.setAuditEnabled(repositoryDTO.auditEnabled);
          repository.setQuarantineEnabled(repositoryDTO.quarantineEnabled);
          repository
              .setPolicyCompliantComponentSelectionEnabled(repositoryDTO.policyCompliantComponentSelectionEnabled);
          repository.setNamespaceConfusionProtectionEnabled(repositoryDTO.namespaceConfusionProtectionEnabled);
          repository.setMonitoringEnabled(repositoryDTO.monitoringEnabled);

          if (updateLastManualConfigureTime) {
            repository.setLastManualConfigureTime(new Date());
          }

          try {
            repositoryDAO.insert(repository);
            auditConfigureRepository(repository, null /* errorMessage */);
          }
          catch (RuntimeException e) {
            String errorMessage =
                String.format("Error updating repository %s: %s", repository.getName(), e.getMessage());
            log.error(errorMessage, e);
            auditConfigureRepository(repository, errorMessage);
          }
        }
        else {
          // This is an existing repository
          boolean updated = false;

          if (!repository.getRepositoryType().equals(repositoryDTO.type)) {
            repository.setRepositoryType(repositoryDTO.type);
            updated = true;
          }

          if (!Objects.equals(repository.getFormat(), repositoryDTO.format)) {
            repository.setFormat(repositoryDTO.format);
            updated = true;
          }

          if (repositoryDTO.auditEnabled != repository.isAuditEnabled()) {
            repository.setAuditEnabled(repositoryDTO.auditEnabled);
            updated = true;
          }
          if (repositoryDTO.quarantineEnabled != repository.isQuarantineEnabled()) {
            repository.setQuarantineEnabled(repositoryDTO.quarantineEnabled);
            updated = true;
          }
          if (repositoryDTO.policyCompliantComponentSelectionEnabled != repository
              .isPolicyCompliantComponentSelectionEnabled())
          {
            repository
                .setPolicyCompliantComponentSelectionEnabled(repositoryDTO.policyCompliantComponentSelectionEnabled);
            updated = true;
          }
          if (repositoryDTO.namespaceConfusionProtectionEnabled != repository.isNamespaceConfusionProtectionEnabled()) {
            repository.setNamespaceConfusionProtectionEnabled(repositoryDTO.namespaceConfusionProtectionEnabled);
            updated = true;
          }
          if (repositoryDTO.monitoringEnabled != repository.isMonitoringEnabled()) {
            repository.setMonitoringEnabled(repositoryDTO.monitoringEnabled);
            updated = true;
          }

          if (updated) {
            try {
              if (updateLastManualConfigureTime) {
                repository.setLastManualConfigureTime(new Date());
              }
              repositoryDAO.update(repository);
              auditConfigureRepository(repository, null /* errorMessage */);
            }
            catch (RuntimeException e) {
              String errorMessage = String.format("Error updating repository %s (%s): %s", repository.getName(),
                  repository.getId(), e.getMessage());
              log.error(errorMessage, e);
              auditConfigureRepository(repository, errorMessage);
            }
          }
        }
      }
    }
    finally {
      AuditData.get().commitSubEvents();
    }
  }

  private void validateConfigureRepositoriesRequest(ConfigureRepositoriesRequest configureRepositoriesRequest) {
    if (configureRepositoriesRequest == null) {
      throw new BadRequestException("The configureRepositoriesRequest parameter is required.");
    }
    if (StringUtils.isBlank(configureRepositoriesRequest.repositoryManagerProductName)) {
      throw new BadRequestException("The repositoryManagerProductName parameter is required.");
    }
    if (StringUtils.isBlank(configureRepositoriesRequest.repositoryManagerProductVersion)) {
      throw new BadRequestException("The repositoryManagerProductVersion parameter is required.");
    }
  }

  void removeRepository(String repositoryManagerInstanceId, String repositoryPublicId) {
    AuditData.get().setRepositoryManagerInstanceId(repositoryManagerInstanceId);
    AuditData.get().setRepositoryPublicId(repositoryPublicId);

    Repository repository = repositoryDAO.getByRepositoryManagerInstanceIdAndPublicId(
        repositoryManagerInstanceId, repositoryPublicId);
    if (repository != null) {
      checkEvaluateComponentPermission(repository);
      repositoryService.delete(repository);
      log.info("Deleted repository {}:{} ({})", repositoryManagerInstanceId, repositoryPublicId, repository.getId());
    }
    else {
      log.info("Not found repository {}:{} ", repositoryManagerInstanceId, repositoryPublicId);
    }
  }

  /**
   * @since 1.161
   */
  List<RepositoryDTO> getConfiguredRepositories(
      String repositoryManagerInstanceId,
      Long sinceUtcTimestamp,
      String clientUserAgent)
  {
    RepositoryManager repositoryManager = repositoryManagerDAO.getByInstanceIdNotNull(repositoryManagerInstanceId);

    return getConfiguredRepositories(repositoryManager, sinceUtcTimestamp, clientUserAgent);
  }

  @Authorize(permission = Permission.EVALUATE_COMPONENT)
  List<RepositoryDTO> getConfiguredRepositories(
      @AuthzContext(Key.REPOSITORY_MANAGER) RepositoryManager repositoryManager,
      Long sinceUtcTimestamp,
      String clientUserAgent)
  {
    List<Repository> repositories = getConfiguredRepositoriesNoAuthz(repositoryManager,
        sinceUtcTimestamp,
        clientUserAgent);

    List<RepositoryDTO> repositoryDTOS = repositories.stream().map(this::toRepositoryDTO).collect(Collectors.toList());
    return repositoryDTOS;
  }

  public List<Repository> getConfiguredRepositoriesNoAuthz(
      RepositoryManager repositoryManager,
      Long sinceUtcTimestamp,
      String clientUserAgent)
  {
    updateUserAgent(clientUserAgent, repositoryManager);

    long start = System.currentTimeMillis();

    List<Repository> repositories;
    if (sinceUtcTimestamp == null) {
      repositories = repositoryDAO.getByRepositoryManagerId(repositoryManager.getId());
    }
    else {
      repositories =
          repositoryDAO.getByRepositoryManagerIdAndLastManualConfigureTime(repositoryManager.getId(),
              new Date(sinceUtcTimestamp));
    }

    log.debug("Retrieved {} repositories for repository manager instance ID:{} ({}), configured since {} in {} ms.",
        repositories.size(), repositoryManager.getInstanceId(), repositoryManager.getId(), sinceUtcTimestamp,
        System.currentTimeMillis() - start);

    return repositories;
  }

  private RepositoryDTO toRepositoryDTO(Repository repository) {
    RepositoryDTO repositoryDTO = new RepositoryDTO();
    repositoryDTO.name = repository.getName();
    repositoryDTO.format = repository.getFormat();
    repositoryDTO.type = repository.getRepositoryType();
    repositoryDTO.auditEnabled = repository.isAuditEnabled();
    repositoryDTO.quarantineEnabled = repository.isQuarantineEnabled();
    repositoryDTO.policyCompliantComponentSelectionEnabled = repository.isPolicyCompliantComponentSelectionEnabled();
    repositoryDTO.namespaceConfusionProtectionEnabled = repository.isNamespaceConfusionProtectionEnabled();
    repositoryDTO.monitoringEnabled = repository.isMonitoringEnabled();
    return repositoryDTO;
  }

  // Needs to be at least package visible for the authz annotations to be effective.
  @Authorize(permission = Permission.EVALUATE_COMPONENT)
  void checkEvaluateComponentPermission(@SuppressWarnings("unused") @AuthzContext(AuthzContext.Key.OWNER) Owner owner) {
    // Do nothing as this method is only used to perform authz check for the caller
  }

  public static void validateIsProxyRepository(Repository repository) {
    if (!RepositoryType.proxy.equals(repository.getRepositoryType())) {
      throw new BadRequestException(
          "Repository " + repository.getPublicId() + " (" + repository.getId() + ") is not a proxy repository");
    }
  }

  private static void validateIsHostedRepository(Repository repository) {
    if (!RepositoryType.hosted.equals(repository.getRepositoryType())) {
      throw new BadRequestException(
          "Repository " + repository.getPublicId() + " (" + repository.getId() + ") is not a hosted repository");
    }
  }
}
