/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import DependencyInfoGenerator from '../../../main/frontend/applicationReport/DependencyInfoGenerator';

describe('DependencyInfoGenerator', function() {
  it('handles null dependencies', function() {
    const dependencyInfoGenerator = DependencyInfoGenerator(null);
    expect(dependencyInfoGenerator.getDependencyInfo({
      componentIdentifier: {}
    })).toBeNull();
  });

  it('handles empty dependencies', function() {
    const dependencyInfoGenerator = DependencyInfoGenerator({});
    expect(dependencyInfoGenerator.getDependencyInfo({
      componentIdentifier: {}
    })).toBeNull();
  });

  it('handles empty dependencyGraph', function() {
    const dependencyInfoGenerator = DependencyInfoGenerator({dependencyGraph: []});
    expect(dependencyInfoGenerator.getDependencyInfo({
      componentIdentifier: {}
    })).toBeNull();
  });

  it('handles dependencyGraph with an empty object', function() {
    const dependencyInfoGenerator = DependencyInfoGenerator({dependencyGraph: [{}]});
    expect(dependencyInfoGenerator.getDependencyInfo({
      componentIdentifier: {}
    })).toBeNull();
  });

  describe('getDependencyInfo', function() {
    // dependencyGraph:
    //
    //  logback-access          bar
    //     |      \              |
    //     |  org.mortbay.jetty  |
    //     | /                 \ |
    //    foo                   baz
    const dependencies = {
      dependencyGraph: [
        {
          children: [
            {
              componentIdentifier: {
                format: 'maven',
                coordinates: {
                  artifactId: 'logback-access',
                  classifier: '',
                  extension: 'jar',
                  groupId: 'ch.qos.logback',
                  version: '0.6'
                }
              }
            },
            {
              componentIdentifier: {
                format: 'maven',
                coordinates: {
                  artifactId: 'bar',
                  classifier: '',
                  extension: 'jar',
                  groupId: 'test',
                  version: '1'
                }
              }
            }
          ]
        },
        {
          componentIdentifier: {
            format: 'maven',
            coordinates: {
              artifactId: 'logback-access',
              classifier: '',
              extension: 'jar',
              groupId: 'ch.qos.logback',
              version: '0.6'
            }
          },
          children: [
            {
              componentIdentifier: {
                format: 'maven',
                coordinates: {
                  artifactId: 'jetty',
                  classifier: '',
                  extension: 'jar',
                  groupId: 'org.mortbay.jetty',
                  version: '6.1.15'
                }
              }
            },
            {
              componentIdentifier: {
                format: 'maven',
                coordinates: {
                  artifactId: 'foo',
                  classifier: '',
                  extension: 'jar',
                  groupId: 'test',
                  version: '1'
                }
              }
            }
          ]
        },
        {
          componentIdentifier: {
            format: 'maven',
            coordinates: {
              artifactId: 'bar',
              classifier: '',
              extension: 'jar',
              groupId: 'test',
              version: '1'
            }
          },
          children: [
            {
              componentIdentifier: {
                format: 'maven',
                coordinates: {
                  artifactId: 'baz',
                  classifier: '',
                  extension: 'jar',
                  groupId: 'test',
                  version: '1'
                }
              }
            }
          ]
        },
        {
          componentIdentifier: {
            format: 'maven',
            coordinates: {
              artifactId: 'jetty',
              classifier: '',
              extension: 'jar',
              groupId: 'org.mortbay.jetty',
              version: '6.1.15'
            }
          },
          children: [
            {
              componentIdentifier: {
                format: 'maven',
                coordinates: {
                  artifactId: 'foo',
                  classifier: '',
                  extension: 'jar',
                  groupId: 'test',
                  version: '1'
                }
              }
            },
            {
              componentIdentifier: {
                format: 'maven',
                coordinates: {
                  artifactId: 'baz',
                  classifier: '',
                  extension: 'jar',
                  groupId: 'test',
                  version: '1'
                }
              }
            }
          ]
        },
        {
          componentIdentifier: {
            format: 'maven',
            coordinates: {
              artifactId: 'foo',
              classifier: '',
              extension: 'jar',
              groupId: 'test',
              version: '1'
            }
          }
        },
        {
          componentIdentifier: {
            format: 'maven',
            coordinates: {
              artifactId: 'baz',
              classifier: '',
              extension: 'jar',
              groupId: 'test',
              version: '1'
            }
          }
        }
      ]
    };

    it('sets isDirectDependency to true for direct dependency', function() {
      const reportEntry = {
        componentIdentifier: {
          format: 'maven',
          coordinates: {
            artifactId: 'logback-access',
            classifier: 'foo',
            extension: 'jar',
            groupId: 'ch.qos.logback',
            version: '0.6'
          }
        }
      };
      const dependencyInfoGenerator = DependencyInfoGenerator(dependencies);
      expect(dependencyInfoGenerator.getDependencyInfo(reportEntry)).toEqual({isDirectDependency: true});
    });

    it('sets isDirectDependency to false and generates rootAncestors for transitive dependency', function() {
      const reportEntry = {
        componentIdentifier: {
          format: 'maven',
          coordinates: {
            artifactId: 'jetty',
            classifier: 'foo',
            extension: 'jar',
            groupId: 'org.mortbay.jetty',
            version: '6.1.15'
          }
        }
      };
      const dependencyInfoGenerator = DependencyInfoGenerator(dependencies);
      expect(dependencyInfoGenerator.getDependencyInfo(reportEntry)).toEqual({
        isDirectDependency: false,
        rootAncestors: [{
          format: 'maven',
          coordinates: {
            artifactId: 'logback-access',
            classifier: '',
            extension: 'jar',
            groupId: 'ch.qos.logback',
            version: '0.6'
          }
        }]
      });
    });

    it('handles multiple rootAncestors', function() {
      const reportEntry = {
        componentIdentifier: {
          format: 'maven',
          coordinates: {
            artifactId: 'baz',
            classifier: 'foo',
            extension: 'jar',
            groupId: 'test',
            version: '1'
          }
        }
      };

      const dependencyInfoGenerator = DependencyInfoGenerator(dependencies);
      expect(dependencyInfoGenerator.getDependencyInfo(reportEntry)).toEqual({
        isDirectDependency: false,
        rootAncestors: [{
          format: 'maven',
          coordinates: {
            artifactId: 'logback-access',
            classifier: '',
            extension: 'jar',
            groupId: 'ch.qos.logback',
            version: '0.6'
          }
        },
        {
          format: 'maven',
          coordinates: {
            artifactId: 'bar',
            classifier: '',
            extension: 'jar',
            groupId: 'test',
            version: '1'
          }
        }]
      });
    });

    it('dedupes rootAncestors', function() {
      const reportEntry = {
        componentIdentifier: {
          format: 'maven',
          coordinates: {
            artifactId: 'foo',
            classifier: 'foo',
            extension: 'jar',
            groupId: 'test',
            version: '1'
          }
        }
      };

      const dependencyInfoGenerator = DependencyInfoGenerator(dependencies);
      expect(dependencyInfoGenerator.getDependencyInfo(reportEntry)).toEqual({
        isDirectDependency: false,
        rootAncestors: [{
          format: 'maven',
          coordinates: {
            artifactId: 'logback-access',
            classifier: '',
            extension: 'jar',
            groupId: 'ch.qos.logback',
            version: '0.6'
          }
        }]
      });
    });

    it('returns null if no dependency info found for given entry', function() {
      const reportEntry = {
        componentIdentifier: {
          format: 'maven',
          coordinates: {
            artifactId: 'jetty',
            classifier: '',
            extension: 'jar',
            groupId: 'org.mortbay.jetty',
            version: '6.1.16'
          }
        }
      };
      const dependencyInfoGenerator = DependencyInfoGenerator(dependencies);
      expect(dependencyInfoGenerator.getDependencyInfo(reportEntry)).toBeNull();
    });

    it('returns null if given entry has no componentIdentifier', function() {
      const reportEntry = {
        componentIdentifier: null
      };
      const dependencyInfoGenerator = DependencyInfoGenerator(dependencies);
      expect(dependencyInfoGenerator.getDependencyInfo(reportEntry)).toBeNull();
    });
  });

  describe('when dependencyGraph has circular dependency', function() {

    it('handles circular dependency between root node and child node', function() {
      const dependencies = {
        dependencyGraph: [
          {
            children: [
              {
                componentIdentifier: {
                  format: 'maven',
                  coordinates: {
                    artifactId: 'logback-access',
                    classifier: '',
                    extension: 'jar',
                    groupId: 'ch.qos.logback',
                    version: '0.6'
                  }
                }
              }
            ]
          },
          {
            componentIdentifier: {
              format: 'maven',
              coordinates: {
                artifactId: 'logback-access',
                classifier: '',
                extension: 'jar',
                groupId: 'ch.qos.logback',
                version: '0.6'
              }
            },
            children: [
              {
                componentIdentifier: {
                  format: 'maven',
                  coordinates: {
                    artifactId: 'foo',
                    classifier: '',
                    extension: 'jar',
                    groupId: 'test',
                    version: '1'
                  }
                }
              }
            ]
          },
          {
            componentIdentifier: {
              format: 'maven',
              coordinates: {
                artifactId: 'foo',
                classifier: '',
                extension: 'jar',
                groupId: 'test',
                version: '1'
              }
            },
            children: [
              {
                componentIdentifier: {
                  format: 'maven',
                  coordinates: {
                    artifactId: 'logback-access',
                    classifier: '',
                    extension: 'jar',
                    groupId: 'ch.qos.logback',
                    version: '0.6'
                  }
                }
              }
            ]
          }
        ]
      };

      const parent = {
        componentIdentifier: {
          format: 'maven',
          coordinates: {
            artifactId: 'logback-access',
            classifier: 'foo',
            extension: 'jar',
            groupId: 'ch.qos.logback',
            version: '0.6'
          }
        }
      };

      const child = {
        componentIdentifier: {
          format: 'maven',
          coordinates: {
            artifactId: 'foo',
            classifier: 'foo',
            extension: 'jar',
            groupId: 'test',
            version: '1'
          }
        }
      };

      const dependencyInfoGenerator = DependencyInfoGenerator(dependencies);
      expect(dependencyInfoGenerator.getDependencyInfo(parent)).toEqual({isDirectDependency: true});
      expect(dependencyInfoGenerator.getDependencyInfo(child)).toEqual({
        isDirectDependency: false,
        rootAncestors: [{
          format: 'maven',
          coordinates: {
            artifactId: 'logback-access',
            classifier: '',
            extension: 'jar',
            groupId: 'ch.qos.logback',
            version: '0.6'
          }
        }]
      });
    });

    it('handles circular dependency between child nodes', function() {
      // dependencyGraph with circular dependency from child bar to child foo)
      const dependencies = {
        dependencyGraph: [
          {
            children: [
              {
                componentIdentifier: {
                  format: 'maven',
                  coordinates: {
                    artifactId: 'logback-access',
                    classifier: '',
                    extension: 'jar',
                    groupId: 'ch.qos.logback',
                    version: '0.6'
                  }
                }
              }
            ]
          },
          {
            componentIdentifier: {
              format: 'maven',
              coordinates: {
                artifactId: 'logback-access',
                classifier: '',
                extension: 'jar',
                groupId: 'ch.qos.logback',
                version: '0.6'
              }
            },
            children: [
              {
                componentIdentifier: {
                  format: 'maven',
                  coordinates: {
                    artifactId: 'foo',
                    classifier: '',
                    extension: 'jar',
                    groupId: 'test',
                    version: '1'
                  }
                }
              }
            ]
          },
          {
            componentIdentifier: {
              format: 'maven',
              coordinates: {
                artifactId: 'foo',
                classifier: '',
                extension: 'jar',
                groupId: 'test',
                version: '1'
              }
            },
            children: [
              {
                componentIdentifier: {
                  format: 'maven',
                  coordinates: {
                    artifactId: 'bar',
                    classifier: '',
                    extension: 'jar',
                    groupId: 'test',
                    version: '1'
                  }
                }
              }
            ]
          },
          {
            componentIdentifier: {
              format: 'maven',
              coordinates: {
                artifactId: 'bar',
                classifier: '',
                extension: 'jar',
                groupId: 'test',
                version: '1'
              }
            },
            children: [
              {
                componentIdentifier: {
                  format: 'maven',
                  coordinates: {
                    artifactId: 'foo',
                    classifier: '',
                    extension: 'jar',
                    groupId: 'test',
                    version: '1'
                  }
                }
              }
            ]
          }
        ]
      };

      const parent = {
        componentIdentifier: {
          format: 'maven',
          coordinates: {
            artifactId: 'logback-access',
            classifier: 'foo',
            extension: 'jar',
            groupId: 'ch.qos.logback',
            version: '0.6'
          }
        }
      };

      const foo = {
        componentIdentifier: {
          format: 'maven',
          coordinates: {
            artifactId: 'foo',
            classifier: 'foo',
            extension: 'jar',
            groupId: 'test',
            version: '1'
          }
        }
      };

      const bar = {
        componentIdentifier: {
          format: 'maven',
          coordinates: {
            artifactId: 'bar',
            classifier: 'foo',
            extension: 'jar',
            groupId: 'test',
            version: '1'
          }
        }
      };

      const dependencyInfoGenerator = DependencyInfoGenerator(dependencies);
      expect(dependencyInfoGenerator.getDependencyInfo(parent)).toEqual({isDirectDependency: true});
      expect(dependencyInfoGenerator.getDependencyInfo(foo)).toEqual({
        isDirectDependency: false,
        rootAncestors: [{
          format: 'maven',
          coordinates: {
            artifactId: 'logback-access',
            classifier: '',
            extension: 'jar',
            groupId: 'ch.qos.logback',
            version: '0.6'
          }
        }]
      });
      expect(dependencyInfoGenerator.getDependencyInfo(bar)).toEqual({
        isDirectDependency: false,
        rootAncestors: [{
          format: 'maven',
          coordinates: {
            artifactId: 'logback-access',
            classifier: '',
            extension: 'jar',
            groupId: 'ch.qos.logback',
            version: '0.6'
          }
        }]
      });
    });

    it('handles circular dependency of a root node to itself', function() {
      const dependencies = {
        dependencyGraph: [
          {
            children: [
              {
                componentIdentifier: {
                  format: 'maven',
                  coordinates: {
                    artifactId: 'logback-access',
                    classifier: '',
                    extension: 'jar',
                    groupId: 'ch.qos.logback',
                    version: '0.6'
                  }
                }
              }
            ]
          },
          {
            componentIdentifier: {
              format: 'maven',
              coordinates: {
                artifactId: 'logback-access',
                classifier: '',
                extension: 'jar',
                groupId: 'ch.qos.logback',
                version: '0.6'
              }
            },
            children: [
              {
                componentIdentifier: {
                  format: 'maven',
                  coordinates: {
                    artifactId: 'logback-access',
                    classifier: '',
                    extension: 'jar',
                    groupId: 'ch.qos.logback',
                    version: '0.6'
                  }
                }
              }
            ]
          }
        ]
      };

      const reportEntry = {
        componentIdentifier: {
          format: 'maven',
          coordinates: {
            artifactId: 'logback-access',
            classifier: 'foo',
            extension: 'jar',
            groupId: 'ch.qos.logback',
            version: '0.6'
          }
        }
      };

      const dependencyInfoGenerator = DependencyInfoGenerator(dependencies);
      expect(dependencyInfoGenerator.getDependencyInfo(reportEntry)).toEqual({isDirectDependency: true});
    });

    it('handles circular dependency of a child node to itself', function() {
      const dependencies = {
        dependencyGraph: [
          {
            children: [
              {
                componentIdentifier: {
                  format: 'maven',
                  coordinates: {
                    artifactId: 'logback-access',
                    classifier: '',
                    extension: 'jar',
                    groupId: 'ch.qos.logback',
                    version: '0.6'
                  }
                }
              }
            ]
          },
          {
            componentIdentifier: {
              format: 'maven',
              coordinates: {
                artifactId: 'logback-access',
                classifier: '',
                extension: 'jar',
                groupId: 'ch.qos.logback',
                version: '0.6'
              }
            },
            children: [
              {
                componentIdentifier: {
                  format: 'maven',
                  coordinates: {
                    artifactId: 'foo',
                    classifier: '',
                    extension: 'jar',
                    groupId: 'test',
                    version: '1'
                  }
                }
              }
            ]
          },
          {
            componentIdentifier: {
              format: 'maven',
              coordinates: {
                artifactId: 'foo',
                classifier: '',
                extension: 'jar',
                groupId: 'test',
                version: '1'
              }
            },
            children: [
              {
                componentIdentifier: {
                  format: 'maven',
                  coordinates: {
                    artifactId: 'foo',
                    classifier: '',
                    extension: 'jar',
                    groupId: 'test',
                    version: '1'
                  }
                }
              }
            ]
          }
        ]
      };

      const parent = {
        componentIdentifier: {
          format: 'maven',
          coordinates: {
            artifactId: 'logback-access',
            classifier: 'foo',
            extension: 'jar',
            groupId: 'ch.qos.logback',
            version: '0.6'
          }
        }
      };

      const foo = {
        componentIdentifier: {
          format: 'maven',
          coordinates: {
            artifactId: 'foo',
            classifier: 'foo',
            extension: 'jar',
            groupId: 'test',
            version: '1'
          }
        }
      };

      const dependencyInfoGenerator = DependencyInfoGenerator(dependencies);
      expect(dependencyInfoGenerator.getDependencyInfo(parent)).toEqual({isDirectDependency: true});
      expect(dependencyInfoGenerator.getDependencyInfo(foo)).toEqual({
        isDirectDependency: false,
        rootAncestors: [{
          format: 'maven',
          coordinates: {
            artifactId: 'logback-access',
            classifier: '',
            extension: 'jar',
            groupId: 'ch.qos.logback',
            version: '0.6'
          }
        }]
      });
    });
  });
});
