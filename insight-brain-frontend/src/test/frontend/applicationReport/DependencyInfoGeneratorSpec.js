/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import DependencyInfoGenerator from '../../../main/frontend/applicationReport/DependencyInfoGenerator';
import { serializeComponentIdentifier } from 'MainRoot/util/componentIdentifierUtils';

describe('DependencyInfoGenerator', function () {
  it('handles null dependencies', function () {
    const dependencyInfoGenerator = DependencyInfoGenerator(null);
    expect(
      dependencyInfoGenerator.getDependencyInfo({
        componentIdentifier: {},
      })
    ).toBeNull();
  });

  it('handles empty dependencies', function () {
    const dependencyInfoGenerator = DependencyInfoGenerator({});
    expect(
      dependencyInfoGenerator.getDependencyInfo({
        componentIdentifier: {},
      })
    ).toBeNull();
  });

  it('handles null dependencyTree', function () {
    const dependencyInfoGenerator = DependencyInfoGenerator({
      dependencyTree: null,
    });
    expect(
      dependencyInfoGenerator.getDependencyInfo({
        componentIdentifier: {},
      })
    ).toBeNull();
  });

  it('handles empty dependencyTree', function () {
    const dependencyInfoGenerator = DependencyInfoGenerator({
      dependencyTree: {},
    });
    expect(
      dependencyInfoGenerator.getDependencyInfo({
        componentIdentifier: {},
      })
    ).toBeNull();
  });

  it('handles nul dependencyTree.children', function () {
    const dependencyInfoGenerator = DependencyInfoGenerator({
      dependencyTree: {
        children: null,
      },
    });
    expect(
      dependencyInfoGenerator.getDependencyInfo({
        componentIdentifier: {},
      })
    ).toBeNull();
  });

  it('handles dependencyTree where all the children are empty modules', function () {
    const dependencyInfoGenerator = DependencyInfoGenerator({
      dependencyTree: {
        packageUrl: 'a',
        children: [
          { packageUrl: 'a1', module: true },
          { packageUrl: 'a2', module: true, children: [] },
          { packageUrl: 'a3', module: true, children: null },
        ],
      },
    });
    expect(
      dependencyInfoGenerator.getDependencyInfo({
        componentIdentifier: {},
      })
    ).toBeNull();
  });

  describe('getDependencyInfo', function () {
    // dependencyTree:
    //
    //  logback-access        module1  module2
    //     |      \               |     /
    //     |  org.mortbay.jetty  bar  bar
    //     | /                 \  |  /   \
    //    foo                    baz     qux
    const dependencies = {
      dependencyTree: {
        children: [
          {
            componentIdentifier: {
              format: 'maven',
              coordinates: {
                artifactId: 'logback-access',
                classifier: '',
                extension: 'jar',
                groupId: 'ch.qos.logback',
                version: '0.6',
              },
            },
            children: [
              {
                componentIdentifier: {
                  format: 'maven',
                  coordinates: {
                    artifactId: 'foo',
                    extension: 'jar',
                    groupId: 'test',
                    version: '1',
                  },
                },
              },
              {
                componentIdentifier: {
                  format: 'maven',
                  coordinates: {
                    artifactId: 'jetty',
                    classifier: '',
                    extension: 'jar',
                    groupId: 'org.mortbay.jetty',
                    version: '6.1.15',
                  },
                },
                children: [
                  {
                    componentIdentifier: {
                      format: 'maven',
                      coordinates: {
                        artifactId: 'foo',
                        extension: 'jar',
                        groupId: 'test',
                        version: '1',
                      },
                    },
                  },
                  {
                    componentIdentifier: {
                      format: 'maven',
                      coordinates: {
                        artifactId: 'baz',
                        extension: 'jar',
                        groupId: 'test',
                        version: '1',
                      },
                    },
                  },
                ],
              },
            ],
          },
          // multi-module with duplicate direct but different transitives
          {
            packageUrl: 'module1',
            module: true,
            children: [
              {
                componentIdentifier: {
                  format: 'maven',
                  coordinates: {
                    artifactId: 'bar',
                    extension: 'jar',
                    groupId: 'test',
                    version: '1',
                  },
                },
                children: [
                  {
                    componentIdentifier: {
                      format: 'maven',
                      coordinates: {
                        artifactId: 'baz',
                        extension: 'jar',
                        groupId: 'test',
                        version: '1',
                      },
                    },
                  },
                ],
              },
            ],
          },
          {
            packageUrl: 'module2',
            module: true,
            children: [
              {
                componentIdentifier: {
                  format: 'maven',
                  coordinates: {
                    artifactId: 'bar',
                    extension: 'jar',
                    groupId: 'test',
                    version: '1',
                  },
                },
                children: [
                  {
                    componentIdentifier: {
                      format: 'maven',
                      coordinates: {
                        artifactId: 'baz',
                        extension: 'jar',
                        groupId: 'test',
                        version: '1',
                      },
                    },
                  },
                  {
                    componentIdentifier: {
                      format: 'maven',
                      coordinates: {
                        artifactId: 'qux',
                        extension: 'jar',
                        groupId: 'test',
                        version: '1',
                      },
                    },
                  },
                ],
              },
            ],
          },
        ],
      },
    };

    it('sets isDirectDependency to true for direct dependency', function () {
      const reportEntry = {
        componentIdentifier: {
          format: 'maven',
          coordinates: {
            artifactId: 'logback-access',
            classifier: '',
            extension: 'jar',
            groupId: 'ch.qos.logback',
            version: '0.6',
          },
        },
      };
      const dependencyInfoGenerator = DependencyInfoGenerator(dependencies);
      expect(dependencyInfoGenerator.getDependencyInfo(reportEntry)).toEqual({
        isDirectDependency: true,
      });
    });

    it('sets isDirectDependency to true for direct dependency duplicated in different modules', function () {
      const reportEntry = {
        componentIdentifier: {
          format: 'maven',
          coordinates: {
            artifactId: 'bar',
            extension: 'jar',
            groupId: 'test',
            version: '1',
          },
        },
      };
      const dependencyInfoGenerator = DependencyInfoGenerator(dependencies);
      expect(dependencyInfoGenerator.getDependencyInfo(reportEntry)).toEqual({
        isDirectDependency: true,
      });
    });

    it('sets isDirectDependency to false and generates rootAncestors for transitive dependency', function () {
      const reportEntry = {
        componentIdentifier: {
          format: 'maven',
          coordinates: {
            artifactId: 'jetty',
            classifier: '',
            extension: 'jar',
            groupId: 'org.mortbay.jetty',
            version: '6.1.15',
          },
        },
      };
      const dependencyInfoGenerator = DependencyInfoGenerator(dependencies);
      expect(dependencyInfoGenerator.getDependencyInfo(reportEntry)).toEqual({
        isDirectDependency: false,
        rootAncestors: [
          serializeComponentIdentifier({
            format: 'maven',
            coordinates: {
              artifactId: 'logback-access',
              classifier: '',
              extension: 'jar',
              groupId: 'ch.qos.logback',
              version: '0.6',
            },
          }),
        ],
      });
    });

    it('handles rootAncestors from a module', function () {
      const reportEntry = {
        componentIdentifier: {
          format: 'maven',
          coordinates: {
            artifactId: 'qux',
            extension: 'jar',
            groupId: 'test',
            version: '1',
          },
        },
      };

      const dependencyInfoGenerator = DependencyInfoGenerator(dependencies);
      expect(dependencyInfoGenerator.getDependencyInfo(reportEntry)).toEqual({
        isDirectDependency: false,
        rootAncestors: [
          serializeComponentIdentifier({
            format: 'maven',
            coordinates: {
              artifactId: 'bar',
              extension: 'jar',
              groupId: 'test',
              version: '1',
            },
          }),
        ],
      });
    });

    it('handles multiple rootAncestors', function () {
      const reportEntry = {
        componentIdentifier: {
          format: 'maven',
          coordinates: {
            artifactId: 'baz',
            extension: 'jar',
            groupId: 'test',
            version: '1',
          },
        },
      };

      const dependencyInfoGenerator = DependencyInfoGenerator(dependencies);
      expect(dependencyInfoGenerator.getDependencyInfo(reportEntry)).toEqual({
        isDirectDependency: false,
        rootAncestors: [
          serializeComponentIdentifier({
            format: 'maven',
            coordinates: {
              artifactId: 'logback-access',
              classifier: '',
              extension: 'jar',
              groupId: 'ch.qos.logback',
              version: '0.6',
            },
          }),
          serializeComponentIdentifier({
            format: 'maven',
            coordinates: {
              artifactId: 'bar',
              extension: 'jar',
              groupId: 'test',
              version: '1',
            },
          }),
        ],
      });
    });

    it('dedupes rootAncestors', function () {
      const reportEntry = {
        componentIdentifier: {
          format: 'maven',
          coordinates: {
            artifactId: 'foo',
            extension: 'jar',
            groupId: 'test',
            version: '1',
          },
        },
      };

      const dependencyInfoGenerator = DependencyInfoGenerator(dependencies);
      expect(dependencyInfoGenerator.getDependencyInfo(reportEntry)).toEqual({
        isDirectDependency: false,
        rootAncestors: [
          serializeComponentIdentifier({
            format: 'maven',
            coordinates: {
              artifactId: 'logback-access',
              classifier: '',
              extension: 'jar',
              groupId: 'ch.qos.logback',
              version: '0.6',
            },
          }),
        ],
      });
    });

    it('dedupes rootAncestors from different modules', function () {
      const reportEntry = {
        componentIdentifier: {
          format: 'maven',
          coordinates: {
            artifactId: 'baz',
            extension: 'jar',
            groupId: 'test',
            version: '1',
          },
        },
      };

      const dependencyInfoGenerator = DependencyInfoGenerator(dependencies);
      expect(dependencyInfoGenerator.getDependencyInfo(reportEntry)).toEqual({
        isDirectDependency: false,
        rootAncestors: [
          serializeComponentIdentifier({
            format: 'maven',
            coordinates: {
              artifactId: 'logback-access',
              classifier: '',
              extension: 'jar',
              groupId: 'ch.qos.logback',
              version: '0.6',
            },
          }),
          serializeComponentIdentifier({
            format: 'maven',
            coordinates: {
              artifactId: 'bar',
              extension: 'jar',
              groupId: 'test',
              version: '1',
            },
          }),
        ],
      });
    });

    it('returns null if no dependency info found for given entry', function () {
      const reportEntry = {
        componentIdentifier: {
          format: 'maven',
          coordinates: {
            artifactId: 'jetty',
            classifier: '',
            extension: 'jar',
            groupId: 'org.mortbay.jetty',
            version: '6.1.16',
          },
        },
      };
      const dependencyInfoGenerator = DependencyInfoGenerator(dependencies);
      expect(dependencyInfoGenerator.getDependencyInfo(reportEntry)).toBeNull();
    });

    it('returns null if given entry has no componentIdentifier', function () {
      const reportEntry = {
        componentIdentifier: null,
      };
      const dependencyInfoGenerator = DependencyInfoGenerator(dependencies);
      expect(dependencyInfoGenerator.getDependencyInfo(reportEntry)).toBeNull();
    });

    it('returns empty object for direct dependency when dependency data is included in bom data', function () {
      const reportEntry = {
        directDependency: true,
        componentIdentifier: {
          format: 'maven',
          coordinates: {
            artifactId: 'logback-access',
            classifier: '',
            extension: 'jar',
            groupId: 'ch.qos.logback',
            version: '0.6',
          },
        },
      };
      const dependencyInfoGenerator = DependencyInfoGenerator(dependencies, {
        isDependencyDataIncludedInBomData: true,
      });
      expect(dependencyInfoGenerator.getDependencyInfo(reportEntry)).toEqual({});
    });

    it('returns only root ancestors data for transitive dependency when dependency data is included in bom data', function () {
      const reportEntry = {
        directDependency: false,
        componentIdentifier: {
          format: 'maven',
          coordinates: {
            artifactId: 'jetty',
            classifier: '',
            extension: 'jar',
            groupId: 'org.mortbay.jetty',
            version: '6.1.15',
          },
        },
      };
      const dependencyInfoGenerator = DependencyInfoGenerator(dependencies, {
        isDependencyDataIncludedInBomData: true,
      });
      expect(dependencyInfoGenerator.getDependencyInfo(reportEntry)).toEqual({
        rootAncestors: [
          serializeComponentIdentifier({
            format: 'maven',
            coordinates: {
              artifactId: 'logback-access',
              classifier: '',
              extension: 'jar',
              groupId: 'ch.qos.logback',
              version: '0.6',
            },
          }),
        ],
      });
    });
  });
});
