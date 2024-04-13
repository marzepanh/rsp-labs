package lab4;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class Helper implements HttpHandler {

    private static final Map<String, byte[]> cache = new HashMap<>();

    private final int port;

    public Helper(int port) {
        this.port = port;
    }

    public void handle(HttpExchange exchange) {
        String method = exchange.getRequestMethod();

        try {
            String url;
            if (exchange.getRequestHeaders().containsKey("Referer")) {
                url = exchange.getRequestHeaders().get("Referer").get(0)
                        .replace("localhost:" + port + "/", "") + exchange.getRequestURI().toString();
            } else {
                url = "http://" + exchange.getRequestURI().toString().substring(1);
            }

            if (cache.containsKey(url)) {
                System.out.println("URL found in cache: " + url);
                getFromCache(exchange, cache.get(url));
            } else {
                System.out.println("No URl in cache: " + url);
                getUnknown(exchange, method, url);
            }
        } catch (Exception e) {
            System.out.println("Couldn't load resource " + exchange.getRequestURI());
        }
    }

    private void getFromCache(HttpExchange exchange, byte[] cachedResponse) throws IOException {
        exchange.sendResponseHeaders(200, cachedResponse.length);
        exchange.getResponseBody().write(cachedResponse);
    }

    private void getUnknown(HttpExchange exchange, String method, String url) throws IOException {
        OkHttpClient client = new OkHttpClient();
        Request.Builder requestBuilder = new Request.Builder().url(url);

        if ("POST".equalsIgnoreCase(method)) {
            InputStream requestBody = exchange.getRequestBody();
            byte[] bytes = requestBody.readAllBytes();
            RequestBody body = RequestBody.create(MediaType.parse("application/octet-stream"), bytes);
            requestBuilder.post(body);
        }

        Request request = requestBuilder.build();
        Call call = client.newCall(request);
        Response response = call.execute();

        byte[] responseBody = new byte[0];
        if (response.body() != null) {
            responseBody = response.body().bytes();
        }
        cache.put(url, responseBody);

        Map<String, List<String>> headers = new HashMap<>();
        for (String headerName : response.headers().names()) {
            headers.put(headerName, response.headers(headerName));
        }

        exchange.getResponseHeaders().putAll(headers);
        exchange.sendResponseHeaders(response.code(), responseBody.length);
        exchange.getResponseBody().write(responseBody);
    }
}
