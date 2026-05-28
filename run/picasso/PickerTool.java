import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;

// Farbauswahl-Tool um Farben auszuwählen
public class PickerTool extends Tool {

    public PickerTool(Painting painting) {
        super(painting, "Picker");
    }

    @Override
    public void onHover(int x, int y, Color color, Graphics2D hoverGraphics) {
        // Male keine Vorschau
    }

    @Override
    public void onMousePressed(int x, int y, Color color, Graphics2D hoverGraphics) {
        if (isInsideImage(painting.image, new Point(x, y))) {
            if (color == painting.primaryColor) { //wenn linke Taste gedrückt
                // Setze Primärfarbe zu Farbe an Punkt
                painting.primaryColor = new Color(painting.image.getRGB(x, y));
                painting.primaryColorChooser.setColor(painting.primaryColor);
                painting.primaryColorChooser.repaint(); // Aktualisiere Farbwähler
            } else { // wenn rechte Taste gedrückt
                // Setze Sekundärfarbe zu Farbe an Punkt
                painting.secondaryColor = new Color(painting.image.getRGB(x, y));
                painting.secondaryColorChooser.setColor(painting.secondaryColor);
                painting.secondaryColorChooser.repaint(); // Aktualisiere Farbwähler
            }
        }
    }

    @Override
    public void onMouseDragged(int x, int y, Color color, Graphics2D hoverGraphics) {
        // Tue nichts
    }

    @Override
    public void onMouseReleased(int x, int y, Color color, Graphics2D hoverGraphics) {
        // Tue nichts
    }

}
