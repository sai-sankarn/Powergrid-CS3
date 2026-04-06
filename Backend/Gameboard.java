package Backend;

import java.util.*;

public class Gameboard {
    private Map<City, List<Connection>> adjacencyList;
    private Set<City> activeCities;

    public Gameboard() {
        this.adjacencyList = new HashMap<>();
        this.activeCities = new HashSet<>();
    }

    // --- Getters ---

    public Map<City, List<Connection>> getAdjacencyList() { return adjacencyList; }

    public Set<City> getActiveCities() { return activeCities; }

    public void setActiveCities(Set<City> cities) { this.activeCities = cities; }

    // --- Map setup helpers ---

    /**
     * Adds a city to the board's adjacency list and active set.
     */
    public void addCity(City city) {
        adjacencyList.putIfAbsent(city, new ArrayList<>());
        activeCities.add(city);
    }

    /**
     * Adds a bidirectional edge between two cities.
     */
    public void addConnection(City a, City b, int cost) {
        adjacencyList.computeIfAbsent(a, k -> new ArrayList<>()).add(new Connection(b, cost));
        adjacencyList.computeIfAbsent(b, k -> new ArrayList<>()).add(new Connection(a, cost));
    }

    /**
     * Returns the City object with the given name, or null if not found.
     */
    public City getCityByName(String name) {
        for (City c : adjacencyList.keySet()) {
            if (c.getName().equalsIgnoreCase(name)) return c;
        }
        return null;
    }

    // --- Core game methods ---

    /**
     * Multi-source Dijkstra from every city in playerNetwork to targetCity.
     * Returns the cheapest total connection cost to reach targetCity.
     * Returns 0 if targetCity is already in playerNetwork (no cost).
     * Returns Integer.MAX_VALUE if unreachable.
     */
    public int calculateConnectionCost(Set<City> playerNetwork, City targetCity) {
        if (playerNetwork.contains(targetCity)) return 0;
        if (playerNetwork.isEmpty()) return 0; // first city: flat cost, no connection needed

        // dist map: cheapest known cost to reach each city from any source
        Map<City, Integer> dist = new HashMap<>();
        PriorityQueue<CityDist> pq = new PriorityQueue<>(Comparator.comparingInt(cd -> cd.dist));

        // initialize: all player-owned cities are sources with distance 0
        for (City source : playerNetwork) {
            dist.put(source, 0);
            pq.add(new CityDist(source, 0));
        }

        while (!pq.isEmpty()) {
            CityDist current = pq.poll();
            int curDist = current.dist;
            City curCity = current.city;

            // skip if we already found a better path
            if (curDist > dist.getOrDefault(curCity, Integer.MAX_VALUE)) continue;

            // found the target
            if (curCity.equals(targetCity)) return curDist;

            List<Connection> neighbors = adjacencyList.getOrDefault(curCity, new ArrayList<>());
            for (Connection conn : neighbors) {
                City neighbor = conn.getTarget();
                if (!activeCities.contains(neighbor)) continue; // skip inactive cities
                int newDist = curDist + conn.getCost();
                if (newDist < dist.getOrDefault(neighbor, Integer.MAX_VALUE)) {
                    dist.put(neighbor, newDist);
                    pq.add(new CityDist(neighbor, newDist));
                }
            }
        }

        return dist.getOrDefault(targetCity, Integer.MAX_VALUE);
    }

    /**
     * Returns the slot cost for the next available slot in this city.
     * Returns -1 if no slot is available for the current step.
     */
    public int getCitySlotCost(City city) {
        Player[] occupants = city.getOccupants();
        int[] costs = city.getSlotCosts();
        for (int i = 0; i < occupants.length; i++) {
            if (occupants[i] == null) return costs[i];
        }
        return -1; // city full
    }

