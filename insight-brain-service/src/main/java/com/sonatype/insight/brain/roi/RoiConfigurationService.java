/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.roi;

import java.util.Set;
import javax.inject.Inject;
import javax.inject.Named;

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
      roiConfigurationCurrentAndMinimumValuesDTO.developerHourlyRateValue = roiConfiguration.getDeveloperHourlyRate();
      roiConfigurationCurrentAndMinimumValuesDTO.fixRateHoursValue = roiConfiguration.getFixRateHours();
      roiConfigurationCurrentAndMinimumValuesDTO.securityViolationCriticalEnabled =
          roiConfiguration.isSecurityViolationCriticalEnabled();
      roiConfigurationCurrentAndMinimumValuesDTO.securityViolationCriticalValue =
          roiConfiguration.getSecurityViolationCriticalValue();
      roiConfigurationCurrentAndMinimumValuesDTO.securityViolationHighEnabled =
          roiConfiguration.isSecurityViolationHighEnabled();
      roiConfigurationCurrentAndMinimumValuesDTO.securityViolationHighValue =
          roiConfiguration.getSecurityViolationHighValue();
      roiConfigurationCurrentAndMinimumValuesDTO.securityViolationMediumEnabled =
          roiConfiguration.isSecurityViolationMediumEnabled();
      roiConfigurationCurrentAndMinimumValuesDTO.securityViolationMediumValue =
          roiConfiguration.getSecurityViolationMediumValue();
      roiConfigurationCurrentAndMinimumValuesDTO.securityViolationLowEnabled =
          roiConfiguration.isSecurityViolationLowEnabled();
      roiConfigurationCurrentAndMinimumValuesDTO.securityViolationLowValue =
          roiConfiguration.getSecurityViolationLowValue();
      roiConfigurationCurrentAndMinimumValuesDTO.supplyChainAttacksBlockedValue =
          roiConfiguration.getSupplyChainAttacksBlocked();
      roiConfigurationCurrentAndMinimumValuesDTO.namespaceAttacksBlockedValue =
          roiConfiguration.getNamespaceAttacksBlocked();
      roiConfigurationCurrentAndMinimumValuesDTO.safeComponentsAutoSelectedValue =
          roiConfiguration.getSafeComponentsAutoSelected();
      roiConfigurationCurrentAndMinimumValuesDTO.waivedPoliciesCounted =
          roiConfiguration.isWaivedPoliciesCounted();
    }

    if (roiConfigurationDefaultValues != null) {
      roiConfigurationCurrentAndMinimumValuesDTO.developerHourlyRateMinimum =
          roiConfigurationDefaultValues.getDeveloperHourlyRateMinimum();
      roiConfigurationCurrentAndMinimumValuesDTO.fixRateHoursMinimum =
          roiConfigurationDefaultValues.getFixRateHoursMinimum();
      roiConfigurationCurrentAndMinimumValuesDTO.securityViolationCriticalValueMinimum =
          roiConfigurationDefaultValues.getSecurityViolationCriticalMinimum();
      roiConfigurationCurrentAndMinimumValuesDTO.securityViolationHighValueMinimum =
          roiConfigurationDefaultValues.getSecurityViolationHighMinimum();
      roiConfigurationCurrentAndMinimumValuesDTO.securityViolationMediumValueMinimum =
          roiConfigurationDefaultValues.getSecurityViolationMediumMinimum();
      roiConfigurationCurrentAndMinimumValuesDTO.securityViolationLowValueMinimum =
          roiConfigurationDefaultValues.getSecurityViolationLowMinimum();
      roiConfigurationCurrentAndMinimumValuesDTO.supplyChainAttacksBlockedValueMinimum =
          roiConfigurationDefaultValues.getSupplyChainAttacksBlockedMinimum();
      roiConfigurationCurrentAndMinimumValuesDTO.namespaceAttacksBlockedValueMinimum =
          roiConfigurationDefaultValues.getNamespaceAttacksBlockedMinimum();
      roiConfigurationCurrentAndMinimumValuesDTO.safeComponentsAutoSelectedValueMinimum =
          roiConfigurationDefaultValues.getSafeComponentsAutoSelectedMinimum();
    }
    return roiConfigurationCurrentAndMinimumValuesDTO;
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public RoiConfigurationDTO restoreToDefaultValuesByCurrencyType(String currencyType) {
    CurrencyTypes currencyTypeEnum = CurrencyTypes.fromString(currencyType);

    RoiConfigurationDefaultValues roiConfigurationDefaultValues =
        roiConfigurationDefaultValuesDAO.getByCurrencyType(currencyTypeEnum);

    RoiConfiguration roiConfiguration = new RoiConfiguration();

    if (roiConfigurationDefaultValues != null) {
      roiConfiguration.setCurrency(roiConfigurationDefaultValues.getCurrency());
      roiConfiguration.setDeveloperHourlyRate(roiConfigurationDefaultValues.getDeveloperHourlyRateDefault());
      roiConfiguration.setFixRateHours(roiConfigurationDefaultValues.getFixRateHoursDefault());
      roiConfiguration.setSecurityViolationCriticalValue(roiConfigurationDefaultValues
          .getSecurityViolationCriticalDefault());
      roiConfiguration.setSecurityViolationCriticalEnabled(roiConfigurationDefaultValues
          .isSecurityViolationCriticalEnabled());
      roiConfiguration.setSecurityViolationHighValue(roiConfigurationDefaultValues
          .getSecurityViolationHighDefault());
      roiConfiguration.setSecurityViolationHighEnabled(roiConfigurationDefaultValues
          .isSecurityViolationHighEnabled());
      roiConfiguration.setSecurityViolationMediumValue(roiConfigurationDefaultValues
          .getSecurityViolationMediumDefault());
      roiConfiguration.setSecurityViolationMediumEnabled(roiConfigurationDefaultValues
          .isSecurityViolationMediumEnabled());
      roiConfiguration.setSecurityViolationLowValue(roiConfigurationDefaultValues
          .getSecurityViolationLowDefault());
      roiConfiguration.setSecurityViolationLowEnabled(roiConfigurationDefaultValues
          .isSecurityViolationLowEnabled());
      roiConfiguration.setSupplyChainAttacksBlocked(roiConfigurationDefaultValues
          .getSupplyChainAttacksBlockedDefault());
      roiConfiguration.setNamespaceAttacksBlocked(roiConfigurationDefaultValues
          .getNamespaceAttacksBlockedDefault());
      roiConfiguration.setSafeComponentsAutoSelected(roiConfigurationDefaultValues
          .getSafeComponentsAutoSelectedDefault());
      roiConfiguration.setWaivedPoliciesCounted(roiConfigurationDefaultValues
          .isWaivedPoliciesCounted());

      return saveRoiConfiguration(mapRoiConfigurationToDTO(roiConfiguration));
    }
    throw new NotFoundException("No default configuration values found for currency type " + currencyType + ".");
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public RoiConfigurationDTO saveRoiConfiguration(final RoiConfigurationDTO roiConfigurationDTO) {
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
    return mapRoiConfigurationToDTO(roiConfigurationDAO.getByCurrencyType(roiConfiguration.getCurrency()));
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
          roiConfigurationDTO.developerHourlyRate(),
          roiConfigurationDTO.fixRateHours(),
          roiConfigurationDTO.securityViolationCriticalEnabled(),
          roiConfigurationDTO.securityViolationCriticalValue(),
          roiConfigurationDTO.securityViolationHighEnabled(),
          roiConfigurationDTO.securityViolationHighValue(),
          roiConfigurationDTO.securityViolationMediumEnabled(),
          roiConfigurationDTO.securityViolationMediumValue(),
          roiConfigurationDTO.securityViolationLowEnabled(),
          roiConfigurationDTO.securityViolationLowValue(),
          roiConfigurationDTO.supplyChainAttacksBlocked(),
          roiConfigurationDTO.namespaceAttacksBlocked(),
          roiConfigurationDTO.safeComponentsAutoSelected(),
          roiConfigurationDTO.waivedPoliciesCounted()
      );
    }
    else if (licensedSolutions.contains(Solution.LIFECYCLE)) {
      validateLifecycleValues(roiConfigurationDTO, roiConfigurationDefaultValues);
      return new RoiConfiguration(
          CurrencyTypes.valueOf(roiConfigurationDTO.currency().name()),
          roiConfigurationDTO.developerHourlyRate(),
          roiConfigurationDTO.fixRateHours(),
          roiConfigurationDTO.securityViolationCriticalEnabled(),
          roiConfigurationDTO.securityViolationCriticalValue(),
          roiConfigurationDTO.securityViolationHighEnabled(),
          roiConfigurationDTO.securityViolationHighValue(),
          roiConfigurationDTO.securityViolationMediumEnabled(),
          roiConfigurationDTO.securityViolationMediumValue(),
          roiConfigurationDTO.securityViolationLowEnabled(),
          roiConfigurationDTO.securityViolationLowValue(),
          roiConfigurationDTO.waivedPoliciesCounted()
      );
    }
    else if (licensedSolutions.contains(Solution.FIREWALL)) {
      validateFirewallValues(roiConfigurationDTO, roiConfigurationDefaultValues);
      return new RoiConfiguration(
          CurrencyTypes.valueOf(roiConfigurationDTO.currency().name()),
          roiConfigurationDTO.supplyChainAttacksBlocked(),
          roiConfigurationDTO.namespaceAttacksBlocked(),
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
    validateValue("Developer hourly rate", roiConfigurationDTO.developerHourlyRate(),
        roiConfigurationDefaultValues.getDeveloperHourlyRateMinimum());
    validateValue("Fix rate hours", roiConfigurationDTO.fixRateHours(),
        roiConfigurationDefaultValues.getFixRateHoursMinimum());
    validateValue("Security violation critical value", roiConfigurationDTO.securityViolationCriticalValue(),
        roiConfigurationDefaultValues.getSecurityViolationCriticalMinimum());
    validateValue("Security violation high value", roiConfigurationDTO.securityViolationHighValue(),
        roiConfigurationDefaultValues.getSecurityViolationHighMinimum());
    validateValue("Security violation medium value", roiConfigurationDTO.securityViolationMediumValue(),
        roiConfigurationDefaultValues.getSecurityViolationMediumMinimum());
    validateValue("Security violation low value", roiConfigurationDTO.securityViolationLowValue(),
        roiConfigurationDefaultValues.getSecurityViolationLowMinimum());
  }

  private void validateFirewallValues(
      RoiConfigurationDTO roiConfigurationDTO,
      RoiConfigurationDefaultValues roiConfigurationDefaultValues)
  {
    validateValue("Supply chain attacks blocked", roiConfigurationDTO.supplyChainAttacksBlocked(),
        roiConfigurationDefaultValues.getSupplyChainAttacksBlockedMinimum());
    validateValue("Namespace attacks blocked", roiConfigurationDTO.namespaceAttacksBlocked(),
        roiConfigurationDefaultValues.getNamespaceAttacksBlockedMinimum());
    validateValue("Safe components auto selected", roiConfigurationDTO.safeComponentsAutoSelected(),
        roiConfigurationDefaultValues.getSafeComponentsAutoSelectedMinimum());
  }

  private void generateAuditInformationForRoiChange(RoiConfiguration roiConfiguration) {
    AuditData.get().setData("currency", roiConfiguration.getCurrency());
    AuditData.get().setData("developerHourlyRate", roiConfiguration.getDeveloperHourlyRate().toString());
    AuditData.get().setData("fixRateHours", roiConfiguration.getFixRateHours().toString());
    AuditData.get().setData("securityViolationCriticalEnabled", roiConfiguration.isSecurityViolationCriticalEnabled());
    AuditData.get()
        .setData("securityViolationCriticalValue", roiConfiguration.getSecurityViolationCriticalValue().toString());
    AuditData.get().setData("securityViolationHighEnabled", roiConfiguration.isSecurityViolationHighEnabled());
    AuditData.get().setData("securityViolationHighValue", roiConfiguration.getSecurityViolationHighValue().toString());
    AuditData.get().setData("securityViolationMediumEnabled", roiConfiguration.isSecurityViolationMediumEnabled());
    AuditData.get()
        .setData("securityViolationMediumValue", roiConfiguration.getSecurityViolationMediumValue().toString());
    AuditData.get().setData("securityViolationLowEnabled", roiConfiguration.isSecurityViolationLowEnabled());
    AuditData.get().setData("securityViolationLowValue", roiConfiguration.getSecurityViolationLowValue().toString());
    AuditData.get().setData("supplyChainAttacksBlocked", roiConfiguration.getSupplyChainAttacksBlocked().toString());
    AuditData.get().setData("namespaceAttacksBlocked", roiConfiguration.getNamespaceAttacksBlocked().toString());
    AuditData.get().setData("safeComponentsAutoSelected", roiConfiguration.getSafeComponentsAutoSelected().toString());
    AuditData.get().setData("waivedPoliciesCounted", roiConfiguration.isWaivedPoliciesCounted());
  }

  private void generateTelemetryEventForRoiChange(RoiConfiguration roiConfiguration) {
    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.ROI_CONFIG_CHANGED);
    telemetryData.put("currency", roiConfiguration.getCurrency());
    telemetryData.put("developerHourlyRate", roiConfiguration.getDeveloperHourlyRate());
    telemetryData.put("fixRateHours", roiConfiguration.getFixRateHours());
    telemetryData.put("securityViolationCriticalEnabled", roiConfiguration.isSecurityViolationCriticalEnabled());
    telemetryData.put("securityViolationCriticalValue", roiConfiguration.getSecurityViolationCriticalValue());
    telemetryData.put("securityViolationHighEnabled", roiConfiguration.isSecurityViolationHighEnabled());
    telemetryData.put("securityViolationHighValue", roiConfiguration.getSecurityViolationHighValue());
    telemetryData.put("securityViolationMediumEnabled", roiConfiguration.isSecurityViolationMediumEnabled());
    telemetryData.put("securityViolationMediumValue", roiConfiguration.getSecurityViolationMediumValue());
    telemetryData.put("securityViolationLowEnabled", roiConfiguration.isSecurityViolationLowEnabled());
    telemetryData.put("securityViolationLowValue", roiConfiguration.getSecurityViolationLowValue());
    telemetryData.put("supplyChainAttacksBlocked", roiConfiguration.getSupplyChainAttacksBlocked());
    telemetryData.put("namespaceAttacksBlocked", roiConfiguration.getNamespaceAttacksBlocked());
    telemetryData.put("safeComponentsAutoSelected", roiConfiguration.getSafeComponentsAutoSelected());
    telemetryData.put("waivedPoliciesCounted", roiConfiguration.isWaivedPoliciesCounted());
    telemetrySender.send(telemetryData);
  }

  private RoiConfigurationDTO mapRoiConfigurationToDTO(RoiConfiguration roiConfiguration) {
    return new RoiConfigurationDTO(
        roiConfiguration.getId(),
        CurrencyTypes.valueOf(roiConfiguration.getCurrency().name()),
        roiConfiguration.getDeveloperHourlyRate(),
        roiConfiguration.getFixRateHours(),
        roiConfiguration.isSecurityViolationCriticalEnabled(),
        roiConfiguration.getSecurityViolationCriticalValue(),
        roiConfiguration.isSecurityViolationHighEnabled(),
        roiConfiguration.getSecurityViolationHighValue(),
        roiConfiguration.isSecurityViolationMediumEnabled(),
        roiConfiguration.getSecurityViolationMediumValue(),
        roiConfiguration.isSecurityViolationLowEnabled(),
        roiConfiguration.getSecurityViolationLowValue(),
        roiConfiguration.getSupplyChainAttacksBlocked(),
        roiConfiguration.getNamespaceAttacksBlocked(),
        roiConfiguration.getSafeComponentsAutoSelected(),
        roiConfiguration.isWaivedPoliciesCounted()
    );
  }

  private <T extends Comparable<T>> void validateValue(String fieldName, T value, T minimumValue) {
    if (value == null || value.compareTo(minimumValue) < 0) {
      throw new BadRequestException(fieldName + " cannot be less than " + minimumValue);
    }
  }
}
