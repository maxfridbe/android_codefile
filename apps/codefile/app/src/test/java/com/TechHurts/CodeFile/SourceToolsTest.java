package com.TechHurts.CodeFile;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
public class SourceToolsTest {
 @Test public void indentationPreservesStringsCommentsAndLineCount(){String input="class C {\nString x = \"}\";\n/* {\n untouched */\n}\n";String out=SourceTools.indent(input,2,false);assertTrue(out.contains("  String x = \"}\";"));assertTrue(out.contains(" untouched */"));assertEquals(input.split("\n",-1).length,out.split("\n",-1).length);assertEquals(input.replaceAll("\\s", ""),out.replaceAll("\\s", ""));}
 @Test public void jsonFormatterDoesNotRewriteStringWhitespace(){String source="{\"s\":\"a b\\\"c\",\"n\":[1,2]}";String result=SourceTools.jsonFormat(source,2,false);assertTrue(result.contains("\"a b\\\"c\""));assertTrue(result.contains("\n"));assertArrayEquals(new String[]{"1","1","1","1","1","1","1","~1"},SourceTools.sourceLines(source,result));}
 @Test public void sourceReferencesRemainExactForIndentOnly(){assertArrayEquals(new String[]{"1","2","3"},SourceTools.sourceLines("a{\nb\n}","a{\n  b\n}"));}
 @Test public void snippetFencesCannotBeEscaped(){String snippet=SourceTools.snippet("x\n```\ny","a.md",2,2,null,true,true);assertTrue(snippet.contains("````\n2 | ```\n````"));}
 @Test public void localSymbolsAndReferencesAreExplicitlyLexical(){List<SourceTools.Symbol> symbols=SourceTools.symbols("// class Fake\nclass Real {\n void hello() {\n }\n}");assertEquals("Real",symbols.get(0).name);assertEquals(1,symbols.get(0).line);assertTrue(symbols.stream().anyMatch(s->s.name.equals("hello")));assertEquals(2,SourceTools.references("name names\nname","name").size());}
 @Test public void nativeDiffProducesPatchAndAlignedRows(){DiffModel diff=new DiffModel("old.java","int x=1;\n","new.java","int x=2;\nhello();\n");assertEquals(2,diff.added);assertEquals(1,diff.removed);assertTrue(diff.patch.contains("-int x=1;"));assertTrue(diff.patch.contains("+int x=2;"));assertEquals(1,diff.rows.get(0).before);}
 @Test public void invalidRangeCannotShareAnotherRegion(){assertThrows(IllegalArgumentException.class,()->SourceTools.snippet("a\nb","x",0,2,null,true,true));}
}
