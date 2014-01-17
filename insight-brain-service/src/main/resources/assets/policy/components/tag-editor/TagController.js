/**
 * @license Copyright (c) 2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular, $, clmBuildTimestamp */
(function() {
  'use strict';

  var tagModule = angular.module('Tags', ['AngularCommon', 'CLMAppLocation', 'CommonServices', 'ResourceModule']);

  tagModule.service('TagStore', [
    'CLMResource', 'CLMAppLocations', function(CLMResource, CLMAppLocations) {
      var tagStore = null, tagStores = {};

      function refreshTagStore() {
        var entityId = CLMAppLocations.getEntityId();
        tagStore = tagStores[entityId];
        if (!tagStore) {
          tagStore = tagStores[entityId] = CLMResource.getStore(angular.extend({ url: CLMAppLocations.getTagsUrl() },
            tagStoreTemplate));
        }
      }

      var tagStoreTemplate = {
        template: {id: null, organizationId: null, name: null, description: null},
        params: {
          timestamp: new Date().getTime()
        }
      };

      return {
        get: function() {
          refreshTagStore();
          return tagStore.get();
        },
        refresh: function() {
          refreshTagStore();
          return tagStore.refresh();
        },
        create: function() {
          return tagStore.create();
        }
      };
    }
  ]);

  function showAlert(alerts, alert) {
    alerts.length = 0;
    alerts.push(alert);
  }

  tagModule.controller('TagController', [
    '$scope', '$http', '$q', 'CLMAppLocations', 'Messages', 'CLMResource', 'TagStore', 'ownerChange', 'Dialog',
    function($scope, $http, $q, clmAppLocations, messages, clmResource, TagStore, ownerChange, Dialog) {
      $scope.alerts = [];

      function deselect() {
        if ($scope.selectedTag) {
          $scope.selectedTag.$revert();
        }
        $scope.selectedTag = null;
        $scope.submitActive = false;
        $scope.alerts.length = 0;
      }

      function executeIfClean(fn) {
        if ($scope.selectedTag && $scope.selectedTag.isDirty()) {
          showAlert($scope.alerts, {
            type: 'error',
            msg: 'Please finish editing before trying to modify another tag.'
          });
        }
        else {
          fn();
        }
      }

      $scope.deselect = deselect;

      $scope.doLoad = function() {
        $scope.error = null;
        TagStore.refresh().then(function(results) {
          $scope.tags = results;
        }, function(error) {
          $scope.error = error;
        });
      };

      $scope.$on('ownerChanged', ownerChange.getEventHandler($scope, 'tags'));
      $scope.$on('refresh', $scope.doLoad);

      $scope.editTag = function(tag) {
        executeIfClean(function() {
          deselect();
          $scope.selectedTag = tag.$clone();
        });
      };

      $scope.createNew = function() {
        executeIfClean(function() {
          $scope.selectedTag = TagStore.create();
        });
      };

      $scope.deleteTag = function(tag, $event) {
        $event.stopPropagation();
        Dialog.open({
          title: 'Delete Tag',
          body: 'Are you sure you want to delete this tag?',
          buttons: [
            {
              name: 'Cancel'
            },
            {
              name: 'Delete',
              type: 'danger',
              click: function() {
                tag.$delete().then(function() {
                  if ($scope.selectedTag && label.id === $scope.selectedTag.id) {
                    $scope.selectedTag = null;
                  }
                }, function(error) {
                  showAlert($scope.alerts, {
                    type: 'error',
                    msg: 'An error occurred while deleting the tag ' + tag.name + '. (' +
                      messages.getHttpErrorMessage(error) + ')'
                  });
                });
              }
            }
          ]
        });
      };

      $scope.$on('tags.cancelEditTag', function(event, tag) {
        event.stopPropagation();
        deselect();
      });

      $scope.doLoad();
    }
  ]);

  tagModule.controller('TagEditorController', ['$scope', 'Messages', function($scope, messages) {
      function errorFn(error) {
        $scope.submitActive = false;
        showAlert($scope.editorAlerts, {
          type: 'error',
          msg: 'An error occurred while saving the tag. (' +
            messages.getHttpErrorMessage(error) + ')'
        });
      }

      $scope.editorAlerts = [];

      $scope.cancelEditTag = function() {
        $scope.$emit('tags.cancelEditTag');
      };

      $scope.canSaveEdit = function(valid, tag) {
        return valid && !$scope.submitActive && tag != null && tag.name && tag.description;
      };

      $scope.$on('pageChangeStarted', function(event) {
        if ($scope.selectedTag) {
          if ($scope.selectedTag.isDirty()) {
            event.preventDefault();
          }
        }
      });

      $scope.saveTag = function() {
        $scope.submitActive = true;
        $scope.selectedTag.$save().then(function(tag) {
          $scope.deselect();
        }, errorFn);
      };
    }
  ]);

  tagModule.controller('TagApplicationController', ['$scope', '$http', '$q', 'CLMLocations', 'selectedApplication', 'Messages',
    function($scope, $http, $q, CLMLocations, selectedApplication, Messages) {
      $scope.alerts = [];
      var promises = [ $http.get(CLMLocations.getOrganizationTagUrl(selectedApplication.organizationId)),
                       $http.get(CLMLocations.getApplicationTagUrl(selectedApplication.publicId)) ];

      $scope.doLoad = function() {
        $scope.error = null;
        $q.all(promises).then(function(results) {
          var organizationTags = results[0].data;
          var applicationTags = results[1].data;
          var tags = [];

          for (var i = 0; i < organizationTags.length; i++) {
            var organizationTag = organizationTags[i];
            organizationTag.isApplied = false;
            for (var j = 0; j < applicationTags.length; j++) {
              var applicationTag = applicationTags[j];
              if (organizationTag.id === applicationTag.id) {
                organizationTag.isApplied = true;
                applicationTags.splice(j, 1);
                break;
              }
            }
            tags.push(organizationTag);
          }

          $scope.tags = tags;
        }, function (error) {
          $scope.error = error;
        });
      };

      $scope.toggleApply = function(tag) {
        if (tag.isApplied) {
          $http['delete'](CLMLocations.getDeleteApplicationTagUrl(selectedApplication.publicId, tag.id)).success(function() {
            tag.isApplied = false;
          }).error(function() {
            showAlert($scope.alerts, {
              type: 'error',
              msg: 'An error occurred while detaching the tag ' + tag.name + '. (' +
                Messages.getHttpErrorMessage(arguments) + ')'
            });
          });
        } else {
          $http.post(CLMLocations.getApplicationTagUrl(selectedApplication.publicId), tag).success(function() {
            tag.isApplied = true;
          }).error(function() {
            showAlert($scope.alerts, {
              type: 'error',
              msg: 'An error occurred while applying the tag ' + tag.name + '. (' +
                Messages.getHttpErrorMessage(arguments) + ')'
            });
          });
        }
      };

      $scope.doLoad();
  }]);
}());
