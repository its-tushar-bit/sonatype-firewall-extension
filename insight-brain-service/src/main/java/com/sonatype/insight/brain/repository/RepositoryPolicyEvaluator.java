/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
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
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.api.v2.ApiFirewallMetricsService;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditSession;
import com.sonatype.insight.brain.component.ComponentDetailsAdapter;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLock;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLockManager;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.eventbus.AsyncEventBus;
import com.sonatype.insight.brain.hds.ComponentDetailsLoader;
import com.sonatype.insight.brain.hds.ComponentDetailsLoaderFactory;
import com.sonatype.insight.brain.hds.FirewallAuditHdsClient;
import com.sonatype.insight.brain.hds.FirewallQuarantineHdsClient;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.hds.HdsClientAnalytics;
import com.sonatype.insight.brain.integration.repository.FirewallIgnorePatternService;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.ProxyStageType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.policy.evaluator.ComponentPolicyEvaluator;
import com.sonatype.insight.brain.policy.evaluator.PolicyResults;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationDiff;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationDigester;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLogEvent;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLoggerFactory;
import com.sonatype.insight.brain.policy.violation.RepositoryPolicyViolationLogger;
import com.sonatype.insight.brain.telemetry.RepositoryComponentTelemetry.ReleaseQuarantineType;
import com.sonatype.insight.brain.telemetry.RepositoryComponentTelemetry.RepositoryComponentTelemetryEventType;
import com.sonatype.insight.brain.telemetry.RepositoryComponentTelemetryCreator;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.json.store.JsonUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.18.0
 */
@Named
public class RepositoryPolicyEvaluator
{
  public static final String HDS_COMPONENT_DETAILS_PATH = "rest/component/details/firewall";

  private static final Logger log = LoggerFactory.getLogger(RepositoryPolicyEvaluator.class);

  private final ComponentPolicyEvaluator componentPolicyEvaluator;

  private final RepositoryComponentDAO repositoryComponentDAO;

  private final RepositoryPolicyViolationDAO repositoryPolicyViolationDAO;

  private final PolicyDAO policyDAO;

  private final FirewallAuditHdsClient auditHdsClient;

  private final FirewallQuarantineHdsClient quarantineHdsClient;

  private final PolicyViolationLoggerFactory policyViolationLoggerFactory;

  private final FirewallIgnorePatternService firewallIgnorePatternService;

  private final ComponentDetailsLoaderFactory componentDetailsLoaderFactory;

  private final RepositoryComponentDeleteService repositoryComponentDeleteService;

  private final RepositoryPolicyAlertEmailer repositoryPolicyAlertEmailer;

  private final RepositoryComponentTelemetryCreator repositoryComponentTelemetryCreator;

  private final ClusterLockManager clusterLockManager;

  private final AsyncEventBus eventBus;

  private final ApiFirewallMetricsService firewallMetricsService;

  @Inject
  public RepositoryPolicyEvaluator(
      ComponentPolicyEvaluator componentPolicyEvaluator,
      RepositoryComponentDAO repositoryComponentDAO,
      RepositoryPolicyViolationDAO repositoryPolicyViolationDAO,
      PolicyDAO policyDAO,
      FirewallAuditHdsClient auditHdsClient,
      FirewallQuarantineHdsClient quarantineHdsClient,
      PolicyViolationLoggerFactory policyViolationLoggerFactory,
      FirewallIgnorePatternService firewallIgnorePatternService,
      ComponentDetailsLoaderFactory componentDetailsLoaderFactory,
      RepositoryComponentDeleteService repositoryComponentDeleteService,
      RepositoryPolicyAlertEmailer repositoryPolicyAlertEmailer,
      RepositoryComponentTelemetryCreator repositoryComponentTelemetryCreator,
      final ClusterLockManager clusterLockManager,
      AsyncEventBus eventBus,
      ApiFirewallMetricsService firewallMetricsService
  )
  {
    this.componentPolicyEvaluator = componentPolicyEvaluator;
    this.repositoryComponentDAO = repositoryComponentDAO;
    this.repositoryPolicyViolationDAO = repositoryPolicyViolationDAO;
    this.policyDAO = policyDAO;
    this.auditHdsClient = auditHdsClient;
    this.quarantineHdsClient = quarantineHdsClient;
    this.policyViolationLoggerFactory = policyViolationLoggerFactory;
    this.firewallIgnorePatternService = firewallIgnorePatternService;
    this.componentDetailsLoaderFactory = componentDetailsLoaderFactory;
    this.repositoryComponentDeleteService = repositoryComponentDeleteService;
    this.repositoryPolicyAlertEmailer = repositoryPolicyAlertEmailer;
    this.repositoryComponentTelemetryCreator = repositoryComponentTelemetryCreator;
    this.clusterLockManager = clusterLockManager;
    this.eventBus = eventBus;
    this.firewallMetricsService = firewallMetricsService;
  }

