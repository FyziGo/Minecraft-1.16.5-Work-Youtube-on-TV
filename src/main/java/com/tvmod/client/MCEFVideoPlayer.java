package com.tvmod.client;

import com.tvmod.TVMod;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.lang.reflect.Method;

/**
 * Video player using MCEF (Minecraft Chromium Embedded Framework).
 * Uses embedded Chromium browser to play YouTube videos.
 */
@OnlyIn(Dist.CLIENT)
public class MCEFVideoPlayer {

    private final BlockPos pos;
    private Object browser = null;
    private boolean mcefAvailable = false;

    private String currentUrl = "";
    private boolean isPlaying = false;
    private float volume = 1.0f;
    private String quality = "medium";
    private int sourceIndex = 0;
    private float playbackSpeed = 1.0f;

    public static final float[] SPEED_OPTIONS = {0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f};

    public static final String[] VIDEO_SOURCES = {
        "YouTube", "Invidious 1", "Invidious 2", "Invidious 3", "Invidious 4", "Direct URL"
    };

    private static final String[] SOURCE_URLS = {
        "https://www.youtube.com/embed/",
        "https://yewtu.be/embed/",
        "https://inv.nadeko.net/embed/",
        "https://invidious.nerdvpn.de/embed/",
        "https://inv.tux.pizza/embed/",
        ""
    };

    private int textureId = -1;
    private int textureWidth = 854;
    private int textureHeight = 480;

    private java.util.function.Consumer<String> urlChangeListener;
    private java.util.function.Consumer<String> titleChangeListener;
    private java.util.function.Consumer<Boolean> loadingStateListener;

    private String pendingUrl = null;
    private int browserInitTicks = 0;
    private static final int BROWSER_INIT_DELAY = 10;

    public MCEFVideoPlayer(BlockPos pos) {
        this.pos = pos;
        checkMCEFAvailability();
    }

    private void checkMCEFAvailability() {
        try {
            Class.forName("net.montoyo.mcef.api.API");
            mcefAvailable = true;
            TVMod.LOGGER.info("MCEF detected at {}", pos);
        } catch (ClassNotFoundException e) {
            try {
                Class.forName("com.cinemamod.mcef.MCEF");
                mcefAvailable = true;
                TVMod.LOGGER.info("CinemaMod MCEF detected at {}", pos);
            } catch (ClassNotFoundException e2) {
                mcefAvailable = false;
                TVMod.LOGGER.warn("MCEF not found at {} - browser playback unavailable", pos);
            }
        }
    }

    private void initBrowser() {
        if (!mcefAvailable || browser != null) return;

        try {
            Class<?> mcefApiClass = Class.forName("net.montoyo.mcef.api.MCEFApi");
            Method isLoadedMethod = mcefApiClass.getMethod("isMCEFLoaded");
            boolean isLoaded = (Boolean) isLoadedMethod.invoke(null);

            if (!isLoaded) {
                TVMod.LOGGER.error("MCEF is not loaded yet");
                mcefAvailable = false;
                return;
            }

            Method getApiMethod = mcefApiClass.getMethod("getAPI");
            Object api = getApiMethod.invoke(null);

            if (api == null) {
                TVMod.LOGGER.error("MCEFApi.getAPI() returned null");
                mcefAvailable = false;
                return;
            }

            Method createBrowserMethod = api.getClass().getMethod("createBrowser", String.class, boolean.class);
            browser = createBrowserMethod.invoke(api, "about:blank", true);

            if (browser == null) {
                TVMod.LOGGER.error("createBrowser returned null");
                mcefAvailable = false;
                return;
            }

            Method resizeMethod = browser.getClass().getMethod("resize", int.class, int.class);
            resizeMethod.invoke(browser, textureWidth, textureHeight);
            TVMod.LOGGER.info("MCEF browser created and resized to {}x{}", textureWidth, textureHeight);

        } catch (Exception e) {
            TVMod.LOGGER.error("Failed to initialize MCEF browser: {}", e.getMessage());
            mcefAvailable = false;
        }
    }

    public void play(String url) { play(url, this.quality); }
    public void play(String url, String quality) { play(url, quality, this.sourceIndex); }

    public void play(String url, String quality, int sourceIndex) {
        if (url == null || url.isEmpty()) {
            TVMod.LOGGER.warn("Cannot play empty URL at {}", pos);
            return;
        }

        this.currentUrl = url;
        this.quality = quality != null ? quality : "medium";
        this.sourceIndex = sourceIndex;
        this.isPlaying = true;

        if (mcefAvailable) {
            initBrowser();
            String embedUrl = convertToEmbedUrl(url);

            if (browser != null) {
                loadUrlInternal(embedUrl);
            } else {
                this.pendingUrl = embedUrl;
                this.browserInitTicks = 0;
            }
        }
    }

