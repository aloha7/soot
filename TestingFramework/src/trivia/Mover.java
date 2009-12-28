package trivia;

/** An example class to illustrate Jumble coverage. */
public class Mover
{
  /** How much slower to travel in the horizontal direction */
  protected final int SLOWER = 5;

  protected int x,y;

  /** Move this object in one of the four possible directions.
   *  Horizontal movements have less effect than vertical ones.
   *
   *  @throws RuntimeException if direction is not valid
   *  @param  direction must be "left", "right", "up" or "down".
   *  @param  speed how far/fast we would like it to move.
   */
  public void move(String direction, int speed)
  {
    if (direction.equals("up")) {
      y -= speed;
    }
    else if (direction.equals("down")) {
      y += speed;
    }
    else if (direction.equals("left")) {
      x -= speed / SLOWER;
    }
    else if (direction.equals("right")) {
      x += speed / SLOWER;
    }
    else {
      throw new RuntimeException("illegal direction: "+direction);
    }
  }

  public String toString()
  {
    return Integer.toString(x) + "," + Integer.toString(y);
  }

  public String prettyString()
  {
    return "(" + Integer.toString(x) + "," + Integer.toString(y) + ")";
  }
}
