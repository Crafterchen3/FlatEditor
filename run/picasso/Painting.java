import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

// Datenklasse um alle Infos eines Bildes zu speichern
public class Painting {

    public BufferedImage image; // das Bild
    public Color primaryColor = Color.WHITE; //Primärfarbe
    public Color secondaryColor = Color.BLACK; //Sekundärfarbe
    public ColorChooser primaryColorChooser = new ColorChooser(this.primaryColor, newColor -> this.primaryColor = newColor); //Primärfarbwähler
    public ColorChooser secondaryColorChooser = new ColorChooser(this.secondaryColor, newColor -> this.secondaryColor = newColor);; //Sekundärfarbwähler
    public Tool[] tools = new Tool[]{ // Alle tools
        new PencilTool(this), // Stift-Tool
        new RectangleTool(this), // Rechteck-Tool
        new FillTool(this), // Füll-Tool
        new PickerTool(this) // Farbwahl-Tool
    };
    public int currentTool = 0; // Index für aktuelles Tool
    public String file = null; // Aktueller Datei-Pfad

    // Erstelle ein Bild mit geg. Größer
    public Painting(int width, int height){
        image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    }

    // Erstelle ein Bild anhand eines Datei-Pfads
    public Painting(String file){
        this.file = file;
        try {
            image = ImageIO.read(new File(file)); // Lese Bild aus Datei
        } catch (IOException e) {
            e.printStackTrace(); // Logge Fehler
        }
    }

    public Tool getCurrentTool(){
        return tools[currentTool];
    }

}
