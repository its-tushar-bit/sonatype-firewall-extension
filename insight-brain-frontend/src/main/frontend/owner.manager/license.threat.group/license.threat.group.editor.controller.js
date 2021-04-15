/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
export default function LicenseThreatGroupEditorController(
  $scope,
  $q,
  $http,
  $stateParams,
  $state,
  CLMLocations,
  licenseGroupStore,
  CLMContextLocations,
  DeleteModalService,
  SameOwnerStateNavigationService
) {
  var originalPickedLicenseIds = [],
    isNavigatingAfterRemove,
    vm = this;

  vm.isApp = CLMContextLocations.isApplication();
  vm.availableLicenses = [];
  vm.deleteLTG = deleteLTG;
  vm.dirtyLTG = undefined;
  vm.doLoad = doLoad;
  vm.isLTGDirty = isLTGDirty;
  vm.loadError = undefined;
  vm.ltgEditor = undefined;
  vm.ltgEditorMask = undefined;
  vm.save = save;
  vm.siblings = [];
  vm.submitError = undefined;
  vm.nextLTG = undefined;
  vm.getTooltip = getTooltip;

  vm.doLoad();

  $scope.$on('pageChangeStarted', function (event) {
    if (!isNavigatingAfterRemove && vm.isLTGDirty()) {
      event.preventDefault();
    }
  });

  function deleteLTG() {
    DeleteModalService.deleteResource(
      'License Threat Group',
      vm.dirtyLTG.name,
      vm.dirtyLTG
    ).then(function () {
      isNavigatingAfterRemove = true;

      if (vm.isApp) {
        if (vm.nextLTG) {
          SameOwnerStateNavigationService.goEdit('edit-license-threat-group', {
            licenseThreatGroupId: vm.nextLTG.id,
          });
        } else {
          $state.go('management.view.application', {
            applicationPublicId: $stateParams.applicationPublicId,
          });
        }
      } else {
        SameOwnerStateNavigationService.goEdit('create-license-threat-group');
      }
    });
  }

  function doLoad() {
    var promises = [
      $http.get(CLMLocations.getLicensesUrl()),
      $http.get(CLMContextLocations.getApplicableLicenseGroupsUrl()),
    ];

    if ($stateParams.licenseThreatGroupId) {
      promises.push(licenseGroupStore[vm.loadError ? 'refresh' : 'get']());
    }

    $q.all(promises).then(
      function (results) {
        vm.availableLicenses = results[0].data.map((item) => ({
          ...item,
          fullDisplayName: getFullDisplayName(item),
        }));

        results[1].data.licenseThreatGroupsByOwner.forEach(function (owner) {
          owner.licenseThreatGroups.some(function (ltg, index) {
            if (
              $stateParams.licenseThreatGroupId &&
              ltg.id === $stateParams.licenseThreatGroupId
            ) {
              vm.nextLTG =
                owner.licenseThreatGroups[index + 1] ||
                owner.licenseThreatGroups[index - 1];
              return true;
            }
          });

          vm.siblings = vm.siblings.concat(owner.licenseThreatGroups);
        });

        if (!$stateParams.licenseThreatGroupId) {
          vm.dirtyLTG = licenseGroupStore.create();
        } else {
          results[2].some(function (ltgCandidate) {
            if (ltgCandidate.id === $stateParams.licenseThreatGroupId) {
              vm.dirtyLTG = ltgCandidate.$clone();

              originalPickedLicenseIds = vm.dirtyLTG.licenses.map(function (
                license
              ) {
                return license.licenseId;
              });

              vm.availableLicenses.forEach(function (license) {
                license.picked =
                  originalPickedLicenseIds.indexOf(license.id) > -1;
              });

              return true;
            }
          });
        }
        if (!vm.dirtyLTG) {
          vm.loadError = 'Unable to locate License Threat Group.';
        }
      },
      function (error) {
        vm.loadError = error;
      }
    );
    delete vm.loadError;
  }

  function save() {
    if (vm.ltgEditor.$valid && vm.isLTGDirty()) {
      var isNew = vm.dirtyLTG.$new;
      delete vm.submitError;

      // Clears the array, without losing attached properties
      vm.dirtyLTG.licenses.length = 0;

      vm.availableLicenses.forEach(function (license) {
        if (license.picked) {
          var newLicense = angular.extend(
            licenseGroupStore.create('licenses'),
            { licenseId: license.id }
          );
          vm.dirtyLTG.licenses.push(newLicense);
        }
      });

      vm.ltgEditorMask.wrap(vm.dirtyLTG.$save()).then(
        function () {
          if (isNew) {
            vm.siblings.push(vm.dirtyLTG);
            vm.dirtyLTG = licenseGroupStore.create();
            vm.availableLicenses.forEach(function (license) {
              license.picked = false;
            });
          }

          vm.ltgEditor.$setPristine();
          originalPickedLicenseIds = vm.dirtyLTG.licenses.map(function (
            license
          ) {
            return license.licenseId;
          });
        },
        function (error) {
          vm.submitError = error;
        }
      );
    }
  }

  function isLTGDirty() {
    var isLicensePickerDirty = vm.availableLicenses.some(function (license) {
      return (
        (license.picked &&
          originalPickedLicenseIds.indexOf(license.id) === -1) ||
        (!license.picked && originalPickedLicenseIds.indexOf(license.id) > -1)
      );
    });

    return vm.dirtyLTG.isDirty() || isLicensePickerDirty;
  }

  function getTooltip(item) {
    return item.longDisplayName;
  }

  const getFullDisplayName = ({ shortDisplayName, longDisplayName }) =>
    `(${shortDisplayName}) ${longDisplayName}`;
}

LicenseThreatGroupEditorController.$inject = [
  '$scope',
  '$q',
  '$http',
  '$stateParams',
  '$state',
  'CLMLocations',
  'licenseGroupStore',
  'CLMContextLocations',
  'DeleteModalService',
  'SameOwnerStateNavigationService',
];
