package map;

/**
 * A generic high-level class that implements a mask on a 3D map.
 */

public abstract class MapMask {
    public abstract void init();

    public abstract boolean hasNext();

    public abstract ChangeRecord next();
}