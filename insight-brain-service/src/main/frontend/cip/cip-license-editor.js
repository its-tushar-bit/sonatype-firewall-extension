/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular, $, CLM, Insight, InsightDatatable */
(function() {
  'use strict';
  function BrainLicenseEditorTab(node, options) {
    this.node = node;
    this.options = options;
  }

  function createPlugin() {

    BrainLicenseEditorTab.prototype = new Insight.InformationPanelPlugin({ priority: 80 });

    BrainLicenseEditorTab.prototype.destroy = function() {
      if (this.node) {
        this.node.empty();
      }
    };
    BrainLicenseEditorTab.prototype.getTitle = function() {
      return 'Licenses';
    };
    BrainLicenseEditorTab.prototype.isVisible = function() {
      return this.gav.matchState !== 'unknown';
    };

    BrainLicenseEditorTab.prototype.create = function() {
      var timestamp = new Date().getTime(),
          container = $('<div clm-include="\'' + CLM.path + 'cip/cip-license-editor.html\'"></div>'),
          me = this;

      me.node.empty();
      container.appendTo(this.node);

      angular.module('componentProvider' + timestamp, ['ComponentUtils']).service('SelectedComponent', [
        'ComponentUtil', function(ComponentUtil) {
          var component = me.component || me.gav;
          ComponentUtil.enhanceWithComponentIdentifier(component);
          return component;
        }
      ]).service('DataView', function() {
        return me.grid.getData();
      });

      angular.bootstrap(container[0], ['LicenseEditor', 'componentProvider' + timestamp]);
    };

    return BrainLicenseEditorTab;
  }

  var licenseEditor = angular.module('LicenseEditor',
      ['CommonServices', 'AngularCommon', 'ApplicationIdProvider', 'HttpInterceptors', 'UnauthenticatedResponseHttpInterceptor']);

  licenseEditor.controller('LicenseEditorController', [
    '$scope', '$q', '$http', 'Messages', 'SelectedComponent', 'DataView', 'ApplicationId',
    function($scope, $q, $http, Messages, SelectedComponent, DataView, ApplicationId) {

      function getHierarchyById(id) {
        for (var i = 0; i < $scope.hierarchy.length; i++) {
          if ($scope.hierarchy[i].ownerId === id) {
            return $scope.hierarchy[i];
          }
        }
      }

      function updateStatuses() {
        $scope.statuses = angular.copy(statuses);
        if ($scope.override && $scope.override.ownerId) {
          var overrideScope = getHierarchyById($scope.override.ownerId);
          if (overrideScope && overrideScope.licenseOverride && overrideScope.ownerType === 'application' &&
              $scope.hierarchy.length > 1) {
            for (var i = 0; i < $scope.hierarchy.length; i++) {
              if ($scope.hierarchy[i].ownerType !== 'application') {
                $scope.statuses.push({
                  value: 'DELETE',
                  label: 'Inherit Status (' +
                      ($scope.hierarchy[i].licenseOverride ? getStatusName($scope.hierarchy[i].licenseOverride.status) : 'Open') +
                      ')'
                });
                break;
              }
            }
          }
        }
      }

      function updateTable() {
        if (SelectedComponent) {
          var licenseOverride = null,
              component = SelectedComponent;
          for (var i = 0; i < $scope.hierarchy.length; i++) {
            if ($scope.hierarchy[i].licenseOverride) {
              licenseOverride = $scope.hierarchy[i].licenseOverride;
              break;
            }
          }

          if (licenseOverride && licenseOverride.licenseIds && licenseOverride.licenseIds.length > 0) {
            component.overriddenLicenses = $.map(licenseOverride.licenseIds, function(val) {
              return [$scope.licenses[val].shortDisplayName];
            });
            component.effectiveLicenses = component.overriddenLicenses;
            component.overriddenLicenseThreat = InsightDatatable.getLicenseThreatLevelFromArray(component.overriddenLicenses[0]);
            component.effectiveLicenseThreat = InsightDatatable.getLicenseThreatLevelFromArray(component.overriddenLicenses[0]);
          }
          else {
            component.overriddenLicenses = null;
            component.overriddenLicenseThreat = null;

            var licenses = {};
            component.effectiveLicenses = [];
            if (component.declaredLicenses) {
              $.each(component.declaredLicenses, function(index, license) {
                licenses[license] = license;
                component.effectiveLicenses.push(license);
              });
            }
            if (component.observedLicenses) {
              $.each(component.observedLicenses, function(index, license) {
                if (!licenses[license]) {
                  component.effectiveLicenses.push(license);
                }
              });
            }

            // Update threat
            component.effectiveLicenseThreat = InsightDatatable.getLicenseThreatLevelFromArray(component.effectiveLicenses);
          }
          component._formattedEffectiveLicenseThreat = component.effectiveLicenses.join(', ');
          component.status = licenseOverride ? getStatusName(licenseOverride.status) : 'Open';

          // Update Grid
          DataView.updateItem(SelectedComponent.id, component);
          // Update Summary Page
          Insight.updateSummary();
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
        }
        else {
          $scope.override.status = 'OPEN';
        }
      }

      function getStatusName(id) {
        for (var i = 0; i < statuses.length; i++) {
          if (statuses[i].value === id) {
            return statuses[i].label;
          }
        }
        //you send me junk, i send you junk back ;)
        return id;
      }

      var statuses = [
        { value: 'OPEN', label: 'Open' },
        { value: 'ACKNOWLEDGED', label: 'Acknowledged' },
        { value: 'OVERRIDDEN', label: 'Overridden' },
        { value: 'SELECTED', label: 'Selected' },
        { value: 'CONFIRMED', label: 'Confirmed' }
      ];

      $scope.doLoad = function() {
        $scope.error = null;

        var promises = [];
        // List of licenses
        promises.push($http.get(CLM.path + 'rest/license?filterSynthetic=true'));
        // Current override state
        promises.push($http.get(CLM.path + 'rest/licenseOverride/application/' + ApplicationId.encoded() +
            '?componentIdentifier=' + encodeURIComponent(JSON.stringify(SelectedComponent.componentIdentifier))));

        // Component licenses
        promises.push($http.get(CLM.path + 'rest/ci/componentDetails/licenses/' + ApplicationId.encoded() +
            '?componentIdentifier=' + encodeURIComponent(JSON.stringify(SelectedComponent.componentIdentifier))));

        $q.all(promises).then(function(results) {
          var licenses = results[0].data,
              currentOverride = results[1].data,
              component = results[2].data;

          $scope.component = component;
          $scope.licenses = {};
          $scope.rawLicenses = licenses;

          angular.forEach($scope.rawLicenses, function(rawLicense) {
            //this is solely for use in the multi select dropdown
            rawLicense.name = rawLicense.shortDisplayName;
            $scope.licenses[rawLicense.id] = rawLicense;
          });

          $scope.hierarchy = currentOverride.licenseOverridesByOwner;
          $scope.reset();

          $scope.selectableLicenses = {};
          angular.forEach($scope.component.selectableLicenses, function (license) {
            $scope.selectableLicenses[license.licenseId] = $scope.licenses[license.licenseId];
          });

          // Hide SELECTED status if there are no licenses to choose from
          if ($scope.component.selectableLicenses.length === 0 ) {
            for (var i=0; i<$scope.statuses.length; i++) {
              if ($scope.statuses[i].value === 'SELECTED') {
                $scope.statuses.splice(i, 1);
                statuses.splice(i, 1);
                break;
              }
            }
          }
        }, function() {
          $scope.error = arguments[0];
        });
      };

      $scope.save = function() {
        $scope.saving = true;

        var licenseOverride = {
              id: null,
              ownerId: null,
              componentIdentifier: SelectedComponent.componentIdentifier,
              status: $scope.override.status.toUpperCase(),
              licenseIds: [],
              comment: $scope.override.comment || ''
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
          $http['delete'](CLM.path + 'rest/licenseOverride/application/' + licenseOverride.ownerId + '/' +
                  owner.licenseOverride.id).success(function() {
            $scope.saving = false;
            owner.licenseOverride = null;
            $scope.reset();
            updateStatuses();
            updateTable();
          }).error(function() {
                $scope.alert = Messages.getHttpErrorMessage(arguments);
                $scope.saving = false;
              });
        }
        else {
          $http.post(CLM.path + 'rest/licenseOverride/' + owner.ownerType + '/' + owner.ownerId,
                  licenseOverride).success(function(data) {
            $scope.saving = false;
            owner.licenseOverride = data;
            $scope.reset();
            updateStatuses();
            updateTable();
          }).error(function() {
            $scope.alert = Messages.getHttpErrorMessage(arguments);
            $scope.saving = false;
          });
        }
      };

      $scope.reset = function() {
        $scope.override = {
          status: null,
          ownerId: null,
          licenseIds: []
        };
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

      $scope.getLicenseThreatClass = function(threat) {
        if (threat === null || typeof threat === 'undefined') {
          return 'unspecified';
        }
        else if (threat > 7) {
          return 'critical';
        }
        else if (threat > 3) {
          return 'severe';
        }
        else if (threat > 0) {
          return 'moderate';
        }
        else {
          return 'none';
        }
      };

      $scope.isClaimedComponent = function() {
        return SelectedComponent.identificationSource === 'Manual';
      };

      $scope.isSubmitEnabled = function() {
        return $scope.licenseEditorForm && $scope.licenseEditorForm.$dirty && !$scope.licenseEditorForm.$invalid && !$scope.saving;
      };

      // Remove licenses when changing status
      $scope.$watch('override.status', function() {
        if ($scope.override) {
          $scope.override.licenseIds = [];
        }
      });

      // Create synthetic Inherit
      $scope.$watch('override.ownerId', function(newValue) {
        if (newValue) {
          setOverrideScope(getHierarchyById(newValue));
        }
        updateStatuses();
      });

      $scope.doLoad();
    }
  ]);

  CLM.loadPlugin(createPlugin, 'Edit License');
}());
