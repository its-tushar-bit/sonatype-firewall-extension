/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.license;

import java.time.Duration;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupLicenseDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.license.LicenseThreatGroupCount;
import com.sonatype.insight.brain.model.license.LicenseThreatGroupLicense;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.conditions.LicenseThreatGroupConditionType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.tenancy.TenantReference;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.brain.webhook.EventAction;
import com.sonatype.insight.brain.webhook.ManagementEventService;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.17.0
 */
@Named
@Singleton
public class LicenseThreatGroupService
{
  private static final Logger log = LoggerFactory.getLogger(LicenseThreatGroupService.class);

  static final Duration COUNTS_CACHE_EXPIRATION = Duration.ofSeconds(30);

  static final long COUNTS_CACHE_MAXIMUM_SIZE = 1000L;

  private final LicenseThreatGroupDAO licenseThreatGroupDAO;

  private final LicenseThreatGroupLicenseDAO licenseThreatGroupLicenseDAO;

  private final PolicyDAO policyDAO;

  private final OwnerDAO ownerDAO;

  private final ManagementEventService managementEventService;

  private final IdUtils idUtils;

  private final LicenseThreatGroupUnreviewedComponentCounter unreviewedComponentCounter;

  // Tenant-scoped cache of per-owner LTG counts. Short TTL balances freshness with protection against repeated
  // aggregation queries on a dashboard widget (see CLM-38159-style fan-out scenarios). TenantReference wrapper
  // ensures MTIQ tenants never share cached entries.
  private final TenantReference<Cache<Map.Entry<OwnerType, String>, List<LicenseThreatGroupCount>>> countsCaches;

  @Inject
  public LicenseThreatGroupService(
      final LicenseThreatGroupDAO licenseThreatGroupDAO,
      final LicenseThreatGroupLicenseDAO licenseThreatGroupLicenseDAO,
      final PolicyDAO policyDAO,
      final OwnerDAO ownerDAO,
      final ManagementEventService managementEventService,
      final IdUtils idUtils,
      final LicenseThreatGroupUnreviewedComponentCounter unreviewedComponentCounter)
  {
    this.licenseThreatGroupDAO = licenseThreatGroupDAO;
    this.licenseThreatGroupLicenseDAO = licenseThreatGroupLicenseDAO;
    this.policyDAO = policyDAO;
    this.ownerDAO = ownerDAO;
    this.managementEventService = managementEventService;
    this.idUtils = idUtils;
    this.unreviewedComponentCounter = unreviewedComponentCounter;
    this.countsCaches = new TenantReference<>(this::createCountsCache);
  }

  @Authorize(permission = Permission.READ)
  public List<LicenseThreatGroup> getLicenseThreatGroups(
      @AuthzContext(AuthzContext.Key.TYPE) final OwnerType ownerType,
      @AuthzContext(AuthzContext.Key.ID) String ownerId)
  {
    ownerId = idUtils.getInternalOwnerId(ownerType, ownerId);

    return licenseThreatGroupDAO.getByOwnerId(ownerId);
  }

