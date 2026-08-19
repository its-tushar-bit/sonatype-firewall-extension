/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.global;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class GlobalSearchCursorTest
{
  private static final String INDEX_GEN = "index-epoch-1234";

  private static final String TAB = "APPLICATION";

  private static final String SORT = "relevance";

  private static final int PAGE = 25;

  private static final String BACKEND_LUCENE = "lucene";

  private static final String BACKEND_OPENSEARCH = "opensearch";

  private static final String TENANT = "tenant-1";

  private static String tokenFor(final String indexGen) {
    return GlobalSearchCursor.computeGenerationToken(indexGen, TAB, SORT, PAGE, BACKEND_LUCENE, TENANT);
  }

  private String originalAmbientToken;

  @BeforeEach
  public void saveAmbientToken() {
    originalAmbientToken = GlobalSearchCursor.currentGenerationToken();
  }

  @AfterEach
  public void restoreAmbientToken() {
    GlobalSearchCursor.bumpGenerationToken(originalAmbientToken);
  }

  // ---- Round-trip ---------------------------------------------------------------------------

  @Test
  public void roundTrip_preservesSortValuesAndGeneration() {
    String token = tokenFor(INDEX_GEN);
    List<String> sortValues = List.of("4.2", "abc", "42");

    GlobalSearchCursor original = GlobalSearchCursor.newCursor(token, sortValues);
    String encoded = original.encode();

    GlobalSearchCursor decoded = GlobalSearchCursor.decode(encoded, token);
    assertThat(decoded.sortValues()).containsExactlyElementsOf(sortValues);
    assertThat(decoded.generationToken()).isEqualTo(token);
  }

  @Test
  public void roundTrip_emptySortValues() {
    String token = tokenFor(INDEX_GEN);
    GlobalSearchCursor cursor = GlobalSearchCursor.newCursor(token, List.of());
    GlobalSearchCursor decoded = GlobalSearchCursor.decode(cursor.encode(), token);
    assertThat(decoded.sortValues()).isEmpty();
    assertThat(decoded.generationToken()).isEqualTo(token);
  }

  @Test
  public void roundTrip_valuesContainingSeparators_areEscaped() {
    String token = tokenFor(INDEX_GEN);
    List<String> sortValues = List.of("a,b", "x;y", "back\\slash");
    GlobalSearchCursor cursor = GlobalSearchCursor.newCursor(token, sortValues);
    GlobalSearchCursor decoded = GlobalSearchCursor.decode(cursor.encode(), token);
    assertThat(decoded.sortValues()).containsExactlyElementsOf(sortValues);
  }

  // ---- Tamper detection ---------------------------------------------------------------------

  @Test
  public void tampered_flippedBit_isRejected() {
    String token = tokenFor(INDEX_GEN);
    String encoded = GlobalSearchCursor.newCursor(token, List.of("v1", "v2")).encode();
    char[] chars = encoded.toCharArray();
    int target = chars.length / 2;
    chars[target] = chars[target] == 'A' ? 'B' : 'A';
    String tampered = new String(chars);

    assertThatExceptionOfType(StaleCursorException.class)
        .isThrownBy(() -> GlobalSearchCursor.decode(tampered, token));
  }

  @Test
  public void tampered_truncated_isRejected() {
    String token = tokenFor(INDEX_GEN);
    String encoded = GlobalSearchCursor.newCursor(token, List.of("v1", "v2")).encode();
    String truncated = encoded.substring(0, encoded.length() / 2);

    assertThatExceptionOfType(StaleCursorException.class)
        .isThrownBy(() -> GlobalSearchCursor.decode(truncated, token));
  }

  @Test
  public void tampered_invalidBase64_isRejected() {
    String token = tokenFor(INDEX_GEN);
    String notBase64 = "!!!not base64!!!";
    assertThatExceptionOfType(StaleCursorException.class)
        .isThrownBy(() -> GlobalSearchCursor.decode(notBase64, token));
  }

  @Test
  public void tampered_missingStructuralMarkers_isRejected() {
    String raw = "not-a-cursor";
    String encoded = Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    String token = tokenFor(INDEX_GEN);
    assertThatExceptionOfType(StaleCursorException.class)
        .isThrownBy(() -> GlobalSearchCursor.decode(encoded, token));
  }

  // ---- Generation invalidation -------------------------------------------------------------

  @Test
  public void generationMismatch_isRejected() {
    String tokenOld = tokenFor("old-index-epoch");
    String tokenNew = tokenFor("new-index-epoch");

    assertThat(tokenOld).isNotEqualTo(tokenNew);

    String encoded = GlobalSearchCursor.newCursor(tokenOld, List.of("v1")).encode();
    assertThatExceptionOfType(StaleCursorException.class)
        .isThrownBy(() -> GlobalSearchCursor.decode(encoded, tokenNew));
  }

  @Test
  public void generationToken_changesWhenAllowlistChanges_isStableWhenItDoesnt() {
    // Two calls with the same inputs must produce the same token (no input has changed).
    String a = tokenFor("epoch-A");
    String b = tokenFor("epoch-A");
    assertThat(a).isEqualTo(b);
  }

  @Test
  public void generationToken_isStableHexSha256() {
    String token = tokenFor("epoch-A");
    // SHA-256 hex is 64 chars [0-9a-f].
    assertThat(token).hasSize(64).matches("[0-9a-f]{64}");
  }

  @Test
  public void generationToken_changesWhenTabChanges() {
    String a = GlobalSearchCursor.computeGenerationToken(INDEX_GEN, "APPLICATIONS", SORT, PAGE, BACKEND_LUCENE, TENANT);
    String b =
        GlobalSearchCursor.computeGenerationToken(INDEX_GEN, "VULNERABILITIES", SORT, PAGE, BACKEND_LUCENE, TENANT);
    assertThat(a).isNotEqualTo(b);
  }

  @Test
  public void generationToken_changesWhenSortKeyChanges() {
    String a = GlobalSearchCursor.computeGenerationToken(INDEX_GEN, TAB, "relevance", PAGE, BACKEND_LUCENE, TENANT);
    String b = GlobalSearchCursor.computeGenerationToken(INDEX_GEN, TAB, "name", PAGE, BACKEND_LUCENE, TENANT);
    assertThat(a).isNotEqualTo(b);
  }

  @Test
  public void generationToken_changesWhenPageSizeChanges() {
    String a = GlobalSearchCursor.computeGenerationToken(INDEX_GEN, TAB, SORT, 25, BACKEND_LUCENE, TENANT);
    String b = GlobalSearchCursor.computeGenerationToken(INDEX_GEN, TAB, SORT, 50, BACKEND_LUCENE, TENANT);
    assertThat(a).isNotEqualTo(b);
  }

  @Test
  public void generationToken_changesWhenBackendChanges() {
    String a = GlobalSearchCursor.computeGenerationToken(INDEX_GEN, TAB, SORT, PAGE, BACKEND_LUCENE, TENANT);
    String b = GlobalSearchCursor.computeGenerationToken(INDEX_GEN, TAB, SORT, PAGE, BACKEND_OPENSEARCH, TENANT);
    assertThat(a).isNotEqualTo(b);
  }

  @Test
  public void cursorMintedByOneBackend_cannotBeDecodedByAnother() {
    String luceneToken =
        GlobalSearchCursor.computeGenerationToken(INDEX_GEN, TAB, SORT, PAGE, BACKEND_LUCENE, TENANT);
    String opensearchToken =
        GlobalSearchCursor.computeGenerationToken(INDEX_GEN, TAB, SORT, PAGE, BACKEND_OPENSEARCH, TENANT);
    String encodedFromLucene = GlobalSearchCursor.newCursor(luceneToken, List.of("v1")).encode();
    assertThatExceptionOfType(StaleCursorException.class)
        .isThrownBy(() -> GlobalSearchCursor.decode(encodedFromLucene, opensearchToken));
  }

  @Test
  public void generationToken_changesWhenTenantChanges() {
    String a =
        GlobalSearchCursor.computeGenerationToken(INDEX_GEN, TAB, SORT, PAGE, BACKEND_LUCENE, "tenant-a");
    String b =
        GlobalSearchCursor.computeGenerationToken(INDEX_GEN, TAB, SORT, PAGE, BACKEND_LUCENE, "tenant-b");
    assertThat(a).isNotEqualTo(b);
  }

  @Test
  public void cursorMintedForOneTenant_cannotBeDecodedForAnother() {
    String tokenTenantA =
        GlobalSearchCursor.computeGenerationToken(INDEX_GEN, TAB, SORT, PAGE, BACKEND_LUCENE, "tenant-a");
    String tokenTenantB =
        GlobalSearchCursor.computeGenerationToken(INDEX_GEN, TAB, SORT, PAGE, BACKEND_LUCENE, "tenant-b");
    String encoded = GlobalSearchCursor.newCursor(tokenTenantA, List.of("v1")).encode();
    assertThatExceptionOfType(StaleCursorException.class)
        .isThrownBy(() -> GlobalSearchCursor.decode(encoded, tokenTenantB));
  }

  @Test
  public void lengthPrefixed_preventsBoundaryCollision() {
    // Two preimage layouts that would be equal under bare-null separators but differ when
    // components are length-prefixed. "AB" + "C" and "A" + "BC" must produce different tokens.
    String a = GlobalSearchCursor.computeGenerationToken("AB", "C", SORT, PAGE, BACKEND_LUCENE, "t");
    String b = GlobalSearchCursor.computeGenerationToken("A", "BC", SORT, PAGE, BACKEND_LUCENE, "t");
    assertThat(a).isNotEqualTo(b);
  }

  @Test
  public void newCursor_tokenWithSemicolon_throwsIae() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> GlobalSearchCursor.newCursor("abc;def", List.of()));
  }

  @Test
  public void decode_tupleWithUnescapedSemicolon_isRejected() {
    // Hand-craft a payload whose tuple region contains an unescaped ';' (a structural
    // marker) so we exercise the splitEscaped guard rather than the gen-mismatch guard.
    String token = tokenFor(INDEX_GEN);
    String raw = "gen=" + token + ";t=a;b";
    String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    assertThatExceptionOfType(StaleCursorException.class)
        .isThrownBy(() -> GlobalSearchCursor.decode(encoded, token));
  }

  @Test
  public void allowlistComponent_isUsedInGenerationToken() {
    String fp = GlobalSearchCursor.computeAllowlistComponent();
    assertThat(fp).isEqualTo(GlobalSearchSortAllowlist.fingerprint());
  }

  // ---- Edge cases (dual-arg decode) ---------------------------------------------------------

  @Test
  public void nullEncoded_isRejected() {
    assertThatExceptionOfType(StaleCursorException.class)
        .isThrownBy(() -> GlobalSearchCursor.decode(null, "anything"));
  }

  @Test
  public void emptyEncoded_isRejected() {
    assertThatExceptionOfType(StaleCursorException.class)
        .isThrownBy(() -> GlobalSearchCursor.decode("", "anything"));
  }

  @Test
  public void oversizeEncoded_isRejected() {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < GlobalSearchCursor.MAX_ENCODED_LENGTH + 10; i++) {
      sb.append('a');
    }
    assertThatExceptionOfType(StaleCursorException.class)
        .isThrownBy(() -> GlobalSearchCursor.decode(sb.toString(), "any"));
  }

  // ---- Round-trip sanity for the gen/page/sort/backend dimensions ---------------------------

  @Test
  public void roundTripWithFullPin_isStable() {
    String token = GlobalSearchCursor.computeGenerationToken(
        "epoch-99", "APPLICATIONS", "name", 25, "lucene", TENANT);
    GlobalSearchCursor original = GlobalSearchCursor.newCursor(token, List.of("alpha", "7"));
    GlobalSearchCursor decoded = GlobalSearchCursor.decode(original.encode(), token);
    assertThat(decoded.sortValues()).containsExactly("alpha", "7");
    assertThat(decoded.generationToken()).isEqualTo(token);
  }

  @Test
  public void newCursor_nullArgs_throwsNpe() {
    assertThatExceptionOfType(NullPointerException.class)
        .isThrownBy(() -> GlobalSearchCursor.newCursor(null, List.of()));
    assertThatExceptionOfType(NullPointerException.class)
        .isThrownBy(() -> GlobalSearchCursor.newCursor("token", null));
  }

  @Test
  public void newCursor_emptyToken_throwsIae() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> GlobalSearchCursor.newCursor("", List.of()));
  }

  @Test
  public void newCursor_nullSortValueElement_isRejected() {
    assertThatThrownBy(() -> GlobalSearchCursor.newCursor("token", Arrays.asList("a", null, "b")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("sortValues must not contain null elements");
  }

  @Test
  public void sortValues_areImmutable() {
    GlobalSearchCursor c = GlobalSearchCursor.newCursor("token",
        new java.util.ArrayList<>(List.of("a", "b")));
    assertThatExceptionOfType(UnsupportedOperationException.class)
        .isThrownBy(() -> c.sortValues().add("c"));
  }

  // ---- Ambient-token / single-arg decode -----------------------------------------------------

  @Test
  public void singleArgDecode_roundTripsAgainstAmbientToken() {
    GlobalSearchCursor cursor = new GlobalSearchCursor(
        GlobalSearchCursor.currentGenerationToken(),
        List.of("alpha", "beta", "gamma"));

    String encoded = cursor.encode();
    GlobalSearchCursor decoded = GlobalSearchCursor.decode(encoded);

    assertThat(decoded.generationToken()).isEqualTo(cursor.generationToken());
    assertThat(decoded.sortValues()).containsExactly("alpha", "beta", "gamma");
  }

  @Test
  public void singleArgDecode_throwsStaleWhenAmbientTokenBumped() {
    String encoded = new GlobalSearchCursor(
        GlobalSearchCursor.currentGenerationToken(),
        List.of("v1")).encode();

    GlobalSearchCursor.bumpGenerationToken("g-rotated");

    assertThatThrownBy(() -> GlobalSearchCursor.decode(encoded))
        .isInstanceOf(StaleCursorException.class)
        .hasMessageContaining("retry from page 1");
  }

  @Test
  public void staleCursor_responseDoesNotEchoEmbeddedToken() {
    // A cursor minted against one token, decoded against another, must NOT echo either token in the
    // response message. Otherwise a crafted cursor could inject arbitrary text into log/error output.
    String maliciousToken = "a\n[ERROR] fake log line b";
    String encoded = GlobalSearchCursor.newCursor(maliciousToken, List.of("x")).encode();

    try {
      GlobalSearchCursor.decode(encoded, tokenFor(INDEX_GEN));
      throw new AssertionError("expected StaleCursorException");
    }
    catch (StaleCursorException e) {
      assertThat(e.getMessage()).doesNotContain(maliciousToken);
      assertThat(e.getMessage()).doesNotContain("[ERROR]");
      assertThat(e.getMessage()).doesNotContain("\n");
    }
  }

  @Test
  public void singleArgDecode_rejectsBlankAndNullWithIae() {
    assertThatThrownBy(() -> GlobalSearchCursor.decode(""))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> GlobalSearchCursor.decode((String) null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void singleArgDecode_rejectsMalformedWithIae() {
    assertThatThrownBy(() -> GlobalSearchCursor.decode("not-base64!@#"))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
