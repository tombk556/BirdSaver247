package htwd.s224.gruppe1.mnbirdsaver.util;

import androidx.annotation.NonNull;

public class WindTurbine {
    private int id;
    private String name;
    private String ipAddress;

    public WindTurbine(int id, String name, String ipAddress) {
        this.id = id;
        this.name = name;
        this.ipAddress = ipAddress;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    @NonNull
    @Override
    public String toString() {
        if (!ipAddress.isEmpty()){
            return String.format("%s (%s)", name, ipAddress);
        } else {
            return name;
        }
    }
}
