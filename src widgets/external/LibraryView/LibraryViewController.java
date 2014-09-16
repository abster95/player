
package LibraryView;

import AudioPlayer.playlist.Playlist;
import AudioPlayer.playlist.PlaylistItem;
import AudioPlayer.playlist.PlaylistManager;
import AudioPlayer.services.Database.DB;
import AudioPlayer.tagging.Metadata;
import static AudioPlayer.tagging.Metadata.Field.CATEGORY;
import AudioPlayer.tagging.MetadataGroup;
import Configuration.IsConfig;
import GUI.GUI;
import GUI.objects.ContextMenu.ContentContextMenu;
import GUI.objects.ContextMenu.TableContextMenuInstance;
import GUI.objects.FilterGenerator.TableFilterGenerator;
import Layout.Widgets.FXMLController;
import Layout.Widgets.Features.TaggingFeature;
import static Layout.Widgets.Widget.Group.LIBRARY;
import Layout.Widgets.Widget.Info;
import Layout.Widgets.WidgetManager;
import static Layout.Widgets.WidgetManager.WidgetSource.NOLAYOUT;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.Event;
import javafx.fxml.FXML;
import static javafx.scene.control.SelectionMode.MULTIPLE;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import static javafx.scene.input.KeyCode.ENTER;
import static javafx.scene.input.KeyCode.ESCAPE;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseButton.SECONDARY;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import org.reactfx.Subscription;
import org.reactfx.util.Tuples;
import utilities.FxTimer;
import utilities.Util;
import static utilities.Util.createmenuItem;
import utilities.access.Accessor;

/**
 *
 * @author Plutonium_
 */
@Info(
    author = "Martin Polakovic",
    programmer = "Martin Polakovic",
    name = "Library",
    description = "Provides access to database.",
    howto = "Available actions:\n" +
            "    Item left click : Selects item\n" +
            "    Item right click : Opens context menu\n" +
            "    Item double click : Plays item\n" +
//            "    Item drag : \n" +
            "    Press ENTER : Plays item\n" +
            "    Press ESC : Clear selection & filter\n" +
//            "    Type : Searches for item - applies filter\n" +
            "    Scroll : Scroll table vertically\n" +
            "    Scroll + SHIFT : Scroll table horizontally\n" +
            "    Drag column : Changes column order\n" +
            "    Click column : Changes sort order - ascending,\n" +
            "                   descending, none\n" +
            "    Click column + SHIFT : Sorts by multiple columns\n",
    notes = "",
    version = "0.6",
    year = "2014",
    group = LIBRARY
)
public class LibraryViewController extends FXMLController {
    
    private @FXML AnchorPane root;
    private @FXML VBox content;
    private final TableView<MetadataGroup> table = new TableView();
    private final ObservableList<MetadataGroup> allitems = FXCollections.observableArrayList();
    private final FilteredList<MetadataGroup> filtereditems = new FilteredList(allitems);
    private final SortedList<MetadataGroup> sortedItems = new SortedList<>(filtereditems);
    private final TableFilterGenerator<MetadataGroup,MetadataGroup.Field> searchBox = new TableFilterGenerator(filtereditems);
    private Subscription dbMonitor;
    
