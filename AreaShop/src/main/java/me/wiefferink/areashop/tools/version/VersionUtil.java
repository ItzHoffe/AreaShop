package me.wiefferink.areashop.tools.version;

public class VersionUtil {

    public static VersionData MC_1_18_2 = new VersionData(1, 18, 2);

    public static Version parseMinecraftVersion(String minecraftVersion) {
        // Expecting 1.X.X-R0.1-SNAPSHOT
        int stripLength = "-R0.1-SNAPSHOT".length();
        int length = minecraftVersion.length();
        if (length <= stripLength) {
            throw new IllegalArgumentException("Invalid minecraft version: " + minecraftVersion);
        }
        String strippedVersion = minecraftVersion.substring(0, length - stripLength);
        try {
            return Version.parse(strippedVersion);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid minecraft version: " + minecraftVersion, ex);
        }
    }

}
