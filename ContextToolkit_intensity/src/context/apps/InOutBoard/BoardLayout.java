package context.apps.InOutBoard;

import java.awt.Container;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;


/**
 * The <code>BoardLayout</code> class is a layout manager that 
 * lays out a container's components in a rectangular grid. 
 * <p>
 * The container is divided into equal-sized rectangles, 
 * and one component is placed in each rectangle. 
 * <p>
 * This layout has the following differences with GridLayout:
 * - it honors the preferred size (from getPreferredSize) of the components
 * - thus, it will show blank space if there aren't enough components
 * - it also keeps columns equally filled but fills column after column
 *   instead of row after row
 * - it doesn't eat components' bounding boxes to honor spacing
 * - provides margin gap spacing all around the container
 *
 * @version 1.0, 09/27/98
 * @author 	Daniel Salber <salber@acm.org>
 */
public class BoardLayout implements LayoutManager, java.io.Serializable {

    private int hgap;
    private int vgap;
    private int rows;
    private int cols;
    private int mgap;

		private final boolean DEBUG = false;
		
    /**
     * Creates a grid layout with a default of one column per component,
     * in a single row.
     */
    public BoardLayout() {
			this(1, 0, 0, 0, 0);
    }

    /**
     * Creates a grid layout with the specified number of rows and 
     * columns. All components in the layout are given equal size.
     * <p>
     * One, but not both, of <code>rows</code> and <code>cols</code> can 
     * be zero, which means that any number of objects can be placed in a 
     * row or in a column. 
     * @param     rows   the rows, with the value zero meaning 
     *                   any number of rows.
     * @param     cols   the columns, with the value zero meaning 
     *                   any number of columns.
     */
    public BoardLayout(int rows, int cols) {
			this(rows, cols, 0, 0, 0);
    }

    /**
     * Creates a grid layout with the specified number of rows and 
     * columns. All components in the layout are given equal size.
     * <p>
     * In addition, the horizontal and vertical gaps are set to the 
     * specified values. Horizontal gaps are placed at the left and 
     * right edges, and between each of the columns. Vertical gaps are 
     * placed at the top and bottom edges, and between each of the rows.
     * Gaps take extra space. The component's display size isn't altered.
     * <p>
     * One, but not both, of <code>rows</code> and <code>cols</code> can 
     * be zero, which means that any number of objects can be placed in a 
     * row or in a column. 
     * @param     rows   the rows, with the value zero meaning 
     *                   any number of rows.
     * @param     cols   the columns, with the value zero meaning 
     *                   any number of columns.
     * @param     hgap   the horizontal gap. 
     * @param     vgap   the vertical gap. 
     * @param     agap   margin gap. 
     * @exception   IllegalArgumentException  if the of <code>rows</code> 
     *                   or <code>cols</code> is invalid.
     */
    public BoardLayout(int rows, int cols, int hgap, int vgap, int mgap) {
			if ((rows == 0) && (cols == 0)) {
			    throw new IllegalArgumentException("rows and cols cannot both be zero");
			}
			this.rows = rows;
			this.cols = cols;
			this.hgap = hgap;
			this.vgap = vgap;
			this.mgap = mgap;
    }

    /**
     * Gets the number of rows in this layout.
     * @return    the number of rows in this layout.
     */
    public int getRows() {
			return rows;
    }

    /**
     * Sets the number of rows in this layout to the specified value.
     * @param        rows   the number of rows in this layout.
     * @exception    IllegalArgumentException  if the value of both 
     *               <code>rows</code> and <code>cols</code> is set to zero.
     */
    public void setRows(int rows) {
			if ((rows == 0) && (this.cols == 0)) {
			    throw new IllegalArgumentException("rows and cols cannot both be zero");
			}
			this.rows = rows;
    }

    /**
     * Gets the number of columns in this layout.
     * @return     the number of columns in this layout.
     */
    public int getColumns() {
			return cols;
    }

