/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;
import com.sonatype.insight.brain.model.Color;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.artifactory.ArtifactoryConnection;
import com.sonatype.insight.brain.model.configuration.MailConfiguration;
import com.sonatype.insight.brain.model.configuration.ProxyServerConfiguration;
import com.sonatype.insight.brain.model.configuration.crowd.CrowdConfiguration;
import com.sonatype.insight.brain.model.configuration.webhook.Webhook;
import com.sonatype.insight.brain.model.configuration.webhook.WebhookEventType;
import com.sonatype.insight.brain.model.configuration.ldap.LdapConnection;
import com.sonatype.insight.brain.model.configuration.ldap.LdapProtocol;
import com.sonatype.insight.brain.model.jira.JiraConfiguration;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver;
import com.sonatype.insight.brain.model.repository.ProxyRepositoryComponent;
import com.sonatype.insight.brain.model.roi.CurrencyTypes;
import com.sonatype.insight.brain.model.roi.RoiConfiguration;
import com.sonatype.insight.brain.model.repository.RepositoryConnection;
import com.sonatype.insight.brain.model.repository.RepositoryFormat;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.jooq.Record;
import org.jooq.UpdatableRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.ArtifactoryConnection.ARTIFACTORY_CONNECTION;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.Label.LABEL;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.CrowdConfiguration.CROWD_CONFIGURATION;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.JiraConfiguration.JIRA_CONFIGURATION;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.LdapConnection.LDAP_CONNECTION;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.MailConfiguration.MAIL_CONFIGURATION;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.PolicyWaiver.POLICY_WAIVER;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.ProxyServerConfiguration.PROXY_SERVER_CONFIGURATION;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.ProxyRepositoryComponent.PROXY_REPOSITORY_COMPONENT;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.RepositoryConnection.REPOSITORY_CONNECTION;
import static com.sonatype.insight.brain.jooq.generated.aggregation.tables.RoiConfiguration.ROI_CONFIGURATION;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.RepositoryManager.REPOSITORY_MANAGER;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.Webhook.WEBHOOK;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.SourceControl.SOURCE_CONTROL;
import static com.sonatype.nexus.scm.SourceControlProvider.BITBUCKET;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test to verify whether jOOQ's built-in {@link Record#from(Object)} and {@link Record#into(Class)}
 * methods work correctly with our entity classes.
 *
 * This test validates the reviewer's suggestion in PR #15014 to use jOOQ's built-in mapping
 * instead of manual fromEntity/toEntity methods.
 *
 * @see <a href="https://github.com/sonatype/insight-brain/pull/15014#discussion_r2756431499">PR Review Comment</a>
 */
