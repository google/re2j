Releasing RE2/J to Maven Central
================================

This document describes the process for releasing RE2/J binary artifacts to 
Maven Central and to the RE2/J GitHub site.

# Prerequisites

The following are one-time setup steps. You must have:

* git
* an account on OSSRH, see the [initial setup section](http://central.sonatype.org/pages/ossrh-guide.html#initial-setup) of the OSSRH guide
* access to the `com.google.re2` OSSRH repository. For this, you should
  request access by [filing a JIRA ticket](https://issues.sonatype.org/secure/CreateIssue!default.jspa)
  with the OSSRH folks
* an Open Source account on [JFrog Bintray](https://bintray.com/signup/oss)
* access to the Bintray [RE2/J organization](https://bintray.com/re2j/). To get
  this, follow the link and click 'Join'. Your request will be moderated by
  somebody already in the organization.
* an [API key](https://www.jfrog.com/confluence/display/RTF/Updating+Your+Profile#UpdatingYourProfile-APIKey) for Bintray

# Making the release

In a shell, change into the RE2/J source code root directory (the one
containing `build.gradle`). Then:

* edit `build.gradle` and set `versionName` to the name of the next release
  (e.g. "1.3").
* change the download instructions in `README.md` to reflect the new version
  number
* `git commit` the version name change
* `git tag re2j-<versionName>`, e.g. `git tag re2j-1.3`
* `git push --tags`

Now you're ready to build and push the release.

```
BINTRAY_USER=<your bintray username> BINTRAY_KEY=<your bintray API key> ./gradlew bintrayUpload
```

Once successful, the new version needs to be published. Log into Bintray, find
the new version you have uploaded, then click 'Publish'. People may now use
JCenter to get RE2/J.

Some people still use Maven Central, so we push the artifact there as well.
Click on "Maven Central", enter your OSSRH username and password, ensure the
"Close and release" checkbox is selected and click "Sync". RE2/J will be synced
to Maven Central after some time (usually around 10 minutes).

# Problems

If you encounter issues, please reach out to the mailing list at
re2j-discuss@googlegroups.com.
