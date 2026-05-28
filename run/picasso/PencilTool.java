import java.awt.Color;
import java.awt.Graphics2D;

// Stift-Tool um einzelne Pixel anzumalen
public class PencilTool extends SimpleTool{

    public PencilTool(Painting painting) {
        super(painting, "Pencil");
    }

    // Male einen Pixel mit Farbe, als Vorschau
    @Override
    public void onHover(int x, int y, Color color, Graphics2D hoverGraphics) {
        hoverGraphics.setColor(color);
        hoverGraphics.fillRect(x, y, 1, 1);
    }

    // Male einen Pixel, auf Mausclick / Mausdrag
    @Override
    public void onMousePressed(int x, int y, Color color, Graphics2D hoverGraphics) {
        painting.image.setRGB(x, y, color.getRGB());
    }

}
