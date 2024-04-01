package org.plumelib.javacparse;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import org.junit.jupiter.api.Test;

class JavacParseTest {
  @SuppressWarnings("nullness:argument") // JUnit method
  @Test
  void javacParseTest() {
    assertThrows(IOException.class, () -> JavacParse.parseJavaFile("foo bar"));

    assertNotNull(JavacParse.parseJavaCode("class MyClass { void m() {} }"));
    assertNull(JavacParse.parseJavaCode("class SyntaxError { void () {} }"));
  }
}
