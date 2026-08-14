/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.license;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.NameableDAOTest;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.legal.ComponentObligationDAO;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.legal.ComponentObligation;
import com.sonatype.insight.brain.model.legal.ObligationStatus;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * PostgreSQL-backed tests relocated from {@link LicenseThreatGroupDAOTest} (CLM-45228). The H2/unit coverage
 * stays in that origin class; the {@code @PostgresTest} coverage lives here so this module keeps a single
 * (Postgres) DatabaseRule fixture type per JVM. The inherited {@link NameableDAOTest} tests also run here on
 * Postgres, providing intended dual coverage.
 */
@PostgresTest
public class LicenseThreatGroupDAOPgTest
    extends NameableDAOTest<LicenseThreatGroup>
{
  private static final Logger log = LoggerFactory.getLogger(LicenseThreatGroupDAOPgTest.class);

  private OrganizationDAO organizationDAO;

  private LicenseThreatGroupDAO licenseThreatGroupDAO;

  private LicenseThreatGroupLicenseDAO licenseThreatGroupLicenseDAO;

  private LicenseDAO licenseDAO;

  @Before
  @BeforeEach
  @Override
  public void setup() {
    super.setup();
    organizationDAO = daoFactory.createOrganizationDAO();
    licenseThreatGroupDAO = daoFactory.createLicenseThreatGroupDAO();
    licenseThreatGroupLicenseDAO = daoFactory.createLicenseThreatGroupLicenseDAO();
    licenseDAO = daoFactory.createLicenseDAO();
  }

  @Override
  protected LicenseThreatGroup createNameable(String a) {
    return tempEntity.newLicenseThreatGroup(organization.getId(), a, 4);
  }

  @Override
  protected AbstractOperationalSqlDAO<LicenseThreatGroup> getDao() {
    return licenseThreatGroupDAO;
  }

  @Override
  protected int getMaxNameLength() {
    return NameHelper.MAX_NAME_LENGTH;
  }

  @Override
  protected LicenseThreatGroup getEntityByName(String name) {
    return licenseThreatGroupDAO.getByOwnerIdAndName(organization.getId(), name);
  }

  private void testCRUD(String ownerId) {
    // Create
    LicenseThreatGroup group = new LicenseThreatGroup();
    group.setOwnerId(ownerId);
    group.setName("My group");
    group.setThreatLevel(4);
    licenseThreatGroupDAO.insert(group);
    assertThat(group.getId()).isNotNull();

    group = licenseThreatGroupDAO.getById(group.getId());
    assertThat(group).isNotNull();
    assertLicenseThreatGroup(ownerId, "My group", 4, group);

    // Update
    group.setName("My updated name");
    licenseThreatGroupDAO.update(group);

    group = licenseThreatGroupDAO.getById(group.getId());
    assertThat(group).isNotNull();
    assertLicenseThreatGroup(ownerId, "My updated name", 4, group);

    // Delete
    licenseThreatGroupDAO.delete(group);

    group = licenseThreatGroupDAO.getById(group.getId());
    assertThat(group).isNull();
  }

  private void assertLicenseThreatGroup(String applicationId, String name, int threatLevel, LicenseThreatGroup actual) {
    assertThat(actual.getOwnerId()).isEqualTo(applicationId);
    assertThat(actual.getName()).isEqualTo(name);
    assertThat(actual.getThreatLevel()).isEqualTo(threatLevel);
  }

  @Test
  @Override
  public void testInsert_DuplicateName() {
    createNameable("testFilterName");
    assertThatThrownBy(() -> createNameable("testFilterName")).isInstanceOf(InvalidLicenseThreatGroupException.class)
        .hasMessageContaining("A license threat group with the same name already exists.");
  }

  @Test
  @Override
  public void testUpdate_DuplicateName() {
    createNameable("testDuplicateName");
    LicenseThreatGroup nameable1 = createNameable("testDuplicateName1");

    nameable1.setName("Test Duplicate Name");
    assertThatThrownBy(() -> getDao().update(nameable1)).isInstanceOf(InvalidLicenseThreatGroupException.class)
        .hasMessageContaining("A license threat group with the same name already exists.");
  }

  private void assertUpdateLicenseThreatGroupWithDuplicateName(
      final String ownerId,
      final LicenseThreatGroup group,
      final String groupName,
      final Owner expectedOwner)
  {
    // Update without changing the name
    group.setThreatLevel(6);
    licenseThreatGroupDAO.update(group);
    assertLicenseThreatGroup(ownerId, group.getName(), 6, group);

    // Update the group with a case-/whitespace-equivalent name
    group.setName(groupName.replaceAll("\\s", "").toLowerCase(Locale.ENGLISH));
    assertThatThrownBy(() -> licenseThreatGroupDAO.update(group)).isInstanceOf(InvalidLicenseThreatGroupException.class)
        .hasMessage("A license threat group with the same name already exists for the " + expectedOwner.getType() + " '"
            + expectedOwner.getName() + "'.");
  }

  private void assertInsertLicenseThreatGroupWithDuplicateName(
      final String ownerId,
      final String groupName,
      final Owner expectedOwner)
  {
    // Add a group with a case-/whitespace-equivalent name
    LicenseThreatGroup group = newLicenseThreatGroup(ownerId,
        groupName.replaceAll("\\s", "").toLowerCase(Locale.ENGLISH));
    assertThatThrownBy(() -> licenseThreatGroupDAO.insert(group)).isInstanceOf(InvalidLicenseThreatGroupException.class)
        .hasMessage("A license threat group with the same name already exists for the " + expectedOwner.getType() + " '"
            + expectedOwner.getName() + "'.");
  }

  private LicenseThreatGroup newLicenseThreatGroup(final String ownerId, final String groupName) {
    LicenseThreatGroup group = new LicenseThreatGroup();
    group.setOwnerId(ownerId);
    group.setName(groupName);
    group.setThreatLevel(5);
    return group;
  }

  private ComponentIdentifier seedMatchingComponent(Application app, String hash, String licenseId) {
    ComponentIdentifier identifier =
        ComponentIdentifier.createMavenCoordinates("g", "a-" + hash, "1.0.0");
    tempEntity.newApplicationComponent(app.getId(), BuildStageType.ID, hash, identifier);
    String acId = appComponentDAO().getByOwnerIdAndStageTypeIdAndHash(app.getId(), BuildStageType.ID, hash)
        .getId();
    tempEntity.newApplicationComponentLicense(acId, licenseId);
    return identifier;
  }

  private com.sonatype.insight.brain.dataaccess.OwnerComponentDAO appComponentDAO() {
    return daoFactory.createOwnerComponentDAO();
  }

  /**
   * Scale guard for CLM-41470: the previous implementation shipped every distinct candidate component identifier back
   * to PostgreSQL in a row-value {@code (format, coords) IN ((?, ?), ...)} clause, which overflowed the parser's
   * recursion-depth stack ("stack depth limit exceeded") at tens of thousands of tuples. This test seeds
   * {@code scaleComponentCount} candidate components and asserts the single candidate/obligation LEFT JOIN resolves
   * all of their
   * obligations in a single round-trip within a generous wall-clock ceiling — a smoke ceiling to catch a catastrophic
   * plan regression, NOT a tight SLA (per CLAUDE.md section 6). PostgreSQL-only because the parser-depth overflow it
   * guards against is PostgreSQL-specific.
   */
  @Test
  public void testGetCandidateComponentObligationsByOwner_scalesToTensOfThousands_Postgres() {
    // The historical row-value (format, coords) IN-list overflowed PostgreSQL's parser recursion stack in the tens of
    // thousands of tuples, so this many candidates is enough to have tripped the old failure mode while keeping the
    // seeding cost (which dominates the runtime) modest. The new LEFT JOIN never builds that IN-list at any scale.
    final int scaleComponentCount = 50_000;
    final int insertBatchSize = 5_000;
    final long wallClockCeilingMillis = 60_000L;

    tempEntity.newLicenseThreatGroup(organization.getId(), "Banned", 10, "GPL-2.0");

    // Seed candidate components (application_component + application_component_license) and a ROOT obligation each.
    List<ComponentObligation> obligationBatch = new ArrayList<>(insertBatchSize);
    Date now = new Date();
    ComponentObligationDAO componentObligationDAO = daoFactory.createComponentObligationDAO();
    for (int i = 0; i < scaleComponentCount; i++) {
      String hash = "h-scale-" + i;
      ComponentIdentifier ci = seedMatchingComponent(application, hash, "GPL-2.0");
      ComponentObligation obligation = new ComponentObligation(ci, Organization.ROOT_ORGANIZATION_ID, "NOTICE",
          "scale notice " + i, ObligationStatus.OPEN, "hash-scale-" + i, "username");
      obligation.setLastUpdatedAt(now);
      obligationBatch.add(obligation);
      if (obligationBatch.size() == insertBatchSize) {
        try (TransactionContext tx = componentObligationDAO.createTransactionContext()) {
          componentObligationDAO.insertBatch(tx, obligationBatch, false);
          tx.commit();
        }
        obligationBatch.clear();
      }
    }
    if (!obligationBatch.isEmpty()) {
      try (TransactionContext tx = componentObligationDAO.createTransactionContext()) {
        componentObligationDAO.insertBatch(tx, obligationBatch, false);
        tx.commit();
      }
    }

    long startNanos = System.nanoTime();
    LicenseThreatGroupDAO.CandidateComponentObligations result;
    try (TransactionContext tx = licenseThreatGroupDAO.createTransactionContext()) {
      result = licenseThreatGroupDAO.getCandidatesWithObligationsByOwner(tx, OwnerType.APPLICATION,
          application.getId());
    }
    long elapsedMillis = (System.nanoTime() - startNanos) / 1_000_000L;
    log.info("[CLM-41470 perf] candidate/obligation LEFT JOIN over {} candidate components took {} ms",
        scaleComponentCount, elapsedMillis);

    assertThat(result.obligationsByComponent()).hasSize(scaleComponentCount);
    // candidates() holds one entry per distinct (ltgId, appId, hash, format, coords, licenseId) tuple. It equals the
    // component count here only because each component is seeded into exactly one LTG with exactly one license (no
    // LTG x license fanout); a component matching multiple LTGs/licenses would yield more candidate rows.
    assertThat(result.candidates()).hasSize(scaleComponentCount);
    assertThat(elapsedMillis).isLessThan(wallClockCeilingMillis);
  }
}
