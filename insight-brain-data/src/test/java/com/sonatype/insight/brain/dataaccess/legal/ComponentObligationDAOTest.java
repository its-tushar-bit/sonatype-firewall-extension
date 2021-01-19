/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.legal;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.JPA;
import com.sonatype.insight.brain.model.legal.ComponentObligation;
import com.sonatype.insight.brain.model.legal.ObligationStatus;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ComponentObligationDAOTest
    extends AbstractDbDAOTest
{
  private ComponentObligationDAO dao;

  @Before
  public void before() {
    dao = new ComponentObligationDAO();
  }

  @Test
  public void testCRUD() {
    // Create
    ComponentObligation componentObligation = new ComponentObligation(
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"), organization.getId(), "name", "comment",
        ObligationStatus.OPEN, "legalContentHash");
    dao.insert(componentObligation);
    assertThat(componentObligation.getId()).isNotNull();

    // Read
    assertThat(dao.getById(componentObligation.getId())).usingRecursiveComparison().isEqualTo(componentObligation);

    // Update
    componentObligation.setComponentIdentifier(ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2"));
    componentObligation.setOwnerId(application.getId());
    componentObligation.setObligationName(componentObligation.getObligationName() + "2");
    componentObligation.setComment(componentObligation.getComment() + "2");
    componentObligation.setStatus(ObligationStatus.FULFILLED);
    dao.update(componentObligation);
    assertThat(dao.getById(componentObligation.getId())).usingRecursiveComparison().ignoringFields(JPA.IGNORE_FIELDS)
        .isEqualTo(componentObligation);

    // Delete
    dao.delete(componentObligation);
    assertThat(dao.getById(componentObligation.getId())).isNull();
  }

  @Test
  public void testGetByOwnerId() {
    ComponentObligation componentObligation1 = tempEntity
        .newComponentObligation(ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1"), organization.getId(),
            "name1", "comment1", ObligationStatus.OPEN, "legalContentHash1");
    ComponentObligation componentObligation2 = tempEntity
        .newComponentObligation(ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2"), organization.getId(),
            "name2", "comment2", ObligationStatus.OPEN, "legalContentHash2");
    tempEntity.newComponentObligation(ComponentIdentifier.createMavenCoordinates("g3", "a3", "v3"), application.getId(),
        "name3", "comment3", ObligationStatus.OPEN, "legalContentHash3");

    assertThat(dao.getByOwnerId(organization.getId())).usingRecursiveFieldByFieldElementComparator()
        .containsExactlyInAnyOrder(componentObligation1, componentObligation2);
  }
}
