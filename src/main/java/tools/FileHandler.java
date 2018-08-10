package tools;

import org.apache.commons.lang3.RandomStringUtils;

import java.io.*;

import static java.util.UUID.randomUUID;

public class FileHandler {

    public static File createSampleFile() throws IOException {
        File file = File.createTempFile(randomUUID().toString(), ".txt");
        file.deleteOnExit();

        Writer writer = new OutputStreamWriter(new FileOutputStream(file));
        writer.write(RandomStringUtils.random(10, "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"));
        writer.close();

        return file;
    }
}
