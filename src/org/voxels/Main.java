/**
 * this file is part of Voxels
 * 
 * Voxels is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Voxels is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Voxels.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.voxels;

import static org.voxels.PlayerList.players;
import static org.voxels.World.world;

import java.io.*;

import org.voxels.generate.Rand;
import org.voxels.platform.*;

/** @author jacob */
public final class Main
{
    /**
     * 
     */
    public static Platform platform = PlatformImplementation.makePlatform();
    /**
     * 
     */
    public static OpenGL opengl = platform.getOpenGL();
    /***/
    public static Mouse mouse = platform.getMouse();
    /***/
    public static Keyboard keyboard = platform.getKeyboard();

    /** @return the screen x resolution */
    public static int ScreenXRes()
    {
        return platform.getScreenWidth();
    }

    /** @return the screen y resolution */
    public static int ScreenYRes()
    {
        return platform.getScreenHeight();
    }

    /** @return the aspect ratio */
    public static float aspectRatio()
    {
        return (float)ScreenXRes() / ScreenYRes();
    }

    /** the program's version */
    public static final String Version = "0.3.1";
    /** true if this program is running as a server */
    public static boolean isServer = false;
    /** true if this program is the debug version */
    public static boolean DEBUG = false;
    /** if running in fancy graphics mode instead of fast graphics mode */
    public static boolean FancyGraphics = false;
    static boolean lastEventWasLeftButtonDown = false;

    /** @author jacob */
    public static final class MouseEvent implements Allocatable
    {
        private static final Allocator<MouseEvent> allocator = new Allocator<Main.MouseEvent>()
        {
            @SuppressWarnings("synthetic-access")
            @Override
            protected MouseEvent allocateInternal()
            {
                return new MouseEvent();
            }
        };
        /** the mouse x coordinate */
        public float mouseX;
        /** the mouse y coordinate */
        public float mouseY;
        /** the button this event is about */
        public int button;
        /** the amount that the mouse wheel was moved */
        public int dWheel;
        /** true if the button is pressed */
        public boolean isDown;
        /** left mouse button */
        public static final int LEFT = Mouse.BUTTON_LEFT;
        /** right mouse button */
        public static final int RIGHT = Mouse.BUTTON_RIGHT;
        /** middle mouse button */
        public static final int MIDDLE = Mouse.BUTTON_MIDDLE;

        private MouseEvent()
        {
        }

        public static MouseEvent allocate()
        {
            MouseEvent retval = allocator.allocate();
            retval.mouseX = mouse.getEventX();
            retval.mouseY = mouse.getEventY();
            retval.button = mouse.getEventButtonIndex();
            retval.isDown = mouse.isEventButtonDown();
            retval.dWheel = mouse.getEventDWheel();
            return retval;
        }

        @Override
        public MouseEvent dup()
        {
            MouseEvent retval = allocator.allocate();
            retval.mouseX = this.mouseX;
            retval.mouseY = this.mouseY;
            retval.button = this.button;
            retval.isDown = this.isDown;
            retval.dWheel = this.dWheel;
            return retval;
        }

        @Override
        public void free()
        {
            allocator.free(this);
        }
    }

    private static final StringBuilder frameText = new StringBuilder();

    /** adds a string to the current frame's overlaid text
     * 
     * @param str
     *            the string to add */
    public static void addToFrameText(final String str)
    {
        frameText.append(str);
    }

    private static float fps = 30.0f;
    private static Matrix renderFrame_t1 = Matrix.allocate();

    private static void renderFrame()
    {
        players.draw();
        opengl.glClear(opengl.GL_DEPTH_BUFFER_BIT());
        final float dist = 480f / Text.sizeH("A") / 2.0f;
        Text.draw(Matrix.setToTranslate(renderFrame_t1,
                                        -dist * aspectRatio(),
                                        dist - 1.0f,
                                        -dist),
                  Color.RGB(1.0f, 1.0f, 1.0f),
                  frameText.toString());
        frameText.setLength(0);
        if(DEBUG)
        {
            String fpsStr = "?";
            if(getFrameDuration() > 0)
            {
                fps = fps * (1.0f - (float)getFrameDuration()) + 1.0f;
                fpsStr = "0000000" + Integer.toString((int)(fps * 100.0f));
                fpsStr = fpsStr.substring(0, fpsStr.length() - 2) + "."
                        + fpsStr.substring(fpsStr.length() - 2);
                while(fpsStr.charAt(0) == '0' && fpsStr.charAt(1) != '.')
                {
                    fpsStr = fpsStr.substring(1);
                }
            }
            addToFrameText("Voxels " + Version + "\n" + "FPS : " + fpsStr
                    + "\n");
        }
        else
        {
            addToFrameText("Voxels " + Version + "\n");
        }
    }

    private static void setFullscreen(final boolean isFullscreen)
    {
        if(platform.isFullscreen() && !isFullscreen || !platform.isFullscreen()
                && isFullscreen)
        {
            platform.setFullscreen(isFullscreen);
        }
        opengl.glEnable(opengl.GL_DEPTH_TEST());
        final double minGLdist = 1e-2;
        opengl.glEnable(opengl.GL_TEXTURE_2D());
        opengl.glEnable(opengl.GL_ALPHA_TEST());
        opengl.glEnable(opengl.GL_CULL_FACE());
        opengl.glEnable(opengl.GL_BLEND());
        opengl.glCullFace(opengl.GL_BACK());
        opengl.glFrontFace(opengl.GL_CCW());
        opengl.glAlphaFunc(opengl.GL_LESS(), 0.95f);
        opengl.glBlendFunc(opengl.GL_ONE_MINUS_SRC_ALPHA(),
                           opengl.GL_SRC_ALPHA());
        opengl.glTexEnvi(opengl.GL_TEXTURE_ENV(),
                         opengl.GL_TEXTURE_ENV_MODE(),
                         opengl.GL_MODULATE());
        opengl.glHint(opengl.GL_PERSPECTIVE_CORRECTION_HINT(),
                      opengl.GL_NICEST());
        opengl.glViewport(0, 0, ScreenXRes(), ScreenYRes());
        opengl.glMatrixMode(opengl.GL_PROJECTION());
        opengl.glLoadIdentity();
        opengl.glFrustum(-minGLdist * aspectRatio(),
                         minGLdist * aspectRatio(),
                         -minGLdist,
                         minGLdist,
                         minGLdist,
                         150f);
    }

    /** @return the current time in seconds */
    public static double Timer()
    {
        return platform.Timer();
    }

    private static double frameDuration = 0.05;

