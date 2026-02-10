package org.plumelib.javacparse;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberSelectTree;
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

    // Expression
    String e1 = "1 + 2";
    String e2 = "foo.m()";
    String e3 = "foo.m(1 + 2)";
    String e4 = "x instanceof Object";
    String e5 = "x instanceof @Nullable Object";
    String e6 = "String.class";
    String e7 = "java.lang.String.class";
    String e8 = "\"hello\"";

    // Invalid code.
    String invalid1 = "Hello this is nonsense.";
    String invalid2 = "class SyntaxError { void () {} }";
    String invalid3 = si1 + invalid2;
    String invalid4 = si1 + "+";
    String invalid5 = invalid2 + invalid1;
    String invalid6 = scu1 + invalid1;
    String invalid7 = "1 +";

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

    assertTrue(JavacParse.parseCompilationUnit(e1).hasParseError());
    assertTrue(JavacParse.parseCompilationUnit(e2).hasParseError());
    assertTrue(JavacParse.parseCompilationUnit(e3).hasParseError());
    assertTrue(JavacParse.parseCompilationUnit(e4).hasParseError());
    assertTrue(JavacParse.parseCompilationUnit(e5).hasParseError());
    assertTrue(JavacParse.parseCompilationUnit(e6).hasParseError());
    assertTrue(JavacParse.parseCompilationUnit(e7).hasParseError());
    assertTrue(JavacParse.parseCompilationUnit(e8).hasParseError());
    assertTrue(JavacParse.parseCompilationUnit(invalid1).hasParseError());
    assertTrue(JavacParse.parseCompilationUnit(invalid2).hasParseError());
    assertTrue(JavacParse.parseCompilationUnit(invalid3).hasParseError());
    assertTrue(JavacParse.parseCompilationUnit(invalid4).hasParseError());
    assertTrue(JavacParse.parseCompilationUnit(invalid5).hasParseError());
    assertTrue(JavacParse.parseCompilationUnit(invalid6).hasParseError());
    assertTrue(JavacParse.parseCompilationUnit(invalid7).hasParseError());

    // Class
    assertNoParseError(JavacParse.parseTypeDeclaration(sc1), sc1);
    assertNoParseError(JavacParse.parseTypeDeclaration(scu9), scu9);

    assertIllegalArgument(() -> JavacParse.parseTypeDeclaration(scu1), scu1);
    assertIllegalArgument(() -> JavacParse.parseTypeDeclaration(scu2), scu2);
    assertIllegalArgument(() -> JavacParse.parseTypeDeclaration(scu3), scu3);
    assertIllegalArgument(() -> JavacParse.parseTypeDeclaration(scu4), scu4);
    assertIllegalArgument(() -> JavacParse.parseTypeDeclaration(scu5), scu5);
    assertIllegalArgument(() -> JavacParse.parseTypeDeclaration(scu6), scu6);
    assertIllegalArgument(() -> JavacParse.parseTypeDeclaration(scu7), scu7);
    assertIllegalArgument(() -> JavacParse.parseTypeDeclaration(scu8), scu8);
    assertIllegalArgument(() -> JavacParse.parseTypeDeclaration(scu10), scu10);
    assertIllegalArgument(() -> JavacParse.parseTypeDeclaration(scu11), scu11);
    assertIllegalArgument(() -> JavacParse.parseTypeDeclaration(scu12), scu12);
    assertIllegalArgument(() -> JavacParse.parseTypeDeclaration(e1), e1);
    assertIllegalArgument(() -> JavacParse.parseTypeDeclaration(e2), e2);
    assertIllegalArgument(() -> JavacParse.parseTypeDeclaration(e3), e3);
    assertIllegalArgument(() -> JavacParse.parseTypeDeclaration(e4), e4);
    assertIllegalArgument(() -> JavacParse.parseTypeDeclaration(e5), e5);
    assertIllegalArgument(() -> JavacParse.parseTypeDeclaration(e6), e6);
    assertIllegalArgument(() -> JavacParse.parseTypeDeclaration(e7), e7);
    assertIllegalArgument(() -> JavacParse.parseTypeDeclaration(e8), e8);
    assertIllegalArgument(() -> JavacParse.parseTypeDeclaration(invalid1), invalid1);
    assertIllegalArgument(() -> JavacParse.parseTypeDeclaration(invalid2), invalid2);
    assertIllegalArgument(() -> JavacParse.parseTypeDeclaration(invalid3), invalid3);
    assertIllegalArgument(() -> JavacParse.parseTypeDeclaration(invalid4), invalid4);
    assertIllegalArgument(() -> JavacParse.parseTypeDeclaration(invalid5), invalid5);
    assertIllegalArgument(() -> JavacParse.parseTypeDeclaration(invalid6), invalid6);
    assertIllegalArgument(() -> JavacParse.parseTypeDeclaration(invalid7), invalid7);

    // Expression
    assertNoParseError(JavacParse.parseExpression(e1), e1);
    assertNoParseError(JavacParse.parseExpression(e2), e2);
    assertNoParseError(JavacParse.parseExpression(e3), e3);
    assertNoParseError(JavacParse.parseExpression(e4), e4);
    assertNoParseError(JavacParse.parseExpression(e5), e5);
    assertNoParseError(JavacParse.parseExpression(e6), e6);
    assertNoParseError(JavacParse.parseExpression(e7), e7);
    assertNoParseError(JavacParse.parseExpression(e8), e8);
    JavacParseResult<ExpressionTree> e7jpr = JavacParse.parseExpression(e7);
    assertTrue(e7jpr.getTree() instanceof MemberSelectTree);
    JavacParseResult<ExpressionTree> e8jpr = JavacParse.parseExpression(e8);
    assertTrue(e8jpr.getTree() instanceof LiteralTree);

    assertIllegalArgument(() -> JavacParse.parseExpression(scu1), scu1);
    assertIllegalArgument(() -> JavacParse.parseExpression(scu2), scu2);
    assertIllegalArgument(() -> JavacParse.parseExpression(scu3), scu3);
    assertIllegalArgument(() -> JavacParse.parseExpression(scu4), scu4);
    assertIllegalArgument(() -> JavacParse.parseExpression(scu5), scu5);
    assertIllegalArgument(() -> JavacParse.parseExpression(scu6), scu6);
    assertIllegalArgument(() -> JavacParse.parseExpression(scu7), scu7);
    assertIllegalArgument(() -> JavacParse.parseExpression(scu8), scu8);
    assertIllegalArgument(() -> JavacParse.parseExpression(scu9), scu9);
    assertIllegalArgument(() -> JavacParse.parseExpression(scu10), scu10);
    assertIllegalArgument(() -> JavacParse.parseExpression(scu11), scu11);
    assertIllegalArgument(() -> JavacParse.parseExpression(scu12), scu12);
    assertIllegalArgument(() -> JavacParse.parseExpression(invalid1), invalid1);
    assertIllegalArgument(() -> JavacParse.parseExpression(invalid2), invalid2);
    assertIllegalArgument(() -> JavacParse.parseExpression(invalid3), invalid3);
    assertIllegalArgument(() -> JavacParse.parseExpression(invalid4), invalid4);
    assertIllegalArgument(() -> JavacParse.parseExpression(invalid5), invalid5);
    assertIllegalArgument(() -> JavacParse.parseExpression(invalid6), invalid6);
    assertIllegalArgument(() -> JavacParse.parseExpression(invalid7), invalid7);
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
    long initialUsedMemory = (double) SystemPlume.usedMemory(true);
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
    long finalUsedMemory = (double) SystemPlume.usedMemory(true);
    double memoryRatio = (double) finalUsedMemory / (double) initialUsedMemory;
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
