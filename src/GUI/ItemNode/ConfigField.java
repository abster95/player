
package GUI.ItemNode;

import Action.Action;
import Configuration.Config;
import GUI.ItemNode.ItemTextFields.FileTextField;
import GUI.ItemNode.ItemTextFields.FontTextField;
import GUI.objects.CheckIcon;
import GUI.objects.Icon;
import GUI.objects.combobox.ImprovedComboBox;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIconName.*;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import javafx.animation.FadeTransition;
import javafx.geometry.Insets;
import static javafx.geometry.Pos.CENTER_LEFT;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import static javafx.scene.input.KeyCode.*;
import static javafx.scene.input.KeyEvent.KEY_RELEASED;
import static javafx.scene.input.MouseEvent.MOUSE_ENTERED;
import static javafx.scene.input.MouseEvent.MOUSE_EXITED;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import static javafx.scene.layout.Priority.ALWAYS;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.util.Callback;
import javafx.util.Duration;
import org.controlsfx.control.textfield.CustomTextField;
import util.Password;
import static util.Util.*;
import static util.async.Async.run;
import static util.functional.Util.cmpareBy;

/**
 * Editable and setable graphic control for configuring {@Config}.
 * <p>
 * Convenient way to create wide and diverse property sheets, that take 
 * type of configuration into consideration. For example
 * for boolean CheckBox control will be used, for enum ComboBox etc...
 *
 * @author uranium
 */
abstract public class ConfigField<T> {
    
    private static final Tooltip defB_tooltip = new Tooltip("Default value");
    private static final Tooltip globB_tooltip = new Tooltip("Whether shortcut is global (true) or local.");
    
    private final Label label = new Label();
    private final HBox root = new HBox();
    final Config<T> config;
    private boolean applyOnChange = true;
    private Icon defB;
    
    private ConfigField(Config<T> c) {
        config = c;
        label.setText(c.getGuiName());
        
        root.setMinSize(0,0);
        root.setPrefSize(HBox.USE_COMPUTED_SIZE,20); // not sure why this needs manual resizing
        root.setSpacing(5);
        root.setAlignment(CENTER_LEFT);
        root.setPadding(new Insets(0, 15, 0, 0)); // space for defB (11+5)(defB.width+box.spacing)
        
        // display default button when hovered for certain time
        root.addEventFilter(MOUSE_ENTERED, e -> {
            // wait delay
            run(270, () -> {
                // no need to do anything if hover ended
                if(root.isHover()) {
                    // lazily build the button when requested
                    // we dont want hundreds of buttons we will never use anyway
                    if(defB==null) {
                        defB = new Icon(RECYCLE, 11, null, this::setNapplyDefault);
                        defB.setTooltip(defB_tooltip);
                        defB.setOpacity(0);
                        defB.getStyleClass().setAll("congfig-field-default-button");
                        root.getChildren().add(defB);
                        root.setPadding(Insets.EMPTY);
                    }
                    // show it
                    FadeTransition fa = new FadeTransition(Duration.millis(450), defB);
                    fa.stop();
                    fa.setToValue(1);
                    fa.play();
                }
            });
        });
        // hide default button
        root.addEventFilter(MOUSE_EXITED, e-> {
            // return if nothing to hide
            if(defB == null) return;
            // hide it
            FadeTransition fa = new FadeTransition(Duration.millis(450), defB);
            fa.stop();
            fa.setDelay(Duration.ZERO);
            fa.setToValue(0);
            fa.play();
        });
    }
    
    /**
     * Simply compares the current value with the one obtained from Config.
     * Equivalent to: !config.getValue().equals(getItem());
     * @return true if has value that has not been applied
     */
    public boolean hasUnappliedValue() {
        return !config.getValue().equals(getItem());
    }
    
    /**
     * Sets editability by disabling the Nodes responsible for value change
     * @param val 
     */
    public void setEditable(boolean val) {
        getControl().setDisable(!val);
    }
    
    /**
     * Use to get the label to attach it to a scene graph.
     * @return label describing this field
     */
    public Label getLabel() {
        return label;
    }
    
    /**
     * Use to get the control node for setting and displaying the value to 
     * attach it to a scene graph.
     * @return setter control for this field
     */
    public Node getNode() {
        if(!root.getChildren().contains(getControl()))
            root.getChildren().add(0, getControl());
        HBox.setHgrow(getControl(), ALWAYS);
        return root;
    }
    
