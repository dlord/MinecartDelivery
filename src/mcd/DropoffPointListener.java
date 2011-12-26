package mcd;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class DropoffPointListener extends BlockListener {
    private static final BlockFace[] searchDirections={
            BlockFace.NORTH,
            BlockFace.SOUTH,
            BlockFace.WEST,
            BlockFace.EAST
    };
    private final Map<String, DropoffPoint> dropoffPoints=new HashMap<String, DropoffPoint>();
    private MinecartDelivery minecartDelivery;

    public void setMinecartDelivery(MinecartDelivery minecartDelivery) {
        this.minecartDelivery = minecartDelivery;

        if(this.minecartDelivery!=null) {
            configureDropoffPoints();
        }
    }

    private void configureDropoffPoints() {
        List<World> worlds=Bukkit.getServer().getWorlds();

        // at the moment, I only get the first world.
        // multi-world support is debatable at this point.
        // in other words, don't expect any in the forseeable future.

        if(!worlds.isEmpty()) {
            World world=worlds.get(0);

            for(Vector vector:this.minecartDelivery.getDetectorRailLocations()) {
                Location detectorRailLocation=vector.toLocation(world);
                scanForChests(detectorRailLocation.getBlock());
            }
        }
    }

    public Map<String, DropoffPoint> getDropoffPoints() {
        return dropoffPoints;
    }

    @Override
    public void onBlockPlace(BlockPlaceEvent event) {
        super.onBlockPlace(event);

        Block blockPlaced=event.getBlockPlaced();

        if(blockPlaced!=null && Material.CHEST==blockPlaced.getType()) {
            scanForDetectorRail(blockPlaced);
        } else if(blockPlaced!=null && Material.DETECTOR_RAIL==blockPlaced.getType()) {
            scanForChests(blockPlaced);
        }
    }

    @Override
    public void onBlockBreak(BlockBreakEvent event) {
        super.onBlockBreak(event);

        Block blockBreak=event.getBlock();

        if(blockBreak!=null && Material.CHEST==blockBreak.getType()) {
            removeChestFromDropoffPoint(blockBreak.getLocation());
        } else if(blockBreak!=null && Material.DETECTOR_RAIL==blockBreak.getType()) {
            getDropoffPoints().remove(blockBreak.getLocation().toString());
            this.minecartDelivery.removeDetectorRailFromConfig(blockBreak.getLocation());
        }
    }

    private void removeChestFromDropoffPoint(Location location) {
        for(String key:getDropoffPoints().keySet()) {
            DropoffPoint dp=getDropoffPoints().get(key);

            dp.getChestLocations().remove(location);
        }
    }

    private void scanForChests(Block detectorRail) {
        Location detectorRailLocation=detectorRail.getLocation();
        Block scanBlock=detectorRailLocation.clone().add(0.0, -1.0, 0.0).getBlock();

        for(BlockFace blockFace:searchDirections) {
            Block searchBlock=scanBlock.getRelative(blockFace, 1);

            if(searchBlock!=null && Material.CHEST==searchBlock.getType()) {
                DropoffPoint dp=getDropoffPoints().get(detectorRailLocation.toString());
                if(dp==null) {
                    dp=new DropoffPoint();
                    getDropoffPoints().put(detectorRailLocation.toString(), dp);
                }

                dp.getChestLocations().add(searchBlock.getLocation());
                dp.getChestLocations().addAll(lookForAdjacentChests(searchBlock.getLocation()));

                this.minecartDelivery.addDetectorRailToConfig(detectorRailLocation);
            }
        }
    }

    private void scanForDetectorRail(Block chest) {
        // look for any adjacent chests first, before looking for
        // the detector rail.
        List<Location> adjacentChests=lookForAdjacentChests(chest.getLocation());

        Block scanBlock=chest.getLocation().add(0.0, 1.0, 0.0).getBlock();

        for(BlockFace blockFace:searchDirections) {
            Block searchBlock=scanBlock.getRelative(blockFace, 1);

            if(searchBlock!=null && Material.DETECTOR_RAIL==searchBlock.getType()) {
                DropoffPoint dp=getDropoffPoints().get(searchBlock.toString());
                if(dp==null) {
                    dp=new DropoffPoint();
                    getDropoffPoints().put(searchBlock.getLocation().toString(), dp);
                }

                dp.getChestLocations().add(chest.getLocation());
                dp.getChestLocations().addAll(adjacentChests);

                this.minecartDelivery.addDetectorRailToConfig(searchBlock.getLocation());
            }
        }
    }

    private List<Location> lookForAdjacentChests(Location origin) {
        List<Location> adjacentChests=new ArrayList<Location>();

        // I am not putting a limit to the number of chests that
        // can be found, in the off chance that there's actually
        // a plugin that can extend the number of adjacent chests.
        for(BlockFace blockFace:searchDirections) {
            Block searchBlock=origin.getBlock().getRelative(blockFace, 1);

            if(searchBlock!=null && Material.CHEST==searchBlock.getType()) {
                adjacentChests.add(searchBlock.getLocation());
            }
        }

        return adjacentChests;
    }
}
