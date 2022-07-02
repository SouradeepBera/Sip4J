package com.sprinklr.javasip.utils;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.*;
import java.nio.file.Files;

/*
 * Helper class for audio related things
 */
public class AudioHelper {

    private static final String WAV = "wav";

    private AudioHelper() {
        throw new IllegalStateException("Utility class");
    }

    /*
     * Generates an audio file from the stream. The file must be a WAV file.
     *
     * @param data the byte array
     * @param outputFile the file in which to write the audio data could not be
     *            written onto the file
     */
    public static void generateFile(byte[] data, File outputFile) throws IOException, UnsupportedAudioFileException {
        boolean isWavFile = outputFile.getName().endsWith(WAV);
        if (!isWavFile) {
            throw new IllegalArgumentException("Unsupported encoding " + outputFile);
        }
        try(AudioInputStream audioStream = AudioSystem.getAudioInputStream(new ByteArrayInputStream(data))) {
            AudioSystem.write(audioStream, AudioFileFormat.Type.WAVE, Files.newOutputStream(outputFile.toPath()));
        }
    }
}
