package com.minet.sacco.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Authenticates members against the Minet HR system.
 *
 * The HR endpoint is a GET with username/password query parameters (HTTPS only). Because the exact
 * response layout is owned by the HR system, the parser is deliberately tolerant and everything that
 * can differ is configurable. Passwords are never logged.
 */
@Service
public class HrAuthService {

    private static final Logger log = LoggerFactory.getLogger(HrAuthService.class);

    @Value("${hr.api.url:}")
    private String hrApiUrl;

    @Value("${hr.api.username-param:username}")
    private String usernameParam;

    @Value("${hr.api.password-param:password}")
    private String passwordParam;

    /**
     * The Minet HR endpoint (dmvic.minet.co.ke) expects BOTH the username and the password
     * Base64-encoded in the query string (this is encoding, not encryption). Without it every
     * login, even a correct one, is answered with status 1 "Error".
     */
    @Value("${hr.api.base64-credentials:true}")
    private boolean base64Credentials;

    @Value("${hr.api.timeout-seconds:10}")
    private int timeoutSeconds;

    /** When false, members authenticate ONLY against HR (no local password fallback). */
    @Value("${hr.api.allow-local-fallback:true}")
    private boolean allowLocalFallback;

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    /** Last response summary (password masked) - for log diagnostics only. */
    private volatile String lastDiagnostic = "";

    private static String mask(String text, String secret) {
        if (text == null) return "";
        String out = text;
        if (secret != null && secret.length() >= 3) {
            out = out.replace(secret, "***");
        }
        return out.replaceAll("(?i)(\"(?:token|access_token|jwt|password|pass)\"\\s*:\\s*\")[^\"]+", "$1***");
    }

    public boolean isEnabled() {
        return hrApiUrl != null && !hrApiUrl.isBlank();
    }

    public boolean isLocalFallbackAllowed() {
        return allowLocalFallback;
    }

    /** True when HR is the only password source for members (no local passwords, no reset flows). */
    public boolean isHrOnly() {
        return isEnabled() && !allowLocalFallback;
    }

    public enum Outcome { SUCCESS, INVALID_CREDENTIALS, UNAVAILABLE }

    public static class Result {
        private final Outcome outcome;
        private final Map<String, String> attributes;
        private String diagnostic = "";

        public String getDiagnostic() { return diagnostic; }
        void setDiagnostic(String d) { this.diagnostic = d == null ? "" : d; }

        Result(Outcome outcome, Map<String, String> attributes) {
            this.outcome = outcome;
            this.attributes = attributes;
        }

        public Outcome getOutcome() { return outcome; }
        public Map<String, String> getAttributes() { return attributes; }
        public boolean isSuccess() { return outcome == Outcome.SUCCESS; }

        public String get(String... keys) {
            for (String k : keys) {
                String v = attributes.get(k.toLowerCase(Locale.ROOT));
                if (v != null && !v.isBlank()) return v.trim();
            }
            return null;
        }
    }

    @Value("${hr.api.debug:false}")
    private boolean debug;

    /** When true (HR_API_DEBUG=true) sign-in failures include what the HR system replied. Temporary use only. */
    public boolean isDebug() {
        return debug;
    }

    public Result authenticate(String username, String password) {
        lastDiagnostic = "";
        Result r = doAuthenticate(username, password);
        r.setDiagnostic(lastDiagnostic.isEmpty() ? "no HTTP response (" + r.getOutcome() + ")" : lastDiagnostic);
        return r;
    }

