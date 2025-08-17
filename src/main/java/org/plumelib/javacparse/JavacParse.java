package org.plumelib.javacparse;

import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.parser.JavacParser;
import com.sun.tools.javac.parser.ParserFactory;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Log;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.tools.DiagnosticCollector;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;

/**
 * This class contains static methods {@link #parseJavaFile} and {@link #parseJavaCode} that parse
 * Java code into a JCCompilationUnit.
 *
 * <p>Internally, this class calls the javac parser from the JDK.
 */
public final class JavacParse {

  /** Do not instantiate. */
  private JavacParse() {
    throw new Error("Do not instantiate.");
  }

  /**
   * Parse a Java file.
   *
   * @param filename the file to parse
   * @return a (parsed) compilation unit
   * @throws IOException if there is trouble reading the file
   */
  public static JavacParseResult<JCCompilationUnit> parseFile(String filename) throws IOException {
    return parseJavaFileObject(new FileJavaFileObject(filename));
  }

  /**
   * Parse a Java file's contents.
   *
   * @param javaCode the contents of a Java file
   * @return a (parsed) compilation unit
   */
  public static JavacParseResult<CompilationUnit> parseCompilationUnit(String javaCode) {
    try {
      return parseJavaFileObject(new StringJavaFileObject(javaCode));
    } catch (IOException e) {
      throw new Error("This can't happen", e);
    }
  }

  /**
   * Parses the given Java class.
   *
   * @param expressionSource the string representation of a Java expression
   * @return the parsed expression
   */
  public static JavacParseResult<ClassTree> parseClass(String classSource) {
    JavacParseResult<CompilationUnit> parsedFile = parseJavaFileContents(classSource);

    // TODO
  }

  /**
   * Parses the given Java method.
   *
   * @param expressionSource the string representation of a Java expression
   * @return the parsed expression
   */
  public static JavacParseResult<MethodTree> parseMethod(String methodSource) {
    // TODO
  }

  /**
   * Parses the given Java expression string, such as "foo.bar()" or "1 + 2"
   *
   * @param expressionSource the string representation of a Java expression
   * @return the parsed expression
   */
  public static JavacParseResult<ExpressionTree> parseExpression(String expressionSource) {

    String dummySource = "class ParseExpression { Object expression = " + expressionSource + "; }";

    JavaFileObject fileObject =
        new SimpleJavaFileObject(
            URI.create("string:///ParseExpression.java"), JavaFileObject.Kind.SOURCE) {
          @Override
          public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return dummySource;
          }
        };

    DiagnosticCollector<JavaFileObject> diags = new DiagnosticCollector<>();

    // Prepare the file manager and task
    try {
      JavacTask task =
          (JavacTask)
              javaCompiler.getTask(
                  null,
                  fileManager,
                  diags,
                  Collections.emptyList(),
                  null,
                  Collections.singletonList(fileObject));

      // Parse the source and extract the CompilationUnit
      CompilationUnitTree cu = task.parse().iterator().next();

      for (Diagnostic<? extends JavaFileObject> d : diags.getDiagnostics()) {
        if (d.getKind() == Diagnostic.Kind.ERROR) {
          throw new RuntimeException("Expression is not valid: " + d.getMessage(null));
        }
      }

      // Get the first member (the dummy field) from the ClassTree and cast to VariableTree
      ClassTree classTree = (ClassTree) cu.getTypeDecls().get(0);
      VariableTree varTree = (VariableTree) classTree.getMembers().get(0);

      ExpressionTree expr = varTree.getInitializer();
      if (expr == null) {
        throw new RuntimeException("Expression not found in AST.");
      }

      return expr;

    } catch (IOException | IndexOutOfBoundsException | ClassCastException e) {
      throw new RuntimeException("Expression parsing failed", e);
    }
  }

  /**
   * Parse the contents of a JavaFileObject.
   *
   * @param source a JavaFileObject
   * @return a compilation unit and the parse errors encountered in it
   * @throws IOException if there is trouble reading the file
   */
  public static JavacParseResult<JCCompilationUnit> parseJavaFileObject(JavaFileObject source)
      throws IOException {
    // The documentation of Context says "a single Context is used for each invocation of the
    // compiler".  Re-using the Context causes an error "duplicate context value" in the compiler.
    // A Context is just a map.
    Context context = new Context();

    DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
    context.put(DiagnosticListener.class, diagnostics);

    // Needed to avoid "this.fileManager is null" error in com.sun.tools.javac.comp.Modules.<init>.
    try (@SuppressWarnings("UnusedVariable") // `new JavacFileManager` sets a mapping in `context`.
        JavacFileManager fileManagerUnused =
            new JavacFileManager(context, true, StandardCharsets.UTF_8)) {

      Log.instance(context).useSource(source);
      ParserFactory parserFactory = ParserFactory.instance(context);
      JavacParser parser = parserFactory.newParser(source.getCharContent(false), true, true, true);
      JCCompilationUnit cu = parser.parseCompilationUnit();
      cu.sourcefile = source;
      return new JavacParseResult(cu, diagnostics.getDiagnostics());
    }
  }
}
