package de.maxhenkel.radio.radio;

import de.maxhenkel.radio.Radio;
import de.maxhenkel.radio.RadioVoicechatPlugin;
import de.maxhenkel.voicechat.api.Position;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.audiochannel.AudioPlayer;
import de.maxhenkel.voicechat.api.audiochannel.LocationalAudioChannel;
import de.maxhenkel.voicechat.api.opus.OpusEncoderMode;
import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import javax.annotation.Nullable;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.UUID;
import java.util.function.Supplier;

public class RadioStream implements Supplier<short[]> {

    private final RadioData radioData;
    private final UUID id;
    private final ServerWorld serverLevel;
    private final BlockPos position;
    @Nullable
    private LocationalAudioChannel channel;
    @Nullable
    private AudioPlayer audioPlayer;
    @Nullable
    private Bitstream bitstream;
    @Nullable
    private Decoder decoder;
    @Nullable
    private StreamConverter streamConverter;

    public RadioStream(RadioData radioData, ServerWorld serverLevel, BlockPos position) {
        this.radioData = radioData;
        this.id = radioData.getId();
        this.serverLevel = serverLevel;
        this.position = position;
    }

    public void init() {
        if (radioData.isOn()) {
            start();
        }
    }

    public void start() {
        new Thread(() -> {
            try {
                startInternal();
            } catch (IOException e) {
                Radio.LOGGER.error("Failed to start radio stream", e);
            }
        }, "RadioStreamStarter-%s".formatted(id)).start();

    }

    private void startInternal() throws IOException {
        if (radioData.getUrl() == null) {
            Radio.LOGGER.warn("Radio URL is null");
            return;
        }
        VoicechatServerApi api = RadioVoicechatPlugin.voicechatServerApi;
        if (api == null) {
            Radio.LOGGER.debug("Voice chat API is not yet loaded");
            RadioVoicechatPlugin.runWhenReady(this::start);
            return;
        }
        if (channel != null) {
            stop();
        }
        de.maxhenkel.voicechat.api.ServerLevel level = api.fromServerLevel(serverLevel);
        Position pos = api.createPosition(position.getX() + 0.5D, position.getY() + 0.5D, position.getZ() + 0.5D);
        channel = api.createLocationalAudioChannel(UUID.randomUUID(), level, pos);
        channel.setDistance(Radio.SERVER_CONFIG.radioRange.get().floatValue());
        channel.setCategory(RadioVoicechatPlugin.RADIOS_CATEGORY);
        audioPlayer = api.createAudioPlayer(channel, api.createEncoder(OpusEncoderMode.AUDIO), this);

        bitstream = new Bitstream(new BufferedInputStream(new URL(radioData.getUrl()).openStream()));
        decoder = new Decoder();

        audioPlayer.startPlaying();
    }

    public void stop() {
        channel = null;
        if (audioPlayer != null) {
            audioPlayer.stopPlaying();
            audioPlayer = null;
        }
        if (bitstream != null) {
            try {
                bitstream.close();
            } catch (Exception e) {
                Radio.LOGGER.warn("Failed to close bitstream", e);
            }
            bitstream = null;
        }
        decoder = null;
        streamConverter = null;
        Radio.LOGGER.debug("Stopped radio stream for '{}' ({})", radioData.getStationName(), radioData.getId());
    }

    public BlockPos getPosition() {
        return position;
    }

    public ServerWorld getServerLevel() {
        return serverLevel;
    }

    public RadioData getRadioData() {
        return radioData;
    }

    private int lastSampleCount;

    @Override
    public short[] get() {
        if (channel == null) {
            return null;
        }
        if (bitstream == null || decoder == null) {
            throw new IllegalStateException("Radio stream not started");
        }
        checkValid();
        spawnParticle();
        try {
            if (streamConverter != null) {
                if (!streamConverter.canAdd(lastSampleCount)) {
                    return streamConverter.getFrame();
                }
            }

            Header frameHeader = bitstream.readFrame();
            if (frameHeader == null) {
                throw new IOException("End of stream");
            }

            SampleBuffer output = (SampleBuffer) decoder.decodeFrame(frameHeader, bitstream);
            short[] samples = output.getBuffer();
            lastSampleCount = output.getBufferLength();
            bitstream.closeFrame();

            if (streamConverter == null) {
                streamConverter = new StreamConverter(decoder.getOutputFrequency(), decoder.getOutputChannels());
            }

            streamConverter.add(samples, 0, output.getBufferLength());
            return streamConverter.getFrame();
        } catch (Exception e) {
            Radio.LOGGER.warn("Failed to stream audio from {}", radioData.getUrl(), e);
            stop();
            return null;
        }
    }

    private long lastParticle = 0L;

    public void spawnParticle() {
        if (!Radio.SERVER_CONFIG.showMusicParticles.get()) {
            return;
        }
        long time = System.currentTimeMillis();
        if (time - lastParticle < Radio.SERVER_CONFIG.musicParticleFrequency.get()) {
            return;
        }
        lastParticle = time;
        serverLevel.getServer().execute(() -> {
            Vec3d vec3 = Vec3d.ofBottomCenter(position).add(0D, 1D, 0D);
            serverLevel.getPlayers().stream().filter(player -> player.getPos().distanceTo(position.getCenter()) <= 32D).forEach(player -> {
                float random = (float) serverLevel.getRandom().nextInt(4) / 24F;
                serverLevel.spawnParticles(ParticleTypes.NOTE, vec3.getX(), vec3.getY(), vec3.getZ(), 0, random, 0D, 0D, 1D);
            });
        });
    }

    private long lastCheck;

    private void checkValid() {
        long time = System.currentTimeMillis();
        if (time - lastCheck < 30000L) {
            return;
        }
        lastCheck = time;
        serverLevel.getServer().execute(() -> {
            if (!RadioManager.isValidRadioLocation(id, position, serverLevel)) {
                RadioManager.getInstance().stopStream(id);
                Radio.LOGGER.warn("Stopped radio stream {} as it doesn't exist anymore", id);
            }
        });
    }

    public void close() {
        stop();
    }
}
