### 📎 Приложение А. Полный листинг SentimentApp.java

```java
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class SentimentApp {

    // Лексикон для анализа тональности (mock ИИ-модель)
    private static final Map<String, Integer> LEXICON = new HashMap<>();
    static {
        for (String w : new String[]{"good", "great", "love", "happy", "hello", "friend", "nice", "awesome"})
            LEXICON.put(w, 1);
        for (String w : new String[]{"bad", "hate", "terrible", "awful", "sad", "error", "worst", "broken"})
            LEXICON.put(w, -1);
    }

    // Метрики для Prometheus (counters)
    private static final AtomicLong REQ_TOTAL = new AtomicLong(0);
    private static final AtomicLong SENT_POS = new AtomicLong(0);
    private static final AtomicLong SENT_NEG = new AtomicLong(0);
    private static final AtomicLong SENT_NEU = new AtomicLong(0);

    public static void main(String[] args) throws IOException {
        int port = Integer.parseInt(System.getProperty("server.port", "8080"));
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api/sentiment", SentimentApp::handleSentiment);
        server.createContext("/metrics", SentimentApp::handleMetrics);
        server.createContext("/health", SentimentApp::handleHealth);
        server.setExecutor(null);
        System.out.println("Sentiment API started on port " + port);
        server.start();
    }

    static void handleSentiment(HttpExchange ex) throws IOException {
        REQ_TOTAL.incrementAndGet();
        String query = ex.getRequestURI().getQuery();
        String text = "neutral";
        if (query != null && query.startsWith("text=")) {
            text = java.net.URLDecoder.decode(query.substring(5), StandardCharsets.UTF_8);
        }

        String sentiment = analyze(text);
        switch (sentiment) {
            case "positive": SENT_POS.incrementAndGet(); break;
            case "negative": SENT_NEG.incrementAndGet(); break;
            default: SENT_NEU.incrementAndGet();
        }

        String json = "{\"text\":\"" + escape(text) + "\",\"sentiment\":\"" + sentiment + "\"}";
        ex.getResponseHeaders().set("Content-Type", "application/json");
        byte[] resp = json.getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(200, resp.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(resp); }
    }

    static String analyze(String text) {
        if (text == null || text.isEmpty()) return "neutral";
        int score = 0;
        for (String token : text.toLowerCase().split("[^a-z]+")) {
            score += LEXICON.getOrDefault(token, 0);
        }
        if (score > 0) return "positive";
        if (score < 0) return "negative";
        return "neutral";
    }

    static void handleMetrics(HttpExchange ex) throws IOException {
        String m = "# HELP sentiment_requests_total Total API requests\n"
                 + "# TYPE sentiment_requests_total counter\n"
                 + "sentiment_requests_total " + REQ_TOTAL.get() + "\n"
                 + "# HELP sentiment_classified_total Classified requests by label\n"
                 + "# TYPE sentiment_classified_total counter\n"
                 + "sentiment_classified_total{label=\"positive\"} " + SENT_POS.get() + "\n"
                 + "sentiment_classified_total{label=\"negative\"} " + SENT_NEG.get() + "\n"
                 + "sentiment_classified_total{label=\"neutral\"} " + SENT_NEU.get() + "\n";
        ex.getResponseHeaders().set("Content-Type", "text/plain; version=0.0.4");
        byte[] resp = m.getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(200, resp.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(resp); }
    }

    static void handleHealth(HttpExchange ex) throws IOException {
        byte[] resp = "ok".getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "text/plain");
        ex.sendResponseHeaders(200, resp.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(resp); }
    }

    static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