    /**
     * Use to get the control node for setting and displaying the value to 
     * attach it to a scene graph.
     * @return setter control for this field
     */
    abstract Node getControl();
    
    public void focus() {
        getControl().requestFocus();
    }

    /**
     * {@inheritDoc}
     * Returns the currently displayed value. Use to get for custom implementations
     * of setting and applying the value. Usually it is compared to the value obtain
     * from the Config from the {@link #getConfig()} method and then decided whether
     * it should be set or applied or ignored.
     * <p>
     * Current value is value displayed. Because it can be edited in real time
     * by the user and it can be represented visually by a String or differently
     * it doesnt have to be valid at all times - therefore, if the value is not
     * valid (can not be obtained) the method returns currently set value.
     * 
     * @return 
     */
    public abstract T getItem();
    
    /**
     * Refreshes the content of this config field. The content is read from the
     * Config and as such reflects the real value. Using this method after the
     * applying the new value will confirm the success visually to the user.
     */
    public abstract void refreshItem();
    
    /**
     * Returns the {@link Config}. Use for custom implementations of setting and
     * applying new values.
     * @return name of the field
     */
    public Config getConfig() {
        return config;
    }
    
    public boolean isApplyOnChange() {
        return applyOnChange;
    }
    
    public void setApplyOnChange(boolean val) {
        applyOnChange = val;
    }
    
