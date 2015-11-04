/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration.repository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList.ComponentEvaluationData;
import com.sonatype.clm.dto.model.component.NamedComponentDetails;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationData;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataList;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList.RepositoryComponentEvaluationDataRequest;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.clm.dto.model.policy.RepositoryPolicyEvaluationSummary;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.component.ComponentDetailsAdapter;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.hds.ComponentDetailsLoader;
import com.sonatype.insight.brain.hds.FirewallAuditHdsClient;
import com.sonatype.insight.brain.hds.FirewallQuarantineHdsClient;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.landing.UserInterfaceLinksResource;
import com.sonatype.insight.brain.model.HashHelper;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.IdentificationSource;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.DevelopStageType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.policy.evaluator.ComponentPolicyEvaluator;
import com.sonatype.insight.brain.policy.evaluator.PolicyResults;
import com.sonatype.insight.brain.policy.evaluator.PolicyThreats;
import com.sonatype.insight.brain.policy.evaluator.PolicyThreatsAdapter;
import com.sonatype.insight.brain.product.license.CLMLicenseManager;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.repository.RepositoryPolicyThreatDTO;
import com.sonatype.insight.brain.repository.RepositoryPolicyViolationDTO;
import com.sonatype.insight.brain.repository.RepositoryReportDetail;
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

  private final FirewallAuditHdsClient auditHdsClient;

  private final FirewallQuarantineHdsClient quarantineHdsClient;

  private final ComponentPolicyEvaluator componentPolicyEvaluator;

  private final ComponentDetailsLoader componentDetailsLoader;

  private final CLMLicenseManager licenseManager;

  private final PolicyThreatsAdapter policyThreatsAdapter;

  @Inject
  public RepositoryService(FirewallAuditHdsClient auditHdsClient, FirewallQuarantineHdsClient quarantineHdsClient,
      ComponentPolicyEvaluator componentPolicyEvaluator, ComponentDetailsLoader componentDetailsLoader,
      CLMLicenseManager licenseManager, PolicyThreatsAdapter policyThreatsAdapter)
  {
    this.auditHdsClient = auditHdsClient;
    this.quarantineHdsClient = quarantineHdsClient;
    this.componentPolicyEvaluator = componentPolicyEvaluator;
    this.componentDetailsLoader = componentDetailsLoader;
    this.licenseManager = licenseManager;
    this.policyThreatsAdapter = policyThreatsAdapter;
  }

  private void checkLicenseFeature() {
    if (!licenseManager.hasRepositoryFirewall()) {
      throw new InvalidLicenseException("Your product license does not support the repository firewall feature.");
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

    String enableDisableMessage = enable ? "Enabling" : "Disabling";
    log.debug("{} repository {} for repositoryManagerInstanceId {}", enableDisableMessage, repositoryPublicId,
        repositoryManagerInstanceId);

    Repository repository = repositoryDAO.getByRepositoryManagerInstanceIdAndPublicId(
        repositoryManagerInstanceId, repositoryPublicId);
    if (repository == null) {
      repository = new Repository(null, repositoryPublicId);
    }
    setEnabled(repositoryManagerInstanceId, repository, enable);
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

    log.debug("Setting quarantine to {} for repository {} repositoryManagerInstanceId {}", enabled, repositoryPublicId,
        repositoryManagerInstanceId);

    Repository repository = repositoryDAO.getByRepositoryManagerInstanceIdAndPublicIdNotNull(
        repositoryManagerInstanceId, repositoryPublicId);

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
    try (TransactionContext tx = repositoryDAO.createTransactionContext()) {
      tx.begin();

      repository.setQuarantineEnabled(enabled);
      repositoryDAO.update(tx, repository);
      
      if (!enabled) {
        // Un-quarantine the quarantined repository components
        Date unquarantineTime = new Date();
        List<RepositoryComponent> quarantinedComponents = repositoryComponentDAO.getQuarantinedByRepositoryId(tx,
            repository.getId());
        for (RepositoryComponent quarantinedComponent : quarantinedComponents) {
          quarantinedComponent.setUnquarantineTime(unquarantineTime);
          repositoryComponentDAO.update(tx, quarantinedComponent);
        }
      }

      tx.commit();
    }
  }

  public RepositoryComponentEvaluationDataList evaluateComponents(String repositoryManagerInstanceId, String repositoryPublicId,
      RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList, boolean withQuarantine)
  {
    checkLicenseFeature();

    log.debug("Evaluating components for repository {} with quarantine {} for repositoryManagerInstanceId {}",
        repositoryPublicId, withQuarantine, repositoryManagerInstanceId);

    Repository repository = repositoryDAO.getByRepositoryManagerInstanceIdAndPublicIdNotNull(
        repositoryManagerInstanceId, repositoryPublicId);

    return evaluateComponents(repository, componentEvaluationDataRequestList, withQuarantine);
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
  RepositoryComponentEvaluationDataList evaluateComponents(@AuthzContext(Key.REPOSITORY) Repository repository,
      RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList, final boolean withQuarantine)
  {
    long start = System.currentTimeMillis();

    RepositoryComponentEvaluationDataList componentEvaluationResultList =
        new RepositoryComponentEvaluationDataList();

    if (!repository.isEnabled() || (withQuarantine && !repository.isQuarantineEnabled())) {
      repository.setEnabled(true);
      if (withQuarantine) {
        // If this is for quarantine make sure it's enabled in IQ
        repository.setQuarantineEnabled(true);
      }
      repositoryDAO.update(repository);
    }

    if (componentEvaluationDataRequestList == null || componentEvaluationDataRequestList.isEmpty()) {
      return componentEvaluationResultList;
    }
    validateEvaluateRequest(componentEvaluationDataRequestList);

    Date now = new Date();

    truncateHashes(componentEvaluationDataRequestList);

    ComponentEvaluationDataList componentEvaluationDataList = getComponentDetailsFromHds(withQuarantine,
        componentEvaluationDataRequestList);
    List<Component> components = new ArrayList<>();
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
      component.addPathname(normalizePathname(componentEvaluationRequest.pathname));
      components.add(component);
    }

    // Evaluate the policies
    PolicyResults policyResults = componentPolicyEvaluator.evaluate(repository.getId(),
        new Stage(DevelopStageType.ID), components, false /* forMonitoring */);
    
    for (int requestIndex = 0; requestIndex < componentEvaluationDataRequestList.components.size(); requestIndex++) {
      Component component = components.get(requestIndex);

      boolean quarantine = withQuarantine && hasComponentFact(policyResults.getActiveAlerts(), component);
      Date quarantineTime = quarantine ? now : null;

      persistEvaluationResults(repository, now, component, policyResults, withQuarantine, quarantineTime);

      RepositoryComponentEvaluationData repositoryComponentEvaluationResult = new RepositoryComponentEvaluationData();
      repositoryComponentEvaluationResult.requestIndex = requestIndex;
      repositoryComponentEvaluationResult.quarantine = quarantine;
      componentEvaluationResultList.componentEvalResults.add(repositoryComponentEvaluationResult);
    }

    log.debug("Evaluated {} components with quarantine {} for repository id {} in {} ms.",
        componentEvaluationDataList.components.size(), withQuarantine, repository.getId(),
        System.currentTimeMillis() - start);

    return componentEvaluationResultList;
  }

  private void persistEvaluationResults(Repository repository, Date evaluationTime, Component component,
      PolicyResults policyResults, boolean canBeQuarantined, Date quarantineTime)
  {
    try (TransactionContext tx = repositoryComponentDAO.createTransactionContext()) {
      tx.begin();

      persistRepositoryComponent(tx, repository, evaluationTime, component, canBeQuarantined, quarantineTime);
      persistPolicyViolations(tx, repository, evaluationTime, component, policyResults);

      tx.commit();
    }
  }

  private void persistRepositoryComponent(TransactionContext tx, Repository repository, Date evaluationTime,
      Component component, boolean canBeQuarantined, Date quarantineTime)
  {
    String pathname = component.getPathnames().get(0);
    RepositoryComponent repositoryComponent = repositoryComponentDAO.getByRepositoryIdAndPathname(tx,
        repository.getId(), pathname);
    if (repositoryComponent == null) {
      repositoryComponent = new RepositoryComponent(repository.getId(), pathname, evaluationTime,
          component.getHash(), component.getComponentIdentifier(), component.getMatchState().getId(),
          component.getIdentificationSource().getId(), evaluationTime, canBeQuarantined);
      repositoryComponent.setQuarantineTime(quarantineTime);
      repositoryComponentDAO.insert(tx, repositoryComponent);
    }
    else {
      repositoryComponent.setHash(component.getHash());
      repositoryComponent.setComponentIdentifier(component.getComponentIdentifier());
      repositoryComponent.setMatchStateId(component.getMatchState().getId());
      repositoryComponent.setIdentificationSourceId(component.getIdentificationSource().getId());
      repositoryComponent.setLastEvaluationTime(evaluationTime);
      if (canBeQuarantined) {
        repositoryComponent.setCanBeQuarantined(canBeQuarantined);
        repositoryComponent.setQuarantineTime(quarantineTime);
        repositoryComponent.setUnquarantineTime(null);
      }
      repositoryComponentDAO.update(tx, repositoryComponent);
    }
  }

  private void persistPolicyViolations(TransactionContext tx, Repository repository, Date evaluationTime,
      Component component, PolicyResults policyResults)
  {
    String pathname = component.getPathnames().get(0);
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
      ComponentFact componentFact = getComponentFact(policyAlert, component);
      if (componentFact == null) {
        continue;
      }
      RepositoryPolicyViolation policyViolation = createRepositoryPolicyViolation(policyAlert, componentFact, pathname,
          repository, evaluationTime, policyResults.getPolicyWaiver(componentFact) != null);
      repositoryPolicyViolationDAO.insert(tx, policyViolation);
    }
  }

  private RepositoryPolicyViolation createRepositoryPolicyViolation(PolicyAlert policyAlert,
      ComponentFact componentFact, String pathname, Repository repository, Date evaluationTime, boolean waived)
  {
    PolicyFact policyFact = policyAlert.getTrigger();
    Policy policy = policyDAO.getByIdNotNull(policyFact.getPolicyId());
    PolicyThreatCategory threatCategory = policy.getThreatCategory();
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
    policyViolation.setWaived(waived);
    return policyViolation;
  }

  private boolean hasComponentFact(List<PolicyAlert> policyAlerts, Component component) {
    for (PolicyAlert policyAlert : policyAlerts) {
      if (getComponentFact(policyAlert, component) != null) {
        return true;
      }
    }
    return false;
  }

  private ComponentFact getComponentFact(PolicyAlert policyAlert, Component component) {
    for (ComponentFact componentFact : policyAlert.getTrigger().getComponentFacts()) {
      if (component.getPathnames().equals(componentFact.getPathnames())) {
        return componentFact;
      }
    }
    return null;
  }

  private Component augmentComponentDetails(Repository repository, NamedComponentDetails componentDetails) {
    try {
      return componentDetailsLoader.augmentComponentDetails(repository, componentDetails);
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private ComponentEvaluationDataList getComponentDetailsFromHds(boolean withQuarantine,
      final RepositoryComponentEvaluationDataRequestList hdsRequest)
  {
    try {
      long start = System.currentTimeMillis();

      HdsClient hdsClient = withQuarantine ? quarantineHdsClient : auditHdsClient;
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

  public RepositoryReportSummary getReportSummary(String repositoryId) {
    checkLicenseFeature();

    log.debug("Get report summary for repository {}", repositoryId);

    Repository repository = repositoryDAO.getByIdNotNull(repositoryId);

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
    log.debug("Get report details for repository {}", repositoryId);

    checkLicenseFeature();

    final Repository repository = repositoryDAO.getByIdNotNull(repositoryId);

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
