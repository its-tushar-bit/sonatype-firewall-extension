/**
 * @license Copyright (c) 2013 Sonatype, Inc. All rights reserved. Includes the third-party code listed at
 *          http://links.sonatype.com/products/clm/attributions. "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global $, window, CLM, document */
(function() {
  'use strict';

  var modalDiv = null;
  var requestQueue = [];
  
  //override the ajax method and inject our own handling
  var oldAjax = $.ajax;
  $.ajax = function() {
    //used to resend failed requests
    var options = arguments[0];
    //our own deferred that will be returned, to force the callers to wait on our authentication logic
    var deferred = $.Deferred();
    //context to resolve/reject with
    var context = this;
    
    //use the original ajax call
    oldAjax.apply(context, Array.prototype.slice.apply(arguments)).then(function(){
      //success, nothing funky to do, just resolve
      deferred.resolveWith(context, Array.prototype.slice.apply(arguments));
    }, function(jqXHR, textStatus, errorThrown){
      //401 error, time to force them to login
      if (jqXHR.status === 401) {
        //put the request in the queue, as multiple requests may be sent simultaneously
        requestQueue.push(function(){
          oldAjax(options).then(function(){
            deferred.resolveWith(context, Array.prototype.slice.apply(arguments));
          },function(){
            deferred.rejectWith(context, Array.prototype.slice.apply(arguments));
          });
        });
        
        if (!modalDiv) {
          modalDiv = $('<div class="modal" id="loginModal"><form name="loginForm" class="form-horizontal" style="margin-bottom:0px;"><div class="modal-header"><h3>User Login</h3></div><div class="modal-body"><div class="control-group"><label class="control-label" for="login-username">Username</label><div class="controls"><input type="text" id="login-username" placeholder="Enter Username"></div></div><div class="control-group"><label class="control-label" for="login-password">Password</label><div class="controls"><input type="password" id="login-password" placeholder="Enter Password"></div></div></div><div class="modal-footer"><span id="login-error" class="alert alert-error" style="margin-right: 10px; display: none;"></span><button id="login-action" class="btn btn-primary">Sign in</button></div></form></div>').appendTo('body');
          modalDiv.modal({
            backdrop: 'static',
            keyboard: 'false'
          });
        } else {
          modalDiv.modal('show');
        }
        
        $("#login-action").on('click', function(event) {
          event.preventDefault();
          var authz = Base64.encode($('#login-username').val() + ':' + $('#login-password').val());

          //do the login with the original ajax, so we don't hit our code here
          oldAjax({
            url: '../../../../../rest/user/session',
            type: 'POST',
            headers: {
              'Authorization': 'Basic ' + authz
            }
          }).then(function() {
            //login success, go ahead and resend each of the requests
            $.each(requestQueue, function(index, value){
              value();
            });
            
            //clean up
            requestQueue = [];
            modalDiv.modal('hide');
          }, function() {
            $('#login-error').text('Invalid credentials. Please try again.');
            $('#login-error').show();
          });
        });
      } else {
        //non auth error, again nothing funky, just reject
        deferred.rejectWith(context, Array.prototype.slice.apply(arguments));
      }
    });
    
    //make sure to setup these mappings, just as is done in the jquery sources
    deferred.success = deferred.done;
    deferred.error = deferred.fail;
    return deferred;
  };
}());
