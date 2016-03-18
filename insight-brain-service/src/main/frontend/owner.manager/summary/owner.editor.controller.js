/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  function OwnerEditorController($scope, $rootScope, $state, $window, $cookies, $http, $q, owner, ownerType, siblings,
                                 messages, CLMAppLocations, EventNameConstant)
  {
    var vm = this,
        deferred,
        //default to null as that is the value we use for the 'default selection'
        //keep in mind we aren't currently loading any data to say what the existing icon type is
        originalIconType = null;

    vm.cancel = cancel;
    vm.csrfTokenName = $http.defaults.xsrfHeaderName;
    vm.csrfTokenValue = $cookies.get($http.defaults.xsrfCookieName);
    vm.dirtyOwner = owner.$new ? owner : owner.$clone(); // only create a copy for an existing
    vm.error = undefined;
    vm.fileUploadComplete = fileUploadComplete;
    vm.getTypeName = getTypeName;
    vm.icon = {};
    vm.iconUploadUrl = CLMAppLocations.getAddIconUrl(ownerType);
    vm.ownerEditor = undefined;
    vm.ownerEditorMask = undefined;
    vm.ownerType = ownerType;
    vm.robot = robot;
    vm.robotUrl = robotUrl;
    vm.save = save;
    vm.siblings = siblings;
    vm.userIconPreview = undefined;
    vm.unsavedModalVisible = false;

    $scope.$watch('vm.icon.type', function(iconType) {
      if (iconType !== 'source') {
        $('#icon-file').val(''); // reset
      }

      vm.icon.hasRobotSource = (iconType === 'robot');
      if (!vm.icon.hasRobotSource) {
        vm.icon.robotHash = null;
      }
      else {
        vm.robot(vm.dirtyOwner && vm.dirtyOwner.name);
      }
    });

    $scope.$watch('vm.icon.source', function() {
      if ($window.URL && vm.icon.source) {
        vm.userIconPreview = $window.URL.createObjectURL($('#icon-file')[0].files[0]);
      }
    });

    $scope.$on('pageChangeStarted', function(event) {
      if (isDirty()) {
        vm.unsavedModalVisible = true;
        event.preventDefault();
      }
    });

    $scope.$on('pageChangeCanceled', function() {
      vm.unsavedModalVisible = false;
    });

    $scope.$on('pageChangeAccepted', function() {
      $scope.$dismiss();
    });

    function isDirty() {
      return !(vm.icon.type === originalIconType || (!vm.icon.type && !originalIconType)) || vm.dirtyOwner.isDirty();
    }

    function fileUploadComplete(content) {
      if (angular.isString(content) && content) {
        deferred.reject(content);
      }
      else {
        deferred.resolve(vm.dirtyOwner);
      }
      deferred = null;
    }

    function save() {
      var isNew = owner.$new;
      delete vm.error;

      vm.ownerEditorMask.wrap(vm.dirtyOwner.$save().then(function(result) {
        var form = $('#custom-icon-form');

        form.find('input[name=' + ownerType + 'Id]').val(result.id);

        if (vm.icon.type === '') {
          // default icon
          return result;
        }
        else if ($window.FormData) {
          var formData = new FormData(form[0]);
          deferred = $q.defer();

          $http.post(CLMAppLocations.getAddIconUrl(ownerType), formData).then(function() {
            deferred.resolve(result);
          }, function(error) {
            deferred.reject(error);
          }).finally(function() {
            deferred = null;
          });
        }
        else {
          deferred = $q.defer();
          form.submit();
        }
        return deferred.promise;
      })).then(function(updatedOwner) {
        $rootScope.$broadcast(EventNameConstant.OWNER_UPDATED, updatedOwner, ownerType, isNew);
        if (isNew) {
          $state.go('management.view.' + ownerType, ownerType === 'application' ? {
            applicationPublicId: updatedOwner.publicId
          } : {
            organizationId: updatedOwner.id
          });
        }
        $scope.$close();
      }, function(error) {
        vm.error = messages.getHttpErrorMessage(error);
      });
    }

    function getTypeName() {
      return ownerType === 'application' ? 'Application' : 'Organization';
    }

    function cancel() {
      $scope.$dismiss();
    }

    function robot(name) {
      var hash = 0;

      // Once the user has already generated a robot by hashing the name, continue to provide random robots
      if (!name || vm.icon.robotHash) {
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

      vm.icon.robotHash = hash;
    }

    function robotUrl() {
      return CLMAppLocations.getRobotUrl(ownerType, vm.icon.robotHash);
    }
  }

  OwnerEditorController.$inject = [
    '$scope', '$rootScope', '$state', '$window', '$cookies', '$http', '$q', 'owner', 'ownerType', 'siblings',
    'Messages', 'CLMAppLocations', 'event.name.constant'
  ];

  angular //
      .module('owner.manager.module') //
      .controller('owner.editor.controller', OwnerEditorController);

}(angular));
