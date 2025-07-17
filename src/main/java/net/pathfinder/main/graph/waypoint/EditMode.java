package net.pathfinder.main.graph.waypoint;

import net.minecraft.util.Formatting;

public enum EditMode {
    NORMAL("Normal", Formatting.GOLD) {
        public EditMode next() {
            return CONTINUOUS;
        }
    },
    CONTINUOUS("Continuous", Formatting.GREEN) {
        public EditMode next() {
            return STRAIGHT;
        }
    },
    STRAIGHT("Straight", Formatting.GRAY) {
        public EditMode next() {
            return DELETE;
        }
    },
    DELETE("Delete", Formatting.RED) {
        public EditMode next() {
            return NORMAL;
        }
    };

    EditMode(String name, Formatting color) {
        this.name = name;
        this.color = color;
    }

    public final Formatting color;
    public final String name;

    public abstract EditMode next();
}
