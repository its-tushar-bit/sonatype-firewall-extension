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
  },
  successMetrics: {
    inheritPolicy: false,
    enablePurging: false,
    maxAge: null
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
  },
  successMetrics: {
    inheritPolicy: true,
    enablePurging: true,
    maxAge: '1 year'
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
  },
  successMetrics: {
    inheritPolicy: false,
    enablePurging: true,
    maxAge: '2 years'
  }
};
