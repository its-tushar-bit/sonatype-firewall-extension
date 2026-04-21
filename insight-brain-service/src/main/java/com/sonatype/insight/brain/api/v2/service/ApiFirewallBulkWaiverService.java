/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.InternalServerErrorException;

import com.sonatype.insight.brain.api.v2.dto.ApiBulkWaiversDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiWaiverOptionsDTO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.commons.codec.digest.DigestUtils;
import org.jooq.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.api.v2.service.ApiPolicyWaiverService.MAX_BULK_WAIVER_VIOLATIONS;
import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.ALL_VERSIONS;
import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.EXACT_COMPONENT;

@Named
@Singleton
public class ApiFirewallBulkWaiverService
{
  private static final Logger log = LoggerFactory.getLogger(ApiFirewallBulkWaiverService.class);

  private final OwnerDAO ownerDAO;

  private final RepositoryDAO repositoryDAO;

  private final RepositoryComponentDAO repositoryComponentDAO;

  private final RepositoryPolicyViolationDAO repositoryPolicyViolationDAO;

  private final PolicyWaiverDAO policyWaiverDAO;

  private final ApiPolicyWaiverService apiPolicyWaiverService;

  private final IdUtils idUtils;

  @Inject
  public ApiFirewallBulkWaiverService(
      final OwnerDAO ownerDAO,
      final RepositoryDAO repositoryDAO,
      final RepositoryComponentDAO repositoryComponentDAO,
      final RepositoryPolicyViolationDAO repositoryPolicyViolationDAO,
      final PolicyWaiverDAO policyWaiverDAO,
      final ApiPolicyWaiverService apiPolicyWaiverService,
      final IdUtils idUtils)
  {
    this.ownerDAO = ownerDAO;
    this.repositoryDAO = repositoryDAO;
    this.repositoryComponentDAO = repositoryComponentDAO;
    this.repositoryPolicyViolationDAO = repositoryPolicyViolationDAO;
    this.policyWaiverDAO = policyWaiverDAO;
    this.apiPolicyWaiverService = apiPolicyWaiverService;
    this.idUtils = idUtils;
  }

  @Authorize(permission = Permission.WAIVE_POLICY_VIOLATIONS)
  public void addBulkPolicyWaivers(
      @AuthzContext(Key.TYPE) final OwnerType ownerType,
      @AuthzContext(Key.ID) final String ownerId,
      final ApiBulkWaiversDTO bulkWaiversDTO)
  {
    validateRequestData(bulkWaiversDTO);

    final Set<String> uniqueViolationIds = new LinkedHashSet<>(bulkWaiversDTO.violationIds());
    if (uniqueViolationIds.size() > MAX_BULK_WAIVER_VIOLATIONS) {
      throw new BadRequestException("Maximum " + MAX_BULK_WAIVER_VIOLATIONS + " violations allowed per waiver request");
    }

    final ApiWaiverOptionsDTO waiverOptionsDTO = bulkWaiversDTO.apiWaiverOptionsDTO();
    validateWaiverOptions(waiverOptionsDTO);
    validateOwnerType(ownerType);
    final String internalOwnerId = idUtils.getInternalOwnerId(ownerType, ownerId);

    try (TransactionContext tx = repositoryComponentDAO.createTransactionContext()) {
      tx.begin();

      final Map<String, Repository> repositoriesById = new HashMap<>();
      final Map<String, Boolean> repositoryOwnershipCache = new HashMap<>();
      final List<WaiverTelemetryData> telemetryQueue = new ArrayList<>();

      final List<PolicyWaiver> existingWaivers = policyWaiverDAO.getActiveApplicableByOwnerId(internalOwnerId);
      final Set<String> existingWaiverKeys = new HashSet<>(existingWaivers.size());
      for (PolicyWaiver waiver : existingWaivers) {
        existingWaiverKeys.add(buildWaiverKey(waiver.getPolicyId(), waiver.getHash(), waiver.getConstraintFactsJson()));
      }

      final List<String> existingViolations = new ArrayList<>();

      for (String violationId : uniqueViolationIds) {
        final RepositoryPolicyViolation repositoryPolicyViolation =
            repositoryPolicyViolationDAO.getByIdWithConstraintFacts(violationId);

        if (repositoryPolicyViolation == null) {
          throw new BadRequestException("Could not find repository policy violation with ID: " + violationId);
        }

        final String repoId = repositoryPolicyViolation.getRepositoryId();
        boolean belongsToOwner = repositoryOwnershipCache.computeIfAbsent(repoId,
            id -> isViolationOwnedByOwner(id, internalOwnerId));
        if (!belongsToOwner) {
          log.warn("Tenant isolation violation: violation {} does not belong to owner {}", violationId, ownerId);
          throw new BadRequestException("Violation " + violationId + " does not belong to owner " + ownerId);
        }

        final Repository repository = repositoriesById.computeIfAbsent(
            repoId,
            id -> repositoryDAO.getById(tx, id));
        if (repository == null) {
          throw new NotFoundException("Cannot find a repository with ID " + repoId + ".");
        }

        String waiverKey = buildWaiverKey(repositoryPolicyViolation.getPolicyId(), repositoryPolicyViolation.getHash(),
            repositoryPolicyViolation.getConstraintFactsJson());
        if (existingWaiverKeys.contains(waiverKey)) {
          existingViolations.add(violationId);
          continue;
        }

        final PolicyWaiver policyWaiver = apiPolicyWaiverService.savePolicyWaiver(
            tx,
            internalOwnerId,
            repositoryPolicyViolation,
            waiverOptionsDTO.comment,
            waiverOptionsDTO.matcherStrategy,
            waiverOptionsDTO.expiryTime,
            waiverOptionsDTO.waiverReasonId,
            waiverOptionsDTO.expireWhenRemediationAvailable);

        existingWaiverKeys.add(waiverKey);
        telemetryQueue.add(new WaiverTelemetryData(policyWaiver, repositoryPolicyViolation));
      }

      tx.commit();

      int waiversCreated = telemetryQueue.size();
      int waiversExist = existingViolations.size();
      int totalRequestedViolations = uniqueViolationIds.size();
      if (waiversExist > 0) {
        log.info("Bulk waiver completed: {} created, {} already have waivers for policy out of {} total violations",
            waiversCreated, waiversExist, totalRequestedViolations);
      }
      else {
        log.info("Bulk waiver completed: {} waivers created for {} violations", waiversCreated,
            totalRequestedViolations);
      }

      for (WaiverTelemetryData data : telemetryQueue) {
        apiPolicyWaiverService.auditAndSendTelemetry(
            ownerType, internalOwnerId, data.policyWaiver, data.violation);
      }
    }
    catch (BadRequestException | NotFoundException e) {
      throw e;
    }
    catch (Exception e) {
      if (isDatabaseFailure(e)) {
        log.error("Database failure while creating firewall bulk waivers for owner {}:{}", ownerType, ownerId, e);
        throw new InternalServerErrorException("Unable to create firewall bulk waivers due to a database error.");
      }
      log.error("Failed to create firewall bulk waivers for owner {}:{}", ownerType, ownerId, e);
      throw new BadRequestException("Unable to create firewall bulk waivers: " + e.getMessage());
    }
  }