    /** @return the duration in seconds of the last frame */
    public static double getFrameDuration()
    {
        return frameDuration;
    }

    /** the A key */
    public static final int KEY_A = Keyboard.KEY_A;
    /** the B key */
    public static final int KEY_B = Keyboard.KEY_B;
    /** the C key */
    public static final int KEY_C = Keyboard.KEY_C;
    /** the D key */
    public static final int KEY_D = Keyboard.KEY_D;
    /** the E key */
    public static final int KEY_E = Keyboard.KEY_E;
    /** the F key */
    public static final int KEY_F = Keyboard.KEY_F;
    /** the G key */
    public static final int KEY_G = Keyboard.KEY_G;
    /** the H key */
    public static final int KEY_H = Keyboard.KEY_H;
    /** the I key */
    public static final int KEY_I = Keyboard.KEY_I;
    /** the J key */
    public static final int KEY_J = Keyboard.KEY_J;
    /** the K key */
    public static final int KEY_K = Keyboard.KEY_K;
    /** the L key */
    public static final int KEY_L = Keyboard.KEY_L;
    /** the M key */
    public static final int KEY_M = Keyboard.KEY_M;
    /** the N key */
    public static final int KEY_N = Keyboard.KEY_N;
    /** the O key */
    public static final int KEY_O = Keyboard.KEY_O;
    /** the P key */
    public static final int KEY_P = Keyboard.KEY_P;
    /** the Q key */
    public static final int KEY_Q = Keyboard.KEY_Q;
    /** the R key */
    public static final int KEY_R = Keyboard.KEY_R;
    /** the S key */
    public static final int KEY_S = Keyboard.KEY_S;
    /** the T key */
    public static final int KEY_T = Keyboard.KEY_T;
    /** the U key */
    public static final int KEY_U = Keyboard.KEY_U;
    /** the V key */
    public static final int KEY_V = Keyboard.KEY_V;
    /** the W key */
    public static final int KEY_W = Keyboard.KEY_W;
    /** the X key */
    public static final int KEY_X = Keyboard.KEY_X;
    /** the Y key */
    public static final int KEY_Y = Keyboard.KEY_Y;
    /** the Z key */
    public static final int KEY_Z = Keyboard.KEY_Z;
    /** the F1 key */
    public static final int KEY_F1 = Keyboard.KEY_F1;
    /** the F2 key */
    public static final int KEY_F2 = Keyboard.KEY_F2;
    /** the F3 key */
    public static final int KEY_F3 = Keyboard.KEY_F3;
    /** the F4 key */
    public static final int KEY_F4 = Keyboard.KEY_F4;
    /** the F5 key */
    public static final int KEY_F5 = Keyboard.KEY_F5;
    /** the F6 key */
    public static final int KEY_F6 = Keyboard.KEY_F6;
    /** the F7 key */
    public static final int KEY_F7 = Keyboard.KEY_F7;
    /** the F8 key */
    public static final int KEY_F8 = Keyboard.KEY_F8;
    /** the F9 key */
    public static final int KEY_F9 = Keyboard.KEY_F9;
    /** the F10 key */
    public static final int KEY_F10 = Keyboard.KEY_F10;
    /** the F11 key */
    public static final int KEY_F11 = Keyboard.KEY_F11;
    /** the F12 key */
    public static final int KEY_F12 = Keyboard.KEY_F12;
    /** the 0 key */
    public static final int KEY_0 = Keyboard.KEY_0;
    /** the 1 key */
    public static final int KEY_1 = Keyboard.KEY_1;
    /** the 2 key */
    public static final int KEY_2 = Keyboard.KEY_2;
    /** the 3 key */
    public static final int KEY_3 = Keyboard.KEY_3;
    /** the 4 key */
    public static final int KEY_4 = Keyboard.KEY_4;
    /** the 5 key */
    public static final int KEY_5 = Keyboard.KEY_5;
    /** the 6 key */
    public static final int KEY_6 = Keyboard.KEY_6;
    /** the 7 key */
    public static final int KEY_7 = Keyboard.KEY_7;
    /** the 8 key */
    public static final int KEY_8 = Keyboard.KEY_8;
    /** the 9 key */
    public static final int KEY_9 = Keyboard.KEY_9;
    /** the Apostrophe key */
    public static final int KEY_APOSTROPHE = Keyboard.KEY_APOSTROPHE;
    /** the up arrow key */
    public static final int KEY_UP = Keyboard.KEY_UP;
    /** the down arrow key */
    public static final int KEY_DOWN = Keyboard.KEY_DOWN;
    /** the left arrow key */
    public static final int KEY_LEFT = Keyboard.KEY_LEFT;
    /** the right arrow key */
    public static final int KEY_RIGHT = Keyboard.KEY_RIGHT;
    /** the left shift key */
    public static final int KEY_LSHIFT = Keyboard.KEY_LSHIFT;
    /** the right shift key */
    public static final int KEY_RSHIFT = Keyboard.KEY_RSHIFT;
    /** the left control key */
    public static final int KEY_LCTRL = Keyboard.KEY_LCTRL;
    /** the right control key */
    public static final int KEY_RCTRL = Keyboard.KEY_RCTRL;
    /** the left alt key */
    public static final int KEY_LALT = Keyboard.KEY_LALT;
    /** the right alt key */
    public static final int KEY_RALT = Keyboard.KEY_RALT;
    /** the return (enter) key */
    public static final int KEY_RETURN = Keyboard.KEY_RETURN;
    /** the left or right shift key key */
    public static final int KEY_SHIFT = Keyboard.KEY_SHIFT;
    /** the left or right Control key */
    public static final int KEY_CTRL = Keyboard.KEY_CTRL;
    /** the left or right Alt key */
    public static final int KEY_ALT = Keyboard.KEY_ALT;
    /** the special value that means that no key corresponds to this character */
    public static final int KEY_NONE = Keyboard.KEY_NONE;
    /** the Escape key */
    public static final int KEY_ESCAPE = Keyboard.KEY_ESCAPE;
    /** the Space key */
    public static final int KEY_SPACE = Keyboard.KEY_SPACE;
    /** the Delete key */
    public static final int KEY_DELETE = Keyboard.KEY_DELETE;
    /** the special value that means that no character was translated */
    public static final char CHAR_NONE = Keyboard.CHAR_NONE;

    /** @param key
     *            the key to check for
     * @return true if the key is pressed */
    public static boolean isKeyDown(final int key)
    {
        if(keyboard == null)
            return false;
        return keyboard.isKeyDown(key);
    }

