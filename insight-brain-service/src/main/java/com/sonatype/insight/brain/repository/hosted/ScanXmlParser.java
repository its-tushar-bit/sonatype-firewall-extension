/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.hosted;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.GZIPInputStream;

import com.sonatype.insight.brain.scan.datastore.ScanEntity;

import org.codehaus.plexus.util.xml.XmlStreamReader;
import org.codehaus.plexus.util.xml.pull.MXParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parses a hosted scan.xml.gz file to extract component identities.
 * <p>
 * A hosted scan file contains one {@code
 *
<dir>
 * } element per archive the insight-scanner library
 * recognised in the input. For a single {@code .jar} upload that is exactly one element. For an
 * archive-of-archives upload (a {@code .zip} containing multiple {@code .jar} files, for example)
 * the scanner walks each inner archive and emits one {@code
 *
<dir>
 * } per inner artifact. Each
 * {@code
 *
<dir>
 * }'s {@code sha1} attribute is already truncated to 20 chars (matching the
 * {@code proxy_repository_component.hash} column size). Format comes from the
 * {@code <repository format="..."/>} element.
 * <p>
 * For nested archives, the parser preserves the outer/inner relationship in the synthesised
 * {@code pathname} using the {@code !/} separator convention ({@code outer.zip!/inner.jar}) so the
 * IQ-side {@code (repository_id, pathname)} UNIQUE constraint admits each inner artifact as a
 * distinct {@code proxy_repository_component} row.
 */
public class ScanXmlParser
{
  private static final Logger log = LoggerFactory.getLogger(ScanXmlParser.class);

  private ScanXmlParser() {
  }

  /**
   * Extracts every component identity present in a scan.xml.gz entity. The first {@code
   *
  <dir>
   * }
   * element is treated as the outer artifact (its {@code path} attribute is taken verbatim as the
   * pathname). Subsequent {@code
   *
  <dir>
   * } elements represent inner archives discovered by the
   * scanner; their pathname is synthesised as {@code outer.path + "!/" + inner.path-tail} so the
   * IQ-side persistence layer can store each as its own {@code proxy_repository_component} row.
   *
   * @param scanEntity the scan entity to parse
   * @return one {@code ScanComponentInfo} per {@code <dir>} element. Empty list if parsing fails or
   *         no {@code <dir>} is present. The first element is always the outer artifact (when at
   *         least one is found), preserving backward compatibility with single-component scans.
   */
  public static List<ScanComponentInfo> extractComponentInfos(final ScanEntity scanEntity) {
    // Try gzip first; if the entity is plain (uncompressed) XML, GZIPInputStream throws
    // ZipException and we re-open the stream. Both the gzip and the plain branch wrap the raw
    // stream in try-with-resources so a parse failure or a non-ZipException IOException
    // mid-construction can never leak the underlying handle. The "raw is closed automatically
    // even when GZIPInputStream's constructor throws" guarantee comes from the outer
    // try-with-resources — Java closes the resource at the end of the block regardless of
    // whether nested constructors completed.
    try (InputStream raw = scanEntity.getInputStream()) {
      try (InputStream gzipped = new GZIPInputStream(raw)) {
        return parse(gzipped);
      }
      catch (java.util.zip.ZipException e) {
        // Not gzipped — fall through to the plain-XML branch below. `raw` has already been
        // partly consumed by GZIPInputStream looking at the magic bytes, so we can't reuse it;
        // re-open the entity for the plain attempt.
      }
    }
    catch (IOException | XmlPullParserException e) {
      log.warn("Failed to parse scan.xml.gz for component info: {}", e.getMessage(), e);
      return Collections.emptyList();
    }
    try (InputStream plain = scanEntity.getInputStream()) {
      return parse(plain);
    }
    catch (IOException | XmlPullParserException e) {
      log.warn("Failed to parse plain scan.xml for component info: {}", e.getMessage(), e);
      return Collections.emptyList();
    }
  }

