package com.TechHurts.CodeFile;

import com.github.javaparser.*;
import com.github.javaparser.ast.*;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.resolution.declarations.*;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import java.util.*;
import org.json.*;

/**
 * Embedded document service: Java AST/resolution, explicitly lexical fallback for other languages.
 */
final class LocalLanguageService {
  private final String source, uri;
  private CompilationUnit javaTree;

  LocalLanguageService(String name, String language, String source, String uri) {
    this.source = source;
    this.uri = uri;
    if (!language.equals("java") && !name.endsWith(".java")) return;
    ParserConfiguration config =
        new ParserConfiguration()
            .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17)
            .setSymbolResolver(new JavaSymbolSolver(new ReflectionTypeSolver()));
    javaTree = new JavaParser(config).parse(source).getResult().orElse(null);
  }

  String description() {
    return javaTree == null
        ? "On-device navigation · lexical index"
        : "On-device Java service · AST and symbol resolution";
  }

  Object handle(String method, JSONObject params) throws Exception {
    if (method.endsWith("documentSymbol")) return symbols();
    JSONObject position = params.optJSONObject("position");
    int line = position == null ? 0 : position.optInt("line"),
        column = position == null ? 0 : position.optInt("character");
    int offset = MainActivity.offset(source, NativeLsp.object("line", line, "character", column));
    String word = SourceTools.wordAt(source, offset);
    Node declaration = javaDeclaration(line, column);
    if (method.endsWith("hover"))
      return NativeLsp.object(
          "contents",
          NativeLsp.object(
              "kind",
              "plaintext",
              "value",
              declaration == null ? description() + "\n" + word : declaration.toString()));
    if (method.endsWith("definition")) {
      JSONArray found = new JSONArray();
      if (declaration != null) return found.put(location(declaration));
      for (SourceTools.Symbol symbol : SourceTools.symbols(source))
        if (symbol.name.equals(word)) found.put(location(symbol));
      return found;
    }
    if (method.endsWith("references")) {
      JSONArray found = new JSONArray();
      if (javaTree != null && declaration != null) {
        for (SimpleName name : javaTree.findAll(SimpleName.class)) {
          if (!name.asString().equals(word)) continue;
          Position start = name.getBegin().orElse(null);
          if (start == null) continue;
          Node target = javaDeclaration(start.line - 1, start.column - 1);
          if (target != null && target == declaration) found.put(location(name));
        }
        return found;
      }
      for (SourceTools.Symbol symbol : SourceTools.references(source, word))
        found.put(location(symbol));
      return found;
    }
    throw new IllegalArgumentException("Unsupported local language operation: " + method);
  }

  private JSONArray symbols() {
    JSONArray result = new JSONArray();
    if (javaTree == null) {
      for (SourceTools.Symbol symbol : SourceTools.symbols(source))
        result.put(NativeLsp.object("name", symbol.name, "kind", 13, "location", location(symbol)));
      return result;
    }
    for (Node node : javaTree.findAll(Node.class)) {
      String name = declarationName(node);
      if (name == null) continue;
      result.put(
          NativeLsp.object(
              "name",
              name,
              "kind",
              node instanceof TypeDeclaration ? 5 : node instanceof MethodDeclaration ? 6 : 13,
              "location",
              location(node)));
    }
    return result;
  }

  private String declarationName(Node node) {
    if (node instanceof TypeDeclaration) return ((TypeDeclaration<?>) node).getNameAsString();
    if (node instanceof MethodDeclaration) return ((MethodDeclaration) node).getNameAsString();
    if (node instanceof ConstructorDeclaration)
      return ((ConstructorDeclaration) node).getNameAsString();
    if (node instanceof VariableDeclarator) return ((VariableDeclarator) node).getNameAsString();
    if (node instanceof Parameter) return ((Parameter) node).getNameAsString();
    return null;
  }

  private Node javaDeclaration(int line, int column) {
    if (javaTree == null) return null;
    Position point = new Position(line + 1, column + 1);
    for (SimpleName name : javaTree.findAll(SimpleName.class)) {
      if (!name.getRange().map(range -> range.contains(point)).orElse(false)) continue;
      Node parent = name.getParentNode().orElse(null);
      if (parent == null) return null;
      if (declarationName(parent) != null) return parent;
      try {
        ResolvedDeclaration resolved = null;
        if (parent instanceof NameExpr) resolved = ((NameExpr) parent).resolve();
        else if (parent instanceof FieldAccessExpr) resolved = ((FieldAccessExpr) parent).resolve();
        else if (parent instanceof MethodCallExpr) resolved = ((MethodCallExpr) parent).resolve();
        return resolved == null ? null : resolved.toAst().orElse(null);
      } catch (RuntimeException unresolved) {
        return null;
      }
    }
    return null;
  }

  private JSONObject location(Node node) {
    Range range = node.getRange().orElse(new Range(new Position(1, 1), new Position(1, 1)));
    return NativeLsp.object(
        "uri",
        uri,
        "range",
        NativeLsp.object(
            "start",
            NativeLsp.object("line", range.begin.line - 1, "character", range.begin.column - 1),
            "end",
            NativeLsp.object("line", range.end.line - 1, "character", range.end.column)));
  }

  private JSONObject location(SourceTools.Symbol symbol) {
    return NativeLsp.object(
        "uri",
        uri,
        "range",
        NativeLsp.object(
            "start",
            NativeLsp.object("line", symbol.line, "character", symbol.column),
            "end",
            NativeLsp.object(
                "line", symbol.line, "character", symbol.column + symbol.name.length())));
  }
}
