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
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.zscaler.ApiZScalerConfigurationService.EULA_MESSAGE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ComponentH2Test
public class ApiZScalerConfigurationServiceTest
    extends AbstractComponentH2Test
{
  @Inject
  private ZScalerConfigurationDAO zScalerConfigurationDAO;

  @Inject
  private ZscalerFormatDAO zscalerFormatDAO;

  @Inject
  private PasswordHandler passwordHandler;

  @Inject
  private ApiZScalerConfigurationService underTest;

  @BeforeEach
  @Override
  public void setUp() throws Exception {
    super.setUp();
    underTest = new ApiZScalerConfigurationService(zScalerConfigurationDAO, zscalerFormatDAO, passwordHandler);
  }

  @Test
  public void testGetConfiguration() {
    ZScalerConfiguration config = tempEntity.newZScalerConfiguration("user", "password",
        "https://api.zscaler.net", "validapikey1", true, false, false, true);

    ApiZScalerConfigurationDTO dto = underTest.getConfiguration();

    assert (dto.getUsername().equals(config.getUsername()));
    assert (dto.getHostname().equals(config.getHostname()));
    assertThat(dto.getPassword()).isNull();
    assert (dto.getApiKey().equals(config.getApikey()));
    assert (dto.isEulaAgreed().equals(true));
  }

  @Test
  public void testGetConfiguration_notFoundException() {
    assertThrows(NotFoundException.class, () -> underTest.getConfiguration(), "Zscaler not configured.");
  }

  @Test
  public void testSetConfiguration() {
    ApiZScalerConfigurationDTO dto = new ApiZScalerConfigurationDTO();
    dto.setUsername("testusername");
    dto.setPassword("testpassword");
    dto.setHostname("https://api.zscaler.net");
    dto.setApiKey("testapikey12");
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
    assertThrows(NotFoundException.class, () -> underTest.setConfiguration(null), "Configuration is required.");
  }

  @Test
  public void testSetConfiguration_badRequestExceptionWhenEulaNotAgreed() {
    ApiZScalerConfigurationDTO dto = new ApiZScalerConfigurationDTO();
    dto.setUsername("testusername");
    dto.setPassword("testpassword");
    dto.setHostname("https://api.zscaler.net");
    dto.setEulaAgreed(false);
    assertThrows(BadRequestException.class,
        () -> underTest.setConfiguration(dto),
        String.format("You must acknowledge and agree that %s", EULA_MESSAGE));
  }

  @Test
  public void testSetConfiguration_badRequestExceptionWhenformatNotEnabled() {
    ApiZScalerConfigurationDTO dto = new ApiZScalerConfigurationDTO();
    dto.setUsername("testusername");
    dto.setPassword("testpassword");
    dto.setHostname("https://api.zscaler.net");
    dto.setEulaAgreed(true);
    assertThrows(BadRequestException.class,
        () -> underTest.setConfiguration(dto), "At least one format must be enabled.");
  }

  @Test
  public void testDeleteConfiguration() {
    tempEntity.newZScalerConfiguration("user", "password", "https://api.zscaler.net", "validapikey1",
        true, false, false, false);

    underTest.deleteConfiguration();

    ZScalerConfiguration config = zScalerConfigurationDAO.get();
    assertThat(config).isNull();
  }

  @Test
  public void testDeleteConfiguration_notFoundException() {
    assertThrows(NotFoundException.class, () -> underTest.deleteConfiguration(), "Zscaler not configured.");
  }

  @Test
  public void testSetConfiguration_integratesWithUrlValidation_noHostname() {
    ApiZScalerConfigurationDTO dto = new ApiZScalerConfigurationDTO();
    dto.setUsername("testusername");
    dto.setPassword("testpassword");
    dto.setHostname("https://");
    dto.setApiKey("validapikey1");
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
    dto.setApiKey("testapikey12");
    dto.setMavenFormatEnabled(true);
    dto.setEulaAgreed(true);

    BadRequestException exception = assertThrows(BadRequestException.class,
        () -> underTest.setConfiguration(dto));

    assertThat(exception.getMessage()).isEqualTo("Protocol must be http or https");
  }

  @Test
  public void testSetConfiguration_apiKeyValidation_missingApiKey() {
    ApiZScalerConfigurationDTO dto = new ApiZScalerConfigurationDTO();
    dto.setUsername("testusername");
    dto.setPassword("testpassword");
    dto.setHostname("https://api.zscaler.net");
    dto.setApiKey(null);
    dto.setMavenFormatEnabled(true);
    dto.setEulaAgreed(true);

    BadRequestException exception = assertThrows(BadRequestException.class,
        () -> underTest.setConfiguration(dto));

    assertThat(exception.getMessage()).isEqualTo("The apiKey is required.");
  }

  @Test
  public void testSetConfiguration_apiKeyValidation_emptyApiKey() {
    ApiZScalerConfigurationDTO dto = new ApiZScalerConfigurationDTO();
    dto.setUsername("testusername");
    dto.setPassword("testpassword");
    dto.setHostname("https://api.zscaler.net");
    dto.setApiKey("");
    dto.setMavenFormatEnabled(true);
    dto.setEulaAgreed(true);

    BadRequestException exception = assertThrows(BadRequestException.class,
        () -> underTest.setConfiguration(dto));

    assertThat(exception.getMessage()).isEqualTo("The apiKey is required.");
  }

  @Test
  public void testSetConfiguration_apiKeyValidation_blankApiKey() {
    ApiZScalerConfigurationDTO dto = new ApiZScalerConfigurationDTO();
    dto.setUsername("testusername");
    dto.setPassword("testpassword");
    dto.setHostname("https://api.zscaler.net");
    dto.setApiKey("   ");
    dto.setMavenFormatEnabled(true);
    dto.setEulaAgreed(true);

    BadRequestException exception = assertThrows(BadRequestException.class,
        () -> underTest.setConfiguration(dto));

    assertThat(exception.getMessage()).isEqualTo("The apiKey is required.");
  }

  @Test
  public void testSetConfiguration_apiKeyValidation_tooShort() {
    ApiZScalerConfigurationDTO dto = new ApiZScalerConfigurationDTO();
    dto.setUsername("testusername");
    dto.setPassword("testpassword");
    dto.setHostname("https://api.zscaler.net");
    dto.setApiKey("short");
    dto.setMavenFormatEnabled(true);
    dto.setEulaAgreed(true);

    BadRequestException exception = assertThrows(BadRequestException.class,
        () -> underTest.setConfiguration(dto));

    assertThat(exception.getMessage()).isEqualTo("The apiKey must be exactly 12 characters.");
  }

  @Test
  public void testSetConfiguration_apiKeyValidation_tooLong() {
    ApiZScalerConfigurationDTO dto = new ApiZScalerConfigurationDTO();
    dto.setUsername("testusername");
    dto.setPassword("testpassword");
    dto.setHostname("https://api.zscaler.net");
    dto.setApiKey("toolongapikey123");
    dto.setMavenFormatEnabled(true);
    dto.setEulaAgreed(true);

    BadRequestException exception = assertThrows(BadRequestException.class,
        () -> underTest.setConfiguration(dto));

    assertThat(exception.getMessage()).isEqualTo("The apiKey must be exactly 12 characters.");
  }

  @Test
  public void testSetConfiguration_apiKeyValidation_exactlyTwelveCharacters() {
    ApiZScalerConfigurationDTO dto = new ApiZScalerConfigurationDTO();
    dto.setUsername("testusername");
    dto.setPassword("testpassword");
    dto.setHostname("https://api.zscaler.net");
    dto.setApiKey("valid12chars");
    dto.setMavenFormatEnabled(true);
    dto.setEulaAgreed(true);

    String response = underTest.setConfiguration(dto);

    ZScalerConfiguration config = zScalerConfigurationDAO.get();
    assertThat(config.getApikey()).isEqualTo("valid12chars");
    assertThat(response).isEqualTo(String.format("You have acknowledged and agreed that %s", EULA_MESSAGE));
  }
}
