import javax.swing.JInternalFrame;
import javax.swing.JTabbedPane;

public class ChooserFrame extends JInternalFrame {

    // Kleines Fenster um die Farbe auszuwählen
    public ChooserFrame(Painting painting){
        super("Farbauswahl");
        setSize(246, 280);
        setFrameIcon(null); // Icon leer setzen (Aussehen)
        
        JTabbedPane tabbedPane = new JTabbedPane(); // Komponent, dass mehrere Tabs ermöglicht
        tabbedPane.add(painting.primaryColorChooser, "Primärfarbe"); //Füge den ColorChooser für die Primärfarbe zum TabbedPane hinzu
        tabbedPane.add(painting.secondaryColorChooser, "Sekundärfarbe"); //Füge den ColorChooser für die Sekundärfarbe zum TabbedPane hinzu
        add(tabbedPane);
        
        setVisible(true); // Fenster sichtbar setzen
    }

}
