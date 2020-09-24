package com.github.qianniancc.okbrowser;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.webkit.WebSettings;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.github.qianniancc.okbrowser.util.BottomNavigationViewEx;
import com.github.qianniancc.okbrowser.util.BottomNavigationViewHelper;

import java.util.HashMap;
import java.util.HashSet;

import at.pardus.android.webview.gm.run.WebViewGm;
import at.pardus.android.webview.gm.store.ScriptStoreSQLite;

import static android.webkit.WebSettings.LayoutAlgorithm.SINGLE_COLUMN;

public class MainActivity extends AppCompatActivity implements TopBarView.onTitleBarClickListener {

    private DrawerLayout mDrawerLayout;
    private ViewPager mViewPager;
    private TabLayout mTabLayout;

    private static final String TAG = MainActivity.class.getName();
    private static WebViewGm wView;
    private long exitTime = 0;
    private ScriptStoreSQLite sss;
    private static SharedPreferences preferences;
    private McbbsBrowser mcbbs;
    private McbbsDownloadListener mdl;
    private McbbsWebChromeClient mwcc;
    private McbbsWebViewClientGm mwvcg;
    private HashMap<String,String> userscripts=new HashMap<>();
    private HashSet nowscripts=new HashSet();
    private static TopBarView topbar;
    private static EditText tv;
    private static BottomNavigationViewEx bottomNavigationView;
    private  static NavigationView navigationView;
    private static View navHeader;
    private static String index="https://www.baidu.com/";

    public static void setTitle(String title){
        tv.setText(title);
    }


    public static Handler getMyHandler() {
        return myHandler;
    }

