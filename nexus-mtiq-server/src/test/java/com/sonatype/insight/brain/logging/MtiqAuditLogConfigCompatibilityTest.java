/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.logging;

import static org.assertj.core.api.Assertions.assertThat;

import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.MultiTenantInsightConfig;
import com.sonatype.insight.brain.spring.config.DropwizardConfigConfiguration;
import com.sonatype.insight.brain.spring.config.DropwizardConfigLoader;
import java.io.File;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.StandardEnvironment;

public class MtiqAuditLogConfigCompatibilityTest
{
  private static final File CONFIG_FILE =
      new File("src/test/resources/config-test-prod-logging.yml");

  @Test
  public void shouldExtractAuditLogBasePathFromProdMtiqConfig() throws Exception {
    StandardEnvironment environment = new StandardEnvironment();
    new DropwizardConfigLoader().loadConfig(CONFIG_FILE, environment);

    assertThat(environment.getProperty("auditLogBasePath")).isEqualTo("/sonatype-work/clm-cluster");
    assertThat(environment.getProperty("logging.level.root")).isEqualTo("DEBUG");
    assertThat(environment.getProperty("logging.level.org.eclipse.jetty")).isEqualTo("INFO");
  }

  @Test
  public void shouldStrictDeserializeProdMtiqConfigIntoMultiTenantInsightConfig() throws Exception {
    InsightConfig insightConfig = new DropwizardConfigConfiguration()
        .insightConfig(CONFIG_FILE.getAbsolutePath(), MultiTenantInsightConfig.class.getName(), false);

    assertThat(insightConfig).isInstanceOf(MultiTenantInsightConfig.class);
    assertThat(insightConfig.getLogging()).isNotNull();
  }
}