  public RepositoryComponentEvaluationDataList evaluateForMonitoring(
      Repository repository,
      RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList)
  {
    return evaluate(repository, componentEvaluationDataRequestList, false /* withQuarantine */,
        true /* persistEvaluationResults */, null /* clientUserAgent */, true /* forMonitoring */);
  }

  public RepositoryComponentEvaluationDataList evaluate(
      Repository repository,
      RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList,
      final boolean withQuarantine,
      final String clientUserAgent)
  {
    return evaluate(repository, componentEvaluationDataRequestList, withQuarantine, true /* persistEvaluationResults */,
        clientUserAgent, false /* forMonitoring */);
  }

  public RepositoryComponentEvaluationDataList evaluate(
      Repository repository,
      RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList,
      boolean withQuarantine,
      boolean persistEvaluationResults,
      String clientUserAgent,
      boolean forMonitoring)
  {
    long start = System.currentTimeMillis();

    ComponentEvaluationDataList componentDetailsFromHdsList =
        getComponentDetailsFromHds(repository, withQuarantine, componentEvaluationDataRequestList, clientUserAgent);

    RepositoryComponentEvaluationDataList result = evaluate(repository, componentEvaluationDataRequestList,
        componentDetailsFromHdsList, withQuarantine, persistEvaluationResults, forMonitoring);

    log.debug("Evaluated {} components with quarantine {} for repository {}:{} ({}) because of {} in {} ms.",
        componentEvaluationDataRequestList.components.size(), withQuarantine, repository.getRepositoryManagerId(),
        repository.getPublicId(), repository.getId(), componentEvaluationDataRequestList.cause,
        System.currentTimeMillis() - start);

    return result;
  }