    /** @author jacob */
    public static final class KeyboardEvent implements Allocatable
    {
        private static final Allocator<KeyboardEvent> allocator = new Allocator<Main.KeyboardEvent>()
        {
            @SuppressWarnings("synthetic-access")
            @Override
            protected KeyboardEvent allocateInternal()
            {
                return new KeyboardEvent();
            }
        };

        private KeyboardEvent()
        {
        }

        /**
		 * 
		 */
        public int key;
        /**
		 * 
		 */
        public char character;
        /** if the key is pressed */
        public boolean isDown;
        /** if this key press event is a repeat */
        public boolean isRepeat;

        private KeyboardEvent init()
        {
            if(keyboard != null)
            {
                this.key = keyboard.getEventKey();
                this.character = keyboard.getEventCharacter();
                this.isDown = keyboard.getEventKeyDown();
                this.isRepeat = keyboard.isRepeatEvent();
            }
            else
            {
                this.key = KEY_NONE;
                this.character = CHAR_NONE;
                this.isDown = false;
                this.isRepeat = false;
            }
            return this;
        }

        private KeyboardEvent init(final int key,
                                   final char character,
                                   final boolean isDown,
                                   final boolean isRepeat)
        {
            this.key = key;
            this.character = character;
            this.isDown = isDown;
            this.isRepeat = isRepeat;
            return this;
        }

        public static KeyboardEvent allocate()
        {
            return allocator.allocate().init();
        }

        public static KeyboardEvent allocate(final int key,
                                             final char character,
                                             final boolean isDown,
                                             final boolean isRepeat)
        {
            return allocator.allocate().init(key, character, isDown, isRepeat);
        }

        @Override
        public KeyboardEvent dup()
        {
            return allocate(this.key,
                            this.character,
                            this.isDown,
                            this.isRepeat);
        }

        @Override
        public void free()
        {
            allocator.free(this);
        }
    }

    private static Rand.Settings makeLandGeneratorSettings()
    {
        Rand.SettingsCreatorMenu menu = new Rand.SettingsCreatorMenu();
        runMenu(menu);
        return menu.settings;
    }

    private static void generateGame()
    {
        saveFile = null;
        World.clear();
        world.setLandGeneratorSettings(makeLandGeneratorSettings(), false);
        players.clear();
        players.addDefaultPlayer();
        didLoad = true;
    }

    static boolean isFullscreen = false;
    private static double curTime;
    private static double lastFrameStartTime;
    /**
     * 
     */
    public static boolean isCreativeMode = false;

    /** @param menu
     *            the menu to run */
    public static void runMenu(final MenuScreen menu)
    {
        platform.setMouseVisible(true);
        stopAllSound();
        boolean done = false;
        while(!done)
        {
            updateBackgroundMusic();
            setFullscreen(isFullscreen);
            menu.draw();
            platform.update();
            {
                while(mouse.nextEvent())
                {
                    MouseEvent event = MouseEvent.allocate();
                    if(event.isDown)
                        menu.onClick(event.mouseX / ScreenXRes() * 2f - 1f, 1f
                                - event.mouseY / ScreenYRes() * 2f);
                    event.free();
                }
                if(keyboard != null)
                {
                    while(keyboard.nextEvent())
                    {
                        KeyboardEvent event = KeyboardEvent.allocate();
                        if(event.isDown && event.key == KEY_F4
                                && isKeyDown(KEY_ALT))
                        {
                            done = true;
                            event.free();
                            continue;
                        }
                        if(event.isDown && event.key == KEY_F11)
                        {
                            isFullscreen = !isFullscreen;
                            event.free();
                            continue;
                        }
                        event.free();
                    }
                }
                float mouseX = mouse.getX();
                float mouseY = mouse.getY();
                menu.onMouseOver(mouseX / ScreenXRes() * 2f - 1f, 1f - mouseY
                        / ScreenYRes() * 2f);
            }
            platform.waitForNextFrame();
            curTime = Timer();
            frameDuration = curTime - lastFrameStartTime;
            internalSaveAll();
            lastFrameStartTime = curTime;
            if(menu.isDone())
                done = true;
            if(platform.isCloseRequested())
                done = true;
        }
        updateBackgroundMusic();
        while(true)
            if(!mouse.nextEvent())
                break;
    }

    /** @param title
     *            the title
     * @param message
     *            the message */
    public static void alert(final String title, final String message)
    {
        runMenu(new MenuScreen(Color.V(0.75f))
        {
            @Override
            protected void drawBackground(final Matrix tform)
            {
                drawTextBackground(title, tform);
            }

            {
                add(new TextMenuItem(message,
                                     Color.RGB(0f, 0f, 0f),
                                     getBackgroundColor(),
                                     Color.RGB(0f, 0f, 0f),
                                     Color.RGB(0.0f, 0.0f, 1.0f),
                                     this)
                {
                    @Override
                    public void onMouseOver(final float mouseX,
                                            final float mouseY)
                    {
                    }

                    @Override
                    public void onClick(final float mouseX, final float mouseY)
                    {
                    }
                });
                add(new SpacerMenuItem(Color.V(0.0f), this));
                MenuItem okButton = new TextMenuItem("OK",
                                                     Color.RGB(0f, 0f, 0f),
                                                     getBackgroundColor(),
                                                     Color.RGB(0f, 0f, 0f),
                                                     Color.RGB(0.0f, 0.0f, 1.0f),
                                                     this)
                {
                    @Override
                    public void onMouseOver(final float mouseX,
                                            final float mouseY)
                    {
                        select();
                    }

                    @Override
                    public void onClick(final float mouseX, final float mouseY)
                    {
                        this.container.close();
                    }
                };
                add(okButton);
                okButton.select();
            }
        });
    }

