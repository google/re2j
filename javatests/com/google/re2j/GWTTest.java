package com.google.re2j;

import com.google.common.base.Joiner;
import com.google.common.io.ByteStreams;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static com.google.common.truth.Truth.assertThat;

/**
 * This class exercises the GWT compiler against a small RE2J test GWT module. The purpose of the
 * test is to break when GWT-incompatible changes are made to RE2J.
 */
@RunWith(JUnit4.class)
public class GWTTest {
  @Rule public TemporaryFolder gwtProjectDir = new TemporaryFolder();

  @Test
  public void testCompilesWithGwt() throws IOException, InterruptedException {
    // First, create a JAR containing a fake GWT project.
    // The jar contains:
    //   - all non-test source files
    //   - all non-test compiled class files
    //   - the RE2J.gwt.xml file in the root.
    File jarFile = new File(gwtProjectDir.getRoot(), "re2j.jar");
    JarOutputStream jar = new JarOutputStream(new FileOutputStream(jarFile));
    String projectRoot = System.getProperty("user.dir");
    addFiles(jar, "", new File(projectRoot, "java"));
    addFiles(jar, "", new File(projectRoot, "/target/classes"));
    addFile(
        jar,
        "com/google/re2j",
        new File(projectRoot, "/build/resources/test/com/google/re2j/RE2J-Fake.gwt.xml"));
    jar.finish();
    jar.close();

    String javaHome = System.getProperty("java.home");
    // Now spawn a new JVM that runs the GWT compiler. The sub JVM:
    //   - has the newly created JAR file as the first entry in the classpath
    //   - otherwise has the same classpath as the parent
    //   - has a PWD directory set to a temporary scratch dir
    //       (so the GWT compiler doesn't touch the working dir)
    //   - redirects its stderr to stdout
    Process p =
        new ProcessBuilder(
                javaHome + "/bin/java",
                "-classpath",
                Joiner.on(File.pathSeparatorChar)
                    .join(jarFile.getAbsolutePath(), System.getProperty("java.class.path")),
                "com.google.gwt.dev.Compiler",
                "com.google.re2j.RE2J-Fake")
            .directory(gwtProjectDir.getRoot())
            .redirectErrorStream(true)
            .start();

    // Copy the output of the sub JVM to our stderr so we can see it.
    ByteStreams.copy(p.getInputStream(), System.err);
    assertThat(p.waitFor()).isEqualTo(0);
  }

  private void addFiles(JarOutputStream jar, String base, File f) throws IOException {
    File[] files = f.listFiles();
    if (files == null) {
      return;
    }

    for (File subF : files) {
      if (subF.isDirectory()) {
        addFiles(jar, entryName(base, subF), subF);
      } else {
        addFile(jar, base, subF);
      }
    }
  }

  private void addFile(JarOutputStream jar, String base, File f) throws IOException {
    ZipEntry e = new ZipEntry(entryName(base, f));
    jar.putNextEntry(e);
    ByteStreams.copy(new FileInputStream(f), jar);
  }

  private static String entryName(String base, File f) {
    return base.isEmpty() ? f.getName() : base + "/" + f.getName();
  }
}
