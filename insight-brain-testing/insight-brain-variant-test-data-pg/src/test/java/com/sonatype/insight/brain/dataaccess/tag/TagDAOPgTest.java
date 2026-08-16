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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

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
import com.sonatype.insight.brain.model.InvalidNameException;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.tag.Tag;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * PostgreSQL-backed tests relocated from {@link TagDAOTest} (CLM-45228). The H2/unit coverage stays in that
 * origin class; the {@code @PostgresTest} coverage lives here so this module keeps a single (Postgres)
 * DatabaseRule fixture type per JVM. The inherited {@link NameableDAOTest} tests also run here on Postgres,
 * providing intended dual coverage.
 *
 * @since 1.9
 */
@PostgresTest
public class TagDAOPgTest
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

  @BeforeEach
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