    /**
     * Validates and executes a player building in a city.
     * Checks:
     *   - City is in activeCities
     *   - City has an open slot for the current step
     *   - Player doesn't already occupy this city
     *   - Player can afford the total cost
     * Charges the player, fills the slot, updates player.ownedCities.
     * Returns true if successful.
     */
    public boolean buildCitySuccess(Player player, City city, int currentStep) {
        // city must be in the active playing zone
        if (!activeCities.contains(city)) return false;

        // city must have an open slot for this step
        if (!city.hasOpenSlot(currentStep)) return false;

        // player can't build in a city they already occupy
        if (city.isOccupiedBy(player)) return false;

        // calculate cost
        int slotCost = city.getNextSlotCost(currentStep);
        if (slotCost < 0) return false;

        // check affordability (connection cost must be calculated and added by caller)
        if (!player.canAfford(slotCost)) return false;

        // charge player and fill slot
        player.spendMoney(slotCost);
        city.addOccupant(player, currentStep);
        player.addCity(city);

        return true;
    }

    /**
     * Full build: calculates connection cost + slot cost, validates, and executes.
     * Use this version when the player already has at least one city.
     * Returns the total cost paid, or -1 if the build failed.
     */
    public int buildCity(Player player, City targetCity, int currentStep) {
        if (!activeCities.contains(targetCity)) return -1;
        if (!city(targetCity).hasOpenSlot(currentStep)) return -1;
        if (targetCity.isOccupiedBy(player)) return -1;

        int connectionCost = calculateConnectionCost(player.getOwnedCities(), targetCity);
        int slotCost = targetCity.getNextSlotCost(currentStep);
        int totalCost = connectionCost + slotCost;

        if (!player.canAfford(totalCost)) return -1;

        player.spendMoney(totalCost);
        targetCity.addOccupant(player, currentStep);
        player.addCity(targetCity);

        return totalCost;
    }

    // helper to avoid name collision
    private City city(City c) { return c; }

    // --- German map initialization ---

