package de.unikassel.cgroup;

import java.io.Serializable;

@FunctionalInterface
public interface CGroupBuilder extends Serializable {
    CGroup buildCGroup(double... predictions);
}
