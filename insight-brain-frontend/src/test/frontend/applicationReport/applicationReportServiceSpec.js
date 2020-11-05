/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as applicationReportService from '../../../main/frontend/applicationReport/applicationReportService';
import {ascend, isNil, map, prop, propEq, reject, sortWith} from 'ramda';

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
          reportEntries = applicationReportService.createReportEntries(
              policyThreatData, bomData, unknownJSData, partialMatchData),
          result = reportEntries.policies,
          reportEntries2 = applicationReportService.createReportEntries(
              policyThreatData2, bomData, unknownJSData, partialMatchData),
          result2 = reportEntries2.policies;

      expect(result.length).toEqual(4);
      expect(reportEntries.isInnerSourceEnabled).toEqual(false);
      expect(result2.length).toEqual(4);
      expect(reportEntries.isInnerSourceEnabled).toEqual(false);

      expect(result).toContain(jasmine.objectContaining({
        hash: 'fooHash',
        componentIdentifier: {
          format: 'a-name',
          coordinates: {
            name: 'foo',
            version: '1'
          }
        },
        serializedComponentIdentifier: 'a-name:name\u001ffoo\u001eversion\u001f1',
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
        serializedComponentIdentifier: 'a-name:name\u001ffoo\u001eversion\u001f1',
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
        serializedComponentIdentifier: 'maven:artifactId\u001fbar\u001egroupId\u001fbarGroup\u001eversion\u001f2',
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

      const bazHashEntry = result.find(propEq('hash', 'bazHash'));
      expect(bazHashEntry.serializedComponentIdentifier).toBeUndefined();

      expect(result2).toContain(jasmine.objectContaining({
        hash: 'fooHash',
        componentIdentifier: {
          format: 'a-name',
          coordinates: {
            name: 'foo',
            version: '1'
          }
        },
        serializedComponentIdentifier: 'a-name:name\u001ffoo\u001eversion\u001f1',
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
        serializedComponentIdentifier: 'a-name:name\u001ffoo\u001eversion\u001f1',
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
        serializedComponentIdentifier: 'maven:artifactId\u001fbar\u001egroupId\u001fbarGroup\u001eversion\u001f2',
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

      const bazHashEntry2 = result2.find(propEq('hash', 'bazHash'));
      expect(bazHashEntry2.serializedComponentIdentifier).toBeUndefined();

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
              policyThreatData, bomData, unknownJSData, partialMatchData).policies,
          result2 = applicationReportService.createReportEntries(
              policyThreatData2, bomData, unknownJSData, partialMatchData).policies;

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
        serializedComponentIdentifier: 'a-name:name\u001ffoo\u001eversion\u001f1',
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
        serializedComponentIdentifier: 'a-name:name\u001ffoo\u001eversion\u001f1',
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
        serializedComponentIdentifier: 'maven:artifactId\u001fbar\u001egroupId\u001fbarGroup\u001eversion\u001f2',
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

      const bazHashEntry = result.find(propEq('hash', 'bazHash'));
      expect(bazHashEntry.serializedComponentIdentifier).toBeUndefined();

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
              policyThreatData, bomData, unknownJSData, partialMatchData).policies;

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
        serializedComponentIdentifier: 'a-name:name\u001ffoo\u001eversion\u001f1',
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
        serializedComponentIdentifier: 'a-name:name\u001ffoo\u001eversion\u001f1',
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
        serializedComponentIdentifier: 'maven:artifactId\u001fbar\u001egroupId\u001fbarGroup\u001eversion\u001f2',
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

      const bazHashEntry = result.find(propEq('hash', 'bazHash'));
      expect(bazHashEntry.serializedComponentIdentifier).toBeUndefined();

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
          result = applicationReportService.createReportEntries(policyThreatData, bomData).policies;

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
      expect(applicationReportService.createReportEntries(undefined, undefined)).toEqual(
          {policies: [], isInnerSourceEnabled: false});
    });

    it('generates dependencyInfo for violating and non-violating entries', function() {
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
            }]
          },
          dependencies = {
            dependencyGraph: [{
              children: [{
                componentIdentifier: {
                  format: 'a-name',
                  coordinates: {
                    name: 'foo',
                    version: '1'
                  }
                }
              }]
            }, {
              componentIdentifier: {
                format: 'a-name',
                coordinates: {
                  name: 'foo',
                  version: '1'
                }
              },
              children: [{
                componentIdentifier: {
                  format: 'maven',
                  coordinates: {
                    groupId: 'barGroup',
                    artifactId: 'bar',
                    version: '2'
                  }
                }
              }]
            }, {
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
          result = applicationReportService.createReportEntries(
              policyThreatData, bomData, unknownJSData, partialMatchData, dependencies).policies;

      expect(result.length).toEqual(4);

      expect(result).toContain(jasmine.objectContaining({
        hash: 'fooHash',
        derivedViolationState: 'open',
        policyName: 'Security-High',
        policyThreatLevel: 9,
        dependencyInfo: { isDirectDependency: true },
        derivedDependencyType: 'direct'
      }));

      expect(result).toContain(jasmine.objectContaining({
        hash: 'fooHash',
        derivedViolationState: 'waived+grandfathered',
        policyName: 'License-High',
        policyThreatLevel: 8,
        dependencyInfo: { isDirectDependency: true },
        derivedDependencyType: 'direct'
      }));

      expect(result).toContain(jasmine.objectContaining({
        hash: 'barHash',
        derivedViolationState: 'notViolating',
        policyName: 'None',
        policyThreatLevel: 0,
        dependencyInfo: {
          isDirectDependency: false,
          rootAncestors: [{
            format: 'a-name',
            coordinates: {
              name: 'foo',
              version: '1'
            }
          }]
        },
        derivedDependencyType: 'transitive'
      }));

      const bazHashEntry = result.find(propEq('hash', 'bazHash'));
      expect(bazHashEntry.dependencyInfo).toBeUndefined();
      expect(bazHashEntry.derivedDependencyType).toBe('unknown');
    });

    it('report entries with inner source data', function() {
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
              innerSource: true,
              ownerApplicationName: 'app',
              ownerApplicationId: '123'
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
              ownerApplicationName: 'app',
              ownerApplicationId: '123'
            }]
          }, policyThreatData = {
            aaData: []
          },
          dependencies = {
            dependencyGraph: [{
              children: [{
                componentIdentifier: {
                  format: 'a-name',
                  coordinates: {
                    name: 'foo',
                    version: '1'
                  }
                }
              }]
            }, {
              componentIdentifier: {
                format: 'a-name',
                coordinates: {
                  name: 'foo',
                  version: '1'
                }
              },
              children: [{
                componentIdentifier: {
                  format: 'maven',
                  coordinates: {
                    groupId: 'barGroup',
                    artifactId: 'bar',
                    version: '2'
                  }
                }
              }]
            }, {
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
          result = applicationReportService.createReportEntries(policyThreatData, bomData,
              unknownJSData, partialMatchData, dependencies).policies;

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
        derivedDependencyType: 'direct',
        innerSource: true,
        innerSourceTDIndicator: false,
        ownerApplicationName: 'app',
        ownerApplicationId: '123'
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
        innerSourceTDIndicator: true,
        ownerApplicationName: 'app',
        ownerApplicationId: '123',
        derivedDependencyType: 'transitive'
      }));
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
            result = applicationReportService
                .filterReportEntries(exactValueFilters, substringFilters, numericFilters)(input);

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

      it('treats both undefined numeric min and max filters as including blanks', () => {
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
          cvssScore: ['', '']
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

  describe('getVulnerabilities', function() {
    function mkPolicyEntry(cveNums, additionalProps = {}, componentName = 'bar', additionalConditions = [],
                           additionalConstraints = []) {
      const cveNumList = [].concat(cveNums),
          cveConditions = map(num => ({
            conditionTriggerReference: {
              type: 'SECURITY_VULNERABILITY_REFID',
              value: `CVE-${num}`
            }
          }), reject(isNil, cveNumList)),
          conditions = cveConditions.concat(additionalConditions),
          constraints = (conditions.length ? [{ conditions }] : []).concat(additionalConstraints);

      return {
        componentIdentifier: {
          format: 'compFormat',
          coordinates: {
            foo: componentName
          }
        },
        constraints,
        ...additionalProps
      };
    }

    function mkRawDataEntry(cveNum, cvssScore = 5, componentName = 'bar', additionalProps = {}) {
      // props that would be on any raw data entry, not just a security entry
      const baseEntry = {
        componentIdentifier: {
          format: 'compFormat',
          coordinates: {
            foo: componentName
          }
        },
        ...additionalProps
      };

      return cveNum ? {
        ...baseEntry,
        securityCode: `CVE-${cveNum}`,
        cvssScore
      } : baseEntry;
    }

    it('returns an empty list when rawDataEntries is an empty list', function() {
      expect(applicationReportService.getVulnerabilities([], [])).toEqual([]);
    });

    it('outputs an entry for each security vulnerability rawDataEntry that matches a policy violation, ' +
        'containing the fields in that rawDataEntry', function() {
      const policyEntries = [
            mkPolicyEntry(1235),
            mkPolicyEntry(1234),
            mkPolicyEntry(null, null, 'bar', [{
              conditionTriggerReference: {
                type: 'OTHER',
                value: 'asdf'
              }
            }]),
            mkPolicyEntry(1236, null, 'baz', [{
              // a non-security condition in a policy that also has a security condition.
              // Should be ignored while the security condition is still picked up
              conditionTriggerReference: {
                type: 'OTHER',
                value: 'asdf'
              }
            }])
          ],
          rawDataEntries = [
            mkRawDataEntry(1234, 5, undefined, { foo: 'bar' }),
            mkRawDataEntry(1235, 4, undefined, { foo: 'bar', baz: 'qwerty' }),
            mkRawDataEntry(1236, 3, 'baz'),

            // non-security entry
            mkRawDataEntry(null, null, 'baz'),

            // no violation for this vulnerability
            mkRawDataEntry(1237, 2)
          ];

      expect(applicationReportService.getVulnerabilities(policyEntries, rawDataEntries)).toEqual([
        jasmine.objectContaining({
          foo: 'bar',
          securityCode: 'CVE-1234',
          cvssScore: 5
        }),
        jasmine.objectContaining({
          foo: 'bar',
          baz: 'qwerty',
          securityCode: 'CVE-1235',
          cvssScore: 4
        }),
        jasmine.objectContaining({
          securityCode: 'CVE-1236',
          cvssScore: 3
        })
      ]);
    });

    it('calculates a distinct key for each row that includes the component identifier and security code', function() {
      const policyEntries = [
            mkPolicyEntry(1235, {}, 'baz'),
            mkPolicyEntry(1234),
            mkPolicyEntry(null, {}, 'bar', [{
              conditionTriggerReference: {
                type: 'OTHER',
                value: 'asdf'
              }
            }])
          ],
          rawDataEntries = [mkRawDataEntry(1234), mkRawDataEntry(1235, null, 'baz'), mkRawDataEntry(1236)];

      expect(applicationReportService.getVulnerabilities(policyEntries, rawDataEntries)).toEqual([
        jasmine.objectContaining({
          key: jasmine.stringMatching(/bar.*CVE-1234/)
        }),
        jasmine.objectContaining({
          key: jasmine.stringMatching(/baz.*CVE-1235/)
        })
      ]);
    });

    it('handles policy entries that have no constraints', function() {
      const policyEntries = [{
            componentIdentifier: {
              format: 'compFormat',
              coordinates: {
                foo: 'bar'
              }
            }
          }],
          rawDataEntries = [mkRawDataEntry(1234, 5)];

      expect(applicationReportService.getVulnerabilities(policyEntries, rawDataEntries)).toEqual([]);
    });

    describe('vulnerability aggregation', function() {
      const policyEntries = [
            // CVE-1234 has two open violations (one of which is shared with CVE-1235)
            mkPolicyEntry(1234, {
              waived: false,
              grandfathered: false,
              policyThreatLevel: 1,
              derivedViolationState: 'open'
            }),
            mkPolicyEntry([1234, 1235], {
              waived: false,
              grandfathered: false,
              policyThreatLevel: 2,
              derivedViolationState: 'open'
            }),

            // CVE-1235 has one open violation (shared with CVE-1234) and one waived violation
            mkPolicyEntry(1235, {
              waived: true,
              grandfathered: false,
              policyThreatLevel: 3,
              derivedViolationState: 'waived'
            }),

            // CVE-1236 has one open violation and one grandfathered violation
            mkPolicyEntry(1236, {
              waived: false,
              grandfathered: false,
              policyThreatLevel: 1,
              derivedViolationState: 'open'
            }),
            mkPolicyEntry(1236, {
              waived: false,
              grandfathered: true,
              policyThreatLevel: 2,
              derivedViolationState: 'waived'
            }),

            // CVE-1236 on baz only has one grandfathered violation
            mkPolicyEntry(1236, {
              waived: false,
              grandfathered: true,
              policyThreatLevel: 1,
              derivedViolationState: 'grandfathered'
            }, 'baz'),

            // CVE-1238 has two waived violations
            mkPolicyEntry(1238, {
              waived: true,
              grandfathered: false,
              policyThreatLevel: 1,
              derivedViolationState: 'waived'
            }),
            mkPolicyEntry(1238, {
              waived: true,
              grandfathered: false,
              policyThreatLevel: 2,
              derivedViolationState: 'waived'
            }),

            // CVE-1239 has one violation that is waived, one violation that is both, and one violation that
            // is open
            mkPolicyEntry(1239, {
              waived: true,
              grandfathered: false,
              policyThreatLevel: 1,
              derivedViolationState: 'waived'
            }),
            mkPolicyEntry(1239, {
              waived: true,
              grandfathered: true,
              policyThreatLevel: 2,
              derivedViolationState: 'waived+grandfathered'
            }),
            mkPolicyEntry(1239, {
              waived: false,
              grandfathered: false,
              policyThreatLevel: 3,
              derivedViolationState: 'open'
            }),

            // CVE-1240 has one violation that is both
            mkPolicyEntry(1240, {
              waived: true,
              grandfathered: true,
              policyThreatLevel: 1,
              derivedViolationState: 'waived+grandfathered'
            }),

            // CVE-1241 has one violation that is waived and one that is grandfathered
            mkPolicyEntry(1241, {
              waived: true,
              grandfathered: false,
              policyThreatLevel: 1,
              derivedViolationState: 'waived'
            }),
            mkPolicyEntry(1241, {
              waived: false,
              grandfathered: true,
              policyThreatLevel: 2,
              derivedViolationState: 'grandfathered'
            })

            // CVE-1242 does not have any violations
          ],
          rawDataEntries = [
            mkRawDataEntry(1234),
            mkRawDataEntry(1235),
            mkRawDataEntry(1236),
            mkRawDataEntry(1236, null, 'baz'),
            mkRawDataEntry(1238),
            mkRawDataEntry(1239),
            mkRawDataEntry(1240),
            mkRawDataEntry(1241),
            mkRawDataEntry(1242)
          ];

      it('includes the waived flag and grandfathered flags only if every matching violation is waived or grandfathered',
          function() {
            expect(applicationReportService.getVulnerabilities(policyEntries, rawDataEntries)).toEqual([
              jasmine.objectContaining({
                securityCode: 'CVE-1234',
                waived: false,
                grandfathered: false
              }),
              jasmine.objectContaining({
                securityCode: 'CVE-1235',
                waived: false,
                grandfathered: false
              }),
              jasmine.objectContaining({
                securityCode: 'CVE-1236',
                waived: false,
                grandfathered: false
              }),
              jasmine.objectContaining({
                securityCode: 'CVE-1236',
                waived: false,
                grandfathered: true
              }),
              jasmine.objectContaining({
                securityCode: 'CVE-1238',
                waived: true,
                grandfathered: false
              }),
              jasmine.objectContaining({
                securityCode: 'CVE-1239',
                waived: false,
                grandfathered: false
              }),
              jasmine.objectContaining({
                securityCode: 'CVE-1240',
                waived: true,
                grandfathered: true
              }),
              jasmine.objectContaining({
                securityCode: 'CVE-1241',
                waived: true,
                grandfathered: true
              })
            ]);
          }
      );

      it('calculates a violationSortState that sorts open violations first, followed by non-violating, ' +
          'followed by waived, grandfathered & waived, and then finally grandfathered', function() {
        expect(applicationReportService.getVulnerabilities(policyEntries, rawDataEntries)).toEqual([
          jasmine.objectContaining({
            securityCode: 'CVE-1234',
            violationSortState: 0
          }),
          jasmine.objectContaining({
            securityCode: 'CVE-1235',
            violationSortState: 0
          }),
          jasmine.objectContaining({
            securityCode: 'CVE-1236',
            violationSortState: 0
          }),
          jasmine.objectContaining({
            securityCode: 'CVE-1236',
            violationSortState: 4
          }),
          jasmine.objectContaining({
            securityCode: 'CVE-1238',
            violationSortState: 2
          }),
          jasmine.objectContaining({
            securityCode: 'CVE-1239',
            violationSortState: 0
          }),
          jasmine.objectContaining({
            securityCode: 'CVE-1240',
            violationSortState: 3
          }),
          jasmine.objectContaining({
            securityCode: 'CVE-1241',
            violationSortState: 3
          })
        ]);
      });

      it('includes the highest matching policyThreatLevel for each vulnerability, ' +
          'treating waived and grandfathered violations as 0', function() {
        expect(applicationReportService.getVulnerabilities(policyEntries, rawDataEntries)).toEqual([
          jasmine.objectContaining({
            securityCode: 'CVE-1234',
            policyThreatLevel: 2
          }),
          jasmine.objectContaining({
            securityCode: 'CVE-1235',
            policyThreatLevel: 2
          }),
          jasmine.objectContaining({
            securityCode: 'CVE-1236',
            policyThreatLevel: 1
          }),
          jasmine.objectContaining({
            securityCode: 'CVE-1236',
            policyThreatLevel: 0
          }),
          jasmine.objectContaining({
            securityCode: 'CVE-1238',
            policyThreatLevel: 0
          }),
          jasmine.objectContaining({
            securityCode: 'CVE-1239',
            policyThreatLevel: 3
          }),
          jasmine.objectContaining({
            securityCode: 'CVE-1240',
            policyThreatLevel: 0
          }),
          jasmine.objectContaining({
            securityCode: 'CVE-1241',
            policyThreatLevel: 0
          })
        ]);
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
