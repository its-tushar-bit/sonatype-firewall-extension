/**
 * @license Copyright (c) 2013 Sonatype, Inc. All rights reserved. Includes the
 *          third-party code listed at
 *          http://links.sonatype.com/products/clm/attributions. "Sonatype" is a
 *          trademark of Sonatype, Inc.
 */
/* global angular, $, window, CLM, setTimeout */
(function() {
    'use strict';
    // from the datepicker.js
    var formatDate = function(date, format) {
        var val = {
            d : date.getDate(),
            m : date.getMonth() + 1,
            yy : date.getFullYear().toString().substring(2),
            yyyy : date.getFullYear()
        };
        val.dd = (val.d < 10 ? '0' : '') + val.d;
        val.mm = (val.m < 10 ? '0' : '') + val.m;
        var date = [];
        for ( var i = 0, cnt = format.parts.length; i < cnt; i++) {
            date.push(val[format.parts[i]]);
        }
        return date.join(format.separator);
    };
    var parseFormat = function(format) {
        if (!format) {
            return;
        }
        var separator = format.match(/[.\/\-\s].*?/), parts = format.split(/\W+/);
        if (!separator || !parts || parts.length === 0) {
            throw new Error("Invalid date format.");
        }
        return {
            separator : separator,
            parts : parts
        };
    };
    var parseDate = function(date, format) {
        if (!date) {
            return;
        }
        var parts = date.split(format.separator), date = new Date(), val;
        date.setHours(0);
        date.setMinutes(0);
        date.setSeconds(0);
        date.setMilliseconds(0);
        if (parts.length === format.parts.length) {
            var year = date.getFullYear(), day = date.getDate(), month = date.getMonth();
            for ( var i = 0, cnt = format.parts.length; i < cnt; i++) {
                val = parseInt(parts[i], 10) || 1;
                switch (format.parts[i]) {
                case 'dd':
                case 'd':
                    day = val;
                    date.setDate(val);
                    break;
                case 'mm':
                case 'm':
                    month = val - 1;
                    date.setMonth(val - 1);
                    break;
                case 'yy':
                    year = 2000 + val;
                    date.setFullYear(2000 + val);
                    break;
                case 'yyyy':
                    year = val;
                    date.setFullYear(val);
                    break;
                }
            }
            date = new Date(year, month, day, 0, 0, 0);
        }
        return date;
    };
    $.extend(true, window, {
        'Insight' : {
            'ClaimComponent' : function(node, applicationId, hash) {
                function applyFocus() {
                    if (node.find('input').length > 0) {
                        node.find('input')[0].focus();
                        return;
                    }

                    setTimeout(applyFocus, 100);
                }
                var timestamp = (new Date()).getTime(), container = $('<div ng-include src="\'' + CLM.path + 'policy-assets/components/cip-claim-component.html\'"></div>');
                node.empty();
                container.appendTo(node);

                angular.module('claimComponent' + timestamp, []).service('CurrentHash', function() {
                    return {
                        hash : hash
                    };
                });
                angular.bootstrap(container[0], [ 'ClaimComponent', 'claimComponent' + timestamp ]);

                applyFocus();
            }
        }
    });

    var claimApp = angular.module('ClaimComponent', [ 'Hudson' ]);

    claimApp.controller('ClaimComponentController', [ 'hudson', '$scope', 'CurrentHash', function(hudson, $scope, CurrentHash) {
        $scope.resetClaimData = function() {
            $scope.claimData = {};
            $scope.submitted = false;
            $scope.disableSubmit = false;
        };

        $scope.claimSubmit = function() {
            $scope.createError = '';
            $scope.createSuccess = '';
            $scope.submitted = true;
            if ($scope.claimForm.$valid) {
                $scope.disableSubmit = true;
                $scope.claimData.hash = CurrentHash.hash;
                if ($scope.claimData.createTimeText) {
                    $scope.claimData.createTime = parseDate($scope.claimData.createTimeText, parseFormat('mm/dd/yyyy')).getTime();
                }
                hudson.post(CLM.path + 'rest/component/identified', $scope.claimData).success(function(data) {
                    var dataView = InsightDatatable.getActiveTable().dataView, currentItem;

                    $.each(dataView.getItems(), function(index, item) {
                        if (item.hash === CurrentHash.hash) {
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

        $scope.resetClaimData();
    } ]);

    claimApp.directive('disablenav', function() {
        return function(scope, element, attrs) {
            element.bind("keydown.nav", function(e) {
                // 9 is tab, others are arrow keys
                if (e.keyCode == 9 || (e.keyCode >= 37 && e.keyCode <= 40)) {
                    e.stopImmediatePropagation();
                }
            });
        };
    });

    claimApp.directive('datepicker', function() {
        return function(scope, element, attrs) {
            element.datepicker({
                format : 'mm/dd/yyyy'
            }).on('changeDate', function(event) {
                scope.$apply(function() {
                    scope.claimData.createTimeText = formatDate(event.date, parseFormat('mm/dd/yyyy'));
                });
            });
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

        ClaimComponentTab.prototype = new Insight.InformationPanelPlugin();

        ClaimComponentTab.prototype.isVisible = function() {
            return !freemium && this.gav.matchState !== 'exact';
        };

        ClaimComponentTab.prototype.create = function() {
            var timestamp = (new Date()).getTime(), container = $('<div id="claim-component-' + timestamp + '"></div>'), me = this, retry = function() {
                if (Insight.ClaimComponent) {
                    Insight.ClaimComponent(container, applicationId, me.gav.hash);
                } else {
                    setTimeout(retry, 1000);
                }
            };
            this.node.empty();
            container.appendTo(this.node);

            retry();
        };

        ClaimComponentTab.prototype.destroy = function() {
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