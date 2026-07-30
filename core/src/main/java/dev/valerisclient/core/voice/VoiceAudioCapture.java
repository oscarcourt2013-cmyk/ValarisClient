package dev.valerisclient.core.voice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import java.util.concurrent.atomic.AtomicBoolean;

/** Captures microphone PCM at 16 kHz mono. */
final class VoiceAudioCapture implements AutoCloseable {

    static final int SAMPLE_RATE = 16_000;
    static final int FRAME_BYTES = 640; // 20 ms

    private static final Logger LOGGER = LoggerFactory.getLogger("Valeris Voice");
    private static final AudioFormat FORMAT = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);

    private TargetDataLine line;
    private Thread thread;
    private final AtomicBoolean running = new AtomicBoolean();

    interface FrameListener {
        void onFrame(byte[] pcm);
    }

    void start(FrameListener listener) {
        if (running.getAndSet(true)) {
            return;
        }
        try {
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, FORMAT);
            line = (TargetDataLine) javax.sound.sampled.AudioSystem.getLine(info);
            line.open(FORMAT);
            line.start();
        } catch (LineUnavailableException e) {
            running.set(false);
            LOGGER.warn("Microphone unavailable: {}", e.getMessage());
            return;
        }
        thread = Thread.ofPlatform().daemon().name("PrimeVoice-Mic").start(() -> captureLoop(listener));
    }

    private void captureLoop(FrameListener listener) {
        byte[] buffer = new byte[FRAME_BYTES];
        while (running.get() && line != null) {
            int read = line.read(buffer, 0, buffer.length);
            if (read > 0) {
                byte[] frame = read == buffer.length ? buffer.clone() : buffer.clone();
                listener.onFrame(frame);
            }
        }
    }

    @Override
    public void close() {
        running.set(false);
        if (line != null) {
            line.stop();
            line.close();
            line = null;
        }
    }
}
