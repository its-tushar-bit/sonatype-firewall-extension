import * as applicationReportService from '../../../main/frontend/applicationReport/applicationReportService';

describe('applicationReportService', function() {
  describe('createReportEntries', function() {
    const bomData = {
          aaData: [{
            hash: 'fooHash',
            componentIdentifier: {
              format: 'a-name',
              coordinates: {
                name: 'foo',
                version: '1'
              }
            }
          }, {
            hash: 'barHash',
            componentIdentifier: {
              format: 'maven',
              coordinates: {
                groupId: 'barGroup',
                artifactId: 'bar',
                version: '2'
              }
            }
          }]
        },
        unknownJSData = {
          aaData: [{
            hash: 'bazHash',
            otherProp: 'baz'
          }]
        };

    it('creates entries from report V3 data', function() {
      const policyThreatData = {
            version: 3,
            aaData: [{
              hash: 'fooHash',
              componentIdentifier: {
                format: 'a-name',
                coordinates: {
                  name: 'foo',
                  version: '1'
                }
              },
              policyId: '546fa744e6434a9e855e1ef5bcaf2067',
              policyName: 'Security-High',
              policyThreatLevel: 9,
              activeViolations: [{
                policyId: '546fa744e6434a9e855e1ef5bcaf2067',
                policyName: 'Security-High',
                policyThreatLevel: 9,
                waived: false,
                grandfathered: false
              }],
              waivedViolations: [{
                policyId: '546fa744e6434a9e855e1ef5bcaf2068',
                policyName: 'License-High',
                policyThreatLevel: 8,
                waived: true,
                grandfathered: false
              }],
              allViolations: [{
                policyId: '546fa744e6434a9e855e1ef5bcaf2067',
                policyName: 'Security-High',
                policyThreatLevel: 9,
                waived: false,
                grandfathered: false
              }, {
                policyId: '546fa744e6434a9e855e1ef5bcaf2068',
                policyName: 'License-High',
                policyThreatLevel: 8,
                waived: true,
                grandfathered: false
              }]
            }, {
              hash: 'barHash',
              componentIdentifier: {
                format: 'maven',
                coordinates: {
                  groupId: 'barGroup',
                  artifactId: 'bar',
                  version: '2'
                }
              },
              policyId: '546fa744e6434a9e855e1ef5bcaf2067',
              policyName: 'Security-High',
              policyThreatLevel: 9,
              activeViolations: [{
                policyId: '546fa744e6434a9e855e1ef5bcaf2067',
                policyName: 'Security-High',
                policyThreatLevel: 9,
                waived: false,
                grandfathered: true
              }],
              waivedViolations: [],
              allViolations: [{
                policyId: '546fa744e6434a9e855e1ef5bcaf2067',
                policyName: 'Security-High',
                policyThreatLevel: 9,
                waived: false,
                grandfathered: true
              }]
            }]
          },
          result = applicationReportService.createReportEntries(policyThreatData, bomData, unknownJSData);

      expect(result.length).toEqual(4);

      expect(result).toContain({
        hash: 'fooHash',
        componentIdentifier: {
          format: 'a-name',
          coordinates: {
            name: 'foo',
            version: '1'
          }
        },
        policyName: 'Security-High',
        policyThreatLevel: 9,
        waived: false,
        grandfathered: false
      });

      expect(result).toContain({
        hash: 'fooHash',
        componentIdentifier: {
          format: 'a-name',
          coordinates: {
            name: 'foo',
            version: '1'
          }
        },
        policyName: 'License-High',
        policyThreatLevel: 8,
        waived: true,
        grandfathered: false
      });

      expect(result).toContain({
        hash: 'barHash',
        componentIdentifier: {
          format: 'maven',
          coordinates: {
            groupId: 'barGroup',
            artifactId: 'bar',
            version: '2'
          }
        },
        policyName: 'Security-High',
        policyThreatLevel: 9,
        waived: false,
        grandfathered: true
      });

      expect(result).toContain({
        hash: 'bazHash',
        otherProp: 'baz',
        policyName: 'None',
        policyThreatLevel: 0,
        waived: false,
        grandfathered: false
      });
    });

    it('creates entries from report V1/V2 data', function() {
      const policyThreatData = {
            version: 1,
            aaData: [{
              hash: 'fooHash',
              componentIdentifier: {
                format: 'a-name',
                coordinates: {
                  name: 'foo',
                  version: '1'
                }
              },
              policyId: '546fa744e6434a9e855e1ef5bcaf2067',
              policyName: 'Security-High',
              policyThreatLevel: 9,
              activeViolations: [{
                policyId: '546fa744e6434a9e855e1ef5bcaf2067',
                policyName: 'Security-High',
                policyThreatLevel: 9
              }],
              waivedViolations: [{
                policyId: '546fa744e6434a9e855e1ef5bcaf2068',
                policyName: 'License-High',
                policyThreatLevel: 8
              }]
            }, {
              hash: 'barHash',
              componentIdentifier: {
                format: 'maven',
                coordinates: {
                  groupId: 'barGroup',
                  artifactId: 'bar',
                  version: '2'
                }
              },
              policyId: '546fa744e6434a9e855e1ef5bcaf2067',
              policyName: 'Security-High',
              policyThreatLevel: 9,
              activeViolations: [{
                policyId: '546fa744e6434a9e855e1ef5bcaf2067',
                policyName: 'Security-High',
                policyThreatLevel: 9
              }],
              waivedViolations: []
            }]
          },
          policyThreatData2 = { ...policyThreatData, version: 2 },
          result = applicationReportService.createReportEntries(policyThreatData, bomData, unknownJSData),
          result2 = applicationReportService.createReportEntries(policyThreatData2, bomData, unknownJSData);

      expect(result.length).toEqual(4);

      expect(result).toContain({
        hash: 'fooHash',
        componentIdentifier: {
          format: 'a-name',
          coordinates: {
            name: 'foo',
            version: '1'
          }
        },
        policyName: 'Security-High',
        policyThreatLevel: 9,
        waived: false,
        grandfathered: false
      });

      expect(result).toContain({
        hash: 'fooHash',
        componentIdentifier: {
          format: 'a-name',
          coordinates: {
            name: 'foo',
            version: '1'
          }
        },
        policyName: 'License-High',
        policyThreatLevel: 8,
        waived: true,
        grandfathered: false
      });

      expect(result).toContain({
        hash: 'barHash',
        componentIdentifier: {
          format: 'maven',
          coordinates: {
            groupId: 'barGroup',
            artifactId: 'bar',
            version: '2'
          }
        },
        policyName: 'Security-High',
        policyThreatLevel: 9,
        waived: false,
        grandfathered: false
      });

      expect(result).toContain({
        hash: 'bazHash',
        otherProp: 'baz',
        policyName: 'None',
        policyThreatLevel: 0,
        waived: false,
        grandfathered: false
      });

      expect(result2).toEqual(result);
    });

    it('creates entries from report pre-V1 data', function() {
      const policyThreatData = {
            aaData: [{
              hash: 'fooHash',
              componentIdentifier: {
                format: 'a-name',
                coordinates: {
                  name: 'foo',
                  version: '1'
                }
              },
              policyId: '546fa744e6434a9e855e1ef5bcaf2067',
              policyName: 'Security-High',
              policyThreatLevel: 9
            }, {
              hash: 'fooHash',
              componentIdentifier: {
                format: 'a-name',
                coordinates: {
                  name: 'foo',
                  version: '1'
                }
              },
              policyId: '546fa744e6434a9e855e1ef5bcaf2068',
              policyName: 'License-High',
              policyThreatLevel: 8
            }, {
              hash: 'barHash',
              componentIdentifier: {
                format: 'maven',
                coordinates: {
                  groupId: 'barGroup',
                  artifactId: 'bar',
                  version: '2'
                }
              },
              policyId: '546fa744e6434a9e855e1ef5bcaf2067',
              policyName: 'Security-High',
              policyThreatLevel: 9
            }]
          },
          result = applicationReportService.createReportEntries(policyThreatData, bomData, unknownJSData);

      expect(result.length).toEqual(4);

      expect(result).toContain({
        hash: 'fooHash',
        componentIdentifier: {
          format: 'a-name',
          coordinates: {
            name: 'foo',
            version: '1'
          }
        },
        policyName: 'Security-High',
        policyThreatLevel: 9,
        waived: false,
        grandfathered: false
      });

      expect(result).toContain({
        hash: 'fooHash',
        componentIdentifier: {
          format: 'a-name',
          coordinates: {
            name: 'foo',
            version: '1'
          }
        },
        policyName: 'License-High',
        policyThreatLevel: 8,
        waived: false,
        grandfathered: false
      });

      expect(result).toContain({
        hash: 'barHash',
        componentIdentifier: {
          format: 'maven',
          coordinates: {
            groupId: 'barGroup',
            artifactId: 'bar',
            version: '2'
          }
        },
        policyName: 'Security-High',
        policyThreatLevel: 9,
        waived: false,
        grandfathered: false
      });

      expect(result).toContain({
        hash: 'bazHash',
        otherProp: 'baz',
        policyName: 'None',
        policyThreatLevel: 0,
        waived: false,
        grandfathered: false
      });
    });

    it('treats the unknownJSResult parameter as optional', function() {
      const policyThreatData = {
            aaData: [{
              hash: 'fooHash',
              componentIdentifier: {
                format: 'a-name',
                coordinates: {
                  name: 'foo',
                  version: '1'
                }
              },
              policyId: '546fa744e6434a9e855e1ef5bcaf2067',
              policyName: 'Security-High',
              policyThreatLevel: 9
            }, {
              hash: 'fooHash',
              componentIdentifier: {
                format: 'a-name',
                coordinates: {
                  name: 'foo',
                  version: '1'
                }
              },
              policyId: '546fa744e6434a9e855e1ef5bcaf2068',
              policyName: 'License-High',
              policyThreatLevel: 8
            }, {
              hash: 'barHash',
              componentIdentifier: {
                format: 'maven',
                coordinates: {
                  groupId: 'barGroup',
                  artifactId: 'bar',
                  version: '2'
                }
              },
              policyId: '546fa744e6434a9e855e1ef5bcaf2067',
              policyName: 'Security-High',
              policyThreatLevel: 9
            }]
          },
          result = applicationReportService.createReportEntries(policyThreatData, bomData);

      expect(result.length).toEqual(3);

      expect(result).toContain({
        hash: 'fooHash',
        componentIdentifier: {
          format: 'a-name',
          coordinates: {
            name: 'foo',
            version: '1'
          }
        },
        policyName: 'Security-High',
        policyThreatLevel: 9,
        waived: false,
        grandfathered: false
      });

      expect(result).toContain({
        hash: 'fooHash',
        componentIdentifier: {
          format: 'a-name',
          coordinates: {
            name: 'foo',
            version: '1'
          }
        },
        policyName: 'License-High',
        policyThreatLevel: 8,
        waived: false,
        grandfathered: false
      });

      expect(result).toContain({
        hash: 'barHash',
        componentIdentifier: {
          format: 'maven',
          coordinates: {
            groupId: 'barGroup',
            artifactId: 'bar',
            version: '2'
          }
        },
        policyName: 'Security-High',
        policyThreatLevel: 9,
        waived: false,
        grandfathered: false
      });
    });

    it('can handle undefined values for all parameters', function() {
      expect(applicationReportService.createReportEntries(undefined, undefined)).toEqual([]);
    });
  });

  describe('aggregateReportEntries', function() {
    const input = [{
      hash: '1',
      policyThreatLevel: 9,
      policyName: 'Policy 4',
      waived: true,
      grandfathered: false,
      componentIdentifier: 'bar'
    }, {
      hash: '2',
      policyThreatLevel: 4,
      policyName: 'Policy 2',
      waived: false,
      grandfathered: false,
      componentIdentifier: 'baz'
    }, {
      hash: '2',
      policyThreatLevel: 3,
      policyName: 'Policy 3',
      waived: false,
      grandfathered: false,
      componentIdentifier: 'baz'
    }, {
      hash: '1',
      policyThreatLevel: 4,
      policyName: 'Policy 1',
      waived: false,
      grandfathered: false,
      componentIdentifier: 'bar'
    }, {
      hash: '3',
      policyThreatLevel: 4,
      policyName: 'Policy 5',
      waived: true,
      grandfathered: false,
      componentIdentifier: 'qux'
    }, {
      hash: '1',
      policyThreatLevel: 8,
      policyName: 'Policy 6',
      waived: false,
      grandfathered: false,
      componentIdentifier: 'bar'
    }, {
      hash: '1',
      policyThreatLevel: 9,
      policyName: 'Policy 9',
      waived: true,
      grandfathered: false,
      componentIdentifier: 'bar'
    }, {
      hash: '3',
      policyThreatLevel: 5,
      policyName: 'Policy 7',
      waived: true,
      grandfathered: false,
      componentIdentifier: 'qux'
    }, {
      hash: '3',
      policyThreatLevel: 4,
      policyName: 'Policy 8',
      waived: true,
      grandfathered: false,
      componentIdentifier: 'qux'
    }, {
      hash: '4',
      policyThreatLevel: 0,
      policyName: 'None',
      waived: false,
      grandfathered: false,
      componentIdentifier: 'apache'
    }, {
      hash: '5',
      policyThreatLevel: 3,
      policyName: 'Policy 11',
      waived: false,
      grandfathered: true,
      componentIdentifier: 'foo'
    }, {
      hash: '5',
      policyThreatLevel: 2,
      policyName: 'Policy 10',
      waived: false,
      grandfathered: false,
      componentIdentifier: 'foo'
    }, {
      hash: '5',
      policyThreatLevel: 5,
      policyName: 'Policy 12',
      waived: false,
      grandfathered: true,
      componentIdentifier: 'foo'
    }, {
      hash: '6',
      policyThreatLevel: 5,
      policyName: 'Policy 12',
      waived: false,
      grandfathered: true,
      componentIdentifier: 'foo2'
    }];

    it('returns a list including only the highest-threat unwaived ungrandfathered violation for each component',
        function() {
          const result = applicationReportService.aggregateReportEntries(input),
              hash1Result = result.filter(r => r.hash === '1'),
              hash2Result = result.filter(r => r.hash === '2'),
              hash5Result = result.filter(r => r.hash === '5');

          expect(hash1Result.length).toBe(1);
          expect(hash2Result.length).toBe(1);

          expect(hash1Result[0].policyThreatLevel).toBe(8);
          expect(hash1Result[0].policyName).toBe('Policy 6');
          expect(hash1Result[0].waived).toBe(false);
          expect(hash1Result[0].grandfathered).toBe(false);
          expect(hash1Result[0].componentIdentifier).toBe('bar');

          expect(hash2Result[0].policyThreatLevel).toBe(4);
          expect(hash2Result[0].policyName).toBe('Policy 2');
          expect(hash2Result[0].waived).toBe(false);
          expect(hash2Result[0].grandfathered).toBe(false);
          expect(hash2Result[0].componentIdentifier).toBe('baz');

          expect(hash5Result[0].policyThreatLevel).toBe(2);
          expect(hash5Result[0].policyName).toBe('Policy 10');
          expect(hash5Result[0].waived).toBe(false);
          expect(hash5Result[0].grandfathered).toBe(false);
          expect(hash5Result[0].componentIdentifier).toBe('foo');

        }
    );

    it('includes a zero-threat record in the output if all violations for a component are waived', function() {
      const result = applicationReportService.aggregateReportEntries(input),
          hash3Result = result.filter(r => r.hash === '3');

      expect(hash3Result.length).toBe(1);

      expect(hash3Result[0].policyThreatLevel).toBe(0);
      expect(hash3Result[0].policyName).toBe('None');
      expect(hash3Result[0].waived).toBe(false);
      expect(hash3Result[0].grandfathered).toBe(false);
      expect(hash3Result[0].componentIdentifier).toBe('qux');
    });

    it('includes a zero-threat record in the output if all violations for a component are grandfathered', function() {
      const result = applicationReportService.aggregateReportEntries(input),
          hash6Result = result.filter(r => r.hash === '6');

      expect(hash6Result.length).toBe(1);

      expect(hash6Result[0].policyThreatLevel).toBe(0);
      expect(hash6Result[0].policyName).toBe('None');
      expect(hash6Result[0].waived).toBe(false);
      expect(hash6Result[0].grandfathered).toBe(false);
      expect(hash6Result[0].componentIdentifier).toBe('foo2');
    });

    it('passes through zero-threat records from the input', function() {
      const result = applicationReportService.aggregateReportEntries(input),
          hash4Result = result.filter(r => r.hash === '4');

      expect(hash4Result.length).toBe(1);

      expect(hash4Result[0].policyThreatLevel).toBe(0);
      expect(hash4Result[0].policyName).toBe('None');
      expect(hash4Result[0].waived).toBe(false);
      expect(hash4Result[0].grandfathered).toBe(false);
      expect(hash4Result[0].componentIdentifier).toBe('apache');
    });
  });
});
