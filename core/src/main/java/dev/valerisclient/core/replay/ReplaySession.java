package dev.valerisclient.core.replay;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** In-memory replay timeline with playback controls. */
public final class ReplaySession {

    public static final float[] SPEEDS = {0.25f, 0.5f, 1f, 2f};

    private final List<ReplayFrame> frames = new ArrayList<>();
    private boolean recording;
    private boolean playing;
    private boolean paused;
    private int playIndex;
    private float speed = 1f;
    private float playAccumulator;
    private int recordIntervalTicks = 2;
    private int recordTickCounter;

    public boolean recording() {
        return recording;
    }

    public boolean playing() {
        return playing;
    }

    public boolean paused() {
        return paused;
    }

    public float speed() {
        return speed;
    }

    public List<ReplayFrame> frames() {
        return Collections.unmodifiableList(frames);
    }

    public void startRecording() {
        frames.clear();
        recording = true;
        playing = false;
        paused = false;
        playIndex = 0;
        recordTickCounter = 0;
    }

    public void stopRecording() {
        recording = false;
    }

    public void startPlayback() {
        if (frames.isEmpty()) {
            return;
        }
        playing = true;
        paused = false;
        playIndex = 0;
        playAccumulator = 0f;
    }

    public void stopPlayback() {
        playing = false;
        paused = false;
    }

    public void togglePause() {
        if (playing) {
            paused = !paused;
        }
    }

    public void cycleSpeed() {
        for (int i = 0; i < SPEEDS.length; i++) {
            if (Math.abs(SPEEDS[i] - speed) < 0.01f) {
                speed = SPEEDS[(i + 1) % SPEEDS.length];
                return;
            }
        }
        speed = SPEEDS[0];
    }

    public void recordFrame(ReplayFrame frame) {
        if (!recording) {
            return;
        }
        recordTickCounter++;
        if (recordTickCounter < recordIntervalTicks) {
            return;
        }
        recordTickCounter = 0;
        if (!frames.isEmpty()) {
            ReplayFrame last = frames.getLast();
            double dx = frame.x() - last.x();
            double dy = frame.y() - last.y();
            double dz = frame.z() - last.z();
            if (dx * dx + dy * dy + dz * dz < 0.0025) {
                return;
            }
        }
        frames.add(frame);
        if (frames.size() > 4000) {
            frames.removeFirst();
        }
    }

    /** @return current playback frame or null */
    public ReplayFrame tickPlayback(float deltaSeconds) {
        if (!playing || paused || frames.isEmpty()) {
            return null;
        }
        playAccumulator += deltaSeconds * speed * 20f;
        while (playAccumulator >= 1f && playIndex < frames.size() - 1) {
            playAccumulator -= 1f;
            playIndex++;
        }
        if (playIndex >= frames.size() - 1 && playAccumulator >= 1f) {
            playing = false;
        }
        return frames.get(Math.min(playIndex, frames.size() - 1));
    }

    public ReplayFrame ghostFrame() {
        if (frames.isEmpty()) {
            return null;
        }
        return playing ? frames.get(Math.min(playIndex, frames.size() - 1)) : frames.getLast();
    }

    public JsonElement toJson() {
        JsonArray array = new JsonArray();
        for (ReplayFrame frame : frames) {
            JsonObject o = new JsonObject();
            o.addProperty("t", frame.worldTimeMillis());
            o.addProperty("x", frame.x());
            o.addProperty("y", frame.y());
            o.addProperty("z", frame.z());
            o.addProperty("yaw", frame.yaw());
            o.addProperty("pitch", frame.pitch());
            array.add(o);
        }
        return array;
    }

    public void loadJson(JsonElement element) {
        frames.clear();
        if (element == null || !element.isJsonArray()) {
            return;
        }
        for (JsonElement entry : element.getAsJsonArray()) {
            JsonObject o = entry.getAsJsonObject();
            frames.add(new ReplayFrame(
                    o.get("t").getAsLong(),
                    o.get("x").getAsDouble(),
                    o.get("y").getAsDouble(),
                    o.get("z").getAsDouble(),
                    o.get("yaw").getAsFloat(),
                    o.get("pitch").getAsFloat(),
                    false, false));
        }
    }
}
