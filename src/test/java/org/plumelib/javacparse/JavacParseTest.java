package org.plumelib.javacparse;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.source.tree.Tree;
import java.io.IOException;
import java.util.StringJoiner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.plumelib.util.SystemPlume;

class JavacParseTest {
  @Test
  void javacParseTest() {
    assertThrows(IOException.class, () -> JavacParse.parseFile("foo bar"));

    // Class
    String sc1 = "class MyClass { void m() {} }";

    // Import and package
    String si1 = "package x.y.z;\n";
    String si2 = "import a.b.C.d;\nimport x.y.Z;\n";

    // Compilation unit.
    String scu1 =
        "class MyClass { void m() {} } \nclass OtherClass { String f = \"hello world\"; }";
    String scu2 = si1 + scu1;
    String scu3 = si2 + scu1;
    String scu4 = si1 + si2 + scu1;
    String scu5 = ";";
    String scu6 = si1 + scu3;
    String scu7 = si2 + scu3;
    String scu8 = si1 + si2 + scu3;
    String scu9 = sc1;
    String scu10 = si1 + scu5;
    String scu11 = si2 + scu5;
    String scu12 = si1 + si2 + scu5;

    // Invalid code.
    String sinv1 = "class SyntaxError { void () {} }";
    String sinv2 = si1 + sinv1;
    String sinv3 = "Hello this is nonsense.";

    // Compilation unit.

    assertNoParseError(JavacParse.parseCompilationUnit(scu1), scu1);
    assertNoParseError(JavacParse.parseCompilationUnit(scu2), scu2);
    assertNoParseError(JavacParse.parseCompilationUnit(scu3), scu3);
    assertNoParseError(JavacParse.parseCompilationUnit(scu4), scu4);
    assertNoParseError(JavacParse.parseCompilationUnit(scu5), scu5);
    assertNoParseError(JavacParse.parseCompilationUnit(scu6), scu6);
    assertNoParseError(JavacParse.parseCompilationUnit(scu7), scu7);
    assertNoParseError(JavacParse.parseCompilationUnit(scu8), scu8);
    assertNoParseError(JavacParse.parseCompilationUnit(scu9), scu9);
    assertNoParseError(JavacParse.parseCompilationUnit(scu10), scu10);
    assertNoParseError(JavacParse.parseCompilationUnit(scu11), scu11);
    assertNoParseError(JavacParse.parseCompilationUnit(scu12), scu12);

    assertTrue(JavacParse.parseCompilationUnit(sinv1).hasParseError());
    assertTrue(JavacParse.parseCompilationUnit(sinv2).hasParseError());
    assertTrue(JavacParse.parseCompilationUnit(sinv3).hasParseError());

    // Class
    assertNoParseError(JavacParse.parseTypeDeclaration(sc1), sc1);

    assertIllegalArgument(() -> JavacParse.parseTypeDeclaration(scu1), scu1);
    assertIllegalArgument(() -> JavacParse.parseTypeDeclaration(scu2), scu2);
    assertIllegalArgument(() -> JavacParse.parseTypeDeclaration(scu3), scu3);
    assertIllegalArgument(() -> JavacParse.parseTypeDeclaration(scu4), scu4);
    assertIllegalArgument(() -> JavacParse.parseTypeDeclaration(scu5), scu5);
    assertIllegalArgument(() -> JavacParse.parseTypeDeclaration(scu6), scu6);
    assertIllegalArgument(() -> JavacParse.parseTypeDeclaration(scu7), scu7);
    assertIllegalArgument(() -> JavacParse.parseTypeDeclaration(scu8), scu8);
    assertNoParseError(JavacParse.parseTypeDeclaration(scu9), scu9);
    assertIllegalArgument(() -> JavacParse.parseTypeDeclaration(scu10), scu10);
    assertIllegalArgument(() -> JavacParse.parseTypeDeclaration(scu11), scu11);
    assertIllegalArgument(() -> JavacParse.parseTypeDeclaration(scu12), scu12);
    assertTrue(JavacParse.parseCompilationUnit(sinv1).hasParseError());
    assertTrue(JavacParse.parseCompilationUnit(sinv2).hasParseError());
    assertTrue(JavacParse.parseCompilationUnit(sinv3).hasParseError());
  }

  /**
   * Throws an error if the parse result has a parse error.
   *
   * @param jpr a parse result
   * @param s the parsed code, for diagnostics
   */
  void assertNoParseError(JavacParseResult<? extends Tree> jpr, String s) {
    if (jpr.hasParseError()) {
      throw new Error("Code=" + s + ", diagnostics=" + jpr.getDiagnostics());
    }
  }

  /**
   * Throws an error if the parsing does not throw IllegalArgumentException.
   *
   * @param thunk a thunk that parses Java code
   * @param s the parsed code, for diagnostics
   */
  void assertIllegalArgument(Executable thunk, String s) {
    try {
      thunk.execute();
      throw new JPTException();
    } catch (JPTException jpte) {
      throw new Error("no exception for " + s);
    } catch (IllegalArgumentException iae) {
      // OK, IllegalArgumentException is expected.
    } catch (Throwable t) {
      throw new Error("wrong exception " + t + " for " + s);
    }
  }

  /** An exception used by assertIllegalArgument. */
  private static final class JPTException extends Exception {
    /** The serial version UID. */
    private static final long serialVersionUID = 20250817;
  }

  @Test
  void memoryTest() {
    double initialUsedMemory = (double) SystemPlume.usedMemory(true);
    int numIterations = 10; // Each iteration takes approximately 1 second.
    if (System.getenv("GITHUB_HEAD_REF") != null) {
      // If this line is reached, the program is running in GitHub Actions continuous integration.
      numIterations *= 10;
    }
    for (int i = 0; i < numIterations; i++) {
      for (int j = 0; j < 1000; j++) {
        // Make a new String each time through the loop.
        StringJoiner sj = new StringJoiner(System.lineSeparator());
        for (int k = 0; k < 1000; k++) {
          sj.add("class MyClass" + k + " { void m() {} }");
        }
        JavacParse.parseCompilationUnit(sj.toString());
      }
      String msg = SystemPlume.gcUsageMessage(.3, 10);
      if (msg != null) {
        System.out.println(msg);
      }
    }
    double finalUsedMemory = (double) SystemPlume.usedMemory(true);
    double memoryRatio = finalUsedMemory / initialUsedMemory;
    if (memoryRatio > 1.03) {
      String msg =
          "initial used memory = "
              + initialUsedMemory
              + ", final used memory = "
              + finalUsedMemory
              + ", ratio = "
              + memoryRatio;
      throw new Error(msg);
    }
  }
}