    /**
     * Hardcodes all 42 German cities and their connections.
     * Edge costs are taken from the standard Power Grid Germany map.
     */
    public void initializeGermanMap() {
        // --- Create all cities ---
        City flensburg    = new City("Flensburg");
        City kiel         = new City("Kiel");
        City hamburg      = new City("Hamburg");
        City schwerin     = new City("Schwerin");
        City wilhelmshaven= new City("Wilhelmshaven");
        City bremen       = new City("Bremen");
        City hannover     = new City("Hannover");
        City berlin       = new City("Berlin");
        City rostock      = new City("Rostock");
        City torgelow     = new City("Torgelow");
        City osnabrueck   = new City("Osnabrück");
        City dortmund     = new City("Dortmund");
        City munster      = new City("Münster");
        City bielefeld    = new City("Bielefeld");
        City magdeburg    = new City("Magdeburg");
        City halle        = new City("Halle");
        City leipzig      = new City("Leipzig");
        City cottbus      = new City("Cottbus");
        City duisburg     = new City("Duisburg");
        City dusseldorf   = new City("Düsseldorf");
        City essen        = new City("Essen");
        City koln         = new City("Köln");
        City aachen       = new City("Aachen");
        City kassel       = new City("Kassel");
        City erfurt       = new City("Erfurt");
        City dresden      = new City("Dresden");
        City siegen       = new City("Siegen");
        City fulda        = new City("Fulda");
        City frankfurt    = new City("Frankfurt");
        City wiesbaden    = new City("Wiesbaden");
        City trier        = new City("Trier");
        City mannheim     = new City("Mannheim");
        City saarbrucken  = new City("Saarbrücken");
        City wuerzburg    = new City("Würzburg");
        City nurnberg     = new City("Nürnberg");
        City stuttgart    = new City("Stuttgart");
        City freiburg     = new City("Freiburg");
        City ulm          = new City("Ulm");
        City augsburg     = new City("Augsburg");
        City regensburg   = new City("Regensburg");
        City munchen      = new City("München");
        City passau       = new City("Passau");

        // --- Register all cities ---
        City[] all = {
            flensburg, kiel, hamburg, schwerin, wilhelmshaven, bremen, hannover,
            berlin, rostock, torgelow, osnabrueck, dortmund, munster, bielefeld,
            magdeburg, halle, leipzig, cottbus, duisburg, dusseldorf, essen,
            koln, aachen, kassel, erfurt, dresden, siegen, fulda, frankfurt,
            wiesbaden, trier, mannheim, saarbrucken, wuerzburg, nurnberg, stuttgart,
            freiburg, ulm, augsburg, regensburg, munchen, passau
        };
        for (City c : all) addCity(c);

        // --- Connections (bidirectional, cost per edge) ---
        addConnection(flensburg, kiel, 4);
        addConnection(flensburg, hamburg, 8);
        addConnection(kiel, hamburg, 8);
        addConnection(kiel, schwerin, 16);
        addConnection(hamburg, schwerin, 8);
        addConnection(hamburg, wilhelmshaven, 11);
        addConnection(hamburg, hannover, 17);
        addConnection(hamburg, bremen, 13);
        addConnection(schwerin, rostock, 10);
        addConnection(schwerin, torgelow, 19);
        addConnection(schwerin, berlin, 19);
        addConnection(schwerin, hannover, 20);
        addConnection(wilhelmshaven, bremen, 12);
        addConnection(rostock, torgelow, 17);
        addConnection(torgelow, berlin, 16);
        addConnection(berlin, magdeburg, 10);
        addConnection(berlin, cottbus, 16);
        addConnection(berlin, halle, 20);
        addConnection(bremen, hannover, 10);
        addConnection(bremen, osnabrueck, 11);
        addConnection(hannover, magdeburg, 15);
        addConnection(hannover, bielefeld, 17);
        addConnection(hannover, kassel, 18);
        addConnection(magdeburg, halle, 11);
        addConnection(halle, leipzig, 6);
        addConnection(halle, erfurt, 12);
        addConnection(halle, kassel, 20);
        addConnection(leipzig, erfurt, 12);
        addConnection(leipzig, dresden, 14);
        addConnection(leipzig, cottbus, 19);
        addConnection(cottbus, dresden, 16);
        addConnection(dresden, erfurt, 20);
        addConnection(osnabrueck, munster, 7);
        addConnection(osnabrueck, bielefeld, 12);
        addConnection(osnabrueck, dortmund, 15);
        addConnection(munster, dortmund, 8);
        addConnection(munster, duisburg, 12);
        addConnection(bielefeld, dortmund, 11);
        addConnection(bielefeld, kassel, 18);
        addConnection(dortmund, essen, 6);
        addConnection(dortmund, duisburg, 6);
        addConnection(dortmund, siegen, 15);
        addConnection(essen, duisburg, 4);
        addConnection(duisburg, dusseldorf, 4);
        addConnection(dusseldorf, koln, 9);
        addConnection(dusseldorf, aachen, 18);
        addConnection(koln, aachen, 14);
        addConnection(koln, siegen, 14);
        addConnection(koln, trier, 20);
        addConnection(koln, wiesbaden, 21);
        addConnection(aachen, trier, 19);
        addConnection(siegen, fulda, 16);
        addConnection(siegen, frankfurt, 19);
        addConnection(kassel, erfurt, 13);
        addConnection(kassel, fulda, 11);
        addConnection(erfurt, fulda, 16);
        addConnection(erfurt, wuerzburg, 20);
        addConnection(fulda, wuerzburg, 12);
        addConnection(fulda, frankfurt, 11);
        addConnection(frankfurt, wiesbaden, 7);
        addConnection(frankfurt, wuerzburg, 14);
        addConnection(wiesbaden, mannheim, 14);
        addConnection(wiesbaden, saarbrucken, 17);
        addConnection(trier, saarbrucken, 11);
        addConnection(saarbrucken, mannheim, 11);
        addConnection(saarbrucken, stuttgart, 16);
        addConnection(mannheim, wuerzburg, 10);
        addConnection(mannheim, stuttgart, 10);
        addConnection(wuerzburg, nurnberg, 11);
        addConnection(nurnberg, regensburg, 12);
        addConnection(nurnberg, augsburg, 18);
        addConnection(nurnberg, stuttgart, 20);
        addConnection(stuttgart, ulm, 10);
        addConnection(stuttgart, freiburg, 16);
        addConnection(freiburg, ulm, 19);
        addConnection(ulm, augsburg, 9);
        addConnection(ulm, munchen, 19);
        addConnection(augsburg, munchen, 8);
        addConnection(munchen, regensburg, 16);
        addConnection(munchen, passau, 14);
        addConnection(regensburg, passau, 12);
    }

    // --- Inner helper class for Dijkstra ---

    private static class CityDist {
        City city;
        int dist;
        CityDist(City city, int dist) {
            this.city = city;
            this.dist = dist;
        }
    }
}