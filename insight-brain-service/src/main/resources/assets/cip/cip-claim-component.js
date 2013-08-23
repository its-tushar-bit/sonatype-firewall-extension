/**
 * @license Copyright (c) 2013 Sonatype, Inc. All rights reserved. Includes the
 *          third-party code listed at
 *          http://links.sonatype.com/products/clm/attributions. "Sonatype" is a
 *          trademark of Sonatype, Inc.
 */
/* global angular, $, window, CLM, setTimeout */
(function() {
    'use strict';
    
    function pad(str) {
        return ('' + str).length < 2 ? pad("0" + str, 2) : str;
    }
    
    function dateToString(date) {
        if (!date) {
            return null;
        }
        
        return pad(date.getMonth() + 1) + '/' + pad(date.getDate()) + '/' + date.getFullYear();
    }
    
    function stringToDate(str) {
        if (!str) {
            return null;
        }
        
        var parts = str.split('/');
        
        if (parts.length != 3) {
            return null;
        }
        
        return new Date(parts[2],parts[0] - 1,parts[1]);
    }

    $.extend(true, window, {
        'Insight' : {
            'ClaimComponent' : function(node, applicationId, component) {
                function applyFocus() {
                    if (node.find('input').length > 0) {
                        node.find('input')[0].focus();
                        return;
                    }

                    setTimeout(applyFocus, 100);
                }
                var timestamp = (new Date()).getTime(), container = $('<div clm-include="\'' + CLM.path + 'cip/cip-claim-component.html\'"></div>');
                node.empty();
                container.appendTo(node);

                angular.module('claimComponent' + timestamp, []).service('CurrentData', function() {
                    return {
                        hash : component.hash,
                        createTime : component.lastModifiedEntryTime ? component.lastModifiedEntryTime : component.lastModifiedTime
                    };
                });
                angular.bootstrap(container[0], [ 'ClaimComponent', 'claimComponent' + timestamp, 'AngularCommon' ]);

                applyFocus();
            }
        }
    });

    var claimApp = angular.module('ClaimComponent', [ 'Hudson' ]);

    claimApp.controller('ClaimComponentController', [ 'hudson', '$scope', 'CurrentData', function(hudson, $scope, CurrentData) {
        $scope.resetClaimData = function() {
            $scope.claimData = {};
            $scope.claimData.createTimeText = CurrentData.createTime ? dateToString(new Date(CurrentData.createTime)) : null;
            $scope.submitted = false;
            $scope.disableSubmit = false;
        };

        $scope.claimSubmit = function() {
            $scope.createError = '';
            $scope.createSuccess = '';
            $scope.submitted = true;
            if ($scope.claimForm.$valid) {
                $scope.disableSubmit = true;
                $scope.claimData.hash = CurrentData.hash;
                if ($scope.claimData.createTimeText) {
                    $scope.claimData.createTime = stringToDate($scope.claimData.createTimeText).getTime();
                }
                hudson.post(CLM.path + 'rest/component/identified', $scope.claimData).success(function(data) {
                    var dataView = InsightDatatable.getActiveTable().dataView, currentItem;

                    $.each(dataView.getItems(), function(index, item) {
                        if (item.hash === CurrentData.hash) {
                            dataView.beginUpdate();
                            dataView.updateItem(item.id, $.extend({}, item, {
                                identificationSource : 'Manual',
                                matchState : 'exact',
                                groupId : data.groupId,
                                artifactId : data.artifactId,
                                version : data.version,
                                classifier : data.classifier,
                                extension : data.extension,
                                createTime : data.createTime,
                                age : data.createTime ? Math.floor((new Date().getTime() - data.createTime) / (1000 * 60 * 60 * 24)) : null
                            }));
                            dataView.endUpdate();
                            return false;
                        }
                    });

                    $scope.createSuccess = 'Component successfully claimed as ' + data.groupId + ':' + data.artifactId + ':' + data.version;
                    $scope.resetClaimData();
                    // TODO: need to close the info panel as the available
                    // tabs no longer match??
                }).error(function(data, status, headersFn, config) {
                    var header = headersFn();
                    if (header['content-type'] && header['content-type'].indexOf('text/html') === 0) {
                        $scope.createError = 'Server Error';
                    } else if (status === 0) {
                        $scope.errorResponse = 'Unable to connect to CLM server';
                    } else {
                        $scope.createError = data;
                    }
                    $scope.disableSubmit = false;
                });
            }
        };

        $scope.formValid = function() {
            var data = $scope.claimData;
            if (!data.groupId) {
                return false;
            } else if (!data.artifactId) {
                return false;
            } else if (!data.version) {
                return false;
            }
            return true;
        };
        
        $scope.getValidationMessage = function () {
            var claimForm = $scope.claimForm;
            if ($scope.submitted && (claimForm.groupId.$error.required || claimForm.artifactId.$error.required || claimForm.version.$error.required)) {
                return 'Group ID, Artifact ID and Version are required';
            } else if (claimForm.createTimeText.$dirty && claimForm.createTimeText.$error.pattern) {
                return 'Date format is MM/DD/YYYY';
            }
        };

        $scope.resetClaimData();
    } ]);

    claimApp.directive('disablenav', function() {
        return function(scope, element, attrs) {
            element.bind("keydown.nav", function(e) {
                // 9 is tab, others are arrow keys
                if (e.keyCode == 9 || (e.keyCode >= 37 && e.keyCode <= 40)) {
                    e.stopPropagation();
                }
            });
        };
    });

    claimApp.directive('datepicker', function() {
        return function(scope, element, attrs) {
            element.datepicker({
                format : 'mm/dd/yyyy',
                autoclose : true,
                endDate : new Date(),
                clearBtn : true,
                forceParse : false
            }).on('changeDate', function(event) {
                scope.$apply(function() {
                    scope.claimData.createTimeText = dateToString(event.date);
                });
            });
            element.datepicker('update', scope.claimData.createTimeText);
        };
    });
}());

/* add claim component tab as an information panel plugin */
(function() {
    "use strict";

    function doLoad() {
        function ClaimComponentTab(node, options) {
            this.node = node;
            this.options = options;
        }

        ClaimComponentTab.prototype = new Insight.InformationPanelPlugin({ priority : 128 });

        ClaimComponentTab.prototype.isVisible = function() {
            return !freemium && this.gav.matchState !== 'exact';
        };

        ClaimComponentTab.prototype.create = function() {
            var timestamp = (new Date()).getTime(), container = $('<div id="claim-component-' + timestamp + '"></div>'), me = this, retry = function() {
                if (Insight.ClaimComponent) {
                    Insight.ClaimComponent(container, applicationId, me.gav);
                } else {
                    setTimeout(retry, 1000);
                }
            };
            this.node.empty();
            container.appendTo(this.node);

            retry();
        };

        ClaimComponentTab.prototype.destroy = function() {
            var nodeEl = $(this.node).find('.claimComponent');
            nodeEl.on('$destroy',function(event){
              nodeEl.scope().$destroy();
            });
            this.node.empty();
        };

        ClaimComponentTab.prototype.getTitle = function() {
            return 'Claim Component';
        };
        
        Insight.InformationPanelPlugins.push(ClaimComponentTab);
    }

    function check() {
        if (window.Insight && window.Insight.InformationPanelPlugin) {
            doLoad();
        } else {
            setTimeout(check, 100);
        }
    }

    setTimeout(check, 0);
}());