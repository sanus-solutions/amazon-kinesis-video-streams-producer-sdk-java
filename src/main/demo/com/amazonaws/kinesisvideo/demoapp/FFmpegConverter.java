package com.amazonaws.kinesisvideo.demoapp;

import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;

import java.io.IOException;

public class FFmpegConverter {
    private FFmpegExecutor executor;

    public FFmpegConverter(String ffmpegPath, String ffprobePath) throws IOException {
        FFmpeg ffmpeg = new FFmpeg(ffmpegPath);
        FFprobe ffprobe = new FFprobe(ffprobePath);
        executor = new FFmpegExecutor(ffmpeg, ffprobe);
    }

    public void encodeToH264(String inputPath, String output) {
        FFmpegBuilder builder = new FFmpegBuilder()
            .setInput(inputPath)     // Filename, or a FFmpegProbeResult
            .overrideOutputFiles(true) // Override the output if it exists

            .addOutput(output)   // Filename for the destination
            .setFormat("h264")        // Format is inferred from filename, or can be set

            .setVideoCodec("libx264")     // Video using x264
            .setVideoFrameRate(24, 1)     // at 24 frames per second
            .setVideoResolution(1280, 720) // at 640x480 resolution

            .addExtraArgs("-preset", "ultrafast")
                .done();
        executor.createJob(builder).run();
    }
}