  public RepositoryComponentEvaluationDataList evaluate(
      Repository repository,
      RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList,
      ComponentEvaluationDataList componentDetailsFromHds,
      boolean withQuarantine,
      boolean persistEvaluationResults,
      boolean forMonitoring)
  {
    RepositoryComponentEvaluationDataList componentEvaluationResultList = new RepositoryComponentEvaluationDataList();

    Date now = new Date();

    Predicate<String> componentPathnameMatchesIgnorePattern =
        firewallIgnorePatternService.componentPathnameMatchesIgnorePattern(repository);
    List<Component> components = new ArrayList<>();
    ComponentDetailsLoader componentDetailsLoader = componentDetailsLoaderFactory.newInstance(repository);

    List<String> hashes = validateIndexesMatchAndGetHashes(componentEvaluationDataRequestList, componentDetailsFromHds,
        componentPathnameMatchesIgnorePattern);

    Map<String, NamedComponentDetails> namedComponentDetails =
        ComponentDetailsLoader.getComponentDetailsLocallyByHashes(hashes);

    for (int requestIndex = 0; requestIndex < componentEvaluationDataRequestList.components.size(); requestIndex++) {
      RepositoryComponentEvaluationDataRequest componentEvaluationRequest =
          componentEvaluationDataRequestList.components.get(requestIndex);
      ComponentEvaluationData componentEvaluationData = componentDetailsFromHds.components.get(requestIndex);

      // If the component matches the repository ignore pattern then
      // 1. Remove it if it is already persisted
      // 2. Do not evaluate policies on it
      // 3. Do not persist it
      if (componentPathnameMatchesIgnorePattern.test(componentEvaluationRequest.pathname)) {
        RepositoryComponent repositoryComponent = repositoryComponentDAO
            .getByRepositoryIdAndPathname(repository.getId(), componentEvaluationRequest.pathname);
        if (repositoryComponent != null) {
          repositoryComponentDeleteService.deleteComponent(repositoryComponent);
        }
        components.add(null);
      }
      else {
        NamedComponentDetails componentDetails = namedComponentDetails.get(componentEvaluationData.hash);

        if (componentDetails == null) {
          componentDetails = ComponentDetailsAdapter.convert(componentEvaluationData);
          componentDetails.setIdentificationSource(IdentificationSource.SONATYPE.getId());
        }
        Component component = componentDetailsLoader.augmentComponentDetails(componentDetails);
        component.addPathname(componentEvaluationRequest.pathname);
        component.setAnalyzerFeatures(componentEvaluationData.analyzerFeatures);
        components.add(component);
      }
    }

    // Evaluate the policies
    List<Policy> policies = policyDAO.getApplicableByOwnerIdWithHierarchy(repository.getId());
    PolicyResults policyResults = componentPolicyEvaluator.evaluate(repository.getId(), new Stage(ProxyStageType.ID),
        policies, components.stream().filter(Objects::nonNull).collect(Collectors.toList()), false /* forMonitoring */);

    // Only notify new component evaluation policy violations
    boolean shouldSendNotifications =
        RepositoryComponentEvaluationDataRequestList.NEW_COMPONENT.equals(componentEvaluationDataRequestList.cause);

    Map<String, RepositoryComponent> repositoryComponents = Collections.emptyMap();
    if (withQuarantine) {
      repositoryComponents = getRepositoryComponents(repository, components);
    }

    CreateRepositoryPolicyViolationsEvent event = null;
    if (firewallMetricsService.isValidProductLicense()) {
      event = new CreateRepositoryPolicyViolationsEvent();
    }

    for (int requestIndex = 0; requestIndex < componentEvaluationDataRequestList.components.size(); requestIndex++) {
      RepositoryComponentEvaluationData repositoryComponentEvaluationResult = new RepositoryComponentEvaluationData();
      repositoryComponentEvaluationResult.requestIndex = requestIndex;
      Component component = components.get(requestIndex);
      if (component != null) {
        if (component.getCatalogDate() != null) {
          repositoryComponentEvaluationResult.catalogDate = new Date(component.getCatalogDate());
        }
        if (persistEvaluationResults) {
          RepositoryComponent repositoryComponent = persistEvaluationResults(
              repository,
              now,
              component,
              policyResults,
              policies,
              withQuarantine,
              shouldSendNotifications,
              forMonitoring,
              event);
          repositoryComponentEvaluationResult.quarantine = repositoryComponent.isQuarantined();
        }
        else {
          repositoryComponentEvaluationResult.policyAlerts = getPolicyAlerts(policyResults, component);
          if (withQuarantine) {
            RepositoryComponent repositoryComponent =
                repositoryComponents.getOrDefault(component.getPathnames().get(0), null);
            repositoryComponentEvaluationResult.quarantine =
                canQuarantine(repositoryComponentEvaluationResult.policyAlerts, repositoryComponent, component);
          }
        }
      }
      componentEvaluationResultList.componentEvalResults.add(repositoryComponentEvaluationResult);
    }

    if (event != null && !event.repositoryPolicyViolations.isEmpty()) {
      eventBus.post(event);
    }

    // Only notify new component evaluation policy violations
    if (shouldSendNotifications) {
      repositoryPolicyAlertEmailer.sendNotifications(repository, policyResults.getActiveNotifications());
    }
    return componentEvaluationResultList;
  }