    /**
     * Sets the number of columns in this layout to the specified value.
     * @param        cols   the number of columns in this layout.
     * @exception    IllegalArgumentException  if the value of both 
     *               <code>rows</code> and <code>cols</code> is set to zero.
     */
    public void setColumns(int cols) {
			if ((cols == 0) && (this.rows == 0)) {
			    throw new IllegalArgumentException("rows and cols cannot both be zero");
			}
			this.cols = cols;
    }

    /**
     * Gets the horizontal gap between components.
     * @return       the horizontal gap between components.
     */
    public int getHgap() {
			return hgap;
    }
    
    /**
     * Sets the horizontal gap between components to the specified value.
     * @param        hgap   the horizontal gap between components.
     */
    public void setHgap(int hgap) {
			this.hgap = hgap;
    }
    
    /**
     * Gets the vertical gap between components.
     * @return       the vertical gap between components.
     */
    public int getVgap() {
			return vgap;
    }
    
    /**
     * Sets the vertical gap between components to the specified value.
     * @param         vgap  the vertical gap between components.
     */
    public void setVgap(int vgap) {
			this.vgap = vgap;
    }

    /**
     * Gets the margin gap between components and the container.
     * @return       the margin gap.
     */
    public int getMgap() {
			return mgap;
    }
    
    /**
     * Sets the margin gap between components and the container to the specified value.
     * @param         mgap  the margin gap.
     */
    public void setMgap(int mgap) {
			this.mgap = mgap;
    }

    /**
     * Adds the specified component with the specified name to the layout.
     * @param name the name of the component.
     * @param comp the component to be added.
     */
    public void addLayoutComponent(String name, Component comp) {
    }

    /**
     * Removes the specified component from the layout. 
     * @param comp the component to be removed.
     * @since JDK1.0
     */
    public void removeLayoutComponent(Component comp) {
    }

    /** 
     * Determines the preferred size of the container argument using 
     * this grid layout. 
     * <p>
     * The preferred width of a grid layout is the largest preferred 
     * width of any of the widths in the container times the number of 
     * columns, plus the horizontal padding times the number of columns 
     * plus one, plus the left and right insets of the target container. 
     * <p>
     * The preferred height of a grid layout is the largest preferred 
     * height of any of the widths in the container times the number of 
     * rows, plus the vertical padding times the number of rows plus one, 
     * plus the top and left insets of the target container. 
     * 
     * @param     target   the container in which to do the layout.
     * @return    the preferred dimensions to lay out the 
     *                      subcomponents of the specified container.
     * @see       java.awt.BoardLayout#minimumLayoutSize 
     * @see       java.awt.Container#getPreferredSize()
     */
    public Dimension preferredLayoutSize(Container parent) {
			Insets insets = parent.getInsets();
			int ncomponents = parent.getComponentCount();
			int nrows = rows;
			int ncols = cols;

			if (nrows > 0) {
			    ncols = (ncomponents + nrows - 1) / nrows;
			} else {
			    nrows = (ncomponents + ncols - 1) / ncols;
			}
			int w = 0;
			int h = 0;
			for (int i = 0 ; i < ncomponents ; i++) {
			    Component comp = parent.getComponent(i);
			    Dimension d = comp.getPreferredSize();
			    if (w < d.width) {
						w = d.width;
			    }
			    if (h < d.height) {
						h = d.height;
			    }
			}
			return new Dimension(insets.left + insets.right + 2*mgap + ncols*w + (ncols-1)*hgap, 
					     insets.top + insets.bottom + 2*mgap + nrows*h + (nrows-1)*vgap);
    }

