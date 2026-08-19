<!--

    Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
    Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
    "Sonatype" is a trademark of Sonatype, Inc.

-->
# DevDocs vs ADRs

This documentation is meant to solve two different problems

1. Sharing context with other engineers (/devdocs)
2. Enabling engineers to make architectural proposals (/adr)

## DevDocs

There is a significant amount of context that is stored only in the heads of IQ engineers. The DevDocs folder is a place
to share that knowledge.

### What is suitable for a DevDoc?

- Anything that is broader than a code comment. These DO NOT replace code comments!!!
- If you're implementing something and think to yourself "someone else might not understand what I've done here and why"
  then create a DevDoc
- If you find/learn something about the system you think others would benefit from knowing, document it.

There are plenty examples of Slack conversations where someone injects vital context into a conversation, in those
situations we should encourage the person to write a low-fuss DevDoc.

### What is the process / format?

Creating a DevDoc is intentionally meant to be friction-free. It is more important we have documentation than we have
perfectly organized/formatted documentation. If we get too many documents that is hard to manage, that will be a nice
problem to have, and we can fix it later.

* Low / No process!
* Create a doc in the DevDocs folder
* You decide the format
* Not meant to be a quality gate

## ADRs

ADRs are more formal and are used to propose architectural changes that need to be reviewed by the Tech Leads. These
will be created in `/docs/adr`.

### What needs an ADR?

What needs an ADR falls into two broad categories:

1. Large changes that would need significant roadmap time.
2. Anything that challenges the status quo / the current set of trade-offs

Examples: 
* Structure (for example, patterns such as microservices)
* Non-functional requirements (security, high availability, and fault tolerance)
* Construction techniques (libraries, frameworks, tools, and processes)
* …TBD

### ADR Process

1. Copy template in ADR folder
2. Name ADR with the Next ID - Title - Status (e.g. 0001-distributed-architecture-proposed.md)
3. Create a PR for proposal
4. Proposal reviewed by TLs / Segment TL
5. Within 2 weeks proposal status changed to either Rejected or Accepted
6. Update status in title of document
7. If approved - Segment TL to work with Product/Engineering Management to get work scheduled (PDLC Process)

### Consequences Section

At the end of each ADR is a section titled Consequences. This is meant as a place to record how our decision affected
the application/development over time. Anyone can update this section at any time.