  private Map<String, RepositoryComponent> getRepositoryComponents(
      final Repository repository,
      final List<Component> components)
  {
    List<String> pathnames = components.stream()
        .filter(Objects::nonNull)
        .map(component -> component.getPathnames().get(0))
        .collect(Collectors.toList());

    return repositoryComponentDAO.getByRepositoryIdAndPathnames(repository.getId(), pathnames).stream()
        .collect(Collectors.toMap(RepositoryComponent::getPathname, Function.identity()));
  }

  private static List<String> validateIndexesMatchAndGetHashes(
      RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList,
      ComponentEvaluationDataList componentDetailsFromHds,
      Predicate<String> componentPathnameMatchesIgnorePattern)
  {
    List<String> hashes = new ArrayList<>();

    for (int requestIndex = 0; requestIndex < componentEvaluationDataRequestList.components.size(); requestIndex++) {
      RepositoryComponentEvaluationDataRequest componentEvaluationRequest =
          componentEvaluationDataRequestList.components.get(requestIndex);
      ComponentEvaluationData componentEvaluationData = componentDetailsFromHds.components.get(requestIndex);
      if (componentEvaluationData.requestIndex != requestIndex) {
        throw new IllegalStateException("The request index does not match. Expected " + requestIndex + ", but found "
            + componentEvaluationData.requestIndex + ".");
      }

      if (!componentPathnameMatchesIgnorePattern.test(componentEvaluationRequest.pathname)) {
        hashes.add(componentEvaluationData.hash);
      }
    }

    return hashes;
  }

  /**
   * If the specified component exists in the db, then return its existing quarantine status. Otherwise, return
   * quarantine status based on policy alerts.
   */
  private boolean canQuarantine(
      List<PolicyAlert> policyAlerts,
      RepositoryComponent repositoryComponent,
      Component component)
  {
    if (repositoryComponent != null && repositoryComponent.getHash().equals(component.getHash())) {
      return repositoryComponent.isQuarantined();
    }
    return shouldQuarantine(policyAlerts, component);
  }

  private List<PolicyAlert> getPolicyAlerts(final PolicyResults policyResults, final Component component) {
    return policyResults.getActiveAlerts().stream()
        .filter(policyAlert -> getComponentFact(policyAlert, component) != null)
        .collect(Collectors.toList());
  }

  private RepositoryComponent persistEvaluationResults(
      Repository repository,
      Date evaluationTime,
      Component component,
      PolicyResults policyResults,
      List<Policy> policies,
      boolean canBeQuarantined,
      boolean isNotificationsToBeSent,
      boolean forMonitoring,
      CreateRepositoryPolicyViolationsEvent event)
  {
    RepositoryComponent repositoryComponent;
    try (ClusterLock clusterLock =
             clusterLockManager.createForRepositoryComponent(repository.getId(), component.getPathnames().get(0));
         TransactionContext tx = policyDAO.createTransactionContext()) {
      clusterLock.lock();
      tx.begin();

      RepositoryPolicyViolationLogger policyViolationLogger =
          policyViolationLoggerFactory.newLogger(evaluationTime, repository);

      // The order of the following calls are important and must not be changed. See: CLM-13853
      persistPolicyViolations(tx, repository, evaluationTime, component, policyResults, policies,
          policyViolationLogger, event);
      repositoryComponent = persistRepositoryComponent(tx, repository, evaluationTime, component,
          canBeQuarantined, policyResults, isNotificationsToBeSent, forMonitoring);

      tx.commit();
      AuditData.get().commitSubEvents();
      policyViolationLogger.log();
    }
    return repositoryComponent;
  }

