/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.license;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ProductLicenseServiceExceptionMappingTest
{
  @Mock
  private CLMLicenseManager licenseManager;

  @Mock
  private ProductLicense productLicense;

  private ProductLicenseService productLicenseService;

  @Before
  public void setUp() {
    productLicenseService = new ProductLicenseService(licenseManager, productLicense);
  }

  @Test
  public void shouldTranslateInvalidLicenseToPaymentRequired() {
    doThrow(new InvalidLicenseException("No valid product license installed.")).when(productLicense).validate();

    assertThatThrownBy(() -> productLicenseService.validateLicense())
        .isInstanceOf(WebApplicationException.class)
        .satisfies(exception -> {
          Response response = ((WebApplicationException) exception).getResponse();
          assertThat(response.getStatus()).isEqualTo(402);
          assertThat(response.getEntity()).isEqualTo("No valid product license installed.");
        });
  }
}
