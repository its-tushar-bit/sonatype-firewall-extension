/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import com.sonatype.insight.purl.InvalidPackageURLException;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import com.github.packageurl.MalformedPackageURLException;
import com.github.packageurl.PackageURL;
import com.github.packageurl.PackageURLBuilder;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SbomIdentityUtils
{
  private static final Logger log = LoggerFactory.getLogger(SbomIdentityUtils.class);

  private static final String CPE_2_2_PREFIX = "cpe:/";

  private static final String CPE_2_3_PREFIX = "cpe:2.3:";

  /**
   * CPE 2.2 Structure:
   * <pre>
   * cpe:/{part}:{vendor}:{product}:{version}:{update}:{edition}:{language}
   * </pre>
   * Mapping:
   * <pre>
   * pkg:cpe/{vendor}/{product}@{version}?update={update}&edition={edition}&language={language}
   * </pre>
   * <p>
   * CPE 2.3 Structure :
   * <pre>
   * cpe:{cpe_version}:{part}:{vendor}:{product}:{version}:{update}:{edition}:
   *    {language}:{sw_edition}:{target_sw}:{target_hw}:{other}
   * </pre>
   * Mapping:
   * <pre>
   * pkg:cpe/{vendor}/{product}@{version}?update={update}&edition={edition}&language={language}&
   *    sw_edition={sw_edition}&target_sw={target_sw}&target_hw={target_hw}&other={other}
   * </pre>
   * <p>
   * All PURL qualifiers are specified only if they are not empty or *.
   */
  public static  PackageUrlIdentifier buildPackageUrlFromCpe(final String cpe) {
    // CPE examples:
    // cpe:/a:microsoft:internet_explorer:8.0.6001:beta
    // cpe:2.3:a:microsoft:internet_explorer:8.0.6001:beta:*:*:*:*:*:*

    if (StringUtils.isBlank(cpe)) {
      log.debug("Invalid cpe; it cannot be blank");
      return null;
    }
    String payload;
    if (cpe.startsWith(CPE_2_3_PREFIX)) {
      payload = cpe.substring(CPE_2_3_PREFIX.length());
    }
    else {
      payload = cpe.substring(CPE_2_2_PREFIX.length());
    }

    String[] cpeParts = payload.split(":");
    if (cpeParts.length < 4) {
      log.debug("Invalid cpe: {}", cpe);
      return null;
    }

    try {
      PackageURLBuilder packageURLBuilder = PackageURLBuilder.aPackageURL();
      packageURLBuilder
          .withType("cpe")
          .withName(ThirdPartyScanResultUtils.getTruncatedName(urlDecode(cpeParts[2])))
          .withVersion(ThirdPartyScanResultUtils.getTruncatedVersion(urlDecode(cpeParts[3])));

      if (StringUtils.isNotBlank(cpeParts[1])) { // vendor
        packageURLBuilder.withNamespace(urlDecode(cpeParts[1]));
      }
      addQualifierIfExists(packageURLBuilder, "update", cpeParts, 4);
      addQualifierIfExists(packageURLBuilder, "edition", cpeParts, 5);
      addQualifierIfExists(packageURLBuilder, "language", cpeParts, 6);
      addQualifierIfExists(packageURLBuilder, "sw_edition", cpeParts, 7);
      addQualifierIfExists(packageURLBuilder, "target_sw", cpeParts, 8);
      addQualifierIfExists(packageURLBuilder, "target_hw", cpeParts, 9);
      addQualifierIfExists(packageURLBuilder, "other", cpeParts, 10);

      PackageURL packageUrl = packageURLBuilder.build();
      return new PackageUrlIdentifier(packageUrl.canonicalize());
    }
    catch (MalformedPackageURLException | UnsupportedEncodingException e) {
      throw new InvalidPackageURLException(e.getMessage(), e);
    }
  }

  private static void addQualifierIfExists(PackageURLBuilder builder, String name, String[] parts, int index)
      throws UnsupportedEncodingException
  {
    if (parts.length > index && StringUtils.isNotBlank(parts[index]) && !"*".equals(parts[index])) {
      builder.withQualifier(name, urlDecode(parts[index]));
    }
  }

  private static String urlDecode(String input) throws UnsupportedEncodingException {
    if (StringUtils.isEmpty(input)) {
      return input;
    }
    return URLDecoder.decode(input, StandardCharsets.UTF_8.name());
  }
}
