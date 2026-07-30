/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationBaseDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiConstraintFactDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiStaleApplicationEvaluationDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiStaleEvaluationStageDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiStaleEvaluationsDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiStaleRepositoryEvaluationDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiStaleWaiverDTO;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dashboard.PolicyViolationState;
import com.sonatype.insight.brain.dashboard.filters.PolicyViolationStateFilter;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.PolicyEvaluationRequiredException;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverReasonDAO;
import com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dto.repository.RepositoryDTO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.PolicyWaiverReason;
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

  private final PolicyEvaluationDAO policyEvaluationDAO;

  private final RepositoryService repositoryService;

  private final RepositoryPolicyViolationDAO repositoryPolicyViolationDAO;

  private final ApplicationService applicationService;

  private final PolicyViolationLoader policyViolationLoader;

  private static final String STALE_WAIVERS_AUDIT_KEY = "numberOfStaleWaivers";

  private final Comparator<PolicyEvaluation> policyEvalTimeComparator =
      Comparator.comparingLong(o -> o.getTime().getTime());

  private final RepositoryComponentDAO repositoryComponentDAO;

  private final ApplicationDAO applicationDAO;

  private final PolicyWaiverReasonDAO policyWaiverReasonDAO;

  @Inject
  public ApiStaleWaiverService(
      OwnerDAO ownerDAO,
      PolicyDAO policyDAO,
      RepositoryDAO repositoryDAO,
      PolicyWaiverDAO policyWaiverDAO,
      PolicyEvaluationDAO policyEvaluationDAO,
      RepositoryService repositoryService,
      RepositoryPolicyViolationDAO repositoryPolicyViolationDAO,
      ApplicationService applicationService,
      PolicyViolationLoader policyViolationLoader,
      RepositoryComponentDAO repositoryComponentDAO,
      ApplicationDAO applicationDAO,
      PolicyWaiverReasonDAO policyWaiverReasonDAO)
  {
    this.ownerDAO = ownerDAO;
    this.policyDAO = policyDAO;
    this.repositoryDAO = repositoryDAO;
    this.policyWaiverDAO = policyWaiverDAO;
    this.policyEvaluationDAO = policyEvaluationDAO;
    this.repositoryService = repositoryService;
    this.repositoryPolicyViolationDAO = repositoryPolicyViolationDAO;
    this.applicationService = applicationService;
    this.policyViolationLoader = policyViolationLoader;
    this.repositoryComponentDAO = repositoryComponentDAO;
    this.applicationDAO = applicationDAO;
    this.policyWaiverReasonDAO = policyWaiverReasonDAO;
  }

  public List<ApiStaleWaiverDTO> getStaleWaivers() {
    Set<String> allUsedPolicyWaiverIds = new HashSet<>();

    List<Application> allApplications = applicationDAO.getAll();
    allUsedPolicyWaiverIds.addAll(getAllUsedApplicationWaiverIds(allApplications));
    allUsedPolicyWaiverIds.addAll(getAllUsedRepositoryWaiverIds());

    List<RepositoryDTO> authorizedRepositoryDTOs = repositoryService.getRepositories().repositories;

    Map<String, Repository> authorizedReposMap = Collections.emptyMap();
    if (authorizedRepositoryDTOs != null && !authorizedRepositoryDTOs.isEmpty()) {
      authorizedReposMap = authorizedRepositoryDTOs.stream()
          .map(repositoryDTO -> repositoryDTO.repository)
          .collect(Collectors.toMap(Repository::getId, Function.identity()));
    }

    // TreeSet with custom comparator on policy waiver id
    Set<PolicyWaiver> staleWaivers =
        getStaleRepositoryWaivers(allUsedPolicyWaiverIds, authorizedReposMap);

    List<Application> authorizedApplications = applicationService.getApplications();
    Set<String> authorizedApplicationIds =
        authorizedApplications.stream().map(Application::getId).collect(Collectors.toCollection(HashSet::new));
    staleWaivers.addAll(getStaleApplicationWaivers(allUsedPolicyWaiverIds, authorizedApplications));

    Map<String, PolicyWaiverReason> policyWaiversReasons = policyWaiverReasonDAO
        .getPolicyWaiverReasonIdToPolicyWaiverReasonMap();

    List<PolicyEvaluation> evaluations = policyEvaluationDAO.getAllLast();

    // evaluations is read-only, creating a copy to sort
    List<PolicyEvaluation> lastEvaluations = new ArrayList<>(evaluations);
    lastEvaluations.sort(policyEvalTimeComparator);

    Map<String, List<PolicyEvaluation>> lastEvaluationsByAppId =
        lastEvaluations.stream().collect(Collectors.groupingBy(PolicyEvaluation::getOwnerId));

    Map<String, String> policyIdToNameMap = policyDAO.getAll()
        .stream()
        .collect(Collectors.toMap(Policy::getId, Policy::getName));

    Map<String, Application> allApplicationsMap = allApplications.stream()
        .collect(
            Collectors.toMap(Application::getId, app -> app));

    Map<String, RepositoryWithDate> oldestEvalTimesByRepoId = getOldestEvalTimesByRepoId();

    List<ApiStaleWaiverDTO> staleWaiverDTOs = new ArrayList<>();
    for (PolicyWaiver policyWaiver : staleWaivers) {
      ApiStaleWaiverDTO staleWaiverDTO =
          createApiStaleWaiverDTO(policyIdToNameMap, policyWaiver, lastEvaluationsByAppId, lastEvaluations,
              allApplicationsMap, authorizedApplicationIds, oldestEvalTimesByRepoId, authorizedReposMap,
              policyWaiversReasons);
      staleWaiverDTOs.add(staleWaiverDTO);
    }

    AuditData.get().setData(STALE_WAIVERS_AUDIT_KEY, staleWaiverDTOs.size());

    return staleWaiverDTOs;
  }

  /**
   * @return All repos that have a component evaluation time (no empty repos)
   */
  private Map<String, RepositoryWithDate> getOldestEvalTimesByRepoId() {
    List<Repository> allRepositories = repositoryDAO.getAll();
    return allRepositories.stream()
        .map(
            repository -> {
              Date oldestDate =
                  repositoryComponentDAO.getOldestComponentEvaluationTimeByRepositoryId(repository.getId());
              return new RepositoryWithDate(repository, oldestDate);
            })
        .filter(repoWithDate -> repoWithDate.date != null) // no date on repo means it has no components
        .collect(Collectors.toMap(repoWithDate -> repoWithDate.repository.getId(), Function.identity()));
  }

  private List<ApiStaleApplicationEvaluationDTO> getStaleApplicationEvaluations(
      PolicyWaiver policyWaiver,
      Map<String, List<PolicyEvaluation>> evaluationsByAppId,
      List<PolicyEvaluation> lastEvaluations,
      Map<String, Application> allApplications,
      Set<String> authorizedApplicationIds,
      Owner owner)
  {
    // figure out if ownerId is app or root
    if (owner.getType() == OwnerType.APPLICATION) {
      List<PolicyEvaluation> staleAppEvaluations =
          getStaleAppEvaluationsByAppIds(policyWaiver, evaluationsByAppId, Collections.singleton(owner.getId()));
      return buildApplicationEvaluationDTOs(staleAppEvaluations, allApplications, authorizedApplicationIds);
    }
    else if (owner.getType() == OwnerType.ORGANIZATION) {
      if (owner.getId().equals(Organization.ROOT_ORGANIZATION_ID)) {
        PolicyEvaluation tempEval = new PolicyEvaluation();
        tempEval.setTime(policyWaiver.getCreateTime());

        int foundIndex = Collections.binarySearch(lastEvaluations, tempEval, policyEvalTimeComparator);
        int cutoffIndex = getCutoffIndex(lastEvaluations, foundIndex);

        return buildApplicationEvaluationDTOs(lastEvaluations.subList(0, cutoffIndex), allApplications,
            authorizedApplicationIds);
      }
      else {
        Set<String> applicationIdsForOrganization = applicationService
            .getApplicationIdsByOrganizationIds(new HashSet<>(Collections.singletonList(owner.getId())));
        List<PolicyEvaluation> allStaleAppEvaluations =
            getStaleAppEvaluationsByAppIds(policyWaiver, evaluationsByAppId, applicationIdsForOrganization);
        return buildApplicationEvaluationDTOs(allStaleAppEvaluations, allApplications, authorizedApplicationIds);
      }
    }
    // we expect that some items in list will not get processed

    return Collections.emptyList();
  }

  private int getCutoffIndex(final List<PolicyEvaluation> lastEvaluations, int foundIndex) {
    if (foundIndex >= 0) {
      // could be more than one that matches the time and we can't assume the index returned was the last.
      foundIndex = getLatestFoundIndex(foundIndex, lastEvaluations);
    }
    // we don't expect to find the value, if we do return one more,
    // else we will get back neg value of (the index that it would be inserted at + 1)
    return foundIndex >= 0 ? foundIndex + 1 : Math.abs(foundIndex) - 1;
  }

  /**
   * Returns the highest index that contains the same time as the evaluation at foundIndex.
   * package scope for testing
   */
  int getLatestFoundIndex(int foundIndex, final List<PolicyEvaluation> evaluations) {
    long timeToMatch = evaluations.get(foundIndex).getTime().getTime();
    while (foundIndex < evaluations.size()) {
      int candidateIndex = foundIndex + 1;
      if (candidateIndex < evaluations.size() && evaluations.get(candidateIndex).getTime().getTime() == timeToMatch) {
        foundIndex++;
      }
      else {
        break;
      }
    }
    return foundIndex;
  }

  private List<PolicyEvaluation> getStaleAppEvaluationsByAppIds(
      final PolicyWaiver policyWaiver,
      final Map<String, List<PolicyEvaluation>> evaluationsByAppId,
      final Set<String> applicationIds)
  {
    List<PolicyEvaluation> allStaleAppEvaluations = new ArrayList<>();
    for (String appId : applicationIds) {
      List<PolicyEvaluation> appPolicyEvaluations = evaluationsByAppId.get(appId);
      if (appPolicyEvaluations != null) {
        List<PolicyEvaluation> staleAppEvaluations = appPolicyEvaluations.stream()
            .filter(eval -> policyWaiver.getCreateTime().getTime() > eval.getTime().getTime())
            .collect(Collectors.toList());
        allStaleAppEvaluations.addAll(staleAppEvaluations);
      }
    }
    return allStaleAppEvaluations;
  }

  private List<ApiStaleApplicationEvaluationDTO> buildApplicationEvaluationDTOs(
      List<PolicyEvaluation> policyEvaluations,
      Map<String, Application> allApplications,
      Set<String> authorizedApplicationIds)
  {
    // will need to group policyEvaluations by applications to create the applicationEvaluationDTOs
    // can cycle through and check if in map, and if not build it, if it is in map just add the stage
    Map<String, ApiStaleApplicationEvaluationDTO> staleEvaluationsMap = new HashMap<>();

    for (PolicyEvaluation policyEvaluation : policyEvaluations) {
      Application application = allApplications.get(policyEvaluation.getOwnerId());
      if (authorizedApplicationIds.contains(application.getId())) {
        if (!staleEvaluationsMap.containsKey(policyEvaluation.getOwnerId())) {
          ApiStaleApplicationEvaluationDTO applicationEvaluationDTO = new ApiStaleApplicationEvaluationDTO();
          applicationEvaluationDTO.application = buildApiApplicationBaseDTO(application);
          staleEvaluationsMap.put(policyEvaluation.getOwnerId(), applicationEvaluationDTO);
        }
        staleEvaluationsMap.get(policyEvaluation.getOwnerId()).stages
            .add(buildApiStaleEvaluationStageDTO(policyEvaluation));
      }
    }
    return new ArrayList<>(staleEvaluationsMap.values());
  }

  private ApiStaleEvaluationStageDTO buildApiStaleEvaluationStageDTO(final PolicyEvaluation policyEvaluation) {
    ApiStaleEvaluationStageDTO stage = new ApiStaleEvaluationStageDTO();
    stage.lastEvaluationDate = policyEvaluation.getTime();
    stage.stageId = policyEvaluation.getStageTypeId();
    return stage;
  }

  private ApiApplicationBaseDTO buildApiApplicationBaseDTO(Application application) {
    ApiApplicationBaseDTO applicationBase = new ApiApplicationBaseDTO();
    applicationBase.id = application.getId();
    applicationBase.name = application.getName();
    applicationBase.contactUserName = application.getContactInternalName();
    applicationBase.organizationId = application.getOrganizationId();
    applicationBase.publicId = application.getPublicId();
    return applicationBase;
  }

  private Set<String> getAllUsedRepositoryWaiverIds() {
    List<String> allRepositoryIds = repositoryDAO.getAll()
        .stream()
        .map(Repository::getId)
        .collect(Collectors.toList());

    Set<String> allUsedWaiverIds =
        repositoryPolicyViolationDAO.getActiveWaivedRepositoryPolicyViolations(allRepositoryIds)
            .stream()
            .map(RepositoryPolicyViolation::getPolicyWaiverId)
            .collect(Collectors.toSet());

    // Repository violations can have legacy waivers without a waiverId. Throw an exception if we don't know
    // the waiverId for a waived violation. The repository has to be re-evaluated to set the waiverId.
    if (allUsedWaiverIds.contains(null)) {
      throw new PolicyEvaluationRequiredException(
          "All repositories must be re-evaluated to capture current waiver information.");
    }

    return allUsedWaiverIds;
  }

  private Set<PolicyWaiver> getStaleRepositoryWaivers(
      final Set<String> allUsedWaiverIds,
      final Map<String, Repository> authorizedReposMap)
  {
    // exclude duplicate waivers across repos with the custom comparator
    Set<PolicyWaiver> staleRepositoryWaivers = new TreeSet<>(comparing(PolicyWaiver::getId));

    if (!authorizedReposMap.isEmpty()) {
      authorizedReposMap.keySet().forEach(repositoryId -> {
        staleRepositoryWaivers.addAll(policyWaiverDAO.getActiveApplicableByOwnerId(repositoryId)
            .stream()
            .filter(policyWaiver -> !allUsedWaiverIds.contains(policyWaiver.getId()))
            .collect(Collectors.toList()));
      });
    }

    return staleRepositoryWaivers;
  }

  private ApiStaleWaiverDTO createApiStaleWaiverDTO(
      final Map<String, String> policyIdToNameMap,
      final PolicyWaiver policyWaiver,
      final Map<String, List<PolicyEvaluation>> lastEvaluationByApplicationId,
      final List<PolicyEvaluation> lastEvaluations,
      final Map<String, Application> allApplications,
      final Set<String> authorizedApplicationIds,
      final Map<String, RepositoryWithDate> oldestEvalTimesByRepoId,
      final Map<String, Repository> authorizedReposMap,
      final Map<String, PolicyWaiverReason> policyWaiversReasons)
  {
    ApiStaleWaiverDTO staleWaiverDTO = new ApiStaleWaiverDTO();
    staleWaiverDTO.waiverId = policyWaiver.getId();
    staleWaiverDTO.policyId = policyWaiver.getPolicyId();
    staleWaiverDTO.policyName = policyIdToNameMap.get(policyWaiver.getPolicyId());
    staleWaiverDTO.createTime = policyWaiver.getCreateTime();
    staleWaiverDTO.expiryTime = policyWaiver.getExpiryTime();
    staleWaiverDTO.comment = policyWaiver.getComment();
    staleWaiverDTO.creatorId = policyWaiver.getCreatorId();
    staleWaiverDTO.creatorName = policyWaiver.getCreatorName();
    staleWaiverDTO.policyWaiverReasonId = policyWaiver.getWaiverReasonId();
    if (policyWaiversReasons.containsKey(policyWaiver.getWaiverReasonId())) {
      staleWaiverDTO.reasonText = policyWaiversReasons.get(policyWaiver.getWaiverReasonId()).getReasonText();
    }

    List<ConstraintFact> constraintFacts = policyWaiver.getConstraintFacts();
    // older/legacy policy waivers do not have constraint facts
    if (constraintFacts != null && !constraintFacts.isEmpty()) {
      staleWaiverDTO.constraintFacts = constraintFacts.stream()
          .map(ApiConstraintFactDTO::new)
          .collect(Collectors.toList());
    }

    Owner owner = ownerDAO.getById(policyWaiver.getOwnerId());
    staleWaiverDTO.scopeOwnerId = owner.getId();
    staleWaiverDTO.scopeOwnerName = owner.getName();
    staleWaiverDTO.scopeOwnerType = ScopeOwnerUtils.getScopeOwnerType(owner.getType(), owner.getId());

    List<ApiStaleApplicationEvaluationDTO> staleApplicationEvaluations =
        getStaleApplicationEvaluations(policyWaiver, lastEvaluationByApplicationId, lastEvaluations, allApplications,
            authorizedApplicationIds, owner);

    List<ApiStaleRepositoryEvaluationDTO> staleRepoEvaluations =
        getStaleRepositoryEvaluations(policyWaiver, oldestEvalTimesByRepoId, owner, authorizedReposMap);

    if (staleApplicationEvaluations.size() > 0 || staleRepoEvaluations.size() > 0) {
      staleWaiverDTO.staleEvaluations = new ApiStaleEvaluationsDTO();
      if (staleApplicationEvaluations.size() > 0) {
        staleWaiverDTO.staleEvaluations.applications = staleApplicationEvaluations;
      }
      if (staleRepoEvaluations.size() > 0) {
        staleWaiverDTO.staleEvaluations.repositories = staleRepoEvaluations;
      }
    }

    return staleWaiverDTO;
  }

  private Set<String> getAllUsedApplicationWaiverIds(List<Application> allApplications) {
    // getting waived violations for all applications
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

  private Set<PolicyWaiver> getStaleApplicationWaivers(
      final Set<String> allUsedPolicyWaiverIds,
      final List<Application> authorizedApplications)
  {
    // exclude duplicate waivers across apps with the custom comparator
    Set<PolicyWaiver> stalePolicyWaivers = new TreeSet<>(comparing(PolicyWaiver::getId));

    for (Application application : authorizedApplications) {
      List<PolicyWaiver> applicableAppWaivers = policyWaiverDAO.getActiveApplicableByOwnerId(application.getId());

      // If the policy waiver id is not found in all used policy waiver id's, then it's stale (not used)
      stalePolicyWaivers.addAll(applicableAppWaivers.stream()
          .filter(waiver -> !allUsedPolicyWaiverIds.contains(waiver.getId()))
          .collect(Collectors.toList()));
    }

    return stalePolicyWaivers;
  }

  private List<ApiStaleRepositoryEvaluationDTO> getStaleRepositoryEvaluations(
      PolicyWaiver policyWaiver,
      Map<String, RepositoryWithDate> oldestEvalTimesByRepoId,
      Owner owner,
      final Map<String, Repository> authorizedReposMap)
  {
    List<RepositoryWithDate> staleRepositories = getStaleRepositories(policyWaiver, oldestEvalTimesByRepoId, owner);

    List<RepositoryWithDate> authorizedStaleRepositories =
        staleRepositories.stream()
            .filter(repo -> authorizedReposMap.containsKey(repo.repository.getId()))
            .collect(Collectors.toList());

    return buildRepositoryEvaluationDTOs(authorizedStaleRepositories);
  }

  private List<ApiStaleRepositoryEvaluationDTO> buildRepositoryEvaluationDTOs(
      List<RepositoryWithDate> staleRepositories)
  {
    List<ApiStaleRepositoryEvaluationDTO> staleRepoDTOs = new ArrayList<>(staleRepositories.size());
    for (RepositoryWithDate repositoryWithDate : staleRepositories) {
      ApiStaleRepositoryEvaluationDTO repoEval = new ApiStaleRepositoryEvaluationDTO();
      ApiRepositoryDTO repoDTO = new ApiRepositoryDTO();
      Repository repository = repositoryWithDate.repository;
      repoDTO.repositoryId = repository.getId();
      repoDTO.publicId = repository.getPublicId();
      repoDTO.format = repository.getFormat();
      repoEval.repository = repoDTO;
      ApiStaleEvaluationStageDTO stageDTO = new ApiStaleEvaluationStageDTO();
      stageDTO.stageId = Stage.ID_PROXY;
      stageDTO.lastEvaluationDate = repositoryWithDate.date;
      repoEval.stages = Collections.singletonList(stageDTO);
      staleRepoDTOs.add(repoEval);
    }
    return staleRepoDTOs;
  }

  private List<RepositoryWithDate> getStaleRepositories(
      PolicyWaiver policyWaiver,
      Map<String, RepositoryWithDate> oldestEvalTimesByRepoId,
      Owner owner)
  {
    List<RepositoryWithDate> staleRepositories = Collections.emptyList();

    if (owner.getId().equals(Organization.ROOT_ORGANIZATION_ID) ||
        owner.getType().equals(OwnerType.REPOSITORY_CONTAINER))
    {
      staleRepositories = oldestEvalTimesByRepoId.values()
          .stream()
          .filter(repoWithDate -> repoWithDate.date.getTime() <= policyWaiver.getCreateTime().getTime())
          .collect(Collectors.toList());
    }
    else if (owner.getType().equals(OwnerType.REPOSITORY)) {
      RepositoryWithDate repositoryWithDate = oldestEvalTimesByRepoId.get(policyWaiver.getOwnerId());
      if (repositoryWithDate != null && repositoryWithDate.date.getTime() <= policyWaiver.getCreateTime().getTime()) {
        staleRepositories = Collections.singletonList(repositoryWithDate);
      }
    }
    // we expect that some items in list will not get processed
    return staleRepositories;
  }

  private static class RepositoryWithDate
  {
    Repository repository;

    Date date;

    RepositoryWithDate(Repository repository, Date date) {
      this.repository = repository;
      this.date = date;
    }
  }
}
