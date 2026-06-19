/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.hosted;

/**
 * Value object carrying one component identity extracted from a hosted scan.xml.gz.
 * <p>
 * A hosted scan file may contain one {@code
 *
<dir>
 * } element (a single artifact upload — the common
 * case for {@code .jar}/{@code .war}/{@code .aar}/etc.) or many (an archive-of-archives such as a
 * {@code .zip} containing several {@code .jar} files). Each {@code
 *
<dir>
 * } produces one
 * {@code ScanComponentInfo} via {@link ScanXmlParser#extractComponentInfos}.
 * <p>
 * {@code pathname} and {@code hash} come directly from the {@code
 *
<dir>
 * } attributes;
 * {@code format} comes from the {@code <repository format="..."/>} element. For inner archive
 * entries, {@code pathname} encodes the outer/inner relationship using the {@code !/} separator
 * convention ({@code outer.zip!/inner.jar}) so the IQ-side {@code (repository_id, pathname)}
 * UNIQUE constraint accepts each entry as a distinct row.
 */
public record ScanComponentInfo(String pathname, String hash, String format)
{
}
