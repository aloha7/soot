package nl.jamiecraane.mover;

import java.util.List;
import java.util.ArrayList;

/**
 * A van can hold boxes and has a maximum capacity.
 */
public class Van {
    private double capacity;
    private List<Box> contents = new ArrayList();
    private double volumeOfContents = 0.0D;

    /**
     * Creates a new instance with a specified capacity.
     * @param capacity
     */
    public Van(double capacity) {
        this.capacity = capacity;
    }

    /**
     * Adds the specified box when the capacity is sufficient.
     * @param box
     * @return True when the box is added, false otherwise                                                
     */
    public boolean addBox(Box box) {
        if (this.volumeOfContents + box.getVolume() > this.capacity) {
            return false;
        }

        this.contents.add(box);
        this.volumeOfContents += box.getVolume();
        return true;
    }

    /**
     * @return The total volume of the contents in the van.
     */
    public double getVolumeOfContents() {
        return volumeOfContents;
    }

    /**
     * @return The contents in the van.
     */
    public List<Box> getContents() {
        return contents;
    }
}
