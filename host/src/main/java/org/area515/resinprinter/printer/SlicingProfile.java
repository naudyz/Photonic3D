package org.area515.resinprinter.printer;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.area515.resinprinter.inkdetection.PrintMaterialDetector;
import org.area515.resinprinter.inkdetection.PrintMaterialDetectorSettings;
import org.area515.resinprinter.job.InkDetector;
import org.area515.resinprinter.job.PrintJob;
import org.area515.util.TemplateEngine;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

@XmlRootElement(name="SliceBuildConfig")
public class SlicingProfile implements Named {
    private static final Logger logger = LogManager.getLogger();

    public static class TwoDimensionalSettings {
        @XmlElement(name="Font")
    	private Font font;
        @XmlElement(name="PlatformHeightMM")
        private Double platformHeightMM;
        @XmlElement(name="ExtrusionHeightMM")
        private Double extrusionHeightMM;
        @XmlElement(name="PlatformCalculator")
        private String platformCalculator;
        @XmlElement(name="EdgeDetectionDisabled")
        private Boolean edgeDetectionDisabled;
        @XmlElement(name="ScaleImageToFitPrintArea")
        private Boolean scaleImageToFitPrintArea;
        
        @XmlTransient
		public Font getFont() {
			return font;
		}
		public void setFont(Font font) {
			this.font = font;
		}
		
		@XmlTransient
		public Double getPlatformHeightMM() {
			return platformHeightMM;
		}
		public void setPlatformHeightMM(Double platformHeightMM) {
			this.platformHeightMM = platformHeightMM;
		}
		
		@XmlTransient
		public Boolean isEdgeDetectionDisabled() {
			return edgeDetectionDisabled;
		}
		public void setEdgeDetectionDisabled(Boolean edgeDetectionDisabled) {
			this.edgeDetectionDisabled = edgeDetectionDisabled;
		}
		
		@XmlTransient
		public Double getExtrusionHeightMM() {
			return extrusionHeightMM;
		}
		public void setExtrusionHeightMM(Double extrusionHeightMM) {
			this.extrusionHeightMM = extrusionHeightMM;
		}
		
		@XmlTransient
		public String getPlatformCalculator() {
			return platformCalculator;
		}
		public void setPlatformCalculator(String platformCalculator) {
			this.platformCalculator = platformCalculator;
		}
		
		@XmlTransient
		public Boolean isScaleImageToFitPrintArea() {
			return scaleImageToFitPrintArea;
		}
		public void setScaleImageToFitPrintArea(Boolean scaleImageToFitPrintArea) {
			this.scaleImageToFitPrintArea = scaleImageToFitPrintArea;
		}
    }
    
    public static class Font {
        @XmlElement(name="Name")
    	private String name;
        @XmlElement(name="Size")
    	private int size;
        
        public Font() {}
        
        public Font(String name, int size) {
        	this.name = name;
        	this.size = size;
        }
        
        @XmlTransient
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		
        @XmlTransient
		public int getSize() {
			return size;
		}
		public void setSize(int size) {
			this.size = size;
		}
    }
    
    public static class InkConfig {
		@XmlElement(name="PrintMaterialDetector")
		private String printMaterialDetector;
		@XmlElement(name="PercentageOfPrintMaterialConsideredEmpty")
		private float percentageConsideredEmpty;
		@XmlElement(name="Name")
	    private String name;
	    @XmlElement(name="SliceHeight")
	    private double sliceHeight;
	    @XmlElement(name="LayerTime")
	    private int layerTime;
	    @XmlElement(name="FirstLayerTime")
	    private int firstLayerTime;
		@XmlElement(name="NumberofBottomLayers")
	    private int numberOfFirstLayers;
		@XmlElement(name="ResinPriceL")
	    private double resinPriceL;
		private InkDetector detector;
		@XmlElement(name="PrintMaterialDetectorSettings")
		private PrintMaterialDetectorSettings printMaterialDetectorSettings;

		@XmlTransient
		public PrintMaterialDetectorSettings getPrintMaterialDetectorSettings() {
			return printMaterialDetectorSettings;
		}
		public void setPrintMaterialDetectorSettings(PrintMaterialDetectorSettings printMaterialDetectorSettings) {
			this.printMaterialDetectorSettings = printMaterialDetectorSettings;
		}
		
