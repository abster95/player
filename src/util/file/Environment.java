/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.file;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.stream.Stream;

import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Window;

import org.controlsfx.tools.Platform;

import audio.playlist.PlaylistManager;
import layout.widget.feature.ImageDisplayFeature;
import layout.widget.feature.ImagesDisplayFeature;
import gui.Gui;
import util.dev.TODO;
import util.file.AudioFileFormat.Use;

import static layout.widget.WidgetManager.WidgetSource.NO_LAYOUT;
import static java.awt.Desktop.Action.*;
import static java.util.stream.Collectors.groupingBy;
import static main.App.APP;
import static org.controlsfx.tools.Platform.WINDOWS;
import static util.dev.TODO.Purpose.FUNCTIONALITY;
import static util.dev.TODO.Purpose.UNIMPLEMENTED;
import static util.dev.TODO.Purpose.UNTESTED;
import static util.dev.Util.log;
import static util.file.FileType.DIRECTORY;
import static util.functional.Util.filter;
import static util.functional.Util.list;
import static util.functional.Util.map;

/**
 * Provides methods to handle external platform specific tasks. Browsing
 * files, opening files in external apps, clipboard, etc.
 *
 * @author Martin Polakovic
 */
@TODO(purpose = FUNCTIONALITY, note = "support printing, mailing")
@TODO(note = "File highlighting, test non windows platforms")
public class Environment {

    /** Copies the string to system clipboard. Does nothing if null. */
    public static void copyToSysClipboard(String s) {
        copyToSysClipboard(DataFormat.PLAIN_TEXT, s);
    }

    /** Puts given object to system clipboard. Does nothing if object null. */
    public static void copyToSysClipboard(DataFormat df, Object o) {
        if(o != null) {
            ClipboardContent c = new ClipboardContent();
                             c.put(df,o);
            Clipboard.getSystemClipboard().setContent(c);
        }
    }


    /** Equivalent to {@code browse(f.toURI()); } */
    public static void browse(File f) {
        browse(f.toURI());
    }

    public static void browse(File f, boolean openDir) {
        browse(f.toURI(), openDir);
    }

    /**
     * Browses uri - opens it in its respective browser, e.g. internet browser or file explorer.
     * <p/>
     * On some platforms the operation may be unsupported.
     *
     * @param uri to browse
     */
    @TODO(purpose = {UNIMPLEMENTED, UNTESTED}, note = "Non-windows platform impl. naively & untested")
    public static void browse(URI uri) {
        browse(uri, false);
    }

