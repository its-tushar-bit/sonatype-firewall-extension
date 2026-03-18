/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

import java.util.Arrays;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.model.AggregateFile;
import com.sonatype.insight.brain.model.ApplicationComponent;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;

import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Category(SlowTest.class)
public class AggregateFileDAOTest
    extends AbstractDbDAOTest
{
  private AggregateFileDAO dao;

  @Before
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createAggregateFileDAO();
  }

  @Test
  public void testCRUD() {
    // Create
    ApplicationComponent applicationComponent = tempEntity.newApplicationComponent(application.getId(),
        BuildStageType.ID, "hash1", ComponentIdentifier.createMavenCoordinates("g", "a", "v"));
    AggregateFile aggregateFile = new AggregateFile(applicationComponent.getId(), "hash2", null);
    dao.insert(aggregateFile);

    // Read
    assertThat(dao.getById(aggregateFile.getId())).usingRecursiveComparison().isEqualTo(aggregateFile);

    // Update
    assertThatThrownBy(() -> {
      AggregateFile toUpdate = dao.getById(aggregateFile.getId());
      toUpdate.setHash("new" + toUpdate.getHash());
      dao.update(toUpdate);
    }).isInstanceOf(UnsupportedOperationException.class);

    // Delete
    dao.delete(aggregateFile);
    assertThat(dao.getById(aggregateFile.getId())).isNull();
  }

  @Test
  public void testCreate_Pathnames() {
    ApplicationComponent applicationComponent = tempEntity.newApplicationComponent(application.getId(),
        BuildStageType.ID, "hash1", ComponentIdentifier.createMavenCoordinates("g", "a", "v"));
    AggregateFile aggregateFile = new AggregateFile(applicationComponent.getId(), "hash2",
        Sets.newLinkedHashSet(Arrays.asList("pathname1", "pathname2")));

    dao.insert(aggregateFile);

    assertThat(dao.getById(aggregateFile.getId())).usingRecursiveComparison().isEqualTo(aggregateFile);
  }

  @Test
  public void testGetByApplicationComponentId() {
    ApplicationComponent applicationComponent = tempEntity.newApplicationComponent(application.getId(),
        BuildStageType.ID, "hash1", ComponentIdentifier.createMavenCoordinates("g", "a", "v"));
    ApplicationComponent otherApplicationComponent = tempEntity.newApplicationComponent(application.getId(),
        BuildStageType.ID, "hash2", ComponentIdentifier.createMavenCoordinates("g", "a", "v"));
    AggregateFile aggregateFile1 = tempEntity.newAggregateFile(applicationComponent.getId(), "hash3", null);
    AggregateFile aggregateFile2 = tempEntity.newAggregateFile(applicationComponent.getId(), "hash4",
        Sets.newLinkedHashSet(Arrays.asList("pathname1", "pathname2")));
    AggregateFile otherAggregateFile = tempEntity.newAggregateFile(otherApplicationComponent.getId(), "hash5",
        Sets.newLinkedHashSet(Arrays.asList("pathname3", "pathname4")));

    assertThat(dao.getByApplicationComponentId(applicationComponent.getId()))
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactlyInAnyOrder(aggregateFile1, aggregateFile2);
    assertThat(dao.getByApplicationComponentId(otherApplicationComponent.getId()))
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactly(otherAggregateFile);
  }
}
