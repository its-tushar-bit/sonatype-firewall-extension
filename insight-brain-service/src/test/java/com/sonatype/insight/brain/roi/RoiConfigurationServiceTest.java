/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.roi;

import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Stream;
import jakarta.inject.Inject;

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

public class RoiConfigurationServiceTest
    extends AbstractComponentTest
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
  public void setup() {
    dao.getAll().forEach(dao::delete);
    tempEntity.createRoiConfigurationDefaultValues(
        CurrencyTypes.USD,
        BigDecimal.valueOf(4350000),
        BigDecimal.valueOf(500000),
        BigDecimal.valueOf(35000),
        BigDecimal.valueOf(10000),
        BigDecimal.valueOf(25000),
        BigDecimal.valueOf(5000),
        30,
        15,
        BigDecimal.valueOf(800),
        BigDecimal.valueOf(400));
  }

  @Test
  public void testGetRoiConfigurationByCurrencyType() {
    tempEntity.createRoiConfiguration(
        CurrencyTypes.USD,
        BigDecimal.valueOf(50000),
        BigDecimal.valueOf(60000),
        BigDecimal.valueOf(70000),
        15,
        BigDecimal.valueOf(400));
    RoiConfigurationCurrentAndMinimumValuesDTO roiConfigurationActual =
        roiConfigurationService.getCurrentAndMinimumValuesByCurrencyType("usd");
    assertThat(roiConfigurationActual).isNotNull();
    assertRoiConfigurationEntityValues(roiConfigurationActual);
  }

  @Test
  public void testGetCurrentAndMinimumValuesByCurrencyType_NotFound() {
    tempEntity.createRoiConfiguration(
        CurrencyTypes.USD,
        BigDecimal.valueOf(50000),
        BigDecimal.valueOf(60000),
        BigDecimal.valueOf(70000),
        15,
        BigDecimal.valueOf(400));
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> roiConfigurationService.getCurrentAndMinimumValuesByCurrencyType("aud"))
        .withMessage("Provided currency type aud is not found");
  }

  @Test
  public void testRestoreToDefaultValuesByCurrencyType() {
    RoiConfigurationCurrentAndMinimumValuesDTO roiConfigurationDefaultValuesActual =
        roiConfigurationService.restoreToDefaultValuesByCurrencyType("usd");
    assertThat(roiConfigurationDefaultValuesActual).isNotNull();
    assertThat(roiConfigurationDefaultValuesActual.currency).isEqualTo(CurrencyTypes.USD);
    assertThat(roiConfigurationDefaultValuesActual.namespaceAttacksPrevented).isEqualTo(BigDecimal.valueOf(35000));
    assertThat(roiConfigurationDefaultValuesActual.malwareAttacksPrevented).isEqualTo(BigDecimal.valueOf(4350000));
    assertThat(roiConfigurationDefaultValuesActual.safeComponentsAutoSelected).isEqualTo(BigDecimal.valueOf(25000));
    assertThat(roiConfigurationDefaultValuesActual.baselineDaysToResolveViolation).isEqualTo(30L);
    assertThat(roiConfigurationDefaultValuesActual.dailyRiskCostOfUnfixedViolation).isEqualTo(BigDecimal.valueOf(800));
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
        BigDecimal.valueOf(500000),
        BigDecimal.valueOf(600000),
        BigDecimal.valueOf(700000),
        30,
        BigDecimal.valueOf(800));
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
        BigDecimal.valueOf(500000),
        BigDecimal.valueOf(600000),
        BigDecimal.valueOf(700000),
        30,
        null);
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> roiConfigurationService.saveRoiConfiguration(roiConfigurationDTO))
        .withMessage("Daily risk cost of unfixed violation cannot be less than 400");

    roiConfigurationDao.delete(roiConfigurationDao.getByCurrencyType(CurrencyTypes.USD));
    RoiConfigurationDTO roiConfigurationDTO1 = new RoiConfigurationDTO(
        null,
        CurrencyTypes.USD,
        null,
        null,
        null,
        30,
        BigDecimal.valueOf(800));
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
        BigDecimal.valueOf(500000),
        BigDecimal.valueOf(600000),
        BigDecimal.valueOf(700000),
        30,
        BigDecimal.valueOf(800));
    testProductLicense.setProducts(ProductLicenseDetails.PRODUCT_FIREWALL);
    roiConfigurationService.saveRoiConfiguration(roiConfigurationDTO);
    RoiConfiguration roiConfigurationActual = roiConfigurationDao.getByCurrencyType(CurrencyTypes.USD);
    assertThat(roiConfigurationActual).isNotNull();
    assertThat(roiConfigurationActual.getMalwareAttacksPrevented()).isEqualTo(
        roiConfigurationDTO.malwareAttacksPrevented());
    assertThat(roiConfigurationActual.getNamespaceAttacksPrevented()).isEqualTo(
        roiConfigurationDTO.namespaceAttacksPrevented());
    assertThat(roiConfigurationActual.getSafeComponentsAutoSelected()).isEqualTo(
        roiConfigurationDTO.safeComponentsAutoSelected());
    assertThat(roiConfigurationActual.getBaselineDaysToResolveViolation()).isZero();
    assertThat(roiConfigurationActual.getDailyRiskCostOfUnfixedViolation()).isEqualTo(BigDecimal.ZERO);

    testProductLicense.setProducts(ProductLicenseDetails.PRODUCT_LIFECYCLE_SAAS);
    roiConfigurationService.saveRoiConfiguration(roiConfigurationDTO);
    roiConfigurationActual = roiConfigurationDao.getByCurrencyType(CurrencyTypes.USD);
    assertThat(roiConfigurationActual).isNotNull();
    assertThat(roiConfigurationActual.getBaselineDaysToResolveViolation()).isEqualTo(
        roiConfigurationDTO.baselineDaysToResolveViolation());
    assertThat(roiConfigurationActual.getMalwareAttacksPrevented()).isEqualTo(BigDecimal.ZERO);
    assertThat(roiConfigurationActual.getNamespaceAttacksPrevented()).isEqualTo(BigDecimal.ZERO);
    assertThat(roiConfigurationActual.getSafeComponentsAutoSelected()).isEqualTo(BigDecimal.ZERO);
  }

  @Test
  public void testSaveRoiConfiguration_InvalidMinimumValues() {
    Stream.of(
        Map.entry("Supply chain attacks blocked", BigDecimal.valueOf(500000)),
        Map.entry("Namespace attacks blocked", BigDecimal.valueOf(10000)),
        Map.entry("Safe components auto selected", BigDecimal.valueOf(5000)),
        Map.entry("Baseline days to resolve violation", BigDecimal.valueOf(15)),
        Map.entry("Daily risk cost of unfixed violation", BigDecimal.valueOf(400))).forEach(entry -> {
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
    assertThat(telemetryAttributes.get("currency")).isEqualTo(roiConfiguration.currency());
    assertThat(telemetryAttributes.get("malwareAttacksPrevented")).isEqualTo(
        roiConfiguration.malwareAttacksPrevented());
    assertThat(telemetryAttributes.get("namespaceAttacksPrevented")).isEqualTo(
        roiConfiguration.namespaceAttacksPrevented());
    assertThat(telemetryAttributes.get("safeComponentsAutoSelected")).isEqualTo(
        roiConfiguration.safeComponentsAutoSelected());
    assertThat(telemetryAttributes.get("baselineDaysToResolveViolation")).isEqualTo(
        roiConfiguration.baselineDaysToResolveViolation());
    assertThat(telemetryAttributes.get("dailyRiskCostOfUnfixedViolation")).isEqualTo(
        roiConfiguration.dailyRiskCostOfUnfixedViolation());
  }

  private void assertRoiConfigurationEntityValues(
      RoiConfigurationCurrentAndMinimumValuesDTO roiConfiguration)
  {
    assertThat(roiConfiguration.currency).isEqualTo(CurrencyTypes.USD);
    assertThat(roiConfiguration.namespaceAttacksPrevented).isEqualTo(BigDecimal.valueOf(60000));
    assertThat(roiConfiguration.namespaceAttacksPreventedMinimum).isEqualTo(BigDecimal.valueOf(10000));
    assertThat(roiConfiguration.malwareAttacksPrevented).isEqualTo(BigDecimal.valueOf(50000));
    assertThat(roiConfiguration.malwareAttacksPreventedMinimum).isEqualTo(BigDecimal.valueOf(500000));
    assertThat(roiConfiguration.safeComponentsAutoSelected).isEqualTo(BigDecimal.valueOf(70000));
    assertThat(roiConfiguration.safeComponentsAutoSelectedMinimum).isEqualTo(BigDecimal.valueOf(5000));
    assertThat(roiConfiguration.baselineDaysToResolveViolation).isEqualTo(15);
    assertThat(roiConfiguration.baselineDaysToResolveViolationMinimum).isEqualTo(15);
    assertThat(roiConfiguration.dailyRiskCostOfUnfixedViolation).isEqualTo(BigDecimal.valueOf(400));
    assertThat(roiConfiguration.dailyRiskCostOfUnfixedViolationMinimum).isEqualTo(BigDecimal.valueOf(400));
  }

  private RoiConfigurationDTO createRoiConfigurationDTOMininumValues(String fieldName, BigDecimal value) {
    return switch (fieldName) {
      case "Supply chain attacks blocked" -> new RoiConfigurationDTO(
          null,
          CurrencyTypes.USD,
          value,
          BigDecimal.valueOf(600000),
          BigDecimal.valueOf(700000),
          30,
          BigDecimal.valueOf(800));
      case "Namespace attacks blocked" -> new RoiConfigurationDTO(
          null,
          CurrencyTypes.USD,
          BigDecimal.valueOf(600000),
          value,
          BigDecimal.valueOf(700000),
          30,
          BigDecimal.valueOf(800));
      case "Safe components auto selected" -> new RoiConfigurationDTO(
          null,
          CurrencyTypes.USD,
          BigDecimal.valueOf(600000),
          BigDecimal.valueOf(600000),
          value,
          30,
          BigDecimal.valueOf(800));
      case "Baseline days to resolve violation" -> new RoiConfigurationDTO(
          null,
          CurrencyTypes.USD,
          BigDecimal.valueOf(600000),
          BigDecimal.valueOf(600000),
          BigDecimal.valueOf(700000),
          value.intValue(),
          BigDecimal.valueOf(800));
      case "Daily risk cost of unfixed violation" -> new RoiConfigurationDTO(
          null,
          CurrencyTypes.USD,
          BigDecimal.valueOf(600000),
          BigDecimal.valueOf(600000),
          BigDecimal.valueOf(700000),
          30,
          value);
      default -> throw new IllegalArgumentException("Invalid field name");
    };
  }

  private void assertRoiConfigurationValuesSaved(
      RoiConfiguration roiConfiguration)
  {
    assertThat(roiConfiguration.getCurrency()).isEqualTo(CurrencyTypes.USD);
    assertThat(roiConfiguration.getNamespaceAttacksPrevented()).isEqualTo(BigDecimal.valueOf(600000));
    assertThat(roiConfiguration.getMalwareAttacksPrevented()).isEqualTo(BigDecimal.valueOf(500000));
    assertThat(roiConfiguration.getSafeComponentsAutoSelected()).isEqualTo(BigDecimal.valueOf(700000));
    assertThat(roiConfiguration.getBaselineDaysToResolveViolation()).isEqualTo(30);
    assertThat(roiConfiguration.getDailyRiskCostOfUnfixedViolation()).isEqualTo(BigDecimal.valueOf(800));
  }
}
