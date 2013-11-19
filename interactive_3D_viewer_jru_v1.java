/*******************************************************************************
 * Copyright (c) 2012 Jay Unruh, Stowers Institute for Medical Research.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/
import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.*;
import jguis.*;

public class interactive_3D_viewer_jru_v1 implements PlugIn {

	public void run(String arg) {
		ImagePlus imp=WindowManager.getCurrentImage();
		double psize=jutils.get_psize(imp);
		double zsize=jutils.get_pdepth(imp);
		GenericDialog gd=new GenericDialog("Options");
		gd.addNumericField("Z_Ratio",zsize/psize,5,15,null);
		gd.addNumericField("Threshold",5.0f,5,15,null);
		gd.addNumericField("#_of_threads",1,0);
		gd.showDialog(); if(gd.wasCanceled()){return;}
		float zratio=(float)gd.getNextNumber();
		float thresh=(float)gd.getNextNumber();
		int nthreads=(int)gd.getNextNumber();
		maxproj3D_panel_v2 mpp=new maxproj3D_panel_v2();
		mpp.init(imp,zratio,thresh,nthreads);
		maxproj3D_panel_v2.launch_frame(mpp);
	}

}