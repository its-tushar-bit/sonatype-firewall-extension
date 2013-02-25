/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.json.store;

import java.io.IOException;

import com.fasterxml.jackson.databind.node.ContainerNode;

public interface JsonStore
{
    void commit( String path, ContainerNode<?> data )
        throws IOException;

    ContainerNode<?> restore( String path )
        throws IOException;

    Iterable<String> list()
        throws IOException;

    int modificationCount();

    ContainerNode<?> history( ContainerNode<?> key, String... paths )
        throws IOException;

    <T extends ContainerNode<?>> T augment( T key, String... paths )
        throws IOException;
}
