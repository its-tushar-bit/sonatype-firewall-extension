/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.roi;

import java.util.Set;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.roi.RoiConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.roi.RoiConfigurationDefaultValuesDAO;
import com.sonatype.insight.brain.model.roi.CurrencyTypes;
import com.sonatype.insight.brain.model.roi.RoiConfiguration;
import com.sonatype.insight.brain.model.roi.RoiConfigurationDefaultValues;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.roi.dtos.RoiConfigurationDTO;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.solution.Solution;
import com.sonatype.insight.brain.solution.SolutionResolver;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

@Named
public class RoiConfigurationService
{
  private final RoiConfigurationDAO roiConfigurationDAO;

  private final RoiConfigurationDefaultValuesDAO roiConfigurationDefaultValuesDAO;

  private final SolutionResolver solutionResolver;

  private final TelemetrySender telemetrySender;

  @Inject
  public  RoiConfigurationService(RoiConfigurationDAO roiConfigurationDAO,
                                  RoiConfigurationDefaultValuesDAO roiConfigurationDefaultValuesDAO,
                                  SolutionResolver solutionResolver,
                                  TelemetrySender telemetrySender)
  {
    this.roiConfigurationDAO = roiConfigurationDAO;
    this.roiConfigurationDefaultValuesDAO = roiConfigurationDefaultValuesDAO;
    this.solutionResolver = solutionResolver;
    this.telemetrySender = telemetrySender;
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public RoiConfigurationCurrentAndMinimumValuesDTO getCurrentAndMinimumValuesByCurrencyType(String currencyType) {
    CurrencyTypes currencyTypeEnum = CurrencyTypes.fromString(currencyType);
    RoiConfiguration roiConfiguration = roiConfigurationDAO.getByCurrencyType(currencyTypeEnum);
    RoiConfigurationDefaultValues roiConfigurationDefaultValues =
        roiConfigurationDefaultValuesDAO.getByCurrencyType(currencyTypeEnum);

    //merge both entities to fetch current and minimum values
    RoiConfigurationCurrentAndMinimumValuesDTO roiConfigurationCurrentAndMinimumValuesDTO =
        new RoiConfigurationCurrentAndMinimumValuesDTO();
    if (roiConfiguration != null) {
      roiConfigurationCurrentAndMinimumValuesDTO.currency = roiConfiguration.getCurrency();
      roiConfigurationCurrentAndMinimumValuesDTO.malwareAttacksPrevented =
          roiConfiguration.getMalwareAttacksPrevented();
      roiConfigurationCurrentAndMinimumValuesDTO.namespaceAttacksPrevented =
          roiConfiguration.getNamespaceAttacksPrevented();
      roiConfigurationCurrentAndMinimumValuesDTO.safeComponentsAutoSelected =
          roiConfiguration.getSafeComponentsAutoSelected();
      roiConfigurationCurrentAndMinimumValuesDTO.baselineDaysToResolveViolation =
          roiConfiguration.getBaselineDaysToResolveViolation();
      roiConfigurationCurrentAndMinimumValuesDTO.dailyRiskCostOfUnfixedViolation =
          roiConfiguration.getDailyRiskCostOfUnfixedViolation();
    }

    if (roiConfigurationDefaultValues != null) {
      roiConfigurationCurrentAndMinimumValuesDTO.malwareAttacksPreventedMinimum =
          roiConfigurationDefaultValues.getMalwareAttacksPreventedMinimum();
      roiConfigurationCurrentAndMinimumValuesDTO.namespaceAttacksPreventedMinimum =
          roiConfigurationDefaultValues.getNamespaceAttacksPreventedMinimum();
      roiConfigurationCurrentAndMinimumValuesDTO.safeComponentsAutoSelectedMinimum =
          roiConfigurationDefaultValues.getSafeComponentsAutoSelectedMinimum();
      roiConfigurationCurrentAndMinimumValuesDTO.baselineDaysToResolveViolationMinimum =
          roiConfigurationDefaultValues.getBaselineDaysToResolveViolationMinimum();
      roiConfigurationCurrentAndMinimumValuesDTO.dailyRiskCostOfUnfixedViolationMinimum =
          roiConfigurationDefaultValues.getDailyRiskCostOfUnfixedViolationMinimum();
    }
    return roiConfigurationCurrentAndMinimumValuesDTO;
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public RoiConfigurationCurrentAndMinimumValuesDTO restoreToDefaultValuesByCurrencyType(String currencyType) {
    CurrencyTypes currencyTypeEnum = CurrencyTypes.fromString(currencyType);

    RoiConfigurationDefaultValues roiConfigurationDefaultValues =
        roiConfigurationDefaultValuesDAO.getByCurrencyType(currencyTypeEnum);

    RoiConfiguration roiConfiguration = new RoiConfiguration();

    if (roiConfigurationDefaultValues != null) {
      roiConfiguration.setCurrency(roiConfigurationDefaultValues.getCurrency());
      roiConfiguration.setMalwareAttacksPrevented(roiConfigurationDefaultValues
          .getMalwareAttacksPreventedDefault());
      roiConfiguration.setNamespaceAttacksPrevented(roiConfigurationDefaultValues
          .getNamespaceAttacksPreventedDefault());
      roiConfiguration.setSafeComponentsAutoSelected(roiConfigurationDefaultValues
          .getSafeComponentsAutoSelectedDefault());
      roiConfiguration.setBaselineDaysToResolveViolation(
          roiConfigurationDefaultValues.getBaselineDaysToResolveViolationDefault());
      roiConfiguration.setDailyRiskCostOfUnfixedViolation(
          roiConfigurationDefaultValues.getDailyRiskCostOfUnfixedViolationDefault());
      roiConfiguration.setBaselineDaysToResolveViolation(
          roiConfigurationDefaultValues.getBaselineDaysToResolveViolationDefault());
      return saveRoiConfiguration(mapRoiConfigurationToDTO(roiConfiguration));
    }
    throw new NotFoundException("No default configuration values found for currency type " + currencyType + ".");
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public RoiConfigurationCurrentAndMinimumValuesDTO saveRoiConfiguration(
      final RoiConfigurationDTO roiConfigurationDTO)
  {
    RoiConfiguration roiConfiguration = validateAndMapToRoiConfiguration(roiConfigurationDTO);
    RoiConfiguration saved = roiConfigurationDAO.getByCurrencyType(roiConfigurationDTO.currency());
    if (saved == null) {
      roiConfigurationDAO.insert(roiConfiguration);
    }
    else {
      roiConfiguration.setId(saved.getId());
      roiConfigurationDAO.update(roiConfiguration);
      AuditData.get().setEvent(AuditEvent.ROI_CONFIG_UPDATE);
    }
    generateAuditInformationForRoiChange(roiConfiguration);
    generateTelemetryEventForRoiChange(roiConfiguration);
    RoiConfigurationDefaultValues defaultValues =
        roiConfigurationDefaultValuesDAO.getByCurrencyType(roiConfiguration.getCurrency());
    return new RoiConfigurationCurrentAndMinimumValuesDTO(
        roiConfiguration.getCurrency(),
        roiConfiguration.getMalwareAttacksPrevented(),
        defaultValues.getMalwareAttacksPreventedMinimum(),
        roiConfiguration.getNamespaceAttacksPrevented(),
        defaultValues.getNamespaceAttacksPreventedMinimum(),
        roiConfiguration.getSafeComponentsAutoSelected(),
        defaultValues.getSafeComponentsAutoSelectedMinimum(),
        roiConfiguration.getBaselineDaysToResolveViolation(),
        defaultValues.getBaselineDaysToResolveViolationMinimum(),
        roiConfiguration.getDailyRiskCostOfUnfixedViolation(),
        defaultValues.getDailyRiskCostOfUnfixedViolationMinimum()
    );
  }

  private RoiConfiguration validateAndMapToRoiConfiguration(RoiConfigurationDTO roiConfigurationDTO) {
    Set<Solution> licensedSolutions = solutionResolver.getLicensedSolutions();
    RoiConfigurationDefaultValues roiConfigurationDefaultValues =
        roiConfigurationDefaultValuesDAO.getByCurrencyType(roiConfigurationDTO.currency());

    if (licensedSolutions.contains(Solution.LIFECYCLE) && licensedSolutions.contains(Solution.FIREWALL)) {
      validateFirewallValues(roiConfigurationDTO, roiConfigurationDefaultValues);
      validateLifecycleValues(roiConfigurationDTO, roiConfigurationDefaultValues);
      return new RoiConfiguration(
          CurrencyTypes.valueOf(roiConfigurationDTO.currency().name()),
          roiConfigurationDTO.malwareAttacksPrevented(),
          roiConfigurationDTO.namespaceAttacksPrevented(),
          roiConfigurationDTO.safeComponentsAutoSelected(),
          roiConfigurationDTO.baselineDaysToResolveViolation(),
          roiConfigurationDTO.dailyRiskCostOfUnfixedViolation()
      );
    }
    else if (licensedSolutions.contains(Solution.LIFECYCLE)) {
      validateLifecycleValues(roiConfigurationDTO, roiConfigurationDefaultValues);
      return new RoiConfiguration(
          CurrencyTypes.valueOf(roiConfigurationDTO.currency().name()),
          roiConfigurationDTO.baselineDaysToResolveViolation(),
          roiConfigurationDTO.dailyRiskCostOfUnfixedViolation()
      );
    }
    else if (licensedSolutions.contains(Solution.FIREWALL)) {
      validateFirewallValues(roiConfigurationDTO, roiConfigurationDefaultValues);
      return new RoiConfiguration(
          CurrencyTypes.valueOf(roiConfigurationDTO.currency().name()),
          roiConfigurationDTO.malwareAttacksPrevented(),
          roiConfigurationDTO.namespaceAttacksPrevented(),
          roiConfigurationDTO.safeComponentsAutoSelected());
    }
    else {
      throw new InvalidLicenseException("Invalid License");
    }
  }

  private void validateLifecycleValues(
      RoiConfigurationDTO roiConfigurationDTO,
      RoiConfigurationDefaultValues roiConfigurationDefaultValues)
  {
    validateValue("Baseline days to resolve violation", roiConfigurationDTO.baselineDaysToResolveViolation(),
        roiConfigurationDefaultValues.getBaselineDaysToResolveViolationMinimum());
    validateValue("Daily risk cost of unfixed violation", roiConfigurationDTO.dailyRiskCostOfUnfixedViolation(),
        roiConfigurationDefaultValues.getDailyRiskCostOfUnfixedViolationMinimum());
  }

  private void validateFirewallValues(
      RoiConfigurationDTO roiConfigurationDTO,
      RoiConfigurationDefaultValues roiConfigurationDefaultValues)
  {
    validateValue("Supply chain attacks blocked", roiConfigurationDTO.malwareAttacksPrevented(),
        roiConfigurationDefaultValues.getMalwareAttacksPreventedMinimum());
    validateValue("Namespace attacks blocked", roiConfigurationDTO.namespaceAttacksPrevented(),
        roiConfigurationDefaultValues.getNamespaceAttacksPreventedMinimum());
    validateValue("Safe components auto selected", roiConfigurationDTO.safeComponentsAutoSelected(),
        roiConfigurationDefaultValues.getSafeComponentsAutoSelectedMinimum());
  }

  private void generateAuditInformationForRoiChange(RoiConfiguration roiConfiguration) {
    AuditData.get().setData("currency", roiConfiguration.getCurrency());
    AuditData.get().setData("malwareAttacksPrevented", roiConfiguration.getMalwareAttacksPrevented().toString());
    AuditData.get().setData("namespaceAttacksPrevented", roiConfiguration.getNamespaceAttacksPrevented().toString());
    AuditData.get().setData("safeComponentsAutoSelected", roiConfiguration.getSafeComponentsAutoSelected().toString());
    AuditData.get()
        .setData("baselineDaysToResolveViolation", roiConfiguration.getBaselineDaysToResolveViolation().toString());
    AuditData.get()
        .setData("dailyRiskCostOfUnfixedViolation", roiConfiguration.getDailyRiskCostOfUnfixedViolation().toString());
  }

  private void generateTelemetryEventForRoiChange(RoiConfiguration roiConfiguration) {
    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.ROI_CONFIG_CHANGED);
    telemetryData.put("currency", roiConfiguration.getCurrency());
    telemetryData.put("malwareAttacksPrevented", roiConfiguration.getMalwareAttacksPrevented());
    telemetryData.put("namespaceAttacksPrevented", roiConfiguration.getNamespaceAttacksPrevented());
    telemetryData.put("safeComponentsAutoSelected", roiConfiguration.getSafeComponentsAutoSelected());
    telemetryData.put("baselineDaysToResolveViolation", roiConfiguration.getBaselineDaysToResolveViolation());
    telemetryData.put("dailyRiskCostOfUnfixedViolation",roiConfiguration.getDailyRiskCostOfUnfixedViolation());
    telemetrySender.send(telemetryData);
  }

  private RoiConfigurationDTO mapRoiConfigurationToDTO(RoiConfiguration roiConfiguration) {
    return new RoiConfigurationDTO(
        roiConfiguration.getId(),
        CurrencyTypes.valueOf(roiConfiguration.getCurrency().name()),
        roiConfiguration.getMalwareAttacksPrevented(),
        roiConfiguration.getNamespaceAttacksPrevented(),
        roiConfiguration.getSafeComponentsAutoSelected(),
        roiConfiguration.getBaselineDaysToResolveViolation(),
        roiConfiguration.getDailyRiskCostOfUnfixedViolation()
    );
  }

  private <T extends Comparable<T>> void validateValue(String fieldName, T value, T minimumValue) {
    if (value == null || value.compareTo(minimumValue) < 0) {
      throw new BadRequestException(fieldName + " cannot be less than " + minimumValue);
    }
  }
}
