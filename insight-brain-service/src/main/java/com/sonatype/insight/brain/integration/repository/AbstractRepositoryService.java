/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration.repository;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList.ComponentEvaluationData;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.ProprietaryComponentNames;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataList;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList.RepositoryComponentEvaluationDataRequest;
import com.sonatype.clm.dto.model.component.UnquarantinedComponentList;
import com.sonatype.clm.dto.model.policy.RepositoryPolicyEvaluationSummary;
import com.sonatype.clm.dto.model.repository.QuarantinedComponentReport;
import com.sonatype.clm.dto.model.repository.onboarding.FirewallOnboardingRepositoryDTO;
import com.sonatype.clm.dto.model.repository.onboarding.FirewallOnboardingRequest;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryDTO;
import com.sonatype.insight.brain.api.v2.service.ApiRepositoryAdapter;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditSession;
import com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.dataaccess.repository.onboarding.FirewallOnboardingRepositoryDAO;
import com.sonatype.insight.brain.dataaccess.repository.onboarding.FirewallOnboardingRepositoryManagerDAO;
import com.sonatype.insight.brain.hds.FirewallQuarantineHdsClient;
import com.sonatype.insight.brain.landing.UserInterfaceLinksHelper;
import com.sonatype.insight.brain.model.HashHelper;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.repository.ProprietaryComponentNamePattern;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.repository.onboarding.FirewallOnboardingRepository;
import com.sonatype.insight.brain.model.repository.onboarding.FirewallOnboardingRepositoryManager;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLogEvent;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLoggerFactory;
import com.sonatype.insight.brain.policy.violation.RepositoryPolicyViolationLogger;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.repository.ProprietaryComponentNameDetector;
import com.sonatype.insight.brain.repository.RepositoryPolicyEvaluator;
import com.sonatype.insight.brain.repository.component.DbQuarantinedComponentAccessManager;
import com.sonatype.insight.brain.repository.component.QuarantinedComponentAccessManager;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.brain.security.CurrentUser;
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

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.stream.Collectors.toMap;

public abstract class AbstractRepositoryService
{
  private static final Logger log = LoggerFactory.getLogger(AbstractRepositoryService.class);

  static final String HDS_COMPONENT_METADATA_PATH = "rest/component/details/firewall/allVersions";

  private static final RepositoryManagerDAO repositoryManagerDAO = new RepositoryManagerDAO();

  protected static final RepositoryDAO repositoryDAO = new RepositoryDAO();

  protected static final RepositoryComponentDAO repositoryComponentDAO = new RepositoryComponentDAO();

  private static final RepositoryPolicyViolationDAO repositoryPolicyViolationDAO = new RepositoryPolicyViolationDAO();

  private static final FirewallOnboardingRepositoryManagerDAO firewallOnboardingRepositoryManagerDAO =
      new FirewallOnboardingRepositoryManagerDAO();

  private static final FirewallOnboardingRepositoryDAO firewallOnboardingRepositoryDAO =
      new FirewallOnboardingRepositoryDAO();

  protected final ProductLicense productLicense;

  private final RepositoryPolicyEvaluator repositoryPolicyEvaluator;

  private final ProprietaryComponentNameDetector proprietaryComponentNameDetector;

  private final PolicyViolationLoggerFactory policyViolationLoggerFactory;

  private final RepositoryComponentTelemetryCreator repositoryComponentTelemetryCreator;

  private final QuarantinedComponentAccessManager quarantinedComponentAccessManager;

  private final FirewallQuarantineHdsClient quarantineHdsClient;

  private final TelemetrySender telemetrySender;

  private final CurrentUser currentUser;

  static final String REPOSITORY_COMPONENT_REQUESTED_VERSION_COUNT = "repository_component_requested_version_count";

  static final String REPOSITORY_COMPONENT_POLICY_COMPLIANT_VERSION_COUNT =
      "repository_component_policy_compliant_version_count";

  static final String REPOSITORY_COMPONENT_METADATA_EVALUATION_TIME = "repository_component_metadata_evaluation_time";

