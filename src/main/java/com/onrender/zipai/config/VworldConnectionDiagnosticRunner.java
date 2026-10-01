package com.onrender.zipai.config;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Temporary, key-free outbound connectivity check; disabled by default. */
@Component
@ConditionalOnProperty(name = "zipai.vworld.diagnostics.enabled", havingValue = "true")
public class VworldConnectionDiagnosticRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(VworldConnectionDiagnosticRunner.class);
    private static final URI TARGET = URI.create(
        "https://api.vworld.kr/req/search?service=search&request=search&version=2.0&format=json");

    @Override
    public void run(ApplicationArguments args) {
        log.info("[VWORLD-DIAG] START key-free HTTPS check; PARAM_REQUIRED is an expected response");
        checkJdkClient();
        checkUrlConnection();
        log.info("[VWORLD-DIAG] END; disable ZIPAI_VWORLD_DIAGNOSTICS_ENABLED after collecting these logs");
    }

    private void checkJdkClient() {
        long started = System.nanoTime();
        try (HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(5)).build()) {
            HttpRequest request = HttpRequest.newBuilder(TARGET)
                .timeout(Duration.ofSeconds(8)).GET().build();
            HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            report("JDK_HTTP_1_1", response.statusCode(), response.body(), started);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            failure("JDK_HTTP_1_1", error, started);
        } catch (Exception error) {
            failure("JDK_HTTP_1_1", error, started);
        }
    }

    private void checkUrlConnection() {
        long started = System.nanoTime();
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) TARGET.toURL().openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(8000);
            connection.setInstanceFollowRedirects(false);
            int status = connection.getResponseCode();
            var stream = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
            String body = "";
            if (stream != null) {
                try (var input = stream) {
                    body = new String(input.readNBytes(2048), StandardCharsets.UTF_8);
                }
            }
            report("URL_CONNECTION", status, body, started);
        } catch (Exception error) {
            failure("URL_CONNECTION", error, started);
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private void report(String transport, int status, String body, long started) {
        // Never log response bodies, cookies, API keys, or environment values.
        log.info("[VWORLD-DIAG] transport={} httpStatus={} expectedMissingKey={} elapsedMs={}",
            transport, status, body.contains("PARAM_REQUIRED"), elapsed(started));
    }

    private void failure(String transport, Exception error, long started) {
        Throwable root = error;
        for (int depth = 0; depth < 10 && root.getCause() != null && root.getCause() != root; depth++) {
            root = root.getCause();
        }
        log.warn("[VWORLD-DIAG] transport={} failure={} rootCause={} elapsedMs={}",
            transport, error.getClass().getSimpleName(), root.getClass().getSimpleName(), elapsed(started));
    }

    private long elapsed(long started) {
        return (System.nanoTime() - started) / 1_000_000;
    }
}
