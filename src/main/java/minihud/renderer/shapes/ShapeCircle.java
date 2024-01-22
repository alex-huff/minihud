package minihud.renderer.shapes;

import java.util.HashSet;
import java.util.List;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import net.minecraft.entity.Entity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import malilib.util.MathUtils;
import malilib.util.StringUtils;
import malilib.util.data.json.JsonUtils;
import malilib.util.game.wrap.EntityWrap;
import minihud.config.Configs;
import minihud.util.value.ShapeRenderType;

public class ShapeCircle extends ShapeCircleBase
{
    protected int height = 1;

    public ShapeCircle()
    {
        super(ShapeType.CIRCLE, Configs.Colors.SHAPE_CIRCLE.getColor(), 16);
    }

    @Override
    public void update(Vec3d cameraPos, Entity entity)
    {
        this.renderCircleShape(cameraPos);
        this.onPostUpdate(EntityWrap.getEntityPos(entity));
    }

    public int getHeight()
    {
        return this.height;
    }

    public void setHeight(int height)
    {
        this.height = MathUtils.clamp(height, 1, 260);
        this.setNeedsUpdate();
    }

    protected void renderCircleShape(Vec3d cameraPos)
    {
        BlockPos posCenter = this.getCenterBlock();
        BlockPos.MutableBlockPos posMutable = new BlockPos.MutableBlockPos();
        HashSet<BlockPos> circlePositions = new HashSet<>();

        EnumFacing.Axis axis = this.mainAxis.getAxis();

        for (int i = 0; i < this.height; ++i)
        {
            posMutable.setPos(  posCenter.getX() + this.mainAxis.getXOffset() * i,
                                posCenter.getY() + this.mainAxis.getYOffset() * i,
                                posCenter.getZ() + this.mainAxis.getZOffset() * i);

            if (axis == EnumFacing.Axis.Y)
            {
                this.addPositionsOnHorizontalRing(circlePositions, posMutable, EnumFacing.NORTH);
            }
            else
            {
                this.addPositionsOnVerticalRing(circlePositions, posMutable, EnumFacing.UP, this.mainAxis);
            }
        }

        EnumFacing mainAxis = this.mainAxis;
        EnumFacing[] sides = FACING_ALL;

        // Exclude the two sides on the main axis
        if (this.renderType != ShapeRenderType.FULL_BLOCK)
        {
            sides = new EnumFacing[4];

            for (int i = 0, index = 0; i < 6; ++i)
            {
                EnumFacing side = FACING_ALL[i];

                if (side.getAxis() != mainAxis.getAxis())
                {
                    sides[index++] = side;
                }
            }
        }

        this.startBuffers();
        this.renderPositions(circlePositions, sides, mainAxis, this.color, cameraPos);
        this.uploadBuffers();
    }

    @Override
    protected boolean isPositionOnOrInsideRing(int blockX, int blockY, int blockZ, EnumFacing outSide, EnumFacing mainAxis)
    {
        EnumFacing.Axis axis = mainAxis.getAxis();

        double x = axis == EnumFacing.Axis.X ? this.effectiveCenter.x : (double) blockX + 0.5;
        double y = axis == EnumFacing.Axis.Y ? this.effectiveCenter.y : (double) blockY + 0.5;
        double z = axis == EnumFacing.Axis.Z ? this.effectiveCenter.z : (double) blockZ + 0.5;
        double dist = this.effectiveCenter.squareDistanceTo(x, y, z);
        double diff = this.radiusSq - dist;

        if (diff > 0)
        {
            return true;
        }

        double xAdj = axis == EnumFacing.Axis.X ? this.effectiveCenter.x : (double) blockX + outSide.getXOffset() + 0.5;
        double yAdj = axis == EnumFacing.Axis.Y ? this.effectiveCenter.y : (double) blockY + outSide.getYOffset() + 0.5;
        double zAdj = axis == EnumFacing.Axis.Z ? this.effectiveCenter.z : (double) blockZ + outSide.getZOffset() + 0.5;
        double distAdj = this.effectiveCenter.squareDistanceTo(xAdj, yAdj, zAdj);
        double diffAdj = this.radiusSq - distAdj;

        return diffAdj > 0 && Math.abs(diff) < Math.abs(diffAdj);
    }

    @Override
    public List<String> getWidgetHoverLines()
    {
        List<String> lines = super.getWidgetHoverLines();

        String axis = org.apache.commons.lang3.StringUtils.capitalize(this.getMainAxis().toString().toLowerCase());
        lines.add(2, StringUtils.translate("minihud.hover.shape.height", this.getHeight()));
        lines.add(3, StringUtils.translate("minihud.hover.shape.circle.main_axis", axis));

        return lines;
    }

    @Override
    public JsonObject toJson()
    {
        JsonObject obj = super.toJson();

        obj.add("height", new JsonPrimitive(this.height));

        return obj;
    }

    @Override
    public void fromJson(JsonObject obj)
    {
        super.fromJson(obj);

        this.setHeight(JsonUtils.getInteger(obj, "height"));
    }
}
