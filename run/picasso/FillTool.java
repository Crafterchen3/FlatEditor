import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;

// Tool, um einen Bereich einzufärben
public class FillTool extends Tool{

    public FillTool(Painting painting) {
        super(painting, "Fill");
    }

    @Override
    public void onHover(int x, int y, Color color, Graphics2D hoverGraphics) {
        // Färbe einen Pixel zur Farbe color
        hoverGraphics.setColor(color);
        hoverGraphics.fillRect(x, y, 1, 1);
    }

    // Fülle einen Bereich der selben Farbe zur Farbe color
    @Override
    public void onMousePressed(int x, int y, Color color, Graphics2D hoverGraphics) {
        BufferedImage image = painting.image;
        ArrayDeque<Point> eDeque = new ArrayDeque<>(); // Erstelle Deque um zu bemalende Punkte zu speichern
        int oldColor = image.getRGB(x, y); // Hole die Farbe, die zu ersetzen ist
        // Abbrechen, falls die alte Farbe die selbe wie die neue ist
        if (oldColor == color.getRGB()) {
            return;
        }
        eDeque.add(new Point(x,y)); // den ersten Punkt zu Deque hinzufügen
        
        // Algorithmus alle anliegenden Pixel der selben Farbe einzufärben
        while (!eDeque.isEmpty()) {
            Point point = eDeque.pop(); // Hole einen zu bemalenden Punkt
            if (isInsideImage(image, point)) { // Teste ob der Punkt im Bild ist
                if (image.getRGB((int) point.getX(), (int) point.getY()) == oldColor) { // Wenn der Pixel am Punkt die alte Farbe ist
                    image.setRGB((int) point.getX(), (int) point.getY(), color.getRGB()); // Setze Pixel zur neuen Farbe
                    
                    // Füge anliegende Pixel zur Deque hinzu
                    eDeque.add(new Point((int) point.getX()+1,(int) point.getY()));
                    eDeque.add(new Point((int) point.getX(),(int) point.getY()+1));
                    eDeque.add(new Point((int) point.getX()-1,(int) point.getY()));
                    eDeque.add(new Point((int) point.getX(),(int) point.getY()-1));
                }
            }
        }
    }


    @Override
    public void onMouseDragged(int x, int y, Color color, Graphics2D hoverGraphics) {
        // Mach nichts
    }

    @Override
    public void onMouseReleased(int x, int y, Color color, Graphics2D hoverGraphics) {
        // Mach nichts
    }

}