  private static List<ScanComponentInfo> parse(
      final java.io.InputStream xmlStream) throws IOException, XmlPullParserException
  {
    try (XmlStreamReader reader = new XmlStreamReader(xmlStream)) {
      XmlPullParser parser = new MXParser();
      // Defense-in-depth XXE hardening: disable DOCTYPE / external entity resolution before
      // feeding any input. The scanner library is the only producer of these scan.xml.gz files
      // today (so the practical XXE risk is zero), but turning processing off costs nothing and
      // protects against future code paths that might feed user-controllable XML through the
      // same parser.
      parser.setFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL, false);
      parser.setInput(reader);

      String format = null;
      List<ScanComponentInfo> infos = new ArrayList<>();
      String outerPathname = null;

      parser.next();
      while (parser.getEventType() != XmlPullParser.END_DOCUMENT) {
        if (parser.getEventType() == XmlPullParser.START_TAG) {
          String tagName = parser.getName();

          if ("repository".equals(tagName)) {
            format = parser.getAttributeValue(null, "format");
          }
          else if ("dir".equals(tagName)) {
            String dirPath = parser.getAttributeValue(null, "path");
            String dirHash = parser.getAttributeValue(null, "sha1");
            if (dirPath == null || dirHash == null) {
              // Skip malformed <dir> rather than aborting — the scanner can occasionally emit a
              // <dir> for an unreadable nested archive, and treating that as fatal would lose the
              // valid siblings. Log so it stays visible.
              log.warn("Skipping <dir> with missing path or sha1: path={}, sha1={}", dirPath, dirHash);
              parser.next();
              continue;
            }
            String pathname;
            // Scanner contract (insight-rm-common): the FIRST valid <dir> in scan.xml.gz is the
            // outermost artifact (the thing the user uploaded); every subsequent <dir> represents
            // a nested archive the scanner walked into, in the order it was encountered. We rely
            // on this ordering — out-of-order emission would make a nested file the synthetic
            // "outer". The contract has held since the scanner was introduced; if it ever
            // changes we'll need to either pick the longest path as outer, match against the
            // configured asset path, or have the scanner tag the outer entry explicitly.
            if (outerPathname == null) {
              outerPathname = dirPath;
              pathname = dirPath;
            }
            else {
              // Inner archive: synthesise outer!/inner so the IQ-side UNIQUE(repository_id, pathname)
              // constraint admits each inner artifact as a distinct row. The "!/" separator follows
              // Java's URL convention for jar-in-archive references and never collides with real
              // Maven paths (which contain neither '!' nor "!/").
              pathname = outerPathname + "!/" + stripLeading(dirPath, outerPathname);
            }
            infos.add(new ScanComponentInfo(pathname, dirHash, format));
          }
        }
        parser.next();
      }

      if (infos.isEmpty()) {
        log.warn("Could not extract component info from scan file: no <dir> elements with path+sha1 found");
      }
      return infos;
    }
  }

  /**
   * If the inner {@code
   *
  <dir>
   * } reports its path as {@code outer/...} (the scanner sometimes
   * prefixes nested entries with the outer pathname), trim that prefix so the final synthesised
   * pathname is {@code outer + "!/" + relativeInner} rather than {@code outer + "!/" + outer/inner}.
   * Also handles the {@code outer!/...} form defensively in case the scanner ever switches to
   * the URL-style separator already used downstream — without this guard the synthesis would
   * double-nest as {@code outer!/outer!/inner}. If no prefix overlap is detected, the inner
   * path is returned as-is.
   */
  private static String stripLeading(final String inner, final String outerPath) {
    if (inner == null || outerPath == null) {
      return inner;
    }
    if (inner.startsWith(outerPath + "!/")) {
      return inner.substring(outerPath.length() + 2);
    }
    if (inner.startsWith(outerPath + "/")) {
      return inner.substring(outerPath.length() + 1);
    }
    return inner;
  }

  /**
   * Backward-compatible single-result helper. Returns the first (outermost) component found, or
   * {@code null} if the scan contained no usable {@code
   *
  <dir>
   * } elements. Prefer
   * {@link #extractComponentInfos} for hosted-repo evaluation paths that need to enumerate inner
   * artifacts.
   *
   * @deprecated Use {@link #extractComponentInfos} so archive-of-archives uploads are evaluated
   *             per inner artifact instead of being collapsed to the outer container.
   */
  @Deprecated
  public static ScanComponentInfo extractComponentInfo(final ScanEntity scanEntity) {
    List<ScanComponentInfo> infos = extractComponentInfos(scanEntity);
    return infos.isEmpty() ? null : infos.get(0);
  }
}
