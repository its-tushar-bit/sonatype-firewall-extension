/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
import com.sonatype.insight.brain.component.ComponentDetailsAdapter;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.hds.ComponentDetailsLoader;
import com.sonatype.insight.brain.hds.FirewallAuditHdsClient;
import com.sonatype.insight.brain.hds.FirewallQuarantineHdsClient;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.IdentificationSource;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.ProxyStageType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.policy.evaluator.ComponentPolicyEvaluator;
import com.sonatype.insight.brain.policy.evaluator.PolicyResults;
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

  private final FirewallAuditHdsClient auditHdsClient;

  private final FirewallQuarantineHdsClient quarantineHdsClient;

  private final ComponentDetailsLoader componentDetailsLoader;

  @Inject
  public RepositoryPolicyEvaluator(ComponentPolicyEvaluator componentPolicyEvaluator,
      RepositoryComponentDAO repositoryComponentDAO, RepositoryPolicyViolationDAO repositoryPolicyViolationDAO,
      FirewallAuditHdsClient auditHdsClient, FirewallQuarantineHdsClient quarantineHdsClient,
      ComponentDetailsLoader componentDetailsLoader)
  {
    this.componentPolicyEvaluator = componentPolicyEvaluator;
    this.repositoryComponentDAO = repositoryComponentDAO;
    this.repositoryPolicyViolationDAO = repositoryPolicyViolationDAO;
    this.auditHdsClient = auditHdsClient;
    this.quarantineHdsClient = quarantineHdsClient;
    this.componentDetailsLoader = componentDetailsLoader;
  }

  public RepositoryComponentEvaluationDataList evaluate(Repository repository,
      RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList, final boolean withQuarantine)
  {
    RepositoryComponentEvaluationDataList componentEvaluationResultList = new RepositoryComponentEvaluationDataList();

    Date now = new Date();

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
      NamedComponentDetails componentDetails = componentDetailsLoader
          .getComponentDetailsLocally(null /* componentIdentifier */, componentEvaluationData.hash);
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

      boolean quarantine = withQuarantine && shouldQuarantine(policyResults.getActiveAlerts(), component);
      Date quarantineTime = quarantine ? now : null;

      persistEvaluationResults(repository, now, component, policyResults, withQuarantine, quarantineTime);

      RepositoryComponentEvaluationData repositoryComponentEvaluationResult = new RepositoryComponentEvaluationData();
      repositoryComponentEvaluationResult.requestIndex = requestIndex;
      repositoryComponentEvaluationResult.quarantine = quarantine;
      componentEvaluationResultList.componentEvalResults.add(repositoryComponentEvaluationResult);
    }

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
      repositoryComponent = new RepositoryComponent(repository.getId(), pathname, evaluationTime, component.getHash(),
          component.getComponentIdentifier(), component.getMatchState().getId(),
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
      ComponentEvaluationDataList result = hdsClient.post(ComponentEvaluationDataList.class, HDS_COMPONENT_DETAILS_PATH,
          hdsRequest);

      log.debug("Got component details from HDS for {} components in {} ms.", hdsRequest.components.size(),
          System.currentTimeMillis() - start);

      return result;
    }
    catch (IOException e) {
      throw new RuntimeException("Failed to get component details from HDS: " + e.getMessage(), e);
    }
  }
}
