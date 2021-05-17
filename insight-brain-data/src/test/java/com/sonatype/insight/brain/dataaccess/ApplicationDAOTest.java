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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.configuration.ProprietaryConfigDAO;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.dataaccess.innersource.InnerSourceComponentDAO;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseOverrideDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyMonitoringDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.security.MembershipMappingDAO;
import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDefaultBranchCommitHistoryDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlEventDAO;
import com.sonatype.insight.brain.dataaccess.successmetrics.PolicyViolationAggregationDAO;
import com.sonatype.insight.brain.dataaccess.tag.ApplicationTagDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.SecurityVulnerabilityOverrideDAO;
import com.sonatype.insight.brain.db.DataSourceFactory;
import com.sonatype.insight.brain.db.OperationalDataStoreProvider;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ApplicationComponent;
import com.sonatype.insight.brain.model.Color;
import com.sonatype.insight.brain.model.InvalidNameException;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.model.NameHelperTest;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.SearchIndexChange;
import com.sonatype.insight.brain.model.SearchIndexChange.ChangeType;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.innersource.InnerSourceComponent;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyMonitoring;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlDefaultBranchCommitHistory;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.model.successmetrics.PolicyViolationAggregation;
import com.sonatype.insight.brain.model.tag.ApplicationTag;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverride;
import com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverrideStatus;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.postgres.PostgresServer;
import com.sonatype.nexus.scm.SourceControlProvider;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ApplicationDAOTest
    extends AbstractDbDAOTest
{
  /** Prohibited application public ID whitespace characters. */
  public static final char[] PUBLIC_ID_WHITESPACE_CHARS = { '\t', '\n', '\u000B', '\f', '\r' };

  private ApplicationDAO applicationDAO = new ApplicationDAO();

  @Rule
  public TemporaryFolder tmpDir = new TemporaryFolder();

  @Test
  public void testCRUD() throws Exception {
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
  public void testGetAll() throws Exception {
    // Create a few apps
    int appCount = 3;
    tempEntity.newApplications(organization.getId(), appCount);

    // getAll should return appCount + 1, to account for app created by AbstractDbDAOTest
    assertThat(applicationDAO.getAll()).hasSize(appCount + 1);
  }

  @Test
  public void testGetApplicationsByPublicIds() throws Exception {
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
  public void testGetApplicationsByPublicIds_EmptySet() throws Exception {
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
    Set<String> applicationIds = repoApps.stream().map(p -> p.getId()).collect(Collectors.toSet());
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
  public void testGetApplicationsByTagIds() throws Exception {
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
    assertApplications(applications, Arrays.asList(taggedApplication));
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
  public void testUpdateOrganizationId() {
    Organization organization1 = tempEntity.newOrganization("testUpdateOrganizationId 1");

    // Update with a different organization id - should fail
    application.setOrganizationId(organization1.getId());
    assertThatThrownBy(() -> {
      applicationDAO.update(application);
    }).isInstanceOf(InvalidApplicationException.class)
        .hasMessage("Cannot change the parent organization of an application.");
  }

  @Test
  public void testUpdateOrganizationId_Force() {
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
  public void testValidatePublicIdValidChars_Insert() {
    for (String publicId : NameHelperTest.VALID_NAMES) {
      tempEntity.newApplication(tempEntity.uuid(), publicId.replaceAll("\\s", ""), organization.getId());
    }
  }

  @Test
  public void testValidatePublicIdValidChars_Update() {
    for (String publicId : NameHelperTest.VALID_NAMES) {
      application.setPublicId(publicId.replaceAll("\\s", ""));
      applicationDAO.update(application);
    }
  }

  @Test
  public void testValidatePublicIdInvalidChars_Insert() {
    Application app = new Application(null, "name", organization.getId());
    for (String publicId : NameHelperTest.INVALID_CHARACTERS) {
      app.setPublicId(publicId);
      assertThatThrownBy(() -> {
        applicationDAO.insert(app);
      }).isInstanceOf(InvalidNameException.class).hasMessage(NameHelper.INVALID_CHAR_MESSAGE, "Public ID",
          publicId.charAt(0));
    }
  }

  @Test
  public void testValidatePublicIdInvalidChars_Update() {
    for (String publicId : NameHelperTest.INVALID_CHARACTERS) {
      application.setPublicId(publicId);
      assertThatThrownBy(() -> {
        applicationDAO.update(application);
      }).isInstanceOf(InvalidNameException.class).hasMessage(NameHelper.INVALID_CHAR_MESSAGE, "Public ID",
          publicId.charAt(0));
    }
  }

  @Test
  public void testValidateNullPublicId_Insert() {
    Application app = new Application(null, "name", organization.getId());
    assertThatThrownBy(() -> {
      applicationDAO.insert(app);
    }).isInstanceOf(InvalidNameException.class).hasMessage("Public ID is required.");
  }

  @Test
  public void testValidateNullPublicId_Update() {
    application.setPublicId(null);
    assertThatThrownBy(() -> {
      applicationDAO.update(application);
    }).isInstanceOf(InvalidNameException.class).hasMessage("Public ID is required.");
  }

  @Test
  public void testValidateEmptyPublicId_Insert() {
    Application app = new Application("", "name", organization.getId());
    assertThatThrownBy(() -> {
      applicationDAO.insert(app);
    }).isInstanceOf(InvalidNameException.class).hasMessage("Public ID is required.");
  }

  @Test
  public void testValidateEmptyPublicId_Update() {
    application.setPublicId("");
    assertThatThrownBy(() -> {
      applicationDAO.update(application);
    }).isInstanceOf(InvalidNameException.class).hasMessage("Public ID is required.");
  }

  @Test
  public void testValidatePublicIdWithWhitespaces_Insert() {
    Application app = new Application(null, "name", organization.getId());
    for (char invalidChar : PUBLIC_ID_WHITESPACE_CHARS) {
      app.setPublicId("foo" + invalidChar + "bar");
      assertThatThrownBy(() -> {
        applicationDAO.insert(app);
      }).isInstanceOfAny(InvalidApplicationException.class, InvalidNameException.class)
          .satisfies(e -> assertThat(e.getMessage()).isIn("Public ID cannot contain whitespaces.",
              String.format(NameHelper.INVALID_CHAR_MESSAGE, "Public ID", invalidChar)));
    }
  }

  @Test
  public void testValidatePublicIdWithWhitespaces_Update() {
    for (char invalidChar : PUBLIC_ID_WHITESPACE_CHARS) {
      application.setPublicId("foo" + invalidChar + "bar");
      assertThatThrownBy(() -> {
        applicationDAO.update(application);
      }).isInstanceOfAny(InvalidApplicationException.class, InvalidNameException.class)
          .satisfies(e -> assertThat(e.getMessage()).isIn("Public ID cannot contain whitespaces.",
              String.format(NameHelper.INVALID_CHAR_MESSAGE, "Public ID", invalidChar)));
    }
  }

  @Test
  public void testValidatePublicIdIsDot_Insert() {
    Application app = new Application(".", "name", organization.getId());
    assertThatThrownBy(() -> {
      applicationDAO.insert(app);
    }
    ).isInstanceOf(InvalidApplicationException.class).hasMessage("Public ID cannot be '.' or '..'");
  }

  @Test
  public void testValidatePublicIdIsDot_Update() {
    application.setPublicId(".");
    assertThatThrownBy(() -> {
      applicationDAO.update(application);
    }).isInstanceOf(InvalidApplicationException.class).hasMessage("Public ID cannot be '.' or '..'");
  }

  @Test
  public void testValidatePublicIdIsDotDot_Insert() {
    Application app = new Application("..", "name", organization.getId());
    assertThatThrownBy(() -> {
      applicationDAO.insert(app);
    }).isInstanceOf(InvalidApplicationException.class).hasMessage("Public ID cannot be '.' or '..'");
  }

  @Test
  public void testValidatePublicIdIsDotDot_Update() {
    application.setPublicId("..");
    assertThatThrownBy(() -> {
      applicationDAO.update(application);
    }).isInstanceOf(InvalidApplicationException.class).hasMessage("Public ID cannot be '.' or '..'");
  }

  @Test
  public void testValidatePublicIdIsMaxLength_Insert() {
    String publicId = StringUtils.repeat("a", ApplicationDAO.MAX_PUBLIC_ID_LENGTH);
    Application app = new Application(publicId, "name", organization.getId());
    applicationDAO.insert(app);
    // No need to assert anything as this method throws an exception if not found
    applicationDAO.getByPublicIdNotNull(publicId);
  }

  @Test
  public void testValidatePublicIdIsMaxLength_Update() {
    final String publicId = StringUtils.repeat("a", ApplicationDAO.MAX_PUBLIC_ID_LENGTH);
    application.setPublicId(publicId);
    applicationDAO.update(application);
    // No need to assert anything as this method throws an exception if not found
    applicationDAO.getByPublicIdNotNull(publicId);
  }

  @Test
  public void testValidatePublicIdTooLong_Insert() {
    Application app = new Application(StringUtils.repeat("a", ApplicationDAO.MAX_PUBLIC_ID_LENGTH + 1), "name",
        organization.getId());
    assertThatThrownBy(() -> {
      applicationDAO.insert(app);
    }).isInstanceOf(InvalidNameException.class)
        .hasMessage("Public ID must be " + ApplicationDAO.MAX_PUBLIC_ID_LENGTH + " characters or less.");
  }

  @Test
  public void testValidatePublicIdTooLong_Update() {
    application.setPublicId(StringUtils.repeat("a", ApplicationDAO.MAX_PUBLIC_ID_LENGTH + 1));
    assertThatThrownBy(() -> {
      applicationDAO.update(application);
    }).isInstanceOf(InvalidNameException.class)
        .hasMessage("Public ID must be " + ApplicationDAO.MAX_PUBLIC_ID_LENGTH + " characters or less.");
  }

  @Test
  public void testPublicIdIsCaseInsensitive() {
    String appPublicId = "testPublicIdIsCaseInsensitive";

    Application app = new Application(appPublicId, "test", organization.getId());
    ApplicationDAO applicationDAO = new ApplicationDAO();
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
  public void testValidateNullName_Insert() {
    Application app = new Application("publicId", null, organization.getId());
    assertThatThrownBy(() -> {
      applicationDAO.insert(app);
    }).isInstanceOf(InvalidNameException.class).hasMessage("Name is required.");
  }

  @Test
  public void testValidateNullName_Update() {
    Application app = new Application("publicId", "testValidateNullName", organization.getId());
    assertThat(app.getNameLowercaseNoWhitespace()).isEqualTo("testvalidatenullname");
    applicationDAO.insert(app);

    app.setName(null);
    assertThat(app.getNameLowercaseNoWhitespace()).isNull();
    assertThatThrownBy(() -> {
      applicationDAO.update(app);
    }).isInstanceOf(InvalidNameException.class).hasMessage("Name is required.");
  }

  @Test
  public void testValidateEmptyName_Insert() {
    Application app = new Application("publicId", " ", organization.getId());
    assertThatThrownBy(() -> {
      applicationDAO.insert(app);
    }).isInstanceOf(InvalidNameException.class).hasMessage("Name is required.");
  }

  @Test
  public void testValidateEmptyName_Update() {
    Application app = new Application("publicId", "testValidateEmptyName", organization.getId());
    assertThat(app.getNameLowercaseNoWhitespace()).isEqualTo("testvalidateemptyname");
    applicationDAO.insert(app);

    app.setName(" ");
    assertThat(app.getNameLowercaseNoWhitespace()).isEqualTo("");
    assertThatThrownBy(() -> {
      applicationDAO.update(app);
    }).isInstanceOf(InvalidNameException.class).hasMessage("Name is required.");
  }

  @Test
  public void testValidateNameInvalidChars_Insert() {
    Application app = new Application("publicId", "name", organization.getId());
    for (String name : NameHelperTest.INVALID_CHARACTERS) {
      app.setName(name);
      assertThatThrownBy(() -> {
        applicationDAO.insert(app);
      }).isInstanceOf(InvalidNameException.class).hasMessage(NameHelper.INVALID_CHAR_MESSAGE, "Name", name.charAt(0));
    }
  }

  @Test
  public void testValidateNameInvalidChars_Update() {
    for (String name : NameHelperTest.INVALID_CHARACTERS) {
      application.setName(name);
      assertThatThrownBy(() -> {
        applicationDAO.update(application);
      }).isInstanceOf(InvalidNameException.class).hasMessage(NameHelper.INVALID_CHAR_MESSAGE, "Name", name.charAt(0));
    }
  }

  @Test
  public void testValidateNameValidChars_Insert() {
    for (String name : NameHelperTest.VALID_NAMES) {
      tempEntity.newApplication(name, tempEntity.uuid(), organization.getId());
    }
  }

  @Test
  public void testValidateNameValidChars_Update() {
    Application app = tempEntity.newApplication("a", "publicId", organization.getId());
    for (String name : NameHelperTest.VALID_NAMES) {
      app.setName(name);
      applicationDAO.update(app);
    }
  }

  @Test
  public void testValidateNameSpaces_Insert() {
    Application app = new Application("publicId", "name", organization.getId());
    for (String name : NameHelperTest.INVALID_SPACING_NAMES) {
      app.setName(name);
      assertThatThrownBy(() -> {
        applicationDAO.insert(app);
      }).isInstanceOf(InvalidNameException.class)
          .hasMessage("Name must not have leading or trailing spaces, or have two spaces in a row.");
    }
  }

  @Test
  public void testValidateNameSpaces_Update() {
    for (String name : NameHelperTest.INVALID_SPACING_NAMES) {
      application.setName(name);
      assertThatThrownBy(() -> {
        applicationDAO.update(application);
      }).isInstanceOf(InvalidNameException.class)
          .hasMessage("Name must not have leading or trailing spaces, or have two spaces in a row.");
    }
  }

  @Test
  public void testNameIsCaseAndWhitespaceInsensitive() {
    String name = "test string With Case and Whitespace";

    Application app = tempEntity.newApplication(name, "publicId", organization.getId());

    assertThat(app.getName()).isEqualTo(name);
    assertThat(app.getNameLowercaseNoWhitespace()).isEqualTo("teststringwithcaseandwhitespace");

    String name1 = "TEST String      With    cASE and      whitespace";
    Application application1 = applicationDAO.getByName(name1);
    assertThat(application1).isNotNull();
    assertThat(application1.getId()).isEqualTo(app.getId());
  }

  @Test
  public void testDuplicatePublicId_Insert() {
    assertThatThrownBy(() -> {
      tempEntity.newApplication(tempEntity.uuid(), application.getPublicId(), organization.getId());
    }).isInstanceOf(InvalidApplicationException.class)
        .hasMessage(application.getPublicId() + " is already used as an ID.");
  }

  @Test
  public void testDuplicatePublicId_Update() {
    final String duplicatePublicId = "duplicatePublicId";
    tempEntity.newApplicationWithParent(duplicatePublicId);

    application.setPublicId(duplicatePublicId);

    assertThatThrownBy(() -> {
      applicationDAO.update(application);
    }).isInstanceOf(InvalidApplicationException.class)
        .hasMessage(application.getPublicId() + " is already used as an ID.");
  }

  @Test
  public void testDuplicateName_Insert() {
    tempEntity.newApplication("testDuplicateName", "publicId", organization.getId());
    assertThatThrownBy(() -> {
      tempEntity.newApplication("Test Duplicate Name", "publicId2", organization.getId());
    }).isInstanceOf(InvalidNameException.class).hasMessage("Test Duplicate Name is already used as a name.");
  }

  @Test
  public void testDuplicateName_Update() {
    tempEntity.newApplication("testDuplicateName", "publicId", organization.getId());

    Application application1 = tempEntity.newApplication(application.getOrganizationId());
    application1.setName("Test Duplicate Name");
    assertThatThrownBy(() -> {
      applicationDAO.update(application1);
    }).isInstanceOf(InvalidNameException.class).hasMessage("Test Duplicate Name is already used as a name.");
  }

  @Test
  public void testDelete_CascadesToLabels() {
    LabelDAO labelDAO = new LabelDAO();
    Label label = new Label(application.getId(), "testDelete_CascadesToLabels", Color.dark_blue);
    labelDAO.insert(label);

    applicationDAO.delete(application);
    assertThat(labelDAO.getByOwnerId(application.getId())).isEmpty();
  }

  @Test
  public void testDelete_CascadesToProprietaryConfig() {
    tempEntity.newProprietaryConfig(application.getId());

    applicationDAO.delete(application);
    assertThat(new ProprietaryConfigDAO().getByOwnerId(application.getId())).isNull();
  }

  @Test
  public void testDelete_CascadesToPolicyWaivers() {
    Policy policy = tempEntity.newPolicy(application);
    PolicyWaiver policyWaiver = new PolicyWaiver("12345678901234567890", policy.getId(), application.getId(),
        "My comment");
    PolicyWaiverDAO policyWaiverDAO = new PolicyWaiverDAO();
    policyWaiverDAO.insert(policyWaiver);
    List<PolicyWaiver> policyWaivers = policyWaiverDAO.getActiveByOwnerId(application.getId());
    assertThat(policyWaivers).hasSize(1);

    applicationDAO.delete(application);
    policyWaivers = policyWaiverDAO.getActiveByOwnerId(application.getId());
    assertThat(policyWaivers).isEmpty();
  }

  @Test
  public void testDelete_CascadesToPolicyEvaluations() {
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID,
        "testDelete_CascadesToPolicyEvaluations");

    applicationDAO.delete(application);
    policyEvaluation = new PolicyEvaluationDAO().getById(policyEvaluation.getId());
    assertThat(policyEvaluation).isNull();
  }

  @Test
  public void testDelete_CascadesToPolicyViolations() {
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID,
        "testDelete_CascadesToPolicyEvaluations");
    tempEntity.newPolicyViolation(policyEvaluation, tempEntity.newPolicy(application));

    applicationDAO.delete(application);

    assertThat(new PolicyViolationDAO().getByApplicationId(application.getId())).isEmpty();
  }

  @Test
  public void testDelete_CascadesToPolicies() {
    tempEntity.newPolicy(application);
    PolicyDAO policyDAO = new PolicyDAO();
    List<Policy> policies = policyDAO.getByOwnerId(application.getId());
    assertThat(policies).hasSize(1);

    applicationDAO.delete(application);
    policies = policyDAO.getByOwnerId(application.getId());
    assertThat(policies).isEmpty();
  }

  @Test
  public void testDelete_CascadesToLicenseOverrides() {
    LicenseOverride licenseOverride = new LicenseOverride(application.getId(),
        ComponentIdentifier.createMavenCoordinates("groupId", "artifactId", "version"),
        LicenseOverrideStatus.OVERRIDDEN, "Apache-2.0", "My comment");
    LicenseOverrideDAO licenseOverrideDAO = new LicenseOverrideDAO();
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
        application.getId(), "hash", "source", "refrenceId", SecurityVulnerabilityOverrideStatus.ACKNOWLEDGED);

    applicationDAO.delete(application);

    assertThat(new SecurityVulnerabilityOverrideDAO().getById(securityVulnerabilityOverride.getId())).isNull();
  }

  @Test
  public void testDelete_CascadesToMembershipMappings() {
    String roleId = new RoleDAO().getApplicationRoles().get(0).getId();
    MembershipMappingDAO membershipMappingDAO = new MembershipMappingDAO();
    membershipMappingDAO.setMembershipMappingsForContextAndRole(application.getId(), roleId,
        Arrays.asList(new MembershipMapping("admin", MemberType.USER)));

    applicationDAO.delete(application);

    assertThat(membershipMappingDAO.getByContextId(application.getId())).isEmpty();
  }

  @Test
  public void testDelete_CascadesToApplicationTags() {
    Tag tag = tempEntity.newTag(organization.getId());

    ApplicationTagDAO appTagDAO = new ApplicationTagDAO();
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

    assertThat(new ApplicationComponentDAO().getById(applicationComponent.getId())).isNull();
  }

  @Test
  public void testValidateNameLength_Insert() {
    String name = StringUtils.repeat("a", NameHelper.MAX_NAME_LENGTH_APP_ORG);
    Application app = new Application("publicId", name + "a", organization.getId());
    assertThatThrownBy(() -> {
      applicationDAO.insert(app);
    }).isInstanceOf(InvalidNameException.class)
        .hasMessage("Name must be " + NameHelper.MAX_NAME_LENGTH_APP_ORG + " characters or less.");

    app.setName(name);
    applicationDAO.insert(app);
  }

  @Test
  public void testValidateNameLength_Update() {
    String name = StringUtils.repeat("a", NameHelper.MAX_NAME_LENGTH_APP_ORG);
    application.setName(name + "a");
    assertThatThrownBy(() -> {
      applicationDAO.update(application);
    }).isInstanceOf(InvalidNameException.class)
        .hasMessage("Name must be " + NameHelper.MAX_NAME_LENGTH_APP_ORG + " characters or less.");

    application.setName(name);
    applicationDAO.update(application);
  }

  @Test
  public void testDelete_CascadesToPolicyMonitoring() {
    PolicyMonitoringDAO policyMonitoringDAO = new PolicyMonitoringDAO();
    PolicyMonitoring policyMonitoring = new PolicyMonitoring(application.getId(), Stage.ID_RELEASE);
    policyMonitoringDAO.insert(policyMonitoring);
    assertThat(policyMonitoringDAO.getByOwnerId(application.getId())).isNotNull();

    applicationDAO.delete(application);

    assertThat(policyMonitoringDAO.getByOwnerId(application.getId())).isNull();
  }

  @Test
  public void testDelete_CascadesToPolicyViolationAggregations() {
    PolicyViolationAggregationDAO policyViolationAggregationDAO = new PolicyViolationAggregationDAO();
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

    assertThat(new SourceControlDAO().getById(sourceControl.getId())).isNull();
  }

  @Test
  public void testDelete_CascadesToSourceControlEvent() {
    // given a source control event
    PolicyEvaluation sourcePolicyEvaluation =
        tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "sourceScan", "sourceCommit");

    SourceControlEvent sourceControlEvent =
        tempEntity.newSourceControlEvent(application, sourcePolicyEvaluation);

    SourceControlEventDAO sourceControlEventDAO = new SourceControlEventDAO();
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

    SourceControlDefaultBranchCommitHistoryDAO dao = new SourceControlDefaultBranchCommitHistoryDAO();
    assertThat(dao.getById(defaultBranchCommitHistory.getId())).isNull();
  }

  @Test
  public void testDelete_CascadesToLocks_H2() {
    Application otherApplication = tempEntity.newApplicationWithParent();
    // Lock for policy violations
    try (ClusterLock clusterLock = ClusterLock.createForPolicyViolations(application)) {
      clusterLock.lock();
    }
    assertThat(
        ClusterLock.LOCKS_BY_ID.get(ClusterLock.getLockIdForPolicyViolations(application)))
        .isNotNull();

    // Lock for policy violation aggregations
    try (ClusterLock clusterLock = ClusterLock.createForPolicyViolationAggregations(application.getId())) {
      clusterLock.lock();
    }
    assertThat(ClusterLock.LOCKS_BY_ID
        .get(ClusterLock.getLockIdForPolicyViolationAggregations(application.getId()))).isNotNull();

    // Locks for application reports
    String scanId1 = "scanId1";
    String scanId2 = "scanId2";
    String scanId3 = "scanId3";
    ClusterLock.createForPolicyEvaluation(application, scanId1);
    ClusterLock.createForPolicyEvaluation(application, scanId2);
    ClusterLock.createForPolicyEvaluation(otherApplication, scanId3);
    assertThat(ClusterLock.LOCKS_BY_ID.get(ClusterLock.getLockIdForPolicyEvaluation(application, scanId1))).isNotNull();
    assertThat(ClusterLock.LOCKS_BY_ID.get(ClusterLock.getLockIdForPolicyEvaluation(application, scanId2))).isNotNull();
    assertThat(ClusterLock.LOCKS_BY_ID.get(ClusterLock.getLockIdForPolicyEvaluation(otherApplication, scanId3)))
        .isNotNull();
    
    // Lock for audit json file store
    try (ClusterLock clusterLock = ClusterLock.createForAuditJsonFileStore(application.getId())) {
      clusterLock.lock();
    }
    assertThat(ClusterLock.LOCKS_BY_ID.get(ClusterLock.getLockIdForAuditJsonFileStore(application.getId())))
        .isNotNull();

    applicationDAO.delete(application);

    assertThat(
        ClusterLock.LOCKS_BY_ID.get(ClusterLock.getLockIdForPolicyViolations(application)))
        .isNull();
    assertThat(ClusterLock.LOCKS_BY_ID
        .get(ClusterLock.getLockIdForPolicyViolationAggregations(application.getId()))).isNull();
    assertThat(ClusterLock.LOCKS_BY_ID.get(ClusterLock.getLockIdForPolicyEvaluation(application, scanId1))).isNull();
    assertThat(ClusterLock.LOCKS_BY_ID.get(ClusterLock.getLockIdForPolicyEvaluation(application, scanId2))).isNull();
    assertThat(ClusterLock.LOCKS_BY_ID.get(ClusterLock.getLockIdForPolicyEvaluation(otherApplication, scanId3)))
        .isNotNull();
    assertThat(ClusterLock.LOCKS_BY_ID.get(ClusterLock.getLockIdForAuditJsonFileStore(application.getId()))).isNull();
  }

  @Test
  public void testDelete_CascadesToLocks_Postgres() {
    DataSourceFactory.clear_ForTestsOnly();
    try (PostgresServer postgres = new PostgresServer()) {
      OperationalDataStoreProvider.init(postgres.getDatabaseConfig(), false);
      LockDAO dao = new LockDAO();
      ApplicationDAO applicationDAO = new ApplicationDAO();
      Application application = tempEntity.newApplicationWithParent();
      Application otherApplication = tempEntity.newApplicationWithParent();

      // Lock for policy violations
      try (ClusterLock clusterLock = ClusterLock.createForPolicyViolations(application)) {
        clusterLock.lock();
      }
      assertThat(dao.getById(ClusterLock.getLockIdForPolicyViolations(application))).isNotNull();

      // Lock for policy violation aggregations
      try (ClusterLock clusterLock = ClusterLock.createForPolicyViolationAggregations(application.getId())) {
        clusterLock.lock();
      }
      assertThat(dao.getById(ClusterLock.getLockIdForPolicyViolationAggregations(application.getId())))
          .isNotNull();

      // Locks for application reports
      String scanId1 = "scanId1";
      String scanId2 = "scanId2";
      String scanId3 = "scanId3";
      ClusterLock.createForPolicyEvaluation(application, scanId1);
      ClusterLock.createForPolicyEvaluation(application, scanId2);
      ClusterLock.createForPolicyEvaluation(otherApplication, scanId3);
      assertThat(dao.getById(ClusterLock.getLockIdForPolicyEvaluation(application, scanId1))).isNotNull();
      assertThat(dao.getById(ClusterLock.getLockIdForPolicyEvaluation(application, scanId2))).isNotNull();
      assertThat(dao.getById(ClusterLock.getLockIdForPolicyEvaluation(otherApplication, scanId3))).isNotNull();
      // Lock for audit json file store
      try (ClusterLock clusterLock = ClusterLock.createForAuditJsonFileStore(application.getId())) {
        clusterLock.lock();
      }
      assertThat(dao.getById(ClusterLock.getLockIdForAuditJsonFileStore(application.getId()))).isNotNull();

      applicationDAO.delete(application);

      assertThat(dao.getById(ClusterLock.getLockIdForPolicyViolations(application))).isNull();
      assertThat(dao.getById(ClusterLock.getLockIdForPolicyViolationAggregations(application.getId())))
          .isNull();
      assertThat(dao.getById(ClusterLock.getLockIdForPolicyEvaluation(application, scanId1))).isNull();
      assertThat(dao.getById(ClusterLock.getLockIdForPolicyEvaluation(application, scanId2))).isNull();
      assertThat(dao.getById(ClusterLock.getLockIdForPolicyEvaluation(otherApplication, scanId3))).isNotNull();
      assertThat(dao.getById(ClusterLock.getLockIdForAuditJsonFileStore(application.getId()))).isNull();
    }
    finally {
      DataSourceFactory.clear_ForTestsOnly();
    }
  }

  @Test
  public void testGetApplicationsByContact() {
    final String contactName = "contactName";
    // Create some applications with and without contact name
    final int numApplications = 5;
    final Map<String, Application> expecteApplications = new HashMap<>();
    for (int i = 1; i <= numApplications; i++) {
      // Create some with contact name
      Application application = tempEntity.newApplication("app-with-contact-" + i, tempEntity.uuid(),
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

  class ApplicationComparator
      implements Comparator<Application>
  {
    @Override
    public int compare(final Application o1, final Application o2) {
      return o1.getId().compareTo(o2.getId());
    }
  }

  @Test
  public void testUpdateApplicationWithInvalidPublicId() {
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
  public void testCRUD_RecordSearchIndexChange() {
    new SystemConfigurationPropertyDAO()
        .update(new SystemConfigurationProperty(SystemConfigurationProperty.ADVANCED_SEARCH_ENABLED, "true"));
    SearchIndexChangeDAO searchIndexChangeDAO = new SearchIndexChangeDAO();
    Organization org = tempEntity.newOrganization();
    searchIndexChangeDAO.getAll().forEach(searchIndexChangeDAO::delete);

    Application app = tempEntity.newApplication(org.getId());

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

  @Test
  public void testDelete_CascadesToInnerSource() {
    InnerSourceComponent innerSourceComponent = tempEntity.newInnerSourceComponent("pkg:test/name", application);

    applicationDAO.delete(application);

    InnerSourceComponentDAO innerSourceComponentDAO = new InnerSourceComponentDAO();
    assertThat(innerSourceComponentDAO.getById(innerSourceComponent.getId())).isNull();
  }
}
