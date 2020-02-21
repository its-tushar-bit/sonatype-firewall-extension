<!--

    Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
    Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
    "Sonatype" is a trademark of Sonatype, Inc.

-->
# IQ Global Search 

Provides the ability for users to search CVE vulnerabilities within IQ Server. The search results provides links to 
the relevant application and policy report.

## Indexing
Before the data can be searched an index must be created. This can be achieved by the following cURL command
```
curl -u admin:admin123 -X POST 'http://localhost:8070/api/experimental/search/index'
```

Currently the following fields will be indexed and available for searching.

- organizationId (description goes here)
- organizationName (description goes here)
- applicationId (description goes here)
- applicationName (description goes here)
- applicationPublicId (description goes here)
- itemType (description goes here)
- policyId
- policyName
- policyThreatCategory
- policyThreatLevel
- policyEvaluationStage (description goes here)
- reportId (description goes here)
- componentHash (description goes here)
- componentFormat (description goes here)
- componentName (description goes here)
- componentCoordinate[GroupId|ArtifactId|Version|Extension|Classifier|...]
- componentLabelId
- componentLabelName
- componentLabelColor
- componentLabelDescription
- vulnerabilityId (description goes here)
- vulnerabilityStatus (description goes here)
- vulnerabilityDescription (description goes here)
- applicationCategoryId (description goes here) 
- applicationCategoryName (description goes here)
- applicationCategoryColor (description goes here)
- applicationCategoryDescription (description goes here)

## Searching
Data can be searched for by using the search box located in the top menu bar. If no field is provided then search is 
defaulted to the **refId** field. The following are examples of the types of searches available.

### Example 1 - Search using CVE
CVE-2016-1000031

### Example 2 - Wildcard search
CVE-2016-*

### Example 3 - Search fields
To search a specific field the following format must be used `<fieldname>:<value>`

description:apache - S
format:maven
extension:jar
applicationName:App*


## Contributors
- Richard Mealing
- Steve Baker
- Anna Damtsa
- Usman Shaikh
- Koray Tugay

