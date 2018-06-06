package com.amazonaws.kinesisvideo.demoapp;

import com.amazonaws.kinesisvideo.client.KinesisVideoClient;
import com.amazonaws.kinesisvideo.client.mediasource.MediaSource;
import com.amazonaws.kinesisvideo.common.exception.KinesisVideoException;
import com.amazonaws.kinesisvideo.demoapp.auth.AuthHelper;
import com.amazonaws.kinesisvideo.java.client.KinesisVideoJavaClientFactory;
import com.amazonaws.kinesisvideo.java.mediasource.file.ImageFileMediaSource;
import com.amazonaws.kinesisvideo.java.mediasource.file.ImageFileMediaSourceConfiguration;
import com.amazonaws.regions.Regions;


/**
 * Demo Java Producer.
 */
public final class DemoAppMain {
    private static final String STREAM_NAME = "why-stream";
    private static final int FPS_25 = 25;
    private static final String IMAGE_DIR = "src/main/resources/data/png/";
    private static final String CHECKPOINT_FILE = "src/main/resources/data/checkpoint";
    private static final String IMAGE_FILENAME_FORMAT = "session1_frame%d.png";
    private static final String FFMPEG_PATH = "/usr/local/bin/ffmpeg";
    private static final String FFPROBE_PATH = "/usr/local/bin/ffprobe";
    private static final int MAX_INDEX = 600;
    private static final int START_FILE_INDEX = 1;
    private static final int END_FILE_INDEX = 1500;
    private static final int RETRIES = 20;

    private DemoAppMain() {
        throw new UnsupportedOperationException();
    }

    public static void main(final String[] args) {
        System.out.println("Working Directory = " + System.getProperty("user.dir"));
        try {
            // create Kinesis Video high level client
            final KinesisVideoClient kinesisVideoClient = KinesisVideoJavaClientFactory
                    .createKinesisVideoClient(
                            Regions.US_WEST_2,
                            AuthHelper.getSystemPropertiesCredentialsProvider());

            // create a media source. this class produces the data and pushes it into
            // Kinesis Video Producer lower level components
            final MediaSource bytesMediaSource = createImageFileMediaSource();

            // register media source with Kinesis Video Client
            kinesisVideoClient.registerMediaSource(STREAM_NAME, bytesMediaSource);

            // start streaming
            bytesMediaSource.start();
        } catch (final KinesisVideoException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Create a MediaSource based on local sample H.264 frames.
     *
     * @return a MediaSource backed by local H264 frame files
     */
    private static MediaSource createImageFileMediaSource() {
        final ImageFileMediaSourceConfiguration configuration =
                new ImageFileMediaSourceConfiguration.Builder()
                        .fps(FPS_25)
                        .dir(IMAGE_DIR)
                        .filenameFormat(IMAGE_FILENAME_FORMAT)
                        .startFileIndex(START_FILE_INDEX)
                        .endFileIndex(END_FILE_INDEX)
                        .checkpointDir(CHECKPOINT_FILE)
                        .maxIndex(MAX_INDEX)
                        .retries(RETRIES)
                        .ffmpegPath(FFMPEG_PATH)
                        .ffprobePath(FFPROBE_PATH)
                        .build();
        final ImageFileMediaSource mediaSource = new ImageFileMediaSource();
        mediaSource.configure(configuration);
        return mediaSource;
    }
}
