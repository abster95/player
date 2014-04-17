/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package utilities;

import Configuration.Configuration;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javax.imageio.ImageIO;

/**
 * Provides file operations.
 * 
 * This class provides bunch of fancy methods that are arguably redundant, but in
 * fact make your life hell of a lot easier. They clean the code from low level
 * machinations.
 * 
 */
public final class FileUtil {
    
    /**
     * Returns true if for provided File all conditions are met:
     * - exists
     * - is directory
     * - is readable
     * - is writable
     * 
     * Use this method to see if the diretory is not required to be valid.
     * @param dir
     * @return validity of directory for use
     * @throws NullPointerException if param null
     */
    public static boolean isValidDirectory(File dir){
        if (!dir.exists()) {
            Log.mess("Directory " + dir.getAbsolutePath() + " doesnt exist");
            return false;
        }
        if (!dir.isDirectory()) {
            Log.err("Path " + dir.getAbsolutePath() + " is not directory");
            return false;
        }
        if (!dir.canRead()) {
            Log.mess("Directory " + dir.getAbsolutePath() + " is not readable");
            return false;            
        }
        if (!dir.canWrite()) {
            Log.mess("Directory " + dir.getAbsolutePath() + " is not writable");
            return false;            
        }
        return true;
    }
    
    /**
     * Checks whether directory is valid and if it isnt, attempts
     * to make it valid by setting appropriate permissions or creating it.
     * Returns true if for provided File all conditions are met:
     * - exists
     * - is directory
     * - is readable
     * - is writable
     * 
     * Use this method to see if the diretory is required to be valid.
     * @param dir if not a directory, this method always returns false.
     * @return whether the directory is usable.
     * @throws NullPointerException if param null
     */
    public static boolean isValidatedDirectory(File dir) {
        boolean validity = true;
        if (!dir.exists())
            validity = dir.mkdirs();
        if (!dir.isDirectory()) {
            Log.err("Path " + dir.getAbsolutePath() + " is not directory");
            return false;
        }
        if (!dir.canRead())
            validity = dir.setReadable(true);  
        if (!dir.canWrite())
            validity = dir.setWritable(true);
        
        if (!validity)
            Log.err("Directory " + dir.getAbsolutePath() + " is not valid");
        return validity;
    }

    /**
     * Returns true if for provided File all conditions are met:
     * - exists
     * - is file
     * - is readable
     * 
     * @param file
     * @return validity of file for use
     * @throws NullPointerException if parameter null.
     */
    public static boolean isValidFile(File file) {
        if (!file.exists()) {
            Log.err("File " + file.getAbsolutePath() + " doesnt exist");
            return false;
        } else 
        if (!file.isFile()) {
            Log.err("Path " + file.getAbsolutePath() + " is not directory");
            return false;
        } else
        if (!file.canRead()) {
            Log.err("File " + file.getAbsolutePath() + " is not readable");
            return false;            
        } else {
            return true;
        }
    }
    
    /**
     * Checks validity of a file to be a skin. True return file means the file
     * can be used as a skin (the validity of the skin itself is not included).
     * For files returning false this application will not allow skin change.
     * Valid skin file checks out the following:
     * - not null
     * - isValidFile()
     * - is located in Skins folder set for this application
     * - is .css
     * - is located in its own folder with the same name
     * example: /Skins/MySkin/MySkin.css
     * @param skin
     * @return true if parameter is valid skin file. False otherwise or if null.
     */
    public static boolean isValidSkinFile(File skin) {
        String name = FileUtil.getName(skin);
        String path = Configuration.SKIN_FOLDER + File.separator + name +
                      File.separator + name + ".css";
        File test = new File(path);
        return (skin != null && FileUtil.isValidFile(skin) &&   // is valid file
                    skin.getPath().endsWith(".css") &&              // is .css file
                        skin.equals(test));    // is located in skins folder in its rightful folder
    }
    