  // Visible for tests
  final LicensedFeature requiredFeature;

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
      TelemetrySender telemetrySender,
      CurrentUser currentUser)
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
    this.currentUser = currentUser;
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

    Repository repository = repositoryDAO.getByRepositoryManagerInstanceIdAndPublicIdNotNull(
        repositoryManagerInstanceId, repositoryPublicId);

    if (!repository.isEnabled()) {
      throw new BadRequestException("Repository " + repositoryPublicId + " is disabled.");
    }

    updateUserAgent(clientUserAgent, repository);

    return getPolicyEvaluationSummary(repository);
  }

  @Authorize(permission = Permission.EVALUATE_COMPONENT)
  RepositoryPolicyEvaluationSummary getPolicyEvaluationSummary(
      @AuthzContext(Key.REPOSITORY) final Repository repository)
  {
    return getPolicyEvaluationSummaryInternal(repository);
  }

  ApiRepositoryDTO setEnabled(
      String repositoryManagerInstanceId,
      String repositoryPublicId,
      boolean enable,
      final String clientUserAgent)
  {
    AuditData.get().setRepositoryManagerInstanceId(repositoryManagerInstanceId)
        .setRepositoryPublicId(repositoryPublicId);
    checkLicenseFeature();

    log.debug("{} audit for repository {}:{}", enable ? "Enabling" : "Disabling", repositoryManagerInstanceId,
        repositoryPublicId);

    Repository repository = repositoryDAO.getByRepositoryManagerInstanceIdAndPublicId(repositoryManagerInstanceId,
        repositoryPublicId);
    if (repository == null) {
      repository = new Repository(null, repositoryPublicId);
    }
    setEnabled(repositoryManagerInstanceId, repository, enable);
    if (!enable) {
      policyViolationLoggerFactory.newLogger(new Date(), repository).logClearEvent();
    }

    updateUserAgent(clientUserAgent, repository);

    log.info("{} audit for repository {}:{} ({})", enable ? "Enabled" : "Disabled", repositoryManagerInstanceId,
        repositoryPublicId, repository.getId());

    return ApiRepositoryAdapter.convert(repository);
  }

  @Authorize(permission = Permission.EVALUATE_COMPONENT)
  void setEnabled(String repositoryManagerInstanceId,
                  @AuthzContext(Key.REPOSITORY) Repository repository,
                  boolean enable)
  {
    repository.setEnabled(enable);
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

    if (enabled && !repository.isEnabled()) {
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

    if (!repository.isEnabled() || !repository.isQuarantineEnabled()) {
      throw new BadRequestException("The repository must be enabled in quarantine mode.");
    }

    if (componentEvaluationDataRequestList == null || componentEvaluationDataRequestList.isEmpty()) {
      return new RepositoryComponentEvaluationDataList();
    }
    validateEvaluateRequest(componentEvaluationDataRequestList);

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
    String hash = componentEvaluationDataRequestList.components.get(0).hash;
    ComponentEvaluationDataList componentDetailsFromHds =
        getComponentMetadataFromHds(repository.getFormat(), pathname, hash, clientUserAgent);
    //for (ComponentEvaluationData c : componentDetailsFromHds.components) {
    //  System.err.println("From HDS: " + c.componentIdentifier + ", " + c.hash + ", " + c.filename);
    //}
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

    return result;
  }
  
  private ComponentEvaluationDataList matchHdsComponentDetailsToRequestListByHashAndFilename(
      ComponentEvaluationDataList componentDetailsFromHdsList,
      RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList)
  {
    ComponentEvaluationDataList result = new ComponentEvaluationDataList();
    Map<String, ComponentEvaluationData> componentDetailsFromHdsByHashAndFilename =
        componentDetailsFromHdsList.components.stream()
            .collect(toMap(componentEvaluationData -> toHashFilenameKey(componentEvaluationData), Function.identity()));
    
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
    return componentEvaluationData.hash + "|" + componentEvaluationData.filename;
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
      AuditData.get().setData("componentCount",
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

  private void validateEvaluateRequest(RepositoryComponentEvaluationDataRequestList componentEvalRequestList) {
    for (RepositoryComponentEvaluationDataRequest componentEvalRequest : componentEvalRequestList.components) {
      validateEvaluateRequest(componentEvalRequest);
    }
  }

  private void validateEvaluateRequest(final RepositoryComponentEvaluationDataRequest componentEvaluationDataRequest) {
    if (componentEvaluationDataRequest == null) {
      throw new BadRequestException("The componentEvaluationDataRequest cannot be null.");
    }
    if (StringUtils.isBlank(componentEvaluationDataRequest.pathname)) {
      throw new BadRequestException("The pathname cannot be null or empty.");
    }
    if (StringUtils.isBlank(componentEvaluationDataRequest.format)) {
      throw new BadRequestException("The format cannot be null or empty.");
    }
    if (StringUtils.isBlank(componentEvaluationDataRequest.hash)) {
      throw new BadRequestException("The hash cannot be null or empty.");
    }
  }

  @Authorize(permission = Permission.EVALUATE_COMPONENT)
  RepositoryComponentEvaluationDataList evaluateComponents(
      @AuthzContext(Key.REPOSITORY) Repository repository,
      String repositoryManagerInstanceId,
      RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList,
      final boolean withQuarantine,
      final boolean persistEvaluationResults,
      final String clientUserAgent)
  {
    long start = System.currentTimeMillis();

    if (!repository.isEnabled() || (withQuarantine && !repository.isQuarantineEnabled())
        || repository.getFormat() == null) {
      if (!repository.isEnabled() && persistEvaluationResults) {
        log.info("Enabled audit for repository {}:{} ({})", repository.getRepositoryManagerId(),
            repository.getPublicId(), repository.getId());
        repository.setEnabled(true);
        try (AuditSession auditSession = AuditData.get().recordSubEvent(AuditEvent.CONNECT_REPOSITORY, false)) {
          AuditData.get().setRepository(repository).setData("repositoryManagerInstanceId", repositoryManagerInstanceId);
        }
      }
      if (withQuarantine && persistEvaluationResults) {
        if (!repository.isQuarantineEnabled()) {
          log.info("Enabled quarantine for repository {}:{} ({})", repository.getRepositoryManagerId(),
              repository.getPublicId(), repository.getId());
          repository.setQuarantineEnabled(true);
          try (AuditSession auditSession = AuditData.get().recordSubEvent(AuditEvent.CONFIGURE_QUARANTINE, false)) {
            AuditData.get().setRepository(repository).setData("quarantine", "enabled");
          }
        }
      }
      if (componentEvaluationDataRequestList != null && componentEvaluationDataRequestList.components != null
          && !componentEvaluationDataRequestList.components.isEmpty()) {
        repository.setFormat(componentEvaluationDataRequestList.components.get(0).format);
      }
      repositoryDAO.update(repository);
      AuditData.get().commitSubEvents();
    }

    updateUserAgent(clientUserAgent, repository);

    if (componentEvaluationDataRequestList == null || componentEvaluationDataRequestList.isEmpty()) {
      return new RepositoryComponentEvaluationDataList();
    }
    validateEvaluateRequest(componentEvaluationDataRequestList);

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

  private RepositoryPolicyEvaluationSummary getPolicyEvaluationSummaryInternal(final Repository repository) {
    List<RepositoryPolicyViolation> repositoryPolicyViolations = repositoryPolicyViolationDAO
        .getActiveByRepositoryIdAndNotWaived(repository.getId());

    final Map<String, Integer> componentThreatLevels = new HashMap<>();
    for (RepositoryPolicyViolation repositoryPolicyViolation : repositoryPolicyViolations) {
      String pathname = repositoryPolicyViolation.getPathname();
      Integer threatLevel = componentThreatLevels.get(pathname);
      if (threatLevel == null || threatLevel < repositoryPolicyViolation.getThreatLevel()) {
        componentThreatLevels.put(pathname, repositoryPolicyViolation.getThreatLevel());
      }
    }
    int criticalCount = 0;
    int severeCount = 0;
    int moderateCount = 0;
    for (final int level : componentThreatLevels.values()) {
      if (level >= 8) {
        criticalCount++;
      }
      else if (level >= 4) {
        severeCount++;
      }
      else if (level >= 2) {
        moderateCount++;
      }
    }

    RepositoryPolicyEvaluationSummary policyEvaluationSummary = new RepositoryPolicyEvaluationSummary();
    policyEvaluationSummary.setCriticalComponentCount(criticalCount);
    policyEvaluationSummary.setSevereComponentCount(severeCount);
    policyEvaluationSummary.setModerateComponentCount(moderateCount);
    policyEvaluationSummary.setAffectedComponentCount(criticalCount + severeCount + moderateCount);
    policyEvaluationSummary.setQuarantinedComponentCount(repositoryComponentDAO
        .getQuarantinedComponentCountByRepositoryId(repository.getId()));

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

  @Authorize(permission = Permission.EVALUATE_COMPONENT)
  void removeComponent(@AuthzContext(Key.REPOSITORY) Repository repository, String pathname) {
    if (!repository.isEnabled()) {
      repository.setEnabled(true);
      repositoryDAO.update(repository);
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

    updateUserAgent(clientUserAgent, repository);

    log.debug("Getting unquarantined components for repository {}:{} ({}), since {}.", repositoryManagerInstanceId,
        repositoryPublicId, repository.getId(), sinceUtcTimestamp);

    return getUnquarantinedComponents(repository, sinceUtcTimestamp);
  }

  @Authorize(permission = Permission.EVALUATE_COMPONENT)
  UnquarantinedComponentList getUnquarantinedComponents(@AuthzContext(Key.REPOSITORY) Repository repository,
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

  public void addProprietaryComponentNames(
      String repositoryManagerInstanceId,
      String repositoryPublicId,
      ProprietaryComponentNames proprietaryComponentNames)
  {
    AuditData.get().setRepositoryManagerInstanceId(repositoryManagerInstanceId);
    checkLicenseFeature();
    Repository repository = new Repository(null, repositoryPublicId);
    addProprietaryComponentNames(repositoryManagerInstanceId, repository, proprietaryComponentNames);
  }

  @Authorize(permission = Permission.EVALUATE_COMPONENT)
  void addProprietaryComponentNames(
      String repositoryManagerInstanceId,
      @AuthzContext(Key.REPOSITORY) Repository repository,
      ProprietaryComponentNames proprietaryComponentNames)
  {
    if (proprietaryComponentNames == null) {
      throw new BadRequestException("No component name patterns specified");
    }
    if (StringUtils.isBlank(proprietaryComponentNames.format)) {
      throw new BadRequestException("No component format specified");
    }
    String format = translateRepositoryFormat(proprietaryComponentNames.format);
    List<ProprietaryComponentNamePattern> patterns = new ArrayList<>();
    if (proprietaryComponentNames.namespaces != null) {
      for (String namespace : proprietaryComponentNames.namespaces) {
        validatePattern("namespace", namespace);
        patterns.add(new ProprietaryComponentNamePattern(format).withNamespacePattern(namespace));
      }
    }
    if (proprietaryComponentNames.names != null) {
      for (String name : proprietaryComponentNames.names) {
        validatePattern("name", name);
        patterns.add(new ProprietaryComponentNamePattern(format).withNamePattern(name));
      }
    }
    if (patterns.isEmpty()) {
      throw new BadRequestException("No component name patterns specified");
    }
    for (ProprietaryComponentNamePattern pattern : patterns) {
      pattern.withRepository(repositoryManagerInstanceId, repository.getPublicId());
    }
    int added = proprietaryComponentNameDetector.addPatterns(format, patterns);
    AuditData.get().setData("addedPatternCount", added);
  }

  private void validatePattern(String type, String pattern) {
    if (StringUtils.isBlank(pattern)) {
      throw new BadRequestException("Empty component " + type + " pattern");
    }
    int first = pattern.indexOf('*');
    int next = pattern.indexOf('*', first + 1);
    if (first >= 0 && (next >= 0 || (first > 0 && first < pattern.length() - 1) || pattern.length() == 1)) {
      throw new BadRequestException("Invalid component " + type + " pattern: " + pattern);
    }
  }

  private String translateRepositoryFormat(String format) {
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
      default:
        return format;
    }
  }

  public void removeProprietaryComponentNames(String repositoryManagerInstanceId, String repositoryPublicId) {
    AuditData.get().setRepositoryManagerInstanceId(repositoryManagerInstanceId);
    Repository repository = new Repository(null, repositoryPublicId);
    removeProprietaryComponentNames(repositoryManagerInstanceId, repository);
  }

  @Authorize(permission = Permission.MANAGE_PROPRIETARY)
  void removeProprietaryComponentNames(
      String repositoryManagerInstanceId,
      @AuthzContext(Key.REPOSITORY) Repository repository)
  {
    proprietaryComponentNameDetector.removePatterns(repositoryManagerInstanceId, repository.getPublicId());
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

  protected void firewallOnboarding(FirewallOnboardingRequest firewallOnboardingRequest, String clientUserAgent) {
    checkFirewallOnboardingPermission(RepositoryContainer.SINGLETON);

    if (firewallOnboardingRequest == null) {
      throw new BadRequestException("The request data is required.");
    }
    if (firewallOnboardingRequest.repositoryManager == null) {
      throw new BadRequestException("The repository manager is required.");
    }
    if (CollectionUtils.isEmpty(firewallOnboardingRequest.repositories)) {
      throw new BadRequestException("At least one repository is required.");
    }

    try (TransactionContext tx = firewallOnboardingRepositoryManagerDAO.createTransactionContext()) {
      tx.begin();

      FirewallOnboardingRepositoryManager repositoryManager = new FirewallOnboardingRepositoryManager(
          firewallOnboardingRequest.repositoryManager.instanceId, currentUser.getUsername(), clientUserAgent);
      firewallOnboardingRepositoryManagerDAO.insert(tx, repositoryManager);

      List<FirewallOnboardingRepository> repositories = firewallOnboardingRequest.repositories.stream()
          .map(repositoryDTO -> fromFirewallOnboardingRepositoryDTO(repositoryManager.getId(), repositoryDTO))
          .collect(Collectors.toList());
      repositories.forEach(repository -> firewallOnboardingRepositoryDAO.insert(tx, repository));

      tx.commit();
    }
  }

  private FirewallOnboardingRepository fromFirewallOnboardingRepositoryDTO(
      String repositoryManagerId,
      FirewallOnboardingRepositoryDTO repositoryDTO)
  {
    return new FirewallOnboardingRepository(repositoryManagerId, repositoryDTO.name, repositoryDTO.format,
        repositoryDTO.type);
  }

  // Must have at least package visibility for the authz annotations to take effect.
  @Authorize(permission = Permission.EVALUATE_COMPONENT)
  void checkFirewallOnboardingPermission(
      @SuppressWarnings("unused") @AuthzContext(AuthzContext.Key.OWNER) Owner owner)
  {
    // The permission check is handled by the authz annotations
  }
}
