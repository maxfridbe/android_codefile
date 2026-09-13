package com.TechHurts.CodeFile;

import android.util.JsonReader;
import android.util.JsonToken;
import java.io.*;
import java.util.*;

/** Strict streaming parser preserving numeric spellings and object entry order. */
final class JsonTree {
  static final class Node {
    final String key, path, type, value;
    final List<Node> children = new ArrayList<>();
    boolean expanded;

    Node(String key, String path, String type, String value) {
      this.key = key;
      this.path = path;
      this.type = type;
      this.value = value;
    }

    boolean container() {
      return type.equals("object") || type.equals("array");
    }
  }

  private int count;

  static Node parse(String source) throws IOException {
    if (source.length() > 2 * 1024 * 1024)
      throw new IOException("JSON tree supports files up to 2 MiB");
    try (JsonReader reader = new JsonReader(new StringReader("[" + source + "]"))) {
      reader.setLenient(false);
      reader.beginArray();
      Node root = new JsonTree().read(reader, "$", "$", 0);
      if (reader.hasNext()) throw new IOException("Expected one JSON value");
      reader.endArray();
      if (reader.peek() != JsonToken.END_DOCUMENT) throw new IOException("Trailing JSON content");
      root.expanded = true;
      return root;
    }
  }

  private Node read(JsonReader reader, String key, String path, int depth) throws IOException {
    if (depth > 128 || ++count > 50000)
      throw new IOException("JSON tree exceeds 128 levels or 50,000 values");
    JsonToken token = reader.peek();
    if (token == JsonToken.BEGIN_OBJECT || token == JsonToken.BEGIN_ARRAY)
      return container(reader, key, path, depth, token == JsonToken.BEGIN_OBJECT);
    if (token == JsonToken.STRING) return new Node(key, path, "string", reader.nextString());
    if (token == JsonToken.NUMBER) return new Node(key, path, "number", reader.nextString());
    if (token == JsonToken.BOOLEAN)
      return new Node(key, path, "boolean", String.valueOf(reader.nextBoolean()));
    if (token == JsonToken.NULL) {
      reader.nextNull();
      return new Node(key, path, "null", "null");
    }
    throw new IOException("Expected a JSON value at " + path);
  }

  private Node container(JsonReader reader, String key, String path, int depth, boolean object)
      throws IOException {
    Node node = new Node(key, path, object ? "object" : "array", "");
    if (object) reader.beginObject();
    else reader.beginArray();
    int index = 0;
    while (reader.hasNext()) {
      String child = object ? reader.nextName() : String.valueOf(index++);
      String childPath =
          object ? path + "[" + org.json.JSONObject.quote(child) + "]" : path + "[" + child + "]";
      node.children.add(read(reader, child, childPath, depth + 1));
    }
    if (object) reader.endObject();
    else reader.endArray();
    return node;
  }
}
