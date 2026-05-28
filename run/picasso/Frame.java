import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;

import java.io.File;
import java.io.IOException;

// Kommentare mit KI

// Hauptklasse für das Hauptfenster der Anwendung
public class Frame extends JFrame {

    private JTabbedPane tabbedPane = new JTabbedPane(); // TabbedPane für mehrere Malflächen

    // Konstruktor für das Hauptfenster
    public Frame() {
        super("Picasso"); // Titel des Fensters setzen
        setDefaultCloseOperation(DISPOSE_ON_CLOSE); // Schließen des Fensters
        setSize(800, 600); // Größe des Fensters festlegen
        setLocationRelativeTo(null); // Fenster mittig auf dem Bildschirm platzieren

        JMenuBar bar = new JMenuBar(); // Menüleiste erstellen
        JMenu fileMenu = new JMenu("File"); // Datei-Menü erstellen

        // Menüpunkt für neues Bild
        JMenuItem newItem = new JMenuItem("New...");
        newItem.addActionListener(e -> {
            new CreationPopup(this); // Popup zum Erstellen eines neuen Bildes öffnen
        });
        newItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK)); // Tastenkombination setzen
        fileMenu.add(newItem); // Menüpunkt zum Datei-Menü hinzufügen

        fileMenu.addSeparator(); // Trennlinie im Menü hinzufügen

        // Menüpunkt für Speichern
        JMenuItem saveItem = new JMenuItem("Save");
        saveItem.addActionListener(e -> {
            save(tabbedPane.getSelectedIndex()); // Aktuelles Bild speichern
        });
        saveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK)); // Tastenkombination setzen
        fileMenu.add(saveItem); // Menüpunkt zum Datei-Menü hinzufügen

        // Menüpunkt für Speichern unter
        JMenuItem saveAsItem = new JMenuItem("Save as...");
        saveAsItem.addActionListener(e -> {
            saveAs(tabbedPane.getSelectedIndex()); // Aktuelles Bild speichern unter
        });
        saveAsItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK)); // Tastenkombination setzen
        fileMenu.add(saveAsItem); // Menüpunkt zum Datei-Menü hinzufügen

        fileMenu.addSeparator(); // Trennlinie im Menü hinzufügen
        
        // Menüpunkt für Öffnen
        JMenuItem openItem = new JMenuItem("Open...");
        openItem.addActionListener(e -> {
            open(tabbedPane.getSelectedIndex()); // Bild öffnen
        });
        openItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK)); // Tastenkombination setzen
        fileMenu.add(openItem); // Menüpunkt zum Datei-Menü hinzufügen

        fileMenu.addSeparator(); // Trennlinie im Menü hinzufügen
        
        // Menüpunkt für Schließen
        JMenuItem closeItem = new JMenuItem("Close");
        closeItem.addActionListener(e -> {
            close(tabbedPane.getSelectedIndex()); // Aktuelles Tab schließen
        });
        fileMenu.add(closeItem); // Menüpunkt zum Datei-Menü hinzufügen

        fileMenu.addSeparator(); // Trennlinie im Menü hinzufügen

        // Menüpunkt für Beenden
        JMenuItem quitItem = new JMenuItem("Quit");
        quitItem.addActionListener(e -> {
            dispose(); // Fenster schließen
        });
        quitItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_DOWN_MASK)); // Tastenkombination setzen
        fileMenu.add(quitItem); // Menüpunkt zum Datei-Menü hinzufügen
        
        bar.add(fileMenu); // Datei-Menü zur Menüleiste hinzufügen
        setJMenuBar(bar); // Menüleiste im Fenster setzen

        add(tabbedPane); // TabbedPane zum Fenster hinzufügen

        setVisible(true); // Fenster sichtbar machen
    }

    // Methode zum Hinzufügen eines neuen Bildes
    public void addPainting(int width, int height) {
        Painting painting = new Painting(width, height); // Neues Painting-Objekt erstellen
        PaintingPane paintingPane = new PaintingPane(painting); // Neues PaintingPane erstellen
        tabbedPane.addTab("Untitled", paintingPane); // Tab mit dem neuen PaintingPane hinzufügen
    }

    // Methode zum Abrufen des aktuellen Painting-Objekts
    private Painting getPainting(int tabIndex) {
        PaintingPane pane = (PaintingPane) tabbedPane.getComponent(tabIndex); // Aktuelles PaintingPane abrufen
        return pane.painting; // Das Painting zurückgeben
    }

    // Methode zum Speichern des aktuellen Bildes
    public void save(int tabIndex) {
        Painting painting = getPainting(tabIndex); // Aktuelles Painting abrufen
        if (painting.file == null) {
            saveAs(tabIndex); // Wenn kein Dateipfad vorhanden ist, Speichern unter aufrufen
        }
        try {
            ImageIO.write(painting.image, "png", new File(painting.file)); // Bild speichern
        } catch (IOException e) {
            // Fehlerbehandlung
            e.printStackTrace();
        }
    }

    // Methode für Speichern unter
    public void saveAs(int tabIndex) {
        Painting painting = getPainting(tabIndex); // Aktuelles Painting abrufen
        JFileChooser fileChooser = new JFileChooser(); // JFileChooser erstellen
        fileChooser.setFileFilter(new ImageFileFilter()); // Dateifilter setzen
        // Öffne den Dialog
        int returnValue = fileChooser.showSaveDialog(this); // Dialog anzeigen
        if (returnValue == JFileChooser.APPROVE_OPTION) { // Wenn eine Datei ausgewählt wurde
            File selectedFile = fileChooser.getSelectedFile(); // Ausgewählte Datei abrufen
            painting.file = selectedFile.getPath(); // Dateipfad speichern
            tabbedPane.setTitleAt(tabIndex, selectedFile.getName()); // Tab-Titel aktualisieren
            try {
                ImageIO.write(painting.image, "png", new File(painting.file)); // Bild speichern
            } catch (IOException e) {
                // Fehlerbehandlung
                e.printStackTrace();
            }
            System.out.println("Gespeicherte Datei: " + selectedFile.getAbsolutePath()); // Ausgabe des Dateipfads
        }
    }

    // Methode zum Öffnen eines Bildes
    public void open(int tabIndex) {
        // Erstelle einen JFileChooser
        JFileChooser fileChooser = new JFileChooser(); // JFileChooser erstellen
        fileChooser.setFileFilter(new ImageFileFilter()); // Dateifilter setzen
        // Öffne den Dialog
        int returnValue = fileChooser.showOpenDialog(this); // Dialog anzeigen
        if (returnValue == JFileChooser.APPROVE_OPTION) { // Wenn eine Datei ausgewählt wurde
            File selectedFile = fileChooser.getSelectedFile(); // Ausgewählte Datei abrufen

            PaintingPane paintingPane = new PaintingPane(new Painting(selectedFile.getPath())); // Neues PaintingPane mit dem Bild erstellen
            tabbedPane.addTab(selectedFile.getName(), paintingPane); // Tab mit dem neuen PaintingPane hinzufügen
            System.out.println("Ausgewählte Datei: " + selectedFile.getAbsolutePath()); // Ausgabe des Dateipfads
        }
    }

    // Methode zum Schließen eines Tabs
    public void close(int tabIndex) {
        Painting painting = getPainting(tabIndex); // Aktuelles Painting abrufen
        if (painting.file == null) { // Wenn das Bild nicht gespeichert ist
            switch (showSaveConfirmationDialog()) { // Bestätigungsdialog anzeigen
                case 0: // Speichern (als)
                    save(tabIndex); // Bild speichern
                    tabbedPane.remove(tabIndex); // Tab schließen
                    break;
                case 1: // Abbrechen
                    break; // Nichts tun
                case 2: // Schließen
                    tabbedPane.remove(tabIndex); // Tab schließen
                    break;
            }
        }
    }

    // Methode zum Anzeigen des Bestätigungsdialogs
    private int showSaveConfirmationDialog() {
        String message = "Möchten Sie Ihre Änderungen speichern?"; // Dialognachricht
        String title = "Änderungen speichern"; // Dialogtitel

        Object[] options = { "Speichern (als)", "Abbrechen", "Schließen" }; // Optionen für den Dialog

        return JOptionPane.showOptionDialog(this, message, title, // Dialog anzeigen
                JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, options, options[0]); // Rückgabewert der Auswahl
    }

}
