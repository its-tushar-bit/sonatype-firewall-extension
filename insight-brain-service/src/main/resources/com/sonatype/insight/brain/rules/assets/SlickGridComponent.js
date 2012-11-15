var SlickGridComponent = function() {
    return {
        require: '?ngModel',
        restrict: 'E',
        replace: true,
        template: '<div class="borderedTable"></div>',
        link: function($scope, element, attrs) {
        	var grid;
            var data = [];
            var dataView = new Slick.Data.DataView();
            
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
            
            grid = new Slick.Grid('#' + attrs.id, dataView, tableDef.columns, tableDef.options);
            grid.setSelectionModel(tableDef.selectionModel);
            grid.dataView = dataView;
            dataView.syncGridSelection(grid, true);
            angular.forEach(tableDef.plugins, function(plugin) {
            	grid.registerPlugin(plugin);
            });

            var redraw = function(newScopeData) {
            	dataView.beginUpdate();
            	dataView.setItems(newScopeData);
            	dataView.endUpdate();
                grid.invalidate();
            };

            $scope.$watch(attrs.data, redraw, true);
            $scope[attrs.id] = grid;
        }
    }
}