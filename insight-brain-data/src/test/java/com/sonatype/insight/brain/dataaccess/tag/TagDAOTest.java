/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.tag;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.sonatype.insight.brain.common.test.PostgresTestCategory;
import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.NameableDAOTest;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.SearchIndexChangeDAO;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.VulnerabilityCustomCvssVectorTagDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.VulnerabilityCustomCweTagDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.VulnerabilityCustomRemediationTagDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Color;
import com.sonatype.insight.brain.model.DescriptionHelper;
import com.sonatype.insight.brain.model.InvalidNameException;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.SearchIndexChange;
import com.sonatype.insight.brain.model.SearchIndexChange.ChangeType;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.tag.ApplicationTag;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.error.exception.BadRequestException;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * @since 1.9
 */
@Category(SlowTest.class)
public class TagDAOTest
    extends NameableDAOTest<Tag>
{
  private VulnerabilityCustomRemediationTagDAO vulnerabilityCustomRemediationTagDAO;

  private VulnerabilityCustomCweTagDAO vulnerabilityCustomCweTagDAO;

  private VulnerabilityCustomCvssVectorTagDAO vulnerabilityCustomCvssVectorTagDAO;

  private PolicyTagDAO policyTagDAO;

  private OrganizationDAO organizationDAO;

  private SystemConfigurationPropertyDAO systemConfigurationPropertyDAO;

  private SearchIndexChangeDAO searchIndexChangeDAO;

  private TagDAO dao;

  private ApplicationTagDAO appTagDAO;

  @Override
  protected Tag createNameable(String a) {
    return tempEntity.newTag(organization.getId(), a, "testCRUD description", Color.yellow);
  }

  @Override
  protected AbstractOperationalSqlDAO<Tag> getDao() {
    return dao;
  }

  @Before
  @Override
  public void setup() {
    super.setup();
    vulnerabilityCustomRemediationTagDAO = daoFactory.createVulnerabilityCustomRemediationTagDAO();
    vulnerabilityCustomCweTagDAO = daoFactory.createVulnerabilityCustomCweTagDAO();
    vulnerabilityCustomCvssVectorTagDAO = daoFactory.createVulnerabilityCustomCvssVectorTagDAO();
    policyTagDAO = daoFactory.createPolicyTagDAO();
    organizationDAO = daoFactory.createOrganizationDAO();
    systemConfigurationPropertyDAO = daoFactory.createSystemConfigurationPropertyDAO();
    searchIndexChangeDAO = daoFactory.createSearchIndexChangeDAO();
    dao = daoFactory.createTagDAO();
    appTagDAO = daoFactory.createApplicationTagDAO();
    organization = tempEntity.newOrganization("TagDAOTest");
  }

  @Override
  protected int getMaxNameLength() {
    return NameHelper.MAX_NAME_LENGTH;
  }

  @Override
  protected Tag getEntityByName(String name) {
    return dao.getByName(name).get(0);
  }

  @Test
  public void testCRUD() {
    // Create
    Tag tag = new Tag(organization.getId(), "testCRUD Name", "testCRUD description", Color.yellow);
    dao.insert(tag);
    assertThat(tag.getId()).isNotNull();

    // Get
    tag = dao.getById(tag.getId());
    assertThat(tag).isNotNull();
    assertTag(organization.getId(), "testCRUD Name", "testCRUD description", Color.yellow, tag);

    // Update
    tag.setName("Updated Name");
    tag.setColor(Color.dark_purple);
    dao.update(tag);

    // Get
    tag = dao.getById(tag.getId());
    assertThat(tag).isNotNull();
    assertTag(organization.getId(), "Updated Name", "testCRUD description", Color.dark_purple, tag);

    // Delete
    dao.delete(tag);

    // Get
    tag = dao.getById(tag.getId());
    assertThat(tag).isNull();
  }

  @Test
  public void testDelete_CascadeToVulnerabilityCustomRemediationTag() {
    Tag tag1 = tempEntity.newTag(organization.getId());
    Tag tag2 = tempEntity.newTag(organization.getId());
    tempEntity.newVulnerabilityCustomData(organization.getId(), "CVE-2022-1234", tag1, "rem1",
        "testCWE", "testCvssVector1", 6.05F);
    tempEntity.newVulnerabilityCustomData(organization.getId(), "CVE-2022-1235", tag2, "rem1",
        "testCWE", "testCvssVector2", 6.05F);

    assertThat(vulnerabilityCustomRemediationTagDAO.getByTagId(tag1.getId())).isNotEmpty();
    dao.delete(tag1);
    dao.delete(tag2);
    assertThat(vulnerabilityCustomRemediationTagDAO.getByTagId(tag1.getId())).isEmpty();
    assertThat(vulnerabilityCustomRemediationTagDAO.getByTagId(tag2.getId())).isEmpty();
  }

  @Test
  public void testDelete_CascadeToVulnerabilityCustomCweTag() {
    Tag tag1 = tempEntity.newTag(organization.getId());
    Tag tag2 = tempEntity.newTag(organization.getId());
    tempEntity.newVulnerabilityCustomData(organization.getId(), "CVE-2022-1234", tag1, "rem1",
        "testCWE", "testCvssVector1", 6.05F);
    tempEntity.newVulnerabilityCustomData(organization.getId(), "CVE-2022-1235", tag2, "rem1",
        "testCWE", "testCvssVector2", 6.05F);

    assertThat(vulnerabilityCustomCweTagDAO.getByTagId(tag1.getId())).isNotEmpty();
    dao.delete(tag1);
    dao.delete(tag2);
    assertThat(vulnerabilityCustomCweTagDAO.getByTagId(tag1.getId())).isEmpty();
    assertThat(vulnerabilityCustomCweTagDAO.getByTagId(tag2.getId())).isEmpty();

  }

  @Test
  public void testDelete_CascadeToCVSSVectorTag() {
    Tag tag1 = tempEntity.newTag(organization.getId());
    Tag tag2 = tempEntity.newTag(organization.getId());
    tempEntity.newVulnerabilityCustomData(organization.getId(), "CVE-2022-1234", tag1, "rem1",
        "testCWE", "testCvssVector1", 6.05F);
    tempEntity.newVulnerabilityCustomData(organization.getId(), "CVE-2022-1235", tag2, "rem1",
        "testCWE", "testCvssVector2", 6.05F);

    assertThat(vulnerabilityCustomCweTagDAO.getByTagId(tag1.getId())).isNotEmpty();
    dao.delete(tag1);
    dao.delete(tag2);
    assertThat(vulnerabilityCustomCweTagDAO.getByTagId(tag1.getId())).isEmpty();
    assertThat(vulnerabilityCustomCweTagDAO.getByTagId(tag2.getId())).isEmpty();
  }

  @Test
  public void testDelete_CascadeToCVSSSeverityTag() {
    Tag tag1 = tempEntity.newTag(organization.getId());
    Tag tag2 = tempEntity.newTag(organization.getId());
    tempEntity.newVulnerabilityCustomData(organization.getId(), "CVE-2022-1234", tag1, "rem1",
        "testCWE", "testCvssVector1", 6.05F);
    tempEntity.newVulnerabilityCustomData(organization.getId(), "CVE-2022-1235", tag2, "rem1",
        "testCWE", "testCvssVector2", 6.05F);

    assertThat(vulnerabilityCustomCvssVectorTagDAO.getByTagId(tag1.getId())).isNotEmpty();
    dao.delete(tag1);
    dao.delete(tag2);
    assertThat(vulnerabilityCustomCvssVectorTagDAO.getByTagId(tag1.getId())).isEmpty();
    assertThat(vulnerabilityCustomCvssVectorTagDAO.getByTagId(tag2.getId())).isEmpty();
  }

  @Test
  public void testValidateDescriptionLength_Insert() {
    String description = StringUtils.repeat("a", DescriptionHelper.MAX_DESC_LENGTH);
    Tag tag = new Tag(organization.getId(), "name", description + "a", Color.yellow);
    assertThatThrownBy(() -> dao.insert(tag)).isInstanceOf(BadRequestException.class)
        .hasMessage("The description cannot be longer than 255 characters, the one supplied has 256 characters.");

    tag.setDescription(description);
    dao.insert(tag);
  }

  @Test
  public void testValidateDescriptionLength_Update() {
    Tag tag = new Tag(organization.getId(), "name", "description", Color.yellow);
    dao.insert(tag);

    String description = StringUtils.repeat("a", DescriptionHelper.MAX_DESC_LENGTH);
    tag.setDescription(description + "a");
    assertThatThrownBy(() -> dao.update(tag)).isInstanceOf(BadRequestException.class)
        .hasMessage("The description cannot be longer than 255 characters, the one supplied has 256 characters.");

    tag.setDescription(description);
    dao.update(tag);
  }

  @Test
  public void testValidateNullDescription_Insert() {
    Tag tag = new Tag(organization.getId(), "name", null /* description */, Color.yellow);
    assertThatThrownBy(() -> dao.insert(tag)).isInstanceOf(BadRequestException.class)
        .hasMessage("The description is required.");
  }

  @Test
  public void testValidateNullDescription_Update() {
    Tag tag = new Tag(organization.getId(), "name", "description", Color.yellow);
    dao.insert(tag);

    tag.setDescription(null);
    assertThatThrownBy(() -> dao.update(tag)).isInstanceOf(BadRequestException.class)
        .hasMessage("The description is required.");
  }

  @Test
  public void testValidateEmptyDescription_Insert() {
    Tag tag = new Tag(organization.getId(), "name", " " /* description */, Color.yellow);
    assertThatThrownBy(() -> dao.insert(tag)).isInstanceOf(BadRequestException.class)
        .hasMessage("The description is required.");
  }

  @Test
  public void testValidateEmptyDescription_Update() {
    Tag tag = new Tag(organization.getId(), "name", "description", Color.yellow);
    dao.insert(tag);

    tag.setDescription(" ");
    assertThatThrownBy(() -> dao.update(tag)).isInstanceOf(BadRequestException.class)
        .hasMessage("The description is required.");
  }

  @Test
  public void testValidateNullColor_Insert() {
    Tag tag = new Tag(organization.getId(), "name", "description", null);
    assertThatThrownBy(() -> dao.insert(tag)).isInstanceOf(InvalidTagException.class)
        .hasMessage("The application category color must be assigned.");
  }

  @Test
  public void testValidateNullColor_Update() {
    Tag tag = new Tag(organization.getId(), "name", "description", Color.yellow);
    dao.insert(tag);

    tag.setColor(null);
    assertThatThrownBy(() -> dao.update(tag)).isInstanceOf(InvalidTagException.class)
        .hasMessage("The application category color must be assigned.");
  }

  @Test
  public void testLegacyColorsInvalid() {
    @SuppressWarnings("deprecation")
    Color[] legacyColors = new Color[]{Color.white, Color.grey, Color.black, Color.green, Color.red, Color.blue};

    Tag tag = new Tag();
    tag.setOrganizationId(organization.getId());
    tag.setName("MyLabel");
    tag.setDescription("description");

    // Insert
    for (Color color : legacyColors) {
      tag.setColor(color);
      assertThatThrownBy(() -> dao.insert(tag)).isInstanceOf(InvalidTagException.class)
          .hasMessage("The application category color " + color.toValue() + " is invalid.");
    }

    // Update
    tag.setColor(Color.dark_blue);
    dao.insert(tag);
    for (Color color : legacyColors) {
      tag.setColor(color);
      assertThatThrownBy(() -> dao.update(tag)).isInstanceOf(InvalidTagException.class)
          .hasMessage("The application category color " + color.toValue() + " is invalid.");
    }
  }

  @Test
  public void testGetAppliedApplicationTags() {
    Application app1 = tempEntity.newApplication(organization.getId());
    Application app2 = tempEntity.newApplication(organization.getId());

    List<Tag> app1Tags = new ArrayList<>();
    List<Tag> app2Tags = new ArrayList<>();

    app1Tags.add(tempEntity.newTag(organization.getId()));
    app1Tags.add(tempEntity.newTag(organization.getId()));
    app2Tags.add(tempEntity.newTag(organization.getId()));
    app2Tags.add(tempEntity.newTag(organization.getId()));

    for (Tag tag : app1Tags) {
      tempEntity.newApplicationTag(app1.getId(), tag.getId());
    }

    for (Tag tag : app2Tags) {
      tempEntity.newApplicationTag(app2.getId(), tag.getId());
    }

    assertAppliedTags(app1Tags, dao.getByApplicationId(app1.getId()));
    assertAppliedTags(app2Tags, dao.getByApplicationId(app2.getId()));
  }

  @Test
  public void testCascadeDeleteToApplicationTags() {
    Application app = tempEntity.newApplication(organization.getId());
    Tag tag = tempEntity.newTag(organization.getId());

    ApplicationTag appTag = new ApplicationTag(app.getId(), tag.getId());
    appTagDAO.insert(appTag);

    dao.delete(tag);

    assertThat(appTagDAO.getByTagId(tag.getId())).isEmpty();
  }

  @Test
  public void testDenyCascadeDeleteToPolicyTags() {
    Policy policy = tempEntity.newPolicy(organization);
    Tag tag = tempEntity.newTag(organization.getId());
    tempEntity.newPolicyTag(policy.getId(), tag.getId());

    assertThatThrownBy(() -> dao.delete(tag)).isInstanceOf(BadRequestException.class)
        .hasMessage("Cannot delete the application category because it is associated with policies.");

    assertThat(policyTagDAO.getByTagId(tag.getId())).hasSize(1);
  }

  private void assertTag(String orgId, String name, String description, Color color, Tag actual) {
    assertThat(actual.getOrganizationId()).isEqualTo(orgId);
    assertThat(actual.getName()).isEqualTo(name);
    assertThat(actual.getNameLowercaseNoWhitespace()).isEqualTo(NameHelper.normalize(name));
    assertThat(actual.getDescription()).isEqualTo(description);
    assertThat(actual.getColor()).isEqualTo(color);
  }

  private void assertAppliedTags(List<Tag> expected, List<Tag> actual) {
    assertThat(actual).hasSameSizeAs(expected);

    Set<String> tagIds = new HashSet<>();
    for (Tag tag : expected) {
      tagIds.add(tag.getId());
    }

    for (Tag tag : actual) {
      assertThat(tag.getId()).isIn(tagIds);
    }
  }

  @Test
  public void testNameClashWithChildOrgTag_Insert() {
    String tagName = "some name";
    tempEntity.newTag(organization.getId(), tagName);

    // Add another tag with a case-/whitespace-equivalent name at parent owner level
    assertInsertTagWithDuplicateName(organization.getParentOrganizationId(), tagName, organization);
  }

  @Test
  public void testNameClashWithParentOrgTag_Insert() {
    String tagName = "some name";
    tempEntity.newTag(organization.getParentOrganizationId(), tagName);

    Organization expectedOrg = organizationDAO.getById(organization.getParentOrganizationId());

    // Add another tag with a case-/whitespace-equivalent name at child org level
    assertInsertTagWithDuplicateName(organization.getId(), tagName, expectedOrg);
  }

  @Test
  public void testNameClashWithChildOrgTag_Update() {
    String tagName = "some name";
    tempEntity.newTag(organization.getId(), tagName);

    // Add another tag with a case-/whitespace-equivalent name at parent owner level
    assertUpdateTagWithDuplicateName(organization.getParentOrganizationId(), tagName, organization);
  }

  @Test
  public void testNameClashWithParentOrgTag_Update() {
    String tagName = "some name";
    tempEntity.newTag(organization.getParentOrganizationId(), tagName);

    Organization expectedOrg = organizationDAO.getById(organization.getParentOrganizationId());

    // Add another tag with a case-/whitespace-equivalent name at child org level
    assertUpdateTagWithDuplicateName(organization.getId(), tagName, expectedOrg);
  }

  @Test
  public void testGetByApplicationIds_H2() {
    testGetByApplicationIds(true);
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testGetByApplicationIds_Postgres() {
    testGetByApplicationIds(false);
  }

  private void testGetByApplicationIds(boolean isDatabaseEmbedded) {
    assertThat(dao.isDatabaseEmbedded()).isEqualTo(isDatabaseEmbedded);
    organization = tempEntity.newOrganization();
    Tag tag1 = tempEntity.newTag(organization.getId());
    Tag tag2 = tempEntity.newTag(organization.getId());
    Tag tag3 = tempEntity.newTag(organization.getId());
    Application app1 = tempEntity.newApplication(organization.getId());
    Application app2 = tempEntity.newApplication(organization.getId());
    Application app3 = tempEntity.newApplication(organization.getId());
    tempEntity.newApplicationTag(app1.getId(), tag1.getId());
    tempEntity.newApplicationTag(app2.getId(), tag1.getId());
    tempEntity.newApplicationTag(app2.getId(), tag2.getId());
    tempEntity.newApplicationTag(app3.getId(), tag3.getId());

    List<Tag> tags = dao.getByApplicationIds(Arrays.asList(app1.getId(), app2.getId()));

    assertThat(tags).extracting(Tag::getId).containsExactlyInAnyOrder(tag1.getId(), tag2.getId());
  }

  @Test
  public void testGetByApplicationIds_H2_IN_OPERATOR_THRESHOLD() {
    Tag tag = tempEntity.newTag(organization.getId());
    List<String> appIds = new ArrayList<>();
    for (int i = 0; i < TagDAO.H2_IN_OPERATOR_THRESHOLD; i++) {
      Application app = tempEntity.newApplication(organization.getId());
      appIds.add(app.getId());
      tempEntity.newApplicationTag(app.getId(), tag.getId());
    }

    assertThat(dao.getByApplicationIds(appIds)).extracting(Tag::getId).containsExactly(tag.getId());

    Application app = tempEntity.newApplication(organization.getId());
    appIds.add(app.getId());
    tempEntity.newApplicationTag(app.getId(), tag.getId());

    assertThat(dao.getByApplicationIds(appIds)).extracting(Tag::getId).containsExactly(tag.getId());
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testGetTagsUsedByApplications_MoreThanShortMaxValueOnPostgres() throws Exception {
    OperationalDataStore operationalDataStore = databaseRule.getOperationalDataStore();

    String orgSql = "INSERT INTO " + operationalDataStore.getDatabaseSchema() +
        ".organization (organization_id, parent_organization_id, name, name_lowercase_no_whitespace) "
        + "VALUES ('org1', 'ROOT_ORGANIZATION_ID', 'org1', 'org1');";

    String sql = "INSERT INTO " + operationalDataStore.getDatabaseSchema() +
        ".application (application_id, public_id, public_id_lowercase, name, "
        + "name_lowercase_no_whitespace, organization_id) "
        + "VALUES (?, ?, ?, ?, ?, 'org1');";

    String deleteSql = "TRUNCATE " + operationalDataStore.getDatabaseSchema() +
        ".application CASCADE";

    try (Connection connection = operationalDataStore.getDataSource().getConnection()) {

      try (Statement statement = connection.createStatement()) {
        statement.executeUpdate(orgSql);
      }

      connection.setAutoCommit(false);
      List<String> appIds = new ArrayList<>(Short.MAX_VALUE + 1);
      try (PreparedStatement statement = connection.prepareStatement(sql)) {
        for (int i = 0; i <= Short.MAX_VALUE; i++) {
          String app = "app-" + (i + 1);
          appIds.add(app);
          statement.setString(1, app);
          statement.setString(2, app);
          statement.setString(3, app);
          statement.setString(4, app);
          statement.setString(5, app);
          statement.addBatch();

          if ((i + 1) % 100 == 0) {
            statement.executeBatch();
            connection.commit();
          }
        }
        statement.executeBatch();
        connection.commit();
      }
      connection.setAutoCommit(true);

      assertThat(dao.getByApplicationIds(appIds)).isEmpty();

      try (Statement statement = connection.createStatement()) {
        statement.executeUpdate(deleteSql);
      }
    }
  }

  @Test
  public void testGetByApplicationIds_Null() {
    assertThat(dao.getByApplicationIds(null)).isEmpty();
  }

  @Test
  public void testGetByApplicationIds_Empty() {
    assertThat(dao.getByApplicationIds(Collections.emptyList())).isEmpty();
  }

  @Test
  public void testCRUD_RecordSearchIndexChange() {
    systemConfigurationPropertyDAO
        .update(new SystemConfigurationProperty(SystemConfigurationProperty.ADVANCED_SEARCH_ENABLED, "true"));
    Tag tag = tempEntity.newTag(Organization.ROOT_ORGANIZATION_ID);

    List<SearchIndexChange> searchIndexChanges = searchIndexChangeDAO.getAll();
    assertThat(searchIndexChanges).hasSize(1);
    assertThat(searchIndexChanges.get(0).getChangeType()).isEqualTo(ChangeType.APPLICATION_CATEGORY);
    assertThat(searchIndexChanges.get(0).getChangeData()).isEqualTo(tag.getId());
    searchIndexChangeDAO.delete(searchIndexChanges.get(0));

    dao.update(tag);
    searchIndexChanges = searchIndexChangeDAO.getAll();
    assertThat(searchIndexChanges).hasSize(1);
    assertThat(searchIndexChanges.get(0).getChangeType()).isEqualTo(ChangeType.APPLICATION_CATEGORY);
    assertThat(searchIndexChanges.get(0).getChangeData()).isEqualTo(tag.getId());
    searchIndexChangeDAO.delete(searchIndexChanges.get(0));

    dao.delete(tag);
    searchIndexChanges = searchIndexChangeDAO.getAll();
    assertThat(searchIndexChanges).hasSize(1);
    assertThat(searchIndexChanges.get(0).getChangeType()).isEqualTo(ChangeType.APPLICATION_CATEGORY);
    assertThat(searchIndexChanges.get(0).getChangeData()).isEqualTo(tag.getId());
  }

  @Test
  public void testGetByIds() {
    Tag tag = tempEntity.newTag(organization.getId());
    tempEntity.newTag(organization.getId());

    assertThat(dao.getByIds(Collections.singletonList(tag.getId()))).usingRecursiveFieldByFieldElementComparator()
        .containsExactly(tag);
  }

  @Test
  public void testGetByIds_Batched() {
    Tag tag1 = tempEntity.newTag(organization.getId());
    Tag tag2 = tempEntity.newTag(organization.getId());
    Tag tag3 = tempEntity.newTag(organization.getId());

    dao = spy(dao);
    when(dao.getInOperatorThreshold()).thenReturn(2);

    assertThat(dao.getByIds(Arrays.asList(tag1.getId(), tag2.getId(), tag3.getId())))
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactlyInAnyOrder(tag1, tag2, tag3);
  }

  private void assertInsertTagWithDuplicateName(String orgId, String tagName, Organization expectedOrg) {
    // Add a tag with a case-/whitespace-equivalent name
    Tag tag = new Tag(orgId, tagName.replaceAll("\\s", "").toLowerCase(Locale.ENGLISH), "description");
    assertThatThrownBy(() -> dao.insert(tag)).isInstanceOf(InvalidNameException.class)
        .hasMessage(
            "An application category with the same name already exists for organization '" + expectedOrg.getName()
                + "'");
  }

  private void assertUpdateTagWithDuplicateName(String orgId, String tagName, Organization expectedOrg) {
    // Add a tag with a case-/whitespace-equivalent name
    Tag tag = tempEntity.newTag(orgId, "another name");
    tag.setName(tagName.replaceAll("\\s", "").toLowerCase(Locale.ENGLISH));
    assertThatThrownBy(() -> dao.update(tag)).isInstanceOf(InvalidNameException.class)
        .hasMessage(
            "An application category with the same name already exists for organization '" + expectedOrg.getName()
                + "'");
  }
}
