/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.api.v2.dto.ApiConstraintFactDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiStaleWaiverDTO;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dashboard.PolicyViolationState;
import com.sonatype.insight.brain.dashboard.filters.PolicyViolationStateFilter;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dto.repository.RepositoryDTO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.organization.ApplicationService;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationLoader;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationLoader.ApplicationStageView;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationLoader.ApplicationView;
import com.sonatype.insight.brain.repository.RepositoryService;
import com.sonatype.insight.brain.utils.ScopeOwnerUtils;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toSet;

/**
 * @since 1.81
 */
@Named
public class ApiStaleWaiverService
{
  private final OwnerDAO ownerDAO;

  private final PolicyDAO policyDAO;

  private final RepositoryDAO repositoryDAO;

  private final PolicyWaiverDAO policyWaiverDAO;

  private final RepositoryService repositoryService;

  private final RepositoryPolicyViolationDAO repositoryPolicyViolationDAO;

  private final ApplicationService applicationService;

  private final PolicyViolationLoader policyViolationLoader;

  private static final String STALE_WAIVERS_AUDIT_KEY = "numberOfStaleWaivers";

  @Inject
  public ApiStaleWaiverService(OwnerDAO ownerDAO,
      PolicyDAO policyDAO,
      RepositoryDAO repositoryDAO,
      PolicyWaiverDAO policyWaiverDAO,
      RepositoryService repositoryService,
      RepositoryPolicyViolationDAO repositoryPolicyViolationDAO,
      ApplicationService applicationService,
      PolicyViolationLoader policyViolationLoader)
  {
    this.ownerDAO = ownerDAO;
    this.policyDAO = policyDAO;
    this.repositoryDAO = repositoryDAO;
    this.policyWaiverDAO = policyWaiverDAO;
    this.repositoryService = repositoryService;
    this.repositoryPolicyViolationDAO = repositoryPolicyViolationDAO;
    this.applicationService = applicationService;
    this.policyViolationLoader = policyViolationLoader;
  }

  public List<ApiStaleWaiverDTO> getStaleWaivers() {
    Set<String> allUsedPolicyWaiverIds = new HashSet<>();
    allUsedPolicyWaiverIds.addAll(getAllUsedApplicationWaiverIds());
    allUsedPolicyWaiverIds.addAll(getAllUsedRepositoryWaiverIds());

    // TreeSet with custom comparator on policy waiver id
    Set<PolicyWaiver> staleWaivers = getStaleRepositoryWaivers(allUsedPolicyWaiverIds);
    staleWaivers.addAll(getStaleApplicationWaivers(allUsedPolicyWaiverIds));

    Map<String, String> policyIdToNameMap = policyDAO.getAll().stream()
        .collect(Collectors.toMap(Policy::getId, Policy::getName));

    List<ApiStaleWaiverDTO> staleWaiverDTOs = new ArrayList<>();
    for (PolicyWaiver policyWaiver : staleWaivers) {
      ApiStaleWaiverDTO policyWaiverDTO = createApiStaleWaiverDTO(policyIdToNameMap, policyWaiver);
      staleWaiverDTOs.add(policyWaiverDTO);
    }

    AuditData.get().setData(STALE_WAIVERS_AUDIT_KEY, staleWaiverDTOs.size());

    return staleWaiverDTOs;
  }

  private Set<String> getAllUsedRepositoryWaiverIds() {
    List<String> allRepositoryIds = repositoryDAO.getAll()
        .stream()
        .map(Repository::getId)
        .collect(Collectors.toList());
    return repositoryPolicyViolationDAO.getActiveWaivedRepositoryPolicyViolations(allRepositoryIds)
        .stream()
        .map(RepositoryPolicyViolation::getPolicyWaiverId)
        .collect(Collectors.toSet());
  }

