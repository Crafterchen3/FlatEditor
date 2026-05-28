import java.beans.PropertyVetoException;

import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import javax.swing.plaf.basic.BasicInternalFrameUI;

public class CanvasFrame extends JInternalFrame{

    public CanvasFrame(Painting painting){
        super();
        setVisible(true);
        setUI(new BasicInternalFrameUI(this) {
            @Override
            protected JComponent createNorthPane(JInternalFrame w) {
                return null; // entfernt die Titelzeile
            }
        });
        add(new Canvas(painting));
        try {
            setMaximum(true);
        } catch (PropertyVetoException e) {
        }
    }

}
