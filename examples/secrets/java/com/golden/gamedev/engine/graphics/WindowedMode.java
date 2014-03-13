/*
 * Copyright (c) 2008 Golden T Studios.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.golden.gamedev.engine.graphics;

// JFC
import java.awt.Canvas;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.image.BufferStrategy;
import java.awt.image.VolatileImage;

import com.golden.gamedev.engine.BaseGraphics;
import com.golden.gamedev.util.ImageUtil;

/**
 * Graphics engine for Windowed Environment.
 * <p>
 * 
 * See {@link com.golden.gamedev.engine.BaseGraphics} for how to use graphics
 * engine separated from Golden T Game Engine (GTGE) Frame Work.
 */
public class WindowedMode implements BaseGraphics {
	
	/** ************************ HARDWARE DEVICE ******************************** */
	
	/**
	 * The graphics device that constructs this graphics engine.
	 */
	public static final GraphicsDevice DEVICE = GraphicsEnvironment
	        .getLocalGraphicsEnvironment().getDefaultScreenDevice();
	
	/**
	 * The graphics configuration that constructs this graphics engine.
	 */
	public static final GraphicsConfiguration CONFIG = WindowedMode.DEVICE
	        .getDefaultConfiguration();
	
	/** *************************** AWT COMPONENT ******************************* */
	
	private Frame frame; // top frame where the canvas is put
	private Canvas canvas;
	
	private Dimension size;
	
	/** *************************** BACK BUFFER ********************************* */
	
	private VolatileImage offscreen; // backbuffer image
	
	private BufferStrategy strategy;
	
	// current graphics context
	private Graphics2D currentGraphics;
	
	/** ************************************************************************* */
	/** ***************************** CONSTRUCTOR ******************************* */
	/** ************************************************************************* */
	
	/**
	 * Creates new instance of Windowed Graphics Engine with specified size, and
	 * whether want to use bufferstrategy or volatile image.
	 * @param d The resolution of the window.
	 * @param bufferstrategy If a buffer strategy shall be used.
	 */
	public WindowedMode(Dimension d, boolean bufferstrategy) {
		this.size = d;
		// sets game frame
		this.frame = new Frame("Golden T Game Engine", WindowedMode.CONFIG);
		try {
			// set frame icon
			this.frame.setIconImage(ImageUtil.getImage(WindowedMode.class
			        .getResource("Icon.png")));
		}
		catch (Exception e) {
		}
		this.frame.addWindowListener(WindowExitListener.getInstance());
		this.frame.setResizable(false); // non resizable frame
		this.frame.setIgnoreRepaint(true); // turn off all paint events
		// since we doing active rendering
		
		// frame title bar and border (frame insets) makes
		// game screen smaller than requested size
		// we must enlarge the frame by it's insets size

		this.frame.setVisible(true);
		Insets inset = this.frame.getInsets();
		this.frame.setVisible(false);
		this.frame.setSize(this.size.width + inset.left + inset.right,
		      this.size.height + inset.top + inset.bottom);

	   // CR: FIX FOR JDK 1.7 to avoid "adding a container to a container on a different 
	   // GraphicsDevice" error
	   // create canvas after Frame made visible and use gc from frame
	   
      // the active component where the game drawn
      this.canvas = new Canvas(this.frame.getGraphicsConfiguration());
      this.canvas.setIgnoreRepaint(true);
      this.canvas.setSize(this.size);
      
		this.frame.add(this.canvas);
		this.frame.pack();
		this.frame.setLayout(null);
		this.frame.setLocationRelativeTo(null); // centering game frame
		if (this.frame.getX() < 0) {
			this.frame.setLocation(0, this.frame.getY());
		}
		if (this.frame.getY() < 0) {
			this.frame.setLocation(this.frame.getX(), 0);
		}

		this.frame.setVisible(true);
		
		// create backbuffer
		if (bufferstrategy) {
			bufferstrategy = this.createBufferStrategy();
		}
		
		if (!bufferstrategy) {
			this.createBackBuffer();
		}
		
		this.canvas.requestFocus();
	}
	
	/** ************************************************************************* */
	/** ************************ GRAPHICS FUNCTION ****************************** */
	/** ************************************************************************* */
	
