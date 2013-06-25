/**
 * @license Copyright (c) 2013 Sonatype, Inc. All rights reserved. Includes the
 *          third-party code listed at
 *          http://links.sonatype.com/products/clm/attributions. "Sonatype" is a
 *          trademark of Sonatype, Inc.
 */
/* global angular, $, window, CLM, setTimeout */
(function() {
    'use strict';
    function pad (str, max) {
        return ('' + str).length < max ? pad("0" + str, max) : str;
    }

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

        var claimApp = angular.module('ClaimComponent', ['Hudson']);

        claimApp.controller('ClaimComponentController', [ 'hudson', '$scope', 'CurrentHash', function(hudson, $scope, CurrentHash) {
            $scope.resetClaimData = function() {
                var now = new Date();
                $scope.now = now.getFullYear() + '-' + pad(now.getMonth() + 1,2) + '-' + pad(now.getDate(),2)
                $scope.claimData = {
                    createTime : now.getTime()
                }
            }

            $scope.claimClick = function() {
                $scope.createError = '';
                $scope.createSuccess = '';
                if ($scope.formValid()) {
                    $scope.claimData.hash = CurrentHash.hash;
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
                                    age : $scope.claimData.createTime ? Math.floor((new Date().getTime() - $scope.claimData.createTime) / (1000 * 60 * 60 * 24)) : null
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
        
        claimApp.directive('disablenav', function () {
            return function (scope, element, attrs) {
                element.bind("keydown.nav", function (e) {
                    //9 is tab, others are arrow keys
                    if (e.keyCode == 9 || (e.keyCode >= 37 && e.keyCode <= 40)) {
                        e.stopImmediatePropagation();
                    }
                });
            };
        });

        claimApp.directive('datepicker', function() {
           return function (scope, element, attrs) {
               element.datepicker({
                   format: 'yyyy-mm-dd'
               }).on('changeDate', function(ev){
                   scope.$apply(function(){
                       scope.claimData.createTime = ev.date.getTime();
                   });
               });
           } 
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