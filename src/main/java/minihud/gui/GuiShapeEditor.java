package minihud.gui;

import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import malilib.config.option.OptionListConfig;
import malilib.config.value.BlockSnap;
import malilib.gui.BaseScreen;
import malilib.gui.config.BaseConfigScreen;
import malilib.gui.edit.BaseLayerRangeEditScreen;
import malilib.gui.icon.DefaultIcons;
import malilib.gui.listener.DoubleModifierButtonListener;
import malilib.gui.listener.DoubleTextFieldListener;
import malilib.gui.listener.IntegerModifierButtonListener;
import malilib.gui.listener.IntegerTextFieldListener;
import malilib.gui.widget.BaseTextFieldWidget;
import malilib.gui.widget.ColorIndicatorWidget;
import malilib.gui.widget.DoubleTextFieldWidget;
import malilib.gui.widget.IntegerTextFieldWidget;
import malilib.gui.widget.LabelWidget;
import malilib.gui.widget.Vec3dEditWidget;
import malilib.gui.widget.button.GenericButton;
import malilib.gui.widget.button.OptionListConfigButton;
import malilib.input.ActionResult;
import malilib.util.ListUtils;
import malilib.util.data.DualDoubleConsumer;
import malilib.util.data.DualIntConsumer;
import malilib.util.position.Direction;
import minihud.Reference;
import minihud.renderer.shapes.ShapeBase;
import minihud.renderer.shapes.ShapeCircle;
import minihud.renderer.shapes.ShapeCircleBase;
import minihud.renderer.shapes.ShapeManager;
import minihud.renderer.shapes.ShapeSpawnSphere;
import minihud.util.value.ShapeRenderType;

public class GuiShapeEditor extends BaseLayerRangeEditScreen
{
    private final ShapeBase shape;
    private final OptionListConfig<BlockSnap> configBlockSnap;

    public GuiShapeEditor(ShapeBase shape)
    {
        super("minihud_shape_editor", ConfigScreen.ALL_TABS, ConfigScreen.SHAPES, shape.getLayerRange());

        this.shape = shape;
        this.configBlockSnap = new OptionListConfig<>("blockSnap", BlockSnap.NONE, BlockSnap.VALUES, "");

        this.setTitle("minihud.title.screen.shape_editor", Reference.MOD_VERSION);
    }

    @Override
    protected void initScreen()
    {
        super.initScreen();

        int x = 10;
        int y = 20;

        this.createShapeEditorElements(x, y);

        GenericButton button = GenericButton.create(ConfigScreen.SHAPES.getDisplayName());
        button.setPosition(x, this.height - 24);
        button.setActionListener(() -> {
            BaseConfigScreen.setCurrentTab(Reference.MOD_ID, ConfigScreen.SHAPES);
            BaseScreen.openScreen(new ShapeManagerScreen());
        });
        this.addWidget(button);
    }

    @Override
    protected void updateWidgetPositions()
    {
        super.updateWidgetPositions();

        this.editWidget.setPosition(this.x + 142, this.y + 142);
    }

    private void createColorInput(int x, int y)
    {
        LabelWidget label = new LabelWidget("minihud.label.shapes.color");
        label.setPosition(x, y + 1);
        this.addWidget(label);
        y += 12;

        BaseTextFieldWidget txtField = new BaseTextFieldWidget(70, 16, String.format("#%08X", this.shape.getColor().intValue));
        txtField.setPosition(x, y);
        txtField.setTextValidator(BaseTextFieldWidget.VALIDATOR_HEX_COLOR_8_6_4_3);
        txtField.setListener(this.shape::setColorFromString);
        this.addWidget(txtField);

        ColorIndicatorWidget ci = new ColorIndicatorWidget(18, 18, this.shape.getColor().intValue, this.shape::setColor);
        ci.setPosition(x + 74, y - 1);
        this.addWidget(ci);
    }

    private void createShapeEditorElements(int x, int y)
    {
        LabelWidget label = new LabelWidget("minihud.label.shapes.display_name");
        label.setPosition(x, y + 1);
        this.addWidget(label);
        y += 12;

        BaseTextFieldWidget textField = new BaseTextFieldWidget(240, 16, this.shape.getDisplayName());
        textField.setPosition(x, y);
        textField.setListener(this.shape::setDisplayName);
        this.addWidget(textField);
        y += 20;

        int renderTypeX = x + 230;
        int renderTypeY = y + 2;

        switch (this.shape.getType())
        {
            case CAN_DESPAWN_SPHERE:
            case CAN_SPAWN_SPHERE:
            case DESPAWN_SPHERE:
            {
                ShapeSpawnSphere shape = (ShapeSpawnSphere) this.shape;
                this.createShapeEditorElementsSphereBase(x, y, false);
                this.createShapeEditorElementDoubleField(x + 150, y + 2, shape::getMargin, shape::setMargin, "minihud.label.shapes.margin", false);
                break;
            }

            case CIRCLE:
            {
                ShapeCircle shape = (ShapeCircle) this.shape;
                this.createShapeEditorElementsSphereBase(x, y, true);
                this.createShapeEditorElementIntField(x + 120, y + 38, shape::getHeight, shape::setHeight, "minihud.label.shapes.height", true);
                this.createDirectionButton(x + 230, y + 36, shape::getMainAxis, shape::setMainAxis, "minihud.button.shapes.circle.main_axis");
                this.createRenderTypeButton(renderTypeX, renderTypeY, this.shape::getRenderType, this.shape::setRenderType, "minihud.button.shapes.render_type");
                break;
            }

            case SPHERE_BLOCKY:
                this.createShapeEditorElementsSphereBase(x, y, true);
                this.createRenderTypeButton(renderTypeX, renderTypeY, this.shape::getRenderType, this.shape::setRenderType, "minihud.button.shapes.render_type");
                break;
        }
    }