    private static void browse(URI uri, boolean openDir) {
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(BROWSE)) {
            log(Environment.class).warn("Unsupported operation : " + BROWSE + " uri");
            return;
        }
        try {
            // If uri denotes a file, file explorer should be open, highlighting the file
            // However Desktop.browse does nothing (a bug?). We have 2 alternatives:
            // 1: open the parent directory of the file (and sacrifice the file highlighting functionality)
            // 2: open the file with Desktop.open() which opens the file in the associated program.
            // Both have problems.
            //
            // Ultimately, for Windows we run explorer.exe manually and select the file. For
            // other systems we browse the parent directory instead. Non Windows platforms
            // need some testing to do...
            try {
                File f = new File(uri);
                    boolean isDir = f.isDirectory();
                if (f.exists()) {
                    if(Platform.getCurrent()==WINDOWS && (!isDir || !openDir)) {
                        openWindowsExplorerAndSelect(f);
                    } else {
                        open(isDir ? f.getParentFile() : f);
                    }
                }
                return;
            } catch (IllegalArgumentException e) {
                // ignore exception, it just means the uri does not denote a
                // file which is fine
            }
            Desktop.getDesktop().browse(uri);
        } catch (IOException e) {
            log(Environment.class).error("Browsing uri {} failed", uri, e);
            APP.parameterProcessor.process(list(uri.getPath())); // try open with this app
        }
    }

    /**
     * Browses files or directories. On some platforms the operation may be unsupported.
     *
     * @param files
     */
    public static void browse(Stream<File> files) {
        files.distinct()
             .collect(groupingBy(f -> f.isFile() ? f.getParentFile() : f))
             .forEach((dir,children) -> {
                 if(children.size()==1) browse(children.get(0));
                 else if(children.stream().anyMatch(f -> f==dir)) browse(dir);
                 else open(dir);
             });
    }

    /**
     * Edits file in default associated editor program. If the file denotes a directory,
     * {@link #open(java.io.File)} is called instead.
     * <p/>
     * On some platforms the operation may be unsupported. In that case this method is a no-op.
     *
     * @param f
     */
    public static void edit(File f) {
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(EDIT)) {
            log(Environment.class).warn("Unsupported operation : " + EDIT + " uri");
            return;
        }

        if(f.isDirectory()) {
            open(f);
        } else {
            try {
                Desktop.getDesktop().edit(f);
            } catch (IOException e) {
                log(Environment.class).error("Opening file {} in editor failed", f, e);
                APP.parameterProcessor.process(list(f.getPath())); // try open with this app
            } catch (IllegalArgumentException e) {
                // file does not exists, nothing for us to do
            }
        }
    }

    /**
     * Opens file in default associated program.
     * <p/>
     * On some platforms the operation may be unsupported. In that case this method is a no-op.
     *
     * @param f
     */
    public static void open(File f) {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(OPEN)) {
            try {
                Desktop.getDesktop().open(f);
            } catch (IOException e) {
                log(Environment.class).error("Opening file {} in native app failed", f, e);
                APP.parameterProcessor.process(list(f.getPath())); // try open with this app
            } catch (IllegalArgumentException e) {
                // file does not exists, nothing for us to do
            }
        } else {
            log(Environment.class).warn("Unsupported operation : " + OPEN + " file");
        }
    }

    public static boolean isOpenableInApp(File f) {
        return ((f.isDirectory() && APP.DIR_SKINS.equals(f.getParentFile())) || Util.isValidSkinFile(f)) ||
               ((f.isDirectory() && APP.DIR_WIDGETS.equals(f.getParentFile())) || Util.isValidWidgetFile(f)) ||
               AudioFileFormat.isSupported(f,Use.PLAYBACK) || ImageFileFormat.isSupported(f);
    }

    public static void openIn(File f, boolean inApp) {
        // open skin - always in app
        if((f.isDirectory() && APP.DIR_SKINS.equals(f.getParentFile())) || Util.isValidSkinFile(f)) {
            Gui.setSkin(Util.getName(f));
        }

        // open widget
        else if((f.isDirectory() && APP.DIR_WIDGETS.equals(f.getParentFile())) || Util.isValidWidgetFile(f)) {
            String n = Util.getName(f);
            APP.widgetManager.find(n, NO_LAYOUT, false);
        }

        // open audio file
        else if (inApp && AudioFileFormat.isSupported(f,Use.PLAYBACK)) {
            PlaylistManager.use(p -> p.addUri(f.toURI()));
        }

        // open image file
        else if (inApp && ImageFileFormat.isSupported(f)) {
            APP.widgetManager.use(ImageDisplayFeature.class, NO_LAYOUT, w->w.showImage(f));
        }

        // delegate to native app cant handle
        else open(f);
    }

    public static void openIn(List<File> files, boolean inApp) {
        if(files.isEmpty()) return;
        if(files.size()==1) {
            openIn(files.get(0), inApp);
        } else {
            if(inApp) {
                List<File> audio = filter(files, f -> AudioFileFormat.isSupported(f, Use.PLAYBACK));
                List<File> images = filter(files, f -> ImageFileFormat.isSupported(f));

                if(!audio.isEmpty())
                    PlaylistManager.use(p -> p.addUris(map(audio,File::toURI)));

                if(images.size()==1) {
                    APP.widgetManager.use(ImageDisplayFeature.class,NO_LAYOUT, w->w.showImage(images.get(0)));
                } else if (images.size()>1) {
                    APP.widgetManager.use(ImagesDisplayFeature.class,NO_LAYOUT, w->w.showImages(images));
                }
            } else {
                browse(files.stream());
            }
        }
    }

    public static File chooseFile(String title, FileType type, File initial, Window w, ExtensionFilter... exts) {
        if(type==DIRECTORY) {
            DirectoryChooser c = new DirectoryChooser();
            c.setTitle(title);
            c.setInitialDirectory(util.Util.getExistingParent(initial,APP.DIR_APP));
            return c.showDialog(w);
        } else {
            FileChooser c = new FileChooser();
            c.setTitle(title);
            c.setInitialDirectory(util.Util.getExistingParent(initial,APP.DIR_APP));
            if (exts !=null) c.getExtensionFilters().addAll(exts);
            return c.showOpenDialog(w);
        }
    }

    public static List<File> chooseFiles(String title, File initial, Window w, ExtensionFilter... exts) {
        FileChooser c = new FileChooser();
        c.setTitle(title);
        c.setInitialDirectory(util.Util.getExistingParent(initial,APP.DIR_APP));
        if (exts !=null) c.getExtensionFilters().addAll(exts);
        return c.showOpenMultipleDialog(w);
    }

    public static void saveFile(String title, File initial, String initialName, Window w, ExtensionFilter... exts) {
        FileChooser c = new FileChooser();
        c.setTitle(title);
        c.setInitialDirectory(util.Util.getExistingParent(initial,APP.DIR_APP));
        c.setInitialFileName(title);
        if (exts !=null) c.getExtensionFilters().addAll(exts);
        c.showSaveDialog(w);
    }

    private static void openWindowsExplorerAndSelect(File f) throws IOException {
        // TODO: make sure the path does not have to be quoted in " like:  "path". Although quoting the path
        // does cause FileNotFoundException, the explorer.exe does open and select the file. I added the
        // quoting after I noticed files with ',' do not work properly, but I have removed the quiting
        // since its working now. Not sure what is going on.
        // Anyway, here are some alternatives to play with, in case the problem reappears:
        // new ProcessBuilder("explorer.exe /select," + f.getPath()).start();
         Runtime.getRuntime().exec(new String[] { "explorer.exe", "/select,", "\"" + f.getPath() + "\""});
//        Runtime.getRuntime().exec("explorer.exe /select," + f.getPath());
    }

}