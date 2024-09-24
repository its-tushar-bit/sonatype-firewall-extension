/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular, Brain, clmEndpoint*/

import { selectVersion } from '@sonatype/version-graph';

const NEXT_NO_VIOLATIONS = 'next-no-violations';
const NEXT_NO_VIOLATIONS_DEPENDENCIES = 'next-no-violations-with-dependencies';
const NEXT_NON_FAILING = 'next-non-failing';
const NEXT_NON_FAILING_DEPENDENCIES = 'next-non-failing-with-dependencies';

import { find, propEq } from 'ramda';
import { capitalize } from '../../util/jsUtil';

export default function ComponentController(
  $scope,
  Coordinates,
  OwnerContext,
  errorMessage,
  Properties,
  $http,
  $injector
) {
  function coordinatesChanged() {
    var coordinates = Coordinates.get() ? { coordinates: Coordinates.get(), appId: OwnerContext.ownerId } : null;

    $scope.errorMessage = null;

    if (!angular.equals($scope.coordinates, coordinates)) {
      $scope.componentDetailsList = null;
      $scope.loaded = false;
      $scope.coordinates = coordinates;

      if (coordinates && coordinates.appId && !Properties.isUnknown()) {
        // only pass the hash if current version
        const hash = Coordinates.isOriginalVersion() ? Properties.getHash() : null;
        $http
          .get(
            Brain[clmEndpoint.type].getComponentListUrl(
              OwnerContext.ownerType,
              OwnerContext.ownerId,
              Coordinates.getFormat(),
              hash,
              Properties.getMatchState(),
              Properties.getProprietary(),
              Coordinates.get(),
              Properties.getPathname(),
              Coordinates.getIdentificationSource(),
              OwnerContext.scanId,
              Properties.getStageId(),
              Properties.getDependencyType()
            )
          )
          .then(
            function (response) {
              $scope.componentDetailsList = response.data.allVersions || response.data.list || response.data;
              for (var i = 0; i < $scope.componentDetailsList.length; i++) {
                $scope.componentDetailsList[i].proprietary = Coordinates.get().proprietary;
              }
              $scope.loaded = true;

              setRemediations(response.data);
            },
            function (error) {
              $scope.setError(error);
            }
          );
      }
    }
  }

  function createSuggestedRemediationWithRecommendedVersion(item, remediationVersion) {
    switch (item.type) {
      case NEXT_NO_VIOLATIONS:
        return {
          id: 'next-no-violation-version-link',
          text: ': Next version with no policy violation',
          type: NEXT_NO_VIOLATIONS,
          linkId: 'select-no-violation',
          linkText: remediationVersion,
          version: remediationVersion,
        };
      case NEXT_NON_FAILING:
        return {
          id: 'next-no-fail-version-link',
          text: `: Next version with no ${capitalize(Properties.getStageId())} failure`,
          type: NEXT_NON_FAILING,
          linkId: 'select-no-fail',
          linkText: remediationVersion,
          version: remediationVersion,
        };
      case NEXT_NON_FAILING_DEPENDENCIES:
        return {
          id: 'next-no-fail-dependencies-version-link',
          text:
            `: Next version with no ${capitalize(Properties.getStageId())} failure ` +
            'for this component and its dependencies',
          type: NEXT_NON_FAILING_DEPENDENCIES,
          linkId: 'select-no-fail-dependencies',
          linkText: remediationVersion,
          version: remediationVersion,
        };
      case NEXT_NO_VIOLATIONS_DEPENDENCIES:
        return {
          id: 'next-no-violation-dependencies-version-link',
          text: ': Next version with no policy violations for this component and its dependencies',
          type: NEXT_NO_VIOLATIONS_DEPENDENCIES,
          linkId: 'select-no-violation-dependencies',
          linkText: remediationVersion,
          version: remediationVersion,
        };
    }
  }

  function createSuggestedRemediationWithCurrentVersion(item, remediationVersion) {
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
            `The current version doesn't cause ${capitalize(Properties.getStageId())} failure ` +
            'for this component and its dependencies',
          type: NEXT_NON_FAILING_DEPENDENCIES,
          version: remediationVersion,
        };
      case NEXT_NON_FAILING:
        return {
          id: 'next-no-fail-version',
          text: `The current version doesn't cause ${capitalize(Properties.getStageId())} failure`,
          type: NEXT_NON_FAILING,
          version: remediationVersion,
        };
    }
  }

  function createSuggestedRemediation(item) {
    const applicationVersion = $scope.coordinates.coordinates.version;
    const remediationVersion = item.data.component.componentIdentifier.coordinates.version;

    if (item.data.component.thirdParty) {
      return {
        id: 'remediation-clair',
        text: `Next version: ${remediationVersion}`,
      };
    } else if (remediationVersion !== applicationVersion) {
      return createSuggestedRemediationWithRecommendedVersion(item, remediationVersion);
    } else {
      return createSuggestedRemediationWithCurrentVersion(item, remediationVersion);
    }
  }

  function shouldDisplayWithoutDependenciesRemediation(withDependenciesSuggestion, withoutDependenciesSuggestion) {
    if (!withoutDependenciesSuggestion) {
      return false;
    }
    if (!withDependenciesSuggestion) {
      return true;
    }
    const withDependenciesVersion = withDependenciesSuggestion.data.component.componentIdentifier.coordinates.version;
    const withoutDependenciesVersion =
      withoutDependenciesSuggestion.data.component.componentIdentifier.coordinates.version;
    const currentVersion = $scope.coordinates.coordinates.version;
    return withoutDependenciesVersion !== currentVersion || withDependenciesVersion !== currentVersion;
  }

  function setRemediations({ remediation }) {
    $scope.suggestedRemediations = [];

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

      if (shouldDisplayWithoutDependenciesRemediation(nonViolatingDependencySuggestion, nonViolatingSuggestion)) {
        $scope.suggestedRemediations.push(createSuggestedRemediation(nonViolatingSuggestion));
      }

      if (nonViolatingDependencySuggestion) {
        $scope.suggestedRemediations.push(createSuggestedRemediation(nonViolatingDependencySuggestion));
      }

      if (shouldDisplayWithoutDependenciesRemediation(nonFailingDependencySuggestion, nonFailingSuggestion)) {
        $scope.suggestedRemediations.push(createSuggestedRemediation(nonFailingSuggestion));
      }

      if (nonFailingDependencySuggestion) {
        $scope.suggestedRemediations.push(createSuggestedRemediation(nonFailingDependencySuggestion));
      }
    }

    if (!$scope.suggestedRemediations.length) {
      $scope.suggestedRemediations.push({
        id: 'no-versions-available',
        text: 'There are no suggested versions for this component',
      });
    }
  }

  $scope.setError = function (error) {
    $scope.errorMessage = errorMessage(error);
  };

  $scope.retryFn = function () {
    $scope.$broadcast('reload');
  };

  $scope.$on('reload', coordinatesChanged);

  $scope.$watch(
    function () {
      return Properties.isUnknown();
    },
    function () {
      $scope.isUnknown = Properties.isUnknown();
    }
  );

  $scope.$watch(function () {
    return Coordinates.get();
  }, coordinatesChanged);

  $scope.$watch(function () {
    return OwnerContext.ownerId;
  }, coordinatesChanged);

  $scope.showAddProprietary = function () {
    // 'proprietary.matchers.modal' is available only in the context of CIP (if clmEndpoint.canAddProprietary)
    var ProprietaryMatchersModal = $injector.get('proprietary.matchers.modal');
    ProprietaryMatchersModal.open(OwnerContext.ownerId, getPathNames());
  };

  $scope.canShowAddProprietary = function () {
    if (!clmEndpoint.canAddProprietary || Properties.getProprietary() || OwnerContext.ownerType === 'repository') {
      return false;
    }
    // don't show if there are no pathNames
    return getPathNames().length !== 0;
  };

  $scope.isInnerSource = function () {
    return Properties.isInnerSource();
  };

  function getPathNames() {
    // SelectedComponent is available only in the context of CIP (if clmEndpoint.canAddProprietary)
    var SelectedComponent = $injector.get('SelectedComponent');
    return SelectedComponent.get().pathnames.filter(isNotDependency);
  }

  $scope.markSelection = (remediation) => {
    if (remediation && remediation.type) {
      changeSelectedVersionToSuggestedRemediation(remediation.type);
    }
  };

  $scope.isApplicationOwnerContext = function () {
    if (OwnerContext.ownerType !== 'application') {
      return false;
    }

    if (!OwnerContext.ownerId) {
      return false;
    }
    return true;
  };

  $scope.isRecommendationsAvailable = function () {
    if ($scope.recommendationsSupported === false) {
      return false;
    }
    return true;
  };

  function changeSelectedVersionToSuggestedRemediation(type) {
    let suggestedRemediationVersion = find(propEq('type', type), $scope.suggestedRemediations).version;
    $.each($scope.componentDetailsList, function (index, item) {
      if (item.componentIdentifier.coordinates.version === suggestedRemediationVersion) {
        Coordinates.setSelected(item.componentIdentifier.coordinates);
        Properties.setHash(item.hash);
        selectVersion(index);
      }
    });
  }
}

function isNotDependency(pathName) {
  // doesn't start with "dependency:\"
  return !/^dependency:\//.test(pathName);
}

ComponentController.$inject = [
  '$scope',
  'Coordinates',
  'OwnerContext',
  'ErrorMessage',
  'Properties',
  '$http',
  '$injector',
];
