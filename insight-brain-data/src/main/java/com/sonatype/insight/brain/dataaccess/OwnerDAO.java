/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

import com.sonatype.insight.brain.dataaccess.configuration.DataRetentionPolicyDAO;
import com.sonatype.insight.brain.dataaccess.legal.ComponentCopyrightDAO;
import com.sonatype.insight.brain.dataaccess.legal.ComponentLegalFileDAO;
import com.sonatype.insight.brain.dataaccess.legal.ComponentObligationAttributionDAO;
import com.sonatype.insight.brain.dataaccess.legal.ComponentObligationDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseOverrideDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyMonitoringDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.SecurityVulnerabilityOverrideDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.VulnerabilityCustomCvssSeverityDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.VulnerabilityCustomCvssVectorDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.VulnerabilityCustomCweDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.VulnerabilityCustomRemediationDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.VulnerabilityGroupDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.configuration.DataRetentionPolicy;
import com.sonatype.insight.brain.model.legal.ComponentCopyright;
import com.sonatype.insight.brain.model.legal.ComponentLegalFile;
import com.sonatype.insight.brain.model.legal.ComponentObligation;
import com.sonatype.insight.brain.model.legal.ComponentObligationAttribution;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyMonitoring;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverride;
import com.sonatype.insight.brain.model.vulnerability.VulnerabilityCustomCvssSeverity;
import com.sonatype.insight.brain.model.vulnerability.VulnerabilityCustomCvssVector;
import com.sonatype.insight.brain.model.vulnerability.VulnerabilityCustomCwe;
import com.sonatype.insight.brain.model.vulnerability.VulnerabilityCustomRemediation;
import com.sonatype.insight.brain.model.vulnerability.VulnerabilityGroup;
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

    return repoDAO.getById(tx, id);
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

  public Set<Application> getDescendantOrSelfApplications(Owner owner) {
    Set<Application> applications = new HashSet<>();
    addDescendantOrSelfApplications(applications, owner);
    return applications;
  }

  private void addDescendantOrSelfApplications(Set<Application> applications, Owner owner) {
    if (OwnerType.APPLICATION.equals(owner.getType())) {
      applications.add((Application) owner);
    }
    else {
      getChildOwners(owner).forEach(childOwner -> addDescendantOrSelfApplications(applications, childOwner));
    }
  }

  public Set<String> getDescendantOrSelfApplicationIds(Owner owner) {
    return getDescendantOrSelfApplications(owner).stream().map(Owner::getId).collect(Collectors.toSet());
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

    // Cascade to policy overrides
    PolicyDAO policyDAO = new PolicyDAO();
    for (Policy policy : policyDAO.getAll(tx)) {
      boolean updated = false;
      if (policy.getPolicyActionsOverrides() != null && policy.getPolicyActionsOverrides().containsKey(owner.getId())) {
        policy.getPolicyActionsOverrides().remove(owner.getId());
        updated = true;
      }
      if (policy.getPolicyNotificationsOverrides() != null &&
          policy.getPolicyNotificationsOverrides().containsKey(owner.getId())) {
        policy.getPolicyNotificationsOverrides().remove(owner.getId());
        updated = true;
      }
      if (updated) {
        policyDAO.update(tx, policy);
      }
    }

    // Cascade to data retention policies
    DataRetentionPolicyDAO dataRetentionPolicyDAO = new DataRetentionPolicyDAO();
    for (DataRetentionPolicy dataRetentionPolicy : dataRetentionPolicyDAO.getByOwnerId(tx, owner.getId()).values()) {
      dataRetentionPolicyDAO.delete(tx, dataRetentionPolicy);
    }

    // Cascade to policy monitoring
    PolicyMonitoringDAO policyMonitoringDAO = new PolicyMonitoringDAO();
    PolicyMonitoring policyMonitoring = policyMonitoringDAO.getByOwnerId(tx, owner.getId());
    if (policyMonitoring != null) {
      policyMonitoringDAO.delete(tx, policyMonitoring);
    }

    // Cascade to component copyrights
    ComponentCopyrightDAO componentCopyrightDAO = new ComponentCopyrightDAO();
    for (ComponentCopyright componentCopyright : componentCopyrightDAO.getByOwnerId(tx, owner.getId())) {
      componentCopyrightDAO.delete(tx, componentCopyright);
    }

    // Cascade to component legal files
    ComponentLegalFileDAO componentLegalFileDAO = new ComponentLegalFileDAO();
    for (ComponentLegalFile componentLegalFile : componentLegalFileDAO.getByOwnerId(tx, owner.getId())) {
      componentLegalFileDAO.delete(tx, componentLegalFile);
    }

    // Cascade to component obligations
    ComponentObligationDAO componentObligationDAO = new ComponentObligationDAO();
    for (ComponentObligation componentObligation : componentObligationDAO.getByOwnerId(tx, owner.getId())) {
      componentObligationDAO.delete(tx, componentObligation);
    }

    // Cascade to component obligation attributions
    ComponentObligationAttributionDAO componentObligationAttributionDAO = new ComponentObligationAttributionDAO();
    for (ComponentObligationAttribution componentObligationAttribution : componentObligationAttributionDAO
        .getByOwnerId(tx, owner.getId())) {
      componentObligationAttributionDAO.delete(tx, componentObligationAttribution);
    }

    // Cascade to vulnerability groups
    VulnerabilityGroupDAO vulnerabilityGroupDAO = new VulnerabilityGroupDAO();
    for (VulnerabilityGroup vulnerabilityGroup : vulnerabilityGroupDAO.getByOwnerId(tx, owner.getId())) {
      vulnerabilityGroupDAO.delete(tx, vulnerabilityGroup);
    }

    // Cascade to vulnerability custom remediation
    VulnerabilityCustomRemediationDAO vulnerabilityCustomRemediationDAO = new VulnerabilityCustomRemediationDAO();
    for (VulnerabilityCustomRemediation vulnerabilityCustomRemediation : vulnerabilityCustomRemediationDAO
        .getByOwnerId(tx, owner.getId())) {
      vulnerabilityCustomRemediationDAO.delete(tx, vulnerabilityCustomRemediation);
    }

    // Cascade to vulnerability custom CWE
    VulnerabilityCustomCweDAO vulnerabilityCustomCweDAO = new VulnerabilityCustomCweDAO();
    for (VulnerabilityCustomCwe vulnerabilityCustomCwe : vulnerabilityCustomCweDAO.getByOwnerId(tx, owner.getId())) {
      vulnerabilityCustomCweDAO.delete(tx, vulnerabilityCustomCwe);
    }

    // Cascade to vulnerability custom CVSS vector data
    VulnerabilityCustomCvssVectorDAO vulnerabilityCustomCvssDAO = new VulnerabilityCustomCvssVectorDAO();
    for (VulnerabilityCustomCvssVector vulnerabilityCustomCvss : vulnerabilityCustomCvssDAO.getByOwnerId(tx,
        owner.getId())) {
      vulnerabilityCustomCvssDAO.delete(tx, vulnerabilityCustomCvss);
    }

    // Cascade to vulnerability custom CVSS severity data
    VulnerabilityCustomCvssSeverityDAO vulnerabilityCustomCvssSeverityDAO = new VulnerabilityCustomCvssSeverityDAO();
    for (VulnerabilityCustomCvssSeverity vulnerabilityCustomCvssSeverity :
        vulnerabilityCustomCvssSeverityDAO.getByOwnerId(tx,
        owner.getId())) {
      vulnerabilityCustomCvssSeverityDAO.delete(tx, vulnerabilityCustomCvssSeverity);
    }
  }

  /**
   * Returns the ids of all the owners up in the hierarchy for the specified owner, starting with the id of the input
   * owner.
   */
  public List<String> getOwnerIds(Owner owner) {
    List<String> ownerIds = new ArrayList<>();
    walkHierarchy(owner).forEach(anOwner -> ownerIds.add(anOwner.getId()));
    return ownerIds;
  }

  public List<String> getOwnerIds(String ownerId) {
    Owner owner = getById(ownerId);
    return getOwnerIds(owner);
  }
}
