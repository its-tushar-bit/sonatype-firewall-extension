/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.continuousmonitoring;

import java.util.Date;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class EligibilityCursorTest
{
  @Test
  public void encodeIsEpochMillisColonId() {
    EligibilityCursor cursor = new EligibilityCursor(new Date(1700000000000L), "abc-123");
    assertThat(cursor.encode()).isEqualTo("1700000000000:abc-123");
  }

  @Test
  public void decodeIsInverseOfEncode() {
    EligibilityCursor original = new EligibilityCursor(new Date(1700000000000L), "abc-123");
    EligibilityCursor roundTrip = EligibilityCursor.decode(original.encode());
    assertThat(roundTrip).isEqualTo(original);
  }

  @Test
  public void decodeRejectsMalformed() {
    assertThatThrownBy(() -> EligibilityCursor.decode("nope"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> EligibilityCursor.decode("12345"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void decodeAllowsColonsInComponentId() {
    // Component ids in this system can contain colons (e.g. PURL fragments). The decoder
    // splits on the first colon only so the rest of the string is preserved verbatim.
    EligibilityCursor c = EligibilityCursor.decode("1700000000000:pkg:maven/org.foo:bar:1.0");
    assertThat(c.time().getTime()).isEqualTo(1700000000000L);
    assertThat(c.repositoryComponentId()).isEqualTo("pkg:maven/org.foo:bar:1.0");
  }

  @Test
  public void constructorRejectsEmptyComponentId() {
    // Parity with decode()'s sep == s.length() - 1 check: encode() of (time, "") would produce
    // "<epoch>:" which decode() rejects, so the constructor must reject the empty form too —
    // otherwise the type allows two semantically equivalent representations of "no component id"
    // that differ in their construction path.
    assertThatThrownBy(() -> new EligibilityCursor(new Date(1700000000000L), ""))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void decodeRejectsTrailingColon() {
    // Symmetric to constructorRejectsEmptyComponentId: a "<epoch>:" string is the encoded form of
    // an invalid (epoch, "") cursor. The trailing-colon shape is caught by the
    // sep == s.length() - 1 guard in decode().
    assertThatThrownBy(() -> EligibilityCursor.decode("12345:"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void toStringReturnsEncodedForm() {
    // Records default to a verbose EligibilityCursor[time=..., repositoryComponentId=...] form;
    // the override returns the canonical "epoch:id" shape so logs grep cleanly against the same
    // representation used in producer cycle WARN/ERROR lines (cursor.encode()).
    EligibilityCursor cursor = new EligibilityCursor(new Date(1700000000000L), "abc-123");
    assertThat(cursor.toString()).isEqualTo("1700000000000:abc-123");
  }

  @Test
  public void mutatingConstructorArgumentDoesNotMutateCursor() {
    // Defensive copy on construction: caller's reference to the source Date is decoupled from
    // the cursor's snapshot. Without the copy, time.setTime(...) here would silently shift the
    // cursor's keyset position.
    Date source = new Date(1700000000000L);
    EligibilityCursor cursor = new EligibilityCursor(source, "abc-123");
    source.setTime(0L);
    assertThat(cursor.time().getTime()).isEqualTo(1700000000000L);
  }

  @Test
  public void mutatingAccessorReturnValueDoesNotMutateCursor() {
    // Defensive copy on access: each time() call returns a fresh Date so a caller mutating the
    // returned reference cannot corrupt the cursor — and cannot leak the mutation to other
    // consumers of the same cursor.
    EligibilityCursor cursor = new EligibilityCursor(new Date(1700000000000L), "abc-123");
    cursor.time().setTime(0L);
    assertThat(cursor.time().getTime()).isEqualTo(1700000000000L);
  }
}
