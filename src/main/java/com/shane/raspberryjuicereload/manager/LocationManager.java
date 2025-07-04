package com.shane.raspberryjuicereload.manager;

import com.shane.raspberryjuicereload.commands.base.Context;
import org.bukkit.Location;
import org.bukkit.World;

public class LocationManager {
    private Location origin;

    public LocationManager() {
        this.origin = null;
    }

    public void setOrigin(Location origin) {
        this.origin = origin;
    }

    public Location getOrigin() {
        return origin;
    }

    public Location parseRelativeBlockLocation(String xStr, String yStr, String zStr) {
        int x = (int) Double.parseDouble(xStr);
        int y = (int) Double.parseDouble(yStr);
        int z = (int) Double.parseDouble(zStr);
        return translateCoordinates(origin.getWorld(), x, y, z, origin.getBlockX(), origin.getBlockY(), origin.getBlockZ());
    }

    public Location parseRelativeBlockLocation(String xStr, String yStr, String zStr, float pitch, float yaw) {
        Location loc = parseRelativeBlockLocation(xStr, yStr, zStr);
        loc.setPitch(pitch);
        loc.setYaw(yaw);
        return loc;
    }

    private Location translateCoordinates(World world, int x, int y, int z, int originX, int originY, int originZ) {
        return new Location(world, originX + x, originY + y, originZ + z);
    }

    public Location[] getLocations(Context context) {
        String[] args = context.args();
        if (args.length != 6) return new Location[0];
        Location loc1 = parseRelativeBlockLocation(args[0], args[1], args[2]);
        Location loc2 = parseRelativeBlockLocation(args[3], args[4], args[5]);
        return new Location[]{loc1, loc2};
    }

    public Location parseRelativeLocation(String xStr, String yStr, String zStr, float pitch, float yaw) {
        Location loc = parseRelativeLocation(xStr, yStr, zStr);
        loc.setPitch(pitch);
        loc.setYaw(yaw);
        return loc;
    }

    public Location parseRelativeLocation(String xStr, String yStr, String zStr) {
        double x = Double.parseDouble(xStr);
        double y = Double.parseDouble(yStr);
        double z = Double.parseDouble(zStr);
        return parseLocation(origin.getWorld(), x, y, z, origin.getX(), origin.getY(), origin.getZ());
    }

    public String blockLocationToRelative(Location loc) {
        return parseLocation(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), origin.getBlockX(), origin.getBlockY(), origin.getBlockZ());
    }

    public String locationToRelative(Location loc) {
        return parseLocation(loc.getX(), loc.getY(), loc.getZ(), origin.getX(), origin.getY(), origin.getZ());
    }


    private String parseLocation(double x, double y, double z, double originX, double originY, double originZ) {
        return (x - originX) + ", " + (y - originY) + ", " + (z - originZ);
    }

    private Location parseLocation(World world, double x, double y, double z, double originX, double originY, double originZ) {
        return new Location(world, originX + x, originY + y, originZ + z);
    }

    private String parseLocation(int x, int y, int z, int originX, int originY, int originZ) {
        return (x - originX) + ", " + (y - originY) + ", " + (z - originZ);
    }

    private Location parseLocation(World world, int x, int y, int z, int originX, int originY, int originZ) {
        return new Location(world, originX + x, originY + y, originZ + z);
    }
}
