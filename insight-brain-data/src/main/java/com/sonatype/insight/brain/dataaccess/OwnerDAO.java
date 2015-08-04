/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.dataaccess.TransactionContext;

public class OwnerDAO
{
  private static ApplicationDAO appDAO = new ApplicationDAO();

  private static OrganizationDAO orgDAO = new OrganizationDAO();

  public Owner getById(TransactionContext tx, String id) {
    // Since on any path in the hierarchy there are more orgs than apps, query for org first.
    Organization org = orgDAO.getById(tx, id);
    if (org != null) {
      return org;
    }

    return appDAO.getById(tx, id);
  }

  public Owner getById(String id) {
    try (TransactionContext tx = appDAO.createTransactionContext()) {
      return getById(tx, id);
    }
  }

  public List<Owner> getChildOwners(final Owner owner) {
    try (TransactionContext tx = appDAO.createTransactionContext()) {
      return getChildOwners(tx, owner);
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

  public Iterable<Owner> walkHierarchy(final String ownerId) {
    return new OwnerIterator(ownerId);
  }

  private class OwnerIterator
      implements Iterator<Owner>, Iterable<Owner>
  {
    private String nextOwnerId;

    private Owner nextOwner;

    public OwnerIterator(final String startOwnerId) {
      nextOwnerId = startOwnerId;
    }

    @Override
    public Iterator<Owner> iterator() {
      return new OwnerIterator(nextOwnerId);
    }

    @Override
    public boolean hasNext() {
      if (nextOwner == null) {
        if (nextOwnerId != null) {
          nextOwner = getById(nextOwnerId);
          nextOwnerId = null;
        }
      }
      return nextOwner != null;
    }

    @Override
    public Owner next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      Owner current = nextOwner;
      nextOwnerId = nextOwner.getParentOrganizationId();
      nextOwner = null;
      return current;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }
  }
}
