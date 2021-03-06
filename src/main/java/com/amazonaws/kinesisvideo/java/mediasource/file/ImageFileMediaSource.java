package com.amazonaws.kinesisvideo.java.mediasource.file;

import com.amazonaws.kinesisvideo.client.mediasource.MediaSource;
import com.amazonaws.kinesisvideo.client.mediasource.MediaSourceConfiguration;
import com.amazonaws.kinesisvideo.client.mediasource.MediaSourceSink;
import com.amazonaws.kinesisvideo.client.mediasource.MediaSourceState;
import com.amazonaws.kinesisvideo.common.exception.KinesisVideoException;
import com.amazonaws.kinesisvideo.demoapp.Checkpointer;
import com.amazonaws.kinesisvideo.demoapp.FrameDeleter;
import com.amazonaws.kinesisvideo.mediasource.OnFrameDataAvailable;
import com.amazonaws.kinesisvideo.producer.KinesisVideoFrame;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * MediaSource based on local image files. Currently, this MediaSource expects
 * a series of H264 frames.
 */
public class ImageFileMediaSource implements MediaSource {
    private static final long HUNDREDS_OF_NANOS_IN_MS = 10 * 1000;
    private static final int FRAME_FLAG_KEY_FRAME = 1;
    private static final int FRAME_FLAG_NONE = 0;
    private static final long FRAME_DURATION_20_MS = 20L;
    private static final int FRAGMENT_DURATION_SECONDS = 2;
    private FrameDeleter frameDeleter;
    private final Log log = LogFactory.getLog(ImageFileMediaSource.class);

    private ImageFileMediaSourceConfiguration imageFileMediaSourceConfiguration;
    private MediaSourceState mediaSourceState;
    private MediaSourceSink mediaSourceSink;
    private ImageFrameSource imageFrameSource;
    private int frameIndex;
    private long currentTimeMs;
    private long timeToIncrement;

    @Override
    public MediaSourceState getMediaSourceState() {
        return mediaSourceState;
    }

    @Override
    public MediaSourceConfiguration getConfiguration() {
        return imageFileMediaSourceConfiguration;
    }

    @Override
    public void initialize(@Nonnull final MediaSourceSink mediaSourceSink) throws KinesisVideoException {
        this.mediaSourceSink = mediaSourceSink;
    }

    @Override
    public void configure(final MediaSourceConfiguration configuration) {
        if (!(configuration instanceof ImageFileMediaSourceConfiguration)) {
            throw new IllegalStateException("Configuration must be an instance of OpenCvMediaSourceConfiguration");
        }

        this.imageFileMediaSourceConfiguration = (ImageFileMediaSourceConfiguration) configuration;
        this.frameIndex = 0;
    }

    @Override
    public void start() throws KinesisVideoException {
        mediaSourceState = MediaSourceState.RUNNING;
        try {
            imageFrameSource = new ImageFrameSource(imageFileMediaSourceConfiguration);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("ffmpeg not installed");
        }
        this.frameDeleter = new FrameDeleter();
        this.currentTimeMs = System.currentTimeMillis();
        this.timeToIncrement = 1000L / imageFileMediaSourceConfiguration.getFps();
        imageFrameSource.onBytesAvailable(createKinesisVideoFrameAndPushToProducer());
        imageFrameSource.start();
    }

    @Override
    public void stop() throws KinesisVideoException {
        if (imageFrameSource != null) {
            imageFrameSource.stop();
        }

        mediaSourceState = MediaSourceState.STOPPED;
    }

    @Override
    public boolean isStopped() {
        return false;
    }

    @Override
    public void free() throws KinesisVideoException {

    }

    public long getCurrentTimeMs() {
        return this.currentTimeMs;
    }

    private OnFrameDataAvailable createKinesisVideoFrameAndPushToProducer() {
        return new OnFrameDataAvailable() {
            @Override
            public void onFrameDataAvailable(final ByteBuffer data) {
                final long currentTimeMs = getCurrentTimeMs();

                final int flags = isKeyFrame()
                        ? FRAME_FLAG_KEY_FRAME
                        : FRAME_FLAG_NONE;

                final KinesisVideoFrame frame = new KinesisVideoFrame(
                        frameIndex++,
                        flags,
                        currentTimeMs * HUNDREDS_OF_NANOS_IN_MS,
                        currentTimeMs * HUNDREDS_OF_NANOS_IN_MS,
                        FRAME_DURATION_20_MS * HUNDREDS_OF_NANOS_IN_MS,
                        data);

                if (frame.getSize() == 0) {
                    resetTimestamp();
                    return;
                }

                putFrame(frame);
            }
        };
    }

    public void resetTimestamp() {
        this.currentTimeMs = System.currentTimeMillis();
    }

    private boolean isKeyFrame() {
        return frameIndex % imageFileMediaSourceConfiguration.getFps() == 0;
    }

    private void putFrame(final KinesisVideoFrame kinesisVideoFrame) {
        try {
            mediaSourceSink.onFrame(kinesisVideoFrame);
        } catch (final KinesisVideoException ex) {
            log.error("Failed to put frame with Exception", ex);
        }
        final String frameFileName = imageFrameSource.getFileName();
        frameDeleter.deleteFrame(imageFileMediaSourceConfiguration.getDir(), frameFileName);
        currentTimeMs += this.timeToIncrement;
        System.out.println("Frame supposed to be ended, frame file name: " + frameFileName);
    }
}
