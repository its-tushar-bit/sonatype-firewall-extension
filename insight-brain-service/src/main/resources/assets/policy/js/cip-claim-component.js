/**
 * @license Copyright (c) 2013 Sonatype, Inc. All rights reserved. Includes the
 *          third-party code listed at
 *          http://links.sonatype.com/products/clm/attributions. "Sonatype" is a
 *          trademark of Sonatype, Inc.
 */
/* global angular, $, window, CLM, setTimeout */
(function() {
    'use strict';

    function doLoad() {
        $.extend(true, window, {
            'Insight' : {
                'ClaimComponent' : function(node, applicationId, hash) {
                    var timestamp = (new Date()).getTime(), container = $('<div ng-include src="\'' + CLM.path + 'policy-assets/components/cip-claim-component.html\'"></div>');
                    node.empty();
                    container.appendTo(node);

                    angular.module('claimComponent' + timestamp, []).service('CurrentHash', function() {
                        return {
                            hash : hash
                        };
                    });
                    angular.bootstrap(container[0], [ 'ClaimComponent', 'claimComponent' + timestamp ]);
                }
            }
        });

        var claimApp = angular.module('ClaimComponent', []);

        claimApp.controller('ClaimComponentController', [ '$http', '$scope', 'CurrentHash', function($http, $scope, CurrentHash) {
            $scope.claimData = {};

            $scope.claimClick = function() {
                $scope.createError = '';
                $scope.createSuccess = '';
                if ($scope.formValid()) {
                    $scope.claimData.hash = CurrentHash.hash;
                    $http.post(CLM.path + 'rest/component/identified', $scope.claimData).success(function(data) {
                        var dataView = InsightDatatable.getActiveTable().dataView, currentItem;

                        $.each(dataView.getItems(), function(index, item) {
                            if (item.hash === CurrentHash.hash) {
                                dataView.beginUpdate();
                                dataView.updateItem(item.id, $.extend({}, item, {
                                    identificationSource : 'Manual',
                                    matchState : 'exact',
                                    groupId : $scope.claimData.groupId,
                                    artifactId : $scope.claimData.artifactId,
                                    version : $scope.claimData.version,
                                    classifier : $scope.claimData.classifier,
                                    extension : $scope.claimData.extension
                                }));
                                dataView.endUpdate();
                                return false;
                            }
                        });
                        
                        $scope.createSuccess = 'Component successfully claimed as ' + $scope.claimData.groupId + ':' + $scope.claimData.artifactId + ':' + $scope.claimData.version;
                        $scope.claimData = {};
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
        } ]);
    }

    function check() {
        if (window.angular) {
            doLoad();
        } else {
            setTimeout(check, 100);
        }
    }

    setTimeout(check, 0);
}());

/* add claim component tab as an information panel plugin */
(function() {
    "use strict";

    function ClaimComponentTab(node, options) {
        this.node = node;
        this.options = options;
    }

    function check() {
        if (Insight.InformationPanelPlugins) {
            Insight.InformationPanelPlugins.push(ClaimComponentTab);
        } else {
            setTimeout(check, 100);
        }
    }

    if (window.Insight && window.Insight.InformationPanelPlugin) {
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

        setTimeout(check, 0);
    }
}());