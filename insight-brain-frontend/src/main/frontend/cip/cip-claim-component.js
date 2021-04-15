/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { pick } from 'ramda';
import claimComponentTemplate from './cip-claim-component.html';

import legacyConfigurationModule from '../LegacyConfigurationModule';
/* global angular, $, window, CLM, setTimeout, InsightDatatable, Insight, applicationId */

function pad(str) {
  return ('' + str).length < 2 ? pad('0' + str, 2) : str;
}

function dateToString(date) {
  if (!date) {
    return null;
  }

  return pad(date.getMonth() + 1) + '/' + pad(date.getDate()) + '/' + date.getFullYear();
}

function stringToDate(str) {
  if (!str) {
    return null;
  }

  var parts = str.split('/');

  if (parts.length !== 3) {
    return null;
  }

  return new Date(parts[2], parts[0] - 1, parts[1]);
}

$.extend(true, window, {
  Insight: {
    ClaimComponent: function (node, applicationId, component) {
      function applyFocus() {
        if (node.find('input').length > 0) {
          node.find('input')[0].focus();
          return;
        }

        setTimeout(applyFocus, 100);
      }

      var timestamp = new Date().getTime(),
        container = $(claimComponentTemplate);
      node.empty();
      container.appendTo(node);

      angular.module('claimComponent' + timestamp, []).service('CurrentData', function () {
        return angular.extend(
          {
            createTime: component.lastModifiedEntryTime ? component.lastModifiedEntryTime : component.lastModifiedTime,
          },
          component
        );
      });
      angular.bootstrap(container[0], [
        'ClaimComponent',
        'claimComponent' + timestamp,
        'AngularCommon',
        legacyConfigurationModule.name,
      ]);

      applyFocus();
    },
  },
});

var claimApp = angular.module('ClaimComponent', [
  'HttpInterceptors',
  'UnauthenticatedResponseHttpInterceptor',
  'ComponentUtils',
]);