    /** @param title
     *            the title
     * @param message
     *            the message
     * @param defaultValue
     *            the initial selection
     * @return if yes is clicked */
    public static boolean query(final String title,
                                final String message,
                                final boolean defaultValue)
    {
        final boolean[] retval = new boolean[]
        {
            defaultValue
        };
        runMenu(new MenuScreen(Color.V(0.75f))
        {
            @Override
            protected void drawBackground(final Matrix tform)
            {
                drawTextBackground(title, tform);
            }

            {
                add(new TextMenuItem(message,
                                     Color.RGB(0f, 0f, 0f),
                                     getBackgroundColor(),
                                     Color.RGB(0f, 0f, 0f),
                                     Color.RGB(0.0f, 0.0f, 1.0f),
                                     this)
                {
                    @Override
                    public void onMouseOver(final float mouseX,
                                            final float mouseY)
                    {
                    }

                    @Override
                    public void onClick(final float mouseX, final float mouseY)
                    {
                    }
                });
                add(new SpacerMenuItem(Color.V(0.0f), this));
                MenuItem yesButton = new TextMenuItem("Yes",
                                                      Color.RGB(0f, 0f, 0f),
                                                      getBackgroundColor(),
                                                      Color.RGB(0f, 0f, 0f),
                                                      Color.RGB(0.0f,
                                                                0.0f,
                                                                1.0f), this)
                {
                    @Override
                    public void onMouseOver(final float mouseX,
                                            final float mouseY)
                    {
                        select();
                    }

                    @Override
                    public void onClick(final float mouseX, final float mouseY)
                    {
                        this.container.close();
                        retval[0] = true;
                    }
                };
                add(yesButton);
                MenuItem noButton = new TextMenuItem("No",
                                                     Color.RGB(0f, 0f, 0f),
                                                     getBackgroundColor(),
                                                     Color.RGB(0f, 0f, 0f),
                                                     Color.RGB(0.0f, 0.0f, 1.0f),
                                                     this)
                {
                    @Override
                    public void onMouseOver(final float mouseX,
                                            final float mouseY)
                    {
                        select();
                    }

                    @Override
                    public void onClick(final float mouseX, final float mouseY)
                    {
                        this.container.close();
                        retval[0] = false;
                    }
                };
                add(noButton);
                if(defaultValue)
                    yesButton.select();
                else
                    noButton.select();
            }
        });
        return retval[0];
    }

    private static class FileLoadSaveMenuScreen extends MenuScreen
    {
        private static final int FileSlotCount = 10;
        public int curFile = -1;
        public final boolean isSave;

        private static File fileFromIndex(final int index)
        {
            if(index < 0)
                return null;
            return new File(Main.platform.getUserSettingsDir(), "VoxelsGame"
                    + Integer.toString(index) + ".vw");
        }

        public static boolean slotTaken(final int index)
        {
            File f = fileFromIndex(index);
            if(f == null)
                return false;
            if(!f.exists())
                return false;
            return true;
        }

        private class FileMenuItem extends TextMenuItem
        {
            private final int fileIndex;

            public FileMenuItem(final int fileIndex, final MenuScreen container)
            {
                super("# "
                              + Integer.toString(fileIndex)
                              + " "
                              + (slotTaken(fileIndex) ? " -- Game Saved"
                                      : " --   Empty   "),
                      Color.RGB(0f, 0f, 0f),
                      container.getBackgroundColor(),
                      Color.RGB(0f, 0f, 0f),
                      Color.RGB(0.0f, 0.0f, 1.0f),
                      container);
                this.fileIndex = fileIndex;
            }

            @Override
            public void onClick(final float mouseX, final float mouseY)
            {
                if(FileLoadSaveMenuScreen.this.isSave
                        && slotTaken(this.fileIndex))
                {
                    if(query("Replace", "Replace the saved game?", false))
                    {
                        this.container.close();
                    }
                }
                else
                    this.container.close();
                FileLoadSaveMenuScreen.this.curFile = this.fileIndex;
            }

            @Override
            public void onMouseOver(final float mouseX, final float mouseY)
            {
                select();
            }
        }

        private void addFile(final int index)
        {
            add(new FileMenuItem(index, this));
        }

        private FileLoadSaveMenuScreen(final boolean isSave)
        {
            super(Color.V(0.75f));
            this.isSave = isSave;
            if(isSave)
            {
                for(int i = 0; i < FileSlotCount; i++)
                    addFile(i);
            }
            else
            {
                for(int i = 0; i < FileSlotCount; i++)
                {
                    if(slotTaken(i))
                        addFile(i);
                }
            }
            add(new SpacerMenuItem(Color.V(0.0f), this));
            add(new TextMenuItem("Cancel",
                                 Color.RGB(0f, 0f, 0f),
                                 getBackgroundColor(),
                                 Color.RGB(0f, 0f, 0f),
                                 Color.RGB(0.0f, 0.0f, 1.0f),
                                 this)
            {
                @Override
                public void onMouseOver(final float mouseX, final float mouseY)
                {
                    select();
                }

                @Override
                public void onClick(final float mouseX, final float mouseY)
                {
                    this.container.close();
                    FileLoadSaveMenuScreen.this.curFile = -1;
                }
            });
        }

        public static FileLoadSaveMenuScreen SaveMenuScreen()
        {
            return new FileLoadSaveMenuScreen(true);
        }

        public static FileLoadSaveMenuScreen LoadMenuScreen()
        {
            return new FileLoadSaveMenuScreen(false);
        }

        public File getSelectedFile()
        {
            return fileFromIndex(this.curFile);
        }

        @Override
        protected void drawBackground(final Matrix tform)
        {
            super.drawTextBackground(this.isSave ? "Save" : "Load", tform);
        }
    }

    private static File runFileSave()
    {
        FileLoadSaveMenuScreen dlg = FileLoadSaveMenuScreen.SaveMenuScreen();
        runMenu(dlg);
        return dlg.getSelectedFile();
    }

    private static File runFileLoad()
    {
        FileLoadSaveMenuScreen dlg = FileLoadSaveMenuScreen.LoadMenuScreen();
        runMenu(dlg);
        return dlg.getSelectedFile();
    }

    private static void runScreenResolutionMenu()
    {
        MenuScreen menu = platform.getChangeScreenResolutionMenu();
        if(menu == null)
            return;
        runMenu(menu);
    }

    /**
     * 
     */
    public static boolean isVSyncEnabled = true;

