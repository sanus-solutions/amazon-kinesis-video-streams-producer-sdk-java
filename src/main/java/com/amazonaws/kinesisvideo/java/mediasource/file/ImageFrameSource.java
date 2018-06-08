package com.amazonaws.kinesisvideo.java.mediasource.file;

import com.amazonaws.kinesisvideo.common.preconditions.Preconditions;
import com.amazonaws.kinesisvideo.demoapp.FFmpegConverter;
import com.amazonaws.kinesisvideo.mediasource.OnFrameDataAvailable;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Frame source backed by local image files.
 */
@NotThreadSafe
public class ImageFrameSource {
    public static final int DISCRETENESS_HZ = 25;
    private final ExecutorService executor = Executors.newFixedThreadPool(1);
    private final int fps;
    private final ImageFileMediaSourceConfiguration configuration;

    private final int totalFiles;
    private OnFrameDataAvailable onFrameDataAvailable;
    private boolean isRunning = false;
    private long frameCounter;
    private final Log log = LogFactory.getLog(ImageFrameSource.class);
    private final FFmpegConverter converter;

    public ImageFrameSource(final ImageFileMediaSourceConfiguration configuration) throws IOException {
        this.configuration = configuration;
        this.totalFiles = getTotalFiles();
        this.fps = configuration.getFps();
        this.converter = new FFmpegConverter(configuration.getFFmpegPath(), configuration.getFFprobePath());
    }

    private int getTotalFiles() {
        return configuration.getMaxIndex();
    }

    public String getFileName() {
        return String.format(configuration.getFilenameFormat(), getFileNameIndex(this.frameCounter));
    }

    public String getFileName(int frameCounter) {
        return String.format(configuration.getFilenameFormat(), getFileNameIndex(frameCounter));
    }

    public int getFileNameIndex(int frameCounter) {
        return (configuration.getStartFileIndex() + frameCounter) % totalFiles;
    }

    public long getFileNameIndex(long frameCounter) {
        return (configuration.getStartFileIndex() + frameCounter) % totalFiles;
    }

    public void start() {
        if (isRunning) {
            throw new IllegalStateException("Frame source is already running");
        }

        isRunning = true;
        startFrameGenerator();
    }

    public void stop() {
        isRunning = false;
        stopFrameGenerator();
    }

    public void onBytesAvailable(final OnFrameDataAvailable onFrameDataAvailable) {
        this.onFrameDataAvailable = onFrameDataAvailable;
    }

    private void startFrameGenerator() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                generateFrameAndNotifyListener();
            }
        });
    }

    private void generateFrameAndNotifyListener() {
        while (isRunning) {
            if (onFrameDataAvailable != null) {
//                onFrameDataAvailable.onFrameDataAvailable(createKinesisVideoFrameFromImage(frameCounter));
                onFrameDataAvailable.onFrameDataAvailable(createKinesisVideoFrameFromImagePNG(frameCounter));
            }

            frameCounter++;
            try {
                Thread.sleep(Duration.ofSeconds(1L).toMillis() / fps);
            } catch (final InterruptedException e) {
                log.error("Frame interval wait interrupted by Exception ", e);
            }
        }
    }

    private ByteBuffer createKinesisVideoFrameFromImage(final long index) {
        final String filename = String.format(configuration.getFilenameFormat(),
                (configuration.getStartFileIndex() + index) % totalFiles);
        final Path path = Paths.get(configuration.getDir() + filename);

        int retries = configuration.getRetries();
        while (retries > 0) {
            try {
                final byte[] bytes = Files.readAllBytes(path);
                return ByteBuffer.wrap(bytes);
            } catch (final IOException e) {
                log.error("Read file failed with Exception, sleeping to wait for next file", e);
                try {
                    Thread.sleep(Duration.ofSeconds(10L).toMillis());
                } catch (final InterruptedException ex) {
                    log.error("Frame wait interrupted by Exception ", ex);
                }
            }
            retries--;
        }
        return null;
    }

    private ByteBuffer createKinesisVideoFrameFromImagePNG(final long index) {
        final String filename = String.format(configuration.getFilenameFormat(),
                (configuration.getStartFileIndex() + index) % totalFiles);

        try {
            converter.encodeToH264(configuration.getDir() + filename, "output.h264");
            final Path path = Paths.get("output.h264");
            final byte[] bytes = Files.readAllBytes(path);
            return ByteBuffer.wrap(bytes);
        } catch (final Exception e) {
            log.error("Read file failed with Exception, sleeping to wait for next file", e);
            this.frameCounter -= 1;
            try {
                Thread.sleep(Duration.ofSeconds(1L).toMillis());
            } catch (final InterruptedException ex) {
                log.error("Frame wait interrupted by Exception ", ex);
            }
        }
        return ByteBuffer.wrap(new byte[0]);
    }

    private void stopFrameGenerator() {

    }
}
