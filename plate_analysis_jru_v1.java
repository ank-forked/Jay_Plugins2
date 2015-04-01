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
import ij.plugin.frame.*;
import jalgs.*;
import ij.text.*;
import jguis.*;

public class plate_analysis_jru_v1 implements PlugIn {

	public void run(String arg) {
		int totpts=1536;
		double xyratio=1.5;
		float radius=8.0f;
		int avgx=2;
		int avgy=2;
		GenericDialog gd=new GenericDialog("Options");
		gd.addNumericField("#_of_spots",totpts,0);
		gd.addNumericField("XY_ratio",xyratio,5,15,null);
		gd.addNumericField("spot_radius (pixels)",radius,5,15,null);
		gd.addNumericField("#_x_replicates",avgx,0);
		gd.addNumericField("#_y_replicates",avgy,0);
		gd.addCheckbox("Circ_Background",false);
		gd.addCheckbox("Show_Rois",false);
		gd.addCheckbox("Output_2D_Plot",false);
		gd.showDialog(); if(gd.wasCanceled()){return;}
		totpts=(int)gd.getNextNumber();
		xyratio=gd.getNextNumber();
		radius=(float)gd.getNextNumber();
		avgx=(int)gd.getNextNumber();
		avgy=(int)gd.getNextNumber();
		boolean circsub=gd.getNextBoolean();
		boolean showrois=gd.getNextBoolean();
		boolean outplot=gd.getNextBoolean();
		ImagePlus imp=WindowManager.getCurrentImage();
		int width=imp.getWidth(); int height=imp.getHeight();
		int intrad=1+(int)Math.ceil(radius);
		Roi roi=imp.getRoi();
		if(roi==null){
			IJ.showMessage("Requires Polygon with four vertices at\n four corner colonies starting at upper left");
			return;
		}
		//the roi encompasses the four corners of the plating region
		//use bilinear interpolation to find a grid of points corresponding to the pin positions
		Polygon poly=roi.getPolygon();
		int ypts=(int)Math.sqrt((double)totpts/xyratio);
		int xpts=(int)((double)totpts/(double)ypts);
		float xinc1=(float)(poly.xpoints[3]-poly.xpoints[0])/(float)(ypts-1);
		float xinc2=(float)(poly.xpoints[2]-poly.xpoints[1])/(float)(ypts-1);
		float yinc1=(float)(poly.ypoints[3]-poly.ypoints[0])/(float)(ypts-1);
		float yinc2=(float)(poly.ypoints[2]-poly.ypoints[1])/(float)(ypts-1);
		RoiManager rman=RoiManager.getInstance();
		if(rman==null) rman=new RoiManager();
		//rman.runCommand("show all");
		totpts=xpts*ypts;
		int[][] wells=new int[2][totpts];
		int counter=0;
		for(int i=0;i<ypts;i++){
			float xstart=(float)poly.xpoints[0]+(float)i*xinc1;
			float ystart=(float)poly.ypoints[0]+(float)i*yinc1;
			float xend=(float)poly.xpoints[1]+(float)i*xinc2;
			float yend=(float)poly.ypoints[1]+(float)i*yinc2;
			float xinc3=(xend-xstart)/(float)(xpts-1);
			float yinc3=(yend-ystart)/(float)(xpts-1);
			for(int j=0;j<xpts;j++){
				wells[0][counter]=(int)(xstart+xinc3*(float)j);
				wells[1][counter]=(int)(ystart+yinc3*(float)j);
				if(showrois) rman.addRoi(new OvalRoi(wells[0][counter]-intrad,wells[1][counter]-intrad,2*intrad,2*intrad));
				counter++;
			}
		}
		//now use the minimum intensity within the roi as the background
		//note that this assumes a uniform gel background
		//applications that do not support this should use advanced background
		//subtraction tools to make the background uniform
		float[] pixels=(float[])imp.getProcessor().convertToFloat().getPixels();

		boolean[] mask=new boolean[2*intrad*2*intrad];
		boolean[] circ=new boolean[2*intrad*2*intrad];
		float rad2=radius*radius;
		float circrad2=(radius+1.0f)*(radius+1.0f);
		int area=0;
		int circarea=0;
		for(int i=0;i<2*intrad;i++){
			float ydist2=(float)(i-intrad)*(float)(i-intrad);
			for(int j=0;j<2*intrad;j++){
				float dist2=ydist2+(float)(j-intrad)*(float)(j-intrad);
				if(dist2<=rad2){
					mask[j+i*2*intrad]=true;
					area++;
				} else {
					if(dist2<=circrad2){
						circ[j+i*2*intrad]=true;
						circarea++;
					}
				}
			}
		}
		
		if(showrois) IJ.log("spot area (pix) = "+area);

		//float background=jstatistics.getstatistic("Min",pixels,width,height,poly,null);
		/*float background=jstatistics.getstatistic("Percentile",pixels,width,height,poly,new float[]{5.0f});
		if(showrois) IJ.log("overall background = "+background);
		float[] stats=new float[totpts];
		float[] circstats=new float[totpts];
		for(int i=0;i<totpts;i++){
			int counter2=0;
			for(int j=0;j<2*intrad;j++){
				int ypos=wells[1][i]-intrad+j;
				for(int k=0;k<2*intrad;k++){
					int xpos=wells[0][i]-intrad+k;
					if(mask[counter2]) stats[i]+=(pixels[xpos+ypos*width]-background);
					if(circ[counter2]) circstats[i]+=(pixels[xpos+ypos*width]-background);
					counter2++;
				}
			}
			if(circsub){
				float avgback=circstats[i]/(float)circarea;
				stats[i]-=avgback*(float)area;
			}
		}
		int newxpts=(int)((float)xpts/(float)avgx);
		int newypts=(int)((float)ypts/(float)avgy);
		float[][][] stats2=new float[2][newypts][newxpts];
		int ycounter=0;
		for(int i=0;i<ypts;i+=avgy){
			int xcounter=0;
			for(int j=0;j<xpts;j+=avgx){
				float[] subarray=new float[avgx*avgy];
				for(int k=0;k<avgy;k++){
					for(int l=0;l<avgx;l++){
						subarray[l+k*avgx]=stats[(i+k)*xpts+j+l];
					}
				}
				stats2[0][ycounter][xcounter]=jstatistics.getstatistic("Avg",subarray,null);
				stats2[1][ycounter][xcounter]=jstatistics.getstatistic("StErr",subarray,null);
				xcounter++;
			}
			ycounter++;
		}*/
		float[][][] stats2=analyzePlate(pixels,width,height,poly,intrad,mask,circ,area,circarea,wells,xpts,ypts,avgx,avgy,circsub,showrois);
		int newxpts=(int)((float)xpts/(float)avgx);
		int newypts=(int)((float)ypts/(float)avgy);
		TextWindow tw=new TextWindow("Plate Densities",table_tools.createcollabels(newxpts),table_tools.print_float_array(stats2[0]),400,200);
		TextWindow tw2=new TextWindow("Plate Errors",table_tools.createcollabels(newxpts),table_tools.print_float_array(stats2[1]),400,200);
		if(outplot){
			float[] newstats=new float[stats2[0].length*stats2[0][0].length];
			float[] errors=newstats.clone();
			int counter10=0;
			for(int i=0;i<stats2[0].length;i++){
				for(int j=0;j<stats2[0][0].length;j++){
					newstats[counter10]=stats2[0][i][j];
					errors[counter10]=stats2[1][i][j];
					counter10++;
				}
			}
			PlotWindow4 pw=new PlotWindow4("2D Plate Intensities","group","Intensity",newstats);
			pw.addErrorBars(errors);
			pw.draw();
		}
	}

