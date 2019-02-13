/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Date;
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
import com.sonatype.clm.dto.model.policy.Stage;
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
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.IdentificationSource;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
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
import com.sonatype.insight.dataaccess.TransactionContext;

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

  private final PendingRepositoryPolicyNotifications pendingRepositoryPolicyNotifications;

  private final FirewallAuditHdsClient auditHdsClient;

  private final FirewallQuarantineHdsClient quarantineHdsClient;

  private final ComponentDetailsLoader componentDetailsLoader;

  private final PolicyViolationLoggerFactory policyViolationLoggerFactory;

  @Inject
  public RepositoryPolicyEvaluator(ComponentPolicyEvaluator componentPolicyEvaluator,
                                   RepositoryComponentDAO repositoryComponentDAO,
                                   RepositoryPolicyViolationDAO repositoryPolicyViolationDAO,
                                   FirewallAuditHdsClient auditHdsClient,
                                   FirewallQuarantineHdsClient quarantineHdsClient,
                                   ComponentDetailsLoader componentDetailsLoader,
                                   PendingRepositoryPolicyNotifications pendingRepositoryPolicyNotifications,
                                   PolicyViolationLoggerFactory policyViolationLoggerFactory)
  {
    this.componentPolicyEvaluator = componentPolicyEvaluator;
    this.repositoryComponentDAO = repositoryComponentDAO;
    this.repositoryPolicyViolationDAO = repositoryPolicyViolationDAO;
    this.auditHdsClient = auditHdsClient;
    this.quarantineHdsClient = quarantineHdsClient;
    this.componentDetailsLoader = componentDetailsLoader;
    this.pendingRepositoryPolicyNotifications = pendingRepositoryPolicyNotifications;
    this.policyViolationLoggerFactory = policyViolationLoggerFactory;
  }

  @SuppressWarnings("checkstyle:LineLength")
  public RepositoryComponentEvaluationDataList evaluate(Repository repository,
                                                        RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList,
                                                        final boolean withQuarantine,
                                                        final String clientUserAgent)
  {
    RepositoryComponentEvaluationDataList componentEvaluationResultList = new RepositoryComponentEvaluationDataList();

    Date now = new Date();

    ComponentEvaluationDataList componentEvaluationDataList = getComponentDetailsFromHds(repository, withQuarantine,
        componentEvaluationDataRequestList, clientUserAgent);
    List<Component> components = new ArrayList<>();
    for (int requestIndex = 0; requestIndex < componentEvaluationDataRequestList.components.size(); requestIndex++) {
      RepositoryComponentEvaluationDataRequest componentEvaluationRequest =
          componentEvaluationDataRequestList.components.get(requestIndex);
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
      component.addPathname(componentEvaluationRequest.pathname);
      components.add(component);
    }

    // Evaluate the policies
    PolicyResults policyResults = componentPolicyEvaluator.evaluate(repository.getId(), new Stage(ProxyStageType.ID),
        components, false /* forMonitoring */);

    for (int requestIndex = 0; requestIndex < componentEvaluationDataRequestList.components.size(); requestIndex++) {
      Component component = components.get(requestIndex);
      RepositoryComponent repositoryComponent = persistEvaluationResults(repository, now, component,
          policyResults, withQuarantine);
      RepositoryComponentEvaluationData repositoryComponentEvaluationResult = new RepositoryComponentEvaluationData();
      repositoryComponentEvaluationResult.requestIndex = requestIndex;
      repositoryComponentEvaluationResult.quarantine = repositoryComponent.isQuarantined();
      componentEvaluationResultList.componentEvalResults.add(repositoryComponentEvaluationResult);
    }

    // Only notify new component evaluation policy violations
    if (RepositoryComponentEvaluationDataRequestList.NEW_COMPONENT.equals(componentEvaluationDataRequestList.cause)) {
      for (PolicyNotification policyNotification : policyResults.getActiveNotifications()) {
        pendingRepositoryPolicyNotifications.add(repository.getId(), policyNotification);
      }
    }

    return componentEvaluationResultList;
  }

  private RepositoryComponent persistEvaluationResults(Repository repository,
                                                       Date evaluationTime,
                                                       Component component,
                                                       PolicyResults policyResults,
                                                       boolean canBeQuarantined)
  {
    RepositoryComponent repositoryComponent;
    try (TransactionContext tx = repositoryComponentDAO.createTransactionContext()) {
      tx.begin();

      repositoryComponent = persistRepositoryComponent(tx, repository, evaluationTime, component,
          canBeQuarantined, policyResults);

      RepositoryPolicyViolationLogger policyViolationLogger =
          policyViolationLoggerFactory.newLogger(evaluationTime, repository);
      persistPolicyViolations(tx, repository, evaluationTime, component, policyResults, policyViolationLogger);

      tx.commit();
      AuditData.get().commitSubEvents();
      policyViolationLogger.log();
    }
    return repositoryComponent;
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
      repositoryComponentDAO.insert(tx, repositoryComponent);
    }
    else {
      repositoryComponent.setHash(component.getHash());
      repositoryComponent.setComponentIdentifier(component.getComponentIdentifier());
      repositoryComponent.setMatchStateId(component.getMatchState().getId());
      repositoryComponent.setIdentificationSourceId(component.getIdentificationSource().getId());
      repositoryComponent.setLastEvaluationTime(evaluationTime);
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
    // Update the current RepositoryPolicyViolations for this component
    List<RepositoryPolicyViolation> oldPolicyViolations =
        repositoryPolicyViolationDAO.getActiveByRepositoryIdAndPathname(tx, repository.getId(), pathname);
    for (RepositoryPolicyViolation policyViolation : oldPolicyViolations) {
      policyViolation.setActive(false);
      repositoryPolicyViolationDAO.update(tx, policyViolation);
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
          repository, evaluationTime, policyResults.getPolicyWaiver(componentFact) != null);
      repositoryPolicyViolationDAO.insert(tx, policyViolation);
      newPolicyViolations.add(policyViolation);
    }

    // Log policy violations
    if (policyViolationLogger.isEnabled()) {
      PolicyViolationDiff<RepositoryPolicyViolation> policyViolationDiff =
          PolicyViolationDigester.digestPolicyViolations(oldPolicyViolations, newPolicyViolations);
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
                                                                    boolean waived)
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

  private Component augmentComponentDetails(Repository repository, NamedComponentDetails componentDetails) {
    return componentDetailsLoader.augmentComponentDetails(repository, componentDetails);
  }

  @SuppressWarnings("checkstyle:LineLength")
  private ComponentEvaluationDataList getComponentDetailsFromHds(Repository repository,
                                                                 boolean withQuarantine,
                                                                 final RepositoryComponentEvaluationDataRequestList hdsRequest,
                                                                 final String clientUserAgent)
  {
    try {
      long start = System.currentTimeMillis();

      HdsClient hdsClient = withQuarantine ? quarantineHdsClient : auditHdsClient;
      ComponentEvaluationDataList result = hdsClient.post(HdsClientAnalytics.forOwner(repository),
          ComponentEvaluationDataList.class, HDS_COMPONENT_DETAILS_PATH, clientUserAgent, hdsRequest);

      log.debug("Got component details from HDS for {} components in {} ms.", hdsRequest.components.size(),
          System.currentTimeMillis() - start);

      return result;
    }
    catch (IOException e) {
      throw new UncheckedIOException("Failed to get component details from HDS: " + e.getMessage(), e);
    }
  }
}
