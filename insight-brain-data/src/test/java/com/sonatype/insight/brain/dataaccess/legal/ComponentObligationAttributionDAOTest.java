/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.legal;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.JPA;
import com.sonatype.insight.brain.model.legal.ComponentObligationAttribution;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ComponentObligationAttributionDAOTest
    extends AbstractDbDAOTest
{
  private ComponentObligationAttributionDAO dao;

  @Before
  public void before() {
    dao = new ComponentObligationAttributionDAO();
  }

  @Test
  public void testCRUD() {
    // Create
    ComponentObligationAttribution componentObligationAttribution =
        new ComponentObligationAttribution(ComponentIdentifier.createMavenCoordinates("g", "a", "v"),
            organization.getId(), "name", "content", "legalContentHash");
    dao.insert(componentObligationAttribution);
    assertThat(componentObligationAttribution.getId()).isNotNull();

    // Read
    assertThat(dao.getById(componentObligationAttribution.getId())).usingRecursiveComparison()
        .isEqualTo(componentObligationAttribution);

    // Update
    componentObligationAttribution.setComponentIdentifier(ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2"));
    componentObligationAttribution.setOwnerId(application.getId());
    componentObligationAttribution.setObligationName(componentObligationAttribution.getObligationName() + "2");
    componentObligationAttribution.setContent(componentObligationAttribution.getContent() + "2");
    dao.update(componentObligationAttribution);
    assertThat(dao.getById(componentObligationAttribution.getId())).usingRecursiveComparison()
        .ignoringFields(JPA.IGNORE_FIELDS).isEqualTo(componentObligationAttribution);

    // Delete
    dao.delete(componentObligationAttribution);
    assertThat(dao.getById(componentObligationAttribution.getId())).isNull();
  }

  @Test
  public void testGetByOwnerId() {
    ComponentObligationAttribution componentObligationAttribution1 = tempEntity
        .newComponentObligationAttribution(ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1"),
            organization.getId(), "name1", "content1", "legalContentHash1");
    ComponentObligationAttribution componentObligationAttribution2 = tempEntity
        .newComponentObligationAttribution(ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2"),
            organization.getId(), "name2", "content2", "legalContentHash2");
    tempEntity.newComponentObligationAttribution(ComponentIdentifier.createMavenCoordinates("g3", "a3", "v3"),
        application.getId(), "name3", "content3", "legalContentHash3");

    assertThat(dao.getByOwnerId(organization.getId())).usingRecursiveFieldByFieldElementComparator()
        .containsExactlyInAnyOrder(componentObligationAttribution1, componentObligationAttribution2);
  }
}
