/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular, $, clmBuildTimestamp */
(function() {
  'use strict';

  function getCancelConfig(confirmFn) {
    return {
      title : 'Unsaved Changes',
      body : 'There are unsaved changes, continuing will discard any unsaved changes.',
      buttons : [{
        name : 'Cancel'
      }, {
        name : 'Confirm',
        type : 'danger',
        click : confirmFn
      }]
    };
  }

  var licenseGroupModule = angular.module('LicenseThreatGroup',
      ['AngularCommon', 'ResourceModule', 'CLMAppLocation', 'CommonServices', 'ui.bootstrap']);

  licenseGroupModule.service('licenseGroupStore', [
    '$q', '$http', 'CLMAppLocations', 'CLMResource', function($q, $http, CLMAppLocations, CLMResource) {
      var licenseGroupStore = null, licenseGroupStores = {};

      function refreshLicenseStore() {
        var entityId = CLMAppLocations.getEntityId();
        licenseGroupStore = licenseGroupStores[entityId];
        if (!licenseGroupStore) {
          licenseGroupStore = licenseGroupStores[entityId] = CLMResource.getStore(angular.extend({ url: CLMAppLocations.getLicenseGroupsUrl() },
              licenseGroupStoreTemplate));
        }
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
        get: function() {
          refreshLicenseStore();
          return licenseGroupStore.get();
        },
        refresh: function() {
          refreshLicenseStore();
          return licenseGroupStore.refresh();
        },
        create: function() {
          return licenseGroupStore.create();
        }
      };
    }
  ]);

  licenseGroupModule.service('licenseStore', [
    'CLMLocations', 'CLMResource', function(CLMLocations, CLMResource) {
      var licenseStore = CLMResource.getStore({
        id: 'id',
        url: CLMLocations.getLicensesUrl(),
        params: {
          timestamp: new Date().getTime()
        }
      });
      return licenseStore;
    }
  ]);

  licenseGroupModule.controller('LicenseThreatGroupController', [
    '$scope', '$http', '$q', 'CLMLocations', 'CLMAppLocations', 'licenseStore', 'licenseGroupStore', 'ownerChange',
    function($scope, $http, $q, CLMLocations, CLMAppLocations, licenseStore, licenseGroupStore, ownerChange) {
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

      $scope.allLicenses = null;
      $scope.allExpanded = {};

      $scope.doLoad = function() {
        var promises = [
          licenseStore.get(), $http.get(CLMAppLocations.getApplicableLicenseGroupsUrl(), {
            params: { timestamp: new Date().getTime() }
          }), licenseGroupStore.refresh()
        ];
        if ($scope.error) {
          $scope.error = null;
          $scope.$broadcast('reload');
        }
        $scope.ltgEditorMap = {};

        $q.all(promises).then(function(results) {
          var licenses = results[0];

          $scope.allLicenses = licenses.sort(sortLicense);
          $scope.applicableLicenseGroups = results[1].data.licenseThreatGroupsByOwner;

          angular.forEach($scope.applicableLicenseGroups, function(applicableLicenseGroup, index) {
            applicableLicenseGroup.editable = index === 0;
          });

          $scope.applicableLicenseGroups[0].licenseThreatGroups = $scope.licenseGroups = results[2];
        }, function(errors) {
          $scope.error = angular.isArray(errors) ? errors[0] : errors;
        });
      };

      $scope.doLoad();

      $scope.$on('ownerChanged', ownerChange.getEventHandler($scope, 'applicableLicenseGroups'));
      $scope.$on('refresh', $scope.doLoad);

      $scope.getDisplayName = function(license) {
        var licenseId = license.licenseId;
        for (var i = 0; i < $scope.allLicenses.length; i++) {
          if ($scope.allLicenses[i].id === licenseId) {
            return '(' + $scope.allLicenses[i].shortDisplayName + ') ' + $scope.allLicenses[i].longDisplayName;
          }
        }
      };

      $scope.editLicenseGroup = function(group) {
        $scope.ltgEditorMap[group.id] = true;
        $scope.allExpanded[group.ownerId] = true;
      };

      $scope.toggleAll = function(applicableLicenseGroup) {
        var action = $scope.allExpanded[applicableLicenseGroup.ownerId] ? 'hide' : 'show';
        $('#' + applicableLicenseGroup.ownerId).find('.accordion-body').collapse(action);
        //TODO: to work around collapse bug, fixed in newer release of bootstrap
        //https://github.com/twitter/bootstrap/pull/7424/files
        $('#' + applicableLicenseGroup.ownerId).find('.licenseGroup-top')[action ==
            'hide' ? 'addClass' : 'removeClass']('collapsed');
        $scope.allExpanded[applicableLicenseGroup.ownerId] = !($scope.allExpanded[applicableLicenseGroup.ownerId] ||
            false);
      };

      $scope.isExpanded = function(applicableLicenseGroup) {
        return $scope.allExpanded[applicableLicenseGroup.ownerId] || false;
      };
      $scope.showEditor = function(licenseGroup) {
        $('#collapse' + licenseGroup.id).collapse('show');
        $("a[href='#collapse" + licenseGroup.id + "']").removeClass('collapsed');

        $scope.ltgEditorMap[licenseGroup.id] = true;
      };

      $scope.confirmDeleteLicenseGroup = function(group) {
        $scope.selectedGroup = angular.extend({ id: null, applicationId: null, name: '', threatLevel: 5 }, group);
        $scope.deletedEnabled = true;
        $('#deleteLicenseGroupModal').modal('show');
      };

      $scope.deleteLicenseGroup = function() {
        $scope.deletedEnabled = false;
        $http['delete'](CLMAppLocations.getDeleteLicenseGroupUrl($scope.selectedGroup)).success(function() {
          angular.forEach($scope.licenseGroups, function(licenseCandidate, key) {
            if (licenseCandidate.id === $scope.selectedGroup.id) {
              $scope.licenseGroups.splice(key, 1);
              return false;
            }
          });
          deselect();
          $('#deleteLicenseGroupModal').modal('hide');
        }).error(function() {
              $('#deleteLicenseGroupModal').modal('hide');
              $scope.$broadcast('showServerError', arguments);
            });
      };

      $scope.$on('pageChangeStarted', function(event) {
        var dirty = false;
        angular.forEach($scope.licenseGroups, function(group, index) {
          dirty = dirty || group.isDirty();
        });
        if (dirty) {
          event.preventDefault();
        }
      });
    }
  ]);

  licenseGroupModule.controller('LicenseThreatGroupEditorController',
      [
        '$scope', '$filter', '$http', '$q', 'hudson', 'CLMAppLocations', 'licenseGroupStore', 'licenseStore',
        'Messages',
        function($scope, $filter, $http, $q, hudson, CLMAppLocations, licenseGroupStore, licenseStore, Messages) {
          $scope.alerts = [];
          $scope.licenseSearch = '';

          function load() {
            $scope.licenseGroups = null;
            $scope.allLicenses = null;
            $q.all([licenseGroupStore.get(), licenseStore.get()]).then(function(results) {
              $scope.licenseGroups = results[0];
              $scope.allLicenses = results[1];
            }, function() {
              /* Errors are handled above this point */
            });
            $filter('toLicense'); // Trigger loading licenses
          }

          $scope.$on('reload', function() {
            load();
          });
          load();

          $scope.searchEnter = function() {
            var filter = $filter('filterLicenses');
            var licenses = filter($scope.allLicenses,
                { groupLicenses: $scope.selectedGroupLicenses, searchLicense: $scope.licenseSearch });
            // If only one license is applicable to the current search filter, set isApplied true when enter is pressed
            if (licenses.length == 1) {
              $scope.addLicense(licenses[0]);
              $scope.licenseSearch = '';
            }
          };

          $scope.addLicense = function(license) {
            var newLicense = angular.extend(licenseGroupStore.create('licenses'), { licenseId: license.id });
            $scope.selectedGroup.licenses.push(newLicense);
            $scope.selectedGroupLicenses[license.id] = true;
          };

          $scope.removeLicense = function(license) {
            var index = -1;
            angular.forEach($scope.selectedGroup.licenses, function(l, candidateIndex) {
              if (license.id === l.licenseId) {
                index = candidateIndex;
              }
            });
            if (index !== -1) {
              $scope.selectedGroupLicenses[license.id] = null;
              $scope.selectedGroup.licenses.splice(index, 1);
            }
          };

          $scope.canSaveEdit = function(valid) {
            return valid && !$scope.submitActive && $scope.selectedGroup != null && $scope.selectedGroup.name;
          };

          $scope.saveClick = function() {
            if (!$scope.canSaveEdit($scope.licenseGroupEditor.$valid)) {
              return;
            }

            $scope.submitActive = true;
            $scope.selectedGroup.$save().then(function() {
              $scope.hide();
            }, function(rejection) {
              $scope.submitActive = false;
              $scope.alerts.push({
                type: 'error',
                msg: 'An error occurred while saving the license threat group. (' +
                    Messages.getHttpErrorMessage(rejection) + ')'
              });
            });
          };

          $scope.$on('pageChangeStarted', function(event) {
            if ($scope.selectedGroup) {
              if ($scope.selectedGroup.isDirty()) {
                event.preventDefault();
                return;
              }
            }
          });

          $scope.$watch('selectedGroup', function(newValue) {
            if (newValue) {
              $scope.selectedGroupLicenses = {};
              $scope.licenseSearch = '';

              angular.forEach($scope.selectedGroup.licenses, function(license, index) {
                $scope.selectedGroupLicenses[license.licenseId] = true;
              });
            }
            else {
              $scope.selectedGroupLicenses = null;
            }
          });
        }
      ]);

  licenseGroupModule.filter('toLicense', [
    'licenseStore', function(licenseStore) {
      var licenses = null;
      // Failure / loading delay isn't relevant here as it will be handled in the controllers
      licenseStore.get().then(function(data) {
        licenses = {};
        angular.forEach(data, function(license) {
          licenses[license.id] = license;
        });
      });
      return function(items, filter) {
        var retLicenses = [];
        angular.forEach(items, function(item) {
          retLicenses.push(licenses[item.licenseId]);
        });
        return retLicenses;
      };
    }
  ]);

  licenseGroupModule.filter('filterLicenses', function() {
    return function(items, filter) {
      if (!angular.isArray(items) || (!filter.searchLicense && !filter.groupLicenses)) {
        return items;
      }
      var filteredLicenses = [],
          searchLicense = filter.searchLicense;

      angular.forEach(items, function(license) {
        if (filter.groupLicenses && filter.groupLicenses[license.id] !== true &&
            (!searchLicense || ~license.shortDisplayName.toLowerCase().indexOf(searchLicense.toLowerCase()))) {
          filteredLicenses.push(license);
        }
      });

      return filteredLicenses;
    };
  });

  licenseGroupModule.directive('enterDown', function() {
    return {
      restrict: 'A',
      link: function(scope, element, attrs) {
        if (attrs.preventSubmit !== undefined) {
          $(element).bind('keypress keydown keyup', function(e) {
            if (e.keyCode == 13) {
              e.preventDefault();
            }
          });
        }

        $(element).bind('keydown', function(e) {
          if (e.keyCode == 13) {
            scope.$apply(attrs.enterDown || angular.noop);
          }
        });
      }
    };
  });

  licenseGroupModule.directive('ltgEditor', [
    'Dialog', function(Dialog) {
      return {
        restrict: 'A',
        templateUrl: 'ltgInlineEditor',
        scope: {
          ltgEditor: '=',
          hide: '&'
        },
        controller: 'LicenseThreatGroupEditorController',
        link: function(scope, element, attrs) {
          scope.$watch('ltgEditor', function(val) {
            if (val) {
              scope.selectedGroup = val.$clone();
            }
            else {
              scope.selectedGroup = null;
            }
          });
          scope.alerts = [];
          scope.cancelLicenseGroupEdit = function() {
            if (scope.selectedGroup && scope.selectedGroup.isDirty()) {
              Dialog.open(getCancelConfig(function () {
                scope.hide();
              }));
            }
            else {
              scope.hide();
            }
          };
        }
      };
    }
  ]);

  licenseGroupModule.directive('ltgCreator', [
    'licenseGroupStore', 'Dialog', function(licenseGroupStore, Dialog) {
      return {
        restrict: 'A',
        templateUrl: 'ltgcreator',
        scope: {},
        link: function(scope, element, attrs) {
          scope.createNew = function() {
            scope.selectedGroup = licenseGroupStore.create();
          };
          scope.hide = function() {
            scope.selectedGroup = null;
          };
          scope.cancelLicenseGroupEdit = function() {
            if (scope.selectedGroup && scope.selectedGroup.isDirty()) {
              Dialog.open(getCancelConfig(function () {
                scope.hide();
              }));
            }
            else {
              scope.hide();
            }
          };
        }
      };
    }
  ]);
}());
