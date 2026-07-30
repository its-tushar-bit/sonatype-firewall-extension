/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.inject.Named;

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
import com.sonatype.insight.brain.dataaccess.policy.ProxyRepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.ProxyRepositoryComponentDAO;
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
import com.sonatype.insight.brain.model.policy.ProxyRepositoryPolicyViolation;
import com.sonatype.insight.brain.model.policy.notifications.PolicyNotification;
import com.sonatype.insight.brain.model.policy.stages.ComplianceStageType;
import com.sonatype.insight.brain.model.policy.stages.ProxyStageType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.ProxyRepositoryComponent;
import com.sonatype.insight.brain.policy.evaluator.ComponentPolicyEvaluator;
import com.sonatype.insight.brain.policy.evaluator.PolicyResults;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationDiff;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationDigester;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLogEvent;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLoggerFactory;
import com.sonatype.insight.brain.policy.violation.ProxyRepositoryPolicyViolationLogger;
import com.sonatype.insight.brain.scan.matcher.firewall.RepositoryPathnameParser;
import com.sonatype.insight.brain.telemetry.ProxyRepositoryComponentTelemetry.ReleaseQuarantineType;
import com.sonatype.insight.brain.telemetry.ProxyRepositoryComponentTelemetry.ReleaseReason;
import com.sonatype.insight.brain.telemetry.ProxyRepositoryComponentTelemetry.RepositoryComponentTelemetryEventType;
import com.sonatype.insight.brain.telemetry.ProxyRepositoryComponentTelemetryCreator;
import com.sonatype.insight.brain.webhook.FirewallPolicyAlertEventService;
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

  public static final String CONTINUOUS_MONITORING_CAUSE = "CONTINUOUS_MONITORING";

  // Maven pathname parsing constants
  private static final String PATH_SEPARATOR = "/";

  private static final String GROUP_ID_SEPARATOR = ".";

  private static final String DEFAULT_MAVEN_TYPE = "jar";

  private static final String MAVEN_PURL_FORMAT = "pkg:maven/%s/%s@%s?type=%s";

  private static final String PACKAGE_URL_PREFIX = "pkg:";

  private static final Logger log = LoggerFactory.getLogger(RepositoryPolicyEvaluator.class);

  private final ComponentPolicyEvaluator componentPolicyEvaluator;

  private final ProxyRepositoryComponentDAO proxyRepositoryComponentDAO;

  private final ProxyRepositoryPolicyViolationDAO proxyRepositoryPolicyViolationDAO;

  private final PolicyDAO policyDAO;

  private final FirewallAuditHdsClient auditHdsClient;

  private final FirewallQuarantineHdsClient quarantineHdsClient;

  private final PolicyViolationLoggerFactory policyViolationLoggerFactory;

  private final FirewallIgnorePatternService firewallIgnorePatternService;

  private final ComponentDetailsLoaderFactory componentDetailsLoaderFactory;

  private final ProxyRepositoryComponentDeleteService proxyRepositoryComponentDeleteService;

  private final RepositoryPolicyAlertEmailer repositoryPolicyAlertEmailer;

  private final ProxyRepositoryComponentTelemetryCreator proxyRepositoryComponentTelemetryCreator;

  private final ClusterLockManager clusterLockManager;

  private final AsyncEventBus eventBus;

  private final ApiFirewallMetricsService firewallMetricsService;

  private final RepositoryPathnameParser repositoryPathnameParser;

  private final FirewallPolicyAlertEventService firewallPolicyAlertEventService;

  @Inject
  public RepositoryPolicyEvaluator(
      ComponentPolicyEvaluator componentPolicyEvaluator,
      ProxyRepositoryComponentDAO proxyRepositoryComponentDAO,
      ProxyRepositoryPolicyViolationDAO proxyRepositoryPolicyViolationDAO,
      PolicyDAO policyDAO,
      FirewallAuditHdsClient auditHdsClient,
      FirewallQuarantineHdsClient quarantineHdsClient,
      PolicyViolationLoggerFactory policyViolationLoggerFactory,
      FirewallIgnorePatternService firewallIgnorePatternService,
      ComponentDetailsLoaderFactory componentDetailsLoaderFactory,
      ProxyRepositoryComponentDeleteService proxyRepositoryComponentDeleteService,
      RepositoryPolicyAlertEmailer repositoryPolicyAlertEmailer,
      ProxyRepositoryComponentTelemetryCreator proxyRepositoryComponentTelemetryCreator,
      final ClusterLockManager clusterLockManager,
      AsyncEventBus eventBus,
      ApiFirewallMetricsService firewallMetricsService,
      RepositoryPathnameParser repositoryPathnameParser,
      FirewallPolicyAlertEventService firewallPolicyAlertEventService)
  {
    this.componentPolicyEvaluator = componentPolicyEvaluator;
    this.proxyRepositoryComponentDAO = proxyRepositoryComponentDAO;
    this.proxyRepositoryPolicyViolationDAO = proxyRepositoryPolicyViolationDAO;
    this.policyDAO = policyDAO;
    this.auditHdsClient = auditHdsClient;
    this.quarantineHdsClient = quarantineHdsClient;
    this.policyViolationLoggerFactory = policyViolationLoggerFactory;
    this.firewallIgnorePatternService = firewallIgnorePatternService;
    this.componentDetailsLoaderFactory = componentDetailsLoaderFactory;
    this.proxyRepositoryComponentDeleteService = proxyRepositoryComponentDeleteService;
    this.repositoryPolicyAlertEmailer = repositoryPolicyAlertEmailer;
    this.proxyRepositoryComponentTelemetryCreator = proxyRepositoryComponentTelemetryCreator;
    this.clusterLockManager = clusterLockManager;
    this.eventBus = eventBus;
    this.firewallMetricsService = firewallMetricsService;
    this.repositoryPathnameParser = repositoryPathnameParser;
    this.firewallPolicyAlertEventService = firewallPolicyAlertEventService;
  }

  public RepositoryComponentEvaluationDataList evaluateForMonitoring(
      Repository repository,
      RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList)
  {
    return evaluateForMonitoring(repository, componentEvaluationDataRequestList, ComplianceStageType.ID);
  }

  public RepositoryComponentEvaluationDataList evaluateForMonitoring(
      Repository repository,
      RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList,
      String stageTypeId)
  {
    return evaluate(repository, componentEvaluationDataRequestList, false /* withQuarantine */,
        true /* persistEvaluationResults */, null /* clientUserAgent */, true /* forMonitoring */, null,
        stageTypeId);
  }

  public RepositoryComponentEvaluationDataList evaluateForAutomaticRelease(
      Repository repository,
      RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList)
  {
    return evaluate(repository, componentEvaluationDataRequestList, false /* withQuarantine */,
        true /* persistEvaluationResults */, null /* clientUserAgent */, true /* forMonitoring */,
        ReleaseReason.AUTO_RELEASED);
  }

  public RepositoryComponentEvaluationDataList evaluate(
      Repository repository,
      RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList,
      final boolean withQuarantine,
      final String clientUserAgent)
  {
    return evaluate(repository, componentEvaluationDataRequestList, withQuarantine, true /* persistEvaluationResults */,
        clientUserAgent, false /* forMonitoring */, null, ProxyStageType.ID);
  }

  public RepositoryComponentEvaluationDataList evaluate(
      Repository repository,
      RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList,
      final boolean withQuarantine,
      final String clientUserAgent,
      final String stageTypeId)
  {
    return evaluate(repository, componentEvaluationDataRequestList, withQuarantine, true /* persistEvaluationResults */,
        clientUserAgent, false /* forMonitoring */, null, stageTypeId);
  }

  public RepositoryComponentEvaluationDataList evaluate(
      Repository repository,
      RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList,
      boolean withQuarantine,
      boolean persistEvaluationResults,
      String clientUserAgent,
      boolean forMonitoring)
  {
    return evaluate(repository, componentEvaluationDataRequestList, withQuarantine, persistEvaluationResults,
        clientUserAgent, forMonitoring, null, ProxyStageType.ID);
  }

  private RepositoryComponentEvaluationDataList evaluate(
      Repository repository,
      RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList,
      boolean withQuarantine,
      boolean persistEvaluationResults,
      String clientUserAgent,
      boolean forMonitoring,
      ReleaseReason explicitReleaseReason)
  {
    return evaluate(repository, componentEvaluationDataRequestList, withQuarantine, persistEvaluationResults,
        clientUserAgent, forMonitoring, explicitReleaseReason, ProxyStageType.ID);
  }

  private RepositoryComponentEvaluationDataList evaluate(
      Repository repository,
      RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList,
      boolean withQuarantine,
      boolean persistEvaluationResults,
      String clientUserAgent,
      boolean forMonitoring,
      ReleaseReason explicitReleaseReason,
      String stageTypeId)
  {
    long start = System.currentTimeMillis();

    ComponentEvaluationDataList componentDetailsFromHdsList =
        getComponentDetailsFromHds(repository, withQuarantine, componentEvaluationDataRequestList, clientUserAgent);

    RepositoryComponentEvaluationDataList result = evaluate(repository, componentEvaluationDataRequestList,
        componentDetailsFromHdsList, withQuarantine, persistEvaluationResults, forMonitoring, explicitReleaseReason,
        stageTypeId);

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
    return evaluate(repository, componentEvaluationDataRequestList, componentDetailsFromHds, withQuarantine,
        persistEvaluationResults, forMonitoring, null, ProxyStageType.ID);
  }

  private RepositoryComponentEvaluationDataList evaluate(
      Repository repository,
      RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList,
      ComponentEvaluationDataList componentDetailsFromHds,
      boolean withQuarantine,
      boolean persistEvaluationResults,
      boolean forMonitoring,
      ReleaseReason explicitReleaseReason)
  {
    return evaluate(repository, componentEvaluationDataRequestList, componentDetailsFromHds, withQuarantine,
        persistEvaluationResults, forMonitoring, explicitReleaseReason, ProxyStageType.ID);
  }

  private RepositoryComponentEvaluationDataList evaluate(
      Repository repository,
      RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList,
      ComponentEvaluationDataList componentDetailsFromHds,
      boolean withQuarantine,
      boolean persistEvaluationResults,
      boolean forMonitoring,
      ReleaseReason explicitReleaseReason,
      String stageTypeId)
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

    List<String> ignoredPathnames = componentEvaluationDataRequestList.components.stream()
        .map(c -> c.pathname)
        .filter(Objects::nonNull)
        .filter(componentPathnameMatchesIgnorePattern)
        .collect(Collectors.toList());
    // (repository_id, pathname) is unique in the proxy_repository_component table, so duplicates are not expected.
    // The merge function "keep last" matches the prior singular getByRepositoryIdAndPathname semantic (which
    // returned a single row) and avoids a confusing IllegalStateException if a hypothetical data anomaly ever
    // produced two rows for the same pathname.
    Map<String, ProxyRepositoryComponent> existingRepositoryComponentsByPathname = ignoredPathnames.isEmpty()
        ? Collections.emptyMap()
        : proxyRepositoryComponentDAO.getByRepositoryIdAndPathnames(repository.getId(), ignoredPathnames)
            .stream()
            .collect(toMap(ProxyRepositoryComponent::getPathname, Function.identity(),
                (existing, replacement) -> replacement));

    for (int requestIndex = 0; requestIndex < componentEvaluationDataRequestList.components.size(); requestIndex++) {
      RepositoryComponentEvaluationDataRequest componentEvaluationRequest =
          componentEvaluationDataRequestList.components.get(requestIndex);
      ComponentEvaluationData componentEvaluationData = componentDetailsFromHds.components.get(requestIndex);

      // If the component matches the repository ignore pattern then
      // 1. Remove it if it is already persisted
      // 2. Do not evaluate policies on it
      // 3. Do not persist it
      if (componentPathnameMatchesIgnorePattern.test(componentEvaluationRequest.pathname)) {
        ProxyRepositoryComponent proxyRepositoryComponent =
            existingRepositoryComponentsByPathname.get(componentEvaluationRequest.pathname);
        if (proxyRepositoryComponent != null) {
          proxyRepositoryComponentDeleteService.deleteComponent(proxyRepositoryComponent);
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

        // CLM-40943: prefer path-derived identifier when HDS's hash-only lookup returns a
        // cross-format coordinate. Common case: a real npm tarball (e.g. form-data-2.3.3.tgz)
        // has the same SHA as the webjars maven re-publish (org.webjars.npm:form-data:2.3.3),
        // so HDS returns the maven coordinate even though the request is unambiguously npm.
        // The repository_pathname parser uses the format the caller sent (npm/pypi/...) and
        // produces the format-native identifier; trust it over an HDS hit whose format
        // disagrees with the request. The original "only fall back when HDS returned nothing"
        // behaviour remains for cases where the path can't be parsed.
        ComponentIdentifier existingIdentifier = component.getComponentIdentifier();
        boolean hdsDisagreesOnFormat = existingIdentifier != null
            && componentEvaluationRequest.format != null
            && existingIdentifier.getFormat() != null
            && !componentEvaluationRequest.format.equalsIgnoreCase(existingIdentifier.getFormat());

        if (existingIdentifier == null || hdsDisagreesOnFormat) {
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
                && pathnameToUse != null)
            {
              parsedIdentifier = parseMavenPathname(pathnameToUse);
            }
          }

          // Only overwrite the existing HDS identifier when we successfully parsed a
          // path-derived one in the requested format — otherwise keep what HDS gave us
          // (a wrong-format coordinate is better than no coordinate at all for downstream
          // policy matching that's purely hash-based).
          if (parsedIdentifier != null
              && (existingIdentifier == null
                  || (componentEvaluationRequest.format != null
                      && componentEvaluationRequest.format.equalsIgnoreCase(parsedIdentifier.getFormat()))))
          {
            if (existingIdentifier != null) {
              log.debug(
                  "Overriding cross-format HDS identifier (HDS={}, request={}) with path-derived identifier for pathname={}",
                  existingIdentifier.getFormat(), componentEvaluationRequest.format,
                  componentEvaluationRequest.pathname);
            }
            component.setComponentIdentifier(parsedIdentifier);
          }
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
    final List<Component> componentsWithoutNulls = components.stream()
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
    final Map<List<String>, Component> lookup = mapComponentForQuickLookUp(componentsWithoutNulls);

    PolicyResults policyResults = componentPolicyEvaluator.evaluate(repository.getId(), new Stage(stageTypeId),
        policies, componentsWithoutNulls, false /* forMonitoring */);

    Map<Component, List<PolicyAlert>> policyAlertsByComponent =
        groupPolicyAlertsByComponent(policyResults.getActiveAlerts(), lookup);

    Map<Component, List<PolicyAlert>> waivedAlertsByComponent =
        groupPolicyAlertsByComponent(policyResults.getWaivedAlerts(), lookup);

    List<PolicyNotification> policyNotifications = policyResults.getActiveNotifications();

    // Only notify new component evaluation policy violations
    boolean shouldSendNotifications =
        RepositoryComponentEvaluationDataRequestList.NEW_COMPONENT.equals(componentEvaluationDataRequestList.cause);

    Map<String, ProxyRepositoryComponent> repositoryComponents = Collections.emptyMap();
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

        repositoryComponentEvaluationResult.policyAlerts =
            policyAlertsByComponent.getOrDefault(component, Collections.emptyList());

        if (persistEvaluationResults) {
          List<PolicyAlert> waivedAlerts = waivedAlertsByComponent.getOrDefault(component, Collections.emptyList());
          Map<PolicyAlert, PolicyWaiver> policyWaiversByComponent =
              getPolicyWaivers(policyResults, waivedAlerts, component);

          ProxyRepositoryComponent proxyRepositoryComponent = persistEvaluationResults(
              repository,
              now,
              component,
              policies,
              withQuarantine,
              shouldSendNotifications,
              forMonitoring,
              explicitReleaseReason,
              stageTypeId,
              event,
              repositoryComponentEvaluationResult.policyAlerts,
              waivedAlerts,
              policyWaiversByComponent,
              policyNotifications);
          repositoryComponentEvaluationResult.quarantine = proxyRepositoryComponent.isQuarantined();
        }
        else if (withQuarantine) {
          ProxyRepositoryComponent proxyRepositoryComponent =
              repositoryComponents.getOrDefault(component.getPathnames().get(0), null);
          repositoryComponentEvaluationResult.quarantine =
              canQuarantine(repositoryComponentEvaluationResult.policyAlerts, proxyRepositoryComponent, component);
        }
      }
      componentEvaluationResultList.componentEvalResults.add(repositoryComponentEvaluationResult);
    }

    if (event != null && !event.proxyRepositoryPolicyViolations.isEmpty()) {
      proxyRepositoryPolicyViolationDAO.loadConstraintFacts(event.proxyRepositoryPolicyViolations);
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
    return policyAlert.getTrigger()
        .getComponentFacts()
        .stream()
        .filter(componentFact -> lookup.containsKey(componentFact.getPathnames()))
        .map(componentFact -> lookup.get(componentFact.getPathnames()))
        .findFirst()
        .orElse(null);
  }

  private Map<String, ProxyRepositoryComponent> getRepositoryComponents(
      final Repository repository,
      final List<Component> components)
  {
    List<String> pathnames = components.stream()
        .filter(Objects::nonNull)
        .map(component -> component.getPathnames().get(0))
        .collect(Collectors.toList());

    return proxyRepositoryComponentDAO.getByRepositoryIdAndPathnames(repository.getId(), pathnames)
        .stream()
        .collect(toMap(ProxyRepositoryComponent::getPathname, Function.identity()));
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
      ProxyRepositoryComponent proxyRepositoryComponent,
      Component component)
  {
    if (proxyRepositoryComponent != null &&
        (component.getHash() == null || proxyRepositoryComponent.getHash().equals(component.getHash())))
    {
      return proxyRepositoryComponent.isQuarantined();
    }

    return shouldQuarantine(policyAlerts, component);
  }

  private ProxyRepositoryComponent persistEvaluationResults(
      Repository repository,
      Date evaluationTime,
      Component component,
      List<Policy> policies,
      boolean canBeQuarantined,
      boolean isNotificationsToBeSent,
      boolean forMonitoring,
      ReleaseReason explicitReleaseReason,
      String stageTypeId,
      CreateRepositoryPolicyViolationsEvent event,
      List<PolicyAlert> activeAlerts,
      List<PolicyAlert> waivedAlerts,
      Map<PolicyAlert, PolicyWaiver> policyWaiversByComponent,
      List<PolicyNotification> policyNotifications)
  {
    ProxyRepositoryComponent proxyRepositoryComponent;
    try (ClusterLock clusterLock =
        clusterLockManager.createForRepositoryComponent(repository.getId(), component.getPathnames().get(0));
        TransactionContext tx = policyDAO.createTransactionContext())
    {
      clusterLock.lock();
      tx.begin();

      ProxyRepositoryPolicyViolationLogger policyViolationLogger =
          policyViolationLoggerFactory.newLogger(evaluationTime, repository);

      List<PolicyAlert> allPolicyAlertsByComponent = new ArrayList<>();
      allPolicyAlertsByComponent.addAll(activeAlerts);
      allPolicyAlertsByComponent.addAll(waivedAlerts);

      // Single merged read per component: the existing proxy_repository_component row and its active policy
      // violations for (repositoryId, pathname), in one round trip instead of two (CLM-42134). The
      // per-component read itself remains (still N round trips across an N-component batch); bounded-chunk
      // batching across components is a follow-up if profiling shows reads still dominate (see CLM-42071).
      String pathname = component.getPathnames().get(0);
      ProxyRepositoryComponentDAO.ComponentWithActiveViolations existing =
          proxyRepositoryComponentDAO.getWithActiveViolationsByRepositoryIdAndPathname(tx, repository.getId(),
              pathname);

      // The order of the following calls are important and must not be changed. See: CLM-13853
      List<ProxyRepositoryPolicyViolation> newRepositoryPolicyViolations =
          persistPolicyViolations(tx, repository, evaluationTime, component, policies,
              policyViolationLogger, event, allPolicyAlertsByComponent, policyWaiversByComponent,
              existing.activeViolations());
      proxyRepositoryComponent = persistRepositoryComponent(tx, repository, evaluationTime, component,
          canBeQuarantined, forMonitoring, explicitReleaseReason, stageTypeId, activeAlerts,
          newRepositoryPolicyViolations, existing.component());

      tx.commit();

      sendRepositoryComponentTelemetry(policyNotifications, proxyRepositoryComponent, repository,
          isNotificationsToBeSent, component, newRepositoryPolicyViolations);
      AuditData.get().commitSubEvents();
      policyViolationLogger.log();
    }
    return proxyRepositoryComponent;
  }

  private ProxyRepositoryComponent persistRepositoryComponent(
      TransactionContext tx,
      Repository repository,
      Date evaluationTime,
      Component component,
      boolean canBeQuarantined,
      boolean forMonitoring,
      ReleaseReason explicitReleaseReason,
      String stageTypeId,
      List<PolicyAlert> activeAlerts,
      List<ProxyRepositoryPolicyViolation> newRepositoryPolicyViolations,
      ProxyRepositoryComponent existingRepositoryComponent)
  {
    String pathname = component.getPathnames().get(0);
    ProxyRepositoryComponent proxyRepositoryComponent = existingRepositoryComponent;
    // Must be read before any mutation to proxyRepositoryComponent below.
    boolean wasQuarantined = proxyRepositoryComponent != null && proxyRepositoryComponent.isQuarantined();
    String repositoryComponentId = proxyRepositoryComponent == null ? null : proxyRepositoryComponent.getId();
    if (proxyRepositoryComponent != null && !proxyRepositoryComponent.getHash().equals(component.getHash())) {
      if (proxyRepositoryComponent.isQuarantined()) {
        try (AuditSession auditSession = AuditData.get().recordSubEvent(AuditEvent.RESET_QUARANTINE, false)) {
          AuditData.get()
              .setRepository(repository)
              .setComponentHash(proxyRepositoryComponent.getHash())
              .setData("componentPathname", proxyRepositoryComponent.getPathname());
        }
      }
    }
    if (proxyRepositoryComponent == null || !proxyRepositoryComponent.getHash().equals(component.getHash())) {
      boolean quarantine = canBeQuarantined && shouldQuarantine(activeAlerts, component);
      if (quarantine) {
        log.debug("Component {} in repository {}:{} ({}) was quarantined", pathname,
            repository.getRepositoryManagerId(), repository.getPublicId(), repository.getId());
        try (AuditSession auditSession = AuditData.get().recordSystemEvent(AuditEvent.RETAIN_QUARANTINE, false)) {
          AuditData.get()
              .setRepository(repository)
              .setComponentHash(component.getHash())
              .setData("componentPathname", pathname);
        }
      }
      Date quarantineTime = quarantine ? evaluationTime : null;
      proxyRepositoryComponent =
          new ProxyRepositoryComponent(repository.getId(), pathname, evaluationTime, component.getHash(),
              component.getComponentIdentifier(), component.getMatchState().getId(), component.getIdentificationSource()
                  .getId(),
              evaluationTime);
      proxyRepositoryComponent.setQuarantineTime(quarantineTime);
      proxyRepositoryComponent.setAnalyzerFeaturesJson(JsonUtils.format(component.getAnalyzerFeatures()));
      proxyRepositoryComponent.setLastEvaluationStage(stageTypeId);
      if (repositoryComponentId == null) {
        proxyRepositoryComponentDAO.insert(tx, proxyRepositoryComponent);
      }
      else {
        proxyRepositoryComponent.setId(repositoryComponentId);
        proxyRepositoryComponentDAO.update(tx, proxyRepositoryComponent);
      }
    }
    else {
      proxyRepositoryComponent.setHash(component.getHash());
      proxyRepositoryComponent.setComponentIdentifier(component.getComponentIdentifier());
      proxyRepositoryComponent.setMatchStateId(component.getMatchState().getId());
      proxyRepositoryComponent.setIdentificationSourceId(component.getIdentificationSource().getId());
      proxyRepositoryComponent.setLastEvaluationTime(evaluationTime);
      proxyRepositoryComponent.setLastEvaluationStage(stageTypeId);
      proxyRepositoryComponent.setAnalyzerFeaturesJson(JsonUtils.format(component.getAnalyzerFeatures()));

      if (proxyRepositoryComponent.isQuarantined() && !shouldQuarantine(activeAlerts, component)) {
        // The component is quarantined, but it doesn't have any policy violations/alerts that would quarantine it
        // anymore.
        unquarantineComponent(repository, proxyRepositoryComponent, evaluationTime, forMonitoring,
            explicitReleaseReason, newRepositoryPolicyViolations);
      }

      proxyRepositoryComponentDAO.update(tx, proxyRepositoryComponent);
    }

    boolean nowQuarantined = proxyRepositoryComponent.isQuarantined();
    if (!wasQuarantined && nowQuarantined) {
      try {
        firewallPolicyAlertEventService.postEvent(repository, pathname, component.getHash(),
            proxyRepositoryComponent.getQuarantineTime(), activeAlerts);
      }
      catch (RuntimeException e) {
        log.error("Failed to post FirewallPolicyAlertEvent for repository {} pathname {}",
            repository.getId(), pathname, e);
      }
    }

    return proxyRepositoryComponent;
  }

  private void unquarantineComponent(
      Repository repository,
      ProxyRepositoryComponent proxyRepositoryComponent,
      Date evaluationTime,
      boolean forMonitoring,
      ReleaseReason explicitReleaseReason,
      List<ProxyRepositoryPolicyViolation> newRepositoryPolicyViolations)
  {
    if (AuditData.get().getEvent() != null && !AuditData.get().getEvent().equals(AuditEvent.RELEASE_QUARANTINE)) {
      try (AuditSession auditSession = AuditData.get().recordSubEvent(AuditEvent.RELEASE_QUARANTINE, false)) {
        AuditData.get()
            .setRepository(repository)
            .setComponentHash(proxyRepositoryComponent.getHash())
            .setData("componentPathname", proxyRepositoryComponent.getPathname());
      }
    }

    if (forMonitoring) {
      proxyRepositoryComponent.setUnquarantineTimeForMonitoring(evaluationTime);
    }
    else {
      proxyRepositoryComponent.setUnquarantineTimeForManualRelease(evaluationTime);
    }

    List<ProxyRepositoryPolicyViolation> proxyRepositoryPolicyViolations = newRepositoryPolicyViolations.stream()
        .filter(violation -> !violation.isWaived())
        .collect(Collectors.toList());

    ReleaseQuarantineType releaseQuarantineType =
        forMonitoring ? ReleaseQuarantineType.AUTO : ReleaseQuarantineType.MANUAL;

    // Use explicit release reason if provided, otherwise derive from forMonitoring flag
    String releaseReason;
    if (explicitReleaseReason != null) {
      releaseReason = explicitReleaseReason.getDescription();
    }
    else {
      releaseReason = forMonitoring
          ? ReleaseReason.MONITORING_ENABLED.getDescription()
          : ReleaseReason.POLICY_CHANGE.getDescription();
    }

    proxyRepositoryComponentTelemetryCreator.sendRepositoryComponentTelemetry(proxyRepositoryComponent,
        proxyRepositoryPolicyViolations, repository.getRepositoryManagerId(), repository.getPublicId(),
        RepositoryComponentTelemetryEventType.RELEASE_QUARANTINE,
        releaseQuarantineType, releaseReason, Collections.emptyList());
  }

  private void sendRepositoryComponentTelemetry(
      final List<PolicyNotification> policyNotifications,
      final ProxyRepositoryComponent proxyRepositoryComponent,
      final Repository repository,
      final boolean isNotificationsToBeSent,
      final Component component,
      final List<ProxyRepositoryPolicyViolation> newRepositoryPolicyViolations)
  {
    proxyRepositoryComponentTelemetryCreator.sendRepositoryComponentTelemetry(proxyRepositoryComponent,
        newRepositoryPolicyViolations, repository.getRepositoryManagerId(), repository.getPublicId(),
        proxyRepositoryComponent.isQuarantined()
            ? RepositoryComponentTelemetryEventType.QUARANTINE
            : RepositoryComponentTelemetryEventType.AUDIT,
        isNotificationsToBeSent ? policyNotifications : Collections.emptyList(),
        component);
  }

  private List<ProxyRepositoryPolicyViolation> persistPolicyViolations(
      TransactionContext tx,
      Repository repository,
      Date evaluationTime,
      Component component,
      List<Policy> policies,
      ProxyRepositoryPolicyViolationLogger policyViolationLogger,
      CreateRepositoryPolicyViolationsEvent event,
      List<PolicyAlert> allPolicyAlertsByComponent,
      Map<PolicyAlert, PolicyWaiver> policyWaiversByComponent,
      List<ProxyRepositoryPolicyViolation> oldPolicyViolations)
  {
    String pathname = component.getPathnames().get(0);
    proxyRepositoryPolicyViolationDAO.loadConstraintFacts(oldPolicyViolations);

    // Build the list of current ProxyRepositoryPolicyViolations for this component
    List<ProxyRepositoryPolicyViolation> newPolicyViolations = new ArrayList<>();
    for (PolicyAlert policyAlert : allPolicyAlertsByComponent) {
      ComponentFact componentFact = getComponentFact(policyAlert, component);
      if (componentFact == null) {
        continue;
      }

      Policy policy = policies.stream()
          .filter(p -> p.getId().equals(policyAlert.getTrigger().getPolicyId()))
          .findFirst()
          .get();

      ProxyRepositoryPolicyViolation policyViolation =
          createRepositoryPolicyViolation(policyAlert, policy, componentFact,
              pathname,
              repository, evaluationTime, policyWaiversByComponent.get(policyAlert));
      newPolicyViolations.add(policyViolation);
    }

    PolicyViolationDiff<ProxyRepositoryPolicyViolation> policyViolationDiff =
        PolicyViolationDigester.digestPolicyViolations(oldPolicyViolations, newPolicyViolations);

    for (ProxyRepositoryPolicyViolation clearedPolicyViolation : policyViolationDiff.getCleared()) {
      policyViolationLogger.add(PolicyViolationLogEvent.FIX, clearedPolicyViolation);
    }
    proxyRepositoryPolicyViolationDAO.deleteBatch(tx, policyViolationDiff.getCleared());

    for (ProxyRepositoryPolicyViolation newPolicyViolation : policyViolationDiff.getAppeared()) {
      if (event != null) {
        event.proxyRepositoryPolicyViolations.add(newPolicyViolation);
      }
      policyViolationLogger.add(PolicyViolationLogEvent.CREATE, newPolicyViolation);
      if (newPolicyViolation.isWaived()) {
        policyViolationLogger.add(PolicyViolationLogEvent.WAIVE, newPolicyViolation);
      }
    }
    proxyRepositoryPolicyViolationDAO.insertBatch(tx, policyViolationDiff.getAppeared());

    // Update the existing violations so that 'time' is set and the original violation waive time, if it exists,
    // is brought forward
    List<ProxyRepositoryPolicyViolation> updatedPolicyViolations =
        new ArrayList<>(policyViolationDiff.getSame().size());
    for (Map.Entry<ProxyRepositoryPolicyViolation, ProxyRepositoryPolicyViolation> entry : policyViolationDiff.getSame()
        .entrySet())
    {
      ProxyRepositoryPolicyViolation oldPolicyViolation = entry.getKey();
      ProxyRepositoryPolicyViolation newPolicyViolation = entry.getValue();

      boolean isOldPolicyViolationWaived = oldPolicyViolation.isWaived();
      boolean isNewPolicyViolationWaived = newPolicyViolation.isWaived();

      if (isNewPolicyViolationWaived && isOldPolicyViolationWaived && null != oldPolicyViolation.getWaiveTime()) {
        newPolicyViolation.setWaiveTime(oldPolicyViolation.getWaiveTime());
      }
      newPolicyViolation.setId(oldPolicyViolation.getId());
      updatedPolicyViolations.add(newPolicyViolation);

      if (!isNewPolicyViolationWaived && isOldPolicyViolationWaived) {
        policyViolationLogger.add(PolicyViolationLogEvent.UNWAIVE, newPolicyViolation);
      }
      else if (isNewPolicyViolationWaived && !isOldPolicyViolationWaived) {
        policyViolationLogger.add(PolicyViolationLogEvent.WAIVE, newPolicyViolation);
      }
    }
    proxyRepositoryPolicyViolationDAO.updateBatch(tx, updatedPolicyViolations);

    // Preserve the wire ordering previously provided by getActiveByRepositoryIdAndPathname's
    // ORDER BY threat_level DESC, policy_id. Downstream telemetry consumers (event payloads emitted
    // by proxyRepositoryComponentTelemetryCreator) may rely on highest-threat-first iteration.
    newPolicyViolations.sort(Comparator
        .comparingInt(ProxyRepositoryPolicyViolation::getThreatLevel)
        .reversed()
        .thenComparing(ProxyRepositoryPolicyViolation::getPolicyId, Comparator.nullsLast(Comparator.naturalOrder())));
    return newPolicyViolations;
  }

  /**
   * Creates a ProxyRepositoryPolicyViolation from a policy alert.
   *
   * <p>
   * The actionTypeId will be set to the first non-NOTIFY action (FAIL or WARN). If the policy
   * has only NOTIFY actions, actionTypeId will remain null. This is intentional behavior for Firewall,
   * as NOTIFY-only policies don't represent quarantine or warning actions for repository components.
   * </p>
   *
   * @param policyAlert The policy alert containing actions
   * @param policy The policy that was triggered
   * @param componentFact The component fact from policy evaluation
   * @param pathname The repository pathname of the component
   * @param repository The repository where the violation occurred
   * @param evaluationTime The time of policy evaluation
   * @param policyWaiver Optional policy waiver (can be null)
   * @return ProxyRepositoryPolicyViolation with actionTypeId set to FAIL/WARN, or null if only NOTIFY actions exist
   */
  private ProxyRepositoryPolicyViolation createRepositoryPolicyViolation(
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
    ProxyRepositoryPolicyViolation policyViolation = new ProxyRepositoryPolicyViolation(repository.getId(), pathname,
        evaluationTime, policy.getId(), policy.getName(), policyFact.getThreatLevel(), threatCategory,
        componentFact.getHash(), componentFact.getComponentIdentifier(), componentFact.getConstraintFacts());

    // Don't save notification data into policy violations here because we don't want to send notifications for
    // policy violations on repository components. At least not yet.
    //
    // Set actionTypeId to the first FAIL or WARN action (skip NOTIFY actions).
    // Note: actionTypeId will be null if policy has only NOTIFY actions.
    // This is expected behavior - Firewall intentionally excludes NOTIFY-only actions as they
    // don't represent quarantine/warn behavior for repository components.
    for (Action action : policyAlert.getActions()) {
      if (!Action.ID_NOTIFY.equals(action.getActionTypeId())) {
        policyViolation.setActionTypeId(action.getActionTypeId()); // Will be Action.ID_FAIL or Action.ID_WARN
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
