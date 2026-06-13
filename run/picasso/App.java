import javax.swing.UIManager;

import com.formdev.flatlaf.FlatDarkLaf;

public class App {
    public static void main(String[] args) throws Exception {
        FlatDarkLaf.setup(); //UI Library wird inizialisiert
        new Logger().print("Hello World!");
        UIManager.put( "TitlePane.unifiedBackground", false ); //Titelleiste wird abgehoben vom Inhalt (Aussehen)
        new Frame(); // Fenster wird gestartet
    }
}
