package nl.jamiecraane.mover;

/**
 * Box where items can be put in.
 */
public class Box {
    private int id;
    // The volume of the box
	private double volume;

    public Box(double volume) {
        this.volume = volume;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

	public double getVolume() {
		return this.volume;
	}
	
	@Override
	public String toString() {
		return "Box:" + this.id + ", volume [" + this.getVolume() + "] cubic metres.";
	}
}
