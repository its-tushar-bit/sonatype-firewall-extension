/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
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
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditSession;
import com.sonatype.insight.brain.component.ComponentDetailsAdapter;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.hds.ComponentDetailsLoader;
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
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.json.store.JsonUtils;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
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

  private final PolicyDAO policyDAO = new PolicyDAO();

  private final FirewallAuditHdsClient auditHdsClient;

  private final FirewallQuarantineHdsClient quarantineHdsClient;

  private final PolicyViolationLoggerFactory policyViolationLoggerFactory;
  
  private final FirewallIgnorePatternService firewallIgnorePatternService;
  
  private final RepositoryComponentDeleteService repositoryComponentDeleteService;

  private final RepositoryPolicyAlertEmailer repositoryPolicyAlertEmailer;

  // CLM-13933
  private static final LoadingCache<String, Object> componentLock = CacheBuilder.newBuilder()
      .expireAfterAccess(10, TimeUnit.MINUTES).build(CacheLoader.from(Object::new));

  @Inject
  public RepositoryPolicyEvaluator(
      ComponentPolicyEvaluator componentPolicyEvaluator,
      RepositoryComponentDAO repositoryComponentDAO,
      RepositoryPolicyViolationDAO repositoryPolicyViolationDAO,
      FirewallAuditHdsClient auditHdsClient,
      FirewallQuarantineHdsClient quarantineHdsClient,
      PolicyViolationLoggerFactory policyViolationLoggerFactory,
      FirewallIgnorePatternService firewallIgnorePatternService,
      RepositoryComponentDeleteService repositoryComponentDeleteService,
      RepositoryPolicyAlertEmailer repositoryPolicyAlertEmailer)
  {
    this.componentPolicyEvaluator = componentPolicyEvaluator;
    this.repositoryComponentDAO = repositoryComponentDAO;
    this.repositoryPolicyViolationDAO = repositoryPolicyViolationDAO;
    this.auditHdsClient = auditHdsClient;
    this.quarantineHdsClient = quarantineHdsClient;
    this.policyViolationLoggerFactory = policyViolationLoggerFactory;
    this.firewallIgnorePatternService = firewallIgnorePatternService;
    this.repositoryComponentDeleteService = repositoryComponentDeleteService;
    this.repositoryPolicyAlertEmailer = repositoryPolicyAlertEmailer;
  }

  public RepositoryComponentEvaluationDataList evaluate(
      Repository repository,
      RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList,
      final boolean withQuarantine,
      final String clientUserAgent)
  {
    return evaluate(repository, componentEvaluationDataRequestList, withQuarantine, true, clientUserAgent);
  }

  public RepositoryComponentEvaluationDataList evaluate(
      Repository repository,
      RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList,
      boolean withQuarantine,
      boolean persistEvaluationResults,
      String clientUserAgent)
  {
    RepositoryComponentEvaluationDataList componentEvaluationResultList = new RepositoryComponentEvaluationDataList();

    Date now = new Date();

    ComponentEvaluationDataList componentEvaluationDataList = getComponentDetailsFromHds(repository, withQuarantine,
        componentEvaluationDataRequestList, clientUserAgent);
    Predicate<String> componentPathnameMatchesIgnorePattern =
        firewallIgnorePatternService.componentPathnameMatchesIgnorePattern(repository);
    List<Component> components = new ArrayList<>();
    ComponentDetailsLoader componentDetailsLoader = new ComponentDetailsLoader(repository);
    for (int requestIndex = 0; requestIndex < componentEvaluationDataRequestList.components.size(); requestIndex++) {
      RepositoryComponentEvaluationDataRequest componentEvaluationRequest =
          componentEvaluationDataRequestList.components.get(requestIndex);
      ComponentEvaluationData componentEvaluationData = componentEvaluationDataList.components.get(requestIndex);
      if (componentEvaluationData.requestIndex != requestIndex) {
        throw new IllegalStateException("The request index does not match. Expected " + requestIndex + ", but found "
            + componentEvaluationData.requestIndex + ".");
      }

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
        // Use the claimed component data if found
        NamedComponentDetails componentDetails = ComponentDetailsLoader
            .getComponentDetailsLocally(null /* componentIdentifier */, componentEvaluationData.hash);
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
    PolicyResults policyResults = componentPolicyEvaluator.evaluate(repository.getId(), new Stage(ProxyStageType.ID),
        components.stream().filter(Objects::nonNull).collect(Collectors.toList()), false /* forMonitoring */);

    for (int requestIndex = 0; requestIndex < componentEvaluationDataRequestList.components.size(); requestIndex++) {
      RepositoryComponentEvaluationData repositoryComponentEvaluationResult = new RepositoryComponentEvaluationData();
      repositoryComponentEvaluationResult.requestIndex = requestIndex;
      Component component = components.get(requestIndex);
      if (component != null) {
        if (persistEvaluationResults) {
          RepositoryComponent repositoryComponent = persistEvaluationResults(repository, now, component,
              policyResults, withQuarantine);
          repositoryComponentEvaluationResult.quarantine = repositoryComponent.isQuarantined();
        }
        else {
          repositoryComponentEvaluationResult.policyAlerts = getPolicyAlerts(policyResults, component);
        }
      }
      componentEvaluationResultList.componentEvalResults.add(repositoryComponentEvaluationResult);
    }

    // Only notify new component evaluation policy violations
    if (RepositoryComponentEvaluationDataRequestList.NEW_COMPONENT.equals(componentEvaluationDataRequestList.cause)) {
      repositoryPolicyAlertEmailer.sendNotifications(repository, policyResults.getActiveNotifications());
    }

    return componentEvaluationResultList;
  }

  private List<PolicyAlert> getPolicyAlerts(final PolicyResults policyResults, final Component component) {
    return policyResults.getActiveAlerts().stream()
        .filter(policyAlert -> getComponentFact(policyAlert, component) != null)
        .collect(Collectors.toList());
  }

  private RepositoryComponent persistEvaluationResults(Repository repository,
                                                       Date evaluationTime,
                                                       Component component,
                                                       PolicyResults policyResults,
                                                       boolean canBeQuarantined)
  {
    synchronized (componentLock.getUnchecked(repository.getId().concat(component.getPathnames().get(0)))) {
      RepositoryComponent repositoryComponent;
      try (TransactionContext tx = repositoryComponentDAO.createTransactionContext()) {
        tx.begin();

        RepositoryPolicyViolationLogger policyViolationLogger =
            policyViolationLoggerFactory.newLogger(evaluationTime, repository);

        // The order of the following calls are important and must not be changed. See: CLM-13853
        persistPolicyViolations(tx, repository, evaluationTime, component, policyResults, policyViolationLogger);
        repositoryComponent = persistRepositoryComponent(tx, repository, evaluationTime, component,
            canBeQuarantined, policyResults);

        tx.commit();
        AuditData.get().commitSubEvents();
        policyViolationLogger.log();
      }
      return repositoryComponent;
    }
  }

  private RepositoryComponent persistRepositoryComponent(TransactionContext tx,
                                                         Repository repository,
                                                         Date evaluationTime,
                                                         Component component,
                                                         boolean canBeQuarantined,
                                                         PolicyResults policyResults)
  {
    String pathname = component.getPathnames().get(0);
    RepositoryComponent repositoryComponent = repositoryComponentDAO.getByRepositoryIdAndPathname(tx,
        repository.getId(), pathname);
    if (repositoryComponent != null && !repositoryComponent.getHash().equals(component.getHash())) {
      if (repositoryComponent.isQuarantined()) {
        try (AuditSession auditSession = AuditData.get().recordSubEvent(AuditEvent.RESET_QUARANTINE, false)) {
          AuditData.get().setRepository(repository).setComponentHash(repositoryComponent.getHash())
              .setData("componentPathname", repositoryComponent.getPathname());
        }
      }
      repositoryComponentDAO.delete(tx, repositoryComponent);
      repositoryComponent = null;
    }
    if (repositoryComponent == null) {
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
      repositoryComponentDAO.insert(tx, repositoryComponent);
    }
    else {
      repositoryComponent.setHash(component.getHash());
      repositoryComponent.setComponentIdentifier(component.getComponentIdentifier());
      repositoryComponent.setMatchStateId(component.getMatchState().getId());
      repositoryComponent.setIdentificationSourceId(component.getIdentificationSource().getId());
      repositoryComponent.setLastEvaluationTime(evaluationTime);
      repositoryComponent.setAnalyzerFeaturesJson(JsonUtils.format(component.getAnalyzerFeatures()));
      repositoryComponentDAO.update(tx, repositoryComponent);
    }
    return repositoryComponent;
  }

  private void persistPolicyViolations(TransactionContext tx,
                                       Repository repository,
                                       Date evaluationTime,
                                       Component component,
                                       PolicyResults policyResults,
                                       RepositoryPolicyViolationLogger policyViolationLogger)
  {
    String pathname = component.getPathnames().get(0);
    // Delete the current RepositoryPolicyViolations for this component
    List<RepositoryPolicyViolation> oldPolicyViolations =
        repositoryPolicyViolationDAO.getActiveByRepositoryIdAndPathname(tx, repository.getId(), pathname);
    for (RepositoryPolicyViolation policyViolation : oldPolicyViolations) {
      repositoryPolicyViolationDAO.delete(tx, policyViolation);
    }
    // Insert new RepositoryPolicyViolations for this component
    List<RepositoryPolicyViolation> newPolicyViolations = new ArrayList<>();
    List<PolicyAlert> allPolicyAlerts = new ArrayList<>();
    allPolicyAlerts.addAll(policyResults.getActiveAlerts());
    allPolicyAlerts.addAll(policyResults.getWaivedAlerts());
    for (PolicyAlert policyAlert : allPolicyAlerts) {
      ComponentFact componentFact = getComponentFact(policyAlert, component);
      if (componentFact == null) {
        continue;
      }
      RepositoryPolicyViolation policyViolation = createRepositoryPolicyViolation(policyAlert, componentFact, pathname,
          repository, evaluationTime, policyResults.getPolicyWaiver(componentFact));
      repositoryPolicyViolationDAO.insert(tx, policyViolation);
      newPolicyViolations.add(policyViolation);
    }

    PolicyViolationDiff<RepositoryPolicyViolation> policyViolationDiff =
        PolicyViolationDigester.digestPolicyViolations(oldPolicyViolations, newPolicyViolations);

    /**
     * Since we create new records for repository policy violations even though the policy violation previously
     * existed, we need to preserve the waive time from the existing record. Note, that for older installs that did
     * not have the waive time previously set, we are ok simply using the new record's waive time and preserving that
     * moving forward.
     */
    for (Map.Entry<RepositoryPolicyViolation, RepositoryPolicyViolation> entry : policyViolationDiff.getSame()
        .entrySet()) {
      RepositoryPolicyViolation oldPolicyViolation = entry.getKey();
      RepositoryPolicyViolation newPolicyViolation = entry.getValue();
      if (newPolicyViolation.isWaived() && oldPolicyViolation.isWaived() && oldPolicyViolation.getWaiveTime() != null) {
        // Preserve the original waive time
        newPolicyViolation.setWaiveTime(oldPolicyViolation.getWaiveTime());
        repositoryPolicyViolationDAO.update(tx, newPolicyViolation);
      }
    }

    // Log policy violations
    if (policyViolationLogger.isEnabled()) {
      // New policy violations.
      for (RepositoryPolicyViolation newPolicyViolation : policyViolationDiff.getAppeared()) {
        policyViolationLogger.add(PolicyViolationLogEvent.CREATE, newPolicyViolation);
        if (newPolicyViolation.isWaived()) {
          policyViolationLogger.add(PolicyViolationLogEvent.WAIVE, newPolicyViolation);
        }
      }
      // Fixed policy violations.
      for (RepositoryPolicyViolation oldPolicyViolation : policyViolationDiff.getCleared()) {
        policyViolationLogger.add(PolicyViolationLogEvent.FIX, oldPolicyViolation);
      }
      // Existing policy violations.
      for (Map.Entry<RepositoryPolicyViolation, RepositoryPolicyViolation> entry : policyViolationDiff.getSame()
          .entrySet()) {
        RepositoryPolicyViolation oldPolicyViolation = entry.getKey();
        RepositoryPolicyViolation newPolicyViolation = entry.getValue();
        if (!newPolicyViolation.isWaived() && oldPolicyViolation.isWaived()) {
          // The policy violation was un-waived.
          policyViolationLogger.add(PolicyViolationLogEvent.UNWAIVE, newPolicyViolation);
        }
        else if (newPolicyViolation.isWaived() && !oldPolicyViolation.isWaived()) {
          // The policy violation was waived.
          policyViolationLogger.add(PolicyViolationLogEvent.WAIVE, newPolicyViolation);
        }
      }
    }
  }

  private RepositoryPolicyViolation createRepositoryPolicyViolation(PolicyAlert policyAlert,
                                                                    ComponentFact componentFact,
                                                                    String pathname,
                                                                    Repository repository,
                                                                    Date evaluationTime,
                                                                    PolicyWaiver policyWaiver)
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
