/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.insight.brain.utils.CsvWritable;

public class MoveOrganizationResponseDTO
{
  public List<ValidationError> errors = new ArrayList<>();

  public List<ValidationWarning> warnings = new ArrayList<>();

  public static class ValidationError
      implements CsvWritable
  {
    public enum MoveOrganizationValidationErrorType
    {
      TAG,
      POLICY,
      LICENSE_THREAT_GROUP,
      LABEL,
      PARENT_HIERARCHY
    }

    public String message;

    public MoveOrganizationValidationErrorType type;

    // error messages texts
    public static final String SAME_PARENT_MSG =
        "New parent org %s is already set and in use as the parent of org %s";

    public static final String INVALID_PARENT_HIERARCHY_MSG =
        "The parent org cannot be a child of the current org";

    public static final String TAG_MISSING_MSG =
        "Missing application categories for new parent org %s: ";

    public static final String LABEL_MISSING_MSG =
        "Missing labels for new parent org %s: ";

    public static final String LTG_MISSING_MSG =
        "Missing license threat groups for new parent org %s: ";

    public static final String POLICY_MISSING_MSG =
        "Missing org policies for new parent org %s: ";

    public static final String DUPLICATED_LABELS_MSG = "The following labels already exist on new parent %s: ";

    public static final String DUPLICATED_TAGS_MSG =
        "The following application categories already exist on new parent %s: ";

    public static final String DUPLICATED_LTG_MSG =
        "The following license threat groups already exist on new parent %s: ";

    public static final String DUPLICATED_POLICIES_MSG = "The following policies already exist on new parent %s: ";

    public ValidationError() {
      // default constructor used for jackson de-serialization in response object check in tests
    }

    public ValidationError(final MoveOrganizationValidationErrorType validationErrorType, final String message) {
      this.type = validationErrorType;
      this.message = message;
    }

    public static String getCsvHeader() {
      return "Type, Description";
    }

    @Override
    public String toCsvLine() {
      return CsvWritable.joiner.join(type.name(), CsvWritable.quoteFieldWhenSpecialCsvCharactersPresent(message));
    }
  }

  public static class ValidationWarning
  {
    public static final String LICENSE_OVERRIDES_LOST_MSG =
        "New parent org %s does not inherit the same license overrides as old parent org %s";

    public static final String POLICY_MONITORING_DIFFERENT_MSG = "The new parent organization uses a different stage"
        + " for continuous policy monitoring.";

    public static final String POLICY_MONITORING_MISSING_MSG = "The new parent organization does not use continuous"
        + " policy monitoring.";

    public static final String POLICY_WAIVER_MSG =
        "Some policy waivers that were previously inherited no longer apply in the new parent organization.";

    public enum MoveOrganizationValidationWarningType
    {
      LICENSE_OVERRIDE,
      POLICY_MONITORING,
      POLICY_WAIVER
    }

    public String message;

    public MoveOrganizationValidationWarningType type;

    public ValidationWarning() {
      // default constructor used for jackson de-serialization in response object check in tests
    }

    public ValidationWarning(final MoveOrganizationValidationWarningType type, final String message) {
      this.message = message;
      this.type = type;
    }
  }
}
