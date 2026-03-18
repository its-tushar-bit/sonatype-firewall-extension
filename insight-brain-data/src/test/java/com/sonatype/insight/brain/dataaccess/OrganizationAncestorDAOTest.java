/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.util.List;
import java.util.Objects;

import com.sonatype.insight.brain.model.OrganizationAncestor;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class OrganizationAncestorDAOTest
    extends AbstractDbDAOTest
{
  private OrganizationAncestorDAO dao;

  @Before
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createOrganizationAncestorDAO();

    tempEntity.newOrganizationWithSpecificIdAndParent("ancestorId", "ancestorId", "ROOT_ORGANIZATION_ID");
    tempEntity.newOrganizationWithSpecificIdAndParent("orgId", "orgId", "ancestorId");
    tempEntity.newOrganizationWithSpecificIdAndParent("orgId2", "orgId2", "ancestorId");
  }

  @Test
  public void testGetByOrganizationId() {

    List<OrganizationAncestor> results;
    try (TransactionContext tx = dao.createTransactionContext()) {
      results = dao.getByOrganizationId(tx, "orgId");
    }

    assertThat(results).extracting("organizationId", "ancestorId", "ancestorDistance")
        .containsExactlyInAnyOrder(
            tuple("orgId", "orgId", 0),
            tuple("orgId", "ancestorId", 1),
            tuple("orgId", "ROOT_ORGANIZATION_ID", 2));

    assertThat(results).extracting("id").allMatch(Objects::nonNull);

    for (OrganizationAncestor orgAncestor : results) {
      dao.delete(orgAncestor);
    }

    try (TransactionContext tx = dao.createTransactionContext()) {
      assertThat(dao.getByOrganizationId(tx, "orgId")).isEmpty();
      assertThat(dao.getByOrganizationId(tx, "orgId2")).hasSize(3);
    }
  }
}