    private static void runBackgroundMusicMenu()
    {
        runMenu(new MenuScreen(Color.V(0.75f))
        {
            @Override
            protected void drawBackground(final Matrix tform)
            {
                drawTextBackground("Background Music", tform);
            }

            {
                for(int i = 0; i < backgroundAudioCount; i++)
                {
                    final int index = i;
                    add(new OptionMenuItem("Song #" + (index + 1),
                                           Color.RGB(0f, 0f, 0f),
                                           getBackgroundColor(),
                                           Color.RGB(0f, 0f, 0f),
                                           Color.RGB(0.0f, 0.0f, 1.0f),
                                           this)
                    {
                        @Override
                        public void pick()
                        {
                            Main.runBackgroundMusicIndex = index;
                        }

                        @Override
                        public boolean isPicked()
                        {
                            return Main.runBackgroundMusicIndex == index;
                        }
                    });
                }
                add(new OptionMenuItem("No Background Music",
                                       Color.RGB(0f, 0f, 0f),
                                       getBackgroundColor(),
                                       Color.RGB(0f, 0f, 0f),
                                       Color.RGB(0.0f, 0.0f, 1.0f),
                                       this)
                {
                    @Override
                    public void pick()
                    {
                        Main.runBackgroundMusicIndex = -1;
                    }

                    @Override
                    public boolean isPicked()
                    {
                        return Main.runBackgroundMusicIndex == -1;
                    }
                });
                add(new SpacerMenuItem(Color.V(0), this));
                add(new TextMenuItem("Return To Options",
                                     Color.RGB(0.0f, 0.0f, 0.0f),
                                     getBackgroundColor(),
                                     Color.RGB(0.0f, 0.0f, 0.0f),
                                     Color.RGB(0.0f, 0.0f, 1.0f),
                                     this)
                {
                    @Override
                    public void onMouseOver(final float mouseX,
                                            final float mouseY)
                    {
                        select();
                    }

                    @Override
                    public void onClick(final float mouseX, final float mouseY)
                    {
                        this.container.close();
                    }
                });
            }
        });
    }

    @SuppressWarnings("synthetic-access")
    private static void runOptionsMenu()
    {
        runMenu(new MenuScreen(Color.V(0.75f))
        {
            @Override
            protected void drawBackground(final Matrix tform)
            {
                drawTextBackground("Options", tform);
            }

            {
                add(new CheckMenuItem("Debug Mode",
                                      Color.RGB(0f, 0f, 0f),
                                      getBackgroundColor(),
                                      Color.RGB(0f, 0f, 0f),
                                      Color.RGB(0.0f, 0.0f, 1.0f),
                                      this)
                {
                    @Override
                    public void setChecked(final boolean checked)
                    {
                        Main.DEBUG = checked;
                    }

                    @Override
                    public boolean isChecked()
                    {
                        return Main.DEBUG;
                    }
                });
                // add(new
                // CheckMenuItem("Use Vertex Arrays\nand a Texture Atlas",
                // Color.RGB(0f, 0f, 0f),
                // getBackgroundColor(),
                // Color.RGB(0f, 0f, 0f),
                // Color.RGB(0.0f, 0.0f, 1.0f),
                // this)
                // {
                // @Override
                // public void setChecked(final boolean checked)
                // {
                // RenderingStream.USE_VERTEX_ARRAY_AND_TEXTURE_ATLAS = checked;
                // }
                //
                // @Override
                // public boolean isChecked()
                // {
                // return RenderingStream.USE_VERTEX_ARRAY_AND_TEXTURE_ATLAS;
                // }
                // });
                add(new CheckMenuItem("Creative Mode",
                                      Color.RGB(0f, 0f, 0f),
                                      getBackgroundColor(),
                                      Color.RGB(0f, 0f, 0f),
                                      Color.RGB(0.0f, 0.0f, 1.0f),
                                      this)
                {
                    @Override
                    public void setChecked(final boolean checked)
                    {
                        Main.isCreativeMode = checked;
                    }

                    @Override
                    public boolean isChecked()
                    {
                        return Main.isCreativeMode;
                    }
                });
                add(new CheckMenuItem("Fast Time",
                                      Color.RGB(0f, 0f, 0f),
                                      getBackgroundColor(),
                                      Color.RGB(0f, 0f, 0f),
                                      Color.RGB(0.0f, 0.0f, 1.0f),
                                      this)
                {
                    @Override
                    public void setChecked(final boolean checked)
                    {
                        if(Main.DEBUG)
                            World.useFastTime = checked;
                    }

                    @Override
                    public boolean isChecked()
                    {
                        if(!Main.DEBUG)
                            return false;
                        return World.useFastTime;
                    }
                });
                add(new SpacerMenuItem(Color.V(0), this));
                add(new CheckMenuItem("Fancy Graphics",
                                      Color.RGB(0f, 0f, 0f),
                                      getBackgroundColor(),
                                      Color.RGB(0f, 0f, 0f),
                                      Color.RGB(0.0f, 0.0f, 1.0f),
                                      this)
                {
                    @Override
                    public void setChecked(final boolean checked)
                    {
                        Main.FancyGraphics = checked;
                        world.invalidateLightingArrays();
                    }

                    @Override
                    public boolean isChecked()
                    {
                        return Main.FancyGraphics;
                    }
                });
                add(new SpacerMenuItem(Color.V(0), this));
                add(new OptionMenuItem("Render Distance : Far",
                                       Color.RGB(0f, 0f, 0f),
                                       getBackgroundColor(),
                                       Color.RGB(0f, 0f, 0f),
                                       Color.RGB(0.0f, 0.0f, 1.0f),
                                       this)
                {
                    @Override
                    public void pick()
                    {
                        World.viewDist = 32;
                    }

                    @Override
                    public boolean isPicked()
                    {
                        return World.viewDist == 32;
                    }
                });
                add(new OptionMenuItem("Render Distance : Medium",
                                       Color.RGB(0f, 0f, 0f),
                                       getBackgroundColor(),
                                       Color.RGB(0f, 0f, 0f),
                                       Color.RGB(0.0f, 0.0f, 1.0f),
                                       this)
                {
                    @Override
                    public void pick()
                    {
                        World.viewDist = 16;
                    }

                    @Override
                    public boolean isPicked()
                    {
                        return World.viewDist == 16;
                    }
                });
                add(new OptionMenuItem("Render Distance : Default",
                                       Color.RGB(0f, 0f, 0f),
                                       getBackgroundColor(),
                                       Color.RGB(0f, 0f, 0f),
                                       Color.RGB(0.0f, 0.0f, 1.0f),
                                       this)
                {
                    @Override
                    public void pick()
                    {
                        World.viewDist = 10;
                    }

                    @Override
                    public boolean isPicked()
                    {
                        return World.viewDist == 10;
                    }
                });
                add(new OptionMenuItem("Render Distance : Fast",
                                       Color.RGB(0f, 0f, 0f),
                                       getBackgroundColor(),
                                       Color.RGB(0f, 0f, 0f),
                                       Color.RGB(0.0f, 0.0f, 1.0f),
                                       this)
                {
                    @Override
                    public void pick()
                    {
                        World.viewDist = 8;
                    }

                    @Override
                    public boolean isPicked()
                    {
                        return World.viewDist == 8;
                    }
                });
                add(new SpacerMenuItem(Color.V(0), this));
                if(platform.hasChangeScreenResolutionMenu())
                {
                    add(new TextMenuItem("Change Screen Resolution...",
                                         Color.RGB(0.0f, 0.0f, 0.0f),
                                         getBackgroundColor(),
                                         Color.RGB(0.0f, 0.0f, 0.0f),
                                         Color.RGB(0.0f, 0.0f, 1.0f),
                                         this)
                    {
                        @Override
                        public void onMouseOver(final float mouseX,
                                                final float mouseY)
                        {
                            select();
                        }

                        @Override
                        public void onClick(final float mouseX,
                                            final float mouseY)
                        {
                            runScreenResolutionMenu();
                        }
                    });
                }
                add(new CheckMenuItem("Full Screen",
                                      Color.RGB(0f, 0f, 0f),
                                      getBackgroundColor(),
                                      Color.RGB(0f, 0f, 0f),
                                      Color.RGB(0.0f, 0.0f, 1.0f),
                                      this)
                {
                    @Override
                    public void setChecked(final boolean checked)
                    {
                        Main.isFullscreen = checked;
                    }

                    @Override
                    public boolean isChecked()
                    {
                        return Main.isFullscreen;
                    }
                });
                add(new CheckMenuItem("Use VSync",
                                      Color.RGB(0f, 0f, 0f),
                                      getBackgroundColor(),
                                      Color.RGB(0f, 0f, 0f),
                                      Color.RGB(0.0f, 0.0f, 1.0f),
                                      this)
                {
                    @Override
                    public void setChecked(final boolean checked)
                    {
                        Main.isVSyncEnabled = checked;
                        platform.setVSyncEnabled(Main.isVSyncEnabled);
                    }

                    @Override
                    public boolean isChecked()
                    {
                        return Main.isVSyncEnabled;
                    }
                });
                add(new SpacerMenuItem(Color.V(0), this));
                add(new TextMenuItem("Change Background Music...",
                                     Color.RGB(0.0f, 0.0f, 0.0f),
                                     getBackgroundColor(),
                                     Color.RGB(0.0f, 0.0f, 0.0f),
                                     Color.RGB(0.0f, 0.0f, 1.0f),
                                     this)
                {
                    @Override
                    public void onClick(final float mouseX, final float mouseY)
                    {
                        runBackgroundMusicMenu();
                    }

                    @Override
                    public void onMouseOver(final float mouseX,
                                            final float mouseY)
                    {
                        select();
                    }
                });
                add(new SpacerMenuItem(Color.V(0), this));
                add(new TextMenuItem("Return To Main Menu",
                                     Color.RGB(0.0f, 0.0f, 0.0f),
                                     getBackgroundColor(),
                                     Color.RGB(0.0f, 0.0f, 0.0f),
                                     Color.RGB(0.0f, 0.0f, 1.0f),
                                     this)
                {
                    @Override
                    public void onMouseOver(final float mouseX,
                                            final float mouseY)
                    {
                        select();
                    }

                    @Override
                    public void onClick(final float mouseX, final float mouseY)
                    {
                        this.container.close();
                    }
                });
            }
        });
    }

