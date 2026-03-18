/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.zscaler;

import com.sonatype.insight.error.exception.BadRequestException;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ZScalerValidatorTest
{
  private static final String REQUIRED_MESSAGE = "Host name is required";

  private static final String HOSTNAME_REQUIRED_MESSAGE = "Hostname is required in URL";

  private static final String INVALID_PROTOCOL_MESSAGE =
      "Protocol must be http or https";

  private static final String NO_PATHS_MESSAGE =
      "Only base URL allowed - no paths or trailing slashes";

  private static final String NO_QUERY_PARAMS_MESSAGE =
      "Query parameters and fragments not allowed";

  private static final String INVALID_URL_MESSAGE = "Not a valid URL";

  // ===== VALID CASES (Should Pass) =====

  @Test
  public void testValidHttpsUrls() {
    ZScalerValidator.validateHostName("https://api.company.com");
    ZScalerValidator.validateHostName("https://proxy.internal.net");
    ZScalerValidator.validateHostName("https://gateway.example.org");
    ZScalerValidator.validateHostName("https://firewall.corporate.net");

    ZScalerValidator.validateHostName("https://api.zscaler.net");
    ZScalerValidator.validateHostName("https://zsapi.zscalertwo.net");
    ZScalerValidator.validateHostName("https://admin.zscalerthree.net");
    ZScalerValidator.validateHostName("https://gateway.zscloud.net");
  }

  @Test
  public void testValidHttpUrls() {
    ZScalerValidator.validateHostName("http://api.company.com");
    ZScalerValidator.validateHostName("http://proxy.example.com");
    ZScalerValidator.validateHostName("http://gateway.internal.org");
    ZScalerValidator.validateHostName("http://api.zscaler.net"); // ZScaler still works
  }

  @Test
  public void testValidSubdomains() {
    ZScalerValidator.validateHostName("https://my.company.example.com");
    ZScalerValidator.validateHostName("https://test-env.proxy.internal.net");
    ZScalerValidator.validateHostName("https://api-v2.gateway.corporate.org");

    ZScalerValidator.validateHostName("https://company.zscalerone.net");
    ZScalerValidator.validateHostName("https://dev.zscalertwo.net");
    ZScalerValidator.validateHostName("https://prod.zscalerthree.net");
    ZScalerValidator.validateHostName("https://test.zscloud.net");
  }

  @Test
  public void testValidUrlsWithPorts() {
    ZScalerValidator.validateHostName("https://api.company.com:443");
    ZScalerValidator.validateHostName("http://proxy.internal.net:80");
    ZScalerValidator.validateHostName("https://gateway.example.org:8080");
    ZScalerValidator.validateHostName("http://firewall.corporate.net:9000");

    ZScalerValidator.validateHostName("https://api.zscaler.net:443");
    ZScalerValidator.validateHostName("http://api.zscloud.net:8080");
  }

  @Test
  public void testValidUrlsWithWhitespace() {
    ZScalerValidator.validateHostName("  https://api.company.com  ");
    ZScalerValidator.validateHostName("\thttps://proxy.example.com\n");
    ZScalerValidator.validateHostName("   https://gateway.zscaler.net   "); // ZScaler example
  }

  // ===== INVALID CASES (Should Throw BadRequestException) =====

  @Test
  public void testNullAndEmptyHostnames() {
    assertValidationFails(null, REQUIRED_MESSAGE);
    assertValidationFails("", REQUIRED_MESSAGE);
    assertValidationFails("   ", REQUIRED_MESSAGE);
    assertValidationFails("\t\n", REQUIRED_MESSAGE);
  }

  @Test
  public void testValidGenericDomains() {
    ZScalerValidator.validateHostName("https://google.com");
    ZScalerValidator.validateHostName("https://api.company.com");
    ZScalerValidator.validateHostName("https://proxy.internal.net");
    ZScalerValidator.validateHostName("https://gateway.example.org");
  }

  @Test
  public void testInvalidProtocols() {
    assertValidationFails("ftp://api.company.com", INVALID_PROTOCOL_MESSAGE);
    assertValidationFails("file://proxy.example.com", INVALID_PROTOCOL_MESSAGE);
    assertValidationFails("ldap://gateway.internal.net", INVALID_PROTOCOL_MESSAGE);
    assertValidationFails("ssh://firewall.corporate.org", INVALID_PROTOCOL_MESSAGE);
  }

  @Test
  public void testPathsNotAllowed() {
    assertValidationFails("https://api.company.com/", NO_PATHS_MESSAGE);
    assertValidationFails("https://proxy.example.com/path", NO_PATHS_MESSAGE);
    assertValidationFails("https://gateway.internal.net/api/v1", NO_PATHS_MESSAGE);
    assertValidationFails("https://api.zscaler.net/config", NO_PATHS_MESSAGE); // ZScaler example
  }

  @Test
  public void testQueryParamsAndFragmentsNotAllowed() {
    assertValidationFails("https://api.company.com?param=value", NO_QUERY_PARAMS_MESSAGE);
    assertValidationFails("https://proxy.example.com#section", NO_QUERY_PARAMS_MESSAGE);
    assertValidationFails("https://gateway.internal.net?config=true&debug=1", NO_QUERY_PARAMS_MESSAGE);
    assertValidationFails("https://api.zscaler.net?token=abc", NO_QUERY_PARAMS_MESSAGE); // ZScaler example
  }

  @Test
  public void testInvalidUrlFormats() {
    assertValidationFails("not-a-url", INVALID_URL_MESSAGE);
    assertValidationFails("://api.company.com", INVALID_URL_MESSAGE);
    assertValidationFails("https://", INVALID_URL_MESSAGE);
    assertValidationFails("https:///path", HOSTNAME_REQUIRED_MESSAGE);
  }

  // ===== HELPER METHOD =====

  private void assertValidationFails(String hostname, String expectedMessage) {
    try {
      ZScalerValidator.validateHostName(hostname);
      fail("Expected BadRequestException for hostname: " + hostname);
    }
    catch (BadRequestException e) {
      assertEquals("Unexpected error message for hostname: " + hostname,
          expectedMessage, e.getMessage());
    }
  }
}
