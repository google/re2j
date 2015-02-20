Releasing RE2/J to Maven Central
================================

This document describes the process for releasing RE2/J binary artifacts to 
Maven Central and to the RE2/J GitHub site.

# Prerequisites

You must have:

* git
* [Apache Maven](http://maven.apache.org/), at least version 3.0.3
* an account on OSSRH, see the [initial setup section](http://central.sonatype.org/pages/ossrh-guide.html#initial-setup) of the OSSRH guide
* access to the `com.google.re2` OSSRH repository. For this, you should
  request access by [filing a JIRA ticket](https://issues.sonatype.org/secure/CreateIssue!default.jspa)
  with the OSSRH folks
* the following `$HOME/.m2/settings.xml` file:
```xml
<settings>
  <servers>
    <server>
      <id>sonatype-nexus-staging</id>
      <username>YourOssrhUsername</username>
      <password>YourOssrhPassword</password>
    </server>
  </servers>
</settings>
```
* a GPG key that is published to a public keyserver such as
  [the MIT PGP keyserver](http://pgp.mit.edu/). For detailed instructions, see
  [the GNU Privacy Handbook](https://www.gnupg.org/gph/en/manual.html),
  specifically the sections on [generating a new keypair](https://www.gnupg.org/gph/en/manual.html#AEN26)
  and [distributing keys](https://www.gnupg.org/gph/en/manual.html#AEN464)

# Making the release

In a shell, change into the RE2/J source code root directory (the one
containing `pom.xml`), and then run

```
mvn release:clean
mvn release:prepare
mvn release:perform
```

You may be asked for your GitHub credentials several times. If your account
uses two-factor authentication (2FA), then you will need to generate a
temporary [access token](https://help.github.com/articles/creating-an-access-token-for-command-line-use/)
and then use that access token in place of your normal password. You may delete
the access token from your account once the release is done.

Once you've done these steps, you'll have to
[login to OSSRH](https://oss.sonatype.org)(look for the Log-In button in the 
top-right). Once you are logged in, follow these steps:

* click on "Staging Repositories"
* locate the repository that was created for you when you ran 
  `mvn release:perform`. It will contain re2j in its name
* click the checkbox near the repository's name, then click "Close" in the
  top menubar
* OSSRH will perform some validation on the repository. You may need to hit the
  refresh button a few times before you see the repository enter the "closed"
  state. Once this happens, make sure the repository is checked, then click
  "Release". For the comment, enter "RE2/J release 1.0" (replace "1.0" with
  the actual release number)

At this point, the release will be synced to Maven Central within a few minutes.
You can take this time to update the Maven XML snippet on the RE2/J GitHub page
to mention the new release number.

Some people don't use Maven, so we can upload binaries to the GitHub site for
manual download. To do this, follow the instructions for
[creating releases](https://help.github.com/articles/creating-releases/). For
the Git tag, use the tag that was created as a result of the
`mvn release:prepare` command that you ran earlier.

Before you publish the release, attach the following binaries to the release:

* `target/re2j-(releaseNumber).jar`
* `target/re2j-(releaseNumber)-javadoc.jar`
* `target/re2j-(releaseNumber)-sources.jar`

Once you publish this release, the process is complete!
