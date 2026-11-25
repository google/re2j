Releasing RE2/J to Maven Central
================================

This document describes the process for releasing RE2/J binary artifacts to 
Maven Central and to the RE2/J GitHub site.

# Prerequisites

The following are one-time setup steps. You must have:

* git
* an account on the [Maven Central Portal](https://central.sonatype.com/) with
  publishing rights for the `com.google.re2j` namespace
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

## Gradle properties

Add the following properties to `$HOME/.gradle/gradle.properties`:

```
signing.gnupg.executable=gpg
signing.gnupg.keyName=ABCDEF01
```

# Making the release

In a shell, change into the RE2/J source code root directory (the one
containing `build.gradle`). Then:

* edit `build.gradle` and set `version` to the name of the next release
  (e.g. "1.9").
* change the download instructions in `README.md` to reflect the new version
  number
* `git commit` the version name change
* `git tag re2j-<versionName>`, e.g. `git tag re2j-1.9`
* `git push --tags`

Now you're ready to build the signed release bundle:

```
./gradlew clean createCentralPortalBundle
```

This creates `build/distributions/re2j-<versionName>-bundle.zip` containing the
signed JARs, POM, and checksums. Once created, publish it via the
[Maven Central Portal](https://central.sonatype.com/publishing/deployments):

1. Log into the [Maven Central Portal](https://central.sonatype.com/)
2. Go to **Publish** -> **Publish Component**
3. Upload `build/distributions/re2j-<versionName>-bundle.zip`
4. Wait for validation to succeed, then click **Publish** to release the
   artifact to Maven Central.

As a convenience to our users, you should also publish the sources, javadoc and
artifact JARs as GitHub releases.

After this is all done, make an announcement on `re2j-discuss@googlegroups.com`.

# Problems

If you encounter issues, please reach out to the mailing list at
`re2j-discuss@googlegroups.com`.
