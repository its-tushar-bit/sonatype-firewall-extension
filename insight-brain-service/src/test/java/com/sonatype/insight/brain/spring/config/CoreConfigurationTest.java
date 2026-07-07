/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.spring.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.google.common.cache.CacheBuilderSpec;
import com.sonatype.insight.brain.api.v2.service.ConfigurationUtils;
import com.sonatype.insight.brain.dataaccess.ComponentCategoryDAO;
import com.sonatype.insight.brain.dataaccess.component.HashComponentIdentifierDAO;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyVulnerabilityDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.VulnerabilityGroupDAO;
import com.sonatype.insight.brain.hds.ComponentDetailsLoader;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.policy.conditions.ConditionTypes;
import com.sonatype.insight.brain.model.policy.conditions.LicenseThreatGroupConditionType;
import com.sonatype.insight.brain.utils.DefaultExecutorThreadPools;
import com.sonatype.insight.brain.utils.ExecutorThreadPools;
import com.sonatype.insight.dataaccess.TransactionContext;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import io.dropwizard.jackson.AnnotationSensitivePropertyNamingStrategy;
import io.dropwizard.jackson.DiscoverableSubtypeResolver;
import io.dropwizard.jackson.JsonSnakeCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CoreConfigurationTest
{
  @Mock
  private SystemConfigurationPropertyDAO systemConfigurationPropertyDAO;

  @Mock
  private ComponentCategoryDAO componentCategoryDAO;

  @Mock
  private LicenseDAO licenseDAO;

  @Mock
  private LicenseThreatGroupDAO licenseThreatGroupDAO;

  @Mock
  private LabelDAO labelDAO;

  @Mock
  private VulnerabilityGroupDAO vulnerabilityGroupDAO;

  @Mock
  private RepositoryDAO repositoryDAO;

  @Mock
  private ThirdPartyVulnerabilityDAO thirdPartyVulnerabilityDAO;

  @Mock
  private HashComponentIdentifierDAO hashComponentIdentifierDAO;

  @Mock
  private MultiLicenseDAO multiLicenseDAO;

  @Mock
  private TransactionContext transactionContext;

  private Object savedExecutorThreadPoolsInstance;

  @Before
  public void resetLegacyStaticInjections() throws Exception {
    ((Map<?, ?>) getStaticField(ConditionTypes.class, "allConditionTypes")).clear();
    setStaticField(ConfigurationUtils.class, "systemConfigurationPropertyDAO", null);
    setStaticField(ComponentDetailsLoader.class, "hashComponentIdentifierDAO", null);
    setStaticField(ComponentDetailsLoader.class, "multiLicenseDAO", null);
    savedExecutorThreadPoolsInstance = getStaticField(ExecutorThreadPools.class, "INSTANCE");
    setStaticField(ExecutorThreadPools.class, "INSTANCE", null);

    when(systemConfigurationPropertyDAO.get(transactionContext, SystemConfigurationProperty.BASE_URL))
        .thenReturn("http://localhost:8070/");
    when(hashComponentIdentifierDAO.getByHashes(anyList())).thenReturn(List.of());
  }

  @After
  public void restoreExecutorThreadPools() throws Exception {
    ExecutorThreadPools testInstance = (ExecutorThreadPools) getStaticField(ExecutorThreadPools.class, "INSTANCE");
    setStaticField(ExecutorThreadPools.class, "INSTANCE", savedExecutorThreadPoolsInstance);
    if (testInstance != null && testInstance != savedExecutorThreadPoolsInstance) {
      testInstance.shutdown();
    }
  }

  @Test
  public void objectMapper_shouldUseLegacyDropwizardCompatibilityConfiguration() throws Exception {
    ObjectMapper mapper = new CoreConfiguration().objectMapper();

    assertThat(mapper.getPropertyNamingStrategy()).isInstanceOf(AnnotationSensitivePropertyNamingStrategy.class);
    assertThat(mapper.getSubtypeResolver()).isInstanceOf(DiscoverableSubtypeResolver.class);
    assertThat(mapper.isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)).isTrue();
    assertThat(mapper.writeValueAsString(new SnakeCaseDto("value"))).contains("camel_case_value");
    assertThat(mapper.readValue("\"maximumSize=10\"", CacheBuilderSpec.class).toParsableString())
        .isEqualTo("maximumSize=10");
  }

  @Test
  public void csvMapper_shouldUseLegacyDropwizardCompatibilityConfiguration() {
    CsvMapper mapper = new CoreConfiguration().csvMapper();

    assertThat(mapper.getPropertyNamingStrategy()).isInstanceOf(AnnotationSensitivePropertyNamingStrategy.class);
    assertThat(mapper.getSubtypeResolver()).isInstanceOf(DiscoverableSubtypeResolver.class);
  }

  @Test
  public void staticInjectionInitializer_shouldInitializeLegacyStaticInjections() throws Exception {
    CoreConfiguration configuration = new CoreConfiguration();

    configuration.staticInjectionInitializer(
        systemConfigurationPropertyDAO,
        componentCategoryDAO,
        licenseDAO,
        licenseThreatGroupDAO,
        labelDAO,
        vulnerabilityGroupDAO,
        repositoryDAO,
        thirdPartyVulnerabilityDAO,
        hashComponentIdentifierDAO,
        multiLicenseDAO,
        new DefaultExecutorThreadPools());

    assertThat(ConditionTypes.getById(LicenseThreatGroupConditionType.ID).getId())
        .isEqualTo(LicenseThreatGroupConditionType.ID);
    assertThat(ComponentDetailsLoader.getComponentDetailsLocallyByHashes(List.of("sha1"))).isEmpty();
    assertThat(ConfigurationUtils.forceBaseUrlToString(transactionContext, true))
        .isEqualTo("true");
  }

  private static Object getStaticField(Class<?> type, String name) throws Exception {
    Field field = type.getDeclaredField(name);
    field.setAccessible(true);
    return field.get(null);
  }

  private static void setStaticField(Class<?> type, String name, Object value) throws Exception {
    Field field = type.getDeclaredField(name);
    field.setAccessible(true);
    field.set(null, value);
  }

  @JsonSnakeCase
  private static class SnakeCaseDto
  {
    public String camelCaseValue;

    private SnakeCaseDto(String camelCaseValue) {
      this.camelCaseValue = camelCaseValue;
    }
  }
}