  private void validateOwnerType(final OwnerType ownerType) {
    if (ownerType != OwnerType.ORGANIZATION &&
        ownerType != OwnerType.REPOSITORY &&
        ownerType != OwnerType.REPOSITORY_MANAGER &&
        ownerType != OwnerType.REPOSITORY_CONTAINER)
    {
      throw new BadRequestException("Unsupported Firewall bulk waiver owner type: " + ownerType);
    }
  }

  private void validateRequestData(final ApiBulkWaiversDTO bulkWaiversDTO) {
    if (bulkWaiversDTO == null) {
      throw new BadRequestException("Waivers request cannot be null");
    }
    if (bulkWaiversDTO.violationIds() == null || bulkWaiversDTO.violationIds().isEmpty()) {
      throw new BadRequestException("Violation IDs list cannot be null or empty");
    }
    if (bulkWaiversDTO.apiWaiverOptionsDTO() == null) {
      throw new BadRequestException("Waiver options cannot be null");
    }
  }

  private void validateWaiverOptions(final ApiWaiverOptionsDTO waiverOptionsDTO) {
    if (waiverOptionsDTO.matcherStrategy == null) {
      throw new BadRequestException("Matcher strategy is required");
    }
    if (waiverOptionsDTO.matcherStrategy != EXACT_COMPONENT && waiverOptionsDTO.matcherStrategy != ALL_VERSIONS) {
      throw new BadRequestException("Only EXACT_COMPONENT and ALL_VERSIONS matcher strategies are supported");
    }
    if (Objects.nonNull(waiverOptionsDTO.expiryTime) &&
        !waiverOptionsDTO.expiryTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().isAfter(LocalDate.now()))
    {
      throw new BadRequestException("Expiration date must be in the future.");
    }
    if (waiverOptionsDTO.expireWhenRemediationAvailable && waiverOptionsDTO.matcherStrategy != EXACT_COMPONENT) {
      throw new BadRequestException(
          "Expire When Remediation Available Waivers can only be applied to Exact Components.");
    }
  }

  private boolean isViolationOwnedByOwner(String repositoryId, String internalOwnerId) {
    for (Owner owner : ownerDAO.walkHierarchy(repositoryId)) {
      if (owner.getId().equals(internalOwnerId)) {
        return true;
      }
    }
    return false;
  }

  private boolean isDatabaseFailure(final Throwable throwable) {
    Throwable current = throwable;
    while (current != null) {
      if (current instanceof SQLException || current instanceof DataAccessException) {
        return true;
      }
      current = current.getCause();
    }
    return false;
  }

  private String buildWaiverKey(String policyId, String hash, String constraintFactsJson) {
    return policyId + ":" + (hash != null ? hash : "ALL") + ":" + hashConstraintFactsJson(constraintFactsJson);
  }

  private String hashConstraintFactsJson(String constraintFactsJson) {
    return constraintFactsJson != null ? DigestUtils.sha256Hex(constraintFactsJson) : "";
  }

  private record WaiverTelemetryData(PolicyWaiver policyWaiver, RepositoryPolicyViolation violation)
  {
  }
}