    @SuppressWarnings("synthetic-access")
    private static void runMainMenu()
    {
        runMenu(new MenuScreen(Color.RGB(0.75f, 0.75f, 0.75f))
        {
            @Override
            protected void drawBackground(final Matrix tform)
            {
                drawTextBackground("Main Menu", tform);
            }

            {
                add(new TextMenuItem("New Game",
                                     Color.RGB(0f, 0f, 0f),
                                     getBackgroundColor(),
                                     Color.RGB(0f, 0f, 0f),
                                     Color.RGB(0.0f, 0.0f, 1.0f),
                                     this)
                {
                    @Override
                    public void onMouseOver(final float mouseX,
                                            final float mouseY)
                    {
                        select();
                    }

                    @Override
                    public void onClick(final float mouseX, final float mouseY)
                    {
                        if(didLoad)
                        {
                            saveAll();
                            internalSaveAll();
                        }
                        generateGame();
                        this.container.close();
                    }
                });
                add(new TextMenuItem("Load Game",
                                     Color.RGB(0f, 0f, 0f),
                                     getBackgroundColor(),
                                     Color.RGB(0f, 0f, 0f),
                                     Color.RGB(0.0f, 0.0f, 1.0f),
                                     this)
                {
                    @Override
                    public void onMouseOver(final float mouseX,
                                            final float mouseY)
                    {
                        select();
                    }

                    @Override
                    public void onClick(final float mouseX, final float mouseY)
                    {
                        if(didLoad)
                        {
                            saveAll();
                            internalSaveAll();
                        }
                        didLoad = false;
                        loadAll();
                        if(didLoad)
                            this.container.close();
                    }
                });
                if(didLoad)
                {
                    add(new TextMenuItem("Save Game",
                                         Color.RGB(0.0f, 0.0f, 0.0f),
                                         getBackgroundColor(),
                                         Color.RGB(0.0f, 0.0f, 0.0f),
                                         Color.RGB(0.0f, 0.0f, 1.0f),
                                         this)
                    {
                        @Override
                        public void onMouseOver(final float mouseX,
                                                final float mouseY)
                        {
                            select();
                        }

                        @Override
                        public void onClick(final float mouseX,
                                            final float mouseY)
                        {
                            saveAll();
                        }
                    });
                    add(new SpacerMenuItem(Color.V(0), this));
                    add(new TextMenuItem("Return To Game",
                                         Color.RGB(0.0f, 0.0f, 0.0f),
                                         getBackgroundColor(),
                                         Color.RGB(0.0f, 0.0f, 0.0f),
                                         Color.RGB(0.0f, 0.0f, 1.0f),
                                         this)
                    {
                        @Override
                        public void onMouseOver(final float mouseX,
                                                final float mouseY)
                        {
                            select();
                        }

                        @Override
                        public void onClick(final float mouseX,
                                            final float mouseY)
                        {
                            this.container.close();
                        }
                    });
                }
                add(new SpacerMenuItem(Color.V(0), this));
                add(new TextMenuItem("Quit Game",
                                     Color.RGB(0.0f, 0.0f, 0.0f),
                                     getBackgroundColor(),
                                     Color.RGB(0.0f, 0.0f, 0.0f),
                                     Color.RGB(0.0f, 0.0f, 1.0f),
                                     this)
                {
                    @Override
                    public void onMouseOver(final float mouseX,
                                            final float mouseY)
                    {
                        select();
                    }

                    @Override
                    public void onClick(final float mouseX, final float mouseY)
                    {
                        if(Main.didLoad)
                        {
                            saveAll();
                            internalSaveAll();
                        }
                        platform.close();
                        System.exit(0);
                    }
                });
                add(new TextMenuItem("Options",
                                     Color.RGB(0.0f, 0.0f, 0.0f),
                                     getBackgroundColor(),
                                     Color.RGB(0.0f, 0.0f, 0.0f),
                                     Color.RGB(0.0f, 0.0f, 1.0f),
                                     this)
                {
                    @Override
                    public void onMouseOver(final float mouseX,
                                            final float mouseY)
                    {
                        select();
                    }

                    @Override
                    public void onClick(final float mouseX, final float mouseY)
                    {
                        runOptionsMenu();
                    }
                });
            }
        });
    }

