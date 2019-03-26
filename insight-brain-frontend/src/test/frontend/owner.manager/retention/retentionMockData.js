export const disabledRetentionPolicies = {
  applicationReports: {
    stages: {
      'stage 1': {
        inheritPolicy: false,
        enablePurging: false,
        maxCount: null,
        maxAge: null
      },
      'stage 2': {
        inheritPolicy: false,
        enablePurging: false,
        maxCount: null,
        maxAge: null
      }
    }
  }
};
export const inheritedRetentionPolicies = {
  applicationReports: {
    stages: {
      'stage 1': {
        inheritPolicy: true,
        enablePurging: true,
        maxCount: 1,
        maxAge: '2 days'
      },
      'stage 2': {
        inheritPolicy: true,
        enablePurging: true,
        maxCount: 3,
        maxAge: '4 days'
      }
    }
  }
};
export const customRetentionPolicies = {
  applicationReports: {
    stages: {
      'stage 1': {
        inheritPolicy: false,
        enablePurging: true,
        maxCount: 1,
        maxAge: '2 days'
      },
      'stage 2': {
        inheritPolicy: false,
        enablePurging: true,
        maxCount: 3,
        maxAge: '4 days'
      }
    }
  }
};
