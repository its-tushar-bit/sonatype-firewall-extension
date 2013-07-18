/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular, $, clmBuildTimestamp */
(function () {
  'use strict';

  var licenseGroupModule = angular.module('LicenseThreatGroup', ['AngularCommon', 'ResourceModule', 'CLMAppLocation', 'CommonServices']);

  licenseGroupModule.service('licenseGroupStore', ['$q', '$http', 'CLMAppLocations', 'CLMResource', function ($q, $http, CLMAppLocations, CLMResource) {
    var currentStoreId = null, licenseGroupStore = null;

    function refreshLicenseStore() {
      var isNew = !licenseGroupStore || currentStoreId !== CLMAppLocations.getEntityId();
      if (isNew) {
        currentStoreId = CLMAppLocations.getEntityId();
        licenseGroupStore = CLMResource.getStore(angular.extend({ url: CLMAppLocations.getLicenseGroupsUrl() }, licenseGroupStoreTemplate));
      }
      return isNew;
    }

    var licenseGroupStoreTemplate = {
      id: 'id',
      template: { id: null, ownerId: null, name: '', threatLevel: 5 },
      params: {
        timestamp: new Date().getTime()
      },
      relationalConfigs: {
        'licenses': {
          id: 'licenseId',
          template: { id: null, licenseId: null },
          url: CLMAppLocations.getLicenseGroupLicensesUrl,
          params: {
            timestamp: new Date().getTime()
          }
        }
      }
    };

    return {
      get: function () {
        refreshLicenseStore();
        return licenseGroupStore.get();
      },
      refresh: function () {
        refreshLicenseStore();
        return licenseGroupStore.refresh();
      },
      create: function () {
        return licenseGroupStore.create();
      }
    };
  }]);

  licenseGroupModule.service('licenseStore', ['CLMLocations', 'CLMResource', function (CLMLocations, CLMResource) {
    var licenseStore = CLMResource.getStore({
      id: 'id',
      url: CLMLocations.getLicensesUrl(),
      params: {
        timestamp: new Date().getTime()
      }
    });
    return licenseStore;
  }]);

  licenseGroupModule.controller('LicenseThreatGroupController', ['$scope', '$http', '$q', 'CLMLocations', 'CLMAppLocations', 'licenseStore', 'licenseGroupStore', 'Messages', function ($scope, $http, $q, CLMLocations, CLMAppLocations, licenseStore, licenseGroupStore, Messages) {
    function sortLicense(a, b) {
      if (a.id < b.id) {
        return -1;
      }
      if (a.id > b.id) {
        return 1;
      }
      return 0;
    }

    function sortGroupLicense(a, b) {
      if (a.licenseId < b.licenseId) {
        return -1;
      }
      if (a.licenseId > b.licenseId) {
        return 1;
      }
      return 0;
    }

    function deselect() {
      $scope.selectedGroup = null;
    }

    $scope.editorUrl = '../policy-assets/components/license-threat-group/license-threat-group-editor.html?' + clmBuildTimestamp;
    $scope.allLicenses = null;
    $scope.allExpanded = false;

    $scope.doLoad = function () {
      $scope.error = null;

      $q.all([licenseStore.get(), licenseGroupStore.get()]).then(function (results) {
        var licenses = results[0];
        var licenseGroups = results[1];

        $scope.allLicenses = licenses.sort(sortLicense);
        $scope.licenseGroups = licenseGroups;
      }, function (errors) {
        $scope.error = angular.isArray(errors) ? errors[0] : errors;
      });
    };

    $scope.doLoad();

    $scope.getDisplayName = function (licenseId) {
      for (var i = 0; i < $scope.allLicenses.length; i++) {
        if ($scope.allLicenses[i].id === licenseId) {
          return '(' + $scope.allLicenses[i].shortDisplayName + ') ' + $scope.allLicenses[i].longDisplayName;
        }
      }
    };

    $scope.editLicenseGroup = function (group) {
      if (group) {
        $scope.selectedGroup = group.$clone();
      } else {
        $scope.selectedGroup = licenseGroupStore.create();
      }

      angular.element('#licenseModal').modal('show');
    };

    $scope.inlineChangeThreatLevel = function (licenseGroup, threatLevel) {
      licenseGroup.threatLevel = threatLevel.value;
    };

    $scope.hasInlineChanges = function () {
      if (!$scope.licenseGroups) {
        return false;
      }
      for (var i = 0; i < $scope.licenseGroups.length; i++) {
        if ($scope.licenseGroups[i].isDirty()) {
          return true;
        }
      }
    };

    $scope.canSaveInlineEdit = function () {
      return $scope.hasInlineChanges() && !$scope.inlineLicenseGroupForm.$invalid;
    };

    $scope.inlineSaveLicenseGroup = function () {
      for (var i = 0; i < $scope.licenseGroups.length; i++) {
        var licenseThreatGroup = $scope.licenseGroups[i];
        if (licenseThreatGroup.isDirty()) {
          licenseThreatGroup.$save().then(angular.noop, function (rejection) {
            $scope.alerts.push({
              type: 'error',
              msg: 'An error occurred while saving the license threat group. (' + Messages.getHttpErrorMessage(rejection) + ')'
            });
            $scope.inlineRevertLicenseGroup(licenseThreatGroup);
          });
        }
      }
    };

    $scope.inlineRevertLicenseGroups = function () {
      for (var i = 0; i < $scope.licenseGroups.length; i++) {
        var licenseGroup = $scope.licenseGroups[i];
        $scope.inlineRevertLicenseGroup(licenseGroup);
      }
    };

    $scope.inlineRevertLicenseGroup = function (licenseThreatGroup) {
      var original = licenseThreatGroup.$getOriginal();
      angular.extend(licenseThreatGroup, original);
    };

    $scope.toggleAll = function () {
      var action = $scope.allExpanded ? 'hide' : 'show';
      angular.element('.accordion-body').collapse(action);
      $scope.allExpanded = !$scope.allExpanded;
    };

    $scope.confirmDeleteLicenseGroup = function (group) {
      $scope.selectedGroup = angular.extend({ id: null, applicationId: null, name: '', threatLevel: 5 }, group);
      $scope.deletedEnabled = true;
      $('#deleteLicenseGroupModal').modal('show');
    };

    $scope.deleteLicenseGroup = function () {
      $scope.deletedEnabled = false;
      $http['delete'](CLMAppLocations.getDeleteLicenseGroupUrl($scope.selectedGroup)).success(function () {
        angular.forEach($scope.licenseGroups, function (licenseCandidate, key) {
          if (licenseCandidate.id === $scope.selectedGroup.id) {
            $scope.licenseGroups.splice(key, 1);
            return false;
          }
        });
        deselect();
        $('#deleteLicenseGroupModal').modal('hide');
      }).error(function () {
        $('#deleteLicenseGroupModal').modal('hide');
        $scope.$broadcast('showServerError', arguments);
      });
    };

    $scope.$on('license.cancelLicenseGroupEdit', function (event, licenseGroup) {
      event.stopPropagation();
      deselect();
      delete $scope.newGroupName;
      $('#licenseModal').modal('hide');
    });

    $scope.$on('pageChangeStarted', function (event) {
      var dirty = false;
      angular.forEach($scope.licenseGroups, function (group, index) {
        dirty = dirty || group.isDirty();
      });
      if (dirty) {
        event.preventDefault();
      }
    });

    $scope.$on('pageChangeAccepted', function () {
      $scope.inlineRevertLicenseGroups();
    });
  }]);

  licenseGroupModule.controller('LicenseThreatGroupEditorController', ['$scope', '$filter', '$http', 'hudson', 'CLMAppLocations', 'licenseGroupStore', 'Messages', function ($scope, $filter, $http, hudson, CLMAppLocations, licenseGroupStore, Messages) {
    $scope.alerts = [];
    $scope.licenseSearch = '';

    $scope.searchEnter = function () {
      var filter = $filter('filterLicenses');
      var licenses = filter($scope.allLicenses, { groupLicenses: $scope.selectedGroupLicenses, searchLicense: $scope.licenseSearch });
      // If only one license is applicable to the current search filter, set isApplied true when enter is pressed
      if (licenses.length == 1) {
        $scope.addLicense(licenses[0]);
        $scope.licenseSearch = '';
      }
    };

    $scope.addLicense = function (license) {
      var newLicense = angular.extend(licenseGroupStore.create('licenses'), { licenseId: license.id });
      $scope.selectedGroup.licenses.push(newLicense);
      $scope.selectedGroupLicenses[license.id] = true;
    };

    $scope.removeLicense = function (license) {
      var index = -1;
      angular.forEach($scope.selectedGroup.licenses, function (l, candidateIndex) {
        if (license.id === l.licenseId) {
          index = candidateIndex;
        }
      });
      if (index !== -1) {
        $scope.selectedGroupLicenses[license.id] = null;
        $scope.selectedGroup.licenses.splice(index, 1);
      }
    };

    $scope.canSaveEdit = function (valid) {
      return valid && !$scope.submitActive && $scope.selectedGroup != null && $scope.selectedGroup.name;
    };

    $scope.saveClick = function () {
      if (!$scope.canSaveEdit($scope.licenseGroupEditor.$valid)) {
        return;
      }

      (function (licenseGroup) {
        $scope.submitActive = true;

        licenseGroup.$save().then(function (licenseGroup) {
          for (var i = 0; i < $scope.licenseGroups.length; i++) {
            var licenseGroupIter = $scope.licenseGroups[i];
            if (licenseGroup.id === licenseGroupIter.id) {
              $scope.licenseGroups[i] = licenseGroup;
            }
          }

          $scope.alerts = [];
          $scope.$emit('license.cancelLicenseGroupEdit');
        }, function (rejection) {
          $scope.alerts.push({
            type: 'error',
            msg: 'An error occurred while saving the license threat group. (' + Messages.getHttpErrorMessage(rejection) + ')'
          });
        });

        $scope.submitActive = false;
      })($scope.selectedGroup);
    };

    $scope.cancelLicenseGroupEdit = function () {
      $scope.alerts = [];
      $scope.selectedGroup.$revert();
      $scope.$emit('license.cancelLicenseGroupEdit');
    };
    $scope.$on('$destroy', function () {
      angular.element('.modal-backdrop').remove(); // Bootstrap modal creates elements at the document root
    });

    $scope.$on('pageChangeStarted', function (event) {
      if ($scope.selectedGroup) {
        if ($scope.selectedGroup.isDirty()) {
          event.preventDefault();
          return;
        }
      }
    });

    $scope.$watch('selectedGroup', function (newValue) {
      if (newValue) {
        $scope.selectedGroupLicenses = {};
        $scope.licenseSearch = '';

        angular.forEach($scope.selectedGroup.licenses, function (license, index) {
          $scope.selectedGroupLicenses[license.licenseId] = true;
        });
      } else {
        $scope.selectedGroupLicenses = null;
      }
    });
  }]);

  licenseGroupModule.filter('toLicense', ['licenseStore', function (licenseStore) {
    var licenses = null;
    // Failure / loading delay isn't relevant here as it will be handled in the controllers
    licenseStore.get().then(function (data) {
      licenses = {};
      angular.forEach(data, function (license) {
        licenses[license.id] = license;
      });
    });
    return function (items, filter) {
      var retLicenses = [];
      angular.forEach(items, function (item) {
        retLicenses.push(licenses[item.licenseId]);
      });
      return retLicenses;
    };
  }]);

  licenseGroupModule.filter('filterLicenses', function () {
    return function (items, filter) {
      if (!angular.isArray(items) || (!filter.searchLicense && !filter.groupLicenses)) {
        return items;
      }
      var filteredLicenses = [],
      searchLicense = filter.searchLicense;

      angular.forEach(items, function (license) {
        if (filter.groupLicenses && filter.groupLicenses[license.id] !== true && (!searchLicense || ~license.shortDisplayName.toLowerCase().indexOf(searchLicense.toLowerCase()))) {
          filteredLicenses.push(license);
        }
      });

      return filteredLicenses;
    };
  });

  licenseGroupModule.directive('enterDown', function () {
    return {
      restrict: 'A',
      link: function (scope, element, attrs) {
        if (attrs.preventSubmit !== undefined) {
          $(element).bind('keypress keydown keyup', function (e) {
            if (e.keyCode == 13) {
              e.preventDefault();
            }
          });
        }

        $(element).bind('keydown', function (e) {
          if (e.keyCode == 13) {
            scope.$apply(attrs.enterDown || angular.noop);
          }
        });
      }
    };
  });
}());