    private void loadUrlInternal(String url) {
        if (browser == null || url == null || url.isEmpty()) return;

        try {
            Method loadUrlMethod = browser.getClass().getMethod("loadURL", String.class);
            loadUrlMethod.invoke(browser, url);
            TVMod.LOGGER.info("MCEF loading URL: {} at {}", url, pos);
        } catch (Exception e) {
            TVMod.LOGGER.error("Failed to load URL in MCEF: {}", e.getMessage());
        }
    }

    private String convertToEmbedUrl(String url) {
        if (sourceIndex == 5) return url;

        String videoId = extractVideoId(url);
        if (videoId != null) {
            String baseUrl = SOURCE_URLS[sourceIndex];
            if (sourceIndex == 0) {
                return baseUrl + videoId + "?autoplay=1";
            }
            return baseUrl + videoId + "?autoplay=1&local=true&quality=" + quality;
        }
        return url;
    }

    public void setSourceIndex(int index) {
        if (index >= 0 && index < VIDEO_SOURCES.length) {
            this.sourceIndex = index;
        }
    }

    public int getSourceIndex() { return sourceIndex; }
    public static String getSourceName(int index) {
        if (index >= 0 && index < VIDEO_SOURCES.length) return VIDEO_SOURCES[index];
        return "Unknown";
    }

    public void setQuality(String quality) { this.quality = quality != null ? quality : "medium"; }
    public String getQuality() { return quality; }

    public void setPlaybackSpeed(float speed) {
        this.playbackSpeed = speed;
        if (browser != null && isPlaying) {
            try {
                executeJavaScript("if(document.querySelector('video')) document.querySelector('video').playbackRate = " + speed + ";");
            } catch (Exception e) {}
        }
    }

    public float getPlaybackSpeed() { return playbackSpeed; }
    public static String getSpeedName(float speed) {
        if (speed == 1.0f) return "1x";
        if (speed == (int) speed) return (int) speed + "x";
        return speed + "x";
    }

    private String extractVideoId(String url) {
        int vIndex = url.indexOf("v=");
        if (vIndex != -1) {
            int endIndex = url.indexOf("&", vIndex);
            if (endIndex == -1) endIndex = url.length();
            return url.substring(vIndex + 2, Math.min(vIndex + 13, endIndex));
        }

        if (url.contains("youtu.be/")) {
            int startIndex = url.indexOf("youtu.be/") + 9;
            int endIndex = url.indexOf("?", startIndex);
            if (endIndex == -1) endIndex = url.length();
            return url.substring(startIndex, Math.min(startIndex + 11, endIndex));
        }

        if (url.contains("/embed/")) {
            int startIndex = url.indexOf("/embed/") + 7;
            int endIndex = url.indexOf("?", startIndex);
            if (endIndex == -1) endIndex = url.length();
            return url.substring(startIndex, Math.min(startIndex + 11, endIndex));
        }

        return null;
    }

    public void pause() {
        if (!isPlaying) return;
        this.isPlaying = false;
        if (browser != null) {
            try { executeJavaScript("document.querySelector('video').pause();"); } catch (Exception e) {}
        }
    }

    public void resume() {
        if (isPlaying || currentUrl.isEmpty()) return;
        this.isPlaying = true;
        if (browser != null) {
            try { executeJavaScript("document.querySelector('video').play();"); } catch (Exception e) {}
        }
    }

    public void stop() {
        this.isPlaying = false;
        if (browser != null) {
            try {
                Method loadUrlMethod = browser.getClass().getMethod("loadURL", String.class);
                loadUrlMethod.invoke(browser, "about:blank");
            } catch (Exception e) {}
        }
    }

    public void setVolume(float volume) {
        this.volume = Math.max(0.0f, Math.min(1.0f, volume));
        if (browser != null) {
            try { executeJavaScript("document.querySelector('video').volume = " + this.volume + ";"); } catch (Exception e) {}
        }
    }

    private void executeJavaScript(String script) {
        if (browser == null) return;
        try {
            Method runJsMethod = browser.getClass().getMethod("runJS", String.class, String.class);
            runJsMethod.invoke(browser, script, "");
        } catch (NoSuchMethodException e) {
            try {
                Method execJsMethod = browser.getClass().getMethod("executeJavaScript", String.class);
                execJsMethod.invoke(browser, script);
            } catch (Exception e2) {}
        } catch (Exception e) {}
    }

