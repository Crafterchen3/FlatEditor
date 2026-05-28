import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;

// Rechteck-Tool, um Rechtecke zu zeichnen
public class RectangleTool extends Tool{

    private Point dragStart; // Punkt, an dem ein drag gestartet wurde

    public RectangleTool(Painting painting) {
        super(painting, "Rectangle");
    }

    // Male das Rechteck als Vorschau
    @Override
    public void onHover(int x, int y, Color color, Graphics2D hoverGraphics) {
        hoverGraphics.setColor(color);
        hoverGraphics.fillRect(x, y, 1, 1);
    }

    // Setze dragStart auf Mausclick
    @Override
    public void onMousePressed(int x, int y, Color color, Graphics2D hoverGraphics) {
        dragStart = new Point(x,y);
        // Male Vorschau
        hoverGraphics.setColor(color);
        hoverGraphics.fillRect(x, y, 1, 1);
    }

    // Berechne Rechteck auf Drag
    @Override
    public void onMouseDragged(int x, int y, Color color, Graphics2D hoverGraphics) {
        // Berechnung von KI

        // Calculate the top-left corner (x, y)
        int startX = Math.min((int) dragStart.getX(), x);
        int startY = Math.min((int) dragStart.getY(), y);

        // Calculate the width and height
        int width = Math.abs((int) dragStart.getX() - x);
        int height = Math.abs((int) dragStart.getY() - y);

        // Male Vorschau
        hoverGraphics.setColor(color);
        hoverGraphics.setStroke(new BasicStroke(1));
        hoverGraphics.drawRect(startX, startY, width, height);
    }

    // Zeichne Rechteck auf das Bild
    @Override
    public void onMouseReleased(int x, int y, Color color, Graphics2D hoverGraphics) {
        // Calculate the top-left corner (x, y)
        int startX = Math.min((int) dragStart.getX(), x);
        int startY = Math.min((int) dragStart.getY(), y);

        // Calculate the width and height
        int width = Math.abs((int) dragStart.getX() - x);
        int height = Math.abs((int) dragStart.getY() - y);

        Graphics2D graphics = painting.image.createGraphics();
        graphics.setColor(color);
        // Setze Pinselstärke auf 1
        graphics.setStroke(new BasicStroke(1));
        graphics.drawRect(startX, startY, width, height);
        graphics.dispose();
    }

}