  /**
   * Returns per-License-Threat-Group counts of unreviewed components for the given owner, suitable for the Legal
   * Obligations dashboard tile (CLM-39604). Results include inherited LTGs (with zero counts when there are no
   * matching components) and are sorted by threat level DESC, unreviewed component count DESC, name ASC.
   *
   * <p>
   * Backed by a short-lived tenant-isolated cache to absorb bursts of dashboard refreshes. Cache entries are
   * invalidated on any LTG mutation for the same owner via {@link #invalidateCountsCacheFor(OwnerType, String)}.
   *
   * @since 1.204
   */
  @Authorize(permission = Permission.READ)
  public List<LicenseThreatGroupCount> getLicenseThreatGroupCounts(
      @AuthzContext(AuthzContext.Key.TYPE) final OwnerType ownerType,
      @AuthzContext(AuthzContext.Key.ID) String ownerId)
  {
    // Resolve the owner by id without assuming the path ownerType (e.g. organization path + application id).
    // Public application ids are normalized only when the path declares application scope.
    String lookupId = ownerType == OwnerType.APPLICATION
        ? idUtils.getInternalOwnerId(OwnerType.APPLICATION, ownerId)
        : ownerId;
    Owner owner = ownerDAO.getByIdNotNull(lookupId);
    if (owner.getType() != ownerType) {
      throw new BadRequestException(
          "Owner id '" + ownerId + "' is not a " + ownerType.name().toLowerCase(Locale.ROOT) + " owner.");
    }
    String internalOwnerId = owner.getId();
    Map.Entry<OwnerType, String> key = new AbstractMap.SimpleImmutableEntry<>(ownerType, internalOwnerId);
    try {
      return countsCaches.get().get(key, () -> {
        // Own the transaction here so the counter participates in a single connection rather than opening
        // its own (which would risk a nested-connection deadlock under pool exhaustion).
        try (TransactionContext tx = licenseThreatGroupDAO.createTransactionContext()) {
          return defensiveCopy(unreviewedComponentCounter.countByOwner(tx, ownerType, internalOwnerId));
        }
      });
    }
    catch (ExecutionException e) {
      Throwable cause = e.getCause();
      if (cause instanceof RuntimeException) {
        throw (RuntimeException) cause;
      }
      throw new RuntimeException("Failed to load license threat group counts", cause);
    }
  }

  /**
   * Returns per-License-Threat-Group counts of unreviewed components across the supplied application ids. Intended
   * for stacked dashboard code (CLM-39604 / #16041) where the caller has already resolved the user's authorized
   * application scope.
   *
   * @since 1.204
   */
  public List<LicenseThreatGroupCount> getUnreviewedComponentCountsByApplicationIds(
      final Collection<String> applicationIds)
  {
    if (applicationIds == null || applicationIds.isEmpty()) {
      return new ArrayList<>();
    }
    // Own the transaction here so the counter participates in a single connection rather than opening its
    // own (which would risk a nested-connection deadlock under pool exhaustion).
    try (TransactionContext tx = licenseThreatGroupDAO.createTransactionContext()) {
      return defensiveCopy(unreviewedComponentCounter.countByApplicationIds(tx, applicationIds));
    }
  }

  /**
   * Invalidates the tenant-wide LTG counts cache (conservative full flush — cheap at maxSize 1000). Called from
   * LTG CRUD and from {@link LicenseThreatGroupLicenseResource} when group membership changes.
   */
  public void invalidateLicenseThreatGroupCountsCache() {
    countsCaches.get().invalidateAll();
  }

  @Authorize(permission = Permission.READ)
  public ApplicableLicenseThreatGroups getApplicableLicenseThreatGroups(
      @AuthzContext(AuthzContext.Key.TYPE) final OwnerType ownerType,
      @AuthzContext(AuthzContext.Key.ID) String ownerId)
  {
    ownerId = idUtils.getInternalOwnerId(ownerType, ownerId);

    List<Owner> owners = ownerDAO.getOwnersInHierarchy(ownerId, ownerType);
    Map<String, List<LicenseThreatGroupWithLicenses>> licenseThreatGroupsByOwnerId =
        loadLicenseThreatGroupsByOwnerIds(owners.stream().map(Owner::getId).collect(Collectors.toList()));

    ApplicableLicenseThreatGroups result = new ApplicableLicenseThreatGroups();
    for (Owner owner : owners) {
      result.add(owner.getId(), owner.getName(), owner.getType(),
          licenseThreatGroupsByOwnerId.getOrDefault(owner.getId(), new ArrayList<>()));
    }
    return result;
  }

  @Authorize(permission = Permission.WRITE)
  public LicenseThreatGroup addLicenseThreatGroup(
      @AuthzContext(AuthzContext.Key.ORGANIZATION_ID) String orgId,
      final LicenseThreatGroup licenseThreatGroup)
  {
    licenseThreatGroup.setId(null);
    licenseThreatGroup.setOwnerId(orgId);
    licenseThreatGroupDAO.insert(licenseThreatGroup);

    auditLicenseThreatGroup(licenseThreatGroup);
    managementEventService.postEvent(EventAction.CREATED, licenseThreatGroup);
    invalidateCountsCacheFor(OwnerType.ORGANIZATION, orgId);

    return licenseThreatGroup;
  }

