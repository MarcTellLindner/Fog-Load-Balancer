package de.unikassel.cgroup;

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
