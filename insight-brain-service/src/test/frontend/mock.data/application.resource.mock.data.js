var ApplicationResourceMockData = {
  getApplicationSummaryUrl: function() {
    return {
      id: "fakeId",
      name: "fakeName",
      organizationId: "fakeOrdId",
      organizationName: "fakeOrgName",
      publicId: "fakePublicId",
      policyEvaluations: {
        build: {id: "fakePolicyEvaluationId", scanId: "fakeScanId"},
        "stage-release": {id: "fakePolicyEvaluationId", scanId: "fakeScanId"},
        release: {id: "fakePolicyEvaluationId", scanId: "fakeScanId"}
      }
    };
  },
  getApplicationUrl: function() {
    return {
      contact: null,
      id: "fakeId",
      name: "fakeName",
      organizationId: "fakeOrdId",
      organizationName: "fakeOrgName",
      publicId: "fakePublicId",
    };
  }
};
