var SlickGridComponent = function() {
    return {
        require: '?ngModel',
        restrict: 'E',
        replace: true,
        template: '<div class="borderedTable"></div>',
        link: function($scope, element, attrs) {
        	var grid;
            var data = [];
            
            var deepLocate = function(obj, key) {
            	var keys = key.split('.');
            	
            	if ( keys.length == 1 ) {
            		return obj[key];
            	}
            	
            	var subkey = keys[0].trim();
            	
            	keys.splice(0,1);
            	
            	return deepLocate(obj[subkey],keys.join('.')); 
            }
            
            var tableDef = deepLocate($scope, attrs.tableDef);
            
            grid = new Slick.Grid('#' + attrs.id, [], tableDef.columns, tableDef.options);
            grid.setSelectionModel(tableDef.selectionModel);
            angular.forEach(tableDef.plugins, function(plugin) {
            	grid.registerPlugin(plugin);
            });

            var redraw = function(newScopeData) {
                grid.setData(newScopeData);
                grid.render();
            };

            $scope.$watch(attrs.data, redraw, true);
            $scope[attrs.id] = grid;
        }
    }
}