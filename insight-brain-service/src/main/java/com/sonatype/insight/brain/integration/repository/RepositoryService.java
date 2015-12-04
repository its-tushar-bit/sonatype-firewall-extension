/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration.repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataList;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList.RepositoryComponentEvaluationDataRequest;
import com.sonatype.clm.dto.model.policy.RepositoryPolicyEvaluationSummary;
import com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.landing.UserInterfaceLinksResource;
import com.sonatype.insight.brain.model.HashHelper;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.policy.evaluator.PolicyThreats;
import com.sonatype.insight.brain.policy.evaluator.PolicyThreatsAdapter;
import com.sonatype.insight.brain.product.license.CLMLicenseManager;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.repository.RepositoryPolicyEvaluator;
import com.sonatype.insight.brain.repository.RepositoryPolicyThreatDTO;
import com.sonatype.insight.brain.repository.RepositoryPolicyViolationDTO;
import com.sonatype.insight.brain.repository.RepositoryReevaluationTask;
import com.sonatype.insight.brain.repository.RepositoryReportDetail;
import com.sonatype.insight.brain.repository.RepositoryReportResource.RepositoryReportSummary;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.17.0
 */
@Named
public class RepositoryService
{
  private static final Logger log = LoggerFactory.getLogger(RepositoryService.class);

  private static final RepositoryManagerDAO repositoryManagerDAO = new RepositoryManagerDAO();

  private static final RepositoryDAO repositoryDAO = new RepositoryDAO();

  private static final RepositoryComponentDAO repositoryComponentDAO = new RepositoryComponentDAO();

  private static final RepositoryPolicyViolationDAO repositoryPolicyViolationDAO = new RepositoryPolicyViolationDAO();

  private final CLMLicenseManager licenseManager;

  private final PolicyThreatsAdapter policyThreatsAdapter;

  private final RepositoryPolicyEvaluator repositoryPolicyEvaluator;

  @Inject
  public RepositoryService(RepositoryPolicyEvaluator repositoryPolicyEvaluator, CLMLicenseManager licenseManager,
      PolicyThreatsAdapter policyThreatsAdapter)
  {
    this.repositoryPolicyEvaluator = repositoryPolicyEvaluator;
    this.licenseManager = licenseManager;
    this.policyThreatsAdapter = policyThreatsAdapter;
  }

  private void checkLicenseFeature() {
    if (!licenseManager.hasRepositoryFirewall()) {
      throw new InvalidLicenseException();
    }
  }

  public RepositoryPolicyThreatDTO getPolicyThreats(final String repositoryId, final String pathname) {
    checkLicenseFeature();

    Repository repository = repositoryDAO.getByIdNotNull(repositoryId);
    return getPolicyThreats(repository, pathname);
  }

