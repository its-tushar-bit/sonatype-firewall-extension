/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.security.PrivateKey;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link GitHubAppKeyUtils}.
 */
public class GitHubAppKeyUtilsTest
{
  /**
   * Test-only RSA private key in Base64-encoded PKCS#8 format.
   * Generated for unit testing purposes only - NOT used in production.
   * This matches the format stored in the database after encryption.
   */
  private static final String VALID_BASE64_PKCS8 =
      "MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQCvxBfy5wcqJKrf" +
      "qo/zY6eUcX6/VdtIO7CzbOi860dr9FbxOBXXnUryXNDsgkCbOutuDbNB/CZzeaVV" +
      "MHT/3MXp/e0Ah0bx26Vm8IzerDc8FBbgvyUUJP9OmICVtmTsA4CeuPq3ys6eVRCl" +
      "yePdJ9hio3bKguiW0ZREcv8rEGVZj6ZOg7vEIivvJmOhzZc0RBs1fQwj80hHMlzi" +
      "v94lgaDV/CMGSB7tsZja65G1LIQmyH5EEzNwsSBHfWC+YzirQXhpdYHXTki0m8Ri" +
      "whtboXgrnLRz4VI5i6ZSrWTuDNIn5vMEhlPYdmACLgoMIeZ+yNkkBrl++0ihj3eD" +
      "7L0MLFj9AgMBAAECggEABY8frMkiPBdZOi9UU8j+maetAjfFTe57p7tn3wgO1k1S" +
      "bcbSQ77iLrt4EZQLDTStEfxezLCu2lzXy8FSzpJLD1LjKtx34b6KnnmmgKlLmIBZ" +
      "TSzcNEmwITN1uasKgIi+ZU/lJY0g5feEpX0kiS/oN+lMDlF/xHKG5+VQpAWKhMjN" +
      "k75ofqmVs8d6meH7AijvcFRGAwdJJ7PGi2XeOk9gw4uTxXTGvzO8KGqAXxtC8D5F" +
      "CoSdo8rRH6whKIMb5Ay7Vwc315JbaK/t0zQDWEB5w5gZ5f6O6H9W7NET7XvbLAUD" +
      "32HvBxJ2AzGnO3siRzNQFNB2zl8eVm0ux25T7/vNEQKBgQDxPzhN3xnGCXudae3g" +
      "1EpVFzaoCPECwhKXHqX8wOilXpN1rr/GvOQ/89uiR1/yc379irYZexbm3bcjc7QI" +
      "gxe8NxCkNxmvRIYdo6fSddjECVQYQ/bVS3VKjK2L5fkb2TCVXSjzXKeA/kBhjlBF" +
      "3bZG8WWyGAoVJZnSpoVPt9zIVQKBgQC6g8DM59ZOkAdxywLziWsswzmwi2Er6WtB" +
      "t6oQ8CnD9dYQT0T9y9a7u3Z9ETusmzRQIiE1U8EM89258apvssqYxYsbTG04WS9/" +
      "a5JDpHIHBhjSw8QXA35lEekjAOw5YSvDGXspXUAxx7CzI/V0wtuPDOI70KwJ7Y4x" +
      "F1CWBWoWCQKBgAQySVRxcQ1U1OWkFhM3HiPkx3qczTRzE8e1LMX8xQ87We1OIN/g" +
      "IUhLgaKNA0pAYBEg/JHs5jUV3j2roZIUVbFcc9mna7b0xjB1zFGI40BluSTC6eRD" +
      "78JgOBSa535ohMPUXwX8sp03zv7jbtoIRUduo4o5iNNdWBOl+eOtGxYRAoGAHcKS" +
      "C9/eIRD4Lx7+bI97q1vHI7VJnvESrRy6JRO9Bkh+jIGpd3mD3NaPlGsgg20MTtDz" +
      "TYf8oK10roux7zqu+utiQ8vRDZGlc4Zdgy+FBjvh0BdufWGQVF4kPfKSvqETk4DI" +
      "VxRK/uZm+l59dtD0qYGbw4GLQdZGvuyCbj2U7MkCgYB9Bvot0b1K0GYQgH6E4Ckc" +
      "iQGoPf9T+n7lSIWYia0QPQnn+zKT2gSNwoYzCwF1b7RfG/mQLVasqqeh2rI+LVyg" +
      "ukkm6Cc03G16SqcmicaVFoto9GSgI6ZX2ynicH7B58DARZrZxzMLRLd+NvDhUnLU" +
      "/2Zcph16+6zclEkOgCD+Gw==";

  @Test
  public void testParsePrivateKeyFromBase64Pkcs8_ValidKey() {
    PrivateKey privateKey = GitHubAppKeyUtils.parsePrivateKeyFromBase64Pkcs8(VALID_BASE64_PKCS8);

    assertThat(privateKey).isNotNull();
    assertThat(privateKey.getAlgorithm()).isEqualTo("RSA");
    assertThat(privateKey.getFormat()).isEqualTo("PKCS#8");
  }

  @Test
  public void testParsePrivateKeyFromBase64Pkcs8_NullContent() {
    assertThatThrownBy(() -> GitHubAppKeyUtils.parsePrivateKeyFromBase64Pkcs8(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Private key content cannot be null or empty");
  }

  @Test
  public void testParsePrivateKeyFromBase64Pkcs8_EmptyContent() {
    assertThatThrownBy(() -> GitHubAppKeyUtils.parsePrivateKeyFromBase64Pkcs8(""))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Private key content cannot be null or empty");
  }

  @Test
  public void testParsePrivateKeyFromBase64Pkcs8_WhitespaceOnly() {
    assertThatThrownBy(() -> GitHubAppKeyUtils.parsePrivateKeyFromBase64Pkcs8("   \n\t  "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Private key content cannot be null or empty");
  }

  @Test
  public void testParsePrivateKeyFromBase64Pkcs8_InvalidBase64() {
    String invalidBase64 = "This is not valid Base64!!!";

    assertThatThrownBy(() -> GitHubAppKeyUtils.parsePrivateKeyFromBase64Pkcs8(invalidBase64))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Failed to parse RSA private key from Base64 PKCS8 format");
  }

  @Test
  public void testParsePrivateKeyFromBase64Pkcs8_InvalidKeyFormat() {
    String validBase64ButInvalidKey = "SGVsbG8gV29ybGQh";

    assertThatThrownBy(() -> GitHubAppKeyUtils.parsePrivateKeyFromBase64Pkcs8(validBase64ButInvalidKey))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Failed to parse RSA private key from Base64 PKCS8 format");
  }
}