    public static void runSoundForFrame()
    {
        if(needFuseBurnAudio)
        {
            if(!fuseBurnAudioPlaying)
                playloop(fuseBurnAudio);
            fuseBurnAudioPlaying = true;
        }
        else
        {
            if(fuseBurnAudioPlaying)
                stoploop(fuseBurnAudio);
            fuseBurnAudioPlaying = false;
        }
        needFuseBurnAudio = false;
        if(needFireBurnAudio)
        {
            if(!fireBurnAudioPlaying)
                playloop(fireBurnAudio);
            fireBurnAudioPlaying = true;
        }
        else
        {
            if(fireBurnAudioPlaying)
                stoploop(fireBurnAudio);
            fireBurnAudioPlaying = false;
        }
        needFireBurnAudio = false;
        updateBackgroundMusic();
    }

    public static void updateBackgroundMusic()
    {
        if(backgroundAudioIndex != -1
                && backgroundAudio[backgroundAudioIndex] != null
                && backgroundAudio[backgroundAudioIndex].isPlaying()
                && runBackgroundMusicIndex != backgroundAudioIndex)
            stoploop(backgroundAudio[backgroundAudioIndex]);
        backgroundAudioIndex = runBackgroundMusicIndex;
        if(backgroundAudioIndex != -1
                && backgroundAudio[backgroundAudioIndex] != null
                && !backgroundAudio[backgroundAudioIndex].isPlaying())
            playloop(backgroundAudio[backgroundAudioIndex]);
    }

    public static void stopAllSound()
    {
        if(fuseBurnAudioPlaying)
            stoploop(fuseBurnAudio);
        fuseBurnAudioPlaying = false;
        if(fireBurnAudioPlaying)
            stoploop(fireBurnAudio);
        fireBurnAudioPlaying = false;
    }

    public static int runBackgroundMusicIndex = 0;// TODO finish
    public static boolean needPause = false;

    /** @param args
     *            command line arguments */
    public static void main(final String[] args)
    {
        for(int i = 0; i < args.length; i++)
        {
            if(args[i].equals("--debug"))
                DEBUG = true;
        }
        platform.init();
        isFullscreen = false;
        boolean done = false;
        lastFrameStartTime = Timer();
        curTime = Timer();
        init();
        curTime = Timer();
        if(runBackgroundMusicIndex != -1)
        {
            backgroundAudioIndex = runBackgroundMusicIndex;
            playloop(backgroundAudio[backgroundAudioIndex]);
        }
        runMainMenu();
        if(!didLoad)
        {
            platform.close();
            System.exit(0);
        }
        while(!done)
        {
            if(needPause)
            {
                needPause = false;
                stopAllSound();
                runMainMenu();
            }
            runSoundForFrame();
            setFullscreen(isFullscreen);
            renderFrame();
            platform.update();
            world.generateChunks();
            {
                while(mouse.nextEvent())
                {
                    MouseEvent event = MouseEvent.allocate();
                    players.handleMouseUpDown(event);
                    event.free();
                }
                if(keyboard != null)
                {
                    while(keyboard.nextEvent())
                    {
                        KeyboardEvent event = KeyboardEvent.allocate();
                        if(event.isDown && event.key == KEY_F4
                                && isKeyDown(KEY_ALT))
                        {
                            done = true;
                            event.free();
                            continue;
                        }
                        if(event.isDown && event.key == KEY_F11)
                        {
                            isFullscreen = !isFullscreen;
                            event.free();
                            continue;
                        }
                        if(event.isDown && event.key == KEY_P)
                        {
                            runMainMenu();
                            needPause = false;
                            event.free();
                            continue;
                        }
                        players.handleKeyboardEvent(event);
                        event.free();
                    }
                }
                if(platform.isTouchScreen())
                    players.handleMouseMove(mouse.getX(),
                                            mouse.getY(),
                                            mouse.isButtonDown(Mouse.BUTTON_LEFT));
                else
                {
                    switch(players.handleMouseMove(mouse.getX(),
                                                   mouse.getY(),
                                                   mouse.isButtonDown(Mouse.BUTTON_LEFT)))
                    {
                    case Grabbed:
                        if(platform.isMouseVisible())
                        {
                            float x = mouse.getX(), y = mouse.getY();
                            platform.setMouseVisible(false);
                            mouse.setPosition(x, y);
                        }
                        break;
                    case GrabbedAndCentered:
                        if(platform.isMouseVisible())
                            platform.setMouseVisible(false);
                        mouse.setPosition(ScreenXRes() / 2, ScreenYRes() / 2);
                        break;
                    default:
                        if(!platform.isMouseVisible())
                        {
                            float x = mouse.getX(), y = mouse.getY();
                            platform.setMouseVisible(true);
                            mouse.setPosition(x, y);
                        }
                        break;
                    }
                }
                players.move();
            }
            world.move();
            platform.waitForNextFrame();
            curTime = Timer();
            frameDuration = curTime - lastFrameStartTime;
            internalSaveAll();
            lastFrameStartTime = curTime;
            if(platform.isCloseRequested())
                done = true;
        }
        saveAll(); // signal to save
        internalSaveAll(); // run save
        platform.close();
        System.exit(0);
    }

    static boolean isLoading = false;
    private static File saveFile = null;
    static String progressLabelText = "";
    static float progressLoc = 0;
    private static MenuScreen progressMenu = new MenuScreen(Color.V(0.75f))
    {
        @Override
        protected void drawBackground(final Matrix tform)
        {
            drawTextBackground(progressLabelText, tform);
        }

        {
            add(new ProgressMenuItem(Color.V(0.0f), this)
            {
                @Override
                protected float getPos()
                {
                    return progressLoc;
                }
            });
        }
    };

    private static void showProgressDialog(final String labelText)
    {
        stopAllSound();
        topOfProgressStack = null;
        progressLabelText = labelText;
    }

    private static void hideProgressDialog()
    {
        topOfProgressStack = null;
        progressLoc = 0f;
    }

