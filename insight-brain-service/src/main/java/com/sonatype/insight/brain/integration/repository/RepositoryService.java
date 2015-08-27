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
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Map;

import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.component.ComponentDetails;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList.ComponentEvaluationData;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataRequestList;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataRequestList.ComponentEvaluationDataRequest;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.NamedComponentDetails;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList.RepositoryComponentEvaluationDataRequest;
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
import com.sonatype.clm.dto.model.component.ComponentDetails;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList.ComponentEvaluationData;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataRequestList;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataRequestList.ComponentEvaluationDataRequest;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.NamedComponentDetails;
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
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.hds.ComponentDetailsLoader;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.model.HashHelper;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.IdentificationSource;
import com.sonatype.insight.brain.model.component.MatchState;
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
import com.sonatype.insight.brain.policy.evaluator.ComponentPolicyEvaluator;
import com.sonatype.insight.brain.policy.evaluator.PolicyResults;
import com.sonatype.insight.brain.product.license.CLMLicenseManager;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
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

  static final String HDS_COMPONENT_DETAILS_PATH = "rest/component/details/evaluation";

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

  private static final RepositoryComponentDAO repositoryComponentDAO = new RepositoryComponentDAO();

  private static final RepositoryPolicyViolationDAO repositoryPolicyViolationDAO = new RepositoryPolicyViolationDAO();

  private static final PolicyDAO policyDAO = new PolicyDAO();

  private final HdsClient hdsClient;

  private final ComponentPolicyEvaluator componentPolicyEvaluator;

  private final ComponentDetailsLoader componentDetailsLoader;

  @Inject
  public RepositoryService(HdsClient hdsClient, ComponentPolicyEvaluator componentPolicyEvaluator,
      ComponentDetailsLoader componentDetailsLoader)
  {
    this.hdsClient = hdsClient;
    this.componentPolicyEvaluator = componentPolicyEvaluator;
    this.componentDetailsLoader = componentDetailsLoader;
  }

  public PolicyEvaluationSummary getPolicyEvaluationSummary(final String repositoryManagerInstanceId,
      final String repositoryPublicId)
  {
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
    List<RepositoryPolicyViolation> repositoryPolicyViolations =
        repositoryPolicyViolationDAO.getLastByRepositoryIdAndNotWaived(repository.getId());

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

    return policyEvaluationSummary;
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
      componentEvaluationDataRequest.hash = HashHelper.truncateHash(componentEvaluationDataRequest.hash);
    }
  }

  private void validateEvaluateRequest(RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList)
  {
    for (RepositoryComponentEvaluationDataRequest componentEvaluationDataRequest : componentEvaluationDataRequestList.components) {
      if (StringUtils.isBlank(componentEvaluationDataRequest.pathname)) {
        throw new BadRequestException("The pathname cannot be null or empty.");
      }
      if (StringUtils.isBlank(componentEvaluationDataRequest.hash)) {
        throw new BadRequestException("The hash cannot be null or empty.");
      }
    }
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
    Iterator<ComponentEvaluationData> componentEvaluationDataIterator = componentEvaluationDataList.components
        .iterator();
    ComponentEvaluationData currentComponentEvaluationData = componentEvaluationDataIterator.next();
    for (int requestIndex = 0; requestIndex < componentEvaluationDataRequestList.components.size(); requestIndex++) {
      if (currentComponentEvaluationData.requestIndex != requestIndex) {
        throw new IllegalStateException("The request index does not match. Expected " + requestIndex + ", but found "
            + currentComponentEvaluationData.requestIndex + ".");
      }
      ComponentEvaluationData componentEvaluationData = currentComponentEvaluationData;

      // Use the claimed component data if found
      NamedComponentDetails componentDetails = componentDetailsLoader.getComponentDetailsLocally(
          null /* componentIdentifier */, componentEvaluationData.hash);
      if (componentDetails != null) {
        // This is a claimed component, skip all results from HDS for this component.
        while (componentEvaluationDataIterator.hasNext()) {
          currentComponentEvaluationData = componentEvaluationDataIterator.next();
          if (currentComponentEvaluationData.requestIndex != requestIndex) {
            break;
          }
        }
      }
      else {
        ComponentIdentifier inputComponentIdentifier = componentEvaluationDataRequestList.components.get(requestIndex).componentIdentifier;
        boolean foundBestMatch = Objects.equals(inputComponentIdentifier, componentEvaluationData.componentIdentifier);
        while (componentEvaluationDataIterator.hasNext()) {
          currentComponentEvaluationData = componentEvaluationDataIterator.next();
          if (currentComponentEvaluationData.requestIndex != requestIndex) {
            break;
          }
          // This result is for the same input component. If it matches the input ComponentIdentifier, we'll use it as
          // best match.
          if (!foundBestMatch) {
            if (Objects.equals(inputComponentIdentifier, currentComponentEvaluationData.componentIdentifier)) {
              componentEvaluationData = currentComponentEvaluationData;
              foundBestMatch = true;
            }
          }
        }

        componentDetails = ComponentDetailsAdapter.convert(componentEvaluationData);
        componentDetails.setIdentificationSource(IdentificationSource.SONATYPE.getId());
        if (MatchState.UNKNOWN.getId().equals(componentDetails.getMatchState())) {
          componentDetails.setComponentIdentifier(inputComponentIdentifier);
        }
      }

      Component component = augmentComponentDetails(repository, componentDetails);
      // Evaluate the policies
      PolicyResults policyResults = componentPolicyEvaluator.evaluate(repository.getId(),
          new Stage(DevelopStageType.ID), Collections.singletonList(component), false /* forMonitoring */);

      String pathname = componentEvaluationDataRequestList.components.get(requestIndex).pathname;
      persistEvaluationResults(repository, pathname, now, componentDetails, policyResults);
    }

    log.debug("Evaluated {} components for repository id {} in {} ms.", componentEvaluationDataList.components.size(),
        repository.getId(), System.currentTimeMillis() - start);
  }

  private void persistEvaluationResults(Repository repository, String pathname, Date evaluationTime,
      ComponentDetails componentDetails, PolicyResults policyResults)
  {
    try (TransactionContext tx = repositoryComponentDAO.createTransactionContext()) {
      tx.begin();

      persistRepositoryComponent(tx, repository, pathname, evaluationTime, componentDetails);
      persistPolicyViolations(tx, repository, pathname, evaluationTime, policyResults);

      tx.commit();
    }
  }

  private void persistRepositoryComponent(TransactionContext tx, Repository repository, String pathname,
      Date evaluationTime, ComponentDetails componentDetails)
  {
    RepositoryComponent repositoryComponent = repositoryComponentDAO.getByRepositoryIdAndPathname(tx,
        repository.getId(), pathname);
    if (repositoryComponent == null) {
      repositoryComponent = new RepositoryComponent(repository.getId(), pathname, evaluationTime,
          componentDetails.getHash(), componentDetails.getComponentIdentifier(), componentDetails.getMatchState(),
          componentDetails.getIdentificationSource(), evaluationTime, false /* canBeQuarantined */);
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
        .getLastByRepositoryIdAndPathname(tx, repository.getId(), pathname);
    for (RepositoryPolicyViolation policyViolation : lastPolicyViolations) {
      policyViolation.setLatestEvaluation(false);
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
      RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList)
  {
    ComponentEvaluationDataRequestList hdsRequest = new ComponentEvaluationDataRequestList();
    hdsRequest.components = new ArrayList<>();

    // We want to get component details from HDS by hash only, not by component identifier
    for (RepositoryComponentEvaluationDataRequest componentEvaluationDataRequest : componentEvaluationDataRequestList.components) {
      hdsRequest.components
          .add(new ComponentEvaluationDataRequest(componentEvaluationDataRequest.hash, null /* componentIdentifier */));
    }

    try {
      long start = System.currentTimeMillis();

      ComponentEvaluationDataList result = hdsClient.post(ComponentEvaluationDataList.class,
          HDS_COMPONENT_DETAILS_PATH, hdsRequest);

      log.debug("Got component details from HDS for {} components in {} ms.",
          componentEvaluationDataRequestList.components.size(), System.currentTimeMillis() - start);

      return result;
    }
    catch (IOException e) {
      throw new RuntimeException("Failed to get component details from HDS: " + e.getMessage(), e);
    }
  }

  public void evaluateComponents(String repositoryManagerInstanceId, String repositoryPublicId,
      RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList)
  {
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
      componentEvaluationDataRequest.hash = HashHelper.truncateHash(componentEvaluationDataRequest.hash);
    }
  }

  private void validateEvaluateRequest(RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList)
  {
    for (RepositoryComponentEvaluationDataRequest componentEvaluationDataRequest : componentEvaluationDataRequestList.components) {
      if (StringUtils.isBlank(componentEvaluationDataRequest.pathname)) {
        throw new BadRequestException("The pathname cannot be null or empty.");
      }
      if (StringUtils.isBlank(componentEvaluationDataRequest.hash)) {
        throw new BadRequestException("The hash cannot be null or empty.");
      }
    }
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
    Iterator<ComponentEvaluationData> componentEvaluationDataIterator = componentEvaluationDataList.components
        .iterator();
    ComponentEvaluationData currentComponentEvaluationData = componentEvaluationDataIterator.next();
    for (int requestIndex = 0; requestIndex < componentEvaluationDataRequestList.components.size(); requestIndex++) {
      if (currentComponentEvaluationData.requestIndex != requestIndex) {
        throw new IllegalStateException("The request index does not match. Expected " + requestIndex + ", but found "
            + currentComponentEvaluationData.requestIndex + ".");
      }
      ComponentEvaluationData componentEvaluationData = currentComponentEvaluationData;

      // Use the claimed component data if found
      NamedComponentDetails componentDetails = componentDetailsLoader.getComponentDetailsLocally(
          null /* componentIdentifier */, componentEvaluationData.hash);
      if (componentDetails != null) {
        // This is a claimed component, skip all results from HDS for this component.
        while (componentEvaluationDataIterator.hasNext()) {
          currentComponentEvaluationData = componentEvaluationDataIterator.next();
          if (currentComponentEvaluationData.requestIndex != requestIndex) {
            break;
          }
        }
      }
      else {
        ComponentIdentifier inputComponentIdentifier = componentEvaluationDataRequestList.components.get(requestIndex).componentIdentifier;
        boolean foundBestMatch = Objects.equals(inputComponentIdentifier, componentEvaluationData.componentIdentifier);
        while (componentEvaluationDataIterator.hasNext()) {
          currentComponentEvaluationData = componentEvaluationDataIterator.next();
          if (currentComponentEvaluationData.requestIndex != requestIndex) {
            break;
          }
          // This result is for the same input component. If it matches the input ComponentIdentifier, we'll use it as
          // best match.
          if (!foundBestMatch) {
            if (Objects.equals(inputComponentIdentifier, currentComponentEvaluationData.componentIdentifier)) {
              componentEvaluationData = currentComponentEvaluationData;
              foundBestMatch = true;
            }
          }
        }

        componentDetails = ComponentDetailsAdapter.convert(componentEvaluationData);
        componentDetails.setIdentificationSource(IdentificationSource.SONATYPE.getId());
        if (MatchState.UNKNOWN.getId().equals(componentDetails.getMatchState())) {
          componentDetails.setComponentIdentifier(inputComponentIdentifier);
        }
      }

      Component component = augmentComponentDetails(repository, componentDetails);
      // Evaluate the policies
      PolicyResults policyResults = componentPolicyEvaluator.evaluate(repository.getId(),
          new Stage(DevelopStageType.ID), Collections.singletonList(component), false /* forMonitoring */);

      String pathname = componentEvaluationDataRequestList.components.get(requestIndex).pathname;
      persistEvaluationResults(repository, pathname, now, componentDetails, policyResults);
    }

    log.debug("Evaluated {} components for repository id {} in {} ms.", componentEvaluationDataList.components.size(),
        repository.getId(), System.currentTimeMillis() - start);
  }

  private void persistEvaluationResults(Repository repository, String pathname, Date evaluationTime,
      ComponentDetails componentDetails, PolicyResults policyResults)
  {
    try (TransactionContext tx = repositoryComponentDAO.createTransactionContext()) {
      tx.begin();

      persistRepositoryComponent(tx, repository, pathname, evaluationTime, componentDetails);
      persistPolicyViolations(tx, repository, pathname, evaluationTime, policyResults);

      tx.commit();
    }
  }

  private void persistRepositoryComponent(TransactionContext tx, Repository repository, String pathname,
      Date evaluationTime, ComponentDetails componentDetails)
  {
    RepositoryComponent repositoryComponent = repositoryComponentDAO.getByRepositoryIdAndPathname(tx,
        repository.getId(), pathname);
    if (repositoryComponent == null) {
      repositoryComponent = new RepositoryComponent(repository.getId(), pathname, evaluationTime,
          componentDetails.getHash(), componentDetails.getComponentIdentifier(), componentDetails.getMatchState(),
          componentDetails.getIdentificationSource(), evaluationTime, false /* canBeQuarantined */);
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
        .getLastByRepositoryIdAndPathname(tx, repository.getId(), pathname);
    for (RepositoryPolicyViolation policyViolation : lastPolicyViolations) {
      policyViolation.setLatestEvaluation(false);
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
      RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList)
  {
    ComponentEvaluationDataRequestList hdsRequest = new ComponentEvaluationDataRequestList();
    hdsRequest.components = new ArrayList<>();

    // We want to get component details from HDS by hash only, not by component identifier
    for (RepositoryComponentEvaluationDataRequest componentEvaluationDataRequest : componentEvaluationDataRequestList.components) {
      hdsRequest.components
          .add(new ComponentEvaluationDataRequest(componentEvaluationDataRequest.hash, null /* componentIdentifier */));
    }

    try {
      long start = System.currentTimeMillis();

      ComponentEvaluationDataList result = hdsClient.post(ComponentEvaluationDataList.class,
          HDS_COMPONENT_DETAILS_PATH, hdsRequest);

      log.debug("Got component details from HDS for {} components in {} ms.",
          componentEvaluationDataRequestList.components.size(), System.currentTimeMillis() - start);

      return result;
    }
    catch (IOException e) {
      throw new RuntimeException("Failed to get component details from HDS: " + e.getMessage(), e);
    }
  }
}
