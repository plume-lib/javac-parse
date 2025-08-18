package org.plumelib.javacparse;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.EmptyStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.parser.JavacParser;
import com.sun.tools.javac.parser.ParserFactory;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Log;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.tools.DiagnosticCollector;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;

/**
 * This class contains static methods that parse Java code.
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
   * @return a (parsed) compilation unit, which may include parse errors
   * @throws IOException if there is trouble reading the file
   */
  public static JavacParseResult<CompilationUnitTree> parseFile(String filename)
      throws IOException {
    return parseCompilationUnit(new FileJavaFileObject(filename));
  }

  /**
   * Parse a Java file's contents.
   *
   * @param javaCode the contents of a Java file
   * @return a (parsed) compilation unit, which may include parse errors
   */
  public static JavacParseResult<CompilationUnitTree> parseCompilationUnit(String javaCode) {
    try {
      return parseCompilationUnit(new StringJavaFileObject(javaCode));
    } catch (IOException e) {
      throw new Error("This can't happen", e);
    }
  }

  /**
   * Parses the given Java type declaration (class, interface, enum, record, etc.).
   *
   * @param classSource the string representation of a Java type declaration
   * @return the parsed type declaration
   */
  public static JavacParseResult<ClassTree> parseTypeDeclaration(String classSource) {
    JavacParseResult<CompilationUnitTree> parsedCU = parseCompilationUnit(classSource);

    CompilationUnitTree cu = parsedCU.getTree();

    if (!cu.getImports().isEmpty()) {
      throw new IllegalArgumentException(
          "Type declaration source code has imports: " + classSource);
    }
    if (cu.getModule() != null) {
      throw new IllegalArgumentException(
          "Type declaration source code has a module declaration: " + classSource);
    }
    if (cu.getPackage() != null) {
      throw new IllegalArgumentException(
          "Type declaration source code has a package declaration: " + classSource);
    }

    List<? extends Tree> decls = cu.getTypeDecls();
    for (Tree decl : decls) {
      if (decl instanceof EmptyStatementTree) {
        throw new IllegalArgumentException(
            "Type declaration source code contains a top-level `;`: " + classSource);
      }
    }
    if (decls.size() != 1) {
      throw new IllegalArgumentException(
          String.format(
              "Type declaration source code has %d top-level forms, not 1: %s",
              decls.size(), classSource));
    }

    Tree decl = decls.get(0);
    if (decl instanceof ClassTree) {
      return new JavacParseResult<ClassTree>((ClassTree) decl, parsedCU.getDiagnostics());
    } else {
      throw new IllegalArgumentException(
          "source code should be a type declaration but is "
              + decl.getClass().getSimpleName()
              + ":"
              + classSource);
    }
  }

  /**
   * Parses the given Java method or annotation type element.
   *
   * @param methodSource the string representation of a Java expression
   * @return the parsed expression
   */
  public static JavacParseResult<MethodTree> parseMethod(String methodSource) {
    // TODO
    throw new Error("to implement");
  }

  /**
   * Parses the given Java expression string, such as "foo.bar()" or "1 + 2"
   *
   * @param expressionSource the string representation of a Java expression
   * @return the parsed expression
   */
  public static JavacParseResult<ExpressionTree> parseExpression(String expressionSource) {
    try {
      return parseExpression(new StringJavaFileObject(expressionSource));
    } catch (IOException e) {
      throw new Error("This can't happen", e);
    }
  }

  /*
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

      // Parse the source and extract the CompilationUnitTree
      CompilationUnitTreeTree cu = task.parse().iterator().next();

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
  */

  /**
   * Parses the given Java type declaration (class, interface, enum, record, etc.).
   *
   * @param classSource the string representation of a Java type declaration
   * @return the parsed type declaration
   */
  public static JavacParseResult<ExpressionTree> parseTypeUse(String classSource) {
    try {
      return parseTypeUse(new StringJavaFileObject(classSource));
    } catch (IOException e) {
      throw new Error("This can't happen", e);
    }
  }

  // ///////////////////////////////////////////////////////////////////////////
  // Low-level routines
  //

  // All the routines below this point are copies of one another.

  // Implementation notes:
  // 1. The documentation of Context says "a single Context is used for each invocation of the
  //    compiler".  Re-using the Context causes an error "duplicate context value" in the compiler.
  //    A Context is just a map.
  // 2. Calling `new JavacFileManager` sets a mapping in `context`.  It is necessary to avoid
  //    "this.fileManager is null" error in com.sun.tools.javac.comp.Modules.<init>.

  /**
   * Parse the contents of a JavaFileObject.
   *
   * @param source a JavaFileObject
   * @return a (parsed) compilation unit, which may include parse errors
   * @throws IOException if there is trouble reading the file
   */
  @SuppressWarnings("try") // `fileManagerUnused` is not used
  public static JavacParseResult<CompilationUnitTree> parseCompilationUnit(JavaFileObject source)
      throws IOException {
    Context context = new Context();
    DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
    context.put(DiagnosticListener.class, diagnostics);

    try (@SuppressWarnings("UnusedVariable") // `new JavacFileManager` sets a mapping in `context`.
        JavacFileManager fileManagerUnused =
            new JavacFileManager(context, true, StandardCharsets.UTF_8)) {

      Log.instance(context).useSource(source);
      ParserFactory parserFactory = ParserFactory.instance(context);
      JavacParser parser = parserFactory.newParser(source.getCharContent(false), true, true, true);
      CompilationUnitTree cu = parser.parseCompilationUnit();
      ((JCCompilationUnit) cu).sourcefile = source;
      return new JavacParseResult<>(cu, diagnostics.getDiagnostics());
    }
  }

  /**
   * Parse a Java expression
   *
   * @param source a JavaFileObject
   * @return a (parsed) expression, possibly an ErroneousTree
   * @throws IOException if there is trouble reading the file
   */
  @SuppressWarnings("try") // `fileManagerUnused` is not used
  public static JavacParseResult<ExpressionTree> parseExpression(JavaFileObject source)
      throws IOException {
    Context context = new Context();
    DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
    context.put(DiagnosticListener.class, diagnostics);

    try (@SuppressWarnings("UnusedVariable") // `new JavacFileManager` sets a mapping in `context`.
        JavacFileManager fileManagerUnused =
            new JavacFileManager(context, true, StandardCharsets.UTF_8)) {

      Log.instance(context).useSource(source);
      ParserFactory parserFactory = ParserFactory.instance(context);
      JavacParser parser = parserFactory.newParser(source.getCharContent(false), true, true, true);
      ExpressionTree eTree = parser.parseExpression();
      return new JavacParseResult<ExpressionTree>(eTree, diagnostics.getDiagnostics());
    }
  }

  /**
   * Parse a type use.
   *
   * @param source a JavaFileObject
   * @return a (parsed) type use, possibly an ErroneousTree
   * @throws IOException if there is trouble reading the file
   */
  @SuppressWarnings("try") // `fileManagerUnused` is not used
  public static JavacParseResult<ExpressionTree> parseTypeUse(JavaFileObject source)
      throws IOException {
    Context context = new Context();
    DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
    context.put(DiagnosticListener.class, diagnostics);

    try (@SuppressWarnings("UnusedVariable") // `new JavacFileManager` sets a mapping in `context`.
        JavacFileManager fileManagerUnused =
            new JavacFileManager(context, true, StandardCharsets.UTF_8)) {

      Log.instance(context).useSource(source);
      ParserFactory parserFactory = ParserFactory.instance(context);
      JavacParser parser = parserFactory.newParser(source.getCharContent(false), true, true, true);
      ExpressionTree eTree = parser.parseType();
      return new JavacParseResult<ExpressionTree>(eTree, diagnostics.getDiagnostics());
    }
  }
}
