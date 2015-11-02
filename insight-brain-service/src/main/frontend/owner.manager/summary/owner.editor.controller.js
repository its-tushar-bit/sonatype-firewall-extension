/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  function OwnerEditorController($scope, $state, $window, $cookies, $http, $q, owner, ownerType, siblings, messages,
                                 CLMAppLocations, formMaskDelay)
  {
    $scope.dirtyOwner = owner.$new ? owner : owner.$clone(); // only create a copy for an existing
    $scope.icon = {};

    $scope.csrfTokenName = $http.defaults.xsrfHeaderName;
    $scope.csrfTokenValue = $cookies[$http.defaults.xsrfCookieName];
    $scope.ownerType = ownerType;
    $scope.siblings = siblings;

    $scope.iconUploadUrl = CLMAppLocations.getAddIconSyncUrl(ownerType);

    var deferred;

    $scope.fileUploadComplete = function(content, completed) {
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

      formMaskDelay.wrap($scope, $scope.dirtyOwner.$save().then(function(result) {
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
            headers: {
              'Content-Type': undefined
            },
            transformRequest: angular.identity
          }).then(function() {
            deferred.resolve(result);
          }, function(error) {
            deferred.reject(error);
          }).finally(function() {
            deferred = null;
          });
        }
        else {
          deferred = $q.defer();
          form.find('*[upload-submit]').click();
        }
        return deferred.promise;
      })).then(function(updatedOwner) {
        if (isNew) {
          $state.go('management.view.' + ownerType, ownerType === 'application' ? {
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

    $scope.robot = function(name) {
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

    $scope.robotUrl = function() {
      return CLMAppLocations.getRobotUrl(ownerType, $scope.icon.robotHash);
    };

    $scope.$watch('icon.type', function(iconType) {
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

    $scope.$watch('icon.source', function() {
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
  }
  OwnerEditorController.$inject = [
    '$scope', '$state', '$window', '$cookies', '$http', '$q', 'owner', 'ownerType', 'siblings', 'Messages',
    'CLMAppLocations', 'FormMaskDelay'
  ];

  angular
    .module('owner.manager.module')
    .controller('OwnerEditorController', OwnerEditorController);

}(angular));
