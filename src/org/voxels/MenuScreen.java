/**
 * this file is part of voxels
 * 
 * voxels is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * voxels is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with voxels.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.voxels;

import java.util.ArrayList;

import org.lwjgl.opengl.GL11;

/** @author jacob */
public abstract class MenuScreen
{
    protected static abstract class MenuItem
    {
        public float getX()
        {
            return Main.aspectRatio;
        }

        private float y;

        public float getY()
        {
            return this.y;
        }

        public float getWidth()
        {
            return 2.0f * Main.aspectRatio;
        }

        private float height;

        public float getHeight()
        {
            return this.height;
        }

        public void setY(float y)
        {
            this.y = y;
        }

        public final MenuScreen container;

        public MenuItem(float height, MenuScreen container)
        {
            this.height = height;
            this.y = -1.0f;
            this.container = container;
        }

        /** draw this menu item<BR/>
         * NOTE : <code>rs</code> is already translated by &lt0,
         * <code>getY()</code>, 0&gt;
         * 
         * @param tform
         *            the transform
         * @param isSelected
         *            if this menu item is selected */
        public abstract void draw(Matrix tform, boolean isSelected);

        public abstract void onClick(float mouseX, float mouseY);

        public abstract void onMouseOver(float mouseX, float mouseY);

        @Override
        public boolean equals(Object obj)
        {
            return this == obj;
        }

        public void select()
        {
            this.container.setSelected(this);
        }
    }

    protected static abstract class TextMenuItem extends MenuItem
    {
        private String text;
        private Color textColor, backgroundColor, selectedTextColor,
                selectedBackgroundColor;
        private TextureAtlas.TextureHandle texture;
        private TextureAtlas.TextureHandle selectedTexture;

        public Color getTextColor()
        {
            return new Color(this.textColor);
        }

        public void setTextColor(Color textColor)
        {
            this.textColor = new Color(textColor);
        }

        public Color getBackgroundColor()
        {
            return new Color(this.backgroundColor);
        }

        public void setBackgroundColor(Color backgroundColor)
        {
            this.backgroundColor = new Color(backgroundColor);
        }

        public Color getSelectedTextColor()
        {
            return new Color(this.selectedTextColor);
        }

        public void setSelectedTextColor(Color selectedTextColor)
        {
            this.selectedTextColor = new Color(selectedTextColor);
        }

        public Color getSelectedBackgroundColor()
        {
            return new Color(this.selectedBackgroundColor);
        }

        public void setSelectedBackgroundColor(Color selectedBackgroundColor)
        {
            this.selectedBackgroundColor = new Color(selectedBackgroundColor);
        }

        public String getText()
        {
            return this.text;
        }

        protected final float textScale = Text.sizeH("A") * 2.0f * 2.0f / 480.0f;

        public void setText(String text)
        {
            this.text = text;
        }

        public TextMenuItem(String text,
                            Color textColor,
                            Color backgroundColor,
                            Color selectedTextColor,
                            Color selectedBackgroundColor,
                            MenuScreen container)
        {
            super(Math.max(Text.sizeH("A"), Text.sizeH(text)) * 2.0f * 2.0f / 480.0f,
                  container);
            this.text = text;
            this.textColor = new Color(textColor);
            this.backgroundColor = new Color(backgroundColor);
            this.selectedTextColor = new Color(selectedTextColor);
            this.selectedBackgroundColor = new Color(selectedBackgroundColor);
            this.texture = RenderingStream.Polygon.NO_TEXTURE;
            this.selectedTexture = RenderingStream.Polygon.NO_TEXTURE;
        }

        public TextMenuItem(String text,
                            Color textColor,
                            TextureAtlas.TextureHandle texture,
                            Color selectedTextColor,
                            TextureAtlas.TextureHandle selectedTexture,
                            MenuScreen container)
        {
            super(Math.max(Text.sizeH("A"), Text.sizeH(text)) * 2.0f * 2.0f / 480.0f,
                  container);
            this.text = text;
            this.textColor = new Color(textColor);
            this.backgroundColor = Color.RGB(1.0f, 1.0f, 1.0f);
            this.selectedTextColor = new Color(selectedTextColor);
            this.selectedBackgroundColor = Color.RGB(1.0f, 1.0f, 1.0f);
            this.texture = texture;
            this.selectedTexture = selectedTexture;
        }

