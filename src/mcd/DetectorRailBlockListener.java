package mcd;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Entity;
import org.bukkit.entity.StorageMinecart;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class DetectorRailBlockListener extends BlockListener {
    private DropoffPointListener dropoffPointListener;

    public void setDropoffPointListener(DropoffPointListener dropoffPointListener) {
        this.dropoffPointListener = dropoffPointListener;
    }

    @Override
    public void onBlockRedstoneChange(BlockRedstoneEvent event) {
        super.onBlockRedstoneChange(event);
        Block detectorRail=event.getBlock();

        if(detectorRail==null || Material.DETECTOR_RAIL!=detectorRail.getType()) {
            return;
        }

        Location triggerLocation=detectorRail.getLocation();
        DropoffPoint dropoffPoint=this.dropoffPointListener.getDropoffPoints().get(triggerLocation.toString());


        if(dropoffPoint!=null && event.getNewCurrent()==1) {
            lookForMinecart(event, dropoffPoint);
        }
    }

    private void lookForMinecart(BlockRedstoneEvent event, DropoffPoint dropoffPoint) {
        Block block=event.getBlock();
        Chunk chunk=block.getChunk();
        Entity entities[]=chunk.getEntities();

        if(entities!=null) {
            for(Entity entity:entities) {
                if(isValidStorageMinecart(entity, block.getLocation())) {
                    StorageMinecart storageMinecart=(StorageMinecart) entity;
                    transferItemsToDropoffPoint(storageMinecart, dropoffPoint);
                }
            }
        }
    }

    private boolean isValidStorageMinecart(Entity entity, Location location) {
        if(!(entity instanceof StorageMinecart)) {
            return false;
        }

        StorageMinecart storageMinecart=(StorageMinecart) entity;
        double distanceToDetectorRail=storageMinecart.getLocation().distance(location);

        return distanceToDetectorRail <= 1.5;
    }

    private void transferItemsToDropoffPoint(StorageMinecart storageMinecart, DropoffPoint dropoffPoint) {
        Inventory minecartInventory=storageMinecart.getInventory();

        List<Location> chestLocations=dropoffPoint.getChestLocations();
        List<Chest> dropoffChests=new ArrayList<Chest>();

        for(Location location:chestLocations) {
            dropoffChests.add((Chest)location.getBlock().getState());
        }

        ItemStack[] minecartItems=minecartInventory.getContents();

        if(minecartItems!=null) {
            for(int i=0; i<minecartItems.length; i++) {
                ItemStack item=minecartItems[i];
                if(item!=null && item.getAmount()>0) {
                    addToAvailableDropoffChest(i, minecartInventory, item, dropoffChests);
                }
            }
        }

        // update state of all chests
        for(Chest chest:dropoffChests) {
            chest.update();
        }
    }

    private void addToAvailableDropoffChest(int inventoryIndex, Inventory minecartInventory, ItemStack item, List<Chest> dropoffChests) {
        List<Chest> duplicateListOfChests=new ArrayList<Chest>(dropoffChests);

        Iterator<Chest> iterator=duplicateListOfChests.iterator();
        Map<Integer, ItemStack> excess=new HashMap<Integer, ItemStack>();

        while(iterator.hasNext()) {
            Chest chest=iterator.next();

            excess.putAll(chest.getInventory().addItem(item));

            if(excess.size()>0) {
                for(Integer key:excess.keySet()) {
                    ItemStack excessItem=excess.get(key);
                    item.setAmount(excessItem.getAmount());
                }

                excess.clear();
                iterator.remove();
            } else {
                item=null;
                break;
            }
        }

        minecartInventory.setItem(inventoryIndex, item);
    }
}
