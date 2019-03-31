/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import com.sonatype.insight.brain.dataaccess.configuration.DataRetentionPolicyDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseOverrideDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.SecurityVulnerabilityOverrideDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.configuration.DataRetentionPolicy;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverride;
import com.sonatype.insight.dataaccess.TransactionContext;

public class OwnerDAO
{
  private static ApplicationDAO appDAO = new ApplicationDAO();

  private static OrganizationDAO orgDAO = new OrganizationDAO();

  private static RepositoryDAO repoDAO = new RepositoryDAO();

  public Owner getById(TransactionContext tx, String id) {
    if (RepositoryContainer.REPOSITORY_CONTAINER_ID.equals(id)) {
      return RepositoryContainer.SINGLETON;
    }

    // Since on any path in the hierarchy there are more orgs than apps or repos, query for org first.
    Organization org = orgDAO.getById(tx, id);
    if (org != null) {
      return org;
    }

    Application app = appDAO.getById(tx, id);
    if (app != null) {
      return app;
    }

    return repoDAO.getById(id);
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
    List<Owner> result = new ArrayList<>();
    if (OwnerType.ORGANIZATION.equals(owner.getType())) {
      List<Application> apps = appDAO.getByOrganizationId(tx, owner.getId());
      result.addAll(apps);
      List<Organization> orgs = orgDAO.getByParentOrganizationId(tx, owner.getId());
      result.addAll(orgs);
      if (Organization.ROOT_ORGANIZATION_ID.equals(owner.getId())) {
        result.add(RepositoryContainer.SINGLETON);
      }
    }
    else if (OwnerType.REPOSITORY_CONTAINER.equals(owner.getType())) {
      result.addAll(repoDAO.getAll(tx));
    }

    return result;
  }

  public Owner getParentOwner(Owner owner) {
    return getById(owner.getParentOwnerId());
  }

  public Iterable<Owner> walkHierarchy(Owner owner) {
    return () -> new OwnerIterator(null, owner);
  }

  public Iterable<Owner> walkHierarchy(final String ownerId) {
    return () -> new OwnerIterator(null, ownerId);
  }

  public Iterable<Owner> walkHierarchy(TransactionContext tx, final String ownerId) {
    if (tx == null) {
      throw new IllegalArgumentException();
    }
    return () -> new OwnerIterator(tx, ownerId);
  }

  private class OwnerIterator
      implements Iterator<Owner>
  {
    private final TransactionContext tx;

    private String nextOwnerId;

    private Owner nextOwner;

    OwnerIterator(TransactionContext tx, final String startOwnerId) {
      this.tx = tx;
      nextOwnerId = startOwnerId;
    }

    OwnerIterator(TransactionContext tx, Owner startOwner) {
      this.tx = tx;
      nextOwner = startOwner;
    }

    @Override
    public boolean hasNext() {
      if (nextOwner == null) {
        if (nextOwnerId != null) {
          nextOwner = (tx != null) ? getById(tx, nextOwnerId) : getById(nextOwnerId);
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
      nextOwnerId = nextOwner.getParentOwnerId();
      nextOwner = null;
      return current;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }
  }

  public void cascadeDelete(TransactionContext tx, Owner owner) {
    // Cascade to policy waivers
    PolicyWaiverDAO policyWaiverDAO = new PolicyWaiverDAO();
    List<PolicyWaiver> policyWaivers = policyWaiverDAO.getByOwnerId(tx, owner.getId());
    for (PolicyWaiver policyWaiver : policyWaivers) {
      policyWaiverDAO.delete(tx, policyWaiver);
    }

    // Cascade to license overrides
    LicenseOverrideDAO licenseOverrideDAO = new LicenseOverrideDAO();
    List<LicenseOverride> licenseOverrides = licenseOverrideDAO.getByOwnerId(tx, owner.getId());
    for (LicenseOverride licenseOverride : licenseOverrides) {
      licenseOverrideDAO.delete(tx, licenseOverride);
    }

    // Cascade to security vulnerability overrides
    SecurityVulnerabilityOverrideDAO securityVulnerabilityOverrideDAO = new SecurityVulnerabilityOverrideDAO();
    List<SecurityVulnerabilityOverride> securityVulnerabilityOverrides = securityVulnerabilityOverrideDAO.getByOwnerId(
        tx, owner.getId());
    for (SecurityVulnerabilityOverride securityVulnerabilityOverride : securityVulnerabilityOverrides) {
      securityVulnerabilityOverrideDAO.delete(tx, securityVulnerabilityOverride);
    }

    // Cascade to data retention policies
    DataRetentionPolicyDAO dataRetentionPolicyDAO = new DataRetentionPolicyDAO();
    for (DataRetentionPolicy dataRetentionPolicy : dataRetentionPolicyDAO.getByOwnerId(tx, owner.getId()).values()) {
      dataRetentionPolicyDAO.delete(tx, dataRetentionPolicy);
    }
  }
}