    private Result doAuthenticate(String username, String password) {
        if (!isEnabled()) {
            return new Result(Outcome.UNAVAILABLE, Map.of());
        }
        if (username == null || username.isBlank() || password == null || password.isEmpty()) {
            return new Result(Outcome.INVALID_CREDENTIALS, Map.of());
        }

        try {
            String sendUser = base64Credentials
                    ? java.util.Base64.getEncoder().encodeToString(username.getBytes(StandardCharsets.UTF_8)) : username;
            String sendPass = base64Credentials
                    ? java.util.Base64.getEncoder().encodeToString(password.getBytes(StandardCharsets.UTF_8)) : password;

            String sep = hrApiUrl.contains("?") ? "&" : "?";
            String url = hrApiUrl + sep
                    + URLEncoder.encode(usernameParam, StandardCharsets.UTF_8) + "=" + URLEncoder.encode(sendUser, StandardCharsets.UTF_8)
                    + "&" + URLEncoder.encode(passwordParam, StandardCharsets.UTF_8) + "=" + URLEncoder.encode(sendPass, StandardCharsets.UTF_8);

            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .header("Accept", "application/json, text/plain, */*")
                    .header("User-Agent", "Mozilla/5.0 (compatible; MinetSacco/1.0)")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            String body = response.body() == null ? "" : response.body().trim();
            String contentType = response.headers().firstValue("content-type").orElse("?");
            lastDiagnostic = "HTTP " + status + " " + contentType + " :: "
                    + mask(body, password).replaceAll("\\s+", " ");
            if (lastDiagnostic.length() > 400) lastDiagnostic = lastDiagnostic.substring(0, 400) + "...";

            if (status == 401 || status == 403) {
                log.warn("HR API rejected the request: {}", lastDiagnostic);
                return new Result(Outcome.INVALID_CREDENTIALS, Map.of());
            }
            if (status >= 500 || status == 404) {
                log.warn("HR API unavailable (HTTP {})", status);
                return new Result(Outcome.UNAVAILABLE, Map.of());
            }
            if (status != 200) {
                log.warn("HR API returned an unexpected status: {}", lastDiagnostic);
                return new Result(Outcome.UNAVAILABLE, Map.of());
            }
            if (body.isEmpty() || body.startsWith("<")) {
                // Empty body or an HTML page (login form / error page) is not a usable login answer
                log.warn("HR API returned a non-JSON response: {}", lastDiagnostic);
                return new Result(Outcome.UNAVAILABLE, Map.of());
            }

            JsonNode root;
            try {
                root = mapper.readTree(body);
            } catch (Exception parse) {
                // Not JSON - treat a bare "true"/"ok"/"success" body as success, anything else as failure
                String b = body.toLowerCase(Locale.ROOT);
                boolean ok = b.equals("true") || b.equals("ok") || b.equals("success") || b.equals("1");
                return new Result(ok ? Outcome.SUCCESS : Outcome.INVALID_CREDENTIALS, Map.of());
            }

            Map<String, String> attrs = new HashMap<>();
            flatten(root, "", attrs, 0);

            // Minet HR envelope: { "status": 0, "message": "Success", "data": [ { username, PayrollNo, ... } ] }
            // status 0 = success. A failure carries a non-zero status and/or an empty data list.
            if (root.has("status") && root.has("data")) {
                JsonNode data = root.get("data");
                boolean hasUser = (data.isArray() && data.size() > 0 && data.get(0).isObject() && data.get(0).size() > 0)
                        || (data.isObject() && data.size() > 0);
                String st = root.get("status").asText().trim();
                boolean statusOk = st.equals("0") || st.equalsIgnoreCase("success") || st.equalsIgnoreCase("ok")
                        || st.equalsIgnoreCase("true");
                if (statusOk && hasUser) {
                    return new Result(Outcome.SUCCESS, attrs);
                }
                log.info("HR API rejected the credentials: {}", lastDiagnostic);
                return new Result(Outcome.INVALID_CREDENTIALS, Map.of());
            }

            if (isFailure(root, attrs)) {
                log.info("HR API rejected the credentials: {}", lastDiagnostic);
                return new Result(Outcome.INVALID_CREDENTIALS, Map.of());
            }
            if (!isSuccess(root, attrs)) {
                // Neither a clear success nor a clear failure: do not claim the password was wrong
                log.warn("HR API returned an unrecognised response (keys: {}): {}", attrs.keySet(), lastDiagnostic);
                return new Result(Outcome.UNAVAILABLE, Map.of());
            }
            return new Result(Outcome.SUCCESS, attrs);

        } catch (java.net.http.HttpTimeoutException | java.net.ConnectException e) {
            lastDiagnostic = "connection failed: " + e.getClass().getSimpleName();
            log.warn("HR API unreachable: {}", e.getClass().getSimpleName());
            return new Result(Outcome.UNAVAILABLE, Map.of());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new Result(Outcome.UNAVAILABLE, Map.of());
        } catch (Exception e) {
            lastDiagnostic = "call failed: " + e.getClass().getSimpleName() + " " + String.valueOf(e.getMessage());
            log.warn("HR API call failed: {}", e.getClass().getSimpleName());
            return new Result(Outcome.UNAVAILABLE, Map.of());
        }
    }

