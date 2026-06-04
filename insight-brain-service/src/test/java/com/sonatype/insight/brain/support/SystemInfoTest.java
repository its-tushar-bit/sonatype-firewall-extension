/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sonatype.insight.brain.configuration.saml.SamlConfigurationService;
import com.sonatype.insight.brain.dataaccess.configuration.MailConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ProxyServerConfigurationDAO;
import com.sonatype.insight.brain.model.configuration.MailConfiguration;
import com.sonatype.insight.brain.model.configuration.ProxyServerConfiguration;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.product.license.CLMLicenseManager;
import com.sonatype.insight.brain.product.license.LicenseInfo;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.product.license.ProductLicensingModel;
import com.sonatype.insight.brain.relay.RelayPollerCounters;
import com.sonatype.insight.brain.relay.RelayRegistrationService;
import com.sonatype.insight.brain.security.SamlDeploymentManager;
import com.sonatype.insight.brain.service.ApplicationLifecycle;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.license.model.LicensedFeature;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.SortedMap;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

public class SystemInfoTest
{
  @Rule
  public TemporaryFolder tempDir = new TemporaryFolder();

  private final ObjectMapper objectMapper = new ObjectMapper();

  private InsightConfig insightConfig;

  private SystemInfo systemInfo;

  private CLMLicenseManager clmLicenseManager;

  private ProductLicense productLicense;

  private SamlConfigurationService samlConfigurationService;

  private MailConfigurationDAO mailConfigurationDAO;

  private ProxyServerConfigurationDAO proxyServerConfigurationDAO;

  private SamlDeploymentManager samlDeploymentManager;

  private Configuration configuration;

  private RelayRegistrationService relayRegistrationService;

  private RelayPollerCounters relayPollerCounters;

  @Before
  public void setUp() throws Exception {
    File sonatypeWork = tempDir.newFolder("sonatype-work");
    File clusterDirectory = tempDir.newFolder("cluster-directory");

    insightConfig = new InsightConfig();
    insightConfig.setSonatypeWork(sonatypeWork.getAbsolutePath());
    insightConfig.setClusterDirectory(clusterDirectory.getAbsolutePath());

    clmLicenseManager = mock(CLMLicenseManager.class);
    productLicense = mock(ProductLicense.class);
    samlConfigurationService = mock(SamlConfigurationService.class);
    mailConfigurationDAO = mock(MailConfigurationDAO.class);
    proxyServerConfigurationDAO = mock(ProxyServerConfigurationDAO.class);
    samlDeploymentManager = mock(SamlDeploymentManager.class);
    configuration = mock(Configuration.class);
    relayRegistrationService = mock(RelayRegistrationService.class);
    relayPollerCounters = new RelayPollerCounters();

    systemInfo = new SystemInfo(
        insightConfig,
        new InsightWork(insightConfig),
        productLicense,
        clmLicenseManager,
        samlConfigurationService,
        mailConfigurationDAO,
        proxyServerConfigurationDAO,
        configuration,
        relayRegistrationService,
        relayPollerCounters,
        samlDeploymentManager);
  }

  @After
  public void tearDown() {
    ApplicationLifecycle.setConfigFile(null);
  }