    /**
     * Determines the minimum size of the container argument using this 
     * grid layout. 
     * <p>
     * The minimum width of a grid layout is the largest minimum width 
     * of any of the widths in the container times the number of columns, 
     * plus the horizontal padding times the number of columns plus one, 
     * plus the left and right insets of the target container. 
     * <p>
     * The minimum height of a grid layout is the largest minimum height 
     * of any of the widths in the container times the number of rows, 
     * plus the vertical padding times the number of rows plus one, plus 
     * the top and left insets of the target container. 
     *  
     * @param       target   the container in which to do the layout.
     * @return      the minimum dimensions needed to lay out the 
     *                      subcomponents of the specified container.
     * @see         java.awt.BoardLayout#preferredLayoutSize
     * @see         java.awt.Container#doLayout
     */
    public Dimension minimumLayoutSize(Container parent) {
			Insets insets = parent.getInsets();
			int ncomponents = parent.getComponentCount();
			int nrows = rows;
			int ncols = cols;

			if (nrows > 0) {
			    ncols = (ncomponents + nrows - 1) / nrows;
			} else {
			    nrows = (ncomponents + ncols - 1) / ncols;
			}
			int w = 0;
			int h = 0;
			for (int i = 0 ; i < ncomponents ; i++) {
			    Component comp = parent.getComponent(i);
			    Dimension d = comp.getMinimumSize();
			    if (w < d.width) {
						w = d.width;
			    }
			    if (h < d.height) {
						h = d.height;
			    }
			}
			return new Dimension(insets.left + insets.right + 2*mgap + ncols*w + (ncols-1)*hgap, 
					     insets.top + insets.bottom + 2*mgap + nrows*h + (nrows-1)*vgap);
    }

    /** 
     * Lays out the specified container using this layout. 
     * <p>
     * This method reshapes the components in the specified target 
     * container in order to satisfy the constraints of the 
     * <code>BoardLayout</code> object. 
     * <p>
     * The grid layout manager determines the size of individual 
     * components by dividing the free space in the container into 
     * equal-sized portions according to the number of rows and columns 
     * in the layout. The container's free space equals the container's 
     * size minus any insets and any specified horizontal or vertical 
     * gap. All components in a grid layout are given the same size. 
     *  
     * @param      target   the container in which to do the layout.
     * @see        java.awt.Container
     * @see        java.awt.Container#doLayout
     */
    public void layoutContainer(Container parent) {
    
    	if (DEBUG)
	    	System.out.println("in layoutContainer");
    	
			Insets insets = parent.getInsets();
			int ncomponents = parent.getComponentCount();
			int nrows = rows;
			int ncols = cols;
			
			Dimension size = parent.getSize ();
			int width = size.width;
			int height = size.height;

			if (ncomponents == 0) {
			    return;
			}
			
			if (nrows > 0) {
			    nrows = (ncomponents + ncols - 1) / ncols;
			} else {
			    ncols = (ncomponents + nrows - 1) / nrows;
			}
			
			/*
			int w = width - (insets.left + insets.right);
			int h = height - (insets.top + insets.bottom);
			w = (w - (ncols - 1) * hgap) / ncols;
			h = (h - (nrows - 1) * vgap) / nrows;
			*/
			
			Dimension sz = parent.getComponent(0).getSize();
			int w = sz.width;
			int h = sz.height;
			if (DEBUG) {
				System.out.println ("Got w=" + w + ",h=" + h);
				System.out.println ("Got ncols=" + ncols + ",nrows=" + nrows+",N="+ncomponents);
			}
				
		  for (int r = 0, y = insets.top + mgap ; r < nrows ; r++, y += h + vgap) {
				for (int c = 0, x = insets.left + mgap ; c < ncols ; c++, x += w + hgap) {
					int i = c * nrows + r;
					if (i < ncomponents) {
						if (DEBUG)
							System.out.println ("Setting  " + i + " at " + x + "," + y);
			    	parent.getComponent(i).setLocation(x, y);
					}
		 		}
			}
    }
    
    /**
     * Returns the string representation of this grid layout's values.
     * @return     a string representation of this grid layout.
     */
    public String toString() {
			return getClass().getName() + "[hgap=" + hgap + ",vgap=" + vgap + 
			    			       ",mgap=" + mgap + ",rows=" + rows + ",cols=" + cols + "]";
    }
}