        protected void drawBackground(Matrix tform, boolean isSelected)
        {
            Color backgroundColor = this.backgroundColor;
            TextureAtlas.TextureHandle texture = this.texture;
            if(isSelected)
            {
                backgroundColor = this.selectedBackgroundColor;
                texture = this.selectedTexture;
            }
            RenderingStream rs = new RenderingStream();
            rs.setMatrix(tform);
            rs.concatMatrix(Matrix.scale(1.0f, getHeight(), 1.0f));
            rs.setMatrix(rs.getMatrix().concat(Matrix.scale(1.1f)));
            rs.addRect(-1.0f,
                       0,
                       1.0f,
                       1.0f,
                       0,
                       0,
                       1,
                       1,
                       backgroundColor,
                       texture);
            rs.render();
        }

        @Override
        public void draw(Matrix tform, boolean isSelected)
        {
            drawBackground(tform, isSelected);
            Color textColor = this.textColor;
            if(isSelected)
                textColor = this.selectedTextColor;
            Matrix mat = Matrix.scale(this.textScale, this.textScale, 1.0f)
                               .concat(Matrix.translate(-this.textScale
                                                                / 2f
                                                                * Text.sizeW(this.text)
                                                                / Text.sizeW("A"),
                                                        this.textScale
                                                                * Text.sizeH(this.text)
                                                                / Text.sizeH("A")
                                                                - this.textScale,
                                                        0))
                               .concat(tform);
            Text.draw(mat, textColor, this.text);
        }
    }

    protected static abstract class CheckMenuItem extends TextMenuItem
    {
        public abstract boolean isChecked();

        public abstract void setChecked(boolean checked);

        public CheckMenuItem(String text,
                             Color textColor,
                             Color backgroundColor,
                             Color selectedTextColor,
                             Color selectedBackgroundColor,
                             MenuScreen container)
        {
            super(text,
                  textColor,
                  backgroundColor,
                  selectedTextColor,
                  selectedBackgroundColor,
                  container);
        }

        public CheckMenuItem(String text,
                             Color textColor,
                             TextureAtlas.TextureHandle texture,
                             Color selectedTextColor,
                             TextureAtlas.TextureHandle selectedTexture,
                             MenuScreen container)
        {
            super(text,
                  textColor,
                  texture,
                  selectedTextColor,
                  selectedTexture,
                  container);
        }

        @Override
        public void draw(Matrix tform, boolean isSelected)
        {
            String tempText = getText();
            setText((isChecked() ? "\u221A " : "  ") + tempText);
            super.draw(tform, isSelected);
            setText(tempText);
        }

        @Override
        public void onClick(float mouseX, float mouseY)
        {
            setChecked(!isChecked());
        }

        @Override
        public void onMouseOver(float mouseX, float mouseY)
        {
            select();
        }
    }

    protected static class SpacerMenuItem extends MenuItem
    {
        private final Color color;

        public SpacerMenuItem(Color color, MenuScreen container)
        {
            super(Text.sizeH("A") * 2.0f * 2.0f / 480.0f, container);
            this.color = color;
        }

        @Override
        public void draw(Matrix tform, boolean isSelected)
        {
            Image.unselectTexture();
            GL11.glColor4f(Color.GetRValue(this.color) / 255.0f,
                           Color.GetGValue(this.color) / 255.0f,
                           Color.GetBValue(this.color) / 255.0f,
                           0.0f);
            GL11.glBegin(GL11.GL_LINES);
            Vector.glVertex(tform.apply(new Vector(-1.0f, 0.5f * getHeight(), 0)));
            Vector.glVertex(tform.apply(new Vector(1.0f, 0.5f * getHeight(), 0)));
            GL11.glEnd();
        }

        @Override
        public void onClick(float mouseX, float mouseY)
        {
        }

        @Override
        public void onMouseOver(float mouseX, float mouseY)
        {
        }
    }

    protected static abstract class OptionMenuItem extends TextMenuItem
    {
        public abstract boolean isPicked();

        public abstract void pick();

        public OptionMenuItem(String text,
                              Color textColor,
                              Color backgroundColor,
                              Color selectedTextColor,
                              Color selectedBackgroundColor,
                              MenuScreen container)
        {
            super(text,
                  textColor,
                  backgroundColor,
                  selectedTextColor,
                  selectedBackgroundColor,
                  container);
        }

        public OptionMenuItem(String text,
                              Color textColor,
                              TextureAtlas.TextureHandle texture,
                              Color selectedTextColor,
                              TextureAtlas.TextureHandle selectedTexture,
                              MenuScreen container)
        {
            super(text,
                  textColor,
                  texture,
                  selectedTextColor,
                  selectedTexture,
                  container);
        }

        @Override
        public void draw(Matrix tform, boolean isSelected)
        {
            String tempText = getText();
            setText((isPicked() ? "\u2022 " : "  ") + tempText);
            super.draw(tform, isSelected);
            setText(tempText);
        }

        @Override
        public void onClick(float mouseX, float mouseY)
        {
            pick();
        }

