/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { displayWaiverScope, formatWaiverDetails, waiverMatcherStrategy } from 'MainRoot/util/waiverUtils';

describe('waiverUtils', function () {
  describe('dislayWaiverScope', () => {
    it('returns a readable label if the scopeOwnerType is `root_organization`', () => {
      const waiver = {
        scopeOwnerType: 'root_organization',
      };
      const result = displayWaiverScope(waiver);
      expect(result).toEqual('Root Organization');
    });

    it('returns a readable label with name if the scopeOwnerType is `organization`', () => {
      const waiver = {
        scopeOwnerType: 'organization',
        scopeOwnerName: 'a org',
      };
      const result = displayWaiverScope(waiver);
      expect(result).toEqual('Organization - a org');
    });

    it('returns a readable label with name if the scopeOwnerType is `application`', () => {
      const waiver = {
        scopeOwnerType: 'application',
        scopeOwnerName: 'App X',
      };
      const result = displayWaiverScope(waiver);
      expect(result).toEqual('Application - App X');
    });

    it('returns a readable label with name if the scopeOwnerType is `repository`', () => {
      const waiver = {
        scopeOwnerType: 'repository',
        scopeOwnerName: 'Repo X',
      };
      const result = displayWaiverScope(waiver);
      expect(result).toEqual('Repository - repository');
    });

    it('returns null if the scopeOwnerType is not valid', () => {
      let waiver = {
        scopeOwnerType: 'weird',
      };
      let result = displayWaiverScope(waiver);
      expect(result).toBeNull();

      waiver.scopeOwnerType = undefined;
      result = displayWaiverScope(waiver);
      expect(result).toBeNull();

      waiver.scopeOwnerType = null;
      result = displayWaiverScope(waiver);
      expect(result).toBeNull();
    });
  });

  describe('formatWaiverDetails', () => {
    const componentDisplayName = {
      parts: [
        {
          field: 'Group',
          value: 'test-group',
        },
        {
          value: ':',
        },
        {
          field: 'Artifact',
          value: 'test-artifact',
        },
        {
          value: ':',
        },
        {
          field: 'Version',
          value: '1.2.3',
        },
      ],
    };

    it('returns a falsy object for a falsy waiver object', () => {
      expect(formatWaiverDetails(null)).toEqual({});
    });

    it('returns the correctly formatted data from the waiver object', function () {
      const waiverDetails = {
        comment: 'a comment',
        constraintFacts: [
          { constraintName: 'test constraint', conditionFacts: [{ reason: 'reason 1' }, { reason: 'reason 2' }] },
        ],
        createTime: '08/18/2022',
        creatorName: 'test creator',
        expiryTime: '08/18/2023',
        policyName: 'test policy',
        policyWaiverId: '1234testid',
        scopeOwnerId: 'ROOT_ORGANIZATION',
        scopeOwnerName: 'root org',
        scopeOwnerType: 'root_organization',
        vulnerabilityId: 'CVE-2013-7285',
        associatedPackageUrl: 'a/package/url',
        componentIdentifier: null,
        matcherStrategy: waiverMatcherStrategy.EXACT_COMPONENT,
        displayName: componentDisplayName,
      };

      expect(formatWaiverDetails(waiverDetails)).toEqual({
        policyName: 'test policy',
        constraintName: 'test constraint',
        reasons: ['reason 1', 'reason 2'],
        waiverScope: 'Root Organization',
        expiration: '08/18/2023',
        comment: 'a comment',
        creatorName: 'test creator',
        vulnerabilityId: 'CVE-2013-7285',
        dateCreated: '08/18/2022',
        component: {
          associatedPackageUrl: 'a/package/url',
          componentIdentifier: null,
          displayName: componentDisplayName,
          matcherStrategy: waiverMatcherStrategy.EXACT_COMPONENT,
        },
      });
    });

    it('formats the expiration correctly for waivers with no expiration date', () => {
      const waiverDetails = {
        comment: 'a comment',
        constraintFacts: [
          { constraintName: 'test constraint', conditionFacts: [{ reason: 'reason 1' }, { reason: 'reason 2' }] },
        ],
        createTime: '08/18/2022',
        creatorName: 'test creator',
        policyName: 'test policy',
        policyWaiverId: '1234testid',
        scopeOwnerId: 'ROOT_ORGANIZATION',
        scopeOwnerName: 'root org',
        scopeOwnerType: 'root_organization',
        vulnerabilityId: 'CVE-2013-7285',
        associatedPackageUrl: 'a/package/url',
        componentIdentifier: null,
        matcherStrategy: waiverMatcherStrategy.EXACT_COMPONENT,
        displayName: componentDisplayName,
      };

      expect(formatWaiverDetails(waiverDetails)).toEqual({
        policyName: 'test policy',
        constraintName: 'test constraint',
        reasons: ['reason 1', 'reason 2'],
        waiverScope: 'Root Organization',
        expiration: 'Does not expire',
        comment: 'a comment',
        creatorName: 'test creator',
        vulnerabilityId: 'CVE-2013-7285',
        dateCreated: '08/18/2022',
        component: {
          associatedPackageUrl: 'a/package/url',
          componentIdentifier: null,
          displayName: componentDisplayName,
          matcherStrategy: waiverMatcherStrategy.EXACT_COMPONENT,
        },
      });
    });
  });
});