	public float[][][] analyzePlate(float[] pixels,int width,int height,Polygon boundary,int intrad,boolean[] mask,boolean[] circ,int area,int circarea,int[][] wells,int xpts,int ypts,int avgx,int avgy,boolean circsub,boolean showrois){
		int totpts=xpts*ypts;
		float[] stats=new float[totpts];
		float[] circstats=new float[totpts];
		float background=jstatistics.getstatistic("Percentile",pixels,width,height,boundary,new float[]{5.0f});
		if(showrois) IJ.log("overall background = "+background);
		for(int i=0;i<totpts;i++){
			int counter2=0;
			for(int j=0;j<2*intrad;j++){
				int ypos=wells[1][i]-intrad+j;
				for(int k=0;k<2*intrad;k++){
					int xpos=wells[0][i]-intrad+k;
					if(mask[counter2]) stats[i]+=(pixels[xpos+ypos*width]-background);
					if(circ[counter2]) circstats[i]+=(pixels[xpos+ypos*width]-background);
					counter2++;
				}
			}
			if(circsub){
				float avgback=circstats[i]/(float)circarea;
				stats[i]-=avgback*(float)area;
			}
		}
		int newxpts=(int)((float)xpts/(float)avgx);
		int newypts=(int)((float)ypts/(float)avgy);
		float[][][] stats2=new float[2][newypts][newxpts];
		int ycounter=0;
		for(int i=0;i<ypts;i+=avgy){
			int xcounter=0;
			for(int j=0;j<xpts;j+=avgx){
				float[] subarray=new float[avgx*avgy];
				for(int k=0;k<avgy;k++){
					for(int l=0;l<avgx;l++){
						subarray[l+k*avgx]=stats[(i+k)*xpts+j+l];
					}
				}
				stats2[0][ycounter][xcounter]=jstatistics.getstatistic("Avg",subarray,null);
				stats2[1][ycounter][xcounter]=jstatistics.getstatistic("StErr",subarray,null);
				xcounter++;
			}
			ycounter++;
		}
		return stats2;
	}

}
