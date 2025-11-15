package net.atif.buildnotes.gui.helper;

import net.minecraft.client.gui.DrawContext;
import org.joml.Matrix4f;
import org.joml.Vector4f;

public class ScissorStack {

    /**
     * Pushes a new scissor rectangle onto the stack, respecting the current matrix transformations.
     * It transforms the local coordinates provided into absolute screen-space coordinates before
     * applying the scissor. This allows scissoring to work correctly inside translated matrices.
     *
     * @param context The current DrawContext containing the matrix stack.
     * @param x       The local X coordinate of the scissor box.
     * @param y       The local Y coordinate of the scissor box.
     * @param width   The width of the scissor box.
     * @param height  The height of the scissor box.
     */
    public static void push(DrawContext context, int x, int y, int width, int height) {
        // Get the current transformation matrix from the context
        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();

        // Transform the top-left and bottom-right corners of the local rectangle
        Vector4f corner1 = new Vector4f(x, y, 0, 1);
        matrix.transform(corner1);

        Vector4f corner2 = new Vector4f(x + width, y + height, 0, 1);
        matrix.transform(corner2);

        // Find the min/max of the transformed corners to define the screen-space rectangle
        int transformedX1 = (int) Math.min(corner1.x, corner2.x);
        int transformedY1 = (int) Math.min(corner1.y, corner2.y);
        int transformedX2 = (int) Math.max(corner1.x, corner2.x);
        int transformedY2 = (int) Math.max(corner1.y, corner2.y);

        // enableScissor pushes to DrawContext's internal stack, which handles nesting correctly
        context.enableScissor(transformedX1, transformedY1, transformedX2, transformedY2);
    }

    /**
     * Pops the current scissor rectangle from DrawContext's stack, restoring the previous one.
     * @param context The current DrawContext.
     */
    public static void pop(DrawContext context) {
        context.disableScissor();
    }
}