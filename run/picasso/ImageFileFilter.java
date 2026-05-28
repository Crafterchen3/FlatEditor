import java.io.File;

// Klasse von KI https://app.fobizz.com/ai/chats/4866053b-c30b-4e91-8596-fca6464b5fdd

// Klasse, die bei einem FilePicker nur Bilder zulässt
public class ImageFileFilter extends javax.swing.filechooser.FileFilter {
    @Override
    public boolean accept(File file) {
        // Akzeptiere Verzeichnisse
        if (file.isDirectory()) {
            return true;
        }
        // Überprüfe die Dateiendung
        String extension = getFileExtension(file);
        return extension != null && isImageExtension(extension);
    }

    @Override
    public String getDescription() {
        return "Image Files (JPEG, PNG, GIF, BMP)";
    }

    private String getFileExtension(File file) {
        String name = file.getName();
        int lastIndexOfDot = name.lastIndexOf('.');
        return (lastIndexOfDot > 0 && lastIndexOfDot < name.length() - 1) 
                ? name.substring(lastIndexOfDot + 1).toLowerCase() 
                : null;
    }

    private boolean isImageExtension(String extension) {
        return extension.equals("jpg") || extension.equals("jpeg") ||
               extension.equals("png") || extension.equals("gif") ||
               extension.equals("bmp");
    }
}
