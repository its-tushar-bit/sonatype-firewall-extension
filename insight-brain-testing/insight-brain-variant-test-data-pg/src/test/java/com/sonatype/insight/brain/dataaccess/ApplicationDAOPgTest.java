/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ApplicationRiskDTO;
import com.sonatype.insight.brain.model.InvalidNameException;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.SearchIndexChange;
import com.sonatype.insight.brain.model.SearchIndexChange.ChangeType;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.model.policy.stages.SourceStageType;
import com.sonatype.nexus.scm.SourceControlProvider;

import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * PostgreSQL-backed tests relocated from {@link ApplicationDAOTest} (CLM-45228). The H2/unit coverage stays
 * in that origin class; the {@code @PostgresTest} coverage lives here so this module keeps a single (Postgres)
 * DatabaseRule fixture type per JVM. The inherited {@link NameableDAOTest} tests also run here on Postgres,
 * providing intended dual coverage.
 */
@PostgresTest
public class ApplicationDAOPgTest
    extends NameableDAOTest<Application>
{
  /**
   * Prohibited application public ID whitespace characters.
   */
  public static final char[] PUBLIC_ID_WHITESPACE_CHARS = {'\t', '\n', '\u000B', '\f', '\r'};

  private ApplicationDAO applicationDAO;

  @BeforeEach
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
  public void testInsertBatch_RecordSearchIndexChange_Postgres() {
    doTestInsertBatch_RecordSearchIndexChange();
  }

  private void doTestInsertBatch_RecordSearchIndexChange() {
    SystemConfigurationPropertyDAO systemConfigurationPropertyDAO = daoFactory.createSystemConfigurationPropertyDAO();
    systemConfigurationPropertyDAO.update(
        new SystemConfigurationProperty(SystemConfigurationProperty.ADVANCED_SEARCH_ENABLED, "true"));
    SearchIndexChangeDAO searchIndexChangeDAO = daoFactory.createSearchIndexChangeDAO();
    Organization org = tempEntity.newOrganization();
    searchIndexChangeDAO.getAll().forEach(searchIndexChangeDAO::delete);

    Application app1 = new Application("pub1-" + System.nanoTime(), "app1", org.getId());
    Application app2 = new Application("pub2-" + System.nanoTime(), "app2", org.getId());
    applicationDAO.insertBatch(List.of(app1, app2));

    List<SearchIndexChange> searchIndexChanges = searchIndexChangeDAO.getAll();
    assertThat(searchIndexChanges).hasSize(2);
    Set<String> changedIds = searchIndexChanges.stream()
        .map(SearchIndexChange::getChangeData)
        .collect(Collectors.toSet());
    assertThat(changedIds).containsExactlyInAnyOrder(app1.getId(), app2.getId());
    assertThat(searchIndexChanges).allSatisfy(
        change -> assertThat(change.getChangeType()).isEqualTo(ChangeType.APPLICATION));
  }

  @Test
  public void testUpdateBatch_RecordSearchIndexChange_Postgres() {
    doTestUpdateBatch_RecordSearchIndexChange();
  }

  private void doTestUpdateBatch_RecordSearchIndexChange() {
    SystemConfigurationPropertyDAO systemConfigurationPropertyDAO = daoFactory.createSystemConfigurationPropertyDAO();
    systemConfigurationPropertyDAO.update(
        new SystemConfigurationProperty(SystemConfigurationProperty.ADVANCED_SEARCH_ENABLED, "true"));
    SearchIndexChangeDAO searchIndexChangeDAO = daoFactory.createSearchIndexChangeDAO();
    Organization org = tempEntity.newOrganization();
    searchIndexChangeDAO.getAll().forEach(searchIndexChangeDAO::delete);

    Application app1 = tempEntity.newApplication(org.getId());
    Application app2 = tempEntity.newApplication(org.getId());
    searchIndexChangeDAO.getAll().forEach(searchIndexChangeDAO::delete);

    app1.setName("updated1");
    app2.setName("updated2");
    applicationDAO.updateBatch(List.of(app1, app2));

    List<SearchIndexChange> searchIndexChanges = searchIndexChangeDAO.getAll();
    assertThat(searchIndexChanges).hasSize(2);
    Set<String> changedIds = searchIndexChanges.stream()
        .map(SearchIndexChange::getChangeData)
        .collect(Collectors.toSet());
    assertThat(changedIds).containsExactlyInAnyOrder(app1.getId(), app2.getId());
    assertThat(searchIndexChanges).allSatisfy(
        change -> assertThat(change.getChangeType()).isEqualTo(ChangeType.APPLICATION));
  }

  @Test
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
  public void testGetByAncestorIds_Limit_Postgres() {
    testGetByAncestorIds_Limit();
  }

  // For postgres, this test verifies we handle the PreparedStatement parameter limit correctly.
  // PostgreSQL PreparedStatements can have at most 65,535 parameters.
  // The DAO implementation should split queries or use arrays to avoid this limitation.
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
    assertThat(result.get(0).applicationName()).isEqualTo(application.getName());
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
    assertThat(result.get(0).applicationName()).isEqualTo(application.getName());
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
    // Case-insensitive sorting: "app2" < "ApplicationDAOTest_..." < "Sandbox-app"
    // because in lowercase: "app2" < "applicationdaotest..." (at position 3, '2' < 'l')
    assertThat(result.get(0).applicationName()).isEqualTo("app2");
    assertThat(result.get(1).applicationName()).isEqualTo(application.getName());
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