	private boolean createBufferStrategy() {
		boolean bufferCreated;
		int num = 0;
		do {
			bufferCreated = true;
			try {
				// create bufferstrategy
				this.canvas.createBufferStrategy(2);
			}
			catch (Exception e) {
				// unable to create bufferstrategy!
				bufferCreated = false;
				try {
					Thread.sleep(200);
				}
				catch (InterruptedException excp) {
				}
			}
			
			if (num++ > 5) {
				break;
			}
		} while (!bufferCreated);
		
		if (!bufferCreated) {
			System.err.println("BufferStrategy is not available!");
			return false;
		}
		
		// wait until bufferstrategy successfully setup
		while (this.strategy == null) {
			try {
				this.strategy = this.canvas.getBufferStrategy();
			}
			catch (Exception e) {
			}
		}
		
		// wait until backbuffer successfully setup
		Graphics2D gfx = null;
		while (gfx == null) {
			// this process will throw an exception
			// if the backbuffer has not been created yet
			try {
				gfx = this.getBackBuffer();
			}
			catch (Exception e) {
			}
		}
		
		return true;
	}
	
	private void createBackBuffer() {
		if (this.offscreen != null) {
			// backbuffer is already created,
			// but not validate with current graphics configuration
			this.offscreen.flush();
			
			// clear old backbuffer
			this.offscreen = null;
		}
		
		this.offscreen = WindowedMode.CONFIG.createCompatibleVolatileImage(
		        this.size.width, this.size.height);
	}
	
	public Graphics2D getBackBuffer() {
		if (this.currentGraphics == null) {
			// graphics context is not created yet,
			// or have been disposed by calling flip()
			
			if (this.strategy == null) {
				// using volatile image
				if (this.offscreen.validate(WindowedMode.CONFIG) == VolatileImage.IMAGE_INCOMPATIBLE) {
					// volatile image is not valid
					this.createBackBuffer();
				}
				this.currentGraphics = this.offscreen.createGraphics();
				
			}
			else {
				// using buffer strategy
				this.currentGraphics = (Graphics2D) this.strategy
				        .getDrawGraphics();
			}
		}
		
		return this.currentGraphics;
	}
	
	public boolean flip() {
		// disposing current graphics context
		this.currentGraphics.dispose();
		this.currentGraphics = null;
		
		// show to screen
		if (this.strategy == null) {
			this.canvas.getGraphics().drawImage(this.offscreen, 0, 0, null);
			
			// sync the display on some systems.
			// (on linux, this fixes event queue problems)
			Toolkit.getDefaultToolkit().sync();
			
			return (!this.offscreen.contentsLost());
			
		}
		
			this.strategy.show();
			
			// sync the display on some systems.
			// (on linux, this fixes event queue problems)
			Toolkit.getDefaultToolkit().sync();
			
			return (!this.strategy.contentsLost());
	
	}
	
	/** ************************************************************************* */
	/** ******************* DISPOSING GRAPHICS ENGINE *************************** */
	/** ************************************************************************* */
	
	public void cleanup() {
		try {
			Thread.sleep(200L);
		}
		catch (InterruptedException e) {
		}
		
		try {
			// dispose the frame
			if (this.frame != null) {
				this.frame.dispose();
			}
		}
		catch (Exception e) {
			System.err.println("ERROR: Shutting down graphics context " + e);
			System.exit(-1);
		}
	}
	
	/** ************************************************************************* */
	/** *************************** PROPERTIES ********************************** */
	/** ************************************************************************* */
	
	public Dimension getSize() {
		return this.size;
	}
	
	public Component getComponent() {
		return this.canvas;
	}
	
	/**
	 * Returns the top level frame where this graphics engine is being put on.
	 * @return The top level frame.
	 */
	public Frame getFrame() {
		return this.frame;
	}
	
	/**
	 * Returns whether this graphics engine is using buffer strategy or volatile
	 * image.
	 * @return If a buffer strategy or a volatile image is used.
	 */
	public boolean isBufferStrategy() {
		return (this.strategy != null);
	}
	
	public String getGraphicsDescription() {
		return "Windowed Mode [" + this.getSize().width + "x"
		        + this.getSize().height + "]"
		        + ((this.strategy != null) ? " with BufferStrategy" : "");
	}
	
	public void setWindowTitle(String st) {
		this.frame.setTitle(st);
	}
	
	public String getWindowTitle() {
		return this.frame.getTitle();
	}
	
	public void setWindowIcon(Image icon) {
		try {
			this.frame.setIconImage(icon);
		}
		catch (Exception e) {
		}
	}
	
	public Image getWindowIcon() {
		return this.frame.getIconImage();
	}
	
}
