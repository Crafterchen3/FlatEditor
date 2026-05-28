import javax.swing.JColorChooser;
import javax.swing.JPanel;
import javax.swing.colorchooser.AbstractColorChooserPanel;
import java.awt.*;

//Komponent um eine Farbe auszuwählen
public class ColorChooser extends JPanel{

    private final JColorChooser chooser; // Legacy-Komponente von Java Swing

    public ColorChooser(Color initialColor, ColorChangeListener listener) {
        // Java Swing enthält einen sehr umfangreichen Farbwähler mit vielen Modi
        // Für das Programm brauche ich nur das ColorWheel

        chooser = new JColorChooser();
        chooser.setColor(initialColor); // Setze die Farbe des ColorChoosers
        AbstractColorChooserPanel panel = chooser.getChooserPanels()[1]; // extrahier das 2te Panel vom ColorChooser (das ColorWheel)
        panel.getComponent(0).setVisible(false); // setzt einen kleinen Text über dem ColorWheel auf unsichtbar (Aussehen)
        chooser.getSelectionModel().addChangeListener(e -> listener.handle(chooser.getColor())); // füge Farbänderungs-Listener hinzu
        add(panel);
    }

    public Color getColor(){
        return chooser.getColor();
    }

    public void setColor(Color color){
        chooser.setColor(color);
    }
}
