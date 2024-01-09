/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.sisu.space.ClassFinder;
import org.eclipse.sisu.space.ClassSpace;
import org.eclipse.sisu.space.DefaultClassFinder;

import com.google.common.collect.Iterators;
import com.google.common.collect.Streams;

/**
 * Much like Sisu's DefaultClassFinder (to which it delegates) but with the ability to handle multiple
 * glob paths rather than just one.
 */
public class MultiPackageClassFinder
    implements ClassFinder
{
  public final List<ClassFinder> finders;

  public MultiPackageClassFinder(String ...globs) {
    finders = Arrays.stream(globs)
      .map(DefaultClassFinder::new)
      .collect(Collectors.toList());
  }

  @Override
  public Enumeration<URL> findClasses(ClassSpace space) {
    return Iterators.asEnumeration(
        finders.stream()
            .flatMap(finder -> Streams.stream(Iterators.forEnumeration(finder.findClasses(space))))
            .iterator()
    );
  }
}