  private RepositoryComponent persistRepositoryComponent(
      TransactionContext tx,
      Repository repository,
      Date evaluationTime,
      Component component,
      boolean canBeQuarantined,
      PolicyResults policyResults,
      boolean isNotificationsToBeSent,
      boolean forMonitoring)
  {
    String pathname = component.getPathnames().get(0);
    RepositoryComponent repositoryComponent = repositoryComponentDAO.getByRepositoryIdAndPathname(tx,
        repository.getId(), pathname);
    String repositoryComponentId = repositoryComponent == null ? null : repositoryComponent.getId();
    if (repositoryComponent != null && !repositoryComponent.getHash().equals(component.getHash())) {
      if (repositoryComponent.isQuarantined()) {
        try (AuditSession auditSession = AuditData.get().recordSubEvent(AuditEvent.RESET_QUARANTINE, false)) {
          AuditData.get().setRepository(repository).setComponentHash(repositoryComponent.getHash())
              .setData("componentPathname", repositoryComponent.getPathname());
        }
      }
    }
    if (repositoryComponent == null || !repositoryComponent.getHash().equals(component.getHash())) {
      boolean quarantine = canBeQuarantined && shouldQuarantine(policyResults.getActiveAlerts(), component);
      if (quarantine) {
        log.debug("Component {} in repository {}:{} ({}) was quarantined", pathname,
            repository.getRepositoryManagerId(), repository.getPublicId(), repository.getId());
        try (AuditSession auditSession = AuditData.get().recordSystemEvent(AuditEvent.RETAIN_QUARANTINE, false)) {
          AuditData.get().setRepository(repository).setComponentHash(component.getHash())
              .setData("componentPathname", pathname);
        }
      }
      Date quarantineTime = quarantine ? evaluationTime : null;
      repositoryComponent = new RepositoryComponent(repository.getId(), pathname, evaluationTime, component.getHash(),
          component.getComponentIdentifier(), component.getMatchState().getId(), component.getIdentificationSource()
          .getId(), evaluationTime);
      repositoryComponent.setQuarantineTime(quarantineTime);
      repositoryComponent.setAnalyzerFeaturesJson(JsonUtils.format(component.getAnalyzerFeatures()));
      if (repositoryComponentId == null) {
        repositoryComponentDAO.insert(tx, repositoryComponent);
      }
      else {
        repositoryComponent.setId(repositoryComponentId);
        repositoryComponentDAO.update(tx, repositoryComponent);
      }
    }
    else {
      repositoryComponent.setHash(component.getHash());
      repositoryComponent.setComponentIdentifier(component.getComponentIdentifier());
      repositoryComponent.setMatchStateId(component.getMatchState().getId());
      repositoryComponent.setIdentificationSourceId(component.getIdentificationSource().getId());
      repositoryComponent.setLastEvaluationTime(evaluationTime);
      repositoryComponent.setAnalyzerFeaturesJson(JsonUtils.format(component.getAnalyzerFeatures()));

      if (repositoryComponent.isQuarantined() && !shouldQuarantine(policyResults.getActiveAlerts(), component)) {
        // The component is quarantined, but it doesn't have any policy violations/alerts that would quarantine it
        // anymore.
        unquarantineComponent(repository, repositoryComponent, evaluationTime, forMonitoring);
      }

      repositoryComponentDAO.update(tx, repositoryComponent);
    }
    sendRepositoryComponentTelemetry(policyResults, repositoryComponent, repository, isNotificationsToBeSent);
    return repositoryComponent;
  }

