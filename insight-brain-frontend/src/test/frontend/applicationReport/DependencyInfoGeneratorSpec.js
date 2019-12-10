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
          }
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
            classifier: '',
            extension: 'jar',
            groupId: 'ch.qos.logback',
            version: '0.6'
          }
        }
      };
      const dependencyInfoGenerator = DependencyInfoGenerator(dependencies);
      expect(dependencyInfoGenerator.getDependencyInfo(reportEntry)).toEqual({isDirectDependency: true});
    });

    it('sets isDirectDependency to false for transitive dependency', function() {
      const reportEntry = {
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
      };
      const dependencyInfoGenerator = DependencyInfoGenerator(dependencies);
      expect(dependencyInfoGenerator.getDependencyInfo(reportEntry)).toEqual({isDirectDependency: false});
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
});