  @Authorize(permission = Permission.WRITE)
  public LicenseThreatGroup updateLicenseThreatGroup(
      @AuthzContext(AuthzContext.Key.TYPE) final OwnerType ownerType,
      @AuthzContext(AuthzContext.Key.ID) String ownerId,
      final LicenseThreatGroup licenseThreatGroup)
  {
    String internalOwnerId = idUtils.getInternalOwnerId(ownerType, ownerId);

    if (!internalOwnerId.equals(licenseThreatGroupDAO.getByIdNotNull(licenseThreatGroup.getId()).getOwnerId())) {
      throw new NotFoundException(
          "Cannot find a license threat group with id " + licenseThreatGroup.getId() + " for owner id " + ownerId);
    }

    licenseThreatGroup.setOwnerId(internalOwnerId);
    licenseThreatGroupDAO.update(licenseThreatGroup);

    auditLicenseThreatGroup(licenseThreatGroup);
    managementEventService.postEvent(EventAction.UPDATED, licenseThreatGroup);
    invalidateCountsCacheFor(ownerType, internalOwnerId);

    return licenseThreatGroup;
  }

  @Authorize(permission = Permission.WRITE)
  public void deleteLicenseThreatGroup(
      @AuthzContext(AuthzContext.Key.TYPE) final OwnerType ownerType,
      @AuthzContext(AuthzContext.Key.ID) final String ownerId,
      final String licenseThreatGroupId)
  {
    String internalOwnerId = idUtils.getInternalOwnerId(ownerType, ownerId);

    LicenseThreatGroup licenseThreatGroup = licenseThreatGroupDAO.getByIdNotNull(licenseThreatGroupId);
    if (!internalOwnerId.equals(licenseThreatGroup.getOwnerId())) {
      throw new NotFoundException("Cannot find a license threat group with ID " + licenseThreatGroupId + " for "
          + ownerType + " ID " + ownerId);
    }

    // Verify that the license threat group is not used in a policy condition
    validateLicenseThreatGroupNotUsedInAnyPolicy(ownerDAO.getById(internalOwnerId), licenseThreatGroup);

    licenseThreatGroupDAO.delete(licenseThreatGroup);

    auditLicenseThreatGroup(licenseThreatGroup);
    managementEventService.postEvent(EventAction.DELETED, licenseThreatGroup);
    invalidateCountsCacheFor(ownerType, internalOwnerId);
  }

  public static class ApplicableLicenseThreatGroups
  {
    public List<LicenseThreatGroupsByOwner> licenseThreatGroupsByOwner = new ArrayList<>();

    public void add(
        String ownerId,
        String ownerName,
        OwnerType ownerType,
        List<LicenseThreatGroupWithLicenses> licenseThreatGroups)
    {
      LicenseThreatGroupsByOwner ltgbo = new LicenseThreatGroupsByOwner();
      ltgbo.ownerId = ownerId;
      ltgbo.ownerName = ownerName;
      ltgbo.ownerType = ownerType;
      ltgbo.licenseThreatGroups = licenseThreatGroups;
      licenseThreatGroupsByOwner.add(ltgbo);
    }
  }

  public static class LicenseThreatGroupsByOwner
  {
    public String ownerId;

    public String ownerName;

    public OwnerType ownerType;

    public List<LicenseThreatGroupWithLicenses> licenseThreatGroups;
  }

  public static class LicenseThreatGroupWithLicenses
  {
    public String id;

    public String name;

    public int threatLevel;

    public List<LicenseThreatGroupLicense> licenses;
  }

  private Cache<Map.Entry<OwnerType, String>, List<LicenseThreatGroupCount>> createCountsCache() {
    return CacheBuilder.newBuilder()
        .expireAfterWrite(COUNTS_CACHE_EXPIRATION.toMillis(), TimeUnit.MILLISECONDS)
        .maximumSize(COUNTS_CACHE_MAXIMUM_SIZE)
        .build();
  }

  private void invalidateCountsCacheFor(final OwnerType ownerType, final String ownerId) {
    invalidateLicenseThreatGroupCountsCache();
  }

