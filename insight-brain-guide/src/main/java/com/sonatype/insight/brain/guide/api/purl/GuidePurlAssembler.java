/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.api.purl;

import java.util.Map;
import java.util.TreeMap;

import com.github.packageurl.MalformedPackageURLException;
import com.github.packageurl.PackageURL;
import com.sonatype.insight.brain.guide.api.dto.GuideAffectedComponentVersion;
import com.sonatype.insight.brain.guide.api.dto.GuideComponentDetailDocument;
import com.sonatype.insight.brain.guide.api.dto.GuideComponentDocument;
import com.sonatype.insight.brain.guide.api.error.GuideApiException;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import jakarta.ws.rs.core.Response;

/**
 * Builds a canonical PURL string from individual coordinate values. Used wherever Guide
 * self-hosted code needs to assemble a PURL from {@code (format, namespace, name, version)}
 * — the by-coords branch of every endpoint that accepts either {@code purl} or
 * {@code format/namespace/name/version}, and the Guide-DTO-to-PURL conversion needed by the
 * policy-enrichment path (the {@code purlFor(...)} overloads, which extract the coordinates from
 * a {@link GuideComponentDocument}, {@link GuideComponentDetailDocument}, or {@link
 * GuideAffectedComponentVersion} and produce the policy-eval PURL for it).
 *
 * <p>
 * Requires {@code format}, {@code name}, AND {@code version} to all be present.
 * {@code namespace} is optional (some ecosystems like npm and pypi don't have one).
 * Guide SaaS's Spring routing rejects the request at dispatch time when any of these three
 * are missing (the {@code @RequestMapping params="format,name,version"} predicate doesn't
 * match), so IQ self-hosted enforces the same contract here. Without this validation, IQ
 * would silently build a versionless PURL like {@code pkg:maven/log4j-core} and forward it
 * to the upstream search-server, which would either return spurious results or error out
 * as a 500 — both contract violations vs SaaS.
 *
 * <p>
 * Delegates assembly to {@link PackageURL}'s typed constructor, mirroring SaaS's
 * {@code EntityIdExtractor.buildPurlFromCoordinates}. The constructor URL-encodes each
 * coordinate component automatically, so values containing characters like {@code /},
 * {@code @}, or {@code %} are encoded rather than mis-interpreted as PURL syntax — e.g.
 * {@code name=@types/node} becomes {@code pkg:npm/%40types%2Fnode@25.9.2} (a single name
 * with encoded scope) instead of {@code pkg:npm/@types/node@25.9.2} (which the parser
 * would treat as namespace=@types, name=node).
 *
 * <p>
 * This builder intentionally takes only the four base coordinates. SaaS's parallel
 * {@code seaworthy-common} helper {@code PurlArtifactQualifiers.buildPackageUrlFromCoordinates}
 * additionally accepts {@code extension}/{@code classifier} for the artifact-filtering query
 * params on the SaaS coordinate-based component endpoints (per GUIDE-2618). Self-hosted does
 * not yet accept those params; if/when that feature is backported, add a 6-arg overload here
 * that mirrors the seaworthy helper's {@code extension} → {@code type} qualifier mapping and
 * blank-collapsing rule so the two stay in sync.
 */
public final class GuidePurlAssembler
{
  // PURL qualifier key whose value becomes the pypi ComponentIdentifier "extension" coordinate
  // (unlike maven, which maps the PURL "type" qualifier to extension via PURL_MAVEN_EXTENSION).
  private static final String PYPI_EXTENSION_QUALIFIER = "extension";

  private GuidePurlAssembler() {
  }

  public static String buildPurl(String format, String namespace, String name, String version) {
    return build(format, namespace, name, version, null);
  }

  /**
   * Like {@link #buildPurl(String, String, String, String)} but fills in the format-specific
   * qualifier defaults required by {@code ComponentIdentifier.ensureComplete()} so the
   * resulting PURL can be passed straight into IQ's policy-evaluation chain
   * ({@code ApiComponentDetailsServiceV2}, etc.) without throwing
   * {@code InvalidComponentIdentifierException}.
   *
   * <p>
   * Use this for PURLs being built to feed the policy evaluator. Use the bare
   * {@link #buildPurl(String, String, String, String)} for PURLs being sent upstream to
   * search-server, where omitted qualifiers mean "no filter" rather than "primary artifact".
   *
   * <p>
   * Defaults applied (mirrors {@code ComponentIdentifierHelper.parseMavenIdNotNull}'s
   * fallback for {@code g:a:v} strings, and {@code SbomResultHandler}'s inline default):
   * <ul>
   * <li>{@code maven} → {@code type=jar} (the canonical primary artifact)
   * </ul>
   * Other formats already pass {@code ensureComplete()} with just {@code (format, namespace,
   * name, version)} from the search response, so they're built unchanged. Add cases here as
   * new formats surface that require additional qualifiers ({@code rpm/architecture},
   * {@code terraform/plan}, etc.).
   */
  public static String buildPurlForPolicyEval(String format, String namespace, String name, String version) {
    return buildPurlForPolicyEval(format, namespace, name, version, null);
  }

