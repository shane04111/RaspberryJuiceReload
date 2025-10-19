package com.shane.raspberryjuicereload.util;

import com.google.common.base.MoreObjects;
import com.shane.raspberryjuicereload.RaspberryJuiceReload;
import org.bukkit.Location;
import org.slf4j.Logger;

import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.IntStream;

public class LocationBox {
    private int minX;
    private int minY;
    private int minZ;
    private int maxX;
    private int maxY;
    private int maxZ;

    public LocationBox(Location pos1, Location pos2) {
        this(pos1.getBlockX(), pos1.getBlockY(), pos1.getBlockZ(), pos2.getBlockX(), pos2.getBlockY(), pos2.getBlockZ());
    }

    public LocationBox(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
        if (maxX < minX || maxY < minY || maxZ < minZ) {
            String string = "Invalid bounding box data, inverted bounds for: " + this;
            RaspberryJuiceReload.logger.error(string);
            this.minX = Math.min(minX, maxX);
            this.minY = Math.min(minY, maxY);
            this.minZ = Math.min(minZ, maxZ);
            this.maxX = Math.max(minX, maxX);
            this.maxY = Math.max(minY, maxY);
            this.maxZ = Math.max(minZ, maxZ);
        }

    }

    public static LocationBox create(Location first, Location second) {
        return new LocationBox(
                Math.min(first.getBlockX(), second.getBlockX()),
                Math.min(first.getBlockY(), second.getBlockY()),
                Math.min(first.getBlockZ(), second.getBlockZ()),
                Math.max(first.getBlockX(), second.getBlockX()),
                Math.max(first.getBlockY(), second.getBlockY()),
                Math.max(first.getBlockZ(), second.getBlockZ())
        );
    }

    public static LocationBox infinite() {
        return new LocationBox(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    public boolean intersects(LocationBox other) {
        return this.maxX >= other.minX && this.minX <= other.maxX && this.maxZ >= other.minZ && this.minZ <= other.maxZ && this.maxY >= other.minY && this.minY <= other.maxY;
    }

    public boolean intersectsXZ(int minX, int minZ, int maxX, int maxZ) {
        return this.maxX >= minX && this.minX <= maxX && this.maxZ >= minZ && this.minZ <= maxZ;
    }

    public static Optional<LocationBox> encompassPositions(Iterable<BlockPos> positions) {
        Iterator<BlockPos> iterator = positions.iterator();
        if (!iterator.hasNext()) {
            return Optional.empty();
        } else {
            LocationBox blockBox = new LocationBox((BlockPos) iterator.next());
            Objects.requireNonNull(blockBox);
            iterator.forEachRemaining(blockBox::encompass);
            return Optional.of(blockBox);
        }
    }

    public static Optional<LocationBox> encompass(Iterable<LocationBox> boxes) {
        Iterator<LocationBox> iterator = boxes.iterator();
        if (!iterator.hasNext()) {
            return Optional.empty();
        } else {
            LocationBox blockBox = (LocationBox) iterator.next();
            LocationBox blockBox2 = new LocationBox(blockBox.minX, blockBox.minY, blockBox.minZ, blockBox.maxX, blockBox.maxY, blockBox.maxZ);
            Objects.requireNonNull(blockBox2);
            iterator.forEachRemaining(blockBox2::encompass);
            return Optional.of(blockBox2);
        }
    }

    /**
     * @deprecated
     */
    @Deprecated
    public LocationBox encompass(LocationBox box) {
        this.minX = Math.min(this.minX, box.minX);
        this.minY = Math.min(this.minY, box.minY);
        this.minZ = Math.min(this.minZ, box.minZ);
        this.maxX = Math.max(this.maxX, box.maxX);
        this.maxY = Math.max(this.maxY, box.maxY);
        this.maxZ = Math.max(this.maxZ, box.maxZ);
        return this;
    }

    /**
     * @deprecated
     */
    @Deprecated
    public LocationBox encompass(Location pos) {
        this.minX = Math.min(this.minX, pos.getBlockX());
        this.minY = Math.min(this.minY, pos.getBlockY());
        this.minZ = Math.min(this.minZ, pos.getBlockZ());
        this.maxX = Math.max(this.maxX, pos.getBlockX());
        this.maxY = Math.max(this.maxY, pos.getBlockY());
        this.maxZ = Math.max(this.maxZ, pos.getBlockZ());
        return this;
    }

    /**
     * @deprecated
     */
    @Deprecated
    public LocationBox move(int dx, int dy, int dz) {
        this.minX += dx;
        this.minY += dy;
        this.minZ += dz;
        this.maxX += dx;
        this.maxY += dy;
        this.maxZ += dz;
        return this;
    }

    public LocationBox offset(int x, int y, int z) {
        return new LocationBox(this.minX + x, this.minY + y, this.minZ + z, this.maxX + x, this.maxY + y, this.maxZ + z);
    }

    public LocationBox expand(int offset) {
        return new LocationBox(this.getMinX() - offset, this.getMinY() - offset, this.getMinZ() - offset, this.getMaxX() + offset, this.getMaxY() + offset, this.getMaxZ() + offset);
    }

    public int getBlockCountX() {
        return this.maxX - this.minX + 1;
    }

    public int getBlockCountY() {
        return this.maxY - this.minY + 1;
    }

    public int getBlockCountZ() {
        return this.maxZ - this.minZ + 1;
    }

    public String toString() {
        return MoreObjects.toStringHelper(this).add("minX", this.minX).add("minY", this.minY).add("minZ", this.minZ).add("maxX", this.maxX).add("maxY", this.maxY).add("maxZ", this.maxZ).toString();
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof LocationBox)) {
            return false;
        } else {
            LocationBox blockBox = (LocationBox) o;
            return this.minX == blockBox.minX && this.minY == blockBox.minY && this.minZ == blockBox.minZ && this.maxX == blockBox.maxX && this.maxY == blockBox.maxY && this.maxZ == blockBox.maxZ;
        }
    }

    public int hashCode() {
        return Objects.hash(new Object[]{this.minX, this.minY, this.minZ, this.maxX, this.maxY, this.maxZ});
    }

    public int getMinX() {
        return this.minX;
    }

    public int getMinY() {
        return this.minY;
    }

    public int getMinZ() {
        return this.minZ;
    }

    public int getMaxX() {
        return this.maxX;
    }

    public int getMaxY() {
        return this.maxY;
    }

    public int getMaxZ() {
        return this.maxZ;
    }
}
