/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.license;

import java.util.Set;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toSet;

public class LicenseSummary
{
  public String productEdition;

  public Set<String> products;

  public LicenseSummary() {
  }

  public LicenseSummary(String productEdition, String[] products) {
    this.productEdition = productEdition;
    this.products = Stream.of(products).collect(toSet());
  }

  public LicenseSummary(final String productEdition, final Set<String> products) {
    this.productEdition = productEdition;
    this.products = products;
  }
}
