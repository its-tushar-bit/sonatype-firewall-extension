/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.ApplicationComponentDAO;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.model.ApplicationComponent;
import com.sonatype.insight.brain.model.HasComponentId;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * @since 1.146
 */
@Named
public class PolicyWaiverComponentPurlMigrator
{
  private static final Logger log = LoggerFactory.getLogger(PolicyWaiverComponentPurlMigrator.class);

  // Visible for testing
  static final String MIGRATION_ID = "policy-waiver-component-purl";

  private final MigrationTrackerDAO migrationTrackerDAO;

  private final OrganizationDAO organizationDAO;

  private final ApplicationDAO applicationDAO;

  private final RepositoryDAO repositoryDAO;

  private final PolicyWaiverDAO policyWaiverDAO;

  private final ApplicationComponentDAO applicationComponentDAO;

  private final RepositoryComponentDAO repositoryComponentDAO;

  @Inject
  public PolicyWaiverComponentPurlMigrator(
      final MigrationTrackerDAO migrationTrackerDAO,
      final OrganizationDAO organizationDAO,
      final ApplicationDAO applicationDAO,
      final RepositoryDAO repositoryDAO,
      final PolicyWaiverDAO policyWaiverDAO,
      final ApplicationComponentDAO applicationComponentDAO,
      final RepositoryComponentDAO repositoryComponentDAO)
  {
    this.migrationTrackerDAO = migrationTrackerDAO;
    this.organizationDAO = organizationDAO;
    this.applicationDAO = applicationDAO;
    this.repositoryDAO = repositoryDAO;
    this.applicationComponentDAO = applicationComponentDAO;
    this.repositoryComponentDAO = repositoryComponentDAO;
    this.policyWaiverDAO = policyWaiverDAO;
  }

  public void migrate() {
    if (migrationTrackerDAO.isTrackerPresent(MIGRATION_ID)) {
      log.debug("policy waivers are already migrated to contain purl where possible.");
      return;
    }

    List<Owner> allOwnersIncludingRepositories = getAllOwnersIncludingRepositories();
    for (Owner owner : allOwnersIncludingRepositories) {
      List<PolicyWaiver> allWaiversForOwner = policyWaiverDAO.getByOwnerId(owner.getId());
      allWaiversForOwner.forEach(policyWaiver -> addAssociatedPurlIfRequired(policyWaiver, owner));
    }
    migrationTrackerDAO.insertTracker(MIGRATION_ID);
  }

  private List<Owner> getAllOwnersIncludingRepositories() {
    List<Owner> owners = new ArrayList<>(organizationDAO.getAll()); // Will include de root org
    owners.addAll(applicationDAO.getAll());
    owners.add(RepositoryContainer.SINGLETON);
    owners.addAll(repositoryDAO.getAll());
    return owners;
  }

  @SuppressWarnings("deprecation")
  private void addAssociatedPurlIfRequired(PolicyWaiver policyWaiver, Owner owner) {
    if (policyWaiver.getHash() == null || policyWaiver.getAssociatedPackageUrl() != null) {
      return;
    }

    String purlFromDBComponent = getPurlForPolicyWaiverFromDB(policyWaiver, owner.getType());
    if (purlFromDBComponent != null) {
      policyWaiver.setAssociatedPackageUrl(purlFromDBComponent);
      policyWaiverDAO.updateWithNoChecks(policyWaiver);
    }
  }

  private String getPurlForPolicyWaiverFromDB(PolicyWaiver policyWaiver, OwnerType ownerType) {
    if (OwnerType.REPOSITORY.equals(ownerType)) {
      ComponentIdentifier repositoryComponentIdentifier =
          getRepositoryComponentIdentifierForOwner(policyWaiver, policyWaiver.getOwnerId());
      return PackageUrlIdentifier.toPackageUrl(repositoryComponentIdentifier);
    }

    if (OwnerType.APPLICATION.equals(ownerType) || OwnerType.ORGANIZATION.equals(ownerType)) {
      ApplicationComponent possibleComponent = applicationComponentDAO.getLastByHash(policyWaiver.getHash());
      if (possibleComponent != null) {
        return PackageUrlIdentifier.toPackageUrl(possibleComponent.getComponentIdentifier());
      }
    }

    if (OwnerType.REPOSITORY_CONTAINER.equals(ownerType) || (OwnerType.ORGANIZATION.equals(ownerType) &&
        Organization.ROOT_ORGANIZATION_ID.equals(policyWaiver.getOwnerId())))
    {
      Function<Owner, ComponentIdentifier> findPossibleRepositoryComponentForRepositoryOrNull =
          owner -> getRepositoryComponentIdentifierForOwner(policyWaiver, owner.getId());
      ComponentIdentifier repositoryComponentIdentifier = repositoryDAO.getAll()
          .stream()
          .map(findPossibleRepositoryComponentForRepositoryOrNull)
          .filter(Objects::nonNull)
          .findAny()
          .orElse(null);
      return PackageUrlIdentifier.toPackageUrl(repositoryComponentIdentifier);
    }

    return null;
  }

  private ComponentIdentifier getRepositoryComponentIdentifierForOwner(
      final PolicyWaiver policyWaiver,
      final String ownerId)
  {
    return repositoryComponentDAO.getByRepositoryIdAndHash(ownerId, policyWaiver.getHash())
        .stream()
        .map(HasComponentId::getComponentIdentifier)
        .filter(Objects::nonNull)
        .findAny()
        .orElse(null);
  }
}
