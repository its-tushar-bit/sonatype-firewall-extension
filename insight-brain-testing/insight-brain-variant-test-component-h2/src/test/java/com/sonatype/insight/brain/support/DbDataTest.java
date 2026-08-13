/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.support;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.sonatype.insight.brain.dataaccess.configuration.CiIntegrationsConfigDao;
import com.sonatype.insight.brain.dataaccess.configuration.webhook.WebhookDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.CiIntegrationsConfig;
import com.sonatype.insight.brain.model.configuration.CpeMatchingConfiguration;
import com.sonatype.insight.brain.model.configuration.ReverseProxyAuthenticationConfiguration;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.configuration.crowd.CrowdConfiguration;
import com.sonatype.insight.brain.model.configuration.oauth2.OAuth2Configuration;
import com.sonatype.insight.brain.model.configuration.oauth2.OidcConfiguration;
import com.sonatype.insight.brain.model.configuration.webhook.Webhook;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.successmetrics.SuccessMetricsService;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.hds.TelemetryId.TELEMETRY_GENERATED_INSTANCE_ID_PROPNAME;
import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.ADVANCED_SEARCH_ENABLED;
import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.AUTOMATIC_APPLICATION_CREATION_ENABLED;
import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.AUTOMATIC_APPLICATION_CREATION_ORGANIZATION_ID;
import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_ENABLED;
import static com.sonatype.insight.brain.model.configuration.webhook.WebhookEventType.POLICY_MANAGEMENT;
import static com.sonatype.nexus.scm.SourceControlProvider.BITBUCKET;
import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;

