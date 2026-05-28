import java.awt.GridLayout;

import javax.swing.JButton;
import javax.swing.JInternalFrame;

// Fenster, um Tools auszuwählen
public class ToolsFrame extends JInternalFrame{

    public ToolsFrame(Painting painting){
        setLayout(new GridLayout(painting.tools.length, 1));
        setBounds(650, 0, 100, 100+50*painting.tools.length); // Größe setzen, anhand wie viele Tools es gibt
        setFrameIcon(null); // Icon leer setzen (Aussehen)
        for (int tools = 0; tools < painting.tools.length; tools++) { // Für alle Tools...
            Tool tool = painting.tools[tools];
            int index = tools; // index als Variable definieren, damit es im ActionListener benutzt werden kann
            JButton button = new JButton(tool.name); // Neuer Knopf mit Tool Name
            button.addActionListener(e -> {
                painting.currentTool = index; // Setze Tool, wenn gedrückt
            });
            add(button);
        }
        setVisible(true); // Sichtbar setzen
    }

}