claimApp.controller('ClaimComponentController', [
  '$http',
  '$scope',
  'CurrentData',
  'Dialog',
  'ComponentUtil',
  function ($http, $scope, CurrentData, Dialog, ComponentUtil) {
    $scope.resetClaimData = function () {
      $scope.claimData = {};
      $scope.claimData.createTimeText = CurrentData.createTime ? dateToString(new Date(CurrentData.createTime)) : null;
      $scope.submitted = false;
      $scope.disableSubmit = false;

      if ($scope.claimForm) {
        //if the form has already been dirtied, reset its state
        $scope.claimForm.$setPristine();
      }
      // If we have previously claimed this component, use the stored values
      if (CurrentData.identificationSource === 'Manual') {
        angular.extend($scope.claimData, CurrentData);
        $scope.disableSubmit = true;
      }
    };

    var servicePath = CLM.path + 'rest/component/identified';

    var errorHandler = function (errorResponse) {
      var header = errorResponse.headers();
      if (header['content-type'] && header['content-type'].indexOf('text/html') === 0) {
        $scope.createError = 'Server Error';
      } else if (status === 0) {
        $scope.createError = 'Unable to connect to IQ Server';
      } else {
        $scope.createError = errorResponse.data;
      }
      $scope.disableSubmit = false;
    };

    /**
     * Update the table data to match the updated model
     * @param {Object} newData
     */
    function updateDataView(newData, hash) {
      var dataView = InsightDatatable.getActiveTable().dataView;

      $.each(dataView.getItems(), function (index, item) {
        if (item.hash === hash) {
          dataView.beginUpdate();
          dataView.updateItem(item.id, $.extend({}, item, newData));
          dataView.endUpdate();
        }
      });
    }

    /**
     * Update the data table and inform the UI after claiming a component.
     * @param {Object} data
     */
    function updateView(data) {
      //TODO remove once we can claim other formats https://issues.sonatype.org/browse/CLM-3719
      angular.extend(data, {
        groupId: data.componentIdentifier.coordinates.groupId,
        artifactId: data.componentIdentifier.coordinates.artifactId,
        version: data.componentIdentifier.coordinates.version,
        classifier: data.componentIdentifier.coordinates.classifier,
        extension: data.componentIdentifier.coordinates.extension,
      });
      updateDataView(
        {
          identificationSource: 'Manual',
          matchState: 'exact',
          groupId: data.groupId,
          artifactId: data.artifactId,
          version: data.version,
          classifier: data.classifier,
          extension: data.extension,
          createTime: data.createTime,
          age: establishAge(data.createTime),
          comment: data.comment,
          coordinates: data.coordinates,
          displayName: data.displayName,
          componentIdentifier: data.componentIdentifier,
        },
        CurrentData.hash
      );

      $scope.resetClaimData();
    }

    /**
     * Decide how old the component is
     * @param {Object} data
     * @returns {number}
     */
    function establishAge(createTime) {
      return createTime ? Math.floor((new Date().getTime() - createTime) / (1000 * 60 * 60 * 24)) : null;
    }

    /**
     * Clear messages and update state to indicate we're submitting the form
     */
    function updateStateForSubmit() {
      $scope.createError = '';
      $scope.submitted = true;
    }

    /**
     * Inform the UI a submit is in process and fill in additional required data before submit.
     */
    function prepareForSubmit() {
      $scope.disableSubmit = true;
      $scope.claimData.hash = CurrentData.hash;
      //TODO remove once we can claim other formats https://issues.sonatype.org/browse/CLM-3719
      angular.extend($scope.claimData, {
        componentIdentifier: {
          format: 'maven',
          coordinates: {
            groupId: $scope.claimData.groupId,
            artifactId: $scope.claimData.artifactId,
            version: $scope.claimData.version,
            // as classifier is optional, we want to enforce an empty string when the user has not
            // touched the field
            classifier: $scope.claimData.classifier ? $scope.claimData.classifier : '',
            extension: $scope.claimData.extension,
          },
        },
      });
      if ($scope.claimData.createTimeText) {
        $scope.claimData.createTime = stringToDate($scope.claimData.createTimeText).getTime();
      }
    }

    /**
     * filter out transient properties from the claimData to make it acceptable for a REST body
     */
    function getClaimDataForServer() {
      return pick(['createTime', 'id', 'componentIdentifier', 'comment', 'hash'], $scope.claimData);
    }

    /**
     * Claim the presently selected component
     */
    $scope.claimSubmit = function () {
      updateStateForSubmit();
      if ($scope.claimForm.$valid) {
        prepareForSubmit();
        $http.post(servicePath, getClaimDataForServer()).then(function (response) {
          updateView(response.data);
        }, errorHandler);
      }
    };

    /**
     * Update the claim information for the presently selected component
     */
    $scope.claimUpdateSubmit = function () {
      updateStateForSubmit();
      if ($scope.claimForm.$valid) {
        prepareForSubmit();
        $http.put(servicePath, getClaimDataForServer()).then(function (response) {
          updateView(response.data);
        }, errorHandler);
      }
    };

    /**
     * Remove(delete) an existing claim on a component
     */
    $scope.revokeClaimSubmit = function () {
      function deleteClaim() {
        updateStateForSubmit();
        $http.delete(servicePath + '/' + CurrentData.hash).then(function () {
          // fall back on the display name
          ComponentUtil.setDisplayNameAndCoordinates(CurrentData);
          updateDataView(
            {
              matchState: 'unknown',
              groupId: null,
              artifactId: null,
              version: null,
              classifier: null,
              extension: null,
              identificationSource: null,
              createTime: null,
              age: null,
              comment: null,
              coordinates: CurrentData.coordinates,
              displayName: CurrentData.displayName,
              componentIdentifier: null,
            },
            CurrentData.hash
          );
        }, errorHandler);
      }

      Dialog.open({
        title: 'Revoke Claim',
        body:
          'Are you sure you want to revoke the claim on this component?' +
          ' This change will not be reflected until a new policy evaluation is triggered.',
        buttons: [
          {
            name: 'Revoke',
            type: 'primary',
            click: deleteClaim,
          },
          {
            name: 'Cancel',
            type: 'cancel',
          },
        ],

        // NOTE: temporarily prevent this dialog from using the new iq-modal styles until those styles are made
        // compatible with the version of bootstrap used within the reports
        windowClass: null,
        backdropClass: null,
      });
    };

    $scope.isClaimedComponent = function () {
      return CurrentData.identificationSource === 'Manual';
    };

    $scope.formValid = function () {
      var data = $scope.claimData;
      if (!data.groupId) {
        return false;
      } else if (!data.artifactId) {
        return false;
      } else if (!data.version) {
        return false;
      } else if (!data.extension) {
        return false;
      }
      return true;
    };

    $scope.getValidationMessage = function () {
      var claimForm = $scope.claimForm;
      if (
        $scope.submitted &&
        (claimForm.groupId.$error.required ||
          claimForm.artifactId.$error.required ||
          claimForm.version.$error.required ||
          claimForm.extension.$error.required)
      ) {
        return 'Group ID, Artifact ID, Version and Extension are required';
      } else if (claimForm.createTimeText.$dirty && claimForm.createTimeText.$error.pattern) {
        return 'Date format is MM/DD/YYYY';
      }
    };

    $scope.resetClaimData();
  },
]);

