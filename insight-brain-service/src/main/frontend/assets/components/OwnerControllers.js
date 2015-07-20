/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular, $ */
(function () {
  'use strict';

  var module = angular.module('OwnerModule', ['Stores', 'ui.bootstrap']);

  module.controller('OwnerSummaryController', ['$scope', '$state', 'OwnerEditor', 'ApplicationStore', 'OrganizationStore',
      function($scope, $state, OwnerEditor, ApplicationStore, OrganizationStore) {
        var siblings;

        $scope.doLoad = function() {
          var isApp = $state.current.name.indexOf('application') !== -1, stateIdField = isApp ? 'applicationPublicId'
                  : 'organizationId', idField = isApp ? 'publicId' : 'id';

          $scope.type = isApp ? 'application' : 'organization';

          (isApp ? ApplicationStore : OrganizationStore)[$scope.error ? 'refresh' : 'get']().then(function(candidates) {
            siblings = candidates;
            angular.forEach(candidates, function(candidate) {
              if (candidate[idField] === $state.params[stateIdField]) {
                $scope.owner = candidate;
              }
            });

            if (!$scope.owner) {
              $scope.error = 'Unable to locate ' + $scope.type;
            }
          }, function() {
            $scope.error = arguments;
          });

          delete $scope.error;
        };

        $scope.edit = function() {
          OwnerEditor.open($scope.owner, $scope.type, siblings);
        };

        $scope.doLoad();
      }]);

  module.controller('OwnerEditorController', ['$scope', '$state', 'owner', 'ownerType', 'siblings', 'Messages',
      function($scope, $state, owner, ownerType, siblings, messages) {
        $scope.dirtyOwner = owner.$new ? owner : owner.$clone(); // only create a copy for an existing
        $scope.ownerType = ownerType;
        $scope.siblings = siblings;

        $scope.save = function() {
          var isNew = owner.$new;
          delete $scope.error;

          $scope.dirtyOwner.$save().then(function(updatedOwner) {
            if (isNew) {
              $state.go('management.' + ownerType + '-view', ownerType === 'application' ? {
                applicationPublicId: updatedOwner.publicId
              } : {
                organizationId: updatedOwner.id
              });
            }
            $scope.$close();
          }, function(error) {
            $scope.error = messages.getHttpErrorMessage(error);
          });
        };

        $scope.getTypeName = function() {
          return ownerType === 'application' ? 'Application' : 'Organization';
        };

        $scope.cancel = function() {
          $scope.$dismiss();
        };

        $scope.$on('pageChangeStarted', function(event) {
          if ($scope.dirtyOwner.isDirty()) {
            event.preventDefault();
          }
        });

        $scope.$on('pageChangeAccepted', function() {
          $scope.$dismiss();
        });
      }]);

  module.service('OwnerEditor', ['$modal', function($modal) {
    return {
      open: function(owner, ownerType, siblings) {
        $modal.open({
          animation: false,
          backdrop: 'static',
          keyboard: false,
          windowClass: 'owner-editor-modal clm-modal',
          controller: 'OwnerEditorController',
          templateUrl: 'components/owner-editor.html',
          resolve: {
            owner: function() {
              return owner;
            },
            ownerType: function() {
              return ownerType;
            },
            siblings: function () {
              return siblings;
            }
          }
        });
      }
    };
  }]);

  module.directive('ownerImage', ['CLMAppLocations', function (CLMAppLocations) {
    return {
      scope : {
        owner : '=ownerImage'
      },
      template : '<img ng-src="{{ownerUrl}}" ng-if="ownerUrl">',
      link : function (scope) {
        scope.$watch('owner', function () {
          if (scope.owner) {
            scope.ownerUrl = CLMAppLocations.getOwnerImageUrl(scope.owner);
          }
        });

        scope.$on('owner.image.change', function (owner) {
          if (scope.owner === owner && scope.ownerUrl) {
            if (scope.ownerUrl.indexOf('?') !== -1) {
              scope.ownerUrl = scope.ownerUrl.substring(0, scope.ownerUrl.indexOf('?'));
            }
            scope.ownerUrl += '?timestamp=' + Date.now();
          }
        });
      }
    };
  }]);

  module.directive('scrollspy', function() {
    return {
      scope : {
        scrollspy : '@'
      },
      link : function($scope, element) {
        $(element).scrollspy({
          target: $scope.scrollspy,
          offset: 0
        });

        $($scope.scrollspy + ' .nav li > a').click(function(){
          var dataTarget = $(this).attr('data-target');
          $(element).animate({
            scrollTop: $(dataTarget).position().top + $(element).scrollTop()
          }, 100);
        });
      }
    };
  });

}());
