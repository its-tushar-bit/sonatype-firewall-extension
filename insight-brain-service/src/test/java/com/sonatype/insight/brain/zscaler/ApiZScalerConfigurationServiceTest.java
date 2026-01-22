/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.zscaler;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.dataaccess.configuration.ZScalerConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ZscalerFormatDAO;
import com.sonatype.insight.brain.model.configuration.ZScalerConfiguration;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static com.sonatype.insight.brain.zscaler.ApiZScalerConfigurationService.EULA_MESSAGE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import com.sonatype.insight.brain.common.test.SlowTest;
import org.junit.experimental.categories.Category;

@RunWith(MockitoJUnitRunner.class)
@Category(SlowTest.class)
public class ApiZScalerConfigurationServiceTest
    extends AbstractComponentTest
{
  @Inject
  private ZScalerConfigurationDAO zScalerConfigurationDAO;

  @Inject
  private ZscalerFormatDAO zscalerFormatDAO;

  @Inject
  private PasswordHandler passwordHandler;

  @Inject
  private ApiZScalerConfigurationService underTest;

  @Before
  @Override
  public void setUp() throws Exception {
    super.setUp();
    underTest = new ApiZScalerConfigurationService(zScalerConfigurationDAO, zscalerFormatDAO, passwordHandler);
  }

  @Test
  public void testGetConfiguration() {
    ZScalerConfiguration config = tempEntity.newZScalerConfiguration("user", "password",
        "https://api.zscaler.net", "apikey", true, false, false, true);

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
    dto.setHostname("https://api.zscaler.net");
    dto.setApiKey("testapikey");
    dto.setMavenFormatEnabled(true);
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
  public void testSetConfiguration_badRequestExceptionWhenEulaNotAgreed() {
    ApiZScalerConfigurationDTO dto = new ApiZScalerConfigurationDTO();
    dto.setUsername("testusername");
    dto.setPassword("testpassword");
    dto.setHostname("https://api.zscaler.net");
    dto.setEulaAgreed(false);
    assertThrows(String.format("You must acknowledge and agree that %s", EULA_MESSAGE), BadRequestException.class,
        () -> underTest.setConfiguration(dto));
  }

  @Test
  public void testSetConfiguration_badRequestExceptionWhenformatNotEnabled() {
    ApiZScalerConfigurationDTO dto = new ApiZScalerConfigurationDTO();
    dto.setUsername("testusername");
    dto.setPassword("testpassword");
    dto.setHostname("https://api.zscaler.net");
    dto.setEulaAgreed(true);
    assertThrows("At least one format must be enabled.", BadRequestException.class,
        () -> underTest.setConfiguration(dto));
  }

  @Test
  public void testDeleteConfiguration() {
    tempEntity.newZScalerConfiguration("user", "password", "https://api.zscaler.net", "apikey",
        true, false, false, false);

    underTest.deleteConfiguration();

    ZScalerConfiguration config = zScalerConfigurationDAO.get();
    assertThat(config).isNull();
  }

  @Test
  public void testDeleteConfiguration_notFoundException() {
    assertThrows("Zscaler not configured.", NotFoundException.class, () -> underTest.deleteConfiguration());
  }

  @Test
  public void testSetConfiguration_integratesWithUrlValidation_noHostname() {
    ApiZScalerConfigurationDTO dto = new ApiZScalerConfigurationDTO();
    dto.setUsername("testusername");
    dto.setPassword("testpassword");
    dto.setHostname("https://");
    dto.setApiKey("testapikey");
    dto.setMavenFormatEnabled(true);
    dto.setEulaAgreed(true);

    BadRequestException exception = assertThrows(BadRequestException.class,
        () -> underTest.setConfiguration(dto));

    assertThat(exception.getMessage()).isEqualTo("Not a valid URL");
  }

  @Test
  public void testSetConfiguration_integratesWithUrlValidation_invalidProtocol() {
    ApiZScalerConfigurationDTO dto = new ApiZScalerConfigurationDTO();
    dto.setUsername("testusername");
    dto.setPassword("testpassword");
    dto.setHostname("ftp://api.zscaler.net");
    dto.setApiKey("testapikey");
    dto.setMavenFormatEnabled(true);
    dto.setEulaAgreed(true);

    BadRequestException exception = assertThrows(BadRequestException.class,
        () -> underTest.setConfiguration(dto));

    assertThat(exception.getMessage()).isEqualTo("Protocol must be http or https");
  }
}
