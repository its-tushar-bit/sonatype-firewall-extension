/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.modules;

import com.sonatype.insight.scan.anon.Anonymizer;
import com.sonatype.insight.scan.client.ClientScanner;
import com.sonatype.insight.scan.config.ScanPropertiesLoader;
import com.sonatype.insight.scan.file.FileScanner;
import com.sonatype.insight.scan.hash.Digester;
import com.sonatype.insight.scan.hash.internal.DefaultDigester;
import com.sonatype.insight.scan.hash.internal.JavaDigester;
import com.sonatype.insight.scan.model.io.ScanWriter;
import com.sonatype.insight.scan.model.io.ScanWriterFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Guice module providing explicit bindings for insight scanner components.
 * This replaces Sisu's automatic @Named component discovery.
 * These classes from insight-scanner library use javax.inject annotations,
 * so they need manual binding with @Provides methods.
 */
public class ScannerModule
    extends AbstractModule
{
  @Override
  protected void configure() {
    bind(JavaDigester.class);
    // Bind ScanPropertiesLoader as it's also needed
    bind(ScanPropertiesLoader.class).in(Singleton.class);
  }

  /**
   * Provides Digester instance. This is needed because DefaultDigester uses javax.inject annotations
   * but we've migrated to jakarta.inject, so Guice can't automatically inject its dependencies.
   */
  @Provides
  @Singleton
  public Digester provideDigester(JavaDigester javaDigester) {
    return new DefaultDigester(javaDigester);
  }

  /**
   * Provides ClientScanner instance. This is needed because ClientScanner uses javax.inject annotations
   * from insight-scanner library, but we've migrated to jakarta.inject.
   */
  @Provides
  @Singleton
  public ClientScanner provideClientScanner() {
    return new ClientScanner(LoggerFactory.getLogger(ClientScanner.class));
  }

  /**
   * Provides FileScanner instance. This is needed because FileScanner uses javax.inject annotations
   * from insight-scanner library, but we've migrated to jakarta.inject.
   */
  @Provides
  @Singleton
  public FileScanner provideFileScanner() {
    Logger logger = LoggerFactory.getLogger(FileScanner.class);
    Logger digesterLogger = LoggerFactory.getLogger(DefaultDigester.class);
    return new FileScanner(
        new DefaultDigester(new JavaDigester(), digesterLogger),
        new Anonymizer(),
        logger);
  }

  /**
   * Provides ScanWriterFactory instance. This is needed because ScanWriterFactory uses javax.inject annotations
   * from insight-scanner library, but we've migrated to jakarta.inject.
   */
  @Provides
  @Singleton
  public ScanWriterFactory provideScanWriterFactory() {
    return new ScanWriterFactory(LoggerFactory.getLogger(ScanWriter.class));
  }
}
