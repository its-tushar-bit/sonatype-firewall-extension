/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

// Vendored/copied from hosted-data-services/insight-license-util
package com.sonatype.insight.test.productlicense;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Signature;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.security.FIPSModeDetector;
import com.sonatype.insight.brain.security.certificate.CertificateFactory;
import com.sonatype.insight.brain.security.keystore.KeyStoreFactory;
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
    String keyStorePath = resolveKeyStorePath();
    if (keyStorePath == null) {
      throw new IllegalStateException("ProductLicenseConfig.keyStorePath is not configured");
    }
    try (InputStream keyStoreInputStream = new FileInputStream(keyStorePath)) {
      // Load key store.
      KeyStore keyStore = createKeyStore();
      keyStore.load(keyStoreInputStream, getKeyStorePassword());

      // Load private key.
      String keyAlias = keyStoreConfig.getKeyStoreAliasGroup();
      Key privateKey = keyStore.getKey(keyAlias, getKeyStorePassword());

      // Init signature.
      Signature signature = Signature.getInstance(getSignatureAlgorithm());
      signature.initSign((PrivateKey) privateKey);

      // Add data to be signed.
      for (String feature : signedProductLicenseDetailsDTO.features) {
        signature.update(feature.getBytes(StandardCharsets.UTF_8));
      }
      for (String stageId : signedProductLicenseDetailsDTO.stageIds) {
        signature.update(stageId.getBytes(StandardCharsets.UTF_8));
      }
      signature.update((signedProductLicenseDetailsDTO.maxApplications == null
          ? "0"
          : signedProductLicenseDetailsDTO.maxApplications.toString()).getBytes(StandardCharsets.UTF_8));

      if (signedProductLicenseDetailsDTO.maxSboms != null) {
        signature.update(signedProductLicenseDetailsDTO.maxSboms.toString().getBytes(StandardCharsets.UTF_8));
      }

      if (signedProductLicenseDetailsDTO.creditAmount != null) {
        signature.update(signedProductLicenseDetailsDTO.creditAmount.stripTrailingZeros()
            .toPlainString()
            .getBytes(StandardCharsets.UTF_8));
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

  private String resolveKeyStorePath() {
    String keyStorePath = keyStoreConfig.getKeyStorePath();
    if (!FIPSModeDetector.isEnabled() || keyStorePath == null || !keyStorePath.endsWith(".p12")) {
      return keyStorePath;
    }

    File fipsKeyStore = new File(keyStorePath.substring(0, keyStorePath.length() - 4) + ".bcfks");
    return fipsKeyStore.exists() ? fipsKeyStore.getAbsolutePath() : keyStorePath;
  }

  private KeyStore createKeyStore() throws Exception {
    return FIPSModeDetector.isEnabled() ? KeyStoreFactory.createKeyStore() : KeyStoreFactory.createPkcs12KeyStore();
  }

  private String getSignatureAlgorithm() {
    return FIPSModeDetector.isEnabled()
        ? CertificateFactory.getSignatureAlgorithm()
        : CertificateFactory.SIGNATURE_ALGORITHM;
  }
}