  @Test
  public void testIsSensitiveKey() {
    assertThat(SystemInfo.isSensitiveKey("myPasswordLikePropertyName")).isTrue();
    assertThat(SystemInfo.isSensitiveKey("myPassPhrasePropertyName")).isTrue();
    assertThat(SystemInfo.isSensitiveKey("normalProp")).isFalse();
    assertThat(SystemInfo.isSensitiveKey("")).isFalse();
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testGetObfuscatedYaml() throws Exception {
    String resource = "/SystemInfoTest/config-support-test.yml";

    final String obfuscatedYaml;
    try (InputStream in = getRequiredResource(resource)) {
      obfuscatedYaml = systemInfo.getObfuscatedYaml(in);
    }

    Map<String, Object> obfuscatedMap = new Yaml(new SafeConstructor(new LoaderOptions())).load(obfuscatedYaml);

    assertThat(obfuscatedMap.get("sonatypeWork")).isEqualTo("./sonatype-work/clm-server");
    assertThat(obfuscatedMap.get("clusterDirectory")).isEqualTo("./sonatype-work/cluster-directory");
    assertThat(obfuscatedMap).containsEntry("testPassword", SystemInfo.MASK);

    Map<String, Object> passphraseMap = (Map<String, Object>) obfuscatedMap.get("testphrase");
    assertThat(passphraseMap).containsEntry("mypassphrasearray", SystemInfo.MASK);

    Map<String, Object> testMap = (Map<String, Object>) obfuscatedMap.get("testmap");
    assertThat(testMap).containsEntry("json_map-passwords", SystemInfo.MASK);
    assertThat(testMap).containsEntry("json_seq-passwords", SystemInfo.MASK);
  }

  @Test
  public void testGetObfuscatedYaml_PreservesBlockFlowFormatting() throws Exception {
    String resource = "/SystemInfoTest/testGetObfuscatedYaml_PreservesBlockFlowFormatting-config.yml";

    String expectedYaml;
    try (InputStream in = getRequiredResource(resource)) {
      expectedYaml = new String(in.readAllBytes(), StandardCharsets.UTF_8).replace("\r\n", "\n");
    }

    String actualYaml;
    try (InputStream in = getRequiredResource(resource)) {
      actualYaml = systemInfo.getObfuscatedYaml(in).replace("\r\n", "\n");
    }

    assertThat(actualYaml).isEqualTo(expectedYaml);
  }

  @Test
  public void testGetInstallInfo() throws Exception {
    File logsDir = new File(insightConfig.getSonatypeWork(), "logs");
    assertThat(logsDir.mkdirs()).isTrue();

    File serverLog = new File(logsDir, "clm-server.log");
    File requestLog = new File(logsDir, "request.log");
    File auditLog = new File(logsDir, "audit.log");
    File policyViolationLog = new File(logsDir, "policy-violation.log");

    assertThat(serverLog.createNewFile()).isTrue();
    assertThat(requestLog.createNewFile()).isTrue();
    assertThat(auditLog.createNewFile()).isTrue();
    assertThat(policyViolationLog.createNewFile()).isTrue();

    File auditDir = new InsightWork(insightConfig).getAuditDir();
    assertThat(auditDir.mkdirs()).isTrue();
    assertThat(new File(insightConfig.getSonatypeWork(), "downloads").mkdirs()).isTrue();

    File configFile = tempDir.newFile("config.yml");
    ApplicationLifecycle.setConfigFile(configFile);

    Entry<String, SortedMap<String, Object>> entry = systemInfo.getInstallInfo();

    assertThat(entry.getKey()).isEqualTo("install-info");
    assertThat(entry.getValue())
        .containsEntry("configfile", configFile.getAbsolutePath())
        .containsEntry("sonatypeWork", insightConfig.getSonatypeWork().getAbsolutePath())
        .containsEntry("clusterDirectory", insightConfig.getClusterDirectory().getAbsolutePath())
        .containsEntry("auditDir", auditDir.getAbsolutePath())
        .containsEntry("serverLog", serverLog.getAbsolutePath())
        .containsEntry("requestLog", requestLog.getAbsolutePath())
        .containsEntry("auditLog", auditLog.getAbsolutePath())
        .containsEntry("policyViolationLog", policyViolationLog.getAbsolutePath());
  }

  @Test
  public void testGetProductLicense() throws Exception {
    LicenseInfo licenseInfo = new LicenseInfo(
        "fingerprint-1234",
        123456789L,
        50,
        45,
        100,
        99,
        25,
        java.math.BigDecimal.ZERO,
        "Billy",
        "Acme",
        "billy@example.com",
        new String[]{"Sonatype Lifecycle"},
        new Properties(),
        "Lifecycle");
    when(clmLicenseManager.getLicenseInfo()).thenReturn(licenseInfo);

    StageType stageType = mock(StageType.class);
    when(stageType.getId()).thenReturn("proxy");

    when(productLicense.getFeatures()).thenReturn(Set.of(LicensedFeature.CI_INTEGRATION));
    when(productLicense.getStageTypes()).thenReturn(Set.of(stageType));
    when(productLicense.getLicensingModels()).thenReturn(Set.of(ProductLicensingModel.LEGACY));
    when(productLicense.getMaxApplications()).thenReturn(100);

    SupportZipLicenseInfo supportZipLicenseInfo =
        objectMapper.readValue(systemInfo.getProductLicense(), SupportZipLicenseInfo.class);

    assertThat(supportZipLicenseInfo.licenseInfo.fingerprint).isEqualTo("fingerprint-1234");
    assertThat(supportZipLicenseInfo.licenseInfo.contactName).isEqualTo("Billy");
    assertThat(supportZipLicenseInfo.licenseInfo.contactCompany).isEqualTo("Acme");
    assertThat(supportZipLicenseInfo.licenseInfo.contactEmail).isEqualTo("billy@example.com");
    assertThat(supportZipLicenseInfo.features).containsExactly(LicensedFeature.CI_INTEGRATION.getId());
    assertThat(supportZipLicenseInfo.stageIds).containsExactly("proxy");
    assertThat(supportZipLicenseInfo.licensingModels).containsExactly("LEGACY");
    assertThat(supportZipLicenseInfo.applicationCountLimit).isEqualTo(100);
  }

  @Test
  public void testGetSamlInfo_NotConfigured() {
    when(samlConfigurationService.get()).thenReturn(null);

    assertThat(systemInfo.getSamlInfo()).isEqualTo("null");
    Mockito.verifyNoInteractions(samlDeploymentManager);
  }

  @Test
  public void testGetMailConfig_NotConfigured() {
    when(mailConfigurationDAO.get()).thenReturn(null);

    assertThat(systemInfo.getMailConfig()).isEqualTo("null");
  }

  @Test
  public void testGetMailConfig_Configured_MasksPassword() throws Exception {
    MailConfiguration mailConfiguration = new MailConfiguration();
    mailConfiguration.setId(MailConfigurationDAO.SINGLETON_ENTITY_ID);
    mailConfiguration.setHostname("testHostname");
    mailConfiguration.setPort(4567);
    mailConfiguration.setUsername("testUsername");
    mailConfiguration.setPassword("testPassword".toCharArray());
    mailConfiguration.setSslEnabled(true);
    mailConfiguration.setStartTlsEnabled(true);
    mailConfiguration.setSystemEmail("test@example.com");
    when(mailConfigurationDAO.get()).thenReturn(mailConfiguration);

    MailConfiguration masked = objectMapper.readValue(systemInfo.getMailConfig(), MailConfiguration.class);

    assertThat(masked.getId()).isEqualTo(MailConfigurationDAO.SINGLETON_ENTITY_ID);
    assertThat(masked.getHostname()).isEqualTo("testHostname");
    assertThat(masked.getPort()).isEqualTo(4567);
    assertThat(masked.getUsername()).isEqualTo("testUsername");
    assertThat(masked.getPassword()).containsExactly(SystemInfo.MASK.toCharArray());
    assertThat(masked.isSslEnabled()).isTrue();
    assertThat(masked.isStartTlsEnabled()).isTrue();
    assertThat(masked.getSystemEmail()).isEqualTo("test@example.com");
  }

  @Test
  public void testGetMailConfig_Configured_NoPassword() throws Exception {
    MailConfiguration mailConfiguration = new MailConfiguration();
    mailConfiguration.setId(MailConfigurationDAO.SINGLETON_ENTITY_ID);
    mailConfiguration.setHostname("testHostname");
    mailConfiguration.setPort(4567);
    mailConfiguration.setUsername("testUsername");
    mailConfiguration.setPassword(null);
    when(mailConfigurationDAO.get()).thenReturn(mailConfiguration);

    MailConfiguration masked = objectMapper.readValue(systemInfo.getMailConfig(), MailConfiguration.class);

    assertThat(masked.getPassword()).isNull();
  }

  @Test
  public void testGetProxyServerConfiguration_NotConfigured() {
    when(proxyServerConfigurationDAO.get()).thenReturn(null);

    assertThat(systemInfo.getProxyServerConfiguration()).isEqualTo("null");
  }

  @Test
  public void testGetProxyServerConfiguration_Configured_MasksPassword() throws Exception {
    ProxyServerConfiguration proxyServerConfiguration = new ProxyServerConfiguration();
    proxyServerConfiguration.setId(ProxyServerConfigurationDAO.SINGLETON_ENTITY_ID);
    proxyServerConfiguration.setHostname("testHostname");
    proxyServerConfiguration.setPort(4567);
    proxyServerConfiguration.setUsername("testUsername");
    proxyServerConfiguration.setPassword("testPassword".toCharArray());
    proxyServerConfiguration.setExcludeHosts("host1,host2");
    when(proxyServerConfigurationDAO.get()).thenReturn(proxyServerConfiguration);

    ProxyServerConfiguration masked =
        objectMapper.readValue(systemInfo.getProxyServerConfiguration(), ProxyServerConfiguration.class);

    assertThat(masked.getId()).isEqualTo(ProxyServerConfigurationDAO.SINGLETON_ENTITY_ID);
    assertThat(masked.getHostname()).isEqualTo("testHostname");
    assertThat(masked.getPort()).isEqualTo(4567);
    assertThat(masked.getUsername()).isEqualTo("testUsername");
    assertThat(masked.getPassword()).containsExactly(SystemInfo.MASK.toCharArray());
    assertThat(masked.getExcludeHosts()).isEqualTo("host1,host2");
  }

  @Test
  public void testGetProxyServerConfiguration_Configured_NoPassword() throws Exception {
    ProxyServerConfiguration proxyServerConfiguration = new ProxyServerConfiguration();
    proxyServerConfiguration.setId(ProxyServerConfigurationDAO.SINGLETON_ENTITY_ID);
    proxyServerConfiguration.setHostname("testHostname");
    proxyServerConfiguration.setPort(4567);
    proxyServerConfiguration.setUsername("testUsername");
    proxyServerConfiguration.setPassword(null);
    when(proxyServerConfigurationDAO.get()).thenReturn(proxyServerConfiguration);

    ProxyServerConfiguration masked =
        objectMapper.readValue(systemInfo.getProxyServerConfiguration(), ProxyServerConfiguration.class);

    assertThat(masked.getPassword()).isNull();
  }

  @Test
  public void testGetRelayInfo_disabledByDefault() {
    final Entry<String, SortedMap<String, Object>> entry = systemInfo.getRelayInfo();
    assertThat(entry.getKey()).isEqualTo("relay-info");

    final SortedMap<String, Object> entries = entry.getValue();
    assertThat(entries).containsEntry("featureEnabled", false)
        .containsEntry("registered", false)
        .containsEntry("mode", "disabled")
        .containsEntry("fallbackActive", false)
        .containsEntry("eventsPolled", 0L)
        .containsEntry("eventsProcessed", 0L)
        .containsEntry("eventsUnmatched", 0L)
        .containsEntry("eventsDuplicate", 0L)
        .containsEntry("pollErrors", 0L)
        .containsEntry("ackErrors", 0L)
        .containsEntry("eventsProcessingErrors", 0L);
    // No customer credentials must leak into the support zip section.
    assertThat(entries).doesNotContainKeys("apiKey", "webhookSigningSecret", "webhookSecret");
  }

  @Test
  public void testGetRelayInfo_pendingRegistration() {
    // Coverage for the post-flag-enable / pre-registration window: feature gate is open
    // but no relay_configuration row exists yet (admin enabled the flag, hasn't called
    // registerWithRelay). Distinguishes from "disabled" (flag off), "relay" (registered
    // and healthy), and "fallback" (registered but degraded). Operators reading the
    // support zip in this window need to know the registration step is pending, not
    // that the relay is broken.
    org.mockito.Mockito.when(relayRegistrationService.isFeatureGateOpen()).thenReturn(true);
    org.mockito.Mockito.when(relayRegistrationService.getConfiguration()).thenReturn(null);

    final Entry<String, SortedMap<String, Object>> entry = systemInfo.getRelayInfo();
    assertThat(entry.getKey()).isEqualTo("relay-info");

    final SortedMap<String, Object> entries = entry.getValue();
    assertThat(entries).containsEntry("featureEnabled", true)
        .containsEntry("registered", false)
        .containsEntry("mode", "pending-registration");
    // Same secret-leak guard applies in this state.
    assertThat(entries).doesNotContainKeys("apiKey", "webhookSigningSecret", "webhookSecret");
  }

  @Test
  public void testGetRelayInfo_relayHealthy() {
    // Steady-state: feature on, registered, fallback NOT active. mode='relay' is the
    // only state that confirms IQ is actually consuming relay events end-to-end.
    org.mockito.Mockito.when(relayRegistrationService.isFeatureGateOpen()).thenReturn(true);
    com.sonatype.insight.brain.model.relay.RelayConfiguration cfg =
        new com.sonatype.insight.brain.model.relay.RelayConfiguration();
    cfg.setCustomerId("cust-123");
    org.mockito.Mockito.when(relayRegistrationService.getConfiguration()).thenReturn(cfg);
    // Counters: not in fallback (default false on a fresh RelayPollerCounters).

    final Entry<String, SortedMap<String, Object>> entry = systemInfo.getRelayInfo();

    final SortedMap<String, Object> entries = entry.getValue();
    assertThat(entries).containsEntry("featureEnabled", true)
        .containsEntry("registered", true)
        .containsEntry("mode", "relay")
        .containsEntry("fallbackActive", false)
        .containsEntry("customerId", "cust-123");
    assertThat(entries).doesNotContainKeys("apiKey", "webhookSigningSecret", "webhookSecret");
  }

  @Test
  public void testGetRelayInfo_lastSuccessfulPollAtFormatting() {
    // Coverage for the non-null lastSuccessfulPollAt branch in getRelayInfo(). When the
    // counters bean has recorded a successful poll, the support-zip section must include
    // the ISO-8601 string. Distinguishes the operator-visible "polling has succeeded at
    // least once" state from the not-yet-polled steady state where the field is null.
    org.mockito.Mockito.when(relayRegistrationService.isFeatureGateOpen()).thenReturn(true);
    com.sonatype.insight.brain.model.relay.RelayConfiguration cfg =
        new com.sonatype.insight.brain.model.relay.RelayConfiguration();
    cfg.setCustomerId("cust-789");
    org.mockito.Mockito.when(relayRegistrationService.getConfiguration()).thenReturn(cfg);
    java.time.Instant instant = java.time.Instant.parse("2026-06-01T12:34:56Z");
    relayPollerCounters.setLastSuccessfulPollAt(instant);

    final Entry<String, SortedMap<String, Object>> entry = systemInfo.getRelayInfo();

    final SortedMap<String, Object> entries = entry.getValue();
    assertThat(entries).containsEntry("lastSuccessfulPollAt", "2026-06-01T12:34:56Z");
  }

  @Test
  public void testGetRelayInfo_fallbackActive() {
    // Degraded steady-state: feature on, registered, but consecutive poll failures pushed
    // legacy SCM polling back online. mode='fallback' tells operators users are still
    // getting PR scans (via legacy path) even though relay polling is degraded.
    org.mockito.Mockito.when(relayRegistrationService.isFeatureGateOpen()).thenReturn(true);
    com.sonatype.insight.brain.model.relay.RelayConfiguration cfg =
        new com.sonatype.insight.brain.model.relay.RelayConfiguration();
    cfg.setCustomerId("cust-456");
    org.mockito.Mockito.when(relayRegistrationService.getConfiguration()).thenReturn(cfg);
    // Force fallback by recording threshold-crossing failures on the live counters bean
    // (the test setUp constructs a real RelayPollerCounters; cheaper than mocking the
    // Snapshot).
    relayPollerCounters.setFallbackActive(true);

    final Entry<String, SortedMap<String, Object>> entry = systemInfo.getRelayInfo();

    final SortedMap<String, Object> entries = entry.getValue();
    assertThat(entries).containsEntry("featureEnabled", true)
        .containsEntry("registered", true)
        .containsEntry("mode", "fallback")
        .containsEntry("fallbackActive", true)
        .containsEntry("customerId", "cust-456");
    assertThat(entries).doesNotContainKeys("apiKey", "webhookSigningSecret", "webhookSecret");
  }

  private InputStream getRequiredResource(String path) {
    InputStream inputStream = getClass().getResourceAsStream(path);
    assertThat(inputStream).as("Missing test resource %s", path).isNotNull();
    return inputStream;
  }
}
