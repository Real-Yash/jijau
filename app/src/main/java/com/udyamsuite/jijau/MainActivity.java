package com.udyamsuite.jijau;

import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.FileProvider;
import androidx.core.graphics.ColorUtils;
import androidx.core.graphics.Insets;
import androidx.core.splashscreen.SplashScreen;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textview.MaterialTextView;

import java.io.File;
import java.io.IOException;

/** Native WebView wrapper for the Jijau UdyamSuite ERPNext site. */
public class MainActivity extends ComponentActivity {
    private static final String HOME_URL = "https://jijau.udyamsuite.com";
    private static final String TRUSTED_HOST = "jijau.udyamsuite.com";
    private WebView webView;
    private SwipeRefreshLayout pullToRefresh;
    private LinearProgressIndicator loadingIndicator;
    private View connectionErrorView;
    private ValueCallback<Uri[]> pendingFileCallback;
    private Uri pendingCameraUri;

    private final ActivityResultLauncher<Intent> fileChooserLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (pendingFileCallback == null) {
                    return;
                }

                Uri[] selectedFiles = null;
                if (result.getResultCode() == RESULT_OK) {
                    Intent data = result.getData();
                    if (data == null && pendingCameraUri != null) {
                        selectedFiles = new Uri[]{pendingCameraUri};
                    } else if (data != null) {
                        selectedFiles = WebChromeClient.FileChooserParams.parseResult(
                                result.getResultCode(), data);
                    }
                }
                pendingFileCallback.onReceiveValue(selectedFiles);
                pendingFileCallback = null;
                pendingCameraUri = null;
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        buildContentView();
        applySystemBarAppearance();
        configureWebView();

