/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.roi;

import java.math.BigDecimal;
import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.roi.RoiConfigurationDefaultValuesDAO;
import com.sonatype.insight.brain.model.roi.CurrencyTypes;
import com.sonatype.insight.brain.roi.dtos.RoiConfigurationDTO;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Before;
import org.junit.Test;

public class RoiConfigurationAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private RoiConfigurationService roiConfigurationService;

  @Inject
  private RoiConfigurationDefaultValuesDAO dao;

  @Before
  public void setup() {
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
  public void testGetCurrentAndMinimumValuesByCurrencyType() {
    grantConfigureSystemPermission();
    roiConfigurationService.getCurrentAndMinimumValuesByCurrencyType("usd");
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetCurrentAndMinimumValuesByCurrencyType_Unauthenticated() {
    roiConfigurationService.getCurrentAndMinimumValuesByCurrencyType("usd");
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetCurrentAndMinimumValuesByCurrencyType_UnauthorizedException() {
    login();
    roiConfigurationService.getCurrentAndMinimumValuesByCurrencyType("usd");
  }

  @Test(expected = UnauthorizedException.class)
  public void testSaveRoiConfiguration_UnauthorizedException() {
    login();
    roiConfigurationService.saveRoiConfiguration(getRoiConfigurationDTO());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testSaveRoiConfiguration_Unauthenticated() {
    roiConfigurationService.saveRoiConfiguration(getRoiConfigurationDTO());
  }

  @Test(expected = UnauthorizedException.class)
  public void testRestoreToDefaultValuesByCurrencyType_UnauthorizedException() {
    login();
    roiConfigurationService.saveRoiConfiguration(getRoiConfigurationDTO());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testRestoreToDefaultValuesByCurrencyType_Unauthenticated() {
    roiConfigurationService.saveRoiConfiguration(getRoiConfigurationDTO());
  }

  @Test
  public void testSaveRoiConfiguration() {
    grantConfigureSystemPermission();
    roiConfigurationService.saveRoiConfiguration(getRoiConfigurationDTO());
  }

  private RoiConfigurationDTO getRoiConfigurationDTO() {
    return new RoiConfigurationDTO(
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
  }
}
