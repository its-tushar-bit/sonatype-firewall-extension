/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.roi;

import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Stream;
import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.roi.RoiConfigurationDAO;
import com.sonatype.insight.brain.model.roi.RoiConfiguration;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.roi.dtos.RoiConfigurationDTO;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import com.sonatype.insight.brain.dataaccess.roi.RoiConfigurationDefaultValuesDAO;
import com.sonatype.insight.brain.model.roi.CurrencyTypes;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.license.model.ProductLicenseDetails;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import com.google.inject.Binder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;

public class RoiConfigurationServiceTest extends AbstractComponentTest
{
  @Inject
  private RoiConfigurationService roiConfigurationService;

  @Inject
  private RoiConfigurationDefaultValuesDAO dao;

  @Inject
  private RoiConfigurationDAO roiConfigurationDao;

  @Mock
  private TelemetrySender mockTelemetrySender;

  @Inject
  private TestProductLicense testProductLicense;

  @Override
  public void configure(Binder binder) {
    binder.bind(TelemetrySender.class).toInstance(mockTelemetrySender);
    super.configure(binder);
  }

  @Before
  public  void setup() {
    dao.getAll().forEach(dao::delete);
    tempEntity.createRoiConfigurationDefaultValues(
        CurrencyTypes.USD,
        BigDecimal.valueOf(100),
        BigDecimal.valueOf(50),
        3600L,
        1440L,
        BigDecimal.valueOf(12000),
        BigDecimal.valueOf(6000),
        true,
        BigDecimal.valueOf(24000),
        BigDecimal.valueOf(12000),
        true,
        BigDecimal.valueOf(72000),
        BigDecimal.valueOf(36000),
        false,
        BigDecimal.valueOf(144000),
        BigDecimal.valueOf(72000),
        false,
        BigDecimal.valueOf(4350000),
        BigDecimal.valueOf(500000),
        BigDecimal.valueOf(35000),
        BigDecimal.valueOf(10000),
        BigDecimal.valueOf(25000),
        BigDecimal.valueOf(5000),
        false
    );
  }

  @Test
  public void testGetRoiConfigurationByCurrencyType() {
    tempEntity.createRoiConfiguration(
        CurrencyTypes.USD,
        BigDecimal.valueOf(100),
        1448L,
        true,
        BigDecimal.valueOf(23000),
        true,
        BigDecimal.valueOf(30000),
        false,
        BigDecimal.valueOf(45000),
        false,
        BigDecimal.valueOf(40000),
        BigDecimal.valueOf(50000),
        BigDecimal.valueOf(60000),
        BigDecimal.valueOf(70000),
        false
    );
    RoiConfigurationCurrentAndMinimumValuesDTO roiConfigurationActual =
        roiConfigurationService.getCurrentAndMinimumValuesByCurrencyType("usd");
    assertThat(roiConfigurationActual).isNotNull();
    assertRoiConfigurationEntityValues(roiConfigurationActual);
  }

