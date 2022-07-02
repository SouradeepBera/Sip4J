package com.sprinklr.javasip.utils;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;

/*
* Helper class for audio related things
 */
public class AudioHelper {

    /*
     * Generates an audio file from the stream. The file must be a WAV file.
     *
     * @param data the byte array
     * @param outputFile the file in which to write the audio data could not be
     *            written onto the file
     */
    public static void generateFile(byte[] data, File outputFile) throws IOException, UnsupportedAudioFileException {
        AudioInputStream audioStream = getAudioStream(data);
        if (outputFile.getName().endsWith("wav")) {
            AudioSystem.write(audioStream, AudioFileFormat.Type.WAVE, Files.newOutputStream(outputFile.toPath()));
        }
        else {
            throw new IllegalArgumentException("Unsupported encoding " + outputFile);
        }
    }

    /*
     * Returns the audio stream corresponding to the array of bytes
     *
     * @param byteArray the byte array
     * @return the converted audio stream
     */
    public static AudioInputStream getAudioStream(byte[] byteArray) throws IOException, UnsupportedAudioFileException {

        try {
            ByteArrayInputStream byteStream =
                    new ByteArrayInputStream(byteArray);
            return AudioSystem.getAudioInputStream(byteStream);
        }
        catch (UnsupportedAudioFileException e) {
            byteArray = addWavHeader(byteArray);
            ByteArrayInputStream byteStream =
                    new ByteArrayInputStream(byteArray);
            return AudioSystem.getAudioInputStream(byteStream);
        }
    }

    /*
     * Adds a WAV header to the byte array
     *
     * @param bytes the original array of bytes
     * @return the new array with the header
     * @throws IOException if the byte array is ill-formatted
     */
    private static byte[] addWavHeader(byte[] bytes) {

        ByteBuffer bufferWithHeader = ByteBuffer.allocate(bytes.length + 44);
        bufferWithHeader.order(ByteOrder.LITTLE_ENDIAN);
        bufferWithHeader.put("RIFF".getBytes());
        bufferWithHeader.putInt(bytes.length + 36);
        bufferWithHeader.put("WAVE".getBytes());
        bufferWithHeader.put("fmt ".getBytes());
        bufferWithHeader.putInt(16);
        bufferWithHeader.putShort((short) 1);
        bufferWithHeader.putShort((short) 1);
        bufferWithHeader.putInt(16000);
        bufferWithHeader.putInt(32000);
        bufferWithHeader.putShort((short) 2);
        bufferWithHeader.putShort((short) 16);
        bufferWithHeader.put("data".getBytes());
        bufferWithHeader.putInt(bytes.length);
        bufferWithHeader.put(bytes);
        return bufferWithHeader.array();
    }

    /*
     * Reads the input stream and returns the corresponding array of bytes
     *
     * @param stream the initial stream
     * @return the array of bytes from the stream
     */
    public static byte[] readStream(InputStream stream) throws IOException {
        ByteArrayOutputStream rawBuffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[1024 * 16];
        while ((nRead = stream.read(data, 0, data.length)) != -1) {
            rawBuffer.write(data, 0, nRead);
        }
        rawBuffer.flush();
        rawBuffer.close();
        return rawBuffer.toByteArray();
    }

    private AudioHelper(){}
}
