package com.amazonaws.kinesisvideo.demoapp;

import java.io.File;

public class FrameDeleter {
//    private static final String EXTENSION = ".h264";

    public void deleteFrame(String pathTo, String frameName) {
        File frame = new File(pathTo + frameName);
        boolean ret = frame.delete();
        if (!ret) {
            System.out.println("Did not delete for some reasons!");
        }
    }
}
