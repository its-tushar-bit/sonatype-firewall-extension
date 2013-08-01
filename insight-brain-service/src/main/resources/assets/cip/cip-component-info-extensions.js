/**
 * @license Copyright (c) 2013 Sonatype, Inc. All rights reserved. Includes the
 *          third-party code listed at
 *          http://links.sonatype.com/products/clm/attributions. "Sonatype" is a
 *          trademark of Sonatype, Inc.
 */
/* global angular, $, window, CLM, setTimeout, applicationId */
(function() {
  'use strict';
  var compInfoScope = null;

  function loadAngular() {
    angular.module('ComponentInfo', []).controller(
            'ComponentInfoController',
            [
                '$scope',
                '$http',
                '$q',
                function($scope, $http, $q) {
                  $scope.viewWaivers = function() {
                    // get the waivers and policies from the server
                    var policyWaiverPromise = $http.get(CLM.path + 'rest/policyWaiver/application/'
                            + $scope.applicationId + '/component/' + $scope.hash), policyPromise = $http.get(CLM.path
                            + 'rest/policy/application/' + $scope.applicationId + '/applicable');

                    $q.all([policyWaiverPromise, policyPromise]).then(function(results) {
                      $scope.waiverLoading = false;
                      $scope.waivers = results[0].data;

                      // process the results to add policy name and owner name
                      $.each($scope.waivers, function(waiverIndex, waiver) {
                        var date = new Date(waiver.createTime);
                        waiver.createTimeStr = (date.getMonth() + 1) + '/' + date.getDate() + '/' + date.getFullYear();
                        $.each(results[1].data.policiesByOwner, function(policyOwnerIndex, policyOwner) {
                          $.each(policyOwner.policies, function(policyIndex, policy) {
                            if (waiver.policyId === policy.id) {
                              waiver.policyName = policy.name;
                              waiver.ownerName = policyOwner.ownerName;
                              return false;
                            }
                          });
                          if (waiver.policyName) { return false; }
                        });
                      });

                    }, function() {
                      $scope.waiverLoading = false;
                      $scope.appError = arguments[0];
                    });
                  }

                  compInfoScope = $scope;
                }]);

    $('body')
            .append(
                    '<div id="componentExistingWaiverModal" ng-controller="ComponentInfoController" class="modal hide fade">'
                            + '<div class="modal-header">'
                            + '<button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>'
                            + '<h3>Component Waivers</h3>'
                            + '</div>'
                            + '<div class="modal-body" ng-show="waiversLoading">'
                            + '<span>Loading data...</span>'
                            + '</div>'
                            + '<div class="modal-body" ng-show="!waiversLoading">'
                            + '<table class="table table-condensed">'
                            + '<thead>'
                            + '<tr>'
                            + '<th>Policy</th>'
                            + '<th>Created</th>'
                            + '<th>Owner</th>'
                            + '<th>Comment</th>'
                            + '</tr>'
                            + '</thead>'
                            + '<tr ng-repeat="waiver in waivers">'
                            + '<td>{{waiver.policyName}}</td>'
                            + '<td>{{waiver.createTimeStr}}</td>'
                            + '<td>{{waiver.ownerName}}</td>'
                            + '<td>{{waiver.comment}}</td>'
                            + '</tr>'
                            + '</table>'
                            + '</div>'
                            + '<div class="modal-footer">'
                            + '<span class="alert alert-error" ng-show="appError" style="padding-right:200px;">{{appError}}</span>'
                            + '<button type="button" class="btn btn-primary" data-dismiss="modal">Close</button>'
                            + '</div>' + '</div>');

    angular.bootstrap($('#componentExistingWaiverModal')[0], ['ComponentInfo']);
  }

  function panelLoadHandler(event, data) {
    // insert a new row into the comp info tab for the view waivers button
    $('#infoPanelArtifactTable tr:last').after(
            '<tr><td></td><td><button type="none" class="btn btn-primary" data-toggle="modal" '
                    + 'data-target="#componentExistingWaiverModal">View Waivers</button>' + '</td>' + '</tr>');

    // when the button is clicked, get data in the scope and $apply since we are
    // outside of angular
    $('button[data-target="#componentExistingWaiverModal"]').bind('click', function() {
      compInfoScope.hash = data.gav.hash;
      compInfoScope.applicationId = applicationId;
      compInfoScope.viewWaivers();
      compInfoScope.$apply();
    });
  }

  $(document).ready(function(){
    loadAngular();
    // setup our stuff when the panel is being built
    $(document).bind('artifactInfoPanelLoading', panelLoadHandler);
  });
}());