OrganizationMockData = {
  getGETResponse: function() {
    return [
      {
        "id": "1",
        "name": "org1"
      },
      {
        "id": "2",
        "name": "org2"
      },
      {
        "id": "3",
        "name": "org3"
      }
    ];
  },
  getPOSTResponse: function(name) {
    return {
      "id": "newid",
      "name": name
    }
  }
};