package net.pathfinder.main.graph.waypoint.data;

import com.google.gson.Gson;
import io.netty.util.collection.LongObjectHashMap;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.pathfinder.main.Output;
import net.pathfinder.main.graph.PositionUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import static net.pathfinder.main.config.PFConfig.cfg;

/**
 * A class containing all stable waypoint graph information for a given dimension.
 */
public class DimensionData {

    transient private Path path;

    public long idTracker = 0L;
    public LongObjectHashMap<Waypoint> waypoints = new LongObjectHashMap<>();
    public LongObjectHashMap<LocationData> locations = new LongObjectHashMap<>();

    public void add(Waypoint waypoint) {
        waypoints.put(waypoint.id(), waypoint);
    }

    /**
     * Reads compressed data from a file, if it exists. Otherwise, creates a new instance.
     */
    public static DimensionData read(Path path) {
        if (!Files.exists(path)) return new DimensionData().write(path);
        try {
            Inflater inflater = new Inflater();
            inflater.setInput(Files.readAllBytes(path));

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];

            while (!inflater.finished()) {
                int decompressedSize = inflater.inflate(buffer);
                outputStream.write(buffer, 0, decompressedSize);
            }
            return new Gson().fromJson(outputStream.toString(StandardCharsets.UTF_8), DimensionData.class);
        }
        catch (Exception e) {
            Output.logError("Couldn't read file " + path);
            throw new RuntimeException(e);
        }
    }

    public void onUpdate(Path path) {
        this.path = path;
    }

    public void write() {
        write(path);
    }

    /**
     * Compresses and writes data to a file.
     */
    public DimensionData write(Path path) {
        try {
            Deflater deflater = new Deflater();
            deflater.setInput(new Gson().toJson(this).getBytes(StandardCharsets.UTF_8));
            deflater.finish();

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];

            while (!deflater.finished()) {
                int compressedSize = deflater.deflate(buffer);
                outputStream.write(buffer, 0, compressedSize);
            }

            try {
                Files.write(path, outputStream.toByteArray());
                if (cfg.saveUncompressedData)
                    Files.writeString(Path.of(path + ".uncompressed"), new Gson().toJson(this));
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        catch (Exception e) {
            Output.logError("Couldn't save file " + path);
            Output.logError(e.getMessage());
        }
        return this;
    }

    /**
     * Used for getting the nearest waypoint from stable data.
     */
    public Waypoint getNearest(Vec3i target, int range) {
        Waypoint nearest = null;
        int minDistance = Integer.MAX_VALUE;

        for (Waypoint waypoint : waypoints.values()) {
            BlockPos pos = waypoint.pos();
            if (PositionUtils.isInRange(target, pos, range)) {
                int distance = PositionUtils.getSquaredDistance(target, pos);
                if (minDistance > distance) {
                    minDistance = distance;
                    nearest = waypoint;
                }
            }
        }
        return nearest;
    }
}
