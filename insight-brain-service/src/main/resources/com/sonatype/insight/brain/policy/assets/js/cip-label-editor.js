/*global angular, CLM */
(function () {
	$.extend(true, window, {
	    'Insight' : {
			'LabelEditor' : function (node, applicationId, hash) {
				var timestamp = (new Date()).getTime(),
					container = $('<div ng-include src="\'' + CLM.path + 'policy-assets/components/cip-label-editor.html\'"></div>');
				node.empty();
				container.appendTo(node);

				angular.module('labelEditor' + timestamp, []).service('ComponentLabelEditorGAV', function () {
				    return {
				        applicationId : applicationId,
				        hash : hash
				    };
				});
				angular.bootstrap(container[0], ['ComponentLabelEditor', 'labelEditor' + timestamp]);
	        }
	    }
	});

	function locate(needle, haystack, haystackProperty) {
		var result = null;
		angular.forEach(haystack, function (candidate, key) {
			if (candidate !== null && needle === candidate[haystackProperty]) {
				result = candidate;
				return false;
			}
		});
		return result;
	}

	var labelsApp = angular.module('ComponentLabelEditor', []);

	labelsApp.controller('LabelsController', ['$http', '$scope', 'ComponentLabelEditorGAV', function ($http, $scope, componentLabelEditorGAV) {
	    var componentLabelsUrl = CLM.path + 'rest/label/component/' + componentLabelEditorGAV.applicationId + '/' + componentLabelEditorGAV.hash;

	    function persist(labelArr, color) {
			$http.put(componentLabelsUrl, { labels : labelArr, color : color}).success(function (data) {
				$scope.itemLabels = data;
				$scope.reloadAppLabels(); // Only really necessary if someone adds a brand new label, and removes it in the same session
			}).error(errorFn);
		}

		function errorFn (data, status, headersFn, config) {
			var header = headersFn();
			if (header['content-type'] && header['content-type'].indexOf('text/html') === 0) {
			    $scope.editErrorResponse = 'Server Error';
			} else {
			    $scope.editErrorResponse = data;
			}
		}

		var properties = ['-ms-transform', '-webkit-transform', '-moz-transform', 'transform'];
		function setElement(element, value) {
			angular.forEach(properties, function(prop, key){
				element.css(prop, value);
			});
			return element;
		}

		function rotate ($event) {
			if ($event) {
				setElement($($event.target), '').prop('rotate', null).animate({ rotate : '+360'}, {
					step : function(now,fx) {
					    now = now % 360;
					    setElement($(this), 'rotate('+now+'deg)');
					}
				});
			}
		}

		$scope.reloadLabels = function($event) {
			rotate($event);
			$http.get(componentLabelsUrl, { params : { timestamp : new Date().getTime() } }).success(function (data) {
				$scope.itemLabels = data;
			}).error(errorFn);
		};
		$scope.reloadLabels();

		$scope.reloadAppLabels = function($event) {
			rotate($event);
			$http.get(CLM.path + 'rest/label/application/' + componentLabelEditorGAV.applicationId, { params : { timestamp : new Date().getTime() } }).success(function (data) {
				$scope.availableLabels = data;
			}).error(errorFn);
		};
		$scope.reloadAppLabels(); // do initial load

		$scope.color = null;
		$scope.colors = [{ color : null, text : '(Unset)' },
						{ color : 'white', text : 'White' },
						{ color : 'grey', text : 'Grey' },
						{ color : 'black', text : 'Black' },
						{ color : 'green', text : 'Green' },
						{ color : 'yellow', text : 'Yellow' },
						{ color : 'orange', text : 'Orange' },
						{ color : 'red', text : 'Red' },
						{ color : 'blue', text : 'Blue' }];

		$scope.removeLabel = function (label) {
			var updatedLabels = [];
			angular.forEach($scope.itemLabels, function(candidate, key) {
				if (candidate.label !== label.label) {
					updatedLabels.push(candidate.label);
				}
			});
			persist(updatedLabels);
		};

		$scope.addLabels = function () {
			var newLabels = $scope.labelInput.split(' '),
			    updatedLabels = [],
			    bigLabels = [],
			    hasNewLabel = false;

			if (newLabels.length === 0) {
				return;
			}

			$scope.labelInput = '';

			angular.forEach($scope.itemLabels, function(candidate, key) {
				updatedLabels.push(candidate.label);
			});

			angular.forEach(newLabels, function (label, key) {
				label = $.trim(label);
				if (label.length > 50) {
					bigLabels.push(label);
				} else if (label.length > 0) {
				    updatedLabels.push(label);
				    hasNewLabel = true;
				}
			});

			if (hasNewLabel) {
				persist(updatedLabels, $scope.color);
			}
			if (bigLabels.length > 0) {
				angular.forEach(bigLabels, function (candidate, key) {
					$scope.labelInput += candidate + ' ';
				});
				$scope.labelInput = $.trim($scope.labelInput);
				$scope.editErrorResponse = '';
			}
		};

		$scope.addLabel = function (label) {
			var updatedLabels = [],
			    duplicate = false;

			angular.forEach($scope.itemLabels, function(candidate, key) {
				if (candidate.label.toLowerCase() === label.label.toLowerCase()) {
					duplicate = true;
				}
				updatedLabels.push(candidate.label);
			});

			if(!duplicate) {
				updatedLabels.push(label.label);
				persist(updatedLabels);
			}
		};

		$scope.isWhite = function (label) {
			return label.color === "green" || label.color === "black" || label.color === "orange" || label.color === "red" || label.color === "blue";
		};

		$scope.isApplied = function (label) {
			var duplicate = false;
			angular.forEach($scope.itemLabels, function(candidate, key) {
				duplicate = duplicate || (candidate.label === label.label);
				return !duplicate;
			});
			return !duplicate;
		};

		$scope.checkLength = function () {
			$scope.oversize = false;
			angular.forEach($scope.labelInput.split(' '), function (label, key) {
				if (label.length > 50) {
				    $scope.oversize = true;
				}
			});
		};
	}]);

	labelsApp.directive('disablenav', function () {
		return function (scope, element, attrs) {
			$(element).bind("keydown.nav", function (e) {
				if (e.keyCode === $.ui.keyCode.LEFT || e.keyCode === $.ui.keyCode.RIGHT || e.keyCode === $.ui.keyCode.DOWN || e.keyCode === $.ui.keyCode.UP) {
					e.stopImmediatePropagation();
				}
			});
		};
	});
}());