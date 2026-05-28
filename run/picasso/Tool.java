import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;

// Klasse, um Tools zu definieren
public abstract class Tool {

    public final Painting painting;
    public final String name; // Name des Tools

    public Tool(Painting painting, String name){
        this.painting = painting;
        this.name = name;
    }

    // Hilfsmethode, um zu checken ob ein Punkt innerhalb eines Bilds ist
    public boolean isInsideImage(BufferedImage image, Point point){
        return point.getX() >= 0 && point.getY() >= 0 && point.getX() < image.getWidth() && point.getY() < image.getHeight();
    }
    
    public abstract void onHover(int x, int y, Color color, Graphics2D hoverGraphics);
    public abstract void onMousePressed(int x, int y, Color color, Graphics2D hoverGraphics);
    public abstract void onMouseDragged(int x, int y, Color color, Graphics2D hoverGraphics);
    public abstract void onMouseReleased(int x, int y, Color color, Graphics2D hoverGraphics);

}
