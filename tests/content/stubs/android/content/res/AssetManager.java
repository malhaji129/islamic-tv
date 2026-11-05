package android.content.res;
import java.nio.file.*;
import java.io.*;
public final class AssetManager {
    private final Path directory;
    public int opens;
    public AssetManager(Path directory) { this.directory = directory; }
    public InputStream open(String name) throws IOException { opens++; return Files.newInputStream(directory.resolve(name)); }
}
