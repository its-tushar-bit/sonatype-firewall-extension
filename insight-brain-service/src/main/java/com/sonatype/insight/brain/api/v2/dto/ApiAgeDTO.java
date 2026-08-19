/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sonatype.insight.error.exception.BadRequestException;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * @since 1.63
 */
public class ApiAgeDTO
{
  private static final Pattern AGE_PATTERN = Pattern.compile("([1-9][0-9]*) +(day|week|month|year)s?");

  public enum AgeUnit
  {
    DAY,
    WEEK,
    MONTH,
    YEAR;

    @Override
    public String toString() {
      return name().toLowerCase(Locale.ROOT);
    }

    public static AgeUnit fromString(String unit) {
      return AgeUnit.valueOf(unit.toUpperCase(Locale.ROOT));
    }
  }

  public int amountOfTime;

  public AgeUnit unitOfTime;

  public ApiAgeDTO() {
  }

  public ApiAgeDTO(int amountOfTime, AgeUnit unitOfTime) {
    this.amountOfTime = amountOfTime;
    this.unitOfTime = unitOfTime;
  }

  @Override
  @JsonValue
  public String toString() {
    return amountOfTime + " " + unitOfTime + (amountOfTime != 1 ? "s" : "");
  }

  @JsonCreator
  public static ApiAgeDTO fromString(String age) {
    ApiAgeDTO dto = null;
    if (age != null) {
      Matcher matcher = AGE_PATTERN.matcher(age);
      if (!matcher.matches()) {
        throw new BadRequestException(
            "Invalid age '" + age + "', expected format: <positive-number> \"days|weeks|months|years\"");
      }
      dto = new ApiAgeDTO();
      try {
        dto.amountOfTime = Short.parseShort(matcher.group(1));
      }
      catch (NumberFormatException e) {
        throw new BadRequestException(
            "Invalid age '" + age + "', amount of time must be less than " + (Short.MAX_VALUE + 1));
      }
      dto.unitOfTime = AgeUnit.fromString(matcher.group(2));
    }
    return dto;
  }

  public Integer toDays() {
    switch (unitOfTime) {
      case DAY:
        return amountOfTime * 1;
      case WEEK:
        return amountOfTime * 7;
      case MONTH:
        return amountOfTime * 30;
      case YEAR:
        return amountOfTime * 365;
      default:
        throw new IllegalArgumentException("unknown time unit " + unitOfTime);
    }
  }

  public static ApiAgeDTO fromDays(Integer days) {
    if (days == null) {
      return null;
    }
    ApiAgeDTO dto = new ApiAgeDTO();
    if (days % 365 == 0) {
      dto.amountOfTime = days / 365;
      dto.unitOfTime = AgeUnit.YEAR;
    }
    else if (days % 30 == 0) {
      dto.amountOfTime = days / 30;
      dto.unitOfTime = AgeUnit.MONTH;
    }
    else if (days % 7 == 0) {
      dto.amountOfTime = days / 7;
      dto.unitOfTime = AgeUnit.WEEK;
    }
    else {
      dto.amountOfTime = days;
      dto.unitOfTime = AgeUnit.DAY;
    }
    return dto;
  }
}
