/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular */
(function() {
  'use strict';
  var module = angular.module('ResourceModule', []);

  function createTemplateFn(object) {
    object = object || {};
    return function() {
      return object;
    };
  }

  module.service('CLMResource', function($q, $http, $parse) {
    function Store(config) {
      var store = [],
          error = false,
          storeDeferred = null,
          resourceStore = this;

      config.id = config.id || 'id';
      config.template = angular.isFunction(config.template) ? config.template : createTemplateFn(config.template);
      config.relationalConfigs = config.relationalConfigs || [];

      function checkDeferredResolve(deferredObject, resolve, countDown) {
        if (countDown <= 0) {
          deferredObject.resolve(resolve);
        }
      }

      function getErrorFn(deferred) {
        return function(data, status, headers, config) {
          deferred.reject({
            data: data,
            status: status,
            headers: headers,
            config: config
          });
        };
      }

      function doLoad() {
        var localDeferred = null;

        storeDeferred = localDeferred = $q.defer();

        $http.get(config.url, { params: config.params }).success(function(data) {
          if (localDeferred === storeDeferred) {
            var result = [];
            
            if (config.dataProperty) {
              data = data[config.dataProperty];
            }
            
            var relationsToLoad = data.length * Object.keys(config.relationalConfigs).length;
            var loadChildResource = function(parentResource, childResource, property) {
              $http.get(childResource.config.url,
                  { params: childResource.config.params }).success(function(data) {
                childResource.$updateOriginal(data);
                $parse(property).assign(parentResource, childResource);
                relationsToLoad--;
                checkDeferredResolve(storeDeferred, store, relationsToLoad);
              }).error(function() {
                error = true;
              }).error(getErrorFn(storeDeferred));
            };

            angular.forEach(data, function(obj) {
              var resource = new Resource(obj, false);
              result.push(resource);

              for (var relationalProperty in config.relationalConfigs) {
                if (config.relationalConfigs.hasOwnProperty(relationalProperty)) {
                  var relationalConfig = config.relationalConfigs[relationalProperty];
                  var linkedResource = new LinkedResource([], angular.copy(relationalConfig));

                  // URL should be a function taking the parent resource as an argument
                  if (jQuery.isFunction(linkedResource.config.url)) {
                    linkedResource.config.url = linkedResource.config.url(resource);
                  }

                  loadChildResource(resource, linkedResource, relationalProperty);
                }
              }
            });
            store.splice(0, store.length);
            store.push.apply(store, result);
            checkDeferredResolve(storeDeferred, store, relationsToLoad);
          }
        }).error(function() {
              error = true;
            }).error(getErrorFn(storeDeferred));
      }

      resourceStore.get = function() {
        if (error || !storeDeferred) {
          // An error occurred previously, or the store hasn't been loaded
          error = false;
          doLoad();
        }
        return storeDeferred.promise;
      };
      resourceStore.create = function(relationalConfigName) {
        var relationalConfig = config.relationalConfigs[relationalConfigName];
        if (relationalConfig) {
          return angular.copy(relationalConfig.template);
        }

        var resource = new Resource(config.template(), true);
        for (var property in config.relationalConfigs) {
          if (config.relationalConfigs.hasOwnProperty(property)) {
            relationalConfig = config.relationalConfigs[property];
            $parse(property).assign(resource, new LinkedResource([], angular.copy(relationalConfig)));
          }
        }
        return resource;
      };
      resourceStore.refresh = function() {
        error = false;
        doLoad();
        return storeDeferred.promise;
      };

      function Resource(originalObject, isNew) {
        var original = null,
            me = this;

        me.$new = isNew;
        me.isDirty = function() {
          var currentProperties = [],
              originalProperties = [],
              match = true;
          angular.forEach(original, function(value, key) {
            originalProperties.push(key);
          });
          // Ignore methods we added, or that AngularJS has (prefixed with $$)
          angular.forEach(this, function(value, key) {
            if (!(me[key] instanceof LinkedResource) && resourceStore.objectMethods.indexOf(key) === -1 &&
                !(key.length >= 2 && key.substring(0, 2) === '$$')) {
              currentProperties.push(key);
            }
          });
          if (currentProperties.length !== originalProperties.length) {
            return true;
          }
          var linkedDirt = false;
          angular.forEach(this, function(value, key) {
            if (me[key] instanceof LinkedResource) {
              if (me[key].isDirty()) {
                linkedDirt = true;
                return;
              }
            }
          });
          if (linkedDirt) {
            return true;
          }

          currentProperties.sort();
          originalProperties.sort();
          angular.forEach(currentProperties, function(property, index) {
            if (originalProperties[index] === property) {
              if (typeof original[property] === 'object' || typeof original[property] === 'array') {
                match = match && angular.equals(original[property], me[property]);
              }
              else {
                match = match && original[property] === me[property];
              }
            }
            else {
              match = false;
            }
          });
          return !match;
        };

        me.$updateOriginal = function(updated) {
          original = updated;
          angular.extend(me, angular.copy(original));
        };

        me.$getOriginal = function() {
          return angular.copy(original);
        };

        me.$revert = function() {
          //first clean the data
          angular.forEach(me, function(meValue, meKey) {
            angular.forEach(original, function(origValue, origKey) {
              if (meKey === origKey) {
                delete me[meKey];
              }
            });
          });
          angular.extend(me, angular.copy(original));
          for (var relationalProperty in config.relationalConfigs) {
            if (config.relationalConfigs.hasOwnProperty(relationalProperty)) {
              var relationalResource = $parse(relationalProperty)(me);
              relationalResource.$revert();
            }
          }
        };

        me.$clone = function() {
          var clone = new Resource(original);
          for (var relationalProperty in config.relationalConfigs) {
            if (config.relationalConfigs.hasOwnProperty(relationalProperty)) {
              var originalResource = $parse(relationalProperty)(me);
              var data = [];
              for (var i = 0; i < originalResource.length; i++) {
                data.push(angular.copy(originalResource[i]));
              }
              var linkedResource = new LinkedResource(data, angular.copy(originalResource.config));
              $parse(relationalProperty).assign(clone, linkedResource);
            }
          }
          return clone;
        };

        me.$updateOriginal(originalObject);
      }

      Resource.prototype.$save = function() {
        var deferred = $q.defer(),
            me = this;

        var relationsToSave = Object.keys(config.relationalConfigs).length;

        if (me.$new) {
          // Newly created object
          $http.post(config.url, this, { params: config.params }).success(function(data) {
            var saveRelationalResource = function() {
              relationsToSave--;
              checkDeferredResolve(deferred, me, relationsToSave);
            };
            var errorRelationalResource = function(rejection) {
              deferred.reject(rejection);
            };
            for (var relationalProperty in config.relationalConfigs) {
              if (config.relationalConfigs.hasOwnProperty(relationalProperty)) {
                var relationalResource = $parse(relationalProperty)(me);

                // URL function needs to be resolved using newly created object
                if (jQuery.isFunction(relationalResource.config.url)) {
                  relationalResource.config.url = relationalResource.config.url(data);
                }

                relationalResource.$save().then(saveRelationalResource, errorRelationalResource);
              }
            }
            me.$new = false;
            me.$updateOriginal(data);
            store.push(me);
            checkDeferredResolve(deferred, me, relationsToSave);
          }).error(getErrorFn(deferred));
        }
        else {
          // Update to existing object
          $http.put(config.url, this, { params: config.params }).success(function(data) {
            var properties = [],
                promises = [],
                resourcesToUpdate = [me];

            angular.forEach(config.relationalConfigs, function(descriptor, relationalProperty) {
              properties.push(relationalProperty);
              promises.push($parse(relationalProperty)(me).$save());
            });

            me.$updateOriginal(data);

            // The current resource might be a clone, find & update the original too
            angular.forEach(store, function(storeEntry) {
              if (storeEntry[config.id] === me[config.id] && storeEntry !== me) {
                storeEntry.$updateOriginal(data);
                resourcesToUpdate.push(storeEntry);
              }
            });

            if (promises.length > 0) {
              $q.all(promises).then(function(results) {
                angular.forEach(results, function(response, index) {
                  angular.forEach(resourcesToUpdate, function(rsrc) {
                    rsrc[properties[index]] = response;
                  });
                });
                deferred.resolve(me);
              }, function(reject) {
                deferred.reject(reject);
              });
            }
            else {
              deferred.resolve(me);
            }

          }).error(getErrorFn(deferred));
        }
        return deferred.promise;
      };

      Resource.prototype.$delete = function() {
        var deferred = $q.defer(),
            id = this[config.id],
            url = config.url.charAt(config.url.length - 1) === '/' ? config.url + id : config.url + '/' + id,
            index = -1;

        if (id !== null && angular.isDefined(id)) {
          $http['delete'](url, this, { params: config.params }).success(function() {
            // remove from store
            angular.forEach(store, function(candidate, candidateIndex) {
              if (candidate[config.id] === id) {
                index = candidateIndex;
              }
            });
            if (index !== -1) {
              store.splice(index, 1);
            }
            deferred.resolve(true);
          }).error(getErrorFn(deferred));
        }
        else {
          // new resources shouldn't be part of the store
          deferred.resolve(true);
        }
        return deferred.promise;
      };

      function LinkedResource(originalArray, relationalConfig) {
        var original = angular.copy(originalArray);
        var me = this;
        me.config = relationalConfig;

        for (var i = 0; i < originalArray.length; i++) {
          this.push(originalArray[i]);
        }

        me.isDirty = function() {
          if (original.length !== this.length) {
            return true;
          }
          for (var i = 0; i < this.length; i++) {
            var id = $parse(me.config.id)(this[i]);
            var found = false;
            for (var j = 0; j < original.length; j++) {
              var originalId = $parse(me.config.id)(original[j]);
              if (id === originalId) {
                found = true;
                break;
              }
            }
            if (!found) {
              return true;
            }
          }

          return false;
        };

        me.$updateOriginal = function(updated) {
          original = angular.copy(updated);
          me.length = 0;
          for (var i = 0; i < original.length; i++) {
            me.push(original[i]);
          }
        };

        me.$revert = function() {
          me.length = 0;
          for (var i = 0; i < original.length; i++) {
            me.push(original[i]);
          }
        };
      }

      LinkedResource.prototype = [];
      LinkedResource.prototype.$save = function() {
        var deferred = $q.defer(),
            me = this;

        // Relational data is saved using an array of IDs
        var relationalIDs = [];
        for (var i = 0; i < me.length; i++) {
          var relationalIDValue = $parse(me.config.id)(me[i]);
          relationalIDs.push(relationalIDValue);
        }
        $http.put(me.config.url, relationalIDs, { params: me.config.params }).success(function(data) {
          me.$updateOriginal(data);
          deferred.resolve(me);
        }).error(getErrorFn(deferred));

        return deferred.promise;
      };
    }

    Store.prototype.objectMethods = ['isDirty', 'config', '$updateOriginal', '$getOriginal', '$revert', '$clone', '$new'];

    return {
      'getStore': function(config) {
        return new Store(config);
      }
    };
  });
}());