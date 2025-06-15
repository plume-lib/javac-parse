package org.plumelib.javacparse;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import org.junit.jupiter.api.Test;

class JavacParseTest {
  @Test
  void javacParseTest() {
    assertThrows(IOException.class, () -> JavacParse.parseJavaFile("foo bar"));

    JavacParseResult r1 = JavacParse.parseJavaCode("class MyClass { void m() {} }");
    assertFalse(r1.hasParseError());
    JavacParseResult r2 = JavacParse.parseJavaCode("class SyntaxError { void () {} }");
    assertTrue(r2.hasParseError());
  }
}