    /**
     * Constructs list of image files found in the folder. Filters out files with extension
     * different than supported. To see what extensions are supported check ImageFileFormat
     * class.
     * @param dir - directory.
     * @return
     * Empty list if parameter not a valid directory or no results. Never null.
     */
    public static List<File> getImageFiles(File dir) {
        return getImageFilesRecursive(dir, 0);
    }
    
    /**
     * Constructs list of image files found in the folder and subfolders. Looks 
     * through the folder tree recursively respecting the maxDepth parameter.
     * 
     * Filters out files with extension different than supported. To see what extensions are
     * supported check ImageFileFormat class.
     * 
     * @param dir - directory.
     * @param maxDepth - depth for recursive search. depth=0 = currentFolder only
     * @return
     * Empty list if parameter not a valid directory or no results. Never null.
     */
    public static List<File> getImageFilesRecursive(File dir, int maxDepth) {
        if (!isValidDirectory(dir)) return new ArrayList<>();
        
        List<File> out = new ArrayList<>();
        File[] files = dir.listFiles();
        for (File f: files) {
            if(ImageFileFormat.isSupported(f)) {
                out.add(f);
            }
            if (maxDepth>=1 && f.isDirectory() && isValidDirectory(f)) {
                out.addAll(getImageFilesRecursive(f, maxDepth-1));
            }
        }
        return out;
    }
    
    /**
     * Constructs list of Images from provided file list. Filters out unsupported
     * types.
     * @param files
     * @return Empty if null or empty parameter or no results.
     */
    public static List<Image> FilesToImages(List<File> files) {
        List<Image> list = new ArrayList<>();
        for(File f: files) {
            if(ImageFileFormat.isSupported(f)) {
                Image img = new Image(f.toURI().toString());
                list.add(img);
            }
        }
        return list;
    }

    /**
     * Constructs list of audio files found in the folder. Filters out files with extension
     * different than supported. To see what extensions are supported check ImageFileFormat
     * class.
     * @param dir - directory
     * @return
     * List of audio files or empty list if parameter not a valid directory or no
     * results. Never null.
     */
    public static List<File> getAudioFiles(File dir) {
        return getAudioFiles(dir, 0);
    }
    /**
     * Constructs list of audio files found in the folder and subfolders. Looks 
     * through the folder tree recursively respecting the maxDepth parameter.
     * 
     * Filters out files with extension different than supported. To see what extensions are
     * supported check AudioFileFormat class.
     * 
     * @param dir - directory.
     * @param maxDepth - depth for recursive search. depth 0 = currentFolder only
     * @return Empty list if parameter not a valid directory or no results. Never
     * null.
     */
    public static List<File> getAudioFiles(File dir, int maxDepth) {
        if (!isValidDirectory(dir)) return new ArrayList<>(); 
        
        List<File> out = new ArrayList<>();
        File[] files = dir.listFiles();
        for (File f: files) {
            if(AudioFileFormat.isSupported(f))
                out.add(f);
            if (maxDepth>=1 && f.isDirectory() && isValidDirectory(f))
                out.addAll(getAudioFiles(f, maxDepth-1));
        }
        return out;
    }
    /**
     * Filters out all but supported audio files. Directories will be searched
     * recursively.
     * @param files - list of fiels and directories to filter
     * @param maxDepth - depth for recursive search. depth 0 = currentFolder only
     * @return Empty list if parameter not a valid directory or no results. Never
     * null.
     */
    public static List<File> getAudioFiles(List<File> files, int maxDepth) {
        ArrayList<File> out = new ArrayList<>();
        for(File f: files) {
            if(AudioFileFormat.isSupported(f))
                out.add(f);
            if (f.isDirectory()) {
                for(File ff: FileUtil.getAudioFiles(f,maxDepth))
                    out.add(ff);
            }
        }
        return out;
    }
    
