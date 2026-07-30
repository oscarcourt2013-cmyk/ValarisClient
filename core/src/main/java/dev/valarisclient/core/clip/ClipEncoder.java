package dev.valarisclient.core.clip;

import org.jcodec.api.awt.AWTSequenceEncoder;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/** Encodes captured PNG/JPEG frames into an H.264 MP4 clip. */
public final class ClipEncoder {

    private ClipEncoder() {
    }

    public static void encode(Path framesDir, Path outputMp4, int fps) throws IOException {
        List<Path> frames;
        try (Stream<Path> walk = Files.list(framesDir)) {
            frames = walk
                    .filter(p -> {
                        String name = p.getFileName().toString().toLowerCase();
                        return name.startsWith("frame_") && (name.endsWith(".png") || name.endsWith(".jpg"));
                    })
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .toList();
        }

        if (frames.isEmpty()) {
            throw new IOException("No frames to encode.");
        }

        Files.createDirectories(outputMp4.getParent());
        AWTSequenceEncoder encoder = AWTSequenceEncoder.createSequenceEncoder(outputMp4.toFile(), fps);
        try {
            for (Path frame : frames) {
                BufferedImage image = ImageIO.read(frame.toFile());
                if (image == null) {
                    continue;
                }
                encoder.encodeImage(image);
            }
        } finally {
            encoder.finish();
        }
    }
}