  @Test
  public void testGetCurrentAndMinimumValuesByCurrencyType_NotFound() {
    tempEntity.createRoiConfiguration(
        CurrencyTypes.USD,
        BigDecimal.valueOf(100),
        1448L,
        true,
        BigDecimal.valueOf(23000),
        true,
        BigDecimal.valueOf(30000),
        false,
        BigDecimal.valueOf(45000),
        false,
        BigDecimal.valueOf(40000),
        BigDecimal.valueOf(50000),
        BigDecimal.valueOf(60000),
        BigDecimal.valueOf(70000),
        false
    );
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() ->
        roiConfigurationService.getCurrentAndMinimumValuesByCurrencyType("aud"))
        .withMessage("Provided currency type aud is not found");
  }

  @Test
  public void testRestoreToDefaultValuesByCurrencyType() {
    RoiConfigurationDTO roiConfigurationDefaultValuesActual =
        roiConfigurationService.restoreToDefaultValuesByCurrencyType("usd");
    assertThat(roiConfigurationDefaultValuesActual).isNotNull();
    assertRoiConfigurationValuesSavedDefaultValues(roiConfigurationDefaultValuesActual);
  }

  @Test
  public void testRestoreToDefaultValuesByCurrencyType_CurrencyTypeNotFound() {
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> roiConfigurationService.restoreToDefaultValuesByCurrencyType("aud"))
        .withMessage("Provided currency type aud is not found");
  }

  @Test
  public void testRestoreToDefaultValuesByCurrencyType_DefaultConfigNotFound() {
    dao.getAll().forEach(dao::delete);
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> roiConfigurationService.restoreToDefaultValuesByCurrencyType("usd"))
        .withMessage("No default configuration values found for currency type usd.");
  }

  @Test
  public void testSaveRoiConfiguration() {
    RoiConfigurationDTO roiConfigurationDTO = new RoiConfigurationDTO(
        null,
        CurrencyTypes.USD,
        BigDecimal.valueOf(100),
        1448L,
        true,
        BigDecimal.valueOf(23000),
        true,
        BigDecimal.valueOf(30000),
        false,
        BigDecimal.valueOf(45000),
        false,
        BigDecimal.valueOf(80000),
        BigDecimal.valueOf(500000),
        BigDecimal.valueOf(600000),
        BigDecimal.valueOf(700000),
        false
    );
    roiConfigurationService.saveRoiConfiguration(roiConfigurationDTO);
    RoiConfiguration roiConfigurationActual = roiConfigurationDao.getByCurrencyType(CurrencyTypes.USD);
    assertThat(roiConfigurationActual).isNotNull();
    assertRoiConfigurationValuesSaved(roiConfigurationActual);
    assertTelemetryData(roiConfigurationDTO);
  }

  @Test
  public void testSaveRoiConfiguration_MissingValues() {
    RoiConfigurationDTO roiConfigurationDTO = new RoiConfigurationDTO(
        null,
        CurrencyTypes.USD,
        BigDecimal.valueOf(100),
        1448L,
        true,
        null,
        true,
        BigDecimal.valueOf(30000),
        false,
        BigDecimal.valueOf(45000),
        false,
        BigDecimal.valueOf(80000),
        BigDecimal.valueOf(500000),
        BigDecimal.valueOf(600000),
        BigDecimal.valueOf(700000),
        false
    );
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> roiConfigurationService.saveRoiConfiguration(roiConfigurationDTO))
        .withMessage("Security violation critical value cannot be less than 6000");

    roiConfigurationDao.delete(roiConfigurationDao.getByCurrencyType(CurrencyTypes.USD));
    RoiConfigurationDTO roiConfigurationDTO1 = new RoiConfigurationDTO(
        null,
        CurrencyTypes.USD,
        BigDecimal.valueOf(100),
        1448L,
        true,
        BigDecimal.valueOf(23000),
        true,
        BigDecimal.valueOf(30000),
        false,
        BigDecimal.valueOf(45000),
        false,
        BigDecimal.valueOf(80000),
        null,
        null,
        null,
        false
    );
    testProductLicense.setProducts(ProductLicenseDetails.PRODUCT_LIFECYCLE_SAAS);
    roiConfigurationService.saveRoiConfiguration(roiConfigurationDTO1);
    RoiConfiguration roiConfigurationActual = roiConfigurationDao.getByCurrencyType(CurrencyTypes.USD);
    assertThat(roiConfigurationActual).isNotNull();

    roiConfigurationDao.delete(roiConfigurationDao.getByCurrencyType(CurrencyTypes.USD));
    testProductLicense.setProducts(ProductLicenseDetails.PRODUCT_FIREWALL);
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> roiConfigurationService.saveRoiConfiguration(roiConfigurationDTO1))
        .withMessage("Supply chain attacks blocked cannot be less than 500000");
  }

  @Test
  public void testSaveRoiConfiguration_ValidateLicense() {
    RoiConfigurationDTO roiConfigurationDTO = new RoiConfigurationDTO(
        null,
        CurrencyTypes.USD,
        BigDecimal.valueOf(100),
        1448L,
        true,
        BigDecimal.valueOf(23000),
        true,
        BigDecimal.valueOf(30000),
        false,
        BigDecimal.valueOf(45000),
        false,
        BigDecimal.valueOf(80000),
        BigDecimal.valueOf(500000),
        BigDecimal.valueOf(600000),
        BigDecimal.valueOf(700000),
        false
    );
    testProductLicense.setProducts(ProductLicenseDetails.PRODUCT_FIREWALL);
    roiConfigurationService.saveRoiConfiguration(roiConfigurationDTO);
    RoiConfiguration roiConfigurationActual = roiConfigurationDao.getByCurrencyType(CurrencyTypes.USD);
    assertThat(roiConfigurationActual).isNotNull();
    assertThat(roiConfigurationActual.getSupplyChainAttacksBlocked()).isEqualTo(
        roiConfigurationDTO.supplyChainAttacksBlocked());
    assertThat(roiConfigurationActual.getNamespaceAttacksBlocked()).isEqualTo(
        roiConfigurationDTO.namespaceAttacksBlocked());
    assertThat(roiConfigurationActual.getSafeComponentsAutoSelected()).isEqualTo(
        roiConfigurationDTO.safeComponentsAutoSelected());
    assertThat(roiConfigurationActual.getSecurityViolationCriticalValue()).isEqualTo(BigDecimal.ZERO);
    assertThat(roiConfigurationActual.getSecurityViolationHighValue()).isEqualTo(BigDecimal.ZERO);
    assertThat(roiConfigurationActual.getSecurityViolationMediumValue()).isEqualTo(BigDecimal.ZERO);
    assertThat(roiConfigurationActual.getSecurityViolationLowValue()).isEqualTo(BigDecimal.ZERO);

    testProductLicense.setProducts(ProductLicenseDetails.PRODUCT_LIFECYCLE_SAAS);
    roiConfigurationService.saveRoiConfiguration(roiConfigurationDTO);
    roiConfigurationActual = roiConfigurationDao.getByCurrencyType(CurrencyTypes.USD);
    assertThat(roiConfigurationActual).isNotNull();
    assertThat(roiConfigurationActual.getSecurityViolationCriticalValue()).isEqualTo(
        roiConfigurationDTO.securityViolationCriticalValue());
    assertThat(roiConfigurationActual.getSecurityViolationHighValue()).isEqualTo(
        roiConfigurationDTO.securityViolationHighValue());
    assertThat(roiConfigurationActual.getSecurityViolationMediumValue()).isEqualTo(
        roiConfigurationDTO.securityViolationMediumValue());
    assertThat(roiConfigurationActual.getSecurityViolationLowValue()).isEqualTo(
        roiConfigurationDTO.securityViolationLowValue());
    assertThat(roiConfigurationActual.getSupplyChainAttacksBlocked()).isEqualTo(BigDecimal.ZERO);
    assertThat(roiConfigurationActual.getNamespaceAttacksBlocked()).isEqualTo(BigDecimal.ZERO);
    assertThat(roiConfigurationActual.getSafeComponentsAutoSelected()).isEqualTo(BigDecimal.ZERO);
  }

  @Test
  public void testSaveRoiConfiguration_InvalidMinimumValues() {
    Stream.of(
        Map.entry("Supply chain attacks blocked", BigDecimal.valueOf(500000)),
        Map.entry("Namespace attacks blocked", BigDecimal.valueOf(10000)),
        Map.entry("Safe components auto selected", BigDecimal.valueOf(5000)),
        Map.entry("Developer hourly rate", BigDecimal.valueOf(50)),
        Map.entry("Fix rate hours", BigDecimal.valueOf(1440)),
        Map.entry("Security violation critical value", BigDecimal.valueOf(6000)),
        Map.entry("Security violation high value", BigDecimal.valueOf(12000)),
        Map.entry("Security violation medium value", BigDecimal.valueOf(36000)),
        Map.entry("Security violation low value", BigDecimal.valueOf(72000))
    ).forEach(entry -> {
      RoiConfigurationDTO roiConfigurationDTO =
          createRoiConfigurationDTOMininumValues(entry.getKey(), entry.getValue().subtract(BigDecimal.ONE));
      assertThatExceptionOfType(BadRequestException.class)
          .isThrownBy(() -> roiConfigurationService.saveRoiConfiguration(roiConfigurationDTO))
          .withMessage(entry.getKey() + " cannot be less than " + entry.getValue());
    });
  }

  private void assertTelemetryData(final RoiConfigurationDTO roiConfiguration) {
    ArgumentCaptor<TelemetryData> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(mockTelemetrySender, times(1)).send(telemetryDataArgumentCaptor.capture());
    TelemetryData telemetryData = telemetryDataArgumentCaptor.getValue();
    assertThat(telemetryData).isNotNull();
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.ROI_CONFIG_CHANGED);
    Map<String, Object> telemetryAttributes = telemetryData.getAttributes();
    assertThat(telemetryAttributes).isNotNull();
    assertThat(telemetryAttributes.get("developerHourlyRate")).isEqualTo(roiConfiguration.developerHourlyRate());
    assertThat(telemetryAttributes.get("currency")).isEqualTo(roiConfiguration.currency());
    assertThat(telemetryAttributes.get("fixRateHours")).isEqualTo(roiConfiguration.fixRateHours());
    assertThat(telemetryAttributes.get("securityViolationCriticalEnabled"))
        .isEqualTo(roiConfiguration.securityViolationCriticalEnabled());
    assertThat(telemetryAttributes.get("securityViolationCriticalValue"))
        .isEqualTo(roiConfiguration.securityViolationCriticalValue());
    assertThat(telemetryAttributes.get("securityViolationHighEnabled"))
        .isEqualTo(roiConfiguration.securityViolationHighEnabled());
    assertThat(telemetryAttributes.get("securityViolationHighValue"))
        .isEqualTo(roiConfiguration.securityViolationHighValue());
    assertThat(telemetryAttributes.get("securityViolationMediumEnabled")).isEqualTo(
        roiConfiguration.securityViolationMediumEnabled());
    assertThat(telemetryAttributes.get("securityViolationMediumValue")).isEqualTo(
        roiConfiguration.securityViolationMediumValue());
    assertThat(telemetryAttributes.get("securityViolationLowEnabled")).isEqualTo(
        roiConfiguration.securityViolationLowEnabled());
    assertThat(telemetryAttributes.get("securityViolationLowValue")).isEqualTo(
        roiConfiguration.securityViolationLowValue());
    assertThat(telemetryAttributes.get("supplyChainAttacksBlocked")).isEqualTo(
        roiConfiguration.supplyChainAttacksBlocked());
    assertThat(telemetryAttributes.get("namespaceAttacksBlocked")).isEqualTo(
        roiConfiguration.namespaceAttacksBlocked());
    assertThat(telemetryAttributes.get("safeComponentsAutoSelected")).isEqualTo(
        roiConfiguration.safeComponentsAutoSelected());
    assertThat(telemetryAttributes.get("waivedPoliciesCounted")).isEqualTo(roiConfiguration.waivedPoliciesCounted());
  }

  private void assertRoiConfigurationEntityValues(
      RoiConfigurationCurrentAndMinimumValuesDTO roiConfiguration)
  {
    assertThat(roiConfiguration.currency).isEqualTo(CurrencyTypes.USD);
    assertThat(roiConfiguration.fixRateHoursValue).isEqualTo(1448L);
    assertThat(roiConfiguration.fixRateHoursMinimum).isEqualTo(1440L);
    assertThat(roiConfiguration.developerHourlyRateValue).isEqualTo(BigDecimal.valueOf(100));
    assertThat(roiConfiguration.developerHourlyRateMinimum).isEqualTo(BigDecimal.valueOf(50));
    assertThat(roiConfiguration.securityViolationCriticalEnabled).isTrue();
    assertThat(roiConfiguration.securityViolationCriticalValue).isEqualTo(BigDecimal.valueOf(23000));
    assertThat(roiConfiguration.securityViolationCriticalValueMinimum).isEqualTo(BigDecimal.valueOf(6000));
    assertThat(roiConfiguration.securityViolationHighEnabled).isTrue();
    assertThat(roiConfiguration.securityViolationHighValue).isEqualTo(BigDecimal.valueOf(30000));
    assertThat(roiConfiguration.securityViolationHighValueMinimum).isEqualTo(BigDecimal.valueOf(12000));
    assertThat(roiConfiguration.securityViolationMediumEnabled).isFalse();
    assertThat(roiConfiguration.securityViolationMediumValue).isEqualTo(BigDecimal.valueOf(45000));
    assertThat(roiConfiguration.securityViolationMediumValueMinimum).isEqualTo(BigDecimal.valueOf(36000));
    assertThat(roiConfiguration.securityViolationLowEnabled).isFalse();
    assertThat(roiConfiguration.securityViolationLowValue).isEqualTo(BigDecimal.valueOf(40000));
    assertThat(roiConfiguration.securityViolationLowValueMinimum).isEqualTo(BigDecimal.valueOf(72000));
    assertThat(roiConfiguration.namespaceAttacksBlockedValue).isEqualTo(BigDecimal.valueOf(60000));
    assertThat(roiConfiguration.namespaceAttacksBlockedValueMinimum).isEqualTo(BigDecimal.valueOf(10000));
    assertThat(roiConfiguration.supplyChainAttacksBlockedValue).isEqualTo(BigDecimal.valueOf(50000));
    assertThat(roiConfiguration.supplyChainAttacksBlockedValueMinimum).isEqualTo(BigDecimal.valueOf(500000));
    assertThat(roiConfiguration.safeComponentsAutoSelectedValue).isEqualTo(BigDecimal.valueOf(70000));
    assertThat(roiConfiguration.safeComponentsAutoSelectedValueMinimum).isEqualTo(BigDecimal.valueOf(5000));
    assertThat(roiConfiguration.waivedPoliciesCounted).isFalse();
  }

  private RoiConfigurationDTO createRoiConfigurationDTOMininumValues(String fieldName, BigDecimal value) {
    return switch (fieldName) {
      case "Supply chain attacks blocked" -> new RoiConfigurationDTO(
          null,
          CurrencyTypes.USD,
          BigDecimal.valueOf(100),
          1448L,
          true,
          BigDecimal.valueOf(23000),
          true,
          BigDecimal.valueOf(30000),
          false,
          BigDecimal.valueOf(45000),
          false,
          value,
          value,
          BigDecimal.valueOf(600000),
          BigDecimal.valueOf(700000),
          false
      );
      case "Namespace attacks blocked" -> new RoiConfigurationDTO(
          null,
          CurrencyTypes.USD,
          BigDecimal.valueOf(100),
          1448L,
          true,
          BigDecimal.valueOf(23000),
          true,
          BigDecimal.valueOf(30000),
          false,
          BigDecimal.valueOf(45000),
          false,
          BigDecimal.valueOf(80000),
          BigDecimal.valueOf(600000),
          value,
          BigDecimal.valueOf(700000),
          false
      );
      case "Safe components auto selected" -> new RoiConfigurationDTO(
          null,
          CurrencyTypes.USD,
          BigDecimal.valueOf(100),
          1448L,
          true,
          BigDecimal.valueOf(23000),
          true,
          BigDecimal.valueOf(30000),
          false,
          BigDecimal.valueOf(45000),
          false,
          BigDecimal.valueOf(80000),
          BigDecimal.valueOf(600000),
          BigDecimal.valueOf(600000),
          value,
          false
      );
      case "Developer hourly rate" -> new RoiConfigurationDTO(
          null,
          CurrencyTypes.USD,
          value,
          1448L,
          true,
          BigDecimal.valueOf(23000),
          true,
          BigDecimal.valueOf(30000),
          false,
          BigDecimal.valueOf(45000),
          false,
          BigDecimal.valueOf(80000),
          BigDecimal.valueOf(600000),
          BigDecimal.valueOf(600000),
          BigDecimal.valueOf(700000),
          false
      );
      case "Fix rate hours" -> new RoiConfigurationDTO(
          null,
          CurrencyTypes.USD,
          BigDecimal.valueOf(100),
          value.longValue(),
          true,
          BigDecimal.valueOf(23000),
          true,
          BigDecimal.valueOf(30000),
          false,
          BigDecimal.valueOf(45000),
          false,
          BigDecimal.valueOf(80000),
          BigDecimal.valueOf(600000),
          BigDecimal.valueOf(600000),
          BigDecimal.valueOf(700000),
          false
      );
      case "Security violation critical value" -> new RoiConfigurationDTO(
          null,
          CurrencyTypes.USD,
          BigDecimal.valueOf(100),
          1448L,
          true,
          value,
          true,
          BigDecimal.valueOf(30000),
          false,
          BigDecimal.valueOf(45000),
          false,
          BigDecimal.valueOf(80000),
          BigDecimal.valueOf(600000),
          BigDecimal.valueOf(600000),
          BigDecimal.valueOf(700000),
          false
      );
      case "Security violation high value" -> new RoiConfigurationDTO(
          null,
          CurrencyTypes.USD,
          BigDecimal.valueOf(100),
          1448L,
          true,
          BigDecimal.valueOf(23000),
          true,
          value,
          false,
          BigDecimal.valueOf(45000),
          false,
          BigDecimal.valueOf(80000),
          BigDecimal.valueOf(600000),
          BigDecimal.valueOf(600000),
          BigDecimal.valueOf(700000),
          false
      );
      case "Security violation medium value" -> new RoiConfigurationDTO(
          null,
          CurrencyTypes.USD,
          BigDecimal.valueOf(100),
          1448L,
          true,
          BigDecimal.valueOf(23000),
          true,
          BigDecimal.valueOf(30000),
          false,
          value,
          false,
          BigDecimal.valueOf(80000),
          BigDecimal.valueOf(600000),
          BigDecimal.valueOf(600000),
          BigDecimal.valueOf(700000),
          false
      );
      case "Security violation low value" -> new RoiConfigurationDTO(
          null,
          CurrencyTypes.USD,
          BigDecimal.valueOf(100),
          1448L,
          true,
          BigDecimal.valueOf(23000),
          true,
          BigDecimal.valueOf(30000),
          false,
          BigDecimal.valueOf(45000),
          false,
          value,
          BigDecimal.valueOf(600000),
          BigDecimal.valueOf(600000),
          BigDecimal.valueOf(700000),
          false
      );
      default -> throw new IllegalArgumentException("Invalid field name");
    };
  }

  private void assertRoiConfigurationValuesSaved(
      RoiConfiguration roiConfiguration)
  {
    assertThat(roiConfiguration.getCurrency()).isEqualTo(CurrencyTypes.USD);
    assertThat(roiConfiguration.getFixRateHours()).isEqualTo(1448L);
    assertThat(roiConfiguration.getDeveloperHourlyRate()).isEqualTo(BigDecimal.valueOf(100));
    assertThat(roiConfiguration.isSecurityViolationCriticalEnabled()).isTrue();
    assertThat(roiConfiguration.getSecurityViolationCriticalValue()).isEqualTo(BigDecimal.valueOf(23000));
    assertThat(roiConfiguration.isSecurityViolationHighEnabled()).isTrue();
    assertThat(roiConfiguration.getSecurityViolationHighValue()).isEqualTo(BigDecimal.valueOf(30000));
    assertThat(roiConfiguration.isSecurityViolationMediumEnabled()).isFalse();
    assertThat(roiConfiguration.getSecurityViolationMediumValue()).isEqualTo(BigDecimal.valueOf(45000));
    assertThat(roiConfiguration.isSecurityViolationLowEnabled()).isFalse();
    assertThat(roiConfiguration.getSecurityViolationLowValue()).isEqualTo(BigDecimal.valueOf(80000));
    assertThat(roiConfiguration.getNamespaceAttacksBlocked()).isEqualTo(BigDecimal.valueOf(600000));
    assertThat(roiConfiguration.getSupplyChainAttacksBlocked()).isEqualTo(BigDecimal.valueOf(500000));
    assertThat(roiConfiguration.getSafeComponentsAutoSelected()).isEqualTo(BigDecimal.valueOf(700000));
    assertThat(roiConfiguration.isWaivedPoliciesCounted()).isFalse();
  }

  private void assertRoiConfigurationValuesSavedDefaultValues(
      RoiConfigurationDTO roiConfiguration)
  {
    assertThat(roiConfiguration.currency()).isEqualTo(CurrencyTypes.USD);
    assertThat(roiConfiguration.fixRateHours()).isEqualTo(3600L);
    assertThat(roiConfiguration.developerHourlyRate()).isEqualTo(BigDecimal.valueOf(100));
    assertThat(roiConfiguration.securityViolationCriticalEnabled()).isTrue();
    assertThat(roiConfiguration.securityViolationCriticalValue()).isEqualTo(BigDecimal.valueOf(12000));
    assertThat(roiConfiguration.securityViolationHighEnabled()).isTrue();
    assertThat(roiConfiguration.securityViolationHighValue()).isEqualTo(BigDecimal.valueOf(24000));
    assertThat(roiConfiguration.securityViolationMediumEnabled()).isFalse();
    assertThat(roiConfiguration.securityViolationMediumValue()).isEqualTo(BigDecimal.valueOf(72000));
    assertThat(roiConfiguration.securityViolationLowEnabled()).isFalse();
    assertThat(roiConfiguration.securityViolationLowValue()).isEqualTo(BigDecimal.valueOf(144000));
    assertThat(roiConfiguration.namespaceAttacksBlocked()).isEqualTo(BigDecimal.valueOf(35000));
    assertThat(roiConfiguration.supplyChainAttacksBlocked()).isEqualTo(BigDecimal.valueOf(4350000));
    assertThat(roiConfiguration.safeComponentsAutoSelected()).isEqualTo(BigDecimal.valueOf(25000));
    assertThat(roiConfiguration.waivedPoliciesCounted()).isFalse();
  }
}
