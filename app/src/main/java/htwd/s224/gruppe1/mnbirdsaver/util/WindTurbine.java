/**
 * Represents a wind turbine with an ID, name, and IP address.
 */
package htwd.s224.gruppe1.mnbirdsaver.util;

import androidx.annotation.NonNull;

public class WindTurbine {
    private int id;
    private String name;
    private String ipAddress;

    /**
     * Constructs a WindTurbine with the specified ID, name, and IP address.
     *
     * @param id The ID of the wind turbine.
     * @param name The name of the wind turbine.
     * @param ipAddress The IP address of the wind turbine.
     */
    public WindTurbine(int id, String name, String ipAddress) {
        this.id = id;
        this.name = name;
        this.ipAddress = ipAddress;
    }

    /**
     * Returns the ID of the wind turbine.
     *
     * @return The ID of the wind turbine.
     */
    public int getId() {
        return id;
    }

    /**
     * Returns the name of the wind turbine.
     *
     * @return The name of the wind turbine.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the IP address of the wind turbine.
     *
     * @return The IP address of the wind turbine.
     */
    public String getIpAddress() {
        return ipAddress;
    }

    /**
     * Returns a string representation of the wind turbine.
     * If the IP address is not empty, the format will be "name (ipAddress)".
     * Otherwise, it will return just the name.
     *
     * @return A string representation of the wind turbine.
     */
    @NonNull
    @Override
    public String toString() {
        if (!ipAddress.isEmpty()) {
            return String.format("%s (%s)", name, ipAddress);
        } else {
            return name;
        }
    }
}
