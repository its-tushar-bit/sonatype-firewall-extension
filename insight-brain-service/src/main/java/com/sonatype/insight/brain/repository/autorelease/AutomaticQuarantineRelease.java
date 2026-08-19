/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.autorelease;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataList;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList.RepositoryComponentEvaluationDataRequest;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.api.v2.ApiFirewallService;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.audit.AuditSession;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLock;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLockManager;
import com.sonatype.insight.brain.dataaccess.policy.PolicyMonitoringDAO;
import com.sonatype.insight.brain.dataaccess.policy.ProxyRepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.ProxyRepositoryComponentDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.policy.PolicyMonitoring;
import com.sonatype.insight.brain.model.policy.ProxyRepositoryPolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.ProxyStageType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.ProxyRepositoryComponent;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.repository.RepositoryPolicyEvaluator;
import com.sonatype.insight.brain.repository.RepositoryService;
import com.sonatype.insight.brain.tenancy.TenantThreadLocal;
import com.sonatype.insight.license.model.LicensedFeature;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.152
 */
@Named
public class AutomaticQuarantineRelease
{
  private static final Logger log = LoggerFactory.getLogger(AutomaticQuarantineRelease.class);

  // derived from https://docs.sonatype.com/display/ADP/Firewall+Auto+Release+Quarantine+Policy+Condition+Types
  static final int MAX_REEVALUATION_DAYS_FOR_AUTO_RELEASED = 14;

  private final ProductLicense productLicense;

  private final AuditRecorder auditRecorder;

  private final RepositoryPolicyEvaluator repositoryPolicyEvaluator;

  private final ApiFirewallService apiFirewallService;

  private final PolicyMonitoringDAO policyMonitoringDAO;

  private final OwnerDAO ownerDAO;

  private final RepositoryDAO repositoryDAO;

  private final ProxyRepositoryComponentDAO proxyRepositoryComponentDAO;

  private final ProxyRepositoryPolicyViolationDAO proxyRepositoryPolicyViolationDAO;

  private final ClusterLockManager clusterLockManager;

  @Inject
  public AutomaticQuarantineRelease(
      final ProductLicense productLicense,
      final AuditRecorder auditRecorder,
      final RepositoryPolicyEvaluator repositoryPolicyEvaluator,
      final ApiFirewallService apiFirewallService,
      final PolicyMonitoringDAO policyMonitoringDAO,
      final OwnerDAO ownerDAO,
      final RepositoryDAO repositoryDAO,
      final ProxyRepositoryComponentDAO proxyRepositoryComponentDAO,
      final ProxyRepositoryPolicyViolationDAO proxyRepositoryPolicyViolationDAO,
      final ClusterLockManager clusterLockManager)
  {
    this.productLicense = productLicense;
    this.auditRecorder = auditRecorder;
    this.repositoryPolicyEvaluator = repositoryPolicyEvaluator;
    this.apiFirewallService = apiFirewallService;
    this.policyMonitoringDAO = policyMonitoringDAO;
    this.ownerDAO = ownerDAO;
    this.repositoryDAO = repositoryDAO;
    this.proxyRepositoryComponentDAO = proxyRepositoryComponentDAO;
    this.proxyRepositoryPolicyViolationDAO = proxyRepositoryPolicyViolationDAO;
    this.clusterLockManager = clusterLockManager;
    log.debug("Created a new AutomaticQuarantineRelease for tenant {}", TenantThreadLocal.getTenant());
  }

  public void run() {
    log.info("Starting automatic quarantine release for tenant {}", TenantThreadLocal.getTenant());

    long start = System.currentTimeMillis();

    List<PolicyMonitoring> policyMonitorings = policyMonitoringDAO.getAll();
    if (policyMonitorings.isEmpty()) {
      log.info("Policy monitoring was not configured for any applications, organizations, or repositories.");
      return;
    }

    Map<String, PolicyMonitoring> policyMonitoringsByOwnerId = new LinkedHashMap<>();
    for (PolicyMonitoring policyMonitoring : policyMonitorings) {
      policyMonitoringsByOwnerId.put(policyMonitoring.getOwnerId(), policyMonitoring);
    }

    evaluateApplicableQuarantinedRepositoryComponents(policyMonitoringsByOwnerId);

    log.info("Completed automatic quarantine release in {} ms for tenant {}", System.currentTimeMillis() - start,
        TenantThreadLocal.getTenant());
  }

