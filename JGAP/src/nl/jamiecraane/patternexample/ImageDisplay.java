package nl.jamiecraane.patternexample;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.InputStream;

import javax.imageio.ImageIO;
import javax.swing.JFrame;

/**
 * Simple frame which displays a new image every time the population is
 * evoluted to a new best fitness value.
 */
public class ImageDisplay extends JFrame {
	public ImageDisplay() throws Exception {
		a = new javax.swing.JLabel();
		setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
		InputStream is = this.getClass().getResourceAsStream("pattern.gif");
		BufferedImage targetImage = ImageIO.read(is);
		is.close();
		a.setIcon(new javax.swing.ImageIcon(targetImage));
		add(a);
		pack();
	}

    /**
     * Called by the PatternRegocnition class when the image is changed.
     * @param newImage
     */
    public void imageChanged(Image newImage) {
		a.setIcon(new javax.swing.ImageIcon(newImage));
		a.updateUI();
	}

	public static void main(String args[]) throws Exception {
		ImageDisplay display = new ImageDisplay();
		PatternRecognition pr = new PatternRecognition(display);
		display.setVisible(true);
		pr.start();
	}

	private javax.swing.JLabel a;
}
