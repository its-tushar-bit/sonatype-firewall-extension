/**
 * @license Copyright (c) 2013 Sonatype, Inc. All rights reserved. Includes the
 * third-party code listed at
 * http://links.sonatype.com/products/clm/attributions. "Sonatype" is a
 * trademark of Sonatype, Inc.
 */
/* global angular */
(function() {

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
      return !((freemium && !this.options.sampleData) || this.gav.matchState === 'unknown' ||
          this.gav.identificationSource === 'Manual');
    };

    BrainLicenseEditorTab.prototype.create = function() {
      var timestamp = new Date().getTime(),
          container = $('<div clm-include="\'' + CLM.path + 'cip/cip-license-editor.html\'"></div>'),
          me = this;

      me.node.empty();
      container.appendTo(this.node);

      angular.module('componentProvider' + timestamp, []).service('SelectedComponent',function() {
        return me.gav;
      }).service('DataView', function() {
            return me.grid.getData();
          });

      angular.bootstrap(container[0], ['LicenseEditor', 'componentProvider' + timestamp]);
    };

    return BrainLicenseEditorTab;
  }

  var licenseEditor = angular.module('LicenseEditor',
      ['CommonServices', 'AngularCommon', 'Hudson', 'ApplicationIdProvider']);

  licenseEditor.controller('LicenseEditorController', [
    '$scope', '$q', '$http', 'hudson', 'Messages', 'SelectedComponent', 'DataView', 'ApplicationId',
    function($scope, $q, $http, hudson, Messages, SelectedComponent, DataView, ApplicationId) {

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
                  value: "DELETE",
                  label: 'Inherit Status (' +
                      ($scope.hierarchy[i].licenseOverride ? getStatusName($scope.hierarchy[i].licenseOverride.status) : "Open") +
                      ')'
                });
                break;
              }
            }
          }
        }
      }

      function updateTable() {
        if (SelectedComponent && SelectedComponent.observedLicenses) {
          var licenseOverride = null,
              component = SelectedComponent;
          for (var i = 0; i < $scope.hierarchy.length; i++) {
            if ($scope.hierarchy[i].licenseOverride) {
              licenseOverride = $scope.hierarchy[i].licenseOverride;
              break;
            }
          }

          if (licenseOverride && licenseOverride.licenseId) {
            component.overriddenLicenses = [$scope.licenses[licenseOverride.licenseId].shortDisplayName];
            component.effectiveLicenses = component.overriddenLicenses;
            component.overriddenLicenseThreat = InsightDatatable.getLicenseThreatLevelFromArray(component.overriddenLicenses[0]);
            component.effectiveLicenseThreat = InsightDatatable.getLicenseThreatLevelFromArray(component.overriddenLicenses[0]);
          }
          else {
            component.overriddenLicenses = null;
            component.overriddenLicenseThreat = null;

            var licenses = {};
            component.effectiveLicenses = [];
            $.each(component.declaredLicenses, function(index, license) {
              licenses[license] = license;
              component.effectiveLicenses.push(license);
            });
            $.each(component.observedLicenses, function(index, license) {
              if (!licenses[license]) {
                component.effectiveLicenses.push(license);
              }
            });

            // Update threat
            component.effectiveLicenseThreat = InsightDatatable.getLicenseThreatLevelFromArray(component.effectiveLicenses);
          }
          component._formattedEffectiveLicenseThreat = component.effectiveLicenses.join(', ');
          component.status = licenseOverride ? getStatusName(licenseOverride.status) : 'Open';

          // Update Grid
          DataView.updateItem(SelectedComponent.id, component);
          // Update Summary Page
          InsightDatatable.updateSummary();
        }
      }

      function setOverrideScope(overrideScope) {
        $scope.override.licenseId = null;
        $scope.override.ownerId = overrideScope.ownerId;

        if (overrideScope.licenseOverride) {
          $scope.override.status = overrideScope.licenseOverride.status;

          if ($scope.override.status === 'OVERRIDDEN' || $scope.override.status === 'SELECTED') {
            $scope.override.licenseId = overrideScope.licenseOverride.licenseId;
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
        promises.push($http.get(CLM.path + 'rest/license'));
        // Current override state
        promises.push($http.get(CLM.path + 'rest/licenseOverride/application/' + ApplicationId.encoded() + '/applied/' +
            SelectedComponent.groupId + '/' + SelectedComponent.artifactId + '/' + SelectedComponent.version, {
          params: {
            'timestamp': new Date().getTime()
          }
        }));
        // Component licenses
        promises.push($http.get(CLM.path + 'rest/ci/component/details/licenses/' + ApplicationId.encoded(), {
          params: {
            'artifactId': SelectedComponent.artifactId,
            'groupId': SelectedComponent.groupId,
            'version': SelectedComponent.version,
            'timestamp': new Date().getTime()
          }
        }));

        $q.all(promises).then(function(results) {
          var licenses = results[0].data,
              currentOverride = results[1].data,
              component = results[2].data;

          $scope.component = component;
          $scope.licenses = {};
          angular.forEach(licenses, function(license) {
            $scope.licenses[license.id] = license;
          });

          $scope.hierarchy = currentOverride.licenseOverridesByOwner;
          $scope.reset();

          $scope.selectableLicenses = {};
          angular.forEach($scope.component.declaredlicenses, function(license) {
            $scope.selectableLicenses[license.license.licenseId] = $scope.licenses[license.license.licenseId];
          });
          angular.forEach($scope.component.observedlicenses, function(license) {
            $scope.selectableLicenses[license.license.licenseId] = $scope.licenses[license.license.licenseId];
          });

        }, function() {
          $scope.error = arguments[0];
        });
      };

      $scope.save = function() {
        $scope.saving = true;

        var licenseOverride = {
              id: null,
              ownerId: null,
              artifactId: SelectedComponent.artifactId,
              groupId: SelectedComponent.groupId,
              version: SelectedComponent.version,
              status: $scope.override.status.toUpperCase(),
              licenseId: null,
              comment: $scope.override.comment
            },
            owner = null;

        // Only set license for Override or Select states
        if (licenseOverride.status === 'OVERRIDDEN' || licenseOverride.status === 'SELECTED') {
          licenseOverride.licenseId = $scope.override.licenseId;
        }

        // Find owner
        owner = getHierarchyById($scope.override.ownerId);
        licenseOverride.ownerId = owner.ownerId;

        if (licenseOverride.status === 'DELETE') {
          hudson['delete'](CLM.path + 'rest/licenseOverride/application/' + licenseOverride.ownerId + '/' +
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
          hudson.post(CLM.path + 'rest/licenseOverride/' + owner.ownerType + '/' + owner.ownerId,
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
          licenseId: null
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

      $scope.getColor = function(threat) {
        if (threat === null) {
          return 'grey';
        }
        else if (threat > 7) {
          return 'red';
        }
        else if (threat > 3) {
          return 'orange';
        }
        else if (threat > 0) {
          return 'yellow';
        }
        else {
          return 'blue';
        }
      };

      // Remove license when changing away from Override/Selected status
      $scope.$watch('override.status', function(val) {
        if ($scope.override && val !== 'OVERRIDDEN' && val !== 'SELECTED') {
          $scope.override.licenseId = null;
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