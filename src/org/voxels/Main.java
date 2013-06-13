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

import static org.lwjgl.opengl.GL11.*;
import static org.voxels.PlayerList.players;
import static org.voxels.World.world;

import java.awt.GridLayout;
import java.io.*;
import java.lang.reflect.InvocationTargetException;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;

import org.lwjgl.LWJGLException;
import org.lwjgl.Sys;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.openal.AL;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.newdawn.slick.openal.Audio;
import org.newdawn.slick.openal.AudioLoader;

/**
 * @author jacob
 * 
 */
public final class Main
{
	/**
	 * the screen x resolution
	 */
	public static int ScreenXRes = 640;
	/**
	 * the screen y resolution
	 */
	public static int ScreenYRes = 480;
	/**
	 * the program's version
	 */
	public static final String Version = "0.2.2";
	/**
	 * true if this program is running as a server
	 */
	public static boolean isServer = false;
	/**
	 * true if this program is the debug version
	 */
	public static boolean DEBUG = false;
	static boolean lastEventWasLeftButtonDown = false;

	/**
	 * @author jacob
	 * 
	 */
	public static class MouseEvent
	{
		/**
		 * the mouse x coordinate
		 */
		public final int mouseX;
		/**
		 * the mouse y coordinate
		 */
		public final int mouseY;
		/**
		 * the button this event is about
		 */
		public final int button;
		/**
		 * the amount that the mouse wheel was moved
		 */
		public final int dWheel;
		/**
		 * true if the button is pressed
		 */
		public final boolean isDown;
		/**
		 * left mouse button
		 */
		public static final int LEFT = 0;
		/**
		 * right mouse button
		 */
		public static final int RIGHT = 1;
		/**
		 * middle mouse button
		 */
		public static final int MIDDLE = 2;

		/**
		 * 
		 */
		public MouseEvent()
		{
			this.mouseX = Mouse.getEventX();
			this.mouseY = Display.getHeight() - Mouse.getEventY() - 1;
			int button = Mouse.getEventButton();
			this.isDown = Mouse.getEventButtonState();
			boolean eventWasLeftButtonDown = false;
			if(button == LEFT && Main.isKeyDown(Main.KEY_ALT)
			        && (!lastEventWasLeftButtonDown || this.isDown))
				button = RIGHT;
			else if(button == LEFT && this.isDown)
				eventWasLeftButtonDown = true;
			lastEventWasLeftButtonDown = eventWasLeftButtonDown;
			this.button = button;
			this.dWheel = Mouse.getEventDWheel();
		}
	}

	private static String frameText = "";

	/**
	 * adds a string to the current frame's overlaid text
	 * 
	 * @param str
	 *            the string to add
	 */
	public static void addToFrameText(String str)
	{
		frameText += str;
	}

	private static float fps = 30.0f;

