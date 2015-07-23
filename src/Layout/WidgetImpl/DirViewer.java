/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Layout.WidgetImpl;

import Configuration.Config;
import Configuration.Config.ListAccessor;
import Configuration.IsConfig;
import Layout.Widgets.IsWidget;
import Layout.Widgets.Widget;
import static Layout.Widgets.Widget.Group.OTHER;
import Layout.Widgets.controller.ClassController;
import gui.objects.image.Thumbnail;
import gui.pane.CellPane;
import java.io.File;
import static java.lang.Character.isAlphabetic;
import java.util.ArrayList;
import java.util.List;
import static java.util.stream.Collectors.toList;
import java.util.stream.Stream;
import static javafx.collections.FXCollections.observableArrayList;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseButton.SECONDARY;
import static javafx.scene.input.MouseEvent.MOUSE_CLICKED;
import javafx.scene.layout.*;
import util.File.Environment;
import util.File.FileUtil;
import static util.File.FileUtil.getName;
import static util.File.FileUtil.listFiles;
import util.File.ImageFileFormat;
import static util.Util.setAnchors;
import static util.functional.Util.by;
import static util.functional.Util.filterMap;
import static util.functional.Util.split;
import static util.functional.Util.stream;
import static util.functional.Util.toS;

/**
 *
 * @author Plutonium_
 */
@IsWidget
@Widget.Info(
    author = "Martin Polakovic",
    programmer = "Martin Polakovic",
    name = "Dir Viewer",
    description = "Displays directory hierarchy and files as thumbnails in a "
            + "vertically scrollable grid. Intended as simple library",
    howto = "",
    notes = "",
    version = "0.3",
    year = "2015",
    group = OTHER
)
public class DirViewer extends ClassController {
    
    @IsConfig(name = "Location", info = "Root directory the contents of to display "
            + "This is not a file system browser, and it is not possible to "
            + "visit parent of this directory.")
    final ListAccessor<File> files = new ListAccessor<>(() -> new File("C:\\"),f -> Config.forValue("File",f));
    
    Cell item = null;
    CellPane cells = new CellPane(160,220,5);
    
    public DirViewer() {
        addEventHandler(MOUSE_CLICKED, e -> {
            if(e.getButton()==SECONDARY && item!=null && item.parent!=null) {
                viewDir(item.parent);
            }
        });
        
        files.onInvalid(list -> viewDir(new TopCell()));
        
        ScrollPane layout = cells.scrollable();
        getChildren().add(layout);
        setAnchors(layout,0);
        
        setOnScroll(Event::consume);
    }

    @Override
    public void refresh() {
        viewDir(new TopCell());
    }
    
    public void viewDir(Cell dir) {
        item = dir;
        cells.getChildren().clear();
        if(item!=null) 
            cells.getChildren().addAll(filterMap(item.children(),i->
                !ImageFileFormat.isSupported(i.val) && !i.val.isHidden() && i.val.canRead()
            ,Cell::load));
    }
    
    
    public class Cell {
        
        public final File val;
        public final Cell parent;
        
        private boolean isLeaf;
        private boolean isFirstTimeLeaf = true;
        private boolean isFirstTimeChildren = true;
        private final ObservableList<Cell> childr = observableArrayList();
        
        public Cell(Cell parent, File value) {
            this.val = value;
            this.parent = parent;
        }
        
        public ObservableList<Cell> children() {
            if (isFirstTimeChildren) {
                childr.setAll(buildChildren());
                isFirstTimeChildren = false;
            }
            return childr;
        }
        
        boolean isFirstTimeCover = true;
        Image cover = null;
        public File getCoverFile() {
            File f = val;
            File i = f.isDirectory() ? new File(f,"cover.jpg") : filImage(f);
            return i;
        }
        public Image getCover() {
            return cover;
        }
        public void setCover(Image i) {
            cover = i;
            isFirstTimeCover=false;
        }
        
        private File filImage(File f) {
            File i = new File(f.getParent(),FileUtil.getName(f)+".jpg");
            if(!i.exists()) return parent.getCoverFile();
            else return i;
        }

        protected List<Cell> buildChildren() {
            // we want to sort the items : directories first
            // we make use of the fact that listFiles() gives us already
            // sorted list
            List<Cell> dirs = new ArrayList<>();
            List<Cell> fils = new ArrayList<>();
            listFiles(val).forEach(f -> {
                if(!f.isDirectory()) dirs.add(new Cell(this,f));
                else                 fils.add(new Cell(this,f));
            });
                   dirs.addAll(fils);
            return dirs;
        }
        
        
        private VBox root;
        public Node load() {
            if(root==null) {
                File f = val;
                root = new VBox();
                root.setPrefSize(160,220);

                Thumbnail t = new Thumbnail(160,200);
                if(isFirstTimeCover) {
                    File cf = getCoverFile();
                    t.image.addListener((o,ov,nv) -> setCover(nv));
                    t.loadImage(cf);
                } else {
                    t.loadImage(getCover());
                }
                t.getPane().setOnMouseClicked(e -> {
                    if(e.getButton()==PRIMARY && e.getClickCount()==2) {
                        if(f.isDirectory()) viewDir(this);
                        else Environment.open(f);
                        e.consume();
                    }
                });

                String m = getName(f);
                       m = m.replaceAll("\\[.*\\]", "");
                       m = m.replaceAll("\\(.*\\)", "");
                       m = m.trim();
                String n = f.isDirectory() ? m : toS(split(m," ",x->x),
                    s -> s.length()<=1 || !isAlphabetic(s.charAt(0)) ? s : s.substring(0,1),""
                );
                Label l = new Label(n);
                root.getChildren().addAll(t.getPane(),l);

                root.setAlignment(Pos.CENTER);
            }
            return root;
        }
    }
    public class TopCell extends Cell {

        public TopCell() {
            super(null,null);System.out.println("new");
        }

        @Override
        protected List<Cell> buildChildren() {
            return files.list.stream()
                      .flatMap(f->f.isDirectory() ? stream(listFiles(f)) : Stream.empty())
                      .sorted(by(File::getName))
                      .map(f -> new Cell(this,f))
                      .collect(toList());
        }

        @Override
        public File getCoverFile() {
            return null;
        }
        
    }
}