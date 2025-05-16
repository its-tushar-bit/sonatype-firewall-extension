/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.zscaler;

import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.configuration.ZScalerConfigurationDAO;
import com.sonatype.insight.brain.model.configuration.ZScalerConfiguration;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.zscaler.ApiZScalerConfigurationService.ApiZScalerConfigurationDTO;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static com.sonatype.insight.brain.zscaler.ApiZScalerConfigurationService.EULA_MESSAGE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;

@RunWith(MockitoJUnitRunner.class)
public class ApiZScalerConfigurationServiceTest
    extends AbstractComponentTest
{
  @Inject
  private ZScalerConfigurationDAO zScalerConfigurationDAO;

  @Inject
  private PasswordHandler passwordHandler;

  @Inject
  private ApiZScalerConfigurationService underTest;

  @Before
  @Override
  public void setUp() throws Exception {
    super.setUp();
    underTest = new ApiZScalerConfigurationService(zScalerConfigurationDAO, passwordHandler);
  }

  @Test
  public void testGetConfiguration() {
    ZScalerConfiguration config = tempEntity.newZScalerConfiguration("user", "password", "host", "apikey");

    ApiZScalerConfigurationDTO dto = underTest.getConfiguration();

    assert (dto.getUsername().equals(config.getUsername()));
    assert (dto.getHostname().equals(config.getHostname()));
    assertThat(dto.getPassword()).isNull();
    assert (dto.getApiKey().equals(config.getApikey()));
    assert (dto.isEulaAgreed().equals(true));
  }

  @Test
  public void testGetConfiguration_notFoundException() {
    assertThrows("Zscaler not configured.", NotFoundException.class, () -> underTest.getConfiguration());
  }

  @Test
  public void testSetConfiguration() {
    ApiZScalerConfigurationDTO dto = new ApiZScalerConfigurationDTO();
    dto.setUsername("testusername");
    dto.setPassword("testpassword");
    dto.setHostname("testhostname");
    dto.setApiKey("testapikey");
    dto.setEulaAgreed(true);

    String response = underTest.setConfiguration(dto);

    ZScalerConfiguration config = zScalerConfigurationDAO.get();
    assert (config.getUsername().equals(dto.getUsername()));
    assert (config.getHostname().equals(dto.getHostname()));
    assert (passwordHandler.decryptPassword(config.getPassword()).equals(dto.getPassword()));
    assert (config.getApikey().equals(dto.getApiKey()));
    assertThat(response).isEqualTo(String.format("You have acknowledged and agreed that %s", EULA_MESSAGE));
  }

  @Test
  public void testSetConfiguration_notFoundException() {
    assertThrows("Configuration is required.", NotFoundException.class, () -> underTest.setConfiguration(null));
  }

  @Test
  public void testSetConfiguration_badRequestException() {
    ApiZScalerConfigurationDTO dto = new ApiZScalerConfigurationDTO();
    dto.setUsername("testusername");
    dto.setPassword("testpassword");
    dto.setHostname("testhostname");
    dto.setEulaAgreed(false);
    assertThrows(String.format("You must acknowledge and agree that %s", EULA_MESSAGE), BadRequestException.class,
        () -> underTest.setConfiguration(dto));
  }

  @Test
  public void testDeleteConfiguration() {
    tempEntity.newZScalerConfiguration("user", "password", "host", "apikey");

    underTest.deleteConfiguration();

    ZScalerConfiguration config = zScalerConfigurationDAO.get();
    assertThat(config).isNull();
  }

  @Test
  public void testDeleteConfiguration_notFoundException() {
    assertThrows("Zscaler not configured.", NotFoundException.class, () -> underTest.deleteConfiguration());
  }
}
