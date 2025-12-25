/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
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
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
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
import com.sonatype.insight.brain.model.policy.notifications.PolicyNotification;
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
import com.sonatype.insight.brain.scan.matcher.firewall.RepositoryPathnameParser;
import com.sonatype.insight.brain.telemetry.RepositoryComponentTelemetry.ReleaseQuarantineType;
import com.sonatype.insight.brain.telemetry.RepositoryComponentTelemetry.ReleaseReason;
import com.sonatype.insight.brain.telemetry.RepositoryComponentTelemetry.RepositoryComponentTelemetryEventType;
import com.sonatype.insight.brain.telemetry.RepositoryComponentTelemetryCreator;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.purl.InvalidPackageURLException;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.stream.Collectors.toMap;

/**
 * @since 1.18.0
 */
@Named
public class RepositoryPolicyEvaluator
{
  public static final String HDS_COMPONENT_DETAILS_PATH = "rest/component/details/firewall";

  // Maven pathname parsing constants
  private static final String PATH_SEPARATOR = "/";

  private static final String GROUP_ID_SEPARATOR = ".";

  private static final String DEFAULT_MAVEN_TYPE = "jar";

  private static final String MAVEN_PURL_FORMAT = "pkg:maven/%s/%s@%s?type=%s";

  private static final String PACKAGE_URL_PREFIX = "pkg:";

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

  private final RepositoryPathnameParser repositoryPathnameParser;

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
      ApiFirewallMetricsService firewallMetricsService,
      RepositoryPathnameParser repositoryPathnameParser
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
    this.repositoryPathnameParser = repositoryPathnameParser;
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
    long start = System.currentTimeMillis();

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

