/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.utils;

import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.sbom.utils.SbomValidationMessageTranslator.translate;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure-function tests for {@link SbomValidationMessageTranslator}. Every rewrite
 * must preserve the {@code "Line: L, Column: C, Path: P, Error: "} prefix and only
 * swap the schema-generated suffix. CLM-40052.
 */
public class SbomValidationMessageTranslatorTest
{
  @Test
  public void rewrites_versionRange_requires_isExternal_constraint() {
    String raw = "Line: 16, Column: 6, Path: $.components[0], "
        + "Error: must not be valid to the schema {\"required\":[\"versionRange\"]}";

    assertThat(translate(raw))
        .isEqualTo("Line: 16, Column: 6, Path: $.components[0], "
            + "Error: versionRange is only allowed when isExternal=true. "
            + "Set isExternal: true on this component, or remove versionRange.");
  }

  @Test
  public void rewrites_version_and_versionRange_mutually_exclusive() {
    String raw = "Line: 19, Column: 6, Path: $.components[1], "
        + "Error: must not be valid to the schema {\"required\":[\"version\",\"versionRange\"]}";

    assertThat(translate(raw))
        .isEqualTo("Line: 19, Column: 6, Path: $.components[1], "
            + "Error: version and versionRange are mutually exclusive. Set one, not both.");
  }

  @Test
  public void rewrites_missing_required_property_with_captured_field_name() {
    String raw = "Line: 28, Column: 6, Path: $.components[2], Error: required property 'type' not found";

    assertThat(translate(raw))
        .isEqualTo("Line: 28, Column: 6, Path: $.components[2], Error: Missing required field \"type\".");
  }

  @Test
  public void rewrites_missing_required_property_with_rfc6901_path_prefix() {
    // Production now emits RFC 6901 pointers (e.g. /components/2) rather than $.components[2].
    // The translator is path-agnostic: it only rewrites the text after "Error: ", so the new-format
    // prefix is preserved verbatim and the error text is still rewritten.
    String raw = "Line: 28, Column: 6, Path: /components/2, Error: required property 'type' not found";

    assertThat(translate(raw))
        .isEqualTo("Line: 28, Column: 6, Path: /components/2, Error: Missing required field \"type\".");
  }

  @Test
  public void captured_field_name_with_dollar_sign_is_inserted_literally() {
    // A property name like "$ref" is inserted as the literal value of group 1; replaceFirst does
    // not re-scan the substituted group value for $/\\ replacement sequences, so no back-reference
    // interpretation or IndexOutOfBounds occurs.
    String raw = "Line: 5, Column: 6, Path: $.components[0], Error: required property '$ref' not found";

    assertThat(translate(raw))
        .isEqualTo("Line: 5, Column: 6, Path: $.components[0], Error: Missing required field \"$ref\".");
  }

  @Test
  public void rewrites_undefined_additional_property_with_captured_field_name() {
    String raw = "Line: 28, Column: 6, Path: $.components[2], "
        + "Error: property 'notARealField' is not defined in the schema and the schema does not allow additional properties";

    assertThat(translate(raw))
        .isEqualTo("Line: 28, Column: 6, Path: $.components[2], "
            + "Error: Field \"notARealField\" is not defined in this CycloneDX schema. "
            + "Remove it or use a properties[] entry.");
  }

  @Test
  public void rewrites_date_time_pattern_mismatch() {
    String raw = "Line: 7, Column: 40, Path: $.metadata.timestamp, "
        + "Error: does not match the date-time pattern must be a valid RFC 3339 date-time";

    assertThat(translate(raw))
        .isEqualTo("Line: 7, Column: 40, Path: $.metadata.timestamp, "
            + "Error: Not a valid RFC 3339 date-time (expected e.g. \"2026-06-30T12:00:00Z\").");
  }

  @Test
  public void rewrites_unsupported_xml_namespace() {
    String raw = "CycloneDX XML namespace is not supported";

    assertThat(translate(raw))
        .isEqualTo("This SBOM's XML namespace isn't a recognized CycloneDX version. "
            + "IQ supports CycloneDX 1.1 through 1.7.");
  }

  @Test
  public void rewrites_unsupported_json_version_1_8() {
    String raw = "CycloneDX JSON 1.8 version is not supported";

    assertThat(translate(raw))
        .isEqualTo("CycloneDX 1.8 isn't supported. IQ supports CycloneDX 1.1 through 1.7.");
  }

