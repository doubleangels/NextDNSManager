package com.doubleangels.nextdnsmanagement.webview;

/**
 * JavaScript injected after each page load to coordinate swipe-refresh with
 * in-page horizontal tab scrolling and modal dialogs.
 */
public final class WebViewInteractionScript {

    private WebViewInteractionScript() {
    }

    public static final String PAGE_FINISHED_SCRIPT =
            "(function() {" +
            "   function disableSwipeRefresh() {" +
            "       if (typeof AndroidInterface !== 'undefined') {" +
            "           AndroidInterface.setSwipeRefreshEnabled(false);" +
            "       }" +
            "   }" +
            "   function enableSwipeRefresh() {" +
            "       if (typeof AndroidInterface !== 'undefined') {" +
            "           AndroidInterface.setSwipeRefreshEnabled(true);" +
            "       }" +
            "   }" +
            "   function attachSwipeRefreshGuard(element) {" +
            "       if (!element || element.getAttribute('data-swipe-refresh-guard')) return;" +
            "       element.setAttribute('data-swipe-refresh-guard', 'true');" +
            "       element.addEventListener('touchstart', disableSwipeRefresh, {passive: true});" +
            "       element.addEventListener('touchend', enableSwipeRefresh, {passive: true});" +
            "       element.addEventListener('touchcancel', enableSwipeRefresh, {passive: true});" +
            "   }" +
            "   function setupInteractionGuards() {" +
            "       document.querySelectorAll('.modal-dialog.modal-lg.modal-dialog-scrollable')" +
            "           .forEach(attachSwipeRefreshGuard);" +
            "       document.querySelectorAll('nav[role=\"tablist\"], .nav-tabs, .nav')" +
            "           .forEach(attachSwipeRefreshGuard);" +
            "       var accountMenu = document.querySelector('.account-menu, .equipment-menu');" +
            "       if (accountMenu) {" +
            "           accountMenu.style.position = 'relative';" +
            "           accountMenu.style.zIndex = '1000';" +
            "       }" +
            "       document.querySelectorAll('.nav-item').forEach(function(navItem) {" +
            "           if (!navItem.getAttribute('data-nav-flex')) {" +
            "               navItem.setAttribute('data-nav-flex', 'true');" +
            "               navItem.style.flexShrink = '0';" +
            "           }" +
            "       });" +
            "   }" +
            "   setupInteractionGuards();" +
            // Coalesce mutation bursts into one pass per frame. The dashboard's live
            // query log mutates the DOM constantly, and re-running querySelectorAll
            // on every mutation record wastes CPU and battery.
            "   var guardsScheduled = false;" +
            "   function scheduleInteractionGuards() {" +
            "       if (guardsScheduled) return;" +
            "       guardsScheduled = true;" +
            "       requestAnimationFrame(function() {" +
            "           guardsScheduled = false;" +
            "           setupInteractionGuards();" +
            "       });" +
            "   }" +
            "   if (!window.__ndmsInteractionObserver) {" +
            "       window.__ndmsInteractionObserver = new MutationObserver(scheduleInteractionGuards);" +
            "       window.__ndmsInteractionObserver.observe(document.body, {childList: true, subtree: true});" +
            "   }" +
            "   window.__ndmsDisconnectInteractionObserver = function() {" +
            "       if (window.__ndmsInteractionObserver) {" +
            "           window.__ndmsInteractionObserver.disconnect();" +
            "           window.__ndmsInteractionObserver = null;" +
            "       }" +
            "   };" +
            "})();";

    public static final String DISCONNECT_SCRIPT =
            "(function(){if(window.__ndmsDisconnectInteractionObserver)window.__ndmsDisconnectInteractionObserver();})();";
}
