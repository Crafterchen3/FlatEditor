import javax.swing.UIManager;


public class App {
    public static void main(String[] args) throws Exception {
        UIManager.put( "TitlePane.unifiedBackground", false ); //Titelleiste wird abgehoben vom Inhalt (Aussehen)
        new Frame(); // Fenster wird gestartet
    }
}
