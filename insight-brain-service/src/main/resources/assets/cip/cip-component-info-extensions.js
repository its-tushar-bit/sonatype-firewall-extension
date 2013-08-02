/**
 * @license Copyright (c) 2013 Sonatype, Inc. All rights reserved. Includes the
 *          third-party code listed at
 *          http://links.sonatype.com/products/clm/attributions. "Sonatype" is a
 *          trademark of Sonatype, Inc.
 */
/* global angular, $, window, CLM, setTimeout, applicationId */
(function() {
  'use strict';

  function processResults($scope, results) {
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
  }

  function panelLoadHandler(event, data) {
    $('body')
            .append(
                    '<div id="componentExistingWaiverModal" ng-controller="ComponentInfoController" data-keyboard="false" data-backdrop="static" class="modal hide fade">'
                            + '<div class="modal-header">'
                            + '<button type="button" class="close" ng-click="close()" aria-hidden="true">&times;</button>'
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
                            + '<button type="button" class="btn btn-primary" ng-click="close()">Close</button>'
                            + '</div>' + '</div>');

    // insert a new row into the comp info tab for the view waivers button
    $('#infoPanelArtifactTable tr:last').after(
            '<tr><td></td><td><button type="none" class="btn btn-primary" data-toggle="modal" '
                    + 'data-target="#componentExistingWaiverModal">View Waivers</button>' + '</td>' + '</tr>');

    angular.bootstrap($('#componentExistingWaiverModal')[0], ['ComponentInfo']);

    // when the button is clicked, get data in the scope and $apply since we are
    // outside of angular
    $('button[data-target="#componentExistingWaiverModal"]').bind('click', function() {
      var scope = angular.element('#componentExistingWaiverModal').scope();
      scope.$apply(function() {
        scope.hash = data.gav.hash;
        scope.applicationId = applicationId;
        scope.viewWaivers();
      });
    });
  }

  function doBind() {
    $(document).bind('artifactInfoPanelLoading', panelLoadHandler);
  }

  var componentInfoApp = angular.module('ComponentInfo', []);

  componentInfoApp.controller('ComponentInfoController', [
      '$scope',
      '$http',
      '$q',
      '$timeout',
      function($scope, $http, $q, $timeout) {
        $scope.viewWaivers = function() {
          // get the waivers and policies from the server
          var policyWaiverPromise = $http.get(CLM.path + 'rest/policyWaiver/application/' + $scope.applicationId
                  + '/component/' + $scope.hash), policyPromise = $http.get(CLM.path + 'rest/policy/application/'
                  + $scope.applicationId + '/applicable');

          $q.all([policyWaiverPromise, policyPromise]).then(function(results) {
            processResults($scope, results);
          }, function() {
            $scope.waiverLoading = false;
            $scope.appError = arguments[0];
          });
        };
        $scope.close = function() {
          $('#componentExistingWaiverModal').modal('hide');
          // simply giving the animation enough time to run
          $timeout(function() {
            $('#componentExistingWaiverModal').remove();
          }, 500);
        };
        // this is solely for test purposes, since the angular mock is junking
        // all events in between tests
        $scope.rebind = function() {
          doBind();
        }
      }]);

  doBind();

}());