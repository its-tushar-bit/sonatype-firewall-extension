var SlickGridComponent = function() {
    return {
        require: '?ngModel',
        restrict: 'E',
        replace: true,
        template: '<div></div>',
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
            
            var hideEmptyMessage = function() {
            	$('#' + attrs.id).find('.message').remove();
            }
            
            var showEmptyMessage = function() {
            	var table = $('#' + attrs.id);
                
                if (tableDef.emptyMessage && table) {
                	var offset = table.offset();
                	if (offset) {
                		hideEmptyMessage();
                		var viewport = table.find('.slick-viewport');
                		viewport.append('<div class="message">' + tableDef.emptyMessage + '</div>');
            			var message = table.find('.message');
            			message.css('left', (viewport.outerWidth()/2 - message.outerWidth()/2) + 'px');
            			message.css('top', (viewport.outerHeight() - message.outerHeight())/2);
                	}
                }
            }
           
            grid = new Slick.Grid('#' + attrs.id, dataView, tableDef.columns, tableDef.options);
            grid.setSelectionModel(tableDef.selectionModel);
            grid.dataView = dataView;
            dataView.syncGridSelection(grid, true);
            angular.forEach(tableDef.plugins, function(plugin) {
            	grid.registerPlugin(plugin);
            });

            grid.redraw = function(newScopeData) {
            	if (newScopeData) {
	            	var now = new Date().getTime();
	            	for ( var i = 0 ; i < newScopeData.length ; i++ ) {
	            		if (!newScopeData[i].id) {
	            			newScopeData[i].id = '' + (now + i);
	            		}
	            	}
	            	dataView.beginUpdate();
	            	dataView.setItems(newScopeData);
	            	dataView.endUpdate();
	                grid.invalidate();
            	}
                
                if (dataView.getLength() == 0) {
                	showEmptyMessage();
                } else {
                	hideEmptyMessage();
                }
            };

            $scope.$watch(attrs.data, grid.redraw, true);
            $scope[attrs.id] = grid;
            
            if (dataView.getLength() == 0) {
            	showEmptyMessage();
            } else {
            	hideEmptyMessage();
            }
        }
    }
}