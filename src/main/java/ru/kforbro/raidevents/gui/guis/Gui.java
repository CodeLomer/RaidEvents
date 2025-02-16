/**
 * MIT License
 * <p>
 * Copyright (c) 2021 TriumphTeam
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package ru.kforbro.raidevents.gui.guis;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import ru.kforbro.raidevents.gui.builder.gui.*;
import ru.kforbro.raidevents.gui.components.GuiContainer;
import ru.kforbro.raidevents.gui.components.GuiType;
import ru.kforbro.raidevents.gui.components.InteractionModifier;
import ru.kforbro.raidevents.gui.components.ScrollType;

import java.util.Set;

/**
 * Standard GUI implementation of {@link BaseGui}
 */
public class Gui extends BaseGui {

    public Gui(final @NotNull GuiContainer guiContainer, final @NotNull Set<InteractionModifier> interactionModifiers) {
        super(guiContainer, interactionModifiers);
    }

    /***
     * @param type The {@link GuiType} to be used
     * @return A {@link TypedGuiBuilder}
     * @since 3.0.0
     */
    @Contract("_ -> new")
    public static @NotNull TypedGuiBuilder gui(final @NotNull GuiType type) {
        return new TypedGuiBuilder(type);
    }

    /**
     * Creates a {@link ChestGuiBuilder} with CHEST as the {@link GuiType}
     *
     * @return A CHEST {@link ChestGuiBuilder}
     * @since 3.0.0
     */
    @Contract(" -> new")
    public static @NotNull ChestGuiBuilder gui() {
        return new ChestGuiBuilder();
    }

    /**
     * Creates a {@link StorageBuilder}.
     *
     * @return A CHEST {@link StorageBuilder}.
     * @since 3.0.0.
     */
    @Contract(" -> new")
    public static @NotNull StorageBuilder storage() {
        return new StorageBuilder();
    }

    /**
     *
     * @return A {@link PaginatedBuilder}
     * @since 3.0.0
     */
    @Contract(" -> new")
    public static @NotNull PaginatedBuilder paginated() {
        return new PaginatedBuilder();
    }

    /**
     *
     * @param scrollType The {@link ScrollType} to be used by the GUI
     * @return A {@link ScrollingBuilder}
     * @since 3.0.0
     */
    @Contract("_ -> new")
    public static @NotNull ScrollingBuilder scrolling(@NotNull final ScrollType scrollType) {
        return new ScrollingBuilder(scrollType);
    }

    /**
     * Creates a {@link ScrollingBuilder} with VERTICAL as the {@link ScrollType}
     *
     * @return A vertical {@link ChestGuiBuilder}
     * @since 3.0.0
     */
    @Contract(" -> new")
    public static @NotNull ScrollingBuilder scrolling() {
        return scrolling(ScrollType.VERTICAL);
    }
}
