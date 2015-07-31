PolicyMonitoringMockData = {
  policyMonitoringByOwner: [
    {
      ownerName: 'Dummy App',
      policyMonitoring: {
        id: 'policyMonitoringDummyAppId',
        stageTypeId: 'build',
        ownerId: 'bom1-12345678'
      }
    }, {
      ownerName: 'Dummy Org',
      policyMonitoring: {
        id: "policyMonitoringDummyOrgId",
        ownerId: "3fd6498a8a0d4488a76a54adf41d297b",
        stageTypeId: "develop"
      }
    }, {
      ownerName: 'Dummy Root Org',
      policyMonitoring: {
        id: "policyMonitoringDummyRootOrgId",
        ownerId: "ROOT_ORGANIZATION_ID",
        stageTypeId: "release"
      }
    }
  ]
};
