/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain;
import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.scan.file.PoetryFile;
import com.sonatype.nexus.iq.manifests.go.parser.GoModFileParser;
import com.sonatype.nexus.iq.manifests.npm.NpmManifestUpdateFinder;

import de.schlichtherle.truezip.file.TFile;
import org.antlr.v4.runtime.atn.ATNDeserializer;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * These tests are to help avoid a reoccurrence of CLM-24899
 */
@Category(SlowTest.class)
public class Antlr4RuntimeCompatibilityTest
    extends AbstractComponentTest
{
  @Test
  public void testDependencyMatches() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createNpmCoordinates("marked", "0.3.5");
    assertThat(new NpmManifestUpdateFinder().dependencyMatches("", componentIdentifier, "marked", "^0.3.5")).isTrue();
  }

  @Test
  public void testConvertToModel() throws Exception {
    GoModFileParser goModFileParser = new GoModFileParser();
    URI resource = getClass().getResource("/" + getClass().getSimpleName() + "/go.mod").toURI();
    Path path = Paths.get(resource);
    assertThat(goModFileParser.convertToModel(path)).isNotNull();
  }

  @Test
  public void testFromFileContents() throws Exception {
    URI resource = getClass().getResource("/" + getClass().getSimpleName() + "/poetry.lock").toURI();
    Path path = Paths.get(resource);
    assertThat(PoetryFile.fromFileContents(new TFile(path.toFile()))).isNotNull();
  }

  @Test
  public void testATNDeserializerVersion() {
    assertThat(ATNDeserializer.SERIALIZED_VERSION).isEqualTo(3);
  }
}
