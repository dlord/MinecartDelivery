/*
 * MinecartDelivery - Bukkit plugin to allow automated storage minecart deliveries
 *
 * Copyright 2011 John Paul Alcala
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package mcd;

import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

public class MinecartDelivery extends JavaPlugin {
    private static final Logger log=Logger.getLogger("Minecraft");
    private static final String DETECTOR_RAILS_LIST_CONFIG="mcd.detector.rails.locations";
    private DropoffPointListener dropoffPointListener;
    private DetectorRailBlockListener detectorRailBlockListener;

    private final List<Vector> detectorRailLocations=new ArrayList<Vector>();

    public List<Vector> getDetectorRailLocations() {
        return detectorRailLocations;
    }

    @Override
    public void onDisable() {
        destroyListeners();
        log.info("MinecartDelivery has been unloaded!");
    }

    @Override
    public void onEnable() {
        loadConfig();
        configureListeners();

        PluginManager pluginManager=this.getServer().getPluginManager();
        pluginManager.registerEvent(Event.Type.BLOCK_PLACE, this.dropoffPointListener, Event.Priority.Normal, this);
        pluginManager.registerEvent(Event.Type.BLOCK_BREAK, this.dropoffPointListener, Event.Priority.Normal, this);
        pluginManager.registerEvent(Event.Type.REDSTONE_CHANGE, this.detectorRailBlockListener, Event.Priority.Normal, this);

        log.info("MinecartDelivery has been loaded!");
    }

    private void configureListeners() {
        this.dropoffPointListener =new DropoffPointListener();
        this.dropoffPointListener.setMinecartDelivery(this);
        this.detectorRailBlockListener=new DetectorRailBlockListener();
        this.detectorRailBlockListener.setDropoffPointListener(this.dropoffPointListener);
    }

    private void destroyListeners() {
        this.dropoffPointListener.setMinecartDelivery(null);
        this.detectorRailBlockListener.setDropoffPointListener(null);

        this.dropoffPointListener=null;
        this.detectorRailBlockListener=null;
    }

    private void loadConfig() {
        List<String> detectorRailsList=getConfig().getStringList(DETECTOR_RAILS_LIST_CONFIG);

        if(detectorRailsList!=null) {
            for(String coordinates:detectorRailsList) {
                String point[]=coordinates.split(",");
                Vector v=new Vector(Double.valueOf(point[0]), Double.valueOf(point[1]), Double.valueOf(point[2]));

                detectorRailLocations.add(v);
            }
        }
    }

    protected void addDetectorRailToConfig(Location detectorRailLocation) {
        Vector vectorLocation=detectorRailLocation.toVector();

        if(detectorRailLocations.contains(vectorLocation)) {
            return;
        }

        detectorRailLocations.add(vectorLocation);
        saveDetectorRailConfig();
    }

    protected void removeDetectorRailFromConfig(Location detectorRailLocation) {
        Vector vectorLocation=detectorRailLocation.toVector();

        if(!detectorRailLocations.contains(vectorLocation)) {
            return;
        }

        detectorRailLocations.remove(vectorLocation);
        saveDetectorRailConfig();
    }

    private void saveDetectorRailConfig() {
        List<String> forSaving=new ArrayList<String>();
        List<Vector> copy=new ArrayList<Vector>(detectorRailLocations);

        for(Vector vector:copy) {
            StringBuilder sb=new StringBuilder();
            sb.append(vector.getX());
            sb.append(',');
            sb.append(vector.getY());
            sb.append(',');
            sb.append(vector.getZ());

            forSaving.add(sb.toString());
        }

        getConfig().set(DETECTOR_RAILS_LIST_CONFIG, forSaving);
        saveConfig();
    }
}
