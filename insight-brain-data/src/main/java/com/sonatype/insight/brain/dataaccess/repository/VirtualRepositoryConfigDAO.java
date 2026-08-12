/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.repository;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.repository.ManagerType;
import com.sonatype.insight.brain.model.repository.VirtualRepositoryConfig;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;

import com.google.common.base.Strings;
import org.jooq.SQLDialect;
import org.jooq.Table;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.Repository.REPOSITORY;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.RepositoryManager.REPOSITORY_MANAGER;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.VirtualRepositoryConfig.VIRTUAL_REPOSITORY_CONFIG;

@Named
@Singleton
public class VirtualRepositoryConfigDAO
    extends AbstractOperationalSqlDAO<VirtualRepositoryConfig>
{
  private static final Set<String> ALLOWED_URL_SCHEMES = Set.of("http", "https");

  // Matches the varchar(2048) column width on both upstream_url and package_host_url. Enforcing
  // at the DAO turns an over-length client value into a 400 with a clean message instead of a
  // driver-level 500 (see CLM-38729 for the recurrence class).
  static final int MAX_URL_LENGTH = 2048;

  static final String UPSTREAM_URL_FIELD = "Upstream URL";

  static final String PACKAGE_HOST_URL_FIELD = "Package host URL";

  @Inject
  public VirtualRepositoryConfigDAO(final OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  @Override
  public Table<?> getJooqTable() {
    return VIRTUAL_REPOSITORY_CONFIG;
  }

  /**
   * @return the satellite config row for {@code repositoryId}, or {@code null} if none exists —
   *         absence is the majority case (only VRM-owned repositories carry a row here), so
   *         callers should null-check by default.
   */
  public VirtualRepositoryConfig getByRepositoryId(final TransactionContext tx, final String repositoryId) {
    return toEntity(tx.dsl()
        .selectFrom(VIRTUAL_REPOSITORY_CONFIG)
        .where(VIRTUAL_REPOSITORY_CONFIG.REPOSITORY_ID.eq(repositoryId))
        .fetchOne());
  }

  /**
   * @see #getByRepositoryId(TransactionContext, String)
   */
  public VirtualRepositoryConfig getByRepositoryId(final String repositoryId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByRepositoryId(tx, repositoryId);
    }
  }

  @Override
  public int insert(
      final TransactionContext tx,
      final VirtualRepositoryConfig entity,
      final boolean ignoreDuplicateKey)
  {
    validateEntity(tx, entity);
    return super.insert(tx, entity, ignoreDuplicateKey);
  }

  @Override
  public int update(final TransactionContext tx, final VirtualRepositoryConfig entity) {
    validateEntity(tx, entity);
    return super.update(tx, entity);
  }

  /**
   * On PostgreSQL the parent's batch path uses a bulk jOOQ INSERT that never routes through the
   * per-entity {@link #insert} override, so per-row validation must be re-applied here. On H2 the
   * parent falls back to a per-entity {@code insert()} loop where the override runs, so we skip
   * validation at the batch level to avoid double-work. Matches the guard pattern in
   * {@code ThirdPartyFileCoordinateDAO} and {@code AbstractPolicyViolationDAO}.
   */
  @Override
  public int insertBatch(
      final TransactionContext tx,
      final List<VirtualRepositoryConfig> entities,
      final boolean ignoreDuplicateKey)
  {
    if (tx.dsl().dialect() != SQLDialect.H2) {
      validateEntities(tx, entities);
    }
    return super.insertBatch(tx, entities, ignoreDuplicateKey);
  }

  /**
   * @see #insertBatch(TransactionContext, List, boolean)
   */
  @Override
  public int updateBatch(final TransactionContext tx, final List<VirtualRepositoryConfig> entities) {
    if (tx.dsl().dialect() != SQLDialect.H2) {
      validateEntities(tx, entities);
    }
    return super.updateBatch(tx, entities);
  }

  private static void validateUrls(final VirtualRepositoryConfig entity) {
    validateUrl(UPSTREAM_URL_FIELD, entity.getUpstreamUrl());
    validateUrl(PACKAGE_HOST_URL_FIELD, entity.getPackageHostUrl());
  }

  /**
   * Combines the URL structural checks with the class-level invariant that this row must belong
   * to a repository owned by a {@code manager_type = 'VIRTUAL'} manager. The invariant is stated
   * on {@link VirtualRepositoryConfig}'s Javadoc as a persistence-boundary contract; enforcing it
   * here — rather than relying on the current single caller in {@code ApiFirewallService} — keeps
   * a future batch or admin-path writer from silently attaching virtual config to a traditional
   * repository (which would then leak through any {@code getByRepositoryId} caller).
   */
  private void validateEntity(final TransactionContext tx, final VirtualRepositoryConfig entity) {
    if (Strings.isNullOrEmpty(entity.getRepositoryId())) {
      throw new BadRequestException("Repository ID is required.");
    }
    validateUrls(entity);
    assertVirtualOwner(entity.getRepositoryId(), fetchOwnerManagerTypes(tx, Set.of(entity.getRepositoryId())));
  }

  /**
   * Batch-path counterpart to {@link #validateEntity}: collects the distinct repositoryIds from
   * {@code entities} and issues one IN-clause query to fetch each owner manager_type, so an
   * N-entity batch does exactly one owner-lookup round trip instead of N sequential SELECTs. URL
   * validation still runs per entity (structural check, no DB call).
   */
  private void validateEntities(final TransactionContext tx, final List<VirtualRepositoryConfig> entities) {
    for (VirtualRepositoryConfig entity : entities) {
      if (Strings.isNullOrEmpty(entity.getRepositoryId())) {
        throw new BadRequestException("Repository ID is required.");
      }
      validateUrls(entity);
    }
    Set<String> repositoryIds = entities.stream()
        .map(VirtualRepositoryConfig::getRepositoryId)
        .collect(Collectors.toSet());
    Map<String, String> ownerManagerTypes = fetchOwnerManagerTypes(tx, repositoryIds);
    for (VirtualRepositoryConfig entity : entities) {
      assertVirtualOwner(entity.getRepositoryId(), ownerManagerTypes);
    }
  }

  private Map<String, String> fetchOwnerManagerTypes(final TransactionContext tx, final Set<String> repositoryIds) {
    if (repositoryIds.isEmpty()) {
      return Map.of();
    }
    List<Map.Entry<String, String>> rows = getListWithSqlInClause(
        repositoryIds,
        chunk -> tx.dsl()
            .select(REPOSITORY.REPOSITORY_ID, REPOSITORY_MANAGER.MANAGER_TYPE)
            .from(REPOSITORY)
            .join(REPOSITORY_MANAGER)
            .on(REPOSITORY.REPOSITORY_MANAGER_ID.eq(REPOSITORY_MANAGER.REPOSITORY_MANAGER_ID))
            .where(REPOSITORY.REPOSITORY_ID.in(chunk))
            .fetch()
            .stream()
            .map(r -> Map.entry(r.value1(), r.value2()))
            .toList(),
        getDataStore());
    Map<String, String> map = new HashMap<>();
    rows.forEach(e -> map.put(e.getKey(), e.getValue()));
    return map;
  }

  private static void assertVirtualOwner(final String repositoryId, final Map<String, String> ownerManagerTypes) {
    if (!ownerManagerTypes.containsKey(repositoryId)) {
      throw new BadRequestException("Repository " + repositoryId + " does not exist.");
    }
    if (!ManagerType.VIRTUAL.name().equals(ownerManagerTypes.get(repositoryId))) {
      throw new BadRequestException(
          "Virtual repository configuration requires a repository owned by a virtual repository manager.");
    }
  }

  /**
   * Cheap structural checks on user-supplied URL fields at the persistence boundary: enforce the
   * varchar(2048) column limit, require http/https, require a non-empty host, and reject embedded
   * credentials (which would land in DB dumps and support zips in cleartext — see
   * {@code proxy_server_configuration} for the pattern of dedicated username/encrypted-password
   * columns when authenticated upstreams are needed). A {@code null} URL is treated as a no-op —
   * only non-null user-supplied values are checked.
   *
   * <p>
   * This validator is deliberately network-free. Outbound-boundary SSRF enforcement (address
   * classification, DNS-rebinding protection) is owned by FIRE-664, which sees the resolved
   * connection and can pin it. Doing address classification here would (a) add unbounded DNS I/O
   * inside the DAO transaction, (b) still be defeated by a DNS record flip between persistence
   * and the outbound request, and (c) reject legitimate internal upstreams (e.g. an on-prem
   * Nexus Repository at an RFC1918 address) with no override — a false-positive class every
   * sibling admin-configurable URL feature in this codebase (webhooks, JIRA, SCM, mail, reverse
   * proxy) declines to introduce.
   */
  static void validateUrl(final String fieldName, final String url) {
    if (url == null) {
      return;
    }
    if (url.length() > MAX_URL_LENGTH) {
      throw new BadRequestException(fieldName + " must be " + MAX_URL_LENGTH + " characters or fewer.");
    }
    URI parsed;
    try {
      parsed = new URI(url);
    }
    catch (URISyntaxException e) {
      throw new BadRequestException(fieldName + " is not a valid URL.", e);
    }
    String scheme = parsed.getScheme();
    if (scheme == null || !ALLOWED_URL_SCHEMES.contains(scheme.toLowerCase())) {
      throw new BadRequestException(fieldName + " must use http or https.");
    }
    if (Strings.isNullOrEmpty(parsed.getHost())) {
      throw new BadRequestException(fieldName + " must include a host.");
    }
    if (parsed.getRawUserInfo() != null) {
      throw new BadRequestException(fieldName + " must not embed credentials.");
    }
  }

  @Override
  public Class<VirtualRepositoryConfig> getEntityClass() {
    return VirtualRepositoryConfig.class;
  }
}
