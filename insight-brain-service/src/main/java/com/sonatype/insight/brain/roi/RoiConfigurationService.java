/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.roi;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.dataaccess.roi.RoiConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.roi.RoiConfigurationDefaultValuesDAO;
import com.sonatype.insight.brain.model.roi.CurrencyTypes;
import com.sonatype.insight.brain.model.roi.RoiConfiguration;
import com.sonatype.insight.brain.model.roi.RoiConfigurationDefaultValues;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;

@Named
public class RoiConfigurationService
{
  private final RoiConfigurationDAO roiConfigurationDAO;

  private final RoiConfigurationDefaultValuesDAO roiConfigurationDefaultValuesDAO;

  @Inject
  public  RoiConfigurationService(RoiConfigurationDAO roiConfigurationDAO,
                                  RoiConfigurationDefaultValuesDAO roiConfigurationDefaultValuesDAO)
  {
    this.roiConfigurationDAO = roiConfigurationDAO;
    this.roiConfigurationDefaultValuesDAO = roiConfigurationDefaultValuesDAO;
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
}
