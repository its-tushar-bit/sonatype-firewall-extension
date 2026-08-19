/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.packageurl.MalformedPackageURLException;
import com.github.packageurl.PackageURL;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SbomResultsMatcher
{
  private static final Logger log = LoggerFactory.getLogger(SbomResultsMatcher.class);

  /**
   * PURL (and equally Hash) matches provides one of the best match out of all matching elements we currently have. A
   * match with purl should bias the result slightly more that way so carries a higher weight
   */
  private static final float WEIGHT_PURL = 20.0f;

  /**
   * In an ideal world hash match should provide the best match (better than purl). But there are some eco-systems that
   * sonatype results in fake hashes. Although they are rare we need to account for that possibility. So providing the
   * same weight if it matches with a hash of a sonatype identified result.
   */
  private static final float WEIGHT_HASH = 20.0f;

  /**
   * format, name, version match is kind of a partial match considering there could be other coordinates that could
   * determine the exact component. But these 3 coordinates still is a big factor in matching so considered with a lower
   * weight
   */
  private static final float WEIGHT_FORMAT_NAME_VERSION = 15.0f;

  /**
   * majority of the purl's match should be given to the mandatory coordinates(format, name, version) existence and
   * match. Matching all other coords should never better the match result of these coords.
   */
  public static final float WEIGHT_PERCENT_FOR_PURL_MANDATORY_COORDS = 0.75f;

  /**
   * percent of the weight from purl weight above that carries for a namespace match. It sets to 50% of the difference
   * between a purl and format_name_version match so even in the case of many/all qualifier matches it can only equal to
   * namespace match but will never better match. Meaning namespace match is critical when compared with all other
   * qualifier matches.
   */
  private static final float WEIGHT_PERCENT_FOR_NAMESPACE = 0.125f;

  public static Pair<ComponentIdentifier, JsonNode> bestMatch(
      final ThirdPartyFileCoordinate sbomComponent,
      final Collection<Pair<ComponentIdentifier, JsonNode>> identityResults,
      final SbomResultsMatcherTelemetry telemetry)
  {
    Pair<Pair<ComponentIdentifier, JsonNode>, Float> bestMatch = Pair.of(null, -0.1f);
    PackageURL sbomPurl = getPurlFromSbomComponent(sbomComponent);
    String sbomHash = sbomComponent.getHash();
    String sbomName = sbomComponent.getName();
    String sbomVersion = sbomComponent.getVersion();
    String sbomFormat = sbomComponent.getFormat();

    for (Pair<ComponentIdentifier, JsonNode> identityResult : identityResults) {
      float matchScore = 0.0f;
      ComponentIdentifier resultId = identityResult.getKey();
      JsonNode resultNode = identityResult.getValue();
      PackageURL resultPurl = getNodePackageUrlSafely(resultNode);
      // perform purl match
      float purlScore = calculateMatchScoreFromPurls(sbomPurl, resultPurl, resultId);
      matchScore += purlScore;

      // perform hash match
      String nodeIdentificationSource = getNodeFieldSafely(resultNode, "identificationSource");
      float hashScore = calculateMatchScoreFromHash(sbomHash, nodeIdentificationSource, resultNode);
      matchScore += hashScore;

      // perform format name and version match
      PackageURL resultComponentId =
          constructPackageURLSafely(PackageUrlIdentifier.toPackageUrl(resultId), "result component identifier");
      float coordScore = calcualteMatchScoreFromCoords(sbomFormat, sbomName, sbomVersion, resultComponentId);
      matchScore += coordScore;

      // replace best match if this has a better score
      if (matchScore > bestMatch.getValue()) {
        if (bestMatch.getKey() != null) {
          log.debug("replacing {} having score {} with {} having {}", bestMatch.getKey().getKey(), bestMatch.getValue(),
              identityResult.getKey(), matchScore);
        }
        bestMatch = Pair.of(identityResult, matchScore);
        telemetry.addMatchStat(resultId, purlScore, hashScore, coordScore, true);
      }
      else {
        telemetry.addMatchStat(resultId, purlScore, hashScore, coordScore, false);
      }
    }

    log.debug("best match result {} with score {}", bestMatch.getKey().getKey(), bestMatch.getValue());
    return bestMatch.getKey();
  }

  private static float calcualteMatchScoreFromCoords(
      final String sbomFormat,
      final String sbomName,
      final String sbomVersion,
      final PackageURL resultComponentId)
  {
    if (ObjectUtils.allNotNull(sbomFormat, sbomName, sbomVersion, resultComponentId)) {
      if (sbomFormat.equals(resultComponentId.getType()) &&
          sbomName.equals(resultComponentId.getName()) &&
          sbomVersion.equals(resultComponentId.getVersion()))
      {
        return WEIGHT_FORMAT_NAME_VERSION;
      }
    }
    return 0.0f;
  }

  private static float calculateMatchScoreFromHash(
      final String sbomHash,
      final String nodeIdentificationSource,
      final JsonNode resultNode)
  {
    float hashScore = 0f;
    if (sbomHash != null) {
      // hash match only makes sense if it is a real hash (Sonatype identified component)
      if (StringUtils.equals(IdentificationSource.SONATYPE.getId(), nodeIdentificationSource) &&
          StringUtils.equals(getNodeFieldSafely(resultNode, "hash"), sbomHash))
      {
        hashScore += WEIGHT_HASH;
      }
    }
    return hashScore;
  }

  private static float calculateMatchScoreFromPurls(
      final PackageURL sbomPurl,
      PackageURL resultPurl,
      final ComponentIdentifier resultId)
  {
    float purlScore = 0.0f;
    if (sbomPurl != null) {
      if (resultPurl == null) {
        resultPurl = tryGettingPurlFromComponentIdentifier(resultId);
      }
      if (resultPurl != null) {
        if (sbomPurl.equals(resultPurl)) {
          purlScore += WEIGHT_PURL;
        }
        else {
          purlScore += calculateMatchScoreFromPurls(sbomPurl, resultPurl);
        }
      }
    }
    return purlScore;
  }

  private static float calculateMatchScoreFromPurls(PackageURL sbomPurl, PackageURL resultPurl) {
    float score = 0.0f;
    // purl comparison is meaningful only if both base coordinates (type, name, version) exists and are equal
    if (ObjectUtils.allNotNull(
        sbomPurl.getType(), resultPurl.getType(),
        sbomPurl.getName(), resultPurl.getName(),
        sbomPurl.getVersion(), resultPurl.getVersion()))
    {
      if (sbomPurl.getType().equals(resultPurl.getType()) &&
          sbomPurl.getName().equals(resultPurl.getName()) &&
          sbomPurl.getVersion().equals(resultPurl.getVersion()))
      {
        // matching these 3 coords should have the maximum weight (equal to format, name, version).
        // There shouldn't be a chance for any other coordinates to override/influence the match score of these coords
        score += WEIGHT_PERCENT_FOR_PURL_MANDATORY_COORDS * WEIGHT_PURL;
      }
      if (ObjectUtils.allNotNull(sbomPurl.getNamespace(), resultPurl.getNamespace())) {
        if (sbomPurl.getNamespace().equals(resultPurl.getNamespace())) {
          score += WEIGHT_PERCENT_FOR_NAMESPACE * WEIGHT_PURL;
        }
      }
      Map<String, String> sbomPurlQs = sbomPurl.getQualifiers();
      Map<String, String> resultPurlQs = resultPurl.getQualifiers();
      if (MapUtils.isNotEmpty(sbomPurlQs) && MapUtils.isNotEmpty(resultPurlQs)) {
        // calculate the weight for each qualifier match. The total of all purl parts match should always be
        // less than or equal to WEIGHT_PURL (equal can only happen if all parts of the sbom purl matches
        // fully with result purls)
        float weightPerQ = (WEIGHT_PURL -
            ((WEIGHT_PERCENT_FOR_PURL_MANDATORY_COORDS * WEIGHT_PURL) + (WEIGHT_PERCENT_FOR_NAMESPACE * WEIGHT_PURL)))
            / sbomPurlQs.size();
        for (Entry<String, String> entry : sbomPurlQs.entrySet()) {
          if (resultPurlQs.containsKey(entry.getKey()) &&
              StringUtils.equals(resultPurlQs.get(entry.getKey()), entry.getValue()))
          {
            score += weightPerQ;
          }
        }
      }
    }
    return score;
  }

  private static PackageURL getPurlFromSbomComponent(final ThirdPartyFileCoordinate sbomComponent) {
    if (StringUtils.isNotEmpty(sbomComponent.getPackageUrl())) {
      return constructPackageURLSafely(sbomComponent.getPackageUrl(), "sbom component");
    }
    return null;
  }

  private static PackageURL tryGettingPurlFromComponentIdentifier(final ComponentIdentifier resultId) {
    PackageUrlIdentifier purl = PackageUrlIdentifier.fromComponentIdentifier(resultId);
    return purl == null ? null : constructPackageURLSafely(purl.getPackageUrl(), "component identifier");
  }

  private static PackageURL getNodePackageUrlSafely(final JsonNode resultNode) {
    if (resultNode.get("packageUrl") != null) {
      String purlFromHds = resultNode.get("packageUrl").asText();
      return constructPackageURLSafely(purlFromHds, "sonatype data services");
    }
    return null;
  }

  private static String getNodeFieldSafely(final JsonNode resultNode, String field) {
    if (resultNode.get(field) != null) {
      return resultNode.get(field).asText();
    }
    return null;
  }

  private static PackageURL constructPackageURLSafely(final String purlString, final String errorContext) {
    try {
      return new PackageURL(purlString);
    }
    catch (MalformedPackageURLException e) {
      log.debug("packageUrl constructed from {} is not valid {}", errorContext, purlString);
    }
    return null;
  }
}
