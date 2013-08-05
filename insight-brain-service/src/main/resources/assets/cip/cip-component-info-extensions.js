/**
 * @license Copyright (c) 2013 Sonatype, Inc. All rights reserved. Includes the
 *          third-party code listed at
 *          http://links.sonatype.com/products/clm/attributions. "Sonatype" is a
 *          trademark of Sonatype, Inc.
 */
/* global angular, $, window, CLM, setTimeout, applicationId */

(function() {
  'use strict';

  function appendModal() {
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
                            + '<div ng-show="!waivers.length"><em style="margin-left:25px;">No waivers assigned</em></div>'
                            + '<table ng-show="waivers.length" class="table table-condensed">'
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
                            + '<td>{{waiver.createTime | date:"yyyy-MM-dd"}}</td>'
                            + '<td>{{waiver.ownerName}}</td>'
                            + '<td>{{waiver.comment}}</td>'
                            + '<td><button class="btn btn-mini" ng-click="remove(waiver)" title="Remove {{placeHolder}}">'
                            + '<i class="icon-minus-sign"></i></button></td>'
                            + '</tr>'
                            + '</table>'
                            + '</div>'
                            + '<div class="modal-footer">'
                            + '<span class="alert alert-error" ng-show="appError" style="margin-right:10px;">{{appError}}</span>'
                            + '<button type="button" class="btn btn-primary" ng-click="close()">Close</button>'
                            + '</div>' + '</div>');
  }

  function appendButton() {
    // insert a new row into the comp info tab for the view waivers button
    $('#infoPanelArtifactTable tr:last').after(
            '<tr><td></td><td><button type="none" class="btn btn-primary" data-toggle="modal" '
                    + 'data-target="#componentExistingWaiverModal">View Waivers</button>' + '</td>' + '</tr>');
  }

  function panelLoadHandler(event, data) {
    if (!$('button[data-target="#componentExistingWaiverModal"]').length) {
      appendButton();
      
      // when the button is clicked, get data in the scope and $apply since we
      // are outside of angular
      $('button[data-target="#componentExistingWaiverModal"]').bind('click', function() {
        appendModal();
        var scope = data.scope;
        //support for testing, will only bootstrap if necessary
        if (!scope) {
          angular.bootstrap($('#componentExistingWaiverModal')[0], ['ComponentInfo']);
          scope = angular.element('#componentExistingWaiverModal').scope();
        }
        scope.$apply(function() {
          scope.hash = data.gav.hash;
          scope.applicationId = applicationId;
          scope.viewWaivers();
        });
      });
    }
  }

  //bind to the info panel loading event
  function doBind() {
    $(document).unbind('artifactInfoPanelLoading', panelLoadHandler);
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
          function processResults(results) {
            $scope.waiverLoading = false;
            $scope.waivers = results[0].data;

            // process the results to add policy name and owner name
            $.each($scope.waivers, function(waiverIndex, waiver) {
              $.each(results[1].data.policiesByOwner, function(policyOwnerIndex, policyOwner) {
                if (waiver.ownerId === policyOwner.ownerId) {
                  waiver.type = policyOwner.ownerType;
                  waiver.ownerName = policyOwner.ownerName;
                  //for apps, we need to find the publicId and set that as the ownerId
                  if (waiver.type === 'application') {
                    $.each(results[2].data, function(applicationIndex, application){
                      if (application.id === waiver.ownerId) {
                        waiver.ownerId = application.publicId;
                        return false;
                      }
                    });
                  }
                }
                $.each(policyOwner.policies, function(policyIndex, policy) {
                  if (waiver.policyId === policy.id) {
                    waiver.policyName = policy.name;
                    return false;
                  }
                });
              });
            });
          }
          // get the waivers and policies from the server
          var policyWaiverPromise = $http.get(CLM.path + 'rest/policyWaiver/application/' + $scope.applicationId
                  + '/component/' + $scope.hash), policyPromise = $http.get(CLM.path + 'rest/policy/application/'
                  + $scope.applicationId + '/applicable'), applicationPromise = $http.get(CLM.path + 'rest/application');

          $q.all([policyWaiverPromise, policyPromise, applicationPromise]).then(function(results) {
            processResults(results);
          }, function() {
            $scope.waiverLoading = false;
            $scope.appError = arguments[0];
          });
        };
        $scope.close = function() {
          // simply giving the animation enough time to run
          $('#componentExistingWaiverModal').on('hidden.bs.modal', function () {
            $('#componentExistingWaiverModal').remove();
          });

          $('#componentExistingWaiverModal').modal('hide');
        };
        $scope.remove = function(waiver) {
          $http['delete'](CLM.path + 'rest/policyWaiver/' + waiver.type + '/' + waiver.ownerId + '/' + waiver.id).success(function(){
            $scope.waivers.splice($scope.waivers.indexOf(waiver),1);
          }).error(function(){$scope.appError = arguments[0]});
        };
        $scope.rebind = function() {
          doBind();
        }
      }]);

  doBind();
}());