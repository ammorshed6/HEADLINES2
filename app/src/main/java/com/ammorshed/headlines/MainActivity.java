package com.ammorshed.headlines;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    private static final String UA =
            "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) "
                    + "Chrome/124.0.0.0 Mobile Safari/537.36";
    private static final int MAX_BYTES = 3 * 1024 * 1024;

    private WebView web;
    private final ExecutorService pool = Executors.newFixedThreadPool(6);

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        web = new WebView(this);
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);

        // নেটিভ HTTP ব্রিজ: Google News সহ যেকোনো RSS CORS ছাড়াই আনে
        web.addJavascriptInterface(new Bridge(), "AndroidBridge");

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri u = request.getUrl();
                if ("file".equals(u.getScheme())) {
                    return false;
                }
                // খবরের লিংক ব্রাউজারে খুলবে, অ্যাপের পেজ বদলাবে না
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, u));
                } catch (ActivityNotFoundException ignored) {
                }
                return true;
            }
        });

        web.loadUrl("file:///android_asset/index.html");
    }

    private String httpGet(String start) throws Exception {
        String url = start;
        for (int i = 0; i < 6; i++) {
            if (!(url.startsWith("https://") || url.startsWith("http://"))) {
                throw new Exception("bad scheme");
            }
            HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
            try {
                c.setInstanceFollowRedirects(false);
                c.setConnectTimeout(8000);
                c.setReadTimeout(12000);
                c.setRequestProperty("User-Agent", UA);
                c.setRequestProperty("Accept",
                        "application/rss+xml, application/atom+xml, application/xml, text/xml, */*");
                c.setRequestProperty("Accept-Language", "bn-BD,bn;q=0.9,en;q=0.7");

                int code = c.getResponseCode();
                if (code >= 300 && code < 400) {
                    String loc = c.getHeaderField("Location");
                    if (loc == null) {
                        throw new Exception("redirect without location");
                    }
                    url = new URL(new URL(url), loc).toString();
                    continue;
                }
                if (code < 200 || code >= 300) {
                    throw new Exception("http " + code);
                }

                InputStream in = c.getInputStream();
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) {
                    out.write(buf, 0, n);
                    if (out.size() > MAX_BYTES) {
                        break;
                    }
                }
                in.close();
                return out.toString("UTF-8");
            } finally {
                c.disconnect();
            }
        }
        throw new Exception("too many redirects");
    }

    private class Bridge {
        @JavascriptInterface
        public void get(final String id, final String url) {
            pool.execute(new Runnable() {
                @Override
                public void run() {
                    boolean ok = false;
                    String body = "";
                    try {
                        body = httpGet(url);
                        ok = true;
                    } catch (Exception ignored) {
                    }
                    final String js = "window.__nat&&window.__nat("
                            + JSONObject.quote(id) + "," + ok + "," + JSONObject.quote(body) + ")";
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (web != null && !isDestroyed()) {
                                web.evaluateJavascript(js, null);
                            }
                        }
                    });
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        pool.shutdownNow();
        if (web != null) {
            web.destroy();
            web = null;
        }
        super.onDestroy();
    }
}
