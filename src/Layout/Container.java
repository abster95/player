
package Layout;

import Configuration.PropertyMap;
import com.thoughtworks.xstream.annotations.XStreamOmitField;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javafx.scene.Node;
import javafx.scene.layout.AnchorPane;

/**
 * @author uranium
 * Defines behavior for Container - Component able to store Containers or Widgets.
 * The point is to create modular layouts.
 * 
 * Containers are objects storing its children and implementing methods for high level
 * functioning - layout level behavior such as loading the component, its children and
 * other tasks involving layout in general.
 * The key idea is that Containers are not GUI components, Containers wrap them. This creates
 * an abstraction layer that allows for defining layout maps. Container storing all its
 * children recursively is basically a layout map.
 * This is made use of in layout managing department. Storing root Container
 * equals storing the whole layout map.
 * Thats why Containers need to be lightweight objects able to be serialized.
 */
public abstract class Container extends Component implements AltState {
    
    /*
     * Property map - map of properties. The map serves as unified property
     * storage mechanism to allow easy manipulation (serialization, etc) of
     * properties.
     * Its responsibility of the wrapped component to maintain updated state
     * reflecting the properties in the map.
     */
    public final PropertyMap properties = new PropertyMap();
    @XStreamOmitField
    AnchorPane parent_pane;
    @XStreamOmitField
    Container parent;
    
    @Override
    public String getName() {
        return this.getClass().getName();
    }
    
    /**
     * Equivalent to hasParent()
     * @return true if container is root - has no parent
     */
    public boolean isRoot() {
        return (parent == null);
    }
    
    /** @return whether has parent */
    public boolean hasParent() {
        return (parent == null);
    }
    
    /** @return parent container this container is child of */
    public Container getParent() {
        return parent;
    }
    
    /** @return the children */
    public abstract Map<Integer, ? extends Component> getChildren();
    
    /**
     * Adds component to specified index as child of the container. The index serves
     * for the purpose of identifying individual children in case of multiple
     * children.
     * The inner logic handling the indexes is left up on the individual
     * container implementation.
     * Container must handle
     * - adding the child to its child map
     * - removing previously assigned children
     * - reload itself so the change takes place visually
     * - handle wrong indexes and exceptions)
     * @WARNING: invalid indexes must never be changed to any of the children, but
     * ignored! This is because indexOf() method returns invalid (but still number)
     * index if component is not found. Therefore such index must be ignored.
     * @param index index of a child.
     * @param w component to ad or null to remove it
     */
    public abstract void addChild(int index, Component w);
    
    /**
     * Removes child of this container if it exists.
     * @param w
     */
    public void removeChild(Component w) {
        int i = indexOf(w);
        if (i != -1) removeChild(i);
        
    }
    
    /**
     * Removes child of this container at specified index.
     * @param index 
     */
    public void removeChild(int index) {
        addChild(index, null);
    }
    
    /**
     * Swaps children in the layout.
     * @param w1 child of this widget to swap.
     * @param c2 child to swap with
     * @param w2 container containing the child to swap with
     */
    public void swapChildren(Component w1, Container c2, Component w2) {
        System.out.println("swapping "+w1.getName() + " with " + w2.getName());
        Container c1 = this;
        if (c1.equals(c2)) return;
        int i1 = c1.indexOf(w1);
        int i2 = c2.indexOf(w2);
        c1.addChild(i1, w2);
        c2.addChild(i2, w1);
        c1.load();
        c2.load();
    }
    
    /**
     * Returns index of a child or -1 if no child
     * @param c 
     * @return index of a child or -1 if no child
     */
    public int indexOf(Component c) {
        for (Map.Entry<Integer, ? extends Component> entry: getChildren().entrySet()) {
            if (entry.getValue().equals(c))
                return entry.getKey();
        }
        return -1;
    }
    
    /**
     * Returns all components in layout map of which this is the root. In other 
     * words all  children recursively. The root is included in the list.
     * @return 
     */
    public List<Component> getAllChildren() {
        List<Component> out = new ArrayList<>();
                        out.add(this);
        for (Component w: getChildren().values()) {
            if(w!=null) out.add(w);
            if (w instanceof Container)
                out.addAll(((Container)w).getAllChildren());
        }
        return out;
    }
    /**
     * Returns all widgets in layout map of which this is the root. In other words
     * all widget children recursively.
     * @return 
     */
    public List<Widget> getAllWidgets() {
        List<Widget> out = new ArrayList<>();
        for (Component w: getChildren().values()) {
            if (w instanceof Container)
                out.addAll(((Container)w).getAllWidgets());
            else
            if (w instanceof Widget)
                out.add((Widget)w);
        }
        return out;
    }
    /**
     * Returns all containers in layout map of which this is the root. In other words
     * all container children recursively. The root is included in the list.
     * @return 
     */
    public List<Container> getAllContainers() {
        List<Container> out = new ArrayList<>();
                        out.add(this);
        for (Component c: getChildren().values()) {
            if (c instanceof Container) {
                out.add((Container)c);
                out.addAll(((Container)c).getAllContainers());
            }
        }
        return out;
    }
    
    /**
     * Loads the graphical element this container wraps. Furthermore all the children
     * get loaded too.
     * Use for as the first load of the controller to assign the parent_pane.
     * Here, the term parent isnt parent Container, but instead the very AnchorPane
     * this container will be loaded into.
     * @param _parent
     * @return 
     */
    public Node load(AnchorPane _parent){
        parent_pane = _parent;
        return load();   
    }
    
    /**
     * Effectively a reload.
     * Loads the whole container and its children - the whole layout sub branch
     * having this container as root - to its parent_pane. The parent_pane must be assigned
     * before calling this method.
     * @return 
     */
    @Override
    public abstract Node load();
    
    /**
     * Closes this container and its content. Can not be undone.
     * In practice, this method removes this container as a child from its 
     * parent container.
     * If the container is root, this method is a no-op.
     */
    public void close() {
        if (!isRoot())
            parent.removeChild(this);
    }
    
    public void setParentPane(AnchorPane pane) {
        parent_pane = pane;
    }

    /**
     * Properly links up this container with its children and propagates this
     * call down on the children and so on.
     * This method is required to fully initialize the layout after deserialization
     * because some field values can not be serialized and need to be manually
     * initialized.
     * Use on layout reload, immediately after the container.load() method.
     */
    public void initialize() {
        for (Component c: getChildren().values()) {
            if (c instanceof Container) {
                ((Container)c).parent = this;
                ((Container)c).initialize();
            }
        }
    }
    
/******************************************************************************/
    
    @Override
    public void show() {
        getChildren().values().stream().filter(child -> child instanceof AltState)
                .forEach(child -> ((AltState) child).show());
    }
    @Override
    public void hide() {
        getChildren().values().stream().filter(child -> child instanceof AltState)
                .forEach(child -> ((AltState) child).hide()); 
    }
}