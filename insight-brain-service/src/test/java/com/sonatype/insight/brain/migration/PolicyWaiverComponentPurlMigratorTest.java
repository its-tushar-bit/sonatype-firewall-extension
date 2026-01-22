/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import java.lang.reflect.Modifier;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.ApplicationComponentDAO;
import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.test.LogOutput;

import com.google.inject.Binder;
import com.google.inject.matcher.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verifyNoInteractions;

public class PolicyWaiverComponentPurlMigratorTest
    extends AbstractComponentTest
{
  @Rule
  public LogOutput logOutput = new LogOutput(PolicyWaiverComponentPurlMigrator.class);

  @Inject
  private MigrationTrackerDAO migrationTrackerDAO;

  @Inject
  private PolicyWaiverDAO policyWaiverDAO;

  @Inject
  private PolicyWaiverComponentPurlMigrator policyWaiverComponentPurlMigrator;

  @Mock
  ApplicationComponentDAO mockApplicationComponentDAO;

  @Override
  public void configure(Binder binder) {
    configureApplicationComponentDAO(binder);
    super.configure(binder);
  }

  private void configureApplicationComponentDAO(final Binder binder) {
    // Funky config to be able to test testMigrate_waiverDoesNotRequireMigration but also allow the DAO to
    // continue working to set up the rest of the test cases
    binder.bindInterceptor(Matchers.subclassesOf(ApplicationComponentDAO.class), Matchers.any(), invocation -> {
      if (invocation.getMethod().getModifiers() == Modifier.PUBLIC) {
        invocation.getMethod().invoke(mockApplicationComponentDAO, invocation.getArguments());
      }
      return invocation.proceed();
    });
  }

  @Before
  public void beforeEach() {
    migrationTrackerDAO.deleteById(PolicyWaiverComponentPurlMigrator.MIGRATION_ID);
  }

  @Test
  public void testMigrate_alreadyMigrated() {
    migrationTrackerDAO.insertTracker(PolicyWaiverComponentPurlMigrator.MIGRATION_ID);
    policyWaiverComponentPurlMigrator.migrate();
    logOutput.assertThat().atDebugLevel()
        .contains("policy waivers are already migrated to contain purl where possible.");
  }

  @Test
  public void testMigrate_waiverDoesNotRequireMigration() {
    reset(mockApplicationComponentDAO);
    Organization organization = tempEntity.newOrganization();
    Application application = tempEntity.newApplication(organization.getId());
    Policy policy = tempEntity.newPolicy(organization);
    tempEntity.newWaiver(null, policy.getId(), application.getId());
    tempEntity.newWaiver("hash", policy.getId(), application.getId(), null, "purl",
        ComponentMatcherStrategyForWaiver.EXACT_COMPONENT, "comment");

    policyWaiverComponentPurlMigrator.migrate();
    verifyNoInteractions(mockApplicationComponentDAO);
  }

  @Test
  public void testMigrate_applicationWaiver() {
    Organization organization = tempEntity.newOrganization();
    Application application = tempEntity.newApplication(organization.getId());
    Policy policy = tempEntity.newPolicy(organization);

    tempEntity.newApplicationComponent(application.getId(), StageTypes.BUILD.getName(), "hash",
        getBasicComponentIdentifier());
    PolicyWaiver policyWaiver = tempEntity.newWaiver("hash", policy.getId(), application.getId(), null, null,
        ComponentMatcherStrategyForWaiver.EXACT_COMPONENT, "comment");

    policyWaiverComponentPurlMigrator.migrate();
    PolicyWaiver waiverAfterMigration = policyWaiverDAO.getById(policyWaiver.getId());
    assertThat(waiverAfterMigration.getAssociatedPackageUrl()).isNotNull().isEqualTo(getBasicPurl());
  }

  @Test
  public void testMigrate_organizationWaiver() {
    Organization organization = tempEntity.newOrganization();
    Application application = tempEntity.newApplication(organization.getId());
    Policy policy = tempEntity.newPolicy(organization);

    tempEntity.newApplicationComponent(application.getId(), StageTypes.BUILD.getName(), "hash",
        getBasicComponentIdentifier());
    PolicyWaiver policyWaiver = tempEntity.newWaiver("hash", policy.getId(), organization.getId(), null, null,
        ComponentMatcherStrategyForWaiver.EXACT_COMPONENT, "comment");

    policyWaiverComponentPurlMigrator.migrate();
    PolicyWaiver waiverAfterMigration = policyWaiverDAO.getById(policyWaiver.getId());
    assertThat(waiverAfterMigration.getAssociatedPackageUrl()).isNotNull().isEqualTo(getBasicPurl());
  }

  @Test
  public void testMigrate_repositoryWaiver() {
    Organization organization = tempEntity.newOrganization();
    Repository repository = tempEntity.newRepository();
    Policy policy = tempEntity.newPolicy(organization);

    tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT, "", "hashRepo",
        getBasicComponentIdentifier(),
        false);
    PolicyWaiver policyWaiver = tempEntity.newWaiver("hashRepo", policy.getId(), repository.getId(), null, null,
        ComponentMatcherStrategyForWaiver.EXACT_COMPONENT, "comment");

    policyWaiverComponentPurlMigrator.migrate();
    PolicyWaiver waiverAfterMigration = policyWaiverDAO.getById(policyWaiver.getId());
    assertThat(waiverAfterMigration.getAssociatedPackageUrl()).isNotNull().isEqualTo(getBasicPurl());
  }

  @Test
  public void testMigrate_repositoryContainerWaiver() {
    Organization organization = tempEntity.newOrganization();
    Repository repository = tempEntity.newRepository();
    Policy policy = tempEntity.newPolicy(organization);

    tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT, "", "hashRepo",
        getBasicComponentIdentifier(),
        false);
    PolicyWaiver policyWaiver =
        tempEntity.newWaiver("hashRepo", policy.getId(), RepositoryContainer.REPOSITORY_CONTAINER_ID, null, null,
            ComponentMatcherStrategyForWaiver.EXACT_COMPONENT, "comment");

    policyWaiverComponentPurlMigrator.migrate();
    PolicyWaiver waiverAfterMigration = policyWaiverDAO.getById(policyWaiver.getId());
    assertThat(waiverAfterMigration.getAssociatedPackageUrl()).isNotNull().isEqualTo(getBasicPurl());
  }

  @Test
  public void testMigrate_rootOrganizationWaiver() {
    Organization organization = tempEntity.newOrganization();
    Application application = tempEntity.newApplication(organization.getId());
    Repository repository = tempEntity.newRepository();
    Policy policy = tempEntity.newPolicy(organization);

    tempEntity.newApplicationComponent(application.getId(), StageTypes.BUILD.getName(), "hash",
        getBasicComponentIdentifier());
    tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT, "", "hashRepo",
        getBasicComponentIdentifier(),
        false);
    PolicyWaiver applicationHashWaiver =
        tempEntity.newWaiver("hash", policy.getId(), Organization.ROOT_ORGANIZATION_ID, null, null,
            ComponentMatcherStrategyForWaiver.EXACT_COMPONENT, "comment");
    PolicyWaiver repositoryHashWaiver =
        tempEntity.newWaiver("hashRepo", policy.getId(), Organization.ROOT_ORGANIZATION_ID, null, null,
            ComponentMatcherStrategyForWaiver.EXACT_COMPONENT, "comment");

    policyWaiverComponentPurlMigrator.migrate();
    PolicyWaiver applicationWaiverAfterMigration = policyWaiverDAO.getById(applicationHashWaiver.getId());
    assertThat(applicationWaiverAfterMigration.getAssociatedPackageUrl()).isNotNull().isEqualTo(getBasicPurl());
    PolicyWaiver repositoryWaiverAfterMigration = policyWaiverDAO.getById(repositoryHashWaiver.getId());
    assertThat(repositoryWaiverAfterMigration.getAssociatedPackageUrl()).isNotNull().isEqualTo(getBasicPurl());
  }

  @Test
  public void testMigrate_duplicateWaivers() {
    Organization organization = tempEntity.newOrganization();
    Application application = tempEntity.newApplication(organization.getId());
    Policy policy = tempEntity.newPolicy(organization);

    String policyId = policy.getId();
    String ownerId = organization.getId();
    String hash1 = TemporaryEntity.uuid().substring(0, 8);

    tempEntity.newApplicationComponent(application.getId(), StageTypes.BUILD.getName(), hash1,
        getBasicComponentIdentifier());

    PolicyWaiver expiredPolicyWaiver =
        new PolicyWaiver(hash1, policyId, ownerId, null, TemporaryEntity.uuid().substring(0, 8));
    expiredPolicyWaiver.setExpiryTime(Date.from(Instant.now().minus(5, ChronoUnit.DAYS)));
    expiredPolicyWaiver.setComponentMatchStrategy(ComponentMatcherStrategyForWaiver.EXACT_COMPONENT);
    tempEntity.newWaiver(expiredPolicyWaiver);

    PolicyWaiver activePolicyWaiver =
        new PolicyWaiver(hash1, policyId, ownerId, null, TemporaryEntity.uuid().substring(0, 8));
    activePolicyWaiver.setExpiryTime(Date.from(Instant.now().plus(1, ChronoUnit.DAYS)));
    activePolicyWaiver.setComponentMatchStrategy(ComponentMatcherStrategyForWaiver.EXACT_COMPONENT);

    tempEntity.newWaiver(activePolicyWaiver);

    assertThat(expiredPolicyWaiver.getAssociatedPackageUrl())
        .as("Associated package url on expired waiver should not be set before migration")
        .isNull();

    assertThat(activePolicyWaiver.getAssociatedPackageUrl())
        .as("Associated package url on active waiver should not be set before migration")
        .isNull();

    policyWaiverComponentPurlMigrator.migrate();

    PolicyWaiver expiredWaiverAfterMigration = policyWaiverDAO.getById(expiredPolicyWaiver.getId());
    assertThat(expiredWaiverAfterMigration.getAssociatedPackageUrl())
        .as("Associated package url on expired waiver should be set after the migration")
        .isEqualTo(getBasicPurl());

    PolicyWaiver activeWaiverAfterMigration = policyWaiverDAO.getById(activePolicyWaiver.getId());
    assertThat(activeWaiverAfterMigration.getAssociatedPackageUrl())
        .as("Associated package url on active waiver should be set after the migration")
        .isEqualTo(getBasicPurl());
  }

  private static ComponentIdentifier getBasicComponentIdentifier() {
    return ComponentIdentifier.createMavenCoordinates("testGroupId", "testArtifactId", "testVersionId", null, "jar");
  }

  private static String getBasicPurl() {
    return PackageUrlIdentifier.fromComponentIdentifier(getBasicComponentIdentifier()).getPackageUrl();
  }
}
