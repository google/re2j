Releasing RE2/J to Maven Central
================================

This document describes the process for releasing RE2/J binary artifacts to 
Maven Central and to the RE2/J GitHub site.

# Prerequisites

The following are one-time setup steps. You must have:

* git
* an account on OSSRH, see the [initial setup
  section](http://central.sonatype.org/pages/ossrh-guide.html#initial-setup) of
  the OSSRH guide
* access to the `com.google.re2` OSSRH repository. For this, you should
  request access by [filing a JIRA ticket](https://issues.sonatype.org/secure/CreateIssue!default.jspa)
  with the OSSRH folks
* a GPG key (see below)

## GPG key

You must create a GPG key for signing the RE2/J artifact. The key must be
published to one of the GPG keyservers, otherwise Sonatype will reject the
RE2/J artifact.

```
$ gpg --gen-key

... follow the prompts

$ gpg --list-keys
/home/sjr/.gnupg/pubring.kbx
----------------------------
pub   rsa3072 2022-06-30 [SC] [expires: 2024-06-29]
      ABCDEF01234567890ABCDEF01234567890ABCDEF
uid           [ultimate] Your Name <your@email.com>
sub   rsa3072 2022-06-30 [E] [expires: 2024-06-29]

# Now send the key to the keyservers

$ gpg --keyserver keys.openpgp.org --send-keys 90ABCDEF
gpg: sending key 1234567890ABCDEF to hkp://keys.openpgp.org
```

# Making the release

In a shell, change into the RE2/J source code root directory (the one
containing `build.gradle`). Then:

* edit `build.gradle` and set `version` to the name of the next release
  (e.g. "1.8").
* change the download instructions in `README.md` to reflect the new version
  number
* `git commit` the version name change
* `git tag re2j-<versionName>`, e.g. `git tag re2j-1.8`
* `git push --tags`

Now you're ready to build and push the release.

```
./gradlew -Psonatype.username='sonatypeUsername' -Psonatype.password='sonatypeApiKey' \
        clean bintrayUpload
```

Once successful, the new version needs to be published:

1. Log into the [Sonatype Nexus frontend](https://oss.sonatype.org/)
2. Click on "Staging Repositories" on the left
3. Select the latest staging repository, click "Close"
4. If all of the repository checks pass, the repository will be closed. You can
   now click "Release". This will send the artifact out to Maven Central, ready
   for people to use.

As a convenience to our users, you should also publish the sources, javadoc and
artifact JARs as GitHub releases.

After this is all done, make an announcement on `re2j-discuss@googlegroups.com`.

# Problems

If you encounter issues, please reach out to the mailing list at
`re2j-discuss@googlegroups.com`.
