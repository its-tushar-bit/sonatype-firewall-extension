/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import com.sonatype.insight.brain.api.v2.dto.ApiAgeDTO.AgeUnit;
import com.sonatype.insight.error.exception.BadRequestException;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.RecursiveComparisonAssert;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class ApiAgeDTOTest
{
  private RecursiveComparisonAssert<?> assertThatAge(ApiAgeDTO ageDTO) {
    return assertThat(ageDTO).usingRecursiveComparison();
  }

  @Test
  public void testToString_Day() {
    assertThat(new ApiAgeDTO(1, AgeUnit.DAY)).hasToString("1 day");
    assertThat(new ApiAgeDTO(2, AgeUnit.DAY)).hasToString("2 days");
  }

  @Test
  public void testToString_Week() {
    assertThat(new ApiAgeDTO(1, AgeUnit.WEEK)).hasToString("1 week");
    assertThat(new ApiAgeDTO(5, AgeUnit.WEEK)).hasToString("5 weeks");
  }

  @Test
  public void testToString_Month() {
    assertThat(new ApiAgeDTO(1, AgeUnit.MONTH)).hasToString("1 month");
    assertThat(new ApiAgeDTO(7, AgeUnit.MONTH)).hasToString("7 months");
  }

  @Test
  public void testToString_Year() {
    assertThat(new ApiAgeDTO(1, AgeUnit.YEAR)).hasToString("1 year");
    assertThat(new ApiAgeDTO(9, AgeUnit.YEAR)).hasToString("9 years");
  }

  @Test
  public void testFromString_Day() {
    assertThatAge(ApiAgeDTO.fromString("1 day")).isEqualTo(new ApiAgeDTO(1, AgeUnit.DAY));
    assertThatAge(ApiAgeDTO.fromString("2 days")).isEqualTo(new ApiAgeDTO(2, AgeUnit.DAY));
  }

  @Test
  public void testFromString_Week() {
    assertThatAge(ApiAgeDTO.fromString("1 week")).isEqualTo(new ApiAgeDTO(1, AgeUnit.WEEK));
    assertThatAge(ApiAgeDTO.fromString("2 weeks")).isEqualTo(new ApiAgeDTO(2, AgeUnit.WEEK));
  }

  @Test
  public void testFromString_Month() {
    assertThatAge(ApiAgeDTO.fromString("1 month")).isEqualTo(new ApiAgeDTO(1, AgeUnit.MONTH));
    assertThatAge(ApiAgeDTO.fromString("2 months")).isEqualTo(new ApiAgeDTO(2, AgeUnit.MONTH));
  }

  @Test
  public void testFromString_Year() {
    assertThatAge(ApiAgeDTO.fromString("1 year")).isEqualTo(new ApiAgeDTO(1, AgeUnit.YEAR));
    assertThatAge(ApiAgeDTO.fromString("2 years")).isEqualTo(new ApiAgeDTO(2, AgeUnit.YEAR));
  }

  @Test
  public void testFromString_InvalidFormat() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> ApiAgeDTO.fromString("1"))
        .withMessageContaining("Invalid age")
        .withMessageContaining("expected format");
  }

  @Test
  public void testFromString_ExcessiveAmount() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> ApiAgeDTO.fromString("123456 days"))
        .withMessageContaining("Invalid age")
        .withMessageContaining("must be less than");
  }

  @Test
  public void testJson_Serialization() throws Exception {
    assertThat(new ObjectMapper().writeValueAsString(new ApiAgeDTO(2, AgeUnit.WEEK))).isEqualTo("\"2 weeks\"");
  }

  @Test
  public void testJson_Deserialization() throws Exception {
    assertThatAge(new ObjectMapper().readValue("\"2 weeks\"", ApiAgeDTO.class))
        .isEqualTo(new ApiAgeDTO(2, AgeUnit.WEEK));
  }

  @Test
  public void testFromDays_Day() {
    assertThatAge(ApiAgeDTO.fromDays(1)).isEqualTo(new ApiAgeDTO(1, AgeUnit.DAY));
    assertThatAge(ApiAgeDTO.fromDays(6)).isEqualTo(new ApiAgeDTO(6, AgeUnit.DAY));
    assertThatAge(ApiAgeDTO.fromDays(8)).isEqualTo(new ApiAgeDTO(8, AgeUnit.DAY));
    assertThatAge(ApiAgeDTO.fromDays(29)).isEqualTo(new ApiAgeDTO(29, AgeUnit.DAY));
    assertThatAge(ApiAgeDTO.fromDays(31)).isEqualTo(new ApiAgeDTO(31, AgeUnit.DAY));
    assertThatAge(ApiAgeDTO.fromDays(366)).isEqualTo(new ApiAgeDTO(366, AgeUnit.DAY));
  }

  @Test
  public void testFromDays_Week() {
    for (int i = 1; i < 12; i++) {
      assertThatAge(ApiAgeDTO.fromDays(i * 7)).isEqualTo(new ApiAgeDTO(i, AgeUnit.WEEK));
    }
  }

  @Test
  public void testFromDays_Month() {
    for (int i = 1; i < 12; i++) {
      assertThatAge(ApiAgeDTO.fromDays(i * 30)).isEqualTo(new ApiAgeDTO(i, AgeUnit.MONTH));
    }
  }

  @Test
  public void testFromDays_Year() {
    for (int i = 1; i < 12; i++) {
      assertThatAge(ApiAgeDTO.fromDays(i * 365)).isEqualTo(new ApiAgeDTO(i, AgeUnit.YEAR));
    }
  }

  @Test
  public void testToDays_Day() {
    assertThat(new ApiAgeDTO(1, AgeUnit.DAY).toDays()).isEqualTo(1);
    assertThat(new ApiAgeDTO(7, AgeUnit.DAY).toDays()).isEqualTo(7);
  }

  @Test
  public void testToDays_Week() {
    assertThat(new ApiAgeDTO(1, AgeUnit.WEEK).toDays()).isEqualTo(7);
    assertThat(new ApiAgeDTO(52, AgeUnit.WEEK).toDays()).isEqualTo(364);
  }

  @Test
  public void testToDays_Month() {
    assertThat(new ApiAgeDTO(1, AgeUnit.MONTH).toDays()).isEqualTo(30);
    assertThat(new ApiAgeDTO(12, AgeUnit.MONTH).toDays()).isEqualTo(360);
  }

  @Test
  public void testToDays_Year() {
    assertThat(new ApiAgeDTO(1, AgeUnit.YEAR).toDays()).isEqualTo(365);
    assertThat(new ApiAgeDTO(10, AgeUnit.YEAR).toDays()).isEqualTo(3650);
  }
}
