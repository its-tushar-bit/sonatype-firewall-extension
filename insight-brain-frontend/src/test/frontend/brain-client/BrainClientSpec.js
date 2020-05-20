/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
const Brain = window.Brain;

describe('brain.client.js', function() {
  describe('getCsrfHeaders', function() {
    it('can read the CSRF cookie', function() {
      document.cookie = 'CLM-CSRF-TOKEN=';
      document.cookie = 'CLM-SESSION=';
      expect(Brain.getCsrfHeaders()).toEqual({ 'X-CSRF-TOKEN': '' });
      document.cookie = 'CLM-CSRF-TOKEN=csrfToken';
      document.cookie = 'CLM-SESSION=sessionId';
      expect(Brain.getCsrfHeaders()).toEqual({ 'X-CSRF-TOKEN': 'csrfToken' });
    });
  });

  describe('getSuggestedRemediationUrlForApplication', function() {
    it('can get the suggested remediation URL', function() {
      var appId = 'APPID';
      expect(Brain.getSuggestedRemediationUrlForApplication(appId)).toContain(
          '/api/v2/components/remediation/application/' + appId
      );
    });
  });

  describe('getInternalApplicationIdUrlForApplicationId', function() {
    it('can get the suggested remediation URL', function() {
      var appId = 'APPID';
      expect(Brain.getInternalApplicationIdUrlForApplicationId(appId)).toContain(
          '/api/v2/applications?publicId=' + appId
      );
    });
  });
});
