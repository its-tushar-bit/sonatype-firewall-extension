/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.dataaccess.development.prioritization;

import java.util.Arrays;
import java.util.Map;

import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionType;
import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.actions.WarnActionType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.prioritization.DevelopmentPrioritization;
import com.sonatype.insight.brain.model.prioritization.DevelopmentPrioritizationComponentInfo;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.junit.Before;
import org.junit.Test;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;

import static org.assertj.core.api.Assertions.assertThat;

public class DevelopmentPrioritizationComponentInfoDAOTest
    extends AbstractDbDAOTest
{
  private DevelopmentPrioritizationComponentInfoDAO dao;

  private DevelopmentPrioritization scan1prioritization;

  private DevelopmentPrioritization scan2prioritization;

  private DevelopmentPrioritizationComponentInfo scan1component1;

  private DevelopmentPrioritizationComponentInfo scan1component2;

  private DevelopmentPrioritizationComponentInfo scan2component1;

  private DevelopmentPrioritizationComponentInfo scan2component2;

  @Before
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createDevelopmentPrioritizationComponentInfoDAO();
    insertComponentInfoRows();
  }

  @Test
  public void testGetAllByScanId() {
    assertThat(dao.getAllByScanId("scan1"))
        .hasSize(2)
        .containsExactlyInAnyOrder(scan1component1, scan1component2);
    assertThat(dao.getAllByScanId("scan2"))
        .hasSize(2)
        .containsExactlyInAnyOrder(scan2component1, scan2component2);
    assertThat(dao.getAllByScanId("scanX"))
        .isEmpty();
  }

  @Test
  public void testGetByScanIdAndComponentHash() {
    assertThat(dao.getByScanIdAndComponentHash("scan1", "hash1"))
        .isEqualTo(scan1component1);
    assertThat(dao.getByScanIdAndComponentHash("scanX", "hash1"))
        .isNull();
    assertThat(dao.getByScanIdAndComponentHash("scan1", "hashX"))
        .isNull();
  }

  @Test
  public void testInsertBatch() {
    DevelopmentPrioritization scan3prioritization = tempEntity.newDevelopmentPrioritization("scan3");
    DevelopmentPrioritizationComponentInfo scan3component1 = new DevelopmentPrioritizationComponentInfo(
        scan3prioritization.getId(), scan3prioritization.getScanId(), "hash1",
        ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS, "1.0.5");
    DevelopmentPrioritizationComponentInfo scan3component2 = new DevelopmentPrioritizationComponentInfo(
        scan3prioritization.getId(), scan3prioritization.getScanId(), "hash2",
        ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS, "1.0.6");
    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      dao.insertBatch(tx, Arrays.asList(scan3component1, scan3component2));
      tx.commit();
    }
    assertThat(dao.getAllByScanId("scan3"))
        .hasSize(2)
        .containsExactlyInAnyOrder(scan3component1, scan3component2);
  }

  @Test
  public void testGetStageStatusesByScanIdAndComponentHash() {
    final DevelopmentPrioritization developmentPrioritization = tempEntity.newDevelopmentPrioritization("scan123");
    final DevelopmentPrioritizationComponentInfo componentInfo =
        tempEntity.newDevelopmentPrioritizationComponentInfo(developmentPrioritization.getId(),
            developmentPrioritization.getScanId(), "hash123", ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS,
            "1.0.1", "none", FailActionType.ID, "none", WarnActionType.ID);

    final Map<StageType, String> componentStatuses =
        dao.getStageStatusesByScanIdAndComponentHash(componentInfo.getScanId(), componentInfo.getComponentHash());
    final Map<StageType, String> expectedStatuses = ImmutableMap.of(
        StageTypes.SOURCE, "none",
        StageTypes.BUILD, FailActionType.ID,
        StageTypes.STAGE_RELEASE, "none",
        StageTypes.RELEASE, WarnActionType.ID
    );
    assertThat(componentStatuses)
        .hasSize(4)
        .doesNotContainKeys(StageTypes.DEVELOP, StageTypes.OPERATE, StageTypes.PROXY)
        .containsExactlyInAnyOrderEntriesOf(expectedStatuses);
  }

  @Test
  public void testGetStageStatusesByScanIdAndComponentHash_WhenComponentNotFound() {
    final Map<StageType, String> componentStatuses =
        dao.getStageStatusesByScanIdAndComponentHash("nonexistent", "nonexistent");
    assertThat(componentStatuses)
        .isEmpty();
  }

  @Test
  public void testUpdate_UpdateStageStatus() {
    assertThat(dao.getByScanIdAndComponentHash(scan1component1.getScanId(), scan1component1.getComponentHash())
        .getBuildStatus())
        .isNull();

    scan1component1.setBuildStatus(WarnActionType.ID);
    dao.update(scan1component1);

    assertThat(dao.getByScanIdAndComponentHash(scan1component1.getScanId(), scan1component1.getComponentHash())
        .getBuildStatus())
        .isEqualTo(WarnActionType.ID);
  }

  private void insertComponentInfoRows() {
    scan1prioritization = tempEntity.newDevelopmentPrioritization("scan1");
    scan2prioritization = tempEntity.newDevelopmentPrioritization("scan2");

    scan1component1 = tempEntity.newDevelopmentPrioritizationComponentInfo(
        scan1prioritization.getId(), scan1prioritization.getScanId(), "hash1",
        ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS, "1.0.1");
    scan1component2 = tempEntity.newDevelopmentPrioritizationComponentInfo(
        scan1prioritization.getId(), scan1prioritization.getScanId(), "hash2",
        ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS, "1.0.2");
    scan2component1 = tempEntity.newDevelopmentPrioritizationComponentInfo(
        scan2prioritization.getId(), scan2prioritization.getScanId(), "hash1",
        ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS, "1.0.3");
    scan2component2 = tempEntity.newDevelopmentPrioritizationComponentInfo(
        scan2prioritization.getId(), scan2prioritization.getScanId(), "hash2",
        ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS, "1.0.4");
  }
}
