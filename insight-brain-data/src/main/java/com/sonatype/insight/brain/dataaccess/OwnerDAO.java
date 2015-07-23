/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.dataaccess.TransactionContext;

public class OwnerDAO
{
  private static ApplicationDAO appDAO = new ApplicationDAO();

  private static OrganizationDAO orgDAO = new OrganizationDAO();

  public Owner getById(TransactionContext tx, String id) {
    Application app = appDAO.getById(tx, id);
    if (app != null) {
      return app;
    }

    return new OrganizationDAO().getById(tx, id);
  }

  private Owner getById(String id) {
    try (TransactionContext tx = appDAO.createTransactionContext()) {
      return getById(tx, id);
    }
  }

  public List<Owner> getChildOwners(TransactionContext tx, Owner owner) {
    if (!owner.canHaveChildren()) {
      return Collections.emptyList();
    }

    List<Owner> result = new ArrayList<>();
    List<Application> apps = appDAO.getByOrganizationId(tx, owner.getId());
    result.addAll(apps);
    List<Organization> orgs = orgDAO.getByParentOrganizationId(tx, owner.getId());
    result.addAll(orgs);

    return result;
  }

  public Owner getParentOwner(Owner owner) {
    return getById(owner.getParentOrganizationId());
  }
}