		@XmlTransient
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		
		@XmlTransient
		public double getSliceHeight() {
			return sliceHeight;
		}
		public void setSliceHeight(double sliceHeight) {
			this.sliceHeight = sliceHeight;
		}
		
		@XmlTransient
		public int getNumberOfFirstLayers() {
			return numberOfFirstLayers;
		}
		public void setNumberOfFirstLayers(int numberOfFirstLayers) {
			this.numberOfFirstLayers = numberOfFirstLayers;
		}
		
		@XmlTransient
		public double getResinPriceL() {
			return resinPriceL;
		}
		public void setResinPriceL(double resinPriceL) {
			this.resinPriceL = resinPriceL;
		}
		
		@XmlTransient
		public int getExposureTime() {
			return layerTime;
		}
		public void setExposureTime(int layerTime) {
			this.layerTime = layerTime;
		}
		
		@XmlTransient
		public int getFirstLayerExposureTime() {
			return firstLayerTime;
		}
		public void setFirstLayerExposureTime(int firstLayerTime) {
			this.firstLayerTime = firstLayerTime;
		}

		@XmlTransient
		public String getPrintMaterialDetector() {
			return printMaterialDetector;
		}
		public void setPrintMaterialDetector(String printMaterialDetector) {
			this.printMaterialDetector = printMaterialDetector;
		}
		
		@XmlTransient
		public float getPercentageOfInkConsideredEmpty() {
			return percentageConsideredEmpty;
		}
		public void setPercentageOfInkConsideredEmpty(float percentageConsideredEmpty) {
			this.percentageConsideredEmpty = percentageConsideredEmpty;
		}

