package net.atif.buildnotes.gui.helper;

import com.mojang.blaze3d.systems.RenderSystem;
import net.atif.buildnotes.Buildnotes;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.Stack;

public class ScissorStack {

    private static final Stack<Rect> stack = new Stack<>();

    private record Rect(int x, int y, int width, int height) {}

    /**
     * Pushes a new scissor rectangle onto the stack.
     * The new rectangle is intersected with the current one to create a nested effect.
     * Coordinates are transformed by the provided MatrixStack to get absolute screen positions.
     *
     * @param x The local X coordinate of the scissor box.
     * @param y The local Y coordinate of the scissor box.
     * @param width The width of the scissor box.
     * @param height The height of the scissor box.
     * @param matrices The current MatrixStack used for rendering the element.
     */

    public static void push(int x, int y, int width, int height, MatrixStack matrices) {
        // Get the correct Matrix4f type from the MatrixStack
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        // Use JOML's Vector4f for transformations
        Vector4f corner1 = new Vector4f(x, y, 0, 1);
        matrix.transform(corner1);

        Vector4f corner2 = new Vector4f(x + width, y + height, 0, 1);
        matrix.transform(corner2);

        int transformedX = (int) Math.min(corner1.x, corner2.x);
        int transformedY = (int) Math.min(corner1.y, corner2.y);
        int transformedWidth = (int) (Math.max(corner1.x, corner2.x) - transformedX);
        int transformedHeight = (int) (Math.max(corner1.y, corner2.y) - transformedY);

        Rect newRect = new Rect(transformedX, transformedY, transformedWidth, transformedHeight);

        // If a scissor is already active, intersect the new one with it
        if (!stack.isEmpty()) {
            Rect parent = stack.peek();
            newRect = intersect(parent, newRect);
        }

        stack.push(newRect);
        applyScissor(newRect);
    }

    /**
     * Pops the current scissor rectangle from the stack and restores the previous one.
     */
    public static void pop() {
        if (stack.isEmpty()) {
            Buildnotes.LOGGER.warn("ScissorStack pop attempted on an empty stack.");
            return;
        }

        stack.pop();

        if (stack.isEmpty()) {
            RenderSystem.disableScissor();
        } else {
            applyScissor(stack.peek());
        }
    }

    private static Rect intersect(Rect r1, Rect r2) {
        int x = Math.max(r1.x, r2.x);
        int y = Math.max(r1.y, r2.y);
        int width = Math.min(r1.x + r1.width, r2.x + r2.width) - x;
        int height = Math.min(r1.y + r1.height, r2.y + r2.height) - y;

        return new Rect(x, y, Math.max(0, width), Math.max(0, height));
    }

    private static void applyScissor(Rect rect) {
        double scale = MinecraftClient.getInstance().getWindow().getScaleFactor();
        // OpenGL's Y is from the bottom-left, so we must convert our top-left Y coordinate
        int windowHeight = MinecraftClient.getInstance().getWindow().getFramebufferHeight();
        int scissorY = (int)(windowHeight - (rect.y + rect.height) * scale);

        RenderSystem.enableScissor(
                (int)(rect.x * scale),
                scissorY,
                (int)(rect.width * scale),
                (int)(rect.height * scale)
        );
    }
}