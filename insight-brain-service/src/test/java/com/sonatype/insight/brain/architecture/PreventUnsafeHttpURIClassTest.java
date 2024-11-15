/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import org.eclipse.jetty.http.HttpURI;
import org.junit.Test;

public class PreventUnsafeHttpURIClassTest
{
  // CVE-2024-6763 - Jetty versions below 12.0.12
  @Test
  public void test_NoClassesMakeUseOf_HttpURIMutable() {
    JavaClasses importedClasses = new ClassFileImporter()
        .withImportOption(new DoNotIncludeTests())
        .importPackages("com.sonatype.insight.brain");

    ArchRule rule = ArchRuleDefinition.noClasses()
        .should()
        .dependOnClassesThat()
        .haveFullyQualifiedName(HttpURI.Mutable.class.getName());

    rule.check(importedClasses);
  }
}
