package main.service;

import main.ServiceConfig;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class DemoServer {

    private static final HttpClient client = HttpClient.newHttpClient();

    public static void main(String[] args) throws Exception {
        int[] ports = new int[1];
        List<String> cluster = new ArrayList<>(ports.length);
        for (int i = 0; i < ports.length; i++) {
            ports[i] = i + 12353;
            cluster.add("http://localhost:" + ports[i]);
        }

        for (int i = 0; i < ports.length; i++) {
            String url = cluster.get(i);
            ServiceConfig cfg = new ServiceConfig(
                    ports[i],
                    url,
                    cluster,
                    Files.createTempDirectory("server")
            );
            DemoService demoService = new DemoService(cfg);
            demoService.start().get(1, TimeUnit.SECONDS);
            for (int j = 0; j < 5000; j++) {
                upsert("k" + j, ("v" + j).getBytes(), cfg.selfUrl());
            }
            HttpResponse<InputStream> response = get("k1", null, cfg.selfUrl());
            System.out.println(new String(response.body().readAllBytes()));
            System.out.println("Socket is ready: " + url);
        }
    }

    public static HttpResponse<InputStream> get(String start, String end, String url) throws Exception {
        return client.send(
                requestForKeys(start, end, url).GET().build(),
                HttpResponse.BodyHandlers.ofInputStream()
        );
    }

    public static HttpResponse<byte[]> upsert(String key, byte[] data, String url) throws Exception {
        return client.send(
                requestForKey(key, url).PUT(HttpRequest.BodyPublishers.ofByteArray(data)).build(),
                HttpResponse.BodyHandlers.ofByteArray()
        );
    }

    private static HttpRequest.Builder requestForKey(String key, String url) {
        return request("/v0/entity?id=" + key, url);
    }

    private static HttpRequest.Builder requestForKeys(String start, String end, String url) {
        return request("/v0/entities?start=" + start + "&end=" + end , url);
    }
    private static HttpRequest.Builder request(String path, String url) {
        return HttpRequest.newBuilder(URI.create(url + path));
    }
}
