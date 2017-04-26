/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration.repository;

import javax.inject.Inject;

import com.sonatype.insight.brain.TestProductLicenseManager;
import com.sonatype.insight.brain.product.license.CLMLicenseManager;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static com.sonatype.insight.brain.integration.repository.FirewallMigrationService.PROTOCOL_V1;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class FirewallMigrationServiceTest
    extends AbstractComponentTest
{
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Inject
  private CLMLicenseManager clmLicenseManager;

  @Inject
  private TestProductLicenseManager productLicenseManager;

  @Inject
  private FirewallMigrationService migrationService;

  @Test
  public void testVerifyMigrationSupport() throws Exception {
    migrationService.verifyMigrationSupport(PROTOCOL_V1);
  }

  @Test
  public void testVerifyMigrationSupport_UnsupportedProtocolVersion() throws Exception {
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage(endsWith("does not support migration protocol v2, please update your IQ Server."));
    migrationService.verifyMigrationSupport("v2");
  }

  @Test
  public void testVerifyMigrationSupport_MissingLicenseFeature() throws Exception {
    productLicenseManager.setProducts(ProductLicenseDetails.PRODUCT_RISK);
    clmLicenseManager.installLicense(null);
    try {
      migrationService.verifyMigrationSupport(PROTOCOL_V1);
      fail("Expected exception");
    }
    catch (InvalidLicenseException expected) {
      assertThat(expected.getMessage(), is(InvalidLicenseException.INVALID_LICENSE_MSG));
    }
  }
}
