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
export const RECOMMENDED_NON_BREAKING = 'recommended-non-breaking';
export const RECOMMENDED_NON_BREAKING_WITH_DEPENDENCIES = 'recommended-non-breaking-with-dependencies';

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
        isGolden: false,
      };
    case NEXT_NON_FAILING:
      return {
        id: 'next-no-fail-version',
        text: `Next version with no ${capitalize(stageId)} failure`,
        type: NEXT_NON_FAILING,
        linkId: 'select-no-fail',
        linkText: remediationVersion,
        version: remediationVersion,
        isGolden: false,
      };
    case NEXT_NON_FAILING_DEPENDENCIES:
      return {
        id: 'next-no-fail-dependencies-version',
        text: `Next version with no ${capitalize(stageId)} failure for this component and its dependencies`,
        type: NEXT_NON_FAILING_DEPENDENCIES,
        linkId: 'select-no-fail-dependencies',
        linkText: remediationVersion,
        version: remediationVersion,
        isGolden: false,
      };
    case NEXT_NO_VIOLATIONS_DEPENDENCIES:
      return {
        id: 'next-no-violation-dependencies-version',
        text: 'Next version with no policy violations for this component and its dependencies',
        type: NEXT_NO_VIOLATIONS_DEPENDENCIES,
        linkId: 'select-no-violation-dependencies',
        linkText: remediationVersion,
        version: remediationVersion,
        isGolden: false,
      };
    case RECOMMENDED_NON_BREAKING:
      return {
        id: 'recommended-non-breaking-version',
        text: 'No breaking changes, No policy violations for this component',
        type: RECOMMENDED_NON_BREAKING,
        linkId: 'select-no-violation-no-breaking',
        linkText: remediationVersion,
        version: remediationVersion,
        isGolden: false,
      };
    case RECOMMENDED_NON_BREAKING_WITH_DEPENDENCIES:
      return {
        id: 'recommended-non-breaking-with-dependencies-version',
        text: 'No breaking changes, No policy violations for this component, No policy violations for its dependencies',
        type: RECOMMENDED_NON_BREAKING_WITH_DEPENDENCIES,
        linkId: 'select-no-violation-no-breaking-dependencies',
        linkText: remediationVersion,
        version: remediationVersion,
        isGolden: true,
      };
  }
};

const getRemediationVersion = (item) =>
  item &&
  item.data &&
  item.data.component &&
  item.data.component.componentIdentifier &&
  item.data.component.componentIdentifier.coordinates &&
  item.data.component.componentIdentifier.coordinates.version;

