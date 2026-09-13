package com.TechHurts.CodeFile;
public class ApiEndpointTest {
    static void accept(String url) throws Exception { ApiEndpoint.parse(url); }
    static void reject(String url) throws Exception { try { ApiEndpoint.parse(url); throw new AssertionError("Accepted: "+url); } catch(IllegalArgumentException expected) { } }
    public static void main(String[] args) throws Exception {
        accept("https://openrouter.ai/api/v1/chat/completions");
        accept("http://127.0.0.1:11434/v1/chat/completions");
        accept("http://192.168.1.10:1234/v1/chat/completions");
        accept("http://10.0.2.2:8080/v1/chat/completions");
        accept("http://[::1]:8080/v1/chat/completions");
        reject("http://8.8.8.8/v1/chat/completions");
        reject("http://example.com/v1/chat/completions");
        reject("https://user:secret@example.com/v1/chat/completions");
        reject("file:///v1/chat/completions");
        reject("https://example.com/v1/models");
        reject("https://example.com/v1/chat/completions?key=x");
        System.out.println("11 API endpoint checks passed");
    }
}