		public InkDetector getInkDetector(PrintJob printJob) {
			if (this.detector != null) {
				return this.detector;
			}
			
			String detectorClass = getPrintMaterialDetector();
			if (detectorClass == null || detectorClass.trim().isEmpty()) {
				return null;
			}
			
			try {
				this.detector = new InkDetector(printJob.getPrinter(), printJob, ((Class<PrintMaterialDetector>)Class.forName(detectorClass)).newInstance(), getPrintMaterialDetectorSettings(), percentageConsideredEmpty);
				return this.detector;
			} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
				logger.info("Failed to load PrintMaterialDetector:{}", detector);
				return null;
			}
		}
	}

    @XmlElement(name="DotsPermmX")
	private double dotsPermmX;
    @XmlElement(name="DotsPermmY")
	private double dotsPermmY;
    @XmlElement(name="XResolution")
	private int xResolution;
    @XmlElement(name="YResolution")
	private int yResolution;
    @XmlElement(name="BlankTime")
	private int blankTime;
    @XmlElement(name="PlatformTemp")
	private int platformTemp;
    @XmlElement(name="ExportSVG")
	private int exportSVG;
    @XmlElement(name="Export")
	private boolean export;
    @XmlElement(name="ExportPNG")
	private boolean exportPNG;
    @XmlElement(name="XOffset")
	private Integer xOffset;
    @XmlElement(name="YOffset")
	private Integer yOffset;
    @XmlElement(name="Direction")
	private BuildDirection direction;
    @XmlElement(name="LiftDistance")
	private double liftDistance;
    @XmlElement(name="SlideTiltValue")
	private int slideTiltValue;
    @XmlElement(name="AntiAliasing")
	private boolean antiAliasing;
    @XmlElement(name="UseMainLiftGCode")
	private boolean useMainLiftGCode;
    @XmlElement(name="AntiAliasingValue")
	private double antiAliasingValue;
    // FIXME: 2017/9/25 zyd add for parameters -s
    @XmlElement(name="LiftFeedSpeed")
	private double liftFeedSpeed;
    @XmlElement(name="LiftRetractSpeed")
	private double liftRetractSpeed;
    // FIXME: 2017/9/25 zyd add for parameters -e
    @XmlElement(name="ExportOption")
	private ExportOption exportOption;
    @XmlElement(name="FlipX")
	private boolean flipX;
    @XmlElement(name="FlipY")
	private boolean flipY;
    @XmlElement(name="Notes")
	private String notes;
	private String gCodeHeader;
	private String gCodeFooter;
	private String gCodePreslice;
	// FIXME: 2017/9/20 zyd add for lift gcode -s
	private String gCodeLiftFeed;
	private String gCodeLiftRetract;
	// FIXME: 2017/9/20 zyd add for lift gcode -e
	private String gCodeShutter;
	// FIXME: 2017/9/18 zyd add for run gcode as job pause -s
	private String gCodeBeforePause;
	private String gCodeAfterPause;
	// FIXME: 2017/9/18 zyd add for run gcode as job pause -e
	@XmlElement(name="ZLiftDistanceCalculator")
	private String zLiftDistanceCalculator;
	// FIXME: 2017/9/25 zyd add for parameters -s
	@XmlElement(name="ZLiftFeedSpeedCalculator")
	private String zLiftFeedSpeedCalculator;
	@XmlElement(name="ZLiftRetractSpeedCalculator")
	private String zLiftRetractSpeedCalculator;
	// FIXME: 2017/9/25 zyd add for parameters -e
	@XmlElement(name="ProjectorGradientCalculator")
	private String projectorGradientCalculator;
	@XmlElement(name="ExposureTimeCalculator")
	private String exposureTimeCalculator;
	@XmlElement(name="SelectedInk")
	private String selectedInk;
    @XmlElement(name="MinTestExposure")
	private int minTestExposure;
    @XmlElement(name="TestExposureStep")
	private int testExposureStep;
    @XmlElement(name="InkConfig")
	private List<InkConfig> inkConfig;
    @XmlElement(name="TwoDimensionalSettings")
    private TwoDimensionalSettings twoDimensionalSettings;
	// FIXME: 2017/11/6 zyd add for increase exposure time if the job has been paused -s
	@XmlElement(name="ResumeLayerTime")
	private int resumeLayerTime;
	// FIXME: 2017/11/6 zyd add for increase exposure time if the job has been paused -e
	// FIXME: 2017/9/15 zyd add for set delay time -s
	@XmlElement(name="DelayTimeBeforeSolidify")
	private int delayTimeBeforeSolidify;
	@XmlElement(name="DelayTimeAfterSolidify")
	private int delayTimeAfterSolidify;
	@XmlElement(name="DelayTimeAsLiftedTop")
	private int delayTimeAsLiftedTop;
	@XmlElement(name="DelayTimeForAirPump")
	private int delayTimeForAirPump;
	// FIXME: 2017/9/15 zyd add for set delay time -e
	// FIXME: 2017/9/18 zyd add for set Z travel -s
	@XmlElement(name="ZTravel")
	private int zTravel;
	// FIXME: 2017/9/18 zyd add for set Z travel -e
	// FIXME: 2017/10/24 zyd add for set detection and parameter enabled -s
	@XmlElement(name="ParameterEnabled")
	private boolean parameterEnabled;
	@XmlElement(name="DetectionEnabled")
	private boolean detectionEnabled;
	// FIXME: 2017/10/24 zyd add for set detection and parameter enabled -e
	private String name;
    
	@XmlTransient
	@JsonProperty
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	@XmlTransient
	public int getSlideTiltValue() {
		return slideTiltValue;
	}
	public void setSlideTiltValue(int slideTiltValue) {
		this.slideTiltValue = slideTiltValue;
	}
	
	@XmlTransient
	public int getxResolution() {
		return xResolution;
	}
	public void setxResolution(int xResolution) {
		this.xResolution = xResolution;
	}
	
	@XmlTransient
	public int getyResolution() {
		return yResolution;
	}
	public void setyResolution(int yResolution) {
		this.yResolution = yResolution;
	}
	
	@XmlTransient
	public double getDotsPermmX() {
		return dotsPermmX;
	}
	public void setDotsPermmX(double dotsPermmX) {
		this.dotsPermmX = dotsPermmX;
	}
	
	@XmlTransient
	public double getDotsPermmY() {
		return dotsPermmY;
	}
	public void setDotsPermmY(double dotsPermmY) {
		this.dotsPermmY = dotsPermmY;
	}
	
	@XmlTransient
	public BuildDirection getDirection() {
		return direction;
	}
	public void setDirection(BuildDirection direction) {
		this.direction = direction;
	}
	
	@XmlTransient
	public double getLiftDistance() {
		return liftDistance;
	}
	public void setLiftDistance(double liftDistance) {
		this.liftDistance = liftDistance;
	}

    // FIXME: 2017/9/25 zyd add for parameters -s
	@XmlTransient
	public double getLiftFeedSpeed() {
		return liftFeedSpeed;
	}
	public void setLiftFeedSpeed(double liftFeedSpeed) {
		this.liftFeedSpeed = liftFeedSpeed;
	}

	@XmlTransient
	public double getLiftRetractSpeed() {
		return liftRetractSpeed;
	}
	public void setLiftRetractSpeed(double liftRetractSpeed) {
		this.liftRetractSpeed = liftRetractSpeed;
	}
	// FIXME: 2017/9/25 zyd add for parameters -e
	
	@XmlTransient
	public boolean isFlipX() {
		return flipX;
	}
	public void setFlipX(boolean flipX) {
		this.flipX = flipX;
	}
	
	@XmlTransient
	public boolean isFlipY() {
		return flipY;
	}
	public void setFlipY(boolean flipY) {
		this.flipY = flipY;
	}
	
	@XmlTransient
	public String getNotes() {
		return notes;
	}
	public void setNotes(String notes) {
		this.notes = notes;
	}
	
	@XmlTransient
	public TwoDimensionalSettings getTwoDimensionalSettings() {
		return twoDimensionalSettings;
	}
	public void setTwoDimensionalSettings(TwoDimensionalSettings twoDimensionalSettings) {
		this.twoDimensionalSettings = twoDimensionalSettings;
	}
	
	public String getgCodeHeader() {
		return gCodeHeader;
	}
	public void setgCodeHeader(String gCodeHeader) {
		this.gCodeHeader = TemplateEngine.convertToFreeMarkerTemplate(gCodeHeader);
	}
	
	public String getgCodeFooter() {
		return gCodeFooter;
	}
	public void setgCodeFooter(String gCodeFooter) {
		this.gCodeFooter = TemplateEngine.convertToFreeMarkerTemplate(gCodeFooter);
	}
	
	public String getgCodePreslice() {
		return gCodePreslice;
	}
	public void setgCodePreslice(String gCodePreslice) {
		this.gCodePreslice = TemplateEngine.convertToFreeMarkerTemplate(gCodePreslice);
	}

	// FIXME: 2017/9/20 zyd add for lift gcode -s
	public String getgCodeLiftFeed() {
		return gCodeLiftFeed;
	}
	public void setgCodeLiftFeed(String gCodeLiftFeed) {
		this.gCodeLiftFeed = TemplateEngine.convertToFreeMarkerTemplate(gCodeLiftFeed);
	}

	public String getgCodeLiftRetract() {
		return gCodeLiftRetract;
	}
	public void setgCodeLiftRetract(String gCodeLiftRetract) {
		this.gCodeLiftRetract = TemplateEngine.convertToFreeMarkerTemplate(gCodeLiftRetract);
	}
	// FIXME: 2017/9/20 zyd add for lift gcode -e
	
	public String getgCodeShutter() {
		return gCodeShutter;
	}
	public void setgCodeShutter(String gCodeShutter) {
		this.gCodeShutter = TemplateEngine.convertToFreeMarkerTemplate(gCodeShutter);
	}

	// FIXME: 2017/9/18 zyd add for run gcode as job pause -s
	public String getgCodeBeforePause() {
		return gCodeBeforePause;
	}
	public void setgCodeBeforePause(String gCodeBeforePause) {
		this.gCodeBeforePause = TemplateEngine.convertToFreeMarkerTemplate(gCodeBeforePause);
	}

	public String getgCodeAfterPause() {
		return gCodeAfterPause;
	}
	public void setgCodeAfterPause(String gCodeAfterPause) {
		this.gCodeAfterPause = TemplateEngine.convertToFreeMarkerTemplate(gCodeAfterPause);
	}
	// FIXME: 2017/9/18 zyd add for run gcode as job pause -e

	@XmlTransient
	public List<InkConfig> getInkConfigs() {
		return inkConfig;
	}
	public void setInkConfigs(List<InkConfig> inkConfig) {
		this.inkConfig = inkConfig;
	}
	
	@XmlTransient
	public String getSelectedInkConfigName() {
		return selectedInk;
	}
	public void setSelectedInkConfigName(String selectedInk) {
		this.selectedInk = selectedInk;
	}
	
	@XmlTransient
	@JsonProperty
	public Integer getSelectedInkConfigIndex() {
		for (int t = 0; t < inkConfig.size(); t++) {
			InkConfig config = inkConfig.get(t);
			if (config.getName().equals(selectedInk)) {
				return t;
			}
		}
		
		return null;
	}
	private void setSelectedInkConfigIndex(Integer selectedIndex) {
	}
	
	@JsonIgnore
	public InkConfig getSelectedInkConfig() {
		for (InkConfig currentInkConfig : inkConfig) {
			if (currentInkConfig.getName().equals(selectedInk)) {
				return currentInkConfig;
			}
		}
		
		return null;
	}

	@XmlTransient
	public String getzLiftDistanceCalculator() {
		return zLiftDistanceCalculator;
	}
	public void setzLiftDistanceCalculator(String zLiftDistanceCalculator) {
		this.zLiftDistanceCalculator = zLiftDistanceCalculator;
	}

	// FIXME: 2017/9/25 zyd add for parameters -s
	@XmlTransient
	public String getzLiftFeedSpeedCalculator() {
		return zLiftFeedSpeedCalculator;
	}
	public void setzLiftFeedSpeedCalculator(String zLiftFeedSpeedCalculator) {
		this.zLiftFeedSpeedCalculator = zLiftFeedSpeedCalculator;
	}

	@XmlTransient
	public String getzLiftRetractSpeedCalculator() {
		return zLiftRetractSpeedCalculator;
	}
	public void setzLiftRetractSpeedCalculator(String zLiftRetractSpeedCalculator) {
		this.zLiftRetractSpeedCalculator = zLiftRetractSpeedCalculator;
	}
	// FIXME: 2017/9/25 zyd add for parameters -e
	
	@XmlTransient
	public String getProjectorGradientCalculator() {
		return projectorGradientCalculator;
	}
	public void setProjectorGradientCalculator(String projectorGradientCalculator) {
		this.projectorGradientCalculator = projectorGradientCalculator;
	}
	
	@XmlTransient
	public String getExposureTimeCalculator() {
		return exposureTimeCalculator;
	}
	public void setExposureTimeCalculator(String exposureTimeCalculator) {
		this.exposureTimeCalculator = exposureTimeCalculator;
	}

	// FIXME: 2017/11/6 zyd add for increase exposure time if the job has been paused -s
	@XmlTransient
	public int getResumeLayerExposureTime() {
		return resumeLayerTime;
	}
	public void setResumeLayerExposureTime(int resumeLayerTime) {
		this.resumeLayerTime = resumeLayerTime;
	}
	// FIXME: 2017/11/6 zyd add for increase exposure time if the job has been paused -e

	// FIXME: 2017/9/15 zyd add for set delay time -s
	@XmlTransient
	public int getDelayTimeBeforeSolidify() {
		return delayTimeBeforeSolidify;
	}
	public void setDelayTimeBeforeSolidify(int delayTimeBeforeSolidify) {
		this.delayTimeBeforeSolidify = delayTimeBeforeSolidify;
	}

	@XmlTransient
	public int getDelayTimeAfterSolidify() {
		return delayTimeAfterSolidify;
	}
	public void setDelayTimeAfterSolidify(int delayTimeAfterSolidify) {
		this.delayTimeAfterSolidify = delayTimeAfterSolidify;
	}

	@XmlTransient
	public int getDelayTimeAsLiftedTop() {
		return delayTimeAsLiftedTop;
	}
	public void setDelayTimeAsLiftedTop(int delayTimeAsLiftedTop) {
		this.delayTimeAsLiftedTop = delayTimeAsLiftedTop;
	}

	@XmlTransient
	public int getDelayTimeForAirPump() {
		return delayTimeForAirPump;
	}
	public void setDelayTimeForAirPump(int delayTimeForAirPump) {
		this.delayTimeForAirPump = delayTimeForAirPump;
	}
	// FIXME: 2017/9/15 zyd add for set delay time -e

	// FIXME: 2017/9/18 zyd add for set Z travel -s
	@XmlTransient
	public int getZTravel() {
		return zTravel;
	}
	public void setZTravel(int zTravel) {
		this.zTravel = zTravel;
	}
	// FIXME: 2017/9/18 zyd add for set Z travel -e

	// FIXME: 2017/10/24 zyd add for set detection and parameter enabled -s
	@XmlTransient
	public boolean getParameterEnabled() {
		return parameterEnabled;
	}
	public void setParameterEnabled(boolean parameterEnabled) {
		this.parameterEnabled = parameterEnabled;
	}

	@XmlTransient
	public boolean getDetectionEnabled() {
		return detectionEnabled;
	}
	public void setDetectionEnabled(boolean detectionEnabled) {
		this.detectionEnabled = detectionEnabled;
	}
	// FIXME: 2017/10/24 zyd add for set detection and parameter enabled -e

	public String toString() {
		return getName();
	}
}
