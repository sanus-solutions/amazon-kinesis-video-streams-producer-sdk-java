package com.amazonaws.kinesisvideo.demoapp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.FileReader;

public class Checkpointer {
    private File checkpointFile;

    public Checkpointer(String checkpointDir) {
        this.checkpointFile = new File(checkpointDir);
    }

    public void saveNewIndex(int i) throws IOException {
        FileWriter fileWriter = new FileWriter(checkpointFile);
        fileWriter.write(String.valueOf(i) + "\n");
        fileWriter.close();
    }

    public int getCheckpoint() throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(checkpointFile));
        String checkpoint = br.readLine();
        return Integer.parseInt(checkpoint);
    }
}
