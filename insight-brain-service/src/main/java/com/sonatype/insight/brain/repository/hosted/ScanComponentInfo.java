/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.hosted;

/**
 * Value object carrying the component identity extracted from a hosted scan.xml.gz.
 * <p>
 * Each hosted scan file contains exactly one artifact (one {@code
 *
<dir>
 * } element).
 * {@code pathname} and {@code hash} come directly from the {@code
 *
<dir>
 * } attributes;
 * {@code format} comes from the {@code <repository format="..."/>} element.
 */
public record ScanComponentInfo(String pathname, String hash, String format)
{
}
