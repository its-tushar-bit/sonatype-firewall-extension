/**
 * @license Copyright (c) 2013 Sonatype, Inc. All rights reserved. Includes the
 *          third-party code listed at
 *          http://links.sonatype.com/products/clm/attributions. "Sonatype" is a
 *          trademark of Sonatype, Inc.
 */
/* global angular, $, window, CLM, setTimeout, InsightDatatable, Insight, applicationId */
(function() {
  'use strict';

  function pad(str) {
    return ('' + str).length < 2 ? pad("0" + str, 2) : str;
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
    'Insight': {
      'ClaimComponent': function(node, applicationId, component) {
        function applyFocus() {
          if (node.find('input').length > 0) {
            node.find('input')[0].focus();
            return;
          }

          setTimeout(applyFocus, 100);
        }

        var timestamp = (new Date()).getTime(), container = $('<div clm-include="\'' + CLM.path +
            'cip/cip-claim-component.html\'"></div>');
        node.empty();
        container.appendTo(node);

        angular.module('claimComponent' + timestamp, []).service('CurrentData', function() {
          return angular.extend({
            createTime: component.lastModifiedEntryTime ? component.lastModifiedEntryTime : component.lastModifiedTime
          }, component);
        });
        angular.bootstrap(container[0], ['ClaimComponent', 'claimComponent' + timestamp, 'AngularCommon']);

        applyFocus();
      }
    }
  });

  var claimApp = angular.module('ClaimComponent', ['HttpInterceptors', 'UnauthenticatedResponseHttpInterceptor']);

  claimApp.controller('ClaimComponentController', [
    '$http', '$scope','CurrentData', 'Dialog', function($http, $scope, CurrentData, Dialog) {
      $scope.resetClaimData = function() {
        $scope.claimData = {};
        $scope.claimData.createTimeText = CurrentData.createTime ? dateToString(new Date(CurrentData.createTime)) : null;
        $scope.submitted = false;
        $scope.disableSubmit = false;

        if ($scope.claimForm) {
          //if the form has already been dirtied, reset its state
          $scope.claimForm.$setPristine();
        }
        // If we have previously claimed this component, use the stored values
        if(CurrentData.identificationSource === 'Manual') {
          angular.extend($scope.claimData, CurrentData);
          $scope.disableSubmit = true;
        }
      };

      var servicePath = CLM.path + 'rest/component/identified';

      var errorHandler = function(data, status, headersFn, config) {
        var header = headersFn();
        if (header['content-type'] && header['content-type'].indexOf('text/html') === 0) {
          $scope.createError = 'Server Error';
        }
        else if (status === 0) {
          $scope.errorResponse = 'Unable to connect to CLM server';
        }
        else {
          $scope.createError = data;
        }
        $scope.disableSubmit = false;
      };

      /**
       * Update the table data to match the updated model
       * @param {Object} newData
       */
      function updateDataView(newData, hash) {
        var dataView = InsightDatatable.getActiveTable().dataView;

        $.each(dataView.getItems(), function(index, item) {
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
        updateDataView({
          identificationSource: 'Manual',
          matchState: 'exact',
          groupId: data.groupId,
          artifactId: data.artifactId,
          version: data.version,
          classifier: data.classifier,
          extension: data.extension,
          createTime: data.createTime,
          age: establishAge(data.createTime),
          comment: data.comment
        }, CurrentData.hash);

        $scope.createSuccess = 'Component successfully claimed as ' + data.groupId + ':' + data.artifactId + ':' +
          data.version;
        $scope.resetClaimData();
      }

      /**
       * Decide how old the component is
       * @param {Object} data
       * @returns {number}
       */
      function establishAge(createTime) {
        return createTime ? Math.floor((new Date().getTime() - createTime) /
          (1000 * 60 * 60 * 24)) : null;
      }

      /**
       * Clear messages and update state to indicate we're submitting the form
       */
      function updateStateForSubmit() {
        $scope.createError = '';
        $scope.createSuccess = '';
        $scope.submitted = true;
      }

      /**
       * Inform the UI a submit is in process and fill in additional required data before submit.
       */
      function prepareForSubmit() {
        $scope.disableSubmit = true;
        $scope.claimData.hash = CurrentData.hash;
        if ($scope.claimData.createTimeText) {
          $scope.claimData.createTime = stringToDate($scope.claimData.createTimeText).getTime();
        }
      }

      /**
       * Claim the presently selected component
       */
      $scope.claimSubmit = function() {
        updateStateForSubmit();
        if ($scope.claimForm.$valid) {
          prepareForSubmit();
          $http.post(servicePath, $scope.claimData).success(function(data) {
            updateView(data);
          }).error(errorHandler);
        }
      };

      /**
       * Update the claim information for the presently selected component
       */
      $scope.claimUpdateSubmit = function() {
        updateStateForSubmit();
        if ($scope.claimForm.$valid) {
          prepareForSubmit();
          $http.put(servicePath, $scope.claimData).success(function(data) {
            updateView(data);
          }).error(errorHandler);
        }
      };

      /**
       * Remove(delete) an existing claim on a component
       */
      $scope.revokeClaimSubmit = function() {

        function deleteClaim() {
          updateStateForSubmit();
          $http.delete(servicePath + '/' + CurrentData.hash).success(function() {
            updateDataView({
              matchState: 'unknown',
              groupId: null,
              artifactId: null,
              version: null,
              classifier: null,
              extension: null,
              identificationSource: null,
              createTime: null,
              age: null,
              comment: null
            }, CurrentData.hash);
            $scope.createSuccess = 'Component claim has been revoked';
          }).error(errorHandler);
        }

        Dialog.open({
          title: 'Revoke Claim',
          body: 'Are you sure you want to revoke the claim on this component?' +
            ' This change will not be reflected until a new policy evaluation is triggered.',
          buttons: [
            {
              name: 'Cancel'
            },
            {
              name : 'Revoke',
              type : 'danger',
              click : deleteClaim
            }
          ]
        });
      };

      $scope.isClaimedComponent = function() {
        return CurrentData.identificationSource === 'Manual';
      };

      $scope.formValid = function() {
        var data = $scope.claimData;
        if (!data.groupId) {
          return false;
        }
        else if (!data.artifactId) {
          return false;
        }
        else if (!data.version) {
          return false;
        }
        return true;
      };

      $scope.getValidationMessage = function() {
        var claimForm = $scope.claimForm;
        if ($scope.submitted && (claimForm.groupId.$error.required || claimForm.artifactId.$error.required ||
            claimForm.version.$error.required)) {
          return 'Group ID, Artifact ID and Version are required';
        }
        else if (claimForm.createTimeText.$dirty && claimForm.createTimeText.$error.pattern) {
          return 'Date format is MM/DD/YYYY';
        }
      };

      $scope.resetClaimData();
    }
  ]);

  claimApp.directive('disablenav', function() {
    return function(scope, element, attrs) {
      element.bind("keydown.nav", function(e) {
        // 9 is tab, others are arrow keys
        if (e.keyCode === 9 || (e.keyCode >= 37 && e.keyCode <= 40)) {
          e.stopPropagation();
        }
      });
    };
  });

  claimApp.directive('clmDatepicker', function() {
    return function(scope, element, attrs) {
      element.datepicker({
        format: 'mm/dd/yyyy',
        autoclose: true,
        endDate: new Date(),
        clearBtn: true,
        forceParse: false
      }).on('changeDate', function(event) {
        scope.$apply(function() {
          scope.claimData.createTimeText = dateToString(event.date);
          scope.claimForm.$setDirty();
        });
      });
      element.datepicker('update', scope.claimData.createTimeText);
    };
  });
}());

/* add claim component tab as an information panel plugin */
(function() {
  "use strict";

  function doLoad() {
    function ClaimComponentTab(node, options) {
      this.node = node;
      this.options = options;
    }

    ClaimComponentTab.prototype = new Insight.InformationPanelPlugin({ priority: 128 });

    ClaimComponentTab.prototype.isVisible = function() {
      return this.gav.matchState !== 'exact' || this.gav.identificationSource === 'Manual';
    };

    ClaimComponentTab.prototype.create = function() {
      var timestamp = (new Date()).getTime(), container = $('<div id="claim-component-' + timestamp +
          '"></div>'), me = this,
        retry = function() {
        if (Insight.ClaimComponent) {
          Insight.ClaimComponent(container, applicationId, me.gav);
        }
        else {
          setTimeout(retry, 1000);
        }
      };
      this.node.empty();
      container.appendTo(this.node);

      retry();
    };

    ClaimComponentTab.prototype.destroy = function() {
      var nodeEl = $(this.node).find('.claimComponent');
      nodeEl.on('$destroy', function(event) {
        nodeEl.scope().$destroy();
      });
      this.node.empty();
    };

    ClaimComponentTab.prototype.getTitle = function() {
      return 'Claim Component';
    };

    Insight.InformationPanelPlugins.push(ClaimComponentTab);
  }

  function check() {
    if (window.Insight && window.Insight.InformationPanelPlugin) {
      doLoad();
    }
    else {
      setTimeout(check, 100);
    }
  }

  setTimeout(check, 0);
}());