    private static void internalSetProgress(final float progress)
    {
        progressLoc = Math.max(0, Math.min(1, progress));
        progressMenu.draw();
        platform.update();
    }

    private static class ProgressStackNode
    {
        public float scale, offset;
        public ProgressStackNode next;

        public ProgressStackNode()
        {
        }
    }

    private static ProgressStackNode topOfProgressStack = null;

    private static float getProgressOffset()
    {
        if(topOfProgressStack != null)
            return topOfProgressStack.offset;
        return 0.0f;
    }

    private static float getProgressScale()
    {
        if(topOfProgressStack != null)
            return topOfProgressStack.scale;
        return 1.0f;
    }

    static void pushProgress(final float offset, final float scale)
    {
        ProgressStackNode n = new ProgressStackNode();
        n.next = topOfProgressStack;
        n.scale = scale * getProgressScale();
        n.offset = getProgressOffset() + offset * getProgressScale();
        topOfProgressStack = n;
    }

    private static double lastSetProgressTime = -1;

    static void setProgress(final float progress)
    {
        final double updatePeriod = 0.05f;
        final double curTime = Timer();
        if(curTime - lastSetProgressTime < updatePeriod)
            return;
        lastSetProgressTime = curTime;
        float finalProgress = progress * getProgressScale()
                + getProgressOffset();
        internalSetProgress(Math.max(0, Math.min(1, finalProgress)));
    }

    static void popProgress()
    {
        if(topOfProgressStack != null)
            topOfProgressStack = topOfProgressStack.next;
    }

    private static boolean getSaveFile()
    {
        isLoading = false;
        File newSaveFile = runFileSave();
        if(newSaveFile != null)
        {
            saveFile = newSaveFile;
            return true;
        }
        return false;
    }

    private static boolean needSave = false; // true if saveAll() has been
                                             // called

    /** saves everything */
    public static void saveAll()
    {
        needSave = true;
    }

    private static void internalSaveAll()
    {
        if(!needSave)
            return;
        needSave = false;
        if(saveFile == null)
        {
            if(!getSaveFile())
                return;
        }
        showProgressDialog("Saving...");
        OutputStream fos = null;
        boolean needHide = true;
        try
        {
            fos = new BufferedOutputStream(new FileOutputStream(saveFile));
            DataOutputStream dos = new DataOutputStream(fos);
            World.write(dos);
            players.write(dos);
            dos.close();
            hideProgressDialog();
            needHide = false;
        }
        catch(IOException e)
        {
            hideProgressDialog();
            needHide = false;
            alert("Voxels", "Can't save : " + e.getMessage());
        }
        finally
        {
            if(fos != null)
            {
                try
                {
                    fos.close();
                }
                catch(IOException e)
                {
                    System.err.println(e.getMessage());
                }
            }
            if(needHide)
                hideProgressDialog();
        }
    }

    private static boolean getLoadFile()
    {
        saveFile = null;
        isLoading = true;
        File newSaveFile = runFileLoad();
        if(newSaveFile != null)
        {
            saveFile = newSaveFile;
            return true;
        }
        return false;
    }

    private static void init()
    {
        showProgressDialog("Initializing...");
        Entity.init();
        hideProgressDialog();
    }

    private static boolean didLoad = false;

    private static boolean loadAll()
    {
        boolean done = false;
        boolean retval = false;
        while(!done)
        {
            if(!getLoadFile())
                return false;
            showProgressDialog("Loading...");
            InputStream fis = null;
            boolean needHide = true;
            try
            {
                fis = new BufferedInputStream(new FileInputStream(saveFile));
                DataInputStream dis = new DataInputStream(fis);
                int version = World.read(dis);
                PlayerList.read(dis, version);
                dis.close();
                hideProgressDialog();
                needHide = false;
                done = true;
                didLoad = true;
                retval = true;
            }
            catch(EOFException e)
            {
                hideProgressDialog();
                needHide = false;
                alert("Voxels", "Can't load : unexpected EOF");
            }
            catch(IOException e)
            {
                hideProgressDialog();
                needHide = false;
                alert("Voxels", "Can't load : " + e.getMessage());
            }
            finally
            {
                if(fis != null)
                {
                    try
                    {
                        fis.close();
                    }
                    catch(IOException e)
                    {
                        System.err.println(e.getMessage());
                    }
                }
                if(needHide)
                    hideProgressDialog();
            }
        }
        return retval;
    }

    private Main()
    {
    }

    /** load a audio file
     * 
     * @param filename
     *            the file to load
     * @param isStream
     *            if the file should be loaded as a stream
     * @return the loaded <code>Audio</code> or <code>null</code> */
    @SuppressWarnings("resource")
    public static Audio
        loadAudio(final String filename, final boolean isStream)
    {
        InputStream in = null;
        Audio retval = null;
        try
        {
            if(isStream)
            {
                retval = platform.loadAudioStream(filename);
            }
            else
            {
                in = platform.getFileInputStream(filename);
                retval = platform.loadAudio(in);
            }
        }
        catch(IOException e)
        {
        }
        finally
        {
            if(in != null)
                try
                {
                    in.close();
                }
                catch(IOException e)
                {
                    e.printStackTrace();
                    System.exit(1);
                }
        }
        return retval;
    }

    public static void play(final Audio a)
    {
        if(a != null)
            a.play(1.0f, false);
    }

    public static void playloop(final Audio a)
    {
        if(a != null)
            a.play(1.0f, true);
    }

    public static void stoploop(final Audio a)
    {
        if(a != null)
            a.stop();
    }

    static Audio clickAudio = loadAudio("click.ogg", false);
    static Audio popAudio = loadAudio("pop.ogg", false);
    static Audio destructAudio = loadAudio("destruct.ogg", false);
    static Audio explodeAudio = loadAudio("explode.ogg", false);
    static Audio fuseBurnAudio = loadAudio("fuse.ogg", false);
    static boolean needFuseBurnAudio = false, fuseBurnAudioPlaying = false;
    static Audio fireBurnAudio = loadAudio("fire.ogg", false);
    static boolean needFireBurnAudio = false, fireBurnAudioPlaying = false;
    static final int backgroundAudioCount = 5;
    static final Audio[] backgroundAudio = new Audio[backgroundAudioCount];
    static int backgroundAudioIndex = -1;
    static
    {
        for(int i = 0; i < backgroundAudioCount; i++)
            backgroundAudio[i] = loadAudio(i == 0 ? "background.ogg"
                    : "background" + (i + 1) + ".ogg", true);
    }
}
