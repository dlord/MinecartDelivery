package mcd;

import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;

public class DropoffPoint {
    private final List<Location> chestLocations=new ArrayList<Location>();

    public List<Location> getChestLocations() {
        return chestLocations;
    }
}
