/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.service.config.MultiTenantConfigurationDefaultsService;

import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.AUTOMATIC_QUARANTINE_RELEASE_TIME_INTERVAL_IN_MINUTES;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * MTIQ variant conversion of {@code MultiTenantConfigurationDefaultsServiceTest} (which extended
 * {@code AbstractMultiTenantBaseIntegrationTest}). No base class, an injected {@link MtiqTestContext} supplies
 * the reused multi-tenant server and global-tenant access.
 */
@MtiqTest
class MtiqConfigurationDefaultsServiceTest
{
  private MtiqTestContext ctx;

  @Test
  void shouldSetGlobalConfigurationOnSystemStart() {
    ctx.testAsGlobal(t -> {
      // MultiTenantConfigurationDefaultsService.register() is the GlobalTenantJob that seeds these
      // defaults at system start. The reused MTIQ test server resets the database between tests, so
      // run that same registration here before asserting its effect.
      ctx.lookup(MultiTenantConfigurationDefaultsService.class).register();

      SystemConfigurationPropertyDAO dao = ctx.lookup(SystemConfigurationPropertyDAO.class);

      assertThat(dao.get(AUTOMATIC_QUARANTINE_RELEASE_TIME_INTERVAL_IN_MINUTES)).isEqualTo("120");
    });
  }
}
