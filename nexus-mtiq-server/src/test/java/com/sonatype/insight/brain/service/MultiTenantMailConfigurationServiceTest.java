/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sonatype.insight.brain.dataaccess.configuration.MailConfigurationDAO;
import com.sonatype.insight.brain.migration.MailConfigurationMigrator.MailConfig;
import com.sonatype.insight.brain.model.configuration.MailConfiguration;
import com.sonatype.insight.brain.security.PasswordHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class MultiTenantMailConfigurationServiceTest
{
  @Mock
  private MailConfigurationDAO mailConfigurationDAO;

  @Mock
  private InsightConfig insightConfig;

  @Mock
  private PasswordHandler passwordHandler;

  private MultiTenantMailConfigurationService underTest;

  @BeforeEach
  public void setup() {
    underTest = new MultiTenantMailConfigurationService(mailConfigurationDAO,
        insightConfig,
        passwordHandler);
  }

  @Test
  public void testStart_ThereIsMailConfig() throws Exception {
    MailConfig mailConfig = new MailConfig();
    mailConfig.setHostname("mail.example.com");
    mailConfig.setPort(12345);
    mailConfig.setUsername("testUsername");
    mailConfig.setPassword("somepass".toCharArray());
    mailConfig.setSystemEmail("noreply@example.com");

    when(insightConfig.getMailConfig()).thenReturn(mailConfig);
    when(passwordHandler.encryptPassword(any(char[].class))).thenReturn("zzzz".toCharArray());

    underTest.init();

    verify(insightConfig).getMailConfig();
    verify(passwordHandler).encryptPassword("somepass".toCharArray());

    ArgumentCaptor<MailConfiguration> argumentCaptor = ArgumentCaptor.forClass(MailConfiguration.class);
    verify(mailConfigurationDAO).set(argumentCaptor.capture());
    MailConfiguration mailConfiguration = argumentCaptor.getValue();
    assertThat(mailConfiguration.getPassword()).contains("zzzz".toCharArray());
  }

  @Test
  public void testStart_ThereIsNotMailConfig() throws Exception {
    underTest.init();
    verify(mailConfigurationDAO, never()).set(any());
  }
}
