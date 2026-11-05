package org.local.karbalascreen;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
/** Bundled corpus budget is independent of the much smaller HTTP-response cap. */
final class ContentAssetText {
    static final int MAX_BYTES = 10_000_000;
    static String read(InputStream input) throws IOException {
        ByteArrayOutputStream output=new ByteArrayOutputStream();
        byte[] buffer=new byte[8192]; int size;
        while((size=input.read(buffer))!=-1) {
            if(Thread.currentThread().isInterrupted())throw new IOException("Cancelled");
            if(output.size()>MAX_BYTES-size)throw new IOException("Content asset too large");
            output.write(buffer,0,size);
        }
        return output.toString(StandardCharsets.UTF_8.name());
    }
}
