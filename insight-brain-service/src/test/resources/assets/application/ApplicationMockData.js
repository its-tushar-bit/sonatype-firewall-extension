ApplicationMockData = {
    getApplicationsData: function () {
        return [
            {
                "id": "78c1d44c07584e57945f04890c672e82",
                "publicId": "bom1-12345678",
                "policyEvaluations": {
                	"build": {
	                	"stage": {
	                		"stageTypeId": "build"
	                	},
	                	"scanId": "2e12e6a9811347a78031b8969b604c49",
	                	"time": 1371487786570,
	                	"user": "anonymous"
                	}
            	},
                "policyEvaluationsResults": {
                	"build": {
	                	"alerts": [ ],
	                	"affectedComponentCount": 0,
	                	"criticalComponentCount": 0,
	                	"severeComponentCount": 0,
	                	"moderateComponentCount": 0
                	}
            	}
            }
        ];
    }
};