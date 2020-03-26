/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import applicationReportModule from '../../../../../main/frontend/applicationReport/module';
import { serializeComponentIdentifier } from '../../../../../main/frontend/util/componentIdentifierUtils';

import {
  mapStateToThis
} from '../../../../../main/frontend/applicationReport/results/cipModal/rootAncestors/rootAncestors';

describe('rootAncestorsComponent', function() {

  let vm, scope;

  beforeEach(angular.mock.module(applicationReportModule.name, function($provide) {
    SpecUtil.mockNgRedux($provide);
  }));

  beforeEach(inject(
      function($componentController, $rootScope) {
        scope = $rootScope.$new();
        vm = $componentController('rootAncestors', {
          $scope: scope
        });
        scope.vm = vm;
        vm.$onInit();
      }
  ));

  beforeEach(function() {
    vm.selectedReport = {
      allEntries: [
        {
          serializedComponentIdentifier: serializeComponentIdentifier({
            format: 'maven',
            coordinates: {
              artifactId: 'foo',
              extension: 'jar',
              groupId: 'test',
              version: 1
            }
          })
        },
        {
          serializedComponentIdentifier: serializeComponentIdentifier({
            format: 'maven',
            coordinates: {
              artifactId: 'bar',
              extension: 'jar',
              groupId: 'test',
              version: 2
            }
          })
        },
        {
          serializedComponentIdentifier: serializeComponentIdentifier({
            format: 'maven',
            coordinates: {
              artifactId: 'baz',
              extension: 'jar',
              groupId: 'test',
              version: 3
            }
          })
        },
        {
          serializedComponentIdentifier: serializeComponentIdentifier({
            format: 'maven',
            coordinates: {
              artifactId: 'bla',
              extension: 'jar',
              groupId: 'test',
              version: 4
            }
          })
        }
      ]
    };
  });

  it('sets vm.showAll to false by default', function() {
    expect(vm.showAll).toBe(false);
  });

  describe('$onInit()', function() {
    describe('vm.selectedComponent watcher', function() {
      describe('vm.rootAncestors', function() {
        it('is set to empty array if dependencyInfo.rootAncestors is undefined', function() {
          vm.selectedComponent = {
            dependencyInfo: undefined
          };
          scope.$digest();
          expect(vm.rootAncestors).toEqual([]);
        });

        it('is set to empty array if dependencyInfo.rootAncestors is empty', function() {
          vm.selectedComponent = {
            dependencyInfo: []
          };
          scope.$digest();
          expect(vm.rootAncestors).toEqual([]);
        });

        it('is set to matching components in selectedReport.allEntries', function() {
          vm.selectedComponent = {
            dependencyInfo: {
              rootAncestors: [
                {
                  format: 'maven',
                  coordinates: {
                    artifactId: 'foo',
                    extension: 'jar',
                    groupId: 'test',
                    version: 1
                  }
                },
                {
                  format: 'maven',
                  coordinates: {
                    artifactId: 'baz',
                    extension: 'jar',
                    groupId: 'test',
                    version: 3
                  }
                }
              ]
            }
          };

          scope.$digest();
          expect(vm.rootAncestors.length).toBe(2);
          expect(vm.rootAncestors).toContain(vm.selectedReport.allEntries[0]);
          expect(vm.rootAncestors).toContain(vm.selectedReport.allEntries[2]);
        });

        it('is set to empty array if selected component is a Direct dependency', function() {
          vm.selectedComponent = {
            dependencyInfo: {
              isDirectDependency: true,
              rootAncestors: [
                {
                  format: 'maven',
                  coordinates: {
                    artifactId: 'foo',
                    extension: 'jar',
                    groupId: 'test',
                    version: 1
                  }
                },
                {
                  format: 'maven',
                  coordinates: {
                    artifactId: 'baz',
                    extension: 'jar',
                    groupId: 'test',
                    version: 3
                  }
                }
              ]
            }
          };

          scope.$digest();
          expect(vm.rootAncestors).toEqual([]);
        });

        it('is set to empty array if there are no matching components in selectedReport.allEntries', function() {
          vm.selectedComponent = {
            dependencyInfo: {
              rootAncestors: [
                {
                  format: 'maven',
                  coordinates: {
                    artifactId: 'foo',
                    extension: 'jar',
                    groupId: 'test',
                    version: 5
                  }
                },
                {
                  format: 'maven',
                  coordinates: {
                    artifactId: 'baz',
                    extension: 'jar',
                    groupId: 'test',
                    version: 6
                  }
                }
              ]
            }
          };

          scope.$digest();
          expect(vm.rootAncestors).toEqual([]);
        });
      });

      describe('vm.isShowMoreLinkDisplayed', function() {
        it('is set to false if there was no rootAncestors found', function() {
          vm.selectedComponent = {
            dependencyInfo: {
              rootAncestors: [
                {
                  format: 'maven',
                  coordinates: {
                    artifactId: 'foo',
                    extension: 'jar',
                    groupId: 'test',
                    version: 5
                  }
                }
              ]
            }
          };

          scope.$digest();
          expect(vm.isShowMoreLinkDisplayed).toBe(false);
        });

        it('is set to false if vm.rootAncestors length is 3 or less', function() {
          vm.selectedComponent = {
            dependencyInfo: {
              rootAncestors: [
                {
                  format: 'maven',
                  coordinates: {
                    artifactId: 'foo',
                    extension: 'jar',
                    groupId: 'test',
                    version: 1
                  }
                },
                {
                  format: 'maven',
                  coordinates: {
                    artifactId: 'bar',
                    extension: 'jar',
                    groupId: 'test',
                    version: 2
                  }
                },
                {
                  format: 'maven',
                  coordinates: {
                    artifactId: 'baz',
                    extension: 'jar',
                    groupId: 'test',
                    version: 3
                  }
                }
              ]
            }
          };

          scope.$digest();
          expect(vm.rootAncestors.length).toBe(3);
          expect(vm.isShowMoreLinkDisplayed).toBe(false);
        });

        it('is set to true if vm.rootAncestors length is more then 3', function() {
          vm.selectedComponent = {
            dependencyInfo: {
              rootAncestors: [
                {
                  format: 'maven',
                  coordinates: {
                    artifactId: 'foo',
                    extension: 'jar',
                    groupId: 'test',
                    version: 1
                  }
                },
                {
                  format: 'maven',
                  coordinates: {
                    artifactId: 'bar',
                    extension: 'jar',
                    groupId: 'test',
                    version: 2
                  }
                },
                {
                  format: 'maven',
                  coordinates: {
                    artifactId: 'baz',
                    extension: 'jar',
                    groupId: 'test',
                    version: 3
                  }
                },
                {
                  format: 'maven',
                  coordinates: {
                    artifactId: 'bla',
                    extension: 'jar',
                    groupId: 'test',
                    version: 4
                  }
                }
              ]
            }
          };

          scope.$digest();
          expect(vm.rootAncestors.length).toBe(4);
          expect(vm.isShowMoreLinkDisplayed).toBe(true);
        });
      });
    });
  });

  describe('$onDestroy()', function() {
    it('unsubscribes from redux store', function() {
      expect(vm.unsubscribe).not.toHaveBeenCalled();
      vm.$onDestroy();
      expect(vm.unsubscribe).toHaveBeenCalledTimes(1);
    });
  });

  describe('vm.toggleShowAll', function() {
    it('toggles vm.showAll flag', function() {
      expect(vm.showAll).toBe(false);
      vm.toggleShowAll();
      expect(vm.showAll).toBe(true);
      vm.toggleShowAll();
      expect(vm.showAll).toBe(false);
    });
  });

  describe('vm.getDisplayedRootAncestors', function() {
    beforeEach(function() {
      vm.selectedComponent = {
        dependencyInfo: {
          rootAncestors: [
            {
              format: 'maven',
              coordinates: {
                artifactId: 'foo',
                extension: 'jar',
                groupId: 'test',
                version: 1
              }
            },
            {
              format: 'maven',
              coordinates: {
                artifactId: 'bar',
                extension: 'jar',
                groupId: 'test',
                version: 2
              }
            },
            {
              format: 'maven',
              coordinates: {
                artifactId: 'baz',
                extension: 'jar',
                groupId: 'test',
                version: 3
              }
            },
            {
              format: 'maven',
              coordinates: {
                artifactId: 'bla',
                extension: 'jar',
                groupId: 'test',
                version: 4
              }
            }
          ]
        }
      };
      scope.$digest();
    });

    it('returns all vm.rootAncestors if vm.showAll is true', function() {
      vm.showAll = true;
      const displayedRootAncestors = vm.getDisplayedRootAncestors();
      expect(displayedRootAncestors.length).toBe(4);
      expect(displayedRootAncestors).toContain(vm.selectedReport.allEntries[0]);
      expect(displayedRootAncestors).toContain(vm.selectedReport.allEntries[1]);
      expect(displayedRootAncestors).toContain(vm.selectedReport.allEntries[2]);
      expect(displayedRootAncestors).toContain(vm.selectedReport.allEntries[3]);
    });

    it('returns first 3 entries from vm.rootAncestors if vm.showAll is false', function() {
      vm.showAll = false;
      const displayedRootAncestors = vm.getDisplayedRootAncestors();
      expect(displayedRootAncestors.length).toBe(3);
      expect(displayedRootAncestors).toContain(vm.selectedReport.allEntries[0]);
      expect(displayedRootAncestors).toContain(vm.selectedReport.allEntries[1]);
      expect(displayedRootAncestors).toContain(vm.selectedReport.allEntries[2]);
    });
  });

  describe('vm.isRootAncestorsSectionDisplayed', function() {
    it('is false if vm.rootAncestors is empty', function() {
      vm.rootAncestors = [];
      expect(vm.isRootAncestorsSectionDisplayed()).toBe(false);
    });

    it('is true if vm.rootAncestors is not empty', function() {
      vm.rootAncestors = [{}];
      expect(vm.isRootAncestorsSectionDisplayed()).toBe(true);
    });
  });

  describe('mapStateToThis', function() {
    it('sets selectedReport', function() {
      const state = {
        applicationReport: {
          selectedReport: {
            displayedEntries: []
          }
        }
      };

      const output = mapStateToThis(state);
      expect(output.selectedReport).toBe(state.applicationReport.selectedReport);
    });

    describe('when selectedRootAncestor is not set', function() {
      it('sets selectedComponent using selectedComponentIndex', function() {
        const selectedComponent = {foo: 'bar'};
        const state = {
          applicationReport: {
            selectedRootAncestor: null,
            selectedComponentIndex: 1,
            selectedReport: {
              displayedEntries: [{}, selectedComponent, {}]
            }
          }
        };

        const output = mapStateToThis(state);
        expect(output.selectedComponent).toBe(selectedComponent);
      });
    });

    describe('when selectedRootAncestor is set', function() {
      it('sets selectedComponent to selectedRootAncestor', function() {
        const selectedRootAncestor = {foo: 'baz'};
        const selectedComponent = {foo: 'bar'};
        const state = {
          applicationReport: {
            selectedRootAncestor,
            selectedComponentIndex: 0,
            selectedReport: {
              displayedEntries: [selectedComponent]
            }
          }
        };

        const output = mapStateToThis(state);
        expect(output.selectedComponent).toBe(selectedRootAncestor);
      });
    });
  });
});