const createSuggestedRemediation = (item, applicationVersion, stageId) => {
  const remediationVersion = getRemediationVersion(item);

  if (item.data.component.thirdParty) {
    return {
      id: 'remediation-clair',
      text: `Next version: ${remediationVersion}`,
      version: remediationVersion,
      isGolden: false,
    };
  } else if (remediationVersion !== applicationVersion) {
    return createSuggestedRemediationWithRecommendedVersion(item, remediationVersion, stageId);
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

const getRemediationsWithoutDuplicates = (versions) => {
  const uniqueVersions = [];
  const seenVersions = new Set();

  for (const version of versions) {
    if (!seenVersions.has(version.version)) {
      uniqueVersions.push(version);
      seenVersions.add(version.version);
    }
  }

  return uniqueVersions;
};

export const setRemediations = (remediation, actualVersion, stageId) => {
  let suggestedRemediations = [];

  if (remediation && remediation.versionChanges) {
    const filteredVersions = remediation.versionChanges.filter((item) => getRemediationVersion(item) !== actualVersion);
    const recommendedSuggestion =
      remediation.suggestedVersionChange?.type === RECOMMENDED_NON_BREAKING ? remediation.suggestedVersionChange : null;
    const recommendedWithDependenciesSuggestion =
      remediation.suggestedVersionChange?.type === RECOMMENDED_NON_BREAKING_WITH_DEPENDENCIES
        ? remediation.suggestedVersionChange
        : null;
    const nonViolatingDependencySuggestion = find(propEq('type', NEXT_NO_VIOLATIONS_DEPENDENCIES), filteredVersions);
    const nonViolatingSuggestion = find(propEq('type', NEXT_NO_VIOLATIONS), filteredVersions);
    const nonFailingDependencySuggestion = find(propEq('type', NEXT_NON_FAILING_DEPENDENCIES), filteredVersions);
    const nonFailingSuggestion = find(propEq('type', NEXT_NON_FAILING), filteredVersions);

    if (recommendedWithDependenciesSuggestion) {
      suggestedRemediations.push(
        createSuggestedRemediation(recommendedWithDependenciesSuggestion, actualVersion, stageId)
      );
    }

    if (
      shouldDisplayWithoutDependenciesRemediation(
        recommendedWithDependenciesSuggestion,
        recommendedSuggestion,
        actualVersion
      )
    ) {
      suggestedRemediations.push(createSuggestedRemediation(recommendedSuggestion, actualVersion, stageId));
    }

    if (nonViolatingDependencySuggestion) {
      suggestedRemediations.push(createSuggestedRemediation(nonViolatingDependencySuggestion, actualVersion, stageId));
    }

    if (
      shouldDisplayWithoutDependenciesRemediation(
        nonViolatingDependencySuggestion,
        nonViolatingSuggestion,
        actualVersion
      )
    ) {
      suggestedRemediations.push(createSuggestedRemediation(nonViolatingSuggestion, actualVersion, stageId));
    }

    if (nonFailingDependencySuggestion) {
      suggestedRemediations.push(createSuggestedRemediation(nonFailingDependencySuggestion, actualVersion, stageId));
    }

    if (
      shouldDisplayWithoutDependenciesRemediation(nonFailingDependencySuggestion, nonFailingSuggestion, actualVersion)
    ) {
      suggestedRemediations.push(createSuggestedRemediation(nonFailingSuggestion, actualVersion, stageId));
    }
  }

  if (!suggestedRemediations.length) {
    suggestedRemediations.push({
      id: 'no-versions-available',
      text: 'There are no suggested versions for this component',
    });
  }

  return getRemediationsWithoutDuplicates(suggestedRemediations);
};

export const getAsyncRecommendationsPrioritiesPage = (remediation, actualVersion, stageId) => {
  if (remediation && remediation.versionChanges) {
    const filteredVersions = remediation.versionChanges.filter((item) => getRemediationVersion(item) !== actualVersion);

    const recommendedSuggestion =
      remediation.suggestedVersionChange?.type === RECOMMENDED_NON_BREAKING ? remediation.suggestedVersionChange : null;
    const recommendedWithDependenciesSuggestion =
      remediation.suggestedVersionChange?.type === RECOMMENDED_NON_BREAKING_WITH_DEPENDENCIES
        ? remediation.suggestedVersionChange
        : null;
    const nonViolatingDependencySuggestion = find(propEq('type', NEXT_NO_VIOLATIONS_DEPENDENCIES), filteredVersions);
    const nonViolatingSuggestion = find(propEq('type', NEXT_NO_VIOLATIONS), filteredVersions);
    const nonFailingDependencySuggestion = find(propEq('type', NEXT_NON_FAILING_DEPENDENCIES), filteredVersions);
    const nonFailingSuggestion = find(propEq('type', NEXT_NON_FAILING), filteredVersions);

    if (recommendedWithDependenciesSuggestion) {
      return createSuggestedRemediation(recommendedWithDependenciesSuggestion, actualVersion, stageId);
    }

    if (
      shouldDisplayWithoutDependenciesRemediation(
        recommendedWithDependenciesSuggestion,
        recommendedSuggestion,
        actualVersion
      )
    ) {
      return createSuggestedRemediation(recommendedSuggestion, actualVersion, stageId);
    }

    if (nonViolatingDependencySuggestion) {
      return createSuggestedRemediation(nonViolatingDependencySuggestion, actualVersion, stageId);
    }

    if (
      shouldDisplayWithoutDependenciesRemediation(
        nonViolatingDependencySuggestion,
        nonViolatingSuggestion,
        actualVersion
      )
    ) {
      return createSuggestedRemediation(nonViolatingSuggestion, actualVersion, stageId);
    }

    if (nonFailingDependencySuggestion) {
      return createSuggestedRemediation(nonFailingDependencySuggestion, actualVersion, stageId);
    }

    if (
      shouldDisplayWithoutDependenciesRemediation(nonFailingDependencySuggestion, nonFailingSuggestion, actualVersion)
    ) {
      return createSuggestedRemediation(nonFailingSuggestion, actualVersion, stageId);
    }
  }

  return {
    id: 'no-versions-available',
    text: 'No recommendation available',
  };
};

export const getRecommendationsPrioritiesPage = (remediationType, remediationVersion, actualVersion, stageId) => {
  if (remediationType && remediationVersion !== actualVersion) {
    return createSuggestedRemediationWithRecommendedVersion({ type: remediationType }, remediationVersion, stageId);
  }
  return {
    id: 'no-versions-available',
    text: 'No recommendation available',
  };
};
