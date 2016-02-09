window.CLM = {
  path: '../brain/'
};
window.angularDebug = true;
var SpecUtil = {
  setupProviders: function(applicationId, organizationId) {
    angular.module('ApplicationIdProvider', []).service('ApplicationId',function() {
      // TODO Are ui-router parameters encoded or decoded?
      return {
        encoded: function() {
          return applicationId;
        }
      };
    }).service('OrganizationId', function() {
          return {
            encoded: function() {
              return organizationId;
            }
          };
        });
  },
  getTemplate: function(url) {
    // Karma Html2Js stores these html snippets on the window
    if (window.__html__ && window.__html__['src/main/resources/assets/' + url.replace('../', '')]) {
      return window.__html__['src/main/resources/assets/' + url.replace('../', '')];
    }

    url = url.split('/');
    if (url[0] === '..') {
      url.splice(0, 1);
    }

    if (location.hostname) {
      url = 'src/main/resources/assets/' + url.join('/');
    }
    else {
      url = 'src/' + url.join('/');
    }

    var data = null;
    $.ajax({
      async: false,
      dataType: 'html',
      url: url,
      success: function(responseData) {
        data = responseData;
      }
    });
    return data;
  },

  respondWithTemplate: function(httpBackend, templateUrl) {
    var ownerTreeViewTemplate = SpecUtil.getTemplate('../../../frontend/' + templateUrl);
    httpBackend.expectGET(templateUrl).respond(ownerTreeViewTemplate);
  },

  toRegExp: function toRegExp(url) {
    var addedTimestamp = false, parts = url.split('?');
    //Note that i go through all of this funkiness as the params are added to the request
    //alphabetically from the angular code, so when testing query param matching, need
    //to make sure the timestamp param is in the proper position
    if (parts.length > 1) {
      parts = parts[1].split('&');
      
      for (var i = 0 ; i < parts.length ; i++) {
        if ('timestamp' < parts[i]) {
          url = url.replace(parts[i], 'timestamp=[0-9]+&' + parts[i]);
          addedTimestamp = true;
          break;
        }
      }
    }
    
    return new RegExp(url.replace('?', '\\?') + (!addedTimestamp ? ((url.indexOf('?') < 0 ? '\\?' : '&') + 'timestamp=[0-9]+') : ''));
  },

  setInput: function(inputElement, val) {
    var evt = document.createEvent('HTMLEvents');
    inputElement.val(val);

    inject(function($sniffer) {
      var type = inputElement[0].localName;
      evt.initEvent($sniffer.hasEvent(type) ? type : 'change', false, false);
    });
    inputElement[0].dispatchEvent(evt);
  },

  mockPermissionService: function($provide) {
    $provide.factory('PermissionService', ['$q', function ($q) {
      var deferred = $q.defer();
      deferred.resolve();
      function fn() {
        return deferred.promise;
      }
      return {
        isAuthorized: fn
      };
    }]);
  },

  promiseWrapper: function($q) {
    return function(promise) {
      var deferred = $q.defer();

      promise.then(function() {
        deferred.resolve.apply(deferred, arguments);
      }, function() {
        deferred.reject.apply(deferred, arguments);
      });

      return deferred.promise;
    };
  }
};
