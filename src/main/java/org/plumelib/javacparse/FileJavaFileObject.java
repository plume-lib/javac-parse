package org.plumelib.javacparse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;

/** A JavaFileObject constructed from a file. */
class FileJavaFileObject extends SimpleJavaFileObject {

  /** The contents of the file. */
  private final String javaCode;

  /**
   * Creates a FileJavaFileObject for the given file.
   *
   * @param filename the file name of a Java source file
   * @throws IOException if there is trouble reading the file
   */
  public FileJavaFileObject(String filename) throws IOException {
    this(Path.of(filename));
  }

  /**
   * Creates a FileJavaFileObject for the given path.
   *
   * @param pathname the path name of a Java source file
   * @throws IOException if there is trouble reading the file
   */
  public FileJavaFileObject(Path pathname) throws IOException {
    super(pathname.toUri(), JavaFileObject.Kind.SOURCE);
    javaCode = Files.readString(pathname, StandardCharsets.UTF_8);
  }

  @Override
  public CharSequence getCharContent(boolean ignoreEncodingErrors) {
    return javaCode;
  }
}