    private static Handler myHandler=new Handler() {
        @SuppressWarnings("unchecked")
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                if(wView.getSettings().getLoadsImagesAutomatically()){
                    wView.getSettings().setLoadsImagesAutomatically(false);
                }
            }else if(msg.what==2){
                if(!wView.getSettings().getLoadsImagesAutomatically()){
                    wView.getSettings().setLoadsImagesAutomatically(true);
                }
            }else if(msg.what==3){
                loadTheme();
            }
        }
    };


    private String getVersionName()
    {
        PackageManager packageManager = getPackageManager();
        PackageInfo packInfo = null;
        try {
            packInfo = packageManager.getPackageInfo(getPackageName(),0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        String version = packInfo.versionName;
        return version;
    }

    public static boolean isValidURI(String uri) {
        if (uri == null || uri.indexOf(' ') >= 0 || uri.indexOf('\n') >= 0) {
            return false;
        }
        String scheme = Uri.parse(uri).getScheme();
        if (scheme == null) {
            return false;
        }

        // Look for period in a domain but followed by at least a two-char TLD
        // Forget strings that don't have a valid-looking protocol
        int period = uri.indexOf('.');
        if (period >= uri.length() - 2) {
            return false;
        }
        int colon = uri.indexOf(':');
        if (period < 0 && colon < 0) {
            return false;
        }
        if (colon >= 0) {
            if (period < 0 || period > colon) {
                // colon ends the protocol
                for (int i = 0; i < colon; i++) {
                    char c = uri.charAt(i);
                    if ((c < 'a' || c > 'z') && (c < 'A' || c > 'Z')) {
                        return false;
                    }
                }
            } else {
                // colon starts the port; crudely look for at least two numbers
                if (colon >= uri.length() - 2) {
                    return false;
                }
                for (int i = colon + 1; i < colon + 3; i++) {
                    char c = uri.charAt(i);
                    if (c < '0' || c > '9') {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        tv= (EditText) findViewById(R.id.title_name);



        tv.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    if(isValidURI(tv.getText().toString())){
                        wView.loadUrl(tv.getText().toString());
                    }else{
                        wView.loadUrl("https://m.baidu.com/s?word="+tv.getText().toString());
                    }

                }
                return true;
            }
        });


        tv.setOnFocusChangeListener(new android.view.View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus) {
                    tv.setText(wView.getUrl());
                    tv.setSelectAllOnFocus(true);
                    tv.selectAll();

                } else {
                    tv.setText(wView.getTitle());
                }
            }
        });

        mDrawerLayout = (DrawerLayout) findViewById(R.id.dl_main_drawer);


        mDrawerLayout.setBackgroundResource(R.color.blue);
       navigationView =
                (NavigationView) findViewById(R.id.nv_main_navigation);
        if (navigationView != null) {
            navigationView.setNavigationItemSelectedListener(
                    new NavigationView.OnNavigationItemSelectedListener() {
                        @Override
                        public boolean onNavigationItemSelected(MenuItem menuItem) {
                            menuItem.setCheckable(false);
                            menuItem.setChecked(false);
                            mDrawerLayout.closeDrawers();
                            if (menuItem.getTitle().equals("关于")) {
                                Toast.makeText(getApplicationContext(), "版本：" + getVersionName() + "\n开发者：浅念",
                                        Toast.LENGTH_SHORT).show();
                            } else if (menuItem.getTitle().equals("历史/收藏")) {
                                Intent intent = new Intent(MainActivity.this, HistoryActivity.class);
                                startActivity(intent);
                            } else if (menuItem.getTitle().equals("设置")) {
                                Intent intent = new Intent(MainActivity.this, SettingActivity.class);
                                startActivity(intent);
                            }

                            return true;
                        }
                    });
        }
        topbar = (TopBarView) findViewById(R.id.topbar);
        topbar.setClickListener(this);
        wView = (WebViewGm) findViewById(R.id.wView);
        sss = new ScriptStoreSQLite(this);
        mcbbs = new McbbsBrowser(this, sss, index);
        mdl = new McbbsDownloadListener(mcbbs);
        mwcc = new McbbsWebChromeClient(mcbbs);
        sss.open();
        mwvcg = new McbbsWebViewClientGm(sss,
                wView.getWebViewClient().getJsBridgeName(), wView
                .getWebViewClient().getSecret(), mcbbs, myHandler, getApplicationContext());

        bindViews();
        preferences= PreferenceManager.getDefaultSharedPreferences(this);

        bottomNavigationView = (BottomNavigationViewEx) findViewById(R.id.bottom_navigation_view);
       BottomNavigationViewHelper.disableShiftMode(bottomNavigationView);
       bottomNavigationView.enableAnimation(false);
       bottomNavigationView.enableShiftingMode(false);
        bottomNavigationView.enableItemShiftingMode(false);

        navHeader=navigationView.inflateHeaderView(R.layout.navigation_header);
        loadTheme();


        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.menu:
                        mDrawerLayout.openDrawer(GravityCompat.START);
                        break;
                    case R.id.plugin:
                        wView.loadUrl("http://mcbbs.tvt.im/forum.php?mod=forumdisplay&fid=138");
                        break;
                    case R.id.index:
                        wView.loadUrl(index);
                        break;
                    case R.id.login:
                        wView.loadUrl("http://mcbbs.tvt.im/member.php?mod=logging&action=login");
                        break;
                    case R.id.change:
                        if(wView.getSettings().getUserAgentString().contains("Android")){
                            wView.getSettings().setUserAgentString("Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.102 UBrowser/6.1.2716.5 Safari/537.36");
                        }else{
                            wView.getSettings().setUserAgentString("Mozilla/5.0 (Linux; Android 4.4.4; SAMSUNG-SM-N900A Build/tt) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/33.0.0.0 Mobile Safari/537.36");
                        }
                        wView.reload();
                        break;
                }
                return true;
            }
        });



    }



    private static void loadColor(int color,int resColor){
        topbar.setBackgroundColor(color);
        bottomNavigationView.setItemBackgroundResource(resColor);
        navHeader.setBackgroundColor(color);

    }

    private static void loadTheme() {
        String theme=preferences.getString("skin","0");
        if(theme.equalsIgnoreCase("0")){
            loadColor(Color.parseColor("#808080"),R.color.grey);
        }else if(theme.equalsIgnoreCase("1")){
            loadColor(Color.parseColor("#87ceeb"),R.color.skyblue);
        }
    }



    private void bindViews(){
        wView.setScriptStore(sss);
        wView.setWebViewClient(mwvcg);
        wView.setDownloadListener(mdl);
        wView.setWebChromeClient(mwcc);
        wView.getSettings().setSupportZoom(true);
        wView.getSettings().setBuiltInZoomControls(true);
        wView.getSettings().setUseWideViewPort(true);
        wView.setInitialScale(100);
        wView.getSettings().setUseWideViewPort(true);
        wView.getSettings().setLoadWithOverviewMode(true);
        wView.getSettings().setJavaScriptEnabled(true);
        wView.getSettings().setDomStorageEnabled(true);
        wView.getSettings().setLayoutAlgorithm(SINGLE_COLUMN);
        WindowManager wm = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);
        wView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        wView.loadUrl(index);
    }
    @Override
    public void onBackPressed() {

        if(tv.hasFocus()){
            mDrawerLayout.requestFocus();
            return;
        }
        if(mDrawerLayout.isDrawerOpen(GravityCompat.START)){
            mDrawerLayout.closeDrawers();
            return;
        }

        if(wView.canGoBack()){
            wView.goBack();
            return;
        }

        if ((System.currentTimeMillis() - exitTime) > 2000) {
            Toast.makeText(getApplicationContext(), "再按一次退出"+getString(R.string.app_name),
                    Toast.LENGTH_SHORT).show();
            exitTime = System.currentTimeMillis();
        } else {
            finish();
        }

    }
    @Override
    public void onBackClick() {
        wView.goBack();
    }
    @Override
    public void onRightClick() {
        wView.goForward();
    }
}