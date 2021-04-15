/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular, jQuery */
import { omit } from 'ramda';

import storeObserveTypesConstant from './resource/store.observe.types.constant';

var module = angular.module('ResourceModule', []);
export default module;

function createTemplateFn(object) {
  object = object || {};
  return function () {
    return object;
  };
}

module.constant('store.observe.type.constant', storeObserveTypesConstant);

module.service('StoreFactory', [
  '$q',
  '$http',
  '$parse',
  '$rootScope',
  'store.observe.type.constant',
  function ($q, $http, $parse, $rootScope, StoreObserveTypeConstant) {
    /**
     * Store Constructor.
     * Store represents RESTful collection resource (like '/rest/application' or '/rest/organization')
     * It caches the collection as an array of Resource objects. (see Resource API below)
     *
     * Store API:
     *  - get(): Promise<[Resource]>     - returns cached collection (lazily loaded if empty)
     *  - getById(id): Promise<Resource> - returns Resource with provided id
     *  - create(): Resource             - creates new Resource (but doesn't save it to collection)
     *  - refresh(): Promise<[Resource]> - refreshes cached collection and returns it
     *  - observe(callback): Function    - registers callback to observe store changes (see store.observe.type.constant)
     *                                     returns unregister function
     *  - peek(): [Resource]             - returns currently cached Resources
     *
     * Resource API:
     *  - $save(): Promise<Resource>     - Saves Resource to collections and updates cache
     *  - $delete(): Promise             - Deletes Resource from collections and removes it from cache
     *  - $revert(): void                - Reverts Resource to it's original state
     *  - $clone(): Resource             - Creates new Resource object - copy of itself
     *  - isDirty(): boolean
     *  - $new: boolean
     *
     * @param config object with following properties:
     *  - url: <String>               - resource URL
     *  - type: <String>              - resource type (used only in Promise reject message)
     *
     *  optional config properties:
     *  - params: <Object>            - resource URL parameters
     *  - template: <Object|Function> - object to use as a template for a new resource
     *  - dataProperty: <String>      - property of GET result to use as resource (if not provided -
     *                                  the actual result object is used)
     *  - id: <String>                - name of the property to use as ID (default is 'id')
     *  - relationalConfigs: <Array>  - used to create LinkedResources. (we only use this in licenseGroupStore)
     *  - transientProperties: <Array>- list of property names to filter out of the JSON that is sent to the server
     *
     */
    function Store(config) {
      var store = [],
        error = false,
        storeDeferred = null,
        resourceStore = this,
        observers = [];

      config.id = config.id || 'id';
      config.template = angular.isFunction(config.template) ? config.template : createTemplateFn(config.template);
      config.relationalConfigs = config.relationalConfigs || [];
      config.transientProperties = config.transientProperties || [];

      function checkDeferredResolve(deferredObject, result, countDown) {
        if (countDown <= 0) {
          deferredObject.resolve(result);
        }
      }

      function getErrorFn(deferred) {
        return function (errorResponse) {
          deferred.reject({
            data: errorResponse.data,
            status: errorResponse.status,
            headers: errorResponse.headers,
            config: errorResponse.config,
          });
        };
      }

      function doLoad() {
        var localDeferred = null;

        storeDeferred = localDeferred = $q.defer();

        $http.get(config.url, { params: config.params }).then(
          function (response) {
            var data = response.data;

            if (localDeferred === storeDeferred) {
              var resources = [];

              if (config.dataProperty) {
                data = data[config.dataProperty];
              }

              // NOTE: if data is not an array this will be NaN and the promise will never be resolved
              var relationsToLoad = data.length * Object.keys(config.relationalConfigs).length;
              var loadChildResource = function (parentResource, childResource, property) {
                $http
                  .get(childResource.config.url, {
                    params: childResource.config.params,
                  })
                  .then(
                    function (response) {
                      childResource.$updateOriginal(response.data);
                      $parse(property).assign(parentResource, childResource);
                      relationsToLoad--;
                      checkDeferredResolve(storeDeferred, store, relationsToLoad);
                    },
                    function (errorResponse) {
                      error = true;
                      getErrorFn(storeDeferred)(errorResponse);
                    }
                  );
              };

              angular.forEach(data, function (obj) {
                var resource = new Resource(obj, false);
                resources.push(resource);

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
              // empty the store and push all the resources
              store.splice(0, store.length);
              store.push.apply(store, resources);

              notifyObservers(StoreObserveTypeConstant.UPDATE, resources);
              checkDeferredResolve(storeDeferred, store, relationsToLoad);
              storeDeferred.isResolved = true;
            }
          },
          function (errorResponse) {
            error = true;
            getErrorFn(storeDeferred)(errorResponse);
          }
        );
      }

      function doRefreshLoad() {
        error = false;
        // If there is an existing unfulfilled request by the store ($$state.status === 0), then it is in a current
        // state of refreshing and doLoad() does not need be called. If not, refresh the store.
        if (!storeDeferred || storeDeferred.promise.$$state.status !== 0) {
          doLoad();
        }
      }

      function notifyObservers() {
        var args = arguments;
        observers.forEach(function (callback) {
          try {
            callback.apply(null, args);
          } catch (e) {
            console.error(e, e.stack);
          }
        });
      }

      resourceStore.get = function () {
        if (error || !storeDeferred) {
          // An error occurred previously, or the store hasn't been loaded
          error = false;
          doLoad();
        }
        return storeDeferred.promise;
      };

      resourceStore.peek = function () {
        return store;
      };

      resourceStore._removeFromStoreByIndex = function (index) {
        if (Number.isInteger(index) && index !== -1) {
          var resource = store[index];
          store.splice(index, 1);
          notifyObservers(StoreObserveTypeConstant.DELETE, [resource]);
        }
      };

      resourceStore.set = function (elements) {
        storeDeferred = $q.defer();

        if (angular.isArray(elements)) {
          angular.forEach(elements, function (obj) {
            store.push(new Resource(obj, false));
          });
        } else {
          store.push(new Resource(elements, false));
        }

        storeDeferred.resolve(store);

        return store;
      };

      resourceStore.observe = function (callback) {
        observers.push(callback);

        return unregister;

        function unregister() {
          observers = observers.filter(function (handler) {
            return handler !== callback;
          });
        }
      };

      resourceStore.getById = function (entityId) {
        function find() {
          var result;
          store.some(function (entity) {
            if (entity[config.id || 'id'] === entityId) {
              result = entity;
              return true;
            }
          });
          return result;
        }

        var result;
        if (storeDeferred && storeDeferred.isResolved && (result = find())) {
          return $q.when(result);
        } else {
          var promise = storeDeferred && storeDeferred.isResolved ? this.refresh() : this.get();
          return promise.then(function () {
            var result = find();

            if (!result) {
              return $q.reject('Could not find an ' + config.type + ' with ID ' + entityId + '.');
            }
            return result;
          });
        }
      };

      resourceStore.create = function (relationalConfigName) {
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
      resourceStore.refresh = function () {
        doRefreshLoad();

        return storeDeferred.promise;
      };

      function Resource(originalObject, isNew) {
        var original = null,
          me = this;

        me.$new = isNew;
        me.isDirty = function () {
          var currentProperties = [],
            originalProperties = [],
            match = true;
          angular.forEach(original, function (value, key) {
            originalProperties.push(key);
          });
          // Ignore methods we added, or that AngularJS has (prefixed with $$)
          angular.forEach(this, function (value, key) {
            if (
              !(me[key] instanceof LinkedResource) &&
              resourceStore.objectMethods.indexOf(key) === -1 &&
              !(key.length >= 2 && key.substring(0, 2) === '$$')
            ) {
              currentProperties.push(key);
            }
          });
          if (currentProperties.length !== originalProperties.length) {
            return true;
          }
          var linkedDirt = false;
          angular.forEach(this, function (value, key) {
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
          angular.forEach(currentProperties, function (property, index) {
            if (originalProperties[index] === property) {
              if (angular.isObject(original[property]) || angular.isArray(original[property])) {
                match = match && angular.equals(original[property], me[property]);
              } else {
                // Note: we consider undefined, empty string and null as equal
                match = match && (original[property] === me[property] || (!original[property] && !me[property]));
              }
            } else {
              match = false;
            }
          });
          return !match;
        };

        me.$updateOriginal = function (updated) {
          original = updated;
          angular.extend(me, angular.copy(original));
        };

        me.$getOriginal = function () {
          return angular.copy(original);
        };

        me.$revert = function () {
          //first clean the data
          angular.forEach(me, function (meValue, meKey) {
            angular.forEach(original, function (origValue, origKey) {
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

        me.$clone = function () {
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

      Resource.prototype.$save = function () {
        const deferred = $q.defer(),
          me = this,
          relationProperties = Object.keys(config.relationalConfigs),
          propertiesToOmit = resourceStore.objectMethods.concat(relationProperties).concat(config.transientProperties),
          // using object spread to avoid prototype properties
          payload = omit(propertiesToOmit, { ...this });

        let relationsToSave = relationProperties.length;

        if (me.$new) {
          // Newly created object
          $http.post(config.url, payload, { params: config.params }).then(function (response) {
            var data = response.data,
              saveRelationalResource = function () {
                relationsToSave--;
                checkDeferredResolve(deferred, me, relationsToSave);
              },
              errorRelationalResource = function (rejection) {
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
          }, getErrorFn(deferred));
        } else {
          // Update to existing object
          $http.put(config.url, payload, { params: config.params }).then(function (response) {
            var properties = [],
              promises = [],
              resourcesToUpdate = [me],
              data = response.data;

            angular.forEach(config.relationalConfigs, function (descriptor, relationalProperty) {
              properties.push(relationalProperty);
              promises.push($parse(relationalProperty)(me).$save());
            });

            me.$updateOriginal(data);

            // The current resource might be a clone, find & update the original too
            angular.forEach(store, function (storeEntry) {
              if (storeEntry[config.id] === me[config.id] && storeEntry !== me) {
                storeEntry.$updateOriginal(data);
                resourcesToUpdate.push(storeEntry);
              }
            });

            if (promises.length > 0) {
              $q.all(promises).then(
                function (results) {
                  angular.forEach(results, function (response, index) {
                    angular.forEach(resourcesToUpdate, function (rsrc) {
                      rsrc[properties[index]] = response;
                    });
                  });
                  deferred.resolve(me);
                },
                function (reject) {
                  deferred.reject(reject);
                }
              );
            } else {
              deferred.resolve(me);
            }
          }, getErrorFn(deferred));
        }
        return deferred.promise.then(function (result) {
          notifyObservers(StoreObserveTypeConstant.UPDATE, [result]);
          $rootScope.$broadcast('resource.data.modified');
          return result;
        });
      };

      Resource.prototype.$delete = function () {
        var deferred = $q.defer(),
          me = this,
          id = me[config.id];

        var queryStringIndex = config.url.indexOf('?');
        var url = queryStringIndex > -1 ? config.url.substring(0, queryStringIndex) : config.url;
        url = url.charAt(url.length - 1) === '/' ? url + id : url + '/' + id;
        url = queryStringIndex > -1 ? url + config.url.substring(queryStringIndex) : url;

        if (id !== null && angular.isDefined(id)) {
          $http['delete'](url, me, { params: config.params }).then(function () {
            // remove from store
            angular.forEach(store, function (candidate, candidateIndex) {
              if (candidate[config.id] === id) {
                resourceStore._removeFromStoreByIndex(candidateIndex);
              }
            });

            deferred.resolve(true);
          }, getErrorFn(deferred));
        } else {
          // new resources shouldn't be part of the store
          deferred.resolve(true);
        }
        return deferred.promise.then(function (result) {
          $rootScope.$broadcast('resource.data.modified');
          return result;
        });
      };

      function LinkedResource(originalArray, relationalConfig) {
        var original = angular.copy(originalArray);
        var me = this;
        me.config = relationalConfig;

        for (var i = 0; i < originalArray.length; i++) {
          this.push(originalArray[i]);
        }

        me.isDirty = function () {
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

        me.$updateOriginal = function (updated) {
          original = angular.copy(updated);
          me.length = 0;
          for (var i = 0; i < original.length; i++) {
            me.push(original[i]);
          }
        };

        me.$revert = function () {
          me.length = 0;
          for (var i = 0; i < original.length; i++) {
            me.push(original[i]);
          }
        };
      }

      LinkedResource.prototype = [];
      LinkedResource.prototype.$save = function () {
        var deferred = $q.defer(),
          me = this;

        // Relational data is saved using an array of IDs
        var relationalIDs = [];
        for (var i = 0; i < me.length; i++) {
          var relationalIDValue = $parse(me.config.id)(me[i]);
          relationalIDs.push(relationalIDValue);
        }
        $http.put(me.config.url, relationalIDs, { params: me.config.params }).then(function (response) {
          me.$updateOriginal(response.data);
          deferred.resolve(me);
        }, getErrorFn(deferred));

        return deferred.promise;
      };
    }

    Store.prototype.objectMethods = [
      'isDirty',
      'config',
      '$updateOriginal',
      '$getOriginal',
      '$revert',
      '$clone',
      '$new',
    ];

    return {
      getStore: function (config) {
        return new Store(config);
      },
    };
  },
]);

module.service('HierarchyStoreFactory', [
  '$http',
  '$q',
  'StoreFactory',
  'CLMContextLocations',
  function ($http, $q, StoreFactory, CLMContextLocations) {
    function getErrorFn(deferred) {
      return function (errorResponse) {
        deferred.reject({
          data: errorResponse.data,
          status: errorResponse.status,
          headers: errorResponse.headers,
          config: errorResponse.config,
        });
      };
    }

    /**
     * HierarchyStore Constructor.
     *
     * Lets say we have the following hierarchy
     * - rootOrg
     *    - myOrg
     *        - myApp
     *
     * Given config.url, such that get(config.url) returns flat collection of hierarchy:
     * - myApp
     *    - policies[]
     * - rootOrg
     *    - policies[]
     * - myOrg
     *    - policies[]
     *
     *
     * Creates Store representing this hierarchy where each object in the hierarchy (owner)
     * - has its children denoted by 'config.storeField' (policies in the example above) converted to Resource objects.
     * - has "store" property - Store instance representing collection of its children
     * - myApp
     *    - policies[Resource]
     *    - store
     *
     * - myOrg
     *    - policies[Resource]
     *    - store
     *
     * - rootOrg
     *    - policies[Resource]
     *    - store
     *
     * Note: owner (an object in hierarchy) itself is not converted into Resource
     *
     * HierarchyStore API
     *  - get(): Promise<[owner]> - returns list of owners
     *  - refresh(): Promise<[owner]> - same as get() excpet reloads the cache
     *  - getById(entityId): Promise<Resource> - search through the children Resources of each owner
     *
     * @param config see Store
     * HierarchyStore specific config parameters
     *  - crudUrl: (ownerType, ownerId) -> URL - function, if provided, called to derive Resource URL,
     *             used for children (owned entities)
     *  - storeField: String - name of the field containing the children (owned entities).
     *                Uses 'entities' by default.@constructor
     */
    function HierarchyStore(config) {
      var storeDeferred,
        error,
        storeConfig = angular.copy(config),
        store = [];

      config.field = config.field || 'entitiesByOwner';
      config.storeField = config.storeField || 'entities';

      function doLoad() {
        var myDeferred = null;

        myDeferred = storeDeferred = $q.defer();

        $http.get(config.url, { params: config.params }).then(
          function (response) {
            var data = response.data;
            if (storeDeferred === myDeferred) {
              angular.forEach(data[config.field], function (owner) {
                if (config.crudUrl) {
                  storeConfig.url = config.crudUrl(
                    owner.ownerType,
                    owner.ownerType === 'application' ? CLMContextLocations.getEntityId() : owner.ownerId
                  );
                }

                var ownerStore = StoreFactory.getStore(angular.copy(storeConfig));
                owner[config.storeField] = ownerStore.set(owner[config.storeField]);
                // note a consumer attempting to get/refresh on the store will not have good results
                owner.store = ownerStore;
              });

              store.splice(0, store.length);
              store.push.apply(store, data[config.field]);

              myDeferred.resolve(store);
              myDeferred.isResolved = true;
            }
          },
          function (errorResponse) {
            getErrorFn(myDeferred)(errorResponse);
            error = true;
          }
        );

        return myDeferred.promise;
      }

      this.get = function () {
        if (error || !storeDeferred) {
          // An error occurred previously, or the store hasn't been loaded
          error = false;
          doLoad();
        }
        return storeDeferred.promise;
      };

      this.getById = function (entityId) {
        function find() {
          var result;
          store.some(function (hierarchyLevel) {
            return hierarchyLevel[config.storeField].some(function (entity) {
              if (entity[storeConfig.id || 'id'] === entityId) {
                result = entity;
                return true;
              }
            });
          });
          return result;
        }

        var result;
        if (storeDeferred && storeDeferred.isResolved && (result = find())) {
          return $q.when(result);
        } else {
          var promise = storeDeferred && storeDeferred.isResolved ? this.refresh() : this.get();
          return promise.then(function () {
            var result = find();

            if (!result) {
              return $q.reject('Could not find an ' + storeConfig.type + ' with ID ' + entityId + '.');
            }
            return result;
          });
        }
      };

      this.refresh = function () {
        error = false;
        doLoad();
        return storeDeferred.promise;
      };
    }

    return {
      getStore: function (config) {
        return new HierarchyStore(config);
      },
    };
  },
]);
