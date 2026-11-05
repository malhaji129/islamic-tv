package android.content;
import android.content.res.AssetManager;
import java.nio.file.Path;
/** JVM-only asset fixture; JSON and production loader remain real. */
public final class Context {
    private final AssetManager assets;
    public Context(Path directory) { assets = new AssetManager(directory); }
    public AssetManager getAssets() { return assets; }
}
