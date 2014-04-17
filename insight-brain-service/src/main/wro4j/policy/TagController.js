/**
 * @license Copyright (c) 2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular, AngularStateUtils */
(function() {
  'use strict';

  var tagTemplate = {id: null, organizationId: null, name: null, description: null, color:'white'};

  var tagModule = angular.module('Tags', ['AngularCommon', 'CLMAppLocation', 'CLMLocation', 'CommonServices', 'ResourceModule', 'Stores']);

  tagModule.service('TagStore', [
    'CachedStore', 'CLMAppLocations', 'CLMLocations', '$http', function(CachedStore, CLMAppLocations, CLMLocations, $http) {
      var tagStoreTemplate = {
        getUrl: CLMAppLocations.getTagsUrl,
        template: tagTemplate
      };
      var tagStores = CachedStore.get(tagStoreTemplate);

      return angular.extend(tagStores, {
        getApplied: function(){
          return $http.get(CLMLocations.getOrganizationAppliedTagUrl(CLMAppLocations.getEntityId()));
        }
      });
    }
  ]);

  tagModule.service('PolicyTagStore', ['$http', 'CachedStore', 'CLMAppLocations', 'CLMLocations',
    function($http, CachedStore, CLMAppLocations, CLMLocations) {
    var policyId, policyTagTemplate = {
      getKey: function() { return policyId; },
      getUrl: function() { return CLMAppLocations.getPolicyTagUrl(policyId); },
      template: tagTemplate
    };
    var store = CachedStore.get(policyTagTemplate);
    return {
      getByPolicyId: function(id) {
        policyId = id;
        return store;
      },
      getApplied: function() {
        return $http.get(CLMLocations.getOrganizationPolicyTagUrl(CLMAppLocations.getEntityId()));
      }
    };
  }]);

  function showAlert(alerts, alert) {
    alerts.length = 0;
    alerts.push(alert);
  }

  tagModule.controller('TagController', [
    '$scope', '$http', '$q', 'CLMAppLocations', 'Messages', 'CLMResource', 'TagStore', 'ownerChange', 'Dialog', 'ApplicationStore', 'PolicyStore', 'PolicyTagStore',
    function($scope, $http, $q, clmAppLocations, messages, clmResource, TagStore, ownerChange, Dialog, ApplicationStore, PolicyStore, PolicyTagStore) {
      $scope.alerts = [];
      $scope.colors = ['white', 'grey', 'black', 'green', 'yellow', 'orange', 'red', 'blue'];

      function deselect() {
        if ($scope.selectedTag) {
          $scope.selectedTag.$revert();
        }
        $scope.selectedTag = null;
        $scope.submitActive = false;
        $scope.alerts.length = 0;
      }

      function showEditingAlert() {
        return Dialog.open({
          title: 'Unsaved Changes',
          body: 'This tag may contain unsaved changes, continuing will discard them.',
          buttons: [
            {
              name: 'Cancel',
              dismiss: true
            },
            {
              name : 'Continue',
              type : 'danger'
            }
          ]
        }).result;
      }

      $scope.deselect = deselect;

      $scope.doLoad = function() {
        $scope.error = null;
        $scope.tags = null;
        $q.all([TagStore.refresh(), TagStore.getApplied(), ApplicationStore.get(), PolicyTagStore.getApplied(), PolicyStore.get().then(function(store) {return store.get();})]).then(function(results) {
          $scope.tags = results[0];
          $scope.appliedTags = results[1].data;
          $scope.applications = results[2];
          var mappedApplications = {};
          angular.forEach($scope.applications, function(application){
            mappedApplications[application.id] = application.name;
          });
          var mappedPolicies = {};
          var policyTags = results[3].data;
          var policies = results[4];
          angular.forEach(policies, function(policy) {
            mappedPolicies[policy.id] = policy.name;
          });
          var mappedTags = {};
          angular.forEach($scope.tags, function(tag){
            tag.appliedTags = [];
            tag.policies = [];
            mappedTags[tag.id] = tag;
          });
          angular.forEach($scope.appliedTags, function(ApplicationTag){
            var appliedTags = mappedTags[ApplicationTag.tagId].appliedTags;
            ApplicationTag.applicationName = mappedApplications[ApplicationTag.applicationId];
            appliedTags.push(ApplicationTag);
          });
          angular.forEach(policyTags, function(policyTag) {
            var policies = mappedTags[policyTag.tagId].policies;
            policyTag.policyName = mappedPolicies[policyTag.policyId];
            policies.push(policyTag);
          });
        }, function(error) {
          $scope.error = error;
        });
      };

      $scope.$on('ownerChanged', ownerChange.getEventHandler($scope, 'tags'));
      $scope.$on('refresh', $scope.doLoad);

      $scope.editTag = function(tag) {
        var e = $scope.$broadcast('tagChangeStarted');
        if (!e.defaultPrevented) {
          deselect();
          $scope.selectedTag = tag.$clone();
        } else {
          showEditingAlert().then(function() {
            $scope.selectedTag = tag.$clone();
          });
        }
      };

      $scope.setColor = function(color) {
        $scope.selectedTag.color = color;
      };

      $scope.createNew = function() {
        var e = $scope.$broadcast('tagChangeStarted');
        if (!e.defaultPrevented) {
          $scope.selectedTag = TagStore.create();
          AngularStateUtils.toNewItemState($scope);
        } else {
          showEditingAlert().then(function() {
            $scope.selectedTag = TagStore.create();
            AngularStateUtils.toNewItemState($scope);
          });
        }
      };

      $scope.deleteTag = function(tag, $event) {
        $event.stopPropagation();
        var body;
        var dialogOptions = {
          title: 'Delete Tag',
          buttons: [
            {
              name: 'Cancel'
            }
          ]
        };
        var policyLength = tag.policies ? tag.policies.length : 0;
        // If there are policies associated with a tag, prevent the user from deleting the tag
        if (policyLength > 0) {
          body = 'You cannot delete this tag because it is associated with the following policies: ';
          body += jQuery.map(tag.policies, function(policy) {
            return policy.policyName;
          }).join(', ') + '.';
        // Else notify the user if there are associated applications and allow deletion
        } else {
          body = 'Are you sure you want to delete this tag?';
          var appliedTagsLength = tag.appliedTags ? tag.appliedTags.length : 0;
          if (appliedTagsLength > 0) {
            body += ' It is in use by the following applications: ';
            body += jQuery.map(tag.appliedTags, function(ApplicationTag){
              return ApplicationTag.applicationName;
            }).join(', ') + '.';
          }
          dialogOptions.buttons.push({
            name: 'Delete',
            type: 'danger',
            click: function() {
              tag.$delete().then(function() {
                if ($scope.selectedTag && tag.id === $scope.selectedTag.id) {
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
          });
        }
        dialogOptions.body = body;
        Dialog.open(dialogOptions);
      };

      $scope.$on('tags.cancelEditTag', function(event) {
        event.stopPropagation();
        deselect();
        AngularStateUtils.toParentStateIfNewItem($scope);
      });
      
      AngularStateUtils.fnOnNewItemState($scope, $scope.createNew);

      $scope.doLoad();
    }
  ]);

  tagModule.controller('TagEditorController', ['$scope', '$http', '$q', 'Messages',
    function($scope, $http, $q, messages) {
      function errorFn(error) {
        $scope.submitActive = false;
        showAlert($scope.editorAlerts, {
          type: 'error',
          msg: 'An error occurred while saving the tag. (' +
            messages.getHttpErrorMessage(error) + ')'
        });
      }

      function isEditing() {
        if ($scope.selectedTag) {
          return $scope.selectedTag.isDirty();
        }
        return false;
      }

      $scope.editorAlerts = [];

      $scope.cancelEditTag = function() {
        $scope.$emit('tags.cancelEditTag');
      };

      $scope.canSaveEdit = function(valid, tag) {
        return valid && !$scope.submitActive && tag !== null && tag.name && tag.description;
      };

      $scope.$on('tagChangeStarted', function(event) {
        if (isEditing()) {
          event.preventDefault();
        }
      });

      $scope.$on('pageChangeStarted', function(event) {
        if (isEditing()) {
          event.preventDefault();
        }
      });

      $scope.saveTag = function() {
        $scope.submitActive = true;
        $scope.selectedTag.$save().then(function() {
          $scope.submitActive = false;
          $scope.deselect();
        }, errorFn);
      };
    }
  ]);

  tagModule.controller('TagApplicationController', ['$scope', '$http', '$q', 'CLMLocations', 'selectedApplication', 'Messages',
    function($scope, $http, $q, CLMLocations, selectedApplication, Messages) {
      $scope.alerts = [];
      var promises = [ $http.get(CLMLocations.getApplicableOrganizationTags(selectedApplication.publicId)),
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
            delete $scope.tagSearch;
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
