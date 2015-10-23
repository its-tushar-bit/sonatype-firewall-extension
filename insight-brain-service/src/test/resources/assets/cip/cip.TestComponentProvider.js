
angular.module('TestComponentProvider', []).service('SelectedComponent', function() {
  var component = {
    hash: '3102cdd0edd5a05afe00'
  };
  return {
    get: function () {
      return component;
    }
  };
}).service('OwnerContext', function () {
  return {
    ownerId: 'bom1-12345678',
    ownerType: 'application'
  };
});

