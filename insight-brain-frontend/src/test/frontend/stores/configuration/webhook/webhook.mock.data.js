/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
export default {
  getWebhooks: function () {
    return [
      {
        id: '3ccc32c267474f5d8ef3ab5d6a9aab1d',
        url: 'http://sonatype.com/3ccc32c267474f5d8ef3ab5d6a9aab1d',
        secretKey: '#~FAKE~CLM~SECRET~KEY~#',
        events: ['MANAGEMENT'],
      },
      {
        id: '7e7413e03c2e4500ae58745862b7397b',
        url: 'http://sonatype.com/7e7413e03c2e4500ae58745862b7397b',
        secretKey: '#~FAKE~CLM~SECRET~KEY~#',
        events: ['MANAGEMENT'],
      },
      {
        id: 'd13a6db67d9b440ba39061be403574d5',
        url: 'http://sonatype.com/d13a6db67d9b440ba39061be403574d5',
        secretKey: '#~FAKE~CLM~SECRET~KEY~#',
        events: ['MANAGEMENT'],
      },
    ];
  },

  getWebhookEventTypes: function () {
    return ['MANAGEMENT', 'APPLICATION_EVALUATION', 'COMPONENT'];
  },
};
