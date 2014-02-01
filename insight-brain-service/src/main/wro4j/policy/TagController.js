/**
 * @license Copyright (c) 2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular*/
(function() {
  'use strict';

  var tagTemplate = {id: null, organizationId: null, name: null, description: null, color:null};

  var tagModule = angular.module('Tags', ['AngularCommon', 'CLMAppLocation', 'CLMLocation', 'CommonServices', 'ResourceModule', 'Stores', 'PolicyEditor']);

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

  tagModule.service('PolicyTagStore', ['CachedStore', 'CLMAppLocations', function(CachedStore, CLMAppLocations) {
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
      }
    };
  }]);

  function showAlert(alerts, alert) {
    alerts.length = 0;
    alerts.push(alert);
  }

  tagModule.controller('TagController', [
    '$scope', '$http', '$q', 'CLMAppLocations', 'Messages', 'CLMResource', 'TagStore', 'PolicyStore', 'ownerChange', 'Dialog', 'ApplicationStore',
    function($scope, $http, $q, clmAppLocations, messages, clmResource, TagStore, PolicyStore, ownerChange, Dialog, ApplicationStore) {
      $scope.alerts = [];
      $scope.colors = [null, 'white', 'grey', 'black', 'green', 'yellow', 'orange', 'red', 'blue'];

      function deselect() {
        if ($scope.selectedTag) {
          $scope.selectedTag.$revert();
        }
        $scope.selectedTag = null;
        $scope.submitActive = false;
        $scope.alerts.length = 0;
      }

      function showEditingAlert() {
        showAlert($scope.alerts, {
          type: 'error',
          msg: 'Please finish editing before trying to modify another tag.'
        });
      }

      $scope.deselect = deselect;

      $scope.doLoad = function() {
        $scope.error = null;
        $scope.tags = null;
        $q.all([TagStore.refresh(), TagStore.getApplied(), ApplicationStore.get(), PolicyStore.get().refresh()]).then(function(results) {
          $scope.tags = results[0];
          $scope.appliedTags = results[1].data;
          $scope.applications = results[2];
          $scope.policies = results[3];
          var mappedApplications = {};
          angular.forEach($scope.applications, function(application){
            mappedApplications[application.id] = application.name;
          });
          var mappedTags = {};
          angular.forEach($scope.tags, function(tag){
            tag.appliedTags = [];
            mappedTags[tag.id] = tag;
          });
          angular.forEach($scope.appliedTags, function(ApplicationTag){
            var appliedTags = mappedTags[ApplicationTag.tagId].appliedTags;
            ApplicationTag.applicationName = mappedApplications[ApplicationTag.applicationId];
            appliedTags.push(ApplicationTag);
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
          showEditingAlert();
        }
      };

      $scope.setColor = function(color) {
        $scope.selectedTag.color = color;
      };

      $scope.createNew = function() {
        var e = $scope.$broadcast('tagChangeStarted');
        if (!e.defaultPrevented) {
          $scope.selectedTag = TagStore.create();
        } else {
          showEditingAlert();
        }
      };

      $scope.deleteTag = function(tag, $event) {
        $event.stopPropagation();
        var body = 'Are you sure you want to delete this tag?';
        var length = tag.appliedTags ? tag.appliedTags.length : 0;
        if (length > 0) {
          body += ' It is in use by the following applications: ';
          body += jQuery.map(tag.appliedTags, function(ApplicationTag){
            return ApplicationTag.applicationName;
          }).join(', ') + '.';
        }
        Dialog.open({
          title: 'Delete Tag',
          body: body,
          buttons: [
            {
              name: 'Cancel'
            },
            {
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
            }
          ]
        });
      };

      $scope.$on('tags.cancelEditTag', function(event) {
        event.stopPropagation();
        deselect();
      });

      $scope.doLoad();
    }
  ]);

  tagModule.controller('TagEditorController', ['$scope', '$http', '$q', 'Messages', 'CLMAppLocations',
    function($scope, $http, $q, messages, CLMAppLocations) {
      var originalAppliedPolicyIds;

      function errorFn(error) {
        $scope.submitActive = false;
        showAlert($scope.editorAlerts, {
          type: 'error',
          msg: 'An error occurred while saving the tag. (' +
            messages.getHttpErrorMessage(error) + ')'
        });
      }

      function isAppliedPolicyChanged() {
        for (var i = 0; i < $scope.policies.length; i++) {
          var policyId = $scope.policies[i].id;
          if ($scope.appliedPolicyIds.indexOf(policyId) > -1 && originalAppliedPolicyIds.indexOf(policyId) === -1 ||
            $scope.appliedPolicyIds.indexOf(policyId) === -1 && originalAppliedPolicyIds.indexOf(policyId) > -1) {
            return true;
          }
        }
        return false;
      }

      function isEditing() {
        if ($scope.selectedTag) {
          if ($scope.selectedTag.isDirty() || isAppliedPolicyChanged()) {
            return true;
          }
        }
        return false;
      }

      $scope.editorAlerts = [];

      $scope.doLoad = function() {
        if ($scope.submitActive) {
          return;
        }
        $scope.appliedPolicyIds = [];
        originalAppliedPolicyIds = [];
        if (!$scope.selectedTag || $scope.selectedTag.$new) {
          return;
        }
        $http.get(CLMAppLocations.getTagAppliedPoliciesUrl($scope.selectedTag.id)).then(function(response) {
          var appliedPolicies = response.data;
          for (var i = 0; i < appliedPolicies.length; i++) {
            originalAppliedPolicyIds.push(appliedPolicies[i].policyId);
          }
          $scope.appliedPolicyIds = angular.copy(originalAppliedPolicyIds);
        });
      };

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
          var promises = [];
          for (var i = 0; i < $scope.policies.length; i++) {
            var policyId = $scope.policies[i].id;
            if ($scope.appliedPolicyIds.indexOf(policyId) > -1 && originalAppliedPolicyIds.indexOf(policyId) === -1) {
              promises.push($http.post(CLMAppLocations.getPolicyTagUrl(policyId), $scope.selectedTag));
            } else if ($scope.appliedPolicyIds.indexOf(policyId) === -1 && originalAppliedPolicyIds.indexOf(policyId) > -1) {
              promises.push($http['delete'](CLMAppLocations.getDeletePolicyTagUrl(policyId, $scope.selectedTag.id)));
            }
          }
          $q.all(promises).then(function() {
            $scope.submitActive = false;
            $scope.deselect();
          }, errorFn);
        }, errorFn);
      };

      $scope.$watch('selectedTag', $scope.doLoad);
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
