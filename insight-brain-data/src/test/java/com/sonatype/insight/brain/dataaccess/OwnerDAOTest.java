/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.insight.brain.model.Owner;

import org.junit.Test;

import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;

public class OwnerDAOTest
    extends AbstractDbDAOTest
{
  private final OwnerDAO ownerDAO = new OwnerDAO();

  @Test
  public void testWalkHierarchy() {
    List<String> ownersIds = new ArrayList<>();
    for (Owner owner : ownerDAO.walkHierarchy(applicationId)) {
      ownersIds.add(owner.getId());
    }
    assertThat(ownersIds, contains(applicationId, organization.getId(), organization.getParentOrganizationId()));
  }
}
