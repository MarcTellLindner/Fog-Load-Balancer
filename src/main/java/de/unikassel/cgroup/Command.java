package de.unikassel.cgroup;

/**
 * Commands usable with cgroups.
 */
public enum Command {
    CGCREATE,
    CGDELETE,
    CGSET,
    CGGET,
    CGEXECUTE,
    CGCLASSIFY;

    private final String lowerCaseName = this.name().toLowerCase();

    @Override
    public String toString() {
        return lowerCaseName;
    }
}
