/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

/**
 * Compares two objects by a string value, case-insensitively
 */
const compareCaseInsensitive = (a, b, field) => a[field].toUpperCase().localeCompare(b[field].toUpperCase());

/**
 * Transforms policy alerts from the API format to a flattened format
 * for display in the UI
 */
export function transformPolicyAlerts(alerts) {
  const retval = [];

  (alerts || []).forEach((alert) => {
    alert.trigger.componentFacts.forEach((componentFact) => {
      componentFact.constraintFacts.forEach((constraintFact) => {
        constraintFact.conditionFacts.forEach((conditionFact) => {
          retval.push({
            policyName: alert.trigger.policyName,
            threatLevel: alert.trigger.threatLevel,
            constraintName: constraintFact.constraintName,
            reason: conditionFact.reason,
          });
        });
      });
    });
  });

  // Sort by threat level (highest first), then policy name, then constraint name
  retval.sort((a, b) => {
    let comparison;
    if (a.threatLevel !== b.threatLevel) {
      return b.threatLevel - a.threatLevel;
    }
    comparison = compareCaseInsensitive(a, b, 'policyName');
    if (comparison !== 0) {
      return comparison;
    }
    return compareCaseInsensitive(a, b, 'constraintName');
  });

  return retval;
}
