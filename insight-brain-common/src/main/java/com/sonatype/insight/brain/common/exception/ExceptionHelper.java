/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.common.exception;

import java.util.ArrayList;
import java.util.List;

public class ExceptionHelper
{
  public static boolean hasCauseOrSuppressedOfType(Throwable throwable, Class<? extends Throwable> causeType) {
    return hasCauseOrSuppressedOfType(throwable, causeType, new ArrayList<>());
  }

  private static boolean hasCauseOrSuppressedOfType(
      Throwable throwable,
      Class<? extends Throwable> causeType,
      List<Throwable> alreadySeenThrowables)
  {
    while (throwable != null && !alreadySeenThrowables.contains(throwable)) {
      if (causeType.isInstance(throwable)) {
        return true;
      }
      alreadySeenThrowables.add(throwable);

      for (Throwable suppressed : throwable.getSuppressed()) {
        if (hasCauseOrSuppressedOfType(suppressed, causeType, alreadySeenThrowables)) {
          return true;
        }
      }

      throwable = throwable.getCause();
    }

    return false;
  }
}
