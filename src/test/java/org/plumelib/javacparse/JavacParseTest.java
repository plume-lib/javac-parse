package org.plumelib.javacparse;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.plumelib.util.SystemPlume;

class JavacParseTest {
  @Test
  void javacParseTest() {
    assertThrows(IOException.class, () -> JavacParse.parseJavaFile("foo bar"));

    JavacParseResult r1 = JavacParse.parseJavaCode("class MyClass { void m() {} }");
    assertFalse(r1.hasParseError());
    JavacParseResult r2 = JavacParse.parseJavaCode("class SyntaxError { void () {} }");
    assertTrue(r2.hasParseError());
  }

  @Test
  void memoryTest() {
    long initialUsedMemory = SystemPlume.usedMemory(true);
    for (int i = 0; i < 1_000_000; i++) {
      JavacParse.parseJavaCode("class MyClass { void m() {} }");
    }
    long finalUsedMemory = SystemPlume.usedMemory(true);
    assertTrue((float) finalUsedMemory / (float) initialUsedMemory < 1.02);
  }
}
