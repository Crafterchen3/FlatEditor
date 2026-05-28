import javax.swing.JDesktopPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.*;
import java.awt.*;

// Pane für alle Bilder Komponente
public class PaintingPane extends JDesktopPane{

    public final Painting painting;

    public PaintingPane(Painting painting){
        this.painting = painting;
        add(new CanvasFrame(painting), JDesktopPane.FRAME_CONTENT_LAYER);
        add(new ChooserFrame(painting));
        add(new ToolsFrame(painting));
    }

}
