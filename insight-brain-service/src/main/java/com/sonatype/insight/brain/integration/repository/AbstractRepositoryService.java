/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration.repository;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataList;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList.RepositoryComponentEvaluationDataRequest;
import com.sonatype.clm.dto.model.component.UnquarantinedComponentList;
import com.sonatype.clm.dto.model.policy.RepositoryPolicyEvaluationSummary;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditSession;
import com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.landing.UserInterfaceLinksResource;
import com.sonatype.insight.brain.model.HashHelper;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLogEvent;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLoggerFactory;
import com.sonatype.insight.brain.policy.violation.RepositoryPolicyViolationLogger;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.repository.RepositoryPolicyEvaluator;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.license.model.LicensedFeature;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractRepositoryService
{
  private static final Logger log = LoggerFactory.getLogger(AbstractRepositoryService.class);

  private static final RepositoryManagerDAO repositoryManagerDAO = new RepositoryManagerDAO();

  private static final RepositoryDAO repositoryDAO = new RepositoryDAO();

  private static final RepositoryComponentDAO repositoryComponentDAO = new RepositoryComponentDAO();

  private static final RepositoryPolicyViolationDAO repositoryPolicyViolationDAO = new RepositoryPolicyViolationDAO();

  protected final ProductLicense productLicense;

  private final RepositoryPolicyEvaluator repositoryPolicyEvaluator;

  protected final HdsClient hdsClient;

  private final PolicyViolationLoggerFactory policyViolationLoggerFactory;

  final LicensedFeature requiredFeature;

  @Inject
  public AbstractRepositoryService(RepositoryPolicyEvaluator repositoryPolicyEvaluator,
                                   ProductLicense productLicense,
                                   HdsClient hdsClient,
                                   PolicyViolationLoggerFactory policyViolationLoggerFactory,
                                   LicensedFeature requiredFeature)
  {
    this.repositoryPolicyEvaluator = repositoryPolicyEvaluator;
    this.productLicense = productLicense;
    this.hdsClient = hdsClient;
    this.policyViolationLoggerFactory = policyViolationLoggerFactory;
    this.requiredFeature = requiredFeature;
  }

  private void checkLicenseFeature() {
    productLicense.validateFeature(requiredFeature);
  }

  private void auditComponentPath(final String pathname) {
    AuditData.get().setData("componentPathname", pathname);
  }

  public RepositoryPolicyEvaluationSummary getPolicyEvaluationSummary(final String repositoryManagerInstanceId,
                                                                      final String repositoryPublicId)
  {
    checkLicenseFeature();

    Repository repository = repositoryDAO.getByRepositoryManagerInstanceIdAndPublicIdNotNull(
        repositoryManagerInstanceId, repositoryPublicId);

    if (!repository.isEnabled()) {
      throw new BadRequestException("Repository " + repositoryPublicId + " is disabled.");
    }

    return getPolicyEvaluationSummary(repository);
  }

  @Authorize(permission = Permission.EVALUATE_COMPONENT)
  RepositoryPolicyEvaluationSummary getPolicyEvaluationSummary(
      @AuthzContext(Key.REPOSITORY) final Repository repository)
  {
    return getPolicyEvaluationSummaryInternal(repository);
  }

  public void setEnabled(String repositoryManagerInstanceId, String repositoryPublicId, boolean enable) {
    AuditData.get().setData("repositoryManagerInstanceId", repositoryManagerInstanceId)
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

    log.info("{} audit for repository {}:{} ({})", enable ? "Enabled" : "Disabled", repositoryManagerInstanceId,
        repositoryPublicId, repository.getId());
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

  public void setQuarantine(final String repositoryManagerInstanceId,
                            final String repositoryPublicId,
                            final boolean enabled)
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

    log.info("{} quarantine for repository {}:{} ({})", enabled ? "Enabled" : "Disabled", repositoryManagerInstanceId,
        repositoryPublicId, repository.getId());
  }

  @Authorize(permission = Permission.EVALUATE_COMPONENT)
  void setQuarantine(@AuthzContext(Key.REPOSITORY) final Repository repository, final boolean enabled) {
    repository.setQuarantineEnabled(enabled);
    repositoryDAO.update(repository);
  }

  public RepositoryComponentEvaluationDataList evaluateComponents(
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

    if (componentEvaluationDataRequestList == null || componentEvaluationDataRequestList.isEmpty()) {
      return new RepositoryComponentEvaluationDataList();
    }
    validateEvaluateRequest(componentEvaluationDataRequestList);

    normalizeComponents(componentEvaluationDataRequestList);

    RepositoryComponentEvaluationDataList result = repositoryPolicyEvaluator.evaluate(repository,
        componentEvaluationDataRequestList, withQuarantine, persistEvaluationResults, clientUserAgent);

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

    policyEvaluationSummary.setReportUrl(UserInterfaceLinksResource.getRepositoryReportUrl(repository.getId()));

    return policyEvaluationSummary;
  }

  void removeComponent(String repositoryManagerInstanceId, String repositoryPublicId, String pathname) {
    checkLicenseFeature();

    Repository repository = repositoryDAO.getByRepositoryManagerInstanceIdAndPublicIdNotNull(
        repositoryManagerInstanceId, repositoryPublicId);

    removeComponent(repository, normalizePathname(pathname));
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
      try (TransactionContext tx = repositoryComponentDAO.createTransactionContext()) {
        tx.begin();
        // Mark all violations for this component as inactive.
        for (RepositoryPolicyViolation policyViolation : repositoryPolicyViolationDAO
            .getActiveByRepositoryIdAndPathname(tx, repositoryComponent.getRepositoryId(),
                repositoryComponent.getPathname())) {
          policyViolation.setActive(false);
          repositoryPolicyViolationDAO.update(tx, policyViolation);
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
    }
  }

  static String normalizePathname(String pathname) {
    if (pathname != null && pathname.startsWith("/")) {
      return pathname.substring(1);
    }

    return pathname;
  }

  UnquarantinedComponentList getUnquarantinedComponents(String repositoryManagerInstanceId,
                                                        String repositoryPublicId,
                                                        long sinceUtcTimestamp)
  {
    checkLicenseFeature();

    Repository repository = repositoryDAO.getByRepositoryManagerInstanceIdAndPublicIdNotNull(
        repositoryManagerInstanceId, repositoryPublicId);

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
}