    private void createShapeEditorElementsSphereBase(int x, int y, boolean addRadiusInput)
    {
        ShapeCircleBase shape = (ShapeCircleBase) this.shape;

        LabelWidget label = new LabelWidget("minihud.label.shapes.center");
        label.setPosition(x, y + 2);
        this.addWidget(label);

        Vec3dEditWidget editWidget = new Vec3dEditWidget(120, 72, 2, true, shape.getCenter(), shape::setCenter);
        editWidget.setPosition(x, y + 12);
        this.addWidget(editWidget);

        if (addRadiusInput)
        {
            this.createShapeEditorElementDoubleField(editWidget.getRight(), y + 2, shape::getRadius, shape::setRadius, "minihud.label.shapes.radius", true);
        }

        x += 11;
        y += 66;

        this.configBlockSnap.setValue(shape.getBlockSnap());

        OptionListConfigButton buttonSnap = new OptionListConfigButton(-1, 20, this.configBlockSnap, "minihud.button.shapes.block_snap");
        buttonSnap.setPosition(editWidget.getRight() + 12, y);
        buttonSnap.setChangeListener(this::onBlockSnapChanged);
        this.addWidget(buttonSnap);

        y += 24;

        this.createColorInput(x, y);
    }

    protected void onBlockSnapChanged()
    {
        ((ShapeCircleBase) this.shape).setBlockSnap(this.configBlockSnap.getValue());
        this.initScreen();
    }

    private void createShapeEditorElementDoubleField(int x, int y, DoubleSupplier supplier,
                                                     DoubleConsumer consumer, String translationKey, boolean addButton)
    {
        LabelWidget label = new LabelWidget(translationKey);
        label.setPosition(x + 12, y);
        this.addWidget(label);
        y += 10;

        DoubleTextFieldWidget txtField = new DoubleTextFieldWidget(40, 16, supplier.getAsDouble());
        txtField.setPosition(x + 12, y);
        txtField.setListener(new DoubleTextFieldListener(consumer));
        txtField.setUpdateListenerAlways(true);
        this.addWidget(txtField);

        if (addButton)
        {
            GenericButton button = GenericButton.create(DefaultIcons.BTN_PLUSMINUS_16);
            button.setPosition(x + 54, y);
            button.setActionListener(new DoubleModifierButtonListener(supplier, new DualDoubleConsumer(consumer, (val) -> txtField.setText(String.valueOf(supplier.getAsDouble())) )));
            button.setCanScrollToClick(true);
            button.translateAndAddHoverString("malilib.gui.button.hover.plus_minus_tip");
            this.addWidget(button);
        }
    }

    private void createShapeEditorElementIntField(int x, int y, IntSupplier supplier, IntConsumer consumer,
                                                  String translationKey, boolean addButton)
    {
        LabelWidget label = new LabelWidget(translationKey);
        label.setPosition(x + 12, y);
        this.addWidget(label);
        y += 10;

        IntegerTextFieldWidget txtField = new IntegerTextFieldWidget(40, 16, supplier.getAsInt());
        txtField.setPosition(x + 12, y);
        txtField.setListener(new IntegerTextFieldListener(consumer));
        txtField.setUpdateListenerAlways(true);
        this.addWidget(txtField);

        if (addButton)
        {
            GenericButton button = GenericButton.create(DefaultIcons.BTN_PLUSMINUS_16);
            button.setPosition(x + 54, y);
            button.setActionListener(new IntegerModifierButtonListener(supplier, new DualIntConsumer(consumer, (val) -> txtField.setText(String.valueOf(supplier.getAsInt())) )));
            button.setCanScrollToClick(true);
            button.translateAndAddHoverString("malilib.gui.button.hover.plus_minus_tip");
            this.addWidget(button);
        }
    }

    private void createDirectionButton(int x, int y, Supplier<Direction> supplier,
                                       Consumer<Direction> consumer, String translationKey)
    {
        LabelWidget label = new LabelWidget(translationKey);
        label.setPosition(x, y);
        this.addWidget(label);
        y += 10;

        String name = org.apache.commons.lang3.StringUtils.capitalize(supplier.get().toString().toLowerCase());
        GenericButton button = GenericButton.create(50, 20, name);
        button.setActionListener((btn) -> { consumer.accept(supplier.get().cycle(btn == 1)); this.initGui(); return true; });
        button.setPosition(x, y);

        this.addWidget(button);
    }

    private void createRenderTypeButton(int x, int y, Supplier<ShapeRenderType> supplier,
                                        Consumer<ShapeRenderType> consumer, String translationKey)
    {
        LabelWidget label = new LabelWidget(translationKey);
        label.setPosition(x, y);
        this.addWidget(label);
        y += 10;

        GenericButton button = GenericButton.create(supplier.get().getDisplayName());
        button.setActionListener((btn) -> { consumer.accept(ListUtils.getNextEntry(ShapeRenderType.VALUES, supplier.get(), btn != 0)); this.initGui(); return true; });
        button.setPosition(x, y);
        this.addWidget(button);
    }

    public static ActionResult openShapeEditor()
    {
        ShapeBase shape = ShapeManager.INSTANCE.getSelectedShape();
        BaseScreen screen = shape != null ? new GuiShapeEditor(shape) : ShapeManagerScreen.openShapeManagerScreen();
        BaseScreen.openScreen(screen);
        return ActionResult.SUCCESS;
    }
}
