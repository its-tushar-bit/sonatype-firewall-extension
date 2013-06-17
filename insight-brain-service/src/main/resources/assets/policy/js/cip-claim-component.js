/**
 * @license Copyright (c) 2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular, $, window, CLM, setTimeout */
(function () {
    'use strict';

    function doLoad() {
        $.extend(true, window, {
            'Insight' : {
                'ClaimComponent' : function (node, applicationId, hash) {
                    var timestamp = (new Date()).getTime(),
                        container = $('<div ng-include src="\'' + CLM.path + 'policy-assets/components/cip-claim-component.html\'"></div>');
                    node.empty();
                    container.appendTo(node);

                    angular.module('claimComponent' + timestamp, []).service('CurrentHash', function () {
                        return {
                            hash: hash
                        };
                    });
                    angular.bootstrap(container[0], ['ClaimComponent', 'claimComponent' + timestamp]);
                }
            }
        });
        
        var claimApp = angular.module('ClaimComponent', []);

        claimApp.controller('ClaimComponentController', ['$http', '$scope', 'CurrentHash', function ($http, $scope, CurrentHash) {
            $scope.claimData = {};
            
            $scope.claimClick = function() {
                if ($scope.formValid()) {
                    $scope.claimData.hash = CurrentHash.hash;
                    $http.post(CLM.path + 'rest/component/identified', $scope.claimData).success(function (data) {
                        $scope.claimData = {};
                        //TODO: add some notification of success??
                    }).error(function(){
                        //TODO: add proper error handling
                        alert('error');
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
                } else if (!data.comment) {
                    return false;
                }
                return true;
            }
        }]);
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
(function () {
    "use strict";

    function ClaimComponentTab(node, options) {
        this.node = node;
        this.options = options;
    }
    
    ClaimComponentTab.prototype = new Insight.InformationPanelPlugin();

    ClaimComponentTab.prototype.isVisible = function () {
        return !freemium || this.gav.matchState !== 'exact';
    };

    ClaimComponentTab.prototype.create = function () {
        var timestamp = (new Date()).getTime(),
            container = $('<div id="claim-component-' + timestamp + '"></div>'),
            me = this,
            retry = function () {
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

    ClaimComponentTab.prototype.destroy = function () {
        this.node.empty();
    };

    ClaimComponentTab.prototype.getTitle = function () {
        return 'Claim Component';
    };
    
    function check() {
        if (Insight.InformationPanelPlugins) {
            Insight.InformationPanelPlugins.push(ClaimComponentTab);
        } else {
            setTimeout(check, 100);
        }
    }

    setTimeout(check, 0);
}());