  private void evaluateApplicableQuarantinedRepositoryComponents(
      final Map<String, PolicyMonitoring> policyMonitoringsByOwnerId)
  {
    if (!isLicensedForFirewall(productLicense)) {
      log.debug("Not licensed for Firewall Automatic Quarantine Release.");
      return;
    }
    log.debug("Licensed for Firewall Automatic Quarantine Release.");

    final Set<String> autoUnquarantineEnabledConditionTypes =
        apiFirewallService.getAutoUnquarantineEnabledPolicyConditionTypesIds();

    if (autoUnquarantineEnabledConditionTypes.isEmpty()) {
      log.debug("Skipping Firewall Automatic Quarantine Release. Auto un-quarantine condition types are not enabled.");
      return;
    }

    long start = System.currentTimeMillis();
    log.info("Starting automatic quarantine release");

    List<Repository> repositories = repositoryDAO.getAll();
    for (Repository repository : repositories) {
      try (ClusterLock clusterLock = clusterLockManager.createForRepositoryReevaluation(repository)) {
        if (clusterLock.tryLock()) {
          log.debug("Starting re-evaluation for repository {}:{} ({})", repository.getRepositoryManagerId(),
              repository.getPublicId(), repository.getId());
          reevaluateRepository(policyMonitoringsByOwnerId, autoUnquarantineEnabledConditionTypes, ownerDAO, repository);
        }
        else {
          log.debug("Skipping, re-evaluation for repository {}:{} ({}) is already in progress",
              repository.getRepositoryManagerId(), repository.getPublicId(), repository.getId());
          clusterLock.unlock();
        }
      }
      catch (Exception e) {
        log.error("An error occurred while re-evaluating repository {}:{} ({})", repository.getRepositoryManagerId(),
            repository.getPublicId(), repository.getId(), e);
        AuditData.get().setException(e);
      }
    }
    log.info("Finished automatic quarantine release in {} ms", System.currentTimeMillis() - start);
  }

  private void reevaluateRepository(
      final Map<String, PolicyMonitoring> policyMonitoringsByOwnerId,
      final Set<String> autoUnquarantineEnabledConditionTypes,
      final OwnerDAO ownerDAO,
      final Repository repository)
  {
    PolicyMonitoring policyMonitoring = null;
    for (Owner owner : ownerDAO.walkHierarchy(repository)) {
      policyMonitoring = policyMonitoringsByOwnerId.get(owner.getId());
      if (policyMonitoring != null) {
        break;
      }
    }

    if (policyMonitoring == null || !policyMonitoring.getStageTypeId().equals(ProxyStageType.ID)) {
      return;
    }

    log.debug("Getting quarantined components supporting auto un-quarantine of repository {}", repository.getName());
    List<ProxyRepositoryComponent> applicableQuarantinedComponents =
        getApplicableQuarantinedComponents(repository, autoUnquarantineEnabledConditionTypes);
    if (applicableQuarantinedComponents.isEmpty()) {
      return;
    }

    log.debug("Starting re-evaluation for {} repository components", applicableQuarantinedComponents.size());
    Iterator<ProxyRepositoryComponent> componentIterator = applicableQuarantinedComponents.iterator();
    int totalUnquarantineCount = 0;
    while (componentIterator.hasNext()) {
      try (AuditSession session = auditRecorder.recordSystemEvent(AuditEvent.EVALUATE_REPOSITORY)) {
        totalUnquarantineCount += autoUnquarantineComponents(repository, componentIterator);
      }
    }
    log.info("Auto un-quarantined {} components of repository {}:{} ({})", totalUnquarantineCount,
        repository.getRepositoryManagerId(), repository.getPublicId(), repository.getId());
  }

  private int autoUnquarantineComponents(
      final Repository repository,
      final Iterator<ProxyRepositoryComponent> componentIterator)
  {
    RepositoryComponentEvaluationDataRequestList evaluationRequestList =
        getRepositoryEvaluationRequest(repository, componentIterator);
    try {
      auditRepositoryComponentEvaluationList(repository, evaluationRequestList);
      // Part of the policy evaluation, the component is unquarantined if it doesn't have any policy violations that
      // require quarantine. Use evaluateForAutomaticRelease to properly set AUTO_RELEASED as the release reason.
      RepositoryComponentEvaluationDataList evaluationResults =
          repositoryPolicyEvaluator.evaluateForAutomaticRelease(repository, evaluationRequestList);

      int unquarantinedComponentsCount = (int) evaluationResults.componentEvalResults.stream()
          .filter(componentEvaluationData -> !componentEvaluationData.quarantine)
          .count();
      log.debug("Auto un-quarantined {} of {} components for repository {}", unquarantinedComponentsCount,
          evaluationResults.componentEvalResults.size(), repository.getName());
      return unquarantinedComponentsCount;
    }
    catch (RuntimeException e) {
      AuditData.get().setException(e);
      log.error("Failed policy evaluating for {} components of repository '{}': {}",
          evaluationRequestList.components.size(), repository.getName(), e.getMessage());
    }

    return 0;
  }

