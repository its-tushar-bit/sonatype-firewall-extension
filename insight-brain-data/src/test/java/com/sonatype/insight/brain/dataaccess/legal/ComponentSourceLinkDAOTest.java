/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.legal;

import java.util.Date;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.JPA;
import com.sonatype.insight.brain.model.legal.ComponentLegalPartStatus;
import com.sonatype.insight.brain.model.legal.ComponentSourceLink;
import com.sonatype.insight.brain.model.legal.SourceLinkOverride;
import com.sonatype.insight.error.exception.BadRequestException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class ComponentSourceLinkDAOTest
    extends AbstractDbDAOTest
{
  private SourceLinkOverrideDAO sourceLinkOverrideDAO;

  private ComponentSourceLinkDAO dao;

  @BeforeEach
  @Override
  public void setup() {
    super.setup();
    sourceLinkOverrideDAO = daoFactory.createSourceLinkOverrideDAO();
    dao = daoFactory.createComponentSourceLinkDAO();
  }

  @Test
  public void testCRUD() {
    // Create
    Date now = new Date();
    ComponentSourceLink componentSourceLink = new ComponentSourceLink(
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"), organization.getId(), "username");
    componentSourceLink.setLastUpdatedAt(new Date(now.getTime() - 1));
    dao.insert(componentSourceLink);
    assertThat(componentSourceLink.getId()).isNotNull();

    // Read
    assertThat(dao.getById(componentSourceLink.getId())).usingRecursiveComparison(JPA.RECURSIVE_COMPARISON_CONFIG)
        .isEqualTo(componentSourceLink);

    // Update
    componentSourceLink.setComponentIdentifier(ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2"));
    componentSourceLink.setOwnerId(application.getId());
    componentSourceLink.setLastUpdatedByUsername("other");
    componentSourceLink.setLastUpdatedAt(now);
    dao.update(componentSourceLink);
    assertThat(dao.getById(componentSourceLink.getId())).usingRecursiveComparison()
        .ignoringFields(JPA.IGNORE_FIELDS)
        .usingOverriddenEquals()
        .isEqualTo(componentSourceLink);

    // Delete
    dao.delete(componentSourceLink);
    assertThat(dao.getById(componentSourceLink.getId())).isNull();
  }

  @Test
  public void testInsert_SetsDateIfNull() {
    ComponentSourceLink componentSourceLink = new ComponentSourceLink(
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"), organization.getId(), "username");
    componentSourceLink.setLastUpdatedAt(null);
    Date now = new Date();

    dao.insert(componentSourceLink);

    assertThat(dao.getById(componentSourceLink.getId()).getLastUpdatedAt()).isAfterOrEqualTo(now);
  }

  @Test
  public void testInsert_SameOwnerAndComponent() {
    ComponentSourceLink componentSourceLink1 = new ComponentSourceLink(
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"), organization.getId(), "username1");
    dao.insert(componentSourceLink1);
    ComponentSourceLink componentSourceLink2 = new ComponentSourceLink(
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"), organization.getId(), "username2");

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> dao.insert(componentSourceLink2))
        .withMessageContaining(
            "Component source link already exists for owner with id " + componentSourceLink2.getOwnerId()
                + " and component " + componentSourceLink2.getComponentIdentifier() + ".");
  }

  @Test
  public void testUpdate_SetsDate() {
    Date now = new Date();
    ComponentSourceLink componentSourceLink = new ComponentSourceLink(
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"), organization.getId(), "username");
    componentSourceLink.setLastUpdatedAt(new Date(now.getTime() - 1));
    dao.insert(componentSourceLink);
    assertThat(dao.getById(componentSourceLink.getId()).getLastUpdatedAt()).isBefore(now);

    dao.update(componentSourceLink);

    assertThat(dao.getById(componentSourceLink.getId()).getLastUpdatedAt()).isAfterOrEqualTo(now);
  }

  @Test
  public void testUpdate_DoesNotExist() {
    ComponentSourceLink componentSourceLink =
        new ComponentSourceLink(ComponentIdentifier.createMavenCoordinates("g", "a", "v"), organization.getId(),
            "username");
    componentSourceLink.setId("doesNotExist");

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> dao.update(componentSourceLink))
        .withMessageContaining(
            "Cannot update component source link with id " + componentSourceLink.getId()
                + " because it does not exist.");
  }

  @Test
  public void testGetByOwnerIdAndComponentIdentifier() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1");
    ComponentSourceLink componentSourceLink =
        tempEntity.newComponentSourceLink(componentIdentifier, organization.getId());
    tempEntity.newComponentSourceLink(componentIdentifier.createAlternativeVersion("v2"), organization.getId());
    tempEntity.newComponentSourceLink(componentIdentifier, application.getId());

    assertThat(dao.getByOwnerIdAndComponentIdentifier(organization.getId(), componentIdentifier))
        .usingRecursiveComparison(JPA.RECURSIVE_COMPARISON_CONFIG)
        .isEqualTo(componentSourceLink);
  }

  @Test
  public void testDelete_CascadesToSourceLinkOverrides() {
    ComponentSourceLink componentSourceLink = tempEntity
        .newComponentSourceLink(ComponentIdentifier.createMavenCoordinates("g", "a", "v"), organization.getId());
    ComponentSourceLink otherComponentSourceLink = tempEntity
        .newComponentSourceLink(ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2"), organization.getId());
    SourceLinkOverride sourceLinkOverride1 =
        tempEntity.newSourceLinkOverride("content1", "originalContent1", ComponentLegalPartStatus.ENABLED,
            componentSourceLink.getId());
    SourceLinkOverride sourceLinkOverride2 =
        tempEntity.newSourceLinkOverride("content2", "originalContent1", ComponentLegalPartStatus.ENABLED,
            componentSourceLink.getId());
    SourceLinkOverride otherSourceLinkOverride =
        tempEntity.newSourceLinkOverride("content3", "originalContent1", ComponentLegalPartStatus.ENABLED,
            otherComponentSourceLink.getId());

    dao.delete(componentSourceLink);

    assertThat(dao.getById(componentSourceLink.getId())).isNull();
    assertThat(sourceLinkOverrideDAO.getById(sourceLinkOverride1.getId())).isNull();
    assertThat(sourceLinkOverrideDAO.getById(sourceLinkOverride2.getId())).isNull();
    assertThat(dao.getById(otherComponentSourceLink.getId())).isNotNull();
    assertThat(sourceLinkOverrideDAO.getById(otherSourceLinkOverride.getId())).isNotNull();
  }
}
