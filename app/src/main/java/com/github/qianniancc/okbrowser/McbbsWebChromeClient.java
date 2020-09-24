package com.github.qianniancc.okbrowser;

import android.webkit.WebChromeClient;
import android.webkit.WebView;

public class McbbsWebChromeClient extends WebChromeClient {

    private McbbsBrowser mcbbsBrowser;

    /**
     * Constructor.
     *
     * @param mcbbsBrowser
     *            reference to its enclosing ScriptBrowser
     */
    public McbbsWebChromeClient(McbbsBrowser mcbbsBrowser) {
        this.mcbbsBrowser = mcbbsBrowser;
    }

    @Override
    public void onProgressChanged(WebView view, int progress) {
        onReceivedTitle(view,view.getTitle());
    }



    @Override
    public void onReceivedTitle(WebView view, String title) {
        MainActivity.setTitle(title);
    }
}
