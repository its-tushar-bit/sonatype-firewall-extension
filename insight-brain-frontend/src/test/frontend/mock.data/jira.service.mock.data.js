/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
window.JiraServiceMockData = {
  getJiraProjectsUrl: function() {
    return [
      {
        'key': 'key1',
        'name': 'Project One',
        'issueTypes': [
          {
            'id': 1,
            'name': 'Bug'
          },
          {
            'id': 2,
            'name': 'Task'
          }
        ]
      },
      {
        'key': 'key2',
        'name': 'Project Two',
        'issueTypes': [
          {
            'id': 1,
            'name': 'Bug'
          },
          {
            'id': 3,
            'name': 'Issue'
          }
        ]
      }
    ];
  }
};