  private Set<PolicyWaiver> getStaleRepositoryWaivers(final Set<String> allUsedWaiverIds) {
    // exclude duplicate waivers across repos with the custom comparator
    Set<PolicyWaiver> staleRepositoryWaivers = new TreeSet<>(comparing(PolicyWaiver::getId));
    List<RepositoryDTO> repositoryDTOs = repositoryService.getRepositories().repositories;

    if (repositoryDTOs != null && !repositoryDTOs.isEmpty()) {
      Map<String, Repository> authorizedReposMap = repositoryDTOs
          .stream()
          .map(repositoryDTO -> repositoryDTO.repository)
          .collect(Collectors.toMap(Repository::getId, Function.identity()));

      authorizedReposMap.keySet().forEach(repositoryId -> {
        staleRepositoryWaivers.addAll(policyWaiverDAO.getApplicableByOwnerId(repositoryId).stream()
            .filter(policyWaiver -> !allUsedWaiverIds.contains(policyWaiver.getId()))
            .collect(Collectors.toList()));
      });
    }

    return staleRepositoryWaivers;
  }

  private ApiStaleWaiverDTO createApiStaleWaiverDTO(
      final Map<String, String> policyIdToNameMap,
      final PolicyWaiver policyWaiver)
  {
    ApiStaleWaiverDTO policyWaiverDTO = new ApiStaleWaiverDTO();
    policyWaiverDTO.waiverId = policyWaiver.getId();
    policyWaiverDTO.policyId = policyWaiver.getPolicyId();
    policyWaiverDTO.policyName = policyIdToNameMap.get(policyWaiver.getPolicyId());
    policyWaiverDTO.createTime = policyWaiver.getCreateTime();
    policyWaiverDTO.comment = policyWaiver.getComment();
    policyWaiverDTO.isObsolete = true;

    List<ConstraintFact> constraintFacts = policyWaiver.getConstraintFacts();
    // older/legacy policy waivers do not have constraint facts
    if (constraintFacts != null && !constraintFacts.isEmpty()) {
      policyWaiverDTO.constraintFacts = constraintFacts.stream()
          .map(ApiConstraintFactDTO::new)
          .collect(Collectors.toList());
    }

    Owner owner = ownerDAO.getById(policyWaiver.getOwnerId());
    if (owner != null) {
      policyWaiverDTO.scopeOwnerId = owner.getId();
      policyWaiverDTO.scopeOwnerName = owner.getName();
      policyWaiverDTO.scopeOwnerType = ScopeOwnerUtils.getScopeOwnerType(owner.getType(), owner.getId());
    }
    return policyWaiverDTO;
  }

  private Set<String> getAllUsedApplicationWaiverIds() {
    // getting waived violations for all applications
    List<Application> allApplications = new ApplicationDAO().getAll();
    Collection<ApplicationView> appViews =
        policyViolationLoader.getViolations(allApplications, null, false,
            new PolicyViolationStateFilter(PolicyViolationState.WAIVED).asPolicyViolationPredicate());

    Set<String> allUsedAppWaiverIds = new HashSet<>();
    for (ApplicationView appView : appViews) {
      // We need to report on the latest evaluation for EACH stage.
      for (ApplicationStageView appStageView : appView.getStageViews()) {
        Collection<PolicyViolation> policyViolations = appStageView.getFilteredViolations();
        allUsedAppWaiverIds
            .addAll(policyViolations.stream().map(PolicyViolation::getPolicyWaiverId).collect(toSet()));
      }
    }
    return allUsedAppWaiverIds;
  }

  private Set<PolicyWaiver> getStaleApplicationWaivers(final Set<String> allUsedPolicyWaiverIds) {
    // exclude duplicate waivers across apps with the custom comparator
    Set<PolicyWaiver> stalePolicyWaivers = new TreeSet<>(comparing(PolicyWaiver::getId));

    List<Application> authorizedApplications = applicationService.getApplications();
    for (Application application : authorizedApplications) {
      List<PolicyWaiver> applicableAppWaivers = policyWaiverDAO.getApplicableByOwnerId(application.getId());

      // If the policy waiver id is not found in all used policy waiver id's, then it's stale (not used)
      stalePolicyWaivers.addAll(applicableAppWaivers.stream()
          .filter(waiver -> !allUsedPolicyWaiverIds.contains(waiver.getId()))
          .collect(Collectors.toList()));
    }

    return stalePolicyWaivers;
  }
}