    // configurables
    @IsConfig(name = "Field")
    public final Accessor<Metadata.Field> fieldFilter = new Accessor<>(CATEGORY, v -> {
        table.getSelectionModel().clearSelection();
        table.getColumns().removeAll(table.getColumns().subList(1, table.getColumns().size()));
        table.setItems(sortedItems);
        sortedItems.comparatorProperty().bind(table.comparatorProperty());
        
        // get new data
        List<MetadataGroup> result = DB.getAllGroups(v);
        // reconstruct columns
        if (table.getColumns().size() <= 1) {
            for(MetadataGroup.Field field : MetadataGroup.Field.values()) {
                String name = field.toString(v);
                TableColumn<MetadataGroup,Object> c = new TableColumn(name);
                c.setCellValueFactory( cf -> {
                    if(cf.getValue()==null) return null;
                    return new ReadOnlyObjectWrapper(cf.getValue().getField(field));
                });
                c.setCellFactory(Util.DEFAULT_ALIGNED_CELL_FACTORY(field.getType(v)));
                table.getColumns().add(c);
            }
        }
        
        allitems.setAll(FXCollections.observableArrayList(result));
        
        // unfortunately the table cells dont get updated for some reason, resizing
        // table or column manually with cursor will do the job, so we invoke that
        // action programmatically, with a delay (or it wont work)
        FxTimer.run(100, ()->{
            TableColumn c = table.getColumns().get(table.getColumns().size()-1);
            table.columnResizePolicyProperty().get().call(new TableView.ResizeFeatures(table, c, c.getWidth()));
        });
        
        
        searchBox.setData(Arrays.asList(MetadataGroup.Field.values()).stream()
                .map(mgf->Tuples.t(mgf.toString(v),mgf.getType(v),mgf)).collect(Collectors.toList()));
    });

    @Override
    public void init() {
        content.getChildren().addAll(searchBox, table);
        
        table.getSelectionModel().setSelectionMode(MULTIPLE);
        table.setFixedCellSize(GUI.font.getValue().getSize() + 5);
        
        // add index column
        TableColumn indexColumn = Util.createIndexColumn("#");
        table.getColumns().add(indexColumn);
        
        // context menu
        table.setOnMouseClicked( e -> {
            if(e.getButton()==PRIMARY) {
                if(e.getClickCount()==2)
                    play(Util.copySelectedItems(table));
            } else
            if(e.getButton()==SECONDARY)
                contxt_menu.show(table, e);
        });
        
        // key actions
        table.setOnKeyReleased( e -> {
            if (e.getCode() == ENTER)     // play first of the selected
                play(table.getSelectionModel().getSelectedItems());
            else if (e.getCode() == ESCAPE)    // deselect
                table.getSelectionModel().clearSelection();
        });
        
        // listen for database changes to refresh library
        dbMonitor = DB.librarychange.subscribe( nothing -> fieldFilter.applyValue());
        
        table.getSelectionModel().selectedItemProperty().addListener( (o,ov,nv) -> {
            if(nv!=null)
                DB.fieldSelectionChange.push(fieldFilter.getValue(),nv.getValue());
        });
        
        // prevent scrol event to propagate up
        root.setOnScroll(Event::consume);
    }

    @Override
    public void refresh() {
        fieldFilter.applyValue();
    }

    @Override
    public void close() {
        // stop listening for db changes
        dbMonitor.unsubscribe();
    }
    
    
/******************************** PUBLIC API **********************************/
    
/******************************** CONTEXT MENU ********************************/
    
    private static final TableContextMenuInstance<MetadataGroup> contxt_menu = new TableContextMenuInstance<>(
        () -> {
            ContentContextMenu<List<MetadataGroup>> m = new ContentContextMenu();
            m.getItems().addAll(
                createmenuItem("Play items", e -> play(m.getValue())),
                createmenuItem("Enqueue items", e -> PlaylistManager.addItems(dbFetch(m.getValue()))),
                createmenuItem("Update from file", e -> DB.updateItemsFromFile(dbFetch(m.getValue()))),
                createmenuItem("Remove from library", e -> DB.removeItems(dbFetch(m.getValue()))),
                createmenuItem("Edit the item/s in tag editor", e -> WidgetManager.use(TaggingFeature.class, NOLAYOUT,w->w.read(dbFetch(m.getValue())))));
            return m;
        },
        (menu,table) -> menu.setValue(Util.copySelectedItems(table))
    );
    
    private static List<Metadata> dbFetch(List<MetadataGroup> filters) {
        return DB.getAllItemsWhere(filters.get(0).getField(), filters.get(0).getValue());
    }
    private static void play(List<MetadataGroup> filters) {
        if(filters.isEmpty()) return;
        List<PlaylistItem> to_play = new ArrayList();
        dbFetch(filters).stream().map(Metadata::toPlaylistItem).forEach(to_play::add);
        PlaylistManager.playPlaylist(new Playlist(to_play));
    }
}