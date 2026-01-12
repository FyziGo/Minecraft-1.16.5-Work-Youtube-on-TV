package com.tvmod.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.tvmod.TVMod;
import com.tvmod.client.MCEFVideoPlayer;
import com.tvmod.client.TVVideoPlayer;
import com.tvmod.client.VideoPlayerManager;
import com.tvmod.network.NetworkHandler;
import com.tvmod.network.TVControlPacket;
import com.tvmod.tileentity.TVTileEntity;
import com.tvmod.util.URLParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TVScreen extends Screen {

    private static final int GUI_WIDTH = 280;
    private static final int GUI_HEIGHT = 260;

    private final TVTileEntity tileEntity;

    private TextFieldWidget urlField;
    private Button playButton, pauseButton, stopButton;
    private Button volumeUpButton, volumeDownButton;
    private Button[] sizeButtons, qualityButtons, sourceButtons, speedButtons;
    private Button clearUrlButton, browserButton;

    private static final String[] QUALITY_OPTIONS = {"low", "medium", "high", "dash"};
    private int currentQualityIndex = 1;
    private int currentSourceIndex = 1;
    private int currentSpeedIndex = 3;

    private String errorMessage = "";
    private int errorMessageTimer = 0;

    public TVScreen(TVTileEntity tileEntity) {
        super(new TranslationTextComponent("gui.tvmod.tv_screen"));
        this.tileEntity = tileEntity;
        this.currentQualityIndex = tileEntity.getQualityIndex();
        this.currentSourceIndex = tileEntity.getSourceIndex();
        this.currentSpeedIndex = tileEntity.getSpeedIndex();
    }

    @Override
    protected void init() {
        super.init();

        int guiLeft = (this.width - GUI_WIDTH) / 2;
        int guiTop = (this.height - GUI_HEIGHT) / 2;

        this.urlField = new TextFieldWidget(this.font, guiLeft + 10, guiTop + 20, GUI_WIDTH - 50, 20, new StringTextComponent("URL"));
        this.urlField.setMaxLength(256);
        this.urlField.setValue(tileEntity.getVideoUrl());
        this.urlField.setResponder(this::onUrlChanged);
        this.children.add(this.urlField);

        this.clearUrlButton = new Button(guiLeft + GUI_WIDTH - 35, guiTop + 20, 25, 20, new StringTextComponent("X"), this::onClearUrlPressed);
        this.addButton(this.clearUrlButton);

        this.playButton = new Button(guiLeft + 10, guiTop + 50, 50, 20, new StringTextComponent("Play"), this::onPlayPressed);
        this.addButton(this.playButton);

        this.pauseButton = new Button(guiLeft + 65, guiTop + 50, 50, 20, new StringTextComponent("Pause"), this::onPausePressed);
        this.addButton(this.pauseButton);

        this.stopButton = new Button(guiLeft + 120, guiTop + 50, 50, 20, new StringTextComponent("Stop"), this::onStopPressed);
        this.addButton(this.stopButton);

        this.volumeDownButton = new Button(guiLeft + 10, guiTop + 80, 30, 20, new StringTextComponent("-"), this::onVolumeDownPressed);
        this.addButton(this.volumeDownButton);

        this.volumeUpButton = new Button(guiLeft + 45, guiTop + 80, 30, 20, new StringTextComponent("+"), this::onVolumeUpPressed);
        this.addButton(this.volumeUpButton);

        int[] sizes = {1, 2, 4, 6, 8, 10, 12};
        this.sizeButtons = new Button[sizes.length];
        int sizeButtonX = guiLeft + 180;
        int sizeButtonY = guiTop + 80;

        for (int i = 0; i < sizes.length; i++) {
            final int size = sizes[i];
            this.sizeButtons[i] = new Button(sizeButtonX + (i % 4) * 25, sizeButtonY + (i / 4) * 22, 23, 20, new StringTextComponent(size + ""), button -> onSizePressed(size));
            this.addButton(this.sizeButtons[i]);
        }

        this.qualityButtons = new Button[QUALITY_OPTIONS.length];
        int qualityButtonX = guiLeft + 10;
        int qualityButtonY = guiTop + 130;

        for (int i = 0; i < QUALITY_OPTIONS.length; i++) {
            final int qualityIndex = i;
            this.qualityButtons[i] = new Button(qualityButtonX + i * 55, qualityButtonY, 50, 18, new StringTextComponent(QUALITY_OPTIONS[i]), button -> onQualityPressed(qualityIndex));
            this.addButton(this.qualityButtons[i]);
        }

        String[] sourceNames = MCEFVideoPlayer.VIDEO_SOURCES;
        this.sourceButtons = new Button[sourceNames.length];
        int sourceButtonY1 = guiTop + 165;
        int sourceButtonY2 = guiTop + 185;

        for (int i = 0; i < sourceNames.length; i++) {
            final int sourceIndex = i;
            int row = i / 3;
            int col = i % 3;
            int buttonY = row == 0 ? sourceButtonY1 : sourceButtonY2;
            String shortName = getShortSourceName(sourceNames[i]);
            this.sourceButtons[i] = new Button(guiLeft + 10 + col * 88, buttonY, 83, 18, new StringTextComponent(shortName), button -> onSourcePressed(sourceIndex));
            this.addButton(this.sourceButtons[i]);
        }

        float[] speedOptions = MCEFVideoPlayer.SPEED_OPTIONS;
        this.speedButtons = new Button[speedOptions.length];
        int speedButtonY = guiTop + 220;
        int speedButtonWidth = 36;

        for (int i = 0; i < speedOptions.length; i++) {
            final int speedIndex = i;
            String speedLabel = MCEFVideoPlayer.getSpeedName(speedOptions[i]);
            this.speedButtons[i] = new Button(guiLeft + 10 + i * (speedButtonWidth + 2), speedButtonY, speedButtonWidth, 18, new StringTextComponent(speedLabel), button -> onSpeedPressed(speedIndex));
            this.addButton(this.speedButtons[i]);
        }

        this.browserButton = new Button(guiLeft + 180, guiTop + 50, 80, 20, new StringTextComponent("Browser"), this::onBrowserPressed);
        this.addButton(this.browserButton);

        updateButtonStates();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.urlField != null) this.urlField.tick();
        if (errorMessageTimer > 0) {
            errorMessageTimer--;
            if (errorMessageTimer == 0) errorMessage = "";
        }
        updateButtonStates();
    }

    private void updateButtonStates() {
        boolean hasUrl = urlField != null && !urlField.getValue().trim().isEmpty();
        boolean isPlaying = tileEntity.isPlaying();

        if (playButton != null) playButton.active = hasUrl && !isPlaying;
        if (pauseButton != null) pauseButton.active = isPlaying;
        if (stopButton != null) stopButton.active = hasUrl;

        if (sizeButtons != null) {
            int currentSize = tileEntity.getScreenSize();
            int[] sizes = {1, 2, 4, 6, 8, 10, 12};
            for (int i = 0; i < sizeButtons.length; i++) {
                if (sizeButtons[i] != null) {
                    boolean isSelected = sizes[i] == currentSize;
                    sizeButtons[i].setMessage(new StringTextComponent(isSelected ? "[" + sizes[i] + "]" : sizes[i] + ""));
                }
            }
        }

        if (qualityButtons != null) {
            for (int i = 0; i < qualityButtons.length; i++) {
                if (qualityButtons[i] != null) {
                    boolean isSelected = i == currentQualityIndex;
                    qualityButtons[i].setMessage(new StringTextComponent(isSelected ? "[" + QUALITY_OPTIONS[i] + "]" : QUALITY_OPTIONS[i]));
                }
            }
        }

        if (sourceButtons != null) {
            String[] sourceNames = MCEFVideoPlayer.VIDEO_SOURCES;
            for (int i = 0; i < sourceButtons.length; i++) {
                if (sourceButtons[i] != null) {
                    boolean isSelected = i == currentSourceIndex;
                    String shortName = getShortSourceName(sourceNames[i]);
                    sourceButtons[i].setMessage(new StringTextComponent(isSelected ? "[" + shortName + "]" : shortName));
                }
            }
        }

        if (speedButtons != null) {
            float[] speedOptions = MCEFVideoPlayer.SPEED_OPTIONS;
            for (int i = 0; i < speedButtons.length; i++) {
                if (speedButtons[i] != null) {
                    boolean isSelected = i == currentSpeedIndex;
                    String speedLabel = MCEFVideoPlayer.getSpeedName(speedOptions[i]);
                    speedButtons[i].setMessage(new StringTextComponent(isSelected ? "[" + speedLabel + "]" : speedLabel));
                }
            }
        }
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(matrixStack);

        int guiLeft = (this.width - GUI_WIDTH) / 2;
        int guiTop = (this.height - GUI_HEIGHT) / 2;

        fill(matrixStack, guiLeft, guiTop, guiLeft + GUI_WIDTH, guiTop + GUI_HEIGHT, 0xCC000000);
        drawCenteredString(matrixStack, this.font, this.title, this.width / 2, guiTop + 6, 0xFFFFFF);
        drawString(matrixStack, this.font, "Video URL:", guiLeft + 10, guiTop + 10, 0xAAAAAA);

        if (this.urlField != null) this.urlField.render(matrixStack, mouseX, mouseY, partialTicks);

        String volumeText = String.format("Volume: %d%%", (int) (tileEntity.getVolume() * 100));
        drawString(matrixStack, this.font, volumeText, guiLeft + 80, guiTop + 85, 0xFFFFFF);

        String statusText = tileEntity.isPlaying() ? "Playing" : "Stopped";
        drawString(matrixStack, this.font, "Status: " + statusText, guiLeft + 180, guiTop + 55, 0xAAAAAA);

        drawString(matrixStack, this.font, "Quality:", guiLeft + 10, guiTop + 120, 0xAAAAAA);
        drawString(matrixStack, this.font, "Size:", guiLeft + 180, guiTop + 70, 0xAAAAAA);
        drawString(matrixStack, this.font, "Source:", guiLeft + 10, guiTop + 155, 0xAAAAAA);
        drawString(matrixStack, this.font, "Speed:", guiLeft + 10, guiTop + 210, 0xAAAAAA);

        if (!errorMessage.isEmpty()) {
            drawCenteredString(matrixStack, this.font, errorMessage, this.width / 2, guiTop + GUI_HEIGHT - 15, 0xFF5555);
        }

        super.render(matrixStack, mouseX, mouseY, partialTicks);
    }

    private void onUrlChanged(String newUrl) {
        if (errorMessageTimer > 0) {
            errorMessage = "";
            errorMessageTimer = 0;
        }
    }

    private void onPlayPressed(Button button) {
        String url = urlField.getValue().trim();
        if (url.isEmpty()) { showError("Please enter a URL"); return; }
        if (!URLParser.isValidUrl(url)) { showError("Invalid URL format"); return; }

        String normalizedUrl = URLParser.normalizeUrl(url);
        tileEntity.setVideoUrl(normalizedUrl);
        tileEntity.play();

        String quality = tileEntity.getQuality();
        int sourceIdx = tileEntity.getSourceIndex();
        float speed = tileEntity.getSpeed();

        TVVideoPlayer player = VideoPlayerManager.getOrCreate(tileEntity.getBlockPos());
        player.play(normalizedUrl, quality, sourceIdx);
        player.setPlaybackSpeed(speed);

        NetworkHandler.sendToServer(TVControlPacket.setUrl(tileEntity.getBlockPos(), normalizedUrl));
        NetworkHandler.sendToServer(TVControlPacket.play(tileEntity.getBlockPos()));
    }

    private void onPausePressed(Button button) {
        tileEntity.pause();
        TVVideoPlayer player = VideoPlayerManager.get(tileEntity.getBlockPos());
        if (player != null) player.pause();
        NetworkHandler.sendToServer(TVControlPacket.pause(tileEntity.getBlockPos()));
    }

    private void onStopPressed(Button button) {
        tileEntity.stop();
        TVVideoPlayer player = VideoPlayerManager.get(tileEntity.getBlockPos());
        if (player != null) player.stop();
        NetworkHandler.sendToServer(TVControlPacket.stop(tileEntity.getBlockPos()));
    }

    private void onSizePressed(int size) {
        tileEntity.setScreenSize(size);
        tileEntity.syncToClients();
        updateButtonStates();
    }

    private void onQualityPressed(int qualityIndex) {
        this.currentQualityIndex = qualityIndex;
        tileEntity.setQualityIndex(qualityIndex);
        tileEntity.syncToClients();
        updateButtonStates();
    }

    private void onSourcePressed(int sourceIndex) {
        this.currentSourceIndex = sourceIndex;
        tileEntity.setSourceIndex(sourceIndex);
        tileEntity.syncToClients();
        updateButtonStates();
    }

    private void onSpeedPressed(int speedIndex) {
        this.currentSpeedIndex = speedIndex;
        tileEntity.setSpeedIndex(speedIndex);
        tileEntity.syncToClients();
        float speed = MCEFVideoPlayer.SPEED_OPTIONS[speedIndex];

        TVVideoPlayer player = VideoPlayerManager.get(tileEntity.getBlockPos());
        if (player != null) player.setPlaybackSpeed(speed);
        updateButtonStates();
    }

    private String getShortSourceName(String fullName) {
        if (fullName.equals("YouTube")) return "YouTube";
        if (fullName.equals("Direct URL")) return "Direct";
        if (fullName.startsWith("Invidious")) return fullName.replace("Invidious ", "Inv");
        return fullName;
    }

    private void onClearUrlPressed(Button button) {
        if (urlField != null) {
            urlField.setValue("");
            urlField.setFocus(true);
        }
    }

    private void onBrowserPressed(Button button) {
        TVVideoPlayer player = VideoPlayerManager.get(tileEntity.getBlockPos());
        if (player != null && player.isMCEFAvailable()) {
            MCEFVideoPlayer mcefPlayer = player.getMCEFPlayer();
            if (mcefPlayer != null) {
                Minecraft.getInstance().setScreen(new BrowserScreen(tileEntity, mcefPlayer));
            } else {
                showError("Browser not initialized");
            }
        } else {
            showError("MCEF not available");
        }
    }

    public String getCurrentQuality() { return QUALITY_OPTIONS[currentQualityIndex]; }

    private void onVolumeDownPressed(Button button) {
        float newVolume = Math.max(0.0f, tileEntity.getVolume() - 0.1f);
        sendVolumeChange(newVolume);
    }

    private void onVolumeUpPressed(Button button) {
        float newVolume = Math.min(1.0f, tileEntity.getVolume() + 0.1f);
        sendVolumeChange(newVolume);
    }

    private void sendVolumeChange(float newVolume) {
        NetworkHandler.sendToServer(TVControlPacket.setVolume(tileEntity.getBlockPos(), newVolume));
        tileEntity.setVolume(newVolume);
    }

    private void showError(String message) {
        this.errorMessage = message;
        this.errorMessageTimer = 60;
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.urlField != null && this.urlField.isFocused()) {
            if (keyCode == 257) {
                if (!urlField.getValue().trim().isEmpty()) onPlayPressed(playButton);
                return true;
            }
            return this.urlField.keyPressed(keyCode, scanCode, modifiers);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.urlField != null && this.urlField.isFocused()) return this.urlField.charTyped(codePoint, modifiers);
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.urlField != null) this.urlField.mouseClicked(mouseX, mouseY, button);
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
