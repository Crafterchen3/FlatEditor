import java.awt.Color;
import java.awt.Graphics2D;

// Helferklasse für unkomplizierte Tools
public abstract class SimpleTool extends Tool{

    public SimpleTool(Painting painting, String name) {
        super(painting, name);
    }

    // Vereinfache onMouseDragged, sodass onMousePressed ausgeführt wird
    @Override
    public void onMouseDragged(int x, int y, Color color, Graphics2D hoverGraphics) {
        onMousePressed(x, y, color, hoverGraphics);
    }

    // Definiere onMouseReleased, damit einfache Tools, sich nicht darum kümmern müssen
    @Override
    public void onMouseReleased(int x, int y, Color color, Graphics2D hoverGraphics) {
        // Tue nichts
    }

}
