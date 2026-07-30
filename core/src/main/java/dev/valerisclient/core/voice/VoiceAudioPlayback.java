package dev.valerisclient.core.voice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Mixes incoming participant PCM into the speakers. */
final class VoiceAudioPlayback implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger("Valeris Voice");
    private static final AudioFormat FORMAT = new AudioFormat(
            VoiceAudioCapture.SAMPLE_RATE, 16, 1, true, false);

    private SourceDataLine line;
    private final Map<String, byte[]> pending = new ConcurrentHashMap<>();

    void start() {
        if (line != null) {
            return;
        }
        try {
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, FORMAT);
            line = (SourceDataLine) javax.sound.sampled.AudioSystem.getLine(info);
            line.open(FORMAT);
            line.start();
        } catch (LineUnavailableException e) {
            LOGGER.warn("Speaker unavailable: {}", e.getMessage());
        }
    }

    void play(String participantId, byte[] pcm, float gain) {
        if (line == null || pcm == null || pcm.length == 0 || gain <= 0.01f) {
            return;
        }
        byte[] scaled = scalePcm(pcm, gain);
        pending.put(participantId, scaled);
        mixAndWrite();
    }

    private void mixAndWrite() {
        if (line == null || pending.isEmpty()) {
            return;
        }
        int length = VoiceAudioCapture.FRAME_BYTES;
        short[] mix = new short[length / 2];
        for (byte[] pcm : pending.values()) {
            for (int i = 0; i + 1 < pcm.length && i + 1 < length; i += 2) {
                short sample = (short) ((pcm[i + 1] << 8) | (pcm[i] & 0xff));
                mix[i / 2] = (short) clamp(mix[i / 2] + sample);
            }
        }
        pending.clear();
        byte[] out = new byte[length];
        for (int i = 0; i < mix.length; i++) {
            out[i * 2] = (byte) (mix[i] & 0xff);
            out[i * 2 + 1] = (byte) ((mix[i] >> 8) & 0xff);
        }
        line.write(out, 0, out.length);
    }

    private static byte[] scalePcm(byte[] pcm, float gain) {
        byte[] out = pcm.clone();
        for (int i = 0; i + 1 < out.length; i += 2) {
            short sample = (short) ((out[i + 1] << 8) | (out[i] & 0xff));
            sample = (short) clamp((int) (sample * gain));
            out[i] = (byte) (sample & 0xff);
            out[i + 1] = (byte) ((sample >> 8) & 0xff);
        }
        return out;
    }

    private static int clamp(int value) {
        return Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, value));
    }

    @Override
    public void close() {
        pending.clear();
        if (line != null) {
            line.drain();
            line.stop();
            line.close();
            line = null;
        }
    }
}
