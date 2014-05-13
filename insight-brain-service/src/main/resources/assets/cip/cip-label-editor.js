/**
 * @license Copyright (c) 2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular, $, window, CLM, setTimeout, Insight, AngularUtils, applicationId */
(function() {
  'use strict';
  function LabelTab(node, options) {
    this.node = node;
    this.options = options;
  }

  function createPlugin() {
    LabelTab.prototype = new Insight.InformationPanelPlugin({ priority: 112 });
    LabelTab.prototype.isVisible = function() {
      return this.gav.matchState !== 'unknown';
    };
    LabelTab.prototype.create = function() {
      var timestamp = new Date().getTime(),
          container = $('<div clm-include="\'' + CLM.path + 'cip/cip-label-editor.html\'"></div>'),
          me = this;
      me.node.empty();
      container.appendTo(this.node);
      angular.module('componentProvider' + timestamp, []).service('ComponentLabelEditorGAV', function() {
        return angular.extend({ applicationId: applicationId }, angular.copy(me.gav));
      });
      angular.bootstrap(container[0], ['ComponentLabelEditor', 'componentProvider' + timestamp, 'AngularCommon', 'ui.bootstrap']);
    };
    LabelTab.prototype.destroy = function() {
      this.node.empty();
    };
    LabelTab.prototype.getTitle = function() {
      return 'Labels';
    };

    return LabelTab;
  }

  function relocateModal(selector) {
    $("body > " + selector).remove();
    $(selector).appendTo("body");
  }

  (function() {
    //create the app, and a service we can use to transfer data between our controllers
    var labelsApp = angular.module('ComponentLabelEditor', ['CommonServices', 'HttpInterceptors', 'UnauthenticatedResponseHttpInterceptor']).service('CurrentLabelData',
        function() {
          var currentLabel = null,
              currentError = null;
          return {
            get: function() {
              return currentLabel;
            },
            set: function(label) {
              currentLabel = label;
            },
            getError: function() {
              return currentError;
            },
            setError: function(error) {
              currentError = error;
            }
          };
        });
    //the add controller, controlling the add modal
    labelsApp.controller('LabelAddController', [
      '$scope', 'CurrentLabelData', 'ComponentLabelEditorGAV', 'Messages', '$http',
      function($scope, currentLabelData, componentLabelEditorGAV, messages, $http) {
        $scope.groupId = componentLabelEditorGAV.groupId;
        $scope.artifactId = componentLabelEditorGAV.artifactId;
        $scope.version = componentLabelEditorGAV.version;
        //decline to add, just dump the modal and move on
        $scope.decline = function() {
          $('#labelAssignScopeModal').modal('hide');
        };
        //they accept, update the server
        $scope.accept = function() {
          $scope.labelSaving = true;
          $scope.labelAddError = null;
          var parts = $scope.label.selectedOwner.split('$$');
          $http.post(CLM.path + 'rest/label/component/' + parts[1] + '/' + parts[0] + '/' +
                  componentLabelEditorGAV.hash, currentLabelData.get()).success(function(responseData) {
            $scope.labelSaving = false;
            $('#labelAssignScopeModal').modal('hide');
          }).error(function(data, status, headersFn, config) {
                $scope.labelSaving = false;
                $scope.labelAddError = messages.getHttpErrorMessage(arguments);
              });
        };
        //after dialog is shown, make sure to apply the angular stuff
        $('#labelAssignScopeModal').on('shown', function() {
          AngularUtils.safeApply($scope, function() {
            $scope.labelLoading = true;
            $scope.labelAddError = null;
            var label = currentLabelData.get();
            $scope.label = {
              selectedOwner: componentLabelEditorGAV.applicationId + '$$application'
            };
            $scope.labelOwners = [];
            $http.get(CLM.path + 'rest/label/' + label.ownerType + '/' + label.ownerId + '/applicable/context/' +
                    label.id).success(function(data) {
              $scope.labelLoading = false;
              function processItem(item) {
                if (item.type === 'application' && item.id === componentLabelEditorGAV.applicationId) {
                  $scope.labelOwners.splice(0, 0, item);
                }
                else if (item.type === 'organization') {
                  $scope.labelOwners.push(item);
                  angular.forEach(item.children, function(child, childIndex) {
                    processItem(child);
                  });
                }
              }

              processItem(data);
            }).error(function(data, status) {
                  $scope.labelLoading = false;
                  $scope.labelAddError = messages.getHttpErrorMessage(arguments);
                });
          });
        });
        //move the dialog onto the body in the dom, so the backdrop shows properly
        relocateModal('#labelAssignScopeModal');
      }
    ]);
    //the remove controller, controlling the remove modal
    labelsApp.controller('LabelRemoveController', [
      '$scope', '$http', 'CurrentLabelData', 'ComponentLabelEditorGAV', 'Messages',
      function($scope, $http, currentLabelData, componentLabelEditorGAV, messages) {
        //decline to remove, just dump the dialog
        $scope.decline = function() {
          $('#labelRemoveModal').modal('hide');
        };
        //accept, send delete request to server
        $scope.accept = function() {
          $scope.labelDeleting = true;
          $scope.labelRemoveError = null;
          var label = currentLabelData.get();
          $http['delete'](CLM.path + 'rest/label/component/' + label.ownerType + '/' + label.ownerId + '/' +
                  componentLabelEditorGAV.hash + '/' + label.id).success(function(responseData) {
            $scope.labelDeleting = false;
            $('#labelRemoveModal').modal('hide');
          }).error(function(data, status, headersFn, config) {
                $scope.labelDeleting = false;
                $scope.labelRemoveError = messages.getHttpErrorMessage(arguments);
              });
        };
        $('#labelRemoveModal').on('show', function() {
          AngularUtils.safeApply($scope, function() {
            $scope.labelRemoveError = null;
          });
        });
        //move the dialog onto the body in the dom, so the backdrop shows properly
        relocateModal('#labelRemoveModal');
      }
    ]);
    //main label controller handling the main view, and launching the other modals when necessary
    labelsApp.controller('LabelsController', [
      '$http', '$scope', 'CurrentLabelData', 'ComponentLabelEditorGAV', 'Messages',
      function($http, $scope, currentLabelData, componentLabelEditorGAV, messages) {
        function errorFn(data, status, headersFn, config) {
          $scope.alerts.length = 0;
          $scope.alerts.push({
            type: 'error',
            msg: messages.getHttpErrorMessage(arguments)
          });
        }

        function flattenLabelList(data) {
          var list = [];
          angular.forEach(data.labelsByOwner, function(labelOwner, labelOwnerIndex) {
            angular.forEach(labelOwner.labels, function(label, labelIndex) {
              label.ownerId = labelOwner.ownerId;
              label.ownerType = labelOwner.ownerType;
              label.ownerName = labelOwner.ownerName;
              list.push(label);
            });
          });
          return list;
        }

        function reloadLabels() {
          $http.get(CLM.path + 'rest/label/component/application/' + componentLabelEditorGAV.applicationId + '/' +
              componentLabelEditorGAV.hash).success(function(data) {
            $scope.itemLabels = flattenLabelList(data);
          }).error(errorFn);
        }
        function reloadAppLabels() {
          $http.get(CLM.path + 'rest/label/application/' + componentLabelEditorGAV.applicationId + '/applicable').success(function(data) {
            $scope.availableLabels = flattenLabelList(data);
          }).error(errorFn);
        }
        $scope.loadLabelData = function() {
          reloadLabels();
          reloadAppLabels();
        };
        $scope.removeLabel = function(label) {
          currentLabelData.set(label);
          $('#labelRemoveModal').modal('show');
        };
        //for labels owned by the app, we simply do the add here, as there is no need to view the dialog to select the owner, app is the only option
        $scope.addLabel = function(label) {
          if (label.ownerType === 'application') {
            $http.post(CLM.path + 'rest/label/component/application/' + componentLabelEditorGAV.applicationId + '/' +
                    componentLabelEditorGAV.hash, label).success(function(responseData) {
              $scope.loadLabelData();
            }).error(errorFn);
          }
          else {
            currentLabelData.set(label);
            $('#labelAssignScopeModal').modal('show');
          }
        };
        $scope.isWhite = function(label) {
          return label.color === "green" || label.color === "black" || label.color === "orange" ||
              label.color === "red" || label.color === "blue";
        };
        $scope.isApplied = function(label) {
          var duplicate = false;
          angular.forEach($scope.itemLabels, function(candidate, key) {
            duplicate = duplicate || (candidate.label === label.label);
            return !duplicate;
          });
          return !duplicate;
        };
        $scope.alerts = [];
        $scope.loadLabelData(); // do initial load
        //when either of the modals go away, refresh the content
        $('#labelAssignScopeModal').on('hide', function() {
          $scope.loadLabelData();
        });
        $('#labelRemoveModal').on('hide', function() {
          $scope.loadLabelData();
        });
      }
    ]);
    /**
     * Enables tipsy tooltip on an element(with fixed parameters)
     */
    labelsApp.directive('tip', function() {
      return function(scope, element, attrs) {
        $(element).tipsy({fade: true, gravity: $.fn.tipsy.autoWE, html: true, opacity: 1.0, delayOut: 0});
      };
    });
    labelsApp.directive('spinner', function() {
      var properties = ['-ms-transform', '-webkit-transform', '-moz-transform', 'transform'];

      function setElement(element, value) {
        angular.forEach(properties, function(prop, key) {
          element.css(prop, value);
        });
        return element;
      }

      return function(scope, element, attrs) {
        element.bind('click', function(e) {
          setElement(element, '').prop('rotate', null).animate({ rotate: '+360'}, {
            step: function(now, fx) {
              now = now % 360;
              setElement(element, 'rotate(' + now + 'deg)');
            }
          });
        });
      };
    });
  }());

  CLM.loadPlugin(createPlugin, 'Labels');
}());