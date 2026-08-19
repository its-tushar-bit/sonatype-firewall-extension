/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;

public class NameSupplierDictionary
    implements Function<String, String>
{
  private final Map<String, Supplier<String>> prefixDictionary;

  public NameSupplierDictionary() {
    prefixDictionary = new HashMap<>();
  }

  @Override
  public String apply(final String prefix) {
    return prefixDictionary.computeIfAbsent(prefix, key -> new NameSupplier(key)).get();
  }

  public void addNewOrganizationNameSupplier(Supplier<String> nameSupplier) {
    prefixDictionary.put("TestOrg_", nameSupplier);
  }

  public void addNewApplicationNameSupplier(Supplier<String> nameSupplier) {
    prefixDictionary.put("TestApp_", nameSupplier);
  }
}

class NameSupplier
    implements Supplier<String>
{
  private final AtomicInteger numOfElements;

  private final String prefix;

  public NameSupplier(String prefix) {
    this.prefix = prefix;
    numOfElements = new AtomicInteger(0);
  }

  @Override
  public String get() {
    return prefix + numOfElements.getAndIncrement();
  }
}
