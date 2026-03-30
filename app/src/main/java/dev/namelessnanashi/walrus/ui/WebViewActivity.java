package dev.namelessnanashi.walrus.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class WebViewActivity extends AppCompatActivity {

    private static final String EXTRA_URL =
            "dev.namelessnanashi.walrus.ui.WebViewActivity.EXTRA_URL";

    public static Intent createIntent(Context context, String url) {
        Intent intent = new Intent(context, WebViewActivity.class);
        intent.putExtra(EXTRA_URL, url);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WebView webView = new WebView(this);
        WebSettings webSettings = webView.getSettings();

        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webView.setWebViewClient(new WebViewClient());

        setContentView(webView);

        Intent intent = getIntent();
        String url = intent.getData() != null ? intent.getData().toString()
                : intent.getStringExtra(EXTRA_URL);
        if (url == null || url.trim().isEmpty()) {
            finish();
            return;
        }

        if (url.startsWith("http://") || url.startsWith("https://")) {
            webSettings.setJavaScriptEnabled(true);
            webSettings.setDomStorageEnabled(true);
            webSettings.setLoadWithOverviewMode(true);
            webSettings.setUseWideViewPort(true);
        }

        webView.loadUrl(url);
    }
}
