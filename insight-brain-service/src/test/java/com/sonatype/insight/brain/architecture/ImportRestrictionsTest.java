/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import org.junit.Test;

public class ImportRestrictionsTest
{
  @Test
  public void testCodeShouldNotImportJavaxInject() {
    JavaClasses classes = new ClassFileImporter().importPackages("com.sonatype.insight.brain");
    ArchRule rule = ArchRuleDefinition.noClasses()
        .should()
        .dependOnClassesThat()
        .resideInAPackage("javax.inject..")
        .because("we use jakarta.inject instead of javax.inject");

    rule.check(classes);
  }
}
