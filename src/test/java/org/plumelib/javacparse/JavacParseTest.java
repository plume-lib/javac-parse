package org.plumelib.javacparse;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.StringJoiner;
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
    double initialUsedMemory = (double) SystemPlume.usedMemory(true);
    for (int i = 0; i < 10; i++) { // increase limit for a more thorough test.
      // Each execution of this loop takes approximately 1 second.
      for (int j = 0; j < 1000; j++) {
        // Make a new String each time through the loop.
        StringJoiner sj = new StringJoiner(System.lineSeparator());
        for (int k = 0; k < 1000; k++) {
          sj.add("class MyClass" + k + " { void m() {} }");
        }
        JavacParse.parseJavaCode(sj.toString());
      }
      String msg = SystemPlume.gcUsageMessage(.3, 10);
      if (msg != null) {
        System.out.println(msg);
      }
    }
    double finalUsedMemory = (double) SystemPlume.usedMemory(true);
    double memoryRatio = finalUsedMemory / initialUsedMemory;
    if (memoryRatio > 1.02) {
      String msg =
          "initial used memory = "
              + initialUsedMemory
              + ", final used memory = "
              + finalUsedMemory
              + ", ratio = "
              + memoryRatio;
      System.out.println(msg);
      System.err.println(msg);
      throw new Error(msg);
    }
  }
}
