/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.stream.Collectors;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.common.test.PostgresTestCategory;
import com.sonatype.insight.brain.dataaccess.configuration.CpeMatchingConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ProprietaryConfigDAO;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.dataaccess.innersource.InnerSourceApplicationDAO;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseOverrideDAO;
import com.sonatype.insight.brain.dataaccess.policy.AutoPolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyMonitoringDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverRequestDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryConnectionDAO;
import com.sonatype.insight.brain.dataaccess.sast.SastFindingDAO;
import com.sonatype.insight.brain.dataaccess.sast.SastScanDAO;
import com.sonatype.insight.brain.dataaccess.security.MembershipMappingDAO;
import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDefaultBranchCommitHistoryDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlEventDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlPullRequestCommentDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlPullRequestResultDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlUserDAO;
import com.sonatype.insight.brain.dataaccess.successmetrics.PolicyViolationAggregationDAO;
import com.sonatype.insight.brain.dataaccess.tag.ApplicationTagDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileCoordinateDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyScanDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.SecurityVulnerabilityOverrideDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.VulnerabilityCustomCvssSeverityDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.VulnerabilityCustomCvssVectorDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.VulnerabilityCustomCweDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.VulnerabilityCustomRemediationDAO;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ApplicationComponent;
import com.sonatype.insight.brain.model.ApplicationRiskDTO;
import com.sonatype.insight.brain.model.Color;
import com.sonatype.insight.brain.model.InvalidNameException;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.model.NameHelperTest;
import com.sonatype.insight.brain.model.Nameable;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.SearchIndexChange;
import com.sonatype.insight.brain.model.SearchIndexChange.ChangeType;
import com.sonatype.insight.brain.model.configuration.CpeMatchingConfiguration;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.innersource.InnerSourceApplication;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiver;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyMonitoring;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.PolicyWaiverRequest;
import com.sonatype.insight.brain.model.policy.notifications.Notifications;
import com.sonatype.insight.brain.model.policy.notifications.UserNotification;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.model.policy.stages.SourceStageType;
import com.sonatype.insight.brain.model.repository.RepositoryConnection;
import com.sonatype.insight.brain.model.sast.SastFinding;
import com.sonatype.insight.brain.model.sast.SastFindingConfidence;
import com.sonatype.insight.brain.model.sast.SastFindingSeverity;
import com.sonatype.insight.brain.model.sast.SastScan;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlDefaultBranchCommitHistory;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlPullRequestComment;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlPullRequestResult;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlUser;
import com.sonatype.insight.brain.model.successmetrics.PolicyViolationAggregation;
import com.sonatype.insight.brain.model.tag.ApplicationTag;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyScan;
import com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverride;
import com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverrideStatus;
import com.sonatype.insight.brain.model.vulnerability.VulnerabilityCustomCvssSeverity;
import com.sonatype.insight.brain.model.vulnerability.VulnerabilityCustomCvssVector;
import com.sonatype.insight.brain.model.vulnerability.VulnerabilityCustomCwe;
import com.sonatype.insight.brain.model.vulnerability.VulnerabilityCustomRemediation;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.scan.model.ClientScanType;
import com.sonatype.nexus.scm.SourceControlProvider;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadataStatus.PENDING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ApplicationDAOTest
    extends NameableDAOTest<Application>
{
  /**
   * Prohibited application public ID whitespace characters.
   */
  public static final char[] PUBLIC_ID_WHITESPACE_CHARS = {'\t', '\n', '\u000B', '\f', '\r'};

  private ApplicationDAO applicationDAO;

  @Before
  @Override
  public void setup() {
    super.setup();
    applicationDAO = daoFactory.createApplicationDAO();
  }

  @Override
  protected Application createNameable(String a) {
    Application app = new Application("publicId" + System.nanoTime(), a, organization.getId());
    applicationDAO.insert(app);
    return app;
  }

  @Override
  protected AbstractOperationalSqlDAO<Application> getDao() {
    return applicationDAO;
  }

  @Override
  protected int getMaxNameLength() {
    return NameHelper.MAX_NAME_LENGTH_APP_ORG;
  }

  @Override
  protected Application getEntityByName(String name) {
    return applicationDAO.getByName(name);
  }

  @Test
  public void testCRUD() {
    // Create
    Application app = tempEntity.newApplication(organization.getId());

    // Update
    app = applicationDAO.getById(app.getId());
    app.setName("ApplicationDAOTest New name");
    applicationDAO.update(app);
    app = applicationDAO.getById(app.getId());
    assertThat(app.getName()).isEqualTo("ApplicationDAOTest New name");

    // Delete
    applicationDAO.delete(app);
    app = applicationDAO.getById(app.getId());
    assertThat(app).isNull();
  }

  @Test
  public void testGetCount() {
    assertThat(applicationDAO.getCount()).isEqualTo(1);
  }

  @Test
  public void testGetAll() {
    // Create a few apps
    int appCount = 3;
    tempEntity.newApplications(organization.getId(), appCount);

    // Create an app with a related repository manager
    Organization orgWithRepoManager = tempEntity.newOrganizationWithRepositoryManager("org-with-repo-manager");
    tempEntity.newApplications(orgWithRepoManager.getId(), 1);

    List<Application> apps = applicationDAO.getAll();

    // getAll should return appCount + 2, accounting for:
    // 1. The app created by AbstractDbDAOTest.
    // 2. The app created with a related repository manager.
    assertThat(apps).hasSize(appCount + 2);
    assertThat(
      apps.stream()
        .anyMatch(app -> app.getOrganizationId().equals(orgWithRepoManager.getId()))
    ).isTrue();
  }

  @Test
  public void testGetAllOrderedByName() {
    tempEntity.newApplicationWithParent("application-1", "Application Z1");
    tempEntity.newApplicationWithParent("application-2", "Application A3");
    tempEntity.newApplicationWithParent("application-3", "Application A2");
    tempEntity.newApplicationWithParent("application-4", "Application A1");
    tempEntity.newApplicationWithParent("application-5", "Application M1");

    assertThat(applicationDAO.getAllOrderedByName().stream().map(a -> a.getName())).isEqualTo(
        Arrays.asList(
            "AbstractDbDAOTest-AppName",
            "Application A1",
            "Application A2",
            "Application A3",
            "Application M1",
            "Application Z1"
        )
    );
  }

  @Test
  public void testGetAllWithoutRelatedRepositoriesOrderedByName() {
    tempEntity.newApplicationWithParent("application-1", "Application Z1");
    tempEntity.newApplicationWithParent("application-2", "Application A3");
    tempEntity.newApplicationWithParent("application-3", "Application A2");
    tempEntity.newApplicationWithParent("application-4", "Application A1");
    tempEntity.newApplicationWithParent("application-5", "Application M1");

    // Create an app with both a related repository manager and repository
    Organization orgWithRelatedRepo = tempEntity.newOrganizationWithRepositoryManager("org-with-repo");
    tempEntity.newApplication(orgWithRelatedRepo.getId());

    assertThat(applicationDAO.getAllWithoutRelatedRepositoriesOrderedByName())
        .extracting(Nameable::getName)
        .containsExactly(
            "AbstractDbDAOTest-AppName",
            "Application A1",
            "Application A2",
            "Application A3",
            "Application M1",
            "Application Z1"
        );
  }

  @Test
  public void testGetAllWithoutRelatedRepositories() {
    // Create an app without a related repository manager
    tempEntity.newApplications(organization.getId(), 1);

    // Create an app with both a related repository manager and repository
    Organization orgWithRelatedRepo = tempEntity.newOrganizationWithRepositoryManager("org-with-repo");
    tempEntity.newApplications(orgWithRelatedRepo.getId(), 1);

    List<Application> appsWithoutRelatedRepositories = applicationDAO.getAllWithoutRelatedRepositories();

    // The getAllWithoutRelatedRepositories method should return 2 applications:
    // 1. The application created by AbstractDbDAOTest.
    // 2. The application without a related repository manager.
    assertThat(appsWithoutRelatedRepositories).hasSize(2);
    for (Application app : appsWithoutRelatedRepositories) {
      assertThat(app.getOrganizationId()).isNotEqualTo(orgWithRelatedRepo.getId());
    }
  }

  @Test
  public void testGetApplicationsByPublicIds() {
    // Create a few applications
    int numApplication = 3;
    List<Application> applications = tempEntity.newApplications(organization.getId(), numApplication);
    Set<String> publicIds = new HashSet<>();
    for (Application app : applications) {
      publicIds.add(app.getPublicId());
    }

    // Note: applicationDAO.getByPublicIds returns an unmodifiable list, since we need to sort the list we create one
    List<Application> retrievedApplications = new ArrayList<>(applicationDAO.getByPublicIds(publicIds));
    assertThat(retrievedApplications).hasSize(numApplication);
    assertApplications(retrievedApplications, applications);
  }

  @Test
  public void testGetApplicationsByPublicIds_CaseInsensitive() {
    Application app = tempEntity.newApplicationWithParent();

    // Note: applicationDAO.getByPublicIds returns an unmodifiable list, since we try to sort the list we create one
    List<Application> retrievedApplications =
        new ArrayList<>(applicationDAO.getByPublicIds(Collections.singleton(StringUtils.swapCase(app.getPublicId()))));

    assertThat(retrievedApplications).hasSize(1);
    assertApplications(retrievedApplications, Collections.singletonList(app));
  }

  @Test
  public void testGetApplicationsByPublicIds_EmptySet() {
    // Create a few applications
    tempEntity.newApplications(organization.getId(), 3);
    Set<String> publicIds = new HashSet<>();
    List<Application> retrievedApplications = new ArrayList<>(applicationDAO.getByPublicIds(publicIds));
    assertThat(retrievedApplications).isEmpty();
  }

  @Test
  public void testGetByRepositoryUrl() {
    // given: a set of applications, a repository URL and some apps associated with that URL and some not
    final String repositoryURL = "http://test.gitlab.com/org/MixedCaseName";

    // set root org source control
    tempEntity.newSourceControl(organization.getParentOrganizationId(), null, "token", SourceControlProvider.GITLAB);

    // app1 is associated with the repo URL
    tempEntity.newApplicationWithSpecificId("app1", "application 1", "app1", organization.getId());
    tempEntity.newSourceControl("app1", repositoryURL);

    // app2 is NOT associated with the repo URL
    tempEntity.newApplicationWithSpecificId("app2", "application 2", "app2", organization.getId());

    // app3 is associated with the repo URL
    tempEntity.newApplicationWithSpecificId("app3", "application 3", "app3", organization.getId());
    tempEntity.newSourceControl("app3", repositoryURL);

    // when: fetch apps associated with the given repo URL
    List<Application> repoApps = applicationDAO.getByRepositoryUrl(repositoryURL);

    // then: app1 and app3 associated, app2 is NOT
    assertThat(repoApps).hasSize(2);
    Set<String> applicationIds = repoApps.stream().map(Application::getId).collect(Collectors.toSet());
    assertThat(applicationIds).contains("app1");
    assertThat(applicationIds).contains("app3");
    assertThat(applicationIds).doesNotContain("app2");

    // when: fetch using repo URL not associated with any applications
    repoApps = applicationDAO.getByRepositoryUrl("bogus URL");

    // then: no matches, yet list not null
    assertThat(repoApps).isNotNull();
    assertThat(repoApps).isEmpty();
  }

  @Test
  public void testGetApplicationsByTagIds() {
    int numApplications = 3;
    Tag tag1 = tempEntity.newTag(organization.getId(), "foo");
    Tag tag2 = tempEntity.newTag(organization.getId(), "bar");
    List<Application> applications = tempEntity.newApplications(organization.getId(), numApplications);
    for (Application app : applications) {
      tempEntity.newApplicationTag(app.getId(), tag1.getId());
    }

    // assign second tag to one of the apps
    tempEntity.newApplicationTag(applications.get(0).getId(), tag2.getId());

    // searching by both tags should result in 3 unique apps
    List<Application> retrievedApplications = Lists
        .newArrayList(applicationDAO.getByTagIds(Sets.newHashSet(tag1.getId(), tag2.getId())));
    assertThat(retrievedApplications).hasSize(numApplications);
    assertApplications(retrievedApplications, applications);

    // find nothing without
    retrievedApplications = Lists.newArrayList(applicationDAO.getByTagIds(Sets.newHashSet("notMyTagId")));
    assertThat(retrievedApplications).isEmpty();
  }

  @Test
  public void testGetApplicationsByTagIds_Untagged() {
    String tagName = "foo";
    Tag tag = tempEntity.newTag(organization.getId(), tagName);
    Application taggedApplication = tempEntity.newApplication(organization.getId());
    tempEntity.newApplicationTag(taggedApplication.getId(), tag.getId());

    // NOTE: the application created in AbstractDbDAOTest has no tags
    List<Application> allApplications = Lists.newArrayList(taggedApplication, application);

    // find both apps with tag and null
    List<Application> retrievedApplications = Lists
        .newArrayList(applicationDAO.getByTagIds(Sets.newHashSet(tag.getId(), null)));
    assertThat(retrievedApplications).hasSize(2);
    assertApplications(retrievedApplications, allApplications);

    // find just the untagged one with just null
    retrievedApplications = Lists.newArrayList(applicationDAO.getByTagIds(Sets.newHashSet((String) null)));
    assertThat(retrievedApplications).hasSize(1);
    assertApplications(retrievedApplications, Lists.newArrayList(application));

    // do not find the untagged one without null
    retrievedApplications = Lists.newArrayList(applicationDAO.getByTagIds(Sets.newHashSet(tag.getId())));
    assertThat(retrievedApplications).hasSize(1);
    assertApplications(retrievedApplications, Lists.newArrayList(taggedApplication));
  }

  /**
   * A given application should only be returned once, no matter how many matching tags it has (cf. CLM-3385).
   */
  @Test
  public void testGetApplicationsByIdsAndTagIds_UniqueResults() {
    Tag tag1 = tempEntity.newTag(organization.getId(), "test-tag-1");
    Tag tag2 = tempEntity.newTag(organization.getId(), "test-tag-2");
    Application taggedApplication = tempEntity.newApplication(organization.getId());
    tempEntity.newApplicationTag(taggedApplication.getId(), tag1.getId());
    tempEntity.newApplicationTag(taggedApplication.getId(), tag2.getId());

    List<Application> applications = applicationDAO.getByIdsAndTagIds(Collections.singleton(taggedApplication.getId()),
        Sets.newHashSet(tag1.getId(), tag2.getId()));

    assertThat(applications).hasSize(1);
    assertApplications(applications, Collections.singletonList(taggedApplication));
  }

  @Test
  public void testGetApplicationsByIdsAndTagIds_Untagged() {
    int numTaggedApplication = 2;
    String tagName = "foo";
    Tag tag = tempEntity.newTag(organization.getId(), tagName);
    List<Application> taggedApplications = tempEntity.newApplications(organization.getId(), numTaggedApplication);
    for (Application app : taggedApplications) {
      tempEntity.newApplicationTag(app.getId(), tag.getId());
    }

    Application taggedApplication = taggedApplications.get(0);
    Application untaggedApplication = application;

    // this list will contain one of the two tagged apps and the untagged app
    List<Application> applications = Lists.newArrayList(taggedApplication, untaggedApplication);

    Set<String> applicationIdsToQuery = Sets.newHashSet(taggedApplication.getId(), untaggedApplication.getId());

    // find the tagged one that we expected and the untagged one
    List<Application> retrievedApplications = Lists
        .newArrayList(applicationDAO.getByIdsAndTagIds(applicationIdsToQuery, Sets.newHashSet(tag.getId(), null)));
    assertThat(retrievedApplications).hasSize(2);
    assertApplications(retrievedApplications, applications);

    // find just the untagged one with just null
    retrievedApplications = Lists
        .newArrayList(applicationDAO.getByIdsAndTagIds(applicationIdsToQuery, Sets.newHashSet((String) null)));
    assertThat(retrievedApplications).hasSize(1);
    assertApplications(retrievedApplications, Lists.newArrayList(untaggedApplication));

    // do not find the untagged one without null
    retrievedApplications = Lists
        .newArrayList(applicationDAO.getByIdsAndTagIds(applicationIdsToQuery, Sets.newHashSet(tag.getId())));
    assertThat(retrievedApplications).hasSize(1);
    assertApplications(retrievedApplications, Lists.newArrayList(taggedApplication));

    // do not find the untagged app if its id isn't in the app id list
    retrievedApplications = Lists.newArrayList(applicationDAO
        .getByIdsAndTagIds(Sets.newHashSet(taggedApplication.getId()), Sets.newHashSet(tag.getId(), null)));
    assertThat(retrievedApplications).hasSize(1);
    assertApplications(retrievedApplications, Lists.newArrayList(taggedApplication));
  }

  @Test
  public void testUpdate_ChangeOrganizationId() {
    Organization organization1 = tempEntity.newOrganization("testUpdateOrganizationId 1");

    // Update with a different organization id - should fail
    application.setOrganizationId(organization1.getId());
    assertThatThrownBy(() -> applicationDAO.update(application)).isInstanceOf(InvalidApplicationException.class)
        .hasMessage("Cannot change the parent organization of an application.");
  }

  @Test
  public void testUpdate_ChangeOrganizationId_Force() {
    Organization organization1 = tempEntity.newOrganization("testUpdateOrganizationId 1");

    application.setOrganizationId(organization1.getId());
    try (TransactionContext tx = applicationDAO.createTransactionContext()) {
      tx.begin();
      applicationDAO.update(tx, application, true);
      tx.commit();
    }
    assertThat(applicationDAO.getById(application.getId()).getOrganizationId()).isEqualTo(organization1.getId());
  }

  @Test
  public void testInsert_ValidatePublicIdValidChars() {
    for (String publicId : NameHelperTest.VALID_NAMES) {
      tempEntity.newApplication(TemporaryEntity.uuid(), publicId.replaceAll("\\s", ""), organization.getId());
    }
  }

  @Test
  public void testUpdate_ValidatePublicIdValidChars() {
    for (String publicId : NameHelperTest.VALID_NAMES) {
      application.setPublicId(publicId.replaceAll("\\s", ""));
      applicationDAO.update(application);
    }
  }

  @Test
  public void testInsert_ValidatePublicIdInvalidChars() {
    Application app = new Application(null, "name", organization.getId());
    for (String publicId : NameHelperTest.INVALID_CHARACTERS) {
      app.setPublicId(publicId);
      assertThatThrownBy(() -> applicationDAO.insert(app)).isInstanceOf(InvalidNameException.class)
          .hasMessage(NameHelper.INVALID_CHAR_MESSAGE, "Public ID", publicId.charAt(0));
    }
  }

  @Test
  public void testUpdate_ValidatePublicIdInvalidChars() {
    for (String publicId : NameHelperTest.INVALID_CHARACTERS) {
      application.setPublicId(publicId);
      assertThatThrownBy(() -> applicationDAO.update(application)).isInstanceOf(InvalidNameException.class)
          .hasMessage(NameHelper.INVALID_CHAR_MESSAGE, "Public ID", publicId.charAt(0));
    }
  }

  @Test
  public void testInsert_ValidateNullPublicId() {
    Application app = new Application(null, "name", organization.getId());
    assertThatThrownBy(() -> applicationDAO.insert(app)).isInstanceOf(InvalidNameException.class)
        .hasMessage("Public ID is required.");
  }

  @Test
  public void testUpdate_ValidateNullPublicId() {
    application.setPublicId(null);
    assertThatThrownBy(() -> applicationDAO.update(application)).isInstanceOf(InvalidNameException.class)
        .hasMessage("Public ID is required.");
  }

  @Test
  public void testInsert_ValidateEmptyPublicId() {
    Application app = new Application("", "name", organization.getId());
    assertThatThrownBy(() -> applicationDAO.insert(app)).isInstanceOf(InvalidNameException.class)
        .hasMessage("Public ID is required.");
  }

  @Test
  public void testUpdate_ValidateEmptyPublicId() {
    application.setPublicId("");
    assertThatThrownBy(() -> applicationDAO.update(application)).isInstanceOf(InvalidNameException.class)
        .hasMessage("Public ID is required.");
  }

  @Test
  public void testInsert_ValidatePublicIdWithWhitespaces() {
    Application app = new Application(null, "name", organization.getId());
    for (char invalidChar : PUBLIC_ID_WHITESPACE_CHARS) {
      app.setPublicId("foo" + invalidChar + "bar");
      assertThatThrownBy(() -> applicationDAO.insert(app)).isInstanceOfAny(InvalidApplicationException.class,
              InvalidNameException.class)
          .satisfies(e -> assertThat(e.getMessage()).isIn("Public ID cannot contain whitespaces.",
              String.format(NameHelper.INVALID_CHAR_MESSAGE, "Public ID", invalidChar)));
    }
  }

  @Test
  public void testUpdate_ValidatePublicIdWithWhitespaces() {
    for (char invalidChar : PUBLIC_ID_WHITESPACE_CHARS) {
      application.setPublicId("foo" + invalidChar + "bar");
      assertThatThrownBy(() -> applicationDAO.update(application)).isInstanceOfAny(InvalidApplicationException.class,
              InvalidNameException.class)
          .satisfies(e -> assertThat(e.getMessage()).isIn("Public ID cannot contain whitespaces.",
              String.format(NameHelper.INVALID_CHAR_MESSAGE, "Public ID", invalidChar)));
    }
  }

  @Test
  public void testInsert_ValidatePublicIdIsDot() {
    Application app = new Application(".", "name", organization.getId());
    assertThatThrownBy(() -> applicationDAO.insert(app)).isInstanceOf(InvalidApplicationException.class)
        .hasMessage("Public ID cannot be '.' or '..'");
  }

  @Test
  public void testUpdate_ValidatePublicIdIsDot() {
    application.setPublicId(".");
    assertThatThrownBy(() -> applicationDAO.update(application)).isInstanceOf(InvalidApplicationException.class)
        .hasMessage("Public ID cannot be '.' or '..'");
  }

  @Test
  public void testInsert_ValidatePublicIdIsDotDot() {
    Application app = new Application("..", "name", organization.getId());
    assertThatThrownBy(() -> applicationDAO.insert(app)).isInstanceOf(InvalidApplicationException.class)
        .hasMessage("Public ID cannot be '.' or '..'");
  }

  @Test
  public void testUpdate_ValidatePublicIdIsDotDot() {
    application.setPublicId("..");
    assertThatThrownBy(() -> applicationDAO.update(application)).isInstanceOf(InvalidApplicationException.class)
        .hasMessage("Public ID cannot be '.' or '..'");
  }

  @Test
  public void testInsert_ValidatePublicIdIsMaxLength() {
    String publicId = StringUtils.repeat("a", ApplicationDAO.MAX_PUBLIC_ID_LENGTH);
    Application app = new Application(publicId, "name", organization.getId());
    applicationDAO.insert(app);
    // No need to assert anything as this method throws an exception if not found
    applicationDAO.getByPublicIdNotNull(publicId);
  }

  @Test
  public void testUpdate_ValidatePublicIdIsMaxLength() {
    final String publicId = StringUtils.repeat("a", ApplicationDAO.MAX_PUBLIC_ID_LENGTH);
    application.setPublicId(publicId);
    applicationDAO.update(application);
    // No need to assert anything as this method throws an exception if not found
    applicationDAO.getByPublicIdNotNull(publicId);
  }

  @Test
  public void testInsert_ValidatePublicIdTooLong() {
    Application app = new Application(StringUtils.repeat("a", ApplicationDAO.MAX_PUBLIC_ID_LENGTH + 1), "name",
        organization.getId());
    assertThatThrownBy(() -> applicationDAO.insert(app)).isInstanceOf(InvalidNameException.class)
        .hasMessage("Public ID must be " + ApplicationDAO.MAX_PUBLIC_ID_LENGTH + " characters or less.");
  }

  @Test
  public void testUpdate_ValidatePublicIdTooLong() {
    application.setPublicId(StringUtils.repeat("a", ApplicationDAO.MAX_PUBLIC_ID_LENGTH + 1));
    assertThatThrownBy(() -> applicationDAO.update(application)).isInstanceOf(InvalidNameException.class)
        .hasMessage("Public ID must be " + ApplicationDAO.MAX_PUBLIC_ID_LENGTH + " characters or less.");
  }

  @Test
  public void testPublicIdIsCaseInsensitive() {
    String appPublicId = "testPublicIdIsCaseInsensitive";

    Application app = new Application(appPublicId, "test", organization.getId());
    applicationDAO.insert(app);
    String applicationId = app.getId();

    assertThat(app.getPublicId()).isEqualTo(appPublicId);
    assertThat(app.getPublicIdLowercase()).isEqualTo(appPublicId.toLowerCase(Locale.ENGLISH));

    app = applicationDAO.getById(applicationId);
    assertThat(app).isNotNull();
    assertThat(app.getPublicId()).isEqualTo(appPublicId);
    assertThat(app.getPublicIdLowercase()).isEqualTo(appPublicId.toLowerCase(Locale.ENGLISH));

    app = applicationDAO.getByPublicId(appPublicId);
    assertThat(app).isNotNull();
    assertThat(app.getId()).isEqualTo(applicationId);

    app = applicationDAO.getByPublicId(appPublicId.toLowerCase(Locale.ENGLISH));
    assertThat(app).isNotNull();
    assertThat(app.getId()).isEqualTo(applicationId);

    app = applicationDAO.getByPublicId(appPublicId.toUpperCase(Locale.ENGLISH));
    assertThat(app).isNotNull();
    assertThat(app.getId()).isEqualTo(applicationId);
  }

  @Test
  public void testInsert_DuplicatePublicId() {
    assertThatThrownBy(() -> tempEntity.newApplication(TemporaryEntity.uuid(), application.getPublicId(),
        organization.getId())).isInstanceOf(InvalidApplicationException.class)
        .hasMessage(application.getPublicId() + " is already used as an ID.");
  }

  @Test
  public void testUpdate_DuplicatePublicId() {
    final String duplicatePublicId = "duplicatePublicId";
    tempEntity.newApplicationWithParent(duplicatePublicId);

    application.setPublicId(duplicatePublicId);

    assertThatThrownBy(() -> applicationDAO.update(application)).isInstanceOf(InvalidApplicationException.class)
        .hasMessage(application.getPublicId() + " is already used as an ID.");
  }

  @Test
  @Override
  public void testInsert_ValidateNameLength() {
    String name = StringUtils.repeat("a", NameHelper.MAX_NAME_LENGTH_APP_ORG);
    Application app = new Application("publicId", name + "a", organization.getId());
    assertThatThrownBy(() -> applicationDAO.insert(app)).isInstanceOf(InvalidNameException.class)
        .hasMessage("Name must be " + NameHelper.MAX_NAME_LENGTH_APP_ORG + " characters or less.");

    app.setName(name);
    applicationDAO.insert(app);
  }

  @Test
  public void testDelete() {
    applicationDAO.delete(application);

    assertThat(applicationDAO.getByPublicId(application.getPublicId())).isNull();
  }

  @Test
  public void testGetApplicationsByContact() {
    final String contactName = "contactName";
    // Create some applications with and without contact name
    final int numApplications = 5;
    final Map<String, Application> expecteApplications = new HashMap<>();
    for (int i = 1; i <= numApplications; i++) {
      // Create some with contact name
      Application application = tempEntity.newApplication("app-with-contact-" + i, TemporaryEntity.uuid(),
          organization.getId(), contactName);
      expecteApplications.put(application.getId(), application);
      // Create some without
      tempEntity.newApplication(organization.getId());
    }

    final List<Application> applications = applicationDAO.getByContactInternalName(contactName);
    assertThat(applications).hasSize(numApplications);
    for (final Application app : applications) {
      validateApplication(app, expecteApplications.get(app.getId()));
    }
  }

  @Test
  public void testUpdate_ApplicationWithInvalidPublicId() {
    // Applications can have invalid public IDs if they were created before the public ID validation was introduced. It
    // should be possible to update these applications without changing the public ID (which is not allowed anyway).
    String invalidAppId = "App Public Id !@#$%^&*()";
    Application app = tempEntity.newApplicationWithInvalidPublicId(invalidAppId);
    String newName = app.getName() + " Updated";
    app.setName(newName);
    applicationDAO.update(app);
    app = applicationDAO.getById(app.getId());
    assertThat(app.getName()).isEqualTo(newName);
    assertThat(app.getPublicId()).isEqualTo(invalidAppId);
  }

  @Test
  public void testShouldAddSearchIndexChange() {
    Organization orgWithRepoManager = tempEntity.newOrganizationWithRepositoryManager("org-with-repo-manager");
    Application appWithRepoManager = tempEntity.newApplication(orgWithRepoManager.getId());

    Organization orgWithoutRepo = tempEntity.newOrganization("org-without-repo");
    Application appWithoutRepo = tempEntity.newApplication(orgWithoutRepo.getId());

    try (TransactionContext tx = applicationDAO.createTransactionContext()) {
      assertThat(applicationDAO.shouldAddSearchIndexChange(tx, appWithRepoManager)).isFalse();
      assertThat(applicationDAO.shouldAddSearchIndexChange(tx, appWithoutRepo)).isTrue();
    }
  }

  @Test
  public void testCRUD_RecordSearchIndexChange() {
    SystemConfigurationPropertyDAO systemConfigurationPropertyDAO = daoFactory.createSystemConfigurationPropertyDAO();
    systemConfigurationPropertyDAO.update(
        new SystemConfigurationProperty(SystemConfigurationProperty.ADVANCED_SEARCH_ENABLED, "true"));
    SearchIndexChangeDAO searchIndexChangeDAO = daoFactory.createSearchIndexChangeDAO();
    Organization org = tempEntity.newOrganization();
    searchIndexChangeDAO.getAll().forEach(searchIndexChangeDAO::delete);

    Application app = tempEntity.newApplication(org.getId());

    Organization orgWithRepo = tempEntity.newOrganizationWithRepositoryManager("org-with-repo-man");
    tempEntity.newApplication(orgWithRepo.getId());

    List<SearchIndexChange> searchIndexChanges = searchIndexChangeDAO.getAll();
    assertThat(searchIndexChanges).hasSize(1);
    assertThat(searchIndexChanges.get(0).getChangeType()).isEqualTo(ChangeType.APPLICATION);
    assertThat(searchIndexChanges.get(0).getChangeData()).isEqualTo(app.getId());
    searchIndexChangeDAO.delete(searchIndexChanges.get(0));

    applicationDAO.update(app);
    searchIndexChanges = searchIndexChangeDAO.getAll();
    assertThat(searchIndexChanges).hasSize(1);
    assertThat(searchIndexChanges.get(0).getChangeType()).isEqualTo(ChangeType.APPLICATION);
    assertThat(searchIndexChanges.get(0).getChangeData()).isEqualTo(app.getId());
    searchIndexChangeDAO.delete(searchIndexChanges.get(0));

    applicationDAO.delete(app);
    searchIndexChanges = searchIndexChangeDAO.getAll();
    assertThat(searchIndexChanges).hasSize(1);
    assertThat(searchIndexChanges.get(0).getChangeType()).isEqualTo(ChangeType.APPLICATION);
    assertThat(searchIndexChanges.get(0).getChangeData()).isEqualTo(app.getId());
  }

  // Cascade Delete Tests

  @Test
  public void testDelete_CascadesToLabels() {
    LabelDAO labelDAO = daoFactory.createLabelDAO();
    Label label = new Label(application.getId(), "testDelete_CascadesToLabels", Color.dark_blue);
    labelDAO.insert(label);

    applicationDAO.delete(application);
    assertThat(labelDAO.getByOwnerId(application.getId())).isEmpty();
  }

  @Test
  public void testDelete_CascadesToProprietaryConfig() {
    tempEntity.newProprietaryConfig(application.getId());

    applicationDAO.delete(application);
    ProprietaryConfigDAO proprietaryConfigDAO = daoFactory.createProprietaryConfigDAO();
    assertThat(proprietaryConfigDAO.getByOwnerId(application.getId())).isNull();
  }

  @Test
  public void testDelete_CascadesToPolicyWaivers() {
    Policy policy = tempEntity.newPolicy(application);
    PolicyWaiver policyWaiver = new PolicyWaiver("12345678901234567890", policy.getId(), application.getId(),
        "My comment");
    PolicyWaiverDAO policyWaiverDAO = daoFactory.createPolicyWaiverDAO();
    policyWaiverDAO.insert(policyWaiver);

    // sanity check
    List<PolicyWaiver> policyWaivers = policyWaiverDAO.getByOwnerId(application.getId());
    assertThat(policyWaivers).hasSize(1);

    applicationDAO.delete(application);
    policyWaivers = policyWaiverDAO.getByOwnerId(application.getId());
    assertThat(policyWaivers).isEmpty();
  }

  @Test
  public void testDelete_CascadesToPolicyWaiverRequests() {
    Policy policy = tempEntity.newPolicy(application);
    PolicyWaiverRequest policyWaiverRequest =
        new PolicyWaiverRequest("12345678901234567890", policy.getId(), application.getId(), "My comment");
    policyWaiverRequest.setPolicyViolationId("policyViolationId");
    tempEntity.newPolicyWaiverRequest(policyWaiverRequest);

    // sanity check
    PolicyWaiverRequestDAO policyWaiverRequestDAO = daoFactory.createPolicyWaiverRequestDAO();
    List<PolicyWaiverRequest> policyWaiverRequests = policyWaiverRequestDAO.getByOwnerId(application.getId());
    assertThat(policyWaiverRequests).hasSize(1);

    applicationDAO.delete(application);
    policyWaiverRequests = policyWaiverRequestDAO.getByOwnerId(application.getId());
    assertThat(policyWaiverRequests).isEmpty();
  }

  @Test
  public void testDelete_CascadesToPolicyEvaluations() {
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID,
        "testDelete_CascadesToPolicyEvaluations");

    applicationDAO.delete(application);
    PolicyEvaluationDAO policyEvaluationDAO = daoFactory.createPolicyEvaluationDAO();
    policyEvaluation = policyEvaluationDAO.getById(policyEvaluation.getId());
    assertThat(policyEvaluation).isNull();
  }

  @Test
  public void testDelete_CascadesToPolicyViolations() {
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID,
        "testDelete_CascadesToPolicyEvaluations");
    tempEntity.newPolicyViolation(policyEvaluation, tempEntity.newPolicy(application));

    applicationDAO.delete(application);

    PolicyViolationDAO policyViolationDAO = daoFactory.createPolicyViolationDAO();
    assertThat(policyViolationDAO.getByApplicationId(application.getId())).isEmpty();
  }

  @Test
  public void testDelete_CascadesToPolicies() {
    tempEntity.newPolicy(application);
    PolicyDAO policyDAO = daoFactory.createPolicyDAO();
    List<Policy> policies = policyDAO.getByOwnerId(application.getId());
    assertThat(policies).hasSize(1);

    applicationDAO.delete(application);
    policies = policyDAO.getByOwnerId(application.getId());
    assertThat(policies).isEmpty();
  }

  @Test
  public void testDelete_CascadesToPolicyOverrides() {
    Map<String, String> policyActionsOverrides = new HashMap<>();
    policyActionsOverrides.put("build", "warn");
    Policy policyWithOverrides = tempEntity.newPolicy(application.getOrganizationId());
    policyWithOverrides.addPolicyActionsOverride(application.getId(), policyActionsOverrides);
    policyWithOverrides.addPolicyActionsOverride("fakeOwnerId", policyActionsOverrides);
    Notifications policyNotificationsOverride = new Notifications();
    policyNotificationsOverride.add(new UserNotification("user@domain", BuildStageType.ID));
    policyWithOverrides.addPolicyNotificationsOverride(application.getId(), policyNotificationsOverride);
    policyWithOverrides.addPolicyNotificationsOverride("fakeOwnerId", policyNotificationsOverride);
    PolicyDAO policyDAO = daoFactory.createPolicyDAO();
    policyDAO.update(policyWithOverrides);

    applicationDAO.delete(application);

    Policy policy = policyDAO.getById(policyWithOverrides.getId());
    assertThat(policy.getPolicyActionsOverrides().keySet()).containsExactly("fakeOwnerId");
    assertThat(policy.getPolicyNotificationsOverrides().keySet()).containsExactly("fakeOwnerId");
  }

  @Test
  public void testDelete_CascadesToLicenseOverrides() {
    LicenseOverride licenseOverride = new LicenseOverride(application.getId(),
        ComponentIdentifier.createMavenCoordinates("groupId", "artifactId", "version"),
        LicenseOverrideStatus.OVERRIDDEN, "Apache-2.0", "My comment");
    LicenseOverrideDAO licenseOverrideDAO = daoFactory.createLicenseOverrideDAO();
    licenseOverrideDAO.insert(licenseOverride);
    List<LicenseOverride> licenseOverrides = licenseOverrideDAO.getByOwnerId(application.getId());
    assertThat(licenseOverrides).hasSize(1);

    applicationDAO.delete(application);
    licenseOverrides = licenseOverrideDAO.getByOwnerId(application.getId());
    assertThat(licenseOverrides).isEmpty();
  }

  @Test
  public void testDelete_CascadesToSecurityVulnerabilityOverrides() {
    SecurityVulnerabilityOverride securityVulnerabilityOverride = tempEntity.newSecurityVulnerabilityOverride(
        application.getId(), "hash", "source", "referenceId", SecurityVulnerabilityOverrideStatus.ACKNOWLEDGED);

    applicationDAO.delete(application);

    SecurityVulnerabilityOverrideDAO securityVulnerabilityOverrideDAO =
        daoFactory.createSecurityVulnerabilityOverrideDAO();
    assertThat(securityVulnerabilityOverrideDAO.getById(securityVulnerabilityOverride.getId())).isNull();
  }

  @Test
  public void testDelete_CascadesToMembershipMappings() {
    RoleDAO roleDAO = daoFactory.createRoleDAO();
    String roleId = roleDAO.getApplicationRoles().get(0).getId();
    MembershipMappingDAO membershipMappingDAO = daoFactory.createMembershipMappingDAO();
    membershipMappingDAO.setMembershipMappingsForContextAndRole(application.getId(), roleId,
        Collections.singletonList(new MembershipMapping("admin", MemberType.USER)));

    applicationDAO.delete(application);

    assertThat(membershipMappingDAO.getByContextId(application.getId())).isEmpty();
  }

  @Test
  public void testDelete_CascadesToApplicationTags() {
    Tag tag = tempEntity.newTag(organization.getId());

    ApplicationTagDAO appTagDAO = daoFactory.createApplicationTagDAO();
    ApplicationTag appTag = new ApplicationTag(application.getId(), tag.getId());
    appTagDAO.insert(appTag);

    applicationDAO.delete(application);

    assertThat(appTagDAO.getByApplicationId(application.getId())).isEmpty();
  }

  @Test
  public void testDelete_CascadesToApplicationComponents() {
    ApplicationComponent applicationComponent = tempEntity.newApplicationComponent(application.getId(),
        BuildStageType.ID, "hash", ComponentIdentifier.createMavenCoordinates("groupId", "artifactId", "version"));

    applicationDAO.delete(application);

    ApplicationComponentDAO applicationComponentDAO = daoFactory.createApplicationComponentDAO();
    assertThat(applicationComponentDAO.getById(applicationComponent.getId())).isNull();
  }

  @Test
  public void testDelete_CascadesToPolicyMonitoring() {
    PolicyMonitoringDAO policyMonitoringDAO = daoFactory.createPolicyMonitoringDAO();
    PolicyMonitoring policyMonitoring = new PolicyMonitoring(application.getId(), Stage.ID_RELEASE);
    policyMonitoringDAO.insert(policyMonitoring);
    List<String> apps = new ArrayList<>();
    apps.add(application.getId());
    assertThat(policyMonitoringDAO.getByOwnerId(application.getId()))
        .isNotEmpty()
        .hasSize(1)
        .extracting("ownerId")
        .isEqualTo(apps);

    applicationDAO.delete(application);

    assertThat(policyMonitoringDAO.getByOwnerId(application.getId())).isEmpty();
  }

  @Test
  public void testDelete_CascadesToPolicyViolationAggregations() {
    PolicyViolationAggregationDAO policyViolationAggregationDAO = daoFactory.createPolicyViolationAggregationDAO();
    PolicyViolationAggregation aggregation = tempEntity.newPolicyViolationAggregation(application.getId(), new Date());

    applicationDAO.delete(application);

    assertThat(policyViolationAggregationDAO.getById(aggregation.getId())).isNull();
  }

  @Test
  public void testDelete_CascadesToSourceControl() {
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, null, SourceControlProvider.GITHUB);
    SourceControl sourceControl = tempEntity.newSourceControl(
        application.getId(), "http://valid.sonatype.com/repository/project",
        "token", null);

    applicationDAO.delete(application);

    SourceControlDAO sourceControlDAO = daoFactory.createSourceControlDAO();
    assertThat(sourceControlDAO.getById(sourceControl.getId())).isNull();
  }

  @Test
  public void testDelete_CascadesToSourceControlEvent() {
    // given a source control event
    PolicyEvaluation sourcePolicyEvaluation =
        tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "sourceScan", "sourceCommit");

    SourceControlEvent sourceControlEvent =
        tempEntity.newSourceControlEvent(application, sourcePolicyEvaluation);

    SourceControlEventDAO sourceControlEventDAO = daoFactory.createSourceControlEventDAO();
    SourceControlEvent sourceControlEventByIdBeforeDelete = sourceControlEventDAO.getById(sourceControlEvent.getId());
    assertThat(sourceControlEventByIdBeforeDelete).isNotNull();

    // when we delete the application
    applicationDAO.delete(application);

    // then the source control event is deleted
    SourceControlEvent sourceControlEventByIAfterDelete = sourceControlEventDAO.getById(sourceControlEvent.getId());
    assertThat(sourceControlEventByIAfterDelete).isNull();
  }

  @Test
  public void testDelete_CascadesToSourceControlDefaultBranchCommitHistory() {
    PolicyEvaluation policyEvaluation =
        tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "testScanId");
    SourceControlDefaultBranchCommitHistory defaultBranchCommitHistory =
        tempEntity.newSourceControlDefaultBranchCommitHistory(application.getId(), "commit2", new Date(),
            policyEvaluation.getId());

    applicationDAO.delete(application);

    SourceControlDefaultBranchCommitHistoryDAO dao = daoFactory.createSourceControlDefaultBranchCommitHistoryDAO();
    assertThat(dao.getById(defaultBranchCommitHistory.getId())).isNull();
  }

  @Test
  public void testDelete_CascadesToSourceControlUser() {
    SourceControlUserDAO sourceControlUserDAO = daoFactory.createSourceControlUserDAO();
    SourceControlUser sourceControlUser = new SourceControlUser(application.getId(), "test@sonatype.com");
    sourceControlUserDAO.insert(sourceControlUser);

    applicationDAO.delete(application);

    assertThat(sourceControlUserDAO.getById(sourceControlUser.getId())).isNull();
  }

  @Test
  public void testDelete_CascadesToAutoPolicyWaivers() {
    AutoPolicyWaiver autoPolicyWaiverOne = new AutoPolicyWaiver(
        application.getId(),
        7,
        true,
        true,
        "creatorId",
        "creatorName",
        new Date()
    );
    AutoPolicyWaiver autoPolicyWaiverTwo = new AutoPolicyWaiver(
        "otherApp",
        7,
        true,
        true,
        "creatorId",
        "creatorName",
        new Date()
    );
    AutoPolicyWaiver autoPolicyWaiverThree = new AutoPolicyWaiver(
        "otherApp",
        7,
        true,
        true,
        "creatorId",
        "creatorName",
        new Date()
    );
    AutoPolicyWaiver autoPolicyWaiverFour = new AutoPolicyWaiver(
        application.getId(),
        7,
        true,
        true,
        "creatorId",
        "creatorName",
        new Date()
    );
    AutoPolicyWaiverDAO autoPolicyWaiverDAO = daoFactory.createAutoPolicyWaiverDAO();
    autoPolicyWaiverDAO.insert(autoPolicyWaiverOne);
    autoPolicyWaiverDAO.insert(autoPolicyWaiverTwo);
    autoPolicyWaiverDAO.insert(autoPolicyWaiverThree);
    autoPolicyWaiverDAO.insert(autoPolicyWaiverFour);
    List<AutoPolicyWaiver> testAppAutoPolicyWaivers = autoPolicyWaiverDAO.getByOwnerId(application.getId());
    assertThat(testAppAutoPolicyWaivers).hasSize(2);

    applicationDAO.delete(application);
    testAppAutoPolicyWaivers = autoPolicyWaiverDAO.getByOwnerId(application.getId());
    assertThat(testAppAutoPolicyWaivers).isEmpty();

    List<AutoPolicyWaiver> otherAppAutoPolicyWaivers = autoPolicyWaiverDAO.getByOwnerId("otherApp");
    assertThat(otherAppAutoPolicyWaivers).hasSize(2);
  }

  @Test
  public void testDelete_CascadesToInnerSource() {
    InnerSourceApplication innerSourceApplication = tempEntity.newInnerSourceApplication("pkg:test/name", application);

    applicationDAO.delete(application);

    InnerSourceApplicationDAO innerSourceApplicationDAO = daoFactory.createInnerSourceApplicationDAO();
    assertThat(innerSourceApplicationDAO.getById(innerSourceApplication.getId())).isNull();
  }

  @Test
  public void testDelete_CascadesToRepositoryConnections() {
    Application application = tempEntity.newApplicationWithParent();
    RepositoryConnection repositoryConnection = tempEntity.newRepositoryConnection(application.getId());

    applicationDAO.delete(application);

    RepositoryConnectionDAO repositoryConnectionDAO = daoFactory.createRepositoryConnectionDAO();
    assertThat(repositoryConnectionDAO.getById(repositoryConnection.getId())).isNull();
  }

  @Test
  public void testDelete_CascadesToSourceControlPullRequestResults() {
    SourceControlPullRequestResultDAO sourceControlPullRequestResultDAO =
        daoFactory.createSourceControlPullRequestResultDAO();
    Application application = tempEntity.newApplicationWithParent();
    tempEntity.newSourceControlPullRequestResult(application.getId(), "json1");
    tempEntity.newSourceControlPullRequestResult(application.getId(), "json2");
    SourceControlPullRequestResult entity =
        tempEntity.newSourceControlPullRequestResult(tempEntity.newApplicationWithParent().getId(), "json3");

    applicationDAO.delete(application);

    assertThat(sourceControlPullRequestResultDAO.getAll())
        .usingRecursiveFieldByFieldElementComparatorIgnoringFields(JPA.IGNORE_FIELDS)
        .containsExactly(entity);
  }

  @Test
  public void testDelete_CascadeToVulnerabilityCustomRemediation() {
    Application application = tempEntity.newApplicationWithParent();
    tempEntity.newVulnerabilityCustomData(application.getId(), "CVE-2022-1234",
        tempEntity.newTag(organization.getId()), "rem1",
        "testCWE", "testCvssVector1", 6.05F);
    tempEntity.newVulnerabilityCustomData(application.getId(), "CVE-2022-4321",
        tempEntity.newTag(organization.getId()), "rem2",
        "testCWE", "testCvssVector2", 6.05F);

    VulnerabilityCustomRemediationDAO vulnerabilityCustomRemediationDAO =
        daoFactory.createVulnerabilityCustomRemediationDAO();
    List<VulnerabilityCustomRemediation> vulnerabilityCustomRemediationList =
        vulnerabilityCustomRemediationDAO.getByOwnerId(application.getId());
    assertThat(vulnerabilityCustomRemediationList).extracting(VulnerabilityCustomRemediation::getRefId)
        .containsExactlyInAnyOrder("CVE-2022-1234", "CVE-2022-4321");
    applicationDAO.delete(application);
    vulnerabilityCustomRemediationList = vulnerabilityCustomRemediationDAO.getByOwnerId(application.getId());
    assertThat(vulnerabilityCustomRemediationList).isEmpty();
  }

  @Test
  public void testDelete_CascadeToSastScan() {
    final Application application = tempEntity.newApplicationWithParent();
    final SastScanDAO sastScanDAO = daoFactory.createSastScanDAO();
    final SastScan sastScan = tempEntity.newSastScan(application.getId());
    assertThat(sastScanDAO.getById(sastScan.getId())).isNotNull();

    applicationDAO.delete(application);

    assertThat(sastScanDAO.getById(sastScan.getId())).isNull();
  }

  @Test
  public void testDelete_CascadeToSastFinding() {
    final Application application = tempEntity.newApplicationWithParent();
    final SastScanDAO sastScanDAO = daoFactory.createSastScanDAO();
    final SastScan sastScan = tempEntity.newSastScan(application.getId());
    final SastFindingDAO sastFindingDAO = daoFactory.createSastFindingDAO();
    final SastFinding sastFinding = new SastFinding();
    sastFinding.setSastScanId(sastScan.getId());
    sastFinding.setCwe("CWE");
    sastFinding.setConfidence(SastFindingConfidence.MEDIUM);
    sastFinding.setSeverity(SastFindingSeverity.HIGH);
    sastFinding.setDescription("someDescription");
    sastFinding.setCoordinate("{\"namespace\":\"namespace\",\"name\":\"CWE\",\"methodName\":\"method\"}");
    sastFinding.setLineNumber(null);
    sastFinding.setRuleName("someRuleName");
    tempEntity.newSastFinding(sastFinding);
    assertThat(sastFindingDAO.getById(sastFinding.getId())).isNotNull();
    assertThat(sastScanDAO.getById(sastScan.getId())).isNotNull();

    applicationDAO.delete(application);
    assertThat(sastScanDAO.getById(sastScan.getId())).isNull();
    assertThat(sastFindingDAO.getById(sastFinding.getId())).isNull();
  }

  @Test
  public void testDelete_CascadeToVulnerabilityCustomCwe() {
    Application application = tempEntity.newApplicationWithParent();
    tempEntity.newVulnerabilityCustomData(application.getId(), "CVE-2022-1234",
        tempEntity.newTag(organization.getId()), "rem1",
        "testCWE", "testCvssVector1", 6.05F);
    tempEntity.newVulnerabilityCustomData(application.getId(), "CVE-2022-4321",
        tempEntity.newTag(organization.getId()), "rem1",
        "testCWE", "testCvssVector2", 6.05F);

    VulnerabilityCustomCweDAO vulnerabilityCustomCweDAO = daoFactory.createVulnerabilityCustomCweDAO();
    List<VulnerabilityCustomCwe> vulnerabilityCustomCweList = vulnerabilityCustomCweDAO
        .getByOwnerId(application.getId());
    assertThat(vulnerabilityCustomCweList).extracting(VulnerabilityCustomCwe::getRefId)
        .containsExactlyInAnyOrder("CVE-2022-1234", "CVE-2022-4321");
    applicationDAO.delete(application);
    assertThat(vulnerabilityCustomCweDAO.getByOwnerId(application.getId())).isEmpty();
  }

  @Test
  public void testDelete_CascadeToCVSSVector() {
    Application application = tempEntity.newApplicationWithParent();
    tempEntity.newVulnerabilityCustomData(application.getId(), "CVE-2022-1234",
        tempEntity.newTag(organization.getId()), "rem1",
        "testCWE", "testCvssVector1", 6.05F);
    tempEntity.newVulnerabilityCustomData(application.getId(), "CVE-2022-4321",
        tempEntity.newTag(organization.getId()), "rem1",
        "testCWE", "testCvssVector2", 6.05F);

    VulnerabilityCustomCvssVectorDAO vulnerabilityCustomCvssVectorDAO =
        daoFactory.createVulnerabilityCustomCvssVectorDAO();
    List<VulnerabilityCustomCvssVector> vulnerabilityCustomCvssVectorList =
        vulnerabilityCustomCvssVectorDAO.getByOwnerId(application.getId());
    assertThat(vulnerabilityCustomCvssVectorList).extracting(VulnerabilityCustomCvssVector::getRefId)
        .containsExactlyInAnyOrder("CVE-2022-1234", "CVE-2022-4321");
    applicationDAO.delete(application);
    assertThat(vulnerabilityCustomCvssVectorDAO.getByOwnerId(application.getId())).isEmpty();
  }

  @Test
  public void testDelete_CascadeToCVSSSeverity() {
    Application application = tempEntity.newApplicationWithParent();
    tempEntity.newVulnerabilityCustomData(application.getId(), "CVE-2022-1234",
        tempEntity.newTag(organization.getId()), "rem1",
        "testCWE", "testCvssVector1", 6.05F);
    tempEntity.newVulnerabilityCustomData(application.getId(), "CVE-2022-4321",
        tempEntity.newTag(organization.getId()), "rem1",
        "testCWE", "testCvssVector2", 6.05F);

    VulnerabilityCustomCvssSeverityDAO vulnerabilityCustomCvssSeverityDAO =
        daoFactory.createVulnerabilityCustomCvssSeverityDAO();
    List<VulnerabilityCustomCvssSeverity> vulnerabilityCustomCvssSeverityList =
        vulnerabilityCustomCvssSeverityDAO.getByOwnerId(application.getId());
    assertThat(vulnerabilityCustomCvssSeverityList).extracting(VulnerabilityCustomCvssSeverity::getRefId)
        .containsExactlyInAnyOrder("CVE-2022-1234", "CVE-2022-4321");
    applicationDAO.delete(application);
    assertThat(vulnerabilityCustomCvssSeverityDAO.getByOwnerId(application.getId())).isEmpty();
  }

  @Test
  public void testGetByOrganizationIds() {
    assertThat(applicationDAO.getByOrganizationIds(Sets.newHashSet(tempEntity.newOrganization().getId()))).isEmpty();

    Organization org1 = tempEntity.newOrganization("org-1");
    Application app11 = tempEntity.newApplication(org1.getId());
    Application app12 = tempEntity.newApplication(org1.getId());

    Organization org2 = tempEntity.newOrganization("org-2");
    Application app21 = tempEntity.newApplication(org2.getId());
    Application app22 = tempEntity.newApplication(org2.getId());

    assertThat(applicationDAO.getByOrganizationIds(Sets.newHashSet(org1.getId())))
        .extracting(Application::getId)
        .containsExactlyInAnyOrder(app11.getId(), app12.getId());

    assertThat(applicationDAO.getByOrganizationIds(Sets.newHashSet(org2.getId())))
        .extracting(Application::getId)
        .containsExactlyInAnyOrder(app21.getId(), app22.getId());

    assertThat(applicationDAO.getByOrganizationIds(Sets.newHashSet(org1.getId(), org2.getId())))
        .extracting(Application::getId)
        .containsExactlyInAnyOrder(app11.getId(), app12.getId(), app21.getId(), app22.getId());
  }

  @Test
  public void testDelete_CascadesToSbomThirdPartyEntities() {
    String appVersion = "1.2.3";
    String sbomSpec = "spdx";
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    ThirdPartySbomMetadata thirdPartySbomMetadata = tempEntity.newSbomEvaluation(app, appVersion, sbomSpec,
        new PackageUrlIdentifier("pkg:maven/com.h2database/h2@1.4.200?type=jar"), "12345deadbeef",
        false, PENDING);

    ThirdPartyFileCoordinateDAO thirdPartyFileCoordinateDAO = daoFactory.createThirdPartyFileCoordinateDAO();
    ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO = daoFactory.createThirdPartySbomMetadataDAO();
    ThirdPartyScanDAO thirdPartyScanDAO = daoFactory.createThirdPartyScanDAO();
    ThirdPartyFileDAO thirdPartyFileDAO = daoFactory.createThirdPartyFileDAO();

    String thirdPartyFileId = thirdPartySbomMetadata.getThirdPartyFileId();
    assertThat(thirdPartyFileId).isNotNull();
    List<ThirdPartyFileCoordinate> thirdPartyFileCoordinateList =
        thirdPartyFileCoordinateDAO.getByThirdPartyFileId(thirdPartyFileId);
    assertThat(thirdPartyFileCoordinateList.size()).isEqualTo(1);
    ThirdPartyScan thirdPartyScan = thirdPartyScanDAO.getByThirdPartyFileId(thirdPartyFileId);
    assertThat(thirdPartyScan).isNotNull();
    ThirdPartyFile thirdPartyFile = thirdPartyFileDAO.getById(thirdPartyFileId);
    assertThat(thirdPartyFile).isNotNull();

    applicationDAO.delete(app);

    thirdPartyFile = thirdPartyFileDAO.getById(thirdPartyFileId);
    assertThat(thirdPartyFile).isNull();

    thirdPartyFileCoordinateList =
        thirdPartyFileCoordinateDAO.getByThirdPartyFileId(thirdPartyFileId);
    assertThat(thirdPartyFileCoordinateList).isEmpty();

    thirdPartySbomMetadata = thirdPartySbomMetadataDAO.getByThirdPartyFileId(thirdPartyFileId);
    assertThat(thirdPartySbomMetadata).isNull();
  }

  @Test
  public void testDelete_CascadesToCpeMatchingConfiguration() {
    Organization organization = tempEntity.newOrganization();
    Application application = tempEntity.newApplication(organization.getPublicId());
    CpeMatchingConfiguration cpeMatchingConfiguration = new CpeMatchingConfiguration(application.getId(), true, false);
    CpeMatchingConfigurationDAO cpeMatchingConfigurationDao = daoFactory.createCpeMatchingConfigurationDAO();

    // create
    cpeMatchingConfigurationDao.insert(cpeMatchingConfiguration);

    // delete application
    applicationDAO.delete(application);

    // verify deletion
    assertThat(cpeMatchingConfigurationDao.getByOwnerId(cpeMatchingConfiguration.getOwnerId())).isNull();
  }

  @Test
  public void testDelete_CascadesToSourceControlPullRequestComments() {
    Application application = tempEntity.newApplicationWithParent();
    
    // Create policy evaluations (required for SourceControlPullRequestComment foreign keys)
    PolicyEvaluation sourcePolicyEvaluation = tempEntity.newPolicyEvaluation(
        application.getId(), BuildStageType.ID, "scanId1", ClientScanType.SONATYPE);
    PolicyEvaluation targetPolicyEvaluation = tempEntity.newPolicyEvaluation(
        application.getId(), BuildStageType.ID, "scanId2", ClientScanType.SONATYPE);
    
    // Create SourceControlPullRequestComment entities
    tempEntity.newSourceControlPullRequestComment(
        application.getId(),
        1, // pullRequestId
        101, // pullRequestCommentId
        1, // pullRequestCommentVersion
        "hash1", // contentHash
        sourcePolicyEvaluation.getId(), // sourcePolicyEvaluationId
        targetPolicyEvaluation.getId() // targetPolicyEvaluationId
    );

    tempEntity.newSourceControlPullRequestComment(
        application.getId(),
        2, // pullRequestId
        102, // pullRequestCommentId
        1, // pullRequestCommentVersion
        "hash2", // contentHash
        sourcePolicyEvaluation.getId(), // sourcePolicyEvaluationId
        targetPolicyEvaluation.getId() // targetPolicyEvaluationId
    );

    // Verify the comments exist
    SourceControlPullRequestCommentDAO pullRequestCommentDAO = daoFactory.createSourceControlPullRequestCommentDAO();
    List<SourceControlPullRequestComment> commentsBeforeDelete = 
        pullRequestCommentDAO.getByApplicationId(application.getId());
    assertThat(commentsBeforeDelete).hasSize(2);

    // Delete application - this should also clean up the pull request comments
    applicationDAO.delete(application);

    // Verify the comments were deleted
    List<SourceControlPullRequestComment> commentsAfterDelete = 
        pullRequestCommentDAO.getByApplicationId(application.getId());
    assertThat(commentsAfterDelete).isEmpty();
  }

  @Test
  public void testGetByAncestorId() {
    assertThat(applicationDAO.getByAncestorId(ROOT_ORGANIZATION_ID))
        .extracting(Application::getId)
        .containsExactlyInAnyOrder(application.getId());

    assertThat(applicationDAO.getByAncestorId(tempEntity.newOrganization().getId())).isEmpty();

    Organization org1 = tempEntity.newOrganization("org-1");
    Application app11 = tempEntity.newApplication(org1.getId());
    Application app12 = tempEntity.newApplication(org1.getId());

    Organization org11 = tempEntity.newOrganization("org-1-1", org1);
    Application app111 = tempEntity.newApplication(org11.getId());
    Application app112 = tempEntity.newApplication(org11.getId());

    Organization org2 = tempEntity.newOrganization("org-2");
    Application app21 = tempEntity.newApplication(org2.getId());
    Application app22 = tempEntity.newApplication(org2.getId());

    assertThat(applicationDAO.getByAncestorId(ROOT_ORGANIZATION_ID))
        .extracting(Application::getId)
        .containsExactlyInAnyOrder(
            application.getId(),
            app11.getId(), app12.getId(),
            app111.getId(), app112.getId(),
            app21.getId(), app22.getId());

    assertThat(applicationDAO.getByAncestorId(org1.getId()))
        .extracting(Application::getId)
        .containsExactlyInAnyOrder(app11.getId(), app12.getId(), app111.getId(), app112.getId());

    assertThat(applicationDAO.getByAncestorId(org11.getId()))
        .extracting(Application::getId)
        .containsExactlyInAnyOrder(app111.getId(), app112.getId());

    assertThat(applicationDAO.getByAncestorId(org2.getId()))
        .extracting(Application::getId)
        .containsExactlyInAnyOrder(app21.getId(), app22.getId());

    // querying directly by app id should not return that app nor anything else
    assertThat(applicationDAO.getByAncestorId(app11.getId())).isEmpty();
  }

  @Test
  public void testGetIdsByAncestorIds() {
    Organization org0 = tempEntity.newOrganization();

    Organization org1 = tempEntity.newOrganization();
    Application app11 = tempEntity.newApplication("app11", org1.getId());
    Application app12 = tempEntity.newApplication("app12", org1.getId());

    Organization org2 = tempEntity.newOrganization();
    Organization org21 = tempEntity.newOrganization(org2);
    Application app211 = tempEntity.newApplication("app211", org21.getId());
    Application app212 = tempEntity.newApplication("app212", org21.getId());

    Organization org3 = tempEntity.newOrganization();
    Application app31 = tempEntity.newApplication("app31", org3.getId());
    Application app32 = tempEntity.newApplication("app32", org3.getId());
    Organization org31 = tempEntity.newOrganization(org3);
    Application app311 = tempEntity.newApplication("app311", org31.getId());
    Application app312 = tempEntity.newApplication("app312", org31.getId());

    assertThat(applicationDAO.getIdsByAncestorIds(Collections.emptySet()))
        .isEmpty();

    assertThat(applicationDAO.getIdsByAncestorIds(Collections.singleton(org0.getId())))
        .isEmpty();

    assertThat(applicationDAO.getIdsByAncestorIds(Collections.singleton(org1.getId())))
        .containsExactlyInAnyOrder(app11.getId(), app12.getId());

    assertThat(applicationDAO.getIdsByAncestorIds(Collections.singleton(org2.getId())))
        .containsExactlyInAnyOrder(app211.getId(), app212.getId());

    assertThat(applicationDAO.getIdsByAncestorIds(Collections.singleton(org3.getId())))
        .containsExactlyInAnyOrder(app31.getId(), app311.getId(), app312.getId(), app32.getId());

    assertThat(applicationDAO.getIdsByAncestorIds(new HashSet<>(Arrays.asList(org3.getId(), org31.getId()))))
        .containsExactlyInAnyOrder(app31.getId(), app311.getId(), app312.getId(), app32.getId());

    assertThat(applicationDAO.getIdsByAncestorIds(
        new HashSet<>(Arrays.asList(org3.getId(), org31.getId(), app11.getId(), app31.getId(), app311.getId()))))
        .containsExactlyInAnyOrder(app11.getId(), app31.getId(), app311.getId(), app312.getId(), app32.getId());

    applicationDAO.delete(application);

    assertThat(applicationDAO.getIdsByAncestorIds(Collections.singleton(ROOT_ORGANIZATION_ID)))
        .containsExactlyInAnyOrder(app11.getId(), app12.getId(), app211.getId(), app212.getId(), app31.getId(),
            app311.getId(), app312.getId(), app32.getId());

    assertThat(applicationDAO.getIdsByAncestorIds(
        new HashSet<>(Arrays.asList(ROOT_ORGANIZATION_ID, org31.getId(), app11.getId()))))
        .containsExactlyInAnyOrder(app11.getId(), app12.getId(), app211.getId(), app212.getId(), app31.getId(),
            app311.getId(), app312.getId(), app32.getId());
  }

  @Test
  public void testGetIdsByAncestorIds_Limit_H2() {
    testGetIdsByAncestorIds_Limit();
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testGetIdsByAncestorIds_Limit_Postgres() {
    testGetIdsByAncestorIds_Limit();
  }

  private void testGetIdsByAncestorIds_Limit() {
    Set<String> ids = new HashSet<>();
    // Go above both H2 (2,000) and postgres (65,535) limits
    // (Short.MAX_VALUE * 2) + 2 = 65,536
    for (int i = 1; i <= (Short.MAX_VALUE * 2) + 2; i++) {
      ids.add(TemporaryEntity.uuid());
    }

    Application app1 = tempEntity.newApplicationWithParent("app1", "app1");
    Application app2 = tempEntity.newApplicationWithParent("app2", "app2");
    Application app3 = tempEntity.newApplicationWithParent("app3", "app3");

    assertThat(applicationDAO.getIdsByAncestorIds(ids))
        .isEmpty();

    assertThat(applicationDAO.getIdsByAncestorIds(Sets.union(Collections.singleton(app1.getId()), ids)))
        .containsExactly(app1.getId());

    assertThat(applicationDAO.getIdsByAncestorIds(Sets.union(new LinkedHashSet<>(
        Arrays.asList(app2.getId(), app3.getId(), app1.getId())), ids)))
        .containsExactlyInAnyOrder(app1.getId(), app2.getId(), app3.getId());
  }

  @Test
  public void testGetAll_Paged() {
    applicationDAO.delete(application);
    Application app1 = tempEntity.newApplicationWithParent("app1", "app1");
    Application app2 = tempEntity.newApplicationWithParent("app2", "app2");
    Application app3 = tempEntity.newApplicationWithParent("app3", "app3");

    assertThat(applicationDAO.getAll(1, 0)).isEmpty();

    assertThat(applicationDAO.getAll(1, 10))
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactly(app1, app2, app3);

    assertThat(applicationDAO.getAll(1, 2))
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactly(app1, app2);
    assertThat(applicationDAO.getAll(2, 2))
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactly(app3);
    assertThat(applicationDAO.getAll(3, 2)).isEmpty();
  }

  @Test
  public void testGetByAncestorIds_Paged_H2() {
    testGetByAncestorIds_Paged();
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testGetByAncestorIds_Paged_Postgres() {
    testGetByAncestorIds_Paged();
  }

  private void testGetByAncestorIds_Paged() {
    Application app1 = tempEntity.newApplicationWithParent("app1", "app1");
    Application app2 = tempEntity.newApplicationWithParent("app2", "app2");
    Application app3 = tempEntity.newApplicationWithParent("app3", "app3");

    assertThat(applicationDAO.getByAncestorIds(Collections.emptySet(), 1, 10)).isEmpty();
    assertThat(applicationDAO.getByAncestorIds(Collections.singleton(app1.getId()), 1, 0))
        .isEmpty();

    assertThat(applicationDAO.getByAncestorIds(Collections.singleton(app1.getId()), 1, 10))
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactly(app1);

    assertThat(applicationDAO.getByAncestorIds(new LinkedHashSet<>(
        Arrays.asList(app2.getId(), app3.getId(), app1.getId())), 1, 10))
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactly(app1, app2, app3);

    assertThat(applicationDAO.getByAncestorIds(new LinkedHashSet<>(
        Arrays.asList(app2.getId(), app3.getId(), app1.getId())), 1, 2))
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactly(app1, app2);
    assertThat(applicationDAO.getByAncestorIds(new LinkedHashSet<>(
        Arrays.asList(app2.getId(), app3.getId(), app1.getId())), 2, 2))
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactly(app3);
    assertThat(applicationDAO.getByAncestorIds(new LinkedHashSet<>(
        Arrays.asList(app2.getId(), app3.getId(), app1.getId())), 3, 2))
        .isEmpty();
  }

  @Test
  public void testGetByAncestorIds_Hierarchy_H2() {
    testGetByAncestorIds_Hierarchy();
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testGetByAncestorIds_Hierarchy_Postgres() {
    testGetByAncestorIds_Hierarchy();
  }

  private void testGetByAncestorIds_Hierarchy() {
    Organization org0 = tempEntity.newOrganization();

    Organization org1 = tempEntity.newOrganization();
    Application app11 = tempEntity.newApplication("app11", org1.getId());
    Application app12 = tempEntity.newApplication("app12", org1.getId());

    Organization org2 = tempEntity.newOrganization();
    Organization org21 = tempEntity.newOrganization(org2);
    Application app211 = tempEntity.newApplication("app211", org21.getId());
    Application app212 = tempEntity.newApplication("app212", org21.getId());

    Organization org3 = tempEntity.newOrganization();
    Application app31 = tempEntity.newApplication("app31", org3.getId());
    Application app32 = tempEntity.newApplication("app32", org3.getId());
    Organization org31 = tempEntity.newOrganization(org3);
    Application app311 = tempEntity.newApplication("app311", org31.getId());
    Application app312 = tempEntity.newApplication("app312", org31.getId());

    assertThat(applicationDAO.getByAncestorIds(Collections.emptySet(), 1, 10))
        .isEmpty();

    assertThat(applicationDAO.getByAncestorIds(Collections.singleton(org0.getId()), 1, 10))
        .isEmpty();

    assertThat(applicationDAO.getByAncestorIds(Collections.singleton(org1.getId()), 1, 10))
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactly(app11, app12);

    assertThat(applicationDAO.getByAncestorIds(Collections.singleton(org2.getId()), 1, 10))
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactly(app211, app212);

    assertThat(applicationDAO.getByAncestorIds(Collections.singleton(org3.getId()), 1, 10))
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactly(app31, app311, app312, app32);

    assertThat(applicationDAO.getByAncestorIds(new LinkedHashSet<>(Arrays.asList(org3.getId(), org31.getId())), 1, 10))
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactly(app31, app311, app312, app32);

    assertThat(applicationDAO.getByAncestorIds(
        new HashSet<>(Arrays.asList(org3.getId(), org31.getId(), app11.getId(), app31.getId(), app311.getId())), 1, 10))
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactly(app11, app31, app311, app312, app32);

    applicationDAO.delete(application);

    assertThat(applicationDAO.getByAncestorIds(Collections.singleton(ROOT_ORGANIZATION_ID), 1, 10))
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactly(app11, app12, app211, app212, app31, app311, app312, app32);

    assertThat(
        applicationDAO.getByAncestorIds(
            new HashSet<>(Arrays.asList(ROOT_ORGANIZATION_ID, org31.getId(), app11.getId())), 1, 10))
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactly(app11, app12, app211, app212, app31, app311, app312, app32);
  }

  @Test
  public void testGetByAncestorIds_Limit_H2() {
    testGetByAncestorIds_Limit();
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testGetByAncestorIds_Limit_Postgres() {
    testGetByAncestorIds_Limit();
  }

  // For postgres, this is to show we will avoid
  // <openjpa-3.2.2-re5933d6 fatal general error> org.apache.openjpa.persistence.PersistenceException: PreparedStatement
  // can have at most 65,535 parameters. Please consider using arrays, or splitting the query in several ones, or using
  // COPY. Given query has 65,536 parameters
  private void testGetByAncestorIds_Limit() {
    Set<String> ids = new HashSet<>();
    // Go above both H2 (2,000) and postgres (65,535) limits
    // (Short.MAX_VALUE * 2) + 2 = 65,536
    for (int i = 1; i <= (Short.MAX_VALUE * 2) + 2; i++) {
      ids.add(TemporaryEntity.uuid());
    }

    Application app1 = tempEntity.newApplicationWithParent("app1", "app1");
    Application app2 = tempEntity.newApplicationWithParent("app2", "app2");
    Application app3 = tempEntity.newApplicationWithParent("app3", "app3");

    assertThat(applicationDAO.getByAncestorIds(ids, 1, 10)).isEmpty();
    assertThat(applicationDAO.getByAncestorIds(Sets.union(Collections.singleton(app1.getId()), ids), 1, 0))
        .isEmpty();

    assertThat(applicationDAO.getByAncestorIds(Sets.union(Collections.singleton(app1.getId()), ids), 1, 10))
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactly(app1);

    assertThat(applicationDAO.getByAncestorIds(Sets.union(new LinkedHashSet<>(
        Arrays.asList(app2.getId(), app3.getId(), app1.getId())), ids), 1, 10))
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactly(app1, app2, app3);

    assertThat(applicationDAO.getByAncestorIds(Sets.union(new LinkedHashSet<>(
        Arrays.asList(app2.getId(), app3.getId(), app1.getId())), ids), 1, 2))
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactly(app1, app2);
    assertThat(applicationDAO.getByAncestorIds(Sets.union(new LinkedHashSet<>(
        Arrays.asList(app2.getId(), app3.getId(), app1.getId())), ids), 2, 2))
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactly(app3);
    assertThat(applicationDAO.getByAncestorIds(Sets.union(new LinkedHashSet<>(
        Arrays.asList(app2.getId(), app3.getId(), app1.getId())), ids), 3, 2))
        .isEmpty();
  }

  @Test
  public void testGetByIdOrPublicId() {
    Application application = tempEntity.newApplicationWithParent();
    tempEntity.newApplicationWithParent();

    assertThat(applicationDAO.getByIdOrPublicId(null)).isNull();
    assertThat(applicationDAO.getByIdOrPublicId("")).isNull();
    assertThat(applicationDAO.getByIdOrPublicId(" ")).isNull();
    assertThat(applicationDAO.getByIdOrPublicId(application.getId()))
        .usingRecursiveComparison()
        .isEqualTo(application);
    assertThat(applicationDAO.getByIdOrPublicId(application.getPublicId()))
        .usingRecursiveComparison()
        .isEqualTo(application);
    assertThat(applicationDAO.getByIdOrPublicId(application.getPublicId().toLowerCase(Locale.ENGLISH)))
        .usingRecursiveComparison()
        .isEqualTo(application);
    assertThat(applicationDAO.getByIdOrPublicId(application.getPublicId().toUpperCase(Locale.ENGLISH)))
        .usingRecursiveComparison()
        .isEqualTo(application);
    assertThat(applicationDAO.getByIdOrPublicId(" " + application.getPublicId() + " "))
        .usingRecursiveComparison()
        .isEqualTo(application);
  }

  @Test
  public void testGetByIdOrPublicIdNotNull() {
    Application application = tempEntity.newApplicationWithParent();
    tempEntity.newApplicationWithParent();

    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> applicationDAO.getByIdOrPublicIdNotNull(null));
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> applicationDAO.getByIdOrPublicIdNotNull(""));
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> applicationDAO.getByIdOrPublicIdNotNull(" "));
    assertThat(applicationDAO.getByIdOrPublicIdNotNull(application.getId()))
        .usingRecursiveComparison()
        .isEqualTo(application);
    assertThat(applicationDAO.getByIdOrPublicIdNotNull(application.getPublicId()))
        .usingRecursiveComparison()
        .isEqualTo(application);
    assertThat(applicationDAO.getByIdOrPublicIdNotNull(application.getPublicId().toLowerCase(Locale.ENGLISH)))
        .usingRecursiveComparison()
        .isEqualTo(application);
    assertThat(applicationDAO.getByIdOrPublicIdNotNull(application.getPublicId().toUpperCase(Locale.ENGLISH)))
        .usingRecursiveComparison()
        .isEqualTo(application);
    assertThat(applicationDAO.getByIdOrPublicIdNotNull(" " + application.getPublicId() + " "))
        .usingRecursiveComparison()
        .isEqualTo(application);
  }

  @Test
  public void testGetDashboardApplicationRisk_H2DatabaseNotSupported() {
    assertThatThrownBy(
        () -> applicationDAO.getDashboardApplicationRisk(Collections.emptySet(), Collections.emptySet(),
            Collections.emptySet(),1, 10, Collections.emptySet(),
            "total_risk_per_stage_unique", "DESC", 0, 100))
        .hasMessage("This operation is only supported for PostgreSQL databases")
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testGetDashboardApplicationRisk_Filters() {
    Policy app1Policy = tempEntity.newPolicy(application.getId(), "app owned policy", 5);
    PolicyEvaluation app1PolicyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID,
        "test scan app1 id", new Date());
    tempEntity.newPolicyViolation(app1PolicyEvaluation, app1Policy);

    List<ApplicationRiskDTO> result =
        applicationDAO.getDashboardApplicationRisk(Set.of(application.getId()),
            Set.of(BuildStageType.ID, SourceStageType.ID, ReleaseStageType.ID), Collections.emptySet(),
            1, 10, Collections.emptySet(),
            "total_risk_per_stage_unique", "DESC", 0, 100);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).applicationName()).isEqualTo("AbstractDbDAOTest-AppName");
    assertThat(result.get(0).totalRiskPerStage()).isEqualTo(5);
    assertThat(result.get(0).criticalPerStage()).isEqualTo(0);
    assertThat(result.get(0).severePerStage()).isEqualTo(5);
    assertThat(result.get(0).moderatePerStage()).isEqualTo(0);
    assertThat(result.get(0).lowPerStage()).isEqualTo(0);

    assertThat(result.get(0).totalRiskPerStageUnique()).isEqualTo(5);
    assertThat(result.get(0).criticalPerStageUnique()).isEqualTo(0);
    assertThat(result.get(0).severePerStageUnique()).isEqualTo(5);
    assertThat(result.get(0).moderatePerStageUnique()).isEqualTo(0);
    assertThat(result.get(0).lowPerStageUnique()).isEqualTo(0);
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testGetDashboardApplicationRisk_Pages() {
    Policy app1Policy = tempEntity.newPolicy(application.getId(), "app owned policy", 5);
    PolicyEvaluation app1PolicyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID,
        "test scan app1 id", new Date());
    tempEntity.newPolicyViolation(app1PolicyEvaluation, app1Policy);

    Application app2 = tempEntity.newApplication("tsta-app2", "tsta-app2", organization.getId());
    Policy policy2 = tempEntity.newPolicy(app2.getId(), "app owned policy2", 5);
    PolicyEvaluation policyEvaluation2 = tempEntity.newPolicyEvaluation(app2.getId(), BuildStageType.ID,
        "test scan app id2", new Date());
    tempEntity.newPolicyViolation(policyEvaluation2, policy2);

    Application app3 = tempEntity.newApplication("tsta-app3", "tsta-app3", organization.getId());
    Policy policy3 = tempEntity.newPolicy(app3.getId(), "app owned policy3", 5);
    PolicyEvaluation policyEvaluation3 = tempEntity.newPolicyEvaluation(app3.getId(), BuildStageType.ID,
        "test scan app id3", new Date());
    tempEntity.newPolicyViolation(policyEvaluation3, policy3);

    List<ApplicationRiskDTO> result =
        applicationDAO.getDashboardApplicationRisk(Set.of(application.getId(), app2.getId(), app3.getId()),
            Set.of(BuildStageType.ID), Collections.emptySet(),
            1, 10, Collections.emptySet(),
            "total_risk_per_stage_unique", "DESC", 0, 2);
    assertThat(result).hasSize(3);
    assertThat(result.get(0).applicationName()).isEqualTo("AbstractDbDAOTest-AppName");
    assertThat(result.get(1).applicationName()).isEqualTo("tsta-app2");
    // an extra app is returned to know if there is a next page
    assertThat(result.get(2).applicationName()).isEqualTo("tsta-app3");

    result =
        applicationDAO.getDashboardApplicationRisk(Set.of(application.getId(), app2.getId(), app3.getId()),
            Set.of(BuildStageType.ID), Collections.emptySet(),
            1, 10, Collections.emptySet(),
            "total_risk_per_stage_unique", "DESC", 1, 2);
    assertThat(result).hasSize(1);

    result =
        applicationDAO.getDashboardApplicationRisk(Set.of(application.getId(), app2.getId(), app3.getId()),
            Set.of(BuildStageType.ID), Collections.emptySet(),
            1, 10, Collections.emptySet(),
            "total_risk_per_stage_unique", "DESC", 2, 2);
    assertThat(result).isEmpty();
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testGetDashboardApplicationRisk_EmptyResultWhenNoMatchWithFilter() {
    Policy app1Policy = tempEntity.newPolicy(application.getId(), "app owned policy", 5);
    PolicyEvaluation app1PolicyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID,
        "test scan app1 id", new Date());
    tempEntity.newPolicyViolation(app1PolicyEvaluation, app1Policy);

    List<ApplicationRiskDTO> result =
        applicationDAO.getDashboardApplicationRisk(Set.of("non-existent-app-id"),
            Set.of(BuildStageType.ID), Collections.emptySet(),
            1, 10, Collections.emptySet(),
            "total_risk_per_stage_unique", "DESC", 0, 100);

    assertThat(result).isEmpty();
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testGetDashboardApplicationRisk_EmptyResultWhenThereIsNoAppId() {
    Policy app1Policy = tempEntity.newPolicy(application.getId(), "app owned policy", 5);
    PolicyEvaluation app1PolicyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID,
        "test scan app1 id", new Date());
    tempEntity.newPolicyViolation(app1PolicyEvaluation, app1Policy);

    List<ApplicationRiskDTO> result =
        applicationDAO.getDashboardApplicationRisk(Collections.emptySet(),
            Collections.emptySet(), Collections.emptySet(),
            1, 10, Collections.emptySet(),
            "total_risk_per_stage_unique", "DESC", 0, 100);

    assertThat(result).isEmpty();
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testGetDashboardApplicationRisk_SortCaseInsensitive() {
    Policy app1Policy = tempEntity.newPolicy(application.getId(), "app1", 5);
    PolicyEvaluation app1PolicyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID,
        "test scan app1 id", new Date());
    tempEntity.newPolicyViolation(app1PolicyEvaluation, app1Policy);

    Application app2 = tempEntity.newApplication("app2", "app2", organization.getId());
    Policy policy2 = tempEntity.newPolicy(app2.getId(), "app owned policy2", 5);
    PolicyEvaluation policyEvaluation2 = tempEntity.newPolicyEvaluation(app2.getId(), BuildStageType.ID,
        "test scan app id2", new Date());
    tempEntity.newPolicyViolation(policyEvaluation2, policy2);

    Application app3 = tempEntity.newApplication("Sandbox-app", "Sandbox-app", organization.getId());
    Policy policy3 = tempEntity.newPolicy(app3.getId(), "app owned policy3", 5);
    PolicyEvaluation policyEvaluation3 = tempEntity.newPolicyEvaluation(app3.getId(), BuildStageType.ID,
        "test scan app id3", new Date());
    tempEntity.newPolicyViolation(policyEvaluation3, policy3);

    List<ApplicationRiskDTO> result =
        applicationDAO.getDashboardApplicationRisk(Set.of(application.getId(), app2.getId(), app3.getId()),
            Set.of(BuildStageType.ID), Collections.emptySet(),
            1, 10, Collections.emptySet(),
            "name", "ASC", 0, 100);
    assertThat(result).hasSize(3);
    assertThat(result.get(0).applicationName()).isEqualTo("AbstractDbDAOTest-AppName");
    assertThat(result.get(1).applicationName()).isEqualTo("app2");
    assertThat(result.get(2).applicationName()).isEqualTo("Sandbox-app");
  }

  /**
   * Setup method for application repository URL tests
   */
  private void setupRepositoryUrlTests() {
    // set root org source control
    tempEntity.newSourceControl(organization.getParentOrganizationId(), null, "token", SourceControlProvider.GITLAB);

    // Create app1 with repo1
    tempEntity.newApplicationWithSpecificId("app1", "application 1", "app1", organization.getId());
    tempEntity.newSourceControl("app1", "http://test.gitlab.com/org/repo1.git");

    // Create app2 with repo2
    tempEntity.newApplicationWithSpecificId("app2", "application 2", "app2", organization.getId());
    tempEntity.newSourceControl("app2", "http://test.gitlab.com/org/repo2.git");

    // Create app3 with repo3
    tempEntity.newApplicationWithSpecificId("app3", "application 3", "app3", organization.getId());
    tempEntity.newSourceControl("app3", "http://test.gitlab.com/org/repo3.git");

    // Create additional apps that share repo1 to test multiple apps per URL
    tempEntity.newApplicationWithSpecificId("app1b", "application 1b", "app1b", organization.getId());
    tempEntity.newSourceControl("app1b", "http://test.gitlab.com/org/repo1.git");

    tempEntity.newApplicationWithSpecificId("app1c", "application 1c", "app1c", organization.getId());
    tempEntity.newSourceControl("app1c", "http://test.gitlab.com/org/repo1.git");
  }

  @Test
  public void testGetApplicationIdsByNormalizedRepositoryUrls_Success() {
    // given: a set of applications with repository URLs (with some URLs having multiple apps)
    setupRepositoryUrlTests();

    final String normalizedURL1 = "http://test.gitlab.com/org/repo1";
    final String normalizedURL2 = "http://test.gitlab.com/org/repo2";
    final String normalizedURL3 = "http://test.gitlab.com/org/repo3";

    // when: Get application IDs for multiple repository URLs
    Map<String, SortedSet<String>> urlToAppIdsMap = applicationDAO.getApplicationIdsByNormalizedRepositoryUrls(
        Set.of(normalizedURL1, normalizedURL2, normalizedURL3)
    );

    // then: Verify the mapping is correct
    assertThat(urlToAppIdsMap).hasSize(3);
    // URL1 has multiple applications
    assertThat(urlToAppIdsMap.get(normalizedURL1))
        .containsExactlyInAnyOrder("app1", "app1b", "app1c");
    // URL2 has only one application
    assertThat(urlToAppIdsMap.get(normalizedURL2))
        .containsExactly("app2");
    // URL3 has only one application
    assertThat(urlToAppIdsMap.get(normalizedURL3))
        .containsExactly("app3");
  }

  @Test
  public void testGetApplicationIdsByNormalizedRepositoryUrls_NonExistentUrl() {
    // given: a set of applications and a non-existent repository URL
    setupRepositoryUrlTests();

    final String repositoryURL1 = "http://test.gitlab.com/org/repo1";
    final String nonExistentRepoURL = "http://test.gitlab.com/org/nonexistent";

    // Normalize the repository URLs as the method expects normalized URLs
    String normalizedURL1 = SourceControl.normalizeRepositoryUrl(repositoryURL1);
    String normalizedNonExistentURL = SourceControl.normalizeRepositoryUrl(nonExistentRepoURL);

    // when: Include a non-existent repository URL
    Set<String> repoUrls = new HashSet<>();
    repoUrls.add(normalizedURL1);
    repoUrls.add(normalizedNonExistentURL);

    // then: Expected exception should be thrown
    assertThatThrownBy(() -> applicationDAO.getApplicationIdsByNormalizedRepositoryUrls(repoUrls))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Repository URLs not found");
  }

  @Test
  public void testGetApplicationIdsByNormalizedRepositoryUrls_EmptySet() {
    // given: an empty set of repository URLs
    setupRepositoryUrlTests();

    // when: Call method with empty set
    Map<String, SortedSet<String>> urlToAppIdsMap =
        applicationDAO.getApplicationIdsByNormalizedRepositoryUrls(Collections.emptySet());

    // then: Result should be empty map
    assertThat(urlToAppIdsMap).isEmpty();
  }

  @Test
  public void testGetCountWithoutRelatedRepositories() {
    // Create an app without a related repository manager
    tempEntity.newApplications(organization.getId(), 1);

    // Create an app with both a related repository manager and repository
    Organization orgWithRelatedRepo = tempEntity.newOrganizationWithRepositoryManager("org-with-repo");
    tempEntity.newApplications(orgWithRelatedRepo.getId(), 1);

    assertThat(applicationDAO.getCountWithoutRelatedRepositories()).isEqualTo(2);
  }
  
  @Test
  public void testGetApplicationsCountByOrganizationIds() {
    Organization org1 = tempEntity.newOrganization();
    Organization org2 = tempEntity.newOrganization();
    
    tempEntity.newApplication(org1.getId());
    tempEntity.newApplication(org1.getId());
    tempEntity.newApplication(org2.getId());
    
    assertThat(applicationDAO.getApplicationsCountByOrganizationId(org1.getId())).isEqualTo(2);
  }

  private void validateApplication(Application actualApp, Application expectedApp) {
    assertThat(actualApp.getName()).isEqualTo(expectedApp.getName());
    assertThat(actualApp.getContactInternalName()).isEqualTo(expectedApp.getContactInternalName());
    assertThat(actualApp.getOrganizationId()).isEqualTo(expectedApp.getOrganizationId());
    assertThat(actualApp.getPublicId()).isEqualTo(expectedApp.getPublicId());
  }

  private void assertApplications(List<Application> actual, List<Application> expected) {
    actual.sort(new ApplicationComparator());
    expected.sort(new ApplicationComparator());

    for (int i = 0; i < actual.size(); i++) {
      Application actualApplication = actual.get(i);
      Application expectedApplication = expected.get(i);
      assertThat(actualApplication.getId()).isEqualTo(expectedApplication.getId());
      assertThat(actualApplication.getName()).isEqualTo(expectedApplication.getName());
      assertThat(actualApplication.getOrganizationId()).isEqualTo(expectedApplication.getOrganizationId());
      assertThat(actualApplication.getPublicId()).isEqualTo(expectedApplication.getPublicId());
      assertThat(actualApplication.getPublicIdLowercase()).isEqualTo(expectedApplication.getPublicIdLowercase());
      assertThat(actualApplication.getContactInternalName()).isEqualTo(expectedApplication.getContactInternalName());
      assertThat(actualApplication.getNameLowercaseNoWhitespace())
          .isEqualTo(expectedApplication.getNameLowercaseNoWhitespace());
    }
  }

  static class ApplicationComparator
      implements Comparator<Application>
  {
    @Override
    public int compare(final Application o1, final Application o2) {
      return o1.getId().compareTo(o2.getId());
    }
  }
}
