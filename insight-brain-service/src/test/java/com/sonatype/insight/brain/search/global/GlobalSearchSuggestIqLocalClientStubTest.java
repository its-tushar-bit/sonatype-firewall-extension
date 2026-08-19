/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.global;

import java.util.List;

import com.sonatype.insight.brain.model.security.UserPrincipal;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contract test for the empty-result {@link GlobalSearchSuggestIqLocalClientStub}. Extending
 * {@link GlobalSearchSuggestIqLocalClientContractTest} keeps every implementation of the SPI honest
 * against the public-type-mapping guarantee.
 */
public class GlobalSearchSuggestIqLocalClientStubTest
    extends GlobalSearchSuggestIqLocalClientContractTest
{
  @Override
  protected GlobalSearchSuggestIqLocalClient createService() {
    return new GlobalSearchSuggestIqLocalClientStub();
  }

  @Test
  public void suggest_withRealPrincipal_stillReturnsEmpty() {
    GlobalSearchSuggestIqLocalClientStub stub = new GlobalSearchSuggestIqLocalClientStub();
    UserPrincipal principal = Mockito.mock(UserPrincipal.class);

    List<SuggestRow> rows = stub.suggest("alpha", List.of(SuggestItemType.APPLICATION), 3, principal);

    assertThat(rows).isEmpty();
  }

  @Test
  public void suggest_emitsWarnExactlyOnceAcrossManyInvocations() {
    // The stub's one-shot WARN must fire exactly once even under many sequential invocations.
    Logger stubLogger = (Logger) LoggerFactory.getLogger(GlobalSearchSuggestIqLocalClientStub.class);
    Level originalLevel = stubLogger.getLevel();
    stubLogger.setLevel(Level.WARN);
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    stubLogger.addAppender(appender);
    try {
      GlobalSearchSuggestIqLocalClientStub stub = new GlobalSearchSuggestIqLocalClientStub();
      for (int i = 0; i < 25; i++) {
        stub.suggest("alpha-" + i, List.of(SuggestItemType.APPLICATION), 3, null);
      }
      long warnCount = appender.list.stream()
          .filter(e -> e.getLevel() == Level.WARN)
          .count();
      assertThat(warnCount)
          .as("stub WARN should fire exactly once across sequential calls")
          .isEqualTo(1);
    }
    finally {
      stubLogger.detachAppender(appender);
      stubLogger.setLevel(originalLevel);
    }
  }
}
