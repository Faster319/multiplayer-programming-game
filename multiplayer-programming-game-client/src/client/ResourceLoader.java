package client;

//TODO use json or another cleaner way to store settings

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import javax.swing.filechooser.FileSystemView;

public class ResourceLoader {

    private Map<String, File> files = new HashMap<String, File>();

    String resourcePath;

    public ResourceLoader() {

        resourcePath = FileSystemView.getFileSystemView().getDefaultDirectory().getPath() + File.separator
            + "multiplayer-programming-game-client" + File.separator;
        

        loadFile("settings.txt");
        loadFile("help.html");
    }

    public boolean loadFile(String fileName) {

        File file = new File(resourcePath + fileName);

        if (!file.exists()) {
            boolean success = copyFile(fileName, file);
            if (!success) return false;
        }

        files.put(fileName, file);

        return true;
    }

    private boolean copyFile(String fileName, File file) { //TODO use fewer parameters
        InputStream inputStream = getClass().getResourceAsStream("/" + fileName);

        try {
            new File(resourcePath).mkdirs();
            Files.copy(inputStream, file.toPath());
            inputStream.close();
        }
        
        catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public boolean isLoaded(String fileName) {
        return files.containsKey(fileName);
    }

    public File getFile(String fileName) {
        return files.get(fileName);
    }

}
