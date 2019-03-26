import * as applicationReportService from '../../../main/frontend/applicationReport/applicationReportService';
import {ascend, map, prop, props, sortWith} from 'ramda';

describe('applicationReportService', function() {

  describe('createRawDataEntries', function() {
    const bomData = {
          aaData: [
            {
              componentIdentifier: {
                format: 'a-name',
                coordinates: {
                  name: 'foo',
                  version: '1'
                }
              },
              hash: 'fooHash',
              displayName: {
                parts: [
                  {field: 'a-name', value: 'foo'}, {value: ' : '}, {field: 'version', value: '1'}
                ]
              }
            }, {
              componentIdentifier: {
                format: 'maven',
                coordinates: {
                  groupId: 'barGroup',
                  artifactId: 'bar',
                  version: '2'
                }
              },
              hash: 'barHash',
              displayName: {
                parts: [
                  {field: 'Group', value: 'barGroup'},
                  {value: ' : '},
                  {field: 'Artifact', value: 'bar'},
                  {value: ' : '},
                  {field: 'Version', value: '2'}
                ]
              }
            }, {
              // same component id as the first entry, but with the keys declared in a different order
              componentIdentifier: {
                coordinates: {
                  version: '1',
                  name: 'foo'
                },
                format: 'a-name'
              },
              // different hash from the first entry
              hash: 'fooHash2',
              displayName: {
                parts: [
                  {field: 'a-name', value: 'foo'}, {value: ' : '}, {field: 'version', value: '1'}
                ]
              }
            }, {
              hash: 'unidentifiedHash1',
              pathnames: ['foo/bar/path1'],
              filenames: ['path1']
            }, {
              hash: 'unidentifiedHash2',
              pathnames: ['foo/bar/path2', 'foo/path3'],
              filenames: ['path2', 'path3']
            }
          ]
        },
        unknownJSData = {
          aaData: [
            {
              hash: 'bazHash',
              filenames: ['baz.js', 'bazzzz.js'],
              otherProp: 'baz'
            }
          ]
        },
        licensesData = {
          aaData: [{
            componentIdentifier: {
              format: 'a-name',
              coordinates: {
                name: 'foo',
                version: '1'
              }
            },
            hash: 'fooHash',
            declaredLicenses: ['Apache 2.0'],
            effectiveLicenses: ['Apache 2.0'],
            observedLicenses: ['Apache 2.1']
          }, {
            componentIdentifier: {
              format: 'maven',
              coordinates: {
                groupId: 'barGroup',
                artifactId: 'bar',
                version: '2'
              }
            },
            hash: 'barHash',
            declaredLicenses: ['Apache 200.0'],
            effectiveLicenses: ['Apache 200.0'],
            observedLicenses: ['Apache 200.0']
          }]
        },
        securityData = {
          aaData: [{
            hash: 'fooHash',
            score: 1.2,
            reference: 'fooCode',
            url: 'fooUrl',
            source: 'fooSource'
          }, {
            hash: 'fooHash',
            score: 3.4,
            reference: 'fooCode2',
            url: 'fooUrl2',
            source: 'fooSource2'
          }, {
            // same reference as the first one, but for the other hash of component foo. Should not result
            // in an additional (duplicated) entry in the createRawDataEntries output
            hash: 'fooHash2',
            score: 1.2,
            reference: 'fooCode',
            url: 'fooUrl',
            source: 'fooSource'
          }, {
            hash: 'bazHash',
            score: 5.6,
            reference: 'bazCode',
            url: 'bazUrl',
            source: 'bazSource'
          }]
        };

    it('creates raw data appropriately', () => {

      const result = applicationReportService.createRawDataEntries(
          securityData, licensesData, bomData, unknownJSData);

      expect(result.length).toEqual(6);

      const sortedResult = sortWith([ascend(prop('cvssScore')), ascend(prop('derivedComponentName'))], result);

      expect(sortedResult[0].license).toBe(licensesData.aaData[1]);
      expect(sortedResult[0].derivedComponentName).toBe('bargroup : bar : 2');
      expect(sortedResult[0].cvssScore).toBeUndefined();
      expect(sortedResult[0].securityCode).toBeUndefined();
      expect(sortedResult[0].url).toBeUndefined();
      expect(sortedResult[0].licenseSortKey).toBe('Apache 200.0');
      expect(sortedResult[0].displayName).toBe(bomData.aaData[1].displayName);

      expect(sortedResult[1].derivedComponentName).toBe('baz.js, bazzzz.js');
      expect(sortedResult[1].license).toBeUndefined();
      expect(sortedResult[1].cvssScore).toBeUndefined();
      expect(sortedResult[1].securityCode).toBeUndefined();
      expect(sortedResult[1].url).toBeUndefined();
      expect(sortedResult[1].source).toBeUndefined();
      expect(sortedResult[1].licenseSortKey).toBeUndefined();

      expect(sortedResult[2].license).toBe(licensesData.aaData[0]);
      expect(sortedResult[2]).toEqual(jasmine.objectContaining({
        derivedComponentName: 'foo : 1',
        cvssScore: 1.2,
        securityCode: 'fooCode',
        url: 'fooUrl',
        source: 'fooSource',
        licenseSortKey: 'Apache 2.0, Apache 2.1',
        displayName: bomData.aaData[0].displayName
      }));

      expect(sortedResult[3].license).toBe(licensesData.aaData[0]);
      expect(sortedResult[3]).toEqual(jasmine.objectContaining({
        derivedComponentName: 'foo : 1',
        cvssScore: 3.4,
        securityCode: 'fooCode2',
        url: 'fooUrl2',
        licenseSortKey: 'Apache 2.0, Apache 2.1',
        source: 'fooSource2',
        displayName: bomData.aaData[0].displayName
      }));

      expect(sortedResult[4].derivedComponentName).toBe('path1');
      expect(sortedResult[4].license).toBeUndefined();
      expect(sortedResult[4].cvssScore).toBeUndefined();
      expect(sortedResult[4].securityCode).toBeUndefined();
      expect(sortedResult[4].url).toBeUndefined();
      expect(sortedResult[4].source).toBeUndefined();

      expect(sortedResult[5].derivedComponentName).toBe('path2, path3');
      expect(sortedResult[5].license).toBeUndefined();
      expect(sortedResult[5].cvssScore).toBeUndefined();
      expect(sortedResult[5].securityCode).toBeUndefined();
      expect(sortedResult[5].url).toBeUndefined();
      expect(sortedResult[5].source).toBeUndefined();
    });
  });

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
            },
            displayName: {
              parts: [
                {field: 'a-name', value: 'foo'}, {value: ' : '}, {field: 'version', value: '1'}
              ]
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
            },
            displayName: {
              parts: [
                {field: 'Group', value: 'barGroup'},
                {value: ' : '},
                {field: 'Artifact', value: 'bar'},
                {value: ' : '},
                {field: 'Version', value: '2'}
              ]
            }
          }]
        },
        unknownJSData = {
          aaData: [{
            hash: 'bazHash',
            filenames: ['baz.js', 'bazzzz.js'],
            otherProp: 'baz'
          }]
        },
        partialMatchData = {
          aaData: [{
            hash: 'barHash',
            matchDetails: [{ artifactId: 'fooBar' }]
          }]
        };

    it('creates entries from report V3/V4 data', function() {
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
                grandfathered: true
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
          policyThreatData2 = {
            version: 4,
            aaData: policyThreatData.aaData.map(threat => ({
              ...threat,
              allViolations: threat.allViolations.map(violation => ({
                ...violation,
                policyThreatCategory: 'OTHER'
              }))
            }))
          },
          result = applicationReportService.createReportEntries(
              policyThreatData, bomData, unknownJSData, partialMatchData),
          result2 = applicationReportService.createReportEntries(
              policyThreatData2, bomData, unknownJSData, partialMatchData);

      expect(result.length).toEqual(4);
      expect(result2.length).toEqual(4);

      expect(result).toContain(jasmine.objectContaining({
        hash: 'fooHash',
        componentIdentifier: {
          format: 'a-name',
          coordinates: {
            name: 'foo',
            version: '1'
          }
        },
        derivedComponentName: 'foo : 1',
        derivedViolationState: 'open',
        policyName: 'Security-High',
        policyThreatLevel: 9,
        waived: false,
        grandfathered: false
      }));

      expect(result).toContain(jasmine.objectContaining({
        hash: 'fooHash',
        componentIdentifier: {
          format: 'a-name',
          coordinates: {
            name: 'foo',
            version: '1'
          }
        },
        derivedComponentName: 'foo : 1',
        derivedViolationState: 'waived+grandfathered',
        policyName: 'License-High',
        policyThreatLevel: 8,
        waived: true,
        grandfathered: true
      }));

      expect(result).toContain(jasmine.objectContaining({
        hash: 'barHash',
        componentIdentifier: {
          format: 'maven',
          coordinates: {
            groupId: 'barGroup',
            artifactId: 'bar',
            version: '2'
          }
        },
        derivedComponentName: 'bargroup : bar : 2',
        derivedViolationState: 'grandfathered',
        policyName: 'Security-High',
        policyThreatLevel: 9,
        waived: false,
        grandfathered: true,
        matchDetails: partialMatchData.aaData[0].matchDetails
      }));

      expect(result).toContain(jasmine.objectContaining({
        hash: 'bazHash',
        otherProp: 'baz',
        derivedComponentName: 'baz.js, bazzzz.js',
        derivedViolationState: 'notViolating',
        policyName: 'None',
        policyThreatLevel: 0,
        waived: false,
        grandfathered: false
      }));

      expect(result2).toContain(jasmine.objectContaining({
        hash: 'fooHash',
        componentIdentifier: {
          format: 'a-name',
          coordinates: {
            name: 'foo',
            version: '1'
          }
        },
        derivedComponentName: 'foo : 1',
        derivedViolationState: 'open',
        policyName: 'Security-High',
        policyThreatLevel: 9,
        policyThreatCategory: 'OTHER',
        waived: false,
        grandfathered: false
      }));

      expect(result2).toContain(jasmine.objectContaining({
        hash: 'fooHash',
        componentIdentifier: {
          format: 'a-name',
          coordinates: {
            name: 'foo',
            version: '1'
          }
        },
        derivedComponentName: 'foo : 1',
        derivedViolationState: 'waived+grandfathered',
        policyName: 'License-High',
        policyThreatLevel: 8,
        policyThreatCategory: 'OTHER',
        waived: true,
        grandfathered: true
      }));

      expect(result2).toContain(jasmine.objectContaining({
        hash: 'barHash',
        componentIdentifier: {
          format: 'maven',
          coordinates: {
            groupId: 'barGroup',
            artifactId: 'bar',
            version: '2'
          }
        },
        derivedComponentName: 'bargroup : bar : 2',
        derivedViolationState: 'grandfathered',
        policyName: 'Security-High',
        policyThreatLevel: 9,
        waived: false,
        policyThreatCategory: 'OTHER',
        grandfathered: true,
        matchDetails: partialMatchData.aaData[0].matchDetails
      }));

      expect(result2).toContain(jasmine.objectContaining({
        hash: 'bazHash',
        otherProp: 'baz',
        derivedComponentName: 'baz.js, bazzzz.js',
        derivedViolationState: 'notViolating',
        policyName: 'None',
        policyThreatLevel: 0,
        waived: false,
        grandfathered: false
      }));

      expectNoExtraMatchData(result);
      expectNoExtraMatchData(result2);
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
          result = applicationReportService.createReportEntries(
              policyThreatData, bomData, unknownJSData, partialMatchData),
          result2 = applicationReportService.createReportEntries(
              policyThreatData2, bomData, unknownJSData, partialMatchData);

      expect(result.length).toEqual(4);

      expect(result).toContain(jasmine.objectContaining({
        hash: 'fooHash',
        componentIdentifier: {
          format: 'a-name',
          coordinates: {
            name: 'foo',
            version: '1'
          }
        },
        derivedComponentName: 'foo : 1',
        derivedViolationState: 'open',
        policyName: 'Security-High',
        policyThreatLevel: 9,
        waived: false,
        grandfathered: false
      }));

      expect(result).toContain(jasmine.objectContaining({
        hash: 'fooHash',
        componentIdentifier: {
          format: 'a-name',
          coordinates: {
            name: 'foo',
            version: '1'
          }
        },
        derivedComponentName: 'foo : 1',
        derivedViolationState: 'waived',
        policyName: 'License-High',
        policyThreatLevel: 8,
        waived: true,
        grandfathered: false
      }));

      expect(result).toContain(jasmine.objectContaining({
        hash: 'barHash',
        componentIdentifier: {
          format: 'maven',
          coordinates: {
            groupId: 'barGroup',
            artifactId: 'bar',
            version: '2'
          }
        },
        derivedComponentName: 'bargroup : bar : 2',
        derivedViolationState: 'open',
        policyName: 'Security-High',
        policyThreatLevel: 9,
        waived: false,
        grandfathered: false,
        matchDetails: partialMatchData.aaData[0].matchDetails
      }));

      expect(result).toContain(jasmine.objectContaining({
        hash: 'bazHash',
        otherProp: 'baz',
        derivedComponentName: 'baz.js, bazzzz.js',
        derivedViolationState: 'notViolating',
        policyName: 'None',
        policyThreatLevel: 0,
        waived: false,
        grandfathered: false
      }));

      expect(result2).toEqual(result);

      expectNoExtraMatchData(result);
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
          result = applicationReportService.createReportEntries(
              policyThreatData, bomData, unknownJSData, partialMatchData);

      expect(result.length).toEqual(4);

      expect(result).toContain(jasmine.objectContaining({
        hash: 'fooHash',
        componentIdentifier: {
          format: 'a-name',
          coordinates: {
            name: 'foo',
            version: '1'
          }
        },
        derivedComponentName: 'foo : 1',
        derivedViolationState: 'open',
        policyName: 'Security-High',
        policyThreatLevel: 9,
        waived: false,
        grandfathered: false
      }));

      expect(result).toContain(jasmine.objectContaining({
        hash: 'fooHash',
        componentIdentifier: {
          format: 'a-name',
          coordinates: {
            name: 'foo',
            version: '1'
          }
        },
        derivedComponentName: 'foo : 1',
        derivedViolationState: 'open',
        policyName: 'License-High',
        policyThreatLevel: 8,
        waived: false,
        grandfathered: false
      }));

      expect(result).toContain(jasmine.objectContaining({
        hash: 'barHash',
        componentIdentifier: {
          format: 'maven',
          coordinates: {
            groupId: 'barGroup',
            artifactId: 'bar',
            version: '2'
          }
        },
        derivedComponentName: 'bargroup : bar : 2',
        derivedViolationState: 'open',
        policyName: 'Security-High',
        policyThreatLevel: 9,
        waived: false,
        grandfathered: false,
        matchDetails: partialMatchData.aaData[0].matchDetails
      }));

      expect(result).toContain(jasmine.objectContaining({
        hash: 'bazHash',
        otherProp: 'baz',
        derivedComponentName: 'baz.js, bazzzz.js',
        derivedViolationState: 'notViolating',
        policyName: 'None',
        policyThreatLevel: 0,
        waived: false,
        grandfathered: false
      }));

      expectNoExtraMatchData(result);
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

      expect(result).toContain(jasmine.objectContaining({
        hash: 'fooHash',
        componentIdentifier: {
          format: 'a-name',
          coordinates: {
            name: 'foo',
            version: '1'
          }
        },
        derivedComponentName: 'foo : 1',
        derivedViolationState: 'open',
        policyName: 'Security-High',
        policyThreatLevel: 9,
        waived: false,
        grandfathered: false
      }));

      expect(result).toContain(jasmine.objectContaining({
        hash: 'fooHash',
        componentIdentifier: {
          format: 'a-name',
          coordinates: {
            name: 'foo',
            version: '1'
          }
        },
        derivedComponentName: 'foo : 1',
        derivedViolationState: 'open',
        policyName: 'License-High',
        policyThreatLevel: 8,
        waived: false,
        grandfathered: false
      }));

      expect(result).toContain(jasmine.objectContaining({
        hash: 'barHash',
        componentIdentifier: {
          format: 'maven',
          coordinates: {
            groupId: 'barGroup',
            artifactId: 'bar',
            version: '2'
          }
        },
        derivedComponentName: 'bargroup : bar : 2',
        derivedViolationState: 'open',
        policyName: 'Security-High',
        policyThreatLevel: 9,
        waived: false,
        grandfathered: false
      }));
    });

    it('can handle undefined values for all parameters', function() {
      expect(applicationReportService.createReportEntries(undefined, undefined)).toEqual([]);
    });
  });

  describe('aggregation, filtering and sorting', function() {
    const input = [
      {
        hash: '1',
        policyThreatLevel: 9,
        policyName: 'Policy 4',
        waived: true,
        grandfathered: false,
        componentIdentifier: 'bar',
        displayName: {
          parts: [
            {field: 'Group', value: 'junit'}, {value: ' : '}, {field: 'Artifact', value: 'junit'}, {value: ' : '},
            {field: 'Version', value: '4.12'}
          ]
        },
        derivedComponentName: 'junit.junit.4.12'
      }, {
        hash: '2',
        policyThreatLevel: 4,
        policyName: 'Policy 2',
        waived: false,
        grandfathered: false,
        componentIdentifier: 'baz',
        derivedComponentName: 'junit.junit.4.8'
      }, {
        hash: '2',
        policyThreatLevel: 3,
        policyName: 'Policy 3',
        waived: false,
        grandfathered: false,
        componentIdentifier: 'baz',
        derivedComponentName: 'ant.ant.1.62'
      }, {
        hash: '1',
        policyThreatLevel: 4,
        policyName: 'Policy 1',
        waived: false,
        grandfathered: false,
        componentIdentifier: 'bar',
        derivedComponentName: 'junit.junit.4.12'
      }, {
        hash: '3',
        policyThreatLevel: 4,
        policyName: 'Policy 5',
        waived: true,
        grandfathered: false,
        componentIdentifier: 'qux',
        displayName: {
          parts: [
            {field: 'Group', value: 'junit'}, {value: ' : '}, {field: 'Artifact', value: 'junit'}, {value: ' : '},
            {field: 'Version', value: '4.12'}
          ]
        },
        derivedComponentName: 'junit.junit.4.12'
      }, {
        hash: '1',
        policyThreatLevel: 8,
        policyName: 'Policy 6',
        waived: false,
        grandfathered: false,
        componentIdentifier: 'bar',
        derivedComponentName: 'junit.junit.4.12'
      }, {
        hash: '1',
        policyThreatLevel: 9,
        policyName: 'Policy 9',
        waived: true,
        grandfathered: false,
        componentIdentifier: 'bar',
        displayName: {
          parts: [
            {field: 'Group', value: 'junit'}, {value: ' : '}, {field: 'Artifact', value: 'junit'}, {value: ' : '},
            {field: 'Version', value: '4.12'}
          ]
        },
        derivedComponentName: 'junit.junit.4.12'
      }, {
        hash: '3',
        policyThreatLevel: 5,
        policyName: 'Policy 7',
        waived: true,
        grandfathered: false,
        componentIdentifier: 'qux',
        displayName: {
          parts: [
            {field: 'Group', value: 'xpp'}, {value: ' : '}, {field: 'Artifact', value: 'xpp3_min'}, {value: ' : '},
            {field: 'Version', value: '1.1.4c'}
          ]
        },
        derivedComponentName: 'xpp.xpp3_min.1.1.4c'
      }, {
        hash: '3',
        policyThreatLevel: 4,
        policyName: 'Policy 8',
        waived: true,
        grandfathered: false,
        componentIdentifier: 'qux',
        displayName: {
          parts: [
            {field: 'Group', value: 'org.springframework'}, {value: ' : '}, {field: 'Artifact', value: 'spring-webmvc'},
            {value: ' : '}, {field: 'Version', value: '4.3.16.RELEASE'}
          ]
        },
        derivedComponentName: 'org.springframework.spring-webmvc.4.3.16.RELEASE'
      }, {
        hash: '4',
        policyThreatLevel: 0,
        policyName: 'None',
        waived: false,
        grandfathered: false,
        componentIdentifier: 'apache',
        derivedComponentName: 'org.apache.tomcat.embed.tomcat-embed-core.8.5.29'
      }, {
        hash: '5',
        policyThreatLevel: 3,
        policyName: 'Policy 11',
        waived: false,
        grandfathered: true,
        componentIdentifier: 'foo',
        displayName: {
          parts: [
            {field: 'Group', value: 'com.fasterxml'}, {value: ' : '},
            {field: 'Artifact', value: 'jackson.core.jackson-annotations'}, {value: ' : '},
            {field: 'Version', value: '2.8.11.1'}
          ]
        },
        derivedComponentName: 'com.fasterxml.jackson.core.jackson-annotations.2.8.11.1'
      }, {
        hash: '5',
        policyThreatLevel: 2,
        policyName: 'Policy 10',
        waived: false,
        grandfathered: false,
        componentIdentifier: 'foo',
        derivedComponentName: 'com.fasterxml.jackson.core.jackson-databind.2.8.11.1'
      }, {
        hash: '5',
        policyThreatLevel: 5,
        policyName: 'Policy 12',
        waived: false,
        grandfathered: true,
        componentIdentifier: 'foo',
        displayName: {
          parts: [
            {field: 'Group', value: 'ognl'}, {value: ' : '}, {field: 'Artifact', value: 'ognl'}, {value: ' : '},
            {field: 'Version', value: '3.0.8'}
          ]
        },
        derivedComponentName: 'ognl.ognl.3.0.8'
      }, {
        hash: '6',
        policyThreatLevel: 5,
        policyName: 'Policy 12',
        waived: false,
        grandfathered: true,
        componentIdentifier: 'foo2',
        displayName: {
          parts: [
            {field: 'Group', value: 'org.postgresql'}, {value: ' : '}, {field: 'Artifact', value: 'postgresql'},
            {value: ' : '}, {field: 'Version', value: '42.2.2'}
          ]
        },
        derivedComponentName: 'org.postgresql.postgresql.42.2.2'
      }, {
        hash: '7',
        policyThreatLevel: 5,
        policyName: 'Policy 13',
        waived: false,
        grandfathered: true,
        componentIdentifier: 'foo3',
        displayName: {
          parts: [
            {field: 'Group', value: 'org.postgresql'}, {value: ' : '}, {field: 'Artifact', value: 'postgresql'},
            {value: ' : '}, {field: 'Version', value: '42.2.3'}
          ]
        },
        derivedComponentName: 'org.postgresql.postgresql.42.2.3'
      }, {
        hash: '7',
        policyThreatLevel: 6,
        policyName: 'Policy 14',
        waived: true,
        grandfathered: false,
        componentIdentifier: 'foo3',
        displayName: {
          parts: [
            {field: 'Group', value: 'org.postgresql'}, {value: ' : '}, {field: 'Artifact', value: 'postgresql'},
            {value: ' : '}, {field: 'Version', value: '42.2.3'}
          ]
        },
        derivedComponentName: 'org.postgresql.postgresql.42.2.3'
      }
    ];

    describe('aggregateReportEntries', function() {
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
        expect(hash3Result[0].waived).toBe(true);
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
        expect(hash6Result[0].grandfathered).toBe(true);
        expect(hash6Result[0].componentIdentifier).toBe('foo2');
      });

      it('sets grandfathered and waived in the zero-threat record if there are some violations with each', function() {
        const result = applicationReportService.aggregateReportEntries(input),
            hash7Result = result.filter(r => r.hash === '7');

        expect(hash7Result.length).toBe(1);

        expect(hash7Result[0].policyThreatLevel).toBe(0);
        expect(hash7Result[0].policyName).toBe('None');
        expect(hash7Result[0].waived).toBe(true);
        expect(hash7Result[0].grandfathered).toBe(true);
        expect(hash7Result[0].componentIdentifier).toBe('foo3');
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

    describe('sortReportEntries', function() {
      it('sorts by supplied properties (in descending order if prefixed with a \'-\')', function() {
        const result = applicationReportService.sortReportEntries(
            ['-policyThreatLevel', 'policyName', 'derivedComponentName'], input);
        expect(map(props(['policyThreatLevel', 'policyName', 'derivedComponentName']))(result)).toEqual([
          [9, 'Policy 4', 'junit.junit.4.12'],
          [9, 'Policy 9', 'junit.junit.4.12'],
          [8, 'Policy 6', 'junit.junit.4.12'],
          [6, 'Policy 14', 'org.postgresql.postgresql.42.2.3'],
          [5, 'Policy 12', 'ognl.ognl.3.0.8'],
          [5, 'Policy 12', 'org.postgresql.postgresql.42.2.2'],
          [5, 'Policy 13', 'org.postgresql.postgresql.42.2.3'],
          [5, 'Policy 7', 'xpp.xpp3_min.1.1.4c'],
          [4, 'Policy 1', 'junit.junit.4.12'],
          [4, 'Policy 2', 'junit.junit.4.8'],
          [4, 'Policy 5', 'junit.junit.4.12'],
          [4, 'Policy 8', 'org.springframework.spring-webmvc.4.3.16.RELEASE'],
          [3, 'Policy 11', 'com.fasterxml.jackson.core.jackson-annotations.2.8.11.1'],
          [3, 'Policy 3', 'ant.ant.1.62'],
          [2, 'Policy 10', 'com.fasterxml.jackson.core.jackson-databind.2.8.11.1'],
          [0, 'None', 'org.apache.tomcat.embed.tomcat-embed-core.8.5.29']
        ]);
      });

      it('sorts null values to the end when sorting descending', () => {
        const nullSortInput = [
          { foo: '3' },
          { foo: '2' },
          { foo: null },
          { foo: '4' },
          { foo: '1' }
        ];
        const result = applicationReportService.sortReportEntries(
            ['-foo'], nullSortInput);
        expect(result).toEqual([
          { foo: '4' },
          { foo: '3' },
          { foo: '2' },
          { foo: '1' },
          { foo: null }
        ]);
      });

      it('sorts undefined values to the end when sorting descending', () => {
        const nullSortInput = [
          { foo: '3' },
          { foo: '2' },
          { foo: undefined },
          { foo: '4' },
          { foo: '1' }
        ];
        const result = applicationReportService.sortReportEntries(
            ['-foo'], nullSortInput);
        expect(result).toEqual([
          { foo: '4' },
          { foo: '3' },
          { foo: '2' },
          { foo: '1' },
          { foo: undefined }
        ]);
      });

      it('sorts null values to the beginning when sorting ascending', () => {
        const nullSortInput = [
          { foo: '3' },
          { foo: '2' },
          { foo: null },
          { foo: '4' },
          { foo: '1' }
        ];
        const result = applicationReportService.sortReportEntries(
            ['foo'], nullSortInput);
        expect(result).toEqual([
          { foo: null },
          { foo: '1' },
          { foo: '2' },
          { foo: '3' },
          { foo: '4' }
        ]);
      });

      it('sorts undefined values to the beginning when sorting ascending', () => {
        const nullSortInput = [
          { foo: '3' },
          { foo: '2' },
          { foo: undefined },
          { foo: '4' },
          { foo: '1' }
        ];
        const result = applicationReportService.sortReportEntries(
            ['foo'], nullSortInput);
        expect(result).toEqual([
          { foo: undefined },
          { foo: '1' },
          { foo: '2' },
          { foo: '3' },
          { foo: '4' }
        ]);
      });

      it('returns the list unchanged if no properties to sort by are supplied', function() {
        const result = applicationReportService.sortReportEntries([], input);
        expect(result).toBe(input);
      });
    });

    describe('filterReportEntries', function() {
      it('filters based on exact values and substring matching and can be partially applied', function() {
        const exactValueFilters = {
              policyThreatLevel: new Set([2, 4, 5, 6]),
              waived: new Set([false])
            },
            substringFilters = {
              derivedComponentName: 'j'
            },
            numericFilters = {
              policyThreatLevel: [3, 4]
            },
            result = applicationReportService.filterReportEntries(exactValueFilters, substringFilters, numericFilters)(input);

        expect(result).toEqual([{
          hash: '2',
          policyThreatLevel: 4,
          policyName: 'Policy 2',
          waived: false,
          grandfathered: false,
          componentIdentifier: 'baz',
          derivedComponentName: 'junit.junit.4.8'
        }, {
          hash: '1',
          policyThreatLevel: 4,
          policyName: 'Policy 1',
          waived: false,
          grandfathered: false,
          componentIdentifier: 'bar',
          derivedComponentName: 'junit.junit.4.12'
        }]);
      });

      it('handles empty filterConfig objects', function() {
        const result = applicationReportService.filterReportEntries({}, {}, {})(input);

        expect(result).toEqual(input);
      });

      it('handles undefined filterConfig objects', function() {
        const result = applicationReportService.filterReportEntries(undefined, undefined, undefined)(input);

        expect(result).toEqual(input);
      });

      it('treats empty substring filters as no filter', function() {
        const substringFilters = {
              derivedComponentName: ''
            },
            result = applicationReportService.filterReportEntries(undefined, substringFilters, undefined)(input);

        expect(result).toEqual(input);
      });

      it('treats blank entry parameters as being filtered out with any value of substring filters', () => {
        const input = [{
          derivedComponentName: 'junit.junit.4.8',
          licenseSortKey: 'Not Provided',
          securityCode: 'sonatype-123'
        }, {
          derivedComponentName: 'junit.junit.4.12',
          licenseSortKey: 'Not Provided'
        }];

        const substringFilters = {
          securityCode: 's'
        };
        const result = applicationReportService.filterReportEntries(undefined, substringFilters, undefined)(input);
        expect(result).toEqual([input[0]]);
      });

      it('treats both empty numeric min and max filters as including blanks', () => {
        const input = [{
          derivedComponentName: 'junit.junit.4.8',
          licenseSortKey: 'Not Provided',
          securityCode: 'sonatype-123',
          cvssScore: 7
        }, {
          derivedComponentName: 'junit.junit.4.12',
          licenseSortKey: 'Not Provided',
          securityCode: 'sonatype-123'
        }];

        const numericFilters = {
          cvssScore: [undefined, undefined]
        };
        const result = applicationReportService.filterReportEntries(undefined, undefined, numericFilters)(input);
        expect(result).toEqual(input);
      });

      it('eliminates blank rows when only the maximum numeric filter is set', () => {
        const input = [{
          derivedComponentName: 'junit.junit.4.8',
          licenseSortKey: 'Not Provided',
          securityCode: 'sonatype-123',
          cvssScore: 7
        }, {
          derivedComponentName: 'junit.junit.4.12',
          licenseSortKey: 'Not Provided',
          securityCode: 'sonatype-123'
        }];

        const numericFilters = {
          cvssScore: [undefined, 9]
        };
        const result = applicationReportService.filterReportEntries(undefined, undefined, numericFilters)(input);
        expect(result).toEqual([input[0]]);
      });

      it('eliminates blank rows when only the minimum numeric filter is set', () => {
        const input = [{
          derivedComponentName: 'junit.junit.4.8',
          licenseSortKey: 'Not Provided',
          securityCode: 'sonatype-123',
          cvssScore: 7
        }, {
          derivedComponentName: 'junit.junit.4.12',
          licenseSortKey: 'Not Provided',
          securityCode: 'sonatype-123'
        }];

        const numericFilters = {
          cvssScore: [2, undefined]
        };
        const result = applicationReportService.filterReportEntries(undefined, undefined, numericFilters)(input);
        expect(result).toEqual([input[0]]);
      });

      it('treats empty exact value filters as no filter', function() {
        const exactValueFilters = {
              waived: new Set([])
            },
            result = applicationReportService.filterReportEntries(exactValueFilters, undefined, undefined)(input);

        expect(result).toEqual(input);
      });
    });
  });

  function expectNoExtraMatchData(result) {
    const hashesWithMatchDetails = new Set(
        result
            .filter(({ matchDetails }) => matchDetails !== undefined)
            .map(({ hash }) => hash)
    );

    expect(hashesWithMatchDetails.size).toBe(1);
    expect(hashesWithMatchDetails.has('barHash')).toBe(true);
  }
});
