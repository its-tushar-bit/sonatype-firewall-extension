/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration.repository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.component.ComponentDetails;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList.ComponentEvaluationData;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList.ComponentEvaluationData;
import com.sonatype.clm.dto.model.component.NamedComponentDetails;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList.RepositoryComponentEvaluationDataRequest;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationResult;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationSummary;
import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.component.ComponentDetailsAdapter;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.hds.ComponentDetailsLoader;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.landing.UserInterfaceLinksResource;
import com.sonatype.insight.brain.model.HashHelper;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.IdentificationSource;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.DevelopStageType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.policy.evaluator.ComponentPolicyEvaluator;
import com.sonatype.insight.brain.policy.evaluator.PolicyResults;
import com.sonatype.insight.brain.product.license.CLMLicenseManager;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.repository.RepositoryReportResource.RepositoryReportSummary;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

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

  static final String HDS_COMPONENT_DETAILS_PATH = "rest/component/details/firewall";

  private static final RepositoryManagerDAO repositoryManagerDAO = new RepositoryManagerDAO();

  private static final RepositoryDAO repositoryDAO = new RepositoryDAO();

  private static final RepositoryComponentDAO repositoryComponentDAO = new RepositoryComponentDAO();

  private static final RepositoryPolicyViolationDAO repositoryPolicyViolationDAO = new RepositoryPolicyViolationDAO();

  private static final PolicyDAO policyDAO = new PolicyDAO();

  private final HdsClient hdsClient;

  private final ComponentPolicyEvaluator componentPolicyEvaluator;

  private final ComponentDetailsLoader componentDetailsLoader;

  private final CLMLicenseManager licenseManager;

  @Inject
  public RepositoryService(HdsClient hdsClient, ComponentPolicyEvaluator componentPolicyEvaluator,
      ComponentDetailsLoader componentDetailsLoader, CLMLicenseManager licenseManager)
  {
    this.hdsClient = hdsClient;
    this.componentPolicyEvaluator = componentPolicyEvaluator;
    this.componentDetailsLoader = componentDetailsLoader;
    this.licenseManager = licenseManager;
  }

  private void checkLicenseFeature() {
    if (!licenseManager.hasRepositoryFirewall()) {
      throw new InvalidLicenseException("Your product license does not support the repository firewall feature.");
    }
  }

  public PolicyEvaluationSummary getPolicyEvaluationSummary(final String repositoryManagerInstanceId,
      final String repositoryPublicId)
  {
    checkLicenseFeature();

    Repository repository = repositoryDAO.getByRepositoryManagerInstanceIdAndPublicId(repositoryManagerInstanceId,
        repositoryPublicId);
    if (repository == null) {
      throw new NotFoundException("Cannot find a repository with repositoryManagerInstanceId=" +
          repositoryManagerInstanceId + " and publicId=" + repositoryPublicId + ".");
    }

    if (!repository.isEnabled()) {
      throw new BadRequestException("Repository " + repositoryPublicId + " is disabled.");
    }

    return getPolicyEvaluationSummary(repository);
  }

  @Authorize(permission = Permission.EVALUATE_COMPONENT)
  PolicyEvaluationSummary getPolicyEvaluationSummary(@AuthzContext(Key.REPOSITORY) final Repository repository)
  {
    return getPolicyEvaluationSummaryInternal(repository);
  }

  public void enableRepository(String repositoryManagerInstanceId, String repositoryPublicId) {
    checkLicenseFeature();

    log.debug("Enabling repository {} for repositoryManagerInstanceId {}", repositoryPublicId,
        repositoryManagerInstanceId);

    Repository repository = repositoryDAO.getByRepositoryManagerInstanceIdAndPublicId(
        repositoryManagerInstanceId, repositoryPublicId);
    if (repository == null) {
      repository = new Repository(null, repositoryPublicId);
    }
    enableRepository(repositoryManagerInstanceId, repository);
  }

  @Authorize(permission = Permission.EVALUATE_COMPONENT)
  void enableRepository(String repositoryManagerInstanceId, @AuthzContext(Key.REPOSITORY) Repository repository) {
    RepositoryManager repositoryManager = repositoryManagerDAO.getByInstanceId(repositoryManagerInstanceId);

    if (repositoryManager == null) {
      repositoryManager = new RepositoryManager(repositoryManagerInstanceId);
      repositoryManagerDAO.insert(repositoryManager);
    }

    repository.setEnabled(true);
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

    log.debug("Setting quarantine to {} for repository {} repositoryManagerInstanceId {}", enabled, repositoryPublicId,
        repositoryManagerInstanceId);

    Repository repository = repositoryDAO.getByRepositoryManagerInstanceIdAndPublicId(repositoryManagerInstanceId,
        repositoryPublicId);
    if (repository == null) {
      throw new NotFoundException("Unknown repository " + repositoryPublicId + " for repositoryManagerInstanceId "
          + repositoryManagerInstanceId + ".");
    }

    if (enabled && !repository.isEnabled()) {
      throw new BadRequestException("Cannot enable quarantine when repository " + repositoryPublicId + " is disabled.");
    }

    setQuarantine(repository, enabled);

    String enabledState = enabled ? "enabled" : "disabled";
    log.info("Quarantine is {} for repository {} repositoryManagerId {}", enabledState, repositoryPublicId,
        repositoryManagerInstanceId);
  }

  @Authorize(permission = Permission.EVALUATE_COMPONENT)
  void setQuarantine(@AuthzContext(Key.REPOSITORY) final Repository repository, final boolean enabled) {
    repository.setQuarantineEnabled(enabled);
    repositoryDAO.update(repository);
  }

  public RepositoryComponentEvaluationResult evaluateComponentWithQuarantine(final String repositoryManagerInstanceId,
      final String repositoryPublicId, final RepositoryComponentEvaluationDataRequest componentEvaluationDataRequest)
  {
    checkLicenseFeature();

    log.debug("Evaluating component with quarantine for repository {} for repositoryManagerInstanceId {}",
        repositoryPublicId, repositoryManagerInstanceId);

    Repository repository = repositoryDAO.getByRepositoryManagerInstanceIdAndPublicId(repositoryManagerInstanceId,
        repositoryPublicId);
    if (repository == null) {
      throw new NotFoundException("Unknown repository " + repositoryPublicId + " for repositoryManagerInstanceId "
          + repositoryManagerInstanceId + ".");
    }

    return evaluateComponentWithQuarantine(repository, componentEvaluationDataRequest);
  }

  public void evaluateComponents(String repositoryManagerInstanceId, String repositoryPublicId,
      RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList)
  {
    checkLicenseFeature();

    log.debug("Evaluating components for repository {} for repositoryManagerInstanceId {}", repositoryPublicId,
        repositoryManagerInstanceId);

    Repository repository = repositoryDAO.getByRepositoryManagerInstanceIdAndPublicId(repositoryManagerInstanceId,
        repositoryPublicId);
    if (repository == null) {
      throw new NotFoundException("Unknown repository " + repositoryPublicId + " for repositoryManagerInstanceId "
          + repositoryManagerInstanceId + ".");
    }

    evaluateComponents(repository, componentEvaluationDataRequestList);
  }

  private void truncateHashes(RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList) {
    for (RepositoryComponentEvaluationDataRequest componentEvaluationDataRequest : componentEvaluationDataRequestList.components) {
      truncateHash(componentEvaluationDataRequest);
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
  RepositoryComponentEvaluationResult evaluateComponentWithQuarantine(@AuthzContext(Key.REPOSITORY) final Repository repository,
      final RepositoryComponentEvaluationDataRequest componentEvaluationDataRequest)
  {
    long start = System.currentTimeMillis();

    if (!repository.isEnabled() || !repository.isQuarantineEnabled()) {
      repository.setEnabled(true);
      repository.setQuarantineEnabled(true);
      repositoryDAO.update(repository);
    }

    validateEvaluateRequest(componentEvaluationDataRequest);

    Date now = new Date();

    truncateHash(componentEvaluationDataRequest);

    RepositoryComponentEvaluationDataRequestList hdsRequest = new RepositoryComponentEvaluationDataRequestList();
    hdsRequest.components.add(componentEvaluationDataRequest);
    ComponentEvaluationDataList componentEvaluationDataList = getComponentDetailsFromHds(hdsRequest);

    ComponentEvaluationData componentEvaluationData = componentEvaluationDataList.components.get(0);

    // Use the claimed component data if found
    NamedComponentDetails componentDetails = componentDetailsLoader.getComponentDetailsLocally(
        null /* componentIdentifier */, componentEvaluationData.hash);
    if (componentDetails == null) {
      componentDetails = ComponentDetailsAdapter.convert(componentEvaluationData);
      componentDetails.setIdentificationSource(IdentificationSource.SONATYPE.getId());
    }

    Component component = augmentComponentDetails(repository, componentDetails);
    // Evaluate the policies
    PolicyResults policyResults = componentPolicyEvaluator.evaluate(repository.getId(),
        new Stage(DevelopStageType.ID), Collections.singletonList(component), false /* forMonitoring */);

    boolean quarantine = !policyResults.getActiveAlerts().isEmpty();
    Date quarantineTime = quarantine ? now : null;

    persistEvaluationResults(repository, componentEvaluationDataRequest.pathname, now,
      componentDetails, policyResults, true, quarantineTime);
    log.debug("Evaluated {} components for repository id {} in {} ms.", componentEvaluationDataList.components.size(),
        repository.getId(), System.currentTimeMillis() - start);

    RepositoryComponentEvaluationResult repositoryComponentEvaluationResult = new RepositoryComponentEvaluationResult();
    repositoryComponentEvaluationResult.quarantine = quarantine;

    return repositoryComponentEvaluationResult;
  }

  @Authorize(permission = Permission.EVALUATE_COMPONENT)
  void evaluateComponents(@AuthzContext(Key.REPOSITORY) Repository repository,
      RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList)
  {
    long start = System.currentTimeMillis();

    if (!repository.isEnabled()) {
      repository.setEnabled(true);
      repositoryDAO.update(repository);
    }

    if (componentEvaluationDataRequestList == null || componentEvaluationDataRequestList.isEmpty()) {
      return;
    }
    validateEvaluateRequest(componentEvaluationDataRequestList);

    Date now = new Date();

    truncateHashes(componentEvaluationDataRequestList);

    ComponentEvaluationDataList componentEvaluationDataList = getComponentDetailsFromHds(componentEvaluationDataRequestList);
    for (int requestIndex = 0; requestIndex < componentEvaluationDataRequestList.components.size(); requestIndex++) {
      RepositoryComponentEvaluationDataRequest componentEvaluationRequest = componentEvaluationDataRequestList.components
          .get(requestIndex);
      ComponentEvaluationData componentEvaluationData = componentEvaluationDataList.components.get(requestIndex);
      if (componentEvaluationData.requestIndex != requestIndex) {
        throw new IllegalStateException("The request index does not match. Expected " + requestIndex + ", but found "
            + componentEvaluationData.requestIndex + ".");
      }

      // Use the claimed component data if found
      NamedComponentDetails componentDetails = componentDetailsLoader.getComponentDetailsLocally(
          null /* componentIdentifier */, componentEvaluationData.hash);
      if (componentDetails == null) {
        componentDetails = ComponentDetailsAdapter.convert(componentEvaluationData);
        componentDetails.setIdentificationSource(IdentificationSource.SONATYPE.getId());
      }

      Component component = augmentComponentDetails(repository, componentDetails);
      // Evaluate the policies
      PolicyResults policyResults = componentPolicyEvaluator.evaluate(repository.getId(),
          new Stage(DevelopStageType.ID), Collections.singletonList(component), false /* forMonitoring */);

      persistEvaluationResults(repository, componentEvaluationRequest.pathname, now, componentDetails, policyResults,
          false, null);
    }

    log.debug("Evaluated {} components for repository id {} in {} ms.", componentEvaluationDataList.components.size(),
        repository.getId(), System.currentTimeMillis() - start);
  }

  private void persistEvaluationResults(Repository repository, String pathname, Date evaluationTime,
      ComponentDetails componentDetails, PolicyResults policyResults, boolean canBeQuarantined, Date quarantineTime)
  {
    pathname = normalizePathname(pathname);
    try (TransactionContext tx = repositoryComponentDAO.createTransactionContext()) {
      tx.begin();

      persistRepositoryComponent(tx, repository, pathname, evaluationTime, componentDetails, canBeQuarantined,
          quarantineTime);
      persistPolicyViolations(tx, repository, pathname, evaluationTime, policyResults);

      tx.commit();
    }
  }

  private void persistRepositoryComponent(TransactionContext tx, Repository repository, String pathname,
      Date evaluationTime, ComponentDetails componentDetails, boolean canBeQuarantined, Date quarantineTime)
  {
    RepositoryComponent repositoryComponent = repositoryComponentDAO.getByRepositoryIdAndPathname(tx,
        repository.getId(), pathname);
    if (repositoryComponent == null) {
      repositoryComponent = new RepositoryComponent(repository.getId(), pathname, evaluationTime,
          componentDetails.getHash(), componentDetails.getComponentIdentifier(), componentDetails.getMatchState(),
          componentDetails.getIdentificationSource(), evaluationTime, canBeQuarantined);
      repositoryComponent.setQuarantineTime(quarantineTime);
      repositoryComponentDAO.insert(tx, repositoryComponent);
    }
    else {
      repositoryComponent.setHash(componentDetails.getHash());
      repositoryComponent.setComponentIdentifier(componentDetails.getComponentIdentifier());
      repositoryComponent.setMatchStateId(componentDetails.getMatchState());
      repositoryComponent.setIdentificationSourceId(componentDetails.getIdentificationSource());
      repositoryComponent.setLastEvaluationTime(evaluationTime);
      repositoryComponentDAO.update(tx, repositoryComponent);
    }
  }
  
  private void persistPolicyViolations(TransactionContext tx, Repository repository, String pathname,
      Date evaluationTime, PolicyResults policyResults)
  {
    // Update the current last RepositoryPolicyViolations for this component
    List<RepositoryPolicyViolation> lastPolicyViolations = repositoryPolicyViolationDAO
        .getActiveByRepositoryIdAndPathname(tx, repository.getId(), pathname);
    for (RepositoryPolicyViolation policyViolation : lastPolicyViolations) {
      policyViolation.setActive(false);
      repositoryPolicyViolationDAO.update(tx, policyViolation);
    }
    // Insert new RepositoryPolicyViolations for this component
    List<PolicyAlert> allPolicyAlerts = new ArrayList<>();
    allPolicyAlerts.addAll(policyResults.getActiveAlerts());
    allPolicyAlerts.addAll(policyResults.getWaivedAlerts());
    for (PolicyAlert policyAlert : allPolicyAlerts) {
      PolicyFact policyFact = policyAlert.getTrigger();
      Policy policy = policyDAO.getByIdNotNull(policyFact.getPolicyId());
      PolicyThreatCategory threatCategory = policy.getThreatCategory();
      for (ComponentFact componentFact : policyFact.getComponentFacts()) {
        RepositoryPolicyViolation policyViolation = new RepositoryPolicyViolation(repository.getId(), pathname,
            evaluationTime, policy.getId(), policy.getName(), policyFact.getThreatLevel(), threatCategory,
            componentFact.getHash(), componentFact.getComponentIdentifier(), componentFact.getConstraintFacts());
        for (Action action : policyAlert.getActions()) {
          // Don't save notification data into policy violations here because we don't want to send notifications for
          // policy violations on repository components. At least not yet.
          if (!Action.ID_NOTIFY.equals(action.getActionTypeId())) {
            policyViolation.setActionTypeId(action.getActionTypeId());
            break;
          }
        }
        PolicyWaiver policyWaiver = policyResults.getPolicyWaiver(componentFact);
        policyViolation.setWaived(policyWaiver != null);
        repositoryPolicyViolationDAO.insert(tx, policyViolation);
      }
    }
  }

  private Component augmentComponentDetails(Repository repository, NamedComponentDetails componentDetails) {
    try {
      return componentDetailsLoader.augmentComponentDetails(repository, componentDetails);
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private ComponentEvaluationDataList getComponentDetailsFromHds(
      final RepositoryComponentEvaluationDataRequestList hdsRequest)
  {
    try {
      long start = System.currentTimeMillis();

      ComponentEvaluationDataList result = hdsClient.post(ComponentEvaluationDataList.class,
          HDS_COMPONENT_DETAILS_PATH, hdsRequest);

      log.debug("Got component details from HDS for {} components in {} ms.",
          hdsRequest.components.size(), System.currentTimeMillis() - start);

      return result;
    }
    catch (IOException e) {
      throw new RuntimeException("Failed to get component details from HDS: " + e.getMessage(), e);
    }
  }

  public RepositoryReportSummary getReportSummary(String repositoryManagerInstanceId, String repositoryPublicId) {
    checkLicenseFeature();

    log.debug("Get report summary for repository {} for repositoryManagerInstanceId {}", repositoryPublicId,
        repositoryManagerInstanceId);

    Repository repository = repositoryDAO.getByRepositoryManagerInstanceIdAndPublicId(repositoryManagerInstanceId,
        repositoryPublicId);
    if (repository == null) {
      throw new NotFoundException("Unknown repository " + repositoryPublicId + " for repositoryManagerInstanceId "
          + repositoryManagerInstanceId + ".");
    }

    return getReportSummary(repository);
  }

  @Authorize(permission = Permission.READ)
  RepositoryReportSummary getReportSummary(@AuthzContext(Key.REPOSITORY) Repository repository) {
    RepositoryReportSummary summary = new RepositoryReportSummary();
    summary.knownComponentCount = repositoryComponentDAO.getKnownComponentCountByRepositoryId(repository.getId());
    summary.totalComponentCount = repositoryComponentDAO.getComponentCountByRepositoryId(repository.getId());

    PolicyEvaluationSummary policyEvalSummary = this.getPolicyEvaluationSummaryInternal(repository);
    summary.criticalComponentCount = policyEvalSummary.getCriticalComponentCount();
    summary.severeComponentCount = policyEvalSummary.getSevereComponentCount();
    summary.moderateComponentCount = policyEvalSummary.getModerateComponentCount();
    summary.affectedComponentCount = policyEvalSummary.getAffectedComponentCount();

    return summary;
  }

  private PolicyEvaluationSummary getPolicyEvaluationSummaryInternal(final Repository repository) {
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

    PolicyEvaluationSummary policyEvaluationSummary = new PolicyEvaluationSummary();
    policyEvaluationSummary.setCriticalComponentCount(criticalCount);
    policyEvaluationSummary.setSevereComponentCount(severeCount);
    policyEvaluationSummary.setModerateComponentCount(moderateCount);
    policyEvaluationSummary.setAffectedComponentCount(criticalCount + severeCount + moderateCount);

    policyEvaluationSummary.setReportUrl(UserInterfaceLinksResource.getRepositoryReportUrl(
        repositoryManagerDAO.getById(repository.getRepositoryManagerId()).getInstanceId(), repository.getPublicId()));

    return policyEvaluationSummary;
  }

  void removeComponent(String repositoryManagerInstanceId, String repositoryPublicId, String pathname) {
    checkLicenseFeature();

    Repository repository = repositoryDAO.getByRepositoryManagerInstanceIdAndPublicId(repositoryManagerInstanceId,
        repositoryPublicId);
    if (repository == null) {
      throw new NotFoundException("Unknown repository " + repositoryPublicId + " for repositoryManagerInstanceId "
          + repositoryManagerInstanceId + ".");
    }

    removeComponent(repository, normalizePathname(pathname));
  }

  @Authorize(permission = Permission.EVALUATE_COMPONENT)
  void removeComponent(@AuthzContext(Key.REPOSITORY) Repository repository, String pathname) {
    if (!repository.isEnabled()) {
      repository.setEnabled(true);
      repositoryDAO.update(repository);
    }

    try (TransactionContext tx = repositoryComponentDAO.createTransactionContext()) {
      tx.begin();

      for (RepositoryPolicyViolation policyViolation : repositoryPolicyViolationDAO.getActiveByRepositoryIdAndPathname(
          tx, repository.getId(), pathname)) {
        policyViolation.setActive(false);
        repositoryPolicyViolationDAO.update(tx, policyViolation);
      }

      RepositoryComponent repositoryComponent = repositoryComponentDAO.getByRepositoryIdAndPathname(tx,
          repository.getId(), pathname);
      if (repositoryComponent != null) {
        repositoryComponentDAO.delete(tx, repositoryComponent);
      }

      tx.commit();
    }
  }

  private String normalizePathname(String pathname) {
    if (pathname != null && pathname.startsWith("/")) {
      return pathname.substring(1);
    }

    return pathname;
  }
}
