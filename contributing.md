<!--

    Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
    Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
    "Sonatype" is a trademark of Sonatype, Inc.

-->
# Contributing

We in Team Insight have the privilege of working on Nexus Lifecycle, but we also have the responsibility to make our 
environment inclusive. 
Sonatype has the best chance for success when we __**all**__ have the ability to make Lifecycle the best product in the 
market, not just a select few.

# TL;DR

Here's the *too long, didn't read* version:

* We welcome your contribution but please don't start the conversation with code and a pull request. [You will ultimately be more successful if you talk to a few humans first](#discovery); preferably a Product Manager and an Engineer.
* Small focused contributions are best. We have some guidelines to follow on [Writing](#writing) code and [Reviewing](#submitting)
* We expect you to bear responsibility to [see it through to the end](#finishing)!

And now, the long form:

# Preface

This document intends to lay out the guidelines and expectations for all contributors so that we can achieve the 
following:

* All Sonatypers have the ability to lend their expertise
* All Sonatypers have a good experience contributing

To help achieve those goals, we have adopted the [Contributor Covenant, version 1.4](code-of-conduct.md).

We want your feedback. Tell us about your experience via a two question form:

[Contributing to Lifecycle](https://forms.gle/93VdqMXDXhjS5crFA)

You can also reach out privately  to `@Nicholas Blair`, `@Tim Levett`, or `@Brandon Sedgwick` directly via Slack, or 
 all three of us with `@iq-em`.

This set of guidelines is broken down into four sections:

* [Discovery](#discovery)
* [Writing](#writing)
* [Submitting](#submitting)
* [Finishing](#finishing)

## Discovery

What problem do you need to solve? We want to hear it :)

Here are some ways to reach us:

* Start a conversation in `#product-management`! All Sonatype product managers - including IQ - hang out there. Maybe 
 your idea is already in flight, has been ruled out, or completely new!
* `#iq` in slack has a large audience, and tends to skew towards technical Q&A. Good topics in this channel are:
  - What's available in the REST API?
  - Command line options
  - Is this a bug? Or expected behavior
* If you like email, you can use the insight-dev mailing list.

We find the most success when we get the goals out in the open before the code is written - you might not even have to 
 write a single line of code! Don't be opaque - start with the outcome you'd like to achieve.

## Writing

So you've done some discovery, and it turns out you've got a novel idea. Now it's time to open up the IDE and get to 
 work. Here are some guidelines for success:

### Do

* Clone this repo and create a branch locally for your contribution. You can push that branch to this repository 
 (check with Nick/Tim/Brandon to get access if you don't already). 
* If your change set includes visual changes, enable our visual testing in your Jenkins feature branch build.
* Code formatting is enforced by [Spotless](https://github.com/diffplug/spotless) with an Eclipse formatter config.
 Locally, `spotless:apply` auto-formats changed files automatically during builds. You can check locally with `mvn spotless:check`.
* Make sure license headers are applied; this is the first test in our CI build.
* Include tests!
* If you see a minor improvement within the context of the change, feel free to include it!

### Don't

* Don't fork this repository, we'd rather maintain branches here.
* Do not introduce -SNAPSHOT dependencies on main branch, as it delays our ability to release from main. It's ok while you
 are developing, but please ensure your pull request only references releases of new dependencies.
* Do not make larger improvements that span outside of the context of the change. We would welcome those changes in a
 separate pull request (so please follow up with it). Avoid making formatting changes outside of the code in context, 
 but certainly follow up with a separate pull request to fix formatting issues you find.

### Summary for Writing 

Looking for someone with technical experience to help guide you? Reach out to 
[Insight Technical Leadership](https://docs.sonatype.com/display/INSIGHT/Insight+Technical+Leadership). 

It helps if you mention the person in particular you are seeking guidance from, something like:

> in #iq: hey @kasun, I'm hoping to accomplish X...; I have a question about Y

If you don't want the audience of the whole channel, direct messages are welcome too.

## Submitting

Now you have something real that's working.
Time to cross your t's and dot the i's - let's get your code in product:

### Do

* Open a pull request against the main branch.
* We prefer a title format that looks like "Brief description - Jira issue id".
* In the description, remind us of the problem, and HOW you've solved it. 
* Run a CI build in jenkins: Go to the 
 [Feature Snapshot Builds job](https://jenkins.ci.sonatype.dev/job/insight/job/insight-brain/job/feature-snapshots/), find
 your branch from the list, and run it.
* Does your change include new User Interface components? Show us in the description with a screenshot, or maybe a 
 before/after.
* Review the code within your small team and any stakeholders as necessary.
* Contact this week's reviewers and ask for their attention (see readme.md for the rotating github usernames).
* Got a Jira issue? Link it! We love to link back to stories in Jira, it helps us keep track of contributions.

Our visual testing tool (Applitools) has a caveat that the pull request must exist before it can associate the test
results with the changes. As a result, here's a recommended workflow for changes that include UI components:

1. Open your pull request as a Draft first.
2. Run our [CI build](https://jenkins.ci.sonatype.dev/job/insight/job/insight-brain/job/feature-snapshots/).
3. Take a gander at 
 [our documentation](https://docs.sonatype.com/pages/viewpage.action?spaceKey=INSIGHT&title=Automated+Visual+Testing+with+Applitools)
 on how to work with Applitools. Reach out in Slack at `#iq-lorage` for help.
4. Once you see all 3 github checks passing (jenkins, applitools, and applitools mergeability), convert the draft to an
 open pull request.

### Don't

* Wait for Team Insight to review, +1, or merge your work. We may not be the best people to review. We prefer that the
 person that opened the contribution be the one to merge the contribution.
* Open a pull request and forget about it. Your PR should be open for only a few days; either merge, or close if it's 
not working out.

## Finishing

You are at the final stages. Your change works, it's polished, and it's ready to merge.

### UI changes

If your change introduces new or modifies any HTML, SCSS, or JavaScript; please request a review from 
[@iq-lorage](https://github.com/orgs/sonatype/teams/iq-lorage) in Github.  A +1 from our team is not needed to proceed with front end development.

### All other changes

"Two plus-ones" is a common lore at Sonatype, but it is not necessary here. 

What is necessary is that someone other than yourself who is in the best position to review the work can attest that
the problem is satisfactorily solved. This may be someone in your delivery team, it does not have to be someone from
Insight.

We appreciate being in the loop, but don't wait for a plus one from us. You may get questions or comments from us if 
it's something we understand.

Merge (squash please) away, and follow through:

* Verify the main snapshot build completes.
* Delete the branch.
* Update any necessary documentation.
* Notify the product managers (in your team and in insight). We may need release notes to go with your contribution.

Last but not least - tell us about your experience!

* Contact `@Nicholas Blair`, `@Sanika Sudhalkar`, or `@Amirali Ghadiri` directly via Slack
* Fill out our two question survey:

[Contributing to Lifecycle](https://forms.gle/93VdqMXDXhjS5crFA)