    /**
     * @param f
     * @return name of the file without suffix
     * @throws NullPointerException if parameter null
     */
    public static String getName(File f) {
        return f.getName().substring(0,f.getName().lastIndexOf('.'));
    }
    /**
     * @param u
     * @return name of the file without suffix
     * @throws NullPointerException if parameter null
     */
    public static String getName(URI u) {
        return getName(new File(u));
    }
    
     /**
      * Writes a textual file with specified content, name and location. 
      * @param filepath Path to the file. The extension is part of the file. Do
      * not use .txt extension as it can cause problems with newline characters.
      * In case the file exists, it will be completely overwritten.
      * @param content Text that will be written to the file.
      * @throws RuntimeException when param is directory
      */
     public static void writeFile(String filepath, String content) {
        File file = new File(filepath);
        if (file.isDirectory()) throw new RuntimeException("File must not be directory.");
        Writer writer;
        try {
            writer = new BufferedWriter(new FileWriter(file));
            writer.write(content);
            writer.close();
        } catch (IOException ex) {
            Log.err("Couldnt save file: " + filepath);
        }
     }
     
     /**
      * Reads file as a text file and returns all its content in form of List of
      * Strings, each representing one line.
      * @param filepath
      * @return List of lines or empty list. Never null.
      */
     public static List<String> readFile(String filepath) {
        File file = new File(filepath);
        List<String> lines; 
        try {
            lines = Files.readAllLines(Paths.get(filepath), Charset.defaultCharset());
        } catch (IOException ex) {
            Log.err("Problems reading file " + file.getPath() + ". File wasnt read.");
            return new ArrayList<>();
        }
        return lines;
     }
     
     /**
      * Reads and parses specified file. This method translates the file into 
      * object oriented paradigm.
      * File format per line (input):
      *     "key : value"
      * Map of lines (output):
      *     <String key, String value>
      * @param file
      * @return 
      */
     public static Map<String,String> parseFile(File file) {
        final List<String> lines = new ArrayList<>();
        Map<String,String> out = new HashMap<>();
        try {
            lines.addAll(Files.readAllLines(Paths.get(file.getPath()), Charset.defaultCharset()));
        } catch (IOException ex) {
            Log.err("Problems reading file " + file.getPath() + ". File wasnt parsed.");
            return out;
        }
        // remove empty lines, comments and other abominations
        for (int i=lines.size()-1; i>=0; i--) {
            String l = Util.emptifyString(lines.get(i));
            if (!l.isEmpty() && !l.startsWith("#")) {
                String key = l.substring(0, l.indexOf(" : "));
                String value = l.substring(l.indexOf(" : ")+3);
                out.put(key, value);                
            }            
        }
        return out;
     }
     
    public static void deleteFile(File file) {
        if (!file.exists()) return;
        try {
           boolean success = file.delete();
           if (!success) {
               Log.err("The file " + file.getPath() + " couldnt be deleted."
                       + " Will attempt to delete on application shutdown.");
               file.deleteOnExit();
           }
        } catch(SecurityException e) {
            Log.err("The file " + file.getPath() + " couldnt be deleted. "
                    + e.getMessage());
        }
    }
    
    /**
     * Saves image as a file, both being provided as parameters. If
     * the file is of type that is not supported by the application, the operation
     * will not take place.
     * @see ImageFileFormat for specifications
     * @param img 
     * @param file
     * @throws NullPointerException if any of the parameters null
     */
    public static void writeImage(Image img, File file) {
        Objects.requireNonNull(img);
        Objects.requireNonNull(file);
        
        ImageFileFormat t = ImageFileFormat.formatOf(file.toURI());
        if (!t.isSupported()) {
            Log.err("Error during saving image " + file.getPath() + ". Format "
                    + t.name() + " not supported.");
            return;
        }
        
        try {
            ImageIO.write(SwingFXUtils.fromFXImage(img, null), "png", file);
        } catch (IOException e) {
            Log.err("Error during saving image " + file.getPath());
        }
    }
}
