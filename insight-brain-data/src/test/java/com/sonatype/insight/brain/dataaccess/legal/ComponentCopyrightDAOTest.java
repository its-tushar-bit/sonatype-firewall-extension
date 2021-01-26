/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.legal;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.JPA;
import com.sonatype.insight.brain.model.legal.ComponentCopyright;
import com.sonatype.insight.brain.model.legal.ComponentLegalPartStatus;
import com.sonatype.insight.brain.model.legal.CopyrightOverride;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ComponentCopyrightDAOTest
    extends AbstractDbDAOTest
{
  private ComponentCopyrightDAO dao;

  @Before
  public void before() {
    dao = new ComponentCopyrightDAO();
  }

  @Test
  public void testCRUD() {
    // Create
    ComponentCopyright componentCopyright = new ComponentCopyright(
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"), organization.getId(), "legalContentHash");
    dao.insert(componentCopyright);
    assertThat(componentCopyright.getId()).isNotNull();

    // Read
    assertThat(dao.getById(componentCopyright.getId())).usingRecursiveComparison().isEqualTo(componentCopyright);

    // Update
    componentCopyright.setComponentIdentifier(ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2"));
    componentCopyright.setOwnerId(application.getId());
    dao.update(componentCopyright);
    assertThat(dao.getById(componentCopyright.getId())).usingRecursiveComparison().ignoringFields(JPA.IGNORE_FIELDS)
        .isEqualTo(componentCopyright);

    // Delete
    dao.delete(componentCopyright);
    assertThat(dao.getById(componentCopyright.getId())).isNull();
  }

  @Test
  public void testGetByOwnerId() {
    ComponentCopyright componentCopyright1 = tempEntity.newComponentCopyright(
        ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1"), organization.getId(), "legalContentHash1");
    ComponentCopyright componentCopyright2 = tempEntity.newComponentCopyright(
        ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2"), organization.getId(), "legalContentHash2");
    tempEntity.newComponentCopyright(ComponentIdentifier.createMavenCoordinates("g3", "a3", "v3"), application.getId(),
        "legalContentHash3");

    assertThat(dao.getByOwnerId(organization.getId())).usingRecursiveFieldByFieldElementComparator()
        .containsExactlyInAnyOrder(componentCopyright1, componentCopyright2);
  }

  @Test
  public void testGetByOwnerIdAndComponentIdentifier() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1");
    ComponentCopyright componentCopyright = tempEntity.newComponentCopyright(componentIdentifier, organization.getId(),
        "legalContentHash1");
    tempEntity.newComponentCopyright(componentIdentifier.createAlternativeVersion("v2"), organization.getId(),
        "legalContentHash2");
    tempEntity.newComponentCopyright(componentIdentifier, application.getId(), "legalContentHash3");

    assertThat(dao.getByOwnerIdAndComponentIdentifier(organization.getId(), componentIdentifier))
        .usingRecursiveComparison().isEqualTo(componentCopyright);
  }

  @Test
  public void testDelete_CascadesToCopyrightOverrides() {
    ComponentCopyright componentCopyright = tempEntity.newComponentCopyright(
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"), organization.getId(), "legalContentHash");
    ComponentCopyright otherComponentCopyright = tempEntity.newComponentCopyright(
        ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2"), organization.getId(), "legalContentHash2");
    CopyrightOverride copyrightOverride1 = tempEntity.newCopyrightOverride("originalHash1", "hash1", "content1",
        ComponentLegalPartStatus.ENABLED, componentCopyright.getId());
    CopyrightOverride copyrightOverride2 = tempEntity.newCopyrightOverride("originalHash2", "hash2", "content2",
        ComponentLegalPartStatus.ENABLED, componentCopyright.getId());
    CopyrightOverride otherCopyrightOverride = tempEntity.newCopyrightOverride("originalHash3", "hash3", "content3",
        ComponentLegalPartStatus.ENABLED, otherComponentCopyright.getId());

    dao.delete(componentCopyright);

    CopyrightOverrideDAO copyrightOverrideDAO = new CopyrightOverrideDAO();
    assertThat(dao.getById(componentCopyright.getId())).isNull();
    assertThat(copyrightOverrideDAO.getById(copyrightOverride1.getId())).isNull();
    assertThat(copyrightOverrideDAO.getById(copyrightOverride2.getId())).isNull();
    assertThat(dao.getById(otherComponentCopyright.getId())).isNotNull();
    assertThat(copyrightOverrideDAO.getById(otherCopyrightOverride.getId())).isNotNull();
  }
}