        if (component.getComponentIdentifier() == null) {
          ComponentIdentifier parsedIdentifier = null;
          String pathnameToUse = componentEvaluationRequest.pathname;

          // Check if the pathname is actually a packageUrl (starts with "pkg:")
          if (isPackageUrl(pathnameToUse)) {
            try {
              PackageUrlIdentifier packageUrlIdentifier = new PackageUrlIdentifier(pathnameToUse);
              parsedIdentifier = packageUrlIdentifier.toComponentIdentifier();
            }
            catch (InvalidPackageURLException e) {
              log.warn("Failed to parse packageUrl: {}", pathnameToUse, e);
            }
          }

          // Fall back to pathname parsing if packageUrl parsing failed or pathname is not a packageUrl
          if (parsedIdentifier == null) {
            // Ensure pathname starts with "/" for proper parsing
            if (pathnameToUse != null && !pathnameToUse.startsWith("/") && !isPackageUrl(pathnameToUse)) {
              pathnameToUse = "/" + pathnameToUse;
            }

            parsedIdentifier = repositoryPathnameParser.parse(
                pathnameToUse, componentEvaluationRequest.format);

            // If repositoryPathnameParser failed and format is maven, try manual parsing
            if (parsedIdentifier == null
                && ComponentIdentifier.FORMAT_MAVEN.equalsIgnoreCase(componentEvaluationRequest.format)
                && pathnameToUse != null) {
              parsedIdentifier = parseMavenPathname(pathnameToUse);
            }
          }

          component.setComponentIdentifier(parsedIdentifier);
        }
        components.add(component);
      }
    }

    // Evaluate the policies
    List<Policy> policies = policyDAO.getApplicableByOwnerIdWithHierarchy(repository.getId());

    // Filter out nulls and crete a quick lookup table for components by path name:
    // This is a hot code path that we have to careful of from a performance perspective.
    // It is used by the Repo Policy Compliant Component Selection. Latency here can slow down builds:
    // https://sonatype.atlassian.net/browse/NEXUS-48520
    final List<Component> componentsWithoutNulls = components.stream().filter(Objects::nonNull)
        .collect(Collectors.toList());
    final Map<List<String>, Component> lookup = mapComponentForQuickLookUp(componentsWithoutNulls);

    PolicyResults policyResults = componentPolicyEvaluator.evaluate(repository.getId(), new Stage(ProxyStageType.ID),
        policies, componentsWithoutNulls, false /* forMonitoring */);

    Map<Component, List<PolicyAlert>> policyAlertsByComponent =
        groupPolicyAlertsByComponent(policyResults.getActiveAlerts(), lookup);

    Map<Component, List<PolicyAlert>> waivedAlertsByComponent =
        groupPolicyAlertsByComponent(policyResults.getWaivedAlerts(), lookup);

    List<PolicyNotification> policyNotifications = policyResults.getActiveNotifications();

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
          List<PolicyAlert> activeAlerts = policyAlertsByComponent.getOrDefault(component, Collections.emptyList());
          List<PolicyAlert> waivedAlerts = waivedAlertsByComponent.getOrDefault(component, Collections.emptyList());
          Map<PolicyAlert, PolicyWaiver> policyWaiversByComponent =
              getPolicyWaivers(policyResults, waivedAlerts, component);

          RepositoryComponent repositoryComponent = persistEvaluationResults(
              repository,
              now,
              component,
              policies,
              withQuarantine,
              shouldSendNotifications,
              forMonitoring,
              event,
              activeAlerts,
              waivedAlerts,
              policyWaiversByComponent,
              policyNotifications);
          repositoryComponentEvaluationResult.quarantine = repositoryComponent.isQuarantined();
        }
        else {
          repositoryComponentEvaluationResult.policyAlerts =
              policyAlertsByComponent.getOrDefault(component, Collections.emptyList());
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
      repositoryPolicyAlertEmailer.sendNotifications(repository, policyNotifications);
    }

    log.trace("Evaluated {} components with quarantine {} for repository {} in {} ms.",
        componentEvaluationDataRequestList.components.size(), withQuarantine,
        repository.getPublicId(), System.currentTimeMillis() - start);

    return componentEvaluationResultList;
  }

  private Map<Component, List<PolicyAlert>> groupPolicyAlertsByComponent(
      final List<PolicyAlert> policyAlerts,
      final Map<List<String>, Component> lookup)
  {
    Map<Component, List<PolicyAlert>> policyAlertsByComponent = new HashMap<>();

    for (PolicyAlert policyAlert : policyAlerts) {
      Component component = findComponentForAlert(policyAlert, lookup);
      if (component != null) {
        policyAlertsByComponent.computeIfAbsent(component, k -> new ArrayList<>()).add(policyAlert);
      }
    }
    return policyAlertsByComponent;
  }

  private Map<PolicyAlert, PolicyWaiver> getPolicyWaivers(
      final PolicyResults policyResults,
      final List<PolicyAlert> waivedAlerts,
      final Component component)
  {
    Map<PolicyAlert, PolicyWaiver> policyWaiverByComponent = new HashMap<>();

    for (PolicyAlert policyAlert : waivedAlerts) {
      ComponentFact componentFact = getComponentFact(policyAlert, component);
      if (componentFact == null) {
        continue;
      }

      PolicyWaiver policyWaiver = policyResults.getPolicyWaiver(componentFact);
      policyWaiverByComponent.put(policyAlert, policyWaiver);
    }
    return policyWaiverByComponent;
  }

  private Map<List<String>, Component> mapComponentForQuickLookUp(final List<Component> components) {
    return components.stream()
        .collect(
          toMap(Component::getPathnames, Function.identity(), (existing, replacement) -> replacement));
  }

  private Component findComponentForAlert(final PolicyAlert policyAlert, final Map<List<String>, Component> lookup) {
    return policyAlert.getTrigger().getComponentFacts()
        .stream()
        .filter(componentFact -> lookup.containsKey(componentFact.getPathnames()))
        .map(componentFact -> lookup.get(componentFact.getPathnames()))
        .findFirst()
        .orElse(null);
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
        .collect(toMap(RepositoryComponent::getPathname, Function.identity()));
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
    if (repositoryComponent != null &&
        (component.getHash() == null || repositoryComponent.getHash().equals(component.getHash()))) {
      return repositoryComponent.isQuarantined();
    }

    return shouldQuarantine(policyAlerts, component);
  }

  private RepositoryComponent persistEvaluationResults(
      Repository repository,
      Date evaluationTime,
      Component component,
      List<Policy> policies,
      boolean canBeQuarantined,
      boolean isNotificationsToBeSent,
      boolean forMonitoring,
      CreateRepositoryPolicyViolationsEvent event,
      List<PolicyAlert> activeAlerts,
      List<PolicyAlert> waivedAlerts,
      Map<PolicyAlert, PolicyWaiver> policyWaiversByComponent,
      List<PolicyNotification> policyNotifications)
  {
    RepositoryComponent repositoryComponent;
    try (ClusterLock clusterLock =
             clusterLockManager.createForRepositoryComponent(repository.getId(), component.getPathnames().get(0));
         TransactionContext tx = policyDAO.createTransactionContext()) {
      clusterLock.lock();
      tx.begin();

      RepositoryPolicyViolationLogger policyViolationLogger =
          policyViolationLoggerFactory.newLogger(evaluationTime, repository);

      List<PolicyAlert> allPolicyAlertsByComponent = new ArrayList<>();
      allPolicyAlertsByComponent.addAll(activeAlerts);
      allPolicyAlertsByComponent.addAll(waivedAlerts);

      // The order of the following calls are important and must not be changed. See: CLM-13853
      persistPolicyViolations(tx, repository, evaluationTime, component, policies,
          policyViolationLogger, event, allPolicyAlertsByComponent, policyWaiversByComponent);
      repositoryComponent = persistRepositoryComponent(tx, repository, evaluationTime, component,
          canBeQuarantined, forMonitoring, activeAlerts);

      tx.commit();

      sendRepositoryComponentTelemetry(policyNotifications, repositoryComponent, repository, isNotificationsToBeSent);
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
      boolean forMonitoring,
      List<PolicyAlert> activeAlerts)
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
      boolean quarantine = canBeQuarantined && shouldQuarantine(activeAlerts, component);
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

      if (repositoryComponent.isQuarantined() && !shouldQuarantine(activeAlerts, component)) {
        // The component is quarantined, but it doesn't have any policy violations/alerts that would quarantine it
        // anymore.
        unquarantineComponent(repository, repositoryComponent, evaluationTime, forMonitoring);
      }

      repositoryComponentDAO.update(tx, repositoryComponent);
    }
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
    repositoryPolicyViolationDAO.loadConstraintFacts(repositoryPolicyViolations);

    ReleaseQuarantineType releaseQuarantineType =
        forMonitoring ? ReleaseQuarantineType.AUTO : ReleaseQuarantineType.MANUAL;
    String releaseReason = forMonitoring
        ? ReleaseReason.MONITORING_ENABLED.getDescription()
        : ReleaseReason.POLICY_CHANGE.getDescription();

    repositoryComponentTelemetryCreator.sendRepositoryComponentTelemetry(repositoryComponent,
        repositoryPolicyViolations, repository.getRepositoryManagerId(),
        RepositoryComponentTelemetryEventType.RELEASE_QUARANTINE,
        releaseQuarantineType, releaseReason, Collections.emptyList());
  }

  private void sendRepositoryComponentTelemetry(
      final List<PolicyNotification> policyNotifications,
      final RepositoryComponent repositoryComponent,
      final Repository repository,
      final boolean isNotificationsToBeSent)
  {
    List<RepositoryPolicyViolation> repositoryPolicyViolations = repositoryPolicyViolationDAO
        .getActiveByRepositoryIdAndPathname(repository.getId(), repositoryComponent.getPathname());
    repositoryPolicyViolationDAO.loadConstraintFacts(repositoryPolicyViolations);
    repositoryComponentTelemetryCreator.sendRepositoryComponentTelemetry(repositoryComponent,
        repositoryPolicyViolations, repository.getRepositoryManagerId(),
        repositoryComponent.isQuarantined() ? RepositoryComponentTelemetryEventType.QUARANTINE
            : RepositoryComponentTelemetryEventType.AUDIT,
        isNotificationsToBeSent ? policyNotifications : Collections.emptyList());
  }

  private void persistPolicyViolations(
      TransactionContext tx,
      Repository repository,
      Date evaluationTime,
      Component component,
      List<Policy> policies,
      RepositoryPolicyViolationLogger policyViolationLogger,
      CreateRepositoryPolicyViolationsEvent event,
      List<PolicyAlert> allPolicyAlertsByComponent,
      Map<PolicyAlert, PolicyWaiver> policyWaiversByComponent)
  {
    String pathname = component.getPathnames().get(0);
    // Get the persisted RepositoryPolicyViolations for this component
    List<RepositoryPolicyViolation> oldPolicyViolations =
        repositoryPolicyViolationDAO.getActiveByRepositoryIdAndPathname(tx, repository.getId(), pathname);
    repositoryPolicyViolationDAO.loadConstraintFacts(oldPolicyViolations);

    // Build the list of current RepositoryPolicyViolations for this component
    List<RepositoryPolicyViolation> newPolicyViolations = new ArrayList<>();
    for (PolicyAlert policyAlert : allPolicyAlertsByComponent) {
      ComponentFact componentFact = getComponentFact(policyAlert, component);
      if (componentFact == null) {
        continue;
      }

      Policy policy = policies.stream()
          .filter(p -> p.getId().equals(policyAlert.getTrigger().getPolicyId()))
          .findFirst().get();

      RepositoryPolicyViolation policyViolation = createRepositoryPolicyViolation(policyAlert, policy, componentFact,
          pathname,
          repository, evaluationTime, policyWaiversByComponent.get(policyAlert));
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

  public ComponentEvaluationDataList getComponentDetailsFromHds(
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

  /**
   * Check if a pathname string is a Package URL (PURL) format.
   * Package URLs start with "pkg:" prefix.
   *
   * @param pathname the pathname to check
   * @return true if the pathname is a package URL, false otherwise
   */
  private static boolean isPackageUrl(String pathname) {
    return pathname != null && pathname.startsWith(PACKAGE_URL_PREFIX);
  }

  /**
   * Manually parse a Maven repository pathname to extract component coordinates.
   * Expected format: /groupId/artifactId/version/filename.ext
   * Example: /org/apache/logging/log4j/log4j-core/2.14.1/log4j-core-2.14.1.jar
   */
  private ComponentIdentifier parseMavenPathname(String pathname) {
    try {
      // Remove leading slash if present
      String path = pathname.startsWith(PATH_SEPARATOR) ? pathname.substring(1) : pathname;

      // Split by slashes
      String[] parts = path.split(PATH_SEPARATOR);
      if (parts.length < 4) {
        log.warn("Maven pathname has insufficient parts: {}", pathname);
        return null;
      }

      // Last part is the filename (e.g., "log4j-core-2.14.1.jar")
      String filename = parts[parts.length - 1];

      // Second-to-last is version (e.g., "2.14.1")
      String version = parts[parts.length - 2];

      // Third-to-last is artifactId (e.g., "log4j-core")
      String artifactId = parts[parts.length - 3];

      // Everything before that is the groupId (join with dots)
      StringBuilder groupId = new StringBuilder();
      for (int i = 0; i < parts.length - 3; i++) {
        if (i > 0) {
          groupId.append(GROUP_ID_SEPARATOR);
        }
        groupId.append(parts[i]);
      }

      // Extract type from filename extension
      String type = DEFAULT_MAVEN_TYPE;
      int lastDot = filename.lastIndexOf(GROUP_ID_SEPARATOR);
      if (lastDot > 0) {
        type = filename.substring(lastDot + 1);
      }

      // Create a PackageUrl and convert to ComponentIdentifier
      String purl = String.format(MAVEN_PURL_FORMAT, groupId, artifactId, version, type);
      PackageUrlIdentifier packageUrlIdentifier = new PackageUrlIdentifier(purl);
      return packageUrlIdentifier.toComponentIdentifier();
    }
    catch (InvalidPackageURLException e) {
      log.warn("Failed to manually parse Maven pathname: {}", pathname, e);
      return null;
    }
  }
}
