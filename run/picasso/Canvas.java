import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

// Kommentare teilweise mit KI

// Komponente, die das bild anzeigt und die Maus Aktionen verarbeitet/an die Tools weiterleitet
public class Canvas extends JComponent implements MouseListener, MouseMotionListener, MouseWheelListener{

    private final Painting painting; // Referenz auf das Painting-Objekt
    public final BufferedImage image; // Das Bild, das gezeichnet wird
    public BufferedImage hoverImage; // Bild für Hover-Effekte
    public Point imagePlacement = null; // Position des Bildes
    public Point dragStart = null; // Startpunkt für das Ziehen des Bildes
    public int zoom = 1; // Zoomfaktor
    public boolean isPainting = false; // Status, ob gerade gemalt wird

    // Konstruktor, der das Painting-Objekt initialisiert
    public Canvas(Painting painting){
        this.painting = painting;
        this.image = painting.image; // Bild von Painting erhalten
        hoverImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB); // Hover-Bild initialisieren
        
        // Hintergrundfarbe des Panels setzen
        setBackground(UIManager.getColor("Panel.background").darker());
        setOpaque(true); // Panel opak machen

        // MouseListener und MouseMotionListener hinzufügen
        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);
    }

    @Override
    public void paint(Graphics graphics) {
        super.paint(graphics); // Standard-Paint-Methode aufrufen
        // Initialisiere die Bildplatzierung, falls sie null ist
        if (imagePlacement == null)
            imagePlacement = new Point((getWidth() - image.getWidth()) / 2, (getHeight() - image.getHeight()) / 2);

        Graphics2D graphics2D = (Graphics2D) graphics; // Graphics-Objekt in Graphics2D umwandeln

        // Hintergrundrechteck zeichnen
        graphics2D.setColor(Color.BLACK);
        graphics2D.fillRect(imagePlacement.x - 1, imagePlacement.y - 1, image.getWidth() * zoom + 2, image.getHeight() * zoom + 2);
        // Bild zeichnen
        graphics2D.drawImage(image, imagePlacement.x, imagePlacement.y, image.getWidth() * zoom, image.getHeight() * zoom, null);
        // Hover-Bild zeichnen
        graphics2D.drawImage(hoverImage, imagePlacement.x, imagePlacement.y, image.getWidth() * zoom, image.getHeight() * zoom, null);
        // Hover-Bild zurücksetzen
        hoverImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
    }

    // Hilfsmethode, um die Bildkoordinaten aus den gegebenen Punkten zu berechnen
    private Point getImagePoint(Point point){
        return new Point((point.x - imagePlacement.x) / zoom,(point.y - imagePlacement.y) / zoom);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        // TODO Auto-generated method stub
        // Hier kann ein Klick-Event behandelt werden
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        // Überprüfen, ob die mittlere Maustaste gedrückt ist
        if (SwingUtilities.isMiddleMouseButton(e)) {
            int dx = e.getX() - dragStart.x; // Berechnung der Verschiebung in X-Richtung
            int dy = e.getY() - dragStart.y; // Berechnung der Verschiebung in Y-Richtung
            imagePlacement.translate(dx, dy); // Bildposition aktualisieren
            dragStart = e.getPoint(); // neuen Startpunkt setzen
        } else {
            // Wenn die linke oder rechte Maustaste gedrückt ist
            Point p = getImagePoint(e.getPoint()); // Bildkoordinaten berechnen
            Graphics2D graphics2d = hoverImage.createGraphics(); // Graphics-Objekt für das Hover-Bild erstellen
            // Werkzeug für das Ziehen verwenden
            painting.getCurrentTool().onMouseDragged((int) p.getX(),(int) p.getY(), SwingUtilities.isLeftMouseButton(e) ? painting.primaryColor : painting.secondaryColor, graphics2d);
            graphics2d.dispose(); // Graphics-Objekt freigeben
        }
        repaint(); // Panel neu zeichnen
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        // Hier kann ein Mouse Enter-Event behandelt werden (nicht nötig)
    }

    @Override
    public void mouseExited(MouseEvent e) {
        // Hier kann ein Mouse Exit-Event behandelt werden (nicht nötig)
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        // Bildkoordinaten berechnen
        Point p = getImagePoint(e.getPoint());
        Graphics2D graphics2d = hoverImage.createGraphics(); // Graphics-Objekt für das Hover-Bild erstellen
        // Werkzeug für Hover-Effekt verwenden
        painting.getCurrentTool().onHover((int) p.getX(),(int) p.getY(),painting.primaryColor, graphics2d);
        graphics2d.dispose(); // Graphics-Objekt freigeben
        repaint(); // Panel neu zeichnen
    }

    @Override
    public void mousePressed(MouseEvent e) {
        // Überprüfen, ob die mittlere Maustaste gedrückt ist
        if (SwingUtilities.isMiddleMouseButton(e)) {
            dragStart = e.getPoint(); // Startpunkt für Ziehen setzen
        } else {
            // Wenn die linke oder rechte Maustaste gedrückt ist
            Point p = getImagePoint(e.getPoint()); // Bildkoordinaten berechnen
            Graphics2D graphics2d = hoverImage.createGraphics(); // Graphics-Objekt für das Hover-Bild erstellen
            // Werkzeug für das Drücken verwenden
            Tool currentTool = painting.getCurrentTool();
            currentTool.onMousePressed((int) p.getX(),(int) p.getY(), SwingUtilities.isLeftMouseButton(e) ? painting.primaryColor : painting.secondaryColor, graphics2d);
            graphics2d.dispose(); // Graphics-Objekt freigeben
            repaint(); // Panel neu zeichnen
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        // Überprüfen, ob die mittlere Maustaste gedrückt ist
        if (SwingUtilities.isMiddleMouseButton(e))
            return; // Wenn ja, nichts tun
        Point p = getImagePoint(e.getPoint()); // Bildkoordinaten berechnen
        Graphics2D graphics2d = hoverImage.createGraphics(); // Graphics-Objekt für das Hover-Bild erstellen
        // Werkzeug für das Loslassen verwenden
        painting.getCurrentTool().onMouseReleased((int) p.getX(),(int) p.getY(), SwingUtilities.isLeftMouseButton(e) ? painting.primaryColor : painting.secondaryColor, graphics2d);
        graphics2d.dispose(); // Graphics-Objekt freigeben
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        int delta = -e.getWheelRotation(); // Berechnung der Scrollrichtung
        int newZoom = zoom + delta; // Neuen Zoomfaktor berechnen

        // Überprüfen, ob der neue Zoomfaktor gültig ist
        if (newZoom != 0) {
            int dx = (e.getX() - imagePlacement.x) / zoom; // Berechnung der Verschiebung in X-Richtung
            int dy = (e.getY() - imagePlacement.y) / zoom; // Berechnung der Verschiebung in Y-Richtung

            imagePlacement.x -= dx * delta; // Bildposition aktualisieren
            imagePlacement.y -= dy * delta; // Bildposition aktualisieren

            zoom = newZoom; // Zoomfaktor aktualisieren
        }
        repaint(); // Panel neu zeichnen
    }
}
