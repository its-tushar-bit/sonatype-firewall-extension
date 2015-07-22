/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular, $ */
(function () {
  'use strict';

  var module = angular.module('OwnerModule', ['Stores', 'ui.bootstrap', 'FormsModule']);

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

  module.controller('OwnerEditorController', ['$scope', '$state', '$window', '$cookies', '$http', '$q', 'owner',
                                              'ownerType', 'siblings', 'Messages', 'CLMAppLocations',  'FormMaskDelay',
      function($scope, $state, $window, $cookies, $http, $q, owner, ownerType, siblings, messages, CLMAppLocations, formMaskDelay) {
        $scope.dirtyOwner = owner.$new ? owner : owner.$clone(); // only create a copy for an existing
        $scope.icon = {};

        $scope.csrfTokenName = $http.defaults.xsrfHeaderName;
        $scope.csrfTokenValue = $cookies[$http.defaults.xsrfCookieName];
        $scope.ownerType = ownerType;
        $scope.siblings = siblings;

        $scope.iconUploadUrl = CLMAppLocations.getAddIconSyncUrl(ownerType);

        var deferred;

        $scope.fileUploadComplete = function (content, completed) {
          if (completed) {
            if (content) {
              deferred.reject(content);
            }
            else {
              deferred.resolve($scope.dirtyOwner);
            }
            deferred = null;
          }
        };

        $scope.save = function() {
          var isNew = owner.$new;
          delete $scope.error;

          formMaskDelay.wrap($scope, $scope.dirtyOwner.$save().then(function (result) {
            var form = $('#custom-icon-form');

            form.find('input[name=' + ownerType + 'Id]').val(result.id);

            if ($scope.icon.type === '') {
              // default icon
              return result;
            }
            else if ($window.FormData) {
              var formData = new FormData(form[0]);
              deferred = $q.defer();

              $http.post(CLMAppLocations.getAddIconUrl(ownerType), formData, {
                headers : {
                  'Content-Type' : undefined
                },
                transformRequest: angular.identity
              }).then(function () {
                deferred.resolve(result);
              }, function (error) {
                deferred.reject(error);
              }).finally(function () {
                deferred = null;
              });
            }
            else {
              form.find('*[upload-submit]').click();
            }
            return deferred.promise;
          })).then(function(updatedOwner) {
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

        $scope.selectIcon = function (selector) {
          angular.element(selector).click();
        };

        $scope.robot = function (name) {
          var hash = 0;
          // Once the user has already generated a robot by hashing the name, continue to provide random robots
          if (!name || $scope.icon.robotHash) {
            hash = Math.floor(Math.random() * 10000);
          }
          else {
            for (var i = 0; i < name.length; i++) {
              var charAtI = name.charCodeAt(i);
              /*jslint bitwise: true */
              hash = ((hash << 5) - hash) + charAtI;
              hash = hash & hash;
            }
          }
          $scope.icon.robotHash = hash;
        };

        $scope.robotUrl = function () {
          return CLMAppLocations.getRobotUrl(ownerType, $scope.icon.robotHash);
        };

        $scope.$watch('icon.type', function (iconType) {
          if (iconType !== 'source') {
            $('#icon-file').val(''); // reset
          }

          $scope.icon.hasRobotSource = (iconType === 'robot');
          if (!$scope.icon.hasRobotSource) {
            $scope.icon.robotHash = null;
          }
          else {
            $scope.robot($scope.dirtyOwner && $scope.dirtyOwner.name);
          }
        });

        $scope.$watch('icon.source', function () {
          if ($window.URL && $scope.icon.source) {
            $scope.userIconPreview = $window.URL.createObjectURL($('#icon-file')[0].files[0]);
          }
        });

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

  module.directive('scrollspy', ['$timeout', function($timeout) {
    return {
      scope : {
        scrollspy : '@'
      },
      link : function($scope, element) {
        element.scrollspy({
          target: $scope.scrollspy,
          offset: 0
        });

        $($scope.scrollspy + ' .nav li > a').click(function(){
          var me = $(this);
          element.scrollTop($(me.attr('data-target')).position().top + element.scrollTop());
          $timeout(function(){
            $($scope.scrollspy + ' .nav li').removeClass('active');
            me.parent().addClass('active');
          });
        });
      }
    };
  }]);

}());
