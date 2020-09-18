/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.utils;

import java.io.IOException;

import com.fasterxml.jackson.databind.node.ContainerNode;

public interface JsonStore
{
  void commit(String path, ContainerNode<?> data) throws IOException;

  ContainerNode<?> restore(String path) throws IOException;

  ContainerNode<?> history(ContainerNode<?> key, String... paths) throws IOException;
}