  @Authorize(permission = Permission.READ)
  RepositoryPolicyThreatDTO getPolicyThreats(@AuthzContext(Key.REPOSITORY) final Repository repository,
      final String pathname)
  {
    RepositoryComponent repositoryComponent =
        repositoryComponentDAO.getByRepositoryIdAndPathname(repository.getId(), pathname);
    if (repositoryComponent == null) {
      throw new NotFoundException("Cannot find a component with path " + pathname +
          " in repository with ID " + repository.getId() + ".");
    }

    List<RepositoryPolicyViolation> repositoryPolicyViolations = repositoryPolicyViolationDAO
        .getActiveByRepositoryIdAndPathnameAndWaived(repository.getId(), repositoryComponent.getPathname(), false);

    List<RepositoryPolicyViolationDTO> activeRepositoryViolationDTOs = new ArrayList<>();
    for (RepositoryPolicyViolation repositoryPolicyViolation : repositoryPolicyViolations) {
      List<PolicyThreats.PolicyConstraint> constraints = policyThreatsAdapter.toPolicyThreatsPolicyConstraints(
          repositoryPolicyViolation.getConstraintFacts());
      activeRepositoryViolationDTOs.add(new RepositoryPolicyViolationDTO(repositoryPolicyViolation.getPolicyId(),
          repositoryPolicyViolation.getPolicyName(), repositoryPolicyViolation.getThreatLevel(), constraints));
    }

    return new RepositoryPolicyThreatDTO(activeRepositoryViolationDTOs);
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
  RepositoryPolicyEvaluationSummary getPolicyEvaluationSummary(@AuthzContext(Key.REPOSITORY) final Repository repository)
  {
    return getPolicyEvaluationSummaryInternal(repository);
  }

  public void setEnabled(String repositoryManagerInstanceId, String repositoryPublicId, boolean enable) {
    checkLicenseFeature();

    log.debug("{} audit for repository {}:{}", enable ? "Enabling" : "Disabling", repositoryManagerInstanceId,
        repositoryPublicId);

    Repository repository = repositoryDAO.getByRepositoryManagerInstanceIdAndPublicId(
        repositoryManagerInstanceId, repositoryPublicId);
    if (repository == null) {
      repository = new Repository(null, repositoryPublicId);
    }
    setEnabled(repositoryManagerInstanceId, repository, enable);

    log.info("{} audit for repository {}:{} ({})", enable ? "Enabled" : "Disabled", repositoryManagerInstanceId,
        repositoryPublicId, repository.getId());
  }

  @Authorize(permission = Permission.EVALUATE_COMPONENT)
  void setEnabled(String repositoryManagerInstanceId, @AuthzContext(Key.REPOSITORY) Repository repository,
      boolean enable) {
    RepositoryManager repositoryManager = repositoryManagerDAO.getByInstanceId(repositoryManagerInstanceId);

    if (repositoryManager == null) {
      repositoryManager = new RepositoryManager(repositoryManagerInstanceId);
      repositoryManagerDAO.insert(repositoryManager);
    }

    repository.setEnabled(enable);
    if (repository.getId() == null) {
      repository.setRepositoryManagerId(repositoryManager.getId());
      repositoryDAO.insert(repository);
    }
    else {
      repositoryDAO.update(repository);
    }
  }

  public void setQuarantine(final String repositoryManagerInstanceId, final String repositoryPublicId,
      final boolean enabled)
  {
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

  public RepositoryComponentEvaluationDataList evaluateComponents(String repositoryManagerInstanceId, String repositoryPublicId,
      RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList, boolean withQuarantine)
  {
    checkLicenseFeature();

    Repository repository = repositoryDAO.getByRepositoryManagerInstanceIdAndPublicIdNotNull(
        repositoryManagerInstanceId, repositoryPublicId);

    log.debug("Evaluating components for repository {}:{} ({}) with quarantine {}", repositoryManagerInstanceId,
        repositoryPublicId, repository.getId(), withQuarantine);

    return evaluateComponents(repository, componentEvaluationDataRequestList, withQuarantine);
  }

  private void normalizeComponents(RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList) {
    for (RepositoryComponentEvaluationDataRequest componentEvaluationDataRequest : componentEvaluationDataRequestList.components) {
      truncateHash(componentEvaluationDataRequest);
      componentEvaluationDataRequest.pathname = normalizePathname(componentEvaluationDataRequest.pathname);
    }
  }

  private void truncateHash(final RepositoryComponentEvaluationDataRequest componentEvaluationDataRequest) {
    componentEvaluationDataRequest.hash = HashHelper.truncateHash(componentEvaluationDataRequest.hash);
  }

  private void validateEvaluateRequest(RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList)
  {
    for (RepositoryComponentEvaluationDataRequest componentEvaluationDataRequest : componentEvaluationDataRequestList.components) {
      validateEvaluateRequest(componentEvaluationDataRequest);
    }
  }

  private void validateEvaluateRequest(final RepositoryComponentEvaluationDataRequest componentEvaluationDataRequest)
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
    if (StringUtils.isBlank(componentEvaluationDataRequest.hash)) {
      throw new BadRequestException("The hash cannot be null or empty.");
    }
  }

  @Authorize(permission = Permission.EVALUATE_COMPONENT)
  RepositoryComponentEvaluationDataList evaluateComponents(@AuthzContext(Key.REPOSITORY) Repository repository,
      RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList, final boolean withQuarantine)
  {
    long start = System.currentTimeMillis();

    if (!repository.isEnabled() || (withQuarantine && !repository.isQuarantineEnabled())
        || repository.getFormat() == null) {
      if (!repository.isEnabled()) {
        log.info("Enabled audit for repository {}:{} ({})", repository.getRepositoryManagerId(),
            repository.getPublicId(), repository.getId());
      }
      repository.setEnabled(true);
      if (withQuarantine) {
        if (!repository.isQuarantineEnabled()) {
          log.info("Enabled quarantine for repository {}:{} ({})", repository.getRepositoryManagerId(),
              repository.getPublicId(), repository.getId());
        }
        // If this is for quarantine make sure it's enabled in IQ
        repository.setQuarantineEnabled(true);
      }
      if (componentEvaluationDataRequestList != null && componentEvaluationDataRequestList.components != null
          && !componentEvaluationDataRequestList.components.isEmpty()) {
        repository.setFormat(componentEvaluationDataRequestList.components.get(0).format);
      }
      repositoryDAO.update(repository);
    }


    if (componentEvaluationDataRequestList == null || componentEvaluationDataRequestList.isEmpty()) {
      return new RepositoryComponentEvaluationDataList();
    }
    validateEvaluateRequest(componentEvaluationDataRequestList);

    normalizeComponents(componentEvaluationDataRequestList);
    
    RepositoryComponentEvaluationDataList result = repositoryPolicyEvaluator.evaluate(repository,
        componentEvaluationDataRequestList, withQuarantine);

    log.debug("Evaluated {} components with quarantine {} for repository {}:{} ({}) in {} ms.",
        componentEvaluationDataRequestList.components.size(), withQuarantine, repository.getRepositoryManagerId(),
        repository.getPublicId(), repository.getId(), System.currentTimeMillis() - start);

    return result;
  }

  public RepositoryReportSummary getReportSummary(String repositoryId) {
    checkLicenseFeature();

    Repository repository = repositoryDAO.getByIdNotNull(repositoryId);

    log.debug("Get report summary for repository {}:{} ({})", repository.getRepositoryManagerId(),
        repository.getPublicId(), repositoryId);

    return getReportSummary(repository);
  }

  @Authorize(permission = Permission.READ)
  RepositoryReportSummary getReportSummary(@AuthzContext(Key.REPOSITORY) Repository repository) {
    RepositoryReportSummary summary = new RepositoryReportSummary();
    summary.knownComponentCount = repositoryComponentDAO.getKnownComponentCountByRepositoryId(repository.getId());
    summary.totalComponentCount = repositoryComponentDAO.getComponentCountByRepositoryId(repository.getId());

    RepositoryPolicyEvaluationSummary policyEvalSummary = this.getPolicyEvaluationSummaryInternal(repository);
    summary.criticalComponentCount = policyEvalSummary.getCriticalComponentCount();
    summary.severeComponentCount = policyEvalSummary.getSevereComponentCount();
    summary.moderateComponentCount = policyEvalSummary.getModerateComponentCount();
    summary.affectedComponentCount = policyEvalSummary.getAffectedComponentCount();
    summary.quarantinedComponentCount = policyEvalSummary.getQuarantinedComponentCount();

    return summary;
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
    policyEvaluationSummary.setQuarantinedComponentCount(
        repositoryComponentDAO.getQuarantinedComponentCountByRepositoryId(repository.getId()));

    policyEvaluationSummary.setReportUrl(UserInterfaceLinksResource.getRepositoryReportUrl(repository.getId()));

    return policyEvaluationSummary;
  }

  public List<RepositoryReportDetail> getReportDetails(final String repositoryId)
  {
    checkLicenseFeature();

    final Repository repository = repositoryDAO.getByIdNotNull(repositoryId);

    log.debug("Get report details for repository {}:{} ({})", repository.getRepositoryManagerId(),
        repository.getPublicId(), repository.getId());

    return getReportDetails(repository);
  }

  @Authorize(permission = Permission.READ)
  List<RepositoryReportDetail> getReportDetails(@AuthzContext(Key.REPOSITORY) final Repository repository) {
    final List<RepositoryReportDetail> details = new ArrayList<>();

    final List<RepositoryComponent> componentList = repositoryComponentDAO.getByRepositoryId(repository.getId());
    for (final RepositoryComponent component : componentList) {

      final List<RepositoryPolicyViolation> componentViolations = repositoryPolicyViolationDAO
          // violations are sorted by 'ThreatLevel DESC, policyId', so highestThreatLevel per component is first
          .getActiveByRepositoryIdAndPathname(repository.getId(), component.getPathname());
      boolean highestThreatLevel = true;

      if (componentViolations.size() > 0) {
        for (final RepositoryPolicyViolation violation : componentViolations) {
          details.add(RepositoryReportDetail.create(component, violation, highestThreatLevel));
          // like the CI report, we choose one of the violations and use it as the highest.
          highestThreatLevel = false;
        }
      }
      else {
        details.add(RepositoryReportDetail.create(component));
      }
    }

    // sort by threatLevel DESC, pathname ASC
    Collections.sort(details, THREAT_LEVEL_DESC_PATHNAME_ASC);

    return details;
  }

  /**
   * Sort by threatLevel DESC, pathname ASC.
   */
  static final Comparator<RepositoryReportDetail> THREAT_LEVEL_DESC_PATHNAME_ASC = new Comparator<RepositoryReportDetail>()
  {
    @Override
    public int compare(final RepositoryReportDetail detail1, final RepositoryReportDetail detail2) {
      // sort ThreatLevel Descending
      final int cmpThreatLevel = detail2.getThreatLevel() - detail1.getThreatLevel();
      if (cmpThreatLevel != 0) {
        return cmpThreatLevel;
      }

      // sort pathname Ascending
      return detail1.getPathname().compareTo(detail2.getPathname());
    }
  };

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
      repositoryComponentDAO.delete(repositoryComponent);
    }
  }

  private String normalizePathname(String pathname) {
    if (pathname != null && pathname.startsWith("/")) {
      return pathname.substring(1);
    }

    return pathname;
  }

  /**
   * @since 1.18.0
   */
  @Authorize(permission = Permission.READ)
  public RepositoryDTO getRepositoryById(@AuthzContext(Key.REPOSITORY_ID) String repositoryId) {
    checkLicenseFeature();

    RepositoryDTO repositoryDTO = new RepositoryDTO();
    repositoryDTO.repository = repositoryDAO.getByIdNotNull(repositoryId);

    Date evaluationTime = repositoryComponentDAO.getOldestComponentEvaluationTimeByRepositoryId(
        repositoryId);

    if (evaluationTime != null) {
      repositoryDTO.oldestEvalTimestamp = evaluationTime.getTime();
    }

    return repositoryDTO;
  }

  public static class RepositoryDTO
  {
    public Long oldestEvalTimestamp;
    public Repository repository;
  }


  private final Executor reevalExecutor = createReevaluationExecutor();

  private final ConcurrentMap<String, AtomicInteger> reevaluations = new ConcurrentHashMap<>();

  @Authorize(permission = Permission.EVALUATE_COMPONENT)
  public void reevaluateRepository(@AuthzContext(Key.REPOSITORY_ID) String repositoryId) {
    checkLicenseFeature();

    Repository repository = repositoryDAO.getByIdNotNull(repositoryId);
    if (reevaluations.putIfAbsent(repositoryId, new AtomicInteger()) == null) {
      log.debug("Starting re-evaluation for repository {}:{} ({})", repository.getRepositoryManagerId(),
          repository.getPublicId(), repositoryId);
      reevalExecutor.execute(new RepositoryReevaluationTask(repository, repositoryPolicyEvaluator, reevalExecutor,
          reevaluations));
    }
    else {
      log.debug("Skipping, re-evaluation for repository {}:{} ({}) is already in progress",
          repository.getRepositoryManagerId(), repository.getPublicId(), repositoryId);
    }
  }

  private static Executor createReevaluationExecutor() {
    ThreadPoolExecutor executor = new ThreadPoolExecutor(20, 20, 5L, TimeUnit.SECONDS,
        new LinkedBlockingQueue<Runnable>(),
        new ThreadFactoryBuilder().setDaemon(true).setNameFormat("ReevaluateRepository-%s").build());

    executor.allowCoreThreadTimeOut(true);

    return executor;
  }
}
