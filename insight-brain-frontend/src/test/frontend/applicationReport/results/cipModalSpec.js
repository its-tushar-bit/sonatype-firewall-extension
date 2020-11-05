/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import applicationReportModule from '../../../../main/frontend/applicationReport/module';
import { mapStateToThis } from '../../../../main/frontend/applicationReport/results/cipModal/cipModal';

describe('cipModal', function() {

  let vm, scope, SelectedComponent, Coordinates, ComponentUtil, Properties;

  beforeEach(angular.mock.module(applicationReportModule.name, function($provide) {
    SpecUtil.mockNgRedux($provide);
  }));

  beforeEach(inject(
      function($componentController, $rootScope, _SelectedComponent_, _Coordinates_, _ComponentUtil_, _Properties_) {
        scope = $rootScope.$new();
        SelectedComponent = _SelectedComponent_;
        Coordinates = _Coordinates_;
        ComponentUtil = _ComponentUtil_;
        Properties = _Properties_;
        vm = $componentController('cipModal', {
          $scope: scope
        });
        scope.vm = vm;
        vm.metadata = {
          stageId: 'test-stage-id'
        };
        spyOn(Properties, 'setStageId');
        vm.$onInit();
      }
  ));

  describe('$onInit()', function() {

    it('calls Properties.setStageId', function() {
      expect(Properties.setStageId).toHaveBeenCalledWith('test-stage-id');
    });

    describe('vm.selectedComponent watcher', function() {
      let component;

      beforeEach(function() {
        spyOn(SelectedComponent, 'toggle');
        spyOn(Coordinates, 'set');
        spyOn(ComponentUtil, 'enhanceWithComponentIdentifier');
        spyOn(Properties, 'setHash');
        spyOn(Properties, 'setFilename');
        spyOn(Properties, 'setProprietary');
        spyOn(Properties, 'setMatchState');
        spyOn(Properties, 'setDependencyType');

        component = {
          hash: '1249e25aebb15358bedd',
          matchState: 'test-match-state',
          identificationSource: 'test-identification-source',
          componentIdentifier: {
            coordinates: 'coordinates',
            format: 'format'
          },
          dependencyInfo: { isDirectDependency: false }
        };
      });

      it('sets SelectedComponent global state', function() {
        vm.selectedComponent = component;
        scope.$digest();
        expect(SelectedComponent.toggle).toHaveBeenCalledWith(component);
      });

      it('sets Coordinates global state', function() {
        vm.selectedComponent = component;
        scope.$digest();
        expect(Coordinates.set).toHaveBeenCalledWith('format', 'coordinates');
      });

      it('calls Properties.setHash', function() {
        vm.selectedComponent = component;
        scope.$digest();
        expect(Properties.setHash).toHaveBeenCalledWith('1249e25aebb15358bedd');
      });

      it('calls Properties.setFilename with component coordinates if matchState is "unknown"', function() {
        component.matchState = 'unknown';
        component.coordinates = 'unknown component coordinates';
        vm.selectedComponent = component;
        scope.$digest();
        expect(Properties.setFilename).toHaveBeenCalledWith('unknown component coordinates');
      });

      it('calls Properties.setFilename with null if matchState is not "unknown"', function() {
        vm.selectedComponent = component;
        scope.$digest();
        expect(Properties.setFilename).toHaveBeenCalledWith(null);
      });

      it('calls Properties.setProprietary with component\'s proprietary property if it is truthy', function() {
        component.proprietary = 'no';
        vm.selectedComponent = component;
        scope.$digest();
        expect(Properties.setProprietary).toHaveBeenCalledWith('no');
      });

      it('calls Properties.setProprietary with false if component\' proprietary property is falsy', function() {
        component.proprietary = undefined;
        vm.selectedComponent = component;
        scope.$digest();
        expect(Properties.setProprietary).toHaveBeenCalledWith(false);
      });

      it('calls Properties.setMatchState', function() {
        vm.selectedComponent = component;
        scope.$digest();
        expect(Properties.setMatchState).toHaveBeenCalledWith('test-match-state');
      });

      it('calls Properties.setDependencyType with "transitive" when isDirectDependency is false', function() {
        vm.selectedComponent = component;
        scope.$digest();
        expect(Properties.setDependencyType).toHaveBeenCalledWith('transitive');
      });

      it('calls Properties.setDependencyType with "direct" when isDirectDependency is true', function() {
        vm.selectedComponent = {
          ...component,
          dependencyInfo: { isDirectDependency: true }
        };
        scope.$digest();
        expect(Properties.setDependencyType).toHaveBeenCalledWith('direct');
      });

      it('calls Properties.setDependencyType with undefined when dependencyInfo is undefined', function() {
        vm.selectedComponent = { ...component, dependencyInfo: undefined };
        scope.$digest();
        expect(Properties.setDependencyType).toHaveBeenCalledWith(undefined);
      });

      it('enhances legacy report data with component identifier', function() {
        vm.selectedComponent = component;
        scope.$digest();
        expect(ComponentUtil.enhanceWithComponentIdentifier).toHaveBeenCalledWith(component);
      });

      it('handles null value', function() {
        vm.selectedComponent = null;
        scope.$digest();
        expect(SelectedComponent.toggle).not.toHaveBeenCalled();
        expect(Coordinates.set).not.toHaveBeenCalled();
        expect(ComponentUtil.enhanceWithComponentIdentifier).not.toHaveBeenCalled();
        expect(Properties.setHash).not.toHaveBeenCalled();
        expect(Properties.setFilename).not.toHaveBeenCalled();
        expect(Properties.setProprietary).not.toHaveBeenCalled();
        expect(Properties.setMatchState).not.toHaveBeenCalled();
      });
    });

    describe('modal.closing event handler', function() {
      it('un-selects  component in SelectedComponent global state', function() {
        spyOn(SelectedComponent, 'toggle');
        scope.$parent.$broadcast('modal.closing');
        expect(SelectedComponent.toggle).toHaveBeenCalledWith();
      });
    });
  });

  describe('$onDestroy()', function() {
    it('unsubscribes from redux store', function() {
      expect(vm.unsubscribeFromReduxStore).not.toHaveBeenCalled();
      vm.$onDestroy();
      expect(vm.unsubscribeFromReduxStore).toHaveBeenCalledTimes(1);
    });
  });

  describe('previous()', function() {
    it('selects component with previous index', function() {
      vm.selectedComponentIndex = 1;
      vm.previous();
      expect(vm.selectComponent).toHaveBeenCalledWith(0);
    });
  });

  describe('next()', function() {
    it('selects component with next index', function() {
      vm.selectedComponentIndex = 0;
      vm.next();
      expect(vm.selectComponent).toHaveBeenCalledWith(1);
    });
  });

  describe('isPreviousDisabled()', function() {
    it('returns true if selectedComponentIndex is 0', function() {
      vm.selectedComponentIndex = 0;
      expect(vm.isPreviousDisabled()).toBe(true);
    });
    it('returns true if selectedComponentIndex is less than 0', function() {
      vm.selectedComponentIndex = -1;
      expect(vm.isPreviousDisabled()).toBe(true);
    });
    it('returns false if selectedComponentIndex is greater than 0', function() {
      vm.selectedComponentIndex = 1;
      expect(vm.isPreviousDisabled()).toBe(false);
    });
  });

  describe('isNextDisabled()', function() {
    beforeEach(function() {
      vm.selectedReport = {
        displayedEntries: ['foo', 'bar']
      };
    });
    it('returns true if selectedComponentIndex is last index', function() {
      vm.selectedComponentIndex = 1;
      expect(vm.isNextDisabled()).toBe(true);
    });
    it('returns true if selectedComponentIndex is greater than last index', function() {
      vm.selectedComponentIndex = 2;
      expect(vm.isNextDisabled()).toBe(true);
    });
    it('returns false if selectedComponentIndex is less than last index', function() {
      vm.selectedComponentIndex = 0;
      expect(vm.isNextDisabled()).toBe(false);
    });
  });

  describe('mapStateToThis', () => {
    describe('when selectedRootAncestor is not set', function() {
      it('sets selectedComponent using selectedComponentIndex and does not set previousComponent', function() {
        const selectedComponent = {foo: 'bar'};
        const state = {
          applicationReport: {
            selectedRootAncestor: null,
            selectedComponentIndex: 1,
            selectedComponent: selectedComponent,
            selectedReport: {
              displayedEntries: [{}, selectedComponent, {}]
            }
          }
        };

        const output = mapStateToThis(state);
        expect(output.selectedComponent).toBe(selectedComponent);
        expect(output.previousComponent).toBeNull();
      });
    });

    describe('when selectedRootAncestor is set', function() {
      it('sets selectedComponent to selectedRootAncestor and sets previousComponent using selectedComponentIndex',
          function() {
            const selectedComponent = {
              displayName: {
                parts: [
                  {field: 'Group', value: 'org.springframework'},
                  {value: ' : '},
                  {field: 'Artifact', value: 'spring-expression'},
                  {value: ' : '},
                  {field: 'Version', value: '3.2.4.RELEASE'}
                ]
              }
            };
            const selectedRootAncestor = {foo: 'baz'};
            const state = {
              applicationReport: {
                selectedRootAncestor,
                selectedComponentIndex: 0,
                selectedComponent: selectedComponent,
                selectedReport: {
                  displayedEntries: [selectedComponent]
                }
              }
            };

            const output = mapStateToThis(state);
            expect(output.selectedComponent).toBe(selectedRootAncestor);
            expect(output.previousComponent).toBe(selectedComponent);
          });
    });
  });
});
