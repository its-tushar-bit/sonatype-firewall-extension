/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.roi;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.dataaccess.roi.RoiConfigurationDefaultValuesDAO;
import com.sonatype.insight.brain.model.roi.CurrencyTypes;
import com.sonatype.insight.brain.roi.dtos.RoiConfigurationDTO;
import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@ComponentH2Test
public class RoiConfigurationAuthzTest
    extends AbstractComponentH2AuthzTest
{
  @Inject
  private RoiConfigurationService roiConfigurationService;

  @Inject
  private RoiConfigurationDefaultValuesDAO dao;

  @BeforeEach
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
  public void testGetCurrentAndMinimumValuesByCurrencyType() {
    grantConfigureSystemPermission();
    roiConfigurationService.getCurrentAndMinimumValuesByCurrencyType("usd");
  }

  @Test
  public void testGetCurrentAndMinimumValuesByCurrencyType_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> roiConfigurationService.getCurrentAndMinimumValuesByCurrencyType("usd"));
  }

  @Test
  public void testGetCurrentAndMinimumValuesByCurrencyType_UnauthorizedException() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> roiConfigurationService.getCurrentAndMinimumValuesByCurrencyType("usd"));
  }

  @Test
  public void testSaveRoiConfiguration_UnauthorizedException() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> roiConfigurationService.saveRoiConfiguration(getRoiConfigurationDTO()));
  }

  @Test
  public void testSaveRoiConfiguration_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> roiConfigurationService.saveRoiConfiguration(getRoiConfigurationDTO()));
  }

  @Test
  public void testRestoreToDefaultValuesByCurrencyType_UnauthorizedException() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> roiConfigurationService.saveRoiConfiguration(getRoiConfigurationDTO()));
  }

  @Test
  public void testRestoreToDefaultValuesByCurrencyType_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> roiConfigurationService.saveRoiConfiguration(getRoiConfigurationDTO()));
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
        BigDecimal.valueOf(500000),
        BigDecimal.valueOf(600000),
        BigDecimal.valueOf(700000),
        30,
        BigDecimal.valueOf(800));
  }
}
