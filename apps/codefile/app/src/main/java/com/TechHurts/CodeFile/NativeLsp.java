package com.TechHurts.CodeFile;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import okhttp3.*;
import org.json.*;

/** Native JSON-RPC over WebSocket. No browser or script runtime. */
final class NativeLsp implements AutoCloseable {
  private final OkHttpClient client =
      new OkHttpClient.Builder()
          .connectTimeout(15, TimeUnit.SECONDS)
          .readTimeout(0, TimeUnit.SECONDS)
          .build();
  private final ConcurrentHashMap<Integer, CompletableFuture<JSONObject>> pending =
      new ConcurrentHashMap<>();
  private final AtomicInteger sequence = new AtomicInteger();
  private WebSocket socket;
  private String uri;
  private volatile boolean closed;
  JSONObject capabilities = new JSONObject();

  static JSONObject object(Object... fields) {
    JSONObject o = new JSONObject();
    try {
      for (int i = 0; i < fields.length; i += 2) o.put((String) fields[i], fields[i + 1]);
    } catch (JSONException e) {
      throw new IllegalArgumentException(e);
    }
    return o;
  }

  void connect(String endpoint, String root, String documentUri, String language, String text)
      throws Exception {
    if (!endpoint.startsWith("wss://"))
      throw new IOException("Use a wss:// JSON-RPC language-server gateway");
    CompletableFuture<Void> connected = new CompletableFuture<>();
    socket =
        client.newWebSocket(
            new Request.Builder().url(endpoint).build(),
            new WebSocketListener() {
              @Override
              public void onOpen(WebSocket ws, Response response) {
                if (closed) {
                  ws.close(1000, "Closed");
                  connected.completeExceptionally(new IOException("Closed"));
                } else connected.complete(null);
              }

              @Override
              public void onMessage(WebSocket ws, String text) {
                try {
                  JSONObject m = new JSONObject(text);
                  if (m.has("method")) {
                    if (m.has("id"))
                      ws.send(
                          object(
                                  "jsonrpc",
                                  "2.0",
                                  "id",
                                  m.get("id"),
                                  "error",
                                  object("code", -32601, "message", "Unsupported client request"))
                              .toString());
                    return;
                  }
                  CompletableFuture<JSONObject> f = pending.remove(m.optInt("id", -1));
                  if (f != null) f.complete(m);
                } catch (Exception ignored) {
                }
              }

              @Override
              public void onFailure(WebSocket ws, Throwable t, Response response) {
                connected.completeExceptionally(t);
                fail(t);
              }

              @Override
              public void onClosed(WebSocket ws, int code, String reason) {
                fail(new IOException("Language server disconnected"));
              }
            });
    connected.get(20, TimeUnit.SECONDS);
    JSONObject result =
        request(
                "initialize",
                object(
                    "processId",
                    JSONObject.NULL,
                    "rootUri",
                    root.isEmpty() ? JSONObject.NULL : root,
                    "capabilities",
                    object(
                        "general",
                        object("positionEncodings", new JSONArray().put("utf-16")),
                        "textDocument",
                        object(
                            "documentSymbol", object("hierarchicalDocumentSymbolSupport", true))),
                    "clientInfo",
                    object("name", "CodeFile Native")))
            .optJSONObject("result");
    capabilities = result == null ? new JSONObject() : result.optJSONObject("capabilities");
    if (capabilities == null) capabilities = new JSONObject();
    if (!capabilities.optString("positionEncoding", "utf-16").equals("utf-16"))
      throw new IOException("Language server must support UTF-16 positions");
    notify("initialized", new JSONObject());
    uri = documentUri;
    notify(
        "textDocument/didOpen",
        object(
            "textDocument",
            object("uri", uri, "languageId", language, "version", 1, "text", text)));
  }

  String uri() {
    return uri;
  }

  JSONObject request(String method, JSONObject params) throws Exception {
    int id = sequence.incrementAndGet();
    CompletableFuture<JSONObject> f = new CompletableFuture<>();
    pending.put(id, f);
    try {
      if (closed
          || socket == null
          || !socket.send(
              object("jsonrpc", "2.0", "id", id, "method", method, "params", params).toString()))
        throw new IOException("Language server disconnected");
      JSONObject result = f.get(20, TimeUnit.SECONDS);
      if (result.has("error"))
        throw new IOException(
            result.getJSONObject("error").optString("message", "Language server error"));
      return result;
    } finally {
      pending.remove(id);
    }
  }

  JSONObject documentRequest(String method, JSONObject params) throws Exception {
    params.put("textDocument", object("uri", uri));
    return request("textDocument/" + method, params);
  }

  private void notify(String method, JSONObject params) {
    if (socket != null)
      socket.send(object("jsonrpc", "2.0", "method", method, "params", params).toString());
  }

  private void fail(Throwable error) {
    pending.values().forEach(f -> f.completeExceptionally(error));
    pending.clear();
  }

  public void close() {
    closed = true;
    if (uri != null) notify("textDocument/didClose", object("textDocument", object("uri", uri)));
    if (socket != null) socket.cancel();
    fail(new IOException("Language service closed"));
    client.dispatcher().executorService().shutdown();
    client.connectionPool().evictAll();
  }
}
