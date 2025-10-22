package net.atif.buildnotes.gui.widget.list;

import net.atif.buildnotes.data.Build;
import net.atif.buildnotes.gui.screen.MainScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.EntryListWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

public class BuildListWidget extends AbstractListWidget<BuildListWidget.BuildEntry> {

    public BuildListWidget(MainScreen parent, MinecraftClient client, int width, int height, int top, int bottom, int itemHeight) {
        super(parent, client, top, bottom, itemHeight);
    }

    public void setBuilds(List<Build> builds) {
        this.clearEntries();
        builds.forEach(build -> this.addEntry(new BuildEntry(build)));
    }

    public BuildEntry getPublicEntry(int index) {
        return this.getEntry(index);
    }

    public Build getSelectedBuild() {
        BuildEntry entry = getSelectedOrNull();
        return entry != null ? entry.getBuild() : null;
    }

    @Override
    public void setSelected(BuildEntry entry) {
        super.setSelected(entry);
        parentScreen.onBuildSelected(); // Notify the parent screen
    }

    public class BuildEntry extends EntryListWidget.Entry<BuildEntry> {
        private final Build build;

        public BuildEntry(Build build) {
            this.build = build;
        }

        public Build getBuild() {
            return this.build;
        }

        @Override
        public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            client.textRenderer.draw(matrices, build.getName(), x + 2, y + 2, 0xFFFFFF);
            Text coords = new LiteralText("Coords: " + build.getCoordinates()).formatted(Formatting.GRAY);
            client.textRenderer.draw(matrices, coords, x + 2, y + 14, 0xCCCCCC);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button == 0) {
                BuildListWidget.this.setSelected(this);

                BuildListWidget.this.handleEntryClick(this);
                return true;
            }
            return false;
        }
    }
}