  private static List<LicenseThreatGroupCount> defensiveCopy(final List<LicenseThreatGroupCount> counts) {
    if (counts == null || counts.isEmpty()) {
      return new ArrayList<>();
    }
    List<LicenseThreatGroupCount> copies = new ArrayList<>(counts.size());
    for (LicenseThreatGroupCount count : counts) {
      copies.add(new LicenseThreatGroupCount(
          count.getLicenseThreatGroupId(),
          count.getLicenseThreatGroupName(),
          count.getThreatLevel(),
          count.getUnreviewedComponentCount()));
    }
    return copies;
  }

  private Map<String, List<LicenseThreatGroupWithLicenses>> loadLicenseThreatGroupsByOwnerIds(List<String> ownerIds) {
    Map<String, List<LicenseThreatGroup>> threatGroupsByOwnerId = licenseThreatGroupDAO.getByOwnerIds(ownerIds)
        .stream()
        .collect(Collectors.groupingBy(LicenseThreatGroup::getOwnerId));

    Set<String> threatGroupIds = threatGroupsByOwnerId.values()
        .stream()
        .flatMap(List::stream)
        .map(LicenseThreatGroup::getId)
        .collect(Collectors.toSet());
    Map<String, List<LicenseThreatGroupLicense>> licensesByThreatGroupId = threatGroupIds.isEmpty()
        ? Map.of()
        : licenseThreatGroupLicenseDAO.getByLicenseThreatGroupIds(threatGroupIds)
            .stream()
            .collect(Collectors.groupingBy(LicenseThreatGroupLicense::getLicenseThreatGroupId));

    Map<String, List<LicenseThreatGroupWithLicenses>> result = new HashMap<>();
    threatGroupsByOwnerId.forEach((ownerId, threatGroups) -> {
      List<LicenseThreatGroupWithLicenses> withLicenses = new ArrayList<>();
      for (LicenseThreatGroup ltg : threatGroups) {
        LicenseThreatGroupWithLicenses ltgwl = new LicenseThreatGroupWithLicenses();
        ltgwl.id = ltg.getId();
        ltgwl.name = ltg.getName();
        ltgwl.threatLevel = ltg.getThreatLevel();
        ltgwl.licenses = licensesByThreatGroupId.getOrDefault(ltg.getId(), new ArrayList<>());
        withLicenses.add(ltgwl);
      }
      result.put(ownerId, withLicenses);
    });
    return result;
  }

  private void auditLicenseThreatGroup(LicenseThreatGroup licenseThreatGroup) {
    AuditData.get() //
        .setLicenseThreatGroup(licenseThreatGroup)
        .setData("licenseThreatGroupThreatLevel", licenseThreatGroup.getThreatLevel());
  }

  private void validateLicenseThreatGroupNotUsedInAnyPolicy(Owner owner, LicenseThreatGroup licenseThreatGroup) {
    for (Policy policy : policyDAO.getByOwnerId(owner.getId())) {
      if (isLicenseThreatGroupUsedInPolicy(licenseThreatGroup.getId(), policy)) {
        String error = "Cannot delete the license threat group because it is used in a condition for the '"
            + policy.getName() + "' policy";
        if (!licenseThreatGroup.getOwnerId().equals(owner.getId())) {
          error += " in " + owner.getType() + " '" + owner.getName() + "'";
        }
        throw new BadRequestException(error);
      }
    }
    for (Owner childOwner : ownerDAO.getChildOwners(owner)) {
      validateLicenseThreatGroupNotUsedInAnyPolicy(childOwner, licenseThreatGroup);
    }
  }

  /**
   * Returns {@code true} if the given licenseThreatGroupId is used in the given policy; otherwise {@code false}.
   *
   */
  private static boolean isLicenseThreatGroupUsedInPolicy(String licenseThreatGroupId, Policy policy) {
    for (Constraint constraint : policy.getConstraints()) {
      for (Condition condition : constraint.getConditions()) {
        if (LicenseThreatGroupConditionType.ID.equals(condition.getConditionTypeId())
            && licenseThreatGroupId.equals(condition.getValue()))
        {
          return true;
        }
      }
    }
    return false;
  }
}
