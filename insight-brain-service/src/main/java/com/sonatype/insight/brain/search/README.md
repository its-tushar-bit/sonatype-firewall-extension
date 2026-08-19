<!--

    Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
    Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
    "Sonatype" is a trademark of Sonatype, Inc.

-->
# Advanced Search 

Provides the ability for users to search CVE vulnerabilities within IQ Server. The search results provides links to 
the relevant application and policy report.

## Indexing
Before the data can be searched an index must be created. This can be achieved by the following cURL command
```
curl -u admin:admin123 -X POST 'http://localhost:8070/api/v2/search/index'
```

`com.sonatype.insight.brain.search.index.FieldIdentifier` lists the indexed fields.
These are also the fields you can prefix the search with as follows:

```
itemType:organization
applicationName:"Awesome-Application"
vulnerabilityId:CVE-2012-*
```

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
