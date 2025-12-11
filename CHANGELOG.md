# javac-parse change log

## 0.9.0 (2025-12-10)

- `JavacParse`:
  - Renamed `parseJavaFile()` to `parseFile();`.
  - Renamed `parseJavaCode()` to `parseCompilationUnit()`.
  - Renamed `parseJavaFileObject()` to `parseCompilationUnit()`.
  - New method `parseTypeDeclaration()`.
  - New method `parseExpression()`.
  - New method `parseTypeUse()`.
- `JavacParseResult`
  - Made `JavacParseResult` a generic class.
  - New method `getParseErrorMessages()`.

## 0.2.0 (2024-06-15)

- `parse*` methods return a `JavacParseResult` rather than a possibly-null `CompilationUnit`.

## 0.1.0 (2024-04-19)

- Changed package from `org.plumelib.mergetools.javacparse` to `org.plumelib.javacparse`.

## 0.0.1 (2024-03-26)

- Define class `JavacParse`.