        if (savedInstanceState == null) {
            webView.loadUrl(HOME_URL);
        } else {
            webView.restoreState(savedInstanceState);
        }
    }

    private void buildContentView() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(getColor(R.color.webview_surface));
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets systemBars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        loadingIndicator = new LinearProgressIndicator(this);
        loadingIndicator.setIndicatorColor(getColor(R.color.progress_indicator));
        loadingIndicator.setTrackColor(getColor(R.color.app_bar));
        loadingIndicator.setVisibility(View.GONE);
        root.addView(loadingIndicator, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(3)));

        pullToRefresh = new SwipeRefreshLayout(this);
        pullToRefresh.setColorSchemeColors(getColor(R.color.progress_indicator));
        pullToRefresh.setOnChildScrollUpCallback((parent, child) -> webView.getScrollY() > 0);
        pullToRefresh.setOnRefreshListener(() -> webView.reload());

        FrameLayout webContainer = new FrameLayout(this);
        webView = new WebView(this);
        webView.setBackgroundColor(getColor(R.color.webview_surface));
        webContainer.addView(webView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        connectionErrorView = createConnectionErrorView();
        webContainer.addView(connectionErrorView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        connectionErrorView.setVisibility(View.GONE);
        pullToRefresh.addView(webContainer, new SwipeRefreshLayout.LayoutParams(
                SwipeRefreshLayout.LayoutParams.MATCH_PARENT, SwipeRefreshLayout.LayoutParams.MATCH_PARENT));
        root.addView(pullToRefresh, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));
        setContentView(root);
        ViewCompat.requestApplyInsets(root);
    }

    /** Matches system bars to the active theme and keeps their icons legible. */
    private void applySystemBarAppearance() {
        int surfaceColor = MaterialColors.getColor(webView,
                com.google.android.material.R.attr.colorSurface);
        getWindow().setStatusBarColor(surfaceColor);
        getWindow().setNavigationBarColor(surfaceColor);

        boolean useDarkIcons = ColorUtils.calculateLuminance(surfaceColor) > 0.5d;
        WindowInsetsControllerCompat insetsController = WindowCompat.getInsetsController(
                getWindow(), getWindow().getDecorView());
        insetsController.setAppearanceLightStatusBars(useDarkIcons);
        insetsController.setAppearanceLightNavigationBars(useDarkIcons);
    }

    private View createConnectionErrorView() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        int padding = dp(24);
        layout.setPadding(padding, padding, padding, padding);
        layout.setBackgroundColor(getColor(R.color.webview_surface));

        MaterialTextView title = new MaterialTextView(this);
        title.setText(R.string.network_error_title);
        title.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_HeadlineSmall);
        title.setTextColor(getColor(R.color.on_surface));
        title.setGravity(Gravity.CENTER);
        layout.addView(title);

        MaterialTextView message = new MaterialTextView(this);
        message.setText(R.string.network_error_message);
        message.setTextColor(getColor(R.color.on_surface));
        message.setGravity(Gravity.CENTER);
        layout.addView(message, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 0));

        MaterialButton retry = new MaterialButton(this);
        retry.setText(R.string.retry);
        retry.setOnClickListener(view -> webView.reload());
        layout.addView(retry);
        return layout;
    }

    private void configureWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadsImagesAutomatically(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);

        CookieManager cookies = CookieManager.getInstance();
        cookies.setAcceptCookie(true);
        cookies.setAcceptThirdPartyCookies(webView, true);

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int progress) {
                setLoadingProgress(progress);
            }

            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> filePathCallback,
                                             FileChooserParams fileChooserParams) {
                if (pendingFileCallback != null) {
                    pendingFileCallback.onReceiveValue(null);
                }
                pendingFileCallback = filePathCallback;
                Intent chooser = createFileChooserIntent(fileChooserParams);
                try {
                    fileChooserLauncher.launch(chooser);
                } catch (ActivityNotFoundException exception) {
                    pendingFileCallback.onReceiveValue(null);
                    pendingFileCallback = null;
                    Toast.makeText(MainActivity.this, "No app is available to select a file.", Toast.LENGTH_LONG).show();
                }
                return true;
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return openUrl(request.getUrl());
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                connectionErrorView.setVisibility(View.GONE);
                setLoadingProgress(0);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                setLoadingProgress(100);
                pullToRefresh.setRefreshing(false);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request,
                                        WebResourceError error) {
                if (request.isForMainFrame() && isConnectivityError(error)) {
                    setLoadingProgress(100);
                    pullToRefresh.setRefreshing(false);
                    connectionErrorView.setVisibility(View.VISIBLE);
                }
            }
        });

        webView.setDownloadListener(this::downloadFile);
    }

    private void setLoadingProgress(int progress) {
        loadingIndicator.setProgressCompat(progress, true);
        loadingIndicator.setVisibility(progress < 100 ? View.VISIBLE : View.GONE);
    }

    private boolean isConnectivityError(WebResourceError error) {
        int code = error.getErrorCode();
        return code == WebViewClient.ERROR_CONNECT
                || code == WebViewClient.ERROR_HOST_LOOKUP
                || code == WebViewClient.ERROR_IO
                || code == WebViewClient.ERROR_TIMEOUT;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private Intent createFileChooserIntent(WebChromeClient.FileChooserParams params) {
        Intent picker = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        picker.addCategory(Intent.CATEGORY_OPENABLE);
        picker.setType("*/*");
        picker.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,
                params.getMode() == WebChromeClient.FileChooserParams.MODE_OPEN_MULTIPLE);

        if (!params.isCaptureEnabled()) {
            return Intent.createChooser(picker, getString(R.string.select_file));
        }

        Intent camera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File cameraDirectory = new File(getCacheDir(), "webview-camera");
        if (!cameraDirectory.exists() && !cameraDirectory.mkdirs()) {
            return Intent.createChooser(picker, getString(R.string.select_file));
        }
        try {
            File image = File.createTempFile("upload-", ".jpg", cameraDirectory);
            pendingCameraUri = FileProvider.getUriForFile(this,
                    getPackageName() + ".fileprovider", image);
            camera.putExtra(MediaStore.EXTRA_OUTPUT, pendingCameraUri);
            camera.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            return Intent.createChooser(picker, getString(R.string.select_file))
                    .putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{camera});
        } catch (IOException exception) {
            pendingCameraUri = null;
            return Intent.createChooser(picker, getString(R.string.select_file));
        }
    }

    private boolean openUrl(Uri uri) {
        if ("https".equalsIgnoreCase(uri.getScheme()) && TRUSTED_HOST.equalsIgnoreCase(uri.getHost())) {
            return false;
        }
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (ActivityNotFoundException exception) {
            Toast.makeText(this, "No app is available to open this link.", Toast.LENGTH_LONG).show();
        }
        return true;
    }

    private void downloadFile(String url, String userAgent, String contentDisposition,
                              String mimeType, long contentLength) {
        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setMimeType(mimeType);
            request.addRequestHeader("User-Agent", userAgent);
            String cookie = CookieManager.getInstance().getCookie(url);
            if (cookie != null) {
                request.addRequestHeader("Cookie", cookie);
            }
            String filename = URLUtil.guessFileName(url, contentDisposition, mimeType);
            request.setTitle(filename);
            request.setDescription("Downloading " + filename);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
            ((DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE)).enqueue(request);
            Toast.makeText(this, "Download started", Toast.LENGTH_SHORT).show();
        } catch (Exception exception) {
            Toast.makeText(this, "Unable to start the download.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onPause() {
        CookieManager.getInstance().flush();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        applySystemBarAppearance();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        webView.saveState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (pendingFileCallback != null) {
            pendingFileCallback.onReceiveValue(null);
            pendingFileCallback = null;
        }
        webView.destroy();
        super.onDestroy();
    }
}
