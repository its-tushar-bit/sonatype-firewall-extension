/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.repository;

import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.JPA;
import com.sonatype.insight.brain.model.repository.ManagerType;
import com.sonatype.insight.brain.model.repository.ProtocolVersion;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.repository.VirtualRepositoryConfig;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;

import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.exception.IntegrityConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.dataaccess.repository.VirtualRepositoryConfigDAO.MAX_URL_LENGTH;
import static com.sonatype.insight.brain.dataaccess.repository.VirtualRepositoryConfigDAO.PACKAGE_HOST_URL_FIELD;
import static com.sonatype.insight.brain.dataaccess.repository.VirtualRepositoryConfigDAO.UPSTREAM_URL_FIELD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class VirtualRepositoryConfigDAOTest
    extends AbstractDbDAOTest
{
  private VirtualRepositoryConfigDAO dao;

  private RepositoryDAO repositoryDAO;

  @BeforeEach
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createVirtualRepositoryConfigDAO();
    repositoryDAO = daoFactory.createRepositoryDAO();
  }

  /**
   * Every insert/update through {@link VirtualRepositoryConfigDAO} joins back to the owning
   * {@link RepositoryManager} and rejects rows whose manager is not {@code VIRTUAL} — that is the
   * persistence-boundary invariant on {@link VirtualRepositoryConfig}. Tests that exercise a valid
   * write path therefore need a virtual-owned repository; {@code tempEntity.newRepository()} would
   * default to TRADITIONAL and hit the new guard instead.
   */
  private Repository newVirtualRepository() {
    RepositoryManager manager = tempEntity.newRepositoryManager();
    manager.setManagerType(ManagerType.VIRTUAL);
    daoFactory.createRepositoryManagerDAO().update(manager);
    return tempEntity.newRepository(manager);
  }

  @Test
  public void testCRUD() {
    Repository repository = newVirtualRepository();

    VirtualRepositoryConfig config = new VirtualRepositoryConfig(repository.getId());
    config.setProtocolVersion(ProtocolVersion.V3);
    config.setPackageHostUrl("https://files.pythonhosted.org/");
    config.setUpstreamUrl("https://pypi.org/simple/");
    dao.insert(config);
    assertThat(config.getId()).isNotNull();

    VirtualRepositoryConfig reloaded = dao.getByRepositoryId(repository.getId());
    assertThat(reloaded).usingRecursiveComparison()
        .ignoringFields(JPA.IGNORE_FIELDS)
        .isEqualTo(config);

    config.setProtocolVersion(ProtocolVersion.V2);
    config.setPackageHostUrl(null);
    dao.update(config);
    reloaded = dao.getByRepositoryId(repository.getId());
    assertThat(reloaded.getProtocolVersion()).isEqualTo(ProtocolVersion.V2);
    assertThat(reloaded.getPackageHostUrl()).isNull();

    // Nullable columns must round-trip as null (PyPI-shaped configs may omit protocolVersion,
    // NuGet-shaped configs may omit packageHostUrl). The schema allows it, so the mapping must too.
    config.setProtocolVersion(null);
    dao.update(config);
    reloaded = dao.getByRepositoryId(repository.getId());
    assertThat(reloaded.getProtocolVersion()).isNull();

    dao.delete(config);
    assertThat(dao.getByRepositoryId(repository.getId())).isNull();
  }

  @Test
  public void testGetByRepositoryId_unknown() {
    assertThat(dao.getByRepositoryId("does-not-exist")).isNull();
  }

  @Test
  public void testInsert_oneConfigPerRepository() {
    Repository repository = newVirtualRepository();
    dao.insert(new VirtualRepositoryConfig(repository.getId()));

    assertThatExceptionOfType(IntegrityConstraintViolationException.class)
        .isThrownBy(() -> dao.insert(new VirtualRepositoryConfig(repository.getId())));
  }

  @Test
  public void testDelete_cascadesFromRepository() {
    Repository repository = newVirtualRepository();
    dao.insert(new VirtualRepositoryConfig(repository.getId()));
    assertThat(dao.getByRepositoryId(repository.getId())).isNotNull();

    repositoryDAO.delete(repository);

    assertThat(dao.getByRepositoryId(repository.getId())).isNull();
  }

  @Test
  public void testInsert_validatesUpstreamUrl_publicHttpsAccepted() {
    Repository repository = newVirtualRepository();

    VirtualRepositoryConfig config = new VirtualRepositoryConfig(repository.getId());
    config.setUpstreamUrl("https://pypi.org/simple/");
    config.setPackageHostUrl("https://files.pythonhosted.org/");

    dao.insert(config);

    assertThat(dao.getByRepositoryId(repository.getId())).isNotNull();
  }

  @Test
  public void testInsert_rejectsNonHttpScheme() {
    Repository repository = newVirtualRepository();

    VirtualRepositoryConfig config = new VirtualRepositoryConfig(repository.getId());
    config.setUpstreamUrl("file:///etc/passwd");

    assertThatThrownBy(() -> dao.insert(config)).isInstanceOf(BadRequestException.class)
        .hasMessageContaining(UPSTREAM_URL_FIELD)
        .hasMessageContaining("http or https");
  }

  @Test
  public void testInsert_rejectsInvalidPackageHostUrl() {
    // Assert via the field name so only the packageHostUrl check can produce this message —
    // the upstreamUrl above is valid, so a wildcard reject would slip past unnoticed.
    Repository repository = newVirtualRepository();

    VirtualRepositoryConfig config = new VirtualRepositoryConfig(repository.getId());
    config.setUpstreamUrl("https://pypi.org/simple/");
    config.setPackageHostUrl("ftp://files.pythonhosted.org/");

    assertThatThrownBy(() -> dao.insert(config)).isInstanceOf(BadRequestException.class)
        .hasMessageContaining(PACKAGE_HOST_URL_FIELD)
        .hasMessageContaining("http or https");
  }

  @Test
  public void testInsert_nullUrlsAreNoOp() {
    Repository repository = newVirtualRepository();

    // Both URL fields are nullable at the schema level (NuGet-shaped configs omit packageHostUrl,
    // early VRM rows may omit upstreamUrl). Null must skip validation, not throw.
    VirtualRepositoryConfig config = new VirtualRepositoryConfig(repository.getId());
    config.setProtocolVersion(ProtocolVersion.V3);

    dao.insert(config);

    VirtualRepositoryConfig reloaded = dao.getByRepositoryId(repository.getId());
    assertThat(reloaded).isNotNull();
    assertThat(reloaded.getUpstreamUrl()).isNull();
    assertThat(reloaded.getPackageHostUrl()).isNull();
  }

  @Test
  public void testUpdate_rejectsInvalidScheme() {
    Repository repository = newVirtualRepository();
    VirtualRepositoryConfig config = new VirtualRepositoryConfig(repository.getId());
    config.setUpstreamUrl("https://pypi.org/simple/");
    dao.insert(config);

    config.setUpstreamUrl("file:///etc/passwd");

    assertThatThrownBy(() -> dao.update(config)).isInstanceOf(BadRequestException.class)
        .hasMessageContaining(UPSTREAM_URL_FIELD)
        .hasMessageContaining("http or https");
  }

  @Test
  public void testValidateUrl_rejectsAllNonHttpSchemes() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> VirtualRepositoryConfigDAO.validateUrl(UPSTREAM_URL_FIELD, "file:///etc/passwd"))
        .withMessageContaining("http or https");
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> VirtualRepositoryConfigDAO.validateUrl(UPSTREAM_URL_FIELD, "ftp://example.com/"))
        .withMessageContaining("http or https");
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> VirtualRepositoryConfigDAO.validateUrl(UPSTREAM_URL_FIELD, "jar:http://example.com/x.jar!/"))
        .withMessageContaining("http or https");
  }

  @Test
  public void testValidateUrl_rejectsMissingHost() {
    // A scheme-only URL parses cleanly but has no host — must be rejected so a downstream
    // outbound-request path cannot dial an empty authority.
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> VirtualRepositoryConfigDAO.validateUrl(UPSTREAM_URL_FIELD, "https:///path"))
        .withMessageContaining("must include a host");
  }

  @Test
  public void testValidateUrl_rejectsEmbeddedCredentials() {
    // Credentials in the URL would be persisted in cleartext into a varchar column and land in
    // DB dumps and support zips. Authenticated upstreams belong in dedicated
    // username/encrypted-password columns following proxy_server_configuration.
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> VirtualRepositoryConfigDAO.validateUrl(UPSTREAM_URL_FIELD, "https://user:pass@example.com/"))
        .withMessageContaining("must not embed credentials");
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> VirtualRepositoryConfigDAO.validateUrl(UPSTREAM_URL_FIELD, "https://user@example.com/"))
        .withMessageContaining("must not embed credentials");
  }

  @Test
  public void testValidateUrl_rejectsOverLengthValue() {
    // Enforce the varchar(2048) column limit at the DAO so an over-length client value becomes a
    // clean 400 instead of a driver-level 500. Guards against CLM-38729-shape recurrences.
    String longUrl = "https://example.com/" + "a".repeat(MAX_URL_LENGTH);
    assertThat(longUrl.length()).isGreaterThan(MAX_URL_LENGTH);
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> VirtualRepositoryConfigDAO.validateUrl(UPSTREAM_URL_FIELD, longUrl))
        .withMessageContaining(String.valueOf(MAX_URL_LENGTH))
        .withMessageContaining("characters or fewer");
  }

  @Test
  public void testValidateUrl_rejectsMalformedUrl() {
    // URISyntaxException must map to BadRequestException, not propagate.
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> VirtualRepositoryConfigDAO.validateUrl(UPSTREAM_URL_FIELD, "http://exa mple.com/"))
        .withMessageContaining("not a valid URL");
  }

  @Test
  public void testValidateUrl_fieldNameIsIncludedInError() {
    // Callers with two URL columns need to know which one was rejected. Field-agnostic messages
    // (which the deny-list previously produced) forced ambiguous surfacing at the REST layer.
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> VirtualRepositoryConfigDAO.validateUrl(PACKAGE_HOST_URL_FIELD, "ftp://example.com/"))
        .withMessageStartingWith(PACKAGE_HOST_URL_FIELD);
  }

  @Test
  public void testValidateUrl_acceptsPublicHttpAndHttps() {
    VirtualRepositoryConfigDAO.validateUrl(UPSTREAM_URL_FIELD, "https://repo1.maven.org/maven2/");
    VirtualRepositoryConfigDAO.validateUrl(UPSTREAM_URL_FIELD, "http://example.com/");
    // Ports and paths must not affect acceptance — authority parsing is jdk-level, but pin it so
    // a future refactor that hand-parses "host:port" can't slip past.
    VirtualRepositoryConfigDAO.validateUrl(UPSTREAM_URL_FIELD, "https://example.com:8443/repo/");
  }

  @Test
  public void testValidateUrl_nullIsNoOp() {
    // Both URL fields are nullable at the schema level; a null value must skip validation
    // silently rather than throw. Exercised by testInsert_nullUrlsAreNoOp at the DAO level too.
    VirtualRepositoryConfigDAO.validateUrl(UPSTREAM_URL_FIELD, null);
    VirtualRepositoryConfigDAO.validateUrl(PACKAGE_HOST_URL_FIELD, null);
  }

  @Test
  public void testInsertBatch_validatesEveryEntityBeforePersisting() {
    // On PostgreSQL the parent DAO's batch path uses a bulk jOOQ INSERT that never routes
    // through the per-entity insert() override, so validateUrl must be re-applied by the batch
    // override. Pin that behavior: one bad entity in the middle of the batch must reject the
    // whole batch, and none of the rows must persist. On H2 the parent falls back to per-entity
    // insert() where the override runs — same observable outcome via a different code path.
    Repository firstRepository = newVirtualRepository();
    Repository secondRepository = newVirtualRepository();

    VirtualRepositoryConfig valid = new VirtualRepositoryConfig(firstRepository.getId());
    valid.setUpstreamUrl("https://pypi.org/simple/");

    VirtualRepositoryConfig invalid = new VirtualRepositoryConfig(secondRepository.getId());
    invalid.setUpstreamUrl("ftp://example.com/");

    assertThatThrownBy(() -> dao.insertBatch(List.of(valid, invalid), false))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("http or https");
    assertThat(dao.getByRepositoryId(firstRepository.getId())).isNull();
    assertThat(dao.getByRepositoryId(secondRepository.getId())).isNull();
  }

  @Test
  public void testInsertBatch_acceptsValidUrls() {
    Repository firstRepository = newVirtualRepository();
    Repository secondRepository = newVirtualRepository();

    VirtualRepositoryConfig first = new VirtualRepositoryConfig(firstRepository.getId());
    first.setUpstreamUrl("https://pypi.org/simple/");
    VirtualRepositoryConfig second = new VirtualRepositoryConfig(secondRepository.getId());
    second.setUpstreamUrl("https://repo1.maven.org/maven2/");

    dao.insertBatch(List.of(first, second), false);

    assertThat(dao.getByRepositoryId(firstRepository.getId())).isNotNull();
    assertThat(dao.getByRepositoryId(secondRepository.getId())).isNotNull();
  }

  @Test
  public void testInsertBatch_validatesPackageHostUrl() {
    // Ensure the batch override runs validateUrl on packageHostUrl too, not just upstreamUrl.
    // Field-name assertion pins that packageHostUrl is the field that threw — a bare scheme-only
    // assertion could pass off the upstreamUrl check.
    Repository repository = newVirtualRepository();
    VirtualRepositoryConfig config = new VirtualRepositoryConfig(repository.getId());
    config.setUpstreamUrl("https://pypi.org/simple/");
    config.setPackageHostUrl("ftp://files.pythonhosted.org/");

    assertThatThrownBy(() -> dao.insertBatch(List.of(config), false))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining(PACKAGE_HOST_URL_FIELD)
        .hasMessageContaining("http or https");
    assertThat(dao.getByRepositoryId(repository.getId())).isNull();
  }

  @Test
  public void testUpdateBatch_validatesEveryEntityBeforePersisting() {
    // Seed two valid rows, then attempt a batch update where one row has an invalid scheme.
    // The override must reject the batch and leave the stored rows unchanged.
    Repository firstRepository = newVirtualRepository();
    Repository secondRepository = newVirtualRepository();
    VirtualRepositoryConfig first = new VirtualRepositoryConfig(firstRepository.getId());
    first.setUpstreamUrl("https://pypi.org/simple/");
    VirtualRepositoryConfig second = new VirtualRepositoryConfig(secondRepository.getId());
    second.setUpstreamUrl("https://repo1.maven.org/maven2/");
    dao.insert(first);
    dao.insert(second);

    first.setUpstreamUrl("http://example.com/one");
    second.setUpstreamUrl("ftp://example.com/two");

    assertThatThrownBy(() -> dao.updateBatch(List.of(first, second)))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("http or https");
    // Both rows keep their pre-update URLs — the whole batch was rejected before any write.
    assertThat(dao.getByRepositoryId(firstRepository.getId()).getUpstreamUrl())
        .isEqualTo("https://pypi.org/simple/");
    assertThat(dao.getByRepositoryId(secondRepository.getId()).getUpstreamUrl())
        .isEqualTo("https://repo1.maven.org/maven2/");
  }

  @Test
  public void testInsert_rejectsTraditionalOwner() {
    // The class-level invariant (manager_type = 'VIRTUAL') is enforced at the DAO write path so a
    // future batch or admin-path writer can't silently attach virtual config to a traditional
    // repository. A row created via tempEntity.newRepository() defaults to a TRADITIONAL manager.
    Repository traditional = tempEntity.newRepository();
    VirtualRepositoryConfig config = new VirtualRepositoryConfig(traditional.getId());
    config.setUpstreamUrl("https://pypi.org/simple/");

    assertThatThrownBy(() -> dao.insert(config)).isInstanceOf(BadRequestException.class)
        .hasMessageContaining("virtual repository manager");
    assertThat(dao.getByRepositoryId(traditional.getId())).isNull();
  }

  @Test
  public void testInsert_rejectsUnknownRepository() {
    VirtualRepositoryConfig config = new VirtualRepositoryConfig("does-not-exist");
    config.setUpstreamUrl("https://pypi.org/simple/");

    assertThatThrownBy(() -> dao.insert(config)).isInstanceOf(BadRequestException.class)
        .hasMessageContaining("does not exist");
  }

  @Test
  public void testInsert_rejectsMissingRepositoryId() {
    // Route the missing-repositoryId case through a clean 400 with a field-named message instead
    // of a driver-level 500 that the FK would otherwise produce.
    VirtualRepositoryConfig config = new VirtualRepositoryConfig(null);
    config.setUpstreamUrl("https://pypi.org/simple/");

    assertThatThrownBy(() -> dao.insert(config)).isInstanceOf(BadRequestException.class)
        .hasMessageContaining("Repository ID is required");
  }

  @Test
  public void testUpdateBatch_acceptsValidUrls() {
    Repository firstRepository = newVirtualRepository();
    Repository secondRepository = newVirtualRepository();
    VirtualRepositoryConfig first = new VirtualRepositoryConfig(firstRepository.getId());
    first.setUpstreamUrl("https://pypi.org/simple/");
    VirtualRepositoryConfig second = new VirtualRepositoryConfig(secondRepository.getId());
    second.setUpstreamUrl("https://repo1.maven.org/maven2/");
    dao.insert(first);
    dao.insert(second);

    first.setUpstreamUrl("http://example.com/one");
    second.setUpstreamUrl("http://example.com/two");

    dao.updateBatch(List.of(first, second));

    assertThat(dao.getByRepositoryId(firstRepository.getId()).getUpstreamUrl())
        .isEqualTo("http://example.com/one");
    assertThat(dao.getByRepositoryId(secondRepository.getId()).getUpstreamUrl())
        .isEqualTo("http://example.com/two");
  }

  /**
   * DAO tests run on H2, so the H2-skip branch in {@link VirtualRepositoryConfigDAO#insertBatch}
   * is what every other batch test in this file actually exercises. This test pins the non-H2
   * branch by handing the DAO a {@link TransactionContext} whose dialect reports as PostgreSQL —
   * a bad-scheme URL then must be rejected by {@code validateEntity} before any DB work runs.
   * If a future refactor silently drops validation on the non-H2 path (or inverts the guard),
   * this test fails; the H2-covered tests above would not.
   */
  @Test
  public void insertBatch_onNonH2Dialect_validatesEachEntity() {
    TransactionContext tx = mock(TransactionContext.class);
    DSLContext dsl = mock(DSLContext.class);
    when(tx.dsl()).thenReturn(dsl);
    when(dsl.dialect()).thenReturn(SQLDialect.POSTGRES);

    VirtualRepositoryConfig invalid = new VirtualRepositoryConfig("repo-1");
    invalid.setUpstreamUrl("ftp://example.com/mirror");

    assertThatThrownBy(() -> dao.insertBatch(tx, List.of(invalid), false))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("must use http or https");
  }

  /**
   * @see #insertBatch_onNonH2Dialect_validatesEachEntity()
   */
  @Test
  public void updateBatch_onNonH2Dialect_validatesEachEntity() {
    TransactionContext tx = mock(TransactionContext.class);
    DSLContext dsl = mock(DSLContext.class);
    when(tx.dsl()).thenReturn(dsl);
    when(dsl.dialect()).thenReturn(SQLDialect.POSTGRES);

    VirtualRepositoryConfig invalid = new VirtualRepositoryConfig("repo-1");
    invalid.setUpstreamUrl("ftp://example.com/mirror");

    assertThatThrownBy(() -> dao.updateBatch(tx, List.of(invalid)))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("must use http or https");
  }
}
