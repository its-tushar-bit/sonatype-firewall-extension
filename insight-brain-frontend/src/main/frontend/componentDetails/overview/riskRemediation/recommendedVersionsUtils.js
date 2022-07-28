/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { find, propEq } from 'ramda';
import { capitalize } from '../../../util/jsUtil';

export const NEXT_NO_VIOLATIONS = 'next-no-violations';
export const NEXT_NO_VIOLATIONS_DEPENDENCIES = 'next-no-violations-with-dependencies';
export const NEXT_NON_FAILING = 'next-non-failing';
export const NEXT_NON_FAILING_DEPENDENCIES = 'next-non-failing-with-dependencies';

const createSuggestedRemediationWithRecommendedVersion = (item, remediationVersion, stageId) => {
  switch (item.type) {
    case NEXT_NO_VIOLATIONS:
      return {
        id: 'next-no-violation-version',
        text: 'Next version with no policy violation',
        type: NEXT_NO_VIOLATIONS,
        linkId: 'select-no-violation',
        linkText: remediationVersion,
        version: remediationVersion,
      };
    case NEXT_NON_FAILING:
      return {
        id: 'next-no-fail-version',
        text: `Next version with no ${capitalize(stageId)} failure`,
        type: NEXT_NON_FAILING,
        linkId: 'select-no-fail',
        linkText: remediationVersion,
        version: remediationVersion,
      };
    case NEXT_NON_FAILING_DEPENDENCIES:
      return {
        id: 'next-no-fail-dependencies-version',
        text: `Next version with no ${capitalize(stageId)} failure for this component and its dependencies`,
        type: NEXT_NON_FAILING_DEPENDENCIES,
        linkId: 'select-no-fail-dependencies',
        linkText: remediationVersion,
        version: remediationVersion,
      };
    case NEXT_NO_VIOLATIONS_DEPENDENCIES:
      return {
        id: 'next-no-violation-dependencies-version',
        text: 'Next version with no policy violations for this component and its dependencies',
        type: NEXT_NO_VIOLATIONS_DEPENDENCIES,
        linkId: 'select-no-violation-dependencies',
        linkText: remediationVersion,
        version: remediationVersion,
      };
  }
};

const createSuggestedRemediationWithCurrentVersion = (item, remediationVersion, stageId) => {
  switch (item.type) {
    case NEXT_NO_VIOLATIONS_DEPENDENCIES:
      return {
        id: 'next-no-violation-dependencies-version',
        text: 'The current version has no policy violations for this component and its dependencies',
        type: NEXT_NO_VIOLATIONS_DEPENDENCIES,
        version: remediationVersion,
      };
    case NEXT_NO_VIOLATIONS:
      return {
        id: 'next-no-violation-version',
        text: 'The current version has no policy violations',
        type: NEXT_NO_VIOLATIONS,
        version: remediationVersion,
      };
    case NEXT_NON_FAILING_DEPENDENCIES:
      return {
        id: 'next-no-fail-dependencies-version',
        text:
          `The current version doesn't cause ${capitalize(stageId)} failure ` +
          'for this component and its dependencies',
        type: NEXT_NON_FAILING_DEPENDENCIES,
        version: remediationVersion,
      };
    case NEXT_NON_FAILING:
      return {
        id: 'next-no-fail-version',
        text: `The current version doesn't cause ${capitalize(stageId)} failure`,
        type: NEXT_NON_FAILING,
        version: remediationVersion,
      };
  }
};

const createSuggestedRemediation = (item, applicationVersion, stageId) => {
  const remediationVersion =
    item &&
    item.data &&
    item.data.component &&
    item.data.component.componentIdentifier &&
    item.data.component.componentIdentifier.coordinates &&
    item.data.component.componentIdentifier.coordinates.version;

  if (item.data.component.thirdParty) {
    return {
      id: 'remediation-clair',
      text: `Next version: ${remediationVersion}`,
    };
  } else if (remediationVersion !== applicationVersion) {
    return createSuggestedRemediationWithRecommendedVersion(item, remediationVersion, stageId);
  } else {
    return createSuggestedRemediationWithCurrentVersion(item, remediationVersion, stageId);
  }
};

const shouldDisplayWithoutDependenciesRemediation = (
  withDependenciesSuggestion,
  withoutDependenciesSuggestion,
  currentVersion
) => {
  if (!withoutDependenciesSuggestion) {
    return false;
  }
  if (!withDependenciesSuggestion) {
    return true;
  }
  const withDependenciesVersion = withDependenciesSuggestion.data.component.componentIdentifier.coordinates.version;
  const withoutDependenciesVersion =
    withoutDependenciesSuggestion.data.component.componentIdentifier.coordinates.version;
  return withoutDependenciesVersion !== currentVersion || withDependenciesVersion !== currentVersion;
};

export const setRemediations = (remediation, actualVersion, stageId) => {
  let suggestedRemediations = [];

  if (remediation && remediation.versionChanges) {
    const nonViolatingDependencySuggestion = find(
      propEq('type', NEXT_NO_VIOLATIONS_DEPENDENCIES),
      remediation.versionChanges
    );
    const nonViolatingSuggestion = find(propEq('type', NEXT_NO_VIOLATIONS), remediation.versionChanges);
    const nonFailingDependencySuggestion = find(
      propEq('type', NEXT_NON_FAILING_DEPENDENCIES),
      remediation.versionChanges
    );
    const nonFailingSuggestion = find(propEq('type', NEXT_NON_FAILING), remediation.versionChanges);

    if (
      shouldDisplayWithoutDependenciesRemediation(
        nonViolatingDependencySuggestion,
        nonViolatingSuggestion,
        actualVersion
      )
    ) {
      suggestedRemediations.push(createSuggestedRemediation(nonViolatingSuggestion, actualVersion, stageId));
    }

    if (nonViolatingDependencySuggestion) {
      suggestedRemediations.push(createSuggestedRemediation(nonViolatingDependencySuggestion, actualVersion, stageId));
    }

    if (
      shouldDisplayWithoutDependenciesRemediation(nonFailingDependencySuggestion, nonFailingSuggestion, actualVersion)
    ) {
      suggestedRemediations.push(createSuggestedRemediation(nonFailingSuggestion, actualVersion, stageId));
    }

    if (nonFailingDependencySuggestion) {
      suggestedRemediations.push(createSuggestedRemediation(nonFailingDependencySuggestion, actualVersion, stageId));
    }
  }

  if (!suggestedRemediations.length) {
    suggestedRemediations.push({
      id: 'no-versions-available',
      text: 'No recommended versions are available for the current component',
    });
  }

  return suggestedRemediations;
};
