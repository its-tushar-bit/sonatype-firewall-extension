/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.api.v2.dto.ApiConstraintFactDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiStaleWaiverDTO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dto.repository.RepositoryDTO;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.repository.RepositoryService;
import com.sonatype.insight.brain.utils.ScopeOwnerUtils;

import com.google.common.collect.Sets;

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

  @Inject
  public ApiStaleWaiverService(OwnerDAO ownerDAO,
      PolicyDAO policyDAO,
      RepositoryDAO repositoryDAO,
      PolicyWaiverDAO policyWaiverDAO,
      RepositoryService repositoryService,
      RepositoryPolicyViolationDAO repositoryPolicyViolationDAO)
  {
    this.ownerDAO = ownerDAO;
    this.policyDAO = policyDAO;
    this.repositoryDAO = repositoryDAO;
    this.policyWaiverDAO = policyWaiverDAO;
    this.repositoryService = repositoryService;
    this.repositoryPolicyViolationDAO = repositoryPolicyViolationDAO;
  }

  public List<ApiStaleWaiverDTO> getStaleRepositoryWaivers() {
    List<ApiStaleWaiverDTO> staleRepositoryWaivers = new ArrayList<>();
    List<RepositoryDTO> repositoryDTOs = repositoryService.getRepositories().repositories;

    if (repositoryDTOs != null && !repositoryDTOs.isEmpty()) {
      List<String> allRepositoryIds = repositoryDAO.getAll()
          .stream()
          .map(Repository::getId)
          .collect(Collectors.toList());

      Map<String, Repository> authorizedReposMap = repositoryDTOs
          .stream()
          .map(repositoryDTO -> repositoryDTO.repository)
          .collect(Collectors.toMap(Repository::getId, Function.identity()));

      Map<String, PolicyWaiver> waiversMapForAuthorizedRepos = new HashMap<>();
      authorizedReposMap.keySet().forEach(repositoryId -> {
        policyWaiverDAO.getApplicableByOwnerId(repositoryId).forEach(policyWaiver -> {
          waiversMapForAuthorizedRepos.put(policyWaiver.getId(), policyWaiver);
        });
      });

      Map<String, String> policyIdToNameMap = policyDAO.getAll()
          .stream()
          .collect(Collectors.toMap(Policy::getId, Policy::getName));

      Set<String> repositoryPolicyViolationWaiverIds =
          repositoryPolicyViolationDAO.getActiveWaivedRepositoryPolicyViolations(allRepositoryIds)
              .stream()
              .map(RepositoryPolicyViolation::getPolicyWaiverId)
              .collect(Collectors.toSet());

      Set<String> stalePolicyWaiverIds = Sets.difference(waiversMapForAuthorizedRepos.keySet(),
          repositoryPolicyViolationWaiverIds);

      stalePolicyWaiverIds.forEach(id -> {
        PolicyWaiver policyWaiver = waiversMapForAuthorizedRepos.get(id);
        ApiStaleWaiverDTO policyWaiverDTO = new ApiStaleWaiverDTO();
        policyWaiverDTO.waiverId = id;
        policyWaiverDTO.policyId = policyWaiver.getPolicyId();
        policyWaiverDTO.policyName = policyIdToNameMap.get(policyWaiver.getPolicyId());
        policyWaiverDTO.createTime = policyWaiver.getCreateTime();
        policyWaiverDTO.comment = policyWaiver.getComment();
        policyWaiverDTO.isObsolete = true;
        List<ConstraintFact> constraintFacts = policyWaiver.getConstraintFacts();
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

        staleRepositoryWaivers.add(policyWaiverDTO);
      });
    }

    return staleRepositoryWaivers;
  }
}
