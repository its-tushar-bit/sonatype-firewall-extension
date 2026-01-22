/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

// Vendored/copied from hosted-data-services/insight-license-util
package com.sonatype.insight.test.productlicense;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Signature;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.license.model.SignedProductLicenseDetailsDTO;
import org.sonatype.licensing.util.LicensingUtil;

@Named
public class ProductLicenseSigner
{
  private static final long[] OBFUSCATED_KEYSTORE_PASSWORD =
      new long[]{0x3B242FD2992ECA8CL, 0x74471D593083D0A6L, 0xEE35ABF8A2A4F8C0L};

  private final ProductLicenseConfig keyStoreConfig;

  @Inject
  ProductLicenseSigner(ProductLicenseConfig keyStoreConfig) {
    this.keyStoreConfig = keyStoreConfig;
  }

  public void sign(SignedProductLicenseDetailsDTO signedProductLicenseDetailsDTO, String licenseFingerprint) {
    try (InputStream keyStoreInputStream = new FileInputStream(keyStoreConfig.getKeyStorePath())) {
      // Load key store.
      KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
      keyStore.load(keyStoreInputStream, getKeyStorePassword());

      // Load private key.
      String keyAlias = keyStoreConfig.getKeyStoreAliasGroup();
      Key privateKey = keyStore.getKey(keyAlias, getKeyStorePassword());

      // Init signature.
      Signature signature = Signature.getInstance("SHA256withRSA");
      signature.initSign((PrivateKey) privateKey);

      // Add data to be signed.
      for (String feature : signedProductLicenseDetailsDTO.features) {
        signature.update(feature.getBytes(StandardCharsets.UTF_8));
      }
      for (String stageId : signedProductLicenseDetailsDTO.stageIds) {
        signature.update(stageId.getBytes(StandardCharsets.UTF_8));
      }
      signature.update((signedProductLicenseDetailsDTO.maxApplications == null ? "0"
          : signedProductLicenseDetailsDTO.maxApplications.toString()).getBytes(StandardCharsets.UTF_8));

      if (signedProductLicenseDetailsDTO.maxSboms != null) {
        signature.update(signedProductLicenseDetailsDTO.maxSboms.toString().getBytes(StandardCharsets.UTF_8));
      }

      signature.update(licenseFingerprint.getBytes(StandardCharsets.UTF_8));

      // Create signature and save it in the DTO.
      signedProductLicenseDetailsDTO.signature = signature.sign();
      signedProductLicenseDetailsDTO.signatureKeyAlias = keyAlias;
    }
    catch (RuntimeException e) {
      throw e;
    }
    catch (Exception e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  private char[] getKeyStorePassword() {
    return LicensingUtil.unobfuscate(OBFUSCATED_KEYSTORE_PASSWORD).toCharArray();
  }
}