    /**
     * Convenience method and default implementation of set and apply mechanism.
     * Also calls the {@link #refreshItem()} when needed.
     * <p>
     * Checks te current value and compares it with the value obtainable from
     * the config (representing the currently set value) and sets and applies
     * the current value of the values differ.
     * <p>
     * To understand the difference
     * between changing and applying refer to {@link Config}.
     * 
     * @return whether any change occured. Occurs when change needs to be applied.
     * Equivalent to calling {@link #hasUnappliedValue()} method.
     */
    public boolean applyNsetIfNeed() {
        if(hasUnappliedValue()) {
            config.setNapplyValue(getItem());
            refreshItem();
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * Sets and applies default value of the config if it has different value
     * set.
     */
    public final void setNapplyDefault() {
        T defVal = config.getDefaultValue();
        if(!config.getValue().equals(defVal)) {
            config.setNapplyValue(defVal);
            refreshItem();
        }
    }
    
/******************************************************************************/
    
    private static Map<Class,Callback<Config,ConfigField>> m = new HashMap();
    
    static {
        m.put(boolean.class, f -> new BooleanField(f));
        m.put(Boolean.class, f -> new BooleanField(f));
        m.put(String.class, f -> new GeneralField(f));
        m.put(Action.class, f -> new ShortcutField(f));
        m.put(Color.class, f -> new ColorField(f));
        m.put(File.class, f -> new FileField(f));
        m.put(Font.class, f -> new FontField(f));
        m.put(Password.class, f -> new PasswordField(f));
    }
    
    /**
     * Creates ConfigFfield best suited for the specified Field.
     * @param f field for which the GUI will be created
     */
    public static ConfigField create(Config f) {
        
        ConfigField cf = null;
        if (f.isTypeEnumerable()) cf = new EnumertionField(f);
        else if(f.isMinMax()) cf = new SliderField(f);
        else cf = m.getOrDefault(f.getType(), GeneralField::new).call(f);
        
        cf.setEditable(f.isEditable());
        
        if(!f.getInfo().isEmpty()) {
            Tooltip t = new Tooltip(f.getInfo());
                    t.setWrapText(true);
                    t.setMaxWidth(300);
            cf.getLabel().setTooltip(t);
            if(!cf.getClass().isInstance(ShortcutField.class))
                Tooltip.install(cf.getControl(),t);
        }
        
        return cf;
    }
    
/***************************** IMPLEMENTATIONS ********************************/
    
    private static final class PasswordField extends ConfigField<Password>{
        
        javafx.scene.control.PasswordField passF = new javafx.scene.control.PasswordField();
        
        public PasswordField(Config<Password> c) {
            super(c);
            refreshItem();
        }

        @Override
        Node getControl() {
            return passF;
        }

        @Override
        public Password getItem() {
            return new Password(passF.getText());
        }

        @Override
        public void refreshItem() {
            passF.setText(config.getValue().get());
        }
        
    }    
    
    private static final class GeneralField extends ConfigField<Object> {
        private static final Tooltip okTooltip = new Tooltip("Apply value");
        private static final Tooltip warnTooltip = new Tooltip("Erroneous value");
        CustomTextField txtF = new CustomTextField();
        final boolean allow_empty; // only for string
        Icon okBL= new Icon();
        Icon warnB = new Icon();
        AnchorPane okB = new AnchorPane(okBL);
        
        private GeneralField(Config c) {
            super(c);
            allow_empty = c.getType().equals(String.class);
            
            // doesnt work because of CustomTextField instead f TextField
            // restrict input
//            if(c.isTypeNumber())
//                InputConstraints.numbersOnly(txtF, !c.isTypeNumberNonegative(), c.isTypeFloatingNumber());
            
            okBL.getStyleClass().setAll("congfig-field-ok-button");
            okBL.icon_size.set(11);
            okBL.setTooltip(okTooltip);
            setAnchors(okBL,0,0,0,8);       // fix alignment
            warnB.icon_size.set(11);
            warnB.getStyleClass().setAll("congfig-field-warn-button");
            warnB.setTooltip(warnTooltip);
            
            txtF.setContextMenu(null);
            txtF.getStyleClass().setAll("text-field","text-input");
            txtF.setPromptText(c.getValueS());
            // start edit
            txtF.setOnMouseClicked( e -> {
                if (txtF.getText().isEmpty())
                    txtF.setText(txtF.getPromptText());
                e.consume();
            });
            
            txtF.focusedProperty().addListener((o,ov,nv) -> {
                if(nv) {
                    if (txtF.getText().isEmpty()) 
                        txtF.setText(txtF.getPromptText());
                } else {
                    // the timer solves a little bug where the focus shift from
                    // txtF to okB has a delay which we need to jump over
                    run(80, () -> {
                        if(!okBL.isFocused() && !okB.isFocused()) {
                            txtF.setText("");
                            showOkButton(false);
                        }
                    });
                }
            });
            
            if (allow_empty)
                txtF.addEventHandler(KEY_RELEASED, e -> {
                    if (e.getCode()==BACK_SPACE || e.getCode()==DELETE) {
                        if (txtF.getPromptText().isEmpty())
                            txtF.setPromptText(config.getValueS());
                        else
                            txtF.setPromptText("");
                    }
                });
            // applying value
            txtF.textProperty().addListener((o,ov,nv)-> {
                boolean erroneous = getItem()==null;
                boolean applicable = (allow_empty || (!allow_empty && !nv.isEmpty())) && !nv.equals(txtF.getPromptText());
                showOkButton(applicable && !erroneous);
                showWarnButton(erroneous);
            });
            okBL.setOnMouseClicked( e -> apply());
            txtF.setOnKeyPressed( e -> { if(e.getCode()==ENTER) apply(); });
        }
        
        @Override public Control getControl() {
            return txtF;
        }

        @Override
        public void focus() {
            txtF.requestFocus();
            txtF.selectAll();
        }
        
        @Override public Object getItem() {
            String text = txtF.getText();
            if(allow_empty) {
                return text.isEmpty() ? txtF.getPromptText() : text;
            } else {
                return text.isEmpty() ? config.getValue() : config.fromS(text);
            }
        }
        @Override public void refreshItem() {
            txtF.setPromptText(config.getValueS());
            txtF.setText("");
            showOkButton(false);
        }
        private void apply() {
            if(isApplyOnChange()) applyNsetIfNeed();
        }
        private void showOkButton(boolean val) {
            if (val) txtF.setLeft(okB);
            else txtF.setLeft(new Region());
            okB.setVisible(val);
        }
        private void showWarnButton(boolean val) {
            if (val) txtF.setRight(warnB);
            else txtF.setRight(new Region());
            warnB.setVisible(val);
        }
        
    }
    
    private static final class BooleanField extends ConfigField<Boolean> {
        CheckIcon cBox;
        
        private BooleanField(Config<Boolean> c) {
            super(c);
            cBox = new CheckIcon();
            refreshItem();
            cBox.selected.addListener((o,ov,nv)->{
                if(isApplyOnChange()) applyNsetIfNeed();
            });
        }
        
        @Override public Node getControl() {
            return cBox;
        }
        @Override public Boolean getItem() {
            return cBox.selected.get();
        }
        @Override public void refreshItem() {
            cBox.selected.set(config.getValue());
        }
    }
    
    private static final class SliderField extends ConfigField<Number> {
        Slider slider;
        Label cur, min, max;
        HBox box;
        private SliderField(Config<Number> c) {
            super(c);
            double v = c.getValue().doubleValue();
            
            min = new Label(String.valueOf(c.getMin()));
            max = new Label(String.valueOf(c.getMax()));
            
            slider = new Slider(c.getMin(),c.getMax(),v);
            cur = new Label(getItem().toString());
            cur.setPadding(new Insets(0, 5, 0, 0)); // add gap
            // there is a slight bug where isValueChanging is false even if it
            // shouldnt. It appears when mouse clicks NOT on the thumb but on
            // the slider track instead and keeps dragging. valueChanging doesn
            // activate
            slider.valueProperty().addListener((o,ov,nv) -> {
                // also bug with snap to tick, which doesnt work on mouse drag
                // so we use getItem() which returns correct value
                cur.setText(getItem().toString());
                if(isApplyOnChange() && !slider.isValueChanging())
                    applyNsetIfNeed();
            });
            slider.setOnMouseReleased(e -> {
                if(isApplyOnChange()) applyNsetIfNeed();
            });
            
            // add scrolling support
            // unfortunately this control is often within scrollpane and user
            // might end up changing value of this config field while scrolling
            // disable this
            slider.setBlockIncrement((c.getMax()-c.getMin())/20);
//            slider.setOnScroll( e -> {
//                if (e.getDeltaY()>0) slider.increment();
//                else slider.decrement();
//                e.consume();
//            });
            slider.setMinWidth(-1);
            slider.setPrefWidth(-1);
            slider.setMaxWidth(-1);
            
            
            box = new HBox(min,slider,max);
            box.setAlignment(CENTER_LEFT);
            box.setSpacing(5);
            
            Class<? extends Number> type = unPrimitivize(config.getType());
            if(Integer.class.equals(type) || type.equals(Long.class)) {
                box.getChildren().add(0,cur);
                slider.setMajorTickUnit(1);
                slider.setSnapToTicks(true);
            }
        }
        
        @Override public Node getControl() {
            return box;
        }
        @Override public Number getItem() {
            Double d = slider.getValue();
            Class<? extends Number> type = unPrimitivize(config.getType());
            if(Integer.class.equals(type)) return d.intValue();
            if(Double.class.equals(type)) return d;
            if(Float.class.equals(type)) return d.floatValue();
            if(Long.class.equals(type)) return d.longValue();
            if(Short.class.equals(type)) return d.shortValue();
            throw new IllegalStateException("wrong number type: " + type);
        }
        @Override public void refreshItem() {
            slider.setValue(config.getValue().doubleValue());
        }
    }
    
    private static final class EnumertionField extends ConfigField<Object> {
        ComboBox<Object> cBox;
        
        private EnumertionField(Config<Object> c) {
            super(c);
            cBox = new ImprovedComboBox(item -> enumToHuman(c.toS(item)));            
            cBox.getItems().addAll(c.enumerateValues());
            cBox.getItems().sort(cmpareBy(v->c.toS(v)));
            cBox.setValue(c.getValue());
            cBox.valueProperty().addListener((o,ov,nv) -> {
                if(isApplyOnChange()) applyNsetIfNeed();
            });
        }
        @Override public Object getItem() {
            return cBox.getValue();
        }

        @Override
        public void refreshItem() {
            cBox.setValue(config.getValue());
        }

        @Override
        Node getControl() {
            return cBox;
        }
    }
    
    private static final class ShortcutField extends ConfigField<Action> {
        TextField txtF;
        CheckIcon globB;
        HBox group;
        String t="";
        Action a;
        
        private ShortcutField(Config<Action> con) {
            super(con);
            a = con.getValue();
            txtF = new TextField();
            txtF.setPromptText(a.getKeys());
            txtF.setOnKeyReleased(e -> {
                KeyCode c = e.getCode();
                // handle substraction
                if (c==BACK_SPACE || c==DELETE) {
                    txtF.setPromptText("");
                    if (!txtF.getText().isEmpty()) txtF.setPromptText(a.getKeys());
                    
                    
                    if (t.isEmpty()) {  // set back to empty
                        txtF.setPromptText(a.getKeys());
                    } else {            // substract one key
                        if (t.indexOf('+') == -1) t="";
                        else t=t.substring(0,t.lastIndexOf('+'));
                        txtF.setText(t);
                    }
                } else if(c==ENTER) {
                    if (isApplyOnChange()) applyNsetIfNeed();
                } else if(c==ESCAPE) {
                    refreshItem();
                // handle addition
                } else {
                    t += t.isEmpty() ? c.getName() : "+" + c.getName();
                    txtF.setText(t);
                }
            });
            txtF.setEditable(false);
            txtF.setTooltip(new Tooltip(a.getInfo()));
            txtF.focusedProperty().addListener( (o,ov,nv) -> {
                if(nv) {
                    txtF.setText(txtF.getPromptText());
                } else {
                    // prevent 'deselection' if we txtF lost focus because glob
                    // received click
                    if(!globB.isFocused())
                        txtF.setText("");
                }
            });
            
            globB = new CheckIcon();
            globB.selected.set(a.isGlobal());
            globB.setTooltip(globB_tooltip);
            globB.selected.addListener((o,ov,nv) -> {
                if (isApplyOnChange()) applyNsetIfNeed();
            });
            group = new HBox(5, globB,txtF);
            group.setAlignment(CENTER_LEFT);
            group.setPadding(Insets.EMPTY);
        }
        
        @Override public Node getControl() {
            return group;
        }
        @Override public boolean hasUnappliedValue() {
            Action a = config.getValue();
            boolean sameglobal = globB.selected.get()==a.isGlobal();
            boolean sameKeys = txtF.getText().equals(a.getKeys()) || 
                    (txtF.getText().isEmpty() && txtF.getPromptText().equals(a.getKeys()));
            return !sameKeys || !sameglobal;
        }
        @Override public final boolean applyNsetIfNeed() {
            // its pointless to make new Action just for this
            // config.applyValue(getItem()); 
            // rather operate on the Action manually

            Action a = config.getValue();
            boolean sameglobal = globB.selected.get()==a.isGlobal();
            boolean sameKeys = txtF.getText().equals(a.getKeys()) || 
                    (txtF.getText().isEmpty() && txtF.getPromptText().equals(a.getKeys()));
            
            if(!sameglobal && !sameKeys)
                a.set(globB.selected.get(), txtF.getText());
            else if (!sameKeys)
                a.setKeys(txtF.getText());
            else if (!sameglobal)
                a.setGlobal(globB.selected.get());
            else {
                refreshItem();
                return false;
            }

            refreshItem();
            return true;
        }
        @Override public Action getItem() {
            return a;
        }
        @Override public void refreshItem() {
            Action a = config.getValue();
            txtF.setPromptText(a.getKeys());
            txtF.setText("");
            globB.selected.set(a.isGlobal());
        }
    }
    
    private static final class ColorField extends ConfigField<Color> {
        ColorPicker picker = new ColorPicker();
        
        private ColorField(Config<Color> c) {
            super(c);
            refreshItem();
            picker.valueProperty().addListener((o,ov,nv) -> {
                if(isApplyOnChange()) applyNsetIfNeed();
            });
        }
        
        @Override public Control getControl() {
            return picker;
        }
        @Override public Color getItem() {
            return picker.getValue();
        }
        @Override public void refreshItem() {
            picker.setValue(config.getValue());
        }
    }
      
    private static final class FontField extends ConfigField<Font> {
        FontTextField txtF = new FontTextField();
        
        private FontField(Config<Font> c) {
            super(c);
            refreshItem();
            txtF.setOnItemChange((oldFont,newFont) -> {
                if(!newFont.equals(oldFont)) {  // we shouldnt rely on Font.equals here
                    applyNsetIfNeed();
                    txtF.setPromptText(c.toS(newFont));
                }
                txtF.setText(""); // always stay in prompt text more
            });
        }
        
        @Override public Control getControl() {
            return txtF;
        }
        @Override public Font getItem() {
            return txtF.getValue();
        }
        @Override public void refreshItem() {
            txtF.setValue(config.getValue());
        }
    }
    
    private static final class FileField extends ConfigField<File> {
        FileTextField txtF = new FileTextField();
        
        public FileField(Config<File> c) {
            super(c);
            refreshItem();
            txtF.setOnItemChange((oldFile,newFile) -> {
                if(!newFile.equals(oldFile)) {
                    applyNsetIfNeed();
                    txtF.setPromptText(c.toS(newFile));
                }
                txtF.setText(""); // always stay in prompt text more
            });
        }
        
        @Override public Control getControl() {
            return txtF;
        }
        @Override public File getItem() {
            return txtF.getValue();
        }
        @Override public void refreshItem() {
            txtF.setValue(config.getValue());
        }
    }
}