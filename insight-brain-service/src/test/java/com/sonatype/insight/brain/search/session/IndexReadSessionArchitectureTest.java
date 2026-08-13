/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.session;

import com.sonatype.insight.brain.search.lucene.LuceneIndexReadSession;
import com.sonatype.insight.brain.search.lucene.LuceneSearchIndexClient;
import com.sonatype.insight.brain.search.lucene.LuceneSearcherManagerHolder;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.junit.jupiter.api.Test;

public class IndexReadSessionArchitectureTest
{
  @Test
  public void directoryReaderOpen_staysInSharedLuceneReaderOwners() {
    // Tests are excluded by location rather than enumerated: they own in-memory readers and throwaway
    // indexes, and are not read paths. Enumerating them made the rule fire on whichever test classes
    // happened to be on a shard's classpath.
    JavaClasses classes = new ClassFileImporter()
        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
        .importPackages("com.sonatype.insight.brain");

    ArchRule rule = ArchRuleDefinition.noClasses()
        .that()
        .resideInAPackage("com.sonatype.insight.brain..")
        .and()
        .areNotAssignableTo(LuceneIndexReadSession.class)
        .and()
        .areNotAssignableTo(LuceneSearcherManagerHolder.class)
        .and()
        // Legacy old-read-path owner. searchIndex/count use SearcherManager when available, but fallback,
        // global, aggregation, and distinct paths still own direct readers until the remaining cutovers.
        .areNotAssignableTo(LuceneSearchIndexClient.class)
        .should()
        .callMethod(DirectoryReader.class, "open", Directory.class)
        .because("new read paths must acquire through the shared session/searcher-manager lifecycle");

    rule.check(classes);
  }

  @Test
  public void luceneLegacyReads_acquireAndReleaseSharedSearcherWhenAvailable() {
    JavaClasses classes = new ClassFileImporter().importPackages("com.sonatype.insight.brain.search.lucene");

    ArchRule acquireRule = ArchRuleDefinition.classes()
        .that()
        .areAssignableTo(LuceneSearchIndexClient.class)
        .should()
        .callMethod(LuceneSearcherManagerHolder.class, "acquire")
        .because("legacy Lucene reads should use the shared SearcherManager when it is available");
    ArchRule releaseRule = ArchRuleDefinition.classes()
        .that()
        .areAssignableTo(LuceneSearchIndexClient.class)
        .should()
        .callMethod(LuceneSearcherManagerHolder.class, "release", IndexSearcher.class)
        .because("Searchers acquired from the shared SearcherManager must be released");

    acquireRule.check(classes);
    releaseRule.check(classes);
  }
}
