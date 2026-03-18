/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.admin.authorization;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
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
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.auth0.jwk.Jwk;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.io.FileUtils;
import org.bouncycastle.util.io.pem.PemReader;

import static com.sonatype.insight.brain.api.admin.authorization.AuthJWTClaims.USER_EMAIL_CLAIM;

public class AuthorizationTestHelper
{
  private static final String RS_256 = "RS256";

  private static final String RSA = "RSA";

  private static final String SIG = "sig";

  private static final String CERT_CHAIN = "CERT_CHAIN";

  private static final String THUMBPRINT = "THUMBPRINT";

  private static final List<String> KEY_OPS = Lists.newArrayList("sign");

  public static String createJwt() throws Exception {
    return createJwt("test/");
  }

  public static String createJwt(String issuer) throws Exception {
    final Algorithm algorithm = getRSA256Algorithm();

    return JWT.create()
        .withKeyId("A1B2C3")
        .withClaim(USER_EMAIL_CLAIM.getClaim(), "test@test.com")
        .withIssuer(issuer)
        .withSubject("test|123456")
        .withAudience("https://test.mtiq-admin-service.cloudy.sonatype.dev/")
        .withIssuedAt(new Date())
        .withExpiresAt(new Date(System.currentTimeMillis() + 100000L))
        .sign(algorithm);
  }

  public static String createJwt(Map<String, ?> withValues, Algorithm algorithm) throws Exception {
    return JWT.create()
        .withPayload(withValues)
        .sign(algorithm);
  }

  public static Jwk createJwk(String kid) throws Exception {
    RSAPublicKey rsaPublicKey = (RSAPublicKey) getCustomKeys().getPublic();

    Map<String, Object> values = Maps.newHashMap();
    values.put("alg", RS_256);
    values.put("kty", RSA);
    values.put("use", SIG);
    values.put("key_ops", KEY_OPS);
    values.put("x5c", Lists.newArrayList(CERT_CHAIN));
    values.put("x5t", THUMBPRINT);
    values.put("kid", kid);
    values.put("n", getPublicKeyModulus(rsaPublicKey));
    values.put("e", getPublicKeyExponent(rsaPublicKey));

    return Jwk.fromValues(values);
  }

  public static Algorithm getRSA256Algorithm() throws Exception {
    final KeyPair keyPair = getCustomKeys();

    return Algorithm.RSA256((RSAPublicKey) keyPair.getPublic(), (RSAPrivateKey) keyPair.getPrivate());
  }

  private static KeyPair getCustomKeys() throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
    final KeyFactory keyFactory = KeyFactory.getInstance("RSA");

    final URL pubResource = AuthorizationTestHelper.class.getClassLoader()
        .getResource(AuthorizationTestHelper.class.getSimpleName() + "/public_key.pem");
    final StringReader pubReader =
        new StringReader(FileUtils.readFileToString(new File(pubResource.getFile()), StandardCharsets.UTF_8));
    final PemReader pubPemReader = new PemReader(pubReader);
    final X509EncodedKeySpec pubSpec = new X509EncodedKeySpec(pubPemReader.readPemObject().getContent());
    final RSAPublicKey rsaPublicKey = (RSAPublicKey) keyFactory.generatePublic(pubSpec);

    final URL keyResource = AuthorizationTestHelper.class.getClassLoader()
        .getResource(
            AuthorizationTestHelper.class.getSimpleName() + "/private_key.pem"); // should be in pkcs8 format!
    final StringReader keyReader =
        new StringReader(FileUtils.readFileToString(new File(keyResource.getFile()), StandardCharsets.UTF_8));
    final PemReader keyPemReader = new PemReader(keyReader);
    final PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyPemReader.readPemObject().getContent());
    final RSAPrivateKey rsaPrivateKey = (RSAPrivateKey) keyFactory.generatePrivate(keySpec);

    return new KeyPair(rsaPublicKey, rsaPrivateKey);
  }

  private static String getPublicKeyModulus(RSAPublicKey rsaPublicKey) {
    return Base64.getUrlEncoder().encodeToString(rsaPublicKey.getModulus().toByteArray());
  }

  private static String getPublicKeyExponent(RSAPublicKey rsaPublicKey) {
    return Base64.getUrlEncoder().encodeToString(rsaPublicKey.getPublicExponent().toByteArray());
  }
}