	private static void renderFrame()
	{
		players.draw();
		glClear(GL_DEPTH_BUFFER_BIT);
		final float dist = ScreenXRes / 22;
		Text.draw(Matrix.translate(-dist, dist - 1.0f, -dist),
		          Color.RGB(1.0f, 1.0f, 1.0f),
		          frameText);
		frameText = "";
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

	private static void setFullscreen(boolean isFullscreen)
	{
		try
		{
			if(Display.isFullscreen() && !isFullscreen
			        || !Display.isFullscreen() && isFullscreen)
			{
				Display.setFullscreen(isFullscreen);
			}
		}
		catch(LWJGLException e)
		{
			e.printStackTrace();
			System.exit(1);
		}
		glEnable(GL_DEPTH_TEST);
		final double minGLdist = 1e-2;
		glEnable(GL_TEXTURE_2D);
		glEnable(GL_ALPHA_TEST);
		glEnable(GL_CULL_FACE);
		glEnable(GL_BLEND);
		glCullFace(GL_BACK);
		glFrontFace(GL_CCW);
		glAlphaFunc(GL_LESS, 0.85f);
		glBlendFunc(GL_ONE_MINUS_SRC_ALPHA, GL_SRC_ALPHA);
		glTexEnvf(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_MODULATE);
		glHint(GL_PERSPECTIVE_CORRECTION_HINT, GL_NICEST);
		glViewport(0, 0, ScreenXRes, ScreenYRes);
		glMatrixMode(GL_PROJECTION);
		glLoadIdentity();
		glFrustum(-minGLdist, minGLdist, -minGLdist, minGLdist, minGLdist, 1e3);
	}

	/**
	 * @return the current time in seconds
	 */
	public static double Timer()
	{
		return (double)Sys.getTime() / Sys.getTimerResolution();
	}

	private static double frameDuration = 0.05;

	/**
	 * @return the duration in seconds of the last frame
	 */
	public static double getFrameDuration()
	{
		return frameDuration;
	}

	/**
	 * the A key
	 */
	public static final int KEY_A = Keyboard.KEY_A;
	/**
	 * the B key
	 */
	public static final int KEY_B = Keyboard.KEY_B;
	/**
	 * the C key
	 */
	public static final int KEY_C = Keyboard.KEY_C;
	/**
	 * the D key
	 */
	public static final int KEY_D = Keyboard.KEY_D;
	/**
	 * the E key
	 */
	public static final int KEY_E = Keyboard.KEY_E;
	/**
	 * the F key
	 */
	public static final int KEY_F = Keyboard.KEY_F;
	/**
	 * the G key
	 */
	public static final int KEY_G = Keyboard.KEY_G;
	/**
	 * the H key
	 */
	public static final int KEY_H = Keyboard.KEY_H;
	/**
	 * the I key
	 */
	public static final int KEY_I = Keyboard.KEY_I;
	/**
	 * the J key
	 */
	public static final int KEY_J = Keyboard.KEY_J;
	/**
	 * the K key
	 */
	public static final int KEY_K = Keyboard.KEY_K;
	/**
	 * the L key
	 */
	public static final int KEY_L = Keyboard.KEY_L;
	/**
	 * the M key
	 */
	public static final int KEY_M = Keyboard.KEY_M;
	/**
	 * the N key
	 */
	public static final int KEY_N = Keyboard.KEY_N;
	/**
	 * the O key
	 */
	public static final int KEY_O = Keyboard.KEY_O;
	/**
	 * the P key
	 */
	public static final int KEY_P = Keyboard.KEY_P;
	/**
	 * the Q key
	 */
	public static final int KEY_Q = Keyboard.KEY_Q;
	/**
	 * the R key
	 */
	public static final int KEY_R = Keyboard.KEY_R;
	/**
	 * the S key
	 */
	public static final int KEY_S = Keyboard.KEY_S;
	/**
	 * the T key
	 */
	public static final int KEY_T = Keyboard.KEY_T;
	/**
	 * the U key
	 */
	public static final int KEY_U = Keyboard.KEY_U;
	/**
	 * the V key
	 */
	public static final int KEY_V = Keyboard.KEY_V;
	/**
	 * the W key
	 */
	public static final int KEY_W = Keyboard.KEY_W;
	/**
	 * the X key
	 */
	public static final int KEY_X = Keyboard.KEY_X;
	/**
	 * the Y key
	 */
	public static final int KEY_Y = Keyboard.KEY_Y;
	/**
	 * the Z key
	 */
	public static final int KEY_Z = Keyboard.KEY_Z;
	/**
	 * the F1 key
	 */
	public static final int KEY_F1 = Keyboard.KEY_F1;
	/**
	 * the F2 key
	 */
	public static final int KEY_F2 = Keyboard.KEY_F2;
	/**
	 * the F3 key
	 */
	public static final int KEY_F3 = Keyboard.KEY_F3;
	/**
	 * the F4 key
	 */
	public static final int KEY_F4 = Keyboard.KEY_F4;
	/**
	 * the F5 key
	 */
	public static final int KEY_F5 = Keyboard.KEY_F5;
	/**
	 * the F6 key
	 */
	public static final int KEY_F6 = Keyboard.KEY_F6;
	/**
	 * the F7 key
	 */
	public static final int KEY_F7 = Keyboard.KEY_F7;
	/**
	 * the F8 key
	 */
	public static final int KEY_F8 = Keyboard.KEY_F8;
	/**
	 * the F9 key
	 */
	public static final int KEY_F9 = Keyboard.KEY_F9;
	/**
	 * the F10 key
	 */
	public static final int KEY_F10 = Keyboard.KEY_F10;
	/**
	 * the F11 key
	 */
	public static final int KEY_F11 = Keyboard.KEY_F11;
	/**
	 * the F12 key
	 */
	public static final int KEY_F12 = Keyboard.KEY_F12;
	/**
	 * the 0 key
	 */
	public static final int KEY_0 = Keyboard.KEY_0;
	/**
	 * the 1 key
	 */
	public static final int KEY_1 = Keyboard.KEY_1;
	/**
	 * the 2 key
	 */
	public static final int KEY_2 = Keyboard.KEY_2;
	/**
	 * the 3 key
	 */
	public static final int KEY_3 = Keyboard.KEY_3;
	/**
	 * the 4 key
	 */
	public static final int KEY_4 = Keyboard.KEY_4;
	/**
	 * the 5 key
	 */
	public static final int KEY_5 = Keyboard.KEY_5;
	/**
	 * the 6 key
	 */
	public static final int KEY_6 = Keyboard.KEY_6;
	/**
	 * the 7 key
	 */
	public static final int KEY_7 = Keyboard.KEY_7;
	/**
	 * the 8 key
	 */
	public static final int KEY_8 = Keyboard.KEY_8;
	/**
	 * the 9 key
	 */
	public static final int KEY_9 = Keyboard.KEY_9;
	/**
	 * the Apostrophe key
	 */
	public static final int KEY_APOSTROPHE = Keyboard.KEY_APOSTROPHE;
	/**
	 * the up arrow key
	 */
	public static final int KEY_UP = Keyboard.KEY_UP;
	/**
	 * the down arrow key
	 */
	public static final int KEY_DOWN = Keyboard.KEY_DOWN;
	/**
	 * the left arrow key
	 */
	public static final int KEY_LEFT = Keyboard.KEY_LEFT;
	/**
	 * the right arrow key
	 */
	public static final int KEY_RIGHT = Keyboard.KEY_RIGHT;
	/**
	 * the left shift key
	 */
	public static final int KEY_LSHIFT = Keyboard.KEY_LSHIFT;
	/**
	 * the right shift key
	 */
	public static final int KEY_RSHIFT = Keyboard.KEY_RSHIFT;
	/**
	 * the left control key
	 */
	public static final int KEY_LCTRL = Keyboard.KEY_LCONTROL;
	/**
	 * the right control key
	 */
	public static final int KEY_RCTRL = Keyboard.KEY_RCONTROL;
	/**
	 * the left alt key
	 */
	public static final int KEY_LALT = Keyboard.KEY_LMENU;
	/**
	 * the right alt key
	 */
	public static final int KEY_RALT = Keyboard.KEY_RMENU;
	/**
	 * the return (enter) key
	 */
	public static final int KEY_RETURN = Keyboard.KEY_RETURN;
	/**
	 * the left or right shift key key
	 */
	public static final int KEY_SHIFT = -10000;
	/**
	 * the left or right Control key
	 */
	public static final int KEY_CTRL = -10001;
	/**
	 * the left or right Alt key
	 */
	public static final int KEY_ALT = -10002;
	/**
	 * the special value that means that no key corresponds to this character
	 */
	public static final int KEY_NONE = Keyboard.KEY_NONE;
	/**
	 * the Escape key
	 */
	public static final int KEY_ESCAPE = Keyboard.KEY_ESCAPE;
	/**
	 * the Space key
	 */
	public static final int KEY_SPACE = Keyboard.KEY_SPACE;
	/**
	 * the Delete key
	 */
	public static final int KEY_DELETE = Keyboard.KEY_DELETE;
	/**
	 * the special value that means that no character was translated
	 */
	public static final char CHAR_NONE = (char)Keyboard.CHAR_NONE;

	/**
	 * @param key
	 *            the key to check for
	 * @return true if the key is pressed
	 */
	public static boolean isKeyDown(int key)
	{
		if(key == KEY_SHIFT)
		{
			return Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)
			        || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
		}
		if(key == KEY_ALT)
		{
			return Keyboard.isKeyDown(Keyboard.KEY_LMENU)
			        || Keyboard.isKeyDown(Keyboard.KEY_RMENU);
		}
		if(key == KEY_CTRL)
		{
			return Keyboard.isKeyDown(Keyboard.KEY_LCONTROL)
			        || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL);
		}
		return Keyboard.isKeyDown(key);
	}

	/**
	 * @author jacob
	 * 
	 */
	public static class KeyboardEvent
	{
		/**
		 * 
		 */
		public final int key;
		/**
		 * 
		 */
		public final char character;
		/**
		 * if the key is pressed
		 */
		public final boolean isDown;
		/**
		 * if this key press event is a repeat
		 */
		public final boolean isRepeat;

		/**
		 * 
		 */
		public KeyboardEvent()
		{
			this.key = Keyboard.getEventKey();
			this.character = Keyboard.getEventCharacter();
			this.isDown = Keyboard.getEventKeyState();
			this.isRepeat = Keyboard.isRepeatEvent();
		}
	}

	private static int getModeDist(DisplayMode mode)
	{
		return Math.abs(mode.getWidth() - 640)
		        + Math.abs(mode.getHeight() - 480);
	}

	private static void generateGame()
	{
		World.clear();
		players.clear();
		players.addDefaultPlayer();
		if(DEBUG)
		{
			for(int i = 0; i < 10; i++)
			{
				players.front().giveBlock(BlockType.BTChest);
				players.front().giveBlock(BlockType.BTFurnace);
				players.front().giveBlock(BlockType.BTSapling);
				players.front().giveBlock(BlockType.BTTorch);
				players.front().giveBlock(BlockType.BTWorkbench);
				players.front().giveBlock(BlockType.BTLadder);
				players.front().giveBlock(BlockType.BTLadder);
				players.front().giveBlock(BlockType.BTLadder);
				players.front().giveBlock(BlockType.BTLadder);
				players.front().giveBlock(BlockType.BTLadder);
				players.front().giveBlock(BlockType.BTLadder);
				players.front().giveBlock(BlockType.BTRedstoneDustOff);
				players.front().giveBlock(BlockType.BTRedstoneDustOff);
				players.front().giveBlock(BlockType.BTRedstoneDustOff);
				players.front().giveBlock(BlockType.BTRedstoneDustOff);
				players.front().giveBlock(BlockType.BTRedstoneDustOff);
				players.front().giveBlock(BlockType.BTRedstoneTorchOff);
				players.front().giveBlock(BlockType.BTLever);
				players.front().giveBlock(BlockType.BTLava);
				players.front().giveBlock(BlockType.BTStone);
				players.front().giveBlock(BlockType.BTRedstoneRepeaterOff);
				players.front().giveBlock(BlockType.BTStickyPiston);
				players.front().giveBlock(BlockType.BTPiston);
				players.front().giveBlock(BlockType.BTSand);
				players.front().giveBlock(BlockType.BTGunpowder);
				players.front().giveBlock(BlockType.BTTNT);
				players.front().giveBlock(BlockType.BTTNT);
				players.front().giveBlock(BlockType.BTTNT);
				players.front().giveBlock(BlockType.BTTNT);
				players.front().giveBlock(BlockType.BTTNT);
				players.front().giveBlock(BlockType.BTTNT);
				players.front().giveBlock(BlockType.BTTNT);
				players.front().giveBlock(BlockType.BTTNT);
				players.front().giveBlock(BlockType.BTTNT);
				players.front().giveBlock(BlockType.BTBlazeRod);
				players.front().giveBlock(BlockType.BTDiamondPick);
				players.front().giveBlock(BlockType.BTGoldPick);
			}
		}
	}

	/**
	 * @param args
	 *            command line arguments
	 */
	public static void main(String[] args)
	{
		for(int i = 0; i < args.length; i++)
		{
			if(args[i].equals("--debug"))
				DEBUG = true;
		}
		init();
		loadAll();
		if(!didLoad)
			generateGame();
		try
		{
			DisplayMode[] modes = Display.getAvailableDisplayModes();
			int closestindex = -1;
			for(int i = 0; i < modes.length; i++)
			{
				if(closestindex == -1)
					closestindex = 0;
				else if(getModeDist(modes[closestindex]) > getModeDist(modes[i]))
					closestindex = i;
			}
			ScreenXRes = modes[closestindex].getWidth();
			ScreenYRes = modes[closestindex].getHeight();
			Display.setDisplayMode(modes[closestindex]);
			Display.setTitle("Voxels " + Version);
			Display.create();
			Mouse.create();
			Keyboard.create();
			Keyboard.enableRepeatEvents(true);
			if(!AL.isCreated())
				AL.create();
		}
		catch(LWJGLException e)
		{
			e.printStackTrace();
			System.exit(1);
		}
		boolean isFullscreen = false;
		boolean done = false;
		double lastFrameStartTime = Timer();
		double curTime = Timer();
		while(!done)
		{
			setFullscreen(isFullscreen);
			renderFrame();
			Display.update();
			world.generateChunks();
			{
				while(Mouse.next())
				{
					players.handleMouseUpDown(new MouseEvent());
				}
				while(Keyboard.next())
				{
					KeyboardEvent event = new KeyboardEvent();
					if(event.isDown && event.key == KEY_F4
					        && isKeyDown(KEY_ALT))
					{
						done = true;
						continue;
					}
					if(event.isDown && event.key == KEY_F11)
					{
						isFullscreen = !isFullscreen;
						continue;
					}
					players.handleKeyboardEvent(event);
				}
				if(players.handleMouseMove(Mouse.getX(), Display.getHeight()
				        - Mouse.getY() - 1, Mouse.isButtonDown(0)))
				{
					Mouse.setCursorPosition(ScreenXRes / 2, Display.getHeight()
					        - ScreenYRes / 2 - 1);
					if(!Mouse.isGrabbed())
						Mouse.setGrabbed(true);
				}
				else
				{
					if(Mouse.isGrabbed())
						Mouse.setGrabbed(false);
				}
				players.move();
			}
			world.move();
			Display.sync(60);
			curTime = Timer();
			frameDuration = curTime - lastFrameStartTime;
			internalSaveAll();
			lastFrameStartTime = curTime;
			if(Display.isCloseRequested())
				done = true;
		}
		saveAll(); // signal to save
		internalSaveAll(); // run save
		AL.destroy();
		Display.destroy();
		System.exit(0);
	}

	static boolean isLoading = false;

	private static JFileChooser makeFileChooser()
	{
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setFileFilter(new FileFilter()
		{
			@Override
			public String getDescription()
			{
				return "Voxels World";
			}

			@Override
			public boolean accept(File f)
			{
				if(f.exists())
				{
					if(f.isDirectory())
						return true;
					if(!f.isFile() || !f.canRead())
						return false;
					if(!isLoading && !f.canWrite())
						return false;
				}
				if(f.getName().toLowerCase().endsWith(".vw"))
					return true;
				return false;
			}
		});
		fileChooser.removeChoosableFileFilter(fileChooser.getAcceptAllFileFilter());
		fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		return fileChooser;
	}

	private static File saveFile = null;
	private static JFileChooser fileChooser = makeFileChooser();
	private static JProgressBar progressBar = new JProgressBar(0, 10000);
	static JLabel progressLabel = new JLabel("");

	private static JDialog genProgressDialog()
	{
		progressLabel.setHorizontalAlignment(SwingConstants.CENTER);
		JDialog progressDialog = new JDialog((JWindow)null, "Voxels " + Version);
		progressDialog.setLayout(new GridLayout(2, 1));
		progressDialog.add(progressLabel);
		progressDialog.add(progressBar);
		progressDialog.setSize(300, 75);
		return progressDialog;
	}

	static JDialog progressDialog = genProgressDialog();
	static String progressLabelText = "";

	private static void showProgressDialog(String labelText)
	{
		topOfProgressStack = null;
		progressLabelText = labelText;
		SwingUtilities.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				progressLabel.setText(progressLabelText);
				progressDialog.setVisible(true);
			}
		});
	}

	private static void hideProgressDialog()
	{
		topOfProgressStack = null;
		SwingUtilities.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				progressDialog.setVisible(false);
			}
		});
	}

	private static class SetProgressRunnable implements Runnable
	{
		private float progress;
		private JProgressBar progressBar;

		public SetProgressRunnable(float progress, JProgressBar progressBar)
		{
			this.progress = progress;
			this.progressBar = progressBar;
		}

		@Override
		public void run()
		{
			this.progressBar.setValue(Math.round(10000 * this.progress));
		}
	}

	private static void internalSetProgress(float progress)
	{
		try
		{
			SwingUtilities.invokeAndWait(new SetProgressRunnable(progress,
			                                                     progressBar));
		}
		catch(InterruptedException e)
		{
		}
		catch(InvocationTargetException e)
		{
		}
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

	static void pushProgress(float offset, float scale)
	{
		ProgressStackNode n = new ProgressStackNode();
		n.next = topOfProgressStack;
		n.scale = scale * getProgressScale();
		n.offset = getProgressOffset() + offset * getProgressScale();
		topOfProgressStack = n;
	}

	private static double lastSetProgressTime = -1;

	static void setProgress(float progress)
	{
		final double updatePeriod = 0.1;
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
		if(Display.isFullscreen())
		{
			try
			{
				Display.setFullscreen(false);
			}
			catch(LWJGLException e)
			{
				e.printStackTrace();
			}
		}
		if(Mouse.isGrabbed())
			Mouse.setGrabbed(false);
		isLoading = false;
		if(JFileChooser.APPROVE_OPTION == fileChooser.showSaveDialog(null))
		{
			saveFile = fileChooser.getSelectedFile();
			if(!saveFile.getName().toLowerCase().endsWith(".vw"))
				saveFile = new File(saveFile.getParentFile(),
				                    saveFile.getName() + ".vw");
			return true;
		}
		return false;
	}

	private static boolean needSave = false; // true if saveAll() has been
	                                         // called

	/**
	 * saves everything
	 */
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
			JOptionPane.showMessageDialog(null,
			                              "Can't save : " + e.getMessage(),
			                              "Voxels",
			                              JOptionPane.WARNING_MESSAGE);
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
		if(JFileChooser.APPROVE_OPTION == fileChooser.showOpenDialog(null))
		{
			saveFile = fileChooser.getSelectedFile();
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

	private static void loadAll()
	{
		boolean done = false;
		while(!done)
		{
			if(!getLoadFile())
				return;
			showProgressDialog("Loading...");
			InputStream fis = null;
			boolean needHide = true;
			try
			{
				fis = new BufferedInputStream(new FileInputStream(saveFile));
				DataInputStream dis = new DataInputStream(fis);
				World.read(dis);
				PlayerList.read(dis);
				dis.close();
				hideProgressDialog();
				needHide = false;
				done = true;
				didLoad = true;
			}
			catch(EOFException e)
			{
				hideProgressDialog();
				needHide = false;
				JOptionPane.showMessageDialog(null,
				                              "Can't load : unexpected EOF",
				                              "Voxels",
				                              JOptionPane.ERROR_MESSAGE);
			}
			catch(IOException e)
			{
				hideProgressDialog();
				needHide = false;
				JOptionPane.showMessageDialog(null,
				                              "Can't load : " + e.getMessage(),
				                              "Voxels",
				                              JOptionPane.ERROR_MESSAGE);
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
	}

	private Main()
	{
	}

	/**
	 * @param filename
	 *            the file to open
	 * @return the created <code>InputStream</code>
	 * @throws FileNotFoundException
	 *             if the file couldn't be opened
	 */
	public static InputStream
	    getInputStream(String filename) throws FileNotFoundException
	{
		InputStream in = Main.class.getResourceAsStream(File.separator + "res"
		        + File.separator + filename);
		if(in == null)
		{
			in = new FileInputStream("res" + File.separator + filename);
		}
		return in;
	}

	/**
	 * load a audio file
	 * 
	 * @param filename
	 *            the file to load
	 * @return the loaded <code>Audio</code> or <code>null</code>
	 */
	@SuppressWarnings("resource")
	public static Audio loadAudio(String filename)
	{
		InputStream in = null;
		Audio retval = null;
		try
		{
			in = getInputStream(filename);
			if(in == null)
				return null;
			retval = AudioLoader.getAudio("OGG", in);
		}
		catch(IOException e)
		{
		}
		finally
		{
			try
			{
				if(in != null)
					in.close();
			}
			catch(IOException e)
			{
			}
		}
		return retval;
	}

	static void play(Audio a)
	{
		if(a != null)
			a.playAsSoundEffect(1.0f, 1.0f, false);
	}

	static Audio clickAudio = loadAudio("click.ogg");
	static Audio popAudio = loadAudio("pop.ogg");
	static Audio destructAudio = loadAudio("destruct.ogg");
}