    public int getTextureId() {
        if (browser == null) return -1;
        try {
            Method getTextureMethod = browser.getClass().getMethod("getTextureID");
            Object result = getTextureMethod.invoke(browser);
            if (result instanceof Integer) return (Integer) result;
        } catch (Exception e) {}
        return -1;
    }

    public ResourceLocation getTextureLocationAsResource() {
        if (browser == null) return null;
        try {
            Method getTexLocMethod = browser.getClass().getMethod("getTextureLocation");
            Object result = getTexLocMethod.invoke(browser);
            if (result instanceof ResourceLocation) return (ResourceLocation) result;
        } catch (Exception e) {}
        return null;
    }

    public Object getTextureLocation() {
        if (browser == null) return null;
        try {
            Method getTexLocMethod = browser.getClass().getMethod("getTextureLocation");
            return getTexLocMethod.invoke(browser);
        } catch (Exception e) {}
        return null;
    }

    public void tick(PlayerEntity player) {
        if (pendingUrl != null && mcefAvailable) {
            browserInitTicks++;
            if (browser == null) initBrowser();

            if (browser != null && browserInitTicks >= BROWSER_INIT_DELAY) {
                loadUrlInternal(pendingUrl);
                pendingUrl = null;
                browserInitTicks = 0;
            } else if (browserInitTicks > BROWSER_INIT_DELAY * 3) {
                pendingUrl = null;
                browserInitTicks = 0;
            }
        }

        if (player != null && isPlaying) {
            double distance = Math.sqrt(player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
            float spatialVolume = VolumeCalculator.calculateVolume(distance, volume);

            if (browser != null && distance <= VolumeCalculator.MAX_DISTANCE) {
                try { executeJavaScript("if(document.querySelector('video')) document.querySelector('video').volume = " + spatialVolume + ";"); } catch (Exception e) {}
            }
        }
    }

    public void release() {
        TVMod.LOGGER.info("Releasing MCEF browser at {}", pos);

        if (browser != null) {
            try {
                Method loadUrlMethod = browser.getClass().getMethod("loadURL", String.class);
                loadUrlMethod.invoke(browser, "about:blank");
            } catch (Exception e) {}

            try {
                Method closeMethod = browser.getClass().getMethod("close");
                closeMethod.invoke(browser);
            } catch (Exception e) {}
            browser = null;
        }

        isPlaying = false;
        currentUrl = "";
        pendingUrl = null;
        mcefAvailable = false;
    }

    // Navigation Methods
    public void goBack() {
        if (browser == null) return;
        try {
            Method method = browser.getClass().getMethod("goBack");
            method.invoke(browser);
        } catch (NoSuchMethodException e) {
            executeJavaScript("history.back();");
        } catch (Exception e) {}
    }

    public void goForward() {
        if (browser == null) return;
        try {
            Method method = browser.getClass().getMethod("goForward");
            method.invoke(browser);
        } catch (NoSuchMethodException e) {
            executeJavaScript("history.forward();");
        } catch (Exception e) {}
    }

    public void reload() {
        if (browser == null) return;
        try {
            Method method = browser.getClass().getMethod("reload");
            method.invoke(browser);
        } catch (NoSuchMethodException e) {
            executeJavaScript("location.reload();");
        } catch (Exception e) {}
    }

    public void loadUrl(String url) {
        if (browser == null || url == null || url.isEmpty()) return;
        this.currentUrl = url;
        try {
            Method method = browser.getClass().getMethod("loadURL", String.class);
            method.invoke(browser, url);
        } catch (Exception e) {}
    }

    public String getPageTitle() {
        if (browser == null) return "";
        try {
            Method method = browser.getClass().getMethod("getTitle");
            Object result = method.invoke(browser);
            if (result instanceof String) return (String) result;
        } catch (Exception e) {}
        return "";
    }

    public boolean isLoading() {
        if (browser == null) return false;
        try {
            Method method = browser.getClass().getMethod("isLoading");
            Object result = method.invoke(browser);
            if (result instanceof Boolean) return (Boolean) result;
        } catch (Exception e) {}
        return false;
    }

    public boolean canGoBack() {
        if (browser == null) return false;
        try {
            Method method = browser.getClass().getMethod("canGoBack");
            Object result = method.invoke(browser);
            if (result instanceof Boolean) return (Boolean) result;
        } catch (Exception e) {}
        return false;
    }

    public boolean canGoForward() {
        if (browser == null) return false;
        try {
            Method method = browser.getClass().getMethod("canGoForward");
            Object result = method.invoke(browser);
            if (result instanceof Boolean) return (Boolean) result;
        } catch (Exception e) {}
        return false;
    }

    public Object getBrowser() { return browser; }

    public void ensureBrowserInitialized() {
        if (mcefAvailable && browser == null) initBrowser();
    }

    // Input Methods
    public void sendKeyEvent(int keyCode, int scanCode, int modifiers, boolean pressed) {
        if (browser == null) return;
        try {
            if (pressed) {
                Method method = browser.getClass().getMethod("sendKeyPress", int.class, long.class, int.class);
                method.invoke(browser, keyCode, (long) scanCode, modifiers);
            } else {
                Method method = browser.getClass().getMethod("sendKeyRelease", int.class, long.class, int.class);
                method.invoke(browser, keyCode, (long) scanCode, modifiers);
            }
        } catch (NoSuchMethodException e) {
            try {
                String methodName = pressed ? "injectKeyPressed" : "injectKeyReleased";
                Method method = browser.getClass().getMethod(methodName, int.class, int.class);
                method.invoke(browser, keyCode, modifiers);
            } catch (Exception e2) {}
        } catch (Exception e) {}
    }

    public void sendCharEvent(char character, int modifiers) {
        if (browser == null) return;
        try {
            Method method = browser.getClass().getMethod("sendKeyTyped", char.class, int.class);
            method.invoke(browser, character, modifiers);
        } catch (NoSuchMethodException e) {
            try {
                Method method = browser.getClass().getMethod("injectKeyTyped", char.class, int.class);
                method.invoke(browser, character, modifiers);
            } catch (Exception e2) {}
        } catch (Exception e) {}
    }

    public void sendMouseMoveEvent(int x, int y) {
        if (browser == null) return;
        try {
            Method method = browser.getClass().getMethod("sendMouseMove", int.class, int.class);
            method.invoke(browser, x, y);
        } catch (NoSuchMethodException e) {
            try {
                Method method = browser.getClass().getMethod("injectMouseMove", int.class, int.class, int.class, boolean.class);
                method.invoke(browser, x, y, 0, false);
            } catch (Exception e2) {}
        } catch (Exception e) {}
    }

    public void sendMouseClickEvent(int x, int y, int button, boolean pressed) {
        if (browser == null) return;
        try {
            String methodName = pressed ? "sendMousePress" : "sendMouseRelease";
            Method method = browser.getClass().getMethod(methodName, int.class, int.class, int.class);
            method.invoke(browser, x, y, button);
        } catch (NoSuchMethodException e) {
            try {
                Method method = browser.getClass().getMethod("injectMouseButton", int.class, int.class, int.class, boolean.class, int.class);
                method.invoke(browser, x, y, 0, pressed, button);
            } catch (Exception e2) {}
        } catch (Exception e) {}
    }

    public void sendMouseScrollEvent(int x, int y, double deltaX, double deltaY) {
        if (browser == null) return;
        int scrollX = (int) (deltaX * 120);
        int scrollY = (int) (deltaY * 120);
        try {
            Method method = browser.getClass().getMethod("sendMouseWheel", int.class, int.class, int.class, int.class);
            method.invoke(browser, x, y, scrollX, scrollY);
        } catch (NoSuchMethodException e) {
            try {
                Method method = browser.getClass().getMethod("injectMouseWheel", int.class, int.class, int.class, int.class);
                method.invoke(browser, x, y, 0, scrollY);
            } catch (Exception e2) {}
        } catch (Exception e) {}
    }

    // Callback Listeners
    public void setUrlChangeListener(java.util.function.Consumer<String> listener) { this.urlChangeListener = listener; }
    public void setTitleChangeListener(java.util.function.Consumer<String> listener) { this.titleChangeListener = listener; }
    public void setLoadingStateListener(java.util.function.Consumer<Boolean> listener) { this.loadingStateListener = listener; }

    protected void notifyUrlChange(String newUrl) {
        this.currentUrl = newUrl;
        if (urlChangeListener != null) urlChangeListener.accept(newUrl);
    }

    protected void notifyTitleChange(String newTitle) {
        if (titleChangeListener != null) titleChangeListener.accept(newTitle);
    }

    protected void notifyLoadingStateChange(boolean isLoading) {
        if (loadingStateListener != null) loadingStateListener.accept(isLoading);
    }

    // Getters
    public boolean isPlaying() { return isPlaying; }
    public boolean isMCEFAvailable() { return mcefAvailable; }
    public String getCurrentUrl() { return currentUrl; }
    public float getVolume() { return volume; }
    public BlockPos getPos() { return pos; }
    public int getTextureWidth() { return textureWidth; }
    public int getTextureHeight() { return textureHeight; }
}
