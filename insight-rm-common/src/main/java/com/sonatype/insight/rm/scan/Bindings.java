/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.rm.scan;

import com.sonatype.insight.scan.anon.Anonymizer;
import com.sonatype.insight.scan.client.ClientScanner;
import com.sonatype.insight.scan.file.FileScanner;
import com.sonatype.insight.scan.hash.internal.DefaultDigester;
import com.sonatype.insight.scan.hash.internal.JavaDigester;
import com.sonatype.insight.scan.model.io.ScanWriter;
import com.sonatype.insight.scan.model.io.ScanWriterFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class Bindings
{
  private static Logger logger(Class<?> type) {
    return LoggerFactory.getLogger(type);
  }

  static ScanWriterFactory scanWriterFactory() {
    return new ScanWriterFactory(logger(ScanWriter.class));
  }

  static ClientScanner clientScanner() {
    return new ClientScanner(logger(ClientScanner.class));
  }

  static FileScanner fileScanner() {
    return new FileScanner(new DefaultDigester(new JavaDigester(), logger(DefaultDigester.class)), new Anonymizer(),
        logger(FileScanner.class));
  }
}
