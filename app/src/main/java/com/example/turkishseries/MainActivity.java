package com.example.turkishseries;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    static class Site {
        String name, url;
        Site(String n, String u) { name = n; url = u; }
    }

    // %s يُستبدل باسم المسلسل. المواقع المضافة من المستخدم تُحفظ هنا بصيغة: الاسم|الرابط
    private static final String[][] DEFAULTS = {
            {"يوتيوب", "https://www.youtube.com/results?search_query=%s"},
            {"بحث جوجل", "https://www.google.com/search?q=%s"},
            {"نتفليكس", "https://www.netflix.com/search?q=%s"},
    };

    private final List<Site> sites = new ArrayList<>();
    private FrameLayout root;
    private LinearLayout home;
    private WebView web;
    private EditText query;
    private ListView listView;
    private ArrayAdapter<String> adapter;
    private SharedPreferences prefs;
    private View customView;
    private WebChromeClient.CustomViewCallback customCb;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = getSharedPreferences("sites", MODE_PRIVATE);
        loadSites();

        root = new FrameLayout(this);
        root.setBackgroundColor(Color.parseColor("#121212"));

        buildHome();
        buildWeb();

        root.addView(home);
        root.addView(web);
        web.setVisibility(View.GONE);
        setContentView(root);
    }

    private void loadSites() {
        sites.clear();
        for (String[] d : DEFAULTS) sites.add(new Site(d[0], d[1]));
        String saved = prefs.getString("custom", "");
        if (!saved.isEmpty()) {
            for (String line : saved.split("\n")) {
                String[] p = line.split("\\|", 2);
                if (p.length == 2) sites.add(new Site(p[0], p[1]));
            }
        }
    }

    private void saveCustom() {
        StringBuilder sb = new StringBuilder();
        for (int i = DEFAULTS.length; i < sites.size(); i++) {
            sb.append(sites.get(i).name).append("|").append(sites.get(i).url).append("\n");
        }
        prefs.edit().putString("custom", sb.toString()).apply();
    }

    private void buildHome() {
        home = new LinearLayout(this);
        home.setOrientation(LinearLayout.VERTICAL);
        home.setPadding(32, 48, 32, 32);
        home.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        TextView title = new TextView(this);
        title.setText("مسلسلات تركية");
        title.setTextSize(24);
        title.setTextColor(Color.WHITE);
        title.setGravity(Gravity.CENTER);
        home.addView(title);

        query = new EditText(this);
        query.setHint("اسم المسلسل");
        query.setText("المدينة البعيدة");
        query.setTextColor(Color.WHITE);
        query.setHintTextColor(Color.GRAY);
        query.setInputType(InputType.TYPE_CLASS_TEXT);
        home.addView(query);

        TextView hint = new TextView(this);
        hint.setText("اختر موقعاً للبحث فيه (اضغط مطولاً على موقع أضفته لحذفه)");
        hint.setTextColor(Color.LTGRAY);
        hint.setPadding(0, 16, 0, 16);
        home.addView(hint);

        listView = new ListView(this);
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, names()) {
            @Override
            public View getView(int pos, View v, ViewGroup parent) {
                View view = super.getView(pos, v, parent);
                ((TextView) view).setTextColor(Color.WHITE);
                return view;
            }
        };
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((p, v, pos, id) -> openSite(sites.get(pos)));
        listView.setOnItemLongClickListener((p, v, pos, id) -> {
            if (pos >= DEFAULTS.length) {
                sites.remove(pos);
                saveCustom();
                refresh();
            }
            return true;
        });
        home.addView(listView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        android.widget.Button add = new android.widget.Button(this);
        add.setText("إضافة موقع");
        add.setOnClickListener(v -> showAddDialog());
        home.addView(add);
    }

    private List<String> names() {
        List<String> n = new ArrayList<>();
        for (Site s : sites) n.add(s.name);
        return n;
    }

    private void refresh() {
        adapter.clear();
        adapter.addAll(names());
        adapter.notifyDataSetChanged();
    }

    private void showAddDialog() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(48, 24, 48, 0);
        final EditText name = new EditText(this);
        name.setHint("اسم الموقع");
        final EditText url = new EditText(this);
        url.setHint("الرابط (ضع %s مكان كلمة البحث إن وُجد)");
        url.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
        box.addView(name);
        box.addView(url);

        new AlertDialog.Builder(this)
                .setTitle("إضافة موقع")
                .setView(box)
                .setPositiveButton("حفظ", (d, w) -> {
                    String n = name.getText().toString().trim();
                    String u = url.getText().toString().trim();
                    if (n.isEmpty() || u.isEmpty()) return;
                    if (!u.startsWith("http")) u = "https://" + u;
                    sites.add(new Site(n, u));
                    saveCustom();
                    refresh();
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private void buildWeb() {
        web = new WebView(this);
        web.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, String url) {
                if (url.startsWith("http")) return false;
                return true; // تجاهل الروابط الغريبة (تطبيقات/إعلانات)
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onShowCustomView(View view, CustomViewCallback cb) {
                customView = view;
                customCb = cb;
                web.setVisibility(View.GONE);
                root.addView(view, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                getWindow().getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            }

            @Override
            public void onHideCustomView() {
                exitFullscreen();
            }
        });
    }

    private void exitFullscreen() {
        if (customView == null) return;
        root.removeView(customView);
        customView = null;
        if (customCb != null) customCb.onCustomViewHidden();
        web.setVisibility(View.VISIBLE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
    }

    private void openSite(Site s) {
        String q = query.getText().toString().trim();
        String url = s.url;
        if (url.contains("%s")) {
            try {
                url = url.replace("%s", URLEncoder.encode(q, "UTF-8"));
            } catch (Exception e) {
                Toast.makeText(this, "خطأ في البحث", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        home.setVisibility(View.GONE);
        web.setVisibility(View.VISIBLE);
        web.loadUrl(url);
    }

    @Override
    public void onBackPressed() {
        if (customView != null) {
            exitFullscreen();
        } else if (web.getVisibility() == View.VISIBLE) {
            if (web.canGoBack()) {
                web.goBack();
            } else {
                web.loadUrl("about:blank");
                web.setVisibility(View.GONE);
                home.setVisibility(View.VISIBLE);
            }
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        web.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        web.onResume();
    }
}