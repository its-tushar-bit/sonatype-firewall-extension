/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import org.drools.builder.KnowledgeBuilder;
import org.drools.builder.KnowledgeBuilderFactory;
import org.drools.builder.ResourceType;
import org.drools.io.ResourceFactory;

/**
 * Helper class for quick parse & compile of drools code.
 */
public class DroolsCodeChecker
{
  public static void main(String[] args) {
    KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();
    // this will parse and compile in one step
    kbuilder.add(ResourceFactory.newFileResource("c:/temp/test.drl"), ResourceType.DRL);
    if (kbuilder.hasErrors()) {
      System.err.println(kbuilder.getErrors());
    }
    else {
      System.out.println("Success");
    }
  }
}
