/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.components;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerComponentDAO;
import com.sonatype.insight.brain.dataaccess.OwnerComponentDAO.ComponentOrganizationUsageRow;
import com.sonatype.insight.brain.dataaccess.OwnerComponentDAO.ComponentOwnerUsageRow;
import com.sonatype.insight.brain.dataaccess.OwnerComponentDAO.PagedOrganizationsByHash;
import com.sonatype.insight.brain.dataaccess.OwnerComponentDAO.PagedOwnersByHash;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.organization.ApplicationService;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.search.session.ReadableContextAuthzCache;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.license.model.LicensedFeature;

import org.apache.commons.lang3.StringUtils;

/**
 * Scalable where-used for Nexus One Component Detail (CLM-43959).
 * <p>
 * Seeks {@code owner_component} by hash (indexed), scopes via {@link ReadableContextAuthzCache}
 * (null owner filter = unrestricted; empty = fail-closed), pages in SQL. Does not materialize the
 * estate like Classic {@code ComponentDetailService} or {@code ApplicationService#getApplications()}.
 */
@Named
@Singleton
public class ComponentUsageService
{
  public static final int DEFAULT_PAGE_SIZE = 25;

  public static final int MAX_PAGE_SIZE = 100;

  /** Caps page so {@code page * pageSize} cannot overflow {@code int} offset. */
  static final int MAX_PAGE = Integer.MAX_VALUE / MAX_PAGE_SIZE;

  /**
   * Matches {@code owner_component.hash} ({@code varchar(20)} — truncated SHA-1). Longer values
   * cannot match stored rows; reject at the API boundary instead of returning empty pages.
   */
  static final int MAX_COMPONENT_HASH_LENGTH = 20;

  private final OwnerComponentDAO ownerComponentDAO;

  private final ApplicationService applicationService;

  private final OrganizationDAO organizationDAO;

  private final ProductLicense productLicense;

  private final ReadableContextAuthzCache readableContextAuthzCache;

  private final CurrentUser currentUser;

  @Inject
  public ComponentUsageService(
      final OwnerComponentDAO ownerComponentDAO,
      final ApplicationService applicationService,
      final OrganizationDAO organizationDAO,
      final ProductLicense productLicense,
      final ReadableContextAuthzCache readableContextAuthzCache,
      final CurrentUser currentUser)
  {
    this.ownerComponentDAO = ownerComponentDAO;
    this.applicationService = applicationService;
    this.organizationDAO = organizationDAO;
    this.productLicense = productLicense;
    this.readableContextAuthzCache = readableContextAuthzCache;
    this.currentUser = currentUser;
  }

  public ComponentUsageApplicationsResponseDTO listApplications(final ComponentUsageRequestDTO request) {
    UsageQuery query = prepareUsageQuery(request);

    PagedOwnersByHash paged = ownerComponentDAO.findDistinctOwnersByHashPaged(
        query.hash(), query.readableOwnerIds(), query.offset(), query.pageSize());

    List<String> pageOwnerIds = paged.rows().stream().map(ComponentOwnerUsageRow::ownerId).toList();
    Map<String, Application> applicationsById = applicationService
        .getAppsByIds(null, new LinkedHashSet<>(pageOwnerIds), null)
        .stream()
        .collect(Collectors.toMap(Application::getId, app -> app, (a, b) -> a));
    Map<String, List<String>> stagesByOwner =
        ownerComponentDAO.getStageTypeIdsByOwnerIdForHash(query.hash(), pageOwnerIds);

    Set<String> orgIds = applicationsById.values()
        .stream()
        .map(Application::getOrganizationId)
        .filter(Objects::nonNull)
        .collect(Collectors.toCollection(LinkedHashSet::new));
    Map<String, Organization> orgsById = organizationDAO.getByIds(orgIds)
        .stream()
        .collect(Collectors.toMap(Organization::getId, org -> org, (a, b) -> a));

    ComponentUsageApplicationsResponseDTO response = new ComponentUsageApplicationsResponseDTO();
    response.page = query.page();
    response.pageSize = query.pageSize();
    response.total = paged.total();
    response.hasNextPage = (long) query.offset() + paged.rows().size() < paged.total();
    for (ComponentOwnerUsageRow row : paged.rows()) {
      Application app = applicationsById.get(row.ownerId());
      if (app == null) {
        // DAO inner-joins application; defensive if getAppsByIds omits a row.
        continue;
      }
      ComponentUsageApplicationRowDTO dto = new ComponentUsageApplicationRowDTO();
      dto.applicationId = app.getId();
      dto.applicationPublicId = app.getPublicId();
      dto.applicationName = app.getName();
      dto.organizationId = app.getOrganizationId();
      Organization org = orgsById.get(app.getOrganizationId());
      dto.organizationName = org == null ? null : org.getName();
      dto.stageTypeIds = new ArrayList<>(stagesByOwner.getOrDefault(row.ownerId(), List.of()));
      dto.lastSeenTime = row.lastSeenTime() == null ? null : row.lastSeenTime().getTime();
      response.applications.add(dto);
    }
    return response;
  }