  private void auditRepositoryComponentEvaluationList(
      Repository repository,
      RepositoryComponentEvaluationDataRequestList repoComponentEvalList)
  {
    AuditData.get()
        .setRepository(repository)
        .setData("componentCount", repoComponentEvalList.components.size())
        .setData("evaluationCause", RepositoryComponentEvaluationDataRequestList.REEVALUATION);

    AuditData.get().setData("componentCount", repoComponentEvalList.components.size());
    if (repoComponentEvalList.cause != null) {
      AuditData.get().setData("evaluationCause", repoComponentEvalList.cause.replace('_', '-'));
    }
  }

  private List<ProxyRepositoryComponent> getApplicableQuarantinedComponents(
      final Repository repository,
      Set<String> autoUnquarantineEnabledConditionTypes)
  {
    Date minQuarantineDate = Date.from(Instant.now().minus(Duration.ofDays(MAX_REEVALUATION_DAYS_FOR_AUTO_RELEASED)));

    List<ProxyRepositoryComponent> quarantinedComponents =
        proxyRepositoryComponentDAO.getQuarantinedByRepositoryIdAndDate(repository.getId(), minQuarantineDate);

    List<ProxyRepositoryComponent> applicableQuarantinedComponents = new ArrayList<>();

    for (ProxyRepositoryComponent component : quarantinedComponents) {
      if (shouldCheckForUpdatedConditionTypes(component, autoUnquarantineEnabledConditionTypes)) {
        applicableQuarantinedComponents.add(component);
      }
    }
    return applicableQuarantinedComponents;
  }

  private RepositoryComponentEvaluationDataRequestList getRepositoryEvaluationRequest(
      Repository repository,
      Iterator<ProxyRepositoryComponent> componentIterator)
  {
    RepositoryComponentEvaluationDataRequestList evaluationDataRequests =
        new RepositoryComponentEvaluationDataRequestList(RepositoryComponentEvaluationDataRequestList.REEVALUATION);
    int componentCount = 0;
    while (componentIterator.hasNext() && componentCount < RepositoryService.MAX_REPOSITORY_EVALUATION_REQUEST_SIZE) {
      ProxyRepositoryComponent component = componentIterator.next();
      RepositoryComponentEvaluationDataRequest evaluationDataRequest =
          new RepositoryComponentEvaluationDataRequest(repository.getFormat(), component.getPathname(),
              component.getHash());
      evaluationDataRequests.components.add(evaluationDataRequest);
      componentCount++;
    }

    return evaluationDataRequests;
  }

  private boolean shouldCheckForUpdatedConditionTypes(
      final ProxyRepositoryComponent quarantinedComponent,
      final Set<String> supportedConditionTypes)
  {
    List<ProxyRepositoryPolicyViolation> violations = proxyRepositoryPolicyViolationDAO
        .getByRepositoryIdAndPathname(quarantinedComponent.getRepositoryId(), quarantinedComponent.getPathname());
    proxyRepositoryPolicyViolationDAO.loadConstraintFacts(violations);

    for (ProxyRepositoryPolicyViolation violation : violations) {
      for (ConstraintFact constraintFact : violation.getConstraintFacts()) {
        for (ConditionFact conditionFact : constraintFact.getConditionFacts()) {
          if (supportedConditionTypes.contains(conditionFact.getConditionTypeId())) {
            return true;
          }
        }
      }
    }
    return false;
  }

  static boolean isLicensedForFirewall(ProductLicense productLicense) {
    // not checking for FIREWALL_FOR_ARTIFACTORY at this time
    return productLicense.hasFeature(LicensedFeature.FIREWALL_AUTO_UNQUARANTINE)
        && productLicense.hasFeature(LicensedFeature.RELEASE_INTEGRITY);
  }
}