claimApp.directive('disablenav', function () {
  return function (scope, element, attrs) {
    element.bind('keydown.nav', function (e) {
      // 9 is tab, others are arrow keys
      if (e.keyCode === 9 || (e.keyCode >= 37 && e.keyCode <= 40)) {
        e.stopPropagation();
      }
    });
  };
});

claimApp.directive('clmDatepicker', function () {
  return function (scope, element, attrs) {
    element
      .datepicker({
        format: 'mm/dd/yyyy',
        autoclose: true,
        endDate: new Date(),
        clearBtn: true,
        forceParse: false,
      })
      .on('changeDate', function (event) {
        scope.$apply(function () {
          scope.claimData.createTimeText = dateToString(event.date);
          scope.claimForm.$setDirty();
        });
      });
    element.datepicker('update', scope.claimData.createTimeText);
  };
});

/* add claim component tab as an information panel plugin */
(function () {
  'use strict';

  function doLoad() {
    function ClaimComponentTab(node, options) {
      this.node = node;
      this.options = options;
    }

    ClaimComponentTab.prototype = new Insight.InformationPanelPlugin({
      priority: 128,
    });

    ClaimComponentTab.prototype.isVisible = function () {
      return this.gav.matchState !== 'exact' || this.gav.identificationSource === 'Manual';
    };

    ClaimComponentTab.prototype.create = function () {
      var timestamp = new Date().getTime(),
        container = $('<div id="claim-component-' + timestamp + '"></div>'),
        me = this,
        retry = function () {
          if (Insight.ClaimComponent) {
            Insight.ClaimComponent(container, applicationId, me.gav);
          } else {
            setTimeout(retry, 1000);
          }
        };
      this.node.empty();
      container.appendTo(this.node);

      retry();
    };

    ClaimComponentTab.prototype.destroy = function () {
      var nodeEl = $(this.node).find('.claimComponent');
      nodeEl.on('$destroy', function (event) {
        nodeEl.scope().$destroy();
      });
      this.node.empty();
    };

    ClaimComponentTab.prototype.getTitle = function () {
      return 'Claim Component';
    };

    Insight.InformationPanelPlugins.push(ClaimComponentTab);
  }

  function check() {
    if (window.Insight && window.Insight.InformationPanelPlugin) {
      doLoad();
    } else {
      setTimeout(check, 100);
    }
  }

  setTimeout(check, 0);
})();

export { claimApp as ClaimComponentModule };
