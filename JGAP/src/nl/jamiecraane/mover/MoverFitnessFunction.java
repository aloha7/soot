package nl.jamiecraane.mover;

import org.jgap.FitnessFunction;
import org.jgap.IChromosome;

/**
 * Fitness function for the Mover example. See this
 * {@link #evaluate(IChromosome)} for the actual fitness function.
 */
public class MoverFitnessFunction extends FitnessFunction {
	private Box[] boxes;
	private double vanCapacity;

	public void setVanCapacity(double vanCapacity) {
		this.vanCapacity = vanCapacity;
	}

	public void setBoxes(Box[] boxes) {
		this.boxes = boxes;
	}

	/**
	 * Fitness function. A lower value value means the difference between the
	 * total volume of boxes in a van is small, which is better. This means a
	 * more optimal distribution of boxes in the vans. The number of vans needed
	 * is multiplied by the size difference as more vans are more expensive.
	 */
	@Override
	protected double evaluate(IChromosome a_subject) {
		double wastedVolume = 0.0D;

		double sizeInVan = 0.0D;
		int numberOfVansNeeded = 1;
		for (int i = 0; i < boxes.length; i++) {
			int index = (Integer) a_subject.getGene(i).getAllele();
			if ((sizeInVan + this.boxes[index].getVolume()) <= vanCapacity) {
				sizeInVan += this.boxes[index].getVolume();
			} else {
				// Compute the difference
				numberOfVansNeeded++;
				wastedVolume += Math.abs(vanCapacity - sizeInVan);
				// Make sure we put the box which did not fit in this van in the next van
				sizeInVan = this.boxes[index].getVolume();
			}
		}
		// Take into account the number of vans needed. More vans produce a higher fitness value.
		return wastedVolume * numberOfVansNeeded;
    }
}