        @Override
        public void onMouseOver(float mouseX, float mouseY)
        {
            select();
        }
    }

    private ArrayList<MenuItem> items = new ArrayList<MenuItem>();
    private float startY;
    private float totalHeight;

    protected void calcTotalHeight()
    {
        this.totalHeight = 0;
        for(MenuItem item : this.items)
            this.totalHeight += item.getHeight();
    }

    protected void calcLayout()
    {
        calcTotalHeight();
        if(this.totalHeight >= 2.0f)
        {
            this.startY = Math.min(1.0f, this.startY);
            this.startY = Math.max(1.0f - this.totalHeight, this.startY);
        }
        else
            this.startY = this.totalHeight / 2.0f;
        float y = this.startY;
        for(MenuItem item : this.items)
        {
            y -= item.getHeight();
            item.setY(y);
        }
    }

    protected void add(MenuItem item)
    {
        this.items.add(item);
        calcLayout();
    }

    protected void scroll(float amount)
    {
        this.startY += amount;
        calcLayout();
    }

    private int selected = -1;

    protected void setSelected(int selected)
    {
        if(selected < 0 || selected >= this.items.size())
            this.selected = -1;
        else
        {
            this.selected = selected;
            MenuItem item = getSelectedMenuItem();
            float y = item.getY();
            float height = item.getHeight();
            if(y < -1.0f)
                scroll(-1.0f - y);
            else if(y + height > 1.0f)
                scroll(y + height - 1.0f);
        }
    }

    protected void setSelected(MenuItem item)
    {
        setSelected(this.items.indexOf(item));
    }

    protected int getMenuItemCount()
    {
        return this.items.size();
    }

    protected int getSelected()
    {
        return this.selected;
    }

    protected MenuItem getSelectedMenuItem()
    {
        if(this.selected < 0 || this.selected >= this.items.size())
            return null;
        return this.items.get(this.selected);
    }

    protected void nextSelected()
    {
        if(getSelectedMenuItem() == null)
        {
            setSelected(0);
            return;
        }
        int index = getSelected();
        if(index >= getMenuItemCount() - 1)
            index = 0;
        else
            index = index + 1;
        setSelected(index);
    }

    protected void previousSelected()
    {
        if(getSelectedMenuItem() == null)
        {
            setSelected(getMenuItemCount() - 1);
            return;
        }
        int index = getSelected();
        if(index <= 0)
            index = getMenuItemCount() - 1;
        else
            index = index - 1;
        setSelected(index);
    }

    private Color backgroundColor;

    protected Color getBackgroundColor()
    {
        return this.backgroundColor;
    }

    protected void setBackgroundColor(Color backgroundColor)
    {
        this.backgroundColor = new Color(backgroundColor);
    }

    /** @param backgroundColor
     *            the background color */
    public MenuScreen(Color backgroundColor)
    {
        this.backgroundColor = backgroundColor;
    }

    protected void drawBackground(@SuppressWarnings("unused") Matrix tform)
    {
    }

    /**
     * 
     */
    public void draw()
    {
        Color.glClearColor(this.backgroundColor);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        drawBackground(Matrix.translate(0, 0, -1.0f));
        for(int i = 0; i < this.items.size(); i++)
        {
            MenuItem item = this.items.get(i);
            item.draw(Matrix.translate(0, item.getY(), -1.0f),
                      i == this.selected);
        }
    }

    /** @param mouseX
     *            mouse x coordinate
     * @param mouseY
     *            mouse y coordinate */
    public void onClick(float mouseX, float mouseY)
    {
        for(MenuItem item : this.items)
        {
            float y = item.getY();
            float height = item.getHeight();
            if(mouseY >= y && mouseY < y + height)
            {
                item.onClick(mouseX, mouseY - y);
                return;
            }
        }
    }

    /** @param mouseX
     *            mouse x coordinate
     * @param mouseY
     *            mouse y coordinate */
    public void onMouseOver(float mouseX, float mouseY)
    {
        for(MenuItem item : this.items)
        {
            float y = item.getY();
            float height = item.getHeight();
            if(mouseY >= y && mouseY < y + height)
            {
                item.onMouseOver(mouseX, mouseY - y);
                break;
            }
        }
        if(Math.abs(mouseY) > 0.95f)
        {
            float sign = 1;
            if(mouseY < 0)
                sign = -1;
            scroll(sign * 0.5f * (float)Main.getFrameDuration());
        }
    }

    private boolean done = false;

    /** @return if this menu is done */
    public boolean isDone()
    {
        return this.done;
    }

    /** close this menu (make <code>isDone</code> return true)
     * 
     * @see #isDone() */
    public void close()
    {
        this.done = true;
    }
}