@ComponentH2Test
public class DbDataTest
    extends AbstractComponentH2Test
{
  @Inject
  private WebhookDAO webhookDAO;

  @Inject
  private CiIntegrationsConfigDao ciIntegrationsConfigDao;

  @Inject
  private DbData dbData;

  private Webhook getWebhook() {
    @SuppressWarnings("unchecked")
    final List<Webhook> webhooks = (List<Webhook>) dbData.getWebhook().getValue();
    assertThat(webhooks).hasSize(1);
    return webhooks.get(0);
  }

  @Test
  public void testGetWebhook_maskSecret() {
    tempEntity.newWebhookWithSecret(Collections.singleton(POLICY_MANAGEMENT));

    assertThat(getWebhook().getSecretKey()).isEqualTo(SystemInfo.MASK);
  }

  @Test
  public void testGetWebhook_secretEmpty() {
    final Webhook tempWebhook = tempEntity.newWebhook(Collections.singleton(POLICY_MANAGEMENT));
    tempWebhook.setSecretKey("");
    webhookDAO.update(tempWebhook);

    assertThat(getWebhook().getSecretKey()).isEqualTo("");
  }

  @Test
  public void testGetWebhook_secretNull() {
    tempEntity.newWebhook(Collections.singleton(POLICY_MANAGEMENT));

    assertThat(getWebhook().getSecretKey()).isNull();
  }

  @Test
  public void testGetSourceControl_maskToken() {
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, "user", "token", BITBUCKET, true, true,
        "base_branch", new Date());

    @SuppressWarnings({"unchecked"})
    List<SourceControl> sourceControls = (List<SourceControl>) dbData.getSourceControl().getValue();

    assertThat(sourceControls).singleElement()
        .satisfies(sourceControl -> assertThat(sourceControl.getToken()).isEqualTo(SystemInfo.MASK));
  }

  @Test
  public void testGetSourceControl_tokenEmpty() {
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, "user", "", BITBUCKET, true, true,
        "base_branch", new Date());

    @SuppressWarnings({"unchecked"})
    List<SourceControl> sourceControls = (List<SourceControl>) dbData.getSourceControl().getValue();

    assertThat(sourceControls).singleElement()
        .satisfies(sourceControl -> assertThat(sourceControl.getToken()).isEqualTo(""));
  }

  @Test
  public void testGetSourceControl_tokenNull() {
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, "user", null, BITBUCKET, true, true,
        "base_branch", new Date());

    @SuppressWarnings({"unchecked"})
    List<SourceControl> sourceControls = (List<SourceControl>) dbData.getSourceControl().getValue();

    assertThat(sourceControls).singleElement()
        .satisfies(sourceControl -> assertThat(sourceControl.getToken()).isNull());
  }

  @Test
  public void testGetSourceControl_repositoryUrlDoesNotContainCredentials() {
    Application application = tempEntity.newApplicationWithParent();
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, null, null, BITBUCKET, true, true, "master", new Date());
    tempEntity.newSourceControl(application.getId(), "https://example.com/scm/project/repo.git",
        "admin", "admin", null, true, true, "base_branch", new Date());

    @SuppressWarnings({"unchecked"})
    List<SourceControl> sourceControls = (List<SourceControl>) dbData.getSourceControl().getValue();

    assertThat(sourceControls).extracting(SourceControl::getRepositoryUrl)
        .filteredOn(Objects::nonNull)
        .containsOnly("https://example.com/scm/project/repo.git"); // .git is preserved as well
  }

  @Test
  public void testGetSourceControl_repositoryUrlContainsCredentials() {
    // given: a stored url with embedded credentials
    Application application = tempEntity.newApplicationWithParent();
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, null, null, BITBUCKET, true, true, "master", new Date());
    tempEntity.newSourceControl(application.getId(), "https://foo:bar@example.com/scm/project/repo",
        "admin", "admin", null, true, true, "base_branch", new Date());

    // when: querying SourceControl records
    @SuppressWarnings({"unchecked"})
    List<SourceControl> sourceControls = (List<SourceControl>) dbData.getSourceControl().getValue();

    // then: embedded credentials are stripped from the value included in support information
    assertThat(sourceControls).extracting(SourceControl::getRepositoryUrl)
        .filteredOn(Objects::nonNull)
        .containsOnly("https://****:****@example.com/scm/project/repo");
  }

  @Test
  public void testGetSourceControl_repositoryUrlContainsUsername() {
    // given: a stored url with embedded username
    Application application = tempEntity.newApplicationWithParent();
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, null, null, BITBUCKET, true, true, "master", new Date());
    tempEntity.newSourceControl(application.getId(), "https://foo@example.com/scm/project/repo",
        "admin", "admin", null, true, true, "base_branch", new Date());

    // when: querying SourceControl records
    @SuppressWarnings({"unchecked"})
    List<SourceControl> sourceControls = (List<SourceControl>) dbData.getSourceControl().getValue();

    // then: embedded username is stripped from the value included in support information
    assertThat(sourceControls).extracting(SourceControl::getRepositoryUrl)
        .filteredOn(Objects::nonNull)
        .containsOnly("https://****:****@example.com/scm/project/repo");
  }

  @Test
  public void testGetReverseProxyAuthenticationConfiguration() {
    tempEntity.newReverseProxyAuthenticationConfiguration(true, "header", true, "logoutUrl");

    ReverseProxyAuthenticationConfiguration configuration =
        (ReverseProxyAuthenticationConfiguration) dbData.getReverseProxyAuthenticationConfiguration().getValue();

    assertThat(configuration.isEnabled()).isTrue();
    assertThat(configuration.getUsernameHeader()).isEqualTo("header");
    assertThat(configuration.isCsrfProtectionDisabled()).isTrue();
    assertThat(configuration.getLogoutUrl()).isEqualTo("logoutUrl");
  }

  @Test
  public void testGetSystemConfiguration() {
    if (systemConfigurationPropertyDAO.getByName(TELEMETRY_GENERATED_INSTANCE_ID_PROPNAME) == null) {
      // We need TELEMETRY_GENERATED_INSTANCE_ID_PROPNAME which is not available by default in sample data
      tempEntity.newSystemConfigurationProperty(TELEMETRY_GENERATED_INSTANCE_ID_PROPNAME, "Sensitive. Must be masked.");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    List<SystemConfigurationProperty> sysConfigs = (List) dbData.getSystemConfiguration().getValue();
    Map<String, String> sysProps =
        sysConfigs.stream().collect(toMap(SystemConfigurationProperty::getName, SystemConfigurationProperty::getValue));

    assertThat(sysProps)
        .containsEntry(AUTOMATIC_APPLICATION_CREATION_ENABLED, "false")
        .containsEntry(AUTOMATIC_APPLICATION_CREATION_ORGANIZATION_ID, "")
        .containsEntry(SuccessMetricsService.PROPERTY_ENABLED, "true")
        .containsEntry(TELEMETRY_GENERATED_INSTANCE_ID_PROPNAME, SystemInfo.MASK)
        .containsEntry(AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_ENABLED, "false")
        .containsEntry(ADVANCED_SEARCH_ENABLED, "false");
  }

  @Test
  public void testGetCpeMatchingConfiguration_withOrgAndAppConfigs() {
    // Create organization with CPE config
    Organization org = tempEntity.newOrganization();
    tempEntity.newCpeMatchingConfiguration(org.getId(), true, true);

    // Create application with CPE config
    Application app = tempEntity.newApplication(org.getId());
    tempEntity.newCpeMatchingConfiguration(app.getId(), false, false);

    @SuppressWarnings({"unchecked"})
    List<CpeMatchingConfiguration> cpeConfigs =
        (List<CpeMatchingConfiguration>) dbData.getCpeMatchingConfiguration().getValue();

    assertThat(cpeConfigs).hasSize(2);
    assertThat(cpeConfigs).anySatisfy(config -> {
      assertThat(config.getOwnerId()).isEqualTo(org.getId());
      assertThat(config.isCpeEnabled()).isTrue();
      assertThat(config.isAllowOverride()).isTrue();
    });
    assertThat(cpeConfigs).anySatisfy(config -> {
      assertThat(config.getOwnerId()).isEqualTo(app.getId());
      assertThat(config.isCpeEnabled()).isFalse();
      assertThat(config.isAllowOverride()).isFalse();
    });
  }

  @Test
  public void testGetCpeMatchingConfiguration_noConfigs() {
    @SuppressWarnings({"unchecked"})
    List<CpeMatchingConfiguration> cpeConfigs =
        (List<CpeMatchingConfiguration>) dbData.getCpeMatchingConfiguration().getValue();

    assertThat(cpeConfigs).isNotNull();
    // May have root org config from sample data, so just check it's a list
    assertThat(cpeConfigs).isInstanceOf(List.class);
  }

  @Test
  public void testGetCpeMatchingConfiguration_nullValues() {
    Organization org = tempEntity.newOrganization();
    tempEntity.newCpeMatchingConfiguration(org.getId(), null, true);

    @SuppressWarnings({"unchecked"})
    List<CpeMatchingConfiguration> cpeConfigs =
        (List<CpeMatchingConfiguration>) dbData.getCpeMatchingConfiguration().getValue();

    assertThat(cpeConfigs).anySatisfy(config -> {
      if (config.getOwnerId().equals(org.getId())) {
        assertThat(config.isCpeEnabled()).isNull();
        assertThat(config.isAllowOverride()).isTrue();
      }
    });
  }

  @Test
  public void testGetCiIntegrationsConfig_maskUrlCredentials() throws IOException {
    Organization org = tempEntity.newOrganization();
    String configJson = """
        {
          "download": {
            "iqCliUrl": "https://user:pass@example.com/iq-cli/download"
          },
          "scanPatterns": ["**/*.jar"]
        }
        """;

    CiIntegrationsConfig ciConfig = new CiIntegrationsConfig(org.getId(), "ORGANIZATION", configJson);
    ciIntegrationsConfigDao.save(ciConfig);

    @SuppressWarnings({"unchecked"})
    List<CiIntegrationsConfig> configs = (List<CiIntegrationsConfig>) dbData.getCiIntegrationsConfig().getValue();

    assertThat(configs).anySatisfy(config -> {
      if (config.getOwnerId().equals(org.getId())) {
        assertThat(config.getConfigurationJson()).contains("https://****:****@example.com/iq-cli/download");
        assertThat(config.getConfigurationJson()).doesNotContain("user:pass");
        assertThat(config.getConfigurationJson()).contains("scanPatterns");
      }
    });
  }

  @Test
  public void testGetCiIntegrationsConfig_urlWithoutCredentials() throws IOException {
    Organization org = tempEntity.newOrganization();
    String configJson = """
        {
          "download": {
            "iqCliUrl": "https://example.com/iq-cli/download"
          },
          "scanPatterns": ["**/*.jar"]
        }
        """;

    CiIntegrationsConfig ciConfig = new CiIntegrationsConfig(org.getId(), "ORGANIZATION", configJson);
    ciIntegrationsConfigDao.save(ciConfig);

    @SuppressWarnings({"unchecked"})
    List<CiIntegrationsConfig> configs = (List<CiIntegrationsConfig>) dbData.getCiIntegrationsConfig().getValue();

    assertThat(configs).anySatisfy(config -> {
      if (config.getOwnerId().equals(org.getId())) {
        assertThat(config.getConfigurationJson()).contains("https://example.com/iq-cli/download");
        assertThat(config.getConfigurationJson()).contains("scanPatterns");
      }
    });
  }

  @Test
  public void testGetOAuth2Configuration_asymmetricAlgorithm_doesNotMaskIdpJwks() {
    OAuth2Configuration oAuth2Configuration =
        tempEntity.newOAuth2Configuration("https://an-idp", "RS256", "https://an-idp/jwks.json", "public-key-material");

    @SuppressWarnings("unchecked")
    List<OAuth2Configuration> result = (List<OAuth2Configuration>) dbData.getOAuth2Configuration().getValue();

    assertThat(result).hasSize(1);
    assertThat(result.get(0)).usingRecursiveComparison().isEqualTo(oAuth2Configuration);
  }

  @Test
  public void testGetOAuth2Configuration_symmetricAlgorithm_masksIdpJwks() {
    OAuth2Configuration oAuth2Configuration =
        tempEntity.newOAuth2Configuration("https://an-idp", "HS256", "https://an-idp/jwks.json", "secret-key-material");

    @SuppressWarnings("unchecked")
    List<OAuth2Configuration> result = (List<OAuth2Configuration>) dbData.getOAuth2Configuration().getValue();

    assertThat(result).hasSize(1);
    assertThat(result.get(0)).usingRecursiveComparison().ignoringFields("idpJwks").isEqualTo(oAuth2Configuration);
    assertThat(result.get(0).getIdpJwks()).isEqualTo(SystemInfo.MASK);
  }

  @Test
  public void testGetOAuth2Configuration_unknownAlgorithm_masksIdpJwks() {
    OAuth2Configuration oAuth2Configuration =
        tempEntity.newOAuth2Configuration("https://an-idp", "other", "https://an-idp/jwks.json", "secret-key-material");

    @SuppressWarnings("unchecked")
    List<OAuth2Configuration> result = (List<OAuth2Configuration>) dbData.getOAuth2Configuration().getValue();

    assertThat(result).hasSize(1);
    assertThat(result.get(0)).usingRecursiveComparison().ignoringFields("idpJwks").isEqualTo(oAuth2Configuration);
    assertThat(result.get(0).getIdpJwks()).isEqualTo(SystemInfo.MASK);
  }

  @Test
  public void testGetOAuth2Configuration_noConfiguration() {
    @SuppressWarnings("unchecked")
    List<OAuth2Configuration> result = (List<OAuth2Configuration>) dbData.getOAuth2Configuration().getValue();

    assertThat(result).isEmpty();
  }

  @Test
  public void testGetOidcConfiguration() {
    OidcConfiguration oidcConfiguration =
        tempEntity.newOidcConfiguration("https://idp.example.com", "client-id", "super-secret",
            "https://idp.example.com/auth", "https://idp.example.com/token");

    OidcConfiguration result = (OidcConfiguration) dbData.getOidcConfiguration().getValue();

    assertThat(result).isNotNull();
    assertThat(result).usingRecursiveComparison().ignoringFields("clientSecret").isEqualTo(oidcConfiguration);
    assertThat(result.getClientSecret()).isEqualTo(SystemInfo.MASK);
  }

  @Test
  public void testGetOidcConfiguration_noConfiguration() {
    OidcConfiguration result = (OidcConfiguration) dbData.getOidcConfiguration().getValue();

    assertThat(result).isNull();
  }

  @Test
  public void testGetCrowdConfiguration() {
    CrowdConfiguration crowdConfiguration = tempEntity.newCrowdConfiguration();

    CrowdConfiguration result = (CrowdConfiguration) dbData.getCrowdConfiguration().getValue();

    assertThat(result).isNotNull();
    assertThat(result).usingRecursiveComparison().ignoringFields("applicationPassword").isEqualTo(crowdConfiguration);
    assertThat(result.getApplicationPassword()).isEqualTo(SystemInfo.MASK.toCharArray());
  }

  @Test
  public void testGetCrowdConfiguration_noConfiguration() {
    CrowdConfiguration result = (CrowdConfiguration) dbData.getCrowdConfiguration().getValue();

    assertThat(result).isNull();
  }

  @Test
  public void testGetCiIntegrationsConfig_nullDownloadConfig() throws IOException {
    Organization org = tempEntity.newOrganization();
    String configJson = """
        {
          "scanPatterns": ["**/*.jar"],
          "moduleExcludes": ["**/test/**"]
        }
        """;

    CiIntegrationsConfig ciConfig = new CiIntegrationsConfig(org.getId(), "ORGANIZATION", configJson);
    ciIntegrationsConfigDao.save(ciConfig);

    @SuppressWarnings({"unchecked"})
    List<CiIntegrationsConfig> configs = (List<CiIntegrationsConfig>) dbData.getCiIntegrationsConfig().getValue();

    assertThat(configs).anySatisfy(config -> {
      if (config.getOwnerId().equals(org.getId())) {
        assertThat(config.getConfigurationJson()).contains("scanPatterns");
        assertThat(config.getConfigurationJson()).contains("moduleExcludes");
      }
    });
  }
}
