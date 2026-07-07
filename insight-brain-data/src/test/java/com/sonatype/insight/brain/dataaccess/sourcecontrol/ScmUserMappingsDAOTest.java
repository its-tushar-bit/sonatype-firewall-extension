/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.sourcecontrol;

import java.util.List;
import java.util.Map.Entry;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.sourcecontrol.ScmUserMappings;
import com.sonatype.insight.brain.utils.ScmUserMappingsBuilder;
import com.sonatype.insight.json.store.JsonUtils;

import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.utils.ScmUserMappingsHelper.getRandomMappings;
import static org.assertj.core.api.Assertions.assertThat;

public class ScmUserMappingsDAOTest
    extends AbstractDbDAOTest
{
  private ScmUserMappingsDAO scmUserMappingsDAO;

  @Before
  @Override
  public void setup() {
    super.setup();
    scmUserMappingsDAO = daoFactory.createScmUserMappingsDAO();
  }

  @Test
  public void testGetByOwnerIdWithHierarchy_returnsClosestAncestor() {
    ScmUserMappings orgMappings =
        tempEntity.createScmUserMappings(organization.getId(), getRandomMappings());
    tempEntity.createScmUserMappings(organization.getParentOrganizationId(), getRandomMappings());

    ScmUserMappings result = scmUserMappingsDAO.getByOwnerIdWithHierarchy(application.getId());

    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(orgMappings.getId());
  }

  @Test
  public void testGetByOwnerIdWithHierarchy_prefersSelfOrganizationOverAncestor() {
    ScmUserMappings ownMappings =
        tempEntity.createScmUserMappings(organization.getId(), getRandomMappings());
    tempEntity.createScmUserMappings(organization.getParentOrganizationId(), getRandomMappings());

    ScmUserMappings result = scmUserMappingsDAO.getByOwnerIdWithHierarchy(organization.getId());

    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(ownMappings.getId());
  }

  @Test
  public void testGetByOwnerIdWithHierarchy_returnsNullWhenNone() {
    ScmUserMappings result = scmUserMappingsDAO.getByOwnerIdWithHierarchy(application.getId());

    assertThat(result).isNull();
  }

  @Test
  public void testGetByOrganizationId() {
    ScmUserMappings existingScmUserMappings = tempEntity.createScmUserMappings(organization.getId(),
        getRandomMappings());

    ScmUserMappings scmUserMappingsFromDB = scmUserMappingsDAO.getByOrganizationId(organization.getId());

    assertThat(scmUserMappingsFromDB.getId()).isEqualTo(existingScmUserMappings.getId());
    assertThat(scmUserMappingsFromDB.getRoleId()).isNull();
    assertThat(scmUserMappingsFromDB.getOrganizationId()).isEqualTo(existingScmUserMappings.getOrganizationId());
    assertThat(scmUserMappingsFromDB.getMappingsJson()).isEqualTo(existingScmUserMappings.getMappingsJson());
  }

  @Test
  public void testInsert() {
    List<Entry<String, String>> mappings = getRandomMappings();

    ScmUserMappings scmUserMappings = new ScmUserMappingsBuilder()
        .withId()
        .withRoleId(Role.DEVELOPER_ROLE_ID)
        .withMappings(mappings)
        .withOrganizationId(organization.getId())
        .build();

    scmUserMappingsDAO.addOrUpdate(scmUserMappings);

    List<ScmUserMappings> scmUserMappingsFromDB = scmUserMappingsDAO.getAll();

    assertThat(scmUserMappingsFromDB).hasSize(1);
    assertThat(scmUserMappingsFromDB.get(0).getId()).isEqualTo(scmUserMappings.getId());
    assertThat(scmUserMappingsFromDB.get(0).getRoleId()).isEqualTo(scmUserMappings.getRoleId());
    assertThat(scmUserMappingsFromDB.get(0).getOrganizationId()).isEqualTo(scmUserMappings.getOrganizationId());
    assertThat(scmUserMappingsFromDB.get(0).getMappingsJson()).isEqualTo(JsonUtils.format(mappings));
  }

  @Test
  public void testUpdate() {
    ScmUserMappings existingScmUserMappings = tempEntity.createScmUserMappings(Role.OWNER_ROLE_ID, organization.getId(),
        getRandomMappings());

    List<Entry<String, String>> newMappings = getRandomMappings();
    ScmUserMappings scmUserMappings = new ScmUserMappingsBuilder()
        .withId(existingScmUserMappings.getId())
        .withMappings(newMappings)
        .withRoleId(Role.DEVELOPER_ROLE_ID)
        .withOrganizationId(organization.getId())
        .build();

    scmUserMappingsDAO.addOrUpdate(scmUserMappings);

    List<ScmUserMappings> scmUserMappingsFromDB = scmUserMappingsDAO.getAll();

    assertThat(scmUserMappingsFromDB).hasSize(1);
    assertThat(scmUserMappingsFromDB.get(0).getId()).isEqualTo(scmUserMappings.getId());
    assertThat(scmUserMappingsFromDB.get(0).getRoleId()).isEqualTo(scmUserMappings.getRoleId());
    assertThat(scmUserMappingsFromDB.get(0).getOrganizationId()).isEqualTo(scmUserMappings.getOrganizationId());
    assertThat(scmUserMappingsFromDB.get(0).getMappingsJson()).isEqualTo(JsonUtils.format(newMappings));
  }
}
