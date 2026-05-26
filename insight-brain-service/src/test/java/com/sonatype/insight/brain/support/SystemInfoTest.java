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
import com.sonatype.insight.brain.security.SamlDeploymentManager;
import com.sonatype.insight.brain.service.ApplicationLifecycle;
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

    systemInfo = new SystemInfo(
        insightConfig,
        new InsightWork(insightConfig),
        productLicense,
        clmLicenseManager,
        samlConfigurationService,
        mailConfigurationDAO,
        proxyServerConfigurationDAO,
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

  private InputStream getRequiredResource(String path) {
    InputStream inputStream = getClass().getResourceAsStream(path);
    assertThat(inputStream).as("Missing test resource %s", path).isNotNull();
    return inputStream;
  }
}
