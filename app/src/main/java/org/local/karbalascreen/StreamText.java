package org.local.karbalascreen;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

final class StreamText {
    static String read(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream(); byte[] buffer = new byte[4096]; int length;
        while ((length = input.read(buffer)) != -1) {
            if (Thread.currentThread().isInterrupted()) throw new IOException("Cancelled");
            output.write(buffer, 0, length); if (output.size() > 512000) throw new IOException("Response too large");
        }
        return output.toString(StandardCharsets.UTF_8.name());
    }
}