@PostgresTest
public class JooqRecordMappingTest
    extends AbstractDbDAOTest
{
  private OrganizationDAO organizationDAO;

  @BeforeEach
  @Override
  public void setup() {
    super.setup();
    organizationDAO = daoFactory.createOrganizationDAO();
  }

  /**
   * Test Case 1: Simple entity with String fields and @Column annotations.
   *
   * Entity: ArtifactoryConnection
   * - Uses @Column(name = "artifactory_connection_id") for 'id' field
   * - All fields are simple String types
   *
   * FINDING: jOOQ's Record.from() does NOT handle char[] to String conversion.
   * This test documents that limitation.
   */
  @Test
  public void testRecordFrom_simpleEntityWithColumnAnnotations() {
    ArtifactoryConnection entity = new ArtifactoryConnection();
    entity.setId(UUID.randomUUID().toString());
    entity.setOwnerId(organization.getId());
    entity.setBaseUrl("https://example.com/artifactory");
    entity.setUsername("testuser");
    // Note: Setting password as null to avoid char[] conversion issue
    // jOOQ cannot convert char[] to String automatically
    entity.setPassword(null);

    try (TransactionContext tx = organizationDAO.createTransactionContext()) {
      UpdatableRecord<?> record = tx.dsl().newRecord(ARTIFACTORY_CONNECTION);

      // Test Record.from(entity) - copies POJO values into record
      record.from(entity);

      // Verify fields were mapped correctly
      // Note: The 'id' field in entity maps to 'artifactory_connection_id' column via @Column annotation
      assertThat(record.get(ARTIFACTORY_CONNECTION.ARTIFACTORY_CONNECTION_ID))
          .as("ID field should be mapped via @Column annotation")
          .isEqualTo(entity.getId());

      assertThat(record.get(ARTIFACTORY_CONNECTION.OWNER_ID))
          .as("ownerId should be mapped")
          .isEqualTo(entity.getOwnerId());

      assertThat(record.get(ARTIFACTORY_CONNECTION.BASE_URL))
          .as("baseUrl should be mapped")
          .isEqualTo(entity.getBaseUrl());

      assertThat(record.get(ARTIFACTORY_CONNECTION.USERNAME))
          .as("username should be mapped")
          .isEqualTo(entity.getUsername());
    }
  }

  @Test
  public void testRecordFrom_artifactoryConfigurationPasswordConverter() {
    ArtifactoryConnection entity = new ArtifactoryConnection();
    entity.setId(UUID.randomUUID().toString());
    entity.setOwnerId(organization.getId());
    entity.setBaseUrl("https://example.com/artifactory");
    entity.setUsername("testuser");
    entity.setPassword("testpassword".toCharArray());

    try (TransactionContext tx = organizationDAO.createTransactionContext()) {
      UpdatableRecord<?> record = tx.dsl().newRecord(ARTIFACTORY_CONNECTION);

      // Record.from() now works for char[] fields
      record.from(entity);

      // Verify password was mapped correctly
      assertThat(record.get(ARTIFACTORY_CONNECTION.PASSWORD))
          .as("Password String should be converted to char[] via converter")
          .isEqualTo("testpassword".toCharArray());
    }
  }

  @Test
  public void testRecordFrom_mailConfigurationPasswordConverter() {
    MailConfiguration entity = new MailConfiguration();
    entity.setId(UUID.randomUUID().toString());
    entity.setHostname("mail.example.com");
    entity.setPort(587);
    entity.setUsername("testuser");
    entity.setPassword("testpassword".toCharArray());
    entity.setSslEnabled(true);
    entity.setStartTlsEnabled(false);
    entity.setSystemEmail("system@example.com");

    try (TransactionContext tx = organizationDAO.createTransactionContext()) {
      UpdatableRecord<?> record = tx.dsl().newRecord(MAIL_CONFIGURATION);

      // Record.from() now works for char[] fields
      record.from(entity);

      // Verify password was mapped correctly
      assertThat(record.get(MAIL_CONFIGURATION.PASSWORD))
          .as("Password String should be converted to char[] via converter")
          .isEqualTo("testpassword".toCharArray());
    }
  }

  @Test
  public void testRecordFrom_proxyServerConfigurationPasswordConverter() {
    ProxyServerConfiguration entity = new ProxyServerConfiguration();
    entity.setId(UUID.randomUUID().toString());
    entity.setHostname("proxy.example.com");
    entity.setPort(8080);
    entity.setUsername("proxyuser");
    entity.setPassword("proxypassword".toCharArray());
    entity.setExcludeHosts("localhost,127.0.0.1");

    try (TransactionContext tx = organizationDAO.createTransactionContext()) {
      UpdatableRecord<?> record = tx.dsl().newRecord(PROXY_SERVER_CONFIGURATION);

      // Record.from() now works for char[] fields
      record.from(entity);

      // Verify password was mapped correctly
      assertThat(record.get(PROXY_SERVER_CONFIGURATION.PASSWORD))
          .as("Password String should be converted to char[] via converter")
          .isEqualTo("proxypassword".toCharArray());
    }
  }

  @Test
  public void testRecordFrom_crowdConfigurationApplicationPasswordConverter() {
    CrowdConfiguration entity = new CrowdConfiguration();
    entity.setId(UUID.randomUUID().toString());
    entity.setServerUrl("https://crowd.example.com");
    entity.setApplicationName("myapp");
    entity.setApplicationPassword("crowdpassword".toCharArray());

    try (TransactionContext tx = organizationDAO.createTransactionContext()) {
      UpdatableRecord<?> record = tx.dsl().newRecord(CROWD_CONFIGURATION);

      // Record.from() now works for char[] fields
      record.from(entity);

      // Verify application_password was mapped correctly
      assertThat(record.get(CROWD_CONFIGURATION.APPLICATION_PASSWORD))
          .as("Application password String should be converted to char[] via converter")
          .isEqualTo("crowdpassword".toCharArray());
    }
  }

  @Test
  public void testRecordFrom_jiraConfigurationPasswordConverter() {
    JiraConfiguration entity = new JiraConfiguration();
    entity.setId(UUID.randomUUID().toString());
    entity.setUrl("https://jira.example.com");
    entity.setUsername("jirauser");
    entity.setPassword("jirapassword".toCharArray());

    try (TransactionContext tx = organizationDAO.createTransactionContext()) {
      UpdatableRecord<?> record = tx.dsl().newRecord(JIRA_CONFIGURATION);

      // Record.from() now works for char[] fields
      record.from(entity);

      // Verify password was mapped correctly
      assertThat(record.get(JIRA_CONFIGURATION.PASSWORD))
          .as("Password String should be converted to char[] via converter")
          .isEqualTo("jirapassword".toCharArray());
    }
  }

  @Test
  public void testRecordFrom_ldapConnectionSystemPasswordConverter() {
    LdapConnection entity = new LdapConnection();
    entity.setId(UUID.randomUUID().toString());
    entity.setServerId(UUID.randomUUID().toString());
    entity.setProtocol(LdapProtocol.LDAP);
    entity.setHostname("ldap.example.com");
    entity.setPort(389);
    entity.setSearchBase("dc=example,dc=com");
    entity.setSystemUsername("cn=admin,dc=example,dc=com");
    entity.setSystemPassword("ldappassword".toCharArray());

    try (TransactionContext tx = organizationDAO.createTransactionContext()) {
      UpdatableRecord<?> record = tx.dsl().newRecord(LDAP_CONNECTION);

      // Record.from() now works for char[] fields
      record.from(entity);

      // Verify system_password was mapped correctly
      assertThat(record.get(LDAP_CONNECTION.SYSTEM_PASSWORD))
          .as("System password String should be converted to char[] via converter")
          .isEqualTo("ldappassword".toCharArray());
    }
  }

  @Test
  public void testRecordFrom_repositoryConnectionPasswordConverter() {
    RepositoryConnection entity = new RepositoryConnection();
    entity.setId(UUID.randomUUID().toString());
    entity.setOwnerId(organization.getId());
    entity.setBaseUrl("https://repo.example.com");
    entity.setFormat(RepositoryFormat.MAVEN);
    entity.setUsername("repouser");
    entity.setPassword("repopassword".toCharArray());

    try (TransactionContext tx = organizationDAO.createTransactionContext()) {
      UpdatableRecord<?> record = tx.dsl().newRecord(REPOSITORY_CONNECTION);

      // Record.from() now works for char[] fields
      record.from(entity);

      // Verify password was mapped correctly
      assertThat(record.get(REPOSITORY_CONNECTION.PASSWORD))
          .as("Password String should be converted to char[] via converter")
          .isEqualTo("repopassword".toCharArray());
    }
  }

  /**
   * Test Case 2: Entity with Date fields - jOOQ forcedType converter handles conversion.
   *
   * Entity: ProxyRepositoryComponent
   * - Has java.util.Date fields: time, lastEvaluationTime, quarantineTime, unquarantineTime
   * - jOOQ forcedTypes configured with LocalDateTimeToDateConverter
   * - The converter makes TableField use Date type directly (not LocalDateTime)
   */
  @Test
  public void testRecordFrom_entityWithDateFields() {
    ProxyRepositoryComponent entity = new ProxyRepositoryComponent();
    entity.setId(UUID.randomUUID().toString());
    entity.setRepositoryId(repository.getId());
    entity.setPathname("/test/path/component.jar");
    entity.setTime(new Date());
    entity.setHash("abc123hash");
    entity.setMatchStateId("EXACT");
    entity.setLastEvaluationTime(new Date());
    entity.setQuarantineTime(new Date());

    try (TransactionContext tx = organizationDAO.createTransactionContext()) {
      UpdatableRecord<?> record = tx.dsl().newRecord(PROXY_REPOSITORY_COMPONENT);

      // Test Record.from(entity) - with forcedType converter, Date fields map directly
      record.from(entity);

      // Verify ID mapping
      assertThat(record.get(PROXY_REPOSITORY_COMPONENT.PROXY_REPOSITORY_COMPONENT_ID))
          .as("ID field should be mapped via @Column annotation")
          .isEqualTo(entity.getId());

      // With LocalDateTimeToDateConverter configured via forcedTypes, the TableField
      // uses Date as its Java type, so record.get() returns Date directly
      assertThat(record.get(PROXY_REPOSITORY_COMPONENT.TIME))
          .as("Date field should be mapped directly (converter handles DB conversion)")
          .isEqualTo(entity.getTime());

      assertThat(record.get(PROXY_REPOSITORY_COMPONENT.LAST_EVALUATION_TIME))
          .as("lastEvaluationTime Date should be mapped directly")
          .isEqualTo(entity.getLastEvaluationTime());
    }
  }

  /**
   * Test Case 3: Entity with Enum fields that need String conversion.
   *
   * Entity: PolicyWaiver
   * - Has ComponentMatcherStrategyForWaiver enum field
   * - jOOQ table uses String for this column
   */
  @Test
  public void testRecordFrom_entityWithEnumFields() {
    PolicyWaiver entity = new PolicyWaiver();
    entity.setId(UUID.randomUUID().toString());
    entity.setPolicyId(tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID).getId());
    entity.setOwnerId(organization.getId());
    entity.setComponentMatchStrategy(ComponentMatcherStrategyForWaiver.ALL_VERSIONS);
    entity.setComment("Test waiver");
    entity.setCreateTime(new Date());
    entity.setCreatorId("test-creator");
    entity.setCreatorName("Test Creator");

    try (TransactionContext tx = organizationDAO.createTransactionContext()) {
      UpdatableRecord<?> record = tx.dsl().newRecord(POLICY_WAIVER);

      // Test Record.from(entity) with Enum -> String conversion
      record.from(entity);

      // Verify enum was converted to String
      assertThat(record.get(POLICY_WAIVER.COMPONENT_MATCH_STRATEGY))
          .as("Enum should be converted to String")
          .isEqualTo(entity.getComponentMatchStrategy().name());
    }
  }

  @Test
  public void testRecordFrom_SourceControlEntityWithEnumFields() {
    SourceControl entity = new SourceControl();
    entity.setProvider(BITBUCKET);
    try (TransactionContext tx = organizationDAO.createTransactionContext()) {

      UpdatableRecord<?> record = tx.dsl().newRecord(SOURCE_CONTROL);

      record.from(entity);

      // Verify enum was converted to String using name() not toString()
      assertThat(record.get(SOURCE_CONTROL.PROVIDER))
          .as("Enum should be converted to String using name()")
          .isEqualTo(entity.getProvider().name());
    }
  }

  @Test
  public void testRecordInto_SourceControlEntityWithEnumFields() {
    try (TransactionContext tx = organizationDAO.createTransactionContext()) {
      UpdatableRecord<?> record = tx.dsl().newRecord(SOURCE_CONTROL);
      record.set(SOURCE_CONTROL.PROVIDER, BITBUCKET.name());

      SourceControl entity = record.into(SourceControl.class);

      assertThat(entity.getProvider())
          .as("String should be converted back to Enum via valueOf(name())")
          .isEqualTo(BITBUCKET);
    }
  }

  @Test
  public void testRecordFrom_LabelEntityWithColorEnum() {
    Label entity = new Label(organization.getId(), "test-label", Color.light_green);

    try (TransactionContext tx = organizationDAO.createTransactionContext()) {
      UpdatableRecord<?> record = tx.dsl().newRecord(LABEL);

      record.from(entity);

      assertThat(record.get(LABEL.COLOR))
          .as("Color enum should be stored using name() — 'light_green', not @JsonValue 'light-green'")
          .isEqualTo(Color.light_green.name());
    }
  }

  @Test
  public void testRecordInto_LabelEntityWithColorEnum() {
    try (TransactionContext tx = organizationDAO.createTransactionContext()) {
      UpdatableRecord<?> record = tx.dsl().newRecord(LABEL);
      record.set(LABEL.COLOR, Color.dark_blue.name());

      Label entity = record.into(Label.class);

      assertThat(entity.getColor())
          .as("String 'dark_blue' should be converted back to Color.dark_blue via valueOf()")
          .isEqualTo(Color.dark_blue);
    }
  }

  @Test
  public void testRecordFrom_RoiConfiguration() {
    RoiConfiguration entity = new RoiConfiguration(
        CurrencyTypes.USD,
        new BigDecimal("50000.67"),
        new BigDecimal("30000.12"),
        new BigDecimal("10000.00"),
        14,
        new BigDecimal("250.50"));
    entity.setId(UUID.randomUUID().toString());

    try (TransactionContext tx = organizationDAO.createTransactionContext()) {
      UpdatableRecord<?> record = tx.dsl().newRecord(ROI_CONFIGURATION);

      record.from(entity);

      assertThat(record.get(ROI_CONFIGURATION.ROI_CONFIGURATION_ID))
          .isEqualTo(entity.getId());
      assertThat(record.get(ROI_CONFIGURATION.CURRENCY))
          .as("CurrencyTypes enum should use name()")
          .isEqualTo(CurrencyTypes.USD.name());
      assertThat(record.get(ROI_CONFIGURATION.MALWARE_ATTACKS_PREVENTED))
          .isEqualByComparingTo(new BigDecimal("50000.67"));
      assertThat(record.get(ROI_CONFIGURATION.NAMESPACE_ATTACKS_PREVENTED))
          .isEqualByComparingTo(new BigDecimal("30000.12"));
      assertThat(record.get(ROI_CONFIGURATION.SAFE_COMPONENTS_AUTO_SELECTED))
          .isEqualByComparingTo(new BigDecimal("10000.00"));
      assertThat(record.get(ROI_CONFIGURATION.BASELINE_DAYS_TO_RESOLVE_VIOLATION))
          .isEqualTo(14);
      assertThat(record.get(ROI_CONFIGURATION.DAILY_RISK_COST_OF_UNFIXED_VIOLATION))
          .isEqualByComparingTo(new BigDecimal("250.50"));
    }
  }

  @Test
  public void testRecordInto_RoiConfiguration() {
    try (TransactionContext tx = organizationDAO.createTransactionContext()) {
      UpdatableRecord<?> record = tx.dsl().newRecord(ROI_CONFIGURATION);
      record.set(ROI_CONFIGURATION.ROI_CONFIGURATION_ID, UUID.randomUUID().toString());
      record.set(ROI_CONFIGURATION.CURRENCY, "USD");
      record.set(ROI_CONFIGURATION.MALWARE_ATTACKS_PREVENTED, new BigDecimal("50000.67"));
      record.set(ROI_CONFIGURATION.NAMESPACE_ATTACKS_PREVENTED, new BigDecimal("30000.12"));
      record.set(ROI_CONFIGURATION.SAFE_COMPONENTS_AUTO_SELECTED, new BigDecimal("10000.00"));
      record.set(ROI_CONFIGURATION.BASELINE_DAYS_TO_RESOLVE_VIOLATION, 14);
      record.set(ROI_CONFIGURATION.DAILY_RISK_COST_OF_UNFIXED_VIOLATION, new BigDecimal("250.50"));

      RoiConfiguration entity = record.into(RoiConfiguration.class);

      assertThat(entity.getCurrency())
          .as("String 'USD' should convert to CurrencyTypes.USD")
          .isEqualTo(CurrencyTypes.USD);
      assertThat(entity.getMalwareAttacksPrevented()).isEqualByComparingTo(new BigDecimal("50000.67"));
      assertThat(entity.getNamespaceAttacksPrevented()).isEqualByComparingTo(new BigDecimal("30000.12"));
      assertThat(entity.getSafeComponentsAutoSelected()).isEqualByComparingTo(new BigDecimal("10000.00"));
      assertThat(entity.getBaselineDaysToResolveViolation()).isEqualTo(14);
      assertThat(entity.getDailyRiskCostOfUnfixedViolation()).isEqualByComparingTo(new BigDecimal("250.50"));
    }
  }

  @Test
  public void testRecordFrom_WebhookWithEventTypes() {
    Webhook entity = new Webhook(
        "https://example.com/hook",
        "secret123",
        Set.of(WebhookEventType.POLICY_ALERT, WebhookEventType.APPLICATION_EVALUATION),
        "test webhook");
    entity.setId(UUID.randomUUID().toString());

    try (TransactionContext tx = organizationDAO.createTransactionContext()) {
      UpdatableRecord<?> record = tx.dsl().newRecord(WEBHOOK);

      record.from(entity);

      assertThat(record.get(WEBHOOK.WEBHOOK_ID)).isEqualTo(entity.getId());
      assertThat(record.get(WEBHOOK.URL)).isEqualTo("https://example.com/hook");
      assertThat(record.get(WEBHOOK.SECRET_KEY)).isEqualTo("secret123");
      assertThat(record.get(WEBHOOK.DESCRIPTION)).isEqualTo("test webhook");

      // eventTypes is a Set<WebhookEventType> mapped via @ElementCollection to a separate
      // join table (webhook_event_type). It has no matching column on the webhook table,
      // so record.from() correctly ignores it — it must be persisted separately.
      assertThat(record.field("event_type")).isNull();
      assertThat(record.field("eventTypes")).isNull();
    }
  }

  @Test
  public void testRecordInto_Webhook() {
    try (TransactionContext tx = organizationDAO.createTransactionContext()) {
      UpdatableRecord<?> record = tx.dsl().newRecord(WEBHOOK);
      record.set(WEBHOOK.WEBHOOK_ID, UUID.randomUUID().toString());
      record.set(WEBHOOK.URL, "https://example.com/hook");
      record.set(WEBHOOK.SECRET_KEY, "secret123");
      record.set(WEBHOOK.DESCRIPTION, "test webhook");

      Webhook entity = record.into(Webhook.class);

      assertThat(entity.getId()).isEqualTo(record.get(WEBHOOK.WEBHOOK_ID));
      assertThat(entity.getUrl()).isEqualTo("https://example.com/hook");
      assertThat(entity.getSecretKey()).isEqualTo("secret123");
      assertThat(entity.getDescription()).isEqualTo("test webhook");

      // eventTypes lives in a separate join table, so record.into() cannot populate it
      assertThat(entity.getEventTypes()).isNull();
    }
  }

  /**
   * Test Case 4: Reverse mapping - record.into(Entity.class)
   *
   * Test if fetchInto(Entity.class) correctly maps record values back to entity,
   * handling LocalDateTime -> Date and String -> Enum conversions.
   */
  @Test
  public void testRecordInto_simpleEntity() {
    // First insert using manual approach
    RepositoryManager entity = tempEntity.newRepositoryManager("test-instance-" + UUID.randomUUID());

    try (TransactionContext tx = organizationDAO.createTransactionContext()) {
      // Fetch using fetchInto(Entity.class)
      RepositoryManager fetched = tx.dsl()
          .selectFrom(REPOSITORY_MANAGER)
          .where(REPOSITORY_MANAGER.REPOSITORY_MANAGER_ID.eq(entity.getId()))
          .fetchOneInto(RepositoryManager.class);

      assertThat(fetched).isNotNull();
      assertThat(fetched.getId()).isEqualTo(entity.getId());
      assertThat(fetched.getInstanceId()).isEqualTo(entity.getInstanceId());
      assertThat(fetched.getName()).isEqualTo(entity.getName());
    }
  }

  /**
   * Test Case 5: Reverse mapping with Date fields
   *
   * Verify LocalDateTime -> Date conversion when using fetchInto(Entity.class)
   */
  @Test
  public void testRecordInto_entityWithDateFields() {
    // Create a repository component using the DAO (which handles conversions correctly)
    ProxyRepositoryComponent entity = new ProxyRepositoryComponent();
    entity.setId(UUID.randomUUID().toString());
    entity.setRepositoryId(repository.getId());
    entity.setPathname("/test/path/component-" + UUID.randomUUID() + ".jar");
    entity.setTime(new Date());
    entity.setHash("abc123hash");
    entity.setMatchStateId("EXACT");
    entity.setLastEvaluationTime(new Date());
    entity.setIdentificationSourceId("SONATYPE"); // Required field

    // Insert using our DAO (which handles conversions)
    var proxyRepositoryComponentDAO = daoFactory.createRepositoryComponentDAO();
    proxyRepositoryComponentDAO.insert(entity);

    try (TransactionContext tx = organizationDAO.createTransactionContext()) {
      // Try to fetch using fetchInto(Entity.class)
      ProxyRepositoryComponent fetched = tx.dsl()
          .selectFrom(PROXY_REPOSITORY_COMPONENT)
          .where(PROXY_REPOSITORY_COMPONENT.PROXY_REPOSITORY_COMPONENT_ID.eq(entity.getId()))
          .fetchOneInto(ProxyRepositoryComponent.class);

      assertThat(fetched).isNotNull();
      assertThat(fetched.getId()).isEqualTo(entity.getId());

      // Verify Date fields were converted from LocalDateTime
      // Note: This may fail if jOOQ doesn't handle LocalDateTime -> Date conversion
      assertThat(fetched.getTime())
          .as("LocalDateTime should be converted back to Date")
          .isNotNull();

      assertThat(fetched.getLastEvaluationTime())
          .as("LocalDateTime should be converted back to Date for lastEvaluationTime")
          .isNotNull();
    }

    // Cleanup
    proxyRepositoryComponentDAO.delete(entity);
  }

  /**
   * Test Case 6: Round-trip test - from entity -> record -> store -> fetch -> entity
   *
   * Complete integration test using Record.from() for insert and fetchInto() for retrieval
   */
  @Test
  public void testRoundTrip_usingBuiltInMapping() {
    String testId = UUID.randomUUID().toString();
    // Instance ID has max length of 50 chars
    String shortInstanceId = "rt-" + testId.substring(0, 10);

    try (TransactionContext tx = organizationDAO.createTransactionContext()) {
      tx.begin();

      // Create entity
      RepositoryManager entity = new RepositoryManager();
      entity.setId(testId);
      entity.setInstanceId(shortInstanceId);
      entity.setName("Round Trip Test");
      entity.setConfigured(true);
      entity.setConfigureTime(new Date());

      // Insert using Record.from()
      UpdatableRecord<?> record = tx.dsl().newRecord(REPOSITORY_MANAGER);
      record.set(REPOSITORY_MANAGER.REPOSITORY_MANAGER_ID, entity.getId());
      record.from(entity);
      record.store();

      tx.commit();
    }

    try (TransactionContext tx = organizationDAO.createTransactionContext()) {
      // Fetch using fetchInto(Entity.class)
      RepositoryManager fetched = tx.dsl()
          .selectFrom(REPOSITORY_MANAGER)
          .where(REPOSITORY_MANAGER.REPOSITORY_MANAGER_ID.eq(testId))
          .fetchOneInto(RepositoryManager.class);

      assertThat(fetched).isNotNull();
      assertThat(fetched.getId()).isEqualTo(testId);
      assertThat(fetched.getInstanceId()).isEqualTo(shortInstanceId);
      assertThat(fetched.getName()).isEqualTo("Round Trip Test");
      assertThat(fetched.isConfigured()).isTrue();

      // Verify Date field round-trip
      assertThat(fetched.getConfigureTime())
          .as("Date field should survive round-trip")
          .isNotNull();
    }

    // Cleanup
    try (TransactionContext tx = organizationDAO.createTransactionContext()) {
      tx.begin();
      tx.dsl()
          .deleteFrom(REPOSITORY_MANAGER)
          .where(REPOSITORY_MANAGER.REPOSITORY_MANAGER_ID.eq(testId))
          .execute();
      tx.commit();
    }
  }
}
