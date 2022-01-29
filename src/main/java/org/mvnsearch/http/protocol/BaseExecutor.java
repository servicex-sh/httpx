package org.mvnsearch.http.protocol;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.List;

public interface BaseExecutor {

    default void writeResponse(Path httpFilePath, String redirectResponse, List<byte[]> content) {
        String[] parts = redirectResponse.split("\\s+", 2);
        String responseFile = parts[1];
        try {
            Path responseFilePath;
            if (responseFile.startsWith("/")) {
                responseFilePath = Path.of(responseFile);
            } else {
                responseFilePath = httpFilePath.toAbsolutePath().getParent().resolve(responseFile);
            }
            final File parentDir = responseFilePath.toAbsolutePath().getParent().toFile();
            if (!parentDir.exists()) {
                //noinspection ResultOfMethodCallIgnored
                parentDir.mkdirs();
            }
            try (FileOutputStream fos = new FileOutputStream(responseFilePath.toFile())) {
                for (byte[] bytes : content) {
                    fos.write(bytes);
                }
            }
            System.out.println("---------------------------------");
            System.out.println("Write to " + responseFile + " successfully!");
        } catch (Exception e) {
            System.out.println("---------------------------------");
            System.out.println("Failed to write file:" + responseFile);
        }
    }

    default boolean isPrintable(String contentType) {
        return contentType.startsWith("text/")
                || contentType.contains("javascript")
                || contentType.contains("typescript")
                || contentType.contains("ecmascript")
                || contentType.contains("json")
                || contentType.contains("xml")
                || contentType.contains("yaml");
    }
}
