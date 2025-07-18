package net.pathfinder.main.graph.waypoint.data;

import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;
import net.pathfinder.main.Output;

import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

//todo config editing
/**
 * A config with individual parameters for every world/server.
 */
@SuppressWarnings("CanBeFinal") //temp
public class WorldConfig {

    transient private Path path;

    //Dimension detection
    public boolean useTabDetection = true;
    public boolean useTabFooter = false;
    public String tabPattern = "(?<=shard:<).*?(?=(-\\d)?>)";
    public boolean useDimensionInfo = true;

    public static WorldConfig read(Path path) {
        if (!Files.exists(path)) return new WorldConfig().write(path);
        else {
            try {
                return new Gson().fromJson(Files.readString(path, StandardCharsets.UTF_8), WorldConfig.class);
            }
            catch (Exception e) {
                Output.logError("Couldn't read file " + path);
                throw new RuntimeException(e);
            }
        }
    }

    public void onRead(Path path) {
        this.path = path;
    }

    public void write() {
        write(path);
    }

    public WorldConfig write(Path path) {
        Gson gson = new Gson();
        try (JsonWriter writer = gson.newJsonWriter(new FileWriter(path.toFile(), StandardCharsets.UTF_8))) {
            writer.setIndent("    ");
            gson.toJson(gson.toJsonTree(this, WorldConfig.class), writer);
        }
        catch (Exception e) {
            Output.logError("Couldn't save file " + path);
            Output.logError(e.getMessage());
        }
        return this;
    }
}
