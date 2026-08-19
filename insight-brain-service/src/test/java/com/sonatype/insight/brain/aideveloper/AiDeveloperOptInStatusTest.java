/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.aideveloper;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AiDeveloperOptInStatusTest
{
  @Test
  public void readsTheRecordingUserAndInstant() {
    AiDeveloperOptInStatus status = AiDeveloperOptInStatus.from("alice,2026-08-17T12:00:00Z", false);

    assertThat(status.optedIn()).isTrue();
    assertThat(status.optedInBy()).isEqualTo("alice");
    assertThat(status.optedInAt()).isEqualTo("2026-08-17T12:00:00Z");
    assertThat(status.message()).isNull();
  }

  /**
   * LDAP distinguished names carry commas, and the instant does not, so the last comma separates the two fields.
   */
  @Test
  public void keepsCommasThatBelongToTheUsername() {
    AiDeveloperOptInStatus status = AiDeveloperOptInStatus.from("cn=alice,ou=eng,dc=acme,2026-08-17T12:00:00Z", false);

    assertThat(status.optedInBy()).isEqualTo("cn=alice,ou=eng,dc=acme");
    assertThat(status.optedInAt()).isEqualTo("2026-08-17T12:00:00Z");
  }

  @Test
  public void countsAValueWithoutAnInstantAsOptedIn() {
    AiDeveloperOptInStatus status = AiDeveloperOptInStatus.from("alice", false);

    assertThat(status.optedIn()).isTrue();
    assertThat(status.optedInBy()).isEqualTo("alice");
    assertThat(status.optedInAt()).isNull();
  }

  /**
   * A field the record does not carry reads as absent, never as an empty string a caller might try to parse.
   */
  @Test
  public void reportsFieldsTheRecordDoesNotCarryAsAbsent() {
    assertThat(AiDeveloperOptInStatus.from("alice,", false).optedInAt()).isNull();
    assertThat(AiDeveloperOptInStatus.from("alice, ", false).optedInAt()).isNull();
    assertThat(AiDeveloperOptInStatus.from(",2026-08-17T12:00:00Z", false).optedInBy()).isNull();
  }

  @Test
  public void readsNoValueAsNotOptedIn() {
    for (String value : new String[]{null, "", "  "}) {
      AiDeveloperOptInStatus status = AiDeveloperOptInStatus.from(value, false);

      assertThat(status.optedIn()).as("value '%s'", value).isFalse();
      assertThat(status.optedInBy()).isNull();
      assertThat(status.optedInAt()).isNull();
      assertThat(status.message()).isNull();
    }
  }

  @Test
  public void reportsThatAnExternalDatabaseIsRequiredWhenEmbedded() {
    AiDeveloperOptInStatus status = AiDeveloperOptInStatus.from("alice,2026-08-17T12:00:00Z", true);

    assertThat(status.optedIn()).isTrue();
    assertThat(status.externalDatabaseRequired()).isTrue();
    assertThat(status.message()).isEqualTo(AiDeveloperOptInStatus.EMBEDDED_DATABASE_MESSAGE);
  }
}