  @Test
  public void rewrites_unsupported_json_version_2_0() {
    // Confirms the version capture group works for arbitrary future versions
    String raw = "CycloneDX JSON 2.0 version is not supported";

    assertThat(translate(raw))
        .isEqualTo("CycloneDX 2.0 isn't supported. IQ supports CycloneDX 1.1 through 1.7.");
  }

  @Test
  public void unsupported_xml_version_anchor_does_not_match_similar_strings() {
    // Only exact "CycloneDX XML namespace is not supported" should trigger the rewrite;
    // near-matches in the middle of a larger message must pass through.
    String raw = "Something happened. CycloneDX XML namespace is not supported. More context.";

    assertThat(translate(raw)).isEqualTo(raw);
  }

  @Test
  public void unknown_error_pattern_passes_through_verbatim() {
    String raw = "Line: 42, Column: 1, Path: $.metadata, Error: something entirely unfamiliar happened here";

    assertThat(translate(raw)).isEqualTo(raw);
  }

  @Test
  public void message_without_line_column_prefix_still_translates_when_pattern_matches() {
    // networknt output when line info is unavailable — path-less raw
    String raw = "must not be valid to the schema {\"required\":[\"versionRange\"]}";

    assertThat(translate(raw))
        .isEqualTo("versionRange is only allowed when isExternal=true. "
            + "Set isExternal: true on this component, or remove versionRange.");
  }

  @Test
  public void null_input_returns_null() {
    assertThat(translate(null)).isNull();
  }

  @Test
  public void empty_input_returns_empty() {
    assertThat(translate("")).isEmpty();
  }

  @Test
  public void first_matching_pattern_wins() {
    // Both the "required property" and "additional property" patterns are in the table.
    // A message that matches only "required property" should get that translation, not fall through.
    String raw = "Path: $.foo, Error: required property 'bar' not found and something else";

    assertThat(translate(raw))
        .contains("Missing required field \"bar\".")
        .doesNotContain("required property 'bar' not found");
  }

  @Test
  public void captured_field_name_with_special_chars_is_preserved() {
    String raw = "Path: $.components[0], "
        + "Error: property 'my_odd-field.name' is not defined in the schema "
        + "and the schema does not allow additional properties";

    assertThat(translate(raw))
        .contains("Field \"my_odd-field.name\" is not defined in this CycloneDX schema.");
  }

  @Test
  public void unanchored_patterns_match_within_larger_message() {
    // Confirms unanchored patterns work with line/column prefixes from the validator.
    // This is the primary use case - patterns should match after the "Line: L, Column: C, Path: P, Error: " prefix.
    String raw =
        "Line: 16, Column: 6, Path: $.components[0], Error: must not be valid to the schema {\"required\":[\"versionRange\"]}";

    assertThat(translate(raw))
        .startsWith("Line: 16, Column: 6, Path: $.components[0], Error: ")
        .contains("versionRange is only allowed when isExternal=true");
  }

  @Test
  public void anchored_unsupported_version_patterns_require_exact_match() {
    // The "not supported" patterns are anchored (use ^...$) so they only match exact strings.
    // This prevents false positives where "CycloneDX JSON 1.8 version is not supported" appears mid-message.
    String rawXml = "Something happened. CycloneDX XML namespace is not supported. More context.";
    String rawJson = "Prefix text. CycloneDX JSON 1.8 version is not supported. Suffix text.";

    assertThat(translate(rawXml)).isEqualTo(rawXml);
    assertThat(translate(rawJson)).isEqualTo(rawJson);
  }

  @Test
  public void multiple_validation_errors_can_each_be_translated_independently() {
    // Simulates the pattern used by ApiThirdPartyScanService where both main exception
    // and suppressed exceptions need translation.
    String mainMessage = "CycloneDX XML namespace is not supported";
    String suppressedMessage = "must not be valid to the schema {\"required\":[\"versionRange\"]}";

    // Each message translates independently
    assertThat(translate(mainMessage))
        .contains("IQ supports CycloneDX 1.1 through 1.7");
    assertThat(translate(suppressedMessage))
        .contains("versionRange is only allowed when isExternal=true");
  }
}