  /**
   * Variant for callers that already have qualifiers in hand (e.g. recommendations preserves
   * the parent PURL's qualifiers when generating per-version candidates). Existing qualifiers
   * are preserved; format defaults are filled only where the existing map is missing them.
   */
  public static String buildPurlForPolicyEval(
      String format,
      String namespace,
      String name,
      String version,
      Map<String, String> existingQualifiers)
  {
    return build(format, namespace, name, version, qualifiersWithFormatDefaults(format, existingQualifiers));
  }

  /**
   * Null-tolerant variant of {@link #buildPurlForPolicyEval(String, String, String, String)} for
   * callers enriching upstream search/detail responses, where an individual row may carry incomplete
   * coordinates (a null {@code format}, {@code name}, or {@code version}) that should be skipped
   * rather than fail the whole response. Returns {@code null} — instead of throwing {@link
   * GuideApiException} — whenever the coordinates are missing or otherwise can't form a valid PURL.
   * {@code namespace} remains optional.
   */
  public static String buildPurlForPolicyEvalOrNull(
      String format,
      String namespace,
      String name,
      String version)
  {
    if (format == null || name == null || version == null) {
      return null;
    }
    try {
      return buildPurlForPolicyEval(format, namespace, name, version);
    }
    catch (GuideApiException e) {
      return null;
    }
  }

  /**
   * The policy-eval PURL for a Guide component search hit, or {@code null} if its coordinates are
   * incomplete. See {@link #buildPurlForPolicyEvalOrNull}.
   */
  public static String purlFor(GuideComponentDocument doc) {
    return buildPurlForPolicyEvalOrNull(doc.format(), doc.namespace(), doc.name(), doc.version());
  }

  /**
   * The policy-eval PURL for a Guide component detail document, or {@code null} if its coordinates
   * are incomplete. See {@link #buildPurlForPolicyEvalOrNull}.
   */
  public static String purlFor(GuideComponentDetailDocument doc) {
    return buildPurlForPolicyEvalOrNull(doc.format(), doc.namespace(), doc.name(), doc.version());
  }

  /**
   * The policy-eval PURL for a vulnerability-affected component version, or {@code null} if its
   * coordinates are incomplete. This shape names the format {@code ecosystem} and the artifact
   * {@code packageName}. See {@link #buildPurlForPolicyEvalOrNull}.
   */
  public static String purlFor(GuideAffectedComponentVersion v) {
    return buildPurlForPolicyEvalOrNull(v.ecosystem(), v.namespace(), v.packageName(), v.version());
  }

  private static TreeMap<String, String> qualifiersWithFormatDefaults(
      String format,
      Map<String, String> existing)
  {
    TreeMap<String, String> result = existing != null ? new TreeMap<>(existing) : new TreeMap<>();
    // Maven's PURL "type" qualifier is the ComponentIdentifier "extension" coordinate, which
    // ensureComplete() requires but does not default; search results omit it, so default to the
    // primary "jar" artifact. PackageUrlIdentifier.PURL_MAVEN_EXTENSION keeps this key in sync with
    // HDS's extension<->type mapping rather than hardcoding the literal.
    if ("maven".equalsIgnoreCase(format) && !result.containsKey(PackageUrlIdentifier.PURL_MAVEN_EXTENSION)) {
      result.put(PackageUrlIdentifier.PURL_MAVEN_EXTENSION, "jar");
    }
    // PyPI: HDS keys component intelligence (vulnerabilities, malware advisories, license threat
    // groups) to the source distribution, whose ComponentIdentifier "extension" coordinate is
    // "tar.gz". A bare pkg:pypi/name@version produces extension="", which HDS resolves to NO facts —
    // so the component falsely evaluates as policy-compliant (e.g. a malware-flagged package reads as
    // PASS). Default the "extension" qualifier to the sdist so policy eval resolves the same component
    // the scanner does. Verified against HDS: have/pyyaml/jinja2 return their advisories only at
    // extension=tar.gz. (npm/gem/nuget/golang identify at the package level and need no default.)
    if ("pypi".equalsIgnoreCase(format) && !result.containsKey(PYPI_EXTENSION_QUALIFIER)) {
      result.put(PYPI_EXTENSION_QUALIFIER, "tar.gz");
    }
    return result.isEmpty() ? null : result;
  }

  private static String build(
      String format,
      String namespace,
      String name,
      String version,
      TreeMap<String, String> qualifiers)
  {
    if (format == null || format.isBlank()
        || name == null || name.isBlank()
        || version == null || version.isBlank())
    {
      throw new GuideApiException(Response.Status.BAD_REQUEST,
          "Either 'purl' or all of 'format', 'name', 'version' are required");
    }
    String namespaceOrNull = (namespace == null || namespace.isEmpty()) ? null : namespace;
    try {
      return new PackageURL(format, namespaceOrNull, name, version, qualifiers, null).canonicalize();
    }
    catch (MalformedPackageURLException e) {
      throw new GuideApiException(Response.Status.BAD_REQUEST, "Invalid PURL coordinates: " + e.getMessage());
    }
  }
}