  private void unquarantineComponent(
      Repository repository,
      RepositoryComponent repositoryComponent,
      Date evaluationTime,
      boolean forMonitoring)
  {
    if (AuditData.get().getEvent() != null && !AuditData.get().getEvent().equals(AuditEvent.RELEASE_QUARANTINE)) {
      try (AuditSession auditSession = AuditData.get().recordSubEvent(AuditEvent.RELEASE_QUARANTINE, false)) {
        AuditData.get().setRepository(repository).setComponentHash(repositoryComponent.getHash())
            .setData("componentPathname", repositoryComponent.getPathname());
      }
    }

    if (forMonitoring) {
      repositoryComponent.setUnquarantineTimeForMonitoring(evaluationTime);
    }
    else {
      repositoryComponent.setUnquarantineTimeForManualRelease(evaluationTime);
    }

    List<RepositoryPolicyViolation> repositoryPolicyViolations = repositoryPolicyViolationDAO
        .getActiveByRepositoryIdAndPathnameAndWaived(repository.getId(), repositoryComponent.getPathname(), false);
    repositoryComponentTelemetryCreator.sendRepositoryComponentTelemetry(repositoryComponent,
        repositoryPolicyViolations, repository.getRepositoryManagerId(),
        RepositoryComponentTelemetryEventType.RELEASE_QUARANTINE,
        forMonitoring ? ReleaseQuarantineType.AUTO : ReleaseQuarantineType.MANUAL);
  }

  private void sendRepositoryComponentTelemetry(
      final PolicyResults policyResults,
      final RepositoryComponent repositoryComponent,
      final Repository repository,
      final boolean isNotificationsToBeSent)
  {
    List<RepositoryPolicyViolation> repositoryPolicyViolations = repositoryPolicyViolationDAO
        .getActiveByRepositoryIdAndPathname(repository.getId(), repositoryComponent.getPathname());
    repositoryComponentTelemetryCreator.sendRepositoryComponentTelemetry(repositoryComponent,
        repositoryPolicyViolations, repository.getRepositoryManagerId(),
        repositoryComponent.isQuarantined() ? RepositoryComponentTelemetryEventType.QUARANTINE
            : RepositoryComponentTelemetryEventType.AUDIT,
        isNotificationsToBeSent ? policyResults.getActiveNotifications() : Collections.emptyList());
  }

  private void persistPolicyViolations(
      TransactionContext tx,
      Repository repository,
      Date evaluationTime,
      Component component,
      PolicyResults policyResults,
      List<Policy> policies,
      RepositoryPolicyViolationLogger policyViolationLogger,
      CreateRepositoryPolicyViolationsEvent event)
  {
    String pathname = component.getPathnames().get(0);
    // Get the persisted RepositoryPolicyViolations for this component
    List<RepositoryPolicyViolation> oldPolicyViolations =
        repositoryPolicyViolationDAO.getActiveByRepositoryIdAndPathname(tx, repository.getId(), pathname);

    // Build the list of current RepositoryPolicyViolations for this component
    List<RepositoryPolicyViolation> newPolicyViolations = new ArrayList<>();
    List<PolicyAlert> allPolicyAlerts = new ArrayList<>();
    allPolicyAlerts.addAll(policyResults.getActiveAlerts());
    allPolicyAlerts.addAll(policyResults.getWaivedAlerts());
    for (PolicyAlert policyAlert : allPolicyAlerts) {
      ComponentFact componentFact = getComponentFact(policyAlert, component);
      if (componentFact == null) {
        continue;
      }

      Policy policy = policies.stream()
          .filter(p -> p.getId().equals(policyAlert.getTrigger().getPolicyId()))
          .findFirst().get();

      RepositoryPolicyViolation policyViolation = createRepositoryPolicyViolation(policyAlert, policy, componentFact,
          pathname,
          repository, evaluationTime, policyResults.getPolicyWaiver(componentFact));
      newPolicyViolations.add(policyViolation);
    }

    // Diff old and new
    PolicyViolationDiff<RepositoryPolicyViolation> policyViolationDiff =
        PolicyViolationDigester.digestPolicyViolations(oldPolicyViolations, newPolicyViolations);

    // Remove the cleared violations
    for (RepositoryPolicyViolation clearedPolicyViolation : policyViolationDiff.getCleared()) {
      repositoryPolicyViolationDAO.delete(tx, clearedPolicyViolation);
      policyViolationLogger.add(PolicyViolationLogEvent.FIX, clearedPolicyViolation);
    }

    // Insert the new policy violations
    for (RepositoryPolicyViolation newPolicyViolation : policyViolationDiff.getAppeared()) {
      repositoryPolicyViolationDAO.insert(tx, newPolicyViolation);

      if (event != null) {
        event.repositoryPolicyViolations.add(newPolicyViolation);
      }

      policyViolationLogger.add(PolicyViolationLogEvent.CREATE, newPolicyViolation);
      if (newPolicyViolation.isWaived()) {
        policyViolationLogger.add(PolicyViolationLogEvent.WAIVE, newPolicyViolation);
      }
    }

    // Update the existing violations so that 'time' is set and the original violation waive time, if it exists,
    // is brought forward
    for (Map.Entry<RepositoryPolicyViolation, RepositoryPolicyViolation> entry : policyViolationDiff.getSame()
        .entrySet()) {
      RepositoryPolicyViolation oldPolicyViolation = entry.getKey();
      RepositoryPolicyViolation newPolicyViolation = entry.getValue();

      boolean isOldPolicyViolationWaived = oldPolicyViolation.isWaived();
      boolean isNewPolicyViolationWaived = newPolicyViolation.isWaived();

      if (isNewPolicyViolationWaived && isOldPolicyViolationWaived && null != oldPolicyViolation.getWaiveTime()) {
        newPolicyViolation.setWaiveTime(oldPolicyViolation.getWaiveTime());
      }
      newPolicyViolation.setId(oldPolicyViolation.getId());
      repositoryPolicyViolationDAO.update(tx, newPolicyViolation);

      if (!isNewPolicyViolationWaived && isOldPolicyViolationWaived) {
        // The policy violation was un-waived.
        policyViolationLogger.add(PolicyViolationLogEvent.UNWAIVE, newPolicyViolation);
      }
      else if (isNewPolicyViolationWaived && !isOldPolicyViolationWaived) {
        // The policy violation was waived.
        policyViolationLogger.add(PolicyViolationLogEvent.WAIVE, newPolicyViolation);
      }
    }
  }