    /** Flatten JSON into lower-cased leaf keys (last path segment wins; nested objects are included). */
    private void flatten(JsonNode node, String prefix, Map<String, String> out, int depth) {
        if (node == null || depth > 4) return;
        if (node.isObject()) {
            node.fields().forEachRemaining(e -> {
                String key = e.getKey().toLowerCase(Locale.ROOT);
                JsonNode v = e.getValue();
                if (v.isValueNode()) {
                    out.putIfAbsent(key, v.asText());
                    if (!prefix.isEmpty()) out.putIfAbsent(prefix + "." + key, v.asText());
                } else {
                    flatten(v, key, out, depth + 1);
                }
            });
        } else if (node.isArray() && node.size() > 0) {
            flatten(node.get(0), prefix, out, depth + 1);
        }
    }

    private static final String[] FAIL_WORDS = {"invalid", "incorrect", "wrong", "failed", "failure", "denied",
            "not found", "unauthor", "unauthoris", "does not exist", "no user", "bad credentials", "error"};
    private static final String[] OK_WORDS = {"success", "logged in", "login ok", "authenticated", "welcome", "valid user"};

    private String messageOf(Map<String, String> a) {
        for (String k : new String[]{"message", "msg", "result", "response", "description", "detail"}) {
            String v = a.get(k);
            if (v != null && !v.isBlank()) return v.toLowerCase(Locale.ROOT);
        }
        return null;
    }

    private boolean isFailure(JsonNode root, Map<String, String> a) {
        if (root.isArray() && root.size() == 0) return true;
        String msg = messageOf(a);
        if (msg != null && !a.containsKey("token") && !a.containsKey("access_token")) {
            for (String w : OK_WORDS) { if (msg.contains(w)) { msg = null; break; } }
            if (msg != null) {
                for (String w : FAIL_WORDS) { if (msg.contains(w)) return true; }
            }
        }
        for (String k : new String[]{"success", "authenticated", "valid", "login", "ok"}) {
            String v = a.get(k);
            if (v != null && (v.equalsIgnoreCase("false") || v.equals("0"))) return true;
        }
        String status = a.get("status");
        if (status != null) {
            String s = status.toLowerCase(Locale.ROOT);
            if (s.equals("error") || s.equals("fail") || s.equals("failed") || s.equals("failure")
                    || s.equals("false") || s.equals("0") || s.equals("invalid") || s.equals("unauthorized")) return true;
        }
        return a.containsKey("error") && !a.get("error").isBlank() && !a.get("error").equalsIgnoreCase("false")
                && !a.get("error").equals("0");
    }

    private boolean isSuccess(JsonNode root, Map<String, String> a) {
        String msg = messageOf(a);
        if (msg != null) {
            for (String w : OK_WORDS) { if (msg.contains(w)) return true; }
        }
        for (String k : new String[]{"success", "authenticated", "valid", "login", "ok"}) {
            String v = a.get(k);
            if (v != null && (v.equalsIgnoreCase("true") || v.equals("1"))) return true;
        }
        String status = a.get("status");
        if (status != null) {
            String s = status.toLowerCase(Locale.ROOT);
            if (s.equals("success") || s.equals("ok") || s.equals("true") || s.equals("1") || s.equals("200")
                    || s.equals("authenticated") || s.equals("active")) return true;
        }
        // Presence of an identity / token is accepted as a successful login
        return a.containsKey("token") || a.containsKey("access_token") || a.containsKey("employee_id")
                || a.containsKey("employeeid") || a.containsKey("user_id") || a.containsKey("userid")
                || a.containsKey("id") || a.containsKey("email") || a.containsKey("name") || a.containsKey("fullname");
    }
}
