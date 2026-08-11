/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.hosted;

import java.util.Date;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.AbstractDataTest;
import com.sonatype.insight.brain.dataaccess.OwnerComponentDAO;
import com.sonatype.insight.brain.dataaccess.repository.HostedRepositoryComponentDAO;
import com.sonatype.insight.brain.model.OwnerComponent;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.repository.HostedRepositoryComponent;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.dataaccess.TransactionContext;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class HostedRepositoryComponentResolverTest
    extends AbstractDataTest
{
  private HostedRepositoryComponentDAO hostedRepositoryComponentDAO;

  private OwnerComponentDAO ownerComponentDAO;

  private MeterRegistry meterRegistry;

  private HostedRepositoryComponentResolver resolver;

  @Before
  public void setup() {
    hostedRepositoryComponentDAO = daoFactory.createHostedRepositoryComponentDAO();
    ownerComponentDAO = daoFactory.createOwnerComponentDAO();
    meterRegistry = new SimpleMeterRegistry();
    resolver = new HostedRepositoryComponentResolver(
        hostedRepositoryComponentDAO, ownerComponentDAO, meterRegistry);
  }

  @Test
  public void getOrCreate_insertsRowWhenAbsent() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager);

    HostedRepositoryComponent hrc = resolver.getOrCreate(
        repository.getId(), "acme-lib-1.0.0.tgz", "sha1-abc", "nxrm-component-id-42");

    assertThat(hrc).isNotNull();
    assertThat(hrc.getId()).isNotBlank();
    assertThat(hrc.getRepositoryId()).isEqualTo(repository.getId());
    assertThat(hrc.getPathname()).isEqualTo("acme-lib-1.0.0.tgz");
    assertThat(hrc.getHash()).isEqualTo("sha1-abc");
    assertThat(hrc.getComponentId()).isEqualTo("nxrm-component-id-42");

    try (TransactionContext tx = hostedRepositoryComponentDAO.createTransactionContext()) {
      HostedRepositoryComponent persisted =
          hostedRepositoryComponentDAO.getByRepositoryIdAndPathname(
              tx, repository.getId(), "acme-lib-1.0.0.tgz");
      assertThat(persisted).isNotNull();
      assertThat(persisted.getId()).isEqualTo(hrc.getId());
    }
  }

  @Test
  public void getOrCreate_returnsExistingRowWhenPresent() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager);
    HostedRepositoryComponent seeded = tempEntity.newHostedRepositoryComponent(repository);
    String seededId = seeded.getId();

    HostedRepositoryComponent got = resolver.getOrCreate(
        repository.getId(), seeded.getPathname(), seeded.getHash(), null);

    assertThat(got.getId()).isEqualTo(seededId);
  }

  @Test
  public void getOrCreate_updatesHashAndComponentIdWhenChanged() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager);
    HostedRepositoryComponent seeded = tempEntity.newHostedRepositoryComponent(repository);
    String seededId = seeded.getId();

    HostedRepositoryComponent got = resolver.getOrCreate(
        repository.getId(), seeded.getPathname(), "sha1-NEWHASH", "nxrm-new");

    assertThat(got.getId()).isEqualTo(seededId);
    assertThat(got.getHash()).isEqualTo("sha1-NEWHASH");
    assertThat(got.getComponentId()).isEqualTo("nxrm-new");
  }

  @Test
  public void getOrCreate_preservesRowWhenHashUnchangedAndComponentIdNull() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager);
    HostedRepositoryComponent seeded = tempEntity.newHostedRepositoryComponent(repository);
    seeded.setComponentId("keep-me");
    try (TransactionContext tx = hostedRepositoryComponentDAO.createTransactionContext()) {
      tx.begin();
      hostedRepositoryComponentDAO.update(tx, seeded);
      tx.commit();
    }

    HostedRepositoryComponent got = resolver.getOrCreate(
        repository.getId(), seeded.getPathname(), seeded.getHash(), null);

    assertThat(got.getComponentId()).isEqualTo("keep-me");
  }

  @Test
  public void pinOwnerComponent_setsOwnerComponentIdWhenMatchExists() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager);
    HostedRepositoryComponent hrc = tempEntity.newHostedRepositoryComponent(repository);

    OwnerComponent oc = new OwnerComponent(hrc.getId(), BuildStageType.ID,
        new Date(), hrc.getHash(),
        ComponentIdentifier.createMavenCoordinates("g", "a", "1.0"),
        MatchState.EXACT.getId(),
        IdentificationSource.SONATYPE.getId(), false, null);
    ownerComponentDAO.insert(oc);

    resolver.pinOwnerComponent(hrc, "scan-1", BuildStageType.ID);

    HostedRepositoryComponent reloaded = hostedRepositoryComponentDAO.getById(hrc.getId());
    assertThat(reloaded.getOwnerComponentId()).isEqualTo(oc.getId());
  }

  /**
   * {@code pinOwnerComponent} receives an {@link HostedRepositoryComponent} its caller read earlier
   * and held across an HDS upload plus a full policy evaluation, so by the time the pin runs another
   * writer may have updated the row. Only {@code owner_component_id} is the pin's business; every
   * other column must keep whatever is in the database.
   * <p>
   * {@code component_id} is the column that matters most here — it is the NXRM id that
   * component-keyed waivers and quarantine join on, so silently reverting it to a stale value stops
   * those matching with no exception, log, or metric.
   */
  @Test
  public void pinOwnerComponent_doesNotRevertConcurrentUpdatesToOtherColumns() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager);
    HostedRepositoryComponent hrc = tempEntity.newHostedRepositoryComponent(repository);

    OwnerComponent oc = new OwnerComponent(hrc.getId(), BuildStageType.ID,
        new Date(), hrc.getHash(),
        ComponentIdentifier.createMavenCoordinates("g", "a", "1.0"),
        MatchState.EXACT.getId(),
        IdentificationSource.SONATYPE.getId(), false, null);
    ownerComponentDAO.insert(oc);

    // A concurrent writer — a hosted upload calling getOrCreate with an NXRM component id — stamps
    // component_id after the caller above read its copy of the row.
    HostedRepositoryComponent concurrent = hostedRepositoryComponentDAO.getById(hrc.getId());
    concurrent.setComponentId("nxrm-id-from-concurrent-upload");
    try (TransactionContext tx = hostedRepositoryComponentDAO.createTransactionContext()) {
      tx.begin();
      hostedRepositoryComponentDAO.update(tx, concurrent);
      tx.commit();
    }

    // hrc is now stale: it still carries component_id == null from before the concurrent write.
    assertThat(hrc.getComponentId()).as("precondition: the caller's copy predates the concurrent write").isNull();

    resolver.pinOwnerComponent(hrc, "scan-1", BuildStageType.ID);

    HostedRepositoryComponent reloaded = hostedRepositoryComponentDAO.getById(hrc.getId());
    assertThat(reloaded.getOwnerComponentId())
        .as("the pin still does its own job")
        .isEqualTo(oc.getId());
    assertThat(reloaded.getComponentId())
        .as("component_id written by the concurrent upload survives the pin")
        .isEqualTo("nxrm-id-from-concurrent-upload");
  }

  /**
   * The path-1 update writes only the columns the resolver owns. A pin that commits between the read
   * and the update must not be reverted, for the same reason as
   * {@link #pinOwnerComponent_doesNotRevertConcurrentUpdatesToOtherColumns}.
   */
  @Test
  public void getOrCreate_updateDoesNotRevertConcurrentOwnerComponentPin() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager);
    HostedRepositoryComponent seeded = tempEntity.newHostedRepositoryComponent(repository);

    OwnerComponent oc = new OwnerComponent(seeded.getId(), BuildStageType.ID,
        new Date(), seeded.getHash(),
        ComponentIdentifier.createMavenCoordinates("g", "a", "1.0"),
        MatchState.EXACT.getId(),
        IdentificationSource.SONATYPE.getId(), false, null);
    ownerComponentDAO.insert(oc);
    try (TransactionContext tx = hostedRepositoryComponentDAO.createTransactionContext()) {
      tx.begin();
      hostedRepositoryComponentDAO.updateOwnerComponentId(tx, seeded.getId(), oc.getId());
      tx.commit();
    }

    // Same hash, fresh NXRM id: dirty via componentId only, so the pin must survive.
    resolver.getOrCreate(repository.getId(), seeded.getPathname(), seeded.getHash(), "nxrm-id-new");

    HostedRepositoryComponent reloaded = hostedRepositoryComponentDAO.getById(seeded.getId());
    assertThat(reloaded.getComponentId()).isEqualTo("nxrm-id-new");
    assertThat(reloaded.getOwnerComponentId())
        .as("owner_component_id set by a concurrent pin survives a componentId-only update")
        .isEqualTo(oc.getId());
  }

  /**
   * A new hash means the artifact at this pathname was replaced, so the existing
   * {@code owner_component_id} — keyed on the previous hash — no longer describes it.
   */
  @Test
  public void getOrCreate_clearsOwnerComponentIdWhenHashChanges() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager);
    HostedRepositoryComponent seeded = tempEntity.newHostedRepositoryComponent(repository);

    OwnerComponent oc = new OwnerComponent(seeded.getId(), BuildStageType.ID,
        new Date(), seeded.getHash(),
        ComponentIdentifier.createMavenCoordinates("g", "a", "1.0"),
        MatchState.EXACT.getId(),
        IdentificationSource.SONATYPE.getId(), false, null);
    ownerComponentDAO.insert(oc);
    try (TransactionContext tx = hostedRepositoryComponentDAO.createTransactionContext()) {
      tx.begin();
      hostedRepositoryComponentDAO.updateOwnerComponentId(tx, seeded.getId(), oc.getId());
      tx.commit();
    }

    HostedRepositoryComponent got =
        resolver.getOrCreate(repository.getId(), seeded.getPathname(), "sha1-brand-new", null);

    assertThat(got.getHash()).isEqualTo("sha1-brand-new");
    assertThat(got.getOwnerComponentId()).as("returned entity reflects the cleared pin").isNull();
    assertThat(hostedRepositoryComponentDAO.getById(seeded.getId()).getOwnerComponentId())
        .as("stale pin cleared in the database when the hash changed")
        .isNull();
  }

  @Test
  public void pinOwnerComponent_isNoOpWhenNoMatch() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager);
    HostedRepositoryComponent hrc = tempEntity.newHostedRepositoryComponent(repository);

    resolver.pinOwnerComponent(hrc, "scan-1", BuildStageType.ID);

    HostedRepositoryComponent reloaded = hostedRepositoryComponentDAO.getById(hrc.getId());
    assertThat(reloaded.getOwnerComponentId()).isNull();
    assertThat(meterRegistry.counter(HostedRepositoryComponentResolver.PIN_MISSED_METRIC).count())
        .isEqualTo(1.0);
  }
}