  private RepositoryPolicyViolation createRepositoryPolicyViolation(
      PolicyAlert policyAlert,
      Policy policy,
      ComponentFact componentFact,
      String pathname,
      Repository repository,
      Date evaluationTime,
      PolicyWaiver policyWaiver)
  {
    PolicyFact policyFact = policyAlert.getTrigger();
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

    if (policyWaiver != null) {
      policyViolation.setWaived(true);
      policyViolation.setPolicyWaiverId(policyWaiver.getId());
      policyViolation.setPolicyWaiverComment(policyWaiver.getComment());
      policyViolation.setWaiveTime(evaluationTime);
    }

    return policyViolation;
  }

  private boolean shouldQuarantine(List<PolicyAlert> policyAlerts, Component component) {
    for (PolicyAlert policyAlert : policyAlerts) {
      if (getComponentFact(policyAlert, component) != null && hasFailAction(policyAlert)) {
        return true;
      }
    }
    return false;
  }

  private boolean hasFailAction(PolicyAlert policyAlert) {
    if (policyAlert.getActions() != null) {
      for (Action action : policyAlert.getActions()) {
        if (Action.ID_FAIL.equals(action.getActionTypeId())) {
          return true;
        }
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

  private ComponentEvaluationDataList getComponentDetailsFromHds(
      Repository repository,
      boolean withQuarantine,
      final RepositoryComponentEvaluationDataRequestList hdsRequest,
      final String clientUserAgent)
  {
    long start = System.currentTimeMillis();

    HdsClient hdsClient = withQuarantine ? quarantineHdsClient : auditHdsClient;
    ComponentEvaluationDataList result = hdsClient.post(HdsClientAnalytics.forOwner(repository),
        ComponentEvaluationDataList.class, HDS_COMPONENT_DETAILS_PATH, clientUserAgent, hdsRequest);

    log.debug("Got component details from HDS for {} components in {} ms.", hdsRequest.components.size(),
        System.currentTimeMillis() - start);

    return result;
  }
}
