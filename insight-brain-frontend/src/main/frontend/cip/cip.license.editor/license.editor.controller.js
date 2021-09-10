/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular, CLM, clmEndpoint */

export default function LicenseEditorController($scope, $q, $http, Messages, SelectedComponent, OwnerContext) {
  var vm = this;

  function getHierarchyById(id) {
    for (var i = 0; i < $scope.hierarchy.length; i++) {
      if ($scope.hierarchy[i].ownerId === id) {
        return $scope.hierarchy[i];
      }
    }
  }

  function getHierarchyIndexById(id) {
    for (var i = 0; i < $scope.hierarchy.length; i++) {
      if ($scope.hierarchy[i].ownerId === id) {
        return i;
      }
    }
  }

  function updateTable(updatedComponent) {
    var licenseOverride = null,
      component = SelectedComponent.get();

    $scope.component = updatedComponent;

    // if the license table is active in an App report, attempt to update it
    if (component && angular.isArray(component.observedLicenses)) {
      component.effectiveLicenses = [];
      component.effectiveLicenseThreat = null;

      updatedComponent.effectiveLicenses.forEach(function (licenseWithThreat) {
        component.effectiveLicenses.push(licenseWithThreat.license.licenseName);
        component.effectiveLicenseThreat = Math.max(licenseWithThreat.threatLevel, component.effectiveLicenseThreat);
      });
      component._formattedEffectiveLicenseThreat = component.effectiveLicenses.join(', ');

      component.overriddenLicenses = null;
      component.overriddenLicenseThreat = null;

      for (var i = 0; i < $scope.hierarchy.length; i++) {
        if ($scope.hierarchy[i].licenseOverride) {
          licenseOverride = $scope.hierarchy[i].licenseOverride;
          break;
        }
      }
      if (licenseOverride) {
        component.status = getStatusName(licenseOverride.status);

        if (licenseOverride.status === 'SELECTED' || licenseOverride.status === 'OVERRIDDEN') {
          component.overriddenLicenseThreat = component.effectiveLicenseThreat;
          component.overriddenLicenses = component.effectiveLicenses;
        }
      } else {
        component.status = 'Open';
      }

      $scope.$emit('clm.grid.licenses.changed', component);
    } else {
      // new style
      $scope.$emit('reevaluate.component', { hash: component.hash });
    }
  }

  function setOverrideScope(overrideScope) {
    $scope.override.licenseIds = [];
    $scope.override.ownerId = overrideScope.ownerId;

    if (overrideScope.licenseOverride) {
      $scope.override.status = overrideScope.licenseOverride.status;

      if ($scope.override.status === 'OVERRIDDEN' || $scope.override.status === 'SELECTED') {
        $scope.override.licenseIds = overrideScope.licenseOverride.licenseIds;
      }
    } else {
      $scope.override.status = 'OPEN';
    }
  }

  function getStatusName(id) {
    if (id === 'OPEN') {
      return 'Open';
    } else if (id === 'SELECTED') {
      return 'Selected';
    } else if (id === 'OVERRIDDEN') {
      return 'Overridden';
    } else if (id === 'ACKNOWLEDGED') {
      return 'Acknowledged';
    } else if (id === 'CONFIRMED') {
      return 'Confirmed';
    } else {
      //you send me junk, i send you junk back ;)
      return id;
    }
  }

  function createComponentRequest() {
    let licensesUrl =
      CLM.path +
      'rest/' +
      clmEndpoint.type +
      '/componentDetails/' +
      OwnerContext.ownerType +
      '/' +
      OwnerContext.ownerId +
      '/licenses?componentIdentifier=' +
      encodeURIComponent(JSON.stringify(SelectedComponent.get().componentIdentifier));

    if (SelectedComponent.get().identificationSource) {
      licensesUrl =
        licensesUrl +
        '&identificationSource=' +
        encodeURIComponent(SelectedComponent.get().identificationSource) +
        '&scanId=' +
        encodeURIComponent(OwnerContext.scanId);
    }
    return $http.get(licensesUrl);
  }

  $scope.canInherit = function () {
    return $scope.override && getHierarchyIndexById($scope.override.ownerId) < $scope.hierarchy.length - 1;
  };

  $scope.getInheritableStatus = function () {
    var index = getHierarchyIndexById($scope.override.ownerId) + 1;

    for (var i = index; i < $scope.hierarchy.length; i++) {
      if ($scope.hierarchy[i].licenseOverride) {
        return getStatusName($scope.hierarchy[i].licenseOverride.status);
      }
    }
    return 'Open';
  };

  $scope.doLoad = function () {
    $scope.error = null;
    $scope.licenses = null; // trigger loading indicator

    var promises = [];
    // List of licenses
    promises.push($http.get(CLM.path + 'rest/license?filterSynthetic=true'));

    // Component licenses
    promises.push(createComponentRequest());

    // Current override state
    promises.push(
      $http.get(
        CLM.path +
          'rest/licenseOverride/' +
          OwnerContext.ownerType +
          '/' +
          OwnerContext.ownerId +
          '?componentIdentifier=' +
          encodeURIComponent(JSON.stringify(SelectedComponent.get().componentIdentifier))
      )
    );

    $q.all(promises).then(
      function (results) {
        var licenses = results[0].data,
          component = results[1].data;

        $scope.hierarchy = results[2].data.licenseOverridesByOwner;

        $scope.component = component;
        $scope.licenses = {};
        $scope.rawLicenses = licenses;

        angular.forEach($scope.rawLicenses, function (rawLicense) {
          //this is solely for use in the multi select dropdown
          rawLicense.name = rawLicense.shortDisplayName;
          $scope.licenses[rawLicense.id] = rawLicense;
        });

        $scope.reset();

        $scope.selectableLicenses = [];
        angular.forEach($scope.component.selectableLicenses, function (license) {
          $scope.selectableLicenses.push($scope.licenses[license.licenseId]);
        });
      },
      function () {
        $scope.error = arguments[0];
      }
    );
  };

  function getCreateComponentRequest() {
    return createComponentRequest().then(
      function (updatedComponent) {
        $scope.saving = false;
        updateTable(updatedComponent.data);
      },
      function (error) {
        $scope.saving = false;
        $scope.alert = Messages.getHttpErrorMessage(error);
      }
    );
  }

  $scope.save = function () {
    $scope.saving = true;

    var licenseOverride = {
        id: null,
        ownerId: null,
        componentIdentifier: SelectedComponent.get().componentIdentifier,
        status: $scope.override.status.toUpperCase(),
        licenseIds: [],
        comment: $scope.override.comment || '',
      },
      owner = null;

    // Only set license for Override or Select states
    if (licenseOverride.status === 'OVERRIDDEN' || licenseOverride.status === 'SELECTED') {
      licenseOverride.licenseIds = $scope.override.licenseIds;
    }

    // Find owner
    owner = getHierarchyById($scope.override.ownerId);
    licenseOverride.ownerId = owner.ownerId;

    if (licenseOverride.status === 'DELETE') {
      if (!owner.licenseOverride) {
        $scope.reset();
        return getCreateComponentRequest();
      }
      $http['delete'](
        CLM.path +
          'rest/licenseOverride/' +
          owner.ownerType +
          '/' +
          licenseOverride.ownerId +
          '/' +
          owner.licenseOverride.id
      ).then(
        function () {
          owner.licenseOverride = null;
          $scope.reset();

          return getCreateComponentRequest();
        },
        function (error) {
          $scope.alert = Messages.getHttpErrorMessage(error);
          $scope.saving = false;
        }
      );
    } else {
      $http.post(CLM.path + 'rest/licenseOverride/' + owner.ownerType + '/' + owner.ownerId, licenseOverride).then(
        function (response) {
          owner.licenseOverride = response.data;
          $scope.reset();

          return getCreateComponentRequest();
        },
        function (error) {
          $scope.alert = Messages.getHttpErrorMessage(error);
          $scope.saving = false;
        }
      );
    }
  };

  $scope.reset = function () {
    $scope.override = {
      status: null,
      ownerId: null,
      licenseIds: [],
    };

    if (vm.licenseEditorForm) {
      vm.licenseEditorForm.$setPristine();
    }

    if ($scope.hierarchy) {
      for (var i = 0; i < $scope.hierarchy.length; i++) {
        if ($scope.hierarchy[i].licenseOverride) {
          setOverrideScope($scope.hierarchy[i]);
          return;
        }
      }
      setOverrideScope($scope.hierarchy[0]);
    }
  };

  $scope.getLicenseThreatClass = function (threat) {
    if (threat === null || typeof threat === 'undefined') {
      return 'unspecified';
    } else if (threat > 7) {
      return 'critical';
    } else if (threat > 3) {
      return 'severe';
    } else if (threat > 0) {
      return 'moderate';
    } else {
      return 'none';
    }
  };

  $scope.isClaimedComponent = function () {
    const selectedComponent = SelectedComponent.get();
    return selectedComponent != null && selectedComponent.identificationSource === 'Manual';
  };

  $scope.isSubmitEnabled = function () {
    var validOverride =
      !($scope.override.status === 'OVERRIDDEN' || $scope.override.status === 'SELECTED') ||
      $scope.override.licenseIds.length > 0;

    return (
      vm.licenseEditorForm &&
      vm.licenseEditorForm.$dirty &&
      !vm.licenseEditorForm.$invalid &&
      !$scope.saving &&
      validOverride
    );
  };

  // Remove licenses when changing status
  $scope.onOverrideStatusChange = function () {
    if ($scope.override) {
      $scope.override.licenseIds = [];
    }
  };

  // Create synthetic Inherit
  $scope.$watch('override.ownerId', function (newValue) {
    if (newValue) {
      setOverrideScope(getHierarchyById(newValue));
    }
  });

  $scope.$watch(
    () => SelectedComponent.get(),
    (newVal, oldVal) => {
      // compare to old value to avoid watcher initialization
      if (newVal && newVal !== oldVal) {
        $scope.doLoad();
      }
    }
  );

  $scope.doLoad();
}
LicenseEditorController.$inject = ['$scope', '$q', '$http', 'Messages', 'SelectedComponent', 'OwnerContext'];