  public ComponentUsageOrganizationsResponseDTO listOrganizations(final ComponentUsageRequestDTO request) {
    UsageQuery query = prepareUsageQuery(request);

    PagedOrganizationsByHash paged = ownerComponentDAO.findDistinctOrganizationsByHashPaged(
        query.hash(), query.readableOwnerIds(), query.offset(), query.pageSize());

    Set<String> orgIds = paged.rows()
        .stream()
        .map(ComponentOrganizationUsageRow::organizationId)
        .collect(Collectors.toCollection(LinkedHashSet::new));
    Map<String, Organization> orgsById = organizationDAO.getByIds(orgIds)
        .stream()
        .collect(Collectors.toMap(Organization::getId, org -> org, (a, b) -> a));

    ComponentUsageOrganizationsResponseDTO response = new ComponentUsageOrganizationsResponseDTO();
    response.page = query.page();
    response.pageSize = query.pageSize();
    response.total = paged.total();
    response.hasNextPage = (long) query.offset() + paged.rows().size() < paged.total();
    for (ComponentOrganizationUsageRow row : paged.rows()) {
      ComponentUsageOrganizationRowDTO dto = new ComponentUsageOrganizationRowDTO();
      dto.organizationId = row.organizationId();
      Organization org = orgsById.get(row.organizationId());
      dto.organizationName = org == null ? null : org.getName();
      dto.applicationCount = row.applicationCount();
      dto.lastSeenTime = row.lastSeenTime() == null ? null : row.lastSeenTime().getTime();
      response.organizations.add(dto);
    }
    return response;
  }

  private UsageQuery prepareUsageQuery(final ComponentUsageRequestDTO request) {
    productLicense.validateFeature(LicensedFeature.DASHBOARD);
    String hash = requireComponentHash(request);
    int page = request.page == null ? 0 : request.page;
    int pageSize = request.pageSize == null ? DEFAULT_PAGE_SIZE : request.pageSize;
    validatePagination(page, pageSize);
    return new UsageQuery(hash, page, pageSize, page * pageSize, readableApplicationIdsOrNullIfUnrestricted());
  }

  private record UsageQuery(String hash, int page, int pageSize, int offset, Set<String> readableOwnerIds)
  {
  }

  /**
   * RBAC application owner scope for SQL.
   * <ul>
   * <li>{@code null} — unrestricted (admin / global / root): hash-only query, no IN clause</li>
   * <li>empty set — fail-closed (no readable contexts)</li>
   * <li>non-empty — APPLICATION ids from {@link ReadableContextAuthzCache} (already expanded)</li>
   * </ul>
   * Round trips: O(1) cache lookup; does not load Application entities for the estate.
   */
  private Set<String> readableApplicationIdsOrNullIfUnrestricted() {
    UserPrincipal principal = currentUser.getUserPrincipal();
    if (principal == null) {
      return Set.of();
    }
    Optional<Map<String, OwnerType>> contexts = readableContextAuthzCache.resolveReadableContexts(principal);
    if (contexts.isEmpty()) {
      return null;
    }
    Map<String, OwnerType> readable = contexts.get();
    if (readable.isEmpty()) {
      return Set.of();
    }
    return readable.entrySet()
        .stream()
        .filter(entry -> entry.getValue() == OwnerType.APPLICATION)
        .map(Map.Entry::getKey)
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  private static String requireComponentHash(final ComponentUsageRequestDTO request) {
    if (request == null || StringUtils.isBlank(request.componentHash)) {
      throw new BadRequestException("componentHash is required.");
    }
    String trimmed = request.componentHash.trim();
    if (trimmed.length() > MAX_COMPONENT_HASH_LENGTH) {
      throw new BadRequestException(
          "componentHash exceeds maximum length of " + MAX_COMPONENT_HASH_LENGTH + " characters.");
    }
    return trimmed;
  }

  private static void validatePagination(final int page, final int pageSize) {
    if (page < 0) {
      throw new BadRequestException("Invalid page: " + page + ". Page must be >= 0.");
    }
    if (page > MAX_PAGE) {
      throw new BadRequestException("Invalid page: " + page + ". Page must be <= " + MAX_PAGE + ".");
    }
    if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
      throw new BadRequestException(
          "Invalid page size: " + pageSize + ". Page size must be between 1 and " + MAX_PAGE_SIZE + ".");
    }
  }
}
