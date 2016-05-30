var JiraServiceMockData = {
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
