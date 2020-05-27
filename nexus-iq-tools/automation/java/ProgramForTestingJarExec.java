/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

public class ProgramForTestingJarExec
{
  public static void main(String[] args) {
    if ( args.length != 1 ) {
      throw new RuntimeException("There are not arguments");
    }

    String typeOfOutput = args[0];
    switch (typeOfOutput) {
      case "--continuous":
        continuous();
        break;
      case "--small":
        small();
        break;
      case "--exception":
        throw new RuntimeException("There was a dummy exception");
      case "--delayed-exception":
        delayedException();
        break;
      default:
        throw new RuntimeException("Not implemented for the type of output: " + typeOfOutput);
    }
  }

  private static void continuous() {
    for (int i = 1;; i++) {
      System.out.println("This is the line " + i);
      try {
        Thread.sleep(1);
      }
      catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private static void small() {
    for (int i = 1; i <= 50; i++) {
      System.out.println("This is the line " + i);
      try {
        Thread.sleep(1);
      }
      catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private static void delayedException() {
    for (int i = 1; i <= 20; i++) {
      System.out.println("This is the line " + i);
      try {
        Thread.sleep(1);
      }
      catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
    throw new RuntimeException("Delayed exception");
  }
}
