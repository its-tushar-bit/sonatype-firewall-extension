/**
/**
 * @license Copyright (c) 2013 Sonatype, Inc. All rights reserved. Includes the
 *          third-party code listed at
 *          http://links.sonatype.com/products/clm/attributions. "Sonatype" is a
 *          trademark of Sonatype, Inc.
 */
/* global angular */
(function() {
    'use strict';
    function wrap($http, method, args, clmLocations) {
    }

    angular.module('EditorTools', []).service('editorTools', function($parse, $q, $timeout, regexFactory, hudson, CLMAppLocations) {
		function EditorController($scope, idSelector, hiddenId, form) {
			var defer, me = this;
      $scope.isPostingIcon = false;

			$scope.alerts = [];
			$scope.hasRobotSource = false;

			$scope.pushAlert = function (obj) {
				$scope.alerts.length = 0;
				$scope.alerts.push(obj);
			};

      $scope.iconUploadComplete = function(content, completed) {
        if (completed) {
          $scope.submitActive = false;
          $scope.isUploadingIcon = false;
          if (content.length === 0) {
            $scope.iconChanged = false;
            $scope.hasRobotSource = false;
            $scope.$emit('resetIconCache');
            defer.resolve(null);
          } else {
            $scope.pushAlert({ type: 'error', msg: content } );
            defer.reject(content);
          }
        }
      };

			me.generateIcon = function(name) {
				var hash = 0;
				if (!name) {
					hash = Math.floor(Math.random() * 100);
				} else {
					for (var i = 0; i < name.length; i++) {
						var charAtI = name.charCodeAt(i);
						hash = ((hash << 5) - hash) + charAtI;
						hash = hash & hash;
					}
				}
				$scope.robotHash = hash;
				$scope.hasRobotSource = true;
				$scope.iconChanged = true;
			};

			me.getIconSource = function(element, defaultSource) {
				if (element.files && element.files.length > 0) {
					var file = element.files[0], src;
					if (window.URL) {
						src = window.URL.createObjectURL(file);
					} else if (window.webkitURL) {
						src = window.webkitURL.createObjectURL(file);
					}
					if (src) {
						return src;
					}
				}

				return defaultSource;
			};

			me.saveIcon = function() {
        defer = $q.defer();

				if (!$scope.iconChanged) {
					$scope.submitActive = false;
          defer.resolve(null);
					return defer.promise;
				}

				// Angular modal does not adjust value of form element so when posting these values need to be set
				hiddenId.val($parse(idSelector)($scope));
				angular.element('[name=hasRobotSource]').val($scope.hasRobotSource);
				angular.element('[name=robotHash]').val($scope.robotHash);

				if (window.FormData) {
					$scope.isUploadingIcon = true;

					var formData = new FormData(form[0]);
					var icon = angular.element('#file')[0];
					if (icon.files.length > 0) {
						formData.append('file', icon.files[0]);
					}

					hudson.ajaxPost({
						url: CLMAppLocations.addIcon(),
						data: formData,
						success: function (data, status, jqXHR) {
							$scope.$apply(function () {
								$scope.submitActive = false;
								$scope.isUploadingIcon = false;
                $scope.iconChanged = false;
                $scope.$emit('resetIconCache');
                defer.resolve(data);
							});
						},
						error: function (jqXHR) {
              var errorText;
              var contentType = jqXHR.getResponseHeader('Content-Type');
              if (contentType.indexOf('text/html') === 0) {
                errorText = 'Server Error';
              } else {
                errorText = jqXHR.responseText;
              }
							$scope.$apply(function () {
								$scope.isUploadingIcon = false;
								$scope.submitActive = false;
								$scope.pushAlert({ type: 'error', msg: errorText });
                defer.reject(errorText);
							});
						}
					});
				} else {
          $scope.isPostingIcon = true;
          $scope.isUploadingIcon = true;
          $('#iconUploadForm').find('input[type=submit]').trigger('click');
				}
        return defer.promise;
			}
		}

        return {
            messages: {
              required: 'Name is required',
              alphanumeric: 'Must be alpha numeric',
              spaces: 'No leading, trailing or double spaces or tabs',
              duplicate: 'Name is already in use'
            },
            validateName : function(name, currentItem, existingItems) {
                // field is required, alphanumeric, and no unnecessary spaces
                if (!name) {
                    return this.messages.required;
                } else if (name.match(new RegExp('[^-' + regexFactory.allLetters().source + '0-9 ]', 'i'))) {
                    return this.messages.alphanumeric;
                } else if (name.match(/^ | {2,}|\t| $/)) {
                    return this.messages.spaces;
                }

                // check for uniqueness
                for ( var i = 0; i < existingItems.length; i++) {
                    if (existingItems[i].name.toLowerCase() === name.toLowerCase()
                          && existingItems[i].id !== currentItem.id) {
                        return this.messages.duplicate;
                    }
                }

                return true;
            },

			getEditorController : function($scope, idSelector, hiddenId, form) {
				return new EditorController($scope, idSelector, hiddenId, form);
			}
        };
    });
}());