import java.awt.BorderLayout;
import java.awt.GridLayout;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;s
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.border.EmptyBorder;

// Kommentare teilweise mit KI

// Dialogfenster, um ein neues Bild zu erstellen
public class CreationPopup extends JDialog{

    // Konstruktor für das CreationPopup, das ein neues Bild erstellt
    public CreationPopup(Frame owner){
        super(owner, "Create Painting"); // Titel des Dialogs setzen
        setSize(300, 200); // Größe des Dialogs festlegen
        setLocationRelativeTo(owner); // das Dialog mittig zum Eigentümer platzieren
        setLayout(new GridLayout(3, 1)); // Layout mit 3 Zeilen und 1 Spalte setzen
        
        // Panel für die Breite des Bildes
        JPanel widthPanel = new JPanel(new BorderLayout(10, 10));
        widthPanel.setBorder(new EmptyBorder(10, 20, 10, 20)); // Rand um das Panel setzen
        JLabel widthLabel = new JLabel("Breite"); // Label für die Breite erstellen
        JSpinner widthField = new JSpinner(); // Spinner für die Breite erstellen
        widthField.setValue(64); // Standardwert für die Breite setzen
        widthPanel.add(widthLabel, BorderLayout.WEST); // Label links im Panel platzieren
        widthPanel.add(widthField, BorderLayout.CENTER); // Spinner in der Mitte des Panels platzieren
        add(widthPanel); // Breiten-Panel zum Dialog hinzufügen
        
        // Panel für die Höhe des Bildes
        JPanel heightPanel = new JPanel(new BorderLayout(10, 10));
        heightPanel.setBorder(new EmptyBorder(10, 20, 10, 20)); // Rand um das Panel setzen
        JLabel heightLabel = new JLabel("Höhe"); // Label für die Höhe erstellen
        JSpinner heightField = new JSpinner(); // Spinner für die Höhe erstellen
        heightField.setValue(64); // Standardwert für die Höhe setzen
        heightPanel.add(heightLabel, BorderLayout.WEST); // Label links im Panel platzieren
        heightPanel.add(heightField, BorderLayout.CENTER); // Spinner in der Mitte des Panels platzieren
        add(heightPanel); // Höhen-Panel zum Dialog hinzufügen
        
        // Panel für die Schaltflächen
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 20, 20));
        buttonPanel.setBorder(new EmptyBorder(10, 20, 20, 20)); // Rand um das Panel setzen
        
        // Schaltfläche zum Abbrechen
        JButton cancelButton = new JButton("Cancel"); // Schaltfläche erstellen
        cancelButton.addActionListener(e -> {
            dispose(); // Dialog schließen, wenn die Schaltfläche gedrückt wird
        });
        
        // Schaltfläche zum Bestätigen
        JButton okButton = new JButton("Ok"); // Schaltfläche erstellen
        okButton.addActionListener(e -> {
            // Bild mit den angegebenen Breiten- und Höhenwerten hinzufügen
            owner.addPainting((int) widthField.getValue(), (int) heightField.getValue());
            dispose(); // Dialog schließen
        });
        
        buttonPanel.add(cancelButton); // Abbrechen-Schaltfläche zum Panel hinzufügen
        buttonPanel.add(okButton); // Bestätigen-Schaltfläche zum Panel hinzufügen
        add(buttonPanel); // Schaltflächen-Panel zum Dialog hinzufügen
        
        setVisible(true); // Dialog sichtbar machen
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); // Schließen des Dialogs beim Schließen des Fensters
    }

}