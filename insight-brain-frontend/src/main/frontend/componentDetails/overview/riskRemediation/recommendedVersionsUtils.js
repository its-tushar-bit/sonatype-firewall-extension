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
export const INNER_SOURCE_LATEST_NON_BREAKING = 'inner-source-latest-non-breaking';
export const INNER_SOURCE_LATEST = 'inner-source-latest';

const createSuggestedRemediationWithRecommendedVersion = (
  item,
  remediationVersion,
  stageId,
  breakingChangesCount = null
) => {
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
        breakingChangesCount,
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
        breakingChangesCount,
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
        breakingChangesCount,
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
        breakingChangesCount,
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
        breakingChangesCount,
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
        breakingChangesCount: 0, // Golden versions always have 0 breaking changes
      };
    case INNER_SOURCE_LATEST_NON_BREAKING:
      return {
        id: 'innersource-latest-non-breaking-version',
        text: 'Latest non-breaking inner source version',
        type: INNER_SOURCE_LATEST_NON_BREAKING,
        linkId: 'select-innersource-latest-non-breaking',
        linkText: remediationVersion,
        version: remediationVersion,
        isGolden: false,
        breakingChangesCount: 0,
      };
    case INNER_SOURCE_LATEST:
      return {
        id: 'innersource-latest-version',
        text: 'Latest inner source version',
        type: INNER_SOURCE_LATEST,
        linkId: 'select-innersource-latest',
        linkText: remediationVersion,
        version: remediationVersion,
        isGolden: false,
        breakingChangesCount: 1,
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

const createSuggestedRemediation = (item, applicationVersion, stageId, allVersions) => {
  const remediationVersion = getRemediationVersion(item);
  let breakingChangesCount = null;

  if (allVersions) {
    if (item.isGolden) {
      breakingChangesCount = 0;
    } else {
      const versionInfo = allVersions.find(
        (version) => version.componentIdentifier.coordinates.version === remediationVersion
      );
      breakingChangesCount = versionInfo?.breakingChangesCount ?? null;
    }
  }

  if (item.data.component.thirdParty) {
    return {
      id: 'remediation-clair',
      text: `Next version: ${remediationVersion}`,
      version: remediationVersion,
      isGolden: false,
      breakingChangesCount,
    };
  } else if (remediationVersion !== applicationVersion) {
    return createSuggestedRemediationWithRecommendedVersion(item, remediationVersion, stageId, breakingChangesCount);
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

export const getAsyncRecommendationsPrioritiesPage = (remediation, actualVersion, stageId, allVersions) => {
  if (remediation && remediation.versionChanges) {
    const filteredVersions = remediation.versionChanges.filter((item) => getRemediationVersion(item) !== actualVersion);

    const innersourceLatestNonBreakingSuggestion =
      remediation.suggestedVersionChange?.type === INNER_SOURCE_LATEST_NON_BREAKING
        ? remediation.suggestedVersionChange
        : null;
    const innersourceLatestSuggestion =
      remediation.suggestedVersionChange?.type === INNER_SOURCE_LATEST ? remediation.suggestedVersionChange : null;

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

    if (innersourceLatestNonBreakingSuggestion) {
      return createSuggestedRemediationWithRecommendedVersion(
        innersourceLatestNonBreakingSuggestion,
        getRemediationVersion(innersourceLatestNonBreakingSuggestion),
        stageId,
        allVersions
      );
    }

    if (innersourceLatestSuggestion) {
      return createSuggestedRemediationWithRecommendedVersion(
        innersourceLatestSuggestion,
        getRemediationVersion(innersourceLatestSuggestion),
        stageId,
        allVersions
      );
    }

    if (recommendedWithDependenciesSuggestion) {
      return createSuggestedRemediation(recommendedWithDependenciesSuggestion, actualVersion, stageId, allVersions);
    }

    if (
      shouldDisplayWithoutDependenciesRemediation(
        recommendedWithDependenciesSuggestion,
        recommendedSuggestion,
        actualVersion
      )
    ) {
      return createSuggestedRemediation(recommendedSuggestion, actualVersion, stageId, allVersions);
    }

    if (nonViolatingDependencySuggestion) {
      return createSuggestedRemediation(nonViolatingDependencySuggestion, actualVersion, stageId, allVersions);
    }

    if (
      shouldDisplayWithoutDependenciesRemediation(
        nonViolatingDependencySuggestion,
        nonViolatingSuggestion,
        actualVersion
      )
    ) {
      return createSuggestedRemediation(nonViolatingSuggestion, actualVersion, stageId, allVersions);
    }

    if (nonFailingDependencySuggestion) {
      return createSuggestedRemediation(nonFailingDependencySuggestion, actualVersion, stageId, allVersions);
    }

    if (
      shouldDisplayWithoutDependenciesRemediation(nonFailingDependencySuggestion, nonFailingSuggestion, actualVersion)
    ) {
      return createSuggestedRemediation(nonFailingSuggestion, actualVersion, stageId, allVersions);
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
