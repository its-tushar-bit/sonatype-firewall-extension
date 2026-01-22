/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.admin.authorization.provider;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.auth0.jwk.Jwk;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.io.IOUtils;
import org.bouncycastle.util.io.pem.PemReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class MultiTenantJwkLocalProvider
    implements MultiTenantJwkProvider
{
  private static final Logger log = LoggerFactory.getLogger(MultiTenantJwkLocalProvider.class.getName());

  private static final String LOCAL_AUTH_DOMAIN = "local/";

  private static final String RS_256 = "RS256";

  private static final String RSA = "RSA";

  private static final String SIG = "sig";

  private static final String CERT_CHAIN = "CERT_CHAIN";

  private static final String THUMBPRINT = "THUMBPRINT";

  private static final List<String> KEY_OPS = Lists.newArrayList("sign");

  private Jwk jwk;

  private boolean denyRequest;

  @Override
  public Jwk getJsonWebKey(final String keyId) {
    RSAPublicKey rsaPublicKey;
    try {
      rsaPublicKey = (RSAPublicKey) getCustomKeys().getPublic();

      Map<String, Object> values = Maps.newHashMap();
      values.put("alg", RS_256);
      values.put("kty", RSA);
      values.put("use", SIG);
      values.put("key_ops", KEY_OPS);
      values.put("x5c", Lists.newArrayList(CERT_CHAIN));
      values.put("x5t", THUMBPRINT);
      values.put("kid", keyId);
      values.put("n", getPublicKeyModulus(rsaPublicKey));
      values.put("e", getPublicKeyExponent(rsaPublicKey));

      jwk = Jwk.fromValues(values);
      log.debug("Jwk from local provider was created");
    }
    catch (Exception e) {
      log.error("Cannot get a Jwk from local provider! All admin access will be denied.", e);
      denyRequest = true;
    }

    return jwk;
  }

  @Override
  public String[] getIssuers() {
    return new String[]{LOCAL_AUTH_DOMAIN};
  }

  @Override
  public boolean denyRequest() {
    return denyRequest;
  }

  private KeyPair getCustomKeys() throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
    final KeyFactory keyFactory = KeyFactory.getInstance("RSA");

    final InputStream pubInputStream =
        getClass().getClassLoader().getResourceAsStream(getClass().getSimpleName() + "/public_key.pem");
    final StringReader pubReader = new StringReader(IOUtils.toString(pubInputStream, StandardCharsets.UTF_8));
    final PemReader pubPemReader = new PemReader(pubReader);
    final X509EncodedKeySpec pubSpec = new X509EncodedKeySpec(pubPemReader.readPemObject().getContent());
    final RSAPublicKey rsaPublicKey = (RSAPublicKey) keyFactory.generatePublic(pubSpec);

    final InputStream keyInputStream = getClass().getClassLoader()
        .getResourceAsStream(getClass().getSimpleName() + "/private_key.pem"); // should be in pkcs8 format!
    final StringReader keyReader = new StringReader(IOUtils.toString(keyInputStream, StandardCharsets.UTF_8));
    final PemReader keyPemReader = new PemReader(keyReader);
    final PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyPemReader.readPemObject().getContent());
    final RSAPrivateKey rsaPrivateKey = (RSAPrivateKey) keyFactory.generatePrivate(keySpec);

    return new KeyPair(rsaPublicKey, rsaPrivateKey);
  }

  private String getPublicKeyModulus(RSAPublicKey rsaPublicKey) {
    return Base64.getUrlEncoder().encodeToString(rsaPublicKey.getModulus().toByteArray());
  }

  private String getPublicKeyExponent(RSAPublicKey rsaPublicKey) {
    return Base64.getUrlEncoder().encodeToString(rsaPublicKey.getPublicExponent().toByteArray());
  }